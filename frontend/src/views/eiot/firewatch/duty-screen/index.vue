<template>
  <div class="duty-screen">
    <!-- 全屏地图背景 -->
    <div class="map-bg">
      <SceneHost
        ref="dongguanMapRef"
        :scene="currentScene"
        :points="mapPoints"
        :snapshot-id="propertyMap.snapshotId.value"
        :viewport="mapSafeViewport"
        @point-click="handleScenePointClick"
        @points-applied="handlePropertyPointsApplied"
        @labels-applied="handlePropertyLabelsApplied"
        @request-back="handleSceneBack"
        @error="handleSceneError"
      />
    </div>
    <!-- API加载错误提示 -->
    <div class="api-error-bar" v-if="loadErrors.length > 0">
      <span>接口加载失败: {{ loadErrors.join(', ') }}，请检查后端服务是否正常及权限配置</span>
      <el-button size="small" type="danger" text @click="loadErrors = []">关闭</el-button>
    </div>
    <div v-if="parkNavigationStatus" class="park-navigation-status" role="status" aria-live="polite">{{ parkNavigationStatus }}</div>
    <header class="header">
      <div class="header-left">
        <h1 class="title">安为智慧消防值守平台</h1>
        <p class="subtitle">ANWEI SMART FIRE DUTY PLATFORM</p>
      </div>
      <div class="header-center">
        <div class="datetime">
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
          <span class="week">{{ currentWeek }}</span>
        </div>
      </div>
      <div class="header-right">
        <el-dropdown
          trigger="hover"
          popper-class="header-dropdown-popper view-dropdown-popper"
          @command="handleViewCommand"
        >
          <el-button class="header-icon-btn active"
            ><el-icon><MapLocation /></el-icon
          ></el-button>
          <template #dropdown>
            <div class="view-dropdown-content">
              <div class="view-row-label">三维场景</div>
              <div class="view-row">
                <div
                  class="view-toggle-item"
                  :class="{ active: currentScene === 'city' }"
                  @click="handleViewCommand('map-reset')"
                  >地图全貌</div
                >
                <div
                  class="view-toggle-item"
                  :class="{ active: currentScene === 'park' }"
                  @click="handleViewCommand('park')"
                  >V7 园区</div
                >
              </div>
              <div class="view-row-label">镜头缩放</div>
              <div class="view-row">
                <div
                  class="view-toggle-item"
                  @click="handleViewCommand('map-zoom-in')"
                  >放大</div
                >
                <div
                  class="view-toggle-item"
                  @click="handleViewCommand('map-zoom-out')"
                  >缩小</div
                >
              </div>
            </div>
          </template>
        </el-dropdown>
        <el-dropdown
          trigger="hover"
          popper-class="header-dropdown-popper"
          @command="handleNavCommand"
        >
          <el-button class="header-icon-btn"
            ><el-icon><Grid /></el-icon
          ></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="admin"
                ><el-icon><Setting /></el-icon>管理后台</el-dropdown-item
              >
              <el-dropdown-item command="ai-analysis"
                ><el-icon><DataAnalysis /></el-icon>智能分析</el-dropdown-item
              >
              <el-dropdown-item divided command="device-screen"
                ><el-icon><Grid /></el-icon>设备控制室</el-dropdown-item
              >
              <el-dropdown-item command="data-center"
                ><el-icon><TrendCharts /></el-icon>数据中心<DevelopingTag
                  mini
                  tip="统计接口后端尚未实现，当前为示例数据"
                /></el-dropdown-item
              >
              <el-dropdown-item command="video-surveillance"
                ><el-icon><VideoCamera /></el-icon>视频监控</el-dropdown-item
              >
              <el-dropdown-item command="patrol"
                ><el-icon><Avatar /></el-icon>查岗管理</el-dropdown-item
              >
              <el-dropdown-item divided command="ops-workbench"
                ><el-icon><SetUp /></el-icon>运维工作台<DevelopingTag
                  mini
                  tip="页面仅为占位，无任何业务接口"
                /></el-dropdown-item
              >
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button class="header-icon-btn"
          ><el-icon><Setting /></el-icon
        ></el-button>
        <el-button class="header-icon-btn" @click="togglePanels"
          ><el-icon><View /></el-icon
        ></el-button>
        <el-tooltip content="全屏" placement="bottom">
          <el-button class="header-icon-btn" @click="toggleFullscreen"
            ><el-icon><FullScreen /></el-icon
          ></el-button>
        </el-tooltip>
        <el-button class="header-icon-btn user-btn" @click="goToAdmin"
          ><el-icon><UserFilled /></el-icon>{{ userNickname }}</el-button
        >
      </div>
    </header>

    <main class="main-content">
      <!-- ====== 左侧面板 ====== -->
      <aside class="left-panel" v-show="showPanels">
        <!-- 企业信息 -->
        <div class="panel-card">
          <div class="ent-header-bar" @click="goToAdmin">
            <div class="ent-header-left">
              <el-icon><OfficeBuilding /></el-icon>
              <span class="ent-header-title">企业总览</span>
            </div>
            <div class="ent-header-right">
              <span class="ent-header-badge">查看{{ propertyList.length }}家企业</span>
              <el-icon class="ent-header-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
          <div class="card-content ent-content">
            <div class="ent-name-list">
              <div
                class="ent-name-item"
                v-for="p in displayProperties"
                :key="p.id"
                @click="openPropertyDetail(p)"
              >
                <span class="eni-dot" :class="'risk-dot-' + (p.riskLevel || 'normal')"></span>
                <span class="eni-name">{{ p.name }}</span>
                <span class="eni-risk" :class="'risk-' + p.riskLevel">{{
                  riskLevelText(p.riskLevel)
                }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 设备状态统计（点击进入设备总览页） -->
        <div class="panel-card device-stats-card" @click="goToDeviceScreen">
          <div class="card-header">
            <span class="card-title"
              ><el-icon><Monitor /></el-icon>设备状态统计</span
            >
            <el-icon class="card-header-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="card-content">
            <div class="device-hero">
              <span class="hero-num">{{ deviceStats.total }}</span>
              <span class="hero-unit">台设备</span>
            </div>
            <div class="device-bars">
              <div class="bar-item normal">
                <span class="bar-label">正常</span>
                <div class="bar-track"
                  ><div
                    class="bar-fill"
                    :style="{ width: pct(deviceStats.normal, deviceStats.total) + '%' }"
                  ></div
                ></div>
                <span class="bar-val">{{ deviceStats.normal }}</span>
              </div>
              <div class="bar-item alarm">
                <span class="bar-label">告警</span>
                <div class="bar-track"
                  ><div
                    class="bar-fill"
                    :style="{ width: pct(deviceStats.alarm, deviceStats.total) + '%' }"
                  ></div
                ></div>
                <span class="bar-val">{{ deviceStats.alarm }}</span>
              </div>
              <div class="bar-item fault">
                <span class="bar-label">故障</span>
                <div class="bar-track"
                  ><div
                    class="bar-fill"
                    :style="{ width: pct(deviceStats.fault, deviceStats.total) + '%' }"
                  ></div
                ></div>
                <span class="bar-val">{{ deviceStats.fault }}</span>
              </div>
              <div class="bar-item offline">
                <span class="bar-label">离线</span>
                <div class="bar-track"
                  ><div
                    class="bar-fill"
                    :style="{ width: pct(deviceStats.offline, deviceStats.total) + '%' }"
                  ></div
                ></div>
                <span class="bar-val">{{ deviceStats.offline }}</span>
              </div>
              <div class="bar-item inactive">
                <span class="bar-label">未激活</span>
                <div class="bar-track"
                  ><div
                    class="bar-fill"
                    :style="{ width: pct(deviceStats.inactive, deviceStats.total) + '%' }"
                  ></div
                ></div>
                <span class="bar-val">{{ deviceStats.inactive }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 值班信息 -->
        <div class="panel-card">
          <div class="card-header"
            ><span class="card-title"
              ><el-icon><User /></el-icon>今日值班</span
            ></div
          >
          <div class="card-content duty-content">
            <div class="duty-card" v-for="d in dutyList" :key="d.id">
              <div class="duty-card-top">
                <div class="duty-avatar"
                  ><el-icon><UserFilled /></el-icon
                ></div>
                <div class="duty-info">
                  <div class="duty-name">{{ d.userName }}</div>
                  <div class="duty-phone" v-if="d.phone">{{ maskPhone(d.phone) }}</div>
                </div>
              </div>
              <div class="duty-card-bottom">
                <div class="duty-time-block">
                  <el-icon><Clock /></el-icon>
                  <span>{{ d.startTime }} - {{ d.endTime }}</span>
                </div>
                <span class="duty-status-btn" :class="d.status">
                  <el-icon v-if="d.status === 'ongoing'"><CircleCheck /></el-icon>
                  <el-icon v-else-if="d.status === 'pending'"><Clock /></el-icon>
                  <el-icon v-else><CircleCheck /></el-icon>
                  {{ dutyStatusText(d.status) }}
                </span>
              </div>
            </div>
            <div class="empty small" v-if="!dutyList.length">暂无值班</div>
          </div>
        </div>
      </aside>

      <!-- ====== 右侧面板 ====== -->
      <aside class="right-panel" v-show="showPanels">
        <!-- 今日警情 -->
        <div class="panel-card card-auto">
          <div class="card-header"
            ><span class="card-title"
              ><el-icon><Warning /></el-icon>今日警情</span
            ></div
          >
          <div class="card-content">
            <div class="stat-grid">
              <div class="stat-cell total"
                ><div class="sv">{{ todayAlarm.totalAlarms }}</div
                ><div class="sl">告警总数</div></div
              >
              <div class="stat-cell handled"
                ><div class="sv">{{ todayAlarm.handledCount }}</div
                ><div class="sl">已处置</div></div
              >
              <div class="stat-cell pending"
                ><div class="sv">{{ todayAlarm.pendingCount }}</div
                ><div class="sl">待处理</div></div
              >
              <div class="stat-cell rate"
                ><div class="sv">{{ todayAlarm.handleRate }}%</div><div class="sl">处置率</div></div
              >
            </div>
          </div>
        </div>

        <!-- 实时告警 -->
        <div class="panel-card alarm-panel">
          <div class="card-header"
            ><span class="card-title"
              ><el-icon><Bell /></el-icon>实时告警<span class="badge red" v-if="alarmList.length">{{
                alarmList.length
              }}</span></span
            ></div
          >
          <div class="card-content alarm-scroll">
            <div
              class="alarm-inner"
              :class="{ scrolling: alarmList.length > 3 }"
              @mouseenter="scrollPaused = true"
              @mouseleave="scrollPaused = false"
            >
              <div
                class="alarm-item"
                v-for="alarm in alarmList"
                :key="alarm.id"
                :class="'level-' + alarm.alarmLevel"
                @click="viewAlarm(alarm)"
              >
                <div class="alarm-dot"></div>
                <div class="alarm-body">
                  <div class="alarm-desc">{{ extractAlarmSummary(alarm.alarmDescription) }}</div>
                  <div class="alarm-time">{{ formatTime(alarm.alarmTime) }}</div>
                </div>
                <span class="alarm-tag" :class="'tag-' + alarm.alarmLevel">{{
                  alarmLevelText(alarm.alarmLevel)
                }}</span>
              </div>
            </div>
            <div class="empty" v-if="!alarmList.length"
              ><el-icon><CircleCheck /></el-icon>暂无告警</div
            >
          </div>
        </div>

        <!-- 告警趋势 -->
        <div class="panel-card card-auto">
          <div class="card-header"
            ><span class="card-title"
              ><el-icon><TrendCharts /></el-icon>告警趋势(近7天)</span
            ></div
          >
          <div class="card-content"><div ref="trendChartRef" class="chart-box"></div></div>
        </div>
      </aside>
    </main>

    <!-- 物业详情弹窗 -->
    <el-dialog
      v-model="propDialogVisible"
      :title="selectedProperty?.name"
      width="460px"
      class="dark-dialog"
      :append-to-body="true"
    >
      <div class="detail-body" v-if="selectedProperty">
        <div class="dr"
          ><span class="dl">地址</span
          ><span class="dv">{{ selectedProperty.address || '-' }}</span></div
        >
        <div class="dr"
          ><span class="dl">联系人</span
          ><span class="dv">{{ selectedProperty.contactName || '-' }}</span></div
        >
        <div class="dr"
          ><span class="dl">联系电话</span
          ><span class="dv">{{ selectedProperty.contactPhone || '-' }}</span></div
        >
        <div class="dr"
          ><span class="dl">风险等级</span
          ><span class="dv" :class="'risk-' + selectedProperty.riskLevel">{{
            riskLevelText(selectedProperty.riskLevel)
          }}</span></div
        >
        <div class="dr"
          ><span class="dl">设备数量</span
          ><span class="dv">{{ selectedProperty.deviceCount || 0 }}台</span></div
        >
        <template v-if="propDevices.length">
          <div class="section-title">设备状态</div>
          <div class="device-status-row">
            <span class="ds-normal"
              >正常 {{ propDevices.filter((d) => d.deviceStatus === 'normal').length }}</span
            >
            <span class="ds-alarm"
              >告警 {{ propDevices.filter((d) => d.deviceStatus === 'alarm').length }}</span
            >
            <span class="ds-fault"
              >故障 {{ propDevices.filter((d) => d.deviceStatus === 'fault').length }}</span
            >
            <span class="ds-offline"
              >离线 {{ propDevices.filter((d) => d.deviceStatus === 'offline').length }}</span
            >
          </div>
        </template>
      </div>
    </el-dialog>

    <!-- 报警弹窗（已禁用） -->
    <!--
    <AlarmPopup
      :show-popup="alarmPopupShow"
      :current-alarm="alarmPopupCurrent"
      :alarm-queue="alarmPopupQueue"
      @dismiss="alarmPopupDismiss"
    />
    -->

    <!-- 告警详情弹窗（内嵌 alarm-popup 页面） -->
    <AlarmPopupPage
      :visible="alarmPopupPageVisible"
      :alarm="alarmPopupPageData"
      @close="alarmPopupPageVisible = false"
    />
  </div>

  <!-- ====== 报警弹窗 ====== -->
  <Teleport to="body">
    <div class="alarm-popup-overlay" :class="'flash-' + getPopupSeverity()" v-if="popupVisible" @click.self="onHandleLater">
      <div class="alarm-popup-card">
        <!-- 倒计时进度条 -->
        <div class="alarm-countdown-bar">
          <div class="alarm-countdown-fill" :style="{ width: popupCountdownPercent + '%' }"></div>
        </div>

        <!-- 顶栏 -->
        <div class="alarm-popup-header">
          <div class="alarm-popup-header-left">
            <span class="alarm-popup-icon">⚠</span>
            <span class="alarm-popup-type">{{ popupAlarm?.name || '设备报警' }}</span>
          </div>
          <div class="alarm-popup-header-right">
            <span class="alarm-popup-severity" :class="'severity-' + getPopupSeverity()">{{ getPopupSeverityText() }}</span>
            <span class="alarm-sound-toggle" :class="{ muted: !soundEnabled }" @click.stop="onToggleSound" :title="soundEnabled ? '点击静音' : '点击开启声音'">{{ soundEnabled ? '🔊' : '🔇' }}</span>
            <span class="alarm-popup-timer">{{ popupCountdown }}s</span>
            <span class="alarm-popup-queue" v-if="alertQueue.length > 1">第 {{ alertQueue.length - remainingAlerts }}/{{ alertQueue.length }} 条</span>
          </div>
        </div>

        <!-- 主体 -->
        <div class="alarm-popup-body">
          <!-- 左侧图片 -->
          <div class="alarm-popup-image">
            <img v-if="popupAlarm?.alertImage" :src="popupAlarm.alertImage" alt="报警抓拍" />
            <div v-else class="alarm-popup-no-image">
              <span class="no-image-icon">📹</span>
              <span class="no-image-text">暂无抓拍图片</span>
            </div>
          </div>

          <!-- 右侧信息 -->
          <div class="alarm-popup-info">
            <div class="alarm-info-row">
              <span class="alarm-info-label">📋 单位</span>
              <span class="alarm-info-value">{{ popupAlarm?.tenantName || '未知单位' }}</span>
            </div>
            <div class="alarm-info-row">
              <span class="alarm-info-label">📟 设备</span>
              <span class="alarm-info-value">{{ popupAlarm?.deviceName || popupAlarm?.name || '未知设备' }}</span>
            </div>
            <div class="alarm-info-row">
              <span class="alarm-info-label">📍 位置</span>
              <span class="alarm-info-value">{{ popupAlarm?.deviceAddr || '未知位置' }}</span>
            </div>
            <div class="alarm-info-row">
              <span class="alarm-info-label">🕐 时间</span>
              <span class="alarm-info-value">{{ formatPopupTime(popupAlarm?.alertTime) }}</span>
            </div>
            <div class="alarm-info-row">
              <span class="alarm-info-label">📝 详情</span>
              <span class="alarm-info-value alarm-detail-text">{{ popupAlarm?.details || '无详情' }}</span>
            </div>
          </div>
        </div>

        <!-- 底栏 -->
        <div class="alarm-popup-footer">
          <button class="alarm-btn alarm-btn-later" @click="onHandleLater">稍后处置</button>
          <button class="alarm-btn alarm-btn-now" @click="onHandleNow">去处置</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { ElMessage } from 'element-plus'
import request from '@/config/axios'
// import AlarmPopup from '@/components/AlarmPopup/index.vue'
// import { useAlarmPopup } from '@/components/AlarmPopup/useAlarmPopup'
import AlarmPopupPage from '@/views/eiot/firewatch/alarm-popup/index.vue'
import DevelopingTag from '@/components/DevelopingTag/index.vue'
import { useUserStore } from '@/store/modules/user'
import { useWebSocket } from '@vueuse/core'
import { getRefreshToken } from '@/utils/auth'
import { getRecentUnprocessedAlerts, handleAlertRecord, deferAlertRecord, findTransferDeviceId } from '@/api/eiot/firewatch/firewatch.api'
import { getCamerasByDevice } from '@/api/eiot/video/deviceCameraRelation.api'
import { stopAlarmSoundLoop, isAlarmSoundEnabled, toggleAlarmSound, closeAlarmAudio } from '@/utils/alarmSound'
import { speakAlarmSpeech, stopAlarmSpeech } from '@/utils/alarmSpeech'
import SceneHost from './components/SceneHost.vue'
import { findPoint } from './components/scene-data'
import { type MapPoint, type SceneName } from './components/scene-types'
import { usePropertyMapData } from './components/usePropertyMapData'
import type { Viewport } from './components/scene-protocol'

const router = useRouter()
const route = useRoute()
const rawScene = computed(() => {
  const value = route.query.scene
  return Array.isArray(value) ? value[0] : value
})
const currentScene = computed<SceneName>(() => rawScene.value === 'park' ? 'park' : 'city')
const dongguanMapRef = ref<InstanceType<typeof SceneHost>>()

// Refs
const trendChartRef = ref<HTMLElement>()

const propertyMap = usePropertyMapData(computed(() => currentScene.value === 'city'))
const mapPoints = computed<MapPoint[]>(() => propertyMap.points.value)
const mapSafeViewport = ref<Viewport | null>(null)
const propertyMapRenderReport = ref<{ snapshotId: string; renderedPropertyIds: string[]; excluded: { outOfBoundary: number; noSurface: number; invalid: number }; excludedIds?: { outOfBoundary: string[]; noSurface: string[]; invalid: string[] } } | null>(null)
const propertyMapVisualReport = ref<{ snapshotId: string; visiblePropertyIds: string[]; clusteredPropertyIds: string[]; hiddenPropertyIds: string[]; offscreenPropertyIds: string[] } | null>(null)
const parkNavigationPending = ref(false)
const parkNavigationStatus = ref('')
let parkNavigationStatusTimer: number | null = null
const setParkNavigationStatus = (message: string, clearAfterMs = 0) => {
  parkNavigationStatus.value = message
  if (parkNavigationStatusTimer !== null) window.clearTimeout(parkNavigationStatusTimer)
  parkNavigationStatusTimer = clearAfterMs > 0 ? window.setTimeout(() => { parkNavigationStatus.value = ''; parkNavigationStatusTimer = null }, clearAfterMs) : null
}
watch(() => propertyMap.snapshotId.value, () => { propertyMapRenderReport.value = null; propertyMapVisualReport.value = null })
const scrollPaused = ref(false)

// State
const currentDate = ref(''),
  currentTime = ref(''),
  currentWeek = ref('')
const enterpriseInfo = ref<any>({})
const propertyList = ref<any[]>([])
const deviceStats = ref({ total: 0, normal: 0, alarm: 0, fault: 0, offline: 0, inactive: 0 })
const todayAlarm = ref({ totalAlarms: 0, handledCount: 0, pendingCount: 0, handleRate: 0 })
const alarmList = ref<any[]>([])
const dutyList = ref<any[]>([])
const propDialogVisible = ref(false)
const selectedProperty = ref<any>(null)
const propDevices = ref<any[]>([])
const alarmPopupPageVisible = ref(false)
const alarmPopupPageData = ref<Record<string, any> | null>(null)
const userStore = useUserStore()
const userNickname = computed(() => userStore.getUser.nickname || '用户001')

// 报警弹窗（已禁用）
// const {
//   showPopup: alarmPopupShow,
//   currentAlarm: alarmPopupCurrent,
//   alarmQueue: alarmPopupQueue,
//   dismissPopup: alarmPopupDismiss,
//   checkNewAlarms: alarmPopupCheckNew,
//   cleanup: alarmPopupCleanup,
// } = useAlarmPopup()

// ==================== 报警弹窗逻辑 ====================
const popupVisible = ref(false)
const popupAlarm = ref<any>(null)
const popupCountdown = ref(15)
const alertQueue = ref<any[]>([])
const alertShownIds = ref<Set<number>>(new Set())
const remainingAlerts = ref(0)
let popupAutoHideTimer: ReturnType<typeof setInterval> | null = null
let alertPollingTimer: ReturnType<typeof setInterval> | null = null

const POPUP_AUTO_HIDE_SECONDS = 15
const popupCountdownPercent = computed(() => (popupCountdown.value / POPUP_AUTO_HIDE_SECONDS) * 100)

const getPopupSeverity = () => {
  const level = popupAlarm.value?.level
  if (level === '1' || level === 'critical') return 'critical'
  if (level === '2' || level === 'major') return 'major'
  if (level === '3' || level === 'warning') return 'warning'
  return 'info'
}

const getPopupSeverityText = () => {
  const map: Record<string, string> = { critical: '紧急', major: '较重', warning: '一般', info: '提示' }
  return map[getPopupSeverity()] || '未知'
}

const formatPopupTime = (t: any) => {
  if (!t) return ''
  if (typeof t === 'number') {
    return new Date(t).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  }
  return String(t).replace('T', ' ').substring(0, 19)
}

const startAutoHide = () => {
  stopAutoHide()
  popupCountdown.value = POPUP_AUTO_HIDE_SECONDS
  popupAutoHideTimer = setInterval(() => {
    popupCountdown.value--
    if (popupCountdown.value <= 0) {
      closePopup()
    }
  }, 1000)
}

const stopAutoHide = () => {
  if (popupAutoHideTimer) {
    clearInterval(popupAutoHideTimer)
    popupAutoHideTimer = null
  }
}

const closePopup = () => {
  stopAutoHide()
  stopAlarmSoundLoop()
  stopAlarmSpeech()
  popupVisible.value = false
  popupAlarm.value = null
  setTimeout(() => showNextAlert(), 300)
}

const showNextAlert = () => {
  if (alertQueue.value.length === 0) {
    popupVisible.value = false
    return
  }
  popupAlarm.value = alertQueue.value.shift()
  remainingAlerts.value = alertQueue.value.length
  popupVisible.value = true
  startAutoHide()
  // 按报警类型语音播报
  speakAlarmSpeech(popupAlarm.value)
}

const onHandleNow = async () => {
  const alarm = popupAlarm.value
  if (alarm?.id) {
    try {
      await handleAlertRecord(alarm.id)
    } catch (e) {
      console.warn('处置告警失败:', e)
    }
  }
  // 查找用传设备并跳转
  const deviceId = alarm?.deviceId
  if (deviceId) {
    try {
      const res = await findTransferDeviceId(Number(deviceId))
      const transferId = typeof res === 'number' ? res : (res as any)?.data
      alertShownIds.value.delete(alarm?.id)
      closePopup()
      if (transferId) {
        // 找到用传设备 → 跳转远程控制中心
        router.push({ path: '/remote-console', query: { deviceId: String(transferId), alertDeviceId: String(deviceId) } })
      } else {
        // 非用传设备 → 查找关联摄像头，跳转视频监控页面
        let cameraId: string | undefined
        try {
          const cams = await getCamerasByDevice(Number(deviceId))
          if (Array.isArray(cams) && cams.length > 0) cameraId = String(cams[0].id)
        } catch {}
        router.push({ path: '/video-surveillance', query: cameraId ? { cameraId } : {} })
      }
    } catch (e) {
      alertShownIds.value.delete(alarm?.id)
      closePopup()
      // 查找失败，默认跳转视频监控
      let cameraId: string | undefined
      try {
        const cams = await getCamerasByDevice(Number(deviceId))
        if (Array.isArray(cams) && cams.length > 0) cameraId = String(cams[0].id)
      } catch {}
      router.push({ path: '/video-surveillance', query: cameraId ? { cameraId } : {} })
    }
  } else {
    alertShownIds.value.delete(alarm?.id)
    closePopup()
  }
}

const onHandleLater = async () => {
  if (popupAlarm.value?.id) {
    try {
      await deferAlertRecord(popupAlarm.value.id)
    } catch (e) {
      // 静默失败
    }
  }
  closePopup()
}

const fetchRecentAlerts = async () => {
  try {
    const res = await getRecentUnprocessedAlerts(5)
    const alerts = (Array.isArray(res) ? res : res?.data) || []
    console.log('[DutyScreen] fetchRecentAlerts =>', alerts.length, '条告警')
    const newAlerts = alerts.filter((a: any) => !alertShownIds.value.has(a.id))
    if (newAlerts.length === 0) return

    alertQueue.value.push(...newAlerts)
    newAlerts.forEach((a: any) => alertShownIds.value.add(a.id))

    if (!popupVisible.value) {
      showNextAlert()
    }
  } catch (e) {
    // 静默失败
  }
}

// WebSocket 连接（实时推送增强）
const wsServer = ref(
  (import.meta.env.VITE_BASE_URL + '/infra/ws').replace('http', 'ws') +
    '?token=' + getRefreshToken()
)

let wsData: any = null
try {
  const ws = useWebSocket(wsServer.value, {
    autoReconnect: true,
    heartbeat: true
  })
  wsData = ws.data
} catch (e) {
  console.warn('WebSocket 连接失败，仅使用轮询模式')
}

if (wsData) {
  watch(wsData, (newVal: any) => {
    if (!newVal || newVal === 'pong') return
    try {
      const msg = JSON.parse(newVal)
      if (msg.type === 'firewatch-alert-new') {
        const content = JSON.parse(msg.content)
        const alertId = content.alertId
        if (alertId && !alertShownIds.value.has(alertId)) {
          alertQueue.value.push({
            id: alertId,
            name: content.alertName || '设备报警',
            deviceId: content.deviceId,
            alertTime: content.alertTime,
            alertImage: content.alertImage || null,
            level: content.level || '2',
            details: content.alertDetails || '',
            deviceName: content.deviceName || '',
            deviceAddr: '',
            productName: '',
            tenantName: content.tenantName || '',
            alarmType: content.alarmType,
            subDeviceName: content.subDeviceName || ''
          })
          alertShownIds.value.add(alertId)
          if (!popupVisible.value) {
            showNextAlert()
          }
        }
      }
    } catch (e) {
      // ignore
    }
  })
}

// 缓存图表数据（loadData 先于 initCharts 执行，需要缓存数据供图表初始化后刷新）
let cachedAlarmTrend: any[] = []
let dataLoaded = false

// Charts
let trendChart: ECharts | null = null

// Sound
const soundEnabled = ref(isAlarmSoundEnabled())
const onToggleSound = () => {
  soundEnabled.value = toggleAlarmSound()
}

// Computed
const maxDisplayCount = 6
const displayProperties = computed(() => propertyList.value.slice(0, maxDisplayCount))
// Helpers
const pct = (v: number, t: number) => (t > 0 ? Math.round((v / t) * 100) : 0)

const updateDateTime = () => {
  const now = new Date()
  const w = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  currentDate.value = now
    .toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
    .replace(/\//g, '-')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentWeek.value = w[now.getDay()]
}
const formatTime = (t: any) => {
  if (!t) return '-'
  if (typeof t === 'number')
    return new Date(t).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  const s = String(t)
  return s ? s.replace('T', ' ').substring(0, 19) : '-'
}
const alarmLevelText = (l: string) =>
  ({ critical: '严重', major: '较大', warning: '一般', info: '提示' })[l] || '一般'
const riskLevelText = (l: string) =>
  ({ critical: '重大风险', major: '较大风险', normal: '一般风险', minor: '较小风险' })[l] ||
  '未评估'
const dutyStatusText = (s: string) =>
  ({ ongoing: '值班中', pending: '待值班', completed: '已完成' })[s] || s
const maskPhone = (phone: string) => {
  if (!phone || phone.length < 7) return phone || ''
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

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
    return addrMatch
      ? `${eventTypeMatch[1].trim()} - ${addrMatch[1].trim()}`
      : eventTypeMatch[1].trim()
  }
  // 兜底：截取前30字符
  return desc.length > 30 ? desc.substring(0, 30) + '...' : desc
}

const openPropertyDetail = (p: any) => {
  router.push({ path: '/alarm-screen', query: { propertyId: p.id, propertyName: p.name } })
}
const viewAlarm = (a: any) => {
  alarmPopupPageData.value = a
  alarmPopupPageVisible.value = true
}
const navRoutes: Record<string, string> = {
  'duty-screen': '/duty-screen',
  'ai-analysis': '/eiot/firewatch/ai-analysis',
  'device-screen': '/device-screen',
  'remote-console': '/remote-console',
  'ops-workbench': '/ops-workbench',
  'data-center': '/data-center',
  'video-surveillance': '/video-surveillance',
  patrol: '/eiot/firewatch/patrol'
}
// 管理后台路径按角色区分：管理员/监管(super_admin/monitor_role) → property，企业用户 → enterprise
const getAdminPath = () => {
  const isAdmin = userStore.getRoles?.some(r => r === 'super_admin' || r === 'monitor_role')
  return isAdmin ? '/eiot/firewatch/property' : '/eiot/firewatch/enterprise'
}
const handleNavCommand = (command: string) => {
  if (command === 'admin') { goToAdmin(); return }
  const path = navRoutes[command]
  if (path && router.currentRoute.value.path !== path) {
    router.push(path)
  }
}
const goToAdmin = () => router.push(getAdminPath())
const goToDeviceScreen = () => router.push('/device-screen')
const toggleFullscreen = () => {
  if (!document.fullscreenElement) document.documentElement.requestFullscreen()
  else document.exitFullscreen()
}

// 面板显隐切换
const showPanels = ref(true)
const togglePanels = () => {
  showPanels.value = !showPanels.value
}

// 三维地图工具命令
const showScene = (scene: SceneName) => {
  if (currentScene.value !== scene) void router.replace({ query: { ...route.query, scene } })
}
const handleViewCommand = (command: string) => {
  if (command === 'park') { showScene('park'); return }
  if (command === 'map-reset') {
    if (currentScene.value === 'park') showScene('city')
    else dongguanMapRef.value?.resetView()
    return
  }
  if (currentScene.value !== 'city') return
  if (command === 'map-zoom-in') dongguanMapRef.value?.zoomIn()
  else if (command === 'map-zoom-out') dongguanMapRef.value?.zoomOut()
}

// Normalize only the scene selector; every other query parameter remains intact.
watch(rawScene, (value) => {
  const normalized = value === 'park' ? 'park' : 'city'
  if (value !== normalized) void router.replace({ query: { ...route.query, scene: normalized } })
}, { immediate: true })

const markAccessibleProperties = (snapshotId: string) => {
  const geometry = propertyMapRenderReport.value
  const labels = propertyMapVisualReport.value
  if (!geometry || !labels || geometry.snapshotId !== snapshotId || labels.snapshotId !== snapshotId) return
  const geometricIds = new Set(geometry.renderedPropertyIds)
  const sentIds = new Set(mapPoints.value.map(point => point.id))
  propertyMap.markApplied(snapshotId, [...labels.visiblePropertyIds, ...labels.clusteredPropertyIds, ...labels.hiddenPropertyIds].filter(id => geometricIds.has(id) && sentIds.has(id)))
}
const handlePropertyPointsApplied = (snapshotId: string, renderedPropertyIds: string[], excluded: { outOfBoundary: number; noSurface: number; invalid: number }, excludedIds?: { outOfBoundary: string[]; noSurface: string[]; invalid: string[] }) => {
  propertyMapRenderReport.value = { snapshotId, renderedPropertyIds, excluded, excludedIds }
  markAccessibleProperties(snapshotId)
}
const handlePropertyLabelsApplied = (snapshotId: string, visiblePropertyIds: string[], clusteredPropertyIds: string[], hiddenPropertyIds: string[], offscreenPropertyIds: string[]) => {
  propertyMapVisualReport.value = { snapshotId, visiblePropertyIds, clusteredPropertyIds, hiddenPropertyIds, offscreenPropertyIds }
  markAccessibleProperties(snapshotId)
}

const handleScenePointClick = async (pointId: string, kind: MapPoint['kind'], snapshotId: string) => {
  const point = findPoint(mapPoints.value, pointId)
  if (kind !== 'property' || snapshotId !== propertyMap.snapshotId.value || !point) return
  if (!point.navigable) return
  if (parkNavigationPending.value) return
  parkNavigationPending.value = true
  setParkNavigationStatus('正在复核园区入口并同步地图…')
  try {
    const verification = await propertyMap.verifyParkEntry(pointId)
    if (!verification.ok || !verification.snapshotId || !(await propertyMap.waitForApplied(verification.snapshotId, pointId))) {
      console.warn('[property-map] 园区入口身份、状态或地图回执无法确认，未进入园区')
      setParkNavigationStatus('园区入口复核失败，未切换场景', 4_000)
      ElMessage.warning('园区入口仍在复核，未切换场景；请刷新物业位置后重试')
      return
    }
    setParkNavigationStatus('园区入口已确认，正在进入园区…')
    if (currentScene.value !== 'park') await router.replace({ query: { ...route.query, scene: 'park' } })
    setParkNavigationStatus('')
  } catch (error) {
    console.warn('[property-map] 园区场景路由切换失败', error)
    setParkNavigationStatus('园区场景切换失败，请重试', 4_000)
    ElMessage.error('园区场景切换失败，请重试')
  } finally {
    parkNavigationPending.value = false
  }
}

const handleSceneBack = () => {
  showScene('city')
}

const handleSceneError = (error: { scene: SceneName; reason: string; retryable: boolean; message?: string }) => {
  console.warn('[DutyScreen] 场景降级', error)
}

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
  deviceStats.value = { total: 0, normal: 0, alarm: 0, fault: 0, offline: 0, inactive: 0 }
  todayAlarm.value = { totalAlarms: 0, handledCount: 0, pendingCount: 0, handleRate: 0 }
  alarmList.value = []
  dutyList.value = []
}

const loadData = async () => {
  // 仅首次加载时填充兜底数据，后续刷新保留旧数据避免页面闪烁
  if (!dataLoaded) {
    loadMockData()
    dataLoaded = true
  }

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
    }
    if (d.todayAlarm)
      todayAlarm.value = {
        totalAlarms: d.todayAlarm.totalAlarms || 0,
        handledCount: d.todayAlarm.handledCount || 0,
        pendingCount: d.todayAlarm.pendingCount || 0,
        handleRate: d.todayAlarm.handleRate ? Math.round(d.todayAlarm.handleRate * 100) / 100 : 0
      }
    if (d.realtimeAlarms?.length) alarmList.value = d.realtimeAlarms
    if (d.alarmTrend?.length) {
      cachedAlarmTrend = d.alarmTrend
      updateTrendChart(d.alarmTrend)
    }
    // ===== 从 statistics 中提取值班/物业数据（统一租户隔离） =====
    // 当前值班列表
    if (d.currentDutyList?.length) {
      dutyList.value = d.currentDutyList.slice(0, 3).map((i: any) => ({
        id: i.id,
        userName: i.userName,
        phone: i.phone || i.mobile || '',
        shiftName: i.shiftName,
        startTime: i.startTime || '08:00',
        endTime: i.endTime || '18:00',
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
    }
  }

  // ========== 2. 告警列表兜底（仅在 statistics 接口失败时请求） ==========
  if (!statsRes) {
    const r = await silentGet('/eiot/firewatch/dashboard/realtime-alarms', { limit: 15 })
    if (r?.length) alarmList.value = r
  }

  // ========== 3. 物业列表（企业信息展示；地图点位由授权物业 list 独立管理） ==========
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
    }
  }

  // ========== 4. 值班数据兜底 ==========
  if (!dutyList.value.length) {
    const dutyRes = await silentGet('/eiot/firewatch/duty-schedule/current-duty')
    if (dutyRes) {
      const arr = Array.isArray(dutyRes) ? dutyRes : [dutyRes]
      dutyList.value = arr.slice(0, 3).map((i: any) => ({
        id: i.id,
        userName: i.dutyPersonName || i.userName,
        phone: i.phone || i.mobile || '',
        shiftName: i.shiftName,
        startTime: i.startTime ? String(i.startTime).substring(0, 5) : '08:00',
        endTime: i.endTime ? String(i.endTime).substring(0, 5) : '18:00',
        status: i.status || 'ongoing'
      }))
    }
  }

  // 检测新报警并触发弹窗（已禁用）
  // alarmPopupCheckNew(alarmList.value)
}

// Charts
const updateTrendChart = (data: any[]) => {
  if (!trendChart) return
  trendChart.setOption({
    xAxis: { data: data.map((d) => (d.date || '').substring(5)) },
    series: [
      { data: data.map((d) => d.alarmCount || 0) },
      { data: data.map((d) => d.handleCount || 0) }
    ]
  })
}

const initCharts = () => {
  // 告警趋势日期（近7天），初始数据为0，等待API覆盖
  const today = new Date()
  const trendDates: string[] = []
  const trendAlarms: number[] = []
  const trendHandled: number[] = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    trendDates.push(
      `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    )
    trendAlarms.push(0)
    trendHandled.push(0)
  }

  // 告警趋势
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['告警数', '处置数'], textStyle: { color: '#b0c0dd', fontSize: 12 }, top: 0 },
      grid: { left: '10%', right: '5%', top: '22%', bottom: '12%' },
      xAxis: {
        type: 'category',
        data: trendDates,
        axisLine: { lineStyle: { color: '#2c4f85' } },
        axisLabel: { color: '#b0c0dd', fontSize: 12 }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#1e3a62' } },
        axisLabel: { color: '#b0c0dd', fontSize: 12 }
      },
      series: [
        {
          name: '告警数',
          type: 'line',
          smooth: true,
          data: trendAlarms,
          lineStyle: { color: '#ff4d4f', width: 2 },
          itemStyle: { color: '#ff4d4f' },
          symbolSize: 4,
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(255,77,79,0.35)' },
              { offset: 1, color: 'rgba(255,77,79,0.02)' }
            ])
          }
        },
        {
          name: '处置数',
          type: 'line',
          smooth: true,
          data: trendHandled,
          lineStyle: { color: '#52c41a', width: 2 },
          itemStyle: { color: '#52c41a' },
          symbolSize: 4
        }
      ]
    })
  }
}

const refreshCharts = () => {
  // loadData 先于 initCharts 执行，此时图表实例已创建，用已加载的数据刷新
  if (cachedAlarmTrend?.length) updateTrendChart(cachedAlarmTrend)
}

const updateMapSafeViewport = () => {
  const frame = document.querySelector<HTMLIFrameElement>('.scene-host__frame')
  if (!frame) return
  const frameRect = frame.getBoundingClientRect()
  if (frameRect.width <= 0 || frameRect.height <= 0 || frame.clientWidth <= 0 || frame.clientHeight <= 0) return
  const scaleX = frame.clientWidth / frameRect.width
  const scaleY = frame.clientHeight / frameRect.height
  const header = document.querySelector<HTMLElement>('.duty-screen .header')?.getBoundingClientRect()
  const left = document.querySelector<HTMLElement>('.duty-screen .left-panel')?.getBoundingClientRect()
  const right = document.querySelector<HTMLElement>('.duty-screen .right-panel')?.getBoundingClientRect()
  const margin = 12
  const localLeft = Math.max(margin, left && left.right > frameRect.left ? (left.right - frameRect.left + margin) * scaleX : margin)
  const localRight = Math.min(frame.clientWidth - margin, right && right.left < frameRect.right ? (right.left - frameRect.left - margin) * scaleX : frame.clientWidth - margin)
  const localTop = Math.max(margin, header && header.bottom > frameRect.top ? (header.bottom - frameRect.top + margin) * scaleY : margin)
  const localBottom = frame.clientHeight - margin
  mapSafeViewport.value = localRight > localLeft && localBottom > localTop
    ? { width: frame.clientWidth, height: frame.clientHeight, safeRect: { left: localLeft, top: localTop, right: localRight, bottom: localBottom } }
    : { width: frame.clientWidth, height: frame.clientHeight, safeRect: { left: margin, top: margin, right: frame.clientWidth - margin, bottom: frame.clientHeight - margin } }
}

const handleResize = () => {
  trendChart?.resize()
  updateMapSafeViewport()
}

let timer: any = null,
  refreshTimer: any = null
onMounted(() => {
  updateDateTime()
  timer = setInterval(updateDateTime, 1000)
  loadData().then(() =>
    setTimeout(() => {
      initCharts()
      refreshCharts()
    }, 100)
  )
  refreshTimer = setInterval(loadData, 30000)

  // 每10秒拉取近5分钟未处置告警
  fetchRecentAlerts()
  alertPollingTimer = setInterval(fetchRecentAlerts, 10000)

  window.addEventListener('resize', handleResize)
  void nextTick(updateMapSafeViewport)
  window.setTimeout(updateMapSafeViewport, 300)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (refreshTimer) clearInterval(refreshTimer)
  if (alertPollingTimer) clearInterval(alertPollingTimer)
  stopAutoHide()
  trendChart?.dispose()
  closeAlarmAudio()
  window.removeEventListener('resize', handleResize)
  if (parkNavigationStatusTimer !== null) window.clearTimeout(parkNavigationStatusTimer)
  // alarmPopupCleanup()
})
</script>

<style lang="scss" scoped>
@use 'sass:color';
$bg1: #8091ad;
$bg2: #0d1f3c;
$bg3: #1a3558;
$bd: #2c4f85;
$t1: #fff;
$t2: #b0c0dd;
$glass-bg: rgba(10, 22, 40, 0.001);
$header-bg: rgba(10, 22, 40, 0.001);
$glass-border: rgba(40, 70, 110, 0.5);
$blue: #1890ff;
$red: #ff4d4f;
$orange: #fa8c16;
$green: #52c41a;
$cyan: #4fc3f7;

.api-error-bar {
  background: rgba($red, 0.85);
  backdrop-filter: blur(8px);
  color: #fff;
  padding: 6px 16px;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-shrink: 0;
  z-index: 999;
  position: relative;
}

.park-navigation-status {
  position: absolute;
  z-index: 13;
  left: 50%;
  top: 112px;
  transform: translateX(-50%);
  max-width: min(560px, calc(100vw - 32px));
  padding: 8px 14px;
  color: #fff5c7;
  font-size: 13px;
  background: rgba(21, 42, 62, .94);
  border: 1px solid rgba(255, 210, 91, .84);
  border-radius: 4px;
  box-shadow: 0 0 16px rgba(255, 190, 56, .24);
}
.duty-screen {
  width: 100vw;
  height: 100vh;
  color: $t1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif;
  position: relative;
  // 与设备控制室/监控大屏统一的深蓝背景，地图加载期间不露白
  background: linear-gradient(160deg, #0a1628 0%, #0d1f3c 50%, #0f2747 100%);
}

// ====== 全屏地图背景 ======
.map-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background: #06131c;
  :deep(.dongguan-map-frame) {
    width: 100%;
    height: 100%;
    display: block;
  }
}

// ====== Header ======
.header {
  height: 56px;
  background: linear-gradient(
    170deg,
    rgba(28, 52, 88, 0.97) 0%,
    rgba(28, 50, 82, 0.97) 35%,
    rgba(32, 60, 102, 0.97) 100%
  );
  border-bottom: 1px solid rgba(79, 195, 247, 0.15);
  box-shadow:
    0 2px 10px rgba(0, 0, 0, 0.18),
    0 0 1px rgba(79, 195, 247, 0.12),
    inset 0 1px 0 rgba(79, 195, 247, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  position: relative;
  z-index: 10;

  .header-left {
    .title {
      font-size: 24px;
      font-weight: bold;
      background: linear-gradient(90deg, #fff, #7ec8f8, #54a0ff);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      margin: 0;
      letter-spacing: 3px;
    }
    .subtitle {
      font-size: 12px;
      color: $t2;
      letter-spacing: 1px;
      margin-top: 1px;
      opacity: 0.7;
    }
  }
  .header-center {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    .datetime {
      display: flex;
      align-items: baseline;
      gap: 16px;
      .date {
        color: $t2;
        font-size: 16px;
        font-weight: 500;
        letter-spacing: 0.5px;
      }
      .time {
        font-size: 24px;
        font-weight: 800;
        color: $blue;
        letter-spacing: 1px;
        font-variant-numeric: tabular-nums;
      }
      .week {
        color: $t2;
        font-size: 16px;
        font-weight: 500;
      }
    }
  }
  .header-right {
    display: flex;
    align-items: center;
    // 统一所有直接子元素（含el-tooltip/el-dropdown包裹层）的间距
    > * {
      margin-left: 8px;
      &:first-child {
        margin-left: 0;
      }
    }
    .header-icon-btn {
      background: transparent;
      border: none;
      color: $t2;
      padding: 4px 6px;
      &:hover {
        color: $blue;
      }
      &.active {
        color: $blue;
      }
      .el-icon {
        font-size: 18px;
      }
    }
    .user-btn {
      padding: 4px 8px;
      font-size: 15px;
    }
  }
}

// ====== Layout ======
.main-content {
  flex: 1;
  display: flex;
  padding: 10px;
  gap: 10px;
  min-height: 0;
  position: relative;
  z-index: 1;
  pointer-events: none;
}
.left-panel {
  width: 400px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  pointer-events: auto;
  height: 100%;
  scrollbar-width: none;
  &::-webkit-scrollbar {
    display: none;
  }
}
.right-panel {
  margin-left: auto;
  width: 400px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  pointer-events: auto;
  height: 100%;
  scrollbar-width: none;
  &::-webkit-scrollbar {
    display: none;
  }
}

// ====== Panel Card（HUD 科技感） ======
.panel-card {
  flex: 1;
  min-height: 0;
  background: linear-gradient(
    170deg,
    rgba(28, 52, 88, 0.97) 0%,
    rgba(28, 50, 82, 0.97) 35%,
    rgba(32, 60, 102, 0.97) 100%
  );
  border: 2px solid rgb(48 191 255 / 22%);
  border-radius: 8px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  overflow: visible;
  position: relative;
  transition: all 0.5s cubic-bezier(0.23, 1, 0.32, 1);
  box-shadow:
    0 2px 10px rgba(0, 0, 0, 0.18),
    0 0 1px rgba(79, 195, 247, 0.12),
    inset 0 1px 0 rgba(79, 195, 247, 0.06),
    inset 0 0 40px rgba(79, 195, 247, 0.02);
  transform-style: preserve-3d;

  &::before {
    content: '';
    position: absolute;
    top: -1px;
    left: -1px;
    width: 24px;
    height: 24px;
    border-top: 2px solid rgba(79, 195, 247, 0.5);
    border-left: 2px solid rgba(79, 195, 247, 0.5);
    border-radius: 8px 0 0 0;
    box-shadow: 0 0 8px rgba(79, 195, 247, 0.15);
    z-index: 2;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: -1px;
    right: -1px;
    width: 24px;
    height: 24px;
    border-bottom: 2px solid rgba(79, 195, 247, 0.5);
    border-right: 2px solid rgba(79, 195, 247, 0.5);
    border-radius: 0 0 8px 0;
    box-shadow: 0 0 8px rgba(79, 195, 247, 0.15);
    z-index: 2;
  }

  .card-content {
    flex: 1;
    overflow-y: auto;
    min-height: 0;
    padding: 10px;
    border: none;
    scrollbar-width: none;
    &::-webkit-scrollbar {
      display: none;
    }
  }
  .card-header {
    padding: 12px 16px;
    background: transparent;
    border-bottom: 1px solid rgba(79, 195, 247, 0.18);
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 20%;
      bottom: 20%;
      width: 3px;
      background: rgba(79, 195, 247, 0.9);
      border-radius: 0 2px 2px 0;
      box-shadow: 0 0 6px rgba(79, 195, 247, 0.5);
    }
    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 17px;
      font-weight: bold;
      color: #fff;
      letter-spacing: 0.5px;
      .el-icon {
        color: rgba(79, 195, 247, 0.9);
        font-size: 18px;
      }
    }
    .badge {
      background: rgba(79, 195, 247, 0.25);
      color: rgba(79, 195, 247, 1);
      padding: 2px 10px;
      border-radius: 10px;
      font-size: 14px;
      margin-left: 4px;
      border: 1px solid rgba(79, 195, 247, 0.2);
      &.red {
        background: rgba($red, 0.2);
        color: $red;
        border-color: rgba($red, 0.25);
      }
    }
    .card-header-arrow {
      color: $t2;
      font-size: 16px;
      transition: transform 0.25s;
    }
    .view-toggle {
      display: flex;
      gap: 8px;
      .el-icon {
        cursor: pointer;
        color: $t2;
        font-size: 18px;
        &.active {
          color: rgba(79, 195, 247, 0.9);
        }
        &:hover {
          color: rgba(79, 195, 247, 0.9);
        }
      }
    }
  }
}

.panel-card.card-auto {
  flex: 0 auto;
  .card-content {
    flex: 0 auto;
    overflow: visible;
  }
}

// ====== Enterprise Overview ======
.ent-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.45) 0%, rgba(24, 144, 255, 0.28) 100%);
  border: none;
  border-radius: 6px;
  margin-bottom: 2px;
  cursor: pointer;
  transition: all 0.25s;
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: $blue;
    border-radius: 0 2px 2px 0;
    box-shadow: 0 0 8px rgba($blue, 0.6);
  }
  &:hover {
    background: linear-gradient(135deg, rgba(24, 144, 255, 0.52) 0%, rgba(24, 144, 255, 0.3) 100%);
    border-color: rgba(24, 144, 255, 0.5);
    .ent-header-arrow {
      transform: translateX(4px);
    }
  }
  .ent-header-left {
    display: flex;
    align-items: center;
    gap: 8px;
    .el-icon {
      color: rgba(79, 195, 247, 0.9);
      font-size: 19px;
    }
    .ent-header-title {
      font-size: 17px;
      font-weight: bold;
      color: #fff;
      letter-spacing: 0.5px;
    }
  }
  .ent-header-right {
    display: flex;
    align-items: center;
    gap: 8px;
    .ent-header-badge {
      font-size: 14px;
      color: rgba(79, 195, 247, 1);
      font-weight: 600;
    }
    .ent-header-arrow {
      font-size: 16px;
      color: rgba(79, 195, 247, 0.9);
      transition: transform 0.25s;
    }
  }
}
.ent-content {
  padding: 6px 10px !important;
}
.ent-name-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ent-name-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  &:hover {
    background: rgba($blue, 0.1);
  }
  .eni-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    flex-shrink: 0;
    &.risk-dot-normal {
      background: $green;
      box-shadow: 0 0 4px rgba($green, 0.4);
    }
    &.risk-dot-major {
      background: $orange;
      box-shadow: 0 0 4px rgba($orange, 0.4);
    }
    &.risk-dot-critical {
      background: $red;
      box-shadow: 0 0 4px rgba($red, 0.4);
    }
    &.risk-dot-minor {
      background: $blue;
    }
  }
  .eni-name {
    flex: 1;
    font-size: 14px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .eni-risk {
    font-size: 12px;
    padding: 1px 7px;
    border-radius: 8px;
    white-space: nowrap;
    flex-shrink: 0;
    font-weight: 500;
    &.risk-critical {
      background: rgba($red, 0.15);
      color: $red;
    }
    &.risk-major {
      background: rgba($orange, 0.15);
      color: $orange;
    }
    &.risk-normal {
      background: rgba($green, 0.15);
      color: $green;
    }
    &.risk-minor {
      background: rgba($blue, 0.1);
      color: $t2;
    }
  }
}

.risk-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  &.risk-critical {
    background: rgba($red, 0.2);
    color: $red;
  }
  &.risk-major {
    background: rgba($orange, 0.2);
    color: $orange;
  }
  &.risk-normal {
    background: rgba($green, 0.2);
    color: $green;
  }
  &.risk-minor {
    background: rgba($blue, 0.15);
    color: $t2;
  }
}

// ====== Device Stats ======
.device-stats-card {
  cursor: pointer;
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
  &:hover {
    border-color: rgba($blue, 0.3);
    box-shadow:
      0 2px 10px rgba(0, 0, 0, 0.18),
      0 0 12px rgba($blue, 0.12),
      inset 0 1px 0 rgba(79, 195, 247, 0.04);
    .card-header-arrow {
      transform: translateX(4px);
      color: $cyan;
    }
  }
}
.device-hero {
  text-align: center;
  padding: 6px 0 10px;
  border-bottom: 1px dashed rgba($bd, 0.35);
  margin-bottom: 10px;
  .hero-num {
    font-size: 38px;
    font-weight: bold;
    background: linear-gradient(180deg, #fff, $blue);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  .hero-unit {
    font-size: 15px;
    color: $t2;
    margin-left: 4px;
  }
}
.device-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
  .bar-item {
    display: flex;
    align-items: center;
    gap: 8px;
    .bar-label {
      font-size: 14px;
      color: $t2;
      width: 32px;
      text-align: right;
    }
    .bar-track {
      flex: 1;
      height: 8px;
      background: rgba($bd, 0.4);
      border-radius: 4px;
      overflow: hidden;
    }
    .bar-fill {
      height: 100%;
      border-radius: 4px;
      transition: width 0.8s ease;
    }
    .bar-val {
      font-size: 15px;
      font-weight: bold;
      width: 32px;
    }
    &.normal {
      .bar-fill {
        background: linear-gradient(90deg, #52c41a, #73d13d);
      }
      .bar-val {
        color: $green;
      }
    }
    &.alarm {
      .bar-fill {
        background: linear-gradient(90deg, #ff4d4f, #ff7875);
      }
      .bar-val {
        color: $red;
      }
    }
    &.fault {
      .bar-fill {
        background: linear-gradient(90deg, #fa8c16, #ffc53d);
      }
      .bar-val {
        color: $orange;
      }
    }
    &.offline {
      .bar-fill {
        background: linear-gradient(90deg, #595959, #8c8c8c);
      }
      .bar-val {
        color: #8c8c8c;
      }
    }
    &.inactive {
      .bar-fill {
        background: linear-gradient(90deg, #bfbfbf, #d9d9d9);
      }
      .bar-val {
        color: #bfbfbf;
      }
    }
  }
}

// ====== Bottom Row ======
.chart-box {
  width: 100%;
  height: 130px;
}

// ====== Stat Grid ======
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  .stat-cell {
    text-align: center;
    padding: 10px 4px;
    border-radius: 6px;
    background: rgba($blue, 0.06);
    border: 1px solid $glass-border;
    transition: border-color 0.3s;
    &:hover {
      border-color: rgba($blue, 0.3);
    }
    .sv {
      font-size: 24px;
      font-weight: bold;
    }
    .sl {
      font-size: 13px;
      color: $t2;
      margin-top: 3px;
    }
    &.total .sv {
      color: $blue;
    }
    &.handled .sv {
      color: $green;
    }
    &.pending .sv {
      color: $orange;
    }
    &.rate .sv {
      color: $green;
    }
  }
}

// ====== Alarm ======
.alarm-panel {
  .card-content {
    overflow: hidden;
  }
}
.alarm-inner {
  &.scrolling {
    animation: scrollUp 18s linear infinite;
  }
}
.alarm-item {
  display: flex;
  align-items: flex-start;
  padding: 7px 8px;
  margin-bottom: 5px;
  background: rgba($red, 0.08);
  border-radius: 6px;
  border-left: 3px solid $red;
  cursor: pointer;
  transition: background 0.2s;
  &:hover {
    background: rgba($red, 0.18);
  }
  &.level-critical {
    border-left-color: $red;
    background: rgba($red, 0.12);
  }
  &.level-major {
    border-left-color: $orange;
    background: rgba($orange, 0.08);
  }
  &.level-warning {
    border-left-color: #fadb14;
    background: rgba(#fadb14, 0.06);
  }
  .alarm-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: $red;
    margin-top: 5px;
    margin-right: 8px;
    flex-shrink: 0;
    animation: blink 2s ease-in-out infinite;
  }
  .alarm-body {
    flex: 1;
    .alarm-name {
      font-size: 16px;
      font-weight: 500;
    }
    .alarm-desc {
      font-size: 16px;
      color: $t2;
      margin-top: 2px;
    }
    .alarm-time {
      font-size: 14px;
      color: $t2;
      margin-top: 1px;
    }
  }
  .alarm-tag {
    font-size: 14px;
    padding: 2px 6px;
    border-radius: 4px;
    white-space: nowrap;
    &.tag-critical {
      background: rgba($red, 0.2);
      color: $red;
    }
    &.tag-major {
      background: rgba($orange, 0.2);
      color: $orange;
    }
  }
}

// ====== Duty ======
.duty-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.duty-card {
  background: rgba($blue, 0.06);
  border: 1px solid rgba($blue, 0.12);
  border-radius: 8px;
  padding: 12px 14px;
  transition: border-color 0.25s;
  &:hover {
    border-color: rgba($blue, 0.25);
  }
  .duty-card-top {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;
    .duty-avatar {
      width: 42px;
      height: 42px;
      border-radius: 50%;
      background: linear-gradient(135deg, rgba($blue, 0.5), rgba($blue, 0.3));
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      color: rgba(#fff, 0.9);
      flex-shrink: 0;
      box-shadow: 0 2px 8px rgba($blue, 0.25);
    }
    .duty-info {
      flex: 1;
      .duty-name {
        font-size: 17px;
        font-weight: 600;
        color: #fff;
      }
      .duty-phone {
        font-size: 14px;
        color: $t2;
        margin-top: 2px;
        letter-spacing: 0.5px;
      }
    }
  }
  .duty-card-bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    .duty-time-block {
      display: flex;
      align-items: center;
      gap: 6px;
      background: rgba($bg3, 0.7);
      border: 1px solid rgba($bd, 0.4);
      border-radius: 6px;
      padding: 6px 12px;
      font-size: 15px;
      color: #fff;
      .el-icon {
        font-size: 16px;
        color: $t2;
      }
    }
    .duty-status-btn {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 6px 14px;
      border-radius: 6px;
      font-size: 15px;
      font-weight: 600;
      cursor: default;
      &.ongoing {
        background: rgba($green, 0.15);
        color: $green;
        border: 1px solid rgba($green, 0.25);
      }
      &.pending {
        background: rgba($orange, 0.15);
        color: $orange;
        border: 1px solid rgba($orange, 0.25);
      }
      &.completed {
        background: rgba($blue, 0.12);
        color: $blue;
        border: 1px solid rgba($blue, 0.2);
      }
      .el-icon {
        font-size: 16px;
      }
    }
  }
}

// ====== Common ======
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: $green;
  .el-icon {
    font-size: 34px;
    margin-bottom: 6px;
  }
  &.small {
    padding: 10px;
    font-size: 14px;
    color: $t2;
    .el-icon {
      font-size: 22px;
    }
  }
}

// ====== Animations ======
@keyframes scrollUp {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-50%);
  }
}
@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

// ====== Dialog ======
:deep(.dark-dialog) {
  .el-dialog {
    background: $glass-bg;
    backdrop-filter: blur(16px);
    border: 1px solid $glass-border;
  }
  .el-dialog__title {
    color: $t1;
  }
  .el-dialog__body {
    color: $t1;
  }
}
.detail-body {
  .dr {
    display: flex;
    padding: 8px 0;
    border-bottom: 1px solid $glass-border;
    .dl {
      width: 70px;
      color: $t2;
      font-size: 15px;
    }
    .dv {
      flex: 1;
      font-size: 15px;
      &.risk-critical {
        color: $red;
      }
      &.risk-major {
        color: $orange;
      }
      &.risk-normal {
        color: $green;
      }
    }
  }
  .section-title {
    font-size: 15px;
    font-weight: 500;
    margin-top: 12px;
    margin-bottom: 8px;
    color: $blue;
  }
  .device-status-row {
    display: flex;
    gap: 14px;
    font-size: 14px;
    .ds-normal {
      color: $green;
    }
    .ds-alarm {
      color: $red;
    }
    .ds-fault {
      color: $orange;
    }
    .ds-offline {
      color: $t2;
    }
  }
}
:deep(.amap-container) {
  .amap-maps {
    width: 100% !important;
    height: 100% !important;
  }
}

// ====== 报警弹窗样式 ======
.alarm-popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(4px);
  animation: alarmFadeIn 0.3s ease;

  // 等级闪烁动画
  &.flash-critical {
    animation: alarmFadeIn 0.3s ease, flashCritical 0.8s ease-in-out infinite;
  }
  &.flash-major {
    animation: alarmFadeIn 0.3s ease, flashMajor 1.2s ease-in-out infinite;
  }
  &.flash-warning {
    animation: alarmFadeIn 0.3s ease, flashWarning 1.8s ease-in-out infinite;
  }
}

@keyframes alarmFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes flashCritical {
  0%, 100% { box-shadow: inset 0 0 80px rgba(255, 77, 79, 0.0), 0 0 0 rgba(255, 77, 79, 0); }
  50% { box-shadow: inset 0 0 80px rgba(255, 77, 79, 0.35), 0 0 60px rgba(255, 77, 79, 0.25); }
}

@keyframes flashMajor {
  0%, 100% { box-shadow: inset 0 0 60px rgba(250, 140, 22, 0.0), 0 0 0 rgba(250, 140, 22, 0); }
  50% { box-shadow: inset 0 0 60px rgba(250, 140, 22, 0.25), 0 0 40px rgba(250, 140, 22, 0.2); }
}

@keyframes flashWarning {
  0%, 100% { box-shadow: inset 0 0 40px rgba(250, 219, 20, 0.0), 0 0 0 rgba(250, 219, 20, 0); }
  50% { box-shadow: inset 0 0 40px rgba(250, 219, 20, 0.15), 0 0 30px rgba(250, 219, 20, 0.12); }
}

.alarm-popup-card {
  width: 680px;
  max-width: 90vw;
  background: #0f2747;
  border: 1px solid rgba(59, 130, 246, 0.4);
  border-radius: 12px;
  overflow: hidden;
  box-shadow:
    0 0 40px rgba(255, 77, 79, 0.25),
    0 16px 48px rgba(0, 0, 0, 0.4);
  animation: alarmSlideIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes alarmSlideIn {
  from { opacity: 0; transform: scale(0.95) translateY(12px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.alarm-countdown-bar {
  height: 3px;
  background: rgba(30, 58, 95, 0.5);
  .alarm-countdown-fill {
    height: 100%;
    background: linear-gradient(90deg, #ff6b6b, #ff4d4f);
    transition: width 1s linear;
    border-radius: 0 2px 2px 0;
  }
}

.alarm-popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: linear-gradient(135deg, rgba(255, 77, 79, 0.12) 0%, transparent 100%);
  border-bottom: 1px solid rgba(30, 58, 95, 0.8);
}

.alarm-popup-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.alarm-popup-icon {
  font-size: 20px;
  animation: alarmIconPulse 1.5s ease-in-out infinite;
}

@keyframes alarmIconPulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.15); }
}

.alarm-popup-type {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.alarm-popup-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.alarm-popup-severity {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  &.severity-critical { background: rgba(255, 77, 79, 0.2); color: #ff8a8a; border: 1px solid rgba(255, 77, 79, 0.4); }
  &.severity-major { background: rgba(250, 140, 22, 0.2); color: #ffa940; border: 1px solid rgba(250, 140, 22, 0.4); }
  &.severity-warning { background: rgba(250, 219, 20, 0.2); color: #fadb14; border: 1px solid rgba(250, 219, 20, 0.4); }
  &.severity-info { background: rgba(24, 144, 255, 0.2); color: #40a9ff; border: 1px solid rgba(24, 144, 255, 0.4); }
}

.alarm-popup-timer {
  font-size: 14px;
  font-weight: 700;
  color: #ff6b6b;
  font-variant-numeric: tabular-nums;
  min-width: 32px;
  text-align: center;
}

.alarm-sound-toggle {
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
  &:hover { transform: scale(1.2); }
  &.muted { opacity: 0.5; }
}

.alarm-popup-queue {
  font-size: 11px;
  color: #8b9dc5;
  background: rgba(30, 58, 95, 0.6);
  padding: 2px 8px;
  border-radius: 8px;
}

.alarm-popup-body {
  display: flex;
  min-height: 280px;
  padding: 0;
}

.alarm-popup-image {
  width: 360px;
  min-height: 280px;
  flex-shrink: 0;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.alarm-popup-no-image {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  .no-image-icon { font-size: 48px; opacity: 0.4; }
  .no-image-text { font-size: 13px; color: #6b7b9a; }
}

.alarm-popup-info {
  flex: 1;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alarm-info-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.alarm-info-label {
  font-size: 13px;
  color: #8b9dc5;
  flex-shrink: 0;
  min-width: 70px;
}

.alarm-info-value {
  font-size: 14px;
  color: #fff;
  font-weight: 500;
  flex: 1;
}

.alarm-detail-text {
  line-height: 1.5;
  color: #c8d6e5;
  font-weight: 400;
  word-break: break-all;
}

.alarm-popup-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid rgba(30, 58, 95, 0.8);
  background: rgba(15, 39, 71, 0.6);
}

.alarm-btn {
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
  letter-spacing: 0.5px;
}

.alarm-btn-later {
  background: transparent;
  border-color: rgba(30, 58, 95, 0.8);
  color: #8b9dc5;
  &:hover { background: rgba(30, 58, 95, 0.5); border-color: #3b82f6; color: #fff; }
}

.alarm-btn-now {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff6b6b 100%);
  border-color: #ff4d4f;
  color: #fff;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.3);
  &:hover { background: linear-gradient(135deg, #ff7875 0%, #ff8a8a 100%); }
}
</style>

<style lang="scss">
// 暗色毛玻璃下拉菜单
.header-dropdown-popper {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0 !important;
  .el-dropdown-menu {
    background: rgba(15, 32, 55, 0.92) !important;
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(40, 70, 110, 0.45) !important;
    border-radius: 6px !important;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.5) !important;
    padding: 4px 0 !important;
  }
  .el-dropdown-menu__item {
    background: transparent !important;
    color: #b0c0dd !important;
    font-size: 15px;
    padding: 6px 16px;
    &:hover {
      background: rgba(24, 144, 255, 0.15) !important;
      color: #fff !important;
    }
  }
  .el-popper__arrow::before {
    background: rgba(15, 32, 55, 0.92);
    border: 1px solid rgba(40, 70, 110, 0.45);
  }
}

// 视图切换下拉菜单（2D/3D + 地图类型）
.view-dropdown-popper {
  .view-dropdown-content {
    background: rgba(15, 32, 55, 0.92);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(40, 70, 110, 0.45);
    border-radius: 6px;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.5);
    padding: 10px 14px;
    min-width: 200px;
  }
  .view-row-label {
    font-size: 13px;
    color: #7a90b5;
    margin-bottom: 6px;
    margin-top: 4px;
    letter-spacing: 0.5px;
  }
  .view-row {
    display: flex;
    gap: 6px;
    margin-bottom: 6px;
    &:last-child {
      margin-bottom: 0;
    }
  }
  .view-toggle-item {
    flex: 1;
    text-align: center;
    padding: 6px 10px;
    border-radius: 5px;
    font-size: 14px;
    color: #b0c0dd;
    background: rgba(40, 70, 110, 0.3);
    border: 1px solid transparent;
    cursor: pointer;
    transition: all 0.2s;
    user-select: none;
    &:hover {
      background: rgba(24, 144, 255, 0.15);
      color: #fff;
    }
    &.active {
      background: rgba(24, 144, 255, 0.25);
      color: #fff;
      border-color: rgba(24, 144, 255, 0.4);
    }
  }
}
</style>
