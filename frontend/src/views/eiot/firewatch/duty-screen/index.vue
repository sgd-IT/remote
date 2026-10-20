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
    <div
      v-if="parkNavigationStatus"
      class="park-navigation-status"
      role="status"
      aria-live="polite"
      >{{ parkNavigationStatus }}</div
    >
    <header class="header">
      <div class="header-left">
        <div class="title-block">
          <h1 class="title">安为智慧消防值守平台</h1>
          <p class="subtitle">ANWEI SMART FIRE DUTY PLATFORM</p>
        </div>
        <el-select
          v-if="isSuperAdmin"
          v-model="selectedTenantId"
          size="small"
          class="tenant-select"
          popper-class="tenant-select-popper"
          @change="handleTenantChange"
        >
          <el-option :value="0" label="全部租户" />
          <el-option v-for="t in tenantOptions" :key="t.id" :value="t.id" :label="t.name" />
        </el-select>
        <span v-else class="tenant-name">{{ loginTenant.tenantName || '-' }}</span>
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
              <!-- <el-dropdown-item command="data-center"
                ><el-icon><TrendCharts /></el-icon>数据中心<DevelopingTag
                  mini
                  tip="统计接口后端尚未实现，当前为示例数据"
              /></el-dropdown-item> -->
              <el-dropdown-item command="video-surveillance"
                ><el-icon><VideoCamera /></el-icon>视频监控</el-dropdown-item
              >
              <el-dropdown-item command="patrol"
                ><el-icon><Avatar /></el-icon>查岗管理</el-dropdown-item
              >
              <!-- <el-dropdown-item divided command="ops-workbench"
                ><el-icon><SetUp /></el-icon>运维工作台<DevelopingTag
                  mini
                  tip="页面仅为占位，无任何业务接口"
              /></el-dropdown-item> -->
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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
                <div class="view-toggle-item" @click="handleViewCommand('map-zoom-in')">放大</div>
                <div class="view-toggle-item" @click="handleViewCommand('map-zoom-out')">缩小</div>
              </div>
            </div>
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
        <router-link
          class="panel-card duty-overview-card"
          to="/eiot/firewatch/duty"
          aria-label="进入值班管理"
        >
          <div class="card-header">
            <span class="card-title"
              ><el-icon><User /></el-icon>今日值班</span
            >
            <el-icon class="card-header-arrow"><ArrowRight /></el-icon>
          </div>
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
        </router-link>
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
              <div class="stat-cell pending" :class="{ flash: pendingFlash }">
                <div class="sv">{{ todayAlarm.pendingCount }}</div
                ><div class="sl">待处理</div>
                <Transition name="toast-fade">
                  <div v-if="pendingToast" class="pending-toast">二级报警，请处置</div>
                </Transition>
              </div>
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
          <div class="card-content alarm-scroll" ref="alarmScrollRef">
            <div
              class="alarm-inner"
              @mouseenter="scrollPaused = true"
              @mouseleave="scrollPaused = false"
            >
              <div
                class="alarm-item"
                v-for="alarm in alarmList"
                :key="alarm.id"
                :class="'level-' + alarm.alarmLevel"
                @click="onAlarmClick(alarm)"
              >
                <div class="alarm-dot"></div>
                <div class="alarm-body">
                  <div class="alarm-desc"
                    ><span class="alarm-tenant" v-if="alarm.tenantName">{{ alarm.tenantName }}</span
                    >{{ extractAlarmSummary(alarm.alarmDescription) }}</div
                  >
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

  </div>

  <!-- 二三级告警通知匣（层级低于一级全屏弹窗，锚在右栏今日警情卡左上侧，位置可微调） -->
  <AlarmNotice
    ref="alarmNoticeRef"
    :offset-right="414"
    :offset-top="66"
    show-tenant
    @open="onNoticeOpen"
  />

  <!-- 报警弹窗（公共组件：两屏共用一份实现，弹窗内动作经事件回到本页） -->
  <AlarmPopupDialog
    ref="popupRef"
    :dim-overlay="false"
    later-text="稍后处置"
    primary-text="去处置"
    @later="onHandleLater"
    @primary="onHandleNow"
    @overlay="onHandleLater"
  />
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
import AlarmNotice, { isLightAlarmLevel } from '@/views/eiot/firewatch/alarm-notice/index.vue'
import DevelopingTag from '@/components/DevelopingTag/index.vue'
import { useUserStore } from '@/store/modules/user'
import { useWebSocket } from '@vueuse/core'
import { getRefreshToken } from '@/utils/auth'
import {
  getRecentUnprocessedAlerts,
  handleAlertRecord,
  deferAlertRecord,
  findTransferDeviceId
} from '@/api/eiot/firewatch/firewatch.api'
import { getCamerasByDevice } from '@/api/eiot/video/deviceCameraRelation.api'
import { closeAlarmAudio } from '@/utils/alarmSound'
import SceneHost from './components/SceneHost.vue'
import { findPoint } from './components/scene-data'
import { type MapPoint, type SceneName } from './components/scene-types'
import { usePropertyMapData } from './components/usePropertyMapData'
import type { Viewport } from './components/scene-protocol'
import { getTenantPage } from '@/api/system/tenant'
import AlarmPopupDialog from '@/views/eiot/firewatch/components/AlarmPopupDialog.vue'

const router = useRouter()
const route = useRoute()
const rawScene = computed(() => {
  const value = route.query.scene
  return Array.isArray(value) ? value[0] : value
})
const currentScene = computed<SceneName>(() => (rawScene.value === 'park' ? 'park' : 'city'))
const dongguanMapRef = ref<InstanceType<typeof SceneHost>>()

//切换租户下拉框
const isSuperAdmin = computed(() => userStore.getRoles?.includes('super_admin'))
const tenantOptions = ref<{ id: number; name: string }[]>([]) //下拉选项
const selectedTenantId = ref(0) //0 =全部租户
const loginTenant = ref<{ tenantId?: number; tenantName?: string }>({}) //返回当前租户

// 超管：加载全部租户作为下拉选项
const loadTenantOptions = async () => {
  if (!isSuperAdmin.value) return // 非超管不发这个请求（会 403）
  try {
    const res = await getTenantPage({ pageNo: 1, pageSize: 100 })
    tenantOptions.value = (res?.list || [])
      .filter((t: any) => t.id !== 1) // 去掉系统租户，避免选到"全部视角"
      .map((t: any) => ({ id: t.id, name: t.name }))
  } catch (e) {
    console.error('[DutyScreen] 加载租户列表失败', e)
  }
}

// Refs
const trendChartRef = ref<HTMLElement>()

const propertyMap = usePropertyMapData(computed(() => currentScene.value === 'city'))
const mapPoints = computed<MapPoint[]>(() => propertyMap.points.value)
const mapSafeViewport = ref<Viewport | null>(null)
const propertyMapRenderReport = ref<{
  snapshotId: string
  renderedPropertyIds: string[]
  excluded: { outOfBoundary: number; noSurface: number; invalid: number }
  excludedIds?: { outOfBoundary: string[]; noSurface: string[]; invalid: string[] }
} | null>(null)
const propertyMapVisualReport = ref<{
  snapshotId: string
  visiblePropertyIds: string[]
  clusteredPropertyIds: string[]
  hiddenPropertyIds: string[]
  offscreenPropertyIds: string[]
} | null>(null)
const parkNavigationPending = ref(false)
const parkNavigationStatus = ref('')
let parkNavigationStatusTimer: number | null = null
const setParkNavigationStatus = (message: string, clearAfterMs = 0) => {
  parkNavigationStatus.value = message
  if (parkNavigationStatusTimer !== null) window.clearTimeout(parkNavigationStatusTimer)
  parkNavigationStatusTimer =
    clearAfterMs > 0
      ? window.setTimeout(() => {
          parkNavigationStatus.value = ''
          parkNavigationStatusTimer = null
        }, clearAfterMs)
      : null
}
watch(
  () => propertyMap.snapshotId.value,
  () => {
    propertyMapRenderReport.value = null
    propertyMapVisualReport.value = null
  }
)
const scrollPaused = ref(false)
// 实时告警滚动容器：自动滚动改成 scrollTop 驱动（原 CSS 动画会覆盖 transform，滚轮就翻不动了）
const alarmScrollRef = ref<HTMLElement>()
let alarmAutoScrollTimer: ReturnType<typeof setInterval> | null = null

// 实时告警自动滚动：约 20px/s；悬停暂停（点选/滚轮期间不抢滚动），到底后回顶部循环
const startAlarmAutoScroll = () => {
  if (alarmAutoScrollTimer) return
  alarmAutoScrollTimer = setInterval(() => {
    const el = alarmScrollRef.value
    if (!el || scrollPaused.value) return
    if (el.scrollHeight <= el.clientHeight + 1) return
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 1) {
      el.scrollTop = 0
    } else {
      el.scrollTop += 1
    }
  }, 50)
}

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

// ==================== 报警弹窗逻辑（一级弹窗由公共组件 AlarmPopupDialog 承载） ====================
const alarmNoticeRef = ref<InstanceType<typeof AlarmNotice> | null>(null)
// 一级报警弹窗组件：enqueue=入队告警，close=关闭弹窗；弹窗内动作经 later/primary/overlay 事件回到本页
const popupRef = ref<InstanceType<typeof AlarmPopupDialog> | null>(null)
// 通知匣（二三级）已推送过的告警 id；一级不复用此集合（一级的周期重复提醒由弹窗组件内部管理）
const alertShownIds = ref<Set<number>>(new Set())
let alertPollingTimer: ReturnType<typeof setInterval> | null = null

// 二级告警到达时，在「待处理」格子弹一个 Toast 提示
const pendingFlash = ref(false)
const pendingToast = ref(false)
let pendingToastTimer: ReturnType<typeof setTimeout> | null = null
const flashPendingToast = () => {
  // 格子高亮闪烁
  pendingFlash.value = true
  setTimeout(() => { pendingFlash.value = false }, 1200)
  // Toast 文案 3 秒后自动消失
  pendingToast.value = true
  if (pendingToastTimer) clearTimeout(pendingToastTimer)
  pendingToastTimer = setTimeout(() => { pendingToast.value = false }, 3000)
}



/** 跳转到告警处置现场：优先用传设备（远程控制中心），无则关联摄像头（视频监控）；两者都无则不跳转仅提示 */
const jumpToAlarmScene = async (deviceId: string | number) => {
  try {
    const res = await findTransferDeviceId(Number(deviceId))
    const transferId = typeof res === 'number' ? res : (res as any)?.data
    if (transferId) {
      // 找到用传设备 → 跳转远程控制中心
      router.push({
        path: '/remote-console',
        query: { deviceId: String(transferId), alertDeviceId: String(deviceId) }
      })
      return
    }
  } catch {
    // 查找失败 → 走下方摄像头兜底
  }
  // 非用传设备 → 有联动摄像头才跳视频监控；无摄像头则不跳，仅提示
  let cameraId: string | undefined
  try {
    const cams = await getCamerasByDevice(Number(deviceId))
    if (Array.isArray(cams) && cams.length > 0) cameraId = String(cams[0].id)
  } catch {}
  if (!cameraId) {
    ElMessage.warning('该设备未关联摄像头，无法跳转')
    return
  }
  router.push({ path: '/video-surveillance', query: { cameraId } })
}

/** 实时告警条目点击：跳到火警处置中心（alarm-order），由其在加载后按 ?id= 选中并打开对应告警详情弹窗 */
const onAlarmClick = (alarm: any) => {
  router.push({ path: '/eiot/firewatch/alarm-order', query: { id: alarm.id } })
}

const onHandleNow = async (alarm: any) => {
  if (alarm?.id) {
    try {
      await handleAlertRecord(alarm.id)
    } catch (e) {
      console.warn('处置告警失败:', e)
    }
  }
  const deviceId = alarm?.deviceId
  popupRef.value?.close()
  if (deviceId) {
    await jumpToAlarmScene(deviceId)
  }
}

/** 通知匣条目点击：跳转到该告警的处置现场（不写处置状态，处置操作由处置现场页完成） */
const onNoticeOpen = (alarm: any) => {
  const deviceId = alarm?.deviceId
  if (deviceId) {
    void jumpToAlarmScene(deviceId)
  } else {
    ElMessage.warning('该告警未关联设备，无法跳转处置现场')
  }
}

// 稍后处置：defer 落库成功（handle_status=1）即静默——fetchRecentAlerts 一级过滤会按落库状态跳过它，不再弹窗；
// 落库失败时状态停留在 0，该告警照常周期重弹，宁可打扰也不漏警
const onHandleLater = async (alarm: any) => {
  if (alarm?.id) {
    try {
      await deferAlertRecord(alarm.id)
    } catch (e) {
      // 静默失败
    }
  }
  popupRef.value?.close()
}

const fetchRecentAlerts = async () => {
  try {
    const res = await getRecentUnprocessedAlerts()
    const alerts = (Array.isArray(res) ? res : res?.data) || []
    console.log('[DutyScreen] fetchRecentAlerts =>', alerts.length, '条告警')

    // 一级（含缺省/未知，就高不就低）走全屏弹窗：不做一次性去重，未处置的按周期重复提醒直到处置（判定在弹窗组件内）；
    // 「稍后处置」过的（handle_status=1）跳过——人工确认以落库状态为准，之后任何刷新/屏幕都不再弹，只在实时告警列表/待处理里处置
    alerts
      .filter((a: any) => !isLightAlarmLevel(a.level) && a.handleStatus !== 1)
      .forEach((a: any) => popupRef.value?.enqueue(a))

    // 二三级进通知匣：页面生命周期内每条只推一次
    alerts
      .filter((a: any) => isLightAlarmLevel(a.level) && !alertShownIds.value.has(a.id))
      .forEach((a: any) => {
        alertShownIds.value.add(a.id)
        alarmNoticeRef.value?.push(a)
      })
    // 有二三级告警时，在「待处理」格子弹 Toast
    if (alerts.some((a: any) => isLightAlarmLevel(a.level))) {
      flashPendingToast()
    }
  } catch (e) {
    // 静默失败
  }
}

// WebSocket 连接（实时推送增强）
const wsServer = ref(
  (import.meta.env.VITE_BASE_URL + '/infra/ws').replace('http', 'ws') +
    '?token=' +
    getRefreshToken()
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
        if (alertId) {
          const item = {
            id: alertId,
            name: content.alertName || '设备报警',
            deviceId: content.deviceId,
            alertTime: content.alertTime,
            alertImage: content.alertImage || null,
            level: content.level,
            alarmKind: content.alarmKind,
            details: content.alertDetails || '',
            deviceName: content.deviceName || '',
            deviceAddr: '',
            productName: '',
            tenantName: content.tenantName || '',
            alarmType: content.alarmType,
            subDeviceName: content.subDeviceName || ''
          }
          // 二三级进通知匣：页面生命周期内每条只推一次
          if (isLightAlarmLevel(item.level)) {
            if (!alertShownIds.value.has(alertId)) {
              alertShownIds.value.add(alertId)
              alarmNoticeRef.value?.push(item)
              flashPendingToast()
            }
          } else {
            // 一级：入队（弹窗组件内部判定重复提醒周期），空闲时立即弹出
            popupRef.value?.enqueue(item)
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
const navRoutes: Record<string, string> = {
  'duty-screen': '/duty-screen',
  'ai-analysis': '/eiot/firewatch/ai-analysis',
  'device-screen': '/device-screen',
  'remote-console': '/remote-console',
  // 'ops-workbench': '/ops-workbench',
  // 'data-center': '/data-center',
  'video-surveillance': '/video-surveillance',
  patrol: '/eiot/firewatch/patrol'
}
// 管理后台路径按角色区分：管理员/监管(super_admin/monitor_role) → property，企业用户 → enterprise
const getAdminPath = () => {
  const isAdmin = userStore.getRoles?.some((r) => r === 'super_admin' || r === 'monitor_role')
  return isAdmin ? '/eiot/firewatch/property' : '/eiot/firewatch/enterprise'
}
const handleNavCommand = (command: string) => {
  if (command === 'admin') {
    goToAdmin()
    return
  }
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
  if (command === 'park') {
    showScene('park')
    return
  }
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
watch(
  rawScene,
  (value) => {
    const normalized = value === 'park' ? 'park' : 'city'
    if (value !== normalized) void router.replace({ query: { ...route.query, scene: normalized } })
  },
  { immediate: true }
)

const markAccessibleProperties = (snapshotId: string) => {
  const geometry = propertyMapRenderReport.value
  const labels = propertyMapVisualReport.value
  if (
    !geometry ||
    !labels ||
    geometry.snapshotId !== snapshotId ||
    labels.snapshotId !== snapshotId
  )
    return
  const geometricIds = new Set(geometry.renderedPropertyIds)
  const sentIds = new Set(mapPoints.value.map((point) => point.id))
  propertyMap.markApplied(
    snapshotId,
    [
      ...labels.visiblePropertyIds,
      ...labels.clusteredPropertyIds,
      ...labels.hiddenPropertyIds
    ].filter((id) => geometricIds.has(id) && sentIds.has(id))
  )
}
const handlePropertyPointsApplied = (
  snapshotId: string,
  renderedPropertyIds: string[],
  excluded: { outOfBoundary: number; noSurface: number; invalid: number },
  excludedIds?: { outOfBoundary: string[]; noSurface: string[]; invalid: string[] }
) => {
  propertyMapRenderReport.value = { snapshotId, renderedPropertyIds, excluded, excludedIds }
  markAccessibleProperties(snapshotId)
}
const handlePropertyLabelsApplied = (
  snapshotId: string,
  visiblePropertyIds: string[],
  clusteredPropertyIds: string[],
  hiddenPropertyIds: string[],
  offscreenPropertyIds: string[]
) => {
  propertyMapVisualReport.value = {
    snapshotId,
    visiblePropertyIds,
    clusteredPropertyIds,
    hiddenPropertyIds,
    offscreenPropertyIds
  }
  markAccessibleProperties(snapshotId)
}

const handleScenePointClick = async (
  pointId: string,
  kind: MapPoint['kind'],
  snapshotId: string
) => {
  const point = findPoint(mapPoints.value, pointId)
  if (kind !== 'property' || snapshotId !== propertyMap.snapshotId.value || !point) return
  if (!point.navigable) return
  if (parkNavigationPending.value) return
  parkNavigationPending.value = true
  setParkNavigationStatus('正在复核园区入口并同步地图…')
  try {
    const verification = await propertyMap.verifyParkEntry(pointId)
    if (
      !verification.ok ||
      !verification.snapshotId ||
      !(await propertyMap.waitForApplied(verification.snapshotId, pointId))
    ) {
      console.warn('[property-map] 园区入口身份、状态或地图回执无法确认，未进入园区')
      setParkNavigationStatus('园区入口复核失败，未切换场景', 4_000)
      ElMessage.warning('园区入口仍在复核，未切换场景；请刷新物业位置后重试')
      return
    }
    setParkNavigationStatus('园区入口已确认，正在进入园区…')
    if (currentScene.value !== 'park')
      await router.replace({ query: { ...route.query, scene: 'park' } })
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

const handleSceneError = (error: {
  scene: SceneName
  reason: string
  retryable: boolean
  message?: string
}) => {
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

/** 实时告警只展示今天的报警：库里存有历史数据，别把昨天的混进大屏滚动列表 */
const filterTodayAlarms = (list: any[]) => {
  const pad = (n: number) => String(n).padStart(2, '0')
  const now = new Date()
  const today = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  return (list || []).filter((a: any) => {
    if (a?.alarmTime == null) return false
    // 兼容毫秒时间戳与 ISO 字符串两种格式
    const d =
      typeof a.alarmTime === 'number'
        ? new Date(a.alarmTime)
        : new Date(String(a.alarmTime).replace(' ', 'T'))
    if (isNaN(d.getTime())) return false
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` === today
  })
}

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
  // 仅 super_admin 选择了具体租户时才携带 tenantId（0 = 全部租户，不带参）
  const params =
    isSuperAdmin.value && selectedTenantId.value ? { tenantId: selectedTenantId.value } : undefined
  const statsRes = await silentGet('/eiot/firewatch/dashboard/statistics', params)
  const d = statsRes || {}
  if (statsRes) {
    // 后端随响应返回登录租户信息，供标题右侧文本展示
    if (d.tenantName) loginTenant.value = { tenantId: d.tenantId, tenantName: d.tenantName }
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
    if (d.realtimeAlarms) alarmList.value = filterTodayAlarms(d.realtimeAlarms)
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
    if (Array.isArray(r)) alarmList.value = filterTodayAlarms(r)
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

// 超管切换租户：清空旧数据 → 作废地图快照 → 重新加载
const handleTenantChange = () => {
  // 兜底接口"列表为空才请求"、statistics"有值才覆盖"，必须先清空避免旧租户数据残留
  propertyList.value = []
  enterpriseInfo.value = {}
  dutyList.value = []
  alarmList.value = []
  // 地图是独立的 30 秒轮询链路，需手动作废当前快照
  propertyMap.clearForSessionChange()
  // 重新拉数据，完成后再刷新地图点位
  void loadData().then(() => propertyMap.refresh('tenant-change'))
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
  if (
    frameRect.width <= 0 ||
    frameRect.height <= 0 ||
    frame.clientWidth <= 0 ||
    frame.clientHeight <= 0
  )
    return
  const scaleX = frame.clientWidth / frameRect.width
  const scaleY = frame.clientHeight / frameRect.height
  const header = document
    .querySelector<HTMLElement>('.duty-screen .header')
    ?.getBoundingClientRect()
  const left = document
    .querySelector<HTMLElement>('.duty-screen .left-panel')
    ?.getBoundingClientRect()
  const right = document
    .querySelector<HTMLElement>('.duty-screen .right-panel')
    ?.getBoundingClientRect()
  const margin = 12
  const localLeft = Math.max(
    margin,
    left && left.right > frameRect.left ? (left.right - frameRect.left + margin) * scaleX : margin
  )
  const localRight = Math.min(
    frame.clientWidth - margin,
    right && right.left < frameRect.right
      ? (right.left - frameRect.left - margin) * scaleX
      : frame.clientWidth - margin
  )
  const localTop = Math.max(
    margin,
    header && header.bottom > frameRect.top
      ? (header.bottom - frameRect.top + margin) * scaleY
      : margin
  )
  const localBottom = frame.clientHeight - margin
  mapSafeViewport.value =
    localRight > localLeft && localBottom > localTop
      ? {
          width: frame.clientWidth,
          height: frame.clientHeight,
          safeRect: { left: localLeft, top: localTop, right: localRight, bottom: localBottom }
        }
      : {
          width: frame.clientWidth,
          height: frame.clientHeight,
          safeRect: {
            left: margin,
            top: margin,
            right: frame.clientWidth - margin,
            bottom: frame.clientHeight - margin
          }
        }
}

const handleResize = () => {
  trendChart?.resize()
  updateMapSafeViewport()
}

let timer: any = null,
  refreshTimer: any = null
onMounted(() => {
  updateDateTime()
  void loadTenantOptions()
  timer = setInterval(updateDateTime, 1000)
  loadData().then(() =>
    setTimeout(() => {
      initCharts()
      refreshCharts()
    }, 100)
  )
  refreshTimer = setInterval(loadData, 30000)

  // 每10秒拉取当天未处置告警（回捞窗口：当天 0 点起，重启/刷新后仍能恢复待处置提示）
  fetchRecentAlerts()
  alertPollingTimer = setInterval(fetchRecentAlerts, 10000)

  // 实时告警列表自动滚动（悬停暂停、滚轮可手动上下翻）
  startAlarmAutoScroll()

  window.addEventListener('resize', handleResize)
  void nextTick(updateMapSafeViewport)
  window.setTimeout(updateMapSafeViewport, 300)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (refreshTimer) clearInterval(refreshTimer)
  if (alertPollingTimer) clearInterval(alertPollingTimer)
  if (alarmAutoScrollTimer) clearInterval(alarmAutoScrollTimer)
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
  background: rgba(21, 42, 62, 0.94);
  border: 1px solid rgba(255, 210, 91, 0.84);
  border-radius: 4px;
  box-shadow: 0 0 16px rgba(255, 190, 56, 0.24);
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
    display: flex;
    align-items: center;
    gap: 12px;
    align-self: stretch;

    .title-block {
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    .tenant-name {
      font-size: 14px;
      color: $t2;
    }

    .tenant-select {
      width: 160px;

      // 深色玻璃质感：圆角 + 青色描边，悬停/聚焦提亮
      :deep(.el-select__wrapper) {
        min-height: 26px;
        background: rgba(15, 32, 55, 0.72);
        border-radius: 6px;
        box-shadow: 0 0 0 1px rgba(79, 195, 247, 0.25) inset;
        transition:
          box-shadow 0.2s ease,
          background 0.2s ease;
        &:hover {
          background: rgba(15, 32, 55, 0.9);
          box-shadow: 0 0 0 1px rgba(79, 195, 247, 0.55) inset;
        }
        &.is-focused {
          box-shadow:
            0 0 0 1px rgba(79, 195, 247, 0.8) inset,
            0 0 10px rgba(79, 195, 247, 0.25);
        }
      }
      :deep(.el-select__placeholder) {
        color: $t2;
        font-size: 13px;
      }
      :deep(.el-select__caret) {
        color: $t2;
      }
    }

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
      margin: 1px 0 0;
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
.device-stats-card,
.duty-overview-card {
  color: inherit;
  text-decoration: none;
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

// 二级告警 Toast 提示（锚在「待处理」格子正上方）
.pending-toast {
  position: absolute;
  bottom: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba($orange, 0.92);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  padding: 5px 12px;
  border-radius: 4px;
  white-space: nowrap;
  z-index: 20;
  pointer-events: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
  &::after {
    content: '';
    position: absolute;
    top: 100%;
    left: 50%;
    transform: translateX(-50%);
    border: 5px solid transparent;
    border-top-color: rgba($orange, 0.92);
  }
}
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-4px);
}

// 二级告警「待处理」格子高亮闪烁
@keyframes pending-cell-flash {
  0%, 100% { box-shadow: none; }
  25% { box-shadow: 0 0 0 3px rgba($orange, 0.45); }
  50% { box-shadow: 0 0 0 6px rgba($orange, 0.2); }
  75% { box-shadow: 0 0 0 3px rgba($orange, 0.35); }
}

// ====== Alarm ======
// 列表容器不要再覆盖 overflow：走 .card-content 默认的 overflow-y:auto（已隐藏滚动条），滚轮才能手动上下翻
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
    .alarm-tenant {
      color: $cyan;
      font-weight: 600;
      margin-right: 6px;
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
        position: relative;
        &.flash {
          animation: pending-cell-flash 1.2s ease;
        }
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

// 租户下拉面板（大屏标题旁，暗色毛玻璃，与头部下拉统一）
.tenant-select-popper {
  background: rgba(15, 32, 55, 0.92) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(40, 70, 110, 0.45) !important;
  border-radius: 6px !important;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.5) !important;

  .el-select-dropdown__item {
    color: #b0c0dd;
    &.is-hovering,
    &:hover {
      background: rgba(24, 144, 255, 0.15);
      color: #fff;
    }
    &.is-selected {
      color: #4fc3f7;
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
