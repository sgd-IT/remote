<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="820px"
    append-to-body
    destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-table v-loading="loading" :data="subDevices" border stripe max-height="480">
      <el-table-column label="设备名称" prop="name" min-width="140" show-overflow-tooltip />
      <el-table-column label="设备标识(DN)" prop="dn" min-width="170" show-overflow-tooltip />
      <el-table-column label="多线/总线" width="100" align="center" v-if="props.lineTypeFilter">
        <template #default="{ row }">
          <el-tag :type="row.lineType === 0 ? 'warning' : 'primary'" size="small" effect="dark">
            {{ row.lineType === 0 ? '多线' : row.lineType === 1 ? '总线' : '--' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="96" align="center">
        <template #default="{ row }">
          <el-tag :type="stateTagType(row.state)" size="small" effect="dark">
            {{ stateText(row.state) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="地址" prop="addr" min-width="150" show-overflow-tooltip />
      <el-table-column label="产品Key" prop="productKey" min-width="140" show-overflow-tooltip />
      <el-table-column label="序列号" prop="serialNo" min-width="140" show-overflow-tooltip />
    </el-table>
    <el-empty
      v-if="!loading && !subDevices.length"
      :description="props.lineTypeFilter ? '该父设备暂无多线/总线子设备' : '该父设备暂无绑定的子设备'"
    />
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="pageNo"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        small
        @current-change="handlePageChange"
      />
    </div>
    <template #footer>
      <span class="dialog-footer">
        共 <b style="color:#409eff">{{ total }}</b> 个{{ props.lineTypeFilter ? '多线/总线' : '' }}子设备
        <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { getSubDevices, type FireDeviceItem } from '@/api/eiot/firewatch/fireDevice.api'

defineOptions({ name: 'SubDeviceModal' })

const props = defineProps<{
  modelValue: boolean
  parentId: number | null
  parentName?: string
  /** 是否按线路类型过滤（仅显示多线/总线设备） */
  lineTypeFilter?: boolean
}>()

defineEmits<{ 'update:modelValue': [v: boolean] }>()

const loading = ref(false)
const subDevices = ref<FireDeviceItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)

const title = computed(() => {
  const prefix = props.lineTypeFilter ? '多线/总线设备列表' : '子设备列表'
  return `${prefix}${props.parentName ? ' · ' + props.parentName : ''}`
})

// 状态文案映射（与 device-screen STATE_MAP 一致）
const STATE_TEXT: Record<number, string> = {
  0: '离线',
  1: '在线',
  3: '未激活',
  4: '故障'
}
const STATE_TAG_TYPE: Record<number, 'success' | 'info' | 'warning' | 'danger'> = {
  0: 'info',
  1: 'success',
  3: 'warning',
  4: 'danger'
}
const stateText = (s: number) => STATE_TEXT[s] ?? '未知'
const stateTagType = (s: number): 'success' | 'info' | 'warning' | 'danger' =>
  STATE_TAG_TYPE[s] ?? 'info'

/**
 * 分页加载子设备：lineTypeFilter 时仅加载多线(0)/总线(1)设备，否则加载全部子设备
 */
const loadSubDevices = async (id: number) => {
  loading.value = true
  try {
    const lineTypes = props.lineTypeFilter ? [0, 1] : undefined
    const res = await getSubDevices(id, pageNo.value, pageSize.value, lineTypes)
    subDevices.value = res?.list || []
    total.value = res?.total || 0
  } catch (e: any) {
    console.error('[SubDeviceModal] load sub devices error:', e?.message || e)
    subDevices.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handlePageChange = () => {
  if (props.parentId) {
    loadSubDevices(props.parentId)
  }
}

watch(
  () => [props.modelValue, props.parentId] as const,
  ([visible, id]) => {
    if (visible && id) {
      // 重新打开时重置页码
      pageNo.value = 1
      loadSubDevices(id)
    } else if (!visible) {
      subDevices.value = []
      total.value = 0
      pageNo.value = 1
    }
  }
)
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
