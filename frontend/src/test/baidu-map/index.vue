<template>
  <div class="duty-screen">
    <!-- API加载错误提示 -->
    <div class="api-error-bar" v-if="loadErrors.length > 0">
      <span>接口加载失败: {{ loadErrors.join(', ') }}，请检查后端服务是否正常及权限配置</span>
      <el-button size="small" type="danger" text @click="loadErrors = []">关闭</el-button>
    </div>
    <header class="header">
      <div class="header-left">
        <h1 class="title">安为智慧消防值守平台</h1>
        <p class="subtitle">ANWEI SMART FIRE DUTY PLATFORM</p>
      </div>
      <div class="header-center">
        <div class="alert-banner" v-if="alarmList.length > 0">
          <span class="alert-pulse"></span>
          <span class="alert-text">实时告警中 有{{ alarmList.length }}条待处理</span>
        </div>
        <div class="datetime" v-else>
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
          <span class="week">{{ currentWeek }}</span>
        </div>
      </div>
      <div class="header-right">
        <div class="datetime-compact" v-if="alarmList.length > 0">
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
          <span class="week">{{ currentWeek }}</span>
        </div>
        <el-button type="primary" class="back-btn" @click="goToAdmin"><el-icon><Setting /></el-icon>管理后台</el-button>
        <el-button class="fullscreen-btn" @click="toggleFullscreen"><el-icon><FullScreen /></el-icon></el-button>
      </div>
    </header>

    <main class="main-content">
      <!-- ====== 左侧面板 ====== -->
      <aside class="left-panel">
        <!-- 企业信息 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title"><el-icon><OfficeBuilding /></el-icon>企业总览</span>
            <span class="ent-total-badge">{{ propertyList.length }}家</span>
          </div>
          <div class="card-content ent-content">
            <div class="ent-name-list">
              <div class="ent-name-item" v-for="p in displayProperties" :key="p.id" @click="openPropertyDetail(p)">
                <span class="eni-dot" :class="'risk-dot-' + (p.riskLevel || 'normal')"></span>
                <span class="eni-name">{{ p.name }}</span>
                <span class="eni-risk" :class="'risk-' + p.riskLevel">{{ riskLevelText(p.riskLevel) }}</span>
              </div>
            </div>
            <div class="ent-view-all" v-if="propertyList.length > maxDisplayCount" @click="goToAdmin">
              <el-icon><ArrowRight /></el-icon>
              <span>查看全部{{ propertyList.length }}家企业</span>
            </div>
          </div>
        </div>

        <!-- 设备状态统计 -->
        <div class="panel-card">
          <div class="card-header"><span class="card-title"><el-icon><Monitor /></el-icon>设备状态统计</span></div>
          <div class="card-content">
            <div class="device-hero">
              <span class="hero-num">{{ deviceStats.total }}</span>
              <span class="hero-unit">台设备</span>
            </div>
            <div class="device-bars">
              <div class="bar-item normal">
                <span class="bar-label">正常</span>
                <div class="bar-track"><div class="bar-fill" :style="{ width: pct(deviceStats.normal, deviceStats.total) + '%' }"></div></div>
                <span class="bar-val">{{ deviceStats.normal }}</span>
              </div>
              <div class="bar-item alarm">
                <span class="bar-label">告警</span>
                <div class="bar-track"><div class="bar-fill" :style="{ width: pct(deviceStats.alarm, deviceStats.total) + '%' }"></div></div>
                <span class="bar-val">{{ deviceStats.alarm }}</span>
              </div>
              <div class="bar-item fault">
                <span class="bar-label">故障</span>
                <div class="bar-track"><div class="bar-fill" :style="{ width: pct(deviceStats.fault, deviceStats.total) + '%' }"></div></div>
                <span class="bar-val">{{ deviceStats.fault }}</span>
              </div>
              <div class="bar-item offline">
                <span class="bar-label">离线</span>
                <div class="bar-track"><div class="bar-fill" :style="{ width: pct(deviceStats.offline, deviceStats.total) + '%' }"></div></div>
                <span class="bar-val">{{ deviceStats.offline }}</span>
              </div>
              <div class="bar-item inactive">
                <span class="bar-label">未激活</span>
                <div class="bar-track"><div class="bar-fill" :style="{ width: pct(deviceStats.inactive, deviceStats.total) + '%' }"></div></div>
                <span class="bar-val">{{ deviceStats.inactive }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 设施分类 -->
        <div class="panel-card">
          <div class="card-header"><span class="card-title"><el-icon><Grid /></el-icon>设施分类</span></div>
          <div class="card-content">
            <div class="facility-list">
              <div class="fac-item" v-for="f in facilityStats" :key="f.name">
                <div class="fac-icon" :style="{ background: f.color }"><el-icon><component :is="f.icon" /></el-icon></div>
                <div class="fac-info"><div class="fac-name">{{ f.name }}</div><div class="fac-count">{{ f.count }}台</div></div>
                <div class="fac-pct">{{ pct(f.count, deviceStats.total) }}%</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 物业单位 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title"><el-icon><HomeFilled /></el-icon>物业单位<span class="badge">{{ filteredProperties.length }}</span></span>
            <div class="view-toggle">
              <el-icon :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'"><Grid /></el-icon>
              <el-icon :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'"><List /></el-icon>
            </div>
          </div>
          <div class="card-content prop-content">
            <div class="filter-inline">
              <el-select v-model="filterDistrict" placeholder="全部区域" size="small" clearable style="width:110px" @change="onFilterChange">
                <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d" />
              </el-select>
              <el-select v-model="filterRisk" placeholder="全部等级" size="small" clearable style="width:110px" @change="onFilterChange">
                <el-option label="重大风险" value="critical" /><el-option label="较大风险" value="major" /><el-option label="一般风险" value="normal" />
              </el-select>
            </div>
            <div v-if="viewMode === 'card'" class="prop-grid">
              <div class="prop-card" v-for="p in filteredProperties" :key="p.id" :class="'risk-' + p.riskLevel" @click="openPropertyDetail(p)">
                <div class="pc-name">{{ p.name }}</div>
                <div class="pc-risk">{{ riskLevelText(p.riskLevel) }}</div>
                <div class="pc-count">{{ p.deviceCount || 0 }}台设备</div>
              </div>
            </div>
            <div v-else class="prop-list">
              <div class="prop-row" v-for="p in filteredProperties" :key="p.id" @click="openPropertyDetail(p)">
                <span class="pr-name">{{ p.name }}</span>
                <span class="pr-info"><i :class="'risk-text-' + p.riskLevel">{{ riskLevelText(p.riskLevel) }}</i> {{ p.deviceCount || 0 }}台</span>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- ====== 中间面板 ====== -->
      <section class="center-panel">
        <!-- 地图 -->
        <div class="map-wrapper">
          <div class="map-bar">
            <span class="map-label">部件分布</span>
            <div class="map-summary">
              <div class="ms-item"><span class="ms-v">{{ propertyList.length || 1 }}</span><span class="ms-l">联网单位</span></div>
              <div class="ms-item"><span class="ms-v">{{ deviceStats.total }}</span><span class="ms-l">接入设备</span></div>
              <div class="ms-item"><span class="ms-v green-v">{{ deviceStats.normal }}</span><span class="ms-l">正常运行</span></div>
              <div class="ms-item"><span class="ms-v alarm-v">{{ alarmList.length }}</span><span class="ms-l">告警数量</span></div>
            </div>
          </div>
          <div id="duty-map" class="map-canvas"></div>
          <div class="map-legend">
            <span class="lg-item"><i style="background:#ff4d4f"></i>告警</span>
            <span class="lg-item"><i style="background:#fa8c16"></i>较大风险</span>
            <span class="lg-item"><i style="background:#52c41a"></i>正常</span>
            <span class="lg-item"><i style="background:#8c8c8c"></i>离线</span>
          </div>
        </div>

        <!-- 视频监控 -->
        <div class="video-panel">
          <div class="video-header-bar">
            <span class="card-title"><el-icon><VideoCamera /></el-icon>视频监控</span>
            <span class="video-count">{{ cameraList.length }}路</span>
          </div>
          <div class="video-grid">
            <div class="video-item" v-for="(cam, idx) in cameraList" :key="idx">
              <div class="video-top"><el-icon><VideoCamera /></el-icon><span>{{ cam.name }}</span></div>
              <div class="video-body"><div class="video-placeholder"><el-icon class="cam-icon"><VideoCamera /></el-icon><span class="live-tag">LIVE</span></div></div>
            </div>
          </div>
        </div>

        <!-- 底部图表行 -->
        <div class="bottom-row">
          <div class="panel-card third">
            <div class="card-header"><span class="card-title"><el-icon><Histogram /></el-icon>区域告警分布</span></div>
            <div class="card-content"><div ref="areaChartRef" class="chart-box"></div></div>
          </div>
          <div class="panel-card third">
            <div class="card-header"><span class="card-title"><el-icon><PieChart /></el-icon>告警类型分布</span></div>
            <div class="card-content"><div ref="alarmTypeChartRef" class="chart-box"></div></div>
          </div>
          <div class="panel-card third">
            <div class="card-header"><span class="card-title"><el-icon><Monitor /></el-icon>设备状态</span></div>
            <div class="card-content"><div ref="deviceChartRef" class="chart-box"></div></div>
          </div>
        </div>
      </section>

      <!-- ====== 右侧面板 ====== -->
      <aside class="right-panel">
        <!-- 今日警情 -->
        <div class="panel-card">
          <div class="card-header"><span class="card-title"><el-icon><Warning /></el-icon>今日警情</span></div>
          <div class="card-content">
            <div class="stat-grid">
              <div class="stat-cell total"><div class="sv">{{ todayAlarm.totalAlarms }}</div><div class="sl">告警总数</div></div>
              <div class="stat-cell handled"><div class="sv">{{ todayAlarm.handledCount }}</div><div class="sl">已处置</div></div>
              <div class="stat-cell pending"><div class="sv">{{ todayAlarm.pendingCount }}</div><div class="sl">待处理</div></div>
              <div class="stat-cell rate"><div class="sv">{{ todayAlarm.handleRate }}%</div><div class="sl">处置率</div></div>
            </div>
          </div>
        </div>

        <!-- 实时告警 -->
        <div class="panel-card alarm-panel">
          <div class="card-header"><span class="card-title"><el-icon><Bell /></el-icon>实时告警<span class="badge red" v-if="alarmList.length">{{ alarmList.length }}</span></span></div>
          <div class="card-content alarm-scroll">
            <div class="alarm-inner" :class="{ scrolling: alarmList.length > 3 }" @mouseenter="scrollPaused = true" @mouseleave="scrollPaused = false">
              <div class="alarm-item" v-for="alarm in alarmList" :key="alarm.id" :class="'level-' + alarm.alarmLevel" @click="viewAlarm(alarm)">
                <div class="alarm-dot"></div>
                <div class="alarm-body">
                  <div class="alarm-desc">{{ extractAlarmSummary(alarm.alarmDescription) }}</div>
                  <div class="alarm-time">{{ formatTime(alarm.alarmTime) }}</div>
                </div>
                <span class="alarm-tag" :class="'tag-' + alarm.alarmLevel">{{ alarmLevelText(alarm.alarmLevel) }}</span>
              </div>
            </div>
            <div class="empty" v-if="!alarmList.length"><el-icon><CircleCheck /></el-icon>暂无告警</div>
          </div>
        </div>

        <!-- 告警趋势 -->
        <div class="panel-card">
          <div class="card-header"><span class="card-title"><el-icon><TrendCharts /></el-icon>告警趋势(近7天)</span></div>
          <div class="card-content"><div ref="trendChartRef" class="chart-box"></div></div>
        </div>

        <!-- 巡检任务 -->
        <div class="panel-card">
          <div class="card-header"><span class="card-title"><el-icon><Checked /></el-icon>巡检任务</span></div>
          <div class="card-content patrol-content">
            <div class="patrol-summary">
              <div class="ps-item"><span class="ps-v blue-v">{{ patrolStats.total }}</span><span class="ps-l">总任务</span></div>
              <div class="ps-item"><span class="ps-v green-v">{{ patrolStats.completed }}</span><span class="ps-l">已完成</span></div>
              <div class="ps-item"><span class="ps-v orange-v">{{ patrolStats.pending }}</span><span class="ps-l">进行中</span></div>
            </div>
            <div class="patrol-list">
              <div class="patrol-item" v-for="p in patrolList" :key="p.id">
                <div class="patrol-status-dot" :class="p.status"></div>
                <div class="patrol-info">
                  <div class="patrol-name">{{ p.taskName }}</div>
                  <div class="patrol-meta">{{ p.inspectorName }} | {{ p.planTime }}</div>
                </div>
                <span class="patrol-tag" :class="p.status">{{ patrolStatusText(p.status) }}</span>
              </div>
            </div>
            <div class="empty small" v-if="!patrolList.length">暂无巡检任务</div>
          </div>
        </div>

        <!-- 值班信息 -->
        <div class="panel-card">
          <div class="card-header"><span class="card-title"><el-icon><User /></el-icon>今日值班</span></div>
          <div class="card-content duty-content">
            <div class="duty-row" v-for="d in dutyList" :key="d.id">
              <div class="duty-avatar"><el-icon><UserFilled /></el-icon></div>
              <div class="duty-info"><div class="duty-name">{{ d.userName }}</div><div class="duty-shift">{{ d.shiftName }} {{ d.startTime }}-{{ d.endTime }}</div></div>
              <span class="duty-tag" :class="d.status">{{ dutyStatusText(d.status) }}</span>
            </div>
            <div class="empty small" v-if="!dutyList.length">暂无值班</div>
          </div>
        </div>
      </aside>
    </main>

    <!-- 物业详情弹窗 -->
    <el-dialog v-model="propDialogVisible" :title="selectedProperty?.name" width="460px" class="dark-dialog" :append-to-body="true">
      <div class="detail-body" v-if="selectedProperty">
        <div class="dr"><span class="dl">地址</span><span class="dv">{{ selectedProperty.address || '-' }}</span></div>
        <div class="dr"><span class="dl">联系人</span><span class="dv">{{ selectedProperty.contactName || '-' }}</span></div>
        <div class="dr"><span class="dl">联系电话</span><span class="dv">{{ selectedProperty.contactPhone || '-' }}</span></div>
        <div class="dr"><span class="dl">风险等级</span><span class="dv" :class="'risk-' + selectedProperty.riskLevel">{{ riskLevelText(selectedProperty.riskLevel) }}</span></div>
        <div class="dr"><span class="dl">设备数量</span><span class="dv">{{ selectedProperty.deviceCount || 0 }}台</span></div>
        <template v-if="propDevices.length">
          <div class="section-title">设备状态</div>
          <div class="device-status-row">
            <span class="ds-normal">正常 {{ propDevices.filter(d => d.deviceStatus === 'normal').length }}</span>
            <span class="ds-alarm">告警 {{ propDevices.filter(d => d.deviceStatus === 'alarm').length }}</span>
            <span class="ds-fault">故障 {{ propDevices.filter(d => d.deviceStatus === 'fault').length }}</span>
            <span class="ds-offline">离线 {{ propDevices.filter(d => d.deviceStatus === 'offline').length }}</span>
          </div>
        </template>
      </div>
    </el-dialog>

    <!-- 告警详情弹窗 -->
    <el-dialog v-model="alarmDialogVisible" title="告警详情" width="460px" class="dark-dialog" :append-to-body="true">
      <div class="detail-body" v-if="selectedAlarm">
        <div class="dr"><span class="dl">设备</span><span class="dv">{{ selectedAlarm.deviceName }}</span></div>
        <div class="dr"><span class="dl">类型</span><span class="dv">{{ selectedAlarm.alarmTypeName || alarmTypeText(selectedAlarm.alarmType) }}</span></div>
        <div class="dr"><span class="dl">级别</span><span class="dv" :class="'level-' + selectedAlarm.alarmLevel">{{ alarmLevelText(selectedAlarm.alarmLevel) }}</span></div>
        <div class="dr"><span class="dl">时间</span><span class="dv">{{ formatTime(selectedAlarm.alarmTime) }}</span></div>
        <div class="dr"><span class="dl">位置</span><span class="dv">{{ selectedAlarm.alarmLocation || '-' }}</span></div>
        <div class="dr"><span class="dl">描述</span><span class="dv">{{ selectedAlarm.alarmDescription || '-' }}</span></div>
      </div>
    </el-dialog>

    <!-- 报警弹窗 -->
    <AlarmPopup
      :show-popup="alarmPopupShow"
      :current-alarm="alarmPopupCurrent"
      :alarm-queue="alarmPopupQueue"
      @dismiss="alarmPopupDismiss"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import request from '@/config/axios'
import AMapLoader from '@amap/amap-jsapi-loader'
import AlarmPopup from '@/components/AlarmPopup/index.vue'
import { useAlarmPopup } from '@/components/AlarmPopup/useAlarmPopup'

const router = useRouter()
const MAP_CENTER = [113.7518, 23.0208]

// Refs
const trendChartRef = ref<HTMLElement>()
const areaChartRef = ref<HTMLElement>()
const deviceChartRef = ref<HTMLElement>()
const alarmTypeChartRef = ref<HTMLElement>()

// Map
let mapInstance: any = null
let mapMarkers: any[] = []
const scrollPaused = ref(false)

// State
const currentDate = ref(''), currentTime = ref(''), currentWeek = ref('')
const enterpriseInfo = ref<any>({})
const propertyList = ref<any[]>([])
// Map markers 不再用 reactive ref 管理，跟监控中心一致：addMarkers(points) 直接传参
const districtOptions = ref<string[]>([])
const filterDistrict = ref('')
const filterRisk = ref('')
const viewMode = ref<'card' | 'list'>('card')
const deviceStats = ref({ total: 0, normal: 0, alarm: 0, fault: 0, offline: 0, inactive: 0 })
const todayAlarm = ref({ totalAlarms: 0, handledCount: 0, pendingCount: 0, handleRate: 0 })
const alarmList = ref<any[]>([])
const dutyList = ref<any[]>([])
const patrolList = ref<any[]>([])
const patrolStats = ref({ total: 0, completed: 0, pending: 0 })
const propDialogVisible = ref(false)
const alarmDialogVisible = ref(false)
const selectedProperty = ref<any>(null)
const selectedAlarm = ref<any>(null)
const propDevices = ref<any[]>([])

// 报警弹窗
const {
  showPopup: alarmPopupShow,
  currentAlarm: alarmPopupCurrent,
  alarmQueue: alarmPopupQueue,
  dismissPopup: alarmPopupDismiss,
  checkNewAlarms: alarmPopupCheckNew,
  cleanup: alarmPopupCleanup,
} = useAlarmPopup()

// Facility stats（从后端 deviceCategoryDistribution 动态生成，初始为空）
const facilityStats = ref<any[]>([])

// Camera list
const cameraList = ref<any[]>([])

// 缓存图表数据（loadData 先于 initCharts 执行，需要缓存数据供图表初始化后刷新）
let cachedAlarmTrend: any[] = []
let cachedDeviceCategory: any[] = []
let cachedAlarmTypeDist: any[] = []

// Charts
let trendChart: ECharts | null = null
let areaChart: ECharts | null = null
let deviceChart: ECharts | null = null
let alarmTypeChart: ECharts | null = null

// Sound
let audioCtx: AudioContext | null = null
let lastSoundTime = 0
const playAlarmSound = () => {
  const now = Date.now()
  if (now - lastSoundTime < 10000) return
  lastSoundTime = now
  try {
    if (!audioCtx) audioCtx = new AudioContext()
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.connect(gain); gain.connect(audioCtx.destination)
    osc.frequency.value = 880; osc.type = 'sine'; gain.gain.value = 0.3
    osc.start(); gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5); osc.stop(audioCtx.currentTime + 0.5)
  } catch {}
}

// Computed
const maxDisplayCount = 6
const displayProperties = computed(() => propertyList.value.slice(0, maxDisplayCount))
const filteredProperties = computed(() => {
  let list = propertyList.value
  if (filterDistrict.value) list = list.filter(p => p.district === filterDistrict.value)
  if (filterRisk.value) list = list.filter(p => p.riskLevel === filterRisk.value)
  return list
})

// Helpers
const pct = (v: number, t: number) => t > 0 ? Math.round((v / t) * 100) : 0

// 动态生成设施分类统计（从后端设备种类数据，与数据库一致）
const buildFacilityStats = (categoryData: any[]) => {
  const iconMap: Record<string, string> = { '烟感': 'Smoking', '温感': 'Sunrise', '灭火': 'FirstAidKit', '消防栓': 'Aim', '手动': 'Bell' }
  const colorArr = [
    'linear-gradient(135deg, #ff7a45, #ff4d4f)',
    'linear-gradient(135deg, #ffa940, #fa8c16)',
    'linear-gradient(135deg, #36cfc9, #13c2c2)',
    'linear-gradient(135deg, #597ef7, #2f54eb)',
    'linear-gradient(135deg, #ff85c0, #eb2f96)',
    'linear-gradient(135deg, #95de64, #52c41a)',
    'linear-gradient(135deg, #ffd666, #faad14)'
  ]
  facilityStats.value = categoryData.map((item: any, idx: number) => {
    const name = item.categoryName || item.categoryCode || '其他'
    const icon = Object.entries(iconMap).find(([k]) => name.includes(k))?.[1] || 'Monitor'
    return { name, count: item.count || 0, color: colorArr[idx % colorArr.length], icon }
  })
}
const updateDateTime = () => {
  const now = new Date()
  const w = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六']
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentWeek.value = w[now.getDay()]
}
const formatTime = (t: any) => {
  if (!t) return '-'
  if (typeof t === 'number') return new Date(t).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  const s = String(t)
  return s ? s.replace('T', ' ').substring(0, 19) : '-'
}
const alarmLevelText = (l: string) => ({ critical: '严重', major: '较大', warning: '一般', info: '提示' }[l] || '一般')
const alarmTypeText = (t: string) => ({ smoke: '烟感', temperature: '温感', manual: '手动', composite: '复合' }[t] || '其他')
const riskLevelText = (l: string) => ({ critical: '重大风险', major: '较大风险', normal: '一般风险', minor: '较小风险' }[l] || '未评估')
const dutyStatusText = (s: string) => ({ ongoing: '值班中', pending: '待值班', completed: '已完成' }[s] || s)
const patrolStatusText = (s: string) => ({ completed: '已完成', in_progress: '进行中', pending: '待巡检', overdue: '已逾期' }[s] || s)

/** 从冗长的告警内容中提取关键摘要信息 */
const extractAlarmSummary = (desc: string) => {
  if (!desc) return '设备告警'
  // 格式1: "⚠️ 警报！JF-01烟感探测器：部件电源故障；设备：..."
  const alertMatch = desc.match(/警报！(.+?)(；|$)/)
  if (alertMatch) return alertMatch[1].trim()
  // 格式2: "事件类型：部件电源故障；"
  const eventTypeMatch = desc.match(/事件类型：(.+?)(；|$)/)
  if (eventTypeMatch) {
    // 同时提取设备地址
    const addrMatch = desc.match(/设备地址：(.+?)(；|$)/)
    return addrMatch ? `${eventTypeMatch[1].trim()} - ${addrMatch[1].trim()}` : eventTypeMatch[1].trim()
  }
  // 兜底：截取前30字符
  return desc.length > 30 ? desc.substring(0, 30) + '...' : desc
}

const onFilterChange = () => {
  // 筛选变更时，根据当前筛选条件重新渲染地图点位
  const points = filteredProperties.value.filter((p: any) => p.longitude && p.latitude).map((p: any) => ({
    name: p.name, lng: p.longitude, lat: p.latitude, address: p.address,
    color: p.riskLevel === 'critical' ? '#ff4d4f' : p.riskLevel === 'major' ? '#fa8c16' : '#52c41a',
    statusText: riskLevelText(p.riskLevel), deviceCount: p.deviceCount
  }))
  addMarkers(points)
}
const openPropertyDetail = async (p: any) => {
  selectedProperty.value = p; propDevices.value = []; propDialogVisible.value = true
  try { const r = await silentGet('/eiot/firewatch/device/list', { propertyId: p.id }); if (r?.data) propDevices.value = r.data } catch {}
}
const viewAlarm = (a: any) => { selectedAlarm.value = a; alarmDialogVisible.value = true }
const goToAdmin = () => router.push('/eiot/firewatch/enterprise')
const toggleFullscreen = () => { if (!document.fullscreenElement) document.documentElement.requestFullscreen(); else document.exitFullscreen() }

// Data
const silentGet = async (url: string, params?: any) => {
  try {
    const r = await request.get({ url, params })
    console.log('[DutyScreen] GET', url, '=>', JSON.stringify(r)?.substring(0, 500))
    return r
  } catch (e: any) {
    console.error('[DutyScreen] GET', url, 'ERROR:', e?.message || e, e)
    // 在页面上显示加载错误，方便排查
    if (!loadErrors.value.includes(url)) {
      loadErrors.value.push(url)
    }
    return null
  }
}
const loadErrors = ref<string[]>([])

// 初始化空数据 - 当API不可用时使用零值兜底
const loadMockData = () => {
  enterpriseInfo.value = {}
  propertyList.value = []
  districtOptions.value = []
  deviceStats.value = { total: 0, normal: 0, alarm: 0, fault: 0, offline: 0, inactive: 0 }
  todayAlarm.value = { totalAlarms: 0, handledCount: 0, pendingCount: 0, handleRate: 0 }
  alarmList.value = []
  dutyList.value = []
  patrolList.value = []
  patrolStats.value = { total: 0, completed: 0, pending: 0 }
}

const loadData = async () => {
  // 先加载兜底数据
  loadMockData()

  // ========== 1. 统计数据（包含巡检/值班/物业，统一走 dashboard 权限+租户隔离） ==========
  const statsRes = await silentGet('/eiot/firewatch/dashboard/statistics')
  const d = statsRes || {}
  if (statsRes) {
    if (d.deviceStatus) {
      deviceStats.value = {
        total: d.deviceStatus.total || 0,
        normal: d.deviceStatus.normal || 0,
        alarm: d.deviceStatus.alarm || 0,
        fault: d.deviceStatus.fault || 0,
        offline: d.deviceStatus.offline || 0,
        inactive: d.deviceStatus.inactive || 0
      }
      updateDeviceChart()
    }
    if (d.todayAlarm) todayAlarm.value = { totalAlarms: d.todayAlarm.totalAlarms || 0, handledCount: d.todayAlarm.handledCount || 0, pendingCount: d.todayAlarm.pendingCount || 0, handleRate: d.todayAlarm.handleRate ? Math.round(d.todayAlarm.handleRate * 100) / 100 : 0 }
    if (d.realtimeAlarms?.length) alarmList.value = d.realtimeAlarms
    if (d.alarmTrend?.length) {
      cachedAlarmTrend = d.alarmTrend
      updateTrendChart(d.alarmTrend)
    }
    if (d.deviceCategoryDistribution?.length) {
      cachedDeviceCategory = d.deviceCategoryDistribution
      updateAreaChart(d.deviceCategoryDistribution)
      buildFacilityStats(d.deviceCategoryDistribution)
    }
    if (d.alarmTypeDistribution?.length) {
      cachedAlarmTypeDist = d.alarmTypeDistribution
      updateAlarmTypeChart(d.alarmTypeDistribution)
    }

    // ===== 从 statistics 中提取巡检/值班/物业数据（统一租户隔离） =====
    // 巡检任务摘要
    if (d.patrolSummary) {
      patrolStats.value = {
        total: d.patrolSummary.total || 0,
        completed: d.patrolSummary.completed || 0,
        pending: d.patrolSummary.pending || 0
      }
      if (d.patrolSummary.tasks?.length) {
        patrolList.value = d.patrolSummary.tasks.map((i: any) => ({
          id: i.id, taskName: i.taskName || '巡检任务',
          inspectorName: i.inspectorName || '未指定',
          planTime: i.planTime || '',
          status: i.status || 'pending'
        }))
      }
    }
    // 当前值班列表
    if (d.currentDutyList?.length) {
      dutyList.value = d.currentDutyList.slice(0, 3).map((i: any) => ({
        id: i.id, userName: i.userName,
        shiftName: i.shiftName,
        startTime: i.startTime || '08:00', endTime: i.endTime || '18:00',
        status: i.status || 'ongoing'
      }))
    }
    // 物业列表（值守端大屏：admin 可看全部企业）
    if (d.properties?.length) {
      const mapped = d.properties.map((p: any) => ({
        id: p.id,
        name: p.name || '未命名企业',
        district: p.district || (p.address ? p.address.substring(0, 6) : '-'),
        riskLevel: p.riskLevel || 'normal',
        deviceCount: p.deviceCount || 0,
        longitude: p.longitude,
        latitude: p.latitude,
        address: p.address || '-',
        contactName: p.contactName || '-',
        contactPhone: p.contactPhone || '-',
        propertyType: p.propertyType || '-',
        creditCode: p.creditCode || '-'
      }))
      propertyList.value = mapped
      enterpriseInfo.value = mapped[0] || {}
      const ds = new Set<string>()
      mapped.forEach((p: any) => { if (p.district && p.district !== '-') ds.add(p.district) })
      districtOptions.value = Array.from(ds)
    }
  }

  // 设施分类兜底
  if (!facilityStats.value.length) {
    const catRes = await silentGet('/eiot/firewatch/dashboard/device-category')
    if (Array.isArray(catRes) && catRes.length) {
      cachedDeviceCategory = catRes
      updateAreaChart(catRes)
      buildFacilityStats(catRes)
    }
  }

  // ========== 2. 告警列表兜底 ==========
  if (!alarmList.value.length) {
    const r = await silentGet('/eiot/firewatch/dashboard/realtime-alarms', { limit: 15 })
    if (r?.length) alarmList.value = r
  }

  // 从告警列表中提取有关联视频的设备作为摄像头列表
  const videoAlarms = alarmList.value.filter((a: any) => a.videoUrl || a.videoDeviceId)
  cameraList.value = videoAlarms.length
    ? videoAlarms.slice(0, 4).map((a: any) => ({ name: a.deviceName || '监控点', status: 'online' }))
    : []

  // ========== 3. 地图点位（只展示设备点位，不展示企业位置） ==========
  let allMapPoints: any[] = []

  // 获取物业列表（仅用于企业信息展示，不加入地图点位）
  if (!propertyList.value.length) {
    const propRes = await silentGet('/eiot/firewatch/property/page', { pageNo: 1, pageSize: 100 })
    const propList = propRes?.list || (Array.isArray(propRes) ? propRes : [])
    if (propList.length) {
      const mapped = propList.map((p: any) => ({
        id: p.id,
        name: p.propertyName || p.name || '未命名企业',
        district: p.district || (p.address ? p.address.substring(0, 6) : '-'),
        riskLevel: p.riskLevel || 'normal',
        deviceCount: p.deviceStats?.total || p.deviceCount || 0,
        longitude: p.longitude,
        latitude: p.latitude,
        address: p.address || '-',
        contactName: p.contactName || '-',
        contactPhone: p.contactPhone || '-',
        propertyType: p.propertyType || '-',
        creditCode: p.creditCode || '-'
      }))
      propertyList.value = mapped
      enterpriseInfo.value = mapped[0] || {}
      const ds = new Set<string>()
      mapped.forEach((p: any) => { if (p.district && p.district !== '-') ds.add(p.district) })
      districtOptions.value = Array.from(ds)
    }
  }

  // ========== 4. 设备位置（地图只展示设备点位） ==========
  const deviceLocRes = await silentGet('/eiot/firewatch/dashboard/device-locations')
  if (deviceLocRes && Array.isArray(deviceLocRes) && deviceLocRes.length > 0) {
    const stateMap: Record<string, string> = { '0': 'offline', '1': 'normal', '3': 'inactive', '4': 'fault' }
    const statusColorMap: Record<string, string> = { normal: '#52c41a', offline: '#8c8c8c', inactive: '#9ca3af', fault: '#fa8c16', alarm: '#ff4d4f' }
    allMapPoints = deviceLocRes.map((dev: any) => ({
      name: dev.name || dev.dn || '未知设备',
      lng: dev.lon,
      lat: dev.lat,
      address: dev.addr || '',
      color: statusColorMap[stateMap[String(dev.state)] || 'offline'] || '#8c8c8c',
      statusText: { offline: '离线', normal: '正常', inactive: '未激活', fault: '故障', alarm: '告警' }[stateMap[String(dev.state)] || 'offline'] || '离线',
      isDevice: true
    })).filter((p: any) => p.lng && p.lat)
  }

  addMarkers(allMapPoints)

  // ========== 5. 巡检任务兜底（如果 statistics 中没有，用 duty-schedule 接口） ==========
  if (!patrolList.value.length) {
    const patrolRes = await silentGet('/eiot/firewatch/duty-schedule/current-duty')
    if (patrolRes) {
      const arr = Array.isArray(patrolRes) ? patrolRes : [patrolRes]
      dutyList.value = arr.slice(0, 3).map((i: any) => ({
        id: i.id, userName: i.dutyPersonName || i.userName, shiftName: i.shiftName,
        startTime: i.startTime ? String(i.startTime).substring(0, 5) : '08:00', endTime: i.endTime ? String(i.endTime).substring(0, 5) : '18:00',
        status: i.status || 'ongoing'
      }))
    }
  }

  // ========== 6. 值班数据兜底 ==========
  if (!dutyList.value.length) {
    const dutyRes = await silentGet('/eiot/firewatch/duty-schedule/current-duty')
    if (dutyRes) {
      const arr = Array.isArray(dutyRes) ? dutyRes : [dutyRes]
      dutyList.value = arr.slice(0, 3).map((i: any) => ({
        id: i.id, userName: i.dutyPersonName || i.userName, shiftName: i.shiftName,
        startTime: i.startTime ? String(i.startTime).substring(0, 5) : '08:00', endTime: i.endTime ? String(i.endTime).substring(0, 5) : '18:00',
        status: i.status || 'ongoing'
      }))
    }
  }

  if (alarmList.value.some(a => a.orderStatus === 'pending')) playAlarmSound()

  // 检测新报警并触发弹窗
  alarmPopupCheckNew(alarmList.value)
}

// Charts
const updateTrendChart = (data: any[]) => {
  if (!trendChart) return
  trendChart.setOption({
    xAxis: { data: data.map(d => (d.date || '').substring(5)) },
    series: [{ data: data.map(d => d.alarmCount || 0) }, { data: data.map(d => d.handleCount || 0) }]
  })
}
const updateAreaChart = (data: any[]) => {
  if (!areaChart) return
  areaChart.setOption({
    xAxis: { data: data.map(d => d.categoryName || d.categoryCode) },
    series: [{ data: data.map(d => d.count || 0) }]
  })
}
const updateAlarmTypeChart = (data: any[]) => {
  if (!alarmTypeChart) return
  const colors = ['#ff4d4f', '#fa8c16', '#fadb14', '#52c41a', '#1890ff']
  alarmTypeChart.setOption({
    series: [{
      data: data.map((d: any, i: number) => ({
        value: d.count || 0, name: d.typeName || d.alarmType || '其他',
        itemStyle: { color: colors[i % colors.length] }
      }))
    }]
  })
}
const updateDeviceChart = () => {
  if (!deviceChart) return
  const ds = deviceStats.value
  deviceChart.setOption({
    series: [{
      data: [
        { value: ds.normal, name: '正常', itemStyle: { color: '#52c41a' } },
        { value: ds.alarm, name: '告警', itemStyle: { color: '#ff4d4f' } },
        { value: ds.fault, name: '故障', itemStyle: { color: '#fa8c16' } },
        { value: ds.offline, name: '离线', itemStyle: { color: '#8c8c8c' } },
        { value: ds.inactive, name: '未激活', itemStyle: { color: '#d9d9d9' } }
      ]
    }]
  })
}

const initCharts = () => {
  // 告警趋势日期（近7天），初始数据为0，等待API覆盖
  const today = new Date()
  const trendDates: string[] = []
  const trendAlarms: number[] = []
  const trendHandled: number[] = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today); d.setDate(d.getDate() - i)
    trendDates.push(`${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
    trendAlarms.push(0)
    trendHandled.push(0)
  }

  // 告警趋势
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['告警数', '处置数'], textStyle: { color: '#8b9dc5', fontSize: 10 }, top: 0 },
      grid: { left: '10%', right: '5%', top: '22%', bottom: '12%' },
      xAxis: { type: 'category', data: trendDates, axisLine: { lineStyle: { color: '#1e3a5f' } }, axisLabel: { color: '#8b9dc5', fontSize: 10 } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#162d50' } }, axisLabel: { color: '#8b9dc5', fontSize: 10 } },
      series: [
        { name: '告警数', type: 'line', smooth: true, data: trendAlarms, lineStyle: { color: '#ff4d4f', width: 2 }, itemStyle: { color: '#ff4d4f' }, symbolSize: 4, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(255,77,79,0.35)' }, { offset: 1, color: 'rgba(255,77,79,0.02)' }]) } },
        { name: '处置数', type: 'line', smooth: true, data: trendHandled, lineStyle: { color: '#52c41a', width: 2 }, itemStyle: { color: '#52c41a' }, symbolSize: 4 }
      ]
    })
  }
  // 区域告警柱状图
  if (areaChartRef.value) {
    areaChart = echarts.init(areaChartRef.value)
    areaChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '10%', right: '5%', top: '8%', bottom: '22%' },
      xAxis: { type: 'category', data: [], axisLine: { lineStyle: { color: '#1e3a5f' } }, axisLabel: { color: '#8b9dc5', fontSize: 10, rotate: 20 } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#162d50' } }, axisLabel: { color: '#8b9dc5', fontSize: 10 } },
      series: [{ type: 'bar', data: [], barWidth: '40%', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#1890ff' }, { offset: 1, color: '#0050b3' }]), borderRadius: [3, 3, 0, 0] } }]
    })
  }
  // 告警类型饼图
  if (alarmTypeChartRef.value) {
    alarmTypeChart = echarts.init(alarmTypeChartRef.value)
    alarmTypeChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie', radius: ['35%', '62%'], center: ['50%', '50%'],
        data: [
          { value: 0, name: '烟感报警', itemStyle: { color: '#ff4d4f' } },
          { value: 0, name: '温感报警', itemStyle: { color: '#fa8c16' } },
          { value: 0, name: '手动报警', itemStyle: { color: '#fadb14' } },
          { value: 0, name: '设备故障', itemStyle: { color: '#1890ff' } }
        ],
        label: { color: '#8b9dc5', fontSize: 10 },
        labelLine: { lineStyle: { color: '#1e3a5f' } },
        emphasis: { scaleSize: 5 }
      }]
    })
  }
  // 设备状态饼图
  if (deviceChartRef.value) {
    deviceChart = echarts.init(deviceChartRef.value)
    deviceChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie', radius: ['35%', '62%'], center: ['50%', '50%'],
        data: [
          { value: deviceStats.value.normal, name: '正常', itemStyle: { color: '#52c41a' } },
          { value: deviceStats.value.alarm, name: '告警', itemStyle: { color: '#ff4d4f' } },
          { value: deviceStats.value.fault, name: '故障', itemStyle: { color: '#fa8c16' } },
          { value: deviceStats.value.offline, name: '离线', itemStyle: { color: '#8c8c8c' } },
          { value: deviceStats.value.inactive, name: '未激活', itemStyle: { color: '#d9d9d9' } }
        ],
        label: { color: '#8b9dc5', fontSize: 10 },
        labelLine: { lineStyle: { color: '#1e3a5f' } },
        emphasis: { scaleSize: 5 }
      }]
    })
  }
}

// Map
const initMap = () => {
  const el = document.getElementById('duty-map')
  if (!el) { setTimeout(initMap, 500); return }
  ;(window as any)._AMapSecurityConfig = { securityJsCode: '330a3d930b185c5b005d31e90fa80b6e' }
  AMapLoader.load({ key: '775f302305d6bbbf2da5ec525db91c74', version: '2.0', plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Marker', 'AMap.InfoWindow'] })
    .then((AMap: any) => {
      mapInstance = new AMap.Map('duty-map', { zoom: 15, center: MAP_CENTER, mapStyle: 'amap://styles/dark', features: ['bg', 'road', 'building', 'point'], viewMode: '2D' })
      mapInstance.addControl(new AMap.Scale())
      mapInstance.addControl(new AMap.ToolBar({ position: 'RB' }))
    }).catch(() => {})
}

// 跟监控中心完全一致：addMarkers(points) 接收参数，不依赖 reactive ref
const addMarkers = (points: any[]) => {
  if (!mapInstance) return
  mapMarkers.forEach(m => m.setMap(null)); mapMarkers = []
  const AMap = (window as any).AMap; if (!AMap) return

  points.forEach((p: any) => {
    const lng = p.lng || p.longitude
    const lat = p.lat || p.latitude
    if (!lng || !lat) return

    const color = p.color || '#52c41a'
    const statusText = p.statusText || ''
    const marker = new AMap.Marker({
      position: [lng, lat], title: p.name,
      content: `<div style="background:${color};width:16px;height:16px;border-radius:50%;border:2px solid rgba(255,255,255,0.9);box-shadow:0 0 10px ${color};cursor:pointer;"></div>`
    })
    const infoContent = p.isDevice
      ? `<div style="padding:10px;font-size:13px;min-width:150px;"><b>${p.name || ''}</b><br/><span style="color:#999;font-size:12px;">${p.address || ''}</span><br/><span style="color:${color};font-size:12px;">${statusText}</span></div>`
      : `<div style="padding:10px;font-size:13px;min-width:150px;"><b>${p.name || ''}</b><br/><span style="color:#999;font-size:12px;">${p.address || ''}</span><br/><span style="color:${color};font-size:12px;">${statusText}</span>${p.deviceCount ? `<br/><span style="color:#999;font-size:12px;">设备: ${p.deviceCount}台</span>` : ''}</div>`
    const info = new AMap.InfoWindow({ content: infoContent, offset: new AMap.Pixel(0, -20) })
    marker.on('click', () => info.open(mapInstance, [lng, lat]))
    marker.setMap(mapInstance); mapMarkers.push(marker)
  })
  if (mapMarkers.length) mapInstance.setFitView(mapMarkers, false, [50, 50, 50, 50])
  else { mapInstance.setCenter(MAP_CENTER); mapInstance.setZoom(15) }
}

const refreshCharts = () => {
  // loadData 先于 initCharts 执行，此时图表实例已创建，用已加载的数据刷新
  updateDeviceChart()
  if (cachedAlarmTrend?.length) updateTrendChart(cachedAlarmTrend)
  if (cachedDeviceCategory?.length) updateAreaChart(cachedDeviceCategory)
  if (cachedAlarmTypeDist?.length) updateAlarmTypeChart(cachedAlarmTypeDist)
}

const handleResize = () => { trendChart?.resize(); areaChart?.resize(); deviceChart?.resize(); alarmTypeChart?.resize(); mapInstance?.resize() }

let timer: any = null, refreshTimer: any = null
onMounted(() => {
  updateDateTime()
  timer = setInterval(updateDateTime, 1000)
  setTimeout(initMap, 300)
  // 跟监控中心一致：loadData 内部已调用 addMarkers(points)，这里只负责 initCharts
  loadData().then(() => setTimeout(() => { initCharts(); refreshCharts() }, 100))
  refreshTimer = setInterval(loadData, 30000)
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (refreshTimer) clearInterval(refreshTimer)
  trendChart?.dispose(); areaChart?.dispose(); deviceChart?.dispose(); alarmTypeChart?.dispose()
  mapInstance?.destroy()
  if (audioCtx) audioCtx.close()
  window.removeEventListener('resize', handleResize)
  alarmPopupCleanup()
})
</script>

<style lang="scss" scoped>
@use 'sass:color';

// ═══════════════════════════════════════════════════
// DUTY SCREEN — 深海蓝/科技蓝 Holographic Theme
// ═══════════════════════════════════════════════════

// ─── Design Tokens ───
$bg-void:     #00253D;
$bg-deep:     #0D2A4C;
$bg-surface:  #0F3360;
$bg-elevated: #1A4A7F;
$bg-glass:    rgba(15, 51, 96, 0.65);
$border:      #0068AD;
$border-glow: #49CFF7;
$cyan:        #49CFF7;
$cyan-dim:    #2A9FD4;
$electric:    #2A6EB5;
$mint:        #00e6a0;
$ember:       #ff5252;
$amber:       #ffab40;
$gold:        #ffd740;
$slate:       #889BC2;
$ghost:       #4e6a8a;
$tx:          #e8edf5;
$blue:        #2A6EB5;
$red:         #ff4d4f;
$orange:      #fa8c16;
$green:       #52c41a;

.api-error-bar { background: $ember; color: #fff; padding: 6px 16px; font-size: 13px; display: flex; align-items: center; justify-content: center; gap: 12px; flex-shrink: 0; z-index: 999; }

.duty-screen {
  width: 100vw; height: 100vh; overflow: hidden;
  display: flex; flex-direction: column;
  color: $tx;
  font-family: 'Microsoft YaHei', 'PingFang SC', -apple-system, sans-serif;
  -webkit-font-smoothing: antialiased;

  background:
    radial-gradient(ellipse 120% 80% at 50% 0%, rgba($cyan, 0.04) 0%, transparent 60%),
    radial-gradient(ellipse 80% 100% at 0% 100%, rgba(#004080, 0.07) 0%, transparent 50%),
    linear-gradient(180deg, $bg-void 0%, $bg-deep 100%);

  &::before {
    content: ''; position: fixed; inset: 0; z-index: 0; pointer-events: none;
    background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' xmlns='http://www.w3.org/2000/svg'%3E%3Cdefs%3E%3Cpattern id='g' width='60' height='60' patternUnits='userSpaceOnUse'%3E%3Cpath d='M60 0H0v60' fill='none' stroke='%23ffffff' stroke-width='0.3' opacity='0.03'/%3E%3C/pattern%3E%3C/defs%3E%3Crect width='60' height='60' fill='url(%23g)'/%3E%3C/svg%3E");
  }
}

// ====== Header ======
.header {
  height: 56px; flex-shrink: 0; position: relative; z-index: 10;
  background: linear-gradient(135deg, $bg-void 0%, $bg-elevated 100%);
  border-bottom: 1px solid $border;
  box-shadow: 0 2px 20px rgba($cyan, 0.15), 0 1px 0 rgba($cyan, 0.15);
  display: flex; align-items: center; justify-content: space-between; padding: 0 20px;

  &::after { content: ''; position: absolute; bottom: -1px; left: 5%; right: 5%; height: 1px; background: linear-gradient(90deg, transparent, rgba($cyan, 0.5), transparent); }

  .header-left {
    .title {
      font-size: 22px; font-weight: bold;
      background: linear-gradient(90deg, #fff 0%, $cyan 50%, $electric 100%);
      -webkit-background-clip: text; -webkit-text-fill-color: transparent;
      margin: 0; letter-spacing: 3px;
      filter: drop-shadow(0 0 8px rgba($cyan, 0.2));
    }
    .subtitle { font-size: 10px; color: $slate; letter-spacing: 1px; margin-top: 1px; opacity: 0.7; }
  }
  .header-center {
    position: absolute; left: 50%; transform: translateX(-50%);
    .alert-banner {
      display: flex; align-items: center; gap: 10px; padding: 6px 24px;
      background: linear-gradient(135deg, rgba($ember, 0.2), rgba($ember, 0.05));
      border: 1px solid rgba($ember, 0.35); border-radius: 20px;
      backdrop-filter: blur(8px);
      animation: alertGlow 2.5s ease-in-out infinite;
      .alert-pulse { width: 10px; height: 10px; border-radius: 50%; background: $ember; box-shadow: 0 0 8px $ember; animation: pulse 1.5s ease-in-out infinite; }
      .alert-text { font-size: 13px; color: #ff9090; font-weight: 500; letter-spacing: 1px; }
    }
    .datetime {
      display: flex; align-items: baseline; gap: 16px;
      .date { color: $slate; font-size: 14px; font-weight: 500; letter-spacing: 0.5px; }
      .time { font-size: 22px; font-weight: 800; color: $cyan; letter-spacing: 1px; font-variant-numeric: tabular-nums; filter: drop-shadow(0 0 6px rgba($cyan, 0.3)); }
      .week { color: $slate; font-size: 14px; font-weight: 500; }
    }
  }
  .header-right {
    display: flex; gap: 10px; align-items: center;
    .datetime-compact {
      display: flex; gap: 10px; align-items: baseline; font-size: 12px;
      .date { color: $slate; } .time { font-size: 14px; font-weight: 800; color: $cyan; font-variant-numeric: tabular-nums; } .week { color: $slate; }
    }
    .back-btn { background: linear-gradient(135deg, $electric, $cyan-dim); border: none; box-shadow: 0 2px 8px rgba($cyan, 0.25), inset 0 1px 0 rgba(#fff, 0.15); font-weight: 600; transition: all 0.25s; &:hover { box-shadow: 0 4px 20px rgba($cyan, 0.35); transform: translateY(-1px); } }
    .fullscreen-btn { background: $bg-glass; border: 1px solid $border; color: $slate; backdrop-filter: blur(8px); &:hover { border-color: $cyan; color: $cyan; box-shadow: 0 0 12px rgba($cyan, 0.15); } }
  }
}

// ====== Layout ======
.main-content { flex: 1; display: flex; padding: 10px; gap: 10px; min-height: 0; position: relative; z-index: 1; }
.left-panel { width: 280px; flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; &::-webkit-scrollbar { width: 4px; } &::-webkit-scrollbar-track { background: transparent; } &::-webkit-scrollbar-thumb { background: $border; border-radius: 2px; &:hover { background: #245099; } } }
.right-panel { width: 350px; flex-shrink: 0; display: flex; flex-direction: column; gap: 14px; overflow-y: auto; &::-webkit-scrollbar { width: 4px; } &::-webkit-scrollbar-track { background: transparent; } &::-webkit-scrollbar-thumb { background: $border; border-radius: 2px; &:hover { background: #245099; } } }
.center-panel { flex: 1; display: flex; flex-direction: column; gap: 8px; min-width: 0; min-height: 0; }

// ====== Panel Card ======
.panel-card {
  background: linear-gradient(165deg, rgba($bg-surface, 0.8) 0%, rgba($bg-deep, 0.9) 100%);
  border: 1px solid $border; border-radius: 8px; overflow: hidden;
  display: flex; flex-direction: column; flex-shrink: 0;
  backdrop-filter: blur(12px);
  box-shadow: 0 2px 12px rgba(0,0,0,0.2), inset 0 1px 0 rgba(#fff, 0.03);
  transition: border-color 0.35s, box-shadow 0.35s;
  &:hover { border-color: rgba($cyan, 0.3); box-shadow: 0 4px 20px rgba(0,0,0,0.25), 0 0 20px rgba($cyan, 0.05), inset 0 1px 0 rgba(#fff, 0.04); }

  .card-header {
    padding: 12px 16px;
    background: linear-gradient(135deg, rgba($cyan, 0.07) 0%, rgba($electric, 0.02) 100%);
    border-bottom: 1px solid rgba($border, 0.8);
    display: flex; align-items: center; justify-content: space-between; position: relative;
    &::before { content: ''; position: absolute; left: 0; top: 5px; bottom: 5px; width: 3px; background: linear-gradient(180deg, $cyan, rgba($cyan, 0.1)); border-radius: 0 3px 3px 0; box-shadow: 0 0 8px rgba($cyan, 0.3); }
    .card-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; letter-spacing: 0.8px; color: rgba($tx, 0.9); .el-icon { color: $cyan; font-size: 16px; filter: drop-shadow(0 0 4px rgba($cyan, 0.3)); } }
    .badge { background: $cyan; color: #fff; padding: 2px 8px; border-radius: 10px; font-size: 12px; margin-left: 4px; font-weight: 600; box-shadow: 0 0 6px rgba($cyan, 0.2); &.red { background: linear-gradient(135deg, $ember, #ff3030); box-shadow: 0 0 8px rgba($ember, 0.3); } }
    .view-toggle { display: flex; gap: 8px; .el-icon { cursor: pointer; color: $slate; font-size: 16px; transition: color 0.2s; &.active { color: $cyan; } &:hover { color: $cyan; } } }
  }
  .card-content { padding: 14px; overflow: hidden; }
}

// ====== Enterprise Overview ======
.ent-total-badge {
  font-size: 11px; color: $cyan; font-weight: 600;
  background: rgba($cyan, 0.12); padding: 2px 10px; border-radius: 10px;
  border: 1px solid rgba($cyan, 0.15);
}
.ent-content { padding: 6px 10px !important; }
.ent-name-list { display: flex; flex-direction: column; gap: 2px; }
.ent-name-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: 6px; cursor: pointer;
  transition: background 0.2s;
  &:hover { background: rgba($cyan, 0.05); }
  .eni-dot {
    width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0;
    &.risk-dot-normal { background: $green; box-shadow: 0 0 4px rgba($green, 0.4); }
    &.risk-dot-major { background: $orange; box-shadow: 0 0 4px rgba($orange, 0.4); }
    &.risk-dot-critical { background: $red; box-shadow: 0 0 4px rgba($red, 0.4); }
    &.risk-dot-minor { background: $cyan; box-shadow: 0 0 4px rgba($cyan, 0.3); }
  }
  .eni-name { flex: 1; font-size: 12px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .eni-risk {
    font-size: 10px; padding: 1px 7px; border-radius: 8px; white-space: nowrap; flex-shrink: 0; font-weight: 500;
    &.risk-critical { background: rgba($red, 0.15); color: $red; }
    &.risk-major { background: rgba($orange, 0.15); color: $orange; }
    &.risk-normal { background: rgba($green, 0.15); color: $green; }
    &.risk-minor { background: rgba($cyan, 0.1); color: $slate; }
  }
}
.ent-view-all {
  display: flex; align-items: center; justify-content: center; gap: 4px;
  margin-top: 6px; padding: 7px 0; border-top: 1px dashed rgba($border, 0.5);
  font-size: 12px; color: $cyan; cursor: pointer; font-weight: 500;
  transition: all 0.2s;
  &:hover { color: color.adjust($cyan, $lightness: 10%); .el-icon { transform: translateX(3px); } }
  .el-icon { font-size: 12px; transition: transform 0.2s; }
}
.risk-badge { font-size: 10px; padding: 2px 8px; border-radius: 10px; white-space: nowrap;
  &.risk-critical { background: rgba($red, 0.2); color: $red; } &.risk-major { background: rgba($orange, 0.2); color: $orange; }
  &.risk-normal { background: rgba($green, 0.2); color: $green; } &.risk-minor { background: rgba($cyan, 0.15); color: $slate; }
}

// ====== Device Stats ======
.device-hero { text-align: center; padding: 6px 0 10px; border-bottom: 1px solid rgba($border, 0.5); margin-bottom: 10px;
  .hero-num { font-size: 36px; font-weight: bold; background: linear-gradient(180deg, #fff 20%, $cyan 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; filter: drop-shadow(0 0 10px rgba($cyan, 0.25)); font-variant-numeric: tabular-nums; }
  .hero-unit { font-size: 13px; color: $slate; margin-left: 4px; font-weight: 500; }
}
.device-bars { display: flex; flex-direction: column; gap: 8px;
  .bar-item { display: flex; align-items: center; gap: 8px;
    .bar-label { font-size: 12px; color: $slate; width: 32px; text-align: right; font-weight: 500; }
    .bar-track { flex: 1; height: 8px; background: rgba($border, 0.35); border-radius: 4px; overflow: hidden; box-shadow: inset 0 1px 2px rgba(0,0,0,0.2); }
    .bar-fill { height: 100%; border-radius: 4px; transition: width 0.8s ease; position: relative; &::after { content: ''; position: absolute; top: 0; right: 0; bottom: 0; width: 20px; background: linear-gradient(90deg, transparent, rgba(#fff, 0.2)); border-radius: 0 4px 4px 0; } }
    .bar-val { font-size: 13px; font-weight: bold; width: 32px; font-variant-numeric: tabular-nums; }
    &.normal { .bar-fill { background: linear-gradient(90deg, #00b86b, $mint); box-shadow: 0 0 8px rgba($mint, 0.3); } .bar-val { color: $mint; } }
    &.alarm { .bar-fill { background: linear-gradient(90deg, #e83030, $ember); box-shadow: 0 0 8px rgba($ember, 0.3); } .bar-val { color: $ember; } }
    &.fault { .bar-fill { background: linear-gradient(90deg, #e68a00, $amber); box-shadow: 0 0 8px rgba($amber, 0.3); } .bar-val { color: $amber; } }
    &.offline { .bar-fill { background: linear-gradient(90deg, #3d4f6f, $ghost); } .bar-val { color: $ghost; } }
    &.inactive { .bar-fill { background: linear-gradient(90deg, #4a5a7a, $slate); } .bar-val { color: $slate; } }
  }
}

// ====== Facility ======
.facility-list { display: flex; flex-direction: column; gap: 6px; }
.fac-item { display: flex; align-items: center; gap: 10px; padding: 5px 0; border-bottom: 1px solid rgba($border, 0.3); transition: background 0.2s; &:hover { background: rgba($cyan, 0.03); } &:last-child { border-bottom: none; }
  .fac-icon { width: 28px; height: 28px; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 14px; flex-shrink: 0; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
  .fac-info { flex: 1; .fac-name { font-size: 12px; font-weight: 500; } .fac-count { font-size: 11px; color: $slate; } }
  .fac-pct { font-size: 12px; color: $cyan; font-weight: 600; }
}

// ====== Properties ======
.filter-inline { display: flex; gap: 6px; margin-bottom: 8px;
  :deep(.el-select) { .el-input__wrapper { background: $bg-deep; box-shadow: 0 0 0 1px $border inset; } .el-input__inner { color: $tx; font-size: 12px; } }
}
.prop-content { max-height: 180px; overflow-y: auto; &::-webkit-scrollbar { width: 4px; } &::-webkit-scrollbar-thumb { background: $border; border-radius: 2px; } }
.prop-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px;
  .prop-card { background: linear-gradient(135deg, rgba($cyan, 0.06), rgba($cyan, 0.02)); border: 1px solid $border; border-radius: 6px; padding: 8px; cursor: pointer; transition: all 0.2s;
    &:hover { background: rgba($cyan, 0.1); border-color: rgba($cyan, 0.3); transform: translateY(-1px); }
    .pc-name { font-size: 12px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .pc-risk { font-size: 10px; margin-top: 2px; } .pc-count { font-size: 10px; color: $slate; margin-top: 1px; }
    &.risk-critical { border-left: 3px solid $red; .pc-risk { color: $red; } }
    &.risk-major { border-left: 3px solid $orange; .pc-risk { color: $orange; } }
    &.risk-normal { border-left: 3px solid $mint; .pc-risk { color: $mint; } }
  }
}
.prop-list { .prop-row { display: flex; align-items: center; justify-content: space-between; padding: 6px 8px; border-bottom: 1px solid $border; cursor: pointer; transition: background 0.2s;
  &:hover { background: rgba($cyan, 0.05); } &:last-child { border-bottom: none; }
  .pr-name { font-size: 12px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .pr-info { display: flex; gap: 6px; font-size: 11px; color: $slate; .risk-text-critical { color: $red; } .risk-text-major { color: $orange; } .risk-text-normal { color: $mint; } }
} }

// ====== Map ======
.map-wrapper {
  flex: 1; min-height: 180px;
  background: $bg-surface; border: 1px solid $border; border-radius: 8px; overflow: hidden; position: relative; display: flex; flex-direction: column;
  box-shadow: 0 2px 16px rgba(0,0,0,0.2);
  .map-bar { padding: 8px 14px; background: linear-gradient(135deg, rgba($cyan, 0.06), transparent); border-bottom: 1px solid $border; display: flex; align-items: center; justify-content: space-between;
    .map-label { font-size: 14px; font-weight: bold; letter-spacing: 1px; }
    .map-summary { display: flex; gap: 20px;
      .ms-item { text-align: center;
        .ms-v { font-size: 18px; font-weight: bold; color: $cyan; font-variant-numeric: tabular-nums; }
        .ms-l { font-size: 10px; color: $ghost; font-weight: 500; }
        .green-v { color: $mint !important; }
        .alarm-v { color: $ember !important; }
      }
    }
  }
  .map-canvas { flex: 1; width: 100%; min-height: 150px; height: 0; }
  .map-legend { position: absolute; bottom: 10px; left: 10px; background: rgba($bg-void, 0.85); padding: 6px 12px; border-radius: 6px;
    display: flex; gap: 12px; z-index: 10; border: 1px solid rgba($border, 0.5); backdrop-filter: blur(4px);
    .lg-item { display: flex; align-items: center; gap: 5px; font-size: 11px; color: $slate; font-weight: 500;
      i { width: 10px; height: 10px; border-radius: 50%; display: inline-block; box-shadow: 0 0 6px currentColor; }
    }
  }
}

// ====== Video ======
.video-panel {
  background: $bg-surface; border: 1px solid $border; border-radius: 8px; overflow: hidden; flex-shrink: 0;
  .video-header-bar { padding: 8px 14px; background: linear-gradient(135deg, rgba($cyan, 0.06), transparent); border-bottom: 1px solid $border; display: flex; align-items: center; justify-content: space-between;
    .card-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; letter-spacing: 0.5px; .el-icon { color: $cyan; font-size: 16px; } }
    .video-count { font-size: 12px; color: $ghost; font-weight: 500; }
  }
  .video-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; padding: 8px; }
  .video-item { background: rgba(0,0,0,0.35); border: 1px solid rgba($border, 0.5); border-radius: 6px; overflow: hidden; transition: border-color 0.3s; &:hover { border-color: rgba($cyan, 0.3); }
    .video-top { padding: 4px 8px; font-size: 11px; display: flex; align-items: center; gap: 4px; color: $slate; background: rgba($border, 0.15); .el-icon { color: $mint; font-size: 12px; } }
    .video-body { height: 80px; display: flex; align-items: center; justify-content: center; background: linear-gradient(180deg, rgba(0,0,0,0.1), rgba(0,0,0,0.3)); }
    .video-placeholder { display: flex; flex-direction: column; align-items: center; gap: 4px;
      .cam-icon { font-size: 28px; color: rgba($ghost, 0.25); }
      .live-tag { font-size: 10px; padding: 1px 8px; background: rgba($ember, 0.15); color: $ember; border-radius: 3px; letter-spacing: 1px; animation: blink 2s ease-in-out infinite; }
    }
  }
}

// ====== Bottom Row ======
.bottom-row { display: flex; gap: 8px; flex-shrink: 0; .third { flex: 1; } }
.chart-box { width: 100%; height: 130px; }

// ====== Stat Grid ======
.stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  .stat-cell { text-align: center; padding: 10px 4px; border-radius: 6px; background: linear-gradient(135deg, rgba($cyan, 0.08), rgba($cyan, 0.02)); border: 1px solid rgba($border, 0.5); transition: all 0.2s;
    &:hover { border-color: rgba($cyan, 0.3); transform: scale(1.03); }
    .sv { font-size: 22px; font-weight: bold; font-variant-numeric: tabular-nums; } .sl { font-size: 11px; color: $slate; margin-top: 3px; font-weight: 500; }
    &.total .sv { color: $cyan; text-shadow: 0 0 8px rgba($cyan, 0.2); } &.handled .sv { color: $mint; } &.pending .sv { color: $amber; } &.rate .sv { color: $mint; }
  }
}

// ====== Alarm ======
.alarm-panel { &:has(.alarm-item) { border-color: rgba($ember, 0.3); } .card-content { max-height: 170px; overflow: hidden; } }
.alarm-inner { &.scrolling { animation: scrollUp 18s linear infinite; } }
.alarm-item { display: flex; align-items: flex-start; padding: 7px 8px; margin-bottom: 5px; background: rgba($ember, 0.08); border-radius: 6px; border-left: 3px solid $ember; cursor: pointer; transition: background 0.2s;
  &:hover { background: rgba($ember, 0.18); }
  &.level-critical { border-left-color: $ember; background: rgba($ember, 0.12); }
  &.level-major { border-left-color: $amber; background: rgba($amber, 0.08); }
  &.level-warning { border-left-color: $gold; background: rgba($gold, 0.06); }
  .alarm-dot { width: 8px; height: 8px; border-radius: 50%; background: $ember; margin-top: 5px; margin-right: 8px; flex-shrink: 0; box-shadow: 0 0 6px rgba($ember, 0.5); animation: blink 2s ease-in-out infinite; }
  .alarm-body { flex: 1; .alarm-name { font-size: 14px; font-weight: 500; } .alarm-desc { font-size: 14px; color: $slate; margin-top: 2px; } .alarm-time { font-size: 12px; color: $ghost; margin-top: 1px; } }
  .alarm-tag { font-size: 12px; padding: 2px 6px; border-radius: 4px; white-space: nowrap; font-weight: 600; &.tag-critical { background: rgba($ember, 0.2); color: $ember; } &.tag-major { background: rgba($amber, 0.2); color: $amber; } }
}

// ====== Patrol ======
.patrol-content {
  .patrol-summary { display: flex; gap: 12px; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px dashed rgba($border, 0.5);
    .ps-item { text-align: center; flex: 1; .ps-v { font-size: 18px; font-weight: bold; font-variant-numeric: tabular-nums; } .ps-l { font-size: 10px; color: $ghost; font-weight: 500; }
      .blue-v { color: $cyan; } .green-v { color: $mint; } .orange-v { color: $amber; }
    }
  }
  .patrol-list { display: flex; flex-direction: column; gap: 8px; }
  .patrol-item { display: flex; align-items: center; gap: 10px; padding: 8px 6px; border-bottom: 1px solid rgba($border, 0.3); transition: background 0.2s; background: rgba($border, 0.05); border-radius: 6px; &:hover { background: rgba($cyan, 0.04); } &:last-child { border-bottom: none; }
    .patrol-status-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
      &.completed { background: $mint; box-shadow: 0 0 4px rgba($mint, 0.4); } &.in_progress { background: $cyan; box-shadow: 0 0 4px rgba($cyan, 0.4); animation: blink 2s ease-in-out infinite; } &.pending { background: $amber; } &.overdue { background: $ember; box-shadow: 0 0 4px rgba($ember, 0.4); }
    }
    .patrol-info { flex: 1; .patrol-name { font-size: 13px; font-weight: 500; } .patrol-meta { font-size: 11px; color: $ghost; margin-top: 2px; } }
    .patrol-tag { font-size: 11px; padding: 3px 10px; border-radius: 6px; font-weight: 600;
      &.completed { background: rgba($mint, 0.12); color: $mint; } &.in_progress { background: rgba($cyan, 0.12); color: $cyan; }
      &.pending { background: rgba($amber, 0.12); color: $amber; } &.overdue { background: rgba($ember, 0.12); color: $ember; }
    }
  }
}

// ====== Duty ======
.duty-content {
  .duty-row { display: flex; align-items: center; padding: 10px 6px; border-bottom: 1px solid rgba($border, 0.4); transition: background 0.2s; background: rgba($border, 0.06); border-radius: 6px; margin-bottom: 4px; border-left: 3px solid rgba($cyan, 0.15); &:hover { background: rgba($cyan, 0.06); } &:last-child { border-bottom: none; margin-bottom: 0; }
    .duty-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, $electric, $cyan-dim); display: flex; align-items: center; justify-content: center; font-size: 18px; color: #fff; box-shadow: 0 3px 10px rgba($electric, 0.3); }
    .duty-info { flex: 1; margin-left: 12px; .duty-name { font-size: 15px; font-weight: 600; } .duty-shift { font-size: 12px; color: $ghost; margin-top: 3px; } }
    .duty-tag { font-size: 11px; padding: 4px 12px; border-radius: 10px; font-weight: 600; &.ongoing { background: rgba($mint, 0.12); color: $mint; border: 1px solid rgba($mint, 0.18); } &.pending { background: rgba($amber, 0.12); color: $amber; border: 1px solid rgba($amber, 0.15); } }
  }
}

// ====== Common ======
.empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; color: $mint; .el-icon { font-size: 32px; margin-bottom: 6px; opacity: 0.6; } &.small { padding: 10px; font-size: 12px; color: $slate; .el-icon { font-size: 20px; } } }

// ====== Animations ======
@keyframes scrollUp { 0% { transform: translateY(0); } 100% { transform: translateY(-50%); } }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
@keyframes alertGlow { 0%, 100% { box-shadow: 0 0 12px rgba($ember, 0.15); } 50% { box-shadow: 0 0 24px rgba($ember, 0.3), 0 0 48px rgba($ember, 0.1); } }
@keyframes pulse { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.3); opacity: 0.6; } }

// ====== Dialog ======
:deep(.dark-dialog) { .el-dialog { background: linear-gradient(180deg, $bg-surface, $bg-deep); border: 1px solid $border; border-radius: 12px; box-shadow: 0 24px 60px rgba(0,0,0,0.5), 0 0 40px rgba($cyan, 0.05); } .el-dialog__title { color: $tx; font-weight: 600; } .el-dialog__body { color: $tx; } }
.detail-body { .dr { display: flex; padding: 8px 0; border-bottom: 1px solid rgba($border, 0.35); .dl { width: 70px; color: $slate; font-size: 13px; font-weight: 500; } .dv { flex: 1; font-size: 13px; font-weight: 500; &.risk-critical { color: $ember; font-weight: 700; } &.risk-major { color: $amber; font-weight: 700; } &.risk-normal { color: $mint; } } }
  .section-title { font-size: 13px; font-weight: 500; margin-top: 12px; margin-bottom: 8px; color: $cyan; }
  .device-status-row { display: flex; gap: 14px; font-size: 12px; .ds-normal { color: $mint; } .ds-alarm { color: $ember; } .ds-fault { color: $amber; } .ds-offline { color: $slate; } }
}
:deep(.amap-container) { .amap-maps { width: 100% !important; height: 100% !important; } }
</style>
