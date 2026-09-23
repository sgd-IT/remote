<template>
  <div class="dashboard-container">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card device-total">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="40"><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ deviceStatus.total || 0 }}</div>
              <div class="stat-label">设备总数</div>
              <div class="stat-rate">离线 {{ deviceStatus.offline || 0 }} 台</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card device-online">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="40"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ deviceStatus.online || 0 }}</div>
              <div class="stat-label">在线设备</div>
              <div class="stat-rate">在线率 {{ deviceStatus.onlineRate || 0 }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card alarm-today">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="40"><Bell /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ todayAlarm.totalAlarms || 0 }}</div>
              <div class="stat-label">今日告警</div>
              <div class="stat-rate">处置率 {{ todayAlarm.handleRate || 0 }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card pending-handle">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="40"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ todayAlarm.pendingCount || 0 }}</div>
              <div class="stat-label">待处理</div>
              <div class="stat-rate">平均响应 {{ todayAlarm.avgResponseTime || 0 }}秒</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主要内容区域 -->
    <el-row :gutter="16" class="main-content">
      <!-- 左侧：告警滚动列表 -->
      <el-col :span="8">
        <el-card class="alarm-list-card">
          <template #header>
            <div class="card-header">
              <span>实时告警</span>
              <el-badge :value="todayAlarm.pendingCount" :max="99" class="badge-item" />
            </div>
          </template>
          <div class="alarm-scroll-container" ref="alarmScrollRef">
            <div class="alarm-list" :class="{ 'animate-scroll': alarmList.length > 5 }">
              <div
                v-for="alarm in alarmList"
                :key="alarm.id"
                class="alarm-item"
                :class="getAlarmLevelClass(alarm.alarmLevel)"
                @click="handleAlarmClick(alarm)"
              >
                <div class="alarm-header">
                  <el-tag :type="getAlarmLevelType(alarm.alarmLevel)" size="small">
                    {{ alarm.alarmLevelName }}
                  </el-tag>
                  <span class="alarm-time">{{ formatTime(alarm.alarmTime) }}</span>
                </div>
                <div class="alarm-content">
                  <div class="alarm-title">{{ alarm.alarmTypeName }} - {{ alarm.deviceName }}</div>
                  <div class="alarm-location">{{ alarm.alarmLocation || alarm.propertyName }}</div>
                </div>
                <div class="alarm-status">
                  <el-tag :type="getStatusType(alarm.orderStatus)" size="small" effect="dark">
                    {{ alarm.orderStatusName }}
                  </el-tag>
                </div>
              </div>
              <el-empty v-if="alarmList.length === 0" description="暂无告警" />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 中间：图表区域 -->
      <el-col :span="8">
        <el-card class="chart-card chart-card-top">
          <template #header>
            <div class="chart-header">
              <span>告警趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="handleTrendDaysChange">
                <el-radio-button :value="7">近7天</el-radio-button>
                <el-radio-button :value="15">近15天</el-radio-button>
                <el-radio-button :value="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="alarmTrendChartRef" class="chart-container"></div>
        </el-card>
        <el-card class="chart-card chart-card-bottom mt-16">
          <template #header>
            <span>设备类型分布</span>
          </template>
          <div ref="deviceCategoryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 右侧：风险分布和设备状态 -->
      <el-col :span="8">
        <el-card class="chart-card chart-card-top">
          <template #header>
            <span>风险单位分布</span>
          </template>
          <div ref="riskDistributionChartRef" class="chart-container"></div>
        </el-card>
        <el-card class="device-status-card mt-16">
          <template #header>
            <span>设备状态统计</span>
          </template>
          <div class="device-status-content">
            <div class="status-item normal">
              <div class="status-count">{{ deviceStatus.normal || 0 }}</div>
              <div class="status-label">正常</div>
            </div>
            <div class="status-item alarm">
              <div class="status-count">{{ deviceStatus.alarm || 0 }}</div>
              <div class="status-label">报警</div>
            </div>
            <div class="status-item fault">
              <div class="status-count">{{ deviceStatus.fault || 0 }}</div>
              <div class="status-label">故障</div>
            </div>
            <div class="status-item offline">
              <div class="status-count">{{ deviceStatus.offline || 0 }}</div>
              <div class="status-label">离线</div>
            </div>
            <div class="status-item inactive">
              <div class="status-count">{{ deviceStatus.inactive || 0 }}</div>
              <div class="status-label">未激活</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作入口 -->
    <el-card class="quick-actions">
      <template #header>
        <span>快捷操作</span>
      </template>
      <div class="action-buttons">
        <el-button type="danger" :icon="Bell" @click="goToAlarmOrder">
          火警处置
          <el-badge v-if="todayAlarm.pendingCount > 0" :value="todayAlarm.pendingCount" class="action-badge" />
        </el-button>
        <el-button type="warning" :icon="Setting" @click="goToDutySchedule">值班管理</el-button>
        <el-button type="info" :icon="Document" @click="goToPatrolTask">巡检任务</el-button>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Monitor, Connection, Bell, Warning, Setting, Document } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getRealtimeAlarms,
  getAlarmTrend,
  getDeviceCategory,
  getRiskDistribution,
  getDeviceStatus,
  getTodayAlarm
} from '@/api/eiot/firewatch/firewatch.api'

const router = useRouter()

// 数据定义
const deviceStatus = ref<any>({
  total: 0,
  online: 0,
  offline: 0,
  alarm: 0,
  fault: 0,
  inactive: 0,
  normal: 0,
  onlineRate: 0,
  alarmRate: 0,
  faultRate: 0
})

const todayAlarm = ref<any>({
  totalAlarms: 0,
  handledCount: 0,
  pendingCount: 0,
  falseAlarmCount: 0,
  handleRate: 0,
  avgResponseTime: 0,
  fireAlarmCount: 0,
  faultAlarmCount: 0
})

const alarmList = ref<any[]>([])
const alarmTrendData = ref<any[]>([])
const deviceCategoryData = ref<any[]>([])
const riskDistributionData = ref<any[]>([])

// 告警趋势时间范围
const trendDays = ref(7)

// 图表引用
const alarmTrendChartRef = ref<HTMLElement>()
const deviceCategoryChartRef = ref<HTMLElement>()
const riskDistributionChartRef = ref<HTMLElement>()
const alarmScrollRef = ref<HTMLElement>()

// 图表实例
let alarmTrendChart: echarts.ECharts | null = null
let deviceCategoryChart: echarts.ECharts | null = null
let riskDistributionChart: echarts.ECharts | null = null

// 定时刷新
let refreshTimer: number | null = null

// 获取数据
const fetchDashboardData = async () => {
  try {
    // 并行请求所有数据
    const [statusRes, alarmRes, alarmsRes, trendRes, categoryRes, riskRes] = await Promise.allSettled([
      getDeviceStatus(),
      getTodayAlarm(),
      getRealtimeAlarms(20),
      getAlarmTrend(7),
      getDeviceCategory(),
      getRiskDistribution()
    ])

    deviceStatus.value = (statusRes.status === 'fulfilled' && statusRes.value) || deviceStatus.value
    todayAlarm.value = (alarmRes.status === 'fulfilled' && alarmRes.value) || todayAlarm.value
    alarmList.value = (alarmsRes.status === 'fulfilled' && alarmsRes.value) || alarmList.value
    alarmTrendData.value = (trendRes.status === 'fulfilled' && trendRes.value) || alarmTrendData.value
    deviceCategoryData.value = (categoryRes.status === 'fulfilled' && categoryRes.value) || deviceCategoryData.value
    riskDistributionData.value = (riskRes.status === 'fulfilled' && riskRes.value) || riskDistributionData.value

    // 更新图表
    nextTick(() => {
      updateCharts()
    })
  } catch (error) {
    console.error('获取大屏数据失败:', error)
    // 使用mock数据并更新图表
    nextTick(() => {
      updateCharts()
    })
  }
}

// 加载mock数据（API不可用时兜底）
const loadMockData = () => {
  const today = new Date()
  deviceStatus.value = {
    total: 20,
    online: 0,
    offline: 20,
    normal: 0,
    alarm: 3,
    fault: 1,
    inactive: 16,
    onlineRate: 0,
    alarmRate: 15,
    faultRate: 5
  }
  todayAlarm.value = {
    totalAlarms: 5,
    handledCount: 2,
    pendingCount: 3,
    falseAlarmCount: 0,
    handleRate: 20,
    avgResponseTime: 0,
    fireAlarmCount: 3,
    faultAlarmCount: 2
  }
  alarmList.value = [
    {
      id: 1,
      alarmLevel: 'critical',
      alarmLevelName: '严重',
      alarmTypeName: '烟感报警',
      deviceName: '烟感探测器-WJ-02',
      alarmLocation: '8栋西入口',
      propertyName: '安为科技示范单位',
      alarmTime: new Date(today.getTime() - 30 * 60000).toISOString(),
      orderStatus: 'pending',
      orderStatusName: '待处置'
    },
    {
      id: 2,
      alarmLevel: 'major',
      alarmLevelName: '产警',
      alarmTypeName: '烟感报警',
      deviceName: '烟感探测器-WJ-01',
      alarmLocation: 'A栋3楼东侧通道',
      propertyName: '安为科技示范单位',
      alarmTime: new Date(today.getTime() - 60 * 60000).toISOString(),
      orderStatus: 'pending',
      orderStatusName: '待处置'
    },
    {
      id: 3,
      alarmLevel: 'major',
      alarmLevelName: '产警',
      alarmTypeName: '烟感报警',
      deviceName: '烟感探测器-GC-01',
      alarmLocation: '1楼原料库中部',
      propertyName: '广州天河广场',
      alarmTime: new Date(today.getTime() - 120 * 60000).toISOString(),
      orderStatus: 'pending',
      orderStatusName: '待处置'
    }
  ]
  // 告警趋势（按天数生成）
  alarmTrendData.value = generateTrendMockData(trendDays.value)
  // 设备类型分布
  deviceCategoryData.value = [
    { categoryName: '灭火器', count: 6 },
    { categoryName: '烟感探测器', count: 8 },
    { categoryName: '温感探测器', count: 3 },
    { categoryName: '手动报警器', count: 2 },
    { categoryName: '消防栓', count: 1 }
  ]
  // 风险分布
  riskDistributionData.value = [
    { riskLevelName: '重大风险', riskLevel: 'critical', count: 1 },
    { riskLevelName: '较大风险', riskLevel: 'major', count: 2 },
    { riskLevelName: '一般风险', riskLevel: 'normal', count: 2 }
  ]
}

// 生成趋势mock数据
const generateTrendMockData = (days: number) => {
  const today = new Date()
  return Array.from({ length: days }, (_, i) => {
    const d = new Date(today)
    d.setDate(d.getDate() - (days - 1 - i))
    const alarmCount = Math.floor(Math.random() * 8) + 1
    const handleCount = Math.max(0, alarmCount - Math.floor(Math.random() * 3))
    return {
      date: d.toISOString().slice(0, 10),
      alarmCount,
      handleCount
    }
  })
}

// 切换告警趋势时间范围
const handleTrendDaysChange = async (days: number) => {
  try {
    const res = await getAlarmTrend(days)
    if (res) {
      alarmTrendData.value = res
    } else {
      alarmTrendData.value = generateTrendMockData(days)
    }
  } catch {
    alarmTrendData.value = generateTrendMockData(days)
  }
  nextTick(() => {
    updateAlarmTrendChart()
  })
}

// 初始化图表
const initCharts = () => {
  if (alarmTrendChartRef.value) {
    alarmTrendChart = echarts.init(alarmTrendChartRef.value)
  }
  if (deviceCategoryChartRef.value) {
    deviceCategoryChart = echarts.init(deviceCategoryChartRef.value)
  }
  if (riskDistributionChartRef.value) {
    riskDistributionChart = echarts.init(riskDistributionChartRef.value)
  }
}

// 更新图表
const updateCharts = () => {
  updateAlarmTrendChart()

  // 设备类型分布图
  if (deviceCategoryChart && deviceCategoryData.value.length > 0) {
    deviceCategoryChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', backgroundColor: '#FFFFFF', borderColor: '#C8DDDC', textStyle: { color: '#293B3D' } },
      legend: { type: 'scroll', orient: 'vertical', right: 10, top: 20, bottom: 20, textStyle: { color: '#52686a' } },
      color: ['#199b96', '#4cb5b0', '#53b57d', '#f3a42a', '#e65353'],
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 10, borderColor: '#eef5f5', borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
          labelLine: { show: false },
          data: deviceCategoryData.value.map((item: any) => ({
            name: item.categoryName,
            value: item.count
          }))
        }
      ]
    })
  }

  // 风险分布图
  if (riskDistributionChart && riskDistributionData.value.length > 0) {
    riskDistributionChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}个', backgroundColor: '#FFFFFF', borderColor: '#C8DDDC', textStyle: { color: '#293B3D' } },
      series: [
        {
          type: 'pie',
          radius: '70%',
          center: ['50%', '50%'],
          data: riskDistributionData.value.map((item: any) => ({
            name: item.riskLevelName,
            value: item.count,
            itemStyle: {
              color: item.riskLevel === 'critical' ? '#e65353' :
                     item.riskLevel === 'major' ? '#f3a42a' : '#53b57d'
            }
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.1)'
            }
          },
          label: { formatter: '{b}\n{c}个', color: '#52686a' }
        }
      ]
    })
  }
}

// 单独更新告警趋势图
const updateAlarmTrendChart = () => {
  if (!alarmTrendChart || alarmTrendData.value.length === 0) return
  alarmTrendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#FFFFFF',
      borderColor: '#C8DDDC',
      borderWidth: 1,
      textStyle: { color: '#293B3D' },
      axisPointer: {
        type: 'cross',
        crossStyle: { color: '#B9CCCC' }
      }
    },
    legend: {
      data: ['告警数', '处置数'],
      top: 0,
      right: 0,
      textStyle: { fontSize: 12, color: '#52686a' }
    },
    grid: {
      left: '3%',
      right: '4%',
      top: 30,
      bottom: 25,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: alarmTrendData.value.map((item: any) => item.date?.substring(5)),
      axisLine: { lineStyle: { color: '#B9CCCC' } },
      axisTick: { show: false },
      axisLabel: { color: '#708486', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#708486', fontSize: 11 },
      splitLine: { lineStyle: { color: '#E5EEEE', type: 'dashed' } }
    },
    series: [
      {
        name: '告警数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: alarmTrendData.value.map((item: any) => item.alarmCount),
        itemStyle: { color: '#E65353' },
        lineStyle: { width: 2, color: '#E65353' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(230,83,83,0.15)' },
            { offset: 1, color: 'rgba(230,83,83,0.01)' }
          ])
        }
      },
      {
        name: '处置数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: alarmTrendData.value.map((item: any) => item.handleCount),
        itemStyle: { color: '#53B57D' },
        lineStyle: { width: 2, color: '#53B57D' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(83,181,125,0.15)' },
            { offset: 1, color: 'rgba(83,181,125,0.01)' }
          ])
        }
      }
    ]
  }, true)
}

// 获取告警级别样式类
const getAlarmLevelClass = (level: string) => {
  return `alarm-level-${level}`
}

// 获取告警级别标签类型
const getAlarmLevelType = (level: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    critical: 'danger',
    major: 'warning',
    warning: 'info',
    info: 'info'
  }
  return typeMap[level] || 'info'
}

// 获取状态标签类型
const getStatusType = (status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    pending: 'danger',
    confirming: 'warning',
    confirmed: 'success',
    false_alarm: 'info',
    real_fire: 'danger',
    closed: 'info'
  }
  return typeMap[status] || 'info'
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 点击告警
const handleAlarmClick = (alarm: any) => {
  router.push({ path: '/eiot/firewatch/alarm-order', query: { id: alarm.id } })
}

// 快捷操作
const goToAlarmOrder = () => {
  router.push('/eiot/firewatch/alarm-order')
}

const goToDutySchedule = () => {
  router.push('/eiot/firewatch/duty')
}

const goToPatrolTask = () => {
  router.push('/eiot/firewatch/patrol')
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  alarmTrendChart?.resize()
  deviceCategoryChart?.resize()
  riskDistributionChart?.resize()
}

onMounted(() => {
  // 先加载mock数据保证UI有内容展示
  loadMockData()
  initCharts()
  // 用mock数据先渲染图表
  nextTick(() => {
    updateCharts()
  })
  // 再尝试从API获取真实数据
  fetchDashboardData()
  
  // 每30秒刷新一次数据
  refreshTimer = window.setInterval(() => {
    fetchDashboardData()
  }, 30000)

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  alarmTrendChart?.dispose()
  deviceCategoryChart?.dispose()
  riskDistributionChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
@use 'sass:color';
$bg1: #f0f7f7; $bg2: #e8f2f2; $bd: #add5d4; $t1: #293b3d; $t2: #52686a;
$glass-bg: rgba(255, 255, 255, 0.96);
$blue: #199b96; $red: #e65353; $orange: #f3a42a; $green: #53b57d; $cyan: #4cb5b0;

.dashboard-container {
  padding: 0;
  background: #eef5f5;
  min-height: calc(100vh - 84px);
}

.stat-cards {
  margin-bottom: 16px;
}

// ====== 统一定义 el-card 浅色主题覆盖 ======
:deep(.el-card) {
  background: $glass-bg;
  border: 1px solid $bd;
  border-radius: 6px;
  box-shadow: 0 4px 14px rgba(57, 91, 92, 0.08);
  .el-card__header {
    border-bottom: 1px solid #e2eded;
    padding: 12px 16px;
    font-size: 15px;
    font-weight: 600;
    color: $t1;
  }
}

.stat-card {
  border-radius: 6px;
  :deep(.el-card__body) {
    padding: 16px;
  }
  
  .stat-content {
    display: flex;
    align-items: center;
    
    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;
    }
    
    .stat-info {
      flex: 1;
      
      .stat-value {
        font-size: 28px;
        font-weight: bold;
        line-height: 1.2;
      }
      
      .stat-label {
        font-size: 14px;
        color: $t2;
        margin-top: 4px;
      }
      
      .stat-rate {
        font-size: 12px;
        color: #7b8e90;
        margin-top: 4px;
      }
    }
  }
  
  &.device-total {
    .stat-icon { background: rgba($blue, 0.1); color: $blue; }
    .stat-value { color: $blue; }
  }
  
  &.device-online {
    .stat-icon { background: rgba($green, 0.1); color: $green; }
    .stat-value { color: $green; }
  }
  
  &.alarm-today {
    .stat-icon { background: rgba($red, 0.1); color: $red; }
    .stat-value { color: $red; }
  }
  
  &.pending-handle {
    .stat-icon { background: rgba($orange, 0.1); color: $orange; }
    .stat-value { color: $orange; }
  }
}

.main-content {
  margin-bottom: 16px;
}

.alarm-list-card {
  height: 576px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .alarm-scroll-container {
    height: 496px;
    overflow: hidden;
  }
  
  .alarm-list {
    height: 100%;
    
    &.animate-scroll {
      animation: scroll 20s linear infinite;
      
      &:hover {
        animation-play-state: paused;
      }
    }
  }
  
  .alarm-item {
    padding: 12px;
    margin-bottom: 8px;
    border-radius: 6px;
    background: #ffffff;
    border: 1px solid #e0ebeb;
    border-left: 4px solid $red;
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      background: #eef8f7;
      transform: translateX(4px);
    }
    
    &.alarm-level-critical {
      border-left-color: $red;
      background: #fde7e7;
    }
    
    &.alarm-level-major {
      border-left-color: $orange;
      background: #fff0da;
    }
    
    .alarm-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      
      .alarm-time {
        font-size: 12px;
        color: #7b8e90;
      }
    }
    
    .alarm-content {
      .alarm-title {
        font-weight: 500;
        color: $t1;
        margin-bottom: 4px;
      }
      
      .alarm-location {
        font-size: 12px;
        color: #7b8e90;
      }
    }
    
    .alarm-status {
      margin-top: 8px;
      text-align: right;
    }
  }
}

.chart-card {
  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 15px;
    font-weight: 600;
    color: $t1;
  }

  &.chart-card-top {
    height: 300px;
    
    :deep(.el-card__body) {
      padding: 8px 12px;
      height: calc(100% - 56px);
      box-sizing: border-box;
    }
    
    .chart-container {
      width: 100%;
      height: 100%;
    }
  }
  
  &.chart-card-bottom {
    height: 260px;
    
    :deep(.el-card__body) {
      padding: 8px 12px;
      height: calc(100% - 56px);
      box-sizing: border-box;
    }
    
    .chart-container {
      width: 100%;
      height: 100%;
    }
  }
}

.device-status-card {
  height: 260px;
  .device-status-content {
    display: flex;
    justify-content: space-around;
    padding: 16px 0;
    
    .status-item {
      text-align: center;
      
      .status-count {
        font-size: 32px;
        font-weight: bold;
      }
      
      .status-label {
        font-size: 14px;
        color: $t2;
        margin-top: 8px;
      }
      
      &.normal {
        .status-count { color: $green; }
      }
      
      &.alarm {
        .status-count { color: $red; }
      }
      
      &.fault {
        .status-count { color: $orange; }
      }
      
      &.offline {
        .status-count { color: #7b8e90; }
      }

      &.inactive {
        .status-count { color: #b0c0c0; }
      }
    }
  }
}

.quick-actions {
  .action-buttons {
    display: flex;
    gap: 16px;
    
    .el-button {
      flex: 1;
      height: 48px;
      font-size: 16px;
    }
    
    .action-badge {
      margin-left: 8px;
    }
  }
}

.mt-16 {
  margin-top: 16px;
}

@keyframes scroll {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-50%);
  }
}
</style>
