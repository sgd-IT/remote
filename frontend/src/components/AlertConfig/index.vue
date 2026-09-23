<template>
  <div class="alert-config">
    <yt-crud
      ref="crudRef"
      :data="data"
      :column="column"
      :table-props="{ selection: false }"
      :add-callback="onAddCallback"
      :edit-callback="onEditCallback"
      @save-fun="onSave"
      @del-fun="handleDelete"
      @onLoad="getData"
      :loading="state.loading"
      :total="state.total"
      v-model:page="state.page"
      v-model:query="state.query"
    >
      <template #status="scope">
        <el-switch
          v-model="scope.row.status"
          :active-value="0"
          :inactive-value="1"
          @change="handleStatusChange(scope.row)"
        />
      </template>
      <template #conditionsStrForm>
        <div class="inline-condition-editor">
          <div class="logic-selector">
            <el-radio-group v-model="tempConditions.logic">
              <el-radio label="AND">所有条件满足 (AND)</el-radio>
              <el-radio label="OR">任一条件满足 (OR)</el-radio>
            </el-radio-group>
          </div>

          <div class="condition-list">
            <div
              v-for="(item, index) in tempConditions.items"
              :key="index"
              class="condition-item"
            >
              <el-row style="width: 100%;" :gutter="10">
                <el-col :span="4">
                  <el-select v-model="item.type" placeholder="类型" @change="() => onTypeChange(item)">
                    <el-option label="设备属性" value="property" />
                    <el-option label="设备状态" value="status" />
                  </el-select>
                </el-col>

                <el-col :span="6">
                  <template v-if="item.type === 'status'">
                    <el-input
                      model-value="是否在线"
                      disabled
                      placeholder="设备状态"
                    />
                  </template>
                  <el-select
                    v-else
                    v-model="item.key"
                    placeholder="选择属性"
                    filterable
                    @change="() => onKeyChange(item)"
                  >
                    <el-option
                      v-for="prop in propertyOptions"
                      :key="prop.value"
                      :label="prop.label"
                      :value="prop.value"
                    />
                  </el-select>
                </el-col>

                <el-col :span="10">
                  <el-row :gutter="5">
                    <el-col :span="8">
                      <el-select
                        v-model="item.operator"
                        placeholder="运算符"
                        @change="() => onOperatorChange(item)"
                      >
                        <el-option
                          v-for="op in getAvailableComparators(item)"
                          :key="op.value"
                          :label="op.label"
                          :value="op.value"
                        />
                      </el-select>
                    </el-col>
                    <el-col :span="16">
                      <el-select
                        v-if="item.type === 'status'"
                        v-model="item.value"
                        placeholder="选择值"
                      >
                        <el-option label="在线" value="online" />
                        <el-option label="离线" value="offline" />
                      </el-select>
                      <el-select
                        v-else-if="getPropertyInfo(item.key)?.dataType?.type === 'bool'"
                        v-model="item.value"
                        placeholder="选择值"
                      >
                        <el-option label="true" value="true" />
                        <el-option label="false" value="false" />
                      </el-select>
                      <el-select
                        v-else-if="getPropertyInfo(item.key)?.dataType?.type === 'enum'"
                        v-model="item.value"
                        placeholder="选择枚举值"
                      >
                        <el-option
                          v-for="opt in getEnumOptions(item.key)"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                        />
                      </el-select>
                      <el-input
                        v-else
                        v-model="item.value"
                        :placeholder="getValuePlaceholder(item)"
                      />
                    </el-col>
                  </el-row>
                </el-col>

                <el-col :span="4" style="text-align: right;">
                  <el-button type="danger" @click="removeCondition(index)">删除</el-button>
                </el-col>
              </el-row>
            </div>
          </div>

          <div v-if="propertyLoading" class="loading-tip">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在加载属性...</span>
          </div>
          <div v-else-if="propertyList.length === 0" class="empty-tip">
            <el-empty description="暂无属性，请先在物模型中添加属性" />
          </div>

          <el-button type="primary" plain @click="addCondition">添加条件</el-button>
        </div>
      </template>
    </yt-crud>
  </div>
</template>

<script lang="ts" setup>
import { IColumn } from '@/components/common/types/tableCommon'
import {
  getDeviceAlertConfigPage,
  createDeviceAlertConfig,
  updateDeviceAlertConfig,
  deleteDeviceAlertConfig,
  ConditionVO,
} from '@/api/eiot/devicealert/devicealert.api'
import YtCrud from '@/components/common/yt-crud.vue'
import { ProductApi } from '@/api/eiot/product'
import { DeviceInfoApi } from '@/api/eiot/deviceinfo'
import { ThingModelApi } from '@/api/eiot/thingmodel'
import { propTypes } from '@/utils/propTypes'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps({
  deviceId: propTypes.number.def(null),
  productId: propTypes.number.def(null),
})

const state = reactive({
  page: { pageSize: 10, pageNo: 1 },
  total: 0,
  loading: false,
  query: {},
})

const column = ref<IColumn[]>([
  {
    label: '告警名称',
    key: 'name',
    search: true,
    rules: [{ required: true, message: '告警名称不能为空' }],
  },
  {
    label: '告警等级',
    key: 'level',
    tableWidth: 120,
    type: 'select',
    rules: [{ required: true, message: '请选择告警等级' }],
    componentProps: {
      options: [
        { label: '火警', value: '1' },
        { label: '故障', value: '2' },
        { label: '其他', value: '3' },
      ],
    },
  },
  {
    label: '条件',
    key: 'conditionsStr',
    tableWidth: 200,
    hide: true,
    formSlot: true,
  },
  {
    label: '是否启用',
    key: 'status',
    tableWidth: 120,
    type: 'switch',
    slot: true,
    componentProps: { activeValue: 0, inactiveValue: 1 },
  },
  {
    label: '备注',
    key: 'remark',
    hide: true,
    componentProps: { type: 'textarea', rows: 2 },
  },
  {
    label: '延迟满足时间',
    key: 'durationSec',
    hide: true,
    componentProps: {
      type: 'number',
      min: 0,
      step: 1,
      placeholder: '条件持续满足多少秒后触发告警'
    }
  },
  {
    label: '静默时间',
    key: 'silentSec',
    hide: true,
    componentProps: {
      type: 'number',
      min: 0,
      step: 1,
      placeholder: '告警触发后多少秒内不重复告警'
    }
  },
  {
    label: '告警恢复',
    key: 'enableRecover',
    hide: true,
    type: 'switch',
    slot: true,
    componentProps: { activeValue: true, inactiveValue: false },
  },
])

const data = ref([])
const crudRef = ref()
const tempConditions = reactive({
  logic: 'AND',
  items: [] as ConditionVO[],
})
const currentEditId = ref<number | null>(null)

const productKey = ref<string>('')
const propertyList = ref<any[]>([])
const propertyLoading = ref(false)

interface ConditionItem {
  type: 'property' | 'status'
  key: string
  operator: string
  value: string
}

const comparators = ref([
  { label: '等于', value: '==' },
  { label: '不等于', value: '!=' },
  { label: '大于', value: '>' },
  { label: '小于', value: '<' },
  { label: '大于等于', value: '>=' },
  { label: '小于等于', value: '<=' },
  { label: '在..之间', value: 'between' },
  { label: '不在..之间', value: 'notBetween' },
  { label: '包含', value: 'contains' },
  { label: '不包含', value: 'notContains' },
])

const propertyOptions = computed(() => {
  return propertyList.value.map(p => ({
    label: `${p.name} (${p.identifier})`,
    value: p.identifier,
    dataType: p.dataType?.type || 'string',
    specs: p.dataType?.specs || {}
  }))
})

const getPropertyDataType = (identifier: string) => {
  if (!identifier) return 'string'
  const property = propertyList.value.find((p: any) => p.identifier === identifier)
  if (!property) return 'string'
  const dataType = property.dataType?.type
  if (!dataType) return 'string'
  switch (dataType) {
    case 'int32':
    case 'int64':
      return 'int'
    case 'bool':
    case 'boolean':
      return 'bool'
    case 'text':
    case 'string':
      return 'string'
    case 'enum':
      return 'enum'
    default:
      return dataType
  }
}

const getPropertyInfo = (identifier: string) => {
  return propertyList.value.find((p: any) => p.identifier === identifier)
}

const getEnumOptions = (identifier: string) => {
  const property = getPropertyInfo(identifier)
  if (!property || property.dataType?.type !== 'enum') return []
  const specs = property.dataType?.specs || {}
  // specs 结构为 { "0": "名称1", "1": "名称2" }
  return Object.keys(specs).map(key => ({
    label: specs[key],
    value: key,
  }))
}

const getAvailableComparators = (item: ConditionItem) => {
  if (item.type === 'status') {
    return comparators.value.filter(c => ['==', '!='].includes(c.value))
  }
  const dataType = getPropertyDataType(item.key)
  switch (dataType) {
    case 'int':
    case 'float':
    case 'double':
    case 'long':
      return comparators.value
    case 'text':
    case 'string':
      return comparators.value.filter(c =>
        !['>', '<', '>=', '<=', 'between', 'notBetween'].includes(c.value)
      )
    case 'bool':
    case 'boolean':
      return comparators.value.filter(c =>
        ['==', '!='].includes(c.value)
      )
    case 'enum':
      return comparators.value.filter(c =>
        ['==', '!=', 'contains', 'notContains'].includes(c.value)
      )
    default:
      return comparators.value.filter(c =>
        ['==', '!=', 'contains', 'notContains'].includes(c.value)
      )
  }
}

const getValuePlaceholder = (item: ConditionItem) => {
  const comparator = item.operator
  if (!comparator) return '请输入值'
  const dataType = item.type === 'status' ? 'string' : getPropertyDataType(item.key)
  switch (comparator) {
    case 'between':
    case 'notBetween':
      return '格式: 最小值-最大值'
    case 'contains':
    case 'notContains':
      return '请输入要匹配的文本'
    case '>':
    case '<':
    case '>=':
    case '<=':
      return '请输入数值'
    case '==':
    case '!=':
      switch (dataType) {
        case 'bool':
          return '选择布尔值'
        case 'int':
        case 'long':
          return '请输入整数'
        case 'float':
        case 'double':
          return '请输入数值'
        case 'enum':
          return '请选择枚举值'
        default:
          return '请输入值'
      }
    default:
      return '请输入值'
  }
}

const onTypeChange = (item: ConditionItem) => {
  if (item.type === 'status') {
    item.key = 'online'
    item.operator = '=='
    item.value = 'online'
  } else {
    item.key = ''
    item.value = ''
    item.operator = ''
  }
}

const onKeyChange = (item: ConditionItem) => {
  const dataType = item.type === 'status' ? 'string' : getPropertyDataType(item.key)
  const operators = getAvailableComparators(item)
  if (operators.length > 0) {
    item.operator = operators[0].value
  }
  if (dataType === 'bool') {
    item.value = 'true'
  } else if (dataType === 'enum') {
    const enumOpts = getEnumOptions(item.key)
    item.value = enumOpts.length > 0 ? enumOpts[0].value : ''
  } else {
    item.value = ''
  }
}

const onOperatorChange = (item: ConditionItem) => {
  if (!item) return
  item.value = ''
}

const addCondition = () => {
  const newItem: ConditionItem = {
    type: 'property',
    key: '',
    operator: '',
    value: '',
  }
  tempConditions.items.push(newItem)
}

const removeCondition = (index: number) => {
  tempConditions.items.splice(index, 1)
}

const loadProductProperties = async () => {
  if (!props.deviceId && !props.productId) return

  propertyLoading.value = true
  try {
    if (props.deviceId) {
      const deviceInfo = await DeviceInfoApi.getDeviceInfo(props.deviceId)
      productKey.value = deviceInfo.productKey
    } else if (props.productId) {
      const productInfo = await ProductApi.getProduct(props.productId)
      productKey.value = productInfo.productKey
    }

    if (productKey.value) {
      const thingModel = await ThingModelApi.getThingModelByProductKey(productKey.value)
      propertyList.value = thingModel.model?.properties || []
    }
  } catch (error) {
    console.error('获取产品物模型失败:', error)
    propertyList.value = []
  } finally {
    propertyLoading.value = false
  }
}

const getData = () => {
  state.loading = true
  const queryParams: any = {
    ...state.page,
    ...state.query,
  }
  if (props.deviceId) {
    queryParams.deviceId = props.deviceId
  }
  if (props.productId) {
    queryParams.productId = props.productId
  }

  getDeviceAlertConfigPage(queryParams)
    .then((res) => {
      data.value = res.list.map((item: any) => ({
        ...item,
        conditionsStr: formatConditions(item.conditions),
      }))
      state.total = res.total
    })
    .finally(() => {
      state.loading = false
    })
}

const formatConditions = (conditions: any) => {
  if (!conditions || !Array.isArray(conditions)) return ''
  return conditions
    .map((item: any) => `${item.type === 'property' ? '属性' : '状态'}: ${item.key} ${item.operator} ${item.value}`)
    .join(' 且 ')
}

const onAddCallback = async (data: any) => {
  if (props.deviceId) data.deviceId = props.deviceId
  if (props.productId) data.productId = props.productId
  currentEditId.value = null
  tempConditions.logic = 'AND'
  tempConditions.items = []
  data.durationSec = 0
  data.silentSec = 0
  data.enableRecover = true
  await loadProductProperties()
  return data
}

const onEditCallback = async (data: any) => {
  currentEditId.value = data.id
  if (data.conditions && Array.isArray(data.conditions)) {
    tempConditions.logic = 'AND'
    tempConditions.items = [...data.conditions]
  } else {
    tempConditions.logic = 'AND'
    tempConditions.items = []
  }
  if (data.triggerOptions) {
    data.durationSec = data.triggerOptions.durationSec ?? 0
    data.silentSec = data.triggerOptions.silentSec ?? 0
    data.enableRecover = data.triggerOptions.enableRecover ?? true
  } else {
    data.durationSec = 0
    data.silentSec = 0
    data.enableRecover = true
  }
  await loadProductProperties()
  return data
}

const validateConditions = () => {
  if (tempConditions.items.length === 0) {
    ElMessage.warning('请至少添加一个告警条件')
    return false
  }

  for (let i = 0; i < tempConditions.items.length; i++) {
    const item = tempConditions.items[i]
    if (!item.type || !item.key || !item.operator || !item.value) {
      ElMessage.warning(`第 ${i + 1} 个条件的类型、属性/状态、运算符和值都必须填写`)
      return false
    }
  }

  return true
}

const onSave = ({ data: saveData, cancel }: any) => {
  if (!validateConditions()) {
    return
  }

  state.loading = true
  const triggerOptions: any = {
    durationSec: Number(saveData.durationSec) || 0,
    silentSec: Number(saveData.silentSec) || 0,
    enableRecover: saveData.enableRecover !== false,
  }

  const configData: any = {
    ...saveData,
    conditions: tempConditions.items.length > 0 ? tempConditions.items : undefined,
    triggerOptions: triggerOptions,
  }

  if (!saveData.id) {
    if (props.deviceId) {
      configData.productKey = productKey.value
      configData.deviceId = props.deviceId
      delete configData.productId
    } else if (props.productId) {
      configData.productKey = productKey.value
      delete configData.deviceId
    }
  }

  const request = saveData.id
    ? updateDeviceAlertConfig(configData)
    : createDeviceAlertConfig(configData)

  request
    .then(() => {
      ElMessage.success('保存成功')
      cancel()
      getData()
    })
    .finally(() => {
      state.loading = false
    })
}

const handleDelete = (row: any) => {
  state.loading = true
  deleteDeviceAlertConfig(row.id)
    .then(() => {
      ElMessage.success('删除成功')
      getData()
    })
    .finally(() => {
      state.loading = false
    })
}

const handleStatusChange = (row: any) => {
  if (!row || !row.id) return
  state.loading = true
  updateDeviceAlertConfig(row)
    .then(() => {
      ElMessage.success('状态更新成功')
    })
    .catch(() => {
      row.status = row.status === 0 ? 1 : 0
      ElMessage.error('状态更新失败')
    })
    .finally(() => {
      state.loading = false
    })
}

onMounted(async () => {
  getData()
  await loadProductProperties()
})
</script>

<style lang="scss" scoped>
.inline-condition-editor {
  .logic-selector {
    margin-bottom: 12px;
  }

  .condition-list {
    margin-bottom: 12px;

    .condition-item {
      padding: 8px;
      background-color: #f5f7fa;
      border-radius: 4px;
      margin-bottom: 8px;
    }
  }

  .empty-tip {
    margin: 16px 0;
  }

  .loading-tip {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 16px 0;
    color: #909399;
  }
}
</style>
