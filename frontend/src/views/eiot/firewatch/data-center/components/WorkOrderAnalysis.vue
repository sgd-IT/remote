<template>
  <div class="analysis-grid">
    <!-- 维修摘要（数字卡组；当前状态快照，超期为当前时点未完成且已过截止时间） -->
    <ChartCard title="维修摘要" :icon="Tools" :loading="loading" :empty="repairEmpty" height="160px">
      <template #extra><span class="card-note">当前状态快照</span></template>
      <StatGrid :items="repairItems" :cols="3" />
    </ChartCard>

    <!-- 隐患摘要（数字卡组；统计范围=时间筛选的近 N 天） -->
    <ChartCard title="隐患摘要" :icon="WarningFilled" :loading="loading" :empty="hazardEmpty" height="160px">
      <template #extra><span class="card-note">整改率=(已整改+已闭环)/(总数-已取消)</span></template>
      <StatGrid :items="hazardItems" :cols="3" />
    </ChartCard>

    <!-- 处置事项趋势（口径：创建=维修新建+隐患新增，完成=维修完工+隐患闭环；非纯工单数） -->
    <ChartCard
      title="处置事项趋势"
      :icon="TrendCharts"
      :loading="loading"
      :empty="isSeriesEmpty(trend, (d) => (d.createdCount || 0) + (d.completedCount || 0))"
      height="300px"
    >
      <template #extra><span class="card-note">口径：维修工单 + 隐患合计</span></template>
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="trendEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { Tools, WarningFilled, TrendCharts } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import StatGrid from './StatGrid.vue'
import { useChart, isSeriesEmpty, fmtInt, fmtRate, darkTooltip, darkAxis } from '../useChart'
import { getRepairStats, getHazardStats, getWorkOrderTrend } from '@/api/eiot/firewatch/dataCenter.api'
// @deprecated 第二轮退场：mock 兜底为过渡期保留，逐 Tab 核对通过后随 mock.ts 一并删除
import { mockRepairStats, mockHazardStats, mockWorkOrderTrend } from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const { renderChart } = useChart()

// loading 仅用于首屏（刷新时不切 loading，保持图表容器 DOM 稳定，避免实例随 slot 重建）
const loading = ref(true)
const repairStats = ref<any>({})
const hazardStats = ref<any>({})
const trend = ref<any[]>([])

const trendEl = ref<HTMLDivElement>()

const repairItems = computed(() => {
  const s = repairStats.value || {}
  return [
    { label: '工单总数', value: fmtInt(s.total), color: '#fff' },
    { label: '待处理', value: fmtInt(s.pending), color: '#faad14' },
    { label: '进行中', value: fmtInt(s.inProgress), color: '#1890ff' },
    { label: '已完成', value: fmtInt(s.completed), color: '#52c41a' },
    { label: '超期', value: fmtInt(s.overdue), color: '#ff4d4f' },
  ]
})

const hazardItems = computed(() => {
  const s = hazardStats.value || {}
  return [
    { label: '隐患总数', value: fmtInt(s.total), color: '#fff' },
    { label: '待整改', value: fmtInt(s.pending), color: '#faad14' },
    { label: '已整改', value: fmtInt(s.rectified), color: '#1890ff' },
    { label: '已闭环', value: fmtInt(s.closed), color: '#52c41a' },
    { label: '超期未整改', value: fmtInt(s.hazardOverdue), color: '#ff4d4f' },
    { label: '整改率', value: fmtRate(s.rectificationRate), color: '#4FC3F7' },
  ]
})

const repairEmpty = computed(() => !Number(repairStats.value?.total) && !Number(repairStats.value?.overdue))
const hazardEmpty = computed(() => !Number(hazardStats.value?.total))

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
      data: ['新增（维修+隐患）', '完成（维修+隐患）'],
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
        name: '新增（维修+隐患）',
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
        name: '完成（维修+隐患）',
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
    const [r, h, t] = await Promise.all([
      safe(() => getRepairStats(props.propertyId)),
      safe(() => getHazardStats(props.days, props.propertyId)),
      safe(() => getWorkOrderTrend(props.days, props.propertyId)),
    ])
    repairStats.value = r && r.total ? r : mockRepairStats
    hazardStats.value = h && h.total ? h : mockHazardStats
    trend.value = t?.length ? t : mockWorkOrderTrend
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
