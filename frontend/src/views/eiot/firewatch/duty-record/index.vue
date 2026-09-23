<template>
  <ContentWrap>
    <!-- 值班记录统计概览 -->
    <el-row :gutter="16" class="mb-10px">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="handleTypeFilter('')">
          <div class="stat-value" style="color: #409eff">{{ statistics.total || 0 }}</div>
          <div class="stat-label">全部记录</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="handleTypeFilter('patrol')">
          <div class="stat-value" style="color: #67c23a">{{ statistics.patrol || 0 }}</div>
          <div class="stat-label">巡查记录</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="handleTypeFilter('alarm')">
          <div class="stat-value" style="color: #e6a23c">{{ statistics.alarm || 0 }}</div>
          <div class="stat-label">告警处理</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="handleTypeFilter('handover')">
          <div class="stat-value" style="color: #909399">{{ statistics.handover || 0 }}</div>
          <div class="stat-label">交接班</div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="所属单位" prop="propertyId">
        <el-select v-model="queryParams.propertyId" placeholder="请选择单位" clearable style="width: 200px">
          <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="记录类型" prop="recordType">
        <el-select v-model="queryParams.recordType" placeholder="请选择" clearable style="width: 130px">
          <el-option label="巡查记录" value="patrol" />
          <el-option label="告警处理" value="alarm" />
          <el-option label="交接班" value="handover" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      <el-form-item label="记录时间" prop="recordTime">
        <el-date-picker
          v-model="queryParams.recordTime"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="记录人" prop="recorderName">
        <el-input v-model="queryParams.recorderName" placeholder="请输入记录人" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 120px">
          <el-option label="正常" value="normal" />
          <el-option label="异常" value="abnormal" />
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
    <el-row :gutter="10" class="mb-10px">
      <el-col :span="1.5">
        <el-button type="primary" @click="handleCreate"><Icon icon="ep:plus" class="mr-5px" />新增记录</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain @click="handleExport"><Icon icon="ep:download" class="mr-5px" />导出</el-button>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="所属单位" prop="propertyName" min-width="120" show-overflow-tooltip />
      <el-table-column label="记录时间" prop="recordTime" width="155" />
      <el-table-column label="记录类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getRecordTypeTag(row.recordType)" size="small">{{ row.recordTypeName || recordTypeMap[row.recordType] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="140" show-overflow-tooltip />
      <el-table-column label="内容" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="content-cell">{{ row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column label="关联设备" prop="deviceName" width="120" show-overflow-tooltip />
      <el-table-column label="记录人" prop="recorderName" width="90" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'abnormal' ? 'danger' : 'success'" size="small">
            {{ row.statusName || (row.status === 'normal' ? '正常' : '异常') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column label="创建时间" prop="createTime" width="155" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">查看</el-button>
          <el-button link type="primary" @click="handleUpdate(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="记录类型" prop="recordType">
            <el-select v-model="formData.recordType" placeholder="请选择" style="width: 100%">
              <el-option label="巡查记录" value="patrol" />
              <el-option label="告警处理" value="alarm" />
              <el-option label="交接班" value="handover" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="记录时间" prop="recordTime">
            <el-date-picker v-model="formData.recordTime" type="datetime" placeholder="请选择时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="记录标题" prop="title">
        <el-input v-model="formData.title" placeholder="请输入记录标题" maxlength="200" show-word-limit />
      </el-form-item>
      <el-form-item label="记录内容" prop="content">
        <el-input v-model="formData.content" type="textarea" placeholder="请输入记录内容" :rows="4" maxlength="2000" show-word-limit />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="关联设备" prop="deviceName">
            <el-input v-model="formData.deviceName" placeholder="请输入设备名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="记录人" prop="recorderName">
            <el-input v-model="formData.recorderName" placeholder="请输入记录人姓名" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio label="normal">正常</el-radio>
              <el-radio label="abnormal">异常</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" placeholder="请输入备注" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="关联排班" prop="scheduleId">
        <el-select v-model="formData.scheduleId" placeholder="选择关联排班(可选)" clearable style="width: 100%">
          <el-option v-for="item in scheduleList" :key="item.id" :label="`${item.scheduleDate} ${item.shiftTypeName} - ${item.dutyUserName}`" :value="item.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>

  <!-- 查看详情对话框 -->
  <el-dialog v-model="detailVisible" title="值班记录详情" width="700px" append-to-body>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="ID" :span="1">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="所属单位" :span="1">{{ detailData.propertyName }}</el-descriptions-item>
      <el-descriptions-item label="记录时间" :span="1">{{ detailData.recordTime }}</el-descriptions-item>
      <el-descriptions-item label="记录类型" :span="1">
        <el-tag :type="getRecordTypeTag(detailData.recordType)" size="small">
          {{ detailData.recordTypeName || recordTypeMap[detailData.recordType] }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="标题" :span="2">{{ detailData.title }}</el-descriptions-item>
      <el-descriptions-item label="内容" :span="2">
        <div class="detail-content">{{ detailData.content }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="关联设备" :span="1">{{ detailData.deviceName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="记录人" :span="1">{{ detailData.recorderName }}</el-descriptions-item>
      <el-descriptions-item label="状态" :span="1">
        <el-tag :type="detailData.status === 'abnormal' ? 'danger' : 'success'" size="small">
          {{ detailData.statusName || (detailData.status === 'normal' ? '正常' : '异常') }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="备注" :span="1">{{ detailData.remark || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间" :span="1">{{ detailData.createTime }}</el-descriptions-item>
      <el-descriptions-item label="关联排班ID" :span="1">{{ detailData.scheduleId || '-' }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getDutyRecordPage,
  createDutyRecord,
  updateDutyRecord,
  deleteDutyRecord,
  getDutyRecordStatistics,
  getDutyScheduleByDateRange
} from '@/api/eiot/firewatch/firewatch.api'
import { getPropertyList } from '@/api/eiot/firewatch/firewatch.api'

defineOptions({ name: 'FireWatchDutyRecord' })

const loading = ref(false)
const total = ref(0)
const tableData = ref<any[]>([])
const propertyList = ref<any[]>([])
const scheduleList = ref<any[]>([])

// 记录类型映射
const recordTypeMap: Record<string, string> = {
  patrol: '巡查记录',
  alarm: '告警处理',
  handover: '交接班',
  other: '其他'
}

// 记录统计
const statistics = reactive<Record<string, number>>({
  total: 0,
  patrol: 0,
  alarm: 0,
  handover: 0,
  other: 0
})

// 加载统计
const loadStatistics = async () => {
  try {
    const now = new Date()
    const startOfMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
    const endOfMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()}`
    const res = await getDutyRecordStatistics(startOfMonth + ' 00:00:00', endOfMonth + ' 23:59:59')
    if (res) {
      Object.assign(statistics, res)
      // 计算总数
      statistics.total = (statistics.patrol || 0) + (statistics.alarm || 0) + (statistics.handover || 0) + (statistics.other || 0)
    }
  } catch (e) {
    // 忽略统计加载失败
  }
}

// 点击统计过滤
const handleTypeFilter = (type: string) => {
  queryParams.recordType = type || undefined
  handleQuery()
}

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  propertyId: undefined as number | undefined,
  recordTime: undefined as string[] | undefined,
  recordType: undefined as string | undefined,
  recorderName: undefined as string | undefined,
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
  propertyName: '',
  scheduleId: undefined,
  recordTime: undefined,
  recordType: 'patrol',
  title: '',
  content: '',
  deviceId: undefined,
  deviceName: '',
  recorderId: undefined,
  recorderName: '',
  status: 'normal',
  remark: ''
})

const formRules = reactive<FormRules>({
  propertyId: [{ required: true, message: '请选择所属单位', trigger: 'change' }],
  recordType: [{ required: true, message: '请选择记录类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入记录标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入记录内容', trigger: 'blur' }]
})

// 查看详情
const detailVisible = ref(false)
const detailData = ref<any>({})

const handleDetail = (row: any) => {
  detailData.value = row
  detailVisible.value = true
}

// 所属单位变化时自动填充名称和加载排班
const handlePropertyChange = (val: number) => {
  const prop = propertyList.value.find((p: any) => p.id === val)
  if (prop) {
    formData.value.propertyName = prop.propertyName
  }
  loadScheduleList(val)
}

// 加载可关联的排班列表
const loadScheduleList = async (propertyId?: number) => {
  try {
    const now = new Date()
    const startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
    const endDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()}`
    const res = await getDutyScheduleByDateRange(startDate, endDate, propertyId)
    scheduleList.value = (res as any[]) || []
  } catch (e) {
    scheduleList.value = []
  }
}

// 获取列表
const getList = async () => {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (queryParams.recordTime && queryParams.recordTime.length === 2) {
      params.recordTimeStart = queryParams.recordTime[0] + ' 00:00:00'
      params.recordTimeEnd = queryParams.recordTime[1] + ' 23:59:59'
      delete params.recordTime
    }
    const res = await getDutyRecordPage(params)
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
  dialogTitle.value = '新增值班记录'
  dialogVisible.value = true
}

// 编辑
const handleUpdate = (row: any) => {
  resetForm()
  dialogTitle.value = '编辑值班记录'
  formData.value = { ...row }
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该值班记录？', '提示', { type: 'warning' })
  await deleteDutyRecord(row.id)
  ElMessage.success('删除成功')
  getList()
  loadStatistics()
}

// 重置表单
const resetForm = () => {
  formData.value = {
    id: undefined,
    propertyId: undefined,
    propertyName: '',
    scheduleId: undefined,
    recordTime: undefined,
    recordType: 'patrol',
    title: '',
    content: '',
    deviceId: undefined,
    deviceName: '',
    recorderId: undefined,
    recorderName: '',
    status: 'normal',
    remark: ''
  }
  scheduleList.value = []
  formRef.value?.resetFields()
}

// 提交表单
const submitForm = async () => {
  await formRef.value?.validate()
  if (formData.value.id) {
    await updateDutyRecord(formData.value)
    ElMessage.success('更新成功')
  } else {
    await createDutyRecord(formData.value)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  getList()
  loadStatistics()
}

// 导出
const handleExport = async () => {
  try {
    const data = tableData.value
    if (!data.length) {
      ElMessage.warning('暂无数据可导出')
      return
    }
    const headers = ['ID', '所属单位', '记录时间', '记录类型', '标题', '内容', '关联设备', '记录人', '状态', '备注', '创建时间']
    const rows = data.map((row: any) => [
      row.id, row.propertyName, row.recordTime,
      row.recordTypeName || recordTypeMap[row.recordType] || row.recordType,
      row.title, row.content, row.deviceName || '',
      row.recorderName,
      row.statusName || (row.status === 'normal' ? '正常' : '异常'),
      row.remark || '', row.createTime || ''
    ])
    const csv = '\uFEFF' + headers.join(',') + '\n' + rows.map((r: string[]) => r.map(c => `"${c}"`).join(',')).join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `值班记录_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// 获取记录类型标签
const getRecordTypeTag = (type: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    patrol: 'success',
    alarm: 'warning',
    handover: 'info',
    other: 'info'
  }
  return map[type] || 'info'
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
.content-cell {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.detail-content {
  white-space: pre-wrap;
  line-height: 1.6;
  max-height: 300px;
  overflow-y: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
