<template>
  <transition name="slide-fade">
    <div v-if="visible && device" class="device-info-card">
      <div class="card-header">
        <div class="device-title">
          <Icon icon="ep:cpu" :size="20" />
          <span class="device-name">{{ device.name }}</span>
        </div>
        <el-button type="text" @click="handleClose">
          <Icon icon="ep:close" />
        </el-button>
      </div>

      <div class="card-body">
        <div class="info-row">
          <span class="label">{{ $t('dashboard.serialNumber') }}:</span>
          <span class="value">{{ device.serialNo }}</span>
        </div>

        <div class="info-row">
          <span class="label">{{ $t('dashboard.status') }}:</span>
          <el-tag :type="getStatusType(device.status)" size="small">
            {{ getStatusLabel(device.status) }}
          </el-tag>
        </div>

        <div class="info-row">
          <span class="label">{{ $t('dashboard.product') }}:</span>
          <span class="value">{{ device.productName }}</span>
        </div>

        <div v-if="device.address" class="info-row">
          <span class="label">{{ $t('dashboard.address') }}:</span>
          <span class="value">{{ device.address }}</span>
        </div>

        <div class="info-row">
          <span class="label">{{ $t('dashboard.coordinates') }}:</span>
          <span class="value">
            {{ device.lat.toFixed(6) }}, {{ device.lng.toFixed(6) }}
          </span>
        </div>
      </div>

      <div class="card-footer">
        <el-button type="primary" size="small" @click="handleViewDetails">
          <Icon icon="ep:view" />
          {{ $t('dashboard.viewDetails') }}
        </el-button>
        <el-button size="small" @click="handleSendCommand">
          <Icon icon="ep:message" />
          {{ $t('dashboard.sendCommand') }}
        </el-button>
        <el-button size="small" @click="handleViewAlerts">
          <Icon icon="ep:warning" />
          {{ $t('dashboard.viewAlerts') }}
        </el-button>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { DeviceLocation } from '@/api/eiot/dashboard'

defineOptions({ name: 'DeviceInfoCard' })

const props = defineProps({
  device: {
    type: Object as () => DeviceLocation | null,
    default: null
  },
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'view-details', 'send-command', 'view-alerts'])

const router = useRouter()

const getStatusType = (status: string) => {
  const types = {
    online: 'success',
    offline: 'info',
    error: 'danger'
  }
  return types[status as keyof typeof types] || 'info'
}

const getStatusLabel = (status: string) => {
  const labels = {
    online: '在线',
    offline: '离线',
    error: '告警'
  }
  return labels[status as keyof typeof labels] || '未知'
}

const handleClose = () => {
  emit('close')
}

const handleViewDetails = () => {
  if (props.device) {
    router.push({
      path: '/eiot/deviceinfo/detail',
      query: { id: props.device.id }
    })
  }
  emit('view-details', props.device)
}

const handleSendCommand = () => {
  emit('send-command', props.device)
}

const handleViewAlerts = () => {
  if (props.device) {
    router.push({
      path: '/eiot/alarm/list',
      query: { deviceId: props.device.id }
    })
  }
  emit('view-alerts', props.device)
}

watch(
  () => props.device,
  (newVal) => {
    if (!newVal) {
      emit('close')
    }
  }
)
</script>

<style scoped lang="scss">
.device-info-card {
  position: absolute;
  top: 80px;
  right: 16px;
  width: 320px;
  background: var(--el-bg-color);
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.device-title {
  display: flex;
  align-items: center;
  gap: 8px;

  .device-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}

.card-body {
  padding: 16px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }

  .label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    min-width: 80px;
  }

  .value {
    font-size: 13px;
    color: var(--el-text-color-primary);
    word-break: break-all;
  }
}

.card-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateX(20px);
  opacity: 0;
}

@media (max-width: 768px) {
  .device-info-card {
    position: fixed;
    top: auto;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    border-radius: 16px 16px 0 0;
  }
}
</style>
