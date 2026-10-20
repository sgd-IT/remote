<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="所属单位" prop="propertyId">
        <el-select v-model="queryParams.propertyId" placeholder="请选择单位" clearable style="width: 200px">
          <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="计划日期" prop="planDate">
        <el-date-picker
          v-model="queryParams.planDate"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="任务类型" prop="taskType">
        <el-select v-model="queryParams.taskType" placeholder="请选择" clearable style="width: 140px">
          <el-option label="日常巡检" value="routine" />
          <el-option label="专项检查" value="special" />
          <el-option label="维保检查" value="maintenance" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 120px">
          <el-option label="待执行" value="pending" />
          <el-option label="进行中" value="in_progress" />
          <el-option label="已完成" value="completed" />
          <el-option label="已超期" value="overdue" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
      </el-form-item>
      <el-form-item label="巡检人" prop="patrolUserName">
        <el-input v-model="queryParams.patrolUserName" placeholder="请输入巡检人姓名" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 操作栏 -->
  <ContentWrap>
    <el-row :gutter="10" class="mb-10px">
      <el-col :span="1.5">
        <el-button type="primary" @click="handleCreate"><Icon icon="ep:plus" class="mr-5px" />新建任务</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" @click="handleStatistics"><Icon icon="ep:data-analysis" class="mr-5px" />统计分析</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button @click="handleExport"><Icon icon="ep:download" class="mr-5px" />导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button @click="handleImport"><Icon icon="ep:upload" class="mr-5px" />导入</el-button>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column label="任务编号" prop="taskNo" width="150" />
      <el-table-column label="所属单位" prop="propertyName" min-width="150" show-overflow-tooltip />
      <el-table-column label="任务名称" prop="taskName" min-width="150" show-overflow-tooltip />
      <el-table-column label="任务类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTaskTypeTag(row.taskType)" size="small">{{ row.taskTypeName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="计划日期" prop="planDate" width="110" />
      <el-table-column label="巡检人" prop="patrolUserName" width="90" />
      <el-table-column label="联系电话" prop="patrolUserPhone" width="110" />
      <el-table-column label="进度" width="140">
        <template #default="{ row }">
          <el-progress :percentage="row.progress || 0" :status="getProgressStatus(row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="点位统计" width="110">
        <template #default="{ row }">
          <span>{{ row.completedPoints || 0 }}/{{ row.totalPoints || 0 }}</span>
          <el-tag v-if="row.abnormalPoints > 0" type="danger" size="small" class="ml-5px">
            异常{{ row.abnormalPoints }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="getStatusTypeTag(row.status)" size="small">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="巡检结果" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.result" :type="row.result === 'normal' ? 'success' : 'danger'" size="small">
            {{ row.resultName }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="290" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'pending'" link type="success" @click="handleStart(row)">开始</el-button>
          <el-button v-if="row.status === 'in_progress'" link type="warning" @click="handleExecute(row)">执行</el-button>
          <el-button v-if="row.status === 'in_progress'" link type="success" @click="handleComplete(row)">完成</el-button>
          <el-button v-if="row.status === 'pending'" link type="info" @click="handleUpdate(row)">编辑</el-button>
          <el-button v-if="row.status === 'pending'" link type="danger" @click="handleCancel(row)">取消</el-button>
          <el-button v-if="row.status === 'pending'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
  </ContentWrap>

  <!-- 新增/编辑对话框 -->
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" append-to-body>
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="所属单位" prop="propertyId">
        <el-select v-model="formData.propertyId" placeholder="请选择单位" style="width: 100%" @change="handlePropertyChange">
          <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务名称" prop="taskName">
        <el-input v-model="formData.taskName" placeholder="请输入任务名称" />
      </el-form-item>
      <el-form-item label="任务类型" prop="taskType">
        <el-radio-group v-model="formData.taskType">
          <el-radio label="routine">日常巡检</el-radio>
          <el-radio label="special">专项检查</el-radio>
          <el-radio label="maintenance">维保检查</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="计划日期" prop="planDate">
        <el-date-picker v-model="formData.planDate" type="date" placeholder="请选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-row>
        <el-col :span="12">
          <el-form-item label="开始时间">
            <el-date-picker v-model="formData.planStartTime" type="datetime" placeholder="请选择" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束时间">
            <el-date-picker v-model="formData.planEndTime" type="datetime" placeholder="请选择" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="巡检人" prop="patrolUserId">
        <el-select v-model="formData.patrolUserId" placeholder="请选择巡检人" style="width: 100%" @change="handlePatrolUserChange">
          <el-option v-for="item in userList" :key="item.id" :label="item.nickname" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="formData.patrolUserPhone" placeholder="请输入联系电话" />
      </el-form-item>
      <!-- 巡检设备选择 -->
      <el-form-item v-if="!formData.id" label="巡检设备">
        <el-select v-model="selectedDevices" multiple placeholder="请选择巡检设备" style="width: 100%" :loading="deviceLoading">
          <el-option v-for="item in deviceList" :key="item.id" :label="`${item.name} (${item.dn || ''})`" :value="item.id">
            <span>{{ item.name }}</span>
            <span class="ml-5px text-gray">[{{ item.addr || '位置未知' }}]</span>
          </el-option>
        </el-select>
        <div class="el-form-item-extra">选择设备自动添加到巡检点位</div>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>

  <!-- 任务详情对话框 -->
  <el-dialog v-model="detailVisible" title="任务详情" width="950px" append-to-body top="30px">
    <el-descriptions :column="3" border>
      <el-descriptions-item label="任务编号">{{ detailData.taskNo }}</el-descriptions-item>
      <el-descriptions-item label="所属单位">{{ detailData.propertyName }}</el-descriptions-item>
      <el-descriptions-item label="任务名称">{{ detailData.taskName }}</el-descriptions-item>
      <el-descriptions-item label="任务类型">{{ detailData.taskTypeName }}</el-descriptions-item>
      <el-descriptions-item label="计划日期">{{ detailData.planDate }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="getStatusTypeTag(detailData.status)">{{ detailData.statusName }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="巡检人">{{ detailData.patrolUserName }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ detailData.patrolUserPhone }}</el-descriptions-item>
      <el-descriptions-item label="进度">{{ detailData.progress }}%</el-descriptions-item>
    </el-descriptions>

    <el-divider content-position="left">巡检点位</el-divider>

    <el-table :data="pointList" stripe border max-height="400">
      <el-table-column label="设备名称" prop="deviceName" min-width="150" />
      <el-table-column label="设备编码" prop="deviceCode" width="120" />
      <el-table-column label="位置" prop="location" min-width="150" />
      <el-table-column label="楼层" prop="floor" width="70" />
      <el-table-column label="区域" prop="area" width="90" />
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 'completed' ? 'success' : 'info'" size="small">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="70">
        <template #default="{ row }">
          <el-tag v-if="row.result" :type="row.result === 'normal' ? 'success' : 'danger'" size="small">{{ row.resultName }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="问题描述" prop="problemDesc" min-width="130" show-overflow-tooltip />
      <el-table-column label="现场照片" width="100">
        <template #default="{ row }">
          <el-button v-if="row.photos" link type="primary" @click="handleViewPhotos(row)">查看</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="巡检时间" width="155">
        <template #default="{ row }">
          {{ formatDate(row.checkTime) }}
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <!-- 查看照片对话框 -->
  <el-dialog v-model="photoViewVisible" title="现场照片" width="600px" append-to-body>
    <div v-if="currentPhotos && currentPhotos.length" class="photo-grid">
      <div v-for="(url, idx) in currentPhotos" :key="idx" class="photo-item">
        <el-image :src="url" fit="contain" style="width: 100%; height: 200px" :preview-src-list="currentPhotos" :initial-index="idx" />
      </div>
    </div>
    <el-empty v-else description="暂无照片" />
  </el-dialog>

  <!-- 执行任务对话框 -->
  <el-dialog v-model="executeVisible" title="执行巡检" width="800px" append-to-body>
    <el-table :data="executePointList" stripe border max-height="500">
      <el-table-column label="设备名称" prop="deviceName" min-width="150" />
      <el-table-column label="位置" prop="location" min-width="150" />
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 'completed' ? 'success' : 'info'" size="small">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="70">
        <template #default="{ row }">
          <el-tag v-if="row.result" :type="row.result === 'normal' ? 'success' : 'danger'" size="small">{{ row.resultName }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" link type="primary" @click="handleCheckPoint(row)">检查</el-button>
          <el-tag v-else :type="row.result === 'normal' ? 'success' : 'danger'" size="small">{{ row.resultName }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <!-- 检查点位对话框 -->
  <el-dialog v-model="checkPointVisible" title="检查点位" width="550px" append-to-body>
    <el-form ref="checkFormRef" :model="checkPointForm" :rules="checkPointRules" label-width="100px">
      <el-form-item label="设备名称">
        <el-input :value="checkPointForm.deviceName" disabled />
      </el-form-item>
      <el-form-item label="位置">
        <el-input :value="checkPointForm.location" disabled />
      </el-form-item>
      <el-form-item label="检查结果" prop="result">
        <el-radio-group v-model="checkPointForm.result">
          <el-radio label="normal">正常</el-radio>
          <el-radio label="abnormal">异常</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="checkPointForm.result === 'abnormal'" label="问题描述" prop="problemDesc">
        <el-input v-model="checkPointForm.problemDesc" type="textarea" placeholder="请描述问题" :rows="3" />
      </el-form-item>
      <el-form-item v-if="checkPointForm.result === 'abnormal'" label="整改建议">
        <el-input v-model="checkPointForm.suggestion" type="textarea" placeholder="请输入整改建议" :rows="2" />
      </el-form-item>
      <el-form-item label="现场照片">
        <el-upload
          ref="photoUploadRef"
          v-model:file-list="checkPhotoFileList"
          :auto-upload="false"
          list-type="picture-card"
          accept="image/*"
          multiple
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="checkPointForm.remark" type="textarea" placeholder="请输入备注" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="checkPointVisible = false">取消</el-button>
      <el-button type="primary" @click="submitCheckPoint">确定</el-button>
    </template>
  </el-dialog>

  <!-- 完成任务对话框 -->
  <el-dialog v-model="completeVisible" title="完成任务" width="500px" append-to-body>
    <el-form label-width="100px">
      <el-form-item label="巡检总结">
        <el-input v-model="completeSummary" type="textarea" placeholder="请输入巡检总结" :rows="4" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="completeVisible = false">取消</el-button>
      <el-button type="primary" @click="submitComplete">确定完成</el-button>
    </template>
  </el-dialog>

  <!-- 导入对话框 -->
  <el-dialog v-model="importVisible" title="导入巡检任务" width="400px" append-to-body>
    <el-upload
      ref="importUploadRef"
      :auto-upload="false"
      :limit="1"
      :headers="importHeaders"
      :action="importUrl"
      accept=".xlsx, .xls"
      drag
      :on-success="handleImportSuccess"
      :on-error="handleImportError"
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
      <el-button type="primary" @click="submitImport">确 定</el-button>
      <el-button @click="importVisible = false">取 消</el-button>
    </template>
  </el-dialog>

  <!-- 统计分析对话框 -->
  <el-dialog v-model="statisticsVisible" title="巡检统计分析" width="860px" append-to-body top="30px" class="statistics-dialog">
    <div class="stat-charts-row">
      <div class="stat-chart-card">
        <div class="stat-chart-header">
          <span class="stat-chart-icon stat-chart-icon-status"></span>
          <span class="stat-chart-label">任务状态分布</span>
        </div>
        <div ref="statusChartRef" class="stat-chart-body"></div>
      </div>
      <div class="stat-chart-card">
        <div class="stat-chart-header">
          <span class="stat-chart-icon stat-chart-icon-type"></span>
          <span class="stat-chart-label">任务类型分布</span>
        </div>
        <div ref="typeChartRef" class="stat-chart-body"></div>
      </div>
    </div>
    <div class="stat-search-bar-wrap">
      <div class="stat-search-inner">
        <div class="stat-search-field">
          <span class="stat-search-label">统计范围</span>
          <el-date-picker
            v-model="statDateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="stat-date-picker"
          />
        </div>
        <div class="stat-search-field">
          <span class="stat-search-label">所属单位</span>
          <el-select v-model="statPropertyId" placeholder="全部单位" clearable class="stat-unit-select">
            <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
          </el-select>
        </div>
        <el-button type="primary" @click="loadStatisticsData" class="stat-search-btn">
          <Icon icon="ep:search" class="mr-5px" />搜索
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { formatDate } from '@/utils/formatTime'
import {
  getPatrolTaskPage,
  createPatrolTask,
  updatePatrolTask,
  deletePatrolTask,
  startPatrolTask,
  completePatrolTask,
  cancelPatrolTask,
  getPatrolTaskPoints,
  checkPatrolPoint,
  getPatrolTaskStatistics,
  checkOverdueTasks,
  exportPatrolTask,
  downloadPatrolTaskTemplate,
  importPatrolTask
} from '@/api/eiot/firewatch/firewatch.api'
import { getPropertyList } from '@/api/eiot/firewatch/firewatch.api'
import { getSimpleUserList } from '@/api/system/user/index'
import * as FileApi from '@/api/infra/file'
import download from '@/utils/download'
import { getAccessToken, getTenantId } from '@/utils/auth'
import { showImportResult } from '@/utils/importResult'

defineOptions({ name: 'FireWatchPatrol' })

const loading = ref(false)
const total = ref(0)
const tableData = ref<any[]>([])
const propertyList = ref<any[]>([])
const userList = ref<any[]>([])
const deviceList = ref<any[]>([])
const deviceLoading = ref(false)
const selectedDevices = ref<number[]>([])

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  propertyId: undefined as number | undefined,
  planDate: undefined as string[] | undefined,
  taskType: undefined,
  status: undefined,
  patrolUserName: undefined
})

const queryFormRef = ref<FormInstance>()

// 导入相关
const importVisible = ref(false)
const importUploadRef = ref<any>()
const importUrl = import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL + '/eiot/firewatch/patrol-task/import'
const importHeaders = computed(() => ({
  Authorization: 'Bearer ' + getAccessToken(),
  'tenant-id': getTenantId()
}))

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const formData = ref<any>({
  id: undefined,
  propertyId: undefined,
  taskName: '',
  taskType: 'routine',
  planDate: undefined,
  planStartTime: undefined,
  planEndTime: undefined,
  patrolUserId: undefined,
  patrolUserPhone: '',
  remark: ''
})

const formRules = reactive<FormRules>({
  propertyId: [{ required: true, message: '请选择所属单位', trigger: 'change' }],
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }],
  patrolUserId: [{ required: true, message: '请选择巡检人', trigger: 'change' }]
})

// 单位变更时加载设备和巡检人（租户数据隔离）
const handlePropertyChange = async (propertyId: number) => {
  if (!propertyId) {
    deviceList.value = []
    selectedDevices.value = []
    return
  }
  deviceLoading.value = true
  try {
    // 获取选中单位的租户ID
    const property = propertyList.value.find(p => p.id === propertyId)
    const tenantId = property?.tenantId

    // 根据租户隔离加载设备列表（只能选该单位所属租户下的设备）
    const { getDevicesList } = await import('@/api/eiot/deviceinfo/devices.api')
    const res = await getDevicesList({ pageNo: 1, pageSize: 100, tenantId })
    deviceList.value = res?.list || []

    // 根据租户隔离加载巡检人列表（只能选该单位所属租户下的人员）
    const users = await getSimpleUserList({ tenantId })
    userList.value = users || []
  } catch (e) {
    deviceList.value = []
  } finally {
    deviceLoading.value = false
  }
}

// 巡检人变更自动填充
const handlePatrolUserChange = (userId: number) => {
  const user = userList.value.find((u: any) => u.id === userId)
  if (user) {
    formData.value.patrolUserPhone = user.mobile || ''
  }
}

// 详情
const detailVisible = ref(false)
const detailData = ref<any>({})
const pointList = ref<any[]>([])

// 照片查看
const photoViewVisible = ref(false)
const currentPhotos = ref<string[]>([])

const handleViewPhotos = (row: any) => {
  try {
    const photos = row.photos ? JSON.parse(row.photos) : []
    currentPhotos.value = Array.isArray(photos) ? photos : [photos]
  } catch {
    currentPhotos.value = row.photos ? [row.photos] : []
  }
  photoViewVisible.value = true
}

// 执行
const executeVisible = ref(false)
const executeTaskId = ref<number>(0)
const executePointList = ref<any[]>([])

// 检查点位
const checkPointVisible = ref(false)
const checkFormRef = ref<FormInstance>()
const photoUploadRef = ref<any>()
const checkPhotoFileList = ref<any[]>([])  // 绑定 upload 组件的已选文件列表

const checkPointForm = ref({
  id: undefined as number | undefined,
  deviceName: '',
  location: '',
  result: 'normal',
  problemDesc: '',
  suggestion: '',
  photos: '',
  remark: ''
})

const checkPointRules = reactive<FormRules>({
  result: [{ required: true, message: '请选择检查结果', trigger: 'change' }],
  problemDesc: [{ required: true, message: '请描述问题', trigger: 'blur' }]
})

// 完成任务
const completeVisible = ref(false)
const completeTaskId = ref<number>(0)
const completeSummary = ref('')

// 统计分析
const statisticsVisible = ref(false)
const statDateRange = ref<string[]>([])
const statPropertyId = ref<number | undefined>(undefined)
const statusChartRef = ref<HTMLDivElement>()
const typeChartRef = ref<HTMLDivElement>()
let statusChart: ECharts | null = null
let typeChart: ECharts | null = null
const statLoading = ref(false)

const loadStatisticsData = async () => {
  const now = new Date()
  const startDate = statDateRange.value?.[0] || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const endDate = statDateRange.value?.[1] || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()).padStart(2, '0')}`
  
  // 根据选择的所属单位查找 tenantId，实现租户级数据隔离
  let tenantId: number | undefined
  if (statPropertyId.value) {
    const property = propertyList.value.find(p => p.id === statPropertyId.value)
    tenantId = property?.tenantId
  }
  console.log('[Statistics] 查询参数:', { startDate, endDate, propertyId: statPropertyId.value, tenantId })
  
  statLoading.value = true
  try {
    const res = await getPatrolTaskStatistics(startDate, endDate, statPropertyId.value, tenantId)
    console.log('[Statistics] API返回原始数据:', res)
    
    // 兼容不同响应格式：有的后端返回 { data: {...} }，有的直接返回 {...}
    const data = res?.data || res || {}
    console.log('[Statistics] 处理后数据:', data)
    
    await nextTick()
    
    // 状态分布饼图
    if (statusChartRef.value) {
      if (!statusChart) {
        statusChart = echarts.init(statusChartRef.value)
      }
      const statusMap: Record<string, string> = {
        pending: '待执行', in_progress: '进行中', completed: '已完成',
        overdue: '已超期', cancelled: '已取消'
      }
      const statusColors: Record<string, string> = {
        pending: '#909399', in_progress: '#e6a23c', completed: '#67c23a',
        overdue: '#f56c6c', cancelled: '#c0c4cc'
      }
      const statusStats = data.statusStatistics || {}
      console.log('[Statistics] 状态统计数据:', statusStats)
      const statusData = Object.entries(statusStats)
        .filter(([_, v]) => (v as number) > 0)
        .map(([k, v]) => ({
          name: statusMap[k] || k,
          value: v,
          itemStyle: { color: statusColors[k] || '#909399' }
        }))
      
      statusChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, textStyle: { fontSize: 12 } },
        series: [{
          type: 'pie',
          radius: ['30%', '60%'],
          center: ['50%', '45%'],
          data: statusData.length ? statusData : [{ name: '暂无数据', value: 1, itemStyle: { color: '#eee' } }],
          label: { show: true, formatter: '{b}\n{d}%', fontSize: 12 }
        }]
      })
      statusChart.resize()
    }
    
    // 类型分布柱状图
    if (typeChartRef.value) {
      if (!typeChart) {
        typeChart = echarts.init(typeChartRef.value)
      }
      const typeMap: Record<string, string> = {
        routine: '日常巡检', special: '专项检查', maintenance: '维保检查'
      }
      
      // 获取类型统计原始数据，兼容多种字段名
      const rawTypeStats = data.typeStatistics || data.taskTypeStatistics || data.typeStats || []
      console.log('[Statistics] 类型统计原始数据:', rawTypeStats)
      
      let typeStats: { taskType: string; count: number }[] = []
      
      if (rawTypeStats.length > 0) {
        // 标准化处理：兼容 taskType/type/name、count/value 等不同字段名
        typeStats = rawTypeStats
          .map((t: any) => ({
            taskType: t.taskType || t.type || t.name || t.key,
            count: t.count || t.value || t.num || 0
          }))
          .filter((t: any) => t.taskType && t.count > 0)
      }
      
      // 如果统计API没有返回类型数据，则从任务列表接口获取并聚合
      if (typeStats.length === 0) {
        console.log('[Statistics] 统计API无类型数据，尝试从任务列表聚合...')
        try {
          const taskRes = await getPatrolTaskPage({
            pageNo: 1,
            pageSize: 9999,
            propertyId: statPropertyId.value,
            planDateStart: startDate,
            planDateEnd: endDate
          })
          const tasks = taskRes?.list || []
          console.log('[Statistics] 任务列表返回条数:', tasks.length)
          
          if (tasks.length > 0) {
            const typeCount: Record<string, number> = {}
            tasks.forEach((t: any) => {
              const type = t.taskType
              if (type) {
                typeCount[type] = (typeCount[type] || 0) + 1
              }
            })
            typeStats = Object.entries(typeCount)
              .map(([taskType, count]) => ({ taskType, count }))
              .filter(t => t.count > 0)
            console.log('[Statistics] 从任务列表聚合后的类型数据:', typeStats)
          }
        } catch (e) {
          console.error('[Statistics] 任务列表聚合失败:', e)
        }
      }

      if (typeStats.length > 0) {
        typeChart.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: '10%', right: '8%', bottom: '18%', top: '12%' },
          xAxis: {
            type: 'category',
            data: typeStats.map((t: any) => typeMap[t.taskType] || t.taskType),
            axisLabel: { fontSize: 12 }
          },
          yAxis: { type: 'value', minInterval: 1 },
          series: [{
            type: 'bar',
            data: typeStats.map((t: any) => t.count),
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#409eff' },
                { offset: 1, color: '#79bbff' }
              ]),
              borderRadius: [4, 4, 0, 0]
            },
            barWidth: '40%',
            label: {
              show: true,
              position: 'top',
              color: '#606266',
              fontSize: 12
            }
          }]
        })
      } else {
        // 无数据时显示空状态
        typeChart.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: '10%', right: '8%', bottom: '18%', top: '12%' },
          xAxis: { type: 'category', data: [] },
          yAxis: { type: 'value' },
          graphic: [{
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
              text: '暂无任务类型数据',
              fill: '#909399',
              fontSize: 14
            }
          }],
          series: [{ type: 'bar', data: [] }]
        })
      }
      typeChart.resize()
    }
  } catch (e) {
    console.error('[Statistics] 统计数据加载失败:', e)
    ElMessage.error('统计数据加载失败，请稍后重试')
  } finally {
    statLoading.value = false
  }
}

// 获取列表
const getList = async () => {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (queryParams.planDate && queryParams.planDate.length === 2) {
      params.planDateStart = queryParams.planDate[0]
      params.planDateEnd = queryParams.planDate[1]
      delete params.planDate
    }
    const res = await getPatrolTaskPage(params)
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
}

// 重置
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

// 新增
const handleCreate = () => {
  resetForm()
  selectedDevices.value = []
  loadUserList() // 重置巡检人列表为当前租户默认列表
  dialogTitle.value = '新建巡检任务'
  dialogVisible.value = true
}

// 编辑
const handleUpdate = (row: any) => {
  resetForm()
  selectedDevices.value = []
  dialogTitle.value = '编辑巡检任务'
  formData.value = { ...row }
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该任务？', '提示', { type: 'warning' })
  await deletePatrolTask(row.id)
  ElMessage.success('删除成功')
  getList()
}

// 详情
const handleDetail = async (row: any) => {
  detailData.value = row
  const res = await getPatrolTaskPoints(row.id)
  pointList.value = res || []
  detailVisible.value = true
}

// 开始任务
const handleStart = async (row: any) => {
  await ElMessageBox.confirm(`确认开始执行任务【${row.taskName}】？`, '提示', { type: 'info' })
  await startPatrolTask(row.id)
  ElMessage.success('任务已开始')
  getList()
}

// 执行任务
const handleExecute = async (row: any) => {
  executeTaskId.value = row.id
  const res = await getPatrolTaskPoints(row.id)
  executePointList.value = res || []
  executeVisible.value = true
}

// 检查点位
const handleCheckPoint = (row: any) => {
  checkPointForm.value = {
    id: row.id,
    deviceName: row.deviceName,
    location: row.location,
    result: 'normal',
    problemDesc: '',
    suggestion: '',
    photos: '',
    remark: ''
  }
  // 清空已选文件列表
  checkPhotoFileList.value = []
  checkPointVisible.value = true
}

// 提交检查
const submitCheckPoint = async () => {
  await checkFormRef.value?.validate()

  // 上传照片到文件存储（可配置为腾讯云COS）并获取URL
  const photos: string[] = []

  // 从 checkPhotoFileList 读取已选文件（通过 v-model:file-list 绑定）
  const files = checkPhotoFileList.value || []

  for (const f of files) {
    const raw = f.raw
    if (raw) {
      try {
        const url = await FileApi.updateFile({ file: raw, path: '' })
        const realUrl = typeof url === 'string' ? url : (url?.data || url?.url || url)
        if (realUrl) {
          photos.push(realUrl)
        }
      } catch (e) {
        console.error('[submitCheckPoint] 照片上传失败:', e)
      }
    }
  }

  if (photos.length > 0) {
    checkPointForm.value.photos = JSON.stringify(photos)
  }

  await checkPatrolPoint(checkPointForm.value)
  ElMessage.success('检查完成')
  checkPointVisible.value = false

  // 刷新点位列表
  const res = await getPatrolTaskPoints(executeTaskId.value)
  executePointList.value = res || []
  getList()
}

// 完成任务
const handleComplete = (row: any) => {
  completeTaskId.value = row.id
  completeSummary.value = ''
  completeVisible.value = true
}

// 提交完成
const submitComplete = async () => {
  await completePatrolTask(completeTaskId.value, completeSummary.value)
  ElMessage.success('任务已完成')
  completeVisible.value = false
  getList()
}

// 取消任务
const handleCancel = async (row: any) => {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消任务', {
    inputPattern: /.+/,
    inputErrorMessage: '请输入取消原因'
  })
  await cancelPatrolTask(row.id, value)
  ElMessage.success('任务已取消')
  getList()
}

// 导出
const handleExport = async () => {
  try {
    const res = await exportPatrolTask(queryParams)
    download.excel(res, '巡检任务.xls')
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出失败', e)
  }
}

// 导入
const handleImport = () => {
  importVisible.value = true
}

const submitImport = () => {
  if (!importUploadRef.value || importUploadRef.value!.uploadFiles.length === 0) {
    ElMessage.error('请先选择文件')
    return
  }
  importUploadRef.value!.submit()
}

const handleImportSuccess = (response: any) => {
  if (response.code !== 0) {
    ElMessage.error(response.msg || '导入失败')
    return
  }
  importVisible.value = false
  getList()
  showImportResult(response)
}

const handleImportError = () => {
  ElMessage.error('导入失败，请检查文件格式')
}

const handleDownloadTemplate = async () => {
  try {
    const res = await downloadPatrolTaskTemplate()
    download.excel(res, '巡检任务导入模板.xls')
  } catch (e) {
    console.error('下载模板失败', e)
  }
}

// 统计分析
const handleStatistics = async () => {
  const now = new Date()
  const firstDay = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const lastDay = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()).padStart(2, '0')}`
  statDateRange.value = [firstDay, lastDay]
  statPropertyId.value = undefined
  statisticsVisible.value = true
  
  await nextTick()
  loadStatisticsData()
}

// 重置表单
const resetForm = () => {
  formData.value = {
    id: undefined,
    propertyId: undefined,
    taskName: '',
    taskType: 'routine',
    planDate: undefined,
    planStartTime: undefined,
    planEndTime: undefined,
    patrolUserId: undefined,
    patrolUserPhone: '',
    remark: ''
  }
  formRef.value?.resetFields()
}

// 提交表单
const submitForm = async () => {
  await formRef.value?.validate()
  
  // 自动填充巡检人信息
  if (formData.value.patrolUserId) {
    const user = userList.value.find((u: any) => u.id === formData.value.patrolUserId)
    if (user) {
      formData.value.patrolUserName = user.nickname
      if (!formData.value.patrolUserPhone) {
        formData.value.patrolUserPhone = user.mobile || ''
      }
    }
  }
  
  if (formData.value.id) {
    await updatePatrolTask(formData.value)
    ElMessage.success('更新成功')
  } else {
    // 将选中的设备ID列表传入创建请求，后端在同一个事务中创建任务和点位
    const deviceIdList = selectedDevices.value
    if (!deviceIdList || deviceIdList.length === 0) {
      console.warn('[createTask] 警告：未选择任何巡检设备，点位统计将为0')
    }
    formData.value.deviceIds = deviceIdList
    await createPatrolTask(formData.value)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  getList()
}

// 获取任务类型标签
type TagType = 'success' | 'warning' | 'info' | 'primary' | 'danger'

const getTaskTypeTag = (type: string): TagType => {
  const map: Record<string, TagType> = {
    routine: 'primary',
    special: 'warning',
    maintenance: 'info'
  }
  return map[type] || 'info'
}

// 获取状态标签
const getStatusTypeTag = (status: string): TagType => {
  const map: Record<string, TagType> = {
    pending: 'info',
    in_progress: 'warning',
    completed: 'success',
    overdue: 'danger',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

// 获取进度条状态
const getProgressStatus = (status: string) => {
  if (status === 'completed') return 'success'
  if (status === 'overdue') return 'exception'
  return undefined
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

// 加载用户列表
const loadUserList = async () => {
  try {
    const res = await getSimpleUserList()
    userList.value = (res as any) || []
  } catch (e) {
    userList.value = []
  }
}

// 初始化
onMounted(() => {
  getList()
  loadPropertyList()
  loadUserList()
})
</script>

<style scoped>
.chart-title {
  text-align: center;
  margin-bottom: 10px;
  color: #606266;
}
.text-gray {
  color: #999;
  font-size: 12px;
}
.ml-5px {
  margin-left: 5px;
}
.mt-20px {
  margin-top: 20px;
}
.text-center {
  text-align: center;
}
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.photo-item {
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #eee;
}
:deep(.el-form-item-extra) {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 统计分析对话框样式 */
:deep(.statistics-dialog .el-dialog__body) {
  padding: 20px 24px;
}

.stat-charts-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-chart-card {
  flex: 1;
  min-width: 0;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.stat-chart-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stat-chart-header {
  display: flex;
  align-items: center;
  padding: 12px 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.stat-chart-icon {
  display: inline-block;
  width: 4px;
  height: 14px;
  border-radius: 2px;
  margin-right: 8px;
  flex-shrink: 0;
}

.stat-chart-icon-status {
  background: linear-gradient(180deg, #409eff, #79bbff);
}

.stat-chart-icon-type {
  background: linear-gradient(180deg, #67c23a, #95d475);
}

.stat-chart-label {
  line-height: 22px;
}

.stat-chart-body {
  height: 280px;
  padding: 8px 4px 0;
}

.stat-search-bar-wrap {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 16px;
}

.stat-search-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.stat-search-field {
  display: flex;
  align-items: center;
  margin-right: 16px;
  white-space: nowrap;
}

.stat-search-label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-right: 8px;
  flex-shrink: 0;
}

.stat-date-picker {
  width: 240px;
}

.stat-unit-select {
  width: 150px;
}

.stat-search-btn {
  min-width: 80px;
  flex-shrink: 0;
}
</style>
