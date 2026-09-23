<template>
  <div class="chart-card" :class="{ 'is-loading': loading }">
    <div class="chart-card__header">
      <span class="chart-card__title">
        <el-icon v-if="icon"><component :is="icon" /></el-icon>
        {{ title }}
      </span>
      <slot name="extra"></slot>
    </div>
    <div class="chart-card__body" :style="{ height: height }">
      <div v-if="loading" class="chart-card__loading">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div v-else-if="empty" class="chart-card__empty">
        <el-icon :size="28" color="rgba(79,195,247,0.3)"><TrendCharts /></el-icon>
        <span>暂无数据</span>
      </div>
      <slot v-else></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Loading, TrendCharts } from '@element-plus/icons-vue'

defineProps<{
  title: string
  icon?: any
  height?: string
  loading?: boolean
  empty?: boolean
}>()
</script>

<style lang="scss" scoped>
$bg-card: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
$bd: rgba(79,195,247,0.1);
$blue: #1890ff;
$cyan: rgba(79,195,247,0.9);
$t2: #8b9dc5;

.chart-card {
  background: $bg-card;
  border: 1px solid $bd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  box-shadow: 0 2px 10px rgba(0,0,0,0.18), 0 0 1px rgba(79,195,247,0.08), inset 0 1px 0 rgba(79,195,247,0.04);

  &::before {
    content: '';
    position: absolute;
    top: -1px; left: -1px;
    width: 20px; height: 20px;
    border-top: 2px solid rgba(79,195,247,0.4);
    border-left: 2px solid rgba(79,195,247,0.4);
    border-radius: 8px 0 0 0;
    z-index: 1;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: -1px; right: -1px;
    width: 20px; height: 20px;
    border-bottom: 2px solid rgba(79,195,247,0.4);
    border-right: 2px solid rgba(79,195,247,0.4);
    border-radius: 0 0 8px 0;
    z-index: 1;
  }

  &__header {
    padding: 12px 16px;
    border-bottom: 1px solid rgba(79,195,247,0.12);
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: relative;
    flex-shrink: 0;

    &::before {
      content: '';
      position: absolute;
      left: 0; top: 20%; bottom: 20%;
      width: 3px;
      background: $cyan;
      border-radius: 0 2px 2px 0;
      box-shadow: 0 0 6px rgba(79,195,247,0.5);
    }
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    font-weight: bold;
    color: #fff;
    letter-spacing: 0.5px;

    .el-icon {
      color: $cyan;
      font-size: 15px;
    }
  }

  &__body {
    flex: 1;
    padding: 12px 16px;
    position: relative;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }

  &__loading,
  &__empty {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: $t2;
    font-size: 13px;
  }
}
</style>
