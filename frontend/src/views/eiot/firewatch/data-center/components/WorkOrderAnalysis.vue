<template>
  <div class="analysis-grid">
    <!-- 维修工单统计 -->
    <div class="stat-cards-row">
      <div class="stat-card">
        <div class="stat-num">{{ repairStats.total || 0 }}</div>
        <div class="stat-label">维修工单总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#fa8c16">{{ repairStats.pending || 0 }}</div>
        <div class="stat-label">待处理</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#1890ff">{{ repairStats.inProgress || 0 }}</div>
        <div class="stat-label">进行中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#52c41a">{{ repairStats.completed || 0 }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#ff4d4f">{{ repairStats.overdue || 0 }}</div>
        <div class="stat-label">超期</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#4FC3F7">{{ repairStats.avgHandleHours ? repairStats.avgHandleHours + 'h' : '--' }}</div>
        <div class="stat-label">平均处置时长</div>
      </div>
    </div>

    <!-- 隐患统计 -->
    <div class="stat-cards-row">
      <div class="stat-card">
        <div class="stat-num">{{ hazardStats.total || 0 }}</div>
        <div class="stat-label">隐患总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#fa8c16">{{ hazardStats.pending || 0 }}</div>
        <div class="stat-label">待整改</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#52c41a">{{ hazardStats.rectified || 0 }}</div>
        <div class="stat-label">已整改</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#8c8c8c">{{ hazardStats.closed || 0 }}</div>
        <div class="stat-label">已关闭</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#4FC3F7">{{ hazardStats.rectificationRate ? hazardStats.rectificationRate + '%' : '--' }}</div>
        <div class="stat-label">整改率</div>
      </div>
    </div>

    <!-- 工单趋势 -->
    <ChartCard title="工单趋势（维修+隐患）" :icon="Stamp" :loading="loading" :empty="!workOrderTrend.length" height="360px">
      <div class="data-list">
        <div v-for="d in workOrderTrend.slice(-14)" :key="d.date" class="data-row">
          <span class="data-label">{{ d.date?.substring(5) }}</span>
          <span class="data-val orange">创建 {{ d.createdCount || 0 }}</span>
          <span class="data-val green">完成 {{ d.completedCount || 0 }}</span>
        </div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Stamp } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import { getRepairStats, getHazardStats, getWorkOrderTrend } from '@/api/eiot/firewatch/dataCenter.api'
import { mockRepairStats, mockHazardStats, mockWorkOrderTrend } from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const loading = ref(false)
const repairStats = ref<any>({})
const hazardStats = ref<any>({})
const workOrderTrend = ref<any[]>([])

const safe = async (fn: () => Promise<any>) => {
  try { return await fn() } catch { return null }
}

const loadData = async () => {
  loading.value = true
  try {
    const [r, h, t] = await Promise.all([
      safe(() => getRepairStats(props.propertyId)),
      safe(() => getHazardStats(props.propertyId)),
      safe(() => getWorkOrderTrend(props.days, props.propertyId)),
    ])
    repairStats.value = (r && r.total) ? r : mockRepairStats
    hazardStats.value = (h && h.total) ? h : mockHazardStats
    workOrderTrend.value = (t && t.length) ? t : mockWorkOrderTrend
  } finally {
    loading.value = false
  }
}

watch(() => [props.propertyId, props.days], () => loadData())
onMounted(() => loadData())
</script>

<style lang="scss" scoped>
$bg-card: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
$bd: rgba(79,195,247,0.1);

.analysis-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.stat-cards-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.stat-card {
  background: $bg-card;
  border: 1px solid $bd;
  border-radius: 8px;
  padding: 18px 14px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0,0,0,0.18);

  .stat-num {
    font-size: 26px;
    font-weight: bold;
    color: #fff;
    margin-bottom: 4px;
  }
  .stat-label {
    font-size: 12px;
    color: #8b9dc5;
  }
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
  padding: 5px 0;
  border-bottom: 1px solid rgba(79,195,247,0.06);
  font-size: 12px;

  .data-label { color: #8b9dc5; }
  .data-val {
    font-weight: 500;
    &.orange { color: #fa8c16; }
    &.green { color: #52c41a; }
  }
}
</style>
