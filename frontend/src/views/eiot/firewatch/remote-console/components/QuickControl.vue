<template>
  <div class="button-grid-container">
    <div v-if="generalFunctions.length > 0" class="button-grid">
      <button
        v-for="fn in generalFunctions"
        :key="fn.id"
        class="grid-btn"
        :class="getBtnClass(fn)"
        @click="handleClick(fn)"
      >
        <span class="btn-icon">{{ getBtnIcon(fn) }}</span>
        <span class="btn-text">{{ fn.buttonName || fn.functionName }}</span>
      </button>
    </div>
    <div v-else class="empty-state">
      <div class="empty-icon">📦</div>
      <div class="empty-text">暂无通用功能</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { DeviceFunctionVO } from '@/api/eiot/remote/deviceRemote.api'

const props = defineProps<{
  functions: DeviceFunctionVO[]
}>()

const emit = defineEmits<{
  (e: 'action', fn: DeviceFunctionVO): void
}>()

const generalFunctions = computed(() => {
  return props.functions.filter(f => !f.functionType || f.functionType === 'GENERAL')
})

const getBtnClass = (fn: DeviceFunctionVO) => {
  const name = (fn.buttonName || fn.functionName || '').toLowerCase()
  if (name.includes('消音') || name.includes('静音')) return 'grid-btn-mute'
  if (name.includes('复位') || name.includes('重置')) return 'grid-btn-reset'
  if (name.includes('启动') || name.includes('开始') || name.includes('开')) return 'grid-btn-start'
  if (name.includes('停止') || name.includes('关闭') || name.includes('关')) return 'grid-btn-stop'
  return 'grid-btn-default'
}

const getBtnIcon = (fn: DeviceFunctionVO) => {
  const name = (fn.buttonName || fn.functionName || '').toLowerCase()
  if (name.includes('消音') || name.includes('静音')) return '🔇'
  if (name.includes('复位') || name.includes('重置')) return '↻'
  if (name.includes('启动') || name.includes('开始') || name.includes('开')) return '▶'
  if (name.includes('停止') || name.includes('关闭') || name.includes('关')) return '■'
  return '⚡'
}

const handleClick = (fn: DeviceFunctionVO) => {
  emit('action', fn)
}
</script>

<style lang="scss" scoped>
.button-grid-container {
  margin-left: 12px;
  display: flex;
  flex-direction: column;
}

.button-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.grid-btn {
  width: 60px;
  height: 60px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.3s;
  color: #fff;
}

.grid-btn .btn-icon {
  font-size: 20px;
}

.grid-btn .btn-text {
  font-size: 11px;
}

.grid-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.grid-btn-reset {
  background: linear-gradient(135deg, #1a3a5c 0%, #0f2744 100%);
  border: 1px solid rgba(0, 180, 255, 0.3);
  box-shadow: 0 0 8px rgba(0, 180, 255, 0.1);
}

.grid-btn-reset:not(.disabled):hover {
  background: linear-gradient(135deg, #1e4a70 0%, #153050 100%);
  border-color: rgba(0, 180, 255, 0.5);
  box-shadow: 0 0 15px rgba(0, 180, 255, 0.25);
}

.grid-btn-reset:not(.disabled):active {
  background: linear-gradient(135deg, #2980b9 0%, #1a5276 100%);
  border-color: #00b4ff;
  box-shadow: 0 0 20px rgba(0, 180, 255, 0.5);
}

.grid-btn-mute {
  background: linear-gradient(135deg, #8b1a1a 0%, #5c1212 100%);
  border: 1px solid rgba(204, 51, 51, 0.3);
  box-shadow: 0 0 8px rgba(204, 51, 51, 0.1);
}

.grid-btn-mute:not(.disabled):hover {
  background: linear-gradient(135deg, #a52020 0%, #6b1515 100%);
  border-color: rgba(204, 51, 51, 0.5);
  box-shadow: 0 0 15px rgba(204, 51, 51, 0.25);
}

.grid-btn-mute:not(.disabled):active {
  background: linear-gradient(135deg, #cc3333 0%, #991f1f 100%);
  border-color: #e74c3c;
  box-shadow: 0 0 20px rgba(231, 76, 60, 0.5);
}

.grid-btn-start {
  background: linear-gradient(135deg, #1a6b4a 0%, #0f4a30 100%);
  border: 1px solid rgba(46, 204, 113, 0.3);
  box-shadow: 0 0 8px rgba(46, 204, 113, 0.1);
}

.grid-btn-start:not(.disabled):hover {
  background: linear-gradient(135deg, #208b5a 0%, #156040 100%);
  border-color: rgba(46, 204, 113, 0.5);
  box-shadow: 0 0 15px rgba(46, 204, 113, 0.25);
}

.grid-btn-start:not(.disabled):active {
  background: linear-gradient(135deg, #27ae60 0%, #1e8449 100%);
  border-color: #2ecc71;
  box-shadow: 0 0 20px rgba(46, 204, 113, 0.5);
}

.grid-btn-stop {
  background: linear-gradient(135deg, #8b5a1a 0%, #5c3d12 100%);
  border: 1px solid rgba(230, 126, 34, 0.3);
  box-shadow: 0 0 8px rgba(230, 126, 34, 0.1);
}

.grid-btn-stop:not(.disabled):hover {
  background: linear-gradient(135deg, #a06a20 0%, #6b4515 100%);
  border-color: rgba(230, 126, 34, 0.5);
  box-shadow: 0 0 15px rgba(230, 126, 34, 0.25);
}

.grid-btn-stop:not(.disabled):active {
  background: linear-gradient(135deg, #e67e22 0%, #ca6f1e 100%);
  border-color: #f39c12;
  box-shadow: 0 0 20px rgba(230, 126, 34, 0.5);
}

.grid-btn-default {
  background: linear-gradient(135deg, #2c3e50 0%, #1a252f 100%);
  border: 1px solid rgba(0, 180, 255, 0.3);
  box-shadow: 0 0 8px rgba(0, 180, 255, 0.1);
}

.grid-btn-default:hover {
  background: linear-gradient(135deg, #34495e 0%, #22303d 100%);
  border-color: rgba(0, 180, 255, 0.5);
  box-shadow: 0 0 15px rgba(0, 180, 255, 0.25);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
  opacity: 0.5;
}

.empty-text {
  color: #8ba3c7;
  font-size: 13px;
}
</style>
