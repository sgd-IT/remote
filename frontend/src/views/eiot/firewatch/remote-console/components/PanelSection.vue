<template>
  <div class="panel-section">
    <div class="section-header">
      <span class="section-title">{{ title }}</span>
      <span class="edit-btn" @click="$emit('edit')">✎ 编辑</span>
    </div>
    <div class="card-grid">
      <DeviceCard
        v-for="(item, idx) in displayItems"
        :key="`${type}-${idx}`"
        :index="idx"
        :item="item"
        @action="emitAction(item, $event)"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { DeviceFunctionVO } from '@/api/eiot/remote/deviceRemote.api'
import DeviceCard from './DeviceCard.vue'

const props = defineProps<{
  title: string
  type: 'MULTI' | 'BUS'
  functions: DeviceFunctionVO[]
}>()

const emit = defineEmits<{
  (e: 'action', fn: DeviceFunctionVO, actionType: 'start' | 'stop'): void
  (e: 'edit'): void
}>()

const displayItems = computed(() => {
  const total = Math.max(20, props.functions.length)
  const arr: (DeviceFunctionVO | null)[] = new Array(total).fill(null)
  props.functions.forEach((fn, i) => { arr[i] = fn })
  return arr
})

const emitAction = (item: DeviceFunctionVO | null, actionType: 'start' | 'stop') => {
  if (item) emit('action', item, actionType)
}
</script>

<style lang="scss" scoped>
@import '../styles/common.scss';

.panel-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  /* 磨砂高亮边框 */
  background: rgba($bg-panel, 0.65);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba($primary, 0.25);
  border-radius: 8px;
  padding: 14px;
  box-shadow: 0 0 16px rgba($primary, 0.06), inset 0 0 10px rgba($primary, 0.03);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 500;
  color: $text-primary;
}

.edit-btn {
  font-size: 12px;
  color: $primary;
  cursor: pointer;
}

.card-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  overflow-y: auto;
  padding-right: 4px;
  align-content: flex-start;
  scrollbar-width: none;
  -ms-overflow-style: none;
  &::-webkit-scrollbar {
    display: none;
  }
}
</style>
