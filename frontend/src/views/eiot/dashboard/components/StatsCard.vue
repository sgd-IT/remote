<template>
  <div class="stats-card" :class="{ 'is-loading': loading, 'is-error': hasError }">
    <el-skeleton :loading="loading" animated>
      <div class="stats-card-content">
        <div class="stats-card-icon" :style="{ backgroundColor: iconBgColor }">
          <Icon :icon="icon" :size="28" :color="iconColor" />
        </div>
        <div class="stats-card-info">
          <div class="stats-card-label">{{ label }}</div>
          <div class="stats-card-value">
            <CountTo
              v-if="!hasError"
              :start-val="0"
              :end-val="value"
              :duration="2000"
              :decimals="decimals"
            />
            <span v-else class="error-value">--</span>
          </div>
        </div>
        <div v-if="showTrend && trend !== undefined" class="stats-card-trend">
          <Icon
            :icon="trend >= 0 ? 'ep:caret-top' : 'ep:caret-bottom'"
            :color="trend >= 0 ? '#67c23a' : '#f56c6c'"
          />
          <span :class="trend >= 0 ? 'trend-up' : 'trend-down'">
            {{ Math.abs(trend) }}%
          </span>
        </div>
      </div>
    </el-skeleton>
    <div v-if="hasError && !loading" class="stats-card-error">
      <el-button size="small" @click="handleRetry">
        <Icon icon="ep:refresh" />
        {{ $t('common.retry') }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { propTypes } from '@/utils/propTypes'
import { ref, computed } from 'vue'

defineOptions({ name: 'StatsCard' })

const props = defineProps({
  label: propTypes.string.def(''),
  value: propTypes.number.def(0),
  icon: propTypes.string.def('ep:data-line'),
  iconColor: propTypes.string.def('#409eff'),
  iconBgColor: propTypes.string.def('#ecf5ff'),
  decimals: propTypes.number.def(0),
  loading: propTypes.bool.def(false),
  error: propTypes.bool.def(false),
  showTrend: propTypes.bool.def(false),
  trend: propTypes.number.def(undefined)
})

const emit = defineEmits(['retry'])

const hasError = computed(() => props.error)

const handleRetry = () => {
  emit('retry')
}
</script>

<style scoped lang="scss">
.stats-card {
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 20px;
  transition: all 0.3s;
  border: 1px solid var(--el-border-color-lighter);

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  &.is-error {
    border-color: #f56c6c;
  }
}

.stats-card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stats-card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stats-card-info {
  flex: 1;
  min-width: 0;
}

.stats-card-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.stats-card-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.2;

  .error-value {
    color: #f56c6c;
  }
}

.stats-card-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  margin-left: 12px;

  .trend-up {
    color: #67c23a;
  }

  .trend-down {
    color: #f56c6c;
  }
}

.stats-card-error {
  margin-top: 12px;
  text-align: center;
}
</style>
