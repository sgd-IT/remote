<template>
  <el-dialog
    v-model="visible"
    title="告警处理"
    width="700px"
    :close-on-click-modal="false"
    custom-class="remote-console-modal"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="alarm-body">
      <div class="alarm-list">
        <div v-for="item in alarmList" :key="item.id" class="alarm-item">
          <div class="alarm-level" :class="getLevelClass(item.level)">{{ getLevelText(item.level) }}</div>
          <div class="alarm-content">
            <div class="alarm-title">{{ item.deviceName }} - {{ item.content }}</div>
            <div class="alarm-time">{{ formatTime(item.alertTime) }}</div>
          </div>
          <button
            class="alarm-action"
            :disabled="processingId === item.id"
            @click="handleConfirm(item)"
          >
            {{ processingId === item.id ? '处理中...' : '确认处理' }}
          </button>
        </div>
        <el-empty v-if="!loading && alarmList.length === 0" description="暂无未处理告警" :image-size="60" />
      </div>
      <div v-if="total > 0" class="alarm-pagination">
        <el-pagination
          v-model:current-page="pageNo"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          small
          @current-change="fetchAlarms"
        />
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAlertRecordPage, confirmAlertRecord, type AlertRecordVO } from '@/api/eiot/remote/deviceRemote.api'

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
const processingId = ref<number | string | null>(null)
const alarmList = ref<AlertRecordVO[]>([])
const pageNo = ref(1)
const pageSize = 10
const total = ref(0)

const fetchAlarms = async () => {
  loading.value = true
  try {
    const params: any = { pageNo: pageNo.value, pageSize }
    if (props.deviceId) params.deviceId = props.deviceId
    const res = await getAlertRecordPage(params)
    const list = (res?.list || []) as AlertRecordVO[]
    alarmList.value = list
    total.value = res?.total || 0
  } catch (e) {
    alarmList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const getLevelClass = (level?: string) => {
  const map: Record<string, string> = { HIGH: 'alarm-high', MEDIUM: 'alarm-medium', LOW: 'alarm-low' }
  return map[level || ''] || 'alarm-low'
}

const getLevelText = (level?: string) => {
  const map: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[level || ''] || '低'
}

const formatTime = (time?: number) => {
  if (!time) return '-'
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

const handleConfirm = async (item: AlertRecordVO) => {
  if (processingId.value) return
  processingId.value = item.id
  try {
    await confirmAlertRecord(item.id)
    ElMessage.success(`已确认处理告警：${item.content}`)
    // 处理后刷新当前页（如果当前页只剩一条且不是第一页，回到上一页）
    if (alarmList.value.length <= 1 && pageNo.value > 1) {
      pageNo.value--
    }
    await fetchAlarms()
  } catch (e: any) {
    ElMessage.error(e?.message || '处理失败，请重试')
  } finally {
    processingId.value = null
  }
}

watch(() => props.modelValue, (val) => {
  if (val) {
    pageNo.value = 1
    fetchAlarms()
  }
})
</script>

<style lang="scss" scoped>
.alarm-body {
  display: flex;
  flex-direction: column;
  min-height: 200px;
}

.alarm-list {
  flex: 1;
  overflow-y: auto;
}

.alarm-pagination {
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 180, 255, 0.1);
  display: flex;
  justify-content: flex-end;
}

.alarm-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  background: rgba(13, 30, 56, 0.6);
  border: 1px solid rgba(0, 180, 255, 0.1);
  border-radius: 6px;
  margin-bottom: 10px;
}

.alarm-level {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.alarm-high {
  background: rgba(231, 76, 60, 0.2);
  color: #e74c3c;
}

.alarm-medium {
  background: rgba(241, 196, 15, 0.2);
  color: #f1c40f;
}

.alarm-low {
  background: rgba(46, 204, 113, 0.2);
  color: #2ecc71;
}

.alarm-content {
  flex: 1;
}

.alarm-title {
  color: #e8f0fe;
  font-size: 14px;
  margin-bottom: 4px;
}

.alarm-time {
  color: #8ba3c7;
  font-size: 12px;
}

.alarm-action {
  padding: 6px 14px;
  background: rgba(0, 180, 255, 0.15);
  border: 1px solid rgba(0, 180, 255, 0.3);
  border-radius: 4px;
  color: #00b4ff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.alarm-action:hover {
  background: rgba(0, 180, 255, 0.25);
}
</style>
