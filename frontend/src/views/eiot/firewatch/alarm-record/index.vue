<template>
  <div class="alarm-record-container">
    <header class="page-head">
      <h1 class="page-title">报警统计</h1>
      <p class="page-sub">查询条件同时作用于统计指标与下方明细</p>
    </header>

    <!-- 筛选与指标合成一块控制面板，避免卡片堆叠 -->
    <div class="control-panel">
      <div class="filter-bar">
        <el-form :model="queryParams" inline>
        <el-form-item label="报警时间">
          <el-date-picker
            v-model="queryParams.timeRange"
            type="datetimerange"
            range-separator="-"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 340px"
            value-format="YYYY-MM-DD HH:mm:ss"
            :shortcuts="dateShortcuts"
          />
        </el-form-item>
        <el-form-item label="所属单位">
          <el-select v-model="queryParams.propertyId" placeholder="全部单位" clearable style="width: 180px">
            <el-option v-for="item in propertyList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="queryParams.deviceType" placeholder="全部类型" clearable style="width: 140px">
            <el-option v-for="item in deviceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="报警类型">
          <el-select v-model="queryParams.alarmType" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="火警" value="fire" />
            <el-option label="故障" value="fault" />
            <el-option label="隐患" value="risk" />
            <el-option label="离线" value="offline" />
            <el-option label="误报" value="false_alarm" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="handling" />
            <el-option label="已处置" value="handled" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
        </el-form>
      </div>

      <div class="stat-strip">
        <div class="stat-cell stat-cell--lead">
          <div class="stat-label">报警总数</div>
          <div class="stat-value">{{ statistics.totalAlarms }}</div>
        </div>
        <div class="stat-cell">
          <div class="stat-label">火警</div>
          <div class="stat-value stat-value--alert">{{ statistics.fireAlarms }}</div>
        </div>
        <div class="stat-cell">
          <div class="stat-label">已处置</div>
          <div class="stat-value">{{ statistics.handledAlarms }}</div>
        </div>
        <div class="stat-cell">
          <div class="stat-label">处置率</div>
          <div class="stat-value">{{ statistics.handleRate }}<span class="unit">%</span></div>
        </div>
        <div class="stat-cell">
          <div class="stat-label">平均响应</div>
          <div class="stat-value">{{ statistics.avgResponseTime }}<span class="unit">s</span></div>
        </div>
      </div>
    </div>

    <!-- 趋势分析 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <div class="card-heading">
            <span class="section-no">01</span>
            <span class="title">报警趋势</span>
            <span class="section-rule"></span>
          </div>
          <div class="header-actions">
            <el-radio-group v-model="trendPeriod" size="small" @change="loadTrendData">
              <el-radio-button value="day">按日</el-radio-button>
              <el-radio-button value="week">按周</el-radio-button>
              <el-radio-button value="month">按月</el-radio-button>
              <el-radio-button value="year">按年</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <div class="chart-row">
        <div class="chart-item">
          <div ref="trendChartRef" style="height: 300px"></div>
        </div>
        <div class="chart-item">
          <div ref="typeChartRef" style="height: 300px"></div>
        </div>
      </div>
    </el-card>

    <!-- 统计分析 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <div class="card-heading">
            <span class="section-no">02</span>
            <span class="title">多维度统计</span>
            <span class="section-rule"></span>
          </div>
          <div class="header-actions">
            <el-radio-group v-model="statDimension" size="small" @change="loadDimensionData">
              <el-radio-button value="unit">按单位</el-radio-button>
              <el-radio-button value="device">按设备</el-radio-button>
              <el-radio-button value="area">按区域</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <div class="chart-row">
        <div class="chart-item">
          <div ref="dimensionChartRef" style="height: 280px"></div>
        </div>
        <div class="chart-item">
          <div ref="handleRateChartRef" style="height: 280px"></div>
        </div>
      </div>
    </el-card>

    <!-- 周期运行报告：工具带形态，与上方分析卡片区分 -->
    <div class="report-strip">
      <div class="report-info">
        <span class="report-title">周期运行报告</span>
        <span class="report-tip">按月 / 季 / 年生成 PDF，含告警分析、设备状态与 AI 建议</span>
      </div>
      <div class="report-actions">
        <el-select v-model="reportPeriodType" style="width: 92px" @change="handlePeriodTypeChange">
          <el-option v-for="opt in periodTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-date-picker
          v-if="reportPeriodType === 'monthly'"
          v-model="reportPeriod"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择月份"
          :clearable="false"
          style="width: 132px"
        />
        <el-date-picker
          v-else-if="reportPeriodType === 'yearly'"
          v-model="reportPeriod"
          type="year"
          value-format="YYYY"
          placeholder="选择年度"
          :clearable="false"
          style="width: 132px"
        />
        <el-select v-else v-model="reportPeriod" placeholder="选择季度" style="width: 146px">
          <el-option v-for="opt in quarterOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button type="primary" :loading="exporting" @click="handleExportReport">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div class="card-heading">
            <span class="section-no">03</span>
            <span class="title">报警记录明细</span>
            <span class="section-rule"></span>
          </div>
          <div class="header-actions">
            <el-button plain @click="handleExportExcel">
              <el-icon><Download /></el-icon>
              导出 Excel
            </el-button>
            <el-button plain @click="handleExportPdf">
              <el-icon><Document /></el-icon>
              导出 PDF
            </el-button>
            <el-button plain @click="loadTableData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        style="width: 100%"
        :default-sort="{ prop: 'alarmTime', order: 'descending' }"
        @sort-change="handleSortChange"
      >
        <el-table-column prop="alarmTime" label="报警时间" width="170" sortable="custom" />
        <el-table-column prop="propertyName" label="所属单位" min-width="150" show-overflow-tooltip />
        <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="deviceType" label="设备类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.deviceTypeName || getDeviceTypeText(row.deviceType) || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmType" label="报警类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getAlarmTypeTag(row.alarmType)" size="small">
              {{ getAlarmTypeText(row.alarmType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="安装位置" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="处置状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="处置人" width="90" />
        <el-table-column prop="handleTime" label="处置时间" width="170" sortable="custom" />
        <el-table-column prop="responseTime" label="响应时长" width="100" sortable="custom">
          <template #default="{ row }">
            <span :class="getResponseTimeClass(row.responseTime)">
              {{ row.responseTime ? row.responseTime + 's' : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadTableData"
          @current-change="loadTableData"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="报警记录详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="报警ID">{{ currentRecord.id }}</el-descriptions-item>
        <el-descriptions-item label="报警时间">{{ currentRecord.alarmTime }}</el-descriptions-item>
        <el-descriptions-item label="所属单位">{{ currentRecord.propertyName || currentRecord.tenantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ currentRecord.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">
          <el-tag size="small">{{ currentRecord.deviceTypeName || getDeviceTypeText(currentRecord.deviceType) || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报警类型">
          <el-tag :type="getAlarmTypeTag(currentRecord.alarmType)" size="small">
            {{ getAlarmTypeText(currentRecord.alarmType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="安装位置" :span="2">{{ currentRecord.location }}</el-descriptions-item>
        <el-descriptions-item label="处置状态">
          <el-tag :type="getStatusTag(currentRecord.status)" size="small">
            {{ getStatusText(currentRecord.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处置人">{{ currentRecord.handler || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处置时间">{{ currentRecord.handleTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="响应时长">
          {{ currentRecord.responseTime ? currentRecord.responseTime + 's' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="处置备注" :span="2">{{ currentRecord.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, Document, View } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/config/axios'
import download from '@/utils/download'
import { getPropertyList, getAlarmOrderPage, getDeviceCategory, getAlarmTrend, getAlarmRecordStatistics } from '@/api/eiot/firewatch/firewatch.api'
import { getMsgList } from '@/api/eiot/alarm/alarm.api'

defineOptions({ name: 'FireWatchAlarmRecord' })

// 图表主色跟随系统主题色，避免页面里再引入第二套品牌色
const THEME_COLOR =
  getComputedStyle(document.documentElement).getPropertyValue('--el-color-primary').trim() || '#409eff'

// 日期快捷选项
const dateShortcuts = [
  { text: '最近一周', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 7); return [start, end] } },
  { text: '最近一月', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 30); return [start, end] } },
  { text: '最近三月', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 90); return [start, end] } },
  { text: '最近一年', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 365); return [start, end] } }
]

// 查询参数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  timeRange: [] as string[],
  propertyId: undefined as number | undefined,
  deviceType: '',
  alarmType: '',
  status: '',
  sortField: 'alarmTime',
  sortOrder: 'descending'
})

// 数据
const propertyList = ref<any[]>([])
const deviceTypeOptions = ref<{ label: string; value: string }[]>([])
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const trendPeriod = ref('day')
const statDimension = ref('unit')

// 统计
const statistics = reactive({
  totalAlarms: 0,
  fireAlarms: 0,
  handledAlarms: 0,
  handleRate: 0,
  avgResponseTime: 0
})

// 图表引用
const trendChartRef = ref<HTMLElement>()
const typeChartRef = ref<HTMLElement>()
const dimensionChartRef = ref<HTMLElement>()
const handleRateChartRef = ref<HTMLElement>()

let trendChart: echarts.ECharts | null = null
let typeChart: echarts.ECharts | null = null
let dimensionChart: echarts.ECharts | null = null
let handleRateChart: echarts.ECharts | null = null

// 映射
const deviceTypeMap: Record<string, string> = {
  smoke: '烟感探测器',
  temperature: '温感探测器',
  manual: '手动报警器',
  gas: '燃气探测器',
  hydrant: '消火栓',
  sprinkler: '喷淋系统'
}
const alarmTypeMap: Record<string, string> = {
  fire: '火警',
  fault: '故障',
  risk: '隐患',
  offline: '离线',
  false_alarm: '误报'
}
const statusMap: Record<string, string> = {
  pending: '待处理',
  handling: '处理中',
  handled: '已处置',
  closed: '已关闭'
}

// 日期格式化工具：兼容后端 LocalDateTime 的各种序列化格式（ISO字符串/数组/时间戳）
const formatDateTime = (value: any): string => {
  if (!value && value !== 0) return ''
  // 1. 时间戳数字（毫秒）
  if (typeof value === 'number') {
    const d = new Date(value)
    if (isNaN(d.getTime())) return ''
    return d.getFullYear() + '-' +
      String(d.getMonth() + 1).padStart(2, '0') + '-' +
      String(d.getDate()).padStart(2, '0') + ' ' +
      String(d.getHours()).padStart(2, '0') + ':' +
      String(d.getMinutes()).padStart(2, '0') + ':' +
      String(d.getSeconds()).padStart(2, '0')
  }
  // 2. 数组格式 [2025, 1, 15, 14, 30, 0]
  if (Array.isArray(value) && value.length >= 3) {
    return String(value[0]) + '-' +
      String(value[1]).padStart(2, '0') + '-' +
      String(value[2]).padStart(2, '0') + ' ' +
      String(value[3] || 0).padStart(2, '0') + ':' +
      String(value[4] || 0).padStart(2, '0') + ':' +
      String(value[5] || 0).padStart(2, '0')
  }
  // 3. 字符串 "2025-01-15T14:30:00" 或 "2025-01-15 14:30:00"
  const s = String(value)
  if (s.includes('T')) return s.replace('T', ' ')
  return s
}

const formatDateOnly = (value: any): string => {
  const dt = formatDateTime(value)
  return dt ? dt.substring(0, 10) : ''
}

const getDeviceTypeText = (type: string) => deviceTypeMap[type] || type
const getAlarmTypeText = (type: string) => alarmTypeMap[type] || type
const getStatusText = (status: string) => statusMap[status] || status

const getAlarmTypeTag = (type: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = { fire: 'danger', fault: 'warning', risk: 'info', offline: 'info', false_alarm: 'info' }
  return map[type] || 'info'
}
const getStatusTag = (status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = { pending: 'danger', handling: 'warning', handled: 'success', closed: 'info' }
  return map[status] || 'info'
}
const getResponseTimeClass = (time: number) => {
  if (!time) return ''
  if (time <= 60) return 'response-fast'
  if (time <= 180) return 'response-normal'
  return 'response-slow'
}

// 加载单位列表
const loadPropertyList = async () => {
  try {
    const res = await getPropertyList(1)
    propertyList.value = res || []
  } catch {
    propertyList.value = [
      { id: 1, name: '安为科技示范单位' },
      { id: 2, name: '天河区商业广场' },
      { id: 3, name: '海珠区工业园' },
      { id: 4, name: '番禺区物流仓库' },
      { id: 5, name: '白云区住宅小区' }
    ]
  }
}

// 加载设备类型选项（从设备管理的设备分类中获取）
const loadDeviceTypeOptions = async () => {
  try {
    const res = await getDeviceCategory()
    if (res && Array.isArray(res)) {
      deviceTypeOptions.value = res.map((item: any) => ({
        label: item.categoryName || item.name || item.productName,
        value: item.categoryCode || item.productKey || item.id
      }))
    }
  } catch {
    // 后端接口不可用时，设备类型下拉框留空
    deviceTypeOptions.value = []
  }
}

// 将后端AlertRecord映射为前端展示字段
const mapAlertRecordToDisplay = (item: any) => {
  // 使用alertTime（毫秒时间戳）转换为日期格式
  const alarmTime = formatDateTime(item.alertTime)

  // 从告警详情中解析设备名称和位置
  const details = item.details || ''
  let deviceName = ''
  let location = ''
  let propertyName = ''
  let deviceType = ''
  let deviceTypeName = ''
  let alarmType = ''
  let status = 'pending'
  let handler = ''
  let handleTime = ''
  let responseTime: number | null = null
  let remark = ''

  // 尝试从details JSON解析扩展字段
  try {
    const extra = typeof details === 'string' && details.startsWith('{') ? JSON.parse(details) : {}
    deviceName = extra.deviceName || item.name || ''
    location = extra.location || ''
    propertyName = extra.propertyName || ''
    deviceType = extra.deviceType || ''
    deviceTypeName = extra.deviceTypeName || ''
    alarmType = extra.alarmType || ''
    status = extra.status || (item.readFlg ? 'handled' : 'pending')
    handler = extra.handler || ''
    handleTime = formatDateOnly(extra.handleTime)
    responseTime = extra.responseTime || null
    remark = extra.remark || ''
  } catch {
    // details不是JSON，使用基本信息
    deviceName = item.name || ''
  }

  // 类型判定（双读）：alarmKind 非空=新语义；为空=旧语义回退（level 旧编码=类型）
  if (!alarmType) {
    const typeMap: Record<string, string> = { '1': 'fire', '2': 'fault', '3': 'risk', '4': 'offline', '5': 'false_alarm', '6': 'info' }
    if (item.alarmKind !== null && item.alarmKind !== undefined) {
      alarmType = typeMap[String(item.alarmKind)] || ''
    } else if (item.level) {
      alarmType = typeMap[String(item.level)] || ''
    }
  }

  return {
    id: item.id,
    alarmTime,
    propertyName,
    deviceName,
    deviceType,
    deviceTypeName,
    alarmType,
    location,
    status,
    handler,
    handleTime,
    responseTime,
    remark,
    name: item.name || '',
    level: item.level || '',
    details: item.details || '',
    readFlg: item.readFlg || false
  }
}

// 加载表格数据 - 从告警中心获取真实报警记录
const loadTableData = async () => {
  console.log('【报警记录搜索】loadTableData 开始执行，当前queryParams:', JSON.stringify(queryParams))
  loading.value = true
  try {
    // 优先使用火警工单接口（已包含租户名称和设备类型等丰富信息）
    let orderRecords: any[] = []
    let orderTotal = 0
    let primaryApiSuccess = false
    try {
      const orderParams: any = {
        pageNo: queryParams.pageNo,
        pageSize: queryParams.pageSize,
        alarmType: queryParams.alarmType || undefined,
        orderStatus: queryParams.status || undefined,
        propertyId: queryParams.propertyId,
        deviceType: queryParams.deviceType || undefined,
      }
      if (queryParams.timeRange && queryParams.timeRange.length === 2) {
        orderParams.alarmTimeBegin = queryParams.timeRange[0]
        orderParams.alarmTimeEnd = queryParams.timeRange[1]
      }
      console.log('【报警记录搜索】接口: /eiot/firewatch/alarm-order/page', JSON.stringify(orderParams))
      const orderRes = await getAlarmOrderPage(orderParams)
      primaryApiSuccess = true
      console.log('【报警记录搜索】接口返回:', JSON.stringify(orderRes))
      if (orderRes && orderRes.list && orderRes.list.length > 0) {
        orderRecords = orderRes.list.map((item: any) => ({
          id: item.id,
          alarmTime: formatDateTime(item.alarmTime),
          propertyName: item.tenantName || item.propertyName || '',
          tenantName: item.tenantName || '',
          deviceName: item.deviceName || '',
          deviceType: item.deviceType || '',
          deviceTypeName: item.deviceTypeName || '',
          alarmType: item.alarmType || '',
          location: item.alarmLocation || '',
          status: item.orderStatus || 'pending',
          handler: item.handler || '',
          handleTime: formatDateOnly(item.handleTime),
          responseTime: item.responseDuration || null,
          remark: item.handleRemark || ''
        }))
        orderTotal = orderRes.total || 0
      }
    } catch (e) {
      console.warn('【报警记录搜索】火警工单接口失败', e)
    }

    // 判断当前是否有有效的筛选条件
    const hasActiveFilters = !!(queryParams.alarmType || queryParams.status || queryParams.propertyId || queryParams.deviceType || (queryParams.timeRange && queryParams.timeRange.length === 2))

    if (orderRecords.length > 0) {
      // 主接口有数据，直接使用
      tableData.value = orderRecords
      total.value = orderTotal
      console.log('【报警记录搜索】使用火警工单数据，共', orderTotal, '条，当前表格行数:', tableData.value.length)
    } else if (primaryApiSuccess && hasActiveFilters) {
      // 主接口成功但返回空 + 有筛选条件 → 说明筛选条件下确实无数据，不降级
      tableData.value = []
      total.value = 0
      console.log('【报警记录搜索】筛选条件下无匹配数据，不降级到备用API，hasActiveFilters=', hasActiveFilters, 'primaryApiSuccess=', primaryApiSuccess)
    } else if (!primaryApiSuccess && hasActiveFilters) {
      // 主接口失败 + 有筛选条件 → 不降级到备用API(备用API不支持精确的处置状态筛选)
      tableData.value = []
      total.value = 0
      console.warn('【报警记录搜索】主接口失败且有筛选条件，不降级到备用API(备用API不支持精确筛选)，显示空数据')
    } else {
      // 主接口失败(无筛选条件) 或 主接口成功但返回空(无筛选条件) → 尝试告警中心原始接口作为兜底
      console.log('【报警记录搜索】主接口无数据，尝试备用API... hasActiveFilters=', hasActiveFilters, 'primaryApiSuccess=', primaryApiSuccess)
      let alertRecords: any[] = []
      let alertTotal = 0
      try {
        const alertParams: any = {
          pageNum: queryParams.pageNo,
          pageSize: queryParams.pageSize,
        }
        if (queryParams.alarmType) {
          alertParams.level = queryParams.alarmType === 'fire' ? '1' : queryParams.alarmType === 'fault' ? '2' : queryParams.alarmType === 'risk' ? '3' : queryParams.alarmType === 'offline' ? '4' : queryParams.alarmType === 'false_alarm' ? '5' : undefined
        }
        if (queryParams.status) {
          alertParams.readFlg = queryParams.status === 'pending' ? false : true
        }
        console.log('【报警记录搜索-备用API】/alert/selectAlertRecordPage 参数:', JSON.stringify(alertParams))
        const alertRes = await getMsgList(alertParams)
        console.log('【报警记录搜索-备用API】返回:', JSON.stringify(alertRes))
        if (alertRes && alertRes.list) {
          alertRecords = alertRes.list.map(mapAlertRecordToDisplay)
          alertTotal = alertRes.total || 0
        }
      } catch (e) {
        console.warn('告警中心记录获取失败', e)
      }

      if (alertRecords.length > 0) {
        tableData.value = alertRecords
        total.value = alertTotal
      } else {
        tableData.value = []
        total.value = 0
      }
    }

    // 统计卡片由 loadStatistics() 从后端API获取，此处仅刷新维度图表
    updateDimensionFromTableData()
    console.log('【报警记录搜索】loadTableData 执行完毕，最终 tableData 行数:', tableData.value.length, 'total:', total.value)
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 从后端统计API加载聚合数据（API不可用时回退到表格数据计算）
const loadStatistics = async () => {
  try {
    const res = await getAlarmRecordStatistics() as any
    if (res) {
      statistics.totalAlarms = res.totalAlarms || 0
      statistics.fireAlarms = res.fireAlarms || 0
      statistics.handledAlarms = res.handledAlarms || 0
      statistics.handleRate = res.handleRate || 0
      statistics.avgResponseTime = res.avgResponseTime || 0

      // 用后端返回的报警类型分布更新饼图
      if (res.alarmTypeDistribution?.length && typeChart) {
        const pieData = res.alarmTypeDistribution.map((item: any) => ({
          value: item.count,
          name: item.typeName || getAlarmTypeText(item.alarmType),
          itemStyle: { color: getAlarmTypeColor(item.alarmType) }
        }))
        typeChart.setOption({
          title: { text: '报警类型分布', left: 'center', textStyle: { fontSize: 14 } },
          series: [{ data: pieData }]
        })
      }
      return // 后端API成功，直接返回
    }
  } catch (e) {
    console.warn('后端统计API不可用，回退到表格数据计算', e)
  }

  // 回退：从表格数据计算统计卡片
  fallbackStatistics()
}

// 回退方案：从分页API的total + 当前页数据计算统计卡片和类型分布
const fallbackStatistics = () => {
  const allData = tableData.value
  if (!allData.length) return

  // 统计卡片：totalAlarms 使用分页API返回的真实总数，其余从当前页估算
  statistics.totalAlarms = total.value || allData.length
  // 火警统计只认类型（alarmType 映射自 alarm_kind / 旧编码）；新语义下 level='1' 表示"一级"而非"火警"，不再参与判定
  const fireRatio = allData.filter(d => d.alarmType === 'fire').length / Math.max(1, allData.length)
  const handledRatio = allData.filter(d => d.status !== 'pending').length / Math.max(1, allData.length)
  statistics.fireAlarms = Math.round(statistics.totalAlarms * fireRatio)
  statistics.handledAlarms = Math.round(statistics.totalAlarms * handledRatio)
  statistics.handleRate = statistics.totalAlarms > 0
    ? Math.round((statistics.handledAlarms / statistics.totalAlarms) * 100) : 0
  const responseTimes = allData.filter((d: any) => d.responseTime).map((d: any) => d.responseTime)
  statistics.avgResponseTime = responseTimes.length
    ? Math.round(responseTimes.reduce((a: number, b: number) => a + b, 0) / responseTimes.length) : 0

  // 报警类型分布饼图（按比例外推到总数）
  if (typeChart) {
    const typeCount: Record<string, number> = {}
    const levelToColor: Record<string, string> = {
      '1': THEME_COLOR, '2': '#9a6b1f', '3': '#3a3a36', '4': '#8b8b80', '5': '#2f6b4f',
      fire: THEME_COLOR, fault: '#9a6b1f', risk: '#3a3a36', offline: '#8b8b80', false_alarm: '#2f6b4f'
    }
    allData.forEach((d: any) => {
      const type = d.alarmType || d.level || 'unknown'
      typeCount[type] = (typeCount[type] || 0) + 1
    })
    const typeNameMap: Record<string, string> = {
      '1': '火警', '2': '故障', '3': '隐患', '4': '离线', '5': '误报',
      fire: '火警', fault: '故障', risk: '隐患', offline: '离线', false_alarm: '误报'
    }
    // 按比例外推到总数
    const ratio = statistics.totalAlarms / Math.max(1, allData.length)
    const pieData = Object.entries(typeCount).map(([key, value]) => ({
      value: Math.round(value * ratio),
      name: typeNameMap[key] || getAlarmTypeText(key) || key,
      itemStyle: { color: levelToColor[key] || '#c0c4cc' }
    }))
    if (pieData.length > 0) {
      typeChart.setOption({
        title: { text: '报警类型分布', left: 'center', textStyle: { fontSize: 14 } },
        series: [{ data: pieData }]
      })
    }
  }
}

// 报警类型对应的颜色
const getAlarmTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    fire: THEME_COLOR, fault: '#9a6b1f', risk: '#3a3a36', offline: '#8b8b80', false_alarm: '#2f6b4f'
  }
  return colorMap[type] || '#c0c4cc'
}

// 基于当前表格数据更新维度统计图（按单位/设备/区域无法在后端聚合，仍用前端数据）
const updateDimensionFromTableData = () => {
  // 统计卡片已由 loadStatistics() 从后端API获取，此处仅更新维度相关图表
  if (dimensionChart) {
    loadDimensionData()
  }
  if (handleRateChart) {
    loadHandleRateChart()
  }
}

// 从真实数据更新维度和处置率图表（类型分布饼图由loadStatistics处理）
const updateChartsFromRealData = () => {
  // 1. 更新维度统计图
  if (dimensionChart) {
    loadDimensionData()
  }

  // 2. 更新处置率图
  if (handleRateChart) {
    loadHandleRateChart()
  }
}

// 单独加载处置率图（供 updateChartsFromRealData 调用）
const loadHandleRateChart = () => {
  if (!handleRateChart) return

  const allData = tableData.value

  // 按单位分组统计
  const unitStats: Record<string, { total: number; handled: number; responseTimes: number[] }> = {}
  allData.forEach(d => {
    const name = d.propertyName || '未知单位'
    if (!unitStats[name]) {
      unitStats[name] = { total: 0, handled: 0, responseTimes: [] }
    }
    unitStats[name].total++
    if (d.status !== 'pending') {
      unitStats[name].handled++
    }
    if (d.responseTime) {
      unitStats[name].responseTimes.push(d.responseTime)
    }
  })

  const categories = Object.keys(unitStats)
  const rates = categories.map(name => {
    const stat = unitStats[name]
    return stat.total > 0 ? Math.round((stat.handled / stat.total) * 100) : 0
  })
  const avgTimes = categories.map(name => {
    const times = unitStats[name].responseTimes
    return times.length > 0 ? Math.round(times.reduce((a, b) => a + b, 0) / times.length) : 0
  })

  if (categories.length === 0) {
    handleRateChart.setOption({
      title: { text: '处置率与响应时长（暂无数据）', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['处置率(%)', '平均响应时长(s)'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '15%', containLabel: true },
      xAxis: { type: 'category', data: [] },
      yAxis: [
        { type: 'value', name: '处置率(%)', max: 100 },
        { type: 'value', name: '响应时长(s)', splitLine: { show: false } }
      ],
      series: [
        { name: '处置率(%)', type: 'bar', data: [], itemStyle: { color: '#2f6b4f' }, barWidth: '30%' },
        { name: '平均响应时长(s)', type: 'line', yAxisIndex: 1, data: [], smooth: true, itemStyle: { color: '#3a3a36' } }
      ]
    })
    return
  }

  handleRateChart.setOption({
    title: { text: '处置率与响应时长', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['处置率(%)', '平均响应时长(s)'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: categories },
    yAxis: [
      { type: 'value', name: '处置率(%)', max: 100 },
      { type: 'value', name: '响应时长(s)', splitLine: { show: false } }
    ],
    series: [
      { name: '处置率(%)', type: 'bar', data: rates, itemStyle: { color: '#2f6b4f', borderRadius: [2, 2, 0, 0] }, barWidth: '30%' },
      { name: '平均响应时长(s)', type: 'line', yAxisIndex: 1, data: avgTimes, smooth: true, itemStyle: { color: '#3a3a36' } }
    ]
  })
}

// 搜索
const handleSearch = () => {
  console.log('【报警记录搜索】点击搜索按钮，当前queryParams:', JSON.stringify(queryParams))
  queryParams.pageNo = 1
  loadTableData()
}

// 重置
const handleReset = () => {
  console.log('【报警记录搜索】点击重置按钮，重置前params:', JSON.stringify(queryParams))
  queryParams.timeRange = []
  queryParams.propertyId = undefined
  queryParams.deviceType = ''
  queryParams.alarmType = ''
  queryParams.status = ''
  queryParams.pageNo = 1
  loadTableData()
}

// 排序
const handleSortChange = ({ prop, order }: any) => {
  queryParams.sortField = prop || 'alarmTime'
  queryParams.sortOrder = order || 'descending'
  loadTableData()
}

// 详情
const handleDetail = (row: any) => {
  currentRecord.value = row
  detailVisible.value = true
}

// 导出 Excel
const handleExportExcel = async () => {
  try {
    ElMessage.info('正在导出Excel...')
    const params: any = {
      deviceType: queryParams.deviceType || undefined,
      alarmType: queryParams.alarmType || undefined,
      status: queryParams.status || undefined,
      propertyId: queryParams.propertyId
    }
    if (queryParams.timeRange && queryParams.timeRange.length === 2) {
      params.startTime = queryParams.timeRange[0]
      params.endTime = queryParams.timeRange[1]
    }

    try {
      const res = await request.download({
        url: '/eiot/firewatch/alarm-order/export',
        params
      })
      download.excel(res as any, `报警记录_${new Date().toLocaleDateString()}.xlsx`)
      ElMessage.success('导出成功')
    } catch {
      // 后端接口不可用时，使用前端生成CSV
      exportTableToCSV()
    }
  } catch {
    ElMessage.error('导出失败')
  }
}

// 前端生成CSV导出（备用方案）
const exportTableToCSV = () => {
  const headers = ['报警时间', '所属单位', '设备名称', '设备类型', '报警类型', '安装位置', '处置状态', '处置人', '处置时间', '响应时长(s)']
  const rows = tableData.value.map(row => [
    row.alarmTime,
    row.propertyName,
    row.deviceName,
    row.deviceTypeName || getDeviceTypeText(row.deviceType),
    getAlarmTypeText(row.alarmType),
    row.location,
    getStatusText(row.status),
    row.handler || '',
    row.handleTime || '',
    row.responseTime || ''
  ])

  const BOM = '\uFEFF'
  const csvContent = BOM + [headers.join(','), ...rows.map(row => row.map(cell => `"${cell}"`).join(','))].join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const href = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = href
  link.download = `报警记录_${new Date().toLocaleDateString()}.csv`
  link.click()
  URL.revokeObjectURL(href)
  ElMessage.success('已导出为CSV文件')
}

// 导出 PDF
const handleExportPdf = async () => {
  try {
    ElMessage.info('正在导出PDF...')
    const params: any = {
      deviceType: queryParams.deviceType || undefined,
      alarmType: queryParams.alarmType || undefined,
      status: queryParams.status || undefined,
      propertyId: queryParams.propertyId
    }
    if (queryParams.timeRange && queryParams.timeRange.length === 2) {
      params.startTime = queryParams.timeRange[0]
      params.endTime = queryParams.timeRange[1]
    }

    try {
      const res = await request.download({
        url: '/eiot/firewatch/alarm-order/export-pdf',
        params
      })
      const blob = new Blob([res as any], { type: 'application/pdf' })
      const href = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = href
      link.download = `报警记录_${new Date().toLocaleDateString()}.pdf`
      link.click()
      URL.revokeObjectURL(href)
      ElMessage.success('导出成功')
    } catch {
      // 后端接口不可用时，使用打印方式
      ElMessage.warning('PDF导出需后端支持，请联系管理员配置导出接口')
    }
  } catch {
    ElMessage.error('导出失败')
  }
}

// ========== 周期运行报告（月/季/年同一模板） ==========
const reportPeriodType = ref<'monthly' | 'quarterly' | 'yearly'>('monthly')
const reportPeriod = ref('')
const exporting = ref(false)

const periodTypeOptions = [
  { label: '月度', value: 'monthly' },
  { label: '季度', value: 'quarterly' },
  { label: '年度', value: 'yearly' },
]

// 默认上一完整期：上月 / 上季 / 上一年
const defaultPeriod = (type: string) => {
  const now = new Date()
  if (type === 'monthly') {
    const prev = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    return `${prev.getFullYear()}-${String(prev.getMonth() + 1).padStart(2, '0')}`
  }
  if (type === 'quarterly') {
    const q = Math.floor(now.getMonth() / 3) + 1
    const prevQ = q === 1 ? 4 : q - 1
    const year = q === 1 ? now.getFullYear() - 1 : now.getFullYear()
    return `${year}-Q${prevQ}`
  }
  return String(now.getFullYear() - 1)
}

// 近 8 个季度选项（含当前季度）
const quarterOptions = computed(() => {
  const now = new Date()
  let year = now.getFullYear()
  let q = Math.floor(now.getMonth() / 3) + 1
  const list: { label: string; value: string }[] = []
  for (let i = 0; i < 8; i++) {
    list.push({ label: `${year}年第${q}季度`, value: `${year}-Q${q}` })
    q--
    if (q === 0) {
      q = 4
      year--
    }
  }
  return list
})

reportPeriod.value = defaultPeriod('monthly')

const handlePeriodTypeChange = () => {
  reportPeriod.value = defaultPeriod(reportPeriodType.value)
}

const periodTypeName = computed(
  () => periodTypeOptions.find((o) => o.value === reportPeriodType.value)?.label || ''
)

const handleExportReport = async () => {
  if (exporting.value) return
  if (!reportPeriod.value) {
    ElMessage.warning('请先选择报告周期')
    return
  }
  exporting.value = true
  try {
    ElMessage.info(`正在生成${periodTypeName.value}报告...`)
    const res = await request.download({
      url: '/eiot/firewatch/monthly-report/export-pdf',
      params: { periodType: reportPeriodType.value, period: reportPeriod.value }
    })
    const blob = new Blob([res as any], { type: 'application/pdf' })
    const href = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = href
    link.download = `${periodTypeName.value}报告_${reportPeriod.value}.pdf`
    link.click()
    URL.revokeObjectURL(href)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

// 初始化趋势图
const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  loadTrendData()
}

// 加载趋势数据 - 从后端API获取真实数据
const loadTrendData = async () => {
  if (!trendChart) return

  // 根据周期计算天数
  const daysMap: Record<string, number> = {
    day: 30,   // 按日：最近30天
    week: 84,  // 按周：最近12周=84天
    month: 365,// 按月：最近12个月=365天
    year: 1825 // 按年：最近5年=1825天
  }
  const days = daysMap[trendPeriod.value] || 30

  try {
    const res = await getAlarmTrend(days)
    if (res && Array.isArray(res)) {
      // 后端返回 AlarmTrendVO 列表: {date, alarmCount, handleCount}
      const xData = res.map((item: any) => item.date)
      const fireData = res.map((item: any) => item.alarmCount)
      const handledData = res.map((item: any) => item.handleCount)

      trendChart.setOption({
        title: { text: '报警趋势', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        legend: { data: ['报警数量', '处置数量'], bottom: 0 },
        grid: { left: '3%', right: '4%', bottom: '15%', top: '15%', containLabel: true },
        xAxis: { type: 'category', data: xData, axisLabel: { rotate: trendPeriod.value === 'day' ? 45 : 0 } },
        yAxis: { type: 'value' },
        series: [
          {
            name: '报警数量', type: 'line', data: fireData, smooth: true,
            itemStyle: { color: THEME_COLOR },
            areaStyle: { color: THEME_COLOR, opacity: 0.1 }
          },
          {
            name: '处置数量', type: 'line', data: handledData, smooth: true,
            itemStyle: { color: '#2f6b4f' },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(47, 107, 79, 0.18)' }, { offset: 1, color: 'rgba(47, 107, 79, 0.02)' }]) }
          }
        ]
      })
    }
  } catch (e) {
    console.error('获取报警趋势数据失败', e)
    // 失败时显示空图表
    trendChart.setOption({
      title: { text: '报警趋势（暂无数据）', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['报警数量', '处置数量'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '15%', containLabel: true },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: [
        { name: '报警数量', type: 'line', data: [], smooth: true, itemStyle: { color: THEME_COLOR } },
        { name: '处置数量', type: 'line', data: [], smooth: true, itemStyle: { color: '#2f6b4f' } }
      ]
    })
  }
}

// 初始化类型分布图 - 基于真实数据
const initTypeChart = () => {
  if (!typeChartRef.value) return
  typeChart = echarts.init(typeChartRef.value)
  // 初始化为空，等待真实数据加载后由 updateChartsFromRealData 填充
  typeChart.setOption({
    title: { text: '报警类型分布（暂无数据）', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'middle' },
    series: [{
      name: '报警类型',
      type: 'pie',
      radius: ['35%', '60%'],
      center: ['60%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 2, borderColor: '#fffefb', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: []
    }]
  })
}

// 初始化维度统计图
const initDimensionChart = () => {
  if (!dimensionChartRef.value) return
  dimensionChart = echarts.init(dimensionChartRef.value)
  loadDimensionData()
}

// 加载维度数据 - 基于真实表格数据统计
const loadDimensionData = () => {
  if (!dimensionChart) return

  const allData = tableData.value
  let categories: string[] = []
  let values: number[] = []

  // 按比例外推到总数
  const ratio = total.value / Math.max(1, allData.length)

  if (statDimension.value === 'unit') {
    // 按单位统计
    const unitCount: Record<string, number> = {}
    allData.forEach(d => {
      const name = d.propertyName || '未知单位'
      unitCount[name] = (unitCount[name] || 0) + 1
    })
    const sorted = Object.entries(unitCount).sort((a, b) => b[1] - a[1])
    categories = sorted.map(item => item[0])
    values = sorted.map(item => Math.round(item[1] * ratio))
  } else if (statDimension.value === 'device') {
    // 按设备统计
    const deviceCount: Record<string, number> = {}
    allData.forEach(d => {
      const name = d.deviceTypeName || getDeviceTypeText(d.deviceType) || d.deviceName || '未知设备'
      deviceCount[name] = (deviceCount[name] || 0) + 1
    })
    const sorted = Object.entries(deviceCount).sort((a, b) => b[1] - a[1])
    categories = sorted.map(item => item[0])
    values = sorted.map(item => Math.round(item[1] * ratio))
  } else {
    // 按区域统计（从安装位置提取区域）
    const areaCount: Record<string, number> = {}
    allData.forEach(d => {
      const location = d.location || '未知区域'
      // 尝试从位置中提取区域信息（如 "天河区XX路" 提取 "天河区"）
      const areaMatch = location.match(/^[\u4e00-\u9fa5]+[市区县镇]/)
      const area = areaMatch ? areaMatch[0] : location
      areaCount[area] = (areaCount[area] || 0) + 1
    })
    const sorted = Object.entries(areaCount).sort((a, b) => b[1] - a[1])
    categories = sorted.map(item => item[0])
    values = sorted.map(item => Math.round(item[1] * ratio))
  }

  if (categories.length === 0) {
    dimensionChart.setOption({
      title: { text: `报警统计（暂无数据）`, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '5%', top: '15%', containLabel: true },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: [] }]
    })
    return
  }

  dimensionChart.setOption({
    title: { text: `报警统计（${statDimension.value === 'unit' ? '按单位' : statDimension.value === 'device' ? '按设备' : '按区域'}）`, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '5%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: categories },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: {
        color: THEME_COLOR,
        borderRadius: [2, 2, 0, 0]
      },
      barWidth: '40%'
    }]
  })
}

// 初始化处置率图 - 初始化为空，等待真实数据
const initHandleRateChart = () => {
  if (!handleRateChartRef.value) return
  handleRateChart = echarts.init(handleRateChartRef.value)
  // 先显示空图表，数据加载完成后由 updateChartsFromRealData -> loadHandleRateChart 更新
  handleRateChart.setOption({
    title: { text: '处置率与响应时长', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['处置率(%)', '平均响应时长(s)'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: [] },
    yAxis: [
      { type: 'value', name: '处置率(%)', max: 100 },
      { type: 'value', name: '响应时长(s)', splitLine: { show: false } }
    ],
    series: [
      { name: '处置率(%)', type: 'bar', data: [], itemStyle: { color: '#2f6b4f', borderRadius: [2, 2, 0, 0] }, barWidth: '30%' },
      { name: '平均响应时长(s)', type: 'line', yAxisIndex: 1, data: [], smooth: true, itemStyle: { color: '#3a3a36' } }
    ]
  })
}

// 窗口大小变化
const handleResize = () => {
  trendChart?.resize()
  typeChart?.resize()
  dimensionChart?.resize()
  handleRateChart?.resize()
}

onMounted(async () => {
  await loadPropertyList()
  loadDeviceTypeOptions()
  // 先加载表格数据（回退统计需要表格数据），再加载后端统计
  await loadTableData()
  await loadStatistics()
  nextTick(() => {
    initTrendChart()
    initTypeChart()
    initDimensionChart()
    initHandleRateChart()
    // 图表初始化后刷新数据
    loadStatistics()
    updateChartsFromRealData()
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  typeChart?.dispose()
  dimensionChart?.dispose()
  handleRateChart?.dispose()
})
</script>

<style lang="scss" scoped>
// 值班记录页：纸张底色 + 细线分隔，强调色统一跟随系统主题色
// 刻意不用渐变/彩色图标/悬浮阴影，避免仪表盘模板感
$ink: #1c1c19;
$ink-2: #57574f;
$muted: #8b8b80;
$line: #e1e0d7;
$paper: #f2f1ec;
$card: #fffefb;
$accent: var(--el-color-primary);

.alarm-record-container {
  min-height: 100%;
  padding: 0;
  color: $ink;
  font-variant-numeric: tabular-nums;
  background: $paper;

  :deep(.el-card) {
    background: $card;
    border: 1px solid $line;
    border-radius: 2px;
    box-shadow: none;

    .el-card__header {
      padding: 14px 18px;
      border-bottom: 1px solid $line;
    }

    .el-card__body {
      padding: 18px;
    }
  }

  :deep(.el-button) {
    border-radius: 2px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    border-radius: 2px;
    box-shadow: 0 0 0 1px $line inset;
  }

  :deep(.el-tag) {
    border-radius: 2px;
  }

  :deep(.el-radio-button__inner) {
    border-radius: 0 !important;
  }
}

// 页头：主题色竖线 + 标题 + 口径说明
.page-head {
  padding: 2px 0 14px;
}

.page-title {
  position: relative;
  padding-left: 12px;
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: $ink;

  &::before {
    position: absolute;
    top: 50%;
    left: 0;
    width: 3px;
    height: 18px;
    content: '';
    background: $accent;
    transform: translateY(-50%);
  }
}

.page-sub {
  margin: 6px 0 0 12px;
  font-size: 12px;
  color: $muted;
}

// 控制面板：筛选与指标同属一块，细线分隔
.control-panel {
  margin-bottom: 16px;
  background: $card;
  border: 1px solid $line;
  border-radius: 2px;
}

.filter-bar {
  padding: 16px 18px 6px;
  border-bottom: 1px solid $line;

  :deep(.el-form-item) {
    margin-bottom: 10px;
  }

  :deep(.el-form-item__label) {
    font-size: 12px;
    letter-spacing: 0.04em;
    color: $muted;
  }
}

.stat-strip {
  display: flex;
  flex-wrap: wrap;
}

.stat-cell {
  flex: 1;
  min-width: 140px;
  padding: 16px 18px;
  border-left: 1px solid $line;

  &:first-child {
    border-left: none;
  }

  .stat-label {
    font-size: 11px;
    letter-spacing: 0.14em;
    color: $muted;
  }

  .stat-value {
    margin-top: 8px;
    font-size: 26px;
    font-weight: 600;
    line-height: 1;
    color: $ink;
    letter-spacing: -0.01em;
  }

  .unit {
    margin-left: 2px;
    font-size: 12px;
    font-weight: 400;
    color: $muted;
  }
}

.stat-cell--lead .stat-value {
  font-size: 34px;
}

.stat-cell .stat-value--alert {
  color: $accent;
}

// 区块头：编号 + 标题 + 延伸细线
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.card-heading {
  display: flex;
  flex: 1;
  align-items: center;
  min-width: 0;
  gap: 10px;

  .section-no {
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
    font-size: 11px;
    letter-spacing: 0.08em;
    color: $muted;
  }

  .title {
    font-size: 14px;
    font-weight: 600;
    color: $ink;
    white-space: nowrap;
  }

  .section-rule {
    flex: 1;
    height: 1px;
    margin-left: 2px;
    background: $line;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-card {
  margin-bottom: 16px;
}

.chart-row {
  display: flex;
  gap: 20px;

  .chart-item {
    flex: 1;
    min-width: 0;
  }
}

// 报表工具带：淡主题色底 + 左侧主题色边，与数据卡区分
.report-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 18px;
  margin-bottom: 16px;
  background: var(--el-color-primary-light-9);
  border: 1px solid $line;
  border-left: 3px solid $accent;
  border-radius: 2px;

  .report-info {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 10px;
  }

  .report-title {
    font-size: 13px;
    font-weight: 600;
    color: $ink;
  }

  .report-tip {
    font-size: 12px;
    color: $muted;
  }

  .report-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  // 全页唯一实心主按钮，跟随主题色
  :deep(.el-button--primary) {
    --el-button-bg-color: #{$accent};
    --el-button-border-color: #{$accent};
    --el-button-hover-bg-color: var(--el-color-primary-dark-2);
    --el-button-hover-border-color: var(--el-color-primary-dark-2);
    --el-button-active-bg-color: var(--el-color-primary-dark-2);
    --el-button-active-border-color: var(--el-color-primary-dark-2);
  }
}

.table-card {
  :deep(.el-table) {
    --el-table-border-color: #{$line};
    --el-table-header-bg-color: #f7f5ee;
    --el-table-header-text-color: #{$ink-2};
    --el-table-text-color: #{$ink-2};

    font-size: 13px;

    th.el-table__cell {
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.08em;
      color: $muted;
    }
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

// 响应时长分级用低饱和色，避免整页彩色
.response-fast {
  color: #2f6b4f;
  font-weight: 600;
}

.response-normal {
  color: #9a6b1f;
  font-weight: 600;
}

.response-slow {
  color: $accent;
  font-weight: 600;
}
</style>
