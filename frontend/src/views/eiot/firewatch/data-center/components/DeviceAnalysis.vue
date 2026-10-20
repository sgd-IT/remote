<template>
  <div class="analysis-grid">
    <!-- 设备状态总览（数字卡组；快照口径，仅告警设备数受时间范围影响） -->
    <ChartCard title="设备状态总览" :icon="Odometer" :loading="loading" :empty="!statusTotal" height="300px">
      <template #extra><span class="card-note">状态为当前快照；告警设备为近 N 天去重</span></template>
      <StatGrid :items="statusItems" :cols="3" />
    </ChartCard>

    <!-- 设备类型分布（环形饼） -->
    <ChartCard title="设备类型分布" :icon="Coin" :loading="loading" :empty="isSeriesEmpty(categories)" height="300px">
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="categoryEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>

    <!-- 各物业设备数量（横向柱；数据按联网单位聚合） -->
    <ChartCard
      title="各物业设备数量"
      :icon="Tickets"
      :loading="loading"
      :empty="isSeriesEmpty(propDevices, (d) => d.deviceCount)"
      height="300px"
    >
      <template #extra><span class="card-note">按联网单位聚合</span></template>
      <div :style="{ flex: 1, minWidth: 0, minHeight: 0 }">
        <div ref="propertyEl" :style="{ width: '100%', height: '100%' }"></div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { Odometer, Coin, Tickets } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import StatGrid from './StatGrid.vue'
import { useChart, isSeriesEmpty, fmtInt, darkTooltip, darkAxis, CHART_COLORS } from '../useChart'
import { getDeviceStatus, getDeviceCategory, getDevicePropertyDistribution } from '@/api/eiot/firewatch/dataCenter.api'
// @deprecated 第二轮退场：mock 兜底为过渡期保留，逐 Tab 核对通过后随 mock.ts 一并删除
import { mockDeviceStatus, mockDeviceCategoryDistribution, mockPropertyDeviceDistribution } from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const { renderChart } = useChart()

// loading 仅用于首屏（刷新时不切 loading，保持图表容器 DOM 稳定，避免实例随 slot 重建）
const loading = ref(true)
const deviceStatus = ref<any>({})
const categories = ref<any[]>([])
const propDevices = ref<any[]>([])

const categoryEl = ref<HTMLDivElement>()
const propertyEl = ref<HTMLDivElement>()

const statusTotal = computed(() => Number(deviceStatus.value?.total) || 0)

const statusItems = computed(() => {
  const s = deviceStatus.value || {}
  return [
    { label: '设备总数', value: fmtInt(s.total), color: '#fff' },
    { label: '正常', value: fmtInt(s.normal), color: '#52c41a' },
    { label: '告警设备', value: fmtInt(s.alarmDeviceCountInRange), color: '#ff4d4f' },
    { label: '故障', value: fmtInt(s.fault), color: '#fa8c16' },
    { label: '离线', value: fmtInt(s.offline), color: '#8c8c8c' },
    { label: '未激活', value: fmtInt(s.inactive), color: '#8b9dc5' },
  ]
})

const safe = async (fn: () => Promise<any>) => {
  try {
    return await fn()
  } catch {
    return null
  }
}

const renderCharts = () => {
  renderChart(categoryEl.value, {
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
        data: categories.value.map((d) => ({ name: d.categoryName || '未分类', value: d.count || 0 })),
      },
    ],
  })

  renderChart(propertyEl.value, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...darkTooltip },
    grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, ...darkAxis },
    yAxis: {
      type: 'category',
      ...darkAxis,
      data: propDevices.value.map((d) => d.propertyName || '未知单位').reverse(), // 反转让第一名在顶部
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: { color: '#1890ff', borderRadius: [0, 3, 3, 0] },
        data: propDevices.value.map((d) => d.deviceCount || 0).reverse(),
      },
    ],
  })
}

const loadData = async () => {
  try {
    const [s, c, p] = await Promise.all([
      safe(() => getDeviceStatus(props.days, props.propertyId)),
      safe(() => getDeviceCategory(props.propertyId)),
      safe(() => getDevicePropertyDistribution()),
    ])
    deviceStatus.value = s && s.total ? s : mockDeviceStatus
    categories.value = c?.length ? c : mockDeviceCategoryDistribution
    propDevices.value = p?.length ? p : mockPropertyDeviceDistribution
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
