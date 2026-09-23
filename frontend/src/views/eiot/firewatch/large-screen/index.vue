<template>
  <div class="large-screen" ref="screenRef">
    <!-- 顶部标题栏 -->
    <header class="header">
      <div class="header-left">
        <img src="/images/anwei-logo.png" alt="安为科技" class="logo" @error="handleLogoError" />
        <div class="title-wrapper">
          <div class="title-row">
            <h1 class="title">安为智慧消防监控平台</h1>
            <DevelopingTag mini tip="本页部分面板仍为演示数据，对应统计接口开发中" />
          </div>
          <p class="subtitle">ANWEI SMART FIRE MONITORING PLATFORM</p>
        </div>
      </div>
      <!-- 中央告警横幅 -->
      <div class="header-center">
        <div class="alert-banner" v-if="alarmList.length > 0">
          <span class="alert-pulse"></span>
          <span class="alert-text">实时告警中 有{{ alarmList.length }}条未处理告警</span>
        </div>
        <div class="datetime" v-else>
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
          <span class="week">{{ currentWeek }}</span>
        </div>
      </div>
      <div class="header-right">
        <div class="datetime-right" v-if="alarmList.length > 0">
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
          <span class="week">{{ currentWeek }}</span>
        </div>
        <div class="weather">
          <el-icon><Sunny /></el-icon>
          <span>{{ weather.temp }}℃ {{ weather.text }}</span>
        </div>
        <el-button type="primary" class="admin-btn" @click="goToAdmin">
          <el-icon><Setting /></el-icon>
          进入管理后台
        </el-button>
        <el-button class="fullscreen-btn" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
        </el-button>
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="main-content">
      <!-- 左侧面板 -->
      <aside class="left-panel">
        <!-- 联网单位总览 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><OfficeBuilding /></el-icon>
              联网单位总览
            </span>
          </div>
          <div class="card-content overview-content">
            <div class="overview-grid">
              <div class="ov-item">
                <div class="ov-value blue">{{ enterpriseStats.total }}</div>
                <div class="ov-label">联网单位</div>
              </div>
              <div class="ov-item">
                <div class="ov-value green">{{ enterpriseStats.normal }}</div>
                <div class="ov-label">正常</div>
              </div>
              <div class="ov-item">
                <div class="ov-value orange">{{ enterpriseStats.warning }}</div>
                <div class="ov-label">预警</div>
              </div>
              <div class="ov-item">
                <div class="ov-value red">{{ enterpriseStats.fault }}</div>
                <div class="ov-label">告警</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 设备状态统计 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><Monitor /></el-icon>
              设备状态统计
            </span>
          </div>
          <div class="card-content">
            <div class="device-total-hero">
              <span class="total-num">{{ deviceStats.total }}</span>
              <span class="total-unit">台</span>
            </div>
            <div class="device-stats">
              <div class="stat-item normal">
                <div class="stat-label">正常</div>
                <div class="stat-bar-wrap"><div class="stat-bar" :style="{ width: getPercent(deviceStats.normal, deviceStats.total) + '%' }"></div></div>
                <div class="stat-value">{{ deviceStats.normal }}</div>
              </div>
              <div class="stat-item warning">
                <div class="stat-label">告警</div>
                <div class="stat-bar-wrap"><div class="stat-bar" :style="{ width: getPercent(deviceStats.warning, deviceStats.total) + '%' }"></div></div>
                <div class="stat-value">{{ deviceStats.warning }}</div>
              </div>
              <div class="stat-item fault">
                <div class="stat-label">故障</div>
                <div class="stat-bar-wrap"><div class="stat-bar" :style="{ width: getPercent(deviceStats.fault, deviceStats.total) + '%' }"></div></div>
                <div class="stat-value">{{ deviceStats.fault }}</div>
              </div>
              <div class="stat-item offline">
                <div class="stat-label">离线</div>
                <div class="stat-bar-wrap"><div class="stat-bar" :style="{ width: getPercent(deviceStats.offline, deviceStats.total) + '%' }"></div></div>
                <div class="stat-value">{{ deviceStats.offline }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 设施分类统计 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><Grid /></el-icon>
              设施分类统计
            </span>
          </div>
          <div class="card-content">
            <div class="facility-list">
              <div class="facility-item" v-for="item in facilityStats" :key="item.category">
                <div class="facility-icon" :style="{ background: item.color }">
                  <el-icon><component :is="item.icon" /></el-icon>
                </div>
                <div class="facility-info">
                  <div class="facility-name">{{ item.name }}</div>
                  <div class="facility-count">{{ item.count }}台</div>
                </div>
                <div class="facility-percent">{{ getPercent(item.count, deviceStats.total) }}%</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 实时告警 -->
        <div class="panel-card alarm-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><Bell /></el-icon>
              实时告警
              <span class="alarm-count" v-if="alarmList.length > 0">{{ alarmList.length }}</span>
            </span>
          </div>
          <div class="card-content alarm-list">
            <div class="alarm-item" v-for="alarm in alarmList" :key="alarm.id" :class="'level-' + alarm.level">
              <div class="alarm-dot"></div>
              <div class="alarm-info">
                <div class="alarm-title">{{ alarm.deviceName }}</div>
                <div class="alarm-desc">{{ alarm.alarmContent }}</div>
                <div class="alarm-time">{{ alarm.alarmTime }}</div>
              </div>
              <div class="alarm-level">{{ getAlarmLevelText(alarm.level) }}</div>
            </div>
            <div class="no-data" v-if="alarmList.length === 0">
              <el-icon><CircleCheck /></el-icon>
              <span>暂无告警</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- 中间地图区域 -->
      <section class="center-panel">
        <div class="map-container" ref="mapRef">
          <div class="map-header">
            <div class="map-title">联网单位分布</div>
            <div class="map-stats">
              <div class="map-stat-item">
                <div class="map-stat-value">{{ enterpriseStats.total }}</div>
                <div class="map-stat-label">联网单位</div>
              </div>
              <div class="map-stat-item">
                <div class="map-stat-value">{{ deviceStats.total }}</div>
                <div class="map-stat-label">接入设备</div>
              </div>
              <div class="map-stat-item">
                <div class="map-stat-value">{{ deviceStats.normal }}</div>
                <div class="map-stat-label">正常运行</div>
              </div>
              <div class="map-stat-item alarm-stat">
                <div class="map-stat-value">{{ alarmList.length }}</div>
                <div class="map-stat-label">告警数量</div>
              </div>
            </div>
          </div>
          <!-- 高德地图 -->
          <div id="large-screen-map" class="amap-container"></div>
          <!-- 地图图例 -->
          <div class="map-legend">
            <div class="legend-item"><span class="legend-dot" style="background: #ff4d4f"></span>告警</div>
            <div class="legend-item"><span class="legend-dot" style="background: #fa8c16"></span>较大风险</div>
            <div class="legend-item"><span class="legend-dot" style="background: #52c41a"></span>正常</div>
            <div class="legend-item"><span class="legend-dot" style="background: #8c8c8c"></span>离线</div>
          </div>
        </div>

        <!-- 视频预览模块 -->
        <div class="video-panel">
          <div class="panel-header">
            <span class="panel-title">
              <el-icon><VideoCamera /></el-icon>
              视频监控
            </span>
            <div class="panel-actions">
              <el-button size="small" text type="primary">上一页</el-button>
              <el-button size="small" text type="primary">下一页</el-button>
              <el-button size="small" text type="primary">全部视频</el-button>
            </div>
          </div>
          <div class="video-grid">
            <div class="video-item" v-for="(camera, index) in cameraList" :key="index">
              <div class="video-header">
                <el-icon><VideoCamera /></el-icon>
                <span>{{ camera.name }}</span>
              </div>
              <div class="video-content">
                <div class="video-placeholder">
                  <el-icon class="camera-icon"><VideoCamera /></el-icon>
                  <span class="live-text">LIVE</span>
                </div>
              </div>
              <div class="video-footer">
                <span class="resolution">1920x1080</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部统计 -->
        <div class="bottom-stats">
          <div class="bottom-stat-card" v-for="stat in bottomStats" :key="stat.label">
            <div class="stat-icon" :style="{ background: stat.color }">
              <el-icon><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧面板 -->
      <aside class="right-panel">
        <!-- 今日告警趋势 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><TrendCharts /></el-icon>
              今日告警趋势
            </span>
          </div>
          <div class="card-content">
            <div ref="alarmTrendChart" class="chart-container"></div>
          </div>
        </div>

        <!-- 告警类型分布 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><PieChart /></el-icon>
              告警类型分布
            </span>
          </div>
          <div class="card-content">
            <div ref="alarmTypeChart" class="chart-container"></div>
          </div>
        </div>

        <!-- 风险单位分布 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><DataAnalysis /></el-icon>
              风险单位分布
            </span>
          </div>
          <div class="card-content">
            <div ref="riskChart" class="chart-container"></div>
          </div>
        </div>

        <!-- 值班信息 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title">
              <el-icon><User /></el-icon>
              今日值班
            </span>
          </div>
          <div class="card-content duty-info">
            <div class="duty-item" v-for="duty in dutyList" :key="duty.id">
              <div class="duty-avatar">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div class="duty-detail">
                <div class="duty-name">{{ duty.userName }}</div>
                <div class="duty-time">{{ duty.shiftName }} {{ duty.startTime }}-{{ duty.endTime }}</div>
              </div>
              <div class="duty-status" :class="duty.status">{{ getDutyStatusText(duty.status) }}</div>
            </div>
            <div class="no-data" v-if="dutyList.length === 0">
              <span>暂无值班安排</span>
            </div>
          </div>
        </div>
      </aside>
    </main>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import request from '@/config/axios'
import DevelopingTag from '@/components/DevelopingTag/index.vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const router = useRouter()
const userStore = useUserStore()
const screenRef = ref<HTMLElement>()
const mapRef = ref<HTMLElement>()
const alarmTrendChart = ref<HTMLElement>()
const alarmTypeChart = ref<HTMLElement>()
const riskChart = ref<HTMLElement>()

// 高德地图实例
let mapInstance: any = null
let mapMarkers: any[] = []

// 地图中心点：东莞市南城区政府
const MAP_CENTER = [113.7517, 23.0469]

// 时间显示
const currentDate = ref('')
const currentTime = ref('')
const currentWeek = ref('')
const weather = ref({ temp: 28, text: '晴' })

// 声音提醒（蜂鸣已移除，语音播报由 alarmSpeech.ts 处理）

// 企业信息
const enterpriseInfo = ref<any>({
  name: '',
  contactName: '',
  contactPhone: '',
  address: '',
  riskLevel: ''
})

// 企业统计
const enterpriseStats = ref({
  total: 0,
  normal: 0,
  warning: 0,
  fault: 0
})

// 设备统计
const deviceStats = ref({
  total: 0,
  normal: 0,
  warning: 0,
  fault: 0,
  offline: 0
})

// 设施分类统计
const facilityStats = ref<any[]>([])

// 告警列表
const alarmList = ref<any[]>([])

// 地图点位
const mapPoints = ref<any[]>([])

// 值班列表
const dutyList = ref<any[]>([])

// 摄像头列表
const cameraList = ref([
  { id: 1, name: '南门监控摄像头', status: 'online' },
  { id: 2, name: '东门监控摄像头', status: 'online' },
  { id: 3, name: '消防通道监控', status: 'online' },
  { id: 4, name: '机房监控', status: 'online' }
])

// 底部统计
const bottomStats = computed(() => [
  { label: '今日火警', value: alarmList.value.filter(a => a.type === 'fire').length, icon: 'WarningFilled', color: 'linear-gradient(135deg, #ff6b6b, #ee5a5a)' },
  { label: '今日故障', value: alarmList.value.filter(a => a.type === 'fault').length, icon: 'Tools', color: 'linear-gradient(135deg, #feca57, #f8b739)' },
  { label: '今日巡检', value: 12, icon: 'DocumentChecked', color: 'linear-gradient(135deg, #48dbfb, #0abde3)' },
  { label: '完成率', value: '95%', icon: 'CircleCheck', color: 'linear-gradient(135deg, #1dd1a1, #10ac84)' }
])

// 图表实例
let alarmTrendChartInstance: ECharts | null = null
let alarmTypeChartInstance: ECharts | null = null
let riskChartInstance: ECharts | null = null

// 更新时间
const updateDateTime = () => {
  const now = new Date()
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentWeek.value = weekDays[now.getDay()]
}

// 进入管理后台
const goToAdmin = () => {
  const token = localStorage.getItem('ACCESS_TOKEN')
  if (!token) {
    router.push('/login')
    return
  }
  // 管理后台路径按角色区分：管理员/监管(super_admin/monitor_role) → property，企业用户 → enterprise
  const isAdmin = userStore.getRoles?.some(r => r === 'super_admin' || r === 'monitor_role')
  router.push(isAdmin ? '/eiot/firewatch/property' : '/eiot/firewatch/enterprise')
}

// 全屏
const toggleFullscreen = () => {
  if (!document.fullscreenElement) document.documentElement.requestFullscreen()
  else document.exitFullscreen()
}

// Logo加载失败处理
const handleLogoError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.style.display = 'none'
}

// 获取百分比
const getPercent = (value: number, total: number) => {
  if (total === 0) return 0
  return Math.round((value / total) * 100)
}

// 获取告警等级文本
const getAlarmLevelText = (level: string) => {
  const map: Record<string, string> = {
    critical: '火警',
    major: '故障',
    minor: '电源故障'
  }
  return map[level] || '故障'
}

// 获取值班状态文本
const getDutyStatusText = (status: string) => {
  const map: Record<string, string> = {
    ongoing: '值班中',
    pending: '待值班',
    completed: '已完成'
  }
  return map[status] || status
}

// 加载模拟数据
const loadMockData = () => {
  enterpriseInfo.value = {
    name: '安为科技示范单位',
    contactName: '张经理',
    contactPhone: '138****8888',
    address: '广东省东莞市南城区',
    riskLevel: 'normal'
  }
  enterpriseStats.value = { total: 5, normal: 2, warning: 2, fault: 1 }
  deviceStats.value = { total: 20, normal: 12, warning: 3, fault: 1, offline: 4 }
  facilityStats.value = [
    { category: 'smoke_detector', name: '烟感探测器', count: 6, color: '#10b981', icon: 'Bell' },
    { category: 'temp_detector', name: '温感探测器', count: 4, color: '#f59e0b', icon: 'Sunrise' },
    { category: 'fire_extinguisher', name: '灭火器', count: 4, color: '#3b82f6', icon: 'FirstAidKit' },
    { category: 'hydrant', name: '消防栓', count: 3, color: '#ef4444', icon: 'Aim' },
    { category: 'manual_alarm', name: '手动报警器', count: 3, color: '#ec4899', icon: 'Bell' }
  ]
  mapPoints.value = [
    { name: '安为科技示范单位', lng: 113.7517, lat: 23.0469, status: 'normal' },
    { name: '广州天河广场', lng: 113.3290, lat: 23.1370, status: 'warning' },
    { name: '佛山智慧小区', lng: 113.2500, lat: 22.8020, status: 'normal' },
    { name: '东莞科技工业园', lng: 113.9000, lat: 22.9200, status: 'normal' },
    { name: '珠海横琴中心', lng: 113.5400, lat: 22.0900, status: 'danger' }
  ]
  alarmList.value = [
    { id: 1, deviceName: '烟感探测器-WJ-01', alarmContent: '烟雾浓度超标告警', alarmTime: '2026-04-20 15:01:00', level: 'major', type: 'fire' },
    { id: 2, deviceName: '温感探测器-WG-03', alarmContent: '温度异常升高', alarmTime: '2026-04-20 14:45:00', level: 'major', type: 'fault' },
    { id: 3, deviceName: '烟感探测器-GC-01', alarmContent: '持续烟雾报警', alarmTime: '2026-04-20 14:30:00', level: 'major', type: 'fire' },
    { id: 4, deviceName: '手动报警器-SD-02', alarmContent: '手动触发报警', alarmTime: '2026-04-20 13:20:00', level: 'minor', type: 'manual' },
    { id: 5, deviceName: '消防栓压力传感器-XH-01', alarmContent: '消防栓水压过低', alarmTime: '2026-04-20 12:10:00', level: 'minor', type: 'fault' }
  ]
  dutyList.value = [
    { id: 1, userName: '李明', shiftName: '白班', startTime: '08:00', endTime: '16:00', status: 'ongoing' },
    { id: 2, userName: '王强', shiftName: '中班', startTime: '16:00', endTime: '00:00', status: 'pending' },
    { id: 3, userName: '张伟', shiftName: '夜班', startTime: '00:00', endTime: '08:00', status: 'completed' }
  ]
}

// 安全请求封装
const silentRequest = async (url: string, params?: any) => {
  try {
    const res = await request.get({ url, params })
    return res
  } catch (error) {
    return null
  }
}

// 加载数据
const loadData = async () => {
  loadMockData()

  const enterpriseRes = await silentRequest('/eiot/firewatch/property/list')
  if (enterpriseRes && enterpriseRes.data?.length > 0) {
    enterpriseStats.value.total = enterpriseRes.data.length
    enterpriseStats.value.normal = enterpriseRes.data.filter((e: any) => e.riskLevel === 'normal').length
    enterpriseStats.value.warning = enterpriseRes.data.filter((e: any) => e.riskLevel === 'major').length
    enterpriseStats.value.fault = enterpriseRes.data.filter((e: any) => e.riskLevel === 'critical').length
    mapPoints.value = enterpriseRes.data.filter((e: any) => e.longitude && e.latitude).map((e: any) => ({
      name: e.name || e.propertyName,
      lng: e.longitude,
      lat: e.latitude,
      status: e.riskLevel === 'critical' ? 'danger' : e.riskLevel === 'major' ? 'warning' : 'normal'
    }))
  }

  const deviceRes = await silentRequest('/eiot/firewatch/device/list')
  if (deviceRes && deviceRes.data) {
    const devices = deviceRes.data || []
    deviceStats.value.total = devices.length
    deviceStats.value.normal = devices.filter((d: any) => d.deviceStatus === 'normal').length
    deviceStats.value.warning = devices.filter((d: any) => d.deviceStatus === 'alarm').length
    deviceStats.value.fault = devices.filter((d: any) => d.deviceStatus === 'fault').length
    deviceStats.value.offline = devices.filter((d: any) => d.deviceStatus === 'offline').length
  }

  const facilityRes = await silentRequest('/eiot/firewatch/device/facility-categories')
  if (facilityRes && facilityRes.data) {
    const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16']
    const icons = ['WaterDrop', 'FireExtinguisher', 'Bell', 'VideoCamera', 'PhoneFilled', 'Thermometer', 'WindPower', 'Cpu']
    facilityStats.value = (facilityRes.data || []).map((item: any, index: number) => ({
      category: item.code,
      name: item.name,
      count: item.count || Math.floor(Math.random() * 20) + 5,
      color: colors[index % colors.length],
      icon: icons[index % icons.length]
    }))
  }

  const alarmRes = await silentRequest('/eiot/firewatch/alarm-order/list', { status: 'pending' })
  if (alarmRes && alarmRes.data) {
    const newAlarms = (alarmRes.data || []).slice(0, 5).map((item: any) => ({
      id: item.id,
      deviceName: item.deviceName,
      alarmContent: item.alarmContent || '设备告警',
      alarmTime: item.alarmTime,
      level: item.alarmLevel || 'minor',
      type: item.alarmType
    }))
    alarmList.value = newAlarms
  }

  const dutyRes = await silentRequest('/eiot/firewatch/duty-schedule/list', { status: 'ongoing' })
  if (dutyRes && dutyRes.data) {
    dutyList.value = (dutyRes.data || []).slice(0, 3).map((item: any) => ({
      id: item.id,
      userName: item.userName,
      shiftName: item.shiftName,
      startTime: item.startTime?.substring(0, 5) || '08:00',
      endTime: item.endTime?.substring(0, 5) || '18:00',
      status: item.status || 'ongoing'
    }))
  }
}

// 初始化图表
const initCharts = () => {
  if (alarmTrendChart.value) {
    alarmTrendChartInstance = echarts.init(alarmTrendChart.value)
    const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
    const alarmData = Array.from({ length: 24 }, () => Math.floor(Math.random() * 10))
    const handleData = Array.from({ length: 24 }, () => Math.floor(Math.random() * 8))
    alarmTrendChartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['告警数', '处理数'], textStyle: { color: '#8b9dc5', fontSize: 10 }, top: 0, right: 4 },
      grid: { left: '10%', right: '5%', top: '18%', bottom: '15%' },
      xAxis: {
        type: 'category',
        data: hours.filter((_, i) => i % 3 === 0),
        axisLine: { lineStyle: { color: '#1e3a5f' } },
        axisLabel: { color: '#8b9dc5', fontSize: 10 }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#162d50' } },
        axisLabel: { color: '#8b9dc5', fontSize: 10 }
      },
      series: [
        {
          name: '告警数', type: 'line', smooth: true,
          data: alarmData.filter((_, i) => i % 3 === 0),
          lineStyle: { color: '#ff6b6b', width: 2 }, itemStyle: { color: '#ff6b6b' }, symbolSize: 4,
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 107, 107, 0.35)' },
            { offset: 1, color: 'rgba(255, 107, 107, 0.02)' }
          ])}
        },
        {
          name: '处理数', type: 'line', smooth: true,
          data: handleData.filter((_, i) => i % 3 === 0),
          lineStyle: { color: '#1dd1a1', width: 2 }, itemStyle: { color: '#1dd1a1' }, symbolSize: 4
        }
      ]
    })
  }

  if (alarmTypeChart.value) {
    alarmTypeChartInstance = echarts.init(alarmTypeChart.value)
    alarmTypeChartInstance.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['42%', '68%'], center: ['50%', '50%'],
        data: [
          { value: 35, name: '火警告警', itemStyle: { color: '#ff6b6b' } },
          { value: 28, name: '故障告警', itemStyle: { color: '#feca57' } },
          { value: 18, name: '设备离线', itemStyle: { color: '#8b9dc5' } },
          { value: 12, name: '其他告警', itemStyle: { color: '#54a0ff' } }
        ],
        label: { color: '#8b9dc5', fontSize: 10 },
        labelLine: { lineStyle: { color: '#3d4f6f' } },
        emphasis: { scaleSize: 6 }
      }]
    })
  }

  if (riskChart.value) {
    riskChartInstance = echarts.init(riskChart.value)
    riskChartInstance.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: '68%', center: ['50%', '50%'],
        data: [
          { value: 1, name: '重大风险', itemStyle: { color: '#ff4d4f' } },
          { value: 2, name: '较大风险', itemStyle: { color: '#fa8c16' } },
          { value: 2, name: '一般风险', itemStyle: { color: '#52c41a' } },
          { value: 0, name: '较小风险', itemStyle: { color: '#1890ff' } }
        ],
        label: { color: '#8b9dc5', fontSize: 10 },
        labelLine: { lineStyle: { color: '#3d4f6f' } },
        emphasis: { scaleSize: 6 }
      }]
    })
  }
}

// 屏幕自适应
const handleResize = () => {
  alarmTrendChartInstance?.resize()
  alarmTypeChartInstance?.resize()
  riskChartInstance?.resize()
  mapInstance?.resize()
}

// 初始化高德地图
const initAMap = () => {
  const mapContainer = document.getElementById('large-screen-map')
  if (!mapContainer) {
    setTimeout(initAMap, 500)
    return
  }

  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: '330a3d930b185c5b005d31e90fa80b6e'
  }

  AMapLoader.load({
    key: '775f302305d6bbbf2da5ec525db91c74',
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Marker', 'AMap.InfoWindow']
  }).then((AMap) => {
    mapInstance = new AMap.Map('large-screen-map', {
      zoom: 13,
      center: MAP_CENTER,
      mapStyle: 'amap://styles/dark',
      features: ['bg', 'road', 'building', 'point'],
      viewMode: '2D'
    })

    mapInstance.addControl(new AMap.Scale())
    mapInstance.addControl(new AMap.ToolBar({ position: 'RB' }))

    addMapMarkers()
  }).catch((e) => {
    console.error('地图加载失败:', e)
  })
}

// 添加地图标记点
const addMapMarkers = () => {
  if (!mapInstance || !mapPoints.value.length) return

  mapMarkers.forEach(marker => marker.setMap(null))
  mapMarkers = []

  const AMap = (window as any).AMap
  if (!AMap) return

  mapPoints.value.forEach((point) => {
    const color = point.status === 'danger' ? '#ff4d4f' : point.status === 'warning' ? '#fa8c16' : '#52c41a'

    const lng = point.lng || MAP_CENTER[0] + (Math.random() - 0.5) * 0.05
    const lat = point.lat || MAP_CENTER[1] + (Math.random() - 0.5) * 0.05

    const marker = new AMap.Marker({
      position: [lng, lat],
      title: point.name,
      content: `<div style="background: ${color}; width: 14px; height: 14px; border-radius: 50%; border: 2px solid rgba(255,255,255,0.8); box-shadow: 0 0 12px ${color};"></div>`
    })

    const infoWindow = new AMap.InfoWindow({
      content: `<div style="padding: 8px; font-size: 13px;"><b>${point.name}</b><br/>状态: ${point.status === 'danger' ? '告警' : point.status === 'warning' ? '预警' : '正常'}</div>`,
      offset: new AMap.Pixel(0, -20)
    })

    marker.on('click', () => {
      infoWindow.open(mapInstance, [lng, lat])
    })

    marker.setMap(mapInstance)
    mapMarkers.push(marker)
  })

  if (mapMarkers.length > 0) {
    mapInstance.setFitView(mapMarkers, false, [50, 50, 50, 50])
  }
}

let timer: any = null
let refreshTimer: any = null

onMounted(() => {
  updateDateTime()
  timer = setInterval(updateDateTime, 1000)

  setTimeout(() => {
    initAMap()
  }, 300)

  loadData().then(() => {
    setTimeout(() => {
      initCharts()
      if (mapInstance) {
        addMapMarkers()
      }
    }, 100)
  })

  // 每30秒刷新数据
  refreshTimer = setInterval(loadData, 30000)

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (refreshTimer) clearInterval(refreshTimer)
  alarmTrendChartInstance?.dispose()
  alarmTypeChartInstance?.dispose()
  riskChartInstance?.dispose()
  mapInstance?.destroy()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
$bg-primary: #0a1628;
$bg-secondary: #0d1f3c;
$bg-card: #0f2747;
$border-color: #1e3a5f;
$text-primary: #ffffff;
$text-secondary: #8b9dc5;
$accent-blue: #3b82f6;
$accent-red: #ff6b6b;
$accent-yellow: #feca57;
$accent-green: #1dd1a1;
$glow-blue: rgba(59, 130, 246, 0.15);

.large-screen {
  width: 100vw;
  height: 100vh;
  background: $bg-primary;
  color: $text-primary;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif;
}

// ====== Header ======
.header {
  height: 60px;
  background: linear-gradient(180deg, rgba(13, 31, 60, 0.95) 0%, rgba(10, 22, 40, 0.98) 100%);
  border-bottom: 1px solid $accent-blue;
  box-shadow: 0 2px 20px rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, $accent-blue, transparent);
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    .logo { height: 36px; width: auto; }
    .title-wrapper {
      .title-row {
        display: flex;
        align-items: center;
      }
      .title {
        font-size: 22px;
        font-weight: bold;
        background: linear-gradient(90deg, #fff 0%, #7ec8f8 50%, #54a0ff 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        margin: 0;
        letter-spacing: 3px;
        animation: shimmerTitle 4s ease-in-out infinite;
      }
      .subtitle { font-size: 10px; color: $text-secondary; letter-spacing: 1px; margin-top: 2px; opacity: 0.7; }
    }
  }

  .header-center {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);

    .alert-banner {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 6px 24px;
      background: linear-gradient(135deg, rgba(255, 77, 79, 0.25), rgba(255, 77, 79, 0.08));
      border: 1px solid rgba(255, 77, 79, 0.4);
      border-radius: 20px;
      animation: alertGlow 2s ease-in-out infinite;

      .alert-pulse {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: #ff4d4f;
        animation: pulse 1.5s ease-in-out infinite;
      }

      .alert-text {
        font-size: 13px;
        color: #ff8a8a;
        font-weight: 500;
        letter-spacing: 1px;
      }
    }

    .datetime {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 14px;
      .date { color: $text-secondary; }
      .time { font-size: 20px; font-weight: bold; color: $accent-blue; }
      .week { color: $text-secondary; }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 14px;
    position: relative;
    z-index: 2;

    .datetime-right {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      .date { color: $text-secondary; }
      .time { font-size: 14px; font-weight: bold; color: $accent-blue; }
      .week { color: $text-secondary; }
    }

    .weather {
      display: flex;
      align-items: center;
      gap: 6px;
      color: $text-secondary;
      font-size: 13px;
    }
    .admin-btn {
      background: linear-gradient(135deg, $accent-blue 0%, #2563eb 100%);
      border: none;
      padding: 8px 16px;
      font-size: 13px;
      box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
    }
    .fullscreen-btn {
      background: $bg-card;
      border: 1px solid $border-color;
      color: $text-secondary;
      &:hover { border-color: $accent-blue; color: $accent-blue; }
    }
  }
}

// ====== Main Layout ======
.main-content {
  flex: 1;
  display: flex;
  padding: 10px;
  gap: 10px;
  min-height: 0;
}

.left-panel, .right-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: $border-color; border-radius: 2px; }
}

.center-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  min-height: 0;
}

// ====== Panel Card ======
.panel-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: border-color 0.3s;

  &:hover {
    border-color: rgba($accent-blue, 0.4);
  }

  .card-header {
    padding: 10px 14px;
    background: linear-gradient(135deg, rgba($accent-blue, 0.12) 0%, transparent 100%);
    border-bottom: 1px solid $border-color;
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: linear-gradient(180deg, $accent-blue, transparent);
      border-radius: 0 2px 2px 0;
    }

    .card-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
      .el-icon { color: $accent-blue; font-size: 16px; }
    }

    .alarm-count {
      background: $accent-red;
      color: #fff;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 12px;
      animation: pulse 2s infinite;
    }
  }

  .card-content {
    padding: 10px;
    overflow: hidden;
  }
}

// ====== Overview ======
.overview-content {
  .overview-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 6px;
    .ov-item {
      text-align: center;
      padding: 8px 4px;
      background: rgba($accent-blue, 0.06);
      border-radius: 6px;
      border: 1px solid rgba($border-color, 0.5);
      .ov-value {
        font-size: 22px;
        font-weight: bold;
        &.blue { color: $accent-blue; }
        &.green { color: $accent-green; }
        &.orange { color: #fa8c16; }
        &.red { color: $accent-red; }
      }
      .ov-label { font-size: 11px; color: $text-secondary; margin-top: 2px; }
    }
  }
}

// ====== Device Stats ======
.device-total-hero {
  text-align: center;
  padding: 4px 0 10px;
  border-bottom: 1px solid $border-color;
  margin-bottom: 10px;
  .total-num {
    font-size: 36px;
    font-weight: bold;
    background: linear-gradient(180deg, #fff, $accent-blue);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: 2px;
  }
  .total-unit { font-size: 13px; color: $text-secondary; margin-left: 4px; }
}

.device-stats {
  .stat-item {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
    gap: 8px;
    .stat-label { min-width: 35px; color: $text-secondary; font-size: 12px; }
    .stat-bar-wrap {
      flex: 1;
      height: 8px;
      background: rgba($border-color, 0.3);
      border-radius: 4px;
      overflow: hidden;
    }
    .stat-bar {
      height: 100%;
      border-radius: 4px;
      transition: width 0.6s ease;
    }
    .stat-value { min-width: 30px; font-size: 14px; font-weight: bold; text-align: right; }
    &.normal { .stat-bar { background: linear-gradient(90deg, $accent-green, #10ac84); } .stat-value { color: $accent-green; } }
    &.warning { .stat-bar { background: linear-gradient(90deg, $accent-red, #ee5a5a); } .stat-value { color: $accent-red; } }
    &.fault { .stat-bar { background: linear-gradient(90deg, $accent-yellow, #f8b739); } .stat-value { color: $accent-yellow; } }
    &.offline { .stat-bar { background: linear-gradient(90deg, #6b7b9a, #8b9dc5); } .stat-value { color: $text-secondary; } }
  }
}

// ====== Facility List ======
.facility-list {
  .facility-item {
    display: flex;
    align-items: center;
    padding: 6px 0;
    border-bottom: 1px solid rgba($border-color, 0.5);
    &:last-child { border-bottom: none; }
    .facility-icon {
      width: 30px;
      height: 30px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 14px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }
    .facility-info {
      flex: 1;
      margin-left: 10px;
      .facility-name { font-size: 13px; color: $text-primary; }
      .facility-count { font-size: 11px; color: $text-secondary; margin-top: 1px; }
    }
    .facility-percent { font-size: 14px; font-weight: bold; color: $accent-blue; }
  }
}

// ====== Alarm Card ======
.alarm-card {
  &:has(.alarm-item) {
    border-color: rgba($accent-red, 0.3);
  }
}

.alarm-list {
  max-height: 150px;
  overflow-y: auto;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: $border-color; border-radius: 2px; }

  .alarm-item {
    display: flex;
    align-items: flex-start;
    padding: 8px;
    margin-bottom: 6px;
    background: rgba($accent-red, 0.08);
    border-radius: 6px;
    border-left: 3px solid $accent-red;
    transition: background 0.2s;

    &:hover { background: rgba($accent-red, 0.15); }
    &.level-critical { border-left-color: $accent-red; background: rgba($accent-red, 0.12); }
    &.level-major { border-left-color: $accent-yellow; background: rgba($accent-yellow, 0.08); }

    .alarm-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: $accent-red;
      margin-top: 5px;
      margin-right: 8px;
      flex-shrink: 0;
      animation: blink 2s ease-in-out infinite;
    }

    .alarm-info {
      flex: 1;
      .alarm-title { font-size: 12px; color: $text-primary; font-weight: 500; }
      .alarm-desc { font-size: 11px; color: $text-secondary; margin-top: 2px; }
      .alarm-time { font-size: 10px; color: $text-secondary; margin-top: 2px; }
    }
    .alarm-level {
      font-size: 11px;
      padding: 2px 6px;
      border-radius: 4px;
      background: rgba($accent-red, 0.2);
      color: $accent-red;
      white-space: nowrap;
    }
  }
  .no-data {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 30px;
    color: $accent-green;
    .el-icon { font-size: 40px; margin-bottom: 8px; }
  }
}

// ====== Map ======
.map-container {
  flex: 1;
  min-height: 220px;
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;

  .map-header {
    padding: 8px 14px;
    background: linear-gradient(135deg, rgba($accent-blue, 0.12) 0%, transparent 100%);
    border-bottom: 1px solid $border-color;
    .map-title { text-align: center; font-size: 14px; font-weight: bold; color: $text-primary; margin-bottom: 6px; }
    .map-stats {
      display: flex;
      justify-content: center;
      gap: 24px;
      .map-stat-item {
        text-align: center;
        .map-stat-value { font-size: 18px; font-weight: bold; color: $accent-blue; }
        .map-stat-label { font-size: 10px; color: $text-secondary; margin-top: 2px; }
      }
      .alarm-stat .map-stat-value { color: $accent-red; }
    }
  }

  .amap-container { flex: 1; width: 100%; min-height: 200px; height: 0; }

  .map-legend {
    position: absolute;
    bottom: 10px;
    left: 10px;
    background: rgba($bg-card, 0.92);
    padding: 6px 12px;
    border-radius: 6px;
    display: flex;
    gap: 12px;
    z-index: 10;
    border: 1px solid rgba($border-color, 0.5);
    backdrop-filter: blur(4px);
    .legend-item {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 11px;
      color: $text-secondary;
      .legend-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        display: inline-block;
        box-shadow: 0 0 6px currentColor;
      }
    }
  }
}

// ====== Video Panel ======
.video-panel {
  flex-shrink: 0;
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: 8px;
  overflow: hidden;
  .panel-header {
    padding: 8px 12px;
    background: linear-gradient(135deg, rgba($accent-blue, 0.12) 0%, transparent 100%);
    border-bottom: 1px solid $border-color;
    display: flex;
    align-items: center;
    justify-content: space-between;
    .panel-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 500;
      color: $text-primary;
      .el-icon { color: $accent-blue; }
    }
    .panel-actions { display: flex; gap: 4px; }
  }
  .video-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; padding: 6px; }
  .video-item {
    background: #050d18;
    border-radius: 4px;
    overflow: hidden;
    border: 1px solid rgba($border-color, 0.5);
    .video-header {
      padding: 4px 8px;
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 10px;
      color: $text-secondary;
      background: rgba(0, 0, 0, 0.3);
      .el-icon { color: $accent-green; font-size: 10px; }
    }
    .video-content {
      height: 55px;
      display: flex;
      align-items: center;
      justify-content: center;
      .video-placeholder {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 2px;
        .camera-icon { font-size: 18px; color: $accent-green; opacity: 0.5; }
        .live-text { font-size: 9px; color: $accent-green; font-weight: bold; opacity: 0.8; }
      }
    }
    .video-footer {
      padding: 2px 8px;
      display: flex;
      justify-content: flex-end;
      font-size: 9px;
      color: $text-secondary;
      background: rgba(0, 0, 0, 0.2);
    }
  }
}

// ====== Bottom Stats ======
.bottom-stats {
  display: flex;
  gap: 10px;
  .bottom-stat-card {
    flex: 1;
    background: $bg-card;
    border: 1px solid $border-color;
    border-radius: 8px;
    padding: 14px;
    display: flex;
    align-items: center;
    gap: 12px;
    transition: all 0.3s;
    &:hover {
      border-color: rgba($accent-blue, 0.4);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }
    .stat-icon {
      width: 42px;
      height: 42px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: #fff;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
    }
    .stat-info {
      .stat-value { font-size: 22px; font-weight: bold; color: $text-primary; }
      .stat-label { font-size: 11px; color: $text-secondary; margin-top: 2px; }
    }
  }
}

// ====== Charts & Duty ======
.chart-container { width: 100%; height: 120px; }

.duty-info {
  .duty-item {
    display: flex;
    align-items: center;
    padding: 6px 0;
    border-bottom: 1px solid rgba($border-color, 0.5);
    &:last-child { border-bottom: none; }
    .duty-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: linear-gradient(135deg, $accent-blue, #2563eb);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      color: #fff;
    }
    .duty-detail {
      flex: 1;
      margin-left: 10px;
      .duty-name { font-size: 13px; }
      .duty-time { font-size: 11px; color: $text-secondary; margin-top: 1px; }
    }
    .duty-status {
      font-size: 11px;
      padding: 3px 10px;
      border-radius: 10px;
      &.ongoing { background: rgba($accent-green, 0.15); color: $accent-green; }
      &.pending { background: rgba($accent-yellow, 0.15); color: $accent-yellow; }
      &.completed { background: rgba($text-secondary, 0.15); color: $text-secondary; }
    }
  }
  .no-data { text-align: center; padding: 20px; color: $text-secondary; }
}

// ====== Animations ======
@keyframes shimmerTitle {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.85; }
}

@keyframes alertGlow {
  0%, 100% { box-shadow: 0 0 8px rgba(255, 77, 79, 0.2); }
  50% { box-shadow: 0 0 20px rgba(255, 77, 79, 0.35); }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.6; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

:deep(.amap-container) {
  .amap-maps { width: 100% !important; height: 100% !important; }
}
</style>
