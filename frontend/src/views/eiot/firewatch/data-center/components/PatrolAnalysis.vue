<template>
  <div class="analysis-grid">
    <!-- 巡检统计摘要 -->
    <div class="stat-cards-row">
      <div class="stat-card">
        <div class="stat-num">{{ patrolStats.total || 0 }}</div>
        <div class="stat-label">总任务数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#52c41a">{{ patrolStats.completed || 0 }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#1890ff">{{ patrolStats.inProgress || 0 }}</div>
        <div class="stat-label">进行中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#ff4d4f">{{ patrolStats.overdue || 0 }}</div>
        <div class="stat-label">超期</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#4FC3F7">{{ patrolStats.completeRate ? patrolStats.completeRate + '%' : '--' }}</div>
        <div class="stat-label">完成率</div>
      </div>
    </div>

    <!-- 巡检任务趋势 -->
    <ChartCard title="巡检任务趋势" :icon="DataBoard" :loading="loading" :empty="!patrolTrend.length" height="360px">
      <div class="data-list">
        <div v-for="d in patrolTrend.slice(-14)" :key="d.date" class="data-row">
          <span class="data-label">{{ d.date?.substring(5) }}</span>
          <span class="data-val blue">创建 {{ d.createdCount || 0 }}</span>
          <span class="data-val green">完成 {{ d.completedCount || 0 }}</span>
        </div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { DataBoard } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import { getPatrolStats, getPatrolTrend } from '@/api/eiot/firewatch/dataCenter.api'
import { mockPatrolStats, mockPatrolTrend } from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const loading = ref(false)
const patrolStats = ref<any>({})
const patrolTrend = ref<any[]>([])

const safe = async (fn: () => Promise<any>) => {
  try { return await fn() } catch { return null }
}

const loadData = async () => {
  loading.value = true
  try {
    const [s, t] = await Promise.all([
      safe(() => getPatrolStats(props.propertyId)),
      safe(() => getPatrolTrend(props.days, props.propertyId)),
    ])
    patrolStats.value = (s && s.total) ? s : mockPatrolStats
    patrolTrend.value = (t && t.length) ? t : mockPatrolTrend
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
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.stat-card {
  background: $bg-card;
  border: 1px solid $bd;
  border-radius: 8px;
  padding: 20px 16px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0,0,0,0.18);

  .stat-num {
    font-size: 28px;
    font-weight: bold;
    color: #fff;
    margin-bottom: 6px;
  }
  .stat-label {
    font-size: 13px;
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
    &.blue { color: #1890ff; }
    &.green { color: #52c41a; }
  }
}
</style>
