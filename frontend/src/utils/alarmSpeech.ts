/**
 * 大屏报警弹窗语音播报工具（Web Speech API）
 *
 * 按报警类型（alarmType 0-5）生成不同规则的播报文本：
 *   0=主机报警       单位名称+主机名称+子设备名称(如存在)+"触发"+等级中文+"报警!"
 *   1=高温事件报警   单位名称+设备名称+"发现高温事件报警, 请立即处理!"
 *   2=热成像事件报警 单位名称+设备名称+"发现热成像事件报警, 请立即处理!"
 *   3=烟火事件报警   单位名称+设备名称+"发现烟火事件报警, 请立即处理!"
 *   4=离岗事件报警   单位名称+"发现离岗事件报警, 请立即处理!"
 *   5=消防通道占用   单位名称+设备名称+"发现防通道占用事件报警, 请立即处理"
 *
 * 说明：
 *   - 受 alarmSound 总开关（localStorage: alarm_sound_enabled）控制
 *   - 浏览器不支持 speechSynthesis 时返回 false，调用方可回退蜂鸣音
 *   - Chrome 等浏览器要求页面至少发生过一次用户交互后才允许语音播报，
 *     模块加载时会监听首次交互事件预热解锁（sticky user activation）
 */
import { isAlarmSoundEnabled } from '@/utils/alarmSound'

// ========== 语音合成解锁（speechSynthesis 自动播放限制） ==========
// Chrome 等浏览器对 speechSynthesis.speak() 有自动播放限制：
// 页面未发生过用户交互时调用会被静默忽略（不抛错、不出声）。
// 大屏页面常处于自动值守状态，需在首次交互时预热解锁。
let speechUnlocked = false
let zhVoice: SpeechSynthesisVoice | null = null

/** 刷新缓存的中文语音（voices 异步加载） */
function refreshZhVoice() {
  try {
    const voices = window.speechSynthesis.getVoices()
    const voice = voices.find((v) => v.lang && v.lang.toLowerCase().startsWith('zh'))
    if (voice) zhVoice = voice
  } catch {}
}

/** 预热解锁语音合成（需在用户交互事件中调用） */
function unlockSpeechSynthesis() {
  if (speechUnlocked) return
  try {
    window.speechSynthesis.resume()
    refreshZhVoice()
    // 播报空文本预热，让浏览器记录"用户已允许语音播报"
    const warm = new SpeechSynthesisUtterance(' ')
    warm.volume = 0
    warm.rate = 1
    window.speechSynthesis.speak(warm)
    speechUnlocked = true
  } catch {}
}

// 模块加载时注册首次用户交互监听（全局一次）
;(function bindSpeechUnlock() {
  if (typeof window === 'undefined' || typeof document === 'undefined') return
  if (!('speechSynthesis' in window)) return
  const events = ['pointerdown', 'keydown', 'touchstart', 'click'] as const
  events.forEach((evt) => {
    document.addEventListener(evt, () => unlockSpeechSynthesis(), { once: true, capture: true })
  })
  // voices 异步加载完成后刷新中文语音
  try {
    window.speechSynthesis.onvoiceschanged = refreshZhVoice
  } catch {}
})()

/** 报警等级数字 → 中文类型 */
const LEVEL_TEXT_MAP: Record<string, string> = {
  '1': '火警',
  '2': '故障',
  '3': '隐患',
  '4': '设备离线',
  '5': '误报',
  '6': '信息',
}

/** 报警等级英文名 → 中文类型 */
const LEVEL_TEXT_MAP_BY_NAME: Record<string, string> = {
  critical: '紧急',
  major: '较重',
  warning: '一般',
  info: '提示',
}

/** 语音播报所需字段（兼容两个大屏与轮询/推送两种数据源） */
export interface AlarmSpeechItem {
  /** 报警类型：0=主机报警 1-5=摄像头事件报警 */
  alarmType?: number
  /** 单位名称 */
  tenantName?: string
  /** 设备名称（主机报警时为"主机名称"） */
  deviceName?: string
  /** 子设备名称（主机报警的部件，如存在） */
  subDeviceName?: string
  /** 报警等级（数字编码或 critical/major/warning/info） */
  level?: string
  /** 报警等级（WS 推送 alarm-push 的字段名，英文名） */
  alarmLevel?: string
  /** 告警详情（用于提取"部件: xx"作为子设备名称兜底） */
  details?: string
}

/** 从详情文本中提取部件信息（子设备名称兜底来源） */
function extractSubDeviceName(details?: string): string {
  if (!details) return ''
  const match = details.match(/部件[:：]\s*([^|]+)/)
  return match ? match[1].trim() : ''
}

/** 等级转中文类型 */
function levelToChinese(level?: string): string {
  if (!level) return '报警'
  if (LEVEL_TEXT_MAP[level]) return LEVEL_TEXT_MAP[level]
  if (LEVEL_TEXT_MAP_BY_NAME[level]) return LEVEL_TEXT_MAP_BY_NAME[level]
  return '报警'
}

/** 取报警等级（优先 level，回退 alarmLevel） */
function pickLevel(alarm: AlarmSpeechItem): string | undefined {
  return alarm.level ?? alarm.alarmLevel
}

/** 按报警类型规则生成播报文本 */
export function buildAlarmSpeechText(alarm: AlarmSpeechItem): string {
  const unit = alarm.tenantName || ''
  const device = alarm.deviceName || ''
  const type = alarm.alarmType ?? 0

  switch (type) {
    // 主机报警：单位名称+主机名称+子设备名称(如存在)+"触发"+等级中文+"报警!"
    case 0: {
      const sub = alarm.subDeviceName || extractSubDeviceName(alarm.details)
      return `${unit}${device}${sub}触发${levelToChinese(pickLevel(alarm))}报警!`
    }
    // 高温事件报警
    case 1:
      return `${unit}${device}发现高温事件报警, 请立即处理!`
    // 热成像事件报警
    case 2:
      return `${unit}${device}发现热成像事件报警, 请立即处理!`
    // 烟火事件报警
    case 3:
      return `${unit}${device}发现烟火事件报警, 请立即处理!`
    // 离岗事件报警
    case 4:
      return `${unit}发现离岗事件报警, 请立即处理!`
    // 消防通道占用事件报警
    case 5:
      return `${unit}${device}发现防通道占用事件报警, 请立即处理`
    // 未知类型：按主机报警规则兜底
    default: {
      const sub = alarm.subDeviceName || extractSubDeviceName(alarm.details)
      return `${unit}${device}${sub}触发${levelToChinese(pickLevel(alarm))}报警!`
    }
  }
}

/**
 * 语音播报
 *
 * @param alarm 报警数据
 * @param onFallback 语音无法发声时（不支持/未解锁/被浏览器静默阻止）的回退回调（如蜂鸣音）
 * @return true=语音已处理（已播报或静音中）；false=无法语音播报（已触发 onFallback）
 */
export function speakAlarmSpeech(alarm: AlarmSpeechItem, onFallback?: () => void): boolean {
  // 总开关关闭时静音，视为已处理（与蜂鸣开关保持一致）
  if (!isAlarmSoundEnabled()) return true
  try {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      onFallback?.()
      return false
    }
    // 未解锁：页面还没有发生过用户交互，speak() 会被浏览器静默忽略，直接回退蜂鸣
    if (!speechUnlocked) {
      onFallback?.()
      return false
    }
    const text = buildAlarmSpeechText(alarm)
    if (!text) return true

    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    utterance.rate = 1
    utterance.volume = 1
    if (zhVoice) utterance.voice = zhVoice

    // 取消未播完的语音，避免叠加（resume 防止长时间未播后进入 paused 状态）
    try {
      window.speechSynthesis.resume()
      window.speechSynthesis.cancel()
    } catch {}

    // 稍延迟播报，规避 Chrome cancel() 后立即 speak() 偶发无声的问题
    setTimeout(() => {
      try {
        window.speechSynthesis.speak(utterance)
      } catch {}
    }, 60)

    // 兑底校验：若语音被浏览器静默阻止（speaking/pending 均未激活），触发回退回调
    if (onFallback) {
      setTimeout(() => {
        try {
          if (!window.speechSynthesis.speaking && !window.speechSynthesis.pending) {
            onFallback()
          }
        } catch {
          onFallback()
        }
      }, 400)
    }
    return true
  } catch {
    onFallback?.()
    return false
  }
}

/** 停止语音播报（关闭弹窗/页面卸载时调用） */
export function stopAlarmSpeech() {
  try {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
  } catch {}
}
