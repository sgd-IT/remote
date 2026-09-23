<template>
  <div class="wsplayer-wrapper">
    <!-- WSPlayer 容器：播放器动态加载后往此 div 内渲染 -->
    <div :id="containerId" class="wsplayer-container"></div>
    <!-- 加载/错误遮罩 -->
    <div v-if="showMask" class="wsplayer-mask" :class="{ 'mask-error': !!errorMsg }">
      <el-icon v-if="!errorMsg" class="is-loading mask-icon"><Loading /></el-icon>
      <span class="mask-text">{{ errorMsg || loadingText }}</span>
      <el-button v-if="errorMsg" size="small" type="primary" @click="retry">重试</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { PlayerManager } from '../../../../icc/PlayerManager'

const props = withDefaults(
  defineProps<{
    /** 摄像头对象（video_camera 表记录：deviceId / channelId / quality 等字段） */
    camera?: any | null
    /** 批量播放列表：传入数组时依次播放到窗口 0..N-1（在线摄像头，与 camera 互斥使用） */
    cameras?: any[] | null
    /** 初始显示窗口数（默认 4 画面，单窗口场景传 1） */
    num?: number
    /** 是否显示底部工具栏（分屏/全屏等，默认显示） */
    showControl?: boolean
  }>(),
  {
    camera: null,
    cameras: null,
    num: 4,
    showControl: true
  }
)

const emit = defineEmits<{
  /** 用户点击播放器窗口关闭按钮 */
  close: []
}>()

// 容器 id 全局唯一（多窗口实例）
let uidSeed = 0
const containerId = `wsplayer_${Date.now().toString(36)}_${(uidSeed++).toString(36)}`

let pm: any = null
let initialized = false
let playing = false
let pendingCamera: any = null
let pendingCameraList: any[] | null = null
let initTimeout: ReturnType<typeof setTimeout> | null = null
/** 内部批量关闭窗口的时间窗口（批量播放前 pm.close() 全关会触发 closeVideo 事件，此期间抑制 close 通知） */
let internalCloseUntil = 0

const loading = ref(true)
const loadingText = ref('播放器加载中...')
const errorMsg = ref<string | null>(null)
/** WSPlayer 当前选中的窗口索引（点击播放器内部窗口时更新） */
const currentWindowIndex = ref(0)

const showMask = computed(() => loading.value || !!errorMsg.value)

/** 组装 4 段式通道编码：channelId 含 $ 则直接使用，否则 deviceId$1$0$channelId（与后端规则一致） */
const buildChannelCode = (cam: any): string => {
  const channelId = cam?.channelId || ''
  if (channelId.includes('$')) return channelId
  return `${cam.deviceId}$1$0$${channelId}`
}

/** WSPlayer 事件回调（签名：method, data, err） */
const onMessage = (method: string, data: any, err: any) => {
  switch (method) {
    case 'initializationCompleted':
      initialized = true
      if (initTimeout) {
        clearTimeout(initTimeout)
        initTimeout = null
      }
      loadingText.value = '正在连接视频流...'
      // 初始化完成后播放等待中的摄像头
      if (pendingCamera) {
        const cam = pendingCamera
        pendingCamera = null
        doPlay(cam)
      } else if (pendingCameraList) {
        const list = pendingCameraList
        pendingCameraList = null
        playList(list)
      }
      break
    case 'realSuccess':
      playing = true
      loading.value = false
      errorMsg.value = null
      break
    case 'realError': {
      playing = false
      loading.value = false
      const info = data?.apiErrorInfo || data || {}
      errorMsg.value = info.errorMsg || info.message || '视频流播放失败'
      break
    }
    case 'talkSuccess':
      // 对讲成功（HTTPS 环境下可用）
      break
    case 'talkError':
      // HTTP 页面浏览器禁用麦克风时对讲不可用，属预期行为（HTTPS 部署后自动可用）
      console.warn('[WSPlayer] 对讲失败（HTTP 环境麦克风受限，HTTPS 部署后可用）:', data)
      break
    case 'selectWindowChanged':
      // 用户点击播放器内部窗口切换选中态
      currentWindowIndex.value = data.playIndex ?? data.selectIndex ?? 0
      break
    case 'closeVideo':
      // 用户点击播放器内的关闭按钮（切换视频引发的关闭不通知）
      if (data && !data.changeVideoFlag) {
        // 批量播放前的内部全关（playList 中 pm.close()）不通知父组件，避免误清空播放状态
        if (Date.now() < internalCloseUntil) return
        emit('close')
      }
      break
    case 'errorInfo':
      console.error('[WSPlayer] 错误信息:', data)
      break
    default:
      break
  }
}

/** 播放指定摄像头（windowIndex 指定目标窗口，缺省使用当前活动窗口） */
const doPlay = (cam: any, windowIndex?: number) => {
  if (!pm) return
  const channelCode = buildChannelCode(cam)
  // channelSeq 取 4 段式最后一段（对讲用）
  const channelSeq = channelCode.split('$').pop() || '0'
  const channelList = [
    {
      id: channelCode, // 通道编码（预览必填）
      deviceCode: cam.deviceId, // 设备编码（对讲必填）
      deviceType: cam.deviceType || '1',
      channelSeq, // 通道序号（对讲必填）
      cameraType: cam.cameraType || '1',
      capability: cam.capability || ''
    }
  ]
  loading.value = true
  loadingText.value = '正在连接视频流...'
  errorMsg.value = null
  // 替换已有窗口视频会触发 closeVideo 事件，抑制此期间的 close 通知（非用户主动关闭）
  internalCloseUntil = Date.now() + 800
  try {
    pm.playRealVideo({
      channelList,
      streamType: cam.quality || '2', // 码流类型：1-主码流 2-辅码流1
      windowIndex: windowIndex ?? currentWindowIndex.value
    })
  } catch (e: any) {
    loading.value = false
    errorMsg.value = e.message || '播放失败'
  }
}

/** 批量播放：关闭全部窗口后，依次将在线摄像头播放到窗口 0..N-1 */
const playList = (list: any[]) => {
  if (!pm) return
  loading.value = true
  loadingText.value = '正在连接视频流...'
  errorMsg.value = null
  // 抑制内部关闭引发的 closeVideo 事件通知（事件可能延迟，留出时间窗口）
  internalCloseUntil = Date.now() + 800
  try {
    pm.close() // 关闭所有窗口
  } catch (e) {
    /* ignore */
  }
  if (!list.length) {
    loading.value = false
    return
  }
  // 仅播放在线摄像头，最多播放窗口上限路数
  const onlines = list.filter((c) => c.status !== 0)
  onlines.slice(0, 9).forEach((cam, i) => doPlay(cam, i))
}

/** 停止播放（默认停止当前窗口） */
const stopPlay = (windowIndex?: number) => {
  playing = false
  pendingCamera = null
  if (pm) {
    try {
      pm.close(windowIndex ?? currentWindowIndex.value)
    } catch (e) {
      /* ignore */
    }
  }
}

/** 重试 */
const retry = () => {
  if (props.camera) {
    doPlay(props.camera)
  } else {
    errorMsg.value = null
  }
}

watch(
  () => props.camera,
  (cam) => {
    errorMsg.value = null
    if (!cam) {
      stopPlay()
      loading.value = false
      return
    }
    loading.value = true
    loadingText.value = '正在连接视频流...'
    if (initialized && pm) {
      doPlay(cam)
    } else {
      // 播放器尚未初始化完成，挂起等待 initializationCompleted
      pendingCamera = cam
    }
  },
  { immediate: true }
)

watch(
  () => props.cameras,
  (list) => {
    if (!list || !list.length) return
    if (initialized && pm) {
      playList(list)
    } else {
      // 播放器尚未初始化完成，挂起等待 initializationCompleted
      pendingCameraList = list
    }
  },
  { immediate: true }
)

onMounted(() => {
  // 文档要求：在 mounted 生命周期中异步执行 new PlayerManager()
  nextTick(() => {
    try {
      pm = new PlayerManager({
        el: containerId,
        type: 'real',
        maxNum: 9, // 播放器上限路数（支持 1/4/9/16/25）
        num: props.num, // 初始化显示路数（默认 4 画面，与插件底部分屏按钮联动）
        importLoad: false, // WSPlayer.js 已在 index.html 静态引入（文档推荐方式）
        // prefixUrl 注意：WSPlayer 内部 processPrefixUrl 对以 "/" 开头的值会再加 "/"（变成 //static），
        // 需传不带前导斜杠的 'static'（内部处理为绝对路径 /static），或 './static'（相对路径）
        prefixUrl: 'static', // 解码库资源前缀（与 public/static 目录对应）
        // WS 协议按页面协议自适应（WSPlayer 内部自动判断 http->ws:9100 / https->wss:9102）
        showControl: props.showControl, // 是否显示底部工具栏
        showTopOperate: true,
        showIcons: {
          // 顶部窗口自定义按钮（文档基础配置）
          streamChangeSelect: true, // 主辅码流切换
          replayIcon: true, // 刷新重播按钮
          ivsIcon: true, // 智能帧按钮
          talkIcon: true, // 对讲功能按钮
          localRecordIcon: true, // 本地录像按钮
          audioIcon: true, // 声音控制按钮
          snapshotIcon: true, // 抓图按钮
          closeIcon: true // 关闭视频按钮
        },
        openIvs: true, // 是否开启智能帧/规则线/目标框
        ivsTypeArr: [1, 2], // 智能信息类型（1-规则线 2-目标框）
        useH265MSE: true, // 解码方式（true-硬解 false-软解）
        picCapCb: true, // 抓图回调（true-回调触发 false-直接下载）
        localeLang: 'zhCN',
        // WS 连接地址定制：https 页面走同源 wss 代理（nginx /wsplayer-proxy 转发到流媒体 9100），
        // 解决浏览器禁止混合内容（https 页面不能连 ws://）且流媒体 9102(wss) 不可用的问题
        setWSUrl: (wsList) => {
          const addr = (wsList && wsList[0]) || ''
          if (location.protocol === 'https:') {
            return `wss://${location.host}/wsplayer-proxy`
          }
          return `ws://${addr}`
        },
        // 流媒体重定向时插件会回调此方法；固定代理模式下 ws 地址不变（代理已固定转发到流媒体 9100）
        setRedirectWSUrl: (wsUrl) => wsUrl,
        receiveMessageFromWSPlayer: onMessage
      })
    } catch (e: any) {
      loading.value = false
      errorMsg.value = '播放器初始化失败: ' + (e.message || '')
      return
    }
    // 兜底：WSPlayer.js 加载失败或初始化异常时提示
    initTimeout = setTimeout(() => {
      if (!initialized && props.camera) {
        loading.value = false
        errorMsg.value = '播放器资源加载失败，请检查 /static/WSPlayer/WSPlayer.js'
      }
    }, 8000)
  })
})

onUnmounted(() => {
  if (initTimeout) {
    clearTimeout(initTimeout)
    initTimeout = null
  }
  stopPlay()
  try {
    pm && pm.destroy()
  } catch (e) {
    /* ignore */
  }
  pm = null
  initialized = false
})
</script>

<style scoped>
.wsplayer-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 200px;
  background: #000;
  overflow: hidden;
}
.wsplayer-container {
  width: 100%;
  height: 100%;
}
.wsplayer-mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  font-size: 14px;
}
.wsplayer-mask.mask-error {
  color: #f56c6c;
}
.mask-icon {
  font-size: 28px;
}
.mask-text {
  max-width: 90%;
  text-align: center;
  word-break: break-all;
}
</style>
