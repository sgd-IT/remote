<template>
  <el-dialog
    v-model="visible"
    title="操作日志"
    width="800px"
    :close-on-click-modal="false"
    custom-class="remote-console-modal"
    append-to-body
    destroy-on-close
  >
    <el-table v-loading="loading" :data="logList" border height="400">
      <el-table-column prop="time" label="时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.time) }}
        </template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作人" width="120" />
      <el-table-column prop="deviceName" label="设备" width="150" />
      <el-table-column prop="content" label="操作内容" />
      <el-table-column prop="status" label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'danger' : 'warning'">
            {{ row.status === 1 ? '成功' : row.status === 0 ? '失败' : '执行中' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, watch, computed } from 'vue'
import { getOperationLogs } from '@/api/eiot/remote/remoteConsole.api'

const props = defineProps<{
  modelValue: boolean
  deviceId: string | number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const logList = ref<any[]>([])

const fetchLogs = async () => {
  if (!props.deviceId) return
  loading.value = true
  try {
    const res = await getOperationLogs(props.deviceId, 50)
    const list = Array.isArray(res) ? res : (res as any)?.list || []
    logList.value = list.map((item: any) => ({
      id: item.id,
      time: item.operationTime,
      operatorName: item.operatorName || '-',
      deviceName: item.deviceName || '-',
      content: item.operationName || '-',
      status: item.commandStatus
    }))
  } catch (e) {
    logList.value = []
  } finally {
    loading.value = false
  }
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

watch(() => props.modelValue, (val) => {
  if (val) fetchLogs()
})
</script>
