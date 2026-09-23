<template>
  <div class="tab-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" inline class="search-form">
      <el-form-item label="报告名称">
        <el-input v-model="queryParams.keyword" placeholder="报告名称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="报告类型">
        <el-select v-model="queryParams.reportType" placeholder="报告类型" clearable style="width: 130px">
          <el-option label="工单报告" value="order" />
          <el-option label="计划报告" value="plan" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
        <el-button type="success" icon="Download" @click="handleExport">导出Excel</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="reportList" v-loading="loading" stripe border>
      <el-table-column prop="reportNo" label="报告编号" width="180" />
      <el-table-column prop="reportName" label="报告名称" min-width="180" />
      <el-table-column prop="reportType" label="报告类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.reportType === 'plan' ? 'primary' : 'success'" size="small" effect="dark">
            {{ row.reportTypeName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="propertyName" label="所属企业" width="150" />
      <el-table-column prop="maintenanceInfo" label="维保单位/维保人" width="150" />
      <el-table-column prop="completeDate" label="完成日期" width="100">
        <template #default="{ row }">{{ formatDate(row.completeDate, 'YYYY-MM-DD') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="queryParams.pageNo" v-model:page-size="queryParams.pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="getList" />
    </div>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewVisible" title="报告详情" width="650px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报告编号">{{ currentReport.reportNo }}</el-descriptions-item>
        <el-descriptions-item label="报告名称">{{ currentReport.reportName }}</el-descriptions-item>
        <el-descriptions-item label="报告类型">{{ currentReport.reportTypeName }}</el-descriptions-item>
        <el-descriptions-item label="所属企业">{{ currentReport.propertyName }}</el-descriptions-item>
        <el-descriptions-item label="维保单位/维保人">{{ currentReport.maintenanceInfo }}</el-descriptions-item>
        <el-descriptions-item label="完成日期">{{ formatDate(currentReport.completeDate, 'YYYY-MM-DD') }}</el-descriptions-item>
        <!-- 工单报告特有字段 -->
        <template v-if="currentReport.reportType === 'order'">
          <el-descriptions-item label="设备名称" v-if="currentReport.deviceName">{{ currentReport.deviceName }}</el-descriptions-item>
          <el-descriptions-item label="故障描述" v-if="currentReport.faultDesc" :span="2">{{ currentReport.faultDesc }}</el-descriptions-item>
          <el-descriptions-item label="完成备注" v-if="currentReport.completeRemark" :span="2">{{ currentReport.completeRemark }}</el-descriptions-item>
          <el-descriptions-item label="满意度" v-if="currentReport.satisfaction != null">{{ currentReport.satisfaction }} 星</el-descriptions-item>
          <el-descriptions-item label="评价建议" v-if="currentReport.suggestion" :span="2">{{ currentReport.suggestion }}</el-descriptions-item>
        </template>
        <!-- 计划报告特有字段 -->
        <template v-if="currentReport.reportType === 'plan'">
          <el-descriptions-item label="计划日期" v-if="currentReport.planDate">{{ formatDate(currentReport.planDate, 'YYYY-MM-DD') }}</el-descriptions-item>
          <el-descriptions-item label="执行人" v-if="currentReport.executePerson">{{ currentReport.executePerson }}</el-descriptions-item>
          <el-descriptions-item label="执行单位" v-if="currentReport.executeUnit" :span="2">{{ currentReport.executeUnit }}</el-descriptions-item>
        </template>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/utils/formatTime'
import { getRepairOrderPage, getPlanPage } from '@/api/eiot/firewatch/firewatch.api'

const queryParams = reactive({ pageNo: 1, pageSize: 10, keyword: '', reportType: '' })
const loading = ref(false)
const reportList = ref<any[]>([])
const total = ref(0)
const viewVisible = ref(false)
const currentReport = ref<any>({})

const getList = async () => {
  loading.value = true
  try {
    // 从已完成工单获取
    const orderRes = await getRepairOrderPage({ pageNo: 1, pageSize: 100, orderStatus: 'completed' })
    const orderReports = (orderRes?.list || []).map((o: any) => ({
      reportNo: o.orderNo,
      reportName: `工单报告-${o.orderNo}`,
      reportType: 'order',
      reportTypeName: '工单报告',
      propertyName: o.propertyName,
      maintenanceInfo: o.repairPerson || o.assignee || '-',
      completeDate: o.completeTime,
      deviceName: o.deviceName,
      faultDesc: o.faultDescription,
      completeRemark: o.completeRemark,
      satisfaction: o.satisfaction,
      suggestion: o.suggestion
    }))

    // 从已完成计划获取
    const planRes = await getPlanPage({ pageNo: 1, pageSize: 100, planStatus: 'completed' })
    const planReports = (planRes?.list || []).map((p: any) => ({
      reportNo: p.planNo,
      reportName: `计划报告-${p.planName || p.planNo}`,
      reportType: 'plan',
      reportTypeName: '计划报告',
      propertyName: p.propertyName,
      maintenanceInfo: p.executePerson || p.executeUnit || '-',
      completeDate: undefined,
      planDate: p.planDate,
      executePerson: p.executePerson,
      executeUnit: p.executeUnit
    }))

    let all = [...orderReports, ...planReports]

    // 搜索过滤
    if (queryParams.keyword) {
      all = all.filter(r => r.reportName.includes(queryParams.keyword))
    }
    if (queryParams.reportType) {
      all = all.filter(r => r.reportType === queryParams.reportType)
    }

    total.value = all.length
    // 分页
    const start = (queryParams.pageNo - 1) * queryParams.pageSize
    const end = start + queryParams.pageSize
    reportList.value = all.slice(start, end)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => { queryParams.pageNo = 1; getList() }
const handleReset = () => { queryParams.keyword = ''; queryParams.reportType = ''; queryParams.pageNo = 1; getList() }

const handleView = (row: any) => {
  currentReport.value = row
  viewVisible.value = true
}

// 导出CSV
const handleExport = async () => {
  ElMessage.info('正在导出维保报告...')
  try {
    const orderRes = await getRepairOrderPage({ pageNo: 1, pageSize: 100, orderStatus: 'completed' })
    const planRes = await getPlanPage({ pageNo: 1, pageSize: 100, planStatus: 'completed' })

    const orderReports = (orderRes?.list || []).map((o: any) => ({
      reportNo: o.orderNo, reportName: `工单报告-${o.orderNo}`, reportType: '工单报告',
      propertyName: o.propertyName || '', maintenanceInfo: o.repairPerson || o.assignee || '',
      deviceName: o.deviceName || '', faultDesc: o.faultDescription || '',
      completeDate: o.completeTime || '', satisfaction: o.satisfaction ?? '', suggestion: o.suggestion || ''
    }))
    const planReports = (planRes?.list || []).map((p: any) => ({
      reportNo: p.planNo, reportName: `计划报告-${p.planName || p.planNo}`, reportType: '计划报告',
      propertyName: p.propertyName || '', maintenanceInfo: p.executePerson || p.executeUnit || '',
      deviceName: '', faultDesc: '', completeDate: p.planDate || '', satisfaction: '', suggestion: ''
    }))

    let all = [...orderReports, ...planReports]
    if (queryParams.keyword) all = all.filter(r => r.reportName.includes(queryParams.keyword))
    if (queryParams.reportType) all = all.filter(r => r.reportType === (queryParams.reportType === 'order' ? '工单报告' : '计划报告'))

    const headers = ['报告编号', '报告名称', '报告类型', '所属企业', '维保信息', '设备名称', '故障描述', '完成日期', '满意度', '评价建议']
    const BOM = '\uFEFF'
    const rows = all.map((r: any) => [
      r.reportNo, r.reportName, r.reportType, r.propertyName, r.maintenanceInfo,
      r.deviceName, r.faultDesc, r.completeDate, r.satisfaction, r.suggestion
    ])
    const csvContent = BOM + [headers.join(','), ...rows.map(row => row.map(cell => `"${cell || ''}"`).join(','))].join('\n')
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const href = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = href
    link.download = `维保报告_${new Date().toLocaleDateString()}.csv`
    link.click()
    URL.revokeObjectURL(href)
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出失败', e)
    ElMessage.error('导出失败')
  }
}

onMounted(() => getList())
</script>

<style scoped>
.tab-container { padding: 16px; }
.search-form { background: #f5f7fa; padding: 16px; border-radius: 6px; margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
