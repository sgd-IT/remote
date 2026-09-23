<template>
  <Transition name="alarm-popup-fade">
    <div v-if="showPopup && currentAlarm" class="alarm-popup-overlay" @click.self="onDismiss">
      <div class="alarm-popup-container" :class="severityClass">
        <!-- 顶部警告条 -->
        <div class="ap-header">
          <div class="ap-header-left">
            <span class="ap-siren-icon">🚨</span>
            <span class="ap-title">报警通知</span>
          </div>
          <div class="ap-header-right">
            <span class="ap-countdown">{{ remaining }}s</span>
            <button class="ap-close-btn" @click="onDismiss">✕</button>
          </div>
        </div>

        <!-- 报警信息体 -->
        <div class="ap-body">
          <div class="ap-severity-badge" :class="severityClass">
            {{ severityText }}
          </div>

          <div class="ap-info-grid">
            <div class="ap-info-item">
              <span class="ap-label">设备名称</span>
              <span class="ap-value ap-device-name">{{ alarmDeviceName }}</span>
            </div>
            <div class="ap-info-item">
              <span class="ap-label">报警类型</span>
              <span class="ap-value ap-alarm-type">{{ alarmTypeText }}</span>
            </div>
            <div class="ap-info-item">
              <span class="ap-label">报警位置</span>
              <span class="ap-value">{{ alarmLocation }}</span>
            </div>
            <div class="ap-info-item">
              <span class="ap-label">报警时间</span>
              <span class="ap-value">{{ alarmTime }}</span>
            </div>
            <div class="ap-info-item ap-info-desc">
              <span class="ap-label">报警描述</span>
              <span class="ap-value">{{ alarmDescription }}</span>
            </div>
          </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="ap-footer">
          <span class="ap-footer-text">还有 {{ alarmQueue.length }} 条未读报警</span>
          <button class="ap-dismiss-btn" @click="onDismiss">我知道了</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue'
import type { AlarmItem } from './useAlarmPopup'

const props = defineProps<{
  showPopup: boolean
  currentAlarm: AlarmItem | null
  alarmQueue: AlarmItem[]
}>()

const emit = defineEmits<{
  dismiss: []
}>()

// 倒计时
const remaining = ref(8)
let countdownTimer: ReturnType<typeof setInterval> | null = null

function startCountdown() {
  stopCountdown()
  remaining.value = 8
  countdownTimer = setInterval(() => {
    remaining.value--
    if (remaining.value <= 0) {
      stopCountdown()
      emit('dismiss')
    }
  }, 1000)
}

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// 监听弹窗状态变化
watch(() => props.showPopup, (val) => {
  if (val) {
    startCountdown()
  } else {
    stopCountdown()
  }
})

function onDismiss() {
  stopCountdown()
  emit('dismiss')
}

onUnmounted(() => {
  stopCountdown()
})

// 计算属性
const severityClass = computed(() => {
  const level = props.currentAlarm?.alarmLevel ?? props.currentAlarm?.level ?? 'warning'
  if (level === 'critical') return 'severity-critical'
  if (level === 'major') return 'severity-major'
  if (level === 'warning') return 'severity-warning'
  return 'severity-info'
})

const severityText = computed(() => {
  const level = props.currentAlarm?.alarmLevel ?? props.currentAlarm?.level ?? 'warning'
  if (level === 'critical') return '严重报警'
  if (level === 'major') return '较大报警'
  if (level === 'warning') return '一般报警'
  return '提示信息'
})

const alarmDeviceName = computed(() => {
  return props.currentAlarm?.deviceName ?? '未知设备'
})

const alarmTypeText = computed(() => {
  return props.currentAlarm?.alarmTypeName ?? props.currentAlarm?.typeName ?? props.currentAlarm?.alarmLevel ?? props.currentAlarm?.level ?? '未知类型'
})

const alarmLocation = computed(() => {
  return props.currentAlarm?.alarmLocation ?? props.currentAlarm?.location ?? '未知位置'
})

const alarmTime = computed(() => {
  return props.currentAlarm?.alarmTime ?? props.currentAlarm?.time ?? ''
})

const alarmDescription = computed(() => {
  return props.currentAlarm?.alarmDescription ?? props.currentAlarm?.alarmContent ?? props.currentAlarm?.content ?? ''
})
</script>

<style scoped>
/* ====== 弹窗遮罩 ====== */
.alarm-popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

/* ====== 弹窗容器 ====== */
.alarm-popup-container {
  width: 480px;
  max-width: 90vw;
  border-radius: 16px;
  overflow: hidden;
  box-shadow:
    0 32px 80px rgba(0, 0, 0, 0.55),
    0 0 40px rgba(255, 77, 79, 0.15);
  animation: alarmSlideIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* ====== 不同严重级别的主题色 ====== */
.severity-critical {
  background: linear-gradient(180deg, #1a0a0a 0%, #0d0505 100%);
  border-color: rgba(255, 77, 79, 0.35);
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.55), 0 0 60px rgba(255, 77, 79, 0.2);
}
.severity-critical .ap-header {
  background: linear-gradient(135deg, rgba(255, 77, 79, 0.2), rgba(255, 77, 79, 0.05));
  border-bottom: 1px solid rgba(255, 77, 79, 0.25);
}
.severity-critical .ap-severity-badge {
  background: rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
  border: 1px solid rgba(255, 77, 79, 0.3);
}

.severity-major {
  background: linear-gradient(180deg, #1a120a 0%, #0d0a05 100%);
  border-color: rgba(250, 140, 22, 0.35);
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.55), 0 0 60px rgba(250, 140, 22, 0.15);
}
.severity-major .ap-header {
  background: linear-gradient(135deg, rgba(250, 140, 22, 0.2), rgba(250, 140, 22, 0.05));
  border-bottom: 1px solid rgba(250, 140, 22, 0.25);
}
.severity-major .ap-severity-badge {
  background: rgba(250, 140, 22, 0.15);
  color: #fa8c16;
  border: 1px solid rgba(250, 140, 22, 0.3);
}

.severity-warning {
  background: linear-gradient(180deg, #1a1a0a 0%, #0d0d05 100%);
  border-color: rgba(250, 219, 20, 0.35);
}
.severity-warning .ap-header {
  background: linear-gradient(135deg, rgba(250, 219, 20, 0.15), rgba(250, 219, 20, 0.03));
  border-bottom: 1px solid rgba(250, 219, 20, 0.2);
}
.severity-warning .ap-severity-badge {
  background: rgba(250, 219, 20, 0.12);
  color: #fadb14;
  border: 1px solid rgba(250, 219, 20, 0.25);
}

/* ====== 顶部 ====== */
.ap-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
}

.ap-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ap-siren-icon {
  font-size: 22px;
  animation: sirenPulse 1s ease-in-out infinite;
}

.ap-title {
  font-size: 17px;
  font-weight: 700;
  color: #e8edf5;
  letter-spacing: 1px;
}

.ap-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ap-countdown {
  font-size: 13px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.45);
  font-variant-numeric: tabular-nums;
  min-width: 28px;
  text-align: right;
}

.ap-close-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  transition: all 0.2s;
}
.ap-close-btn:hover {
  background: rgba(255, 77, 79, 0.2);
  border-color: rgba(255, 77, 79, 0.3);
  color: #ff4d4f;
}

/* ====== 主体 ====== */
.ap-body {
  padding: 20px;
}

.ap-severity-badge {
  display: inline-block;
  padding: 5px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin-bottom: 18px;
  animation: badgeGlow 2s ease-in-out infinite;
}

.ap-info-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ap-info-item {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.ap-info-item:last-child {
  border-bottom: none;
}

.ap-label {
  width: 72px;
  flex-shrink: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  font-weight: 500;
}

.ap-value {
  flex: 1;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.88);
  font-weight: 500;
  word-break: break-all;
}

.ap-device-name {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}

.ap-alarm-type {
  color: #ff4d4f;
  font-weight: 600;
}

.severity-major .ap-alarm-type {
  color: #fa8c16;
}

.severity-warning .ap-alarm-type {
  color: #fadb14;
}

.ap-info-desc .ap-value {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.5;
}

/* ====== 底部 ====== */
.ap-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: rgba(0, 0, 0, 0.25);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.ap-footer-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
}

.ap-dismiss-btn {
  padding: 7px 22px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  letter-spacing: 0.5px;
}
.ap-dismiss-btn:hover {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.2);
}

/* ====== 动画 ====== */
@keyframes alarmSlideIn {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes sirenPulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.15);
  }
}

@keyframes badgeGlow {
  0%, 100% {
    box-shadow: 0 0 4px currentColor;
  }
  50% {
    box-shadow: 0 0 12px currentColor, 0 0 24px currentColor;
  }
}

/* ====== 过渡 ====== */
.alarm-popup-fade-enter-active {
  transition: opacity 0.25s ease;
}
.alarm-popup-fade-leave-active {
  transition: opacity 0.2s ease;
}
.alarm-popup-fade-enter-from,
.alarm-popup-fade-leave-to {
  opacity: 0;
}
</style>
