<template>
  <div class="video-player-wrapper">
    <div v-if="loading" class="video-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在获取视频流...</span>
    </div>
    <div v-else-if="displayError" class="video-error">
      <el-icon><Warning /></el-icon>
      <span>{{ displayError }}</span>
      <el-button size="small" type="primary" @click="retry">重试</el-button>
    </div>
    <template v-else-if="activeUrl">
      <video
        ref="videoRef"
        class="video-element"
        autoplay
        muted
        playsinline
        @pause="isPaused = true"
        @play="isPaused = false"
        @playing="onPlaying"
        @waiting="onWaiting"
        @click="togglePlay"
      ></video>
      <div
        v-if="buffering && !isPaused"
        class="video-buffering"
      >
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在连接视频流...</span>
      </div>
      <div
        v-if="isPaused"
        class="play-overlay"
        @click="togglePlay"
      >
        <el-icon class="play-icon"><VideoPlay /></el-icon>
      </div>
    </template>
    <div v-else class="video-empty">
      <el-icon><VideoCamera /></el-icon>
      <span>暂无视频</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted, computed, nextTick } from 'vue'
import { Loading, Warning, VideoCamera, VideoPlay } from '@element-plus/icons-vue'
import { ensurePlayersLoaded } from '@/utils/player-loader'

declare const Hls: any
declare const dhflvjs: any

const props = withDefaults(defineProps<{
  videoUrl?: string | null
  flvUrl?: string | null
  mode?: 'live' | 'playback'
  loading?: boolean
  error?: string | null
}>(), {
  mode: 'live'
})

const emit = defineEmits<{
  retry: []
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const localError = ref<string | null>(null)
const isPaused = ref(false)
const manuallyPaused = ref(false)

// 缓冲/连接中指示：流地址已就绪但画面尚未出帧（拉流、MSE 缓冲、H.265 降级窗口）时显示，
// 出画（playing）即隐藏；超 15s 仍未出画视为连接超时，给错误+重试入口防止"永远黑屏"
const buffering = ref(false)
let bufferingShowTimer: ReturnType<typeof setTimeout> | null = null
let bufferingStallTimer: ReturnType<typeof setTimeout> | null = null

const clearBufferingTimers = () => {
  if (bufferingShowTimer) {
    clearTimeout(bufferingShowTimer)
    bufferingShowTimer = null
  }
  if (bufferingStallTimer) {
    clearTimeout(bufferingStallTimer)
    bufferingStallTimer = null
  }
}

/** 显示缓冲指示：immediate=true（连接新流）立即显示；false 延迟 300ms（避免播放中瞬断闪一下） */
const showBuffering = (immediate = false) => {
  clearBufferingTimers()
  if (immediate) {
    buffering.value = true
  } else {
    bufferingShowTimer = setTimeout(() => {
      buffering.value = true
    }, 300)
  }
  bufferingStallTimer = setTimeout(() => {
    if (!buffering.value) return
    buffering.value = false
    localError.value = '视频流连接超时，请点击重试'
  }, 15000)
}

const hideBuffering = () => {
  clearBufferingTimers()
  buffering.value = false
}

const onPlaying = () => {
  hideBuffering()
}
const onWaiting = () => {
  if (manuallyPaused.value) return
  showBuffering(false)
}

const togglePlay = () => {
  const video = videoRef.value
  if (!video) return
  if (video.paused) {
    // 用户主动恢复：重置手动暂停标记，重新启动 HLS 拉流
    manuallyPaused.value = false
    try {
      if (hlsInstance.value && typeof hlsInstance.value.startLoad === 'function') {
        hlsInstance.value.startLoad()
      }
    } catch (e: any) {
      console.warn('[HLS] startLoad error:', e)
    }
    video.play().catch((e: any) => {
      console.warn('[VideoPlayer] resume play error:', e)
    })
  } else {
    // 用户主动暂停：标记手动暂停并停止 HLS 继续加载，避免缓冲过期/内部状态异常
    manuallyPaused.value = true
    try {
      if (hlsInstance.value && typeof hlsInstance.value.stopLoad === 'function') {
        hlsInstance.value.stopLoad()
      }
    } catch (e: any) {
      console.warn('[HLS] stopLoad error:', e)
    }
    video.pause()
  }
}
const hlsInstance = ref<any>(null)
const flvInstance = ref<any>(null)
let h265FallbackTimer: ReturnType<typeof setTimeout> | null = null
let restoreConsoleLog: (() => void) | null = null

const clearFallbackTimer = () => {
  if (h265FallbackTimer !== null) {
    clearTimeout(h265FallbackTimer)
    h265FallbackTimer = null
  }
}

/** 移除 dhflv.min.js 注入的 DOM 覆盖层（除 video / loading / error / empty 外的子元素） */
const cleanupDhflvOverlays = () => {
  const wrapper = videoRef.value?.parentElement
  if (!wrapper) return
  const allowedSelectors = '.video-element,.video-loading,.video-buffering,.video-error,.video-empty,.play-overlay'
  wrapper.querySelectorAll(':scope > :not(' + allowedSelectors + ')').forEach(el => el.remove())
}

const displayError = computed(() => props.error || localError.value)

/** 当前激活的播放地址：live 模式优先 FLV，playback 模式用 HLS */
const activeUrl = computed(() => {
  if (props.mode === 'live') return props.flvUrl || props.videoUrl || null
  return props.videoUrl || null
})

/** 当前实际使用的协议 */
const activeMode = computed<'flv' | 'hls'>(() => {
  if (props.mode === 'live' && props.flvUrl) return 'flv'
  return 'hls'
})

const getHls = (): any => {
  return (window as any).Hls || (typeof Hls !== 'undefined' ? Hls : null)
}

const getDhflvjs = (): any => {
  return (window as any).dhflvjs || (typeof dhflvjs !== 'undefined' ? dhflvjs : null)
}

// ==================== FLV 播放 ====================

const createFlvPlayer = () => {
  if (!videoRef.value || !props.flvUrl) return
  destroyPlayer()

  const video = videoRef.value
  const flv = getDhflvjs()

  if (!flv) {
    localError.value = 'dhflv.min.js 播放器未加载，请检查是否正确引入'
    return
  }

  if (!flv.isSupported()) {
    localError.value = '当前浏览器不支持 MSE 播放 FLV 流'
    return
  }

  // 连接新流：显示缓冲指示（降级/重连路径由此兜底；出画后由 playing 隐藏）
  showBuffering(true)

  try {
    const player = flv.createPlayer(
      { type: 'flv', url: props.flvUrl, isLive: true, hasAudio: false, hasVideo: true },
      {
        enableWorker: false,                      // dhflv.min.js Worker 实现有 bug（blob 内 Class extends undefined），必须关闭
        enableStashBuffer: true,                  // 开启暂存缓冲，平滑网络抖动避免卡顿
        stashInitialSize: 512,                    // 暂存区 512KB（越大越平滑，延迟略增）
        autoCleanupSourceBuffer: false            // 关闭自动清理，避免 SourceBuffer 清空中断播放
      }
    )
    flvInstance.value = player
    player.attachMediaElement(video)

    // 监听 PARSED_METADATA 事件，检测 H.265 编码（videocodecid=12）
    const parsedMetaEvent = flv.Events.PARSED_METADATA
    if (parsedMetaEvent) {
      player.on(parsedMetaEvent, (metadata: any) => {
        if (metadata?.videocodecid === 12) {
          console.warn('[FLV] 检测到 H.265 编码 (videocodecid=12)，降级到 dhhls')
          destroyPlayer()
          if (props.videoUrl) {
            createDhhlsPlayer()
            // dhflv 可能已注入覆盖层，微任务 + 延迟双保险清理
            Promise.resolve().then(() => cleanupDhflvOverlays())
            setTimeout(() => cleanupDhflvOverlays(), 200)
          } else {
            localError.value = '该摄像头使用 H.265 编码，FLV 不支持，且无 HLS 流可用'
          }
        }
      })
    }

    // 拦截 dhflv.min.js 的所有 console 输出，在 CodecUnsupported 出现时立即降级
    // dhflv 内部日志输出后还会追加覆盖层到 DOM，需在拦截返回后用微任务清理
    const originalLog = console.log
    const originalWarn = console.warn
    const originalError = console.error
    const restoreAll = () => { console.log = originalLog; console.warn = originalWarn; console.error = originalError; restoreConsoleLog = null }
    restoreConsoleLog = restoreAll

    const interceptH265 = (origFn: Function, args: any[]): boolean => {
      const msg = args.join(' ')
      if (msg.includes('CodecUnsupported') || msg.includes('Unsupported codec')) {
        restoreAll()
        clearFallbackTimer()
        console.warn('[FLV] 拦截到 CodecUnsupported 日志，在覆盖层出现前立即降级到 dhhls')
        destroyPlayer()
        if (props.videoUrl) {
          createDhhlsPlayer()
          // dhflv 在 console 调用返回后可能同步注入覆盖层，微任务 + 延迟双保险清理
          Promise.resolve().then(() => cleanupDhflvOverlays())
          setTimeout(() => cleanupDhflvOverlays(), 200)
        }
        return true
      }
      return false
    }

    console.log = (...args: any[]) => { if (!interceptH265(originalLog, args)) originalLog.apply(console, args) }
    console.warn = (...args: any[]) => { if (!interceptH265(originalWarn, args)) originalWarn.apply(console, args) }
    console.error = (...args: any[]) => { if (!interceptH265(originalError, args)) originalError.apply(console, args) }

    player.load()

    // 等 MSE 缓冲就绪后再播放，避免 buffer underrun 导致反复暂停-开始
    video.addEventListener('canplay', () => {
      if (restoreConsoleLog) restoreConsoleLog()
      clearFallbackTimer()
      // 清理 dhflv 注入的覆盖层 DOM
      cleanupDhflvOverlays()
      video.play().catch((e: any) => {
        console.warn('[FLV] play error:', e)
      })
    }, { once: true })

    // H.265 编码降级兜底：日志拦截未触发时用超时保护
    startFallbackTimer()

    // 监听 FLV 错误事件（网络错误 + H.265 编码降级兜底）
    player.on(flv.Events.ERROR, (errorType: string, errorDetail: string, errorInfo: any) => {
      console.error('[FLV] error:', errorType, errorDetail, errorInfo)

      // H.265 编码不支持 MSE 解码，自动降级到 dhhls（兜底检测：部分 dhflv 版本不通过 ERROR 事件上报）
      const errorStr = JSON.stringify(errorInfo || {})
      if (errorStr.includes('CodecUnsupported') || errorStr.includes('Unsupported codec')) {
        console.warn('[FLV] H.265 编码不支持 MSE 播放，降级到 dhhls')
        destroyPlayer()
        if (props.videoUrl) {
          createDhhlsPlayer()
          // dhflv 可能已注入覆盖层，微任务 + 延迟双保险清理
          Promise.resolve().then(() => cleanupDhflvOverlays())
          setTimeout(() => cleanupDhflvOverlays(), 200)
        } else {
          localError.value = '该摄像头使用 H.265 编码，FLV 不支持，且无 HLS 流可用'
        }
        return
      }

      if (errorType === 'NetworkError') {
        localError.value = '视频流连接中断，正在重试...'
        setTimeout(() => {
          localError.value = null
          destroyPlayer()
          createFlvPlayer()
        }, 2000)
      } else if (errorType === 'MediaError') {
        localError.value = '视频流加载失败: ' + (errorDetail || errorType)
      }
    })
  } catch (e: any) {
    console.error('[FLV] init error:', e)
    localError.value = e.message || 'FLV 播放器初始化失败'
  }
}

// ==================== HLS 播放 ====================

const createHlsPlayer = () => {
  if (!videoRef.value || !props.videoUrl) return
  destroyPlayer()

  const video = videoRef.value
  const HlsLib = getHls()

  // 检查 Mixed Content
  if (window.location.protocol === 'https:' && props.videoUrl.startsWith('http:')) {
    localError.value = 'HTTPS 页面无法加载 HTTP 视频流，请联系管理员配置 HTTPS-HLS（端口 7096）'
    return
  }

  // 连接新流：显示缓冲指示（出画后由 playing 隐藏）
  showBuffering(true)

  // 优先使用 dhhls 播放器，保证所有浏览器行为一致、低延迟参数统一管控
  if (HlsLib && HlsLib.isSupported()) {
    try {
      const hls = new HlsLib({
        autoStartLoad: true,                  // 自动开始加载分片，无需手动触发
        startPosition: -1,                    // -1 表示从直播边缘开始播放（非点播）
        maxBufferLength: 6,                   // 前向缓冲上限 6s（平衡延迟与稳定性，避免反复暂停）
        maxBufferSize: 30 * 1000 * 1000,      // 前向缓冲字节上限 30MB
        maxBufferHole: 0.5,                   // 允许的最大缓冲空洞 0.5s（跳过多段间的间隙）
        liveSyncDurationCount: 3,             // 追直播边缘保持 3 个 segment 距离
        liveMaxLatencyDurationCount: 5,       // 延迟超过 5 个 segment 时加速追帧
        initialLiveManifestSize: 1,           // 首次加载只需 1 个 manifest 即可开始播放
        fragLoadingTimeOut: 10000,            // 分片加载超时 10s
        manifestLoadingTimeOut: 5000,         // m3u8 清单加载超时 5s
        debug: import.meta.env.DEV            // 仅开发环境输出调试日志
      })
      hlsInstance.value = hls

      hls.attachMedia(video)
      hls.on(HlsLib.Events.MEDIA_ATTACHED, () => {
        console.log('[HLS] video and hls.js 已经绑定完成')
        hls.loadSource(props.videoUrl!)
        hls.on(HlsLib.Events.MANIFEST_PARSED, () => {
          console.log('[HLS] m3u8文件解析完成')
        })
      })

      // 等 MSE 缓冲就绪后再播放，避免 buffer underrun 导致反复暂停-开始
      video.addEventListener('canplay', () => {
        video.play().catch((e: any) => {
          console.warn('[HLS] play error:', e)
        })
      }, { once: true })

      // 监听视频暂停，自动恢复播放（解决 HLS 直播网络波动导致的反复暂停；用户手动暂停时不自动恢复）
      video.addEventListener('waiting', () => {
        if (manuallyPaused.value) return
        const resumeOnCanPlay = () => {
          video.play().catch(() => {})
          video.removeEventListener('canplay', resumeOnCanPlay)
        }
        video.addEventListener('canplay', resumeOnCanPlay)
      })

      hls.on(HlsLib.Events.ERROR, (_event: any, data: any) => {
        if (data.fatal) {
          switch (data.type) {
            case HlsLib.ErrorTypes.NETWORK_ERROR:
              console.error('[HLS] 网络错误:', data.details)
              localError.value = '网络连接失败，可能是 CORS 跨域或端口不可访问'
              break
            case HlsLib.ErrorTypes.MEDIA_ERROR:
              console.error('[HLS] 媒体解码错误:', data.details)
              localError.value = '视频解码失败: ' + (data.details || '')
              hlsInstance.value?.recoverMediaError()
              break
            default:
              console.error('[HLS] 不可恢复错误:', data.details)
              localError.value = '视频播放失败: ' + (data.details || '')
              destroyPlayer()
              break
          }
        }
      })
      return
    } catch (e: any) {
      console.warn('[HLS] dhhls 初始化失败，降级为原生播放:', e)
    }
  }

  // dhhls 不可用时，降级为 Safari 原生 HLS
  if (video.canPlayType('application/vnd.apple.mpegurl')) {
    video.src = props.videoUrl
    video.addEventListener('canplay', () => {
      video.play().catch((e: any) => {
        console.warn('[HLS] native play error:', e)
      })
    })
    return
  }

  if (!HlsLib) {
    localError.value = 'dhhls.min.js 播放器未加载，请检查是否正确引入'
    return
  }

  localError.value = '当前浏览器不支持 MSE 播放 HLS 流'
}

// ==================== dhhls 降级播放（H.265 编码摄像头专用）====================

const createDhhlsPlayer = () => {
  if (!videoRef.value || !props.videoUrl) return
  destroyPlayer()

  const video = videoRef.value
  const HlsLib = getHls()

  // 检查 Mixed Content
  if (window.location.protocol === 'https:' && props.videoUrl.startsWith('http:')) {
    localError.value = 'HTTPS 页面无法加载 HTTP 视频流，请联系管理员配置 HTTPS-HLS（端口 7096）'
    return
  }

  if (!HlsLib) {
    localError.value = 'dhhls.min.js 播放器未加载，无法播放 H.265 编码摄像头'
    return
  }

  if (!HlsLib.isSupported()) {
    localError.value = '当前浏览器不支持 MSE 播放 HLS 流'
    return
  }

  // 连接新流（FLV 降级/重连路径）：重新显示缓冲指示（出画后由 playing 隐藏）
  showBuffering(true)

  try {
    const hls = new HlsLib({
      autoStartLoad: true,
      startPosition: -1,
      maxBufferLength: 6,
      maxBufferSize: 30 * 1000 * 1000,
      maxBufferHole: 0.5,
      liveSyncDurationCount: 3,
      liveMaxLatencyDurationCount: 5,
      initialLiveManifestSize: 1,
      fragLoadingTimeOut: 10000,
      manifestLoadingTimeOut: 5000,
      debug: import.meta.env.DEV
    })
    hlsInstance.value = hls

    hls.attachMedia(video)
    hls.on(HlsLib.Events.MEDIA_ATTACHED, () => {
      console.log('[dhhls] video and dhhls 已经绑定完成')
      hls.loadSource(props.videoUrl!)
      hls.on(HlsLib.Events.MANIFEST_PARSED, () => {
        console.log('[dhhls] m3u8文件解析完成')
      })
    })

    // 等 MSE 缓冲就绪后再播放，避免 buffer underrun 导致反复暂停-开始
    video.addEventListener('canplay', () => {
      video.play().catch((e: any) => {
        console.warn('[dhhls] play error:', e)
      })
    }, { once: true })

    // 监听视频暂停，自动恢复播放（解决 HLS 直播网络波动导致的反复暂停；用户手动暂停时不自动恢复）
    video.addEventListener('waiting', () => {
      if (manuallyPaused.value) return
      const resumeOnCanPlay = () => {
        video.play().catch(() => {})
        video.removeEventListener('canplay', resumeOnCanPlay)
      }
      video.addEventListener('canplay', resumeOnCanPlay)
    })

    hls.on(HlsLib.Events.ERROR, (_event: any, data: any) => {
      if (data.fatal) {
        switch (data.type) {
          case HlsLib.ErrorTypes.NETWORK_ERROR:
            console.error('[dhhls] 网络错误:', data.details)
            localError.value = '网络连接失败，可能是 CORS 跨域或端口不可访问'
            break
          case HlsLib.ErrorTypes.MEDIA_ERROR:
            console.error('[dhhls] 媒体解码错误:', data.details)
            localError.value = '视频解码失败: ' + (data.details || '')
            hlsInstance.value?.recoverMediaError()
            break
          default:
            console.error('[dhhls] 不可恢复错误:', data.details)
            localError.value = '视频播放失败: ' + (data.details || '')
            destroyPlayer()
            break
        }
      }
    })
  } catch (e: any) {
    console.error('[dhhls] init error:', e)
    localError.value = 'dhhls 播放器初始化失败: ' + (e.message || '')
  }
}

// ==================== H.265 降级定时器 ====================

const startFallbackTimer = () => {
  clearFallbackTimer()
  h265FallbackTimer = setTimeout(() => {
    if (!flvInstance.value) return
    // dhflv.min.js 遇到 H.265 不会触发任何外部事件，只会显示内部错误覆盖层
    // 4s 内 video 未进入可播放状态，说明 MSE 无法解码该流（大概率 H.265）
    if (videoRef.value && videoRef.value.readyState > 0) return
    console.warn('[FLV] 播放 4s 无视频帧到达，推断为 H.265 编码，降级到 dhhls')
    destroyPlayer()
    if (props.videoUrl) {
      createDhhlsPlayer()
      // dhflv 可能已注入覆盖层，微任务 + 延迟双保险清理
      Promise.resolve().then(() => cleanupDhflvOverlays())
      setTimeout(() => cleanupDhflvOverlays(), 200)
    } else {
      localError.value = '该摄像头视频编码不兼容，且无 HLS 流可用'
    }
  }, 4000)
}

// ==================== 销毁 ====================

const destroyPlayer = () => {
  if (restoreConsoleLog) {
    restoreConsoleLog()
  }
  clearFallbackTimer()
  hideBuffering()
  if (flvInstance.value) {
    try {
      flvInstance.value.pause()
      flvInstance.value.unload()
      flvInstance.value.detachMediaElement()
      flvInstance.value.destroy()
    } catch (e) { /* ignore */ }
    flvInstance.value = null
  }
  if (hlsInstance.value) {
    try {
      hlsInstance.value.destroy()
    } catch (e) { /* ignore */ }
    hlsInstance.value = null
  }
  // 清理 dhflv 注入的覆盖层 DOM
  cleanupDhflvOverlays()
  // 显式清理 video 元素
  const video = videoRef.value
  if (video) {
    video.pause()
    video.removeAttribute('src')
    video.load()
  }
}

// ==================== 监听 URL 变化 ====================

watch(activeUrl, async (newUrl) => {
  localError.value = null
  if (newUrl) {
    // 新流就绪但尚未出画：立即显示"正在连接视频流..."，出画后由 playing 事件隐藏
    showBuffering(true)
    await nextTick()
    // 确保 dhflv / dhhls 脚本已通过 Vite asset 管道加载完成
    try {
      await ensurePlayersLoaded()
    } catch (e: any) {
      console.error('[VideoPlayer] 播放器脚本加载失败:', e)
      localError.value = '播放器脚本加载失败: ' + (e.message || '')
      return
    }
    if (activeMode.value === 'flv') {
      createFlvPlayer()
    } else {
      createHlsPlayer()
    }
  } else {
    hideBuffering()
    destroyPlayer()
  }
}, { immediate: true })

const retry = () => {
  localError.value = null
  emit('retry')
}

onUnmounted(() => {
  destroyPlayer()
})
</script>

<style scoped>
.video-player-wrapper {
  width: 100%;
  height: 100%;
  background: #000;
  min-height: 200px;
  overflow: hidden;
  position: relative;
}
.video-player-wrapper :deep(#dhHlsVideoWrapper),
.video-player-wrapper :deep([id^="dhHlsVideoWrapper"]) {
  position: absolute !important;
  inset: 0 !important;
  width: 100% !important;
  height: 100% !important;
}
.video-element {
  position: absolute !important;
  inset: 0 !important;
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  margin: 0 !important;
  padding: 0 !important;
  cursor: pointer;
}
.video-loading,
.video-buffering,
.video-error,
.video-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #8b9dc5;
  font-size: 14px;
}
.video-error {
  color: #ff4d4f;
}
/* 重试按钮跟随主题 accent（Element 默认蓝 #409eff 与深色 HUD 主题冲突） */
.video-error :deep(.el-button--primary) {
  --el-button-bg-color: rgba(79, 195, 247, 0.12);
  --el-button-border-color: rgba(79, 195, 247, 0.5);
  --el-button-text-color: #4fc3f7;
  --el-button-hover-bg-color: rgba(79, 195, 247, 0.22);
  --el-button-hover-border-color: rgba(79, 195, 247, 0.8);
  --el-button-hover-text-color: #fff;
  --el-button-active-bg-color: rgba(79, 195, 247, 0.3);
  --el-button-active-border-color: rgba(79, 195, 247, 0.9);
  --el-button-active-text-color: #fff;
}
.play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  cursor: pointer;
  z-index: 2;
}
.play-icon {
  font-size: 48px;
  color: rgba(255, 255, 255, 0.9);
}
</style>
