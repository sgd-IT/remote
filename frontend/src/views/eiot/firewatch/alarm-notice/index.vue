<template>
  <Teleport to="body">
    <div class="alarm-notice" :class="[alignClass, { 'anchor-top': offsetTop != null }]" :style="anchorStyle">
      <transition name="notice-zoom">
        <div v-if="expanded" class="notice-panel" @wheel.stop>
          <div v-if="!items.length" class="notice-empty">暂无待处置提醒</div>
          <div
            v-for="item in items"
            :key="item.key"
            class="notice-card"
            :class="'lv-' + item.severity"
            @click="openItem(item)"
          >
            <span class="nc-bar"></span>
            <div class="nc-body">
              <div class="nc-head">
                <span class="nc-tag">{{ item.levelText }}</span>
                <span class="nc-name">{{ item.name }}</span>
                <span v-if="item.count > 1" class="nc-count">×{{ item.count }}</span>
              </div>
              <div class="nc-sub">{{ item.place }}</div>
              <div class="nc-foot">
                <template v-if="showTenant && item.tenantName">{{ item.tenantName }} · </template>
                {{ item.timeText }}
              </div>
            </div>
            <span class="nc-close" title="忽略" @click.stop="ignoreItem(item)">✕</span>
          </div>
        </div>
      </transition>

      <div class="notice-pill" :class="{ 'is-empty': !items.length }" @click="expanded = !expanded">
        <span class="np-dot"></span>
        <span>待处置 {{ items.length }}</span>
      </div>
    </div>
  </Teleport>
</template>

<script lang="ts">
/**
 * 二三级告警轻提示判定（与一级全屏弹窗分流共用）：
 * 明确的一级（'1' / 'critical'）走全屏弹窗；
 * 明确的二三级（'2' '3'、旧编码 '4' '5'、英文 major/warning/info）进通知匣；
 * 缺失或未知 level 按一级处理（就高不就低，宁多打扰不漏火警）。
 */
export function isLightAlarmLevel(level: any): boolean {
  if (level === '1' || level === 'critical') return false
  if (['2', '3', '4', '5', 'major', 'warning', 'info'].includes(String(level))) return true
  return false
}
</script>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { playAlarmSoundOnce } from '@/utils/alarmSound'

defineOptions({ name: 'AlarmNotice' })

const props = withDefaults(
  defineProps<{
    /** 锚定右侧偏移（与 offsetLeft 二选一）：如 duty-screen 右栏左缘附近 */
    offsetRight?: number
    /** 锚定左侧偏移（与 offsetRight 二选一）：如 monitoring-center 中栏左缘附近 */
    offsetLeft?: number
    /** 距底部偏移（默认底部锚定；设定 offsetTop 后改为顶部锚定） */
    offsetBottom?: number
    /** 距顶部偏移（与 offsetBottom 二选一，设定后面板向下展开） */
    offsetTop?: number
    /** 是否展示单位名（多租户屏开、企业单租户屏关） */
    showTenant?: boolean
  }>(),
  { offsetRight: 24, offsetLeft: undefined, offsetBottom: 20, offsetTop: undefined, showTenant: false }
)

const emit = defineEmits<{ open: [alarm: any] }>()

interface NoticeItem {
  key: string
  id?: number
  deviceId?: any
  name: string
  place: string
  tenantName: string
  level: string
  severity: 'major' | 'warning'
  levelText: string
  timeText: string
  ts: number
  count: number
  raw: any
}

// 同设备同事件合并时间窗 / 列表上限
const MERGE_WINDOW = 5 * 60 * 1000
const MAX_ITEMS = 50

const items = ref<NoticeItem[]>([])
const expanded = ref(false)

const anchorStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.offsetTop != null) style.top = `${props.offsetTop}px`
  else style.bottom = `${props.offsetBottom}px`
  if (props.offsetLeft != null) style.left = `${props.offsetLeft}px`
  else style.right = `${props.offsetRight}px`
  return style
})

const alignClass = computed(() => (props.offsetLeft != null ? 'align-left' : 'align-right'))

const formatTime = (t: any) => {
  if (!t) return ''
  if (typeof t === 'number')
    return new Date(t).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  return String(t).replace('T', ' ').substring(0, 19)
}

const normalize = (alarm: any): NoticeItem => {
  const level = String(alarm?.level ?? alarm?.alarmLevel ?? '')
  const severity: 'major' | 'warning' = level === '2' || level === 'major' ? 'major' : 'warning'
  return {
    key: `${alarm?.id ?? alarm?.alertId ?? ''}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    id: alarm?.id ?? alarm?.alertId,
    deviceId: alarm?.deviceId,
    name: alarm?.name || alarm?.alertName || alarm?.alarmTypeName || alarm?.typeName || '设备告警',
    place:
      [alarm?.deviceName, alarm?.deviceAddr || alarm?.alarmLocation || alarm?.location]
        .filter(Boolean)
        .join(' · ') || '未知位置',
    tenantName: alarm?.tenantName || '',
    level,
    severity,
    levelText: severity === 'major' ? '二级' : '三级',
    timeText: formatTime(alarm?.alertTime || alarm?.alarmTime || alarm?.time),
    ts: Date.now(),
    count: 1,
    raw: alarm
  }
}

/** 页面推送入口：二三级告警到达时调用 */
const push = (alarm: any) => {
  const item = normalize(alarm)
  const dup = items.value.find(
    (i) =>
      i.deviceId != null &&
      i.deviceId === item.deviceId &&
      i.name === item.name &&
      item.ts - i.ts < MERGE_WINDOW
  )
  if (dup) {
    dup.count++
    dup.ts = item.ts
    dup.timeText = item.timeText
  } else {
    items.value.unshift(item)
    if (items.value.length > MAX_ITEMS) items.value.pop()
  }
  playAlarmSoundOnce(item.severity)
}

const removeItem = (item: NoticeItem) => {
  const idx = items.value.indexOf(item)
  if (idx >= 0) items.value.splice(idx, 1)
}

/** 点击条目：交给页面打开详情，条目视为已看移除 */
const openItem = (item: NoticeItem) => {
  removeItem(item)
  emit('open', item.raw)
}

/** 忽略：仅移除，不写任何数据状态 */
const ignoreItem = (item: NoticeItem) => removeItem(item)

watch(
  () => items.value.length,
  (len) => {
    if (!len) expanded.value = false
  }
)

defineExpose({ push })
</script>

<style lang="scss" scoped>
.alarm-notice {
  position: fixed;
  z-index: 9900;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;

  &.align-left {
    align-items: flex-start;
  }

  &.anchor-top {
    flex-direction: column-reverse;

    .notice-zoom-enter-active,
    .notice-zoom-leave-active {
      transform-origin: top center;
    }

    .notice-zoom-enter-from,
    .notice-zoom-leave-to {
      transform: scale(0.92) translateY(-8px);
    }
  }
}

.notice-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(13, 38, 70, 0.92);
  border: 1px solid rgba(79, 195, 247, 0.35);
  color: #dce8ff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.35);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;

  &:hover {
    border-color: rgba(79, 195, 247, 0.7);
    box-shadow: 0 2px 16px rgba(48, 191, 255, 0.25);
  }

  &.is-empty {
    border-color: rgba(79, 195, 247, 0.16);
    color: #8fa3c4;

    .np-dot {
      background: #6d80a3;
    }
  }

  .np-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #ffa940;
  }
}

.notice-panel {
  width: 350px;
  max-height: 240px;
  overflow-y: auto;
  scrollbar-width: none;
  background: rgba(12, 34, 62, 0.96);
  border: 1px solid rgba(79, 195, 247, 0.25);
  border-radius: 10px;
  padding: 8px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.4);
  mask-image: linear-gradient(to bottom, #000 calc(100% - 14px), transparent);

  &::-webkit-scrollbar {
    display: none;
  }
}

.notice-empty {
  padding: 12px 0;
  text-align: center;
  color: #8fa3c4;
  font-size: 12px;
}

.notice-card {
  display: flex;
  align-items: stretch;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(18, 44, 78, 0.95);
  border: 1px solid rgba(79, 195, 247, 0.16);
  cursor: pointer;
  transition:
    border-color 0.2s,
    background 0.2s;

  & + .notice-card {
    margin-top: 8px;
  }

  &:hover {
    border-color: rgba(79, 195, 247, 0.45);
    background: rgba(22, 54, 94, 0.98);
  }

  .nc-bar {
    width: 3px;
    border-radius: 2px;
    flex-shrink: 0;
    align-self: stretch;
  }

  &.lv-major {
    .nc-bar {
      background: #ffa940;
    }

    .nc-tag {
      color: #ffa940;
      background: rgba(250, 140, 22, 0.15);
      border: 1px solid rgba(250, 140, 22, 0.35);
    }
  }

  &.lv-warning {
    .nc-bar {
      background: #8c8c8c;
    }

    .nc-tag {
      color: #a6b4cc;
      background: rgba(140, 140, 140, 0.15);
      border: 1px solid rgba(140, 140, 140, 0.35);
    }
  }

  .nc-body {
    flex: 1;
    min-width: 0;
  }

  .nc-head {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .nc-tag {
    font-size: 11px;
    padding: 1px 6px;
    border-radius: 4px;
    font-weight: 600;
    flex-shrink: 0;
  }

  .nc-name {
    color: #e8f1ff;
    font-size: 13px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .nc-count {
    color: #ffa940;
    font-size: 12px;
    font-weight: 700;
    flex-shrink: 0;
  }

  .nc-sub {
    color: #9fb2d0;
    font-size: 12px;
    margin-top: 3px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .nc-foot {
    color: #6d80a3;
    font-size: 11px;
    margin-top: 2px;
  }

  .nc-close {
    color: #6d80a3;
    font-size: 12px;
    padding: 2px 4px;
    flex-shrink: 0;
    align-self: flex-start;
    cursor: pointer;

    &:hover {
      color: #ff4d4f;
    }
  }
}

.notice-zoom-enter-active,
.notice-zoom-leave-active {
  transition:
    opacity 0.2s,
    transform 0.24s cubic-bezier(0.34, 1.2, 0.4, 1);
  transform-origin: bottom center;
}

.notice-zoom-enter-from,
.notice-zoom-leave-to {
  opacity: 0;
  transform: scale(0.92) translateY(8px);
}
</style>
