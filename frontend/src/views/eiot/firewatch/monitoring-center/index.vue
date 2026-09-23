<template>
  <div class="mc-screen">
    <!-- ====== 顶部导航 ====== -->
    <header class="mc-header">
      <div class="hd-left">
        <div class="brand">
          <div class="brand-icon"><i class="fire-icon"></i></div>
          <div class="brand-text">
            <h1>{{ enterpriseInfo.name || '安为智慧消防监控中心' }}</h1>
            <p>SMART FIRE MONITORING CENTER</p>
          </div>
        </div>
      </div>
      <div class="hd-center">
        <div class="alert-bar" v-if="pendingAlarms > 0">
          <span class="alert-dot"></span>
          <span>实时告警中 · {{ pendingAlarms }}条待处理</span>
        </div>
        <div class="datetime-bar" v-else>
          <span class="dt-date">{{ currentDate }}</span>
          <span class="dt-time">{{ currentTime }}</span>
          <span class="dt-week">{{ currentWeek }}</span>
        </div>
      </div>
      <div class="hd-right">
        <div class="datetime-compact" v-if="pendingAlarms > 0">
          <span>{{ currentDate }}</span>
          <span class="time-hl">{{ currentTime }}</span>
        </div>
        <el-button type="primary" class="btn-admin" @click="goToAdmin">
          <el-icon><Setting /></el-icon>管理后台
        </el-button>
        <el-button class="btn-fs" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
        </el-button>
      </div>
    </header>

    <!-- ====== 主体内容 ====== -->
    <main class="mc-body">
      <!-- ====== 左栏 ====== -->
      <aside class="col-left">
        <!-- 今日警情 -->
        <section class="card">
          <div class="card-hd"><span class="card-title"><el-icon><Warning /></el-icon>今日警情</span></div>
          <div class="card-bd">
            <div class="alarm-hero-grid">
              <div class="ah-item total">
                <div class="ah-val">{{ todayStats.totalAlarms }}</div>
                <div class="ah-lbl">告警总数</div>
              </div>
              <div class="ah-item fire">
                <div class="ah-val">{{ todayStats.fireAlarms }}</div>
                <div class="ah-lbl">火警</div>
              </div>
              <div class="ah-item handled">
                <div class="ah-val">{{ todayStats.handledCount }}</div>
                <div class="ah-lbl">已处置</div>
              </div>
              <div class="ah-item rate">
                <div class="ah-val">{{ todayStats.handleRate }}<small>%</small></div>
                <div class="ah-lbl">处置率</div>
              </div>
            </div>
            <div class="response-row">
              <div class="rr-item">
                <span class="rr-lbl">平均响应</span>
                <span class="rr-val">{{ todayStats.avgResponse }}<small>min</small></span>
              </div>
              <div class="rr-item">
                <span class="rr-lbl">待处理</span>
                <span class="rr-val warn">{{ todayStats.pendingCount }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 设备状态 -->
        <section class="card">
          <div class="card-hd"><span class="card-title"><el-icon><Monitor /></el-icon>设备状态</span></div>
          <div class="card-bd">
            <div class="dev-center">
              <span class="dev-num">{{ deviceStats.total }}</span>
              <span class="dev-unit">台设备</span>
            </div>
            <div class="dev-bars">
              <div class="db-row normal"><span class="db-lbl">在线</span><div class="db-track"><div class="db-fill" :style="{ width: pct(deviceStats.online, deviceStats.total) + '%' }"></div></div><span class="db-val">{{ deviceStats.online }}</span></div>
              <div class="db-row alarm"><span class="db-lbl">告警</span><div class="db-track"><div class="db-fill" :style="{ width: pct(deviceStats.alarm, deviceStats.total) + '%' }"></div></div><span class="db-val">{{ deviceStats.alarm }}</span></div>
              <div class="db-row fault"><span class="db-lbl">故障</span><div class="db-track"><div class="db-fill" :style="{ width: pct(deviceStats.fault, deviceStats.total) + '%' }"></div></div><span class="db-val">{{ deviceStats.fault }}</span></div>
              <div class="db-row offline"><span class="db-lbl">离线</span><div class="db-track"><div class="db-fill" :style="{ width: pct(deviceStats.offline, deviceStats.total) + '%' }"></div></div><span class="db-val">{{ deviceStats.offline }}</span></div>
              <div class="db-row inactive"><span class="db-lbl">未激活</span><div class="db-track"><div class="db-fill" :style="{ width: pct(deviceStats.inactive, deviceStats.total) + '%' }"></div></div><span class="db-val">{{ deviceStats.inactive }}</span></div>
            </div>
          </div>
        </section>

        <!-- 设施分类 -->
        <section class="card">
          <div class="card-hd"><span class="card-title"><el-icon><Grid /></el-icon>设施分类</span></div>
          <div class="card-bd">
            <div class="fac-list">
              <div class="fac-row" v-for="f in facilityStats" :key="f.name">
                <div class="fac-icon" :style="{ background: f.color }"><el-icon><component :is="f.icon" /></el-icon></div>
                <div class="fac-name">{{ f.name }}</div>
                <div class="fac-count">{{ f.count }}台</div>
                <div class="fac-pct">{{ pct(f.count, deviceStats.total) }}%</div>
              </div>
            </div>
          </div>
        </section>

        <!-- 快捷操作 -->
        <section class="card">
          <div class="card-hd"><span class="card-title"><el-icon><Promotion /></el-icon>快捷操作</span></div>
          <div class="card-bd">
            <div class="quick-grid">
              <div class="quick-btn" v-for="act in quickActions" :key="act.label" @click="act.handler">
                <div class="qb-icon" :style="{ background: act.color }"><el-icon><component :is="act.icon" /></el-icon></div>
                <span class="qb-text">{{ act.label }}</span>
              </div>
            </div>
          </div>
        </section>
      </aside>

      <!-- ====== 中间区域 ====== -->
      <section class="col-center">
        <!-- 地图 -->
        <div class="map-wrap">
          <div class="map-top-bar">
            <span class="map-title">部件分布</span>
            <div class="map-summary">
              <div class="ms-it"><span class="ms-v">{{ deviceStats.total }}</span><span class="ms-l">设备总数</span></div>
              <div class="ms-it"><span class="ms-v green">{{ deviceStats.online }}</span><span class="ms-l">在线</span></div>
              <div class="ms-it"><span class="ms-v red">{{ pendingAlarms }}</span><span class="ms-l">告警</span></div>
            </div>
          </div>
          <div id="mc-map" class="map-canvas"></div>
          <div class="map-legend">
            <span class="lg-it"><i style="background:#52c41a"></i>正常</span>
            <span class="lg-it"><i style="background:#ff4d4f"></i>告警</span>
            <span class="lg-it"><i style="background:#fa8c16"></i>故障</span>
            <span class="lg-it"><i style="background:#8c8c8c"></i>离线</span>
          </div>
        </div>

        <!-- 视频监控 -->
        <div class="video-wrap">
          <div class="video-top-bar">
            <span class="card-title"><el-icon><VideoCamera /></el-icon>视频监控</span>
            <span class="cam-count">{{ cameraList.length }}路</span>
          </div>
          <div class="video-grid">
            <div class="video-cell" v-for="(cam, idx) in cameraList" :key="idx">
              <div class="vc-head"><el-icon><VideoCamera /></el-icon><span>{{ cam.name }}</span></div>
              <div class="vc-body"><div class="vc-placeholder"><el-icon class="cam-big"><VideoCamera /></el-icon><span class="live-tag">LIVE</span></div></div>
            </div>
          </div>
        </div>

        <!-- 底部图表 -->
        <div class="chart-row">
          <div class="chart-card">
            <div class="card-hd"><span class="card-title"><el-icon><TrendCharts /></el-icon>告警趋势(近7天)</span></div>
            <div class="card-bd"><div ref="trendChartRef" class="chart-box"></div></div>
          </div>
          <div class="chart-card">
            <div class="card-hd"><span class="card-title"><el-icon><PieChart /></el-icon>告警类型分布</span></div>
            <div class="card-bd"><div ref="typeChartRef" class="chart-box"></div></div>
          </div>
          <div class="chart-card">
            <div class="card-hd"><span class="card-title"><el-icon><Histogram /></el-icon>设备状态</span></div>
            <div class="card-bd"><div ref="deviceChartRef" class="chart-box"></div></div>
          </div>
        </div>
      </section>

      <!-- ====== 右栏 ====== -->
      <aside class="col-right">
        <!-- 实时告警 -->
        <section class="card alarm-card">
          <div class="card-hd">
            <span class="card-title"><el-icon><Bell /></el-icon>实时告警<span class="badge-red" v-if="alarmList.length">{{ alarmList.length }}</span></span>
          </div>
          <div class="card-bd alarm-scroll">
            <div class="alarm-inner" :class="{ scrolling: alarmList.length > 4 }">
              <div class="alarm-item" v-for="a in alarmList" :key="a.id" :class="'lv-' + a.level" @click="viewAlarm(a)">
                <div class="ai-dot"></div>
                <div class="ai-body">
                  <div class="ai-desc">{{ extractAlarmSummary(a.content) }}</div>
                  <div class="ai-time">{{ a.time }}</div>
                </div>
                <span class="ai-tag" :class="'tag-' + a.level">{{ levelText(a.level) }}</span>
              </div>
            </div>
            <div class="empty-state" v-if="!alarmList.length"><el-icon><CircleCheck /></el-icon>暂无告警</div>
          </div>
        </section>

        <!-- 风险等级监控 -->
        <section class="card">
          <div class="card-hd"><span class="card-title"><el-icon><DataAnalysis /></el-icon>风险等级监控</span></div>
          <div class="card-bd">
            <div class="risk-meter">
              <div class="rm-level" :class="'rm-' + riskLevel">
                <span class="rm-icon">{{ riskIcon }}</span>
                <span class="rm-text">{{ riskText }}</span>
              </div>
              <div class="rm-detail">
                <div class="rm-row"><span class="rm-lbl">安全天数</span><span class="rm-val">{{ riskInfo.safeDays }}天</span></div>
                <div class="rm-row"><span class="rm-lbl">设备健康率</span><span class="rm-val">{{ riskInfo.healthRate }}%</span></div>
                <div class="rm-row"><span class="rm-lbl">隐患待整改</span><span class="rm-val">{{ riskInfo.pendingIssues }}项</span></div>
              </div>
            </div>
          </div>
        </section>

        <!-- 巡检任务 -->
        <section class="card">
          <div class="card-hd"><span class="card-title"><el-icon><Checked /></el-icon>巡检任务</span></div>
          <div class="card-bd">
            <div class="patrol-stats">
              <div class="ps-it"><span class="ps-v blue">{{ patrolStats.total }}</span><span class="ps-l">总任务</span></div>
              <div class="ps-it"><span class="ps-v green">{{ patrolStats.completed }}</span><span class="ps-l">已完成</span></div>
              <div class="ps-it"><span class="ps-v orange">{{ patrolStats.pending }}</span><span class="ps-l">进行中</span></div>
            </div>
            <div class="patrol-list">
              <div class="pl-item" v-for="p in patrolList" :key="p.id">
                <div class="pl-dot" :class="p.status"></div>
                <div class="pl-info"><div class="pl-name">{{ p.taskName }}</div><div class="pl-meta">{{ p.inspector }} | {{ p.planTime }}</div></div>
                <span class="pl-tag" :class="p.status">{{ patrolStatusText(p.status) }}</span>
              </div>
              <div class="empty-state small" v-if="!patrolList.length">暂无巡检任务</div>
            </div>
          </div>
        </section>

        <!-- 值班信息 -->
        <section class="card">
          <div class="card-hd"><span class="card-title"><el-icon><User /></el-icon>今日值班</span></div>
          <div class="card-bd">
            <div class="duty-list">
              <div class="dl-item" v-for="d in dutyList" :key="d.id">
                <div class="dl-avatar"><el-icon><UserFilled /></el-icon></div>
                <div class="dl-info"><div class="dl-name">{{ d.userName }}</div><div class="dl-shift">{{ d.shiftName }} {{ d.startTime }}-{{ d.endTime }}</div></div>
                <span class="dl-tag" :class="d.status">{{ dutyStatusText(d.status) }}</span>
              </div>
              <div class="empty-state small" v-if="!dutyList.length">暂无值班安排</div>
            </div>
          </div>
        </section>
      </aside>
    </main>

    <!-- 告警详情弹窗 -->
    <el-dialog v-model="alarmDialogVisible" title="告警详情" width="440px" class="dark-dialog" :append-to-body="true">
      <div class="detail-body" v-if="selectedAlarm">
        <div class="dr"><span class="dl">设备</span><span class="dv">{{ selectedAlarm.deviceName }}</span></div>
        <div class="dr"><span class="dl">类型</span><span class="dv">{{ selectedAlarm.typeName || '-' }}</span></div>
        <div class="dr"><span class="dl">级别</span><span class="dv" :class="'lv-' + selectedAlarm.level">{{ levelText(selectedAlarm.level) }}</span></div>
        <div class="dr"><span class="dl">时间</span><span class="dv">{{ selectedAlarm.time }}</span></div>
        <div class="dr"><span class="dl">位置</span><span class="dv">{{ selectedAlarm.location || '-' }}</span></div>
        <div class="dr"><span class="dl">描述</span><span class="dv">{{ selectedAlarm.content || '-' }}</span></div>
      </div>
    </el-dialog>

  </div>

  <!-- ====== 报警弹窗（与值守大屏一致） ====== -->
  <Teleport to="body">
    <div class="alarm-popup-overlay" :class="'flash-' + getPopupSeverity()" v-if="popupVisible" @click.self="closePopup">
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
          <button class="alarm-btn alarm-btn-later" @click="onMarkFalseAlarm">标记误报</button>
          <button class="alarm-btn alarm-btn-now" @click="onConfirmFire">确认火警</button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- 确认火警表单弹窗（与火警处置中心一致） -->
  <el-dialog
    v-model="confirmDialogVisible"
    title="确认报警"
    width="520px"
    class="dark-dialog alarm-form-dialog"
    :append-to-body="true"
    :z-index="10001"
    destroy-on-close
  >
    <el-form :model="confirmForm" label-width="90px" ref="confirmFormRef" :rules="confirmRules">
      <el-form-item label="报警类型" prop="type">
        <el-select v-model="confirmForm.type" placeholder="请选择报警类型" style="width: 100%" :teleported="false">
          <el-option label="火警" value="fire" />
          <el-option label="故障" value="fault" />
          <el-option label="异常" value="abnormal" />
          <el-option label="误报" value="false_alarm" />
        </el-select>
      </el-form-item>
      <el-form-item label="报警级别" prop="level">
        <el-select v-model="confirmForm.level" placeholder="请选择报警级别" style="width: 100%" :teleported="false">
          <el-option label="A级(高危)" value="A" />
          <el-option label="B级(中危)" value="B" />
          <el-option label="C级(低危)" value="C" />
        </el-select>
      </el-form-item>
      <el-form-item label="报警位置">
        <el-input v-model="confirmForm.location" disabled />
      </el-form-item>
      <el-form-item label="备注说明">
        <el-input v-model="confirmForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="confirmDialogVisible = false">取消</el-button>
      <el-button type="danger" @click="submitConfirmFire" :loading="confirmLoading">确认报警</el-button>
    </template>
  </el-dialog>

  <!-- 标记误报表单弹窗（与火警处置中心一致） -->
  <el-dialog
    v-model="falseDialogVisible"
    title="确认误报"
    width="500px"
    class="dark-dialog alarm-form-dialog"
    :append-to-body="true"
    :z-index="10001"
    destroy-on-close
  >
    <el-alert
      title="确认此告警为误报后，将不再需要进一步处置。"
      type="warning"
      :closable="false"
      style="margin-bottom: 16px"
    />
    <el-form :model="falseForm" label-width="90px" ref="falseFormRef" :rules="falseRules">
      <el-form-item label="误报原因" prop="reason">
        <el-select v-model="falseForm.reason" placeholder="请选择误报原因" style="width: 100%" :teleported="false">
          <el-option label="设备误触发" value="device_trigger" />
          <el-option label="烟雾/灰尘干扰" value="smoke_dust" />
          <el-option label="施工/装修影响" value="construction" />
          <el-option label="测试/演练" value="test" />
          <el-option label="设备故障" value="device_fault" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      <el-form-item label="详细说明">
        <el-input v-model="falseForm.remark" type="textarea" :rows="3" placeholder="请输入详细说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="falseDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitFalseAlarm" :loading="falseLoading">确认误报</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import request from '@/config/axios'
import AMapLoader from '@amap/amap-jsapi-loader'
import { stopAlarmSpeech, speakAlarmSpeech } from '@/utils/alarmSpeech'
import { stopAlarmSoundLoop, isAlarmSoundEnabled, toggleAlarmSound, closeAlarmAudio } from '@/utils/alarmSound'
import { getRecentUnprocessedAlerts, confirmAlertRecord, confirmAlarm, markFalseAlarm } from '@/api/eiot/firewatch/firewatch.api'
import { getAccessToken, getRefreshToken } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()
const MAP_CENTER = [113.7518, 23.0208]

// Refs
const trendChartRef = ref<HTMLElement>()
const typeChartRef = ref<HTMLElement>()
const deviceChartRef = ref<HTMLElement>()

// Charts
let trendChart: ECharts | null = null
let typeChart: ECharts | null = null
let deviceChart: ECharts | null = null

// Map
let mapInstance: any = null
let mapMarkers: any[] = []

// State
const currentDate = ref('')
const currentTime = ref('')
const currentWeek = ref('')
const enterpriseInfo = ref<any>({})
const deviceStats = ref({ total: 0, online: 0, alarm: 0, fault: 0, offline: 0, inactive: 0 })
const todayStats = ref({ totalAlarms: 0, fireAlarms: 0, handledCount: 0, pendingCount: 0, handleRate: 0, avgResponse: 0 })
const alarmList = ref<any[]>([])
const dutyList = ref<any[]>([])
const patrolList = ref<any[]>([])
const patrolStats = ref({ total: 0, completed: 0, pending: 0 })
const riskInfo = ref({ safeDays: 0, healthRate: 0, pendingIssues: 0 })
const facilityStats = ref([
  { name: '烟感探测器', count: 0, color: 'linear-gradient(135deg, #ff7a45, #ff4d4f)', icon: 'Smoking' },
  { name: '温感探测器', count: 0, color: 'linear-gradient(135deg, #ffa940, #fa8c16)', icon: 'Sunrise' },
  { name: '灭火器', count: 0, color: 'linear-gradient(135deg, #36cfc9, #13c2c2)', icon: 'FirstAidKit' },
  { name: '消防栓', count: 0, color: 'linear-gradient(135deg, #597ef7, #2f54eb)', icon: 'Aim' },
  { name: '手动报警器', count: 0, color: 'linear-gradient(135deg, #ff85c0, #eb2f96)', icon: 'Bell' }
])
const cameraList = ref<any[]>([])
const alarmDialogVisible = ref(false)
const selectedAlarm = ref<any>(null)

// ==================== 报警弹窗逻辑（与值守大屏一致） ====================
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

/** WebSocket alarm-push 推送的告警转成弹窗数据结构 */
const pushPopupAlarm = (alarm: any) => {
  const id = alarm.id ?? alarm.alertId
  if (id != null && alertShownIds.value.has(id)) return
  const item = {
    id,
    name: alarm.name || alarm.alertName || alarm.alarmTypeName || alarm.typeName || '设备报警',
    deviceId: alarm.deviceId,
    alertTime: alarm.alertTime || alarm.alarmTime || alarm.time,
    alertImage: alarm.alertImage || null,
    level: alarm.level ?? alarm.alarmLevel ?? '2',
    details: alarm.details || alarm.alarmDescription || alarm.content || '',
    deviceName: alarm.deviceName || '',
    deviceAddr: alarm.deviceAddr || alarm.alarmLocation || alarm.location || '',
    productName: alarm.productName || '',
    tenantName: alarm.tenantName || '',
    alarmType: alarm.alarmType
  }
  if (id != null) alertShownIds.value.add(id)
  alertQueue.value.push(item)
  if (!popupVisible.value) {
    showNextAlert()
  }
}

// ==================== 确认火警/标记误报表单（与火警处置中心一致） ====================
const confirmDialogVisible = ref(false)
const confirmLoading = ref(false)
const confirmFormRef = ref<FormInstance>()
const confirmForm = reactive({
  type: 'fire',
  level: 'B',
  location: '',
  remark: ''
})
const confirmRules: FormRules = {
  type: [{ required: true, message: '请选择报警类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择报警级别', trigger: 'change' }]
}

const falseDialogVisible = ref(false)
const falseLoading = ref(false)
const falseFormRef = ref<FormInstance>()
const falseForm = reactive({
  reason: '',
  remark: ''
})
const falseRules: FormRules = {
  reason: [{ required: true, message: '请选择误报原因', trigger: 'change' }]
}

/** 确认火警：打开确认报警表单弹窗（与火警处置中心一致） */
const onConfirmFire = () => {
  const alarm = popupAlarm.value
  if (!alarm?.id) {
    closePopup()
    return
  }
  confirmForm.type = 'fire'
  confirmForm.level = 'B'
  confirmForm.location = alarm.deviceAddr || alarm.deviceName || ''
  confirmForm.remark = ''
  confirmDialogVisible.value = true
}

/** 提交确认报警 */
const submitConfirmFire = async () => {
  if (!confirmFormRef.value) return
  const valid = await confirmFormRef.value.validate().catch(() => false)
  if (!valid) return
  const alarm = popupAlarm.value
  if (!alarm?.id) {
    confirmDialogVisible.value = false
    closePopup()
    return
  }
  confirmLoading.value = true
  try {
    await confirmAlarm(alarm.id, undefined, confirmForm.remark)
    // 同步更新告警记录状态：已读 + 已处理
    try {
      await confirmAlertRecord(alarm.id)
    } catch (e) {
      console.warn('更新告警处理状态失败:', e)
    }
    ElMessage.success('已确认火警')
    confirmDialogVisible.value = false
    closePopup()
  } catch (e) {
    console.warn('确认火警失败:', e)
    ElMessage.error('确认火警失败')
  } finally {
    confirmLoading.value = false
  }
}

/** 标记误报：打开误报原因表单弹窗（与火警处置中心一致） */
const onMarkFalseAlarm = () => {
  const alarm = popupAlarm.value
  if (!alarm?.id) {
    closePopup()
    return
  }
  falseForm.reason = ''
  falseForm.remark = ''
  falseDialogVisible.value = true
}

/** 提交标记误报 */
const submitFalseAlarm = async () => {
  if (!falseFormRef.value) return
  const valid = await falseFormRef.value.validate().catch(() => false)
  if (!valid) return
  const alarm = popupAlarm.value
  if (!alarm?.id) {
    falseDialogVisible.value = false
    closePopup()
    return
  }
  falseLoading.value = true
  try {
    await markFalseAlarm(alarm.id, undefined, falseForm.reason)
    // 同步更新告警记录状态：已读 + 已处理
    try {
      await confirmAlertRecord(alarm.id)
    } catch (e) {
      console.warn('更新告警处理状态失败:', e)
    }
    ElMessage.success('已标记为误报')
    falseDialogVisible.value = false
    closePopup()
  } catch (e) {
    console.warn('标记误报失败:', e)
    ElMessage.error('标记误报失败')
  } finally {
    falseLoading.value = false
  }
}

const fetchRecentAlerts = async () => {
  try {
    const res = await getRecentUnprocessedAlerts(5)
    const alerts = (Array.isArray(res) ? res : res?.data) || []
    console.log('[MonitorCenter] fetchRecentAlerts =>', alerts.length, '条告警')
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

// Sound
const soundEnabled = ref(isAlarmSoundEnabled())
const onToggleSound = () => {
  soundEnabled.value = toggleAlarmSound()
}

// ========== WebSocket 实时告警推送 ==========
let ws: WebSocket | null = null
let wsReconnectTimer: ReturnType<typeof setTimeout> | null = null

function connectWebSocket() {
  try {
    const token = getAccessToken() || ''
    if (!token) return
    const baseUrl = import.meta.env.VITE_BASE_URL || window.location.origin
    const wsUrl = baseUrl.replace('http', 'ws') + '/infra/ws?token=' + encodeURIComponent(token)
    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      console.log('[WebSocket] 告警推送连接已建立')
      if (wsReconnectTimer) { clearTimeout(wsReconnectTimer); wsReconnectTimer = null }
    }
    ws.onmessage = (event) => {
      try {
        if (event.data === 'pong') return
        const msg = JSON.parse(event.data)
        const content = typeof msg.content === 'string' ? JSON.parse(msg.content) : msg.content
        // 值守大屏推送格式：firewatch-alert-new
        if (msg.type === 'firewatch-alert-new' && content) {
          const alertId = content.alertId
          if (alertId && !alertShownIds.value.has(alertId)) {
            alertQueue.value.push({
              id: alertId,
              name: content.alertName || '设备报警',
              deviceId: content.deviceId,
              alertTime: content.alertTime,
              alertImage: content.alertImage || null,
              level: content.level || '2',
              details: '',
              deviceName: content.deviceName || '',
              deviceAddr: '',
              productName: '',
              tenantName: content.tenantName || '',
              alarmType: content.alarmType
            })
            alertShownIds.value.add(alertId)
            if (!popupVisible.value) {
              showNextAlert()
            }
          }
        }
        // 监控中心推送格式：alarm-push
        if (msg.type === 'alarm-push' && content) {
          pushPopupAlarm(content)
        }
      } catch {}
    }
    ws.onclose = () => { wsReconnectTimer = setTimeout(connectWebSocket, 5000) }
    ws.onerror = () => { ws?.close() }
  } catch { wsReconnectTimer = setTimeout(connectWebSocket, 5000) }
}

// 缓存图表数据（loadData 先于 initCharts 执行，需要缓存数据供图表初始化后刷新）
let cachedAlarmTrend: any[] = []
let cachedDeviceCategory: any[] = []
let cachedAlarmTypeDist: any[] = []
// 缓存地图点位（地图初始化可能晚于数据加载，需要等地图就绪后再添加）
let pendingMapPoints: any[] = []

// Sound
// （蜂鸣已移除，语音播报由 alarmSpeech.ts 处理）

// Computed
const pendingAlarms = computed(() => alarmList.value.filter(a => a.status === 'pending').length || alarmList.value.length)
const riskLevel = computed(() => {
  if (deviceStats.value.alarm > 3) return 'high'
  if (deviceStats.value.alarm > 0 || deviceStats.value.fault > 2) return 'medium'
  return 'low'
})
const riskText = computed(() => ({ high: '高风险', medium: '一般风险', low: '安全' }[riskLevel.value] || '安全'))
const riskIcon = computed(() => ({ high: '!', medium: '!', low: '✓' }[riskLevel.value] || '✓'))

// 管理后台路径按角色区分：管理员/监管(super_admin/monitor_role) → property，企业用户 → enterprise
const getAdminPath = () => {
  const isAdmin = userStore.getRoles?.some(r => r === 'super_admin' || r === 'monitor_role')
  return isAdmin ? '/eiot/firewatch/property' : '/eiot/firewatch/enterprise'
}
// Quick actions
const quickActions = ref([
  { label: '火警处置', icon: 'Warning', color: 'linear-gradient(135deg, #ff6b6b, #ee5a5a)', handler: () => router.push('/eiot/firewatch/alarm-order') },
  { label: '设备管理', icon: 'Monitor', color: 'linear-gradient(135deg, #54a0ff, #2e86de)', handler: () => router.push(getAdminPath()) },
  { label: '巡检任务', icon: 'Checked', color: 'linear-gradient(135deg, #1dd1a1, #10ac84)', handler: () => router.push('/eiot/firewatch/patrol') },
  { label: '报警记录', icon: 'Document', color: 'linear-gradient(135deg, #feca57, #f8b739)', handler: () => router.push('/eiot/firewatch/alarm-record') },
  { label: '值班管理', icon: 'Calendar', color: 'linear-gradient(135deg, #fd79a8, #e84393)', handler: () => router.push('/eiot/firewatch/duty') }
])

// Helpers
const pct = (v: number, t: number) => t > 0 ? Math.round((v / t) * 100) : 0
const updateDateTime = () => {
  const now = new Date()
  const w = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentWeek.value = w[now.getDay()]
}
const levelText = (l: string) => ({ critical: '严重', major: '较大', warning: '一般', info: '提示' }[l] || '一般')
const dutyStatusText = (s: string) => ({ ongoing: '值班中', pending: '待值班', completed: '已完成' }[s] || s)
const patrolStatusText = (s: string) => ({ completed: '已完成', in_progress: '进行中', pending: '待巡检', overdue: '已逾期' }[s] || s)

/** 从冗长的告警内容中提取关键摘要信息，只展示事件类型+告警级别+设备地址 */
const extractAlarmSummary = (desc: string) => {
  if (!desc) return '设备告警'
  const typeMatch = desc.match(/事件类型[：:]\s*(.+?)(\s|；|$)/)
  const levelMatch = desc.match(/告警级别[：:]\s*(.+?)(\s|；|$)/)
  const addrMatch = desc.match(/设备地址[：:]\s*(.+?)(\s|；|$)/)
  if (typeMatch || levelMatch || addrMatch) {
    const parts: string[] = []
    if (typeMatch) parts.push(typeMatch[1].trim())
    if (levelMatch) parts.push(levelMatch[1].trim())
    if (addrMatch) {
      const addr = addrMatch[1].trim()
      parts.push(addr.length > 16 ? addr.substring(0, 16) + '...' : addr)
    }
    return parts.join(' | ')
  }
  // 匹配 "警报！xxx" 格式
  const alertMatch = desc.match(/警报！(.+?)(；|$)/)
  if (alertMatch) return alertMatch[1].trim()
  // 其他格式：去掉前缀符号，截取关键内容
  const cleanDesc = desc.replace(/^[⚠️🔴🟠🟡🟢🔔]+/, '').replace(/^[：:，,、\s]+/, '')
  return cleanDesc.length > 25 ? cleanDesc.substring(0, 25) + '...' : cleanDesc
}
const goToAdmin = () => router.push(getAdminPath())
const toggleFullscreen = () => { if (!document.fullscreenElement) document.documentElement.requestFullscreen(); else document.exitFullscreen() }
const viewAlarm = (a: any) => { selectedAlarm.value = a; alarmDialogVisible.value = true }
const formatTime = (t: any) => {
  if (!t) return ''
  if (typeof t === 'number') return new Date(t).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  const s = String(t)
  return s ? s.replace('T', ' ').substring(0, 19) : ''
}

// Data loading
const loadErrors = ref<string[]>([])
const silentGet = async (url: string, params?: any) => {
  try {
    const r = await request.get({ url, params })
    console.log('[MonitorCenter] GET', url, '=>', JSON.stringify(r)?.substring(0, 500))
    return r
  } catch (e: any) {
    console.error('[MonitorCenter] GET', url, 'ERROR:', e?.message || e, e)
    if (!loadErrors.value.includes(url)) {
      loadErrors.value.push(url)
    }
    return null
  }
}

const loadData = async () => {
  // ========== 1. 统计数据（包含巡检/值班/物业，统一走 dashboard 权限+租户隔离） ==========
  const statsRes = await silentGet('/eiot/firewatch/dashboard/statistics')
  const d = statsRes || {}
  if (statsRes) {
    if (d.deviceStatus) {
      deviceStats.value = {
        total: d.deviceStatus.total || 0,
        online: d.deviceStatus.online || d.deviceStatus.normal || 0,
        alarm: d.deviceStatus.alarm || 0,
        fault: d.deviceStatus.fault || 0,
        offline: d.deviceStatus.offline || 0,
        inactive: d.deviceStatus.inactive || 0
      }
    }
    if (d.todayAlarm) {
      todayStats.value = {
        totalAlarms: d.todayAlarm.totalAlarms || 0,
        fireAlarms: d.todayAlarm.fireAlarmCount || 0,
        handledCount: d.todayAlarm.handledCount || 0,
        pendingCount: d.todayAlarm.pendingCount || 0,
        handleRate: d.todayAlarm.handleRate ? Math.round(d.todayAlarm.handleRate * 100) / 100 : 0,
        avgResponse: d.todayAlarm.avgResponseTime || 0
      }
    }
    if (d.realtimeAlarms?.length) {
      alarmList.value = d.realtimeAlarms.map((a: any) => ({
        id: a.id, deviceName: a.deviceName, content: a.alarmDescription || a.alarmContent || '设备告警',
        time: formatTime(a.alarmTime), level: a.alarmLevel || 'warning',
        status: a.orderStatus || 'pending', typeName: a.alarmTypeName, location: a.alarmLocation,
        alarmType: a.alarmCategory, tenantName: a.tenantName, details: a.alarmDescription
      }))
    }
    if (d.alarmTrend?.length) {
      cachedAlarmTrend = d.alarmTrend
      updateTrendChart(d.alarmTrend)
    }
    if (d.alarmTypeDistribution?.length) {
      cachedAlarmTypeDist = d.alarmTypeDistribution
      updateTypeChart(d.alarmTypeDistribution)
    }
    if (d.deviceCategoryDistribution?.length) {
      cachedDeviceCategory = d.deviceCategoryDistribution
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
      facilityStats.value = d.deviceCategoryDistribution.map((item: any, idx: number) => {
        const name = item.categoryName || item.categoryCode || '其他'
        const icon = Object.entries(iconMap).find(([k]) => name.includes(k))?.[1] || 'Monitor'
        return { name, count: item.count || 0, color: colorArr[idx % colorArr.length], icon }
      })
    }
    updateDeviceChart()

    // ===== 从 statistics 中提取巡检/值班/物业数据（监控中心只看本企业数据） =====
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
          inspector: i.inspectorName || '未指定',
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
    // 物业列表（监控中心：企业用户只看到自己企业的物业，不加入地图点位）
    if (d.properties?.length) {
      const mapped = d.properties.map((p: any) => ({
        id: p.id, name: p.name || '未命名企业',
        address: p.address || '-', contactName: p.contactName || '-',
        contactPhone: p.contactPhone || '-', riskLevel: p.riskLevel || 'normal',
        longitude: p.longitude, latitude: p.latitude,
        deviceCount: p.deviceCount || 0
      }))
      enterpriseInfo.value = mapped[0]
    }
  }

  // 告警列表兜底
  if (!alarmList.value.length) {
    const r = await silentGet('/eiot/firewatch/dashboard/realtime-alarms', { limit: 15 })
    if (r?.length) {
      alarmList.value = r.map((a: any) => ({
        id: a.id, deviceName: a.deviceName, content: a.alarmDescription || '设备告警',
        time: (a.alarmTime || '').replace('T', ' ').substring(0, 19), level: a.alarmLevel || 'warning',
        status: a.orderStatus || 'pending', typeName: a.alarmTypeName, location: a.alarmLocation,
        alarmType: a.alarmCategory, tenantName: a.tenantName, details: a.alarmDescription
      }))
    }
  }

  // 从告警列表中提取有关联视频的设备作为摄像头列表
  const videoAlarms = alarmList.value.filter((a: any) => a.videoUrl || a.videoDeviceId)
  if (videoAlarms.length) {
    cameraList.value = videoAlarms.slice(0, 4).map((a: any) => ({
      name: a.deviceName || '监控点', status: 'online'
    }))
  } else {
    cameraList.value = []
  }

  // 企业信息兜底（监控中心只看自己租户，优先用 statistics 返回的 properties）
  if (!enterpriseInfo.value?.id) {
    const propRes = await silentGet('/eiot/firewatch/property/page', { pageNo: 1, pageSize: 100 })
    const propList = propRes?.list || (Array.isArray(propRes) ? propRes : [])
    if (propList.length) {
      const mapped = propList.map((p: any) => ({
        id: p.id, name: p.propertyName || p.name || '未命名企业',
        address: p.address || '-', contactName: p.contactName || '-',
        contactPhone: p.contactPhone || '-', riskLevel: p.riskLevel || 'normal',
        longitude: p.longitude, latitude: p.latitude,
        deviceCount: p.deviceStats?.total || p.deviceCount || 0
      }))
      enterpriseInfo.value = mapped[0]
    }
  }

  // 地图只展示设备点位
  const deviceLocRes = await silentGet('/eiot/firewatch/dashboard/device-locations')
  if (deviceLocRes && Array.isArray(deviceLocRes) && deviceLocRes.length > 0) {
    const stateMap: Record<string, string> = { '0': 'offline', '1': 'normal', '3': 'inactive', '4': 'fault' }
    const statusColorMap: Record<string, string> = { normal: '#52c41a', offline: '#8c8c8c', inactive: '#9ca3af', fault: '#fa8c16', alarm: '#ff4d4f' }
    const devPoints = deviceLocRes.map((dev: any) => ({
      name: dev.name || dev.dn || '未知设备',
      lng: dev.lon,
      lat: dev.lat,
      address: dev.addr || '',
      color: statusColorMap[stateMap[String(dev.state)] || 'offline'] || '#8c8c8c',
      statusText: { offline: '离线', normal: '正常', inactive: '未激活', fault: '故障', alarm: '告警' }[stateMap[String(dev.state)] || 'offline'] || '离线',
      isDevice: true
    })).filter((p: any) => p.lng && p.lat)
    addMarkers(devPoints)
  }

  // 值班数据兜底
  if (!dutyList.value.length) {
    const dutyRes = await silentGet('/eiot/firewatch/duty-schedule/current-duty')
    if (dutyRes) {
      const arr = Array.isArray(dutyRes) ? dutyRes : [dutyRes]
      dutyList.value = arr.slice(0, 3).map((i: any) => ({
        id: i.id, userName: i.dutyPersonName || i.userName, shiftName: i.shiftName,
        startTime: i.startTime?.substring(0, 5) || '08:00', endTime: i.endTime?.substring(0, 5) || '18:00',
        status: i.status || 'ongoing'
      }))
    }
  }

  // 巡检任务兜底（如果 statistics 中没有，不再单独调用 patrol-task/page 以避免权限报错）
  // patrolSummary 数据已由 dashboard/statistics 提供

  // 风险信息
  const ds = deviceStats.value
  riskInfo.value = {
    safeDays: d.safeDays || 0,
    healthRate: d.deviceHealthRate ? Math.round(d.deviceHealthRate * 100) / 100 : (deviceStats.value.total > 0 ? pct(deviceStats.value.online, deviceStats.value.total) : 0),
    pendingIssues: d.pendingIssues || todayStats.value.pendingCount
  }
}

// Chart updates
const updateTrendChart = (data: any[]) => {
  if (!trendChart) return
  trendChart.setOption({
    xAxis: { data: data.map(d => (d.date || '').substring(5)) },
    series: [{ data: data.map(d => d.alarmCount || 0) }, { data: data.map(d => d.handleCount || 0) }]
  })
}
const updateTypeChart = (data: any[]) => {
  if (!typeChart) return
  const colors = ['#ff4d4f', '#fa8c16', '#fadb14', '#52c41a', '#1890ff']
  typeChart.setOption({
    series: [{ data: data.map((d: any, i: number) => ({ value: d.count || 0, name: d.typeName || d.alarmType || '其他', itemStyle: { color: colors[i % colors.length] } })) }]
  })
}
const updateDeviceChart = () => {
  if (!deviceChart) return
  const ds = deviceStats.value
  deviceChart.setOption({
    series: [{
      data: [
        { value: ds.online, name: '在线', itemStyle: { color: '#52c41a' } },
        { value: ds.alarm, name: '告警', itemStyle: { color: '#ff4d4f' } },
        { value: ds.fault, name: '故障', itemStyle: { color: '#fa8c16' } },
        { value: ds.offline, name: '离线', itemStyle: { color: '#8c8c8c' } },
        { value: ds.inactive, name: '未激活', itemStyle: { color: '#d9d9d9' } }
      ]
    }]
  })
}

const initCharts = () => {
  const today = new Date()
  const dates: string[] = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today); d.setDate(d.getDate() - i)
    dates.push(`${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
  }

  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['告警数', '处置数'], textStyle: { color: '#8b9dc5', fontSize: 10 }, top: 0 },
      grid: { left: '10%', right: '5%', top: '22%', bottom: '12%' },
      xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#1e3a5f' } }, axisLabel: { color: '#8b9dc5', fontSize: 10 } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#162d50' } }, axisLabel: { color: '#8b9dc5', fontSize: 10 } },
      series: [
        { name: '告警数', type: 'line', smooth: true, data: new Array(7).fill(0), lineStyle: { color: '#ff4d4f', width: 2 }, itemStyle: { color: '#ff4d4f' }, symbolSize: 4, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(255,77,79,0.35)' }, { offset: 1, color: 'rgba(255,77,79,0.02)' }]) } },
        { name: '处置数', type: 'line', smooth: true, data: new Array(7).fill(0), lineStyle: { color: '#52c41a', width: 2 }, itemStyle: { color: '#52c41a' }, symbolSize: 4 }
      ]
    })
  }

  if (typeChartRef.value) {
    typeChart = echarts.init(typeChartRef.value)
    typeChart.setOption({
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

  if (deviceChartRef.value) {
    deviceChart = echarts.init(deviceChartRef.value)
    const ds = deviceStats.value
    deviceChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie', radius: ['35%', '62%'], center: ['50%', '50%'],
        data: [
          { value: ds.online, name: '在线', itemStyle: { color: '#52c41a' } },
          { value: ds.alarm, name: '告警', itemStyle: { color: '#ff4d4f' } },
          { value: ds.fault, name: '故障', itemStyle: { color: '#fa8c16' } },
          { value: ds.offline, name: '离线', itemStyle: { color: '#8c8c8c' } },
          { value: ds.inactive, name: '未激活', itemStyle: { color: '#d9d9d9' } }
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
  const el = document.getElementById('mc-map')
  if (!el) { setTimeout(initMap, 500); return }
  ;(window as any)._AMapSecurityConfig = { securityJsCode: '330a3d930b185c5b005d31e90fa80b6e' }
  AMapLoader.load({ key: '775f302305d6bbbf2da5ec525db91c74', version: '2.0', plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Marker', 'AMap.InfoWindow'] })
    .then((AMap: any) => {
      mapInstance = new AMap.Map('mc-map', { zoom: 15, center: MAP_CENTER, mapStyle: 'amap://styles/dark', features: ['bg', 'road', 'building', 'point'], viewMode: '2D' })
      mapInstance.addControl(new AMap.Scale())
      mapInstance.addControl(new AMap.ToolBar({ position: 'RB' }))
      // 地图就绪后，添加缓存中的点位
      if (pendingMapPoints.length) addMarkers(pendingMapPoints)
    }).catch(() => {})
}
const addMarkers = (points: any[]) => {
  pendingMapPoints = points // 缓存点位，供地图就绪后使用
  if (!mapInstance) return
  mapMarkers.forEach(m => m.setMap(null)); mapMarkers = []
  const AMap = (window as any).AMap; if (!AMap) return
  points.forEach((p: any) => {
    const lng = p.longitude || p.lng
    const lat = p.latitude || p.lat
    if (!lng || !lat) return
    const isDevice = p.isDevice
    const color = isDevice ? p.color : (p.riskLevel === 'critical' ? '#ff4d4f' : p.riskLevel === 'major' ? '#fa8c16' : '#52c41a')
    const marker = new AMap.Marker({
      position: [lng, lat], title: p.name,
      content: `<div style="background:${color};width:16px;height:16px;border-radius:50%;border:2px solid rgba(255,255,255,0.9);box-shadow:0 0 10px ${color};cursor:pointer;"></div>`
    })
    const infoContent = isDevice
      ? `<div style="padding:10px;font-size:13px;"><b>${p.name || ''}</b><br/><span style="color:#999;">${p.statusText || ''} ${p.address || ''}</span></div>`
      : `<div style="padding:10px;font-size:13px;"><b>${p.name || ''}</b><br/><span style="color:#999;">${p.address || ''}</span></div>`
    const info = new AMap.InfoWindow({ content: infoContent, offset: new AMap.Pixel(0, -20) })
    marker.on('click', () => info.open(mapInstance, [lng, lat]))
    marker.setMap(mapInstance); mapMarkers.push(marker)
  })
  if (mapMarkers.length) mapInstance.setFitView(mapMarkers, false, [50, 50, 50, 50])
}

const handleResize = () => { trendChart?.resize(); typeChart?.resize(); deviceChart?.resize(); mapInstance?.resize() }

// 图表数据刷新（initCharts 创建图表实例后，用已缓存的数据刷新图表）
const refreshCharts = () => {
  updateDeviceChart()
  if (cachedAlarmTrend?.length) updateTrendChart(cachedAlarmTrend)
  if (cachedAlarmTypeDist?.length) updateTypeChart(cachedAlarmTypeDist)
}

let timer: any = null, refreshTimer: any = null
onMounted(() => {
  updateDateTime()
  timer = setInterval(updateDateTime, 1000)
  setTimeout(initMap, 300)
  loadData().then(() => setTimeout(() => { initCharts(); refreshCharts() }, 100))
  refreshTimer = setInterval(loadData, 30000)
  window.addEventListener('resize', handleResize)
  connectWebSocket()

  // 每10秒拉取近5分钟未处置告警
  fetchRecentAlerts()
  alertPollingTimer = setInterval(fetchRecentAlerts, 10000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (refreshTimer) clearInterval(refreshTimer)
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  if (ws) { ws.onclose = null; ws.close(); ws = null }
  if (alertPollingTimer) clearInterval(alertPollingTimer)
  stopAutoHide()
  closeAlarmAudio()
  trendChart?.dispose(); typeChart?.dispose(); deviceChart?.dispose()
  mapInstance?.destroy()
  // 离开页面时停止报警语音播报
  stopAlarmSpeech()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
// ═══════════════════════════════════════════════════
// MONITORING CENTER — Holographic Command Theme
// Deep indigo base / electric cyan accent / warm alerts
// ═══════════════════════════════════════════════════

// ─── Design Tokens (aligned with duty-screen HUD theme) ───
$bg1:      #0a1628;
$bg2:      #0d1f3c;
$bg3:      #0f2747;
$bd:       #1e3a5f;
$glass-bg: rgba(10, 22, 40, 0.001);
$glass-border: rgba(30, 58, 95, 0.45);
$blue:     #1890ff;
$red:      #ff4d4f;
$orange:   #fa8c16;
$green:    #52c41a;
$accent:   rgba(79,195,247,0.9);
$accent-dim: rgba(79,195,247,0.5);
$accent-glow: rgba(79,195,247,0.15);
$slate:    #8b9dc5;
$ghost:    #4e6585;
$tx:       #fff;
$tx2:      #8b9dc5;

.api-error-bar { background: $red; color: #fff; padding: 6px 16px; font-size: 13px; display: flex; align-items: center; justify-content: center; gap: 12px; flex-shrink: 0; z-index: 999; }

// ─── Root ───
.mc-screen {
  width: 100vw; height: 100vh; overflow: hidden;
  display: flex; flex-direction: column;
  background: $bg1;
  color: $tx;
  font-family: 'Microsoft YaHei', 'PingFang SC', -apple-system, sans-serif;
  -webkit-font-smoothing: antialiased;
}

// ─── Header (duty-screen HUD style) ───
.mc-header {
  height: 56px; flex-shrink: 0; position: relative; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border-bottom: 1px solid rgba(79,195,247,0.1);
  box-shadow:
    0 2px 10px rgba(0,0,0,0.18),
    0 0 1px rgba(79,195,247,0.08),
    inset 0 1px 0 rgba(79,195,247,0.04);

  .hd-left .brand {
    display: flex; align-items: center; gap: 12px;

    .brand-icon {
      width: 38px; height: 38px; border-radius: 10px; position: relative;
      background: linear-gradient(135deg, $red, $orange);
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 0 16px rgba($red, 0.35), inset 0 1px 0 rgba(#fff, 0.2);
      animation: iconGlow 3s ease-in-out infinite;

      .fire-icon {
        width: 20px; height: 20px; background: #fff;
        mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 23c-3.6 0-7-2.4-7-7 0-3.3 2.3-5.8 4-7.4.3-.3.8-.1.8.3v2.2c0 .4.5.6.8.3C13.3 8.8 16 5 16 5s1 2.4 1 5c0 1-.2 2-.6 2.8-.2.3.1.7.4.5.8-.5 1.4-1.2 1.8-2 .2-.3.7-.3.8.1.4 1 .6 2.2.6 3.6 0 4.6-3.4 8-8 8z'/%3E%3C/svg%3E") center / contain no-repeat;
        -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 23c-3.6 0-7-2.4-7-7 0-3.3 2.3-5.8 4-7.4.3-.3.8-.1.8.3v2.2c0 .4.5.6.8.3C13.3 8.8 16 5 16 5s1 2.4 1 5c0 1-.2 2-.6 2.8-.2.3.1.7.4.5.8-.5 1.4-1.2 1.8-2 .2-.3.7-.3.8.1.4 1 .6 2.2.6 3.6 0 4.6-3.4 8-8 8z'/%3E%3C/svg%3E") center / contain no-repeat;
      }
    }
    .brand-text {
      h1 {
        font-size: 22px; margin: 0; font-weight: bold; letter-spacing: 3px;
        background: linear-gradient(90deg, #fff, #7ec8f8, #54a0ff);
        -webkit-background-clip: text; -webkit-text-fill-color: transparent;
      }
      p {
        font-size: 10px; color: $slate; letter-spacing: 1px; margin: 1px 0 0; opacity: 0.7;
        font-weight: 500; text-transform: uppercase;
      }
    }
  }

  .hd-center {
    position: absolute; left: 50%; transform: translateX(-50%);
    .alert-bar {
      display: flex; align-items: center; gap: 12px;
      padding: 7px 28px; border-radius: 24px;
      background: linear-gradient(135deg, rgba($red, 0.2), rgba($red, 0.05));
      border: 1px solid rgba($red, 0.35);
      backdrop-filter: blur(8px);
      animation: alertGlow 2.5s ease-in-out infinite;
      .alert-dot {
        width: 10px; height: 10px; border-radius: 50%; background: $red;
        box-shadow: 0 0 8px $red, 0 0 16px rgba($red, 0.4);
        animation: pulse 1.6s ease-in-out infinite;
      }
      span { font-size: 13px; color: #ff9090; font-weight: 600; letter-spacing: 1.5px; }
    }
    .datetime-bar {
      display: flex; align-items: baseline; gap: 16px;
      .dt-date { font-size: 14px; color: $slate; font-weight: 500; letter-spacing: 0.5px; }
      .dt-time { font-size: 22px; font-weight: 800; letter-spacing: 1px; color: $blue; font-variant-numeric: tabular-nums; }
      .dt-week { font-size: 14px; color: $slate; font-weight: 500; }
    }
  }

  .hd-right {
    display: flex; gap: 8px; align-items: center;
    > * { margin-left: 8px; &:first-child { margin-left: 0; } }
    .datetime-compact {
      display: flex; gap: 10px; font-size: 12px; color: $slate; align-items: baseline;
      .time-hl { font-weight: 700; font-size: 14px; color: $blue; }
    }
    .btn-admin {
      background: linear-gradient(135deg, $blue, #0099cc); border: none;
      box-shadow: 0 2px 12px rgba($blue, 0.25), inset 0 1px 0 rgba(#fff, 0.15);
      font-weight: 600; letter-spacing: 0.5px; transition: all 0.25s;
      &:hover { box-shadow: 0 4px 20px rgba($blue, 0.35); transform: translateY(-1px); }
    }
    .btn-fs {
      background: transparent; border: none; color: $slate;
      transition: all 0.25s;
      &:hover { color: $blue; }
    }
  }
}

// ─── Layout ───
.mc-body {
  flex: 1; display: flex; padding: 10px; gap: 10px;
  min-height: 0; position: relative; z-index: 1;
}
.col-left {
  width: 280px; flex-shrink: 0;
  display: flex; flex-direction: column; gap: 8px;
  overflow-y: auto; overflow-x: hidden;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: $bd; border-radius: 3px; &:hover { background: #245099; } }
}
.col-right {
  width: 340px; flex-shrink: 0;
  display: flex; flex-direction: column; gap: 8px;
  overflow-y: auto; overflow-x: hidden;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: $bd; border-radius: 3px; &:hover { background: #245099; } }
}
.col-center {
  flex: 1; display: flex; flex-direction: column; gap: 8px;
  min-width: 0; min-height: 0;
}

// ─── Card System (duty-screen HUD with corner accents) ───
.card {
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border: 1px solid rgba(79,195,247,0.1); border-radius: 8px;
  overflow: visible; display: flex; flex-direction: column; flex-shrink: 0;
  backdrop-filter: blur(12px);
  box-shadow:
    0 2px 10px rgba(0,0,0,0.18),
    0 0 1px rgba(79,195,247,0.08),
    inset 0 1px 0 rgba(79,195,247,0.04),
    inset 0 0 40px rgba(79,195,247,0.012);
  position: relative;
  transition: all 0.5s cubic-bezier(0.23, 1, 0.32, 1);

  &::before {
    content: '';
    position: absolute;
    top: -1px; left: -1px;
    width: 24px; height: 24px;
    border-top: 2px solid $accent-dim;
    border-left: 2px solid $accent-dim;
    border-radius: 8px 0 0 0;
    box-shadow: 0 0 8px $accent-glow;
    z-index: 2;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: -1px; right: -1px;
    width: 24px; height: 24px;
    border-bottom: 2px solid $accent-dim;
    border-right: 2px solid $accent-dim;
    border-radius: 0 0 8px 0;
    box-shadow: 0 0 8px $accent-glow;
    z-index: 2;
  }

  &:hover {
    border-color: rgba(79,195,247,0.25);
    box-shadow:
      0 4px 20px rgba(0,0,0,0.25),
      0 0 20px rgba(79,195,247,0.05),
      inset 0 1px 0 rgba(79,195,247,0.04);
  }

  .card-hd {
    padding: 12px 16px; position: relative;
    background: transparent;
    border-bottom: 1px solid rgba(79,195,247,0.12);
    display: flex; align-items: center; justify-content: space-between;

    &::before {
      content: ''; position: absolute;
      left: 0; top: 20%; bottom: 20%;
      width: 3px;
      background: $accent;
      border-radius: 0 2px 2px 0;
      box-shadow: 0 0 6px rgba(79,195,247,0.5);
    }

    .card-title {
      display: flex; align-items: center; gap: 8px;
      font-size: 15px; font-weight: bold; letter-spacing: 0.5px; color: #fff;
      .el-icon { color: $accent; font-size: 16px; }
    }
    .badge-red {
      background: rgba($red, 0.2); color: $red; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 600;
      margin-left: 4px; border: 1px solid rgba($red, 0.25);
      animation: badgePulse 2s ease-in-out infinite;
    }
  }
  .card-bd { padding: 10px; overflow: hidden; flex: 1; min-height: 0; }
}

// ─── Today Alarm Hero ───
.alarm-hero-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 12px;
  .ah-item {
    text-align: center; padding: 10px 4px; border-radius: 8px; position: relative;
    overflow: hidden; transition: transform 0.2s;
    &:hover { transform: scale(1.03); }

    &::before {
      content: ''; position: absolute; inset: 0; border-radius: 8px;
      border: 1px solid transparent; transition: border-color 0.3s;
    }

    .ah-val {
      font-size: 26px; font-weight: 800; letter-spacing: -0.5px;
      font-variant-numeric: tabular-nums;
      small { font-size: 12px; font-weight: 500; opacity: 0.8; }
    }
    .ah-lbl { font-size: 10px; color: $slate; margin-top: 3px; font-weight: 500; letter-spacing: 0.5px; }

    &.total {
      background: linear-gradient(135deg, rgba($blue, 0.12), rgba($blue, 0.04));
      &::before { border-color: rgba($blue, 0.15); }
      .ah-val { color: $blue; text-shadow: 0 0 12px rgba($blue, 0.3); }
    }
    &.fire {
      background: linear-gradient(135deg, rgba($red, 0.14), rgba($red, 0.04));
      &::before { border-color: rgba($red, 0.15); }
      .ah-val { color: $red; text-shadow: 0 0 12px rgba($red, 0.3); }
    }
    &.handled {
      background: linear-gradient(135deg, rgba($green, 0.12), rgba($green, 0.03));
      &::before { border-color: rgba($green, 0.15); }
      .ah-val { color: $green; text-shadow: 0 0 12px rgba($green, 0.3); }
    }
    &.rate {
      background: linear-gradient(135deg, rgba($orange, 0.12), rgba($orange, 0.03));
      &::before { border-color: rgba($orange, 0.12); }
      .ah-val { color: $orange; text-shadow: 0 0 12px rgba($orange, 0.25); }
    }
  }
}

.response-row {
  display: flex; gap: 8px;
  .rr-item {
    flex: 1; display: flex; justify-content: space-between; align-items: center;
    padding: 7px 12px; border-radius: 6px;
    background: linear-gradient(135deg, rgba($bd, 0.3), rgba($bd, 0.1));
    border: 1px solid rgba($bd, 0.4);
    .rr-lbl { font-size: 11px; color: $slate; font-weight: 500; }
    .rr-val {
      font-size: 17px; font-weight: 800; color: $blue;
      font-variant-numeric: tabular-nums;
      small { font-size: 10px; font-weight: 500; opacity: 0.7; }
      &.warn { color: $orange; }
    }
  }
}

// ─── Device Stats ───
.dev-center {
  text-align: center; padding: 8px 0 12px; margin-bottom: 12px;
  border-bottom: 1px solid rgba($bd, 0.5);
  .dev-num {
    font-size: 38px; font-weight: 900; letter-spacing: -1px;
    background: linear-gradient(180deg, #fff 20%, $blue 100%);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    font-variant-numeric: tabular-nums;
  }
  .dev-unit { font-size: 13px; color: $slate; margin-left: 4px; font-weight: 500; }
}
.dev-bars {
  display: flex; flex-direction: column; gap: 9px;
  .db-row {
    display: flex; align-items: center; gap: 8px;
    .db-lbl { font-size: 11px; color: $slate; width: 32px; text-align: right; font-weight: 500; }
    .db-track {
      flex: 1; height: 7px; border-radius: 4px; overflow: hidden;
      background: rgba($bd, 0.35);
      box-shadow: inset 0 1px 2px rgba(0,0,0,0.2);
    }
    .db-fill {
      height: 100%; border-radius: 4px;
      transition: width 1s cubic-bezier(0.22, 1, 0.36, 1);
    }
    .db-val { font-size: 13px; font-weight: 700; width: 32px; font-variant-numeric: tabular-nums; }
    &.normal { .db-fill { background: linear-gradient(90deg, #52c41a, #73d13d); } .db-val { color: $green; } }
    &.alarm { .db-fill { background: linear-gradient(90deg, #ff4d4f, #ff7875); } .db-val { color: $red; } }
    &.fault { .db-fill { background: linear-gradient(90deg, #fa8c16, #ffc53d); } .db-val { color: $orange; } }
    &.offline { .db-fill { background: linear-gradient(90deg, #595959, #8c8c8c); } .db-val { color: #8c8c8c; } }
  }
}

// ─── Facility List ───
.fac-list { display: flex; flex-direction: column; gap: 2px; }
.fac-row {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 4px; border-radius: 6px;
  transition: background 0.2s;
  &:hover { background: rgba($blue, 0.04); }
  .fac-icon {
    width: 28px; height: 28px; border-radius: 7px;
    display: flex; align-items: center; justify-content: center;
    color: #fff; font-size: 13px; flex-shrink: 0;
    box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  }
  .fac-name { flex: 1; font-size: 12px; color: $tx; font-weight: 500; }
  .fac-count { font-size: 11px; color: $slate; }
  .fac-pct { font-size: 12px; color: $blue; font-weight: 600; width: 38px; text-align: right; }
}

// ─── Quick Actions ───
.quick-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
  .quick-btn {
    display: flex; flex-direction: column; align-items: center; gap: 7px;
    padding: 12px 4px; border-radius: 10px; cursor: pointer;
    background: linear-gradient(160deg, rgba($bd, 0.25), rgba($bd, 0.08));
    border: 1px solid rgba($bd, 0.3);
    transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
    &:hover {
      background: linear-gradient(160deg, rgba($blue, 0.12), rgba($blue, 0.04));
      border-color: rgba($blue, 0.25);
      transform: translateY(-3px);
      box-shadow: 0 8px 20px rgba(0,0,0,0.2), 0 0 12px rgba($blue, 0.08);
      .qb-text { color: $blue; }
    }
    &:active { transform: translateY(-1px); }
    .qb-icon {
      width: 38px; height: 38px; border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      color: #fff; font-size: 18px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.25);
    }
    .qb-text { font-size: 11px; color: $slate; font-weight: 600; transition: color 0.3s; }
  }
}

// ─── Map ───
.map-wrap {
  flex: 1; min-height: 180px; border-radius: 8px; overflow: hidden;
  position: relative; display: flex; flex-direction: column;
  background: $bg2; border: 1px solid rgba(79,195,247,0.1);
  box-shadow: 0 2px 16px rgba(0,0,0,0.2);
  .map-top-bar {
    padding: 10px 16px; display: flex; align-items: center; justify-content: space-between;
    background: linear-gradient(135deg, rgba($blue, 0.06), transparent);
    border-bottom: 1px solid rgba($bd, 0.8);
    .map-title { font-size: 14px; font-weight: 700; letter-spacing: 1px; }
    .map-summary {
      display: flex; gap: 24px;
      .ms-it {
        text-align: center; display: flex; flex-direction: column; align-items: center;
        .ms-v {
          font-size: 20px; font-weight: 800; color: $blue;
          font-variant-numeric: tabular-nums;
          &.green { color: $green; } &.red { color: $red; }
        }
        .ms-l { font-size: 10px; color: $ghost; font-weight: 500; margin-top: 1px; }
      }
    }
  }
  .map-canvas { flex: 1; min-height: 0; }
  .map-legend {
    position: absolute; bottom: 12px; left: 16px;
    display: flex; gap: 14px; padding: 5px 14px; border-radius: 6px;
    background: rgba($bg1, 0.85); backdrop-filter: blur(8px);
    border: 1px solid rgba($bd, 0.5);
    .lg-it {
      display: flex; align-items: center; gap: 5px; font-size: 11px; color: $slate; font-weight: 500;
      i { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
    }
  }
}

// ─── Video ───
.video-wrap {
  background: $bg2; border: 1px solid rgba(79,195,247,0.1); border-radius: 8px; overflow: hidden;
  .video-top-bar {
    padding: 10px 16px; display: flex; align-items: center; justify-content: space-between;
    background: linear-gradient(135deg, rgba($blue, 0.06), transparent);
    border-bottom: 1px solid rgba($bd, 0.8);
    .card-title {
      display: flex; align-items: center; gap: 8px;
      font-size: 13px; font-weight: 600; letter-spacing: 0.8px;
      .el-icon { color: $accent; font-size: 15px; }
    }
    .cam-count {
      font-size: 11px; color: $accent; font-weight: 600;
      background: rgba(79,195,247,0.1); padding: 3px 10px; border-radius: 10px;
      border: 1px solid rgba(79,195,247,0.15);
    }
  }
  .video-grid {
    display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; padding: 8px;
    .video-cell {
      background: rgba(0,0,0,0.35); border: 1px solid rgba($bd, 0.6); border-radius: 8px;
      overflow: hidden; transition: border-color 0.3s;
      &:hover { border-color: rgba(79,195,247,0.3); }
      .vc-head {
        padding: 5px 8px; display: flex; align-items: center; gap: 5px;
        font-size: 11px; color: $slate; font-weight: 500;
        background: rgba($bd, 0.15);
        .el-icon { font-size: 11px; color: $green; }
      }
      .vc-body {
        aspect-ratio: 16/9; display: flex; align-items: center; justify-content: center;
        background: linear-gradient(180deg, rgba(0,0,0,0.1), rgba(0,0,0,0.3));
        .vc-placeholder {
          display: flex; flex-direction: column; align-items: center; gap: 6px;
          .cam-big { font-size: 30px; color: rgba($ghost, 0.25); }
          .live-tag {
            font-size: 9px; font-weight: 700; color: $red; letter-spacing: 1px;
            background: rgba($red, 0.12); padding: 2px 8px; border-radius: 4px;
            border: 1px solid rgba($red, 0.2);
            animation: livePulse 2s ease-in-out infinite;
          }
        }
      }
    }
  }
}

// ─── Charts Row ───
.chart-row {
  display: flex; gap: 8px;
  .chart-card {
    flex: 1; border-radius: 8px; overflow: hidden;
    background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
    border: 1px solid rgba(79,195,247,0.1); display: flex; flex-direction: column;
    backdrop-filter: blur(8px);
    box-shadow: 0 2px 10px rgba(0,0,0,0.15);
    transition: border-color 0.3s;
    &:hover { border-color: rgba(79,195,247,0.25); }
    .card-hd {
      padding: 8px 14px; position: relative;
      background: transparent;
      border-bottom: 1px solid rgba(79,195,247,0.1);
      &::before {
        content: ''; position: absolute; left: 0; top: 3px; bottom: 3px; width: 2px;
        background: $accent;
        border-radius: 0 2px 2px 0;
      }
      .card-title {
        display: flex; align-items: center; gap: 5px;
        font-size: 12px; font-weight: 600; letter-spacing: 0.5px; color: $tx;
        .el-icon { color: $accent; font-size: 13px; }
      }
    }
    .card-bd { padding: 6px; flex: 1; }
    .chart-box { width: 100%; height: 150px; }
  }
}

// ─── Alarm List ───
.alarm-card { flex: 1; min-height: 0; }
.alarm-scroll { flex: 1; overflow: hidden; position: relative; max-height: 360px; }
.alarm-inner {
  display: flex; flex-direction: column; gap: 2px;
  &.scrolling { animation: scrollUp 22s linear infinite; &:hover { animation-play-state: paused; } }
}
.alarm-item {
  display: flex; align-items: flex-start; gap: 10px; padding: 9px 10px;
  border-radius: 8px; cursor: pointer;
  transition: all 0.25s; border: 1px solid transparent;
  &:hover {
    background: rgba($blue, 0.05);
    border-color: rgba($blue, 0.1);
  }
  .ai-dot {
    width: 8px; height: 8px; border-radius: 50%; margin-top: 5px; flex-shrink: 0;
  }
  .ai-body {
    flex: 1; min-width: 0;
    .ai-name { font-size: 14px; font-weight: 600; color: $tx; }
    .ai-desc { font-size: 14px; color: $slate; margin-top: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .ai-time { font-size: 12px; color: $ghost; margin-top: 3px; font-variant-numeric: tabular-nums; }
  }
  .ai-tag {
    font-size: 12px; padding: 3px 9px; border-radius: 10px; white-space: nowrap; flex-shrink: 0;
    font-weight: 600; letter-spacing: 0.3px;
    &.tag-critical { background: rgba($red, 0.15); color: $red; border: 1px solid rgba($red, 0.2); }
    &.tag-major { background: rgba($orange, 0.12); color: $orange; border: 1px solid rgba($orange, 0.18); }
    &.tag-warning { background: rgba(#fadb14, 0.1); color: #fadb14; border: 1px solid rgba(#fadb14, 0.15); }
  }
  &.lv-critical .ai-dot { background: $red; box-shadow: 0 0 8px rgba($red, 0.6); animation: dotPulse 2s ease-in-out infinite; }
  &.lv-major .ai-dot { background: $orange; box-shadow: 0 0 6px rgba($orange, 0.4); }
  &.lv-warning .ai-dot { background: #fadb14; }
  &.lv-info .ai-dot { background: $blue; }
}

.empty-state {
  text-align: center; padding: 24px 0; color: $slate; font-size: 13px;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  .el-icon { font-size: 30px; color: $green; opacity: 0.6; }
  &.small { padding: 12px 0; font-size: 12px; }
}

// ─── Risk Meter ───
.risk-meter {
  .rm-level {
    display: flex; align-items: center; gap: 12px; padding: 12px 14px;
    border-radius: 10px; margin-bottom: 12px;
    .rm-icon { width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 800; flex-shrink: 0; }
    .rm-text { font-size: 17px; font-weight: 700; letter-spacing: 1px; }
    &.rm-low {
      background: linear-gradient(135deg, rgba($green, 0.1), rgba($green, 0.03));
      border: 1px solid rgba($green, 0.15);
      .rm-icon { background: rgba($green, 0.15); color: $green; }
      .rm-text { color: $green; }
    }
    &.rm-medium {
      background: linear-gradient(135deg, rgba($orange, 0.12), rgba($orange, 0.03));
      border: 1px solid rgba($orange, 0.15);
      .rm-icon { background: rgba($orange, 0.15); color: $orange; }
      .rm-text { color: $orange; }
    }
    &.rm-high {
      background: linear-gradient(135deg, rgba($red, 0.12), rgba($red, 0.03));
      border: 1px solid rgba($red, 0.15);
      .rm-icon { background: rgba($red, 0.15); color: $red; animation: riskPulse 2s ease-in-out infinite; }
      .rm-text { color: $red; }
    }
  }
  .rm-detail .rm-row {
    display: flex; justify-content: space-between; padding: 7px 2px;
    border-bottom: 1px solid rgba($bd, 0.35); font-size: 12px;
    &:last-child { border-bottom: none; }
    .rm-lbl { color: $slate; font-weight: 500; }
    .rm-val { font-weight: 700; color: $tx; font-variant-numeric: tabular-nums; }
  }
}

// ─── Patrol ───
.patrol-stats {
  display: flex; gap: 8px; margin-bottom: 12px;
  .ps-it {
    flex: 1; text-align: center; padding: 8px 4px; border-radius: 8px;
    background: linear-gradient(160deg, rgba($bd, 0.25), rgba($bd, 0.08));
    border: 1px solid rgba($bd, 0.3);
    .ps-v {
      font-size: 22px; font-weight: 800; display: block; font-variant-numeric: tabular-nums;
      &.blue { color: $blue; } &.green { color: $green; } &.orange { color: $orange; }
    }
    .ps-l { font-size: 10px; color: $ghost; font-weight: 500; margin-top: 2px; }
  }
}
.patrol-list {
  display: flex; flex-direction: column; gap: 2px;
  .pl-item {
    display: flex; align-items: center; gap: 10px; padding: 6px 4px;
    border-radius: 6px; transition: background 0.2s;
    &:hover { background: rgba($blue, 0.03); }
    .pl-dot {
      width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0;
      &.completed { background: $green; }
      &.in_progress { background: $blue; }
      &.pending { background: $ghost; } &.overdue { background: $red; }
    }
    .pl-info { flex: 1; .pl-name { font-size: 12px; font-weight: 500; } .pl-meta { font-size: 10px; color: $ghost; margin-top: 1px; } }
    .pl-tag {
      font-size: 10px; padding: 2px 8px; border-radius: 8px; font-weight: 600;
      &.completed { background: rgba($green, 0.1); color: $green; } &.in_progress { background: rgba($blue, 0.1); color: $blue; }
      &.pending { background: rgba($ghost, 0.15); color: $slate; } &.overdue { background: rgba($red, 0.1); color: $red; }
    }
  }
}

// ─── Duty ───
.duty-list {
  display: flex; flex-direction: column; gap: 4px;
  .dl-item {
    display: flex; align-items: center; gap: 12px; padding: 6px 4px;
    border-radius: 6px; transition: background 0.2s;
    &:hover { background: rgba($blue, 0.03); }
    .dl-avatar {
      width: 32px; height: 32px; border-radius: 50%;
      background: linear-gradient(135deg, $blue, #0099cc);
      display: flex; align-items: center; justify-content: center;
      color: #fff; font-size: 15px;
      box-shadow: 0 2px 8px rgba($blue, 0.25);
    }
    .dl-info { flex: 1; .dl-name { font-size: 13px; font-weight: 600; } .dl-shift { font-size: 10px; color: $ghost; margin-top: 1px; } }
    .dl-tag {
      font-size: 10px; padding: 3px 8px; border-radius: 8px; font-weight: 600;
      &.ongoing { background: rgba($green, 0.1); color: $green; border: 1px solid rgba($green, 0.15); }
      &.pending { background: rgba($orange, 0.1); color: $orange; border: 1px solid rgba($orange, 0.12); }
      &.completed { background: rgba($ghost, 0.12); color: $slate; }
    }
  }
}

// ─── Dialog ───
.dark-dialog {
  :deep(.el-dialog) {
    background: $bg2;
    border: 1px solid rgba(79,195,247,0.15); border-radius: 8px;
    box-shadow: 0 24px 60px rgba(0,0,0,0.5);
    .el-dialog__header {
      border-bottom: 1px solid rgba(79,195,247,0.1); padding: 16px 20px;
      .el-dialog__title { color: $tx; font-weight: 600; letter-spacing: 0.5px; }
    }
    .el-dialog__body { padding: 20px; }
  }
}
.detail-body .dr {
  display: flex; padding: 10px 0; border-bottom: 1px solid rgba($bd, 0.35);
  &:last-child { border-bottom: none; }
  .dl { width: 60px; color: $slate; font-size: 13px; flex-shrink: 0; font-weight: 500; }
  .dv { flex: 1; font-size: 13px; font-weight: 500; }
  .lv-critical { color: $red; font-weight: 700; } .lv-major { color: $orange; font-weight: 700; } .lv-warning { color: #fadb14; }
}

// ─── 确认报警/误报表单弹窗（暗色表单，与火警处置中心字段一致） ───
.alarm-form-dialog {
  :deep(.el-form-item__label) { color: $slate; font-weight: 500; }
  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    background: rgba(13, 31, 60, 0.9);
    box-shadow: 0 0 0 1px rgba(30, 58, 95, 0.8) inset;
    &:hover { box-shadow: 0 0 0 1px rgba(79, 195, 247, 0.35) inset; }
  }
  :deep(.el-input__inner),
  :deep(.el-textarea__inner) {
    color: #fff;
  }
  :deep(.el-textarea__inner) {
    background: rgba(13, 31, 60, 0.9);
    box-shadow: 0 0 0 1px rgba(30, 58, 95, 0.8) inset;
  }
  :deep(.el-input.is-disabled .el-input__wrapper) {
    background: rgba(13, 31, 60, 0.5);
    box-shadow: 0 0 0 1px rgba(30, 58, 95, 0.5) inset;
  }
  :deep(.el-input.is-disabled .el-input__inner) { color: $ghost; }
  :deep(.el-select__placeholder) { color: $ghost; }
  :deep(.el-select__popper) {
    background: rgba(13, 31, 60, 0.98);
    border: 1px solid rgba(30, 58, 95, 0.8);
    .el-select-dropdown__item {
      color: #c9d7ea;
      &:hover { background: rgba(79, 195, 247, 0.12); }
      &.is-selected { color: #4fc3f7; font-weight: 600; }
    }
    .el-popper__arrow::before {
      background: rgba(13, 31, 60, 0.98);
      border-color: rgba(30, 58, 95, 0.8);
    }
  }
  :deep(.el-alert) {
    background: rgba(250, 140, 22, 0.08);
    border: 1px solid rgba(250, 140, 22, 0.25);
  }
  :deep(.el-alert__title) { color: #ffa940; font-size: 13px; }
  :deep(.el-form-item__error) { color: $red; }
}

// ─── Animations ───
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.4); }
}
@keyframes alertGlow {
  0%, 100% { box-shadow: 0 0 12px rgba($red, 0.15); }
  50% { box-shadow: 0 0 24px rgba($red, 0.3), 0 0 48px rgba($red, 0.1); }
}
@keyframes scrollUp {
  0% { transform: translateY(0); } 100% { transform: translateY(-50%); }
}
@keyframes iconGlow {
  0%, 100% { box-shadow: 0 0 12px rgba($red, 0.3); }
  50% { box-shadow: 0 0 20px rgba($red, 0.5), 0 0 36px rgba($red, 0.15); }
}
@keyframes badgePulse {
  0%, 100% { box-shadow: 0 0 6px rgba($red, 0.2); }
  50% { box-shadow: 0 0 12px rgba($red, 0.4); }
}
@keyframes dotPulse {
  0%, 100% { box-shadow: 0 0 6px rgba($red, 0.4); }
  50% { box-shadow: 0 0 12px rgba($red, 0.7), 0 0 20px rgba($red, 0.3); }
}
@keyframes livePulse {
  0%, 100% { opacity: 1; } 50% { opacity: 0.5; }
}
@keyframes riskPulse {
  0%, 100% { box-shadow: 0 0 10px rgba($red, 0.2); }
  50% { box-shadow: 0 0 20px rgba($red, 0.4), 0 0 30px rgba($red, 0.15); }
}

// ─── Alarm Popup (与值守大屏一致) ───
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
