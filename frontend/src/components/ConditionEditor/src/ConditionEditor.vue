<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    :width="width"
    @closed="onDialogClosed"
  >
    <div class="condition-editor">
      <div class="logic-selector">
        <el-radio-group v-model="internalConditions.logic">
          <el-radio label="AND">所有条件满足 (AND)</el-radio>
          <el-radio label="OR">任一条件满足 (OR)</el-radio>
        </el-radio-group>
      </div>

      <div class="condition-list">
        <div
          v-for="(item, index) in internalConditions.items"
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
              <el-select
                v-model="item.key"
                placeholder="选择属性/状态"
                filterable
                @change="() => onKeyChange(item)"
              >
                <template v-if="item.type === 'property'">
                  <el-option
                    v-for="prop in propertyOptions"
                    :key="prop.value"
                    :label="prop.label"
                    :value="prop.value"
                  />
                </template>
                <template v-else-if="item.type === 'status'">
                  <el-option
                    v-for="status in deviceStatusOptions"
                    :key="status.value"
                    :label="status.label"
                    :value="status.value"
                  />
                </template>
              </el-select>
            </el-col>

            <el-col :span="14">
              <el-row :gutter="5">
                <el-col :span="8">
                  <el-select
                    v-model="item.operator"
                    placeholder="选择运算符"
                    @change="() => onOperatorChange(item)"
                  >
                    <el-option
                      v-for="op in getAvailableComparators(item)"
                      :key="op.value"
                      :label="op.label"
                      :value="op.value"
                    >
                      <span>{{ op.label }}</span>
                      <span v-if="op.description" style="color: #999; font-size: 12px; margin-left: 8px;">
                        {{ op.description }}
                      </span>
                    </el-option>
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
                    @blur="() => validateParamValue(item)"
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

      <div v-if="!propertiesLoaded && !loading" class="empty-tip">
        <el-empty description="暂无属性，请先在物模型中添加属性">
          <el-button type="primary" @click="$emit('load-properties')">加载属性</el-button>
        </el-empty>
      </div>

      <el-button type="primary" plain @click="addCondition">添加条件</el-button>
    </div>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { propTypes } from '@/utils/propTypes'

export interface ConditionItem {
  type: 'property' | 'status'
  key: string
  operator: string
  value: string
}

export interface ConditionConfig {
  logic: 'AND' | 'OR'
  items: ConditionItem[]
}

export interface PropertyOption {
  label: string
  value: string
  dataType?: string
  specs?: any
}

const props = defineProps({
  modelValue: propTypes.bool.def(false),
  title: propTypes.string.def('配置告警条件'),
  width: propTypes.string.def('900px'),
  conditions: {
    type: Object as () => ConditionConfig,
    default: () => ({ logic: 'AND', items: [] })
  },
  propertyList: {
    type: Array as () => any[],
    default: () => []
  },
  productKey: propTypes.string.def(''),
  loading: propTypes.bool.def(false),
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'update:conditions': [value: ConditionConfig]
  'confirm': [value: ConditionConfig]
  'cancel': []
  'load-properties': []
  'add-condition': [item: ConditionItem]
  'remove-condition': [index: number]
}>()

const internalConditions = reactive<ConditionConfig>({
  logic: 'AND',
  items: []
})

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const propertiesLoaded = computed(() => props.propertyList.length > 0 || props.productKey)

const deviceStatusOptions = [
  { label: '是否在线', value: 'online' },
]

const comparators = ref([
  { label: '等于', value: '==', description: '相等比较' },
  { label: '不等于', value: '!=', description: '不等比较' },
  { label: '大于', value: '>', description: '数值大于比较' },
  { label: '小于', value: '<', description: '数值小于比较' },
  { label: '大于等于', value: '>=', description: '数值大于等于比较' },
  { label: '小于等于', value: '<=', description: '数值小于等于比较' },
  { label: '在..之间', value: 'between', description: '范围内，格式: 最小值-最大值' },
  { label: '不在..之间', value: 'notBetween', description: '范围外，格式: 最小值-最大值' },
  { label: '包含', value: 'contains', description: '字符串包含比较' },
  { label: '不包含', value: 'notContains', description: '字符串不包含比较' },
])

const propertyOptions = computed<PropertyOption[]>(() => {
  return props.propertyList.map(p => ({
    label: `${p.name} (${p.identifier})`,
    value: p.identifier,
    dataType: p.dataType?.type || 'string',
    specs: p.dataType?.specs || {}
  }))
})

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
    case 'int32':
    case 'int64':
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

const getPropertyDataType = (identifier: string) => {
  if (!identifier) return 'string'
  const property = props.propertyList.find(p => p.identifier === identifier)
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
  return props.propertyList.find(p => p.identifier === identifier)
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

const getValuePlaceholder = (item: ConditionItem) => {
  const comparator = item.operator
  if (!comparator) return '请输入值'

  const dataType = item.type === 'status' ? 'string' : getPropertyDataType(item.key)

  switch (comparator) {
    case 'between':
    case 'notBetween':
      if (['int', 'float', 'double', 'long'].includes(dataType)) {
        return '格式: 最小值-最大值 (如: 10-20)'
      }
      return '格式: 最小值-最大值'
    case 'contains':
    case 'notContains':
      return '请输入要匹配的文本'
    case '>':
    case '<':
    case '>=':
    case '<=':
      if (['int', 'float', 'double', 'long'].includes(dataType)) {
        return '请输入数值'
      }
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

const validateParamValue = (item: ConditionItem) => {
  if (!item || !item.value || !item.operator) return true
}

const onTypeChange = (item: ConditionItem) => {
  item.key = ''
  item.value = ''
  item.operator = ''
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
  internalConditions.items.push(newItem)
  emit('add-condition', newItem)
}

const removeCondition = (index: number) => {
  internalConditions.items.splice(index, 1)
  emit('remove-condition', index)
}

const handleConfirm = () => {
  emit('update:conditions', { ...internalConditions })
  emit('confirm', { ...internalConditions })
  dialogVisible.value = false
}

const handleCancel = () => {
  emit('cancel')
  dialogVisible.value = false
}

const onDialogClosed = () => {
  // Reset internal state if needed
}

watch(
  () => props.conditions,
  (newVal) => {
    if (newVal) {
      internalConditions.logic = newVal.logic || 'AND'
      internalConditions.items = [...(newVal.items || [])]
    }
  },
  { immediate: true, deep: true }
)

watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal && props.conditions) {
      internalConditions.logic = props.conditions.logic || 'AND'
      internalConditions.items = [...(props.conditions.items || [])]
    }
  }
)
</script>

<style lang="scss" scoped>
.condition-editor {
  .logic-selector {
    margin-bottom: 16px;
  }

  .condition-list {
    margin-bottom: 16px;

    .condition-item {
      padding: 10px;
      background-color: #f2f2f2;
      border-radius: 4px;
      margin-bottom: 8px;
    }
  }

  .empty-tip {
    margin: 20px 0;
  }
}
</style>
