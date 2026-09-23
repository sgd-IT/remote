<template>
  <div class="alarm-popup-overlay" v-if="visible" @click.self="onClose">
    <div class="alarm-popup-card">
      <!-- ====== 顶栏 ====== -->
      <div class="ap-header">
        <div class="ap-header-left">
          <span class="ap-alarm-icon">⚠</span>
          <span class="ap-device-type">{{ alarmTypeName }}</span>
          <span class="ap-separator">·</span>
          <span class="ap-alarm-text">报警</span>
        </div>
        <div class="ap-header-right">
          <span class="ap-severity" :class="'severity-' + severityLevel">{{ severityText }}</span>
          <span class="ap-timer">{{ elapsedTime }}</span>
          <span class="ap-queue-index" v-if="queueTotal > 1"
            >第 {{ queueIndex }}/{{ queueTotal }} 条</span
          >
        </div>
      </div>

      <!-- ====== 主体：视频 + 信息 ====== -->
      <div class="ap-body">
        <!-- 左侧视频区 -->
        <div class="ap-video-section">
          <VideoPlayer v-if="videoUrl" :video-url="videoUrl" mode="live" class="ap-video-player" />
          <div v-else class="ap-video-placeholder">
            <span class="placeholder-icon">📹</span>
            <span class="placeholder-text">暂无视频信号</span>
          </div>
        </div>

        <!-- 右侧信息区 -->
        <div class="ap-info-section">
          <div class="ap-info-row">
            <span class="ap-info-icon">📍</span>
            <span class="ap-info-label">位置</span>
            <span class="ap-info-value">{{ location }}</span>
          </div>
          <div class="ap-info-row">
            <span class="ap-info-icon">📟</span>
            <span class="ap-info-label">设备</span>
            <span class="ap-info-value">{{ deviceName }}</span>
          </div>
          <div class="ap-info-row">
            <span class="ap-info-icon">🕐</span>
            <span class="ap-info-label">时间</span>
            <span class="ap-info-value">{{ alarmTime }}</span>
          </div>
          <div class="ap-info-row">
            <span class="ap-info-icon ap-severity-dot" :class="'severity-' + severityLevel"></span>
            <span class="ap-info-label">级别</span>
            <span class="ap-info-value ap-severity-text" :class="'severity-' + severityLevel">{{
              severityText
            }}</span>
          </div>
          <div class="ap-desc-section">
            <div class="ap-desc-header">
              <span class="ap-info-icon">📝</span>
              <span class="ap-info-label">描述</span>
            </div>
            <div class="ap-desc-content">{{ description }}</div>
          </div>

          <!-- ====== 备注栏 ====== -->
          <div class="ap-remark-section">
            <div class="ap-remark-row">
              <span class="ap-remark-label">备注</span>
              <div class="ap-remark-tags">
                <span
                  v-for="tag in remarkTags"
                  :key="tag"
                  class="ap-remark-tag"
                  :class="{ active: selectedTags.includes(tag) }"
                  @click="toggleTag(tag)"
                  >{{ tag }}</span
                >
              </div>
            </div>
            <input
              v-model="remarkInput"
              class="ap-remark-input"
              placeholder="输入框（选填）"
              maxlength="300"
            />
          </div>
        </div>
      </div>

      <!-- ====== 底栏 ====== -->
      <div class="ap-footer">
        <div class="ap-footer-right">
          <button class="ap-btn ap-btn-later" @click="onHandleLater" :disabled="disposalLoading"
            >稍后处置</button
          >
          <button class="ap-btn ap-btn-now" @click="onHandleNow" :disabled="disposalLoading"
            >立即处置</button
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import VideoPlayer from '@/views/eiot/video/components/VideoPlayer.vue'

defineOptions({ name: 'FireWatchAlarmPopup' })

const props = defineProps<{
  visible: boolean
  alarm: Record<string, any> | null
  queueIndex?: number
  queueTotal?: number
}>()

const emit = defineEmits<{
  close: []
  handleLater: []
  handleNow: []
}>()

// ====== 告警字段映射 ======
const deviceName = computed(() => props.alarm?.deviceName ?? '未知设备')
const alarmTypeName = computed(
  () => props.alarm?.alarmTypeName ?? props.alarm?.alarmType ?? '未知类型'
)
const location = computed(() => props.alarm?.alarmLocation ?? props.alarm?.location ?? '未知位置')
const alarmTime = computed(() => {
  const t = props.alarm?.alarmTime ?? props.alarm?.time ?? ''
  if (typeof t === 'number')
    return new Date(t).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  return t ? String(t).replace('T', ' ').substring(0, 19) : ''
})
const description = computed(
  () =>
    props.alarm?.alarmDescription ??
    props.alarm?.alarmContent ??
    props.alarm?.content ??
    props.alarm?.eventDesc ??
    ''
)
const videoUrl = computed(() => props.alarm?.videoUrl ?? null)
const severityLevel = computed(() => {
  const level = props.alarm?.alarmLevel ?? props.alarm?.level ?? 'warning'
  if (level === 'critical' || level === '1' || level === '紧急') return 'critical'
  if (level === 'major' || level === '2' || level === '较重') return 'major'
  if (level === 'warning' || level === '3' || level === '一般') return 'warning'
  return 'info'
})
const severityText = computed(() => {
  const map: Record<string, string> = {
    critical: '紧急',
    major: '较重',
    warning: '一般',
    info: '提示'
  }
  return map[severityLevel.value] || '未知'
})
const queueIndex = computed(() => props.queueIndex ?? 1)
const queueTotal = computed(() => props.queueTotal ?? 1)

// ====== 未处置计时（自报警触发以来） ======
const elapsedTime = ref('00:00:00')
let elapsedTimer: ReturnType<typeof setInterval> | null = null

function startElapsedTimer() {
  stopElapsedTimer()
  updateElapsed()
  elapsedTimer = setInterval(updateElapsed, 1000)
}

function stopElapsedTimer() {
  if (elapsedTimer) {
    clearInterval(elapsedTimer)
    elapsedTimer = null
  }
}

function updateElapsed() {
  const t = props.alarm?.alarmTime ?? props.alarm?.time
  if (!t) {
    elapsedTime.value = '--:--'
    return
  }
  let ts: number
  if (typeof t === 'number') {
    ts = t
  } else {
    const s = String(t).replace('T', ' ')
    ts = new Date(s).getTime()
  }
  if (isNaN(ts)) {
    elapsedTime.value = '--:--'
    return
  }
  const diff = Math.max(0, Math.floor((Date.now() - ts) / 1000))
  const h = Math.floor(diff / 3600)
  const m = String(Math.floor((diff % 3600) / 60)).padStart(2, '0')
  const s = String(diff % 60).padStart(2, '0')
  elapsedTime.value = h > 0
      ? `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      : `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// ====== 备注快捷标签 ======
const remarkTags = ['误报', '已派保安', '现场核实']
const selectedTags = ref<string[]>([])
const remarkInput = ref('')

function toggleTag(tag: string) {
  const idx = selectedTags.value.indexOf(tag)
  if (idx >= 0) {
    selectedTags.value.splice(idx, 1)
  } else {
    selectedTags.value.push(tag)
  }
  // 同步到输入框：用空格拼接
  remarkInput.value = selectedTags.value.join(' ')
}

// ====== 处置按钮 ======
const disposalLoading = ref(false)

function onClose() {
  stopElapsedTimer()
  emit('close')
}

function onHandleLater() {
  stopElapsedTimer()
  emit('handleLater')
  emit('close')
}

function onHandleNow() {
  stopElapsedTimer()
  disposalLoading.value = true
  emit('handleNow')
  // 让父组件处理 loading / 关闭逻辑
  emit('close')
  setTimeout(() => {
    disposalLoading.value = false
  }, 500)
}

// ====== 监听弹窗显隐 ======
watch(
  () => props.visible,
  (val) => {
    if (val) {
      startElapsedTimer()
      selectedTags.value = []
      remarkInput.value = ''
    } else {
      stopElapsedTimer()
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  stopElapsedTimer()
})
</script>

<style scoped>
/* ========== 遮罩 ========== */
.alarm-popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  font-family: 'Microsoft YaHei', sans-serif;
}

/* ========== 卡片容器 ========== */
.alarm-popup-card {
  width: 1050px;
  max-width: 92vw;
  background: #fff;
  border-radius: 12px;
  box-shadow:
    0 16px 48px rgba(0, 0, 0, 0.18),
    0 0 1px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  animation: cardSlideIn 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes cardSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* ========== 顶栏 ========== */
.ap-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.ap-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ap-alarm-icon {
  font-size: 18px;
  color: #ff4d4f;
  animation: iconPulse 1s ease-in-out infinite;
}

@keyframes iconPulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}

.ap-device-type {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.ap-separator {
  color: #c0c4cc;
  margin: 0 2px;
}

.ap-alarm-text {
  font-size: 14px;
  color: #606266;
}

.ap-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ap-severity {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.severity-critical {
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.severity-major {
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.severity-warning {
  background: #fffbe6;
  color: #fadb14;
  border: 1px solid #fff1b8;
}

.severity-info {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.ap-timer {
  font-size: 13px;
  font-weight: 700;
  color: #ff4d4f;
  font-variant-numeric: tabular-nums;
  min-width: 48px;
  text-align: center;
}

.ap-queue-index {
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 8px;
}

/* ========== 主体 ========== */
.ap-body {
  display: flex;
  min-height: 510px;
}

/* 左侧视频 */
.ap-video-section {
  width: 600px;
  flex-shrink: 0;
  min-height: 510px;
  background: #000;
  position: relative;
  overflow: hidden;
}

.ap-video-player {
  width: 100%;
  height: 100%;
  min-height: 510px;
}

.ap-video-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 510px;
  gap: 8px;
  color: #666;
}

.placeholder-icon {
  font-size: 36px;
  opacity: 0.5;
}

.placeholder-text {
  font-size: 13px;
  color: #999;
}

/* 右侧信息 */
.ap-info-section {
  flex: 1;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.ap-info-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ap-info-icon {
  font-size: 14px;
  width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.ap-severity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  margin: 0 6px;
}

.ap-severity-dot.severity-critical {
  background: #ff4d4f;
}
.ap-severity-dot.severity-major {
  background: #fa8c16;
}
.ap-severity-dot.severity-warning {
  background: #fadb14;
}
.ap-severity-dot.severity-info {
  background: #1890ff;
}

.ap-info-label {
  font-size: 13px;
  color: #909399;
  width: 32px;
  flex-shrink: 0;
}

.ap-info-value {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ap-severity-text.severity-critical {
  color: #ff4d4f;
}
.ap-severity-text.severity-major {
  color: #fa8c16;
}
.ap-severity-text.severity-warning {
  color: #d4b106;
}
.ap-severity-text.severity-info {
  color: #1890ff;
}

/* 描述区 */
.ap-desc-section {
  flex: 1;
  margin-top: 4px;
}

.ap-desc-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.ap-desc-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  padding: 8px 12px;
  background: #f8f9fb;
  border-radius: 6px;
  border-left: 3px solid #ff4d4f;
  word-break: break-all;
}

/* ========== 备注栏 ========== */
.ap-remark-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
  padding-top: 12px;
  border-top: 1px dashed #e4e7ed;
}

.ap-remark-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ap-remark-label {
  font-size: 13px;
  color: #606266;
  font-weight: 600;
  flex-shrink: 0;
}

.ap-remark-tags {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.ap-remark-tag {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: #909399;
  background: #fff;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.ap-remark-tag:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.ap-remark-tag.active {
  background: #e6f7ff;
  border-color: #1890ff;
  color: #1890ff;
  font-weight: 600;
}

.ap-remark-input {
  width: 100%;
  padding: 6px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  color: #303133;
  font-family: inherit;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.ap-remark-input::placeholder {
  color: #c0c4cc;
}

.ap-remark-input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

/* ========== 底栏 ========== */
.ap-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.ap-footer-right {
  display: flex;
  gap: 10px;
}

.ap-btn {
  padding: 8px 22px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
  letter-spacing: 0.5px;
}

.ap-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ap-btn-later {
  background: #fff;
  border-color: #dcdfe6;
  color: #606266;
}

.ap-btn-later:hover:not(:disabled) {
  background: #f5f7fa;
  border-color: #c0c4cc;
}

.ap-btn-now {
  background: #ff4d4f;
  border-color: #ff4d4f;
  color: #fff;
}

.ap-btn-now:hover:not(:disabled) {
  background: #ff7875;
  border-color: #ff7875;
}
</style>
