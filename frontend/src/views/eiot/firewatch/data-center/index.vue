<template>
  <div class="data-center">
    <header class="header">
      <div class="header-left">
        <div class="title-line">
          <h1 class="title">数据中心</h1>
          <DevelopingTag tip="后端 /eiot/firewatch/data-center/* 统计接口尚未实现" />
        </div>
        <p class="subtitle">DATA ANALYTICS CENTER</p>
      </div>
      <div class="header-center">
        <div class="datetime">
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
          <span class="week">{{ currentWeek }}</span>
        </div>
      </div>
      <div class="header-right">
        <el-tooltip content="返回值守大屏" placement="bottom">
          <div class="header-close-btn" @click="goBack">
            <el-icon :size="18"><Close /></el-icon>
          </div>
        </el-tooltip>
      </div>
    </header>

    <!-- Tab 栏 -->
    <div class="tab-bar">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </div>
    </div>

    <!-- 后端 FireDataCenterServiceImpl 全量空壳（15 个接口均返回空），数据来自 mock.ts 兜底
         ponytail: 标识写死在页面级；某块统计接口真实实现后，去掉对应 mock 兜底并把此处提示改成按卡片粒度标注 -->
    <div class="dev-note">
      <el-icon><InfoFilled /></el-icon>
      <span>以下各分析模块的统计接口后端尚未实现（开发中），当前展示为示例数据，不代表真实业务情况</span>
    </div>

    <!-- 主体区域 -->
    <div class="main-body">
      <!-- 左侧筛选栏 -->
      <aside class="filter-sidebar">
        <div class="filter-section">
          <div class="filter-label">时间范围</div>
          <div
            v-for="opt in dayOptions"
            :key="opt.value"
            class="filter-option"
            :class="{ active: selectedDays === opt.value }"
            @click="selectedDays = opt.value"
          >
            {{ opt.label }}
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-label">物业单位</div>
          <div
            class="filter-option"
            :class="{ active: !selectedPropertyId }"
            @click="selectedPropertyId = undefined"
          >
            全部企业
          </div>
          <div
            v-for="p in propertyList"
            :key="p.id"
            class="filter-option"
            :class="{ active: selectedPropertyId === p.id }"
            @click="selectedPropertyId = p.id"
          >
            {{ p.name }}
          </div>
        </div>
      </aside>

      <!-- 图表内容区 -->
      <div class="chart-area">
        <AlarmAnalysis
          v-if="activeTab === 'alarm'"
          :property-id="selectedPropertyId"
          :days="selectedDays"
        />
        <DeviceAnalysis
          v-if="activeTab === 'device'"
          :property-id="selectedPropertyId"
          :days="selectedDays"
        />
        <DutyAnalysis
          v-if="activeTab === 'duty'"
          :property-id="selectedPropertyId"
          :days="selectedDays"
        />
        <PatrolAnalysis
          v-if="activeTab === 'patrol'"
          :property-id="selectedPropertyId"
          :days="selectedDays"
        />
        <WorkOrderAnalysis
          v-if="activeTab === 'workorder'"
          :property-id="selectedPropertyId"
          :days="selectedDays"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Close, Warning, Monitor, User, TrendCharts, Document, InfoFilled } from '@element-plus/icons-vue'
import request from '@/config/axios'
import DevelopingTag from '@/components/DevelopingTag/index.vue'
import AlarmAnalysis from './components/AlarmAnalysis.vue'
import DeviceAnalysis from './components/DeviceAnalysis.vue'
import DutyAnalysis from './components/DutyAnalysis.vue'
import PatrolAnalysis from './components/PatrolAnalysis.vue'
import WorkOrderAnalysis from './components/WorkOrderAnalysis.vue'

const router = useRouter()
const goBack = () => router.push('/duty-screen')

// Tabs
const tabs = [
  { key: 'alarm', label: '告警分析', icon: Warning },
  { key: 'device', label: '设备分析', icon: Monitor },
  { key: 'duty', label: '值班统计', icon: User },
  { key: 'patrol', label: '巡检统计', icon: TrendCharts },
  { key: 'workorder', label: '工单统计', icon: Document },
]

const activeTab = ref('alarm')

// Filters
const dayOptions = [
  { label: '近7天', value: 7 },
  { label: '近30天', value: 30 },
  { label: '近90天', value: 90 },
]
const selectedDays = ref(30)
const selectedPropertyId = ref<number | undefined>(undefined)

// Property list
const propertyList = ref<any[]>([])

const loadProperties = async () => {
  try {
    const r = await request.get({ url: '/eiot/firewatch/property/page', params: { pageNo: 1, pageSize: 100 } })
    const list = r?.list || (Array.isArray(r) ? r : [])
    propertyList.value = list.map((p: any) => ({
      id: p.id,
      name: p.propertyName || p.name || '未命名企业',
    }))
  } catch {
    propertyList.value = []
  }
}

// DateTime
const currentDate = ref('')
const currentTime = ref('')
const currentWeek = ref('')
let timer: ReturnType<typeof setInterval> | null = null

const updateDateTime = () => {
  const now = new Date()
  const w = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六']
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentWeek.value = w[now.getDay()]
}

onMounted(() => {
  updateDateTime()
  timer = setInterval(updateDateTime, 1000)
  loadProperties()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
$bg1: #0a1628;
$bg2: #0d1f3c;
$bd: #1e3a5f;
$t1: #fff;
$t2: #8b9dc5;
$blue: #1890ff;
$cyan: #4FC3F7;
$red: #ff4d4f;

.data-center {
  width: 100vw;
  height: 100vh;
  color: $t1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: $bg1;
  font-family: 'Microsoft YaHei', sans-serif;
}

// ====== Header ======
.header {
  height: 52px;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border-bottom: 1px solid rgba(79,195,247,0.1);
  box-shadow: 0 2px 10px rgba(0,0,0,0.18), inset 0 1px 0 rgba(79,195,247,0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;

  .header-left {
    .title-line {
      display: flex;
      align-items: center;
    }
    .title {
      font-size: 20px;
      font-weight: bold;
      background: linear-gradient(90deg, #fff, #7ec8f8, #54a0ff);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      margin: 0;
      letter-spacing: 3px;
    }
    .subtitle {
      font-size: 10px;
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
      .date { color: $t2; font-size: 14px; font-weight: 500; }
      .time { font-size: 22px; font-weight: 800; color: $blue; letter-spacing: 1px; font-variant-numeric: tabular-nums; }
      .week { color: $t2; font-size: 14px; font-weight: 500; }
    }
  }
  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.header-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  color: $t2;
  cursor: pointer;
  border: 1px solid rgba(79,195,247,0.12);
  transition: all 0.3s;
  &:hover {
    background: rgba(255, 77, 79, 0.15);
    border-color: rgba(255, 77, 79, 0.4);
    color: $red;
  }
}

// ====== Tab Bar ======
.tab-bar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0 24px;
  height: 44px;
  flex-shrink: 0;
  background: rgba(8, 20, 40, 0.6);
  border-bottom: 1px solid rgba(79,195,247,0.08);
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  font-size: 14px;
  color: $t2;
  cursor: pointer;
  border-radius: 6px 6px 0 0;
  border-bottom: 2px solid transparent;
  transition: all 0.25s;
  user-select: none;

  .el-icon { font-size: 15px; }

  &:hover {
    color: $cyan;
    background: rgba(24, 144, 255, 0.06);
  }

  &.active {
    color: $cyan;
    border-bottom-color: $cyan;
    background: rgba(24, 144, 255, 0.08);
    font-weight: 600;
  }
}

// 开发中提示条
.dev-note {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 24px;
  flex-shrink: 0;
  font-size: 12px;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.08);
  border-bottom: 1px solid rgba(230, 162, 60, 0.25);
}

// ====== Main Body ======
.main-body {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

// ====== Filter Sidebar ======
.filter-sidebar {
  width: 180px;
  flex-shrink: 0;
  padding: 12px;
  overflow-y: auto;
  border-right: 1px solid rgba(79,195,247,0.08);
  background: rgba(8, 20, 40, 0.4);
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

.filter-section {
  margin-bottom: 16px;
}

.filter-label {
  font-size: 12px;
  color: $t2;
  margin-bottom: 6px;
  padding-left: 8px;
  letter-spacing: 0.5px;
  font-weight: 600;
}

.filter-option {
  padding: 7px 12px;
  font-size: 13px;
  color: rgba(139, 157, 197, 0.8);
  cursor: pointer;
  border-radius: 5px;
  transition: all 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;

  &:hover {
    color: #fff;
    background: rgba(24, 144, 255, 0.1);
  }

  &.active {
    color: $cyan;
    background: rgba(24, 144, 255, 0.15);
    font-weight: 600;
  }
}

// ====== Chart Area ======
.chart-area {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}
</style>
