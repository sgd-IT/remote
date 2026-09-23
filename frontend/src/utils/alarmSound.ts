/**
 * 报警提示音 - 方案A（按等级频率 Web Audio API）
 *
 * 等级频率配置表:
 *   critical: 1200Hz sawtooth  - 急促尖锐
 *   major:    880Hz  square    - 较高方波
 *   warning:  660Hz  sine      - 柔和正弦
 *   info:     440Hz  sine      - 低频提示
 *
 * 功能:
 *   - 按等级播放不同频率/波形
 *   - 循环播放（可配置间隔）
 *   - 总开关（localStorage 持久化）
 */

// ========== 等级频率配置表 ==========
interface SoundConfig {
  frequency: number
  type: OscillatorType
  duration: number   // 单次持续时间(秒)
  interval: number   // 循环间隔(秒)
  volume: number     // 音量 0~1
}

const SOUND_CONFIG: Record<string, SoundConfig> = {
  critical: { frequency: 1200, type: 'sawtooth', duration: 0.3, interval: 1.0, volume: 0.35 },
  major:    { frequency: 880,  type: 'square',   duration: 0.4, interval: 1.5, volume: 0.25 },
  warning:  { frequency: 660,  type: 'sine',     duration: 0.5, interval: 2.5, volume: 0.2  },
  info:     { frequency: 440,  type: 'sine',     duration: 0.3, interval: 3.0, volume: 0.15 },
}

const STORAGE_KEY = 'alarm_sound_enabled'

// ========== 状态 ==========
let audioCtx: AudioContext | null = null
let loopTimer: ReturnType<typeof setInterval> | null = null
let currentLevel = 'warning'
let _enabled = true

// 初始化读取 localStorage
function loadEnabled(): boolean {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored !== null) return stored === 'true'
  } catch {}
  return true // 默认开启
}

_enabled = loadEnabled()

// ========== 核心函数 ==========

/** 获取 AudioContext（懒初始化） */
function getAudioContext(): AudioContext | null {
  try {
    if (!audioCtx) audioCtx = new AudioContext()
    if (audioCtx.state === 'suspended') audioCtx.resume()
    return audioCtx
  } catch {
    return null
  }
}

/** 播放单次音效 */
function playOnce(level: string) {
  if (!_enabled) return
  const ctx = getAudioContext()
  if (!ctx) return

  const config = SOUND_CONFIG[level] || SOUND_CONFIG.warning
  const osc = ctx.createOscillator()
  const gain = ctx.createGain()
  osc.connect(gain)
  gain.connect(ctx.destination)

  osc.frequency.value = config.frequency
  osc.type = config.type
  gain.gain.setValueAtTime(config.volume, ctx.currentTime)
  gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + config.duration)

  osc.start(ctx.currentTime)
  osc.stop(ctx.currentTime + config.duration)
}

/** 停止循环播放 */
export function stopAlarmSoundLoop() {
  if (loopTimer) {
    clearInterval(loopTimer)
    loopTimer = null
  }
}

/** 启动循环播放（按等级） */
export function startAlarmSoundLoop(level: string = 'warning') {
  currentLevel = level
  stopAlarmSoundLoop()

  if (!_enabled) return

  const config = SOUND_CONFIG[level] || SOUND_CONFIG.warning
  // 立即播放一次
  playOnce(level)
  // 按间隔循环
  loopTimer = setInterval(() => {
    playOnce(currentLevel)
  }, config.interval * 1000)
}

/** 播放单次报警音（不循环） */
export function playAlarmSoundOnce(level: string = 'warning') {
  playOnce(level)
}

/** 更新循环等级（不重启循环，下次循环生效） */
export function updateAlarmSoundLevel(level: string) {
  currentLevel = level
}

/** 获取总开关状态 */
export function isAlarmSoundEnabled(): boolean {
  return _enabled
}

/** 设置总开关 */
export function setAlarmSoundEnabled(enabled: boolean) {
  _enabled = enabled
  try {
    localStorage.setItem(STORAGE_KEY, String(enabled))
  } catch {}
  if (!enabled) {
    stopAlarmSoundLoop()
  }
}

/** 切换总开关 */
export function toggleAlarmSound(): boolean {
  setAlarmSoundEnabled(!_enabled)
  return _enabled
}

/** 关闭 AudioContext（页面卸载时调用） */
export function closeAlarmAudio() {
  stopAlarmSoundLoop()
  if (audioCtx) {
    try { audioCtx.close() } catch {}
    audioCtx = null
  }
}
