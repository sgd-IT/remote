<template>
  <div class="main-keyboard">
    <div class="section-title">主键盘</div>
    <div class="keyboard-body">
      <div class="indicators-panel">
        <div class="panel-label"></div>
        <div class="indicators">
          <div v-for="item in indicators" :key="item.name" class="indicator">
            <span class="indicator-dot" :class="item.active ? item.type : 'off'"></span>
            <span class="indicator-name">{{ item.name }}</span>
          </div>
        </div>
      </div>
      <div class="controls-panel">
        <div class="panel-label">控制操作</div>
        <div class="control-btns">
          <button
            v-for="btn in controlButtons"
            :key="btn.name"
            class="control-btn"
            :class="[`btn-${btn.color}`, { disabled: !btn.function }]"
            :disabled="!btn.function"
            @click="handleClick(btn)"
          >
            <el-icon class="btn-icon"><component :is="btn.iconComp" /></el-icon>
            <span class="btn-text">{{ btn.name }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import type { DeviceFunctionVO } from '@/api/eiot/remote/deviceRemote.api'
import { getDeviceWithProperty } from '@/api/eiot/remote/deviceRemote.api'
import {
  Mute, Refresh, CircleCheck, SwitchButton,
  Setting, Delete, Lock, Unlock, Promotion
} from '@element-plus/icons-vue'

const props = defineProps<{
  functions: DeviceFunctionVO[]
  deviceId?: string | number
}>()

const emit = defineEmits<{
  (e: 'action', fn: DeviceFunctionVO): void
}>()

const generalFunctions = computed(() => props.functions.filter(f => !f.functionType || f.functionType === 'GENERAL'))

// ========== 设备状态轮询（3s） ==========
const statusMap = ref<Record<string, boolean>>({})
const online = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null
let loading = false

const isTrue = (v?: string) => v === 'true' || v === '1'

const loadStatus = async () => {
  if (!props.deviceId || loading) return
  loading = true
  try {
    const res = await getDeviceWithProperty(props.deviceId)
    online.value = !!res?.online
    const p = res?.identifier2property || {}
    statusMap.value = {
      fire: isTrue(p.fire_status?.value),
      fault: isTrue(p.fault_status?.value) || isTrue(p.controller_fault_status?.value),
      feedback: isTrue(p.feedback_status?.value),
      shield: isTrue(p.shield_status?.value),
      start: isTrue(p.start_status?.value),
      supervisory: isTrue(p.supervisory_status?.value)
    }
  } catch (e) {
    // 查询失败保留上一次状态
  } finally {
    loading = false
  }
}

const stopPolling = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

watch(() => props.deviceId, (id) => {
  stopPolling()
  statusMap.value = {}
  if (id) {
    loadStatus()
    pollTimer = setInterval(loadStatus, 3000)
  }
}, { immediate: true })

onBeforeUnmount(stopPolling)

const indicators = computed(() => [
  { name: '火警', type: 'danger', active: statusMap.value.fire === true },
  { name: '监管', type: 'primary', active: statusMap.value.supervisory === true },
  { name: '反馈', type: 'warning', active: statusMap.value.feedback === true },
  { name: '主电', type: 'success', active: online.value },
  { name: '启动', type: 'primary', active: statusMap.value.start === true },
  { name: '故障', type: 'danger', active: statusMap.value.fault === true },
  { name: '屏蔽', type: 'warning', active: statusMap.value.shield === true },
  { name: '备电', type: 'success', active: online.value },
  { name: '手动', type: 'primary', active: false },
  { name: '自动', type: 'success', active: false },
  { name: '喷洒', type: 'primary', active: false },
  { name: '消音', type: 'warning', active: false }
])

/**
 * 兜底固定按钮列表：
 * - 消音 → 红色
 * - 复位、点位屏蔽、解除屏蔽 → 黄色
 * - 自动允许、自动禁止、手动允许、手动禁止 → 蓝色
 */
const FALLBACK_DEFS = [
  { name: '消音', iconComp: Mute, color: 'red' },
  { name: '复位', iconComp: Refresh, color: 'yellow' },
  { name: '自动允许', iconComp: CircleCheck, color: 'blue' },
  { name: '自动禁止', iconComp: Setting, color: 'blue' },
  { name: '手动允许', iconComp: Promotion, color: 'blue' },
  { name: '手动禁止', iconComp: SwitchButton, color: 'blue' },
  { name: '点位屏蔽', iconComp: Lock, color: 'yellow' },
  { name: '解除屏蔽', iconComp: Unlock, color: 'yellow' }
]

/** 按名称猜测按钮颜色 */
const guessColor = (name: string): string => {
  if (name.includes('消音')) return 'red'
  if (name.includes('屏蔽') || name.includes('复位')) return 'yellow'
  return 'blue'
}

const controlButtons = computed(() => {
  const apiFunctions = generalFunctions.value
  // 如果接口返回了可用操作按钮，优先展示
  if (apiFunctions.length > 0) {
    // 先匹配兜底列表中的按钮，再添加 API 独有的按钮
    const matchedNames = new Set<string>()
    const result: any[] = []

    // 1. 按兜底顺序匹配 API 按钮
    for (const def of FALLBACK_DEFS) {
      const fn = apiFunctions.find(f => (f.buttonName || f.functionName || '').includes(def.name))
      if (fn) {
        matchedNames.add(fn.buttonName || fn.functionName || '')
        result.push({ ...def, function: fn })
      }
    }

    // 2. 添加 API 返回但未匹配兜底列表的按钮
    for (const fn of apiFunctions) {
      const fnName = fn.buttonName || fn.functionName || ''
      if (!matchedNames.has(fnName)) {
        result.push({
          name: fnName,
          iconComp: Promotion,
          color: guessColor(fnName),
          function: fn
        })
      }
    }
    return result
  }
  // 兜底：无自定义按钮时显示固定列表（全部禁用）
  return FALLBACK_DEFS.map(d => ({ ...d, function: null }))
})

const handleClick = (btn: any) => {
  if (btn.function) emit('action', btn.function)
}
</script>

<style lang="scss" scoped>
@import '../styles/common.scss';

.main-keyboard {
  background: $bg-panel;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid $border-color;
}

.section-title {
  font-size: 15px;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: 14px;
}

.keyboard-body {
  display: flex;
  gap: 20px;
}

.indicators-panel,
.controls-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.panel-label {
  font-size: 13px;
  color: $text-secondary;
}

.indicators {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  align-content: space-evenly;
}

.indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: $text-secondary;
}

.indicator-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: $text-muted;
  &.danger { background: $danger; box-shadow: 0 0 6px $danger; }
  &.warning { background: $warning; box-shadow: 0 0 6px $warning; }
  &.success { background: $success; box-shadow: 0 0 6px $success; }
  &.primary { background: $primary; box-shadow: 0 0 6px $primary; }
}

/* 控制按钮：3行排列（3+3+2），最后一行2个按钮居中 */
.control-btns {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  align-content: space-evenly;
}

.control-btn {
  grid-column: span 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 42px;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

/* 最后两个按钮居中：第7个占第2-3列，第8个占第4-5列 */
.control-btn:nth-child(7) {
  grid-column: 2 / span 2;
}
.control-btn:nth-child(8) {
  grid-column: 4 / span 2;
}

.btn-icon {
  font-size: 16px;
}

.btn-text {
  white-space: nowrap;
}

/* 红色：消音 */
.control-btn.btn-red {
  background: darken($danger, 22%);
  border-color: darken($danger, 15%);
  color: #fff;
  &:hover:not(:disabled) {
    background: darken($danger, 15%);
    box-shadow: 0 0 10px rgba($danger, 0.35);
  }
}

/* 黄色：复位、点位屏蔽、解除屏蔽 */
.control-btn.btn-yellow {
  background: darken($warning, 22%);
  border-color: darken($warning, 15%);
  color: #fff;
  &:hover:not(:disabled) {
    background: darken($warning, 15%);
    box-shadow: 0 0 10px rgba($warning, 0.35);
  }
}

/* 蓝色：自检、手动禁止、自动禁止、喷洒禁止 */
.control-btn.btn-blue {
  background: darken($primary, 22%);
  border-color: darken($primary, 15%);
  color: #fff;
  &:hover:not(:disabled) {
    background: darken($primary, 15%);
    box-shadow: 0 0 10px rgba($primary, 0.35);
  }
}

.control-btn:disabled,
.control-btn.disabled {
  cursor: not-allowed;
}
</style>
