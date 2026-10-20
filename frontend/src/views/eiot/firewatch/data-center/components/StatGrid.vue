<template>
  <div class="stat-grid" :style="{ gridTemplateColumns: `repeat(${cols}, 1fr)` }">
    <div v-for="item in items" :key="item.label" class="stat-item">
      <div class="stat-num" :style="{ color: item.color || '#fff' }">{{ item.value }}</div>
      <div class="stat-label">{{ item.label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    items: { label: string; value: string | number; color?: string }[]
    cols?: number
  }>(),
  { cols: 3 },
)
</script>

<style lang="scss" scoped>
.stat-grid {
  flex: 1;
  display: grid;
  gap: 10px 12px;
  align-content: center;
  min-width: 0;
  min-height: 0;
}

.stat-item {
  text-align: center;
  min-width: 0;

  .stat-num {
    font-size: 26px;
    font-weight: bold;
    color: #fff;
    line-height: 1.2;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .stat-label {
    font-size: 12px;
    color: #8b9dc5;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
