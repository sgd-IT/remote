<template>
  <div class="analysis-grid">
    <!-- 值班统计摘要卡片 -->
    <div class="stat-cards-row">
      <div class="stat-card">
        <div class="stat-num">{{ dutyStats.monthDutyCount || 0 }}</div>
        <div class="stat-label">本月值班人次</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#52c41a">{{ dutyStats.checkInRate ? dutyStats.checkInRate + '%' : '--' }}</div>
        <div class="stat-label">签到率</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#1890ff">{{ dutyStats.onTimeRate ? dutyStats.onTimeRate + '%' : '--' }}</div>
        <div class="stat-label">准时率</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:#4FC3F7">{{ dutyStats.handoverCount || 0 }}</div>
        <div class="stat-label">交接班记录</div>
      </div>
    </div>

    <!-- 值班人员排班天数排名 -->
    <ChartCard title="值班人员排班排名" :icon="User" :loading="loading" :empty="!userRank.length" height="360px">
      <div class="data-list">
        <div v-for="(d, i) in userRank" :key="d.userName" class="data-row">
          <span class="data-label">{{ i + 1 }}. {{ d.userName }}</span>
          <span class="data-val">{{ d.dutyDays }} 天</span>
        </div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { User } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import { getDutyStats, getDutyUserRank } from '@/api/eiot/firewatch/dataCenter.api'
import { mockDutyStats, mockUserDutyRank } from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const loading = ref(false)
const dutyStats = ref<any>({})
const userRank = ref<any[]>([])

const safe = async (fn: () => Promise<any>) => {
  try { return await fn() } catch { return null }
}

const loadData = async () => {
  loading.value = true
  try {
    const now = new Date()
    const [s, r] = await Promise.all([
      safe(() => getDutyStats(props.propertyId)),
      safe(() => getDutyUserRank(now.getFullYear(), now.getMonth() + 1, props.propertyId)),
    ])
    dutyStats.value = (s && s.monthDutyCount) ? s : mockDutyStats
    userRank.value = (r && r.length) ? r : mockUserDutyRank
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
  grid-template-columns: repeat(4, 1fr);
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
    font-size: 32px;
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
  padding: 6px 0;
  border-bottom: 1px solid rgba(79,195,247,0.06);
  font-size: 13px;

  .data-label { color: #8b9dc5; }
  .data-val { color: #fff; font-weight: 500; }
}
</style>
