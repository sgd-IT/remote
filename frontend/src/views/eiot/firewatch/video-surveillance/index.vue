<template>
  <div class="video-surveillance">

    <header class="header">
      <div class="header-left">
        <h1 class="title">视频监控系统</h1>
        <p class="subtitle">VIDEO SURVEILLANCE SYSTEM</p>
      </div>
      <div class="header-center">
        <div class="datetime">
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
          <span class="week">{{ currentWeek }}</span>
        </div>
      </div>
      <div class="header-right">
        <el-tooltip content="摄像头管理" placement="bottom">
          <el-button class="header-icon-btn" @click="goCameraManage"><el-icon><Setting /></el-icon></el-button>
        </el-tooltip>
        <el-tooltip content="返回值守大屏" placement="bottom">
          <el-button class="header-icon-btn" @click="goBack"><el-icon><Back /></el-icon></el-button>
        </el-tooltip>
        <el-tooltip content="全屏" placement="bottom">
          <el-button class="header-icon-btn" @click="toggleFullscreen"><el-icon><FullScreen /></el-icon></el-button>
        </el-tooltip>
      </div>
    </header>

    <main class="main-content">
      <!-- ====== 左侧面板 ====== -->
      <aside class="left-panel">
        <!-- 监控概览 -->
        <div class="panel-card card-auto">
          <div class="card-header">
            <span class="card-title"><el-icon><VideoCameraFilled /></el-icon>监控概览</span>
          </div>
          <div class="card-content">
            <div class="stat-grid">
              <div class="stat-cell total"><div class="sv">{{ cameraStats.total }}</div><div class="sl">总数</div></div>
              <div class="stat-cell online"><div class="sv">{{ cameraStats.online }}</div><div class="sl">在线</div></div>
              <div class="stat-cell offline"><div class="sv">{{ cameraStats.offline }}</div><div class="sl">离线</div></div>
              <div class="stat-cell alarm"><div class="sv">{{ cameraStats.alarm }}</div><div class="sl">告警</div></div>
            </div>
          </div>
        </div>

        <!-- 设备树 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title"><el-icon><OfficeBuilding /></el-icon>设备列表</span>
          </div>
          <div class="card-content">
            <div class="search-bar">
              <el-input v-model="treeSearch" placeholder="搜索单位/设备..." size="small" :prefix-icon="Search" clearable />
            </div>
            <div class="device-tree">
              <el-tree
                ref="treeRef"
                :data="treeData"
                :props="treeProps"
                node-key="nodeKey"
                :expand-on-click-node="false"
                :default-expand-all="true"
                :filter-node-method="filterTreeNode"
                @node-click="handleTreeClick"
                highlight-current
              >
                <template #default="{ data }">
                  <div class="tree-node" :class="['type-' + data.nodeType, { active: selectedNodeKey === data.nodeKey }]">
                    <el-icon :size="14" class="node-icon">
                      <OfficeBuilding v-if="data.nodeType === 'enterprise'" />
                      <VideoCamera v-else />
                    </el-icon>
                    <span class="node-label">{{ data.label }}</span>
                    <span v-if="data.nodeType === 'camera'" class="node-status" :class="data.state === 1 ? 'online' : 'offline'"></span>
                    <span v-if="data.nodeType === 'enterprise'" class="node-count">{{ data.children?.length || 0 }}</span>
                  </div>
                </template>
              </el-tree>
              <div class="empty" v-if="!treeData.length"><el-icon><CircleClose /></el-icon>暂无设备</div>
            </div>
          </div>
        </div>
      </aside>

      <!-- ====== 中间面板：监控墙（WSPlayer 插件自带分屏功能） ====== -->
      <main class="center-panel">
        <div class="panel-card video-wall-card">
          <!-- 工具栏 -->
          <div class="wall-toolbar">
            <div class="wall-toolbar-left">
              <span class="wall-title">实时监控</span>
              <span class="wall-count">{{ wallStatusText }}</span>
            </div>
          </div>

          <!-- 播放器（分屏切换、全屏等由插件底部工具栏提供） -->
          <div class="wall-player">
            <WsPlayerWrapper
              :camera="currentCamera"
              :cameras="cameraPlayList"
              @close="handlePlayerClose"
            />
          </div>
        </div>
      </main>

      <!-- ====== 右侧面板 ====== -->
      <aside class="right-panel">
        <!-- 设备信息 -->
        <div class="panel-card card-auto">
          <div class="card-header">
            <span class="card-title"><el-icon><InfoFilled /></el-icon>设备信息</span>
          </div>
          <div class="card-content">
            <div class="device-info" v-if="currentCamera">
              <div class="info-row"><span class="info-label">名称</span><span class="info-value highlight">{{ currentCamera.channelName || currentCamera.name }}</span></div>
              <div class="info-row"><span class="info-label">状态</span><span class="info-value" :class="currentCamera.status === 1 ? 'val-online' : 'val-offline'">{{ currentCamera.status === 1 ? '在线' : '离线' }}</span></div>
              <div class="info-row"><span class="info-label">地址</span><span class="info-value">{{ currentCamera.address || '-' }}</span></div>
              <div class="info-row"><span class="info-label">通道</span><span class="info-value">{{ currentCamera.channelId || '-' }}</span></div>
            </div>
            <div class="device-info" v-else>
              <div class="info-empty">
                <el-icon :size="32"><VideoCamera /></el-icon>
                <span>请选择摄像头</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 告警事件 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title"><el-icon><Bell /></el-icon>告警事件</span>
            <span class="badge red" v-if="eventList.length">{{ eventList.length }}</span>
          </div>
          <div class="card-content">
            <div class="event-timeline">
              <div class="event-item" v-for="evt in eventList" :key="evt.id" :class="'level-' + evt.level">
                <div class="evt-dot"></div>
                <div class="evt-body">
                  <div class="evt-desc">{{ evt.desc }}</div>
                  <div class="evt-meta">
                    <span class="evt-cam">{{ evt.camera }}</span>
                    <span class="evt-time">{{ evt.time }}</span>
                  </div>
                </div>
                <span class="evt-tag" :class="'tag-' + evt.level">{{ evt.levelText }}</span>
              </div>
              <div class="empty" v-if="!eventList.length">
                <el-icon><CircleCheck /></el-icon>暂无告警
              </div>
            </div>
          </div>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Back, FullScreen, VideoCameraFilled, VideoCamera,
  Search, InfoFilled, Bell, CircleClose, CircleCheck, Setting,
  OfficeBuilding
} from '@element-plus/icons-vue'
import WsPlayerWrapper from '@/views/eiot/video/components/WsPlayerWrapper.vue'
import { getCameraTree, getVideoCamera } from '@/api/eiot/video/videoCamera.api'
import request from '@/config/axios'

defineOptions({ name: 'VideoSurveillance' })

const router = useRouter()
const route = useRoute()

const currentDate = ref(''), currentTime = ref(''), currentWeek = ref('')

/** 当前选中的单个摄像头（点击树摄像头节点后，在播放器当前活动窗口播放） */
const currentCamera = ref<any>(null)
/** 批量播放列表（点击企业节点后，依次播放到窗口 0..N-1） */
const cameraPlayList = ref<any[]>([])

const wallStatusText = computed(() => {
  if (currentCamera.value) return currentCamera.value.channelName || currentCamera.value.name || '单路播放'
  if (cameraPlayList.value.length) return `${cameraPlayList.value.length} 路画面`
  return '点击左侧设备开始播放'
})

/** 点击播放器窗口关闭按钮 */
const handlePlayerClose = () => {
  currentCamera.value = null
  cameraPlayList.value = []
}

let timer: ReturnType<typeof setInterval> | null = null

const silentGet = async (url: string, params?: any) => {
  try { return await request.get({ url, params }) } catch { return null }
}

const cameraStats = ref({ total: 0, online: 0, offline: 0, alarm: 0 })
const cameraList = ref<any[]>([])
const eventList = ref<any[]>([])

const updateDateTime = () => {
  const now = new Date()
  const w = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六']
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentWeek.value = w[now.getDay()]
}

const goBack = () => router.push('/duty-screen')
const goCameraManage = () => router.push('/video/camera-manage')
const toggleFullscreen = () => { if (!document.fullscreenElement) document.documentElement.requestFullscreen(); else document.exitFullscreen() }

// ====== 设备树 ======
const treeData = ref<any[]>([])
const treeSearch = ref('')
const selectedNodeKey = ref('')
const treeProps = { children: 'children', label: 'label' }
const treeRef = ref()

const filterTreeNode = (value: string, data: any) => {
  if (!value) return true
  return data.label.toLowerCase().includes(value.toLowerCase())
}

watch(treeSearch, (val) => { treeRef.value?.filter(val) })

const handleTreeClick = (data: any) => {
  selectedNodeKey.value = data.nodeKey
  if (data.nodeType === 'enterprise') {
    onEnterpriseClick(data)
  } else if (data.nodeType === 'camera') {
    onCameraNodeClick(data)
  }
}

/** 摄像头数据统一映射（WSPlayer 预览/对讲所需字段） */
const mapCam = (c: any) => ({
  id: c.id,
  deviceId: c.deviceId || '',
  channelId: c.channelId || '',
  channelName: c.channelName || '未命名摄像头',
  name: c.channelName || '未命名',
  status: c.status ?? 1,
  address: c.address || '-',
  quality: c.quality || '2',
  pullType: c.pullType ?? 0,
  type: c.type ?? 0,
  supportReplay: c.supportReplay ?? 0
})

/** 树接口旧版本无 deviceId 时，通过详情接口补全（后端升级后自动跳过） */
const ensureDeviceId = async (cam: any): Promise<any> => {
  if (cam.deviceId) return cam
  try {
    const detail = await getVideoCamera(cam.id)
    return { ...cam, deviceId: detail?.deviceId || '', quality: detail?.quality || cam.quality || '2' }
  } catch {
    return cam
  }
}

const onEnterpriseClick = async (node: any) => {
  const cams = node.cameras || []
  cameraList.value = cams.map(mapCam)
  // 批量播放前 4 路到窗口 0..3（在线摄像头，与插件默认 4 画面一致）
  const targets = cams.slice(0, 4)
  const ensured = await Promise.all(targets.map((c: any) => ensureDeviceId(c)))
  cameraPlayList.value = ensured.map(mapCam)
  currentCamera.value = null
  updateCameraStats()
}

const onCameraNodeClick = async (node: any) => {
  const cam = node.cameraData
  cameraList.value = [mapCam(cam)]
  cameraPlayList.value = []
  const ensured = await ensureDeviceId(cam)
  // 使用新对象引用，确保重复点击同一节点也能触发重新播放（当前活动窗口播放）
  currentCamera.value = { ...mapCam(ensured) }
  updateCameraStats()
}

const loadCameraTree = async () => {
  try {
    const res = await getCameraTree()
    treeData.value = (res || []).map((node: any) => ({
      nodeKey: 'ent_' + node.id,
      label: node.name,
      nodeType: 'enterprise',
      cameras: node.cameras || [],
      children: (node.cameras || []).map((c: any) => ({
        nodeKey: 'cam_' + c.id,
        label: c.channelName || '未命名摄像头',
        nodeType: 'camera',
        cameraData: c,
        state: c.status
      }))
    }))
    // 默认选中第一个企业节点并加载视频
    if (treeData.value.length > 0) {
      handleTreeClick(treeData.value[0])
    }
  } catch { treeData.value = [] }
}

const updateCameraStats = () => {
  const online = cameraList.value.filter(c => c.status === 1).length
  const offline = cameraList.value.filter(c => c.status === 0).length
  cameraStats.value = { total: cameraList.value.length, online, offline, alarm: 0 }
}

const loadCameras = async () => {
  const queryCameraId = route.query.cameraId as string | undefined
  if (queryCameraId) {
    // 从报警跳转过来，加载所有摄像头并定位到指定摄像头（当前活动窗口播放）
    try {
      const treeRes = await getCameraTree()
      const allCams: any[] = []
      for (const node of (treeRes || [])) {
        for (const c of (node.cameras || [])) {
          allCams.push(mapCam(c))
        }
      }
      cameraList.value = allCams
      // 定位到目标摄像头
      const target = allCams.find((c) => String(c.id) === String(queryCameraId))
      if (target) {
        cameraPlayList.value = []
        const ensured = await ensureDeviceId(target)
        currentCamera.value = { ...ensured }
      }
    } catch { cameraList.value = [] }
    updateCameraStats()
    return
  }
  // 正常进入：加载摄像头树
  await loadCameraTree()
}

const loadEvents = async () => {
  const res = await silentGet('/eiot/firewatch/dashboard/realtime-alarms', { limit: 10 })
  if (res?.length) {
    eventList.value = res.map((a: any) => ({
      id: a.id,
      desc: a.alarmDescription ? (a.alarmDescription.length > 40 ? a.alarmDescription.substring(0, 40) + '...' : a.alarmDescription) : '设备告警',
      camera: a.deviceName || a.propertyName || '未知设备',
      time: a.alarmTime ? String(a.alarmTime).replace('T', ' ').substring(11, 16) : '--:--',
      level: a.alarmLevel || 'warning',
      levelText: ({ critical: '严重', major: '较大', warning: '一般', info: '提示' } as Record<string, string>)[a.alarmLevel || 'warning'] || '一般'
    }))
  } else {
    eventList.value = [
      { id: 1, desc: '烟感探测器触发火警信号', camera: '1号楼大厅', time: '14:32', level: 'critical', levelText: '严重' },
      { id: 2, desc: '消防通道温度异常告警', camera: '1号楼消防通道', time: '13:18', level: 'major', levelText: '较大' },
      { id: 3, desc: '配电房电流波动监测', camera: '2号楼配电房', time: '11:45', level: 'warning', levelText: '一般' },
    ]
  }
}

onMounted(async () => {
  updateDateTime()
  timer = setInterval(updateDateTime, 1000)
  await loadCameras()
  loadEvents()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
@use 'sass:color';
$bg1: #0a1628; $bg2: #0d1f3c; $bg3: #0f2747; $bd: #1e3a5f; $t1: #fff; $t2: #8b9dc5;
$glass-border: rgba(30, 58, 95, 0.45);
$blue: #1890ff; $red: #ff4d4f; $orange: #fa8c16; $green: #52c41a; $cyan: #4FC3F7;

.video-surveillance {
  width: 100vw; height: 100vh; color: $t1; overflow: hidden;
  display: flex; flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif; position: relative;
  background:
    radial-gradient(ellipse 80% 60% at 15% 85%, rgba(24,144,255,0.06) 0%, transparent 60%),
    radial-gradient(ellipse 60% 50% at 90% 20%, rgba(79,195,247,0.04) 0%, transparent 55%),
    radial-gradient(ellipse 50% 40% at 50% 100%, rgba(24,144,255,0.03) 0%, transparent 50%),
    linear-gradient(165deg, #060d1a 0%, #0a1628 30%, #0d1f3c 60%, #091424 100%);

  &::before {
    content: '';
    position: absolute; inset: 0; z-index: 0; pointer-events: none;
    background-image:
      radial-gradient(circle at 1px 1px, rgba(79,195,247,0.035) 1px, transparent 0);
    background-size: 40px 40px;
  }
}

.header {
  height: 56px; position: relative; z-index: 10;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border-bottom: 1px solid rgba(79,195,247,0.1);
  box-shadow: 0 2px 10px rgba(0,0,0,0.18), 0 0 1px rgba(79,195,247,0.08), inset 0 1px 0 rgba(79,195,247,0.04);
  display: flex; align-items: center; justify-content: space-between; padding: 0 20px; flex-shrink: 0;

  .header-left {
    .title { font-size: 22px; font-weight: bold; background: linear-gradient(90deg, #fff, #7ec8f8, #54a0ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin: 0; letter-spacing: 3px; }
    .subtitle { font-size: 10px; color: $t2; letter-spacing: 1px; margin-top: 1px; opacity: 0.7; }
  }
  .header-center {
    position: absolute; left: 50%; transform: translateX(-50%);
    .datetime {
      display: flex; align-items: baseline; gap: 16px;
      .date { color: $t2; font-size: 14px; font-weight: 500; letter-spacing: 0.5px; }
      .time { font-size: 22px; font-weight: 800; color: $blue; letter-spacing: 1px; font-variant-numeric: tabular-nums; }
      .week { color: $t2; font-size: 14px; font-weight: 500; }
    }
  }
  .header-right {
    display: flex; align-items: center; gap: 8px;
    .header-icon-btn {
      background: transparent; border: none; color: $t2; padding: 4px 6px;
      &:hover { color: $blue; }
      .el-icon { font-size: 16px; }
    }
  }
}

.main-content {
  flex: 1; display: flex; padding: 10px; gap: 10px; min-height: 0;
  position: relative; z-index: 1;
}
.left-panel { width: 320px; flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; height: 100%; scrollbar-width: none; &::-webkit-scrollbar { display: none; } }
.center-panel { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; height: 100%; }
.right-panel { width: 320px; flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; height: 100%; scrollbar-width: none; &::-webkit-scrollbar { display: none; } }

.panel-card {
  flex: 1; min-height: 0;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border: 1px solid rgba(79,195,247,0.1); border-radius: 8px;
  padding: 16px 18px; display: flex; flex-direction: column;
  overflow: visible; position: relative;
  transition: all 0.5s cubic-bezier(0.23, 1, 0.32, 1);
  box-shadow: 0 2px 10px rgba(0,0,0,0.18), 0 0 1px rgba(79,195,247,0.08), inset 0 1px 0 rgba(79,195,247,0.04), inset 0 0 40px rgba(79,195,247,0.012);
  transform-style: preserve-3d;

  &::before {
    content: ''; position: absolute; top: -1px; left: -1px; width: 24px; height: 24px;
    border-top: 2px solid rgba(79,195,247,0.5); border-left: 2px solid rgba(79,195,247,0.5);
    border-radius: 8px 0 0 0; box-shadow: 0 0 8px rgba(79,195,247,0.15); z-index: 2;
  }
  &::after {
    content: ''; position: absolute; bottom: -1px; right: -1px; width: 24px; height: 24px;
    border-bottom: 2px solid rgba(79,195,247,0.5); border-right: 2px solid rgba(79,195,247,0.5);
    border-radius: 0 0 8px 0; box-shadow: 0 0 8px rgba(79,195,247,0.15); z-index: 2;
  }
  .card-content {
    flex: 1; overflow-y: auto; min-height: 0; padding: 10px; border: none;
    scrollbar-width: none; &::-webkit-scrollbar { display: none; }
  }
  .card-header {
    padding: 12px 16px; background: transparent;
    border-bottom: 1px solid rgba(79,195,247,0.12);
    display: flex; align-items: center; justify-content: space-between; position: relative;
    &::before {
      content: ''; position: absolute; left: 0; top: 20%; bottom: 20%; width: 3px;
      background: rgba(79,195,247,0.9); border-radius: 0 2px 2px 0; box-shadow: 0 0 6px rgba(79,195,247,0.5);
    }
    .card-title {
      display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: bold; color: #fff; letter-spacing: 0.5px;
      .el-icon { color: rgba(79,195,247,0.9); font-size: 16px; }
    }
    .badge {
      background: rgba(79,195,247,0.25); color: rgba(79,195,247,1); padding: 2px 10px; border-radius: 10px; font-size: 12px; margin-left: 4px;
      border: 1px solid rgba(79,195,247,0.2);
      &.red { background: rgba($red, 0.2); color: $red; border-color: rgba($red, 0.25); }
    }
  }
}

.panel-card.card-auto { flex: 0 auto; .card-content { flex: 0 auto; overflow: visible; } }

.stat-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  .stat-cell {
    text-align: center; padding: 10px 4px; border-radius: 6px;
    background: rgba($blue, 0.06); border: 1px solid $glass-border; transition: border-color 0.3s;
    &:hover { border-color: rgba($blue, 0.3); }
    .sv { font-size: 22px; font-weight: bold; }
    .sl { font-size: 11px; color: $t2; margin-top: 3px; }
    &.total .sv { color: $blue; }
    &.online .sv { color: $green; }
    &.offline .sv { color: #8c8c8c; }
    &.alarm .sv { color: $red; }
  }
}

.search-bar {
  margin-bottom: 8px;
  :deep(.el-input__wrapper) { background: rgba($bg3, 0.8); border: 1px solid rgba($bd, 0.5); box-shadow: none; border-radius: 6px; }
  :deep(.el-input__inner) { color: $t1; font-size: 12px; }
  :deep(.el-input__wrapper .el-input__prefix .el-icon) { color: $t2; }
}

// ====== 设备树样式 ======
.device-tree {
  :deep(.el-tree) {
    background: transparent; color: $t2;
    .el-tree-node__content {
      height: 36px; border-radius: 4px; padding-left: 8px !important;
      transition: all 0.2s;
      &:hover { background: rgba($blue, 0.08); }
      &.is-current { background: rgba($blue, 0.15); }
    }
    .el-tree-node__expand-icon { color: $t2; }
    .el-tree-node__expand-icon.is-leaf { color: transparent; }
  }
}
.tree-node {
  display: flex; align-items: center; gap: 8px; width: 100%;
  font-size: 13px; color: $t2; padding-right: 8px;
  &.active { color: #fff; }
  .node-icon { color: rgba($cyan, 0.6); flex-shrink: 0; }
  &.type-enterprise .node-icon { color: rgba($blue, 0.8); }
  .node-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .node-status {
    width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0;
    &.online { background: $green; box-shadow: 0 0 6px rgba($green, 0.5); }
    &.offline { background: #8c8c8c; }
  }
  .node-count {
    font-size: 11px; color: $t2; background: rgba($blue, 0.15);
    padding: 1px 6px; border-radius: 8px; flex-shrink: 0;
  }
}

// ====== 监控墙 ======
.video-wall-card {
  flex: 1; min-height: 0; padding: 12px 14px;
  .card-content, .wall-player { flex: 1; min-height: 0; }

  &:fullscreen {
    width: 100%;
    height: 100vh;
    display: flex;
    flex-direction: column;
    padding: 12px 14px;
    background:
      radial-gradient(ellipse 80% 60% at 15% 85%, rgba(24,144,255,0.06) 0%, transparent 60%),
      radial-gradient(ellipse 60% 50% at 90% 20%, rgba(79,195,247,0.04) 0%, transparent 55%),
      linear-gradient(165deg, #060d1a 0%, #0a1628 30%, #0d1f3c 60%, #091424 100%);
    border-radius: 0;

    .wall-player {
      flex: 1;
      min-height: 0;
    }
  }
}

.wall-player {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;

  :deep(.wsplayer-wrapper) {
    flex: 1;
    min-height: 0;
  }
}

.wall-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 2px 10px; flex-shrink: 0;
}
.wall-toolbar-left {
  display: flex; align-items: center; gap: 10px;
  .wall-title { font-size: 15px; font-weight: 700; color: #fff; letter-spacing: 0.5px; }
  .wall-count { font-size: 11px; color: $t2; background: rgba($cyan, 0.1); padding: 2px 8px; border-radius: 8px; border: 1px solid rgba($cyan, 0.15); }
}

.device-info { display: flex; flex-direction: column; }
.info-row {
  display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid rgba($bd, 0.2);
  &:last-child { border-bottom: none; }
  .info-label { width: 52px; flex-shrink: 0; font-size: 12px; color: $t2; }
  .info-value { flex: 1; font-size: 13px; color: #fff; line-height: 1.4;
    &.highlight { font-weight: 600; }
    &.val-online { color: $green; font-weight: 600; }
    &.val-offline { color: #8c8c8c; }
  }
}
.info-empty {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 24px 0; color: $t2;
  .el-icon { opacity: 0.4; }
}

.event-timeline { display: flex; flex-direction: column; gap: 6px; }
.event-item {
  display: flex; align-items: flex-start; padding: 10px 12px; border-radius: 6px;
  border-left: 3px solid transparent; cursor: default; transition: background 0.2s;
  &:hover { background: rgba($blue, 0.08); }
  &.level-critical { border-left-color: $red; background: rgba($red, 0.1); }
  &.level-major { border-left-color: $orange; background: rgba($orange, 0.06); }
  &.level-warning { border-left-color: #fadb14; background: rgba(#fadb14, 0.05); }
  &.level-info { border-left-color: $blue; background: rgba($blue, 0.05); }
}
.evt-dot {
  width: 8px; height: 8px; border-radius: 50%; background: $red;
  margin-top: 5px; margin-right: 10px; flex-shrink: 0;
  animation: blink 2s ease-in-out infinite;
}
.evt-body { flex: 1; min-width: 0;
  .evt-desc { font-size: 13px; color: #fff; line-height: 1.4; }
  .evt-meta { display: flex; gap: 10px; margin-top: 4px; font-size: 11px; color: $t2; }
}
.evt-tag {
  font-size: 10px; padding: 2px 8px; border-radius: 4px; white-space: nowrap; flex-shrink: 0;
  &.tag-critical { background: rgba($red, 0.2); color: $red; }
  &.tag-major { background: rgba($orange, 0.2); color: $orange; }
  &.tag-warning { background: rgba(#fadb14, 0.15); color: #fadb14; }
  &.tag-info { background: rgba($blue, 0.15); color: $blue; }
}

.empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; color: $green; .el-icon { font-size: 32px; margin-bottom: 6px; } &.small { padding: 10px; font-size: 12px; color: $t2; .el-icon { font-size: 20px; } } }

@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
</style>
