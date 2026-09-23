<template>
  <div class="video-panel">
    <div class="panel-title">视频监控</div>
    <div class="camera-list">
      <div
        v-for="(camera, index) in cameras"
        :key="camera.id"
        class="camera-card"
        :class="{ offline: camera.status !== 1, active: !!playingMap[camera.id] }"
        @click="onCardClick(camera)"
        @dblclick="onCardDblClick(camera)"
      >
        <div class="camera-header">
          <span class="camera-index">{{ index + 1 }}</span>
          <span class="camera-name">{{ camera.channelName || '未命名摄像头' }}</span>
          <span class="camera-status" :class="camera.status === 1 ? 'online' : 'offline'">
            {{ camera.status === 1 ? '正常' : '离线' }}
          </span>
        </div>
        <!-- 一个视频一个播放框：点击卡片后该卡片内渲染 WSPlayer 单窗口播放器 -->
        <div class="camera-body">
          <WsPlayerWrapper
            v-if="playingMap[camera.id]"
            :camera="playingMap[camera.id]"
            :num="1"
            :show-control="false"
            @close="stopCamera(camera.id)"
          />
          <div v-else class="camera-placeholder">
            <div class="camera-icon">📷</div>
            <div class="camera-tip">{{ camera.status === 1 ? '点击加载视频' : '设备离线' }}</div>
          </div>
        </div>
      </div>
      <div v-if="cameras.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <div class="empty-text">暂无关联摄像头</div>
      </div>
    </div>

    <!-- 双击放大观看弹窗（页面中央大弹窗，深色主题） -->
    <el-dialog
      v-model="fullscreenVisible"
      class="video-zoom-dialog"
      width="70%"
      top="5vh"
      :show-close="true"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      append-to-body
      destroy-on-close
    >
      <template #header>
        <div class="fs-dialog-header">
          <el-icon class="fs-cam-icon"><VideoCamera /></el-icon>
          <span class="fs-title">{{ fullscreenCamera?.channelName || '视频观看' }}</span>
          <span
            v-if="fullscreenCamera"
            class="fs-status"
            :class="fullscreenCamera.status === 1 ? 'online' : 'offline'"
          >
            {{ fullscreenCamera.status === 1 ? '正常' : '离线' }}
          </span>
        </div>
      </template>
      <div class="fs-video-body">
        <WsPlayerWrapper
          v-if="fullscreenVisible && fullscreenCamera"
          :key="'fs-' + fullscreenCamera.id"
          :camera="fullscreenCamera"
          :num="1"
          @close="fullscreenVisible = false"
        />
        <div v-else class="fs-placeholder">
          <el-icon :size="48"><VideoCamera /></el-icon>
          <span>暂无视频流</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, watch } from 'vue'
import { VideoCamera } from '@element-plus/icons-vue'
import type { CameraVO } from '@/api/eiot/remote/remoteConsole.api'
import { getVideoCamera } from '@/api/eiot/video/videoCamera.api'
import WsPlayerWrapper from '@/views/eiot/video/components/WsPlayerWrapper.vue'

const props = defineProps<{
  cameras: CameraVO[]
}>()

/** 各摄像头的播放对象（key=cameraId，值存在即渲染对应播放框） */
const playingMap = reactive<Record<string | number, any>>({})

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

/** 单击播放/停止：正在播放则停止，否则在该卡片内播放（再次点击重新播放） */
const selectCamera = async (camera: CameraVO) => {
  if (!camera.id || camera.status !== 1) return
  if (playingMap[camera.id]) {
    // 正在播放 → 停止播放
    delete playingMap[camera.id]
    return
  }
  const cam = await ensureDeviceId(camera)
  // 使用新对象引用，确保重复点击同一摄像头也能触发重新播放
  playingMap[camera.id] = { ...cam }
}

// ==================== 单击/双击区分 ====================
// 双击放大时避免先触发两次单击（播放/停止）
let clickTimer: ReturnType<typeof setTimeout> | null = null

const onCardClick = (camera: CameraVO) => {
  if (clickTimer) {
    clearTimeout(clickTimer)
    clickTimer = null
  }
  clickTimer = setTimeout(() => {
    clickTimer = null
    selectCamera(camera)
  }, 250)
}

const onCardDblClick = (camera: CameraVO) => {
  if (clickTimer) {
    clearTimeout(clickTimer)
    clickTimer = null
  }
  openFullscreen(camera)
}

// ==================== 双击放大观看 ====================

const fullscreenVisible = ref(false)
const fullscreenCamera = ref<any>(null)

const openFullscreen = async (camera: CameraVO) => {
  if (!camera.id) return
  if (camera.status !== 1) return
  const cam = await ensureDeviceId(camera)
  fullscreenCamera.value = { ...cam }
  fullscreenVisible.value = true
}

/** 用户点击播放器窗口关闭按钮：移除该卡片播放框 */
const stopCamera = (cameraId: string | number) => {
  delete playingMap[cameraId]
}

/** 默认自动播放前两个视频（优先在线摄像头，不足两个则只播放可用的） */
const autoPlayFirstTwo = async (list: CameraVO[]) => {
  const onlineCameras = list.filter((c) => c.status === 1)
  const candidates = onlineCameras.length > 0 ? onlineCameras : list
  const targets = candidates.slice(0, 2)
  await Promise.all(
    targets.map(async (camera) => {
      if (!camera.id) return
      const cam = await ensureDeviceId(camera)
      playingMap[camera.id] = { ...cam }
    })
  )
}

/** 设备切换时：清空所有播放框，自动播放前两个在线摄像头（与原有自动播放行为一致） */
watch(
  () => props.cameras,
  (list) => {
    Object.keys(playingMap).forEach((key) => delete playingMap[key])
    if (list.length > 0) {
      autoPlayFirstTwo(list)
    }
  },
  { immediate: true, deep: true }
)
</script>

<style lang="scss" scoped>
@import '../styles/common.scss';

.video-panel {
  width: 100%;
  height: 100%;
  background: $bg-panel;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-title {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 500;
  color: $text-primary;
  border-bottom: 1px solid $border-color;
  flex-shrink: 0;
}

.camera-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.camera-card {
  flex-shrink: 0;
  height: calc((100% - 24px) / 3);
  min-height: 180px;
  background: $bg-primary;
  border: 1px solid $border-color;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: $border-color-strong;
  }

  &.active {
    border-color: $primary;
    box-shadow: 0 0 8px rgba($primary, 0.25);
  }

  &.offline {
    opacity: 0.7;
  }
}

.camera-header {
  height: 36px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid $border-color;
  flex-shrink: 0;
}

.camera-index {
  width: 20px;
  height: 20px;
  @include flex-center;
  background: rgba($primary, 0.15);
  color: $primary;
  border-radius: 50%;
  font-size: 12px;
}

.camera-name {
  flex: 1;
  font-size: 13px;
  color: $text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.camera-status {
  font-size: 12px;

  &.online {
    color: $success;
  }

  &.offline {
    color: $text-muted;
  }
}

.camera-body {
  flex: 1;
  position: relative;
  min-height: 0;
  background: #000;

  :deep(.wsplayer-wrapper) {
    min-height: 100%;
  }
}

.camera-placeholder {
  width: 100%;
  height: 100%;
  @include flex-center;
  flex-direction: column;
  color: $text-secondary;
  gap: 8px;
}

.camera-icon {
  font-size: 36px;
  opacity: 0.6;
}

.camera-tip {
  font-size: 12px;
}

.empty-state {
  flex: 1;
  @include flex-center;
  flex-direction: column;
  color: $text-secondary;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-text {
  font-size: 12px;
}

// ==================== 双击放大弹窗 ====================

.fs-dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fs-cam-icon {
  color: $primary;
  font-size: 18px;
}

.fs-title {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: 1px;
}

.fs-status {
  font-size: 12px;

  &.online {
    color: $success;
  }

  &.offline {
    color: $text-muted;
  }
}

.fs-video-body {
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #000;

  :deep(.wsplayer-wrapper) {
    height: 100%;
  }
}

.fs-placeholder {
  width: 100%;
  height: 100%;
  @include flex-center;
  flex-direction: column;
  gap: 12px;
  color: $text-secondary;
}
</style>

<!-- 放大弹窗 append-to-body，需全局样式，遵循项目深色弹窗规范 -->
<style lang="scss">
@import '../styles/common.scss';

.video-zoom-dialog {
  background: linear-gradient(170deg, #0e2c4e 0%, #0a2442 50%, #081e38 100%);
  border: 1px solid rgba(0, 180, 255, 0.2);
  border-radius: 10px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.6), 0 0 1px rgba(0, 180, 255, 0.15);
  overflow: hidden;

  .el-dialog__header {
    margin: 0;
    padding: 12px 20px;
    background: rgba(0, 180, 255, 0.06);
    border-bottom: 1px solid rgba(30, 58, 95, 0.6);
  }

  .el-dialog__headerbtn {
    top: 14px;
    right: 16px;
    z-index: 10;

    .el-dialog__close {
      color: $text-secondary;
      font-size: 20px;

      &:hover {
        color: $danger;
      }
    }
  }

  .el-dialog__body {
    padding: 12px;
    height: 72vh;
    overflow: hidden;
    background: rgba(0, 0, 0, 0.35);
  }
}
</style>
