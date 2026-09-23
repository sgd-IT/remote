<template>
  <div class="repair-orders-tab">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" inline class="search-form">
      <el-form-item label="工单编号">
        <el-input v-model="queryParams.orderNo" placeholder="工单编号" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="所属企业" v-if="isAdmin">
        <el-select v-model="queryParams.tenantId" placeholder="所属企业" clearable style="width: 160px">
          <el-option v-for="item in tenantList" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.orderStatus" placeholder="状态" clearable style="width: 120px">
          <el-option label="待处理" value="pending" />
          <el-option label="已派单" value="assigned" />
          <el-option label="维护中" value="in_progress" />
          <el-option label="已完成" value="completed" />
          <el-option label="已关闭" value="closed" />
        </el-select>
      </el-form-item>
      <el-form-item label="报修来源">
        <el-select v-model="queryParams.reportSource" placeholder="报修来源" clearable style="width: 130px">
          <el-option label="平台推送" value="platform" />
          <el-option label="电话通知" value="phone" />
          <el-option label="手动登记" value="manual" />
        </el-select>
      </el-form-item>
      <el-form-item label="维保人">
        <el-input v-model="queryParams.repairPerson" placeholder="维保人" clearable style="width: 130px" />
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
      <el-table-column prop="orderNo" label="工单号" width="170" />
      <el-table-column prop="propertyName" label="故障单位" width="160" />
      <el-table-column prop="deviceName" label="故障设备" width="150" />
      <el-table-column prop="faultDescription" label="故障描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sourceName" label="报修来源" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.source === 'auto-alert' ? 'danger' : 'primary'" size="small">
            {{ row.sourceName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderStatusName" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag
            v-if="row.orderStatus === 'in_progress'"
            size="small"
            effect="dark"
            color="#9b59b6"
          >
            {{ row.orderStatusName }}
          </el-tag>
          <el-tag
            v-else
            :type="statusTagType(row.orderStatus)"
            size="small"
            effect="dark"
          >
            {{ row.orderStatusName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="repairPerson" label="维保人" width="100" align="center">
        <template #default="{ row }">
          <span>{{ row.repairPerson || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="deadline" label="截止日期" width="170">
        <template #default="{ row }">
          <span
            v-if="row.deadline"
            :style="{
              color: isOverdue(row.deadline) && !['completed', 'closed'].includes(row.orderStatus) ? '#f56c6c' : '',
              fontWeight: isOverdue(row.deadline) && !['completed', 'closed'].includes(row.orderStatus) ? 'bold' : 'normal'
            }"
          >
            {{ formatDate(row.deadline) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button v-if="row.orderStatus === 'pending'" link type="primary" size="small" @click="handleSign(row)">签单</el-button>
          <el-button v-if="row.orderStatus === 'assigned'" link type="warning" size="small" @click="handleStart(row)">开始维修</el-button>
          <el-button v-if="row.orderStatus === 'in_progress'" link type="success" size="small" @click="handleComplete(row)">完成</el-button>
          <el-button v-if="row.orderStatus === 'completed'" link type="info" size="small" @click="handleRate(row)">评价</el-button>
          <el-button v-if="!['closed', 'completed'].includes(row.orderStatus)" link type="danger" size="small" @click="handleClose(row)">关闭</el-button>
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

    <!-- ============ 新建工单对话框 ============ -->
    <el-dialog v-model="createVisible" title="新建维修工单" width="600px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" label-width="100px" :rules="createRules">
        <el-form-item label="所属企业" prop="tenantId" v-if="isAdmin">
          <el-select v-model="createForm.tenantId" placeholder="请选择企业" style="width: 100%">
            <el-option v-for="item in tenantList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备" prop="deviceName">
          <el-select v-model="createForm.deviceName" placeholder="请选择设备" clearable filterable style="width: 100%" @change="handleDeviceChange">
            <el-option v-for="item in deviceOptions" :key="item.id" :label="item.name" :value="item.name">
              <span>{{ item.name }}</span>
              <span style="float: right; color: #909399; font-size: 12px; margin-left: 10px">{{ item.dn }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input v-model="createForm.deviceCode" placeholder="选择设备后自动填充" :disabled="true" style="width: 100%" />
        </el-form-item>
        <el-form-item label="故障类型" prop="faultType">
          <el-select v-model="createForm.faultType" placeholder="请选择故障类型" clearable style="width: 100%">
            <el-option label="电源故障" value="电源故障" />
            <el-option label="传感器故障" value="传感器故障" />
            <el-option label="通信故障" value="通信故障" />
            <el-option label="设备损坏" value="设备损坏" />
            <el-option label="线路故障" value="线路故障" />
            <el-option label="其他故障" value="其他故障" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDescription">
          <el-input
            v-model="createForm.faultDescription"
            type="textarea"
            :rows="3"
            placeholder="请描述故障情况"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="紧急程度" prop="urgencyLevel">
          <el-select v-model="createForm.urgencyLevel" placeholder="请选择紧急程度" style="width: 100%">
            <el-option label="普通" value="normal" />
            <el-option label="紧急" value="urgent" />
            <el-option label="非常紧急" value="very_urgent" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期" prop="deadline">
          <el-date-picker
            v-model="createForm.deadline"
            type="datetime"
            placeholder="选择截止日期"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="x"
          />
        </el-form-item>
        <el-form-item label="报修来源" prop="reportSource">
          <el-select v-model="createForm.reportSource" placeholder="请选择报修来源" style="width: 100%">
            <el-option label="平台推送" value="platform" />
            <el-option label="电话通知" value="phone" />
            <el-option label="手动登记" value="manual" />
          </el-select>
        </el-form-item>
        <el-form-item label="维保单位">
          <el-select v-model="createForm.repairCompany" placeholder="请选择维保单位" clearable filterable style="width: 100%" @change="handleUnitChange">
            <el-option v-for="item in unitOptions" :key="item.id" :label="item.unitName" :value="item.unitName" />
          </el-select>
        </el-form-item>
        <el-form-item label="维保人">
          <el-input v-model="createForm.repairPerson" placeholder="选择维保单位后自动填充" :disabled="true" style="width: 100%" />
        </el-form-item>
        <el-form-item label="维保人电话">
          <el-input v-model="createForm.repairPersonPhone" placeholder="选择维保单位后自动填充" :disabled="true" style="width: 100%" />
        </el-form-item>
        <el-form-item label="故障图片">
          <UploadImgs v-model="createForm.faultImages" :limit="9" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="submitLoading">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- ============ 完成维修对话框 ============ -->
    <el-dialog v-model="completeVisible" title="完成维修" width="500px" :close-on-click-modal="false">
      <el-form :model="completeForm" label-width="100px">
        <el-form-item label="维修备注">
          <el-input
            v-model="completeForm.completeRemark"
            type="textarea"
            :rows="4"
            placeholder="请输入维修备注（选填）"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplete" :loading="submitLoading">确认完成</el-button>
      </template>
    </el-dialog>

    <!-- ============ 评价工单对话框 ============ -->
    <el-dialog v-model="rateVisible" title="评价工单" width="500px" :close-on-click-modal="false">
      <el-form :model="rateForm" label-width="100px">
        <el-form-item label="满意度">
          <el-rate v-model="rateForm.satisfaction" :max="5" show-score style="margin-top: 8px" />
        </el-form-item>
        <el-form-item label="建议意见">
          <el-input
            v-model="rateForm.suggestion"
            type="textarea"
            :rows="4"
            placeholder="请输入建议或意见（选填）"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rateVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRate" :loading="submitLoading">提交评价</el-button>
      </template>
    </el-dialog>

    <!-- ============ 查看详情对话框 ============ -->
    <el-dialog v-model="detailVisible" title="工单详情" width="650px" :close-on-click-modal="false">
      <el-descriptions :column="2" border v-if="detailData.id">
        <el-descriptions-item label="工单号" :span="2">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="故障单位">{{ detailData.propertyName }}</el-descriptions-item>
        <el-descriptions-item label="故障设备">{{ detailData.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ detailData.faultDescription }}</el-descriptions-item>
        <el-descriptions-item label="故障类型">{{ detailData.faultType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag v-if="detailData.urgencyLevel === 'very_urgent'" type="danger" size="small">非常紧急</el-tag>
          <el-tag v-else-if="detailData.urgencyLevel === 'urgent'" type="warning" size="small">紧急</el-tag>
          <el-tag v-else type="info" size="small">普通</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报修来源">{{ detailData.sourceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工单状态">
          <el-tag :type="statusTagType(detailData.orderStatus)" size="small" effect="dark">
            {{ detailData.orderStatusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="维保人">{{ detailData.repairPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维保单位">{{ detailData.repairCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维保人电话" :span="2">{{ detailData.repairPersonPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="截止日期">
          <span v-if="detailData.deadline" :style="{ color: isOverdue(detailData.deadline) ? '#f56c6c' : '' }">
            {{ formatDate(detailData.deadline) }}
          </span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detailData.completeTime ? formatDate(detailData.completeTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.faultImages && detailData.faultImages.length > 0" label="故障图片" :span="2">
          <div class="detail-images">
            <el-image
              v-for="(img, idx) in detailData.faultImages"
              :key="idx"
              :src="img"
              :preview-src-list="detailData.faultImages"
              fit="cover"
              style="width: 80px; height: 80px; margin-right: 8px; border-radius: 4px; cursor: pointer;"
            />
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="维修备注" :span="2">{{ detailData.completeRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="满意度" v-if="detailData.satisfaction != null">
          <el-rate v-model="detailData.satisfaction" :max="5" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="评价建议" v-if="detailData.suggestion" :span="2">{{ detailData.suggestion }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ============ 从告警生成对话框 ============ -->
    <el-dialog v-model="alertSelectVisible" title="选择告警记录生成工单" width="800px" :close-on-click-modal="false">
      <el-table :data="alertRecords" v-loading="alertLoading" stripe border style="width: 100%" @row-click="handleAlertRowClick">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="alertTime" label="告警时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.alertTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="propertyName" label="所属单位" width="150" />
        <el-table-column prop="alertContent" label="告警内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="alertLevel" label="告警级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.alertLevel === 'critical' ? 'danger' : row.alertLevel === 'high' ? 'warning' : 'info'" size="small">
              {{ row.alertLevelName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'danger' : 'success'" size="small">
              {{ row.status === 0 ? '未处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="alertSelectVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFromAlert" :disabled="!selectedAlertId" :loading="submitLoading">生成工单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/formatTime'
import {
  getRepairOrderPage, createRepairOrder, signRepairOrder,
  startRepairOrder, completeRepairOrder, closeRepairOrder,
  getRepairOrder, rateRepairOrder, createRepairOrderFromAlert,
  getAlertRecordPage, getMaintenanceUnitList
} from '@/api/eiot/firewatch/firewatch.api'
import { getTenantPage } from '@/api/system/tenant'
import { getDevicesList } from '@/api/eiot/deviceinfo/devices.api'
import { getTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles?.includes('super_admin'))

const queryParams = reactive({
  pageNo: 1, pageSize: 10,
  orderNo: '', tenantId: undefined as number | undefined,
  orderStatus: '', reportSource: '', repairPerson: ''
})

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const tenantList = ref<any[]>([])

const statusTagType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    assigned: 'primary',
    in_progress: 'primary',
    completed: 'success',
    closed: 'info'
  }
  return map[status] || 'info'
}

const isOverdue = (deadline: string) => {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

const getList = async () => {
  loading.value = true
  try {
    const params: any = { pageNo: queryParams.pageNo, pageSize: queryParams.pageSize }
    if (queryParams.orderNo) params.orderNo = queryParams.orderNo
    if (queryParams.tenantId) params.tenantId = queryParams.tenantId
    if (queryParams.orderStatus) params.orderStatus = queryParams.orderStatus
    if (queryParams.reportSource) params.reportSource = queryParams.reportSource
    if (queryParams.repairPerson) params.repairPerson = queryParams.repairPerson
    const res = await getRepairOrderPage(params)
    if (res) {
      tableData.value = res.list || []
      total.value = res.total || 0
    }
  } catch (e) {
    console.error('获取列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => { queryParams.pageNo = 1; getList() }
const handleReset = () => {
  queryParams.orderNo = ''
  queryParams.tenantId = undefined
  queryParams.orderStatus = ''
  queryParams.reportSource = ''
  queryParams.repairPerson = ''
  queryParams.pageNo = 1
  getList()
}

// 加载租户列表
const loadTenantList = async () => {
  if (!isAdmin.value) {
    const tenantId = getTenantId()
    if (tenantId) tenantList.value = [{ id: tenantId, name: '当前企业' }]
    return
  }
  try {
    const res = await getTenantPage({ pageNo: 1, pageSize: 100 })
    if (res) tenantList.value = res.list || []
  } catch (e) { console.error('获取租户列表失败', e) }
}

// ========== 新建工单 ==========
const createVisible = ref(false)
const createFormRef = ref()
const createForm = reactive({
  tenantId: undefined as number | undefined,
  deviceId: undefined as number | undefined,
  deviceName: '',
  deviceCode: '',
  faultType: '',
  faultDescription: '',
  faultImages: [] as string[],
  urgencyLevel: 'normal',
  deadline: undefined as any,
  reportSource: 'manual',
  repairCompany: '',
  repairPerson: '',
  repairPersonPhone: ''
})
const createRules = {
  tenantId: [{ required: true, message: '请选择所属企业', trigger: 'change' }],
  faultDescription: [{ required: true, message: '请输入故障描述', trigger: 'blur' }]
}

// 设备下拉数据
const deviceOptions = ref<any[]>([])

// 维保单位下拉数据
const unitOptions = ref<any[]>([])

const loadUnitOptions = async () => {
  try {
    const res = await getMaintenanceUnitList()
    if (res) unitOptions.value = Array.isArray(res) ? res : []
  } catch (e) { console.error('获取维保单位列表失败', e) }
}

// 维保单位选择变化
const handleUnitChange = (unitName: string) => {
  const unit = unitOptions.value.find((u: any) => u.unitName === unitName)
  if (unit) {
    createForm.repairPerson = unit.contactPerson || ''
    createForm.repairPersonPhone = unit.contactPhone || ''
  } else {
    createForm.repairPerson = ''
    createForm.repairPersonPhone = ''
  }
}

const loadDeviceList = async () => {
  try {
    const params: any = { pageNo: 1, pageSize: 100 }
    if (isAdmin.value) {
      if (createForm.tenantId) params.tenantId = createForm.tenantId
    } else {
      const tenantId = getTenantId()
      if (tenantId) params.tenantId = tenantId
    }
    const res = await getDevicesList(params)
    if (res) deviceOptions.value = res.list || []
  } catch (e) { console.error('获取设备列表失败', e) }
}

// 设备选择变化
const handleDeviceChange = (deviceName: string) => {
  const device = deviceOptions.value.find((d: any) => d.name === deviceName)
  if (device) {
    createForm.deviceId = device.id
    createForm.deviceCode = device.dn || ''
  } else {
    createForm.deviceId = undefined
    createForm.deviceCode = ''
  }
}

// 当管理员切换企业时，重新加载设备列表
watch(() => createForm.tenantId, (newVal) => {
  if (createVisible.value && isAdmin.value) {
    loadDeviceList()
  }
})

const handleCreate = () => {
  if (!isAdmin.value) {
    createForm.tenantId = getTenantId() || undefined
  } else {
    createForm.tenantId = undefined
  }
  createForm.deviceId = undefined
  createForm.deviceName = ''
  createForm.deviceCode = ''
  createForm.faultType = ''
  createForm.faultDescription = ''
  createForm.faultImages = []
  createForm.urgencyLevel = 'normal'
  createForm.deadline = undefined
  createForm.reportSource = 'manual'
  createForm.repairCompany = ''
  createForm.repairPerson = ''
  createForm.repairPersonPhone = ''
  createVisible.value = true
  loadDeviceList()
  loadUnitOptions()
}

const submitCreate = async () => {
  submitLoading.value = true
  try {
    const data: any = {
      tenantId: createForm.tenantId,
      deviceId: createForm.deviceId,
      deviceName: createForm.deviceName || undefined,
      deviceCode: createForm.deviceCode || undefined,
      faultType: createForm.faultType || undefined,
      faultDescription: createForm.faultDescription,
      faultImages: createForm.faultImages.length > 0 ? JSON.stringify(createForm.faultImages) : undefined,
      urgencyLevel: createForm.urgencyLevel,
      deadline: createForm.deadline || undefined,
      reportSource: createForm.reportSource,
      repairPerson: createForm.repairPerson || undefined,
      repairCompany: createForm.repairCompany || undefined,
      repairPersonPhone: createForm.repairPersonPhone || undefined
    }
    await createRepairOrder(data)
    ElMessage.success('创建成功')
    createVisible.value = false
    getList()
  } catch (e) {
    console.error('创建失败', e)
    ElMessage.error('创建失败，请检查数据是否正确')
  } finally {
    submitLoading.value = false
  }
}

// ========== 签单 ==========
const handleSign = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认签收该维修工单？', '提示', {
      type: 'info',
      confirmButtonText: '确认签单',
      cancelButtonText: '取消'
    })
    await signRepairOrder(row.id)
    ElMessage.success('签单成功')
    getList()
  } catch (e: any) {
    if (e !== 'cancel') console.error('签单失败', e)
  }
}

// ========== 开始维修 ==========
const handleStart = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认开始维修该工单？', '提示', {
      type: 'warning',
      confirmButtonText: '确认开始',
      cancelButtonText: '取消'
    })
    await startRepairOrder(row.id)
    ElMessage.success('开始维修')
    getList()
  } catch (e: any) {
    if (e !== 'cancel') console.error('开始维修失败', e)
  }
}

// ========== 完成维修 ==========
const completeVisible = ref(false)
const completeForm = reactive({ id: undefined as number | undefined, completeRemark: '' })

const handleComplete = (row: any) => {
  completeForm.id = row.id
  completeForm.completeRemark = ''
  completeVisible.value = true
}

const submitComplete = async () => {
  if (!completeForm.id) return
  submitLoading.value = true
  try {
    await completeRepairOrder(completeForm.id, completeForm.completeRemark || undefined)
    ElMessage.success('完成维修成功')
    completeVisible.value = false
    getList()
  } catch (e) {
    console.error('完成失败', e)
  } finally {
    submitLoading.value = false
  }
}

// ========== 评价工单 ==========
const rateVisible = ref(false)
const rateForm = reactive({ id: undefined as number | undefined, satisfaction: 5, suggestion: '' })

const handleRate = (row: any) => {
  rateForm.id = row.id
  rateForm.satisfaction = 5
  rateForm.suggestion = ''
  rateVisible.value = true
}

const submitRate = async () => {
  if (!rateForm.id) return
  submitLoading.value = true
  try {
    await rateRepairOrder(rateForm.id, rateForm.satisfaction, rateForm.suggestion || undefined)
    ElMessage.success('评价成功')
    rateVisible.value = false
    getList()
  } catch (e) {
    console.error('评价失败', e)
  } finally {
    submitLoading.value = false
  }
}

// ========== 关闭工单 ==========
const handleClose = (row: any) => {
  ElMessageBox.confirm('确认关闭该维修工单？工单关闭后将无法进行后续操作。', '提示', {
    type: 'warning',
    confirmButtonText: '确认关闭',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await closeRepairOrder(row.id)
      ElMessage.success('关闭成功')
      getList()
    } catch (e) {
      console.error('关闭失败', e)
    }
  }).catch(() => {})
}

// ========== 查看详情 ==========
const detailVisible = ref(false)
const detailData = ref<any>({})

const handleView = async (row: any) => {
  try {
    const res = await getRepairOrder(row.id)
    if (res) {
      // 解析故障图片JSON字符串为数组
      if (typeof res.faultImages === 'string') {
        if (res.faultImages) {
          try {
            res.faultImages = JSON.parse(res.faultImages)
          } catch (e) {
            // 如果不是有效JSON，可能存的是单张图片URL
            res.faultImages = [res.faultImages]
          }
        } else {
          res.faultImages = []
        }
      }
      if (typeof res.completeImages === 'string') {
        if (res.completeImages) {
          try {
            res.completeImages = JSON.parse(res.completeImages)
          } catch (e) {
            res.completeImages = [res.completeImages]
          }
        } else {
          res.completeImages = []
        }
      }
      detailData.value = res
      detailVisible.value = true
    }
  } catch (e) {
    console.error('获取详情失败', e)
  }
}

// ========== 从告警生成 ==========
const alertSelectVisible = ref(false)
const alertRecords = ref<any[]>([])
const alertLoading = ref(false)
const selectedAlertId = ref<number | undefined>(undefined)

const handleCreateFromAlert = async () => {
  selectedAlertId.value = undefined
  alertSelectVisible.value = true
  alertLoading.value = true
  try {
    const res = await getAlertRecordPage({ pageNo: 1, pageSize: 50 })
    if (res) alertRecords.value = res.list || []
  } catch (e) {
    console.error('获取告警记录失败', e)
  } finally {
    alertLoading.value = false
  }
}

const handleAlertRowClick = (row: any) => {
  selectedAlertId.value = row.id
}

const submitFromAlert = async () => {
  if (!selectedAlertId.value) {
    ElMessage.warning('请选择一条告警记录')
    return
  }
  submitLoading.value = true
  try {
    await createRepairOrderFromAlert(selectedAlertId.value)
    ElMessage.success('生成工单成功')
    alertSelectVisible.value = false
    getList()
  } catch (e) {
    console.error('生成失败', e)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  getList()
  loadTenantList()
})
</script>

<style scoped>
.repair-orders-tab { padding: 16px; }
.search-form { background: #f5f7fa; padding: 16px; border-radius: 6px; margin-bottom: 16px; }
.action-bar { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
