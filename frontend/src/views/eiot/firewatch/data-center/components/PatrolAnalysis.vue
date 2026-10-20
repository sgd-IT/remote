<template>
  <div class="analysis-grid">
    <!-- 巡检摘要（数字卡组，统计范围=时间筛选的近 N 天） -->
    <ChartCard title="巡检摘要" :icon="DataBoard" :loading="loading" :empty="statsEmpty" height="150px">
      <template #extra><span class="card-note">超期=截至当前仍未完成且已过期限</span></template>
      <StatGrid :items="statItems" :cols="3" />
    </ChartCard>

    <!-- 巡检任务趋势（创建/完成双折线；plan_date 为空的"每天"重复任务不计入） -->
    <ChartCard
      title="巡检任务趋势"
      :icon="TrendCharts"
      :loading="loading"
      :empty="isSeriesEmpty(trend, (d) => (d.createdCount || 0) + (d.completedCount || 0))"
      height="330px"
    >
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="trendEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { DataBoard, TrendCharts } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import StatGrid from './StatGrid.vue'
import { useChart, isSeriesEmpty, fmtInt, fmtRate, darkTooltip, darkAxis } from '../useChart'
import { getPatrolStats, getPatrolTrend } from '@/api/eiot/firewatch/dataCenter.api'
// @deprecated 第二轮退场：mock 兜底为过渡期保留，逐 Tab 核对通过后随 mock.ts 一并删除
import { mockPatrolStats, mockPatrolTrend } from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const { renderChart } = useChart()

// loading 仅用于首屏（刷新时不切 loading，保持图表容器 DOM 稳定，避免实例随 slot 重建）
const loading = ref(true)
const patrolStats = ref<any>({})
const trend = ref<any[]>([])

const trendEl = ref<HTMLDivElement>()

const statItems = computed(() => {
  const s = patrolStats.value || {}
  return [
    { label: '总任务', value: fmtInt(s.total), color: '#fff' },
    { label: '已完成', value: fmtInt(s.completed), color: '#52c41a' },
    { label: '进行中', value: fmtInt(s.inProgress), color: '#1890ff' },
    { label: '超期', value: fmtInt(s.overdue), color: '#ff4d4f' },
    { label: '完成率', value: fmtRate(s.completeRate), color: '#4FC3F7' },
  ]
})

const statsEmpty = computed(() => !Number(patrolStats.value?.total))

const safe = async (fn: () => Promise<any>) => {
  try {
    return await fn()
  } catch {
    return null
  }
}

const renderCharts = () => {
  renderChart(trendEl.value, {
    tooltip: { trigger: 'axis', ...darkTooltip },
    legend: {
      data: ['创建', '完成'],
      top: 0,
      right: 0,
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: '#8b9dc5', fontSize: 11 },
    },
    grid: { left: 40, right: 12, top: 28, bottom: 24 },
    xAxis: {
      type: 'category',
      data: trend.value.map((d) => String(d.date || '').substring(5)),
      ...darkAxis,
      axisLabel: { ...darkAxis.axisLabel, hideOverlap: true },
    },
    yAxis: { type: 'value', minInterval: 1, ...darkAxis },
    series: [
      {
        name: '创建',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        itemStyle: { color: '#4FC3F7' },
        lineStyle: { width: 2 },
        areaStyle: { color: 'rgba(79,195,247,0.12)' },
        data: trend.value.map((d) => d.createdCount || 0),
      },
      {
        name: '完成',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        itemStyle: { color: '#52c41a' },
        lineStyle: { width: 2 },
        areaStyle: { color: 'rgba(82,196,26,0.10)' },
        data: trend.value.map((d) => d.completedCount || 0),
      },
    ],
  })
}

const loadData = async () => {
  try {
    const [s, t] = await Promise.all([
      safe(() => getPatrolStats(props.days, props.propertyId)),
      safe(() => getPatrolTrend(props.days, props.propertyId)),
    ])
    patrolStats.value = s && s.total ? s : mockPatrolStats
    trend.value = t?.length ? t : mockPatrolTrend
  } finally {
    loading.value = false
    await nextTick()
    renderCharts()
  }
}

watch(() => [props.propertyId, props.days], () => loadData())
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
