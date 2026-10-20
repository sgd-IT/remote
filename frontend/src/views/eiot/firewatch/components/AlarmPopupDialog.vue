<template>
  <!-- 一级报警弹窗（两屏共用一份实现）：本组件只管弹窗展示与队列/倒计时，业务动作经 later/primary/overlay 事件交给使用方 -->
  <Teleport to="body">
    <div
      class="alarm-popup-overlay"
      :class="['flash-' + getPopupSeverity(), { 'no-dim': !dimOverlay }]"
      v-if="popupVisible"
      @click.self="onOverlayClick"
    >
      <div class="alarm-popup-card">
        <!-- 倒计时进度条 -->
        <div class="alarm-countdown-bar">
          <div class="alarm-countdown-fill" :style="{ width: popupCountdownPercent + '%' }"></div>
        </div>

        <!-- 顶栏 -->
        <div class="alarm-popup-header">
          <div class="alarm-popup-header-left">
            <svg class="alarm-popup-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z M12 9v4 M12 17h.01"
              />
            </svg>
            <span class="alarm-popup-type">{{ popupAlarm?.name || '设备报警' }}</span>
          </div>
          <div class="alarm-popup-header-right">
            <span class="alarm-popup-severity" :class="'severity-' + getPopupSeverity()">{{
              getPopupSeverityText()
            }}</span>
            <span
              class="alarm-sound-toggle"
              :class="{ muted: !soundEnabled }"
              @click.stop="onToggleSound"
              :title="soundEnabled ? '点击静音' : '点击开启声音'"
            >
              <svg v-if="soundEnabled" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M11 5 6 9H2v6h4l5 4z M15.54 8.46a5 5 0 0 1 0 7.07 M19.07 4.93a10 10 0 0 1 0 14.14"
                />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M11 5 6 9H2v6h4l5 4z M23 9l-6 6 M17 9l6 6" />
              </svg>
            </span>
            <span
              class="alarm-countdown-toggle"
              :class="{ paused: manualPaused }"
              @click.stop="onToggleCountdownPause"
              :title="manualPaused ? '点击继续倒计时' : '点击暂停倒计时'"
            >
              <svg v-if="manualPaused" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 3l14 9-14 9z" />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 4h4v16H6z M14 4h4v16h-4z" />
              </svg>
            </span>
            <span class="alarm-popup-timer">{{ popupCountdown }}s</span>
            <span class="alarm-popup-queue" v-if="queueTotal > 1"
              >第 {{ queueIndex }}/{{ queueTotal }} 条</span
            >
          </div>
        </div>

        <!-- 主体 -->
        <div class="alarm-popup-body">
          <!-- 左侧图片 -->
          <div class="alarm-popup-image">
            <img v-if="popupAlarm?.alertImage" :src="popupAlarm.alertImage" alt="报警抓拍" />
            <div v-else class="alarm-popup-no-image">
              <svg class="no-image-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M23 7l-7 5 7 5V7z M1 5h15v14H1z" />
              </svg>
              <span class="no-image-text">暂无抓拍图片</span>
            </div>
          </div>

          <!-- 右侧信息 -->
          <div class="alarm-popup-info">
            <div class="alarm-info-row">
              <span class="alarm-info-label">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M6 21V5a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v16 M3 21h18 M10 8h1 M13 8h1 M10 12h1 M13 12h1" />
                </svg>
                单位
              </span>
              <span class="alarm-info-value">{{ popupAlarm?.tenantName || '未知单位' }}</span>
            </div>
            <div class="alarm-info-row">
              <span class="alarm-info-label">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M5 5h14v14H5z M9 9h6v6H9z M12 2v3 M12 19v3 M2 12h3 M19 12h3" />
                </svg>
                设备
              </span>
              <span class="alarm-info-value">{{
                popupAlarm?.deviceName || popupAlarm?.name || '未知设备'
              }}</span>
            </div>
            <div class="alarm-info-row">
              <span class="alarm-info-label">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0z" />
                </svg>
                位置
              </span>
              <span class="alarm-info-value">{{ popupAlarm?.deviceAddr || '未知位置' }}</span>
            </div>
            <div class="alarm-info-row">
              <span class="alarm-info-label">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 6v6l4 2" />
                </svg>
                时间
              </span>
              <span class="alarm-info-value">{{ formatPopupTime(popupAlarm?.alertTime) }}</span>
            </div>
            <div class="alarm-info-row">
              <span class="alarm-info-label">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8"
                  />
                </svg>
                详情
              </span>
              <span class="alarm-info-value alarm-detail-text">{{
                popupAlarm?.details || '无详情'
              }}</span>
            </div>
            <!-- 实时视频入口：从抓拍图上移到详细信息区；有绑定摄像头才出现（无摄像头设备不出现） -->
            <div v-if="assistOwnCameras.length" class="alarm-info-row">
              <span class="alarm-info-label">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M23 7l-7 5 7 5V7z M1 5h15v14H1z" />
                </svg>
                视频
              </span>
              <span class="alarm-info-value">
                <button class="alarm-video-btn" @click.stop="openVideoDialog">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M3 6h12v12H3z M15 10l6-3v10l-6-3" />
                  </svg>
                  实时预览
                </button>
              </span>
            </div>
          </div>
        </div>

        <!-- 辅助判断：真实数据（安装空间 + 同区域设备 + 实时预览/设备控制室链接） -->
        <AlarmAssistPanel
          :alarm-id="popupAlarm?.id"
          @loaded="onAssistLoaded"
          @view-video="onRowVideo"
        />

        <!-- 底栏 -->
        <div class="alarm-popup-footer">
          <button class="alarm-btn alarm-btn-later" @click="emit('later', popupAlarm)">
            {{ laterText }}
          </button>
          <button class="alarm-btn alarm-btn-now" @click="emit('primary', popupAlarm)">
            {{ primaryText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- 实时视频弹层（主图区/辅助判断行内共用；打开期间倒计时自动暂停） -->
  <AlarmVideoDialog
    v-model="videoDialogVisible"
    :cameras="activeVideoCameras"
    :title="activeVideoTitle"
  />
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import AlarmAssistPanel from '@/views/eiot/firewatch/components/AlarmAssistPanel.vue'
import AlarmVideoDialog from '@/views/eiot/firewatch/components/AlarmVideoDialog.vue'
import type { AlarmAssistCamera, AlarmAssistData } from '@/api/eiot/firewatch/alarmAssist.api'
import { stopAlarmSoundLoop, isAlarmSoundEnabled, toggleAlarmSound } from '@/utils/alarmSound'
import { speakAlarmSpeech, stopAlarmSpeech } from '@/utils/alarmSpeech'

withDefaults(
  defineProps<{
    /** 左按钮文案（值守大屏=稍后处置，监控中心=标记误报） */
    laterText?: string
    /** 右按钮文案（值守大屏=去处置，监控中心=确认火警） */
    primaryText?: string
    /** 遮罩是否变暗：值守大屏保持透明遮罩传 false，监控中心默认变暗 */
    dimOverlay?: boolean
  }>(),
  { laterText: '稍后处置', primaryText: '去处置', dimOverlay: true }
)

const emit = defineEmits<{
  /** 左按钮点击（动作由使用方处理：稍后处置/标记误报） */
  later: [alarm: any]
  /** 右按钮点击（动作由使用方处理：去处置/确认火警） */
  primary: [alarm: any]
  /** 点击遮罩空白处（使用方自行决定：关闭或按左按钮语义处理） */
  overlay: [alarm: any]
}>()

const popupVisible = ref(false)
const popupAlarm = ref<any>(null)
const popupCountdown = ref(15)
const alertQueue = ref<any[]>([])
// 队列进度（顶栏「第 x/y 条」）：queueIndex=当前正在看的第几条，queueTotal=本轮总数
const queueIndex = ref(0)
const queueTotal = ref(0)
let popupAutoHideTimer: ReturnType<typeof setInterval> | null = null
let nextAlertTimer: ReturnType<typeof setTimeout> | null = null

const POPUP_AUTO_HIDE_SECONDS = 15
const popupCountdownPercent = computed(() => (popupCountdown.value / POPUP_AUTO_HIDE_SECONDS) * 100)

// 一级未处置告警的重复提醒周期（毫秒）：弹窗 15s 自动消失后，只要告警仍未被处置（仍出现在后端未处置列表中），
// 距上次弹出满一个周期就重新入队弹出，直到处置完成（handle_status=2 后不再出现在列表中）
const HEAVY_REMINDER_INTERVAL = 60 * 1000
// 每条一级告警最近一次弹出提醒的时间（页面内存态）
const heavyAlertLastShownAt = new Map<number, number>()

// ====== 视频弹层与倒计时暂停 ======
const videoDialogVisible = ref(false)
const manualPaused = ref(false)
// 报警设备绑定摄像头（辅助接口 ownCameras）：决定「实时预览」入口是否出现
const assistOwnCameras = ref<AlarmAssistCamera[]>([])
// 视频弹层当前播放源：主图区入口=报警设备自身摄像头，行内入口=所选设备行的摄像头
const activeVideoCameras = ref<AlarmAssistCamera[]>([])
const activeVideoTitle = ref('')
const countdownPaused = computed(
  () => manualPaused.value || videoDialogVisible.value
)
const onAssistLoaded = (d: AlarmAssistData | null) => {
  assistOwnCameras.value = d?.ownCameras || []
}
const openVideoDialog = () => {
  if (!assistOwnCameras.value.length) return
  activeVideoCameras.value = assistOwnCameras.value
  activeVideoTitle.value = popupAlarm.value?.deviceName || popupAlarm.value?.name || ''
  videoDialogVisible.value = true
}
// 辅助判断行内「实时预览」：与主图区入口统一走视频弹层（不再跳全屏视频监控页）
const onRowVideo = (payload: { cameras: AlarmAssistCamera[]; title: string }) => {
  if (!payload?.cameras?.length) return
  activeVideoCameras.value = payload.cameras
  activeVideoTitle.value = payload.title
  videoDialogVisible.value = true
}
const onToggleCountdownPause = () => {
  manualPaused.value = !manualPaused.value
}

// 弹窗等级归一：firewatch-alert-new 通道给落库数字（1/2/3），alarm-push 通道给英文级别（critical/major/warning）
const getPopupSeverity = () => {
  const level = popupAlarm.value?.level
  if (level === '1' || level === 'critical') return 'critical'
  if (level === '2' || level === 'major') return 'major'
  if (level === '3' || level === 'warning') return 'warning'
  return 'info'
}

const getPopupSeverityText = () => {
  const map: Record<string, string> = {
    critical: '紧急',
    major: '较重',
    warning: '一般',
    info: '提示'
  }
  return map[getPopupSeverity()] || '未知'
}

const formatPopupTime = (t: any) => {
  if (!t) return ''
  if (typeof t === 'number') {
    return new Date(t).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  }
  return String(t).replace('T', ' ').substring(0, 19)
}

const startAutoHide = () => {
  stopAutoHide()
  popupCountdown.value = POPUP_AUTO_HIDE_SECONDS
  // 新弹窗重置手动暂停（每台设备独立重新计时）
  manualPaused.value = false
  popupAutoHideTimer = setInterval(() => {
    // 暂停中本秒不递减；恢复后沿用剩余秒数继续，不重置
    if (countdownPaused.value) return
    popupCountdown.value--
    if (popupCountdown.value <= 0) {
      closePopup()
    }
  }, 1000)
}

const stopAutoHide = () => {
  if (popupAutoHideTimer) {
    clearInterval(popupAutoHideTimer)
    popupAutoHideTimer = null
  }
}

const closePopup = () => {
  stopAutoHide()
  stopAlarmSoundLoop()
  stopAlarmSpeech()
  videoDialogVisible.value = false
  popupVisible.value = false
  popupAlarm.value = null
  nextAlertTimer = setTimeout(() => showNextAlert(), 300)
}

const showNextAlert = () => {
  if (alertQueue.value.length === 0) {
    popupVisible.value = false
    // 队列消化完毕，重置进度（下一轮从第 1 条重新计）
    queueIndex.value = 0
    queueTotal.value = 0
    return
  }
  popupAlarm.value = alertQueue.value.shift()
  // 切换弹窗时收起视频弹层并清空上一台的摄像头（防止串台）
  videoDialogVisible.value = false
  assistOwnCameras.value = []
  activeVideoCameras.value = []
  activeVideoTitle.value = ''
  // 队列进度：总数按「已弹出序号 + 剩余」修正，新告警入队时只增不减
  queueIndex.value += 1
  queueTotal.value = Math.max(queueTotal.value, queueIndex.value + alertQueue.value.length)
  // 记录本次弹出提醒时间，供入队时对仍未处置的告警按周期重弹
  if (popupAlarm.value?.id != null) {
    heavyAlertLastShownAt.set(popupAlarm.value.id, Date.now())
  }
  popupVisible.value = true
  startAutoHide()
  // 按报警类型语音播报
  speakAlarmSpeech(popupAlarm.value)
}

/** 一级告警是否到期需要（重新）提醒：不在弹窗/队列中，且距上次弹出已满重复提醒周期 */
const isHeavyRepeatDue = (alarm: any) => {
  if (alarm?.id == null) return false
  if (popupAlarm.value?.id === alarm.id) return false
  if (alertQueue.value.some((q: any) => q.id === alarm.id)) return false
  const lastShownAt = heavyAlertLastShownAt.get(alarm.id)
  return lastShownAt == null || Date.now() - lastShownAt >= HEAVY_REMINDER_INTERVAL
}

/**
 * 一级告警入队（轮询/WS 推送统一入口）：重复提醒判定依赖本组件内部状态（当前弹窗/队列/上次弹出时间），
 * 所以判定放在组件内；调用方只需把"可能的一级告警"交给它，空闲时立即弹出、忙碌时排队。
 */
const enqueue = (alarm: any) => {
  if (!isHeavyRepeatDue(alarm)) return
  alertQueue.value.push(alarm)
  queueTotal.value = Math.max(queueTotal.value, queueIndex.value + alertQueue.value.length)
  if (!popupVisible.value) {
    showNextAlert()
  }
}

// Sound
const soundEnabled = ref(isAlarmSoundEnabled())
const onToggleSound = () => {
  soundEnabled.value = toggleAlarmSound()
}

const onOverlayClick = () => {
  if (popupAlarm.value) emit('overlay', popupAlarm.value)
}

onBeforeUnmount(() => {
  stopAutoHide()
  if (nextAlertTimer) {
    clearTimeout(nextAlertTimer)
    nextAlertTimer = null
  }
  stopAlarmSoundLoop()
  stopAlarmSpeech()
})

// 使用方用 ref 调用：enqueue 入队一级告警；close 关闭当前弹窗（动作完成后调用）
defineExpose({ enqueue, close: closePopup })
</script>

<style scoped lang="scss">
// ====== 报警弹窗样式（两屏共用） ======
.alarm-popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(1px);
  animation: alarmFadeIn 0.3s ease;

  // 值守大屏保持透明遮罩（dimOverlay=false）
  &.no-dim {
    background: rgba(0, 0, 0, 0);
  }

  // 等级闪烁动画
  &.flash-critical {
    animation:
      alarmFadeIn 0.3s ease,
      flashCritical 0.8s ease-in-out infinite;
  }
  &.flash-major {
    animation:
      alarmFadeIn 0.3s ease,
      flashMajor 1.2s ease-in-out infinite;
  }
  &.flash-warning {
    animation:
      alarmFadeIn 0.3s ease,
      flashWarning 1.8s ease-in-out infinite;
  }
}

@keyframes alarmFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes flashCritical {
  0%,
  100% {
    box-shadow:
      inset 0 0 80px rgba(255, 77, 79, 0),
      0 0 0 rgba(255, 77, 79, 0);
  }
  50% {
    box-shadow:
      inset 0 0 80px rgba(255, 77, 79, 0.35),
      0 0 60px rgba(255, 77, 79, 0.25);
  }
}

@keyframes flashMajor {
  0%,
  100% {
    box-shadow:
      inset 0 0 60px rgba(250, 140, 22, 0),
      0 0 0 rgba(250, 140, 22, 0);
  }
  50% {
    box-shadow:
      inset 0 0 60px rgba(250, 140, 22, 0.25),
      0 0 40px rgba(250, 140, 22, 0.2);
  }
}

@keyframes flashWarning {
  0%,
  100% {
    box-shadow:
      inset 0 0 40px rgba(250, 219, 20, 0),
      0 0 0 rgba(250, 219, 20, 0);
  }
  50% {
    box-shadow:
      inset 0 0 40px rgba(250, 219, 20, 0.15),
      0 0 30px rgba(250, 219, 20, 0.12);
  }
}

.alarm-popup-card {
  width: 690px;
  max-width: 90vw;
  background: #0f2747;
  border: 1px solid rgba(79, 195, 247, 0.15);
  border-radius: 8px;
  overflow: hidden;
  box-shadow:
    0 0 40px rgba(255, 77, 79, 0.25),
    0 16px 48px rgba(0, 0, 0, 0.4);
  animation: alarmSlideIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);

  // 弹窗内图标统一描边风格（替代 emoji），线宽/端点与辅助判断区设备图标一致
  svg {
    display: block;
    flex: none;
    fill: none;
    stroke: currentColor;
    stroke-width: 1.6;
    stroke-linecap: round;
    stroke-linejoin: round;
  }
}

@keyframes alarmSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.alarm-countdown-bar {
  height: 3px;
  background: rgba(30, 58, 95, 0.5);
  .alarm-countdown-fill {
    height: 100%;
    background: linear-gradient(90deg, #ff6b6b, #ff4d4f);
    transition: width 1s linear;
    border-radius: 0 2px 2px 0;
  }
}

.alarm-popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: linear-gradient(135deg, rgba(255, 77, 79, 0.12) 0%, transparent 100%);
  border-bottom: 1px solid rgba(30, 58, 95, 0.8);
}

.alarm-popup-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.alarm-popup-icon {
  width: 20px;
  height: 20px;
  color: #fadb14;
  animation: alarmIconPulse 1.5s ease-in-out infinite;
}

@keyframes alarmIconPulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.15);
  }
}

.alarm-popup-type {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.alarm-popup-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.alarm-popup-severity {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  &.severity-critical {
    background: rgba(255, 77, 79, 0.2);
    color: #ff8a8a;
    border: 1px solid rgba(255, 77, 79, 0.4);
  }
  &.severity-major {
    background: rgba(250, 140, 22, 0.2);
    color: #ffa940;
    border: 1px solid rgba(250, 140, 22, 0.4);
  }
  &.severity-warning {
    background: rgba(250, 219, 20, 0.2);
    color: #fadb14;
    border: 1px solid rgba(250, 219, 20, 0.4);
  }
  &.severity-info {
    background: rgba(24, 144, 255, 0.2);
    color: #40a9ff;
    border: 1px solid rgba(24, 144, 255, 0.4);
  }
}

.alarm-popup-timer {
  font-size: 14px;
  font-weight: 700;
  color: #ff6b6b;
  font-variant-numeric: tabular-nums;
  min-width: 32px;
  text-align: center;
}

.alarm-sound-toggle {
  display: inline-flex;
  align-items: center;
  color: #8b9dc5;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
  svg {
    width: 18px;
    height: 18px;
  }
  &:hover {
    transform: scale(1.2);
  }
  &.muted {
    opacity: 0.5;
  }
}

.alarm-countdown-toggle {
  display: inline-flex;
  align-items: center;
  color: #8b9dc5;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
  svg {
    width: 16px;
    height: 16px;
  }
  &:hover {
    transform: scale(1.2);
  }
  &.paused {
    opacity: 0.5;
  }
}

// "实时预览"两处入口（主图区 + 辅助判断行内）同属主题 accent 色系：淡填充+描边，hover 统一变亮
.alarm-video-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border: 1px solid rgba(79, 195, 247, 0.5);
  border-radius: 4px;
  background: rgba(79, 195, 247, 0.12);
  color: #4fc3f7;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: rgba(79, 195, 247, 0.22);
    border-color: rgba(79, 195, 247, 0.8);
    color: #fff;
  }
  svg {
    width: 14px;
    height: 14px;
  }
}

.alarm-popup-queue {
  font-size: 11px;
  color: #8b9dc5;
  background: rgba(30, 58, 95, 0.6);
  padding: 2px 8px;
  border-radius: 8px;
}

.alarm-popup-body {
  display: flex;
  min-height: 290px;
  padding: 0;
}

.alarm-popup-image {
  position: relative;
  width: 370px;
  min-height: 290px;
  flex-shrink: 0;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.alarm-popup-no-image {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  .no-image-icon {
    width: 48px;
    height: 48px;
    color: #6b7b9a;
    opacity: 0.4;
  }
  .no-image-text {
    font-size: 13px;
    color: #6b7b9a;
  }
}

.alarm-popup-info {
  flex: 1;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alarm-info-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.alarm-info-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #8b9dc5;
  flex-shrink: 0;
  min-width: 70px;
  svg {
    width: 14px;
    height: 14px;
  }
}

.alarm-info-value {
  font-size: 14px;
  color: #fff;
  font-weight: 500;
  flex: 1;
}

.alarm-detail-text {
  line-height: 1.5;
  color: #c8d6e5;
  font-weight: 400;
  word-break: break-all;
}

.alarm-popup-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid rgba(30, 58, 95, 0.8);
  background: rgba(15, 39, 71, 0.6);
}

.alarm-btn {
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
  letter-spacing: 0.5px;
}

.alarm-btn-later {
  background: transparent;
  border-color: rgba(30, 58, 95, 0.8);
  color: #8b9dc5;
  &:hover {
    background: rgba(30, 58, 95, 0.5);
    border-color: #3b82f6;
    color: #fff;
  }
}

.alarm-btn-now {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff6b6b 100%);
  border-color: #ff4d4f;
  color: #fff;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.3);
  &:hover {
    background: linear-gradient(135deg, #ff7875 0%, #ff8a8a 100%);
  }
}
</style>
