<template>
  <div class="repair-order-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" inline class="search-form">
      <el-form-item label="工单编号">
        <el-input v-model="queryParams.orderNo" placeholder="工单编号" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="所属租户">
        <el-select v-model="queryParams.tenantId" placeholder="所属租户" clearable style="width: 160px" v-if="isAdmin">
          <el-option v-for="item in tenantList" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="故障类型">
        <el-select v-model="queryParams.faultType" placeholder="故障类型" clearable style="width: 130px">
          <el-option v-for="item in faultTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="工单状态">
        <el-select v-model="queryParams.orderStatus" placeholder="工单状态" clearable style="width: 120px">
          <el-option label="待处理" value="pending" />
          <el-option label="待排查" value="assigned" />
          <el-option label="已完成" value="completed" />
          <el-option label="已关闭" value="closed" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源">
        <el-select v-model="queryParams.source" placeholder="来源" clearable style="width: 120px">
          <el-option label="告警生成" value="auto-alert" />
          <el-option label="手动创建" value="manual" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" icon="Plus" @click="handleCreate">新建工单</el-button>
      <el-button type="warning" icon="WarningFilled" @click="handleCreateFromAlert">从告警生成</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
      <el-table-column prop="orderNo" label="工单编号" width="180" />
      <el-table-column prop="propertyName" label="所属租户" width="140" />
      <el-table-column prop="deviceName" label="设备名称" width="140" />
      <el-table-column prop="faultDescription" label="故障描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tooltip :content="row.faultDescription" placement="top">
            <span>{{ row.faultDescription }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="urgencyLevelName" label="紧急程度" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.urgencyLevel === 'urgent' ? 'danger' : row.urgencyLevel === 'major' ? 'warning' : 'info'" size="small">
            {{ row.urgencyLevelName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceName" label="来源" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.source === 'auto-alert' ? 'danger' : 'primary'" size="small">
            {{ row.sourceName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderStatusName" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.orderStatus === 'pending' ? 'warning' : row.orderStatus === 'assigned' ? 'primary' : row.orderStatus === 'completed' ? 'success' : 'info'" size="small">
            {{ row.orderStatusName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deadline" label="排查截止" width="160">
        <template #default="{ row }">
          <span v-if="row.deadline" :style="{ color: isOverdue(row.deadline) && row.orderStatus !== 'completed' && row.orderStatus !== 'closed' ? 'red' : '' }">
            {{ formatDate(row.deadline) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" :formatter="dateFormatter" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button v-if="row.orderStatus === 'pending'" link type="warning" size="small" @click="handleAssign(row)">派单</el-button>
          <el-button v-if="row.orderStatus === 'assigned'" link type="success" size="small" @click="handleComplete(row)">完成</el-button>
          <el-button v-if="row.orderStatus !== 'closed'" link type="danger" size="small" @click="handleClose(row)">关闭</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 30, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="getList"
        @current-change="getList"
      />
    </div>

    <!-- 新建工单弹窗 -->
    <el-dialog v-model="createVisible" title="新建维修工单" width="600px" @close="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="所属租户" prop="tenantId" v-if="isAdmin">
          <el-select v-model="createForm.tenantId" placeholder="选择租户" style="width: 100%" @change="onTenantChange">
            <el-option v-for="item in tenantList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceId">
          <el-select v-model="createForm.deviceId" placeholder="请先选择租户" style="width: 100%" filterable clearable :disabled="!createForm.tenantId" @change="onDeviceChange">
            <el-option v-for="item in deviceList" :key="item.id" :label="item.name" :value="item.id">
              <span>{{ item.name }}</span>
              <span style="color: #999; margin-left: 10px; font-size: 12px;">编码: {{ item.dn }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="设备编码">
          <el-input v-model="createForm.deviceCode" placeholder="选择设备后自动填充" disabled />
        </el-form-item>
        <el-form-item label="故障类型" prop="faultType">
          <el-select v-model="createForm.faultType" placeholder="选择故障类型" style="width: 100%">
            <el-option v-for="item in faultTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDescription">
          <el-input v-model="createForm.faultDescription" type="textarea" :rows="3" placeholder="请输入故障描述" />
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="createForm.urgencyLevel" style="width: 100%">
            <el-option label="一般" value="normal" />
            <el-option label="较大" value="major" />
            <el-option label="紧急" value="urgent" />
          </el-select>
        </el-form-item>
        <el-form-item label="排查截止时间">
          <el-date-picker v-model="createForm.deadline" type="datetime" placeholder="选择排查截止时间" style="width: 100%" format="YYYY-MM-DD HH:mm:ss" value-format="x" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 派单弹窗 -->
    <el-dialog v-model="assignVisible" title="派单（设置排查截止时间）" width="500px">
      <el-form ref="assignFormRef" :model="assignForm" label-width="120px">
        <el-form-item label="租户信息">
          <span>{{ currentOrder?.propertyName }}</span>
        </el-form-item>
        <el-form-item label="自动指派人">
          <span>{{ assignForm.assignee || '将自动填充租户联系人' }}</span>
        </el-form-item>
        <el-form-item label="排查截止时间" prop="deadline">
          <el-date-picker v-model="assignForm.deadline" type="datetime" placeholder="选择排查截止时间" style="width: 100%" format="YYYY-MM-DD HH:mm:ss" value-format="x" />
        </el-form-item>
        <el-form-item>
          <span style="color: #999; font-size: 12px;">不设置则根据紧急程度自动计算截止时间</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <!-- 完成维修弹窗 -->
    <el-dialog v-model="completeVisible" title="完成维修" width="550px">
      <el-form ref="completeFormRef" :model="completeForm" label-width="100px">
        <el-form-item label="完成备注">
          <el-input v-model="completeForm.completeRemark" type="textarea" :rows="4" placeholder="请输入维修完成备注" />
        </el-form-item>
        <el-form-item label="完成图片">
          <el-upload
            v-model:file-list="completeFileList"
            list-type="picture-card"
            :auto-upload="false"
            :limit="9"
            accept="image/*"
            @exceed="handleExceed">
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div style="color: #999; font-size: 12px; margin-top: 4px;">建议上传维修前后的现场照片，最多9张</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="success" @click="submitComplete">确认完成</el-button>
      </template>
    </el-dialog>

    <!-- 从告警生成 - 选择告警记录 -->
    <el-dialog v-model="alertSelectVisible" title="选择告警记录生成工单" width="750px">
      <el-table :data="alertRecords" v-loading="alertLoading" stripe border max-height="400" highlight-current-row @row-click="onSelectAlertRow">
        <el-table-column width="50" label="">
          <template #default="{ row }">
            <span class="select-indicator" :class="{ selected: selectedAlertId === row.id }">
              <el-icon v-if="selectedAlertId === row.id" color="#409eff"><CircleCheckFilled /></el-icon>
              <span v-else class="empty-circle"></span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="告警名称" width="180" show-overflow-tooltip />
        <el-table-column prop="deviceName" label="设备名称" width="140" />
        <el-table-column label="告警时间" width="160">
          <template #default="{ row }">{{ formatDate(row.alertTime) }}</template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="Number(row.level) >= 4 ? 'danger' : Number(row.level) >= 3 ? 'warning' : 'info'" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 12px;">
        <el-pagination
          v-model:current-page="alertPageNo"
          v-model:page-size="alertPageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="alertTotal"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleAlertSizeChange"
          @current-change="handleAlertPageChange"
        />
      </div>
      <template #footer>
        <el-button @click="alertSelectVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFromAlert" :disabled="!selectedAlertId">生成工单</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单编号" :span="2">{{ detailData?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="所属租户">{{ detailData?.propertyName }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ detailData?.sourceName }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData?.deviceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备编码">{{ detailData?.deviceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障类型">{{ detailData?.faultType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ detailData?.faultDescription }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="detailData?.urgencyLevel === 'urgent' ? 'danger' : detailData?.urgencyLevel === 'major' ? 'warning' : 'info'" size="small">
            {{ detailData?.urgencyLevelName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="工单状态">
          <el-tag :type="detailData?.orderStatus === 'pending' ? 'warning' : detailData?.orderStatus === 'assigned' ? 'primary' : detailData?.orderStatus === 'completed' ? 'success' : 'info'" size="small">
            {{ detailData?.orderStatusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="指派人">{{ detailData?.assignee || '-' }}</el-descriptions-item>
        <el-descriptions-item label="指派人电话">{{ detailData?.assigneePhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="派单时间">{{ formatDate(detailData?.assignTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="排查截止时间">
          <span v-if="detailData?.deadline" :style="{ color: isOverdue(detailData.deadline) && detailData?.orderStatus !== 'completed' && detailData?.orderStatus !== 'closed' ? 'red' : '' }">
            {{ formatDate(detailData.deadline) }}
          </span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDate(detailData?.completeTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成备注">{{ detailData?.completeRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(detailData?.createTime) }}</el-descriptions-item>
        <!-- 完成图片 -->
        <el-descriptions-item v-if="detailData?.completeImages" label="完成图片" :span="2">
          <div style="display: flex; flex-wrap: wrap; gap: 8px;">
            <el-image
              v-for="(img, idx) in JSON.parse(detailData.completeImages || '[]')"
              :key="idx"
              :src="img"
              :preview-src-list="JSON.parse(detailData.completeImages || '[]')"
              style="width: 80px; height: 80px; border-radius: 4px; cursor: pointer; object-fit: cover;" />
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CircleCheckFilled } from '@element-plus/icons-vue'
import { dateFormatter, formatDate } from '@/utils/formatTime'
import {
  getRepairOrderPage,
  createRepairOrder,
  assignRepairOrder,
  completeRepairOrder,
  closeRepairOrder,
  getRepairOrder,
  createRepairOrderFromAlert,
  getAlertRecordPage
} from '@/api/eiot/firewatch/firewatch.api'
import { DeviceInfoApi } from '@/api/eiot/deviceinfo'
import { getTenantPage } from '@/api/system/tenant'
import { getTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { updateFile } from '@/api/infra/file'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles?.includes('super_admin'))

// 故障类型选项
const faultTypeOptions = [
  { label: '设备故障', value: 'device_fault' },
  { label: '线路故障', value: 'line_fault' },
  { label: '通讯故障', value: 'comm_fault' },
  { label: '传感器故障', value: 'sensor_fault' },
  { label: '电源故障', value: 'power_fault' },
  { label: '网络故障', value: 'network_fault' },
  { label: '软件故障', value: 'software_fault' },
  { label: '其他故障', value: 'other_fault' }
]

// 查询参数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: '',
  tenantId: undefined as number | undefined,
  faultType: '',
  orderStatus: '',
  source: ''
})

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const tenantList = ref<any[]>([])
const deviceList = ref<any[]>([])
const deviceLoading = ref(false)

// 获取列表
const getList = async () => {
  loading.value = true
  try {
    const params: any = { pageNo: queryParams.pageNo, pageSize: queryParams.pageSize }
    if (queryParams.orderNo) params.orderNo = queryParams.orderNo
    if (queryParams.tenantId) params.tenantId = queryParams.tenantId
    if (queryParams.faultType) params.faultType = queryParams.faultType
    if (queryParams.orderStatus) params.orderStatus = queryParams.orderStatus
    if (queryParams.source) params.source = queryParams.source
    const res = await getRepairOrderPage(params)
    if (res) {
      tableData.value = res.list || []
      total.value = res.total || 0
    }
  } catch (e) {
    console.error('获取维修工单列表失败', e)
  } finally {
    loading.value = false
  }
}

// 获取租户列表（管理员专用）
const loadTenantList = async () => {
  // 企业用户不需要加载租户列表，使用自己的租户
  if (!isAdmin.value) {
    const tenantId = getTenantId()
    if (tenantId) {
      tenantList.value = [{ id: tenantId, name: '当前企业' }]
    }
    return
  }
  try {
    const res = await getTenantPage({ pageNo: 1, pageSize: 100 })
    if (res) {
      tenantList.value = res.list || []
    }
  } catch (e) {
    console.error('获取租户列表失败', e)
  }
}

// 获取设备列表（按租户隔离）
const loadDeviceList = async (tenantId?: number) => {
  deviceLoading.value = true
  try {
    const params: any = { pageNo: 1, pageSize: 100 }
    if (tenantId) {
      params.tenantId = tenantId
    }
    const res = await DeviceInfoApi.getDeviceInfoPage(params)
    if (res) {
      deviceList.value = res.list || []
    }
  } catch (e) {
    console.error('获取设备列表失败', e)
  } finally {
    deviceLoading.value = false
  }
}

// 搜索和重置
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const handleReset = () => {
  queryParams.orderNo = ''
  queryParams.tenantId = undefined
  queryParams.faultType = ''
  queryParams.orderStatus = ''
  queryParams.source = ''
  queryParams.pageNo = 1
  getList()
}

// 新建工单
const createVisible = ref(false)
const createFormRef = ref()
const createForm = reactive({
  tenantId: undefined as number | undefined,
  deviceId: undefined as number | undefined,
  deviceName: '',
  deviceCode: '',
  faultType: '',
  faultDescription: '',
  urgencyLevel: 'normal',
  deadline: undefined as any
})

const createRules = {
  tenantId: [{ required: true, message: '请选择所属租户', trigger: 'change' }],
  faultType: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  faultDescription: [{ required: true, message: '请输入故障描述', trigger: 'blur' }]
}

const handleCreate = () => {
  // 企业用户自动填充自己的租户
  if (!isAdmin.value) {
    const tenantId = getTenantId()
    createForm.tenantId = tenantId || undefined
    if (tenantId) {
      loadDeviceList(tenantId)
    }
  } else {
    createForm.tenantId = undefined
  }
  createForm.deviceId = undefined
  createForm.deviceName = ''
  createForm.deviceCode = ''
  createForm.faultType = ''
  createForm.faultDescription = ''
  createForm.urgencyLevel = 'normal'
  createForm.deadline = undefined
  createVisible.value = true
}

const resetCreateForm = () => {
  createFormRef.value?.resetFields()
}

const onTenantChange = (val: any) => {
  // 切换租户后加载对应租户的设备
  createForm.deviceId = undefined
  createForm.deviceName = ''
  createForm.deviceCode = ''
  if (val) {
    loadDeviceList(val)
  } else {
    deviceList.value = []
  }
}

const onDeviceChange = (val: any) => {
  // 选择设备后自动填充设备名称和编码
  const device = deviceList.value.find(d => d.id === val)
  if (device) {
    createForm.deviceName = device.name
    createForm.deviceCode = device.dn
  } else {
    createForm.deviceName = ''
    createForm.deviceCode = ''
  }
}

const submitCreate = async () => {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    const data = {
      tenantId: createForm.tenantId,
      deviceId: createForm.deviceId,
      deviceName: createForm.deviceName,
      deviceCode: createForm.deviceCode,
      faultType: createForm.faultType,
      faultDescription: createForm.faultDescription,
      urgencyLevel: createForm.urgencyLevel,
      deadline: createForm.deadline || undefined
    }
    await createRepairOrder(data)
    ElMessage.success('创建维修工单成功')
    createVisible.value = false
    getList()
  } catch (e) {
    console.error('创建失败', e)
  }
}

// 派单
const assignVisible = ref(false)
const assignForm = reactive({
  id: undefined as number | undefined,
  deadline: undefined as any,
  assignee: ''
})
const currentOrder = ref<any>({})

const handleAssign = (row: any) => {
  currentOrder.value = row
  assignForm.id = row.id
  assignForm.deadline = undefined
  assignVisible.value = true
}

const submitAssign = async () => {
  if (!assignForm.id) return
  try {
    const data: any = { id: assignForm.id }
    if (assignForm.deadline) {
      data.deadline = assignForm.deadline
    }
    await assignRepairOrder(data)
    ElMessage.success('派单成功，指派人已自动设置为租户联系人')
    assignVisible.value = false
    getList()
  } catch (e) {
    console.error('派单失败', e)
  }
}

// 完成维修（含图片上传）
const completeVisible = ref(false)
const completeForm = reactive({
  id: undefined as number | undefined,
  completeRemark: ''
})
const completeFileList = ref<any[]>([])

const handleExceed = () => {
  ElMessage.warning('最多上传9张图片')
}

const handleComplete = (row: any) => {
  completeForm.id = row.id
  completeForm.completeRemark = ''
  completeFileList.value = []
  completeVisible.value = true
}

const submitComplete = async () => {
  if (!completeForm.id) return
  try {
    let completeImages = undefined
    if (completeFileList.value.length > 0) {
      const urls: string[] = []
      for (const file of completeFileList.value) {
        if (!file.raw) continue
        // auto-upload=false，所有文件都未上传，统一上传到腾讯云 COS
        const formData = new FormData()
        formData.append('file', file.raw)
        const res = await updateFile(formData)
        const url = (res && typeof res === 'object') ? (res.data || res.url || '') : res
        if (url) urls.push(url)
      }
      if (urls.length > 0) {
        completeImages = JSON.stringify(urls)
      }
    }
    await completeRepairOrder(completeForm.id, completeForm.completeRemark || undefined, completeImages)
    ElMessage.success('完成维修成功')
    completeVisible.value = false
    getList()
  } catch (e) {
    console.error('完成维修失败', e)
  }
}

// 关闭工单
const handleClose = (row: any) => {
  ElMessageBox.confirm('确认关闭该维修工单？', '提示', {
    type: 'warning',
    confirmButtonText: '确认关闭',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await closeRepairOrder(row.id)
      ElMessage.success('关闭工单成功')
      getList()
    } catch (e) {
      console.error('关闭失败', e)
    }
  }).catch(() => {})
}

// 从告警生成（弹窗选择告警记录）
const alertSelectVisible = ref(false)
const alertRecords = ref<any[]>([])
const alertLoading = ref(false)
const selectedAlertId = ref<number | undefined>(undefined)
const alertPageNo = ref(1)
const alertPageSize = ref(10)
const alertTotal = ref(0)

const loadAlertRecords = async () => {
  alertLoading.value = true
  try {
    const res = await getAlertRecordPage({ pageNo: alertPageNo.value, pageSize: alertPageSize.value })
    if (res) {
      alertRecords.value = res.list || []
      alertTotal.value = res.total || 0
    }
  } catch (e) {
    console.error('获取告警记录失败', e)
  } finally {
    alertLoading.value = false
  }
}

const handleCreateFromAlert = async () => {
  selectedAlertId.value = undefined
  alertPageNo.value = 1
  alertSelectVisible.value = true
  await loadAlertRecords()
}

const handleAlertPageChange = (page: number) => {
  alertPageNo.value = page
  loadAlertRecords()
}

const handleAlertSizeChange = (size: number) => {
  alertPageSize.value = size
  alertPageNo.value = 1
  loadAlertRecords()
}

const onSelectAlertRow = (row: any) => {
  selectedAlertId.value = row.id
}

const submitFromAlert = async () => {
  if (!selectedAlertId.value) {
    ElMessage.warning('请选择一条告警记录')
    return
  }
  try {
    const res = await createRepairOrderFromAlert(selectedAlertId.value)
    ElMessage.success(`从告警生成维修工单成功，工单ID：${res}`)
    alertSelectVisible.value = false
    getList()
  } catch (e) {
    console.error('从告警生成失败', e)
  }
}

// 查看详情
const detailVisible = ref(false)
const detailData = ref<any>({})

const handleView = async (row: any) => {
  try {
    const res = await getRepairOrder(row.id)
    if (res) {
      detailData.value = res
      detailVisible.value = true
    }
  } catch (e) {
    console.error('获取详情失败', e)
  }
}

// 检查是否超期
const isOverdue = (deadline: string) => {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

onMounted(() => {
  getList()
  loadTenantList()
})
</script>

<style scoped>
.repair-order-container {
  padding: 0;
}

.search-form {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.action-bar {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.select-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.empty-circle {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid #dcdfe6;
  display: inline-block;
}
</style>
