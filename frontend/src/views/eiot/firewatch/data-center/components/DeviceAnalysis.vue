<template>
  <div class="analysis-grid">
    <!-- 设备状态总览 -->
    <ChartCard title="设备状态总览" :icon="Odometer" :loading="loading" :empty="!hasDeviceData" height="300px">
      <div class="status-grid">
        <div class="status-item"><span class="status-num">{{ deviceStatus.total || 0 }}</span><span class="status-label">总数</span></div>
        <div class="status-item green"><span class="status-num">{{ deviceStatus.normal || 0 }}</span><span class="status-label">正常</span></div>
        <div class="status-item red"><span class="status-num">{{ deviceStatus.alarm || 0 }}</span><span class="status-label">告警</span></div>
        <div class="status-item orange"><span class="status-num">{{ deviceStatus.fault || 0 }}</span><span class="status-label">故障</span></div>
        <div class="status-item gray"><span class="status-num">{{ deviceStatus.offline || 0 }}</span><span class="status-label">离线</span></div>
      </div>
    </ChartCard>

    <!-- 设备类型分布 -->
    <ChartCard title="设备类型分布" :icon="Coin" :loading="loading" :empty="!categories.length" height="300px">
      <div class="data-list">
        <div v-for="d in categories" :key="d.categoryName" class="data-row">
          <span class="data-label">{{ d.categoryName }}</span>
          <span class="data-val">{{ d.count }}</span>
        </div>
      </div>
    </ChartCard>

    <!-- 各物业设备数量 -->
    <ChartCard title="各物业设备数量" :icon="Tickets" :loading="loading" :empty="!propDevices.length" height="300px">
      <div class="data-list">
        <div v-for="(d, i) in propDevices" :key="d.propertyName" class="data-row">
          <span class="data-label">{{ i + 1 }}. {{ d.propertyName }}</span>
          <span class="data-val">{{ d.deviceCount }}</span>
        </div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Odometer, Coin, Tickets } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import { getDeviceStatus, getDeviceCategory, getDevicePropertyDistribution } from '@/api/eiot/firewatch/dataCenter.api'
import { mockDeviceStatus, mockDeviceCategoryDistribution, mockPropertyDeviceDistribution } from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const loading = ref(false)
const deviceStatus = ref<any>({})
const categories = ref<any[]>([])
const propDevices = ref<any[]>([])

const hasDeviceData = computed(() => {
  const s = deviceStatus.value
  return s && (s.total || s.normal || s.alarm || s.fault || s.offline || s.inactive)
})

const safe = async (fn: () => Promise<any>) => {
  try { return await fn() } catch { return null }
}

const loadData = async () => {
  loading.value = true
  try {
    const [s, c, p] = await Promise.all([
      safe(() => getDeviceStatus(props.propertyId)),
      safe(() => getDeviceCategory(props.propertyId)),
      safe(() => getDevicePropertyDistribution()),
    ])
    deviceStatus.value = (s && s.total) ? s : mockDeviceStatus
    categories.value = (c && c.length) ? c : mockDeviceCategoryDistribution
    propDevices.value = (p && p.length) ? p : mockPropertyDeviceDistribution
  } finally {
    loading.value = false
  }
}

watch(() => [props.propertyId, props.days], () => loadData())
onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.analysis-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}

.status-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  align-items: center;
  flex: 1;
}

.status-item {
  text-align: center;
  min-width: 60px;
  .status-num { display: block; font-size: 24px; font-weight: bold; color: #fff; }
  .status-label { font-size: 11px; color: #8b9dc5; }
  &.green .status-num { color: #52c41a; }
  &.red .status-num { color: #ff4d4f; }
  &.orange .status-num { color: #fa8c16; }
  &.gray .status-num { color: #8c8c8c; }
}

.data-list {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

.data-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
  border-bottom: 1px solid rgba(79,195,247,0.06);
  font-size: 12px;

  .data-label {
    color: #8b9dc5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 55%;
  }
  .data-val {
    color: #fff;
    font-weight: 500;
  }
}
</style>
