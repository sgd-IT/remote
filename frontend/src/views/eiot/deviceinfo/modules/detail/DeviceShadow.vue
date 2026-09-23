<template>
  <div class="device-shadow-tab">
    <el-alert
      title="设备影子说明"
      type="info"
      :closable="false"
      class="mb-20px"
    >
      设备影子用于存储设备的期望状态和实际状态，支持离线设备的状态同步。
    </el-alert>

    <el-row :gutter="20" v-if="shadowData">
      <!-- 期望状态 -->
      <el-col :span="8">
        <el-card shadow="hover" class="state-card">
          <template #header>
            <div class="card-header">
              <span>期望状态 (Desired)</span>
              <el-tag type="info" size="small">v{{ shadowData.version || 0 }}</el-tag>
            </div>
          </template>
          <div class="state-content">
            <el-empty v-if="!hasDesired" description="暂无期望状态" :image-size="60" />
            <div v-else class="property-list">
              <div v-for="(value, key) in shadowData.desired" :key="key" class="property-item">
                <span class="property-key">{{ key }}:</span>
                <span class="property-value">{{ formatValue(value) }}</span>
                <div v-if="shadowData.metadata?.desired?.[key]" class="property-meta">
                  {{ formatTime(shadowData.metadata.desired[key].timestamp) }}
                </div>
              </div>
            </div>
          </div>
          <template #footer>
            <el-button size="small" type="primary" @click="handleEditDesired">编辑</el-button>
          </template>
        </el-card>
      </el-col>

      <!-- 上报状态 -->
      <el-col :span="8">
        <el-card shadow="hover" class="state-card">
          <template #header>
            <div class="card-header">
              <span>上报状态 (Reported)</span>
              <el-tag type="success" size="small">v{{ shadowData.version || 0 }}</el-tag>
            </div>
          </template>
          <div class="state-content">
            <el-empty v-if="!hasReported" description="暂无上报状态" :image-size="60" />
            <div v-else class="property-list">
              <div v-for="(value, key) in shadowData.reported" :key="key" class="property-item">
                <span class="property-key">{{ key }}:</span>
                <span class="property-value">{{ formatValue(value) }}</span>
                <div v-if="shadowData.metadata?.reported?.[key]" class="property-meta">
                  {{ formatTime(shadowData.metadata.reported[key].timestamp) }}
                </div>
              </div>
            </div>
          </div>
          <template #footer>
            <span class="text-gray-400 text-xs">
              {{ shadowData.lastReportedTime ? '更新: ' + formatDateTime(shadowData.lastReportedTime) : '未更新' }}
            </span>
          </template>
        </el-card>
      </el-col>

      <!-- 差异状态 -->
      <el-col :span="8">
        <el-card shadow="hover" class="state-card">
          <template #header>
            <div class="card-header">
              <span>差异状态 (Delta)</span>
              <el-tag :type="hasDelta ? 'warning' : 'success'" size="small">
                {{ deltaCount }} 项
              </el-tag>
            </div>
          </template>
          <div class="state-content">
            <el-empty v-if="!hasDelta" description="无差异" :image-size="60" />
            <div v-else class="property-list">
              <div v-for="(value, key) in shadowData.delta" :key="key" class="property-item delta">
                <div class="property-key">{{ key }}</div>
                <div class="property-compare">
                  <span class="compare-label">期望: {{ formatValue(value) }}</span>
                  <span class="compare-label">实际: {{ formatValue(shadowData.reported?.[key]) }}</span>
                </div>
              </div>
            </div>
          </div>
          <template #footer>
            <el-alert type="info" :closable="false" v-if="hasDelta">
              <template #default>
                <div class="text-xs">设备下次上线时会自动同步期望状态</div>
              </template>
            </el-alert>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!shadowData" description="暂无影子数据" />

    <!-- 编辑期望状态对话框 -->
    <Dialog v-model="editDialogVisible" title="编辑期望状态" width="500px">
      <el-alert type="warning" :closable="false" class="mb-15px">
        <template #default>
          <div class="text-xs">
            当前版本: v{{ shadowData?.version || 0 }}<br />
            修改后版本将自动 +1，设备下次上线时会收到期望状态推送
          </div>
        </template>
      </el-alert>
      <el-form ref="editFormRef" :model="editForm" label-width="80px">
        <div v-for="(item, index) in editForm.properties" :key="index" class="property-edit-row">
          <el-input v-model="item.key" placeholder="属性名" class="property-input" />
          <el-input v-model="item.value" placeholder="属性值" class="property-input" />
          <el-button type="danger" :icon="Delete" circle @click="removeProperty(index)" />
        </div>
        <el-button type="primary" @click="addProperty" class="mt-10px">
          <Icon icon="ep:plus" class="mr-5px" />
          添加属性
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveDesired" :loading="saving">保存</el-button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import { dateFormatter } from '@/utils/formatTime'
import { getDeviceShadow, updateDeviceShadow, type DeviceShadowVO } from '@/api/eiot/shadow'

const props = defineProps({
  deviceId: {
    type: Number,
    required: true
  }
})

const message = useMessage()

const shadowData = ref<DeviceShadowVO>()
const editDialogVisible = ref(false)
const saving = ref(false)
const editForm = reactive({
  properties: [] as Array<{ key: string; value: any }>
})

const hasDesired = computed(() => {
  return shadowData.value?.desired && Object.keys(shadowData.value.desired).length > 0
})

const hasReported = computed(() => {
  return shadowData.value?.reported && Object.keys(shadowData.value.reported).length > 0
})

const hasDelta = computed(() => {
  return shadowData.value?.delta && Object.keys(shadowData.value.delta).length > 0
})

const deltaCount = computed(() => {
  return shadowData.value?.delta ? Object.keys(shadowData.value.delta).length : 0
})

const loadData = async () => {
  try {
    const data = await getDeviceShadow(props.deviceId)
    shadowData.value = data
  } catch (error) {
    console.error('加载影子数据失败:', error)
  }
}

const formatValue = (value: any) => {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const formatTime = (timestamp: number) => {
  if (!timestamp) return '-'
  return dateFormatter({ cellValue: new Date(timestamp).toISOString() })
}

const formatDateTime = (time: string) => {
  if (!time) return '-'
  return dateFormatter({ cellValue: time })
}

const handleEditDesired = () => {
  editForm.properties = []
  if (shadowData.value?.desired) {
    editForm.properties = Object.entries(shadowData.value.desired).map(([key, value]) => ({
      key,
      value
    }))
  }
  if (editForm.properties.length === 0) {
    editForm.properties.push({ key: '', value: '' })
  }
  editDialogVisible.value = true
}

const addProperty = () => {
  editForm.properties.push({ key: '', value: '' })
}

const removeProperty = (index: number) => {
  editForm.properties.splice(index, 1)
}

const handleSaveDesired = async () => {
  if (!shadowData.value) return

  const desired: Record<string, any> = {}
  editForm.properties.forEach((item) => {
    if (item.key) {
      let value: any = item.value
      // 类型转换
      if (value === 'true') value = true
      else if (value === 'false') value = false
      else if (!isNaN(Number(value)) && value !== '') value = Number(value)
      desired[item.key] = value
    }
  })

  if (Object.keys(desired).length === 0) {
    message.warning('请至少添加一个属性')
    return
  }

  try {
    saving.value = true
    // 传入当前版本 +1 作为新版本号
    await updateDeviceShadow(
      props.deviceId,
      desired,
      shadowData.value.version + 1
    )
    message.success('保存成功，设备下次上线时会收到期望状态推送')
    editDialogVisible.value = false
    await loadData()
  } catch (error: any) {
    if (error?.message?.includes('版本冲突')) {
      message.error('版本冲突，请刷新后重试')
      await loadData()
    } else {
      message.error('保存失败: ' + (error?.message || '未知错误'))
    }
  } finally {
    saving.value = false
  }
}

defineExpose({ loadData })

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.device-shadow-tab {
  .state-card {
    height: 450px;
    display: flex;
    flex-direction: column;

    :deep(.el-card__header) {
      padding: 12px 16px;
      background: #f5f7fa;
    }

    :deep(.el-card__body) {
      flex: 1;
      overflow: auto;
      padding: 16px;
    }

    :deep(.el-card__footer) {
      padding: 10px 16px;
      border-top: 1px solid #ebeef5;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 600;
    }

    .state-content {
      min-height: 280px;
    }

    .property-list {
      .property-item {
        padding: 10px;
        margin-bottom: 8px;
        background: #f5f7fa;
        border-radius: 4px;
        border-left: 3px solid #409eff;

        &.delta {
          border-left-color: #e6a23c;
        }

        .property-key {
          font-weight: 600;
          color: #303133;
          margin-right: 8px;
        }

        .property-value {
          color: #606266;
          font-family: 'Courier New', monospace;
        }

        .property-meta {
          font-size: 11px;
          color: #909399;
          margin-top: 4px;
        }

        .property-compare {
          display: flex;
          flex-direction: column;
          gap: 5px;
          margin-top: 5px;

          .compare-label {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
  }

  .property-edit-row {
    display: flex;
    gap: 10px;
    margin-bottom: 10px;

    .property-input {
      flex: 1;
    }
  }
}
</style>
