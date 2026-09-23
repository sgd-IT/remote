<template>
  <div class="tab-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" inline class="search-form">
      <el-form-item label="合同编号">
        <el-input v-model="queryParams.contractNo" placeholder="合同编号" clearable style="width:160px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="合同名称">
        <el-input v-model="queryParams.contractName" placeholder="合同名称" clearable style="width:160px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="维保单位">
        <el-select v-model="queryParams.maintenanceUnit" placeholder="维保单位" clearable filterable style="width:160px">
          <el-option v-for="item in unitOptions" :key="item.id" :label="item.unitName" :value="item.unitName" />
        </el-select>
      </el-form-item>
      <el-form-item label="合同状态">
        <el-select v-model="queryParams.contractStatus" placeholder="合同状态" clearable style="width:130px">
          <el-option label="有效" value="valid" />
          <el-option label="已到期" value="expired" />
          <el-option label="续签中" value="renewing" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="action-bar">
      <el-button type="primary" icon="Plus" @click="handleCreate">新建合同</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="contractNo" label="合同编号" width="180" />
      <el-table-column prop="contractName" label="合同名称" min-width="160" />
      <el-table-column prop="maintenanceUnit" label="维保单位" width="150" />
      <el-table-column prop="propertyName" label="所属企业" width="150" />
      <el-table-column prop="startDate" label="开始日期" width="100">
        <template #default="{ row }">{{ formatDate(row.startDate, 'YYYY-MM-DD') }}</template>
      </el-table-column>
      <el-table-column prop="endDate" label="结束日期" width="100">
        <template #default="{ row }">{{ formatDate(row.endDate, 'YYYY-MM-DD') }}</template>
      </el-table-column>
      <el-table-column prop="contractStatusName" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.contractStatus === 'valid' ? 'success' : row.contractStatus === 'expired' ? 'danger' : 'warning'" size="small" effect="dark">
            {{ row.contractStatusName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button link type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="queryParams.pageNo" v-model:page-size="queryParams.pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="getList" />
    </div>

    <!-- 新建/编辑合同对话框 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑合同' : '新建合同'" width="600px">
      <el-form ref="formRef" :model="form" label-width="110px">
        <el-form-item label="合同编号" prop="contractNo"><el-input v-model="form.contractNo" placeholder="自动生成" disabled /></el-form-item>
        <el-form-item label="合同名称" prop="contractName"><el-input v-model="form.contractName" placeholder="请输入合同名称" /></el-form-item>
        <el-form-item label="维保单位" prop="maintenanceUnit">
          <el-select v-model="form.maintenanceUnit" placeholder="请选择维保单位" clearable filterable style="width:100%" @change="handleUnitChange">
            <el-option v-for="item in unitOptions" :key="item.id" :label="item.unitName" :value="item.unitName" />
          </el-select>
        </el-form-item>
        <el-form-item label="维保联系人" prop="maintenanceContact"><el-input v-model="form.maintenanceContact" placeholder="选择维保单位后自动填充" :disabled="true" /></el-form-item>
        <el-form-item label="维保电话" prop="maintenancePhone"><el-input v-model="form.maintenancePhone" placeholder="选择维保单位后自动填充" :disabled="true" /></el-form-item>
        <el-form-item label="开始日期" prop="startDate"><el-date-picker v-model="form.startDate" type="date" style="width:100%" /></el-form-item>
        <el-form-item label="结束日期" prop="endDate"><el-date-picker v-model="form.endDate" type="date" style="width:100%" /></el-form-item>
        <el-form-item label="合同金额(元)" prop="contractAmount"><el-input-number v-model="form.contractAmount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="合同文件">
          <UploadFile v-model="form.contractFile" :file-type="['pdf']" :limit="1" :is-show-tip="true" :auto-upload="true" />
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSave">确认</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewVisible" title="合同详情" width="650px">
      <el-descriptions :column="2" border v-if="viewData">
        <el-descriptions-item label="合同编号" :span="2">{{ viewData.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="合同名称" :span="2">{{ viewData.contractName }}</el-descriptions-item>
        <el-descriptions-item label="维保单位">{{ viewData.maintenanceUnit }}</el-descriptions-item>
        <el-descriptions-item label="所属企业">{{ viewData.propertyName }}</el-descriptions-item>
        <el-descriptions-item label="维保联系人">{{ viewData.maintenanceContact }}</el-descriptions-item>
        <el-descriptions-item label="维保电话">{{ viewData.maintenancePhone }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ formatDate(viewData.startDate, 'YYYY-MM-DD') }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ formatDate(viewData.endDate, 'YYYY-MM-DD') }}</el-descriptions-item>
        <el-descriptions-item label="合同金额(元)">{{ viewData.contractAmount != null ? viewData.contractAmount.toFixed(2) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.contractStatus === 'valid' ? 'success' : viewData.contractStatus === 'expired' ? 'danger' : 'warning'" size="small" effect="dark">
            {{ viewData.contractStatusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewData.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="viewData.contractFile" label="合同文件" :span="2">
          <el-link :href="viewData.contractFile" target="_blank" type="primary" :underline="false">
            <Icon icon="ep:download" style="margin-right: 4px;" />下载合同文件
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDate(viewData.createTime, 'YYYY-MM-DD HH:mm:ss') }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/formatTime'
import { getContractPage, createContract, updateContract, deleteContract, getContract } from '@/api/eiot/firewatch/firewatch.api'
import { getMaintenanceUnitList } from '@/api/eiot/firewatch/firewatch.api'

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  contractNo: '',
  contractName: '',
  maintenanceUnit: '',
  contractStatus: ''
})
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const unitOptions = ref<any[]>([])

const getList = async () => {
  loading.value = true
  try {
    const params: any = { pageNo: queryParams.pageNo, pageSize: queryParams.pageSize }
    if (queryParams.contractNo) params.contractNo = queryParams.contractNo
    if (queryParams.contractName) params.contractName = queryParams.contractName
    if (queryParams.maintenanceUnit) params.maintenanceUnit = queryParams.maintenanceUnit
    if (queryParams.contractStatus) params.contractStatus = queryParams.contractStatus
    const res = await getContractPage(params)
    if (res) { tableData.value = res.list || []; total.value = res.total || 0 }
  } finally { loading.value = false }
}

const handleSearch = () => {
  queryParams.pageNo = 1
  getList()
}

const handleReset = () => {
  queryParams.contractNo = ''
  queryParams.contractName = ''
  queryParams.maintenanceUnit = ''
  queryParams.contractStatus = ''
  queryParams.pageNo = 1
  getList()
}

// 维保单位切换：自动填充联系人和电话
const handleUnitChange = (unitName: string) => {
  const unit = unitOptions.value.find((u: any) => u.unitName === unitName)
  if (unit) {
    form.maintenanceContact = unit.contactPerson || ''
    form.maintenancePhone = unit.contactPhone || ''
  } else {
    form.maintenanceContact = ''
    form.maintenancePhone = ''
  }
}

// 加载维保单位列表
const loadUnitOptions = async () => {
  try {
    const res = await getMaintenanceUnitList()
    if (res) unitOptions.value = Array.isArray(res) ? res : []
  } catch (e) { console.error('加载维保单位列表失败', e) }
}

// ---------- 新建 / 编辑 ----------
const formVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = reactive({
  contractNo: '',
  contractName: '',
  maintenanceUnit: '',
  maintenanceContact: '',
  maintenancePhone: '',
  startDate: undefined as any,
  endDate: undefined as any,
  contractAmount: 0,
  contractFile: '',
  remark: ''
})
const formRef = ref()

const resetForm = () => {
  form.contractNo = 'CT-' + new Date().toISOString().slice(0,10).replace(/-/g,'') + '-' + Math.random().toString(36).substring(2,6).toUpperCase()
  form.contractName = ''
  form.maintenanceUnit = ''
  form.maintenanceContact = ''
  form.maintenancePhone = ''
  form.startDate = undefined
  form.endDate = undefined
  form.contractAmount = 0
  form.contractFile = ''
  form.remark = ''
}

const handleCreate = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  formVisible.value = true
}

const handleEdit = async (row: any) => {
  isEdit.value = true
  editId.value = row.id
  try {
    const res = await getContract(row.id)
    if (res) {
      form.contractNo = res.contractNo || ''
      form.contractName = res.contractName || ''
      form.maintenanceUnit = res.maintenanceUnit || ''
      form.maintenanceContact = res.maintenanceContact || ''
      form.maintenancePhone = res.maintenancePhone || ''
      form.startDate = res.startDate || undefined
      form.endDate = res.endDate || undefined
      form.contractAmount = res.contractAmount ?? 0
      form.contractFile = res.contractFile || ''
      form.remark = res.remark || ''
    }
  } catch (e) { console.error('获取合同详情失败', e) }
  formVisible.value = true
}

const submitSave = async () => {
  try {
    if (isEdit.value && editId.value) {
      await updateContract({ id: editId.value, ...form })
      ElMessage.success('更新成功')
    } else {
      // 打印要提交的表单数据
      console.log('[createContract] 提交创建合同表单数据:', JSON.parse(JSON.stringify({ ...form })))
      await createContract({ ...form })
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    getList()
  } catch (e: any) {
    console.error('[createContract] 保存失败，错误详情:', e)
    console.error('[createContract] 提交时的表单数据:', JSON.parse(JSON.stringify({ ...form })))
    // 尝试提取更深层的错误信息
    if (e?.response?.data) {
      console.error('[createContract] 服务端返回错误响应:', JSON.stringify(e.response.data))
    }
    if (e?.message) {
      console.error('[createContract] 错误消息:', e.message)
    }
  }
}

// ---------- 删除 ----------
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确认删除该合同？', '提示', { type: 'warning' }).then(async () => {
    await deleteContract(row.id)
    ElMessage.success('删除成功')
    getList()
  }).catch(() => {})
}

// ---------- 查看详情 ----------
const viewVisible = ref(false)
const viewData = ref<any>(null)

const handleView = async (row: any) => {
  try {
    const res = await getContract(row.id)
    if (res) {
      viewData.value = res
      viewVisible.value = true
    }
  } catch (e) { console.error(e) }
}

onMounted(() => {
  getList()
  loadUnitOptions()
})
</script>

<style scoped>
.tab-container { padding: 16px; }
.search-form { margin-bottom: 16px; }
.search-form .el-form-item { margin-bottom: 0; }
.action-bar { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
