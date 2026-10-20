<template>
  <!-- z-index 取高于报警弹窗 overlay（9999），保证视频弹层盖在弹窗之上 -->
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="820px"
    class="dark-dialog alarm-video-dialog"
    :append-to-body="true"
    :z-index="10001"
    destroy-on-close
    @closed="onClosed"
  >
    <template v-if="cameraList.length">
      <div class="video-stage">
        <!-- 摄像头切换标签：多路时竖排在画面左侧黑边上，点击即换流（复用 selectedCameraId 的 watch 链路） -->
        <div v-if="cameraList.length > 1" class="camera-tags">
          <button
            v-for="c in cameraList"
            :key="c.cameraId"
            type="button"
            class="camera-tag"
            :class="{ 'is-active': c.cameraId === selectedCameraId }"
            :title="optionLabel(c)"
            @click.stop="selectedCameraId = c.cameraId"
          >
            {{ optionLabel(c) }}
          </button>
        </div>
        <!-- ws 类型拉流：WSPlayer 经 WebSocket 连流媒体播放，与视频监控/远程控制中心同方案 -->
        <WsPlayerWrapper
          v-if="activeCamera"
          :camera="activeCamera"
          :num="1"
          :show-control="false"
          @close="visible = false"
        />
      </div>
    </template>
    <div v-else class="video-empty-tip">该设备未绑定摄像头</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getVideoCamera } from '@/api/eiot/video/videoCamera.api'
import type { AlarmAssistCamera } from '@/api/eiot/firewatch/alarmAssist.api'
import WsPlayerWrapper from '@/views/eiot/video/components/WsPlayerWrapper.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    /** 报警设备绑定的摄像头（辅助接口 ownCameras） */
    cameras: AlarmAssistCamera[]
    /** 弹窗标题用设备名 */
    title?: string
  }>(),
  { title: '' }
)

const emit = defineEmits<{ 'update:modelValue': [v: boolean] }>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const cameraList = computed(() => props.cameras || [])
const dialogTitle = computed(() => (props.title ? props.title + ' · 实时视频' : '实时视频'))

const selectedCameraId = ref<number | undefined>(undefined)
/** WsPlayerWrapper 播放对象：需带 deviceId/channelId/quality（与远程控制中心同款） */
const activeCamera = ref<any>(null)

const optionLabel = (c: AlarmAssistCamera) => (c.status === 1 ? c.name : `${c.name}（离线）`)

/** 记录里只有 cameraId/name/status，WSPlayer 拉流要设备编码/通道编码/码流；缺就查摄像头详情补全（抄远程控制中心做法） */
const loadCamera = async (cam: AlarmAssistCamera | undefined) => {
  if (!cam) {
    activeCamera.value = null
    return
  }
  let target: any = cam
  if (!target.deviceId || !target.channelId) {
    try {
      const detail = await getVideoCamera(cam.cameraId)
      target = {
        ...cam,
        deviceId: detail?.deviceId,
        channelId: detail?.channelId,
        quality: detail?.quality || '2'
      }
    } catch {
      /* 详情查询失败：原样交给播放器，由播放器遮罩提示错误 */
    }
  }
  // 快速点标签换路时丢弃过期响应，防止旧选路覆盖新选路
  if (selectedCameraId.value !== cam.cameraId) return
  activeCamera.value = { ...target }
}

watch(selectedCameraId, (id) => {
  void loadCamera(cameraList.value.find((c) => c.cameraId === id))
})

// 打开时默认选中第一路；关闭时播放器由 destroy-on-close 销毁（卸载时 WSPlayer 自行释放）
watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      selectedCameraId.value = cameraList.value[0]?.cameraId ?? undefined
    }
  }
)

const onClosed = () => {
  selectedCameraId.value = undefined
  activeCamera.value = null
}
</script>

<style scoped lang="scss">
/* 深色弹窗主题：底色/描边/分隔线对齐报警弹窗卡片（底色 #0f2747 + 主题 accent 描边 rgba(79,195,247,0.15)、8px 圆角） */
:deep(.el-dialog) {
  background: #0f2747;
  border: 1px solid rgba(79, 195, 247, 0.15);
  border-radius: 8px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.5);
}
:deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(30, 58, 95, 0.8);
  padding: 14px 20px;
}
:deep(.el-dialog__title) {
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 0.5px;
}
:deep(.el-dialog__headerbtn) {
  .el-dialog__close {
    color: #8b9dc5;
    transition: color 0.2s;
  }
  &:hover .el-dialog__close {
    color: #fff;
  }
}
:deep(.el-dialog__body) {
  padding: 16px 20px 20px;
}

.video-stage {
  position: relative;
  height: 440px;
  background: #000;
  border-radius: 6px;
  overflow: hidden;
}

/* 摄像头切换标签：竖排悬浮在画面左侧。
   z-index 必须高于 WSPlayer 的画面窗口：它给 .wsplayer-item 内联发的 z-index 从 100 起倒序（单窗口=100），
   之前用 60 被画面盖得肉眼根本看不见——别往 100 以下调 */
.camera-tags {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  max-width: 116px;
}

.camera-tag {
  max-width: 100%;
  padding: 4px 10px;
  font-family: inherit;
  font-size: 12px;
  line-height: 18px;
  color: #c9d7ea;
  background: rgba(13, 31, 60, 0.78);
  border: 1px solid rgba(30, 58, 95, 0.85);
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.2s, border-color 0.2s, background 0.2s;

  &:hover {
    color: #fff;
    border-color: rgba(79, 195, 247, 0.45);
  }

  /* 当前播出的一路：主题蓝高亮，与报警弹窗 accent 一致 */
  &.is-active {
    color: #4fc3f7;
    background: rgba(79, 195, 247, 0.16);
    border-color: rgba(79, 195, 247, 0.6);
    font-weight: 600;
  }
}

.video-empty-tip {
  padding: 48px 0;
  text-align: center;
  color: #8b9dc5;
  font-size: 13px;
}
</style>
