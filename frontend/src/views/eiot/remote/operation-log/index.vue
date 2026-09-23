<template>
  <div class="operation-log-container">
    <el-card>
      <template #header>
        <span>远程控制操作日志</span>
      </template>

      <el-form :model="queryParams" ref="queryFormRef" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="操作时间">
              <el-date-picker
                v-model="dateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="操作类型">
              <el-select v-model="queryParams.operationType" placeholder="全部" clearable>
                <el-option label="消音" value="MUTE" />
                <el-option label="复位" value="RESET" />
                <el-option label="切换模式" value="SWITCH_MODE" />
                <el-option label="启动" value="START" />
                <el-option label="停止" value="STOP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="结果状态">
              <el-select v-model="queryParams.commandStatus" placeholder="全部" clearable>
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
                <el-option label="执行中" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item>
              <el-button type="primary" @click="handleQuery">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
              <el-button type="success" @click="handleExport">导出</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-table v-loading="loading" :data="logList" border>
        <el-table-column prop="operationTime" label="操作时间" width="180" />
        <el-table-column prop="propertyName" label="企业/物业" width="150" />
        <el-table-column prop="deviceName" label="设备" width="150" />
        <el-table-column prop="operationName" label="操作" width="120" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="reviewerName" label="复核人" width="120" />
        <el-table-column prop="commandStatus" label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.commandStatus)">
              {{ getStatusText(row.commandStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultMsg" label="结果说明" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="操作IP" width="140" />
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOperationLogPage, exportOperationLog } from '@/api/eiot/remote/remoteConsole.api'

const loading = ref(false)
const total = ref(0)
const logList = ref<any[]>([])
const dateRange = ref<string[]>([])

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  operationType: undefined as string | undefined,
  commandStatus: undefined as number | undefined,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined
})

const getList = async () => {
  loading.value = true
  try {
    queryParams.startTime = dateRange.value?.[0]
    queryParams.endTime = dateRange.value?.[1]
    const res = await getOperationLogPage(queryParams)
    logList.value = res?.list || []
    total.value = res?.total || 0
  } catch (err: any) {
    ElMessage.error(err?.message || '获取操作日志失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  dateRange.value = []
  queryParams.operationType = undefined
  queryParams.commandStatus = undefined
  queryParams.startTime = undefined
  queryParams.endTime = undefined
  handleQuery()
}

const handleExport = async () => {
  try {
    queryParams.startTime = dateRange.value?.[0]
    queryParams.endTime = dateRange.value?.[1]
    await exportOperationLog(queryParams)
    ElMessage.success('导出成功')
  } catch (err: any) {
    ElMessage.error(err?.message || '导出失败')
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'danger',
    1: 'success',
    2: 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '失败', 1: '成功', 2: '执行中' }
  return map[status] || '未知'
}

onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.operation-log-container {
  padding: 20px;
}
</style>
