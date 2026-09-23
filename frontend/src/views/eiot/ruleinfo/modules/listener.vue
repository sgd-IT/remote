<template>
  <div>
    <div class="list-box">
      <el-collapse v-model="activeName">
        <el-collapse-item :name="index" v-for="(item, index) in list" :key="index">
          <template #title>
            <div class="flex" style="justify-content: space-between;width: 100%;">
              <div class="cu-title" @click.stop>
                <el-radio-group style="margin-right: 20px;" v-model="item.type" class="ml-4">
                  <el-radio label="device" size="large">设备监听</el-radio>
                </el-radio-group>
                <div class="item">
                  <select-product v-model:pk="item.pk" @on-select="(row) => handleSelectProduct(row, item)" />
                </div>
                <div class="item" v-if="item.pk">
                  <select-device v-model:dn="item.dn" placeholder="默认全部设备" :product-pk="item.pk || ''" @on-select="handleEmits" />
                </div>
              </div>
              <div style="padding-right: 10px;">
                <el-button @click="removeListener(index)">删除</el-button>
              </div>
            </div>
          </template>
          <div class="condition-box" v-if="item.pk">
            <div class="main">
              <div class="title">条件</div>
              <div class="main-box">
                <div class="box" v-for="(cond, condIndex) in item.conditions" :key="condIndex">
                  <div class="item">
                    <el-row style="width: 100%;">
                      <el-col :span="7">
                        <el-select v-model="cond.identifier" @change="(e) => conditionChange(cond, (stateMap.get(item.pk)?.modelItems || []), e)">
                          <el-option-group v-for="group in (stateMap.get(item.pk)?.modelItems || [])" :key="group.name" :label="group.name">
                            <el-option v-for="pro in group.items" :label="pro.name" :value="pro.identifier" :key="pro.identifier" />
                          </el-option-group>
                        </el-select>
                      </el-col>
                      <el-col :span="15" v-if="!cond?.identifier?.endsWith(':*')">
                        <el-row class="param-item" v-for="(param, paramIndex) in cond.parameters" :key="param.identifier">
                          <!-- 属性上报的属性选择 -->
                          <el-col :span="10" v-if="cond.identifier == 'report'">
                            <el-select v-model="param.identifier" style="width: 100%;" placeholder="选择属性">
                              <el-option
                                v-for="p in (stateMap.get(item.pk)?.properties || [])"
                                :label="p.name"
                                :value="p.identifier"
                                :key="p.identifier"
                              />
                            </el-select>
                          </el-col>
                          <!-- 事件的属性选择 -->
                          <el-col :span="10" v-else-if="cond.type == 'event' && cond.identifier && !cond.identifier.endsWith(':*')">
                            <el-select v-model="param.identifier" style="width: 100%;" placeholder="选择事件属性">
                              <el-option
                                v-for="p in getEventProperties(item.pk, cond.identifier)"
                                :label="p.name"
                                :value="p.identifier"
                                :key="p.identifier"
                              />
                            </el-select>
                          </el-col>
                          <!-- 服务的属性选择 -->
                          <el-col :span="10" v-else-if="cond.type == 'service' && cond.identifier && !cond.identifier.endsWith(':*')">
                            <el-select v-model="param.identifier" style="width: 100%;" placeholder="选择服务输出参数">
                              <el-option
                                v-for="p in getServiceProperties(item.pk, cond.identifier)"
                                :label="p.name"
                                :value="p.identifier"
                                :key="p.identifier"
                              />
                            </el-select>
                          </el-col>
                          <el-col :span="6">
                            <el-select v-model="param.comparator" @change="onComparatorChange(param)" placeholder="选择比较方式">
                              <el-option
                                v-for="cp in getAvailableComparators(item.pk, cond, param)"
                                :label="cp.name"
                                :value="cp.value"
                                :key="cp.value"
                              >
                                <span>{{ cp.name }}</span>
                                <span v-if="cp.description" style="color: #999; font-size: 12px; margin-left: 8px;">
                                  {{ cp.description }}
                                </span>
                              </el-option>
                            </el-select>
                          </el-col>
                          <el-col :span="5">
                            <el-input
                              v-model="param.value"
                              auto-complete="off"
                              :placeholder="getValuePlaceholder(param.comparator, item.pk, cond, param)"
                              @blur="validateParamValue(param, item.pk, cond)"
                            />
                          </el-col>
                          <el-col :span="1">
                            <el-button
                              style="margin-left: 6px;"
                              type="danger"
                              size="small"
                              circle
                              @click="removeParmeter(paramIndex, cond)"
                            >
                              <Icon icon="ep:delete" />
                            </el-button>
                          </el-col>
                        </el-row>
                        <!-- 表达式预览 -->
                        <el-row class="param-item" v-if="param?.comparator && param?.value">
                          <el-col :span="24">
                            <div class="expression-preview">
                              <Icon icon="ep:view" style="margin-right: 4px;" />
                              <span>{{ previewExpression(param) }}</span>
                            </div>
                          </el-col>
                        </el-row>
                      </el-col>
                      <el-col :span="2" v-if="!cond?.identifier?.endsWith(':*') && cond.identifier">
                        <el-button type="primary" size="small" circle @click="addParmeter(cond)">
                          <Icon icon="ep:plus" />
                        </el-button>
                      </el-col>
                    </el-row>
                  </div>
                  <el-button
                    type="danger"
                    size="small"
                    style="margin-left: 10px;"
                    @click="handleRemoveCondition(item, condIndex)"
                  >
                    <Icon icon="ep:delete" />
                  </el-button>
                </div>
              </div>
            </div>
            <el-button type="primary" size="small" style="margin-top: 12px;" @click="handleAddCondition(item)">新增条件</el-button>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
    <el-button style="margin-top: 10px;" @click="handleAdd">新增监听器</el-button>
  </div>
</template>
<script lang="ts" setup>
import { propTypes } from '@/utils/propTypes'
import { generateUUID } from '@/utils'

import SelectProduct from '@/components/EiotSelect/select-product.vue'
import SelectDevice from '@/components/EiotSelect/select-device.vue'
import {ThingModelApi} from "@/api/eiot/thingmodel";

const props = defineProps({
  listeners: propTypes.array.def([]),
})
const emits = defineEmits(['update:listeners'])
const arr: number[] = []
for (let i = 0; i < 100; i++) {
  arr.push(i)
}

const activeName = ref<number[]>(arr)
const list = ref<any[]>(props.listeners || [])

// 选择产品-调用物模型
const handleSelectProduct = (product, item) => {
  console.log('product', product)
  if (!product.productKey) return
  console.log('item', item)
  item.dn = ''
  getProductObjectModel(product.productKey)

}

const getProductObjectModel = (pk) => {
  if (!pk) {
    return
  }
  ThingModelApi.getThingModelByProductKey(pk).then((res) => {
    const data = res || {}
    initThingModel(pk, data)
  })
}
const initThingModel = (pk, res) => {
  const state: any = {
    modelItems: [],
    properties: [],
    events: [],
    services: [],
  }

  state.modelItems.push({
    name: '通配',
    items: [
      {
        type: 'state',
        identifier: 'state:*',
        name: '设备上下线',
      },
      {
        type: 'event',
        identifier: 'event:*',
        name: '任意事件上报',
      },
      {
        type: 'service_reply',
        identifier: 'service_reply:*',
        name: '任意服务回复',
      },
    ],
  })

  let items: any[] = []
  state.modelItems.push({
    name: '精确匹配',
    items: items,
  })
  items.push({
    type: 'property',
    identifier: 'report',
    name: '属性上报',
  })
  res?.model?.events &&
  res.model.events.forEach((s) => {
    items.push({
      type: 'event',
      identifier: s.identifier,
      name: s.name,
    })
  })
  res?.model?.services &&
  res.model.services.forEach((s) => {
    items.push({
      type: 'service',
      identifier: s.identifier,
      name: s.name,
    })
  })

  state.properties.push({
    identifier: '*',
    name: '任意',
  })
  res?.model?.properties &&
  res.model.properties.forEach((p) => {
    state.properties.push({
      identifier: p.identifier,
      name: p.name,
      dataType: p.dataType, // 保存完整的 dataType 信息
    })
  })

  res?.model?.events &&
  res.model.events.forEach((s) => {
    let items: any[] = []
    state.events.push({
      identifier: s.identifier,
      items: items,
    })

    s.outputData.forEach((p) => {
      items.push({
        identifier: p.identifier,
        name: p.name,
        dataType: p.dataType, // 保存完整的 dataType 信息
      })
    })
  })

  res?.model?.services &&
  res.model.services.forEach((s) => {
    let items: any[] = []
    state.services.push({
      identifier: s.identifier,
      items: items,
    })

    s.outputData.forEach((p) => {
      items.push({
        identifier: p.identifier,
        name: p.name,
        dataType: p.dataType, // 保存完整的 dataType 信息
      })
    })
  })
  stateMap.value.set(pk, state)
  handleEmits()
}

const stateMap = ref(new Map())
const handleEmits = () => {
  const arr = toRaw(list.value).map((m) => {
    let config = m
    console.log('config:', config)
    if (config.config) {
      config = JSON.parse(config.config || '{}')
    }
    if (!stateMap.value.has(config.pk)) getProductObjectModel(config.pk)
    return {
      ...config,
    }
  })
  list.value = arr
  emits('update:listeners', arr)
}
watch(
  () => list.value.length,
  (newV) => {
    handleEmits()
  },
  {
    immediate: true,
    // deep: true,
  }
)
// 新增监听器
const handleAdd = () => {
  list.value.push({
    type: 'device',
    conditions: [
      {
        parameters: [],
      },
    ],
  })
}

// 删除监听器
const removeListener = (index: number) => {
  list.value.splice(index, 1)
}
/**
 * 条件比较器配置 - 与Expression.java中的表达式处理保持一致
 *
 * 对应Expression.java中的eval方法支持的操作符:
 * - == : 字符串相等比较 (value.equals(triggerValue))
 * - != : 字符串不等比较 (!value.equals(triggerValue))
 * - > : 数值大于比较 (Double.parseDouble(value) > Double.parseDouble(triggerValue))
 * - < : 数值小于比较 (Double.parseDouble(value) < Double.parseDouble(triggerValue))
 * - >= : 数值大于等于比较 (Double.parseDouble(value) >= Double.parseDouble(triggerValue))
 * - <= : 数值小于等于比较 (Double.parseDouble(value) <= Double.parseDouble(triggerValue))
 * - between : 数值范围内比较，格式: "最小值-最大值" (min <= value <= max)
 * - notBetween : 数值范围外比较，格式: "最小值-最大值" (value <= min || value >= max)
 * - contain : 字符串包含比较 (value.contains(triggerValue))
 * - notContain : 字符串不包含比较 (!value.contains(triggerValue))
 */
const comparators = ref([
  {
    name: '等于',
    value: '==',
    description: '字符串相等比较',
  },
  {
    name: '不等于',
    value: '!=',
    description: '字符串不等比较',
  },
  {
    name: '大于',
    value: '>',
    description: '数值大于比较',
  },
  {
    name: '小于',
    value: '<',
    description: '数值小于比较',
  },
  {
    name: '大于等于',
    value: '>=',
    description: '数值大于等于比较',
  },
  {
    name: '小于等于',
    value: '<=',
    description: '数值小于等于比较',
  },
  {
    name: '在..之间',
    value: 'between',
    description: '数值范围内，格式: 最小值-最大值 (如: 10-20)',
  },
  {
    name: '不在..之间',
    value: 'notBetween',
    description: '数值范围外，格式: 最小值-最大值 (如: 10-20)',
  },
  {
    name: '包含',
    value: 'contain',
    description: '字符串包含比较',
  },
  {
    name: '不包含',
    value: 'notContain',
    description: '字符串不包含比较',
  },
])

// 获取事件的属性列表
const getEventProperties = (pk: string, eventIdentifier: string) => {
  const state = stateMap.value.get(pk)
  if (!state || !state.events) return []

  const event = state.events.find(e => e.identifier === eventIdentifier)
  if (!event || !event.items) return []

  // 添加"任意"选项和具体属性
  return [
    { identifier: '*', name: '任意属性' },
    ...event.items
  ]
}

// 获取服务的输出参数列表
const getServiceProperties = (pk: string, serviceIdentifier: string) => {
  const state = stateMap.value.get(pk)
  if (!state || !state.services) return []

  const service = state.services.find(s => s.identifier === serviceIdentifier)
  if (!service || !service.items) return []

  // 添加"任意"选项和具体参数
  return [
    { identifier: '*', name: '任意参数' },
    ...service.items
  ]
}

// 根据属性类型获取可用的比较器
const getAvailableComparators = (pk: string, cond: any, param: any) => {
  // 确保comparators存在
  if (!comparators.value || !Array.isArray(comparators.value) || comparators.value.length === 0) {
    return []
  }

  // 检查必要的参数
  if (!pk || !cond || !param) {
    return comparators.value
  }

  // 如果没有选择具体属性，返回所有比较器
  if (!param.identifier || param.identifier === '*') {
    return comparators.value
  }

  // 获取属性的数据类型
  const propertyType = getPropertyDataType(pk, cond, param)

  // 根据数据类型过滤比较器
  switch (propertyType) {
    case 'int':
    case 'float':
    case 'double':
    case 'long':
    case 'int32':
    case 'int64':
      // 数值类型：支持所有比较器
      return comparators.value

    case 'text':
    case 'string':
      // 字符串类型：不支持数值比较
      return comparators.value.filter(c =>
        !['>', '<', '>=', '<=', 'between', 'notBetween'].includes(c.value)
      )

    case 'bool':
    case 'boolean':
      // 布尔类型：只支持等于和不等于
      return comparators.value.filter(c =>
        ['==', '!='].includes(c.value)
      )

    case 'enum':
      // 枚举类型：支持等于、不等于、包含
      return comparators.value.filter(c =>
        ['==', '!=', 'contain', 'notContain'].includes(c.value)
      )

    default:
      // 未知类型：返回基础比较器
      return comparators.value.filter(c =>
        ['==', '!=', 'contain', 'notContain'].includes(c.value)
      )
  }
}

// 获取属性的数据类型
const getPropertyDataType = (pk: string, cond: any, param: any) => {
  // 防御性检查
  if (!pk || !cond || !param) {
    return 'string'
  }

  const state = stateMap.value.get(pk)

  if (!state) return 'string'

  let dataTypeInfo = null

  // 根据条件类型获取属性信息
  if (cond.identifier === 'report') {
    // 属性上报：从properties中查找
    const property = state.properties?.find(p => p.identifier === param.identifier)
    dataTypeInfo = property?.dataType
  } else if (cond.type === 'event') {
    // 事件：从events中查找
    const event = state.events?.find(e => e.identifier === cond.identifier)
    const eventParam = event?.items?.find(p => p.identifier === param.identifier)
    dataTypeInfo = eventParam?.dataType
  } else if (cond.type === 'service') {
    // 服务：从services中查找
    const service = state.services?.find(s => s.identifier === cond.identifier)
    const serviceParam = service?.items?.find(p => p.identifier === param.identifier)
    dataTypeInfo = serviceParam?.dataType
  }

  // 处理物模型中的数据类型结构
  if (!dataTypeInfo) return 'string'

  // 如果dataType是对象且包含type字段，则使用type字段的值
  if (typeof dataTypeInfo === 'object' && dataTypeInfo.type) {
    const type = dataTypeInfo.type
    // 标准化数据类型名称
    switch (type) {
      case 'int32':
      case 'int64':
        return 'int'
      case 'float':
      case 'double':
        return 'float'
      case 'bool':
      case 'boolean':
        return 'bool'
      case 'text':
      case 'string':
        return 'string'
      case 'enum':
        return 'enum'
      case 'array':
        return 'array'
      case 'struct':
        return 'struct'
      default:
        return type
    }
  }

  // 如果dataType是字符串，直接返回
  if (typeof dataTypeInfo === 'string') {
    return dataTypeInfo
  }

  return 'string'
}

const conditionChange = (cond, list, e) => {
  // 清空之前的参数
  cond.parameters = []

  for (let i in list) {
    for (let k in list[i].items) {
      const item = list[i].items[k]
      if (item.identifier === e) {
        cond.type = item.type || ''

        // 如果是事件、服务或属性上报类型，且不是通配符，自动添加一个参数
        if ((item.type === 'event' || item.type === 'service' || e === 'report') && !e.endsWith(':*')) {
          cond.parameters = [{}]
        }
        return
      }
    }
  }
}

// 获取参数值输入提示
const getValuePlaceholder = (comparator, pk?: string, cond?: any, param?: any) => {
  if (!comparator) return '请输入值'

  // 获取属性数据类型以提供更精确的提示
  const dataType = pk && cond && param ? getPropertyDataType(pk, cond, param) : 'string'

  switch (comparator) {
    case 'between':
    case 'notBetween':
      if (['int', 'float', 'double', 'long'].includes(dataType)) {
        return '请输入数值范围，格式: 最小值-最大值 (如: 10-20)'
      }
      return '请输入范围值，格式: 最小值-最大值 (如: 10-20)'
    case 'contain':
    case 'notContain':
      return '请输入要匹配的文本内容'
    case '>':
    case '<':
    case '>=':
    case '<=':
      if (['int', 'float', 'double', 'long'].includes(dataType)) {
        return '请输入数值 (如: 100)'
      }
      return '请输入数值'
    case '==':
    case '!=':
      switch (dataType) {
        case 'bool':
        case 'boolean':
          return '请输入true或false'
        case 'int':
        case 'long':
          return '请输入整数 (如: 100)'
        case 'float':
        case 'double':
          return '请输入数值 (如: 100.5)'
        case 'enum':
          return '请输入枚举值'
        default:
          return '请输入比较值'
      }
    default:
      return '请输入值'
  }
}

// 比较器变化处理
const onComparatorChange = (param) => {
  if (!param) return
  // 清空之前的值，避免格式不匹配
  param.value = ''
}

// 验证参数值
const validateParamValue = (param, pk?: string, cond?: any) => {
  if (!param || !param.value || !param.comparator) return true

  const { comparator, value } = param

  // 获取属性数据类型
  const dataType = pk && cond ? getPropertyDataType(pk, cond, param) : 'string'

  // 验证between和notBetween格式
  if (comparator === 'between' || comparator === 'notBetween') {
    const parts = value.split('-')
    if (parts.length !== 2) {
      ElMessage.warning('范围值格式错误，请使用格式: 最小值-最大值 (如: 10-20)')
      return false
    }

    const min = parseFloat(parts[0])
    const max = parseFloat(parts[1])

    if (isNaN(min) || isNaN(max)) {
      ElMessage.warning('范围值必须是数字')
      return false
    }

    if (min >= max) {
      ElMessage.warning('最小值必须小于最大值')
      return false
    }
  }

  // 验证数值类型操作符
  if (['>', '<', '>=', '<='].includes(comparator)) {
    if (isNaN(parseFloat(value))) {
      ElMessage.warning('该操作符需要输入数值')
      return false
    }
  }

  // 根据数据类型验证值
  switch (dataType) {
    case 'int':
    case 'long':
      if (!['between', 'notBetween'].includes(comparator)) {
        const intValue = parseInt(value)
        if (isNaN(intValue) || intValue.toString() !== value.trim()) {
          ElMessage.warning('请输入有效的整数')
          return false
        }
      }
      break
    case 'float':
    case 'double':
      if (!['between', 'notBetween'].includes(comparator)) {
        const floatValue = parseFloat(value)
        if (isNaN(floatValue)) {
          ElMessage.warning('请输入有效的数值')
          return false
        }
      }
      break
    case 'bool':
    case 'boolean':
      const boolValue = value.toLowerCase().trim()
      if (!['true', 'false', '1', '0'].includes(boolValue)) {
        ElMessage.warning('布尔值只能是true、false、1或0')
        return false
      }
      break
  }

  return true
}

// 验证所有监听器配置
const validateAllListeners = () => {
  for (const listener of list.value) {
    if (!listener.conditions || listener.conditions.length === 0) {
      ElMessage.error('请至少配置一个监听条件')
      return false
    }

    for (const condition of listener.conditions) {
      if (!condition.identifier) {
        ElMessage.error('请选择监听条件类型')
        return false
      }

      if (!condition.identifier.endsWith(':*') && (!condition.parameters || condition.parameters.length === 0)) {
        ElMessage.error('请至少配置一个参数条件')
        return false
      }

      if (condition.parameters) {
        for (const param of condition.parameters) {
          if (!param.comparator) {
            ElMessage.error('请选择比较操作符')
            return false
          }

          if (!param.value) {
            ElMessage.error('请输入比较值')
            return false
          }

          // 调用单个参数验证
          if (!validateParamValue(param)) {
            return false
          }
        }
      }
    }
  }

  return true
}

// 暴露验证方法给父组件
defineExpose({
  validateAllListeners
})

// 新增条件
const handleAddCondition = (item: any) => {
  if (!item.conditions) item.conditions = []
  item.conditions.push({
    identifier: '',
    type: '',
    parameters: []
  })
}
// 删除条件
const handleRemoveCondition = (item: any, index: number) => {
  item.conditions.splice(index, 1)
}

// 新增参数
const addParmeter = (cond: any) => {
  if (!cond.parameters) cond.parameters = []
  cond.parameters.push({
    identifier: '',
    comparator: '',
    value: ''
  })
}
// 删除参数
const removeParmeter = (index: number, cond: any) => {
  cond.parameters.splice(index, 1)
}

// 表达式预览和测试功能
const previewExpression = (param) => {
  if (!param || !param.comparator || !param.value) {
    return '请完善条件配置'
  }

  const { comparator, value, identifier } = param
  const field = identifier || '设备属性'

  try {
    switch (comparator) {
      case '==':
        return `当 ${field} 等于 "${value}" 时触发`
      case '!=':
        return `当 ${field} 不等于 "${value}" 时触发`
      case '>':
        return `当 ${field} 大于 ${value} 时触发`
      case '<':
        return `当 ${field} 小于 ${value} 时触发`
      case '>=':
        return `当 ${field} 大于等于 ${value} 时触发`
      case '<=':
        return `当 ${field} 小于等于 ${value} 时触发`
      case 'between':
        if (value && value.includes('-')) {
          const [min, max] = value.split('-')
          return `当 ${field} 在 ${min} 到 ${max} 之间时触发`
        }
        return `当 ${field} 在指定范围内时触发`
      case 'notBetween':
        if (value && value.includes('-')) {
          const [min2, max2] = value.split('-')
          return `当 ${field} 不在 ${min2} 到 ${max2} 之间时触发`
        }
        return `当 ${field} 不在指定范围内时触发`
      case 'contain':
        return `当 ${field} 包含 "${value}" 时触发`
      case 'notContain':
        return `当 ${field} 不包含 "${value}" 时触发`
      default:
        return '未知条件类型'
    }
  } catch (error) {
    console.error('表达式预览错误:', error)
    return '表达式格式错误'
  }
}

onUnmounted(() => {
  console.log('onUnmounted')
  list.value = []
})
</script>

<style lang="scss" scoped>
.list-box {
  margin-top: 10px;
  .cu-title {
    width: calc(100% - 30px);
    cursor: auto;
    display: flex;
    align-items: center;
    .item {
      width: 250px;
      margin-right: 10px;
    }
  }
}
::v-deep(.el-collapse-item__header) {
  background-color: #f2f2f2;
  padding: 0 12px;
}
.condition-box {
  background-color: #f2f2f2;
  padding: 15px;
  border-top: 1px solid #d9d9d9;
  .main {
    border: 2px dashed rgb(217, 217, 217);
    .title {
      padding: 12px;
      color: #333;
      font-weight: 600;
      border-bottom: 1px solid rgb(217, 217, 217);
    }
    .main-box {
      padding: 10px;
      .box {
        display: flex;
        align-items: center;
        .item {
          border: 2px dashed rgb(217, 217, 217);
          padding: 6px;
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 10px;
          flex: 1;
          .param-item {
            margin-bottom: 8px;
          }
        }
        // .el-button {
        //   width: 50px;
        // }
      }
    }
  }
}

.expression-preview {
  background-color: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
  padding: 8px 12px;
  margin-top: 8px;
  font-size: 12px;
  color: #1e40af;
  display: flex;
  align-items: center;

  .el-icon {
    color: #3b82f6;
  }
}
</style>
