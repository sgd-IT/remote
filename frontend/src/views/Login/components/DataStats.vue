<template>
  <div class="data-stats">
    <div
      v-for="(item, idx) in stats"
      :key="idx"
      class="stat-card"
      :style="{ animationDelay: `${600 + idx * 100}ms` }"
    >
      <div class="stat-header">
        <Icon :icon="item.icon" :size="12" class="stat-icon" />
        <span class="stat-label">{{ item.label }}</span>
      </div>
      <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { Icon } from '@/components/Icon'

const stats = [
  { icon: 'ep:circle-check', label: '在线设备', value: '1,247', color: '#00e676' },
  { icon: 'ep:warning', label: '实时告警', value: '3', color: '#ff5252' },
  { icon: 'ep:video-camera', label: '视频监控', value: '86', color: '#4fc3f7' },
  { icon: 'ep:map-location', label: 'GIS覆盖', value: '42', color: '#4fc3f7' },
]
</script>

<style lang="scss" scoped>
.data-stats {
  display: flex;
  gap: 12px;
  width: 100%;
}
.stat-card {
  flex: 1;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(79, 195, 247, 0.08);
  border-radius: 8px;
  padding: 10px 12px;
  opacity: 0;
  transform: translateY(10px);
  animation: cardPopIn 0.5s ease-out forwards;
}
.stat-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 5px;
}
.stat-icon {
  font-size: 16px;
}
.stat-label {
  color: rgba(255, 255, 255, 0.35);
  font-size: 12px;
}
.stat-value {
  font-size: 20px;
  font-weight: 700;
}
@keyframes cardPopIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1279px) {
  .data-stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
}
</style>
