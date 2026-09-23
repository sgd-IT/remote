<template>
  <div class="device-card" :class="{ placeholder: !item, offline: item && !item.enabled }">
    <div class="card-name-bar">
      <span class="card-name">{{ item ? (item.buttonName || item.functionName) : '备用' }}</span>
    </div>
    <div class="status-row">
      <span class="status-tag"><i class="dot" :class="item && item.enabled ? 'on' : 'off'"></i>启动</span>
      <span class="status-tag"><i class="dot" :class="item && item.enabled ? 'on' : 'off'"></i>反馈</span>
      <span class="status-tag"><i class="dot off"></i>故障</span>
    </div>
    <div class="action-row">
      <button
        class="action-btn start"
        :class="{ placeholder: !item }"
        :disabled="!item"
        @click="$emit('action', 'start')"
      >启动</button>
      <button
        class="action-btn stop"
        :class="{ placeholder: !item }"
        :disabled="!item"
        @click="$emit('action', 'stop')"
      >停止</button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { DeviceFunctionVO } from '@/api/eiot/remote/deviceRemote.api'

defineProps<{
  index: number
  item: DeviceFunctionVO | null
}>()

defineEmits<{
  (e: 'action', type: 'start' | 'stop'): void
}>()
</script>

<style lang="scss" scoped>
@import '../styles/common.scss';
.device-card {
  min-width: 0;
  background: $bg-primary;
  border: 1px solid $border-color;
  border-radius: 6px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 100px;
}
/* 按钮名称：居中整行带背景 */
.card-name-bar {
  width: 100%;
  text-align: center;
  padding: 6px 8px;
  background: rgba($primary, 0.12);
  border-radius: 4px;
  border: 1px solid rgba($primary, 0.2);
}
.card-name {
  font-size: 13px;
  color: $text-primary;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status-row { display: flex; gap: 10px; }
.status-tag { display: flex; align-items: center; gap: 4px; font-size: 11px; color: $text-secondary; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: $text-muted; &.on { background: $success; box-shadow: 0 0 4px $success; } }
.action-row { display: flex; gap: 8px; margin-top: auto; }
.action-btn {
  flex: 1;
  height: 28px;
  border-radius: 4px;
  border: none;
  font-size: 12px;
  color: #fff;
  cursor: pointer;
  &.start { background: $primary; &:hover:not(:disabled) { background: lighten($primary, 8%); } }
  &.stop { background: $danger; &:hover:not(:disabled) { background: lighten($danger, 8%); } }
  &:disabled,
  &.placeholder {
    cursor: not-allowed;
  }
}
</style>
