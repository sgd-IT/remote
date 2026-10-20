/**
 * 数据中心图表公共 composable
 *
 * 防旧坑（历史上 ECharts 曾因以下原因被整体移除，回归时必须保持）：
 * - 图表容器一律用「行内 style」定尺寸（scoped 样式穿透不到 slot 内容）；
 * - flex 尺寸链每层同时给 min-width 与 min-height: 0（只给 min-height 时宽度方向会被撑爆或缩成 0）；
 * - 初始化链：onMounted → 数据就绪 → nextTick → echarts.init → setOption。
 *
 * 生命周期收口：
 * - 每组件一个 window resize 监听（回调统一 resize 本组件全部实例），卸载时成对移除再 dispose；
 * - KeepAlive 切回（activated）时统一 resize，deactivated 期间不响应 resize。
 */
import { nextTick, onActivated, onBeforeUnmount, onDeactivated } from 'vue'
import * as echarts from 'echarts'

/** 图表统一色板（深色大屏风） */
export const CHART_COLORS = ['#4FC3F7', '#1890ff', '#52c41a', '#faad14', '#ff4d4f', '#a855f7', '#13c2c2', '#2f54eb']

/** 深色主题 tooltip 公共项 */
export const darkTooltip = {
  backgroundColor: 'rgba(10,30,56,0.94)',
  borderColor: 'rgba(79,195,247,0.35)',
  textStyle: { color: '#fff', fontSize: 12 },
}

/** 深色主题坐标轴公共项（axisTick 关闭；类目轴可再叠加 hideOverlap） */
export const darkAxis = {
  axisLine: { lineStyle: { color: 'rgba(79,195,247,0.25)' } },
  axisTick: { show: false },
  axisLabel: { color: '#8b9dc5', fontSize: 11 },
  splitLine: { lineStyle: { color: 'rgba(79,195,247,0.08)' } },
}

/** 计数：整数 */
export const fmtInt = (v?: number | null): string => String(Math.round(Number(v) || 0))

/** 比率：1 位小数 + % */
export const fmtRate = (v?: number | null): string => `${(Number(v) || 0).toFixed(1)}%`

/**
 * 边界规则：空列表或指标全 0 视为无数据（走 ChartCard 空态）；
 * TOP 榜不足 N 条不补空（由后端天然回传多少显示多少）
 */
export const isSeriesEmpty = (list: any[] | undefined | null, pick: (item: any) => number = (i) => i?.count): boolean =>
  !list?.length || list.every((i) => !Number(pick(i) || 0))

export function useChart() {
  const instances: echarts.ECharts[] = []
  let active = true

  /** 渲染/更新单个图表：同容器重复调用仅 setOption；容器不存在（loading/空态）时静默跳过 */
  const renderChart = (el: HTMLDivElement | null | undefined, option: any) => {
    if (!el) return
    const chart = echarts.getInstanceByDom(el) || echarts.init(el)
    chart.setOption(option)
    if (!instances.includes(chart)) instances.push(chart)
  }

  const resizeAll = () => {
    if (active) instances.forEach((c) => c.resize())
  }

  window.addEventListener('resize', resizeAll)
  onActivated(() => {
    active = true
    nextTick(resizeAll) // KeepAlive 切回：缓存期间容器尺寸可能已变化
  })
  onDeactivated(() => {
    active = false
  })
  onBeforeUnmount(() => {
    window.removeEventListener('resize', resizeAll) // 与 addEventListener 成对
    instances.forEach((c) => c.dispose())
    instances.length = 0
  })

  return { renderChart, resizeAll }
}
