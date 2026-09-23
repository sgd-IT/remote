<template>
  <ContentWrap>
    <!-- 排班统计概览 -->
    <el-row :gutter="16" class="mb-10px">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="handleStatFilter('scheduled')">
          <div class="stat-value" style="color: #909399">{{ statistics.scheduled }}</div>
          <div class="stat-label">已排班</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="handleStatFilter('on_duty')">
          <div class="stat-value" style="color: #67c23a">{{ statistics.on_duty }}</div>
          <div class="stat-label">值班中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="handleStatFilter('completed')">
          <div class="stat-value" style="color: #409eff">{{ statistics.completed }}</div>
          <div class="stat-label">已完成</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="handleStatFilter('absent')">
          <div class="stat-value" style="color: #f56c6c">{{ statistics.absent }}</div>
          <div class="stat-label">缺勤</div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="所属单位" prop="propertyId">
        <el-select v-model="queryParams.propertyId" placeholder="请选择单位" clearable style="width: 200px" @change="handleQueryPropertyChange">
          <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="排班日期" prop="scheduleDate">
        <el-date-picker
          v-model="queryParams.scheduleDate"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="班次类型" prop="shiftType">
        <el-select v-model="queryParams.shiftType" placeholder="请选择" clearable style="width: 120px">
          <el-option label="白班" value="day" />
          <el-option label="夜班" value="night" />
          <el-option label="全天班" value="wholeday" />
        </el-select>
      </el-form-item>
      <el-form-item label="值班人" prop="dutyUserName">
        <el-input v-model="queryParams.dutyUserName" placeholder="请输入值班人姓名" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 120px">
          <el-option label="已排班" value="scheduled" />
          <el-option label="值班中" value="on_duty" />
          <el-option label="已完成" value="completed" />
          <el-option label="缺勤" value="absent" />
          <el-option label="早退" value="early_leave" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 操作栏 + 表格 -->
  <ContentWrap>
    <el-row :gutter="10" class="mb-10px" align="middle">
      <el-col :span="1.5">
        <el-button type="primary" @click="handleCreate"><Icon icon="ep:plus" class="mr-5px" />新增排班</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" @click="handleAutoSchedule"><Icon icon="ep:calendar" class="mr-5px" />自动排班</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" @click="handleViewCalendar"><Icon icon="ep:calendar" color="#fff" class="mr-5px" />日历视图</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" @click="handleViewRecords"><Icon icon="ep:document" class="mr-5px" />值班记录</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain @click="handleExport"><Icon icon="ep:download" class="mr-5px" />导出</el-button>
      </el-col>
      <div style="flex: 1"></div>
      <el-col :span="1.5">
        <el-button type="primary" plain @click="handleImport"><Icon icon="ep:upload" class="mr-5px" />导入排班</el-button>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="所属单位" prop="propertyName" min-width="140" show-overflow-tooltip />
      <el-table-column label="排班日期" prop="scheduleDate" width="110" />
      <el-table-column label="班次" width="80">
        <template #default="{ row }">
          <el-tag :type="getShiftTypeTag(row.shiftType)" size="small">{{ row.shiftTypeName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="班次名称" prop="shiftName" width="90" />
      <el-table-column label="值班时间" width="110">
        <template #default="{ row }">
          {{ row.startTime }} - {{ row.endTime }}
        </template>
      </el-table-column>
      <el-table-column label="值班人" prop="dutyUserName" width="90" />
      <el-table-column label="联系电话" prop="dutyUserPhone" width="110" />
      <el-table-column label="岗位" prop="dutyUserPosition" width="90" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="getStatusTypeTag(row.status)" size="small">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="签到时间" prop="checkInTime" width="155">
        <template #default="{ row }">
          <span>{{ formatTimestamp(row.checkInTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="签退时间" prop="checkOutTime" width="155">
        <template #default="{ row }">
          <span>{{ formatTimestamp(row.checkOutTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleUpdate(row)">编辑</el-button>
          <el-button v-if="row.status === 'scheduled'" link type="success" @click="handleCheckIn(row)">签到</el-button>
          <el-button v-if="row.status === 'on_duty'" link type="warning" @click="handleCheckOut(row)">签退</el-button>
          <el-button link type="info" @click="handleChangeDuty(row)">换班</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
  </ContentWrap>

  <!-- 新增/编辑对话框 -->
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="所属单位" prop="propertyId">
        <el-select v-model="formData.propertyId" placeholder="请选择单位" style="width: 100%" @change="handleFormPropertyChange">
          <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="排班日期" prop="scheduleDate">
        <el-date-picker v-model="formData.scheduleDate" type="date" placeholder="请选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="班次类型" prop="shiftType">
        <el-radio-group v-model="formData.shiftType" @change="handleShiftTypeChange">
          <el-radio label="day">白班</el-radio>
          <el-radio label="night">夜班</el-radio>
          <el-radio label="wholeday">全天班</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="班次名称" prop="shiftName">
        <el-input v-model="formData.shiftName" placeholder="请输入班次名称" />
      </el-form-item>
      <el-row>
        <el-col :span="12">
          <el-form-item label="开始时间" prop="startTime">
            <el-time-select v-model="formData.startTime" placeholder="开始时间" :max-time="formData.endTime" start="00:00" end="23:59" step="00:30" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束时间" prop="endTime">
            <el-time-select v-model="formData.endTime" placeholder="结束时间" :min-time="formData.startTime" start="00:00" end="23:59" step="00:30" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="值班人" prop="dutyUserId">
        <el-select v-model="formData.dutyUserId" placeholder="请选择值班人" style="width: 100%" @change="handleUserChange">
          <el-option v-for="item in userList" :key="item.id" :label="item.nickname" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="联系电话" prop="dutyUserPhone">
        <el-input v-model="formData.dutyUserPhone" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="岗位" prop="dutyUserPosition">
        <el-input v-model="formData.dutyUserPosition" placeholder="请输入岗位" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>

  <!-- 自动排班对话框 -->
  <el-dialog v-model="autoScheduleVisible" title="自动排班" width="500px" append-to-body>
    <el-form ref="autoFormRef" :model="autoScheduleForm" :rules="autoScheduleRules" label-width="100px">
      <el-form-item label="所属单位" prop="propertyId">
        <el-select v-model="autoScheduleForm.propertyId" placeholder="请选择单位" style="width: 100%" @change="handleAutoPropertyChange">
          <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="排班月份" prop="month">
        <el-date-picker v-model="autoScheduleForm.month" type="month" placeholder="请选择月份" value-format="YYYY-MM" style="width: 100%" />
      </el-form-item>
      <el-form-item label="值班人员" prop="dutyUsers">
        <el-select v-model="autoScheduleForm.dutyUsers" multiple placeholder="请选择值班人员" style="width: 100%">
          <el-option v-for="item in userList" :key="item.id" :label="item.nickname" :value="item.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="autoScheduleVisible = false">取消</el-button>
      <el-button type="primary" @click="submitAutoSchedule">生成排班</el-button>
    </template>
  </el-dialog>

  <!-- 换班对话框 -->
  <el-dialog v-model="changeDutyVisible" title="换班" width="420px" append-to-body>
    <el-form ref="changeDutyFormRef" :model="changeDutyForm" label-width="100px">
      <el-form-item label="原值班人">
        <el-input :value="changeDutyForm.oldUserName" disabled />
      </el-form-item>
      <el-form-item label="新值班人" prop="newUserId">
        <el-select v-model="changeDutyForm.newUserId" placeholder="请选择新值班人" style="width: 100%">
          <el-option v-for="item in userList" :key="item.id" :label="item.nickname" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="换班原因">
        <el-input v-model="changeDutyForm.reason" type="textarea" placeholder="请输入换班原因" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="changeDutyVisible = false">取消</el-button>
      <el-button type="primary" @click="submitChangeDuty">确定</el-button>
    </template>
  </el-dialog>

  <!-- 导入排班对话框 -->
  <el-dialog v-model="importVisible" title="导入排班" width="480px" append-to-body>
    <el-form label-width="80px" class="mb-16px">
      <el-form-item label="所属单位" required>
        <el-select v-model="importPropertyId" placeholder="请选择单位" style="width: 100%">
          <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <el-upload
      v-model:file-list="importFileList"
      :auto-upload="false"
      :limit="1"
      accept=".xlsx,.xls"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <span>仅允许导入 xls、xlsx 格式文件。</span>
          <el-link :underline="false" style="font-size: 12px; margin-left: 8px" type="primary" @click="handleDownloadTemplate">
            下载模板
          </el-link>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <el-button type="primary" :loading="importLoading" @click="submitImport">确 定</el-button>
      <el-button @click="importVisible = false">取 消</el-button>
    </template>
  </el-dialog>

  <!-- 日历视图对话框 -->
  <el-dialog v-model="calendarVisible" title="排班日历" width="960px" append-to-body top="20px" class="calendar-dialog">
    <div class="calendar-toolbar">
      <el-select v-model="calendarPropertyId" placeholder="选择单位" clearable style="width: 200px" @change="loadCalendarData">
        <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
      </el-select>
      <div class="calendar-nav">
        <el-button @click="changeMonth(-1)"><Icon icon="ep:arrow-left" /></el-button>
        <span class="calendar-month-label">{{ calendarYear }}年{{ calendarMonthIndex }}月</span>
        <el-button @click="changeMonth(1)"><Icon icon="ep:arrow-right" /></el-button>
        <el-button class="ml-10px" @click="goToToday">今天</el-button>
      </div>
      <div class="calendar-legend">
        <el-tag size="small" type="success"><Icon icon="ep:sunny" color="#fff" style="margin-right: 2px; vertical-align: middle;" />白班</el-tag>
        <el-tag size="small" type="info"><Icon icon="ep:moon" color="#fff" style="margin-right: 2px; vertical-align: middle;" />夜班</el-tag>
        <el-tag size="small" type="warning"><Icon icon="ep:clock" color="#fff" style="margin-right: 2px; vertical-align: middle;" />全天班</el-tag>
      </div>
    </div>
    <div class="calendar-summary" v-if="calendarSchedules.length">
      <span class="summary-item">共 <b>{{ calendarSchedules.length }}</b> 个排班</span>
      <span class="summary-divider">|</span>
      <span class="summary-item">白班 <b>{{ countByShiftType('day') }}</b></span>
      <span class="summary-divider">|</span>
      <span class="summary-item">夜班 <b>{{ countByShiftType('night') }}</b></span>
      <span class="summary-divider">|</span>
      <span class="summary-item">全天班 <b>{{ countByShiftType('wholeday') }}</b></span>
    </div>
    <el-calendar v-model="calendarMonth">
      <template #date-cell="{ data }">
        <div class="calendar-cell" @click="handleDayClick(data.date)">
          <div class="calendar-day" :class="{ 'is-today': isToday(data.date) }">{{ data.date.getDate() }}</div>
          <div v-for="item in getCalendarSchedules(data.date)" :key="item.id" class="calendar-schedule-item">
            <el-tag
              :type="getShiftTypeTag(item.shiftType)"
              size="small"
              class="calendar-tag"
            >
              <Icon :icon="getShiftIcon(item.shiftType)" color="#fff" style="margin-right: 2px; vertical-align: middle;" />
              {{ item.dutyUserName }}
            </el-tag>
          </div>
        </div>
      </template>
    </el-calendar>
  </el-dialog>

  <!-- 日历-日期详情对话框 -->
  <el-dialog v-model="dayDetailVisible" :title="selectedDayTitle" width="600px" append-to-body>
    <template v-if="selectedDaySchedules.length">
      <el-timeline>
        <el-timeline-item
          v-for="item in selectedDaySchedules"
          :key="item.id"
          :timestamp="item.startTime + ' - ' + item.endTime"
          placement="top"
          :color="getShiftTimelineColor(item.shiftType)"
        >
          <div class="timeline-card">
            <div class="tl-header">
              <el-tag :type="getShiftTypeTag(item.shiftType)" size="small">{{ item.shiftTypeName }}</el-tag>
              <el-tag :type="getStatusTypeTag(item.status)" size="small" class="ml-5px">{{ item.statusName }}</el-tag>
            </div>
            <div class="tl-body">
              <div><label>值班人：</label>{{ item.dutyUserName }}</div>
              <div><label>岗位：</label>{{ item.dutyUserPosition || '-' }}</div>
              <div><label>电话：</label>{{ item.dutyUserPhone || '-' }}</div>
              <div v-if="item.checkInTime"><label>签到：</label>{{ formatTimestamp(item.checkInTime) }}</div>
              <div v-if="item.checkOutTime"><label>签退：</label>{{ formatTimestamp(item.checkOutTime) }}</div>
              <div v-if="item.remark"><label>备注：</label>{{ item.remark }}</div>
            </div>
            <div class="tl-actions">
              <el-button v-if="item.status === 'scheduled'" type="success" size="small" @click="handleCalendarCheckIn(item)">签到</el-button>
              <el-button v-if="item.status === 'on_duty'" type="warning" size="small" @click="handleCalendarCheckOut(item)">签退</el-button>
              <el-button type="primary" size="small" @click="handleCalendarEdit(item)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleCalendarDelete(item)">删除</el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </template>
    <el-empty v-else description="该日期暂无排班" />
    <template #footer>
      <el-button type="primary" @click="handleCreateFromCalendar">新增排班</el-button>
      <el-button @click="dayDetailVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  getDutySchedulePage,
  createDutySchedule,
  updateDutySchedule,
  deleteDutySchedule,
  checkIn,
  checkOut,
  changeDuty as changeDutyApi,
  autoSchedule as autoScheduleApi,
  getDutyScheduleStatistics,
  getDutyScheduleByDateRange,
  downloadDutyScheduleTemplate,
  importDutySchedule
} from '@/api/eiot/firewatch/firewatch.api'
import { getPropertyList } from '@/api/eiot/firewatch/firewatch.api'
import { getSimpleUserList } from '@/api/system/user/index'
import { getTenantId } from '@/utils/auth'
import download from '@/utils/download'

defineOptions({ name: 'FireWatchDuty' })

const router = useRouter()

const loading = ref(false)
const total = ref(0)
const tableData = ref<any[]>([])
const propertyList = ref<any[]>([])
const userList = ref<any[]>([])

// 排班统计
const statistics = reactive({
  scheduled: 0,
  on_duty: 0,
  completed: 0,
  absent: 0
})


// 加载统计
const loadStatistics = async () => {
  try {
    const startDate = queryParams.scheduleDate?.[0]
    const endDate = queryParams.scheduleDate?.[1]
    const res = await getDutyScheduleStatistics(queryParams.propertyId, startDate, endDate)
    if (res?.statusStatistics) {
      Object.assign(statistics, res.statusStatistics)
    }
  } catch (e) {
    // 忽略统计加载失败
  }
}

// 点击统计过滤
const handleStatFilter = (status: string) => {
  queryParams.status = status === queryParams.status ? undefined : status
  handleQuery()
}

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  propertyId: undefined as number | undefined,
  scheduleDate: undefined as string[] | undefined,
  shiftType: undefined as string | undefined,
  dutyUserName: undefined as string | undefined,
  status: undefined as string | undefined
})

const queryFormRef = ref<FormInstance>()

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const formData = ref<any>({
  id: undefined,
  propertyId: undefined,
  scheduleDate: undefined,
  shiftType: 'day',
  shiftName: '白班',
  startTime: '08:00',
  endTime: '18:00',
  dutyUserId: undefined,
  dutyUserName: '',
  dutyUserPhone: '',
  dutyUserPosition: '',
  remark: ''
})

const formRules = reactive<FormRules>({
  propertyId: [{ required: true, message: '请选择所属单位', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择排班日期', trigger: 'change' }],
  shiftType: [{ required: true, message: '请选择班次类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  dutyUserId: [{ required: true, message: '请选择值班人', trigger: 'change' }]
})

// 值班人选择时自动填充电话
const handleUserChange = (userId: number) => {
  const user = userList.value.find((u: any) => u.id === userId)
  if (user) {
    formData.value.dutyUserName = user.nickname
    formData.value.dutyUserPhone = user.mobile || ''
  }
}

// 自动排班
const autoScheduleVisible = ref(false)
const autoFormRef = ref<FormInstance>()
const autoScheduleForm = ref({
  propertyId: undefined,
  month: undefined,
  dutyUsers: [] as number[]
})

const autoScheduleRules = reactive<FormRules>({
  propertyId: [{ required: true, message: '请选择所属单位', trigger: 'change' }],
  month: [{ required: true, message: '请选择排班月份', trigger: 'change' }],
  dutyUsers: [{ required: true, message: '请选择值班人员', trigger: 'change' }]
})

// 换班
const changeDutyVisible = ref(false)
const changeDutyFormRef = ref<FormInstance>()
const changeDutyForm = ref({
  id: undefined as number | undefined,
  oldUserName: '',
  newUserId: undefined as number | undefined,
  reason: ''
})

// 日历视图
const calendarVisible = ref(false)
const calendarMonth = ref(new Date())
const calendarPropertyId = ref<number | undefined>(undefined)
const calendarSchedules = ref<any[]>([])

// 日历-日期详情
const dayDetailVisible = ref(false)
const selectedDay = ref<Date | null>(null)
const selectedDaySchedules = ref<any[]>([])

const calendarYear = computed(() => calendarMonth.value.getFullYear())
const calendarMonthIndex = computed(() => calendarMonth.value.getMonth() + 1)

const selectedDayTitle = computed(() => {
  if (!selectedDay.value) return '日期详情'
  const d = selectedDay.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 排班详情`
})

const isToday = (date: Date) => {
  const now = new Date()
  return date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
}

const changeMonth = (offset: number) => {
  const newDate = new Date(calendarMonth.value)
  newDate.setMonth(newDate.getMonth() + offset)
  calendarMonth.value = newDate
}

const goToToday = () => {
  calendarMonth.value = new Date()
  loadCalendarData()
}

const countByShiftType = (type: string) => {
  return calendarSchedules.value.filter((s: any) => s.shiftType === type).length
}

const getShiftTimelineColor = (type: string) => {
  const map: Record<string, string> = {
    day: '#67c23a',
    night: '#909399',
    wholeday: '#e6a23c'
  }
  return map[type] || '#909399'
}

const getShiftIcon = (type: string) => {
  const map: Record<string, string> = {
    day: 'ep:sunny',
    night: 'ep:moon',
    wholeday: 'ep:clock'
  }
  return map[type] || 'ep:calendar'
}

const formatTimestamp = (ts: number) => {
  if (!ts) return '-'
  const d = new Date(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const getCalendarSchedules = (date: Date) => {
  const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return calendarSchedules.value.filter((s: any) => {
    const sd = s.scheduleDate
    // 兼容字符串格式 "2026-05-06"
    if (typeof sd === 'string') return sd === dateStr
    // 兼容数组格式 [2026, 5, 6]
    if (Array.isArray(sd)) {
      const [y, m, d] = sd
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}` === dateStr
    }
    return false
  })
}

const handleDayClick = (date: Date) => {
  selectedDay.value = date
  const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  // 使用与 getCalendarSchedules 相同的日期匹配逻辑
  selectedDaySchedules.value = calendarSchedules.value.filter((s: any) => {
    const sd = s.scheduleDate
    if (typeof sd === 'string') return sd === dateStr
    if (Array.isArray(sd)) {
      const [y, m, d] = sd
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}` === dateStr
    }
    return false
  })
  console.log('[handleDayClick] 日期:', dateStr, ', 总排班数:', calendarSchedules.value.length, ', 当日匹配数:', selectedDaySchedules.value.length)
  if (calendarSchedules.value.length > 0) {
    console.log('[handleDayClick] 前3条排班日期示例:', calendarSchedules.value.slice(0, 3).map((s: any) => s.scheduleDate))
  }
  dayDetailVisible.value = true
}

const handleCalendarCheckIn = async (item: any) => {
  await ElMessageBox.confirm(`确认【${item.dutyUserName}】签到？`, '签到确认', { type: 'info' })
  await checkIn(item.id, 'manual')
  ElMessage.success(`${item.dutyUserName} 签到成功`)
  loadCalendarData()
  getList()
  loadStatistics()
  // 刷新当前详情
  if (selectedDay.value) handleDayClick(selectedDay.value)
}

const handleCalendarCheckOut = async (item: any) => {
  await ElMessageBox.confirm(`确认【${item.dutyUserName}】签退？`, '签退确认', { type: 'info' })
  await checkOut(item.id)
  ElMessage.success(`${item.dutyUserName} 签退成功`)
  loadCalendarData()
  getList()
  loadStatistics()
  // 刷新当前详情
  if (selectedDay.value) handleDayClick(selectedDay.value)
}

const handleCalendarEdit = (item: any) => {
  dayDetailVisible.value = false
  handleUpdate(item)
}

const handleCalendarDelete = async (item: any) => {
  dayDetailVisible.value = false
  await ElMessageBox.confirm('确认删除该排班记录？', '提示', { type: 'warning' })
  await deleteDutySchedule(item.id)
  ElMessage.success('删除成功')
  getList()
  loadStatistics()
  loadCalendarData()
}

const handleCreateFromCalendar = () => {
  dayDetailVisible.value = false
  if (selectedDay.value) {
    const d = selectedDay.value
    const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    handleCreate(dateStr)
  } else {
    handleCreate()
  }
}

const loadCalendarData = async () => {
  const year = calendarMonth.value.getFullYear()
  const month = calendarMonth.value.getMonth() + 1
  const lastDay = new Date(year, month, 0).getDate()
  const startDate = `${year}-${String(month).padStart(2, '0')}-01`
  const endDate = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  console.log('[loadCalendarData] 请求参数:', { startDate, endDate, propertyId: calendarPropertyId.value, tenantId: getTenantId() })
  try {
    const res = await getDutyScheduleByDateRange(startDate, endDate, calendarPropertyId.value)
    console.log('[loadCalendarData] 返回数据量:', res?.length, ', 前3条示例:', res?.slice(0, 3))
    calendarSchedules.value = res || []
  } catch (e) {
    console.error('[loadCalendarData] 加载失败:', e)
    calendarSchedules.value = []
  }
}

// 获取列表
const getList = async () => {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (queryParams.scheduleDate && queryParams.scheduleDate.length === 2) {
      params.scheduleDateStart = queryParams.scheduleDate[0]
      params.scheduleDateEnd = queryParams.scheduleDate[1]
      delete params.scheduleDate
    }
    const res = await getDutySchedulePage(params)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
  loadStatistics()
}

// 重置
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

// 新增
const handleCreate = (prefillDate?: string) => {
  resetForm()
  dialogTitle.value = '新增排班'
  if (prefillDate) {
    formData.value.scheduleDate = prefillDate
  }
  dialogVisible.value = true
}

// 编辑
const handleUpdate = (row: any) => {
  resetForm()
  dialogTitle.value = '编辑排班'
  // 使用 Object.assign 保持 formData 对象引用不变，避免 Element Plus 表单内部状态丢失
  Object.assign(formData.value, row, { dutyUserId: row.dutyUserId })
  // 按当前排班所属单位的租户加载值班人员列表
  loadUserList(findPropertyTenantId(formData.value.propertyId))
  // 兼容 scheduleDate 可能的数组格式 [2026, 5, 6] -> "2026-05-06"
  if (formData.value.scheduleDate && Array.isArray(formData.value.scheduleDate)) {
    const [y, m, d] = formData.value.scheduleDate
    formData.value.scheduleDate = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该排班记录？', '提示', { type: 'warning' })
  await deleteDutySchedule(row.id)
  ElMessage.success('删除成功')
  getList()
  loadStatistics()
}

// 签到
const handleCheckIn = async (row: any) => {
  await ElMessageBox.confirm(`确认【${row.dutyUserName}】签到？`, '签到确认', { type: 'info' })
  await checkIn(row.id, 'manual')
  ElMessage.success(`${row.dutyUserName} 签到成功`)
  getList()
  loadStatistics()
}

// 签退
const handleCheckOut = async (row: any) => {
  await ElMessageBox.confirm(`确认【${row.dutyUserName}】签退？`, '签退确认', { type: 'info' })
  await checkOut(row.id)
  ElMessage.success(`${row.dutyUserName} 签退成功`)
  getList()
  loadStatistics()
}

// 换班
const handleChangeDuty = (row: any) => {
  changeDutyForm.value = {
    id: row.id,
    oldUserName: row.dutyUserName,
    newUserId: undefined,
    reason: ''
  }
  // 按原排班所属单位的租户加载值班人员列表
  loadUserList(findPropertyTenantId(row.propertyId))
  changeDutyVisible.value = true
}

// 提交换班
const submitChangeDuty = async () => {
  if (!changeDutyForm.value.newUserId) {
    ElMessage.warning('请选择新值班人')
    return
  }
  if (!changeDutyForm.value.reason) {
    ElMessage.warning('请输入换班原因')
    return
  }
  await changeDutyApi(changeDutyForm.value.id!, changeDutyForm.value.newUserId, changeDutyForm.value.reason)
  ElMessage.success('换班成功')
  changeDutyVisible.value = false
  getList()
}

// 自动排班
const handleAutoSchedule = () => {
  autoScheduleForm.value = {
    propertyId: undefined,
    month: undefined,
    dutyUsers: []
  }
  autoScheduleVisible.value = true
}

// 提交自动排班
const submitAutoSchedule = async () => {
  if (!autoScheduleForm.value.propertyId || !autoScheduleForm.value.month || !autoScheduleForm.value.dutyUsers.length) {
    ElMessage.warning('请填写完整信息')
    return
  }
  const [year, month] = (autoScheduleForm.value.month as string).split('-').map(Number)
  const res = await autoScheduleApi(autoScheduleForm.value.propertyId, year, month, autoScheduleForm.value.dutyUsers)
  ElMessage.success(`自动排班完成，共生成 ${res || 'N'} 条排班记录`)
  autoScheduleVisible.value = false
  getList()
  loadStatistics()
}

// 查看日历
const handleViewCalendar = () => {
  calendarMonth.value = new Date()
  calendarPropertyId.value = undefined
  calendarVisible.value = true
  loadCalendarData()
}

// 日历月份变化监听
watch(calendarMonth, () => {
  if (calendarVisible.value) {
    loadCalendarData()
  }
})

// 查看值班记录 - 跳转到记录页面
const handleViewRecords = () => {
  router.push({ name: 'FireWatchDutyRecord' }).catch(() => {
    ElMessage.info('值班记录页面正在开发中')
  })
}

// 导出
const handleExport = async () => {
  try {
    const data = tableData.value
    if (!data.length) {
      ElMessage.warning('暂无数据可导出')
      return
    }
    // 生成CSV
    const headers = ['排班日期', '班次', '班次名称', '值班时间', '值班人', '联系电话', '岗位', '状态', '签到时间', '签退时间', '备注']
    const rows = data.map((row: any) => [
      row.scheduleDate, row.shiftTypeName, row.shiftName,
      `${row.startTime}-${row.endTime}`, row.dutyUserName, row.dutyUserPhone,
      row.dutyUserPosition, row.statusName, formatTimestamp(row.checkInTime), formatTimestamp(row.checkOutTime), row.remark || ''
    ])
    const csv = '\uFEFF' + headers.join(',') + '\n' + rows.map((r: string[]) => r.map(c => `"${c}"`).join(',')).join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `排班表_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// ==================== 导入排班 ====================
const importVisible = ref(false)
const importPropertyId = ref<number | undefined>(undefined)
const importFileList = ref<any[]>([])
const importLoading = ref(false)

const handleImport = () => {
  importPropertyId.value = queryParams.propertyId
  importFileList.value = []
  importVisible.value = true
}

const submitImport = async () => {
  if (!importPropertyId.value) {
    ElMessage.warning('请先选择所属单位')
    return
  }
  if (!importFileList.value.length) {
    ElMessage.error('请先选择文件')
    return
  }
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFileList.value[0].raw)
    formData.append('propertyId', String(importPropertyId.value))
    const res = await importDutySchedule(formData)
    let text = `导入完成！成功 ${res.successCount || 0} 条`
    if (res.failCount > 0) {
      text += `，失败 ${res.failCount} 条`
      if (res.errors && res.errors.length > 0) {
        text += '<br/>失败详情：'
        for (const item of res.errors) {
          text += `<br/>第${item.row}行：${item.message}`
        }
      }
    }
    ElMessage({ message: text, type: res.failCount > 0 ? 'warning' : 'success', dangerouslyUseHTMLString: true })
    importVisible.value = false
    getList()
    loadStatistics()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

const handleDownloadTemplate = async () => {
  try {
    const res = await downloadDutyScheduleTemplate()
    download.excel(res, '排班导入模板.xls')
  } catch (e) {
    console.error('下载模板失败', e)
  }
}

// 班次类型变化
const handleShiftTypeChange = (val: string) => {
  if (val === 'day') {
    formData.value.shiftName = '白班'
    formData.value.startTime = '08:00'
    formData.value.endTime = '18:00'
  } else if (val === 'night') {
    formData.value.shiftName = '夜班'
    formData.value.startTime = '18:00'
    formData.value.endTime = '08:00'
  } else {
    formData.value.shiftName = '全天班'
    formData.value.startTime = '00:00'
    formData.value.endTime = '23:59'
  }
}

// 重置表单
const resetForm = () => {
  // 使用 Object.assign 保持 formData 对象引用不变，避免 Element Plus 表单内部状态丢失
  Object.assign(formData.value, {
    id: undefined,
    propertyId: undefined,
    scheduleDate: undefined,
    shiftType: 'day',
    shiftName: '白班',
    startTime: '08:00',
    endTime: '18:00',
    dutyUserId: undefined,
    dutyUserName: '',
    dutyUserPhone: '',
    dutyUserPosition: '',
    remark: ''
  })
  formRef.value?.resetFields()
}

// 提交表单
const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  // 防御性校验：el-date-picker 在边缘情况下可能将事件对象（{isTrusted, _vts}）写入 scheduleDate，而非格式化字符串
  if (typeof formData.value.scheduleDate !== 'string') {
    ElMessage.warning('请选择排班日期')
    return
  }
  if (!formData.value.dutyUserId) {
    ElMessage.warning('请选择值班人')
    return
  }
  const user = userList.value.find((u: any) => u.id === formData.value.dutyUserId)
  if (user) {
    formData.value.dutyUserName = user.nickname
    if (!formData.value.dutyUserPhone) {
      formData.value.dutyUserPhone = user.mobile || ''
    }
  }
  // 构建干净的提交数据，只包含后端 VO 需要的字段
  const submitData: any = {
    id: formData.value.id,
    propertyId: formData.value.propertyId,
    scheduleDate: formData.value.scheduleDate,
    shiftType: formData.value.shiftType,
    shiftName: formData.value.shiftName,
    startTime: formData.value.startTime,
    endTime: formData.value.endTime,
    dutyUserId: formData.value.dutyUserId,
    dutyUserName: formData.value.dutyUserName,
    dutyUserPhone: formData.value.dutyUserPhone,
    dutyUserPosition: formData.value.dutyUserPosition,
    remark: formData.value.remark
  }
  console.log('[submitForm] 提交数据:', JSON.stringify(submitData, null, 2))
  console.log('[submitForm] tenantId from cache:', getTenantId())
  try {
    if (formData.value.id) {
      const res = await updateDutySchedule(submitData)
      console.log('[submitForm] 更新响应:', res)
      ElMessage.success('更新成功')
    } else {
      const res = await createDutySchedule(submitData)
      console.log('[submitForm] 创建响应:', res)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
    loadStatistics()
  } catch (e: any) {
    console.error('[submitForm] 保存失败:', e)
    ElMessage.error(e?.response?.data?.msg || e?.message || '保存失败，请重试')
  }
}

// 获取班次类型标签
const getShiftTypeTag = (type: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    day: 'success',
    night: 'info',
    wholeday: 'warning'
  }
  return map[type] || 'info'
}

// 获取状态标签
const getStatusTypeTag = (status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    scheduled: 'info',
    on_duty: 'success',
    completed: 'success',
    absent: 'danger'
  }
  return map[status] || 'info'
}

// 加载物业列表
const loadPropertyList = async () => {
  try {
    const res = await getPropertyList()
    propertyList.value = res || []
  } catch (e) {
    propertyList.value = []
  }
}

// 根据所属单位ID查找对应租户ID
const findPropertyTenantId = (propertyId?: number): number | undefined => {
  if (propertyId == null) return undefined
  const property = propertyList.value.find((item: any) => item.id === propertyId)
  return property?.tenantId
}

// 加载用户列表（携带指定租户ID，实现租户隔离）
const loadUserList = async (tenantId?: number) => {
  if (tenantId == null) {
    userList.value = []
    return
  }
  try {
    const res = await getSimpleUserList({ tenantId })
    userList.value = (res as any) || []
  } catch (e) {
    userList.value = []
  }
}

// 搜索栏所属单位变化：按所选单位租户加载值班人员
const handleQueryPropertyChange = (propertyId?: number) => {
  loadUserList(findPropertyTenantId(propertyId))
}

// 新增/编辑弹窗所属单位变化：按租户加载值班人，并清空已选值班人避免跨单位残留
const handleFormPropertyChange = (propertyId?: number) => {
  formData.value.dutyUserId = undefined
  formData.value.dutyUserName = ''
  formData.value.dutyUserPhone = ''
  loadUserList(findPropertyTenantId(propertyId))
}

// 自动排班弹窗所属单位变化：按租户加载值班人员，并清空已选人员
const handleAutoPropertyChange = (propertyId?: number) => {
  autoScheduleForm.value.dutyUsers = []
  loadUserList(findPropertyTenantId(propertyId))
}

// 初始化
onMounted(async () => {
  getList()
  loadStatistics()
  loadPropertyList()
})
</script>

<style scoped>
.stat-card {
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  line-height: 1.4;
}
.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}
.calendar-cell {
  min-height: 60px;
  font-size: 12px;
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
  transition: background-color 0.2s;
}
.calendar-cell:hover {
  background-color: #f0f5ff;
}
.calendar-day {
  font-weight: bold;
  margin-bottom: 2px;
}
.calendar-day.is-today {
  color: #409eff;
  font-size: 14px;
}
.calendar-schedule-item {
  margin-bottom: 1px;
}
.calendar-tag {
  display: block;
  margin-bottom: 1px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
:deep(.el-calendar-table .el-calendar-day) {
  height: auto !important;
  min-height: 70px;
  padding: 4px;
}
.calendar-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}
.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.calendar-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}
.calendar-month-label {
  font-size: 16px;
  font-weight: bold;
  min-width: 120px;
  text-align: center;
}
.calendar-legend {
  display: flex;
  gap: 6px;
}
.calendar-summary {
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
}
.summary-item {
  margin: 0 4px;
}
.summary-divider {
  margin: 0 8px;
  color: #dcdfe6;
}
.timeline-card {
  padding: 4px 0;
}
.tl-header {
  margin-bottom: 6px;
}
.tl-body {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
}
.tl-body label {
  color: #909399;
  margin-right: 4px;
}
.tl-actions {
  margin-top: 8px;
  display: flex;
  gap: 6px;
}
.ml-5px {
  margin-left: 5px;
}
</style>
