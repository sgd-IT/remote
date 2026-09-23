/**
 * 报警弹窗逻辑 composable
 *
 * 功能：
 * 1. 检测新报警，弹窗展示
 * 2. 5分钟内同一设备同一点位同类型报警去重
 * 3. 报警队列管理（一次只弹一个，关闭后播放下一个）
 * 4. 自动播放报警音
 */
import { ref } from 'vue'
import { speakAlarmSpeech, stopAlarmSpeech } from '@/utils/alarmSpeech'

// 去重缓存条目
interface DedupEntry {
  timestamp: number
  alarmId: string | number
}

// 报警数据结构（兼容两个大屏的字段差异）
export interface AlarmItem {
  id?: string | number
  deviceId?: string | number
  deviceName?: string
  alarmDescription?: string
  alarmContent?: string
  alarmTime?: string
  alarmLevel?: string
  alarmTypeName?: string
  alarmLocation?: string
  // monitoring-center 映射后的字段
  content?: string
  time?: string
  level?: string
  typeName?: string
  location?: string
  // 语音播报字段（后端 WS 推送 alarm-push / 轮询接口提供）
  alarmType?: number
  tenantName?: string
  subDeviceName?: string
  details?: string
}

// ========== 模块级全局状态（跨组件/跨页面共享） ==========

/** 去重缓存 key -> 条目 */
const dedupCache = new Map<string, DedupEntry>()
const DEDUP_WINDOW_MS = 5 * 60 * 1000 // 5 分钟

// 去重缓存清理定时器
let cleanupTimer: ReturnType<typeof setInterval> | null = null
function ensureCleanup() {
  if (cleanupTimer) return
  cleanupTimer = setInterval(() => {
    const now = Date.now()
    for (const [key, entry] of dedupCache.entries()) {
      if (now - entry.timestamp > DEDUP_WINDOW_MS) {
        dedupCache.delete(key)
      }
    }
  }, 60 * 1000)
}
ensureCleanup()

// ========== 工具函数 ==========

/**
 * 构建去重键：设备名 + 报警类型 + 位置
 * 满足"同一设备同一点位的相同信息只播报一次"的需求
 */
function buildDedupKey(alarm: AlarmItem): string {
  const device = alarm.deviceId ?? alarm.deviceName ?? 'unknown'
  const type = alarm.alarmTypeName ?? alarm.typeName ?? alarm.alarmLevel ?? alarm.level ?? alarm.alarmDescription ?? alarm.content ?? 'unknown'
  const loc = alarm.alarmLocation ?? alarm.location ?? 'unknown'
  return `${device}|${type}|${loc}`
}

/** 获取今天的日期字符串 YYYY-MM-DD */
function getTodayDateStr(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 判断告警是否是当天发生的
 * 通过检查 alarmTime/time 字段是否以今天的 YYYY-MM-DD 开头
 * 兼容多种格式：字符串、时间戳、数组 [year,month,day,...]
 */
function isTodayAlarm(alarm: AlarmItem): boolean {
  let timeStr: any = alarm.alarmTime ?? alarm.time ?? ''
  if (!timeStr && timeStr !== 0) return false

  // 1. 数组格式 [2026, 8, 8, 11, 32, 33]（Jackson LocalDateTime 默认序列化格式）
  if (Array.isArray(timeStr) && timeStr.length >= 3) {
    const y = String(timeStr[0])
    const m = String(timeStr[1]).padStart(2, '0')
    const d = String(timeStr[2]).padStart(2, '0')
    timeStr = `${y}-${m}-${d}`
    return timeStr === getTodayDateStr()
  }

  // 2. 时间戳数字（毫秒）
  if (typeof timeStr === 'number') {
    const d = new Date(timeStr)
    if (isNaN(d.getTime())) return false
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    timeStr = `${y}-${m}-${day}`
  }

  // 3. 字符串格式："2026-08-08 11:32:33" 或 "2026-08-08T11:32:33"
  timeStr = String(timeStr)
  const today = getTodayDateStr()
  // 提取日期部分（前10个字符）
  const datePart = timeStr.length >= 10 ? timeStr.substring(0, 10) : timeStr
  return datePart === today
}

/** 播放报警音 */
let audioCtx: AudioContext | null = null
function playAlarmSound() {
  try {
    if (!audioCtx) audioCtx = new AudioContext()
    if (audioCtx.state === 'suspended') audioCtx.resume()
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.connect(gain)
    gain.connect(audioCtx.destination)
    osc.frequency.value = 880
    osc.type = 'sine'
    gain.gain.value = 0.3
    osc.start()
    gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5)
    osc.stop(audioCtx.currentTime + 0.5)
  } catch {
    // 静默失败（浏览器可能限制 AudioContext）
  }
}

/** 关闭 AudioContext（页面卸载时调用） */
export function closeAudio() {
  if (audioCtx) {
    audioCtx.close()
    audioCtx = null
  }
}

// ========== Composable ==========

export function useAlarmPopup() {
  // 是否显示弹窗
  const showPopup = ref(false)
  // 当前显示的报警
  const currentAlarm = ref<AlarmItem | null>(null)
  // 等待队列
  const alarmQueue = ref<AlarmItem[]>([])
  // 已知的报警 ID 集合（用于检测新报警）
  let knownAlarmIds = new Set<string | number>()
  // 是否已完成首次加载
  let initialized = false
  // 自动关闭定时器
  let dismissTimer: ReturnType<typeof setTimeout> | null = null

  /** 检查并显示下一个队列中的报警 */
  function showNextAlarm() {
    if (dismissTimer) {
      clearTimeout(dismissTimer)
      dismissTimer = null
    }
    if (alarmQueue.value.length > 0 && !showPopup.value) {
      currentAlarm.value = alarmQueue.value.shift()!
      showPopup.value = true
      // 按报警类型语音播报
      speakAlarmSpeech(currentAlarm.value)
      // 8 秒后自动关闭
      dismissTimer = setTimeout(() => {
        dismissPopup()
      }, 8000)
    }
  }

  /** 关闭当前弹窗 */
  function dismissPopup() {
    showPopup.value = false
    currentAlarm.value = null
    stopAlarmSpeech()
    if (dismissTimer) {
      clearTimeout(dismissTimer)
      dismissTimer = null
    }
    // 短暂延迟后播放下一个
    setTimeout(() => showNextAlarm(), 500)
  }

  /**
   * 推入新报警到队列
   * 会先过一遍去重检查
   */
  function pushAlarm(alarm: AlarmItem) {
    if (!canBroadcast(alarm)) return
    alarmQueue.value.push(alarm)
    showNextAlarm()
  }

  /**
   * 检查该报警是否可以播报（去重检查）
   */
  function canBroadcast(alarm: AlarmItem): boolean {
    const key = buildDedupKey(alarm)
    const now = Date.now()
    const cached = dedupCache.get(key)
    if (cached && now - cached.timestamp < DEDUP_WINDOW_MS) {
      // 5 分钟内已播报过，跳过
      return false
    }
    // 更新缓存
    dedupCache.set(key, { timestamp: now, alarmId: alarm.id ?? key })
    return true
  }

  /**
   * 批量检查新报警（在 loadData 中调用）
   * @param newAlarms 最新的报警列表
   *
   * 首次调用只记录已知 ID，不触发弹窗
   * 后续调用只处理当天的报警，对比新增的 ID 并触发弹窗
   */
  function checkNewAlarms(newAlarms: AlarmItem[]) {
    // 只保留当天报警
    const todayAlarms = newAlarms.filter(isTodayAlarm)

    // 提取所有报警 ID
    const currentIds = new Set<string | number>()
    for (const a of todayAlarms) {
      if (a.id != null) currentIds.add(a.id)
    }

    // 首次加载：只记录 ID，不弹窗
    if (!initialized) {
      initialized = true
      knownAlarmIds = currentIds
      return
    }

    // 查找新增的当天报警
    const addedAlarms: AlarmItem[] = []
    for (const alarm of todayAlarms) {
      if (alarm.id != null && !knownAlarmIds.has(alarm.id)) {
        addedAlarms.push(alarm)
      }
    }

    // 更新已知 ID 集合
    knownAlarmIds = currentIds

    // 如果有新增报警，推入队列
    for (const alarm of addedAlarms) {
      pushAlarm(alarm)
    }
  }

  /** 组件卸载时清理 */
  function cleanup() {
    if (dismissTimer) {
      clearTimeout(dismissTimer)
      dismissTimer = null
    }
  }

  return {
    /** 是否显示弹窗 */
    showPopup,
    /** 当前显示的报警 */
    currentAlarm,
    /** 报警队列 */
    alarmQueue,
    /** 关闭弹窗 */
    dismissPopup,
    /** 批量检查新报警 */
    checkNewAlarms,
    /** 直接推入单条报警（供 WebSocket 实时推送调用） */
    pushAlarm,
    /** 清理 */
    cleanup,
  }
}
