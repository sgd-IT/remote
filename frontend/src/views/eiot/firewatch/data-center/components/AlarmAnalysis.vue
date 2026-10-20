<template>
  <div class="analysis-grid">
    <!-- 告警趋势（告警/处置双线） -->
    <ChartCard
      title="告警趋势"
      :icon="TrendCharts"
      :loading="loading"
      :empty="isSeriesEmpty(alarmTrend, (d) => (d.alarmCount || 0) + (d.handleCount || 0))"
      height="280px"
    >
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="trendEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>

    <!-- 告警类型分布（环形饼，alarm_kind 双读口径） -->
    <ChartCard
      title="告警类型分布"
      :icon="DataLine"
      :loading="loading"
      :empty="isSeriesEmpty(alarmTypes)"
      height="280px"
    >
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="typeEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>

    <!-- 24h 告警时段分布（柱状，0-23 时由后端补零） -->
    <ChartCard
      title="24h 告警时段分布"
      :icon="Clock"
      :loading="loading"
      :empty="isSeriesEmpty(hourly)"
      height="280px"
    >
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="hourlyEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>

    <!-- 各物业告警排名（横向柱 TOP5；数据按联网单位聚合） -->
    <ChartCard
      title="物业告警排名 TOP5"
      :icon="Rank"
      :loading="loading"
      :empty="isSeriesEmpty(propertyRank, (d) => d.alarmCount)"
      height="280px"
    >
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="rankEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>

    <!-- 档位分布（环形饼；tier 仅一级/火警事件有值） -->
    <ChartCard title="档位分布" :icon="SetUp" :loading="loading" :empty="isSeriesEmpty(alarmLevels)" height="280px">
      <template #extra><span class="card-note">仅一级（火警）事件有档位</span></template>
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="levelEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { TrendCharts, DataLine, SetUp, Rank, Clock } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import { useChart, isSeriesEmpty, darkTooltip, darkAxis, CHART_COLORS } from '../useChart'
import {
  getAlarmTrend,
  getAlarmTypeDistribution,
  getAlarmPropertyRank,
  getAlarmHourlyDistribution,
  getAlarmLevelDistribution,
} from '@/api/eiot/firewatch/dataCenter.api'
// @deprecated 第二轮退场：mock 兜底为过渡期保留，逐 Tab 核对通过后随 mock.ts 一并删除
import {
  mockAlarmTrend,
  mockAlarmTypeDistribution,
  mockAlarmLevelDistribution,
  mockPropertyAlarmRank,
  mockAlarmHourlyDistribution,
} from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const { renderChart } = useChart()

// loading 仅用于首屏（刷新时不切 loading，保持图表容器 DOM 稳定，避免实例随 slot 重建）
const loading = ref(true)
const alarmTrend = ref<any[]>([])
const alarmTypes = ref<any[]>([])
const hourly = ref<any[]>([])
const propertyRank = ref<any[]>([])
const alarmLevels = ref<any[]>([])

const trendEl = ref<HTMLDivElement>()
const typeEl = ref<HTMLDivElement>()
const hourlyEl = ref<HTMLDivElement>()
const rankEl = ref<HTMLDivElement>()
const levelEl = ref<HTMLDivElement>()

const safe = async (fn: () => Promise<any>) => {
  try {
    return await fn()
  } catch {
    return null
  }
}

/** 环形饼（类型分布/档位分布共用） */
const pieOption = (data: { name: string; value: number }[]) => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', ...darkTooltip },
  legend: {
    type: 'scroll',
    orient: 'vertical',
    right: 0,
    top: 'middle',
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#8b9dc5', fontSize: 11 },
  },
  color: CHART_COLORS,
  series: [
    {
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['38%', '50%'],
      label: { show: false },
      labelLine: { show: false },
      itemStyle: { borderColor: '#0a1628', borderWidth: 2, borderRadius: 4 },
      emphasis: { label: { show: true, color: '#fff', fontSize: 13, fontWeight: 'bold', formatter: '{b}\n{c}' } },
      data,
    },
  ],
})

const renderCharts = () => {
  renderChart(trendEl.value, {
    tooltip: { trigger: 'axis', ...darkTooltip },
    legend: { data: ['告警', '处置'], top: 0, right: 0, itemWidth: 12, itemHeight: 8, textStyle: { color: '#8b9dc5', fontSize: 11 } },
    grid: { left: 40, right: 12, top: 28, bottom: 24 },
    xAxis: {
      type: 'category',
      data: alarmTrend.value.map((d) => String(d.date || '').substring(5)),
      ...darkAxis,
      axisLabel: { ...darkAxis.axisLabel, hideOverlap: true },
    },
    yAxis: { type: 'value', minInterval: 1, ...darkAxis },
    series: [
      {
        name: '告警',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        itemStyle: { color: '#ff4d4f' },
        lineStyle: { width: 2 },
        areaStyle: { color: 'rgba(255,77,79,0.12)' },
        data: alarmTrend.value.map((d) => d.alarmCount || 0),
      },
      {
        name: '处置',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        itemStyle: { color: '#52c41a' },
        lineStyle: { width: 2 },
        areaStyle: { color: 'rgba(82,196,26,0.10)' },
        data: alarmTrend.value.map((d) => d.handleCount || 0),
      },
    ],
  })

  renderChart(
    typeEl.value,
    pieOption(alarmTypes.value.map((d) => ({ name: d.typeName || '未知', value: d.count || 0 }))),
  )

  renderChart(hourlyEl.value, {
    tooltip: { trigger: 'axis', ...darkTooltip },
    grid: { left: 36, right: 8, top: 16, bottom: 20 },
    xAxis: {
      type: 'category',
      data: hourly.value.map((d) => `${d.hour}时`),
      ...darkAxis,
      axisLabel: { ...darkAxis.axisLabel, hideOverlap: true },
    },
    yAxis: { type: 'value', minInterval: 1, ...darkAxis },
    series: [
      { type: 'bar', barMaxWidth: 14, itemStyle: { color: '#4FC3F7', borderRadius: [3, 3, 0, 0] }, data: hourly.value.map((d) => d.count || 0) },
    ],
  })

  renderChart(rankEl.value, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...darkTooltip },
    grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, ...darkAxis },
    yAxis: {
      type: 'category',
      ...darkAxis,
      data: propertyRank.value.map((d) => d.propertyName || '未知单位').reverse(), // 反转让第一名在顶部
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: { color: '#ff4d4f', borderRadius: [0, 3, 3, 0] },
        data: propertyRank.value.map((d) => d.alarmCount || 0).reverse(),
      },
    ],
  })

  renderChart(
    levelEl.value,
    pieOption(alarmLevels.value.map((d) => ({ name: d.levelName || d.level || '未知', value: d.count || 0 }))),
  )
}

const loadData = async () => {
  try {
    const [t, tp, r, h, l] = await Promise.all([
      safe(() => getAlarmTrend(props.days, props.propertyId)),
      safe(() => getAlarmTypeDistribution(props.days, props.propertyId)),
      safe(() => getAlarmPropertyRank(props.days)),
      safe(() => getAlarmHourlyDistribution(props.days, props.propertyId)),
      safe(() => getAlarmLevelDistribution(props.days, props.propertyId)),
    ])
    alarmTrend.value = t?.length ? t : mockAlarmTrend
    alarmTypes.value = tp?.length ? tp : mockAlarmTypeDistribution
    propertyRank.value = r?.length ? r : mockPropertyAlarmRank
    hourly.value = h?.length ? h : mockAlarmHourlyDistribution
    alarmLevels.value = l?.length ? l : mockAlarmLevelDistribution
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
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}

.card-note {
  font-size: 11px;
  color: #8b9dc5;
}
</style>
