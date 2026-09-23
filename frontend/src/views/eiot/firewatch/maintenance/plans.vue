<template>
  <div class="tab-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" inline class="search-form">
      <el-form-item label="计划编号">
        <el-input v-model="queryParams.planNo" placeholder="计划编号" clearable style="width:160px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="计划名称">
        <el-input v-model="queryParams.planName" placeholder="计划名称" clearable style="width:160px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="计划类型">
        <el-select v-model="queryParams.planType" placeholder="计划类型" clearable style="width:130px">
          <el-option label="月度维保" value="monthly" />
          <el-option label="季度维保" value="quarterly" />
          <el-option label="年度维保" value="yearly" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.planStatus" placeholder="状态" clearable style="width:130px">
          <el-option label="待执行" value="pending" />
          <el-option label="执行中" value="executing" />
          <el-option label="已完成" value="completed" />
          <el-option label="已逾期" value="overdue" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="action-bar">
      <el-button type="primary" icon="Plus" @click="handleCreate">新建计划</el-button>
      <el-button type="success" icon="Download" @click="handleExport">导出Excel</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="planNo" label="计划编号" width="170" />
      <el-table-column prop="planName" label="计划名称" min-width="160" />
      <el-table-column prop="propertyName" label="所属企业" width="150" />
      <el-table-column prop="executeUnit" label="执行单位" width="150" />
      <el-table-column prop="planDate" label="计划日期" width="100">
        <template #default="{ row }">{{ formatDate(row.planDate, 'YYYY-MM-DD') }}</template>
      </el-table-column>
      <el-table-column prop="planStatusName" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.planStatus === 'completed' ? 'success' : row.planStatus === 'executing' ? 'primary' : row.planStatus === 'overdue' ? 'danger' : 'warning'" size="small" effect="dark">
            {{ row.planStatusName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="executePerson" label="执行人" width="100" align="center" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button v-if="row.planStatus !== 'completed'" link type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.planStatus !== 'completed'" link type="success" size="small" @click="handleComplete(row)">完成</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="queryParams.pageNo" v-model:page-size="queryParams.pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="getList" />
    </div>

    <!-- 新建/编辑计划对话框 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑维保计划' : '新建维保计划'" width="550px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="计划编号" prop="planNo"><el-input v-model="form.planNo" placeholder="自动生成" disabled /></el-form-item>
        <el-form-item label="计划名称" prop="planName"><el-input v-model="form.planName" placeholder="请输入计划名称" /></el-form-item>
        <el-form-item label="计划类型">
          <el-select v-model="form.planType" style="width:100%">
            <el-option label="月度维保" value="monthly" />
            <el-option label="季度维保" value="quarterly" />
            <el-option label="年度维保" value="yearly" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划日期"><el-date-picker v-model="form.planDate" type="date" style="width:100%" /></el-form-item>
        <el-form-item label="执行单位">
          <el-select v-model="form.executeUnit" placeholder="请选择执行单位" clearable filterable style="width:100%" @change="handleUnitChange">
            <el-option v-for="item in unitOptions" :key="item.id" :label="item.unitName" :value="item.unitName" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行人"><el-input v-model="form.executePerson" placeholder="选择执行单位后自动填充" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSave">确认</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewVisible" title="计划详情" width="600px">
      <el-descriptions :column="2" border v-if="viewData">
        <el-descriptions-item label="计划编号" :span="2">{{ viewData.planNo }}</el-descriptions-item>
        <el-descriptions-item label="计划名称" :span="2">{{ viewData.planName }}</el-descriptions-item>
        <el-descriptions-item label="所属企业">{{ viewData.propertyName }}</el-descriptions-item>
        <el-descriptions-item label="计划类型">{{ viewData.planTypeName || planTypeMap[viewData.planType] }}</el-descriptions-item>
        <el-descriptions-item label="执行单位" :span="2">{{ viewData.executeUnit }}</el-descriptions-item>
        <el-descriptions-item label="执行人">{{ viewData.executePerson }}</el-descriptions-item>
        <el-descriptions-item label="计划日期">{{ formatDate(viewData.planDate, 'YYYY-MM-DD') }}</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="viewData.planStatus === 'completed' ? 'success' : viewData.planStatus === 'executing' ? 'primary' : viewData.planStatus === 'overdue' ? 'danger' : 'warning'" size="small" effect="dark">
            {{ viewData.planStatusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewData.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDate(viewData.createTime, 'YYYY-MM-DD HH:mm:ss') }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/formatTime'
import { getPlanPage, createPlan, updatePlan, completePlan, deletePlan, getPlan } from '@/api/eiot/firewatch/firewatch.api'
import { getMaintenanceUnitList, exportPlanExcel } from '@/api/eiot/firewatch/firewatch.api'
import download from '@/utils/download'

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  planNo: '',
  planName: '',
  planType: '',
  planStatus: ''
})
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const unitOptions = ref<any[]>([])

const planTypeMap: Record<string, string> = {
  monthly: '月度维保',
  quarterly: '季度维保',
  yearly: '年度维保'
}

const getList = async () => {
  loading.value = true
  try {
    const params: any = { pageNo: queryParams.pageNo, pageSize: queryParams.pageSize }
    if (queryParams.planNo) params.planNo = queryParams.planNo
    if (queryParams.planName) params.planName = queryParams.planName
    if (queryParams.planType) params.planType = queryParams.planType
    if (queryParams.planStatus) params.planStatus = queryParams.planStatus
    const res = await getPlanPage(params)
    if (res) { tableData.value = res.list || []; total.value = res.total || 0 }
  } finally { loading.value = false }
}

const handleSearch = () => {
  queryParams.pageNo = 1
  getList()
}

const handleReset = () => {
  queryParams.planNo = ''
  queryParams.planName = ''
  queryParams.planType = ''
  queryParams.planStatus = ''
  queryParams.pageNo = 1
  getList()
}

// ---------- 新建 / 编辑 ----------
const formVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = reactive({
  planNo: '',
  planName: '',
  planType: 'monthly',
  planDate: undefined as any,
  executeUnit: '',
  executePerson: '',
  remark: ''
})
const formRef = ref()

// 执行单位选择变化，自动填充执行人
const handleUnitChange = (unitName: string) => {
  const unit = unitOptions.value.find((u: any) => u.unitName === unitName)
  if (unit) {
    form.executePerson = unit.contactPerson || ''
  } else {
    form.executePerson = ''
  }
}

const resetForm = () => {
  form.planNo = 'PL-' + new Date().toISOString().slice(0,10).replace(/-/g,'') + '-' + Math.random().toString(36).substring(2,6).toUpperCase()
  form.planName = ''
  form.planType = 'monthly'
  form.planDate = undefined
  form.executeUnit = ''
  form.executePerson = ''
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
    const res = await getPlan(row.id)
    if (res) {
      form.planNo = res.planNo || ''
      form.planName = res.planName || ''
      form.planType = res.planType || 'monthly'
      form.planDate = res.planDate || undefined
      form.executeUnit = res.executeUnit || ''
      form.executePerson = res.executePerson || ''
      form.remark = res.remark || ''
    }
  } catch (e) { console.error('获取计划详情失败', e) }
  formVisible.value = true
}

const submitSave = async () => {
  try {
    if (isEdit.value && editId.value) {
      await updatePlan({ id: editId.value, ...form })
      ElMessage.success('更新成功')
    } else {
      // 打印要提交的表单数据
      console.log('[createPlan] 提交创建维保计划表单数据:', JSON.parse(JSON.stringify({ ...form })))
      await createPlan({ ...form })
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    getList()
  } catch (e: any) {
    console.error('[createPlan] 保存失败，错误详情:', e)
    console.error('[createPlan] 提交时的表单数据:', JSON.parse(JSON.stringify({ ...form })))
    // 尝试提取更深层的错误信息
    if (e?.response?.data) {
      console.error('[createPlan] 服务端返回错误响应:', JSON.stringify(e.response.data))
    }
    if (e?.message) {
      console.error('[createPlan] 错误消息:', e.message)
    }
  }
}

// ---------- 完成 ----------
const handleComplete = async (row: any) => {
  try {
    await completePlan(row.id)
    ElMessage.success('已完成')
    getList()
  } catch (e) { console.error('完成失败', e) }
}

// ---------- 删除 ----------
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确认删除该计划？', '提示', { type: 'warning' }).then(async () => {
    await deletePlan(row.id)
    ElMessage.success('删除成功')
    getList()
  }).catch(() => {})
}

// ---------- 查看详情 ----------
const viewVisible = ref(false)
const viewData = ref<any>(null)

const handleView = async (row: any) => {
  try {
    const res = await getPlan(row.id)
    if (res) {
      viewData.value = res
      viewVisible.value = true
    }
  } catch (e) { console.error(e) }
}

// 导出Excel
const handleExport = async () => {
  try {
    ElMessage.info('正在导出维保计划...')
    const params: any = {}
    if (queryParams.planNo) params.planNo = queryParams.planNo
    if (queryParams.planName) params.planName = queryParams.planName
    if (queryParams.planType) params.planType = queryParams.planType
    if (queryParams.planStatus) params.planStatus = queryParams.planStatus
    const res = await exportPlanExcel(params)
    download.excel(res as any, `维保计划_${new Date().toLocaleDateString()}.xls`)
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出失败', e)
    ElMessage.error('导出失败')
  }
}

// 加载维保单位列表（用于执行单位下拉）
const loadUnitOptions = async () => {
  try {
    const res = await getMaintenanceUnitList()
    if (res) unitOptions.value = Array.isArray(res) ? res : []
  } catch (e) { console.error('加载维保单位列表失败', e) }
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
