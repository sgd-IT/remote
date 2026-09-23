<template>
  <div class="alarm-screen">

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
        <div class="datetime">
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
          <span class="week">{{ currentWeek }}</span>
        </div>
      </div>
      <div class="header-right">
        <el-dropdown trigger="hover" popper-class="header-dropdown-popper" @command="handleNavCommand">
          <el-button class="header-icon-btn"><el-icon><Grid /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="duty-screen"><el-icon><Monitor /></el-icon>大屏值守台</el-dropdown-item>
              <el-dropdown-item command="device-screen"><el-icon><Grid /></el-icon>设备控制室</el-dropdown-item>
              <el-dropdown-item command="ops-workbench"
                ><el-icon><SetUp /></el-icon>运维工作台<DevelopingTag mini tip="页面仅为占位，无任何业务接口" /></el-dropdown-item
              >
              <el-dropdown-item command="data-center"
                ><el-icon><TrendCharts /></el-icon>数据中心<DevelopingTag mini tip="统计接口后端尚未实现，当前为示例数据" /></el-dropdown-item
              >
              <el-dropdown-item command="video-surveillance"><el-icon><VideoCamera /></el-icon>视频监控</el-dropdown-item>
              <el-dropdown-item command="patrol"><el-icon><Avatar /></el-icon>查岗管理</el-dropdown-item>
              <el-dropdown-item divided command="admin"><el-icon><Setting /></el-icon>管理后台</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button class="header-icon-btn"><el-icon><Setting /></el-icon></el-button>
        <el-button class="header-icon-btn" @click="togglePanels"><el-icon><View /></el-icon></el-button>
        <el-tooltip content="全屏" placement="bottom">
          <el-button class="header-icon-btn" @click="toggleFullscreen"><el-icon><FullScreen /></el-icon></el-button>
        </el-tooltip>
        <el-button class="header-icon-btn user-btn" @click="goToAdmin"><el-icon><UserFilled /></el-icon>项目管理员</el-button>
      </div>
    </header>

    <main class="main-content">
      <!-- ====== 左侧面板 ====== -->
      <aside class="left-panel" v-show="showPanels">
        <!-- 告警统计 -->
        <div class="panel-card card-auto">
          <div class="card-header">
            <span class="card-title"><el-icon><Warning /></el-icon>告警统计</span>
          </div>
          <div class="card-content">
            <div class="today-new">
              <span class="today-label">今日新增</span>
              <span class="today-num">{{ alarmStats.todayNew }}</span>
            </div>
            <div class="alarm-summary-grid">
              <div class="summary-card fire" @click="filterByType('fire')">
                <div class="sc-icon"><el-icon><WarningFilled /></el-icon></div>
                <span class="sc-label">疑似火警</span>
                <span class="sc-num">{{ alarmCategoryStats.fire }}</span>
              </div>
              <div class="summary-card fault" @click="filterByType('fault')">
                <div class="sc-icon"><el-icon><CircleCloseFilled /></el-icon></div>
                <span class="sc-label">故障</span>
                <span class="sc-num">{{ alarmCategoryStats.fault }}</span>
              </div>
              <div class="summary-card risk" @click="filterByType('risk')">
                <div class="sc-icon"><el-icon><WarnTriangleFilled /></el-icon></div>
                <span class="sc-label">隐患</span>
                <span class="sc-num">{{ alarmCategoryStats.risk }}</span>
              </div>
              <div class="summary-card other" @click="filterByType('other')">
                <div class="sc-icon"><el-icon><ChatDotRound /></el-icon></div>
                <span class="sc-label">其他</span>
                <span class="sc-num">{{ alarmCategoryStats.other }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 二级告警统计 -->
        <div class="panel-card card-auto">
          <div class="card-header">
            <span class="card-title"><el-icon><DataLine /></el-icon>二级告警统计</span>
          </div>
          <div class="card-content">
            <div class="secondary-alarm-list">
              <div class="sec-item" v-for="item in secondaryAlarmStats" :key="item.type">
                <div class="sec-left">
                  <el-icon class="sec-icon" :style="{ color: item.color }"><WarningFilled /></el-icon>
                  <span class="sec-label">{{ item.label }}</span>
                </div>
                <div class="sec-bar-wrap">
                  <div class="sec-bar-track">
                    <div class="sec-bar-fill" :style="{ width: item.pct + '%', background: item.color }"></div>
                  </div>
                </div>
                <span class="sec-val" :style="{ color: item.color }">{{ item.count }}</span>
              </div>
              <div class="empty small" v-if="!secondaryAlarmStats.length">暂无二级告警</div>
            </div>
          </div>
        </div>

        <!-- 待办任务 -->
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title"><el-icon><List /></el-icon>待办任务</span>
            <span class="card-header-more" @click="goToTodoList">更多 &gt;</span>
          </div>
          <div class="card-content">
            <div class="todo-list" v-if="todoList.length">
              <div class="todo-item" v-for="item in todoList" :key="item.id" @click="handleTodoClick(item)">
                <div class="todo-dot" :class="'dot-' + item.priority"></div>
                <div class="todo-info">
                  <div class="todo-title">{{ item.title }}</div>
                  <div class="todo-time">{{ item.time }}</div>
                </div>
                <span class="todo-status" :class="item.status">{{ item.statusText }}</span>
              </div>
            </div>
            <div class="todo-empty" v-else>
              <el-icon class="todo-empty-icon"><Box /></el-icon>
              <span class="todo-empty-text">暂无数据</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- ====== 右侧面板 ====== -->
      <aside class="right-panel" v-show="showPanels">
        <!-- 顶部状态栏 -->
        <div class="status-bar">
          <div class="status-bar-left">
            <el-select v-model="deviceFilter" placeholder="设备筛选" clearable size="small" class="device-filter-select">
              <el-option label="全部设备" value="" />
              <el-option label="报警主机" value="host" />
              <el-option label="烟感探测器" value="smoke" />
              <el-option label="温感探测器" value="temperature" />
            </el-select>
            <DevelopingTag v-if="deviceListIsMock" tip="后端无 /dashboard/alarm-devices 接口，下方卡片为占位示例数据" />
          </div>
          <div class="status-bar-center">
            <el-icon class="status-warn-icon"><WarnTriangleFilled /></el-icon>
            <span class="status-warn-text" v-if="latestAlarmText">{{ latestAlarmText }}</span>
            <span class="status-warn-text dim" v-else>暂无告警</span>
          </div>
          <div class="status-bar-right">
            <span class="status-time">{{ currentDate }} {{ currentTime }}</span>
            <span class="status-detail-link" @click="goToAlarmRecord">详情 &raquo;</span>
            <el-button class="refresh-btn" :icon="Refresh" circle size="small" @click="loadData" />
          </div>
        </div>

        <!-- 设备告警卡片列表（3列网格，DeviceCard 组件渲染） -->
        <div class="device-card-list">
          <DeviceCard
            v-for="device in deviceList"
            :key="device.id"
            :device="device"
            :selected="selectedDeviceId === device.id"
            @select="selectDevice"
            @action="handleDeviceAction"
            @tab-change="(device, tab) => device.activeTab = tab" />
          <div class="empty" v-if="!deviceList.length"><el-icon><CircleCheck /></el-icon>暂无告警设备</div>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import request from '@/config/axios'
import alarmHostSvg from '@/assets/svg/alarm-host.svg'
import DeviceCard from './components/DeviceCard.vue'
import DevelopingTag from '@/components/DevelopingTag/index.vue'

defineOptions({ name: 'FireWatchAlarmScreen' })

const router = useRouter()
const userStore = useUserStore()

// 时间
const currentDate = ref(''), currentTime = ref(''), currentWeek = ref('')
const updateDateTime = () => {
  const now = new Date()
  const w = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六']
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentWeek.value = w[now.getDay()]
}

// 筛选
const deviceFilter = ref('')

// 数据
const alarmStats = ref({ total: 0, handled: 0, pending: 0, rate: 0, todayNew: 0 })
const alarmList = ref<any[]>([])
const alarmCategoryStats = ref({ fire: 0, fault: 0, risk: 0, other: 0 })
const secondaryAlarmStats = ref<any[]>([
  { type: 'disassemble', label: '拆卸告警', count: 0, pct: 0, color: '#fadb14' },
  { type: 'tamper', label: '防拆告警', count: 0, pct: 0, color: '#fa8c16' },
  { type: 'signal', label: '信号异常', count: 0, pct: 0, color: '#1890ff' },
  { type: 'power', label: '电源告警', count: 0, pct: 0, color: '#ff4d4f' },
])
const todoList = ref<any[]>([])
const loadErrors = ref<string[]>([])
const showPanels = ref(true)

// 右侧设备卡片数据
const deviceList = ref<any[]>([])
const deviceListIsMock = ref(false) // 接口未就绪时为 true，展示占位数据与「敬请期待」
const selectedDeviceId = ref<number | string>('')
const latestAlarmText = computed(() => {
  if (!alarmList.value.length) return ''
  const latest = alarmList.value[0]
  const name = latest.deviceName || latest.propertyName || '设备'
  const summary = extractAlarmSummary(latest.alarmDescription)
  return `${name}: ${summary}`
})

// Computed
const extractAlarmSummary = (desc: string) => {
  if (!desc) return '设备告警'
  const alertMatch = desc.match(/警报！(.+?)(；|$)/)
  if (alertMatch) return alertMatch[1].trim()
  const eventTypeMatch = desc.match(/事件类型：(.+?)(；|$)/)
  if (eventTypeMatch) {
    const addrMatch = desc.match(/设备地址：(.+?)(；|$)/)
    return addrMatch ? `${eventTypeMatch[1].trim()} - ${addrMatch[1].trim()}` : eventTypeMatch[1].trim()
  }
  return desc.length > 30 ? desc.substring(0, 30) + '...' : desc
}

// Navigation
const navRoutes: Record<string, string> = {
  'duty-screen': '/duty-screen',
  'device-screen': '/device-screen',
  'remote-console': '/remote-console',
  'ops-workbench': '/ops-workbench',
  'data-center': '/data-center',
  'video-surveillance': '/video-surveillance',
  'patrol': '/eiot/firewatch/patrol'
}
// 管理后台路径按角色区分：管理员/监管(super_admin/monitor_role) → property，企业用户 → enterprise
const getAdminPath = () => {
  const isAdmin = userStore.getRoles?.some(r => r === 'super_admin' || r === 'monitor_role')
  return isAdmin ? '/eiot/firewatch/property' : '/eiot/firewatch/enterprise'
}
const handleNavCommand = (command: string) => {
  if (command === 'admin') { goToAdmin(); return }
  const path = navRoutes[command]
  if (path && router.currentRoute.value.path !== path) router.push(path)
}
const goToAdmin = () => router.push(getAdminPath())
const toggleFullscreen = () => { if (!document.fullscreenElement) document.documentElement.requestFullscreen(); else document.exitFullscreen() }
const togglePanels = () => { showPanels.value = !showPanels.value }

const viewAlarm = (alarm: any) => {
  console.log('查看告警详情', alarm)
}

const filterByType = (type: string) => {
  console.log('按类型筛选', type)
}
const goToTodoList = () => {
  router.push('/eiot/firewatch/alarm-order')
}
const handleTodoClick = (item: any) => {
  console.log('点击待办任务', item)
}

// 右侧设备卡片操作
const selectDevice = (device: any) => {
  // 点击已选中的卡片则取消选中（toggle）
  selectedDeviceId.value = selectedDeviceId.value === device.id ? '' : device.id
}
const handleDeviceAction = (device: any, action: string) => {
  console.log('设备操作', device.name, action)
}
const goToAlarmRecord = () => {
  router.push('/eiot/firewatch/alarm-record')
}

// 加载设备列表
const loadDeviceList = async () => {
  const res = await silentGet('/eiot/firewatch/dashboard/alarm-devices')
  if (res && Array.isArray(res)) {
    deviceListIsMock.value = false
    deviceList.value = res.map((d: any) => ({
      id: d.id,
      name: d.name || '报警主机',
      brand: d.brand || '',
      fullName: d.fullName || d.name || '-',
      icon: d.icon || alarmHostSvg,
      fireCount: d.fireCount || 0,
      faultCount: d.faultCount || 0,
      alarmText: d.alarmText || '',
      address: d.address || '-',
      activeTab: 'sub',
      ...d
    }))
  } else {
    // 接口未就绪时的占位数据，预览卡片渲染效果
    deviceListIsMock.value = true
    deviceList.value = [
      { id: 1, name: '报警主机', brand: '四信', fullName: '安为测试消防远控装置', fireCount: 1, faultCount: 2, alarmText: '烟感探测器告警', address: '安为科技1号楼', activeTab: 'sub' },
      { id: 2, name: '报警主机', brand: '海湾', fullName: '海湾消防主机GST5000', fireCount: 0, faultCount: 0, alarmText: '', address: '安为科技2号楼', activeTab: 'sub' },
      { id: 3, name: '报警主机', brand: '利达', fullName: '利达LD128E II', fireCount: 3, faultCount: 1, alarmText: '回路故障', address: '安为科技3号楼', activeTab: 'sub' },
      { id: 4, name: '报警主机', brand: '松江', fullName: '松江JB-3208', fireCount: 0, faultCount: 1, alarmText: '', address: '安为科技4号楼', activeTab: 'sub' },
      { id: 5, name: '报警主机', brand: '赋安', fullName: '赋安FS5000', fireCount: 2, faultCount: 0, alarmText: '手报触发', address: '安为科技5号楼', activeTab: 'sub' },
      { id: 6, name: '报警主机', brand: '北大青鸟', fullName: '北大青鸟JBF-11S', fireCount: 0, faultCount: 0, alarmText: '', address: '安为科技6号楼', activeTab: 'sub' },
    ]
  }
}

// Data loading
const silentGet = async (url: string, params?: any) => {
  try {
    const r = await request.get({ url, params })
    return r
  } catch (e: any) {
    console.error('[AlarmScreen] GET', url, 'ERROR:', e?.message || e)
    if (!loadErrors.value.includes(url)) loadErrors.value.push(url)
    return null
  }
}

const loadData = async () => {
  const statsRes = await silentGet('/eiot/firewatch/dashboard/statistics')
  const d = statsRes || {}
  if (statsRes) {
    if (d.todayAlarm) {
      alarmStats.value = {
        total: d.todayAlarm.totalAlarms || 0,
        handled: d.todayAlarm.handledCount || 0,
        pending: d.todayAlarm.pendingCount || 0,
        rate: d.todayAlarm.handleRate ? Math.round(d.todayAlarm.handleRate * 100) / 100 : 0,
        todayNew: d.todayAlarm.todayNew || d.todayAlarm.totalAlarms || 0
      }
    }
    if (d.realtimeAlarms?.length) alarmList.value = d.realtimeAlarms
  }

  if (!alarmList.value.length) {
    const r = await silentGet('/eiot/firewatch/dashboard/realtime-alarms', { limit: 50 })
    if (r?.length) alarmList.value = r
  }

  updateAlarmCategoryStats()
  updateSecondaryAlarmStats()
  loadDeviceList()
}

const updateAlarmCategoryStats = () => {
  const cat = { fire: 0, fault: 0, risk: 0, other: 0 }
  alarmList.value.forEach(a => {
    const t = a.alarmType || ''
    if (t === 'fire') cat.fire++
    else if (t === 'fault') cat.fault++
    else if (t === 'risk') cat.risk++
    else cat.other++
  })
  alarmCategoryStats.value = cat
}

const updateSecondaryAlarmStats = () => {
  // 从告警列表中按二级类型统计
  const secCount: Record<string, number> = {}
  alarmList.value.forEach(a => {
    const sub = a.subAlarmType || a.alarmSubType || ''
    if (sub) secCount[sub] = (secCount[sub] || 0) + 1
  })
  const max = Math.max(1, ...Object.values(secCount), ...secondaryAlarmStats.value.map(s => s.count))
  secondaryAlarmStats.value = secondaryAlarmStats.value.map(item => ({
    ...item,
    count: secCount[item.type] || 0,
    pct: Math.round(((secCount[item.type] || 0) / max) * 100)
  }))
}

let timer: any = null, refreshTimer: any = null
onMounted(() => {
  updateDateTime()
  timer = setInterval(updateDateTime, 1000)
  loadData()
  refreshTimer = setInterval(loadData, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style lang="scss" scoped>
@use 'sass:color';
$bg1: #0a1628; $bg2: #0d1f3c; $bg3: #0f2747; $bd: #1e3a5f; $t1: #fff; $t2: #8b9dc5;
$glass-bg: rgba(10, 22, 40, 0.001);
$header-bg: rgba(10, 22, 40, 0.001);
$glass-border: rgba(30, 58, 95, 0.45);
$blue: #1890ff; $red: #ff4d4f; $orange: #fa8c16; $green: #52c41a; $yellow: #fadb14;

.api-error-bar { background: rgba($red, 0.85); backdrop-filter: blur(8px); color: #fff; padding: 6px 16px; font-size: 13px; display: flex; align-items: center; justify-content: center; gap: 12px; flex-shrink: 0; z-index: 999; position: relative; }
.alarm-screen { width: 100vw; height: 100vh; color: $t1; overflow: hidden; display: flex; flex-direction: column; font-family: 'Microsoft YaHei', sans-serif; position: relative; background: linear-gradient(160deg, $bg1 0%, $bg2 50%, $bg3 100%); }

// ====== Header（与 duty-screen 完全一致） ======
.header {
  height: 56px;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border-bottom: 1px solid rgba(79,195,247,0.1);
  box-shadow:
    0 2px 10px rgba(0,0,0,0.18),
    0 0 1px rgba(79,195,247,0.08),
    inset 0 1px 0 rgba(79,195,247,0.04);
  display: flex; align-items: center; justify-content: space-between; padding: 0 20px; flex-shrink: 0; position: relative; z-index: 10;

  .header-left {
    .title { font-size: 22px; font-weight: bold; background: linear-gradient(90deg, #fff, #7ec8f8, #54a0ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin: 0; letter-spacing: 3px; }
    .subtitle { font-size: 10px; color: $t2; letter-spacing: 1px; margin-top: 1px; opacity: 0.7; }
  }
  .header-center {
    position: absolute; left: 50%; transform: translateX(-50%);
    .datetime {
      display: flex; align-items: baseline; gap: 16px;
      .date { color: $t2; font-size: 14px; font-weight: 500; letter-spacing: 0.5px; }
      .time { font-size: 22px; font-weight: 800; color: $blue; letter-spacing: 1px; font-variant-numeric: tabular-nums; }
      .week { color: $t2; font-size: 14px; font-weight: 500; }
    }
  }
  .header-right {
    display: flex; align-items: center;
    > * { margin-left: 8px; &:first-child { margin-left: 0; } }
    .header-icon-btn {
      background: transparent; border: none; color: $t2; padding: 4px 6px;
      &:hover { color: $blue; }
      &.active { color: $blue; }
      .el-icon { font-size: 16px; }
    }
    .user-btn { padding: 4px 8px; font-size: 13px; }
  }
}

// ====== Layout ======
.main-content { flex: 1; display: flex; padding: 10px; gap: 10px; min-height: 0; position: relative; z-index: 1; }
.left-panel { width: 400px; flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; height: 100%; scrollbar-width: none; &::-webkit-scrollbar { display: none; } }
.right-panel { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; height: 100%; scrollbar-width: none; &::-webkit-scrollbar { display: none; } }

// ====== Panel Card（HUD 科技感，与 duty-screen 一致） ======
.panel-card {
  flex: 1;
  min-height: 0;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border: 1px solid rgba(79,195,247,0.1);
  border-radius: 8px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  overflow: visible;
  position: relative;
  transition: all 0.5s cubic-bezier(0.23, 1, 0.32, 1);
  box-shadow:
    0 2px 10px rgba(0,0,0,0.18),
    0 0 1px rgba(79,195,247,0.08),
    inset 0 1px 0 rgba(79,195,247,0.04),
    inset 0 0 40px rgba(79,195,247,0.012);
  transform-style: preserve-3d;

  &::before {
    content: '';
    position: absolute;
    top: -1px; left: -1px;
    width: 24px; height: 24px;
    border-top: 2px solid rgba(79,195,247,0.5);
    border-left: 2px solid rgba(79,195,247,0.5);
    border-radius: 8px 0 0 0;
    box-shadow: 0 0 8px rgba(79,195,247,0.15);
    z-index: 2;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: -1px; right: -1px;
    width: 24px; height: 24px;
    border-bottom: 2px solid rgba(79,195,247,0.5);
    border-right: 2px solid rgba(79,195,247,0.5);
    border-radius: 0 0 8px 0;
    box-shadow: 0 0 8px rgba(79,195,247,0.15);
    z-index: 2;
  }

  .card-content {
    flex: 1;
    overflow-y: auto;
    min-height: 0;
    padding: 10px;
    border: none;
    scrollbar-width: none;
    &::-webkit-scrollbar { display: none; }
  }
  .card-header {
    padding: 12px 16px;
    background: transparent;
    border-bottom: 1px solid rgba(79,195,247,0.12);
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 0; top: 20%; bottom: 20%;
      width: 3px;
      background: rgba(79,195,247,0.9);
      border-radius: 0 2px 2px 0;
      box-shadow: 0 0 6px rgba(79,195,247,0.5);
    }
    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: bold;
      color: #fff;
      letter-spacing: 0.5px;
      .el-icon { color: rgba(79,195,247,0.9); font-size: 16px; }
    }
    .card-header-more {
      font-size: 12px;
      color: $t2;
      cursor: pointer;
      transition: color 0.2s;
      &:hover { color: $blue; }
    }
    .badge {
      background: rgba(79,195,247,0.25);
      color: rgba(79,195,247,1);
      padding: 2px 10px;
      border-radius: 10px;
      font-size: 12px;
      margin-left: 4px;
      border: 1px solid rgba(79,195,247,0.2);
      &.red { background: rgba($red, 0.2); color: $red; border-color: rgba($red, 0.25); }
    }
  }
}

.panel-card.card-auto { flex: 0 auto; .card-content { flex: 0 auto; overflow: visible; } }

// ====== 今日新增 ======
.today-new { display: flex; align-items: baseline; gap: 8px; padding: 4px 0 12px; border-bottom: 1px dashed rgba($bd, 0.35); margin-bottom: 12px;
  .today-label { font-size: 13px; color: $t2; }
  .today-num { font-size: 32px; font-weight: bold; color: #fff; line-height: 1; }
}

// ====== 告警统计 2x2 网格 ======
.alarm-summary-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px;
  .summary-card {
    display: flex; flex-direction: column; align-items: center; gap: 6px;
    padding: 14px 8px; border-radius: 8px; cursor: pointer;
    background: rgba($blue, 0.06); border: 1px solid $glass-border;
    transition: all 0.25s;
    &:hover { border-color: rgba($blue, 0.3); transform: translateY(-1px); }
    .sc-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
    .sc-label { font-size: 12px; color: $t2; }
    .sc-num { font-size: 22px; font-weight: bold; }
    &.fire { .sc-icon { background: rgba($red, 0.15); color: $red; } .sc-num { color: $red; } }
    &.fault { .sc-icon { background: rgba($orange, 0.15); color: $orange; } .sc-num { color: $orange; } }
    &.risk { .sc-icon { background: rgba($yellow, 0.15); color: $yellow; } .sc-num { color: $yellow; } }
    &.other { .sc-icon { background: rgba(#8c8c8c, 0.15); color: #8c8c8c; } .sc-num { color: #8c8c8c; } }
  }
}

// ====== 二级告警统计 ======
.secondary-alarm-list { display: flex; flex-direction: column; gap: 12px;
  .sec-item { display: flex; align-items: center; gap: 10px;
    .sec-left { display: flex; align-items: center; gap: 6px; width: 100px; flex-shrink: 0;
      .sec-icon { font-size: 16px; }
      .sec-label { font-size: 12px; color: #fff; white-space: nowrap; }
    }
    .sec-bar-wrap { flex: 1; min-width: 0;
      .sec-bar-track { height: 8px; background: rgba($bd, 0.4); border-radius: 4px; overflow: hidden; }
      .sec-bar-fill { height: 100%; border-radius: 4px; transition: width 0.8s ease; }
    }
    .sec-val { font-size: 13px; font-weight: bold; width: 28px; text-align: right; }
  }
}

// ====== 待办任务 ======
.todo-list { display: flex; flex-direction: column; gap: 6px;
  .todo-item {
    display: flex; align-items: center; gap: 10px;
    padding: 10px 12px; border-radius: 6px;
    background: rgba($blue, 0.06); border: 1px solid $glass-border;
    cursor: pointer; transition: all 0.2s;
    &:hover { border-color: rgba($blue, 0.3); background: rgba($blue, 0.1); }
    .todo-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
      &.dot-high { background: $red; box-shadow: 0 0 4px rgba($red, 0.4); }
      &.dot-medium { background: $orange; box-shadow: 0 0 4px rgba($orange, 0.4); }
      &.dot-low { background: $blue; }
    }
    .todo-info { flex: 1; min-width: 0;
      .todo-title { font-size: 13px; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .todo-time { font-size: 11px; color: $t2; margin-top: 2px; }
    }
    .todo-status { font-size: 11px; padding: 2px 8px; border-radius: 4px; white-space: nowrap; flex-shrink: 0;
      &.pending { background: rgba($orange, 0.15); color: $orange; }
      &.handling { background: rgba($blue, 0.15); color: $blue; }
      &.done { background: rgba($green, 0.15); color: $green; }
    }
  }
}
.todo-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; gap: 12px;
  .todo-empty-icon { font-size: 48px; color: rgba($blue, 0.4); }
  .todo-empty-text { font-size: 13px; color: $t2; }
}

// ====== 状态栏 ======
.status-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 14px; flex-shrink: 0;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border: 1px solid rgba(79,195,247,0.1);
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.18), 0 0 1px rgba(79,195,247,0.08);
  .status-bar-left { flex-shrink: 0;
    .device-filter-select { width: 120px;
      :deep(.el-input__wrapper) { background: rgba($bg3, 0.8); border: 1px solid rgba($bd, 0.5); box-shadow: none; }
      :deep(.el-input__inner) { color: $t1; font-size: 12px; }
      :deep(.el-select__placeholder) { color: $t2; font-size: 12px; }
    }
  }
  .status-bar-center { flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px; min-width: 0;
    .status-warn-icon { color: $yellow; font-size: 16px; flex-shrink: 0; }
    .status-warn-text { font-size: 13px; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
      &.dim { color: $t2; }
    }
  }
  .status-bar-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0;
    .status-time { font-size: 12px; color: $t2; white-space: nowrap; }
    .status-detail-link { font-size: 12px; color: $t2; cursor: pointer; &:hover { color: $blue; } }
    .refresh-btn { background: transparent; border: 1px solid rgba($bd, 0.5); color: $t2; &:hover { color: $blue; border-color: rgba($blue, 0.3); } }
  }
}

// ====== 设备告警卡片（SVG背景框架 + 5列网格） ======
.device-card-list {
  flex: 1; min-height: 0; overflow-y: auto;
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px;
  align-content: start;
  padding: 16px 1rem 2rem 0;
  font-size: 10px;
  color: #fff;
  scrollbar-width: none; &::-webkit-scrollbar { display: none; }
}
// ====== Common ======
.empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; color: $green; .el-icon { font-size: 32px; margin-bottom: 6px; } &.small { padding: 10px; font-size: 12px; color: $t2; .el-icon { font-size: 20px; } } }

</style>

<style lang="scss">
.header-dropdown-popper {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0 !important;
  .el-dropdown-menu {
    background: rgba(10, 22, 40, 0.92) !important;
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(30, 58, 95, 0.35) !important;
    border-radius: 6px !important;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.5) !important;
    padding: 4px 0 !important;
  }
  .el-dropdown-menu__item {
    background: transparent !important;
    color: #8b9dc5 !important;
    font-size: 13px;
    padding: 6px 16px;
    &:hover {
      background: rgba(24, 144, 255, 0.15) !important;
      color: #fff !important;
    }
  }
  .el-popper__arrow::before {
    background: rgba(10, 22, 40, 0.92);
    border: 1px solid rgba(30, 58, 95, 0.35);
  }
}
</style>
