<template>
  <div class="table-container">
    <div class="table-header">
      <div class="table-header-cell">时间</div>
      <div class="table-header-cell">设备</div>
      <div class="table-header-cell">内容</div>
      <div class="table-header-cell">位置信息</div>
      <div class="table-header-cell">设备编号</div>
      <div class="table-header-cell">操作</div>
    </div>
    <div v-loading="loading" class="table-body">
      <template v-if="list.length > 0">
        <div v-for="item in list" :key="item.id" class="table-row">
          <div class="table-cell">{{ formatTime(item.alertTime) }}</div>
          <div class="table-cell">{{ item.deviceName || '-' }}</div>
          <div class="table-cell">{{ item.content }}</div>
          <div class="table-cell">{{ item.addr || '-' }}</div>
          <div class="table-cell">{{ item.deviceId || '-' }}</div>
          <div class="table-cell">
            <button class="table-action" @click="handleAction(item)">处理</button>
          </div>
        </div>
      </template>
      <div v-else class="empty-state">
        <div class="empty-icon">📦</div>
        <div class="empty-text">暂无数据</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { getAlertRecordPage, type AlertRecordVO } from '@/api/eiot/remote/deviceRemote.api'

const props = defineProps<{
  deviceId: string | number
}>()

const loading = ref(false)
const list = ref<AlertRecordVO[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = { pageNo: 1, pageSize: 20 }
    if (props.deviceId) params.deviceId = props.deviceId
    const res = await getAlertRecordPage(params)
    list.value = (res?.list || []) as AlertRecordVO[]
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const formatTime = (time?: number) => {
  if (!time) return '-'
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

const handleAction = (item: AlertRecordVO) => {
  // eslint-disable-next-line no-console
  console.log('处理告警', item)
}

watch(() => props.deviceId, () => {
  fetchData()
}, { immediate: true })
</script>

<style lang="scss" scoped>
.table-container {
  flex: 1;
  background: rgba(13, 30, 56, 0.7);
  border: 1px solid rgba(0, 180, 255, 0.15);
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.table-header {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1.5fr 1.5fr 1fr 0.8fr;
  background: rgba(0, 180, 255, 0.1);
  border-bottom: 1px solid rgba(0, 180, 255, 0.15);
}

.table-header-cell {
  padding: 12px 16px;
  font-size: 13px;
  color: #8ba3c7;
  font-weight: 500;
  text-align: left;
}

.table-body {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
  position: relative;
}

.table-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1.5fr 1.5fr 1fr 0.8fr;
  border-bottom: 1px solid rgba(0, 180, 255, 0.08);
}

.table-row:hover {
  background: rgba(0, 180, 255, 0.05);
}

.table-cell {
  padding: 12px 16px;
  font-size: 13px;
  color: #e8f0fe;
  text-align: left;
  display: flex;
  align-items: center;
}

.table-action {
  padding: 4px 10px;
  background: rgba(0, 180, 255, 0.15);
  border: 1px solid rgba(0, 180, 255, 0.3);
  border-radius: 4px;
  color: #00b4ff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.table-action:hover {
  background: rgba(0, 180, 255, 0.25);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-text {
  color: #8ba3c7;
  font-size: 14px;
}
</style>
