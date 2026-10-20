<template>
  <div class="analysis-grid">
    <!-- 值班摘要（固定本月口径，不受时间范围筛选影响） -->
    <ChartCard title="值班摘要" :icon="User" :loading="loading" :empty="statsEmpty" height="150px">
      <template #extra><span class="card-note">固定本月口径</span></template>
      <StatGrid :items="statItems" :cols="4" />
    </ChartCard>

    <!-- 值班人员排班排名（横向柱；排班天数 = 去重后的排班日期数） -->
    <ChartCard
      title="值班人员排班排名"
      :icon="Trophy"
      :loading="loading"
      :empty="isSeriesEmpty(userRank, (d) => d.dutyDays)"
      height="330px"
    >
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="rankEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { User, Trophy } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import StatGrid from './StatGrid.vue'
import { useChart, isSeriesEmpty, fmtInt, fmtRate, darkTooltip, darkAxis } from '../useChart'
import { getDutyStats, getDutyUserRank } from '@/api/eiot/firewatch/dataCenter.api'
// @deprecated 第二轮退场：mock 兜底为过渡期保留，逐 Tab 核对通过后随 mock.ts 一并删除
import { mockDutyStats, mockUserDutyRank } from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const { renderChart } = useChart()

// loading 仅用于首屏（刷新时不切 loading，保持图表容器 DOM 稳定，避免实例随 slot 重建）
const loading = ref(true)
const dutyStats = ref<any>({})
const userRank = ref<any[]>([])

const rankEl = ref<HTMLDivElement>()

const statItems = computed(() => {
  const s = dutyStats.value || {}
  return [
    { label: '本月值班人次', value: fmtInt(s.monthDutyCount), color: '#fff' },
    { label: '签到率', value: fmtRate(s.checkInRate), color: '#52c41a' },
    { label: '准时率', value: fmtRate(s.onTimeRate), color: '#1890ff' },
    { label: '交接班记录', value: fmtInt(s.handoverCount), color: '#4FC3F7' },
  ]
})

const statsEmpty = computed(() => !Number(dutyStats.value?.monthDutyCount) && !Number(dutyStats.value?.handoverCount))

const safe = async (fn: () => Promise<any>) => {
  try {
    return await fn()
  } catch {
    return null
  }
}

const renderCharts = () => {
  renderChart(rankEl.value, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...darkTooltip },
    grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, ...darkAxis },
    yAxis: {
      type: 'category',
      ...darkAxis,
      data: userRank.value.map((d) => d.userName || '未知人员').reverse(), // 反转让第一名在顶部
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: { color: '#4FC3F7', borderRadius: [0, 3, 3, 0] },
        data: userRank.value.map((d) => d.dutyDays || 0).reverse(),
      },
    ],
  })
}

const loadData = async () => {
  try {
    const now = new Date()
    const [s, r] = await Promise.all([
      safe(() => getDutyStats(props.propertyId)),
      safe(() => getDutyUserRank(now.getFullYear(), now.getMonth() + 1, props.propertyId)),
    ])
    dutyStats.value = s && (s.monthDutyCount || s.handoverCount) ? s : mockDutyStats
    userRank.value = r?.length ? r : mockUserDutyRank
  } finally {
    loading.value = false
    await nextTick()
    renderCharts()
  }
}

// 值班为固定本月口径，days 与数据无关，不触发重载
watch(() => props.propertyId, () => loadData())
onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.analysis-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.card-note {
  font-size: 11px;
  color: #8b9dc5;
}
</style>
