<template>
  <div>
    <div class="list-box">
      <el-collapse v-model="activeName">
        <el-collapse-item :name="index" v-for="(item, index) in list" :key="index">
          <template #title>
            <div class="flex" style="justify-content: space-between;width: 100%;">
              <div class="cu-title" @click.stop>
                <el-radio-group style="margin-right: 20px;" model-value="1" class="ml-4">
                  <el-radio label="1" size="large">设备数据过滤</el-radio>
                </el-radio-group>
                <div class="item">
                  <el-radio-group v-model="item.deviceRadio">
                    <el-radio-button label="指定设备" />
                    <el-radio-button label="使用当前设备" />
                  </el-radio-group>
                </div>
                <div class="item" v-if="item.deviceRadio === '指定设备'">
                  <select-device v-model:dn="item.dn" placeholder="选择设备" @on-select="(row) => hadnleSelectDevice(row, item)" />
                </div>
              </div>
              <div style="padding-right: 10px;">
                <el-button @click="removeFliter(index)">删除</el-button>
              </div>
            </div>
          </template>
          <div class="condition-box" v-if="item.pk || item.deviceRadio === '使用当前设备'">
            <div class="flex items-center" style="padding: 10px;"> 
              <div class="mr-4">条件组合</div> 
              <el-radio-group v-model="item.cond"> 
                <el-radio :label="2">全部满足（AND）</el-radio> 
                <el-radio :label="1">任意满足（OR）</el-radio> 
                <el-radio :label="3">全部不满足（NOT）</el-radio> 
              </el-radio-group> 
            </div>
            <div class="main">
              <div class="title">条件</div>
              <div class="main-box">
                <div class="box" v-for="(cond, condIndex) in item.conditions" :key="condIndex">
                  <div class="item">
                    <el-row style="width: 100%;">
                      <el-col :span="4">
                        <el-select v-model="cond.type" placeholder="类型" @change="onTypeChange(cond)">
                          <el-option label="设备属性" value="property" />
                          <el-option label="设备状态" value="state" />
                          <el-option label="设备标签" value="tag" />
                        </el-select>
                      </el-col>
                      <el-col :span="6">
                        <el-select
                          v-model="cond.identifier"
                          placeholder="选择属性/状态"
                          @change="(e) => conditionChange(cond, e)"
                        >
                           <el-option
                             v-for="opt in getOptions(cond, item)"
                             :key="opt.identifier"
                             :label="opt.name"
                             :value="opt.identifier"
                           />
                        </el-select>
                      </el-col>
                      <el-col :span="14">
                        <el-row class="param-item">
                          <el-col :span="8">
                            <el-select v-model="cond.comparator" @change="onComparatorChange(cond)" placeholder="选择比较方式">
                              <el-option
                                v-for="cp in getAvailableComparators(getPk(item), cond.identifier)"
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
                          <el-col :span="16">
                            <el-input
                              v-model="cond.value"
                              auto-complete="off"
                              :placeholder="getValuePlaceholder(cond.comparator, getPk(item), cond.identifier)"
                              @blur="validateParamValue(cond, getPk(item))"
                            />
                          </el-col>
                        </el-row>
                      </el-col>
                    </el-row>
                  </div>
                  <el-button type="danger" size="small" style="margin-left: 10px;" @click="handleRemoveCondition(item, condIndex)">
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
    <el-button style="margin-top: 10px;" @click="handleAdd">新增过滤器</el-button>
  </div>
</template>
<script lang="ts" setup>
import { propTypes } from '@/utils/propTypes'

import SelectDevice from '@/components/EiotSelect/select-device.vue'
import {ThingModelApi} from "@/api/eiot/thingmodel";

const props = defineProps({
  filters: propTypes.array.def([]),
  listeners: propTypes.array.def([]),
})
const emits = defineEmits(['update:filters'])
const arr: number[] = []
for (let i = 0; i < 100; i++) {
  arr.push(i)
}
const activeName = ref<number[]>(arr)
const list = ref<any[]>(props.filters || [])

const hadnleSelectDevice = (device, row) => {
  if (!device.productKey) return
  row.pk = device.productKey
  getProductObjectModel(device.productKey)
}
const getProductObjectModel = (pk) => {
  if (!pk) {
    return
  }
  ThingModelApi.getThingModelByProductKey(pk).then((res) => {
    const data = res || {}
    initThingModel(pk, data)
  });
}

const stateMap = ref(new Map())
const initThingModel = (pk, res) => {
  const modelItems: any[] = []
  let items: any[] = []
  modelItems.push({
    name: '属性',
    items: items,
  })
  res?.model?.properties &&
    res.model.properties.forEach((p) => {
      items.push({
        type: 'property',
        identifier: p.identifier,
        name: p.name,
        dataType: p.dataType,
      })
    })

  modelItems.push({
    name: '状态',
    items: [
      {
        type: 'state',
        identifier: 'online',
        name: '是否在线',
      },
    ],
  })
  stateMap.value.set(pk, modelItems)
  handleEmits()
}

const getPk = (item) => {
  if (item.deviceRadio === '指定设备') {
    return item.pk
  }
  if (props.listeners && props.listeners.length > 0) {
    const l = props.listeners.find(x => x.type === 'device' && x.pk)
    return l ? l.pk : null
  }
  return null
}

watch(
  () => props.listeners,
  (val) => {
    if (val) {
      val.forEach((l) => {
        if (l.type === 'device' && l.pk) {
          getProductObjectModel(l.pk)
        }
      })
    }
  },
  { deep: true, immediate: true }
)

const handleEmits = () => {
  const arr = toRaw(list.value).map((m) => {
    let config = m
    if (config.config) {
      config = JSON.parse(config.config || '{}')
    }
    if (!stateMap.value.has(config.pk)) getProductObjectModel(config.pk)
    if (config.cond === undefined) config.cond = 2
    
    // Ensure type is set for conditions
    if (config.conditions) {
      config.conditions.forEach((c: any) => {
        if (!c.type && c.identifier && config.pk) {
           const groups = stateMap.value.get(config.pk)
           if (groups) {
             for (const g of groups) {
               const found = g.items.find((i: any) => i.identifier === c.identifier)
               if (found) {
                 c.type = found.type
                 break
               }
             }
           }
        }
      })
    }
    
    return {
      ...config,
    }
  })
  list.value = arr
  emits('update:filters', arr)
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
// 新增过滤器
const handleAdd = () => {
  list.value.push({
    deviceRadio: '指定设备',
    cond: 2,
    conditions: [
      {
        parameters: [],
      },
    ],
  })
}

// 删除过滤器
const removeFliter = (index: number) => {
  list.value.splice(index, 1)
}
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

// 根据属性类型获取可用的比较器
const getAvailableComparators = (pk: string, identifier: string) => {
  // 确保comparators存在
  if (!comparators.value || !Array.isArray(comparators.value) || comparators.value.length === 0) {
    return []
  }

  // 检查必要的参数
  if (!pk || !identifier) {
    return comparators.value
  }

  // 获取属性的数据类型
  const propertyType = getPropertyDataType(pk, identifier)

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
const getPropertyDataType = (pk: string, identifier: string) => {
  if (!pk || !identifier) return 'string'
  const groups = stateMap.value.get(pk)
  if (!groups) return 'string'

  let dataTypeInfo = null
  for (const group of groups) {
    const item = group.items.find(i => i.identifier === identifier)
    if (item) {
       dataTypeInfo = item.dataType
       break
    }
  }

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

// 获取参数值输入提示
const getValuePlaceholder = (comparator, pk?: string, identifier?: string) => {
  if (!comparator) return '请输入值'

  // 获取属性数据类型以提供更精确的提示
  const dataType = pk && identifier ? getPropertyDataType(pk, identifier) : 'string'

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
const validateParamValue = (param, pk?: string) => {
  if (!param || !param.value || !param.comparator) return true
}

// 新增条件
const handleAddCondition = (item: any) => {
  if (!item.conditions) item.conditions = []
  item.conditions.push({})
}
// 删除条件
const handleRemoveCondition = (item: any, index: number) => {
  item.conditions.splice(index, 1)
}

// 新增参数
const addParmeter = (cond: any) => {
  if (!cond.parameters) cond.parameters = []
  cond.parameters.push({})
}
// 删除参数
const removeParmeter = (index: number, cond: any) => {
  cond.parameters.splice(index, 1)
}
const conditionChange = (cond, e) => {
  // identifier changed
  // maybe reset comparator or value
}

// 监听类型变化
const onTypeChange = (cond: any) => {
  cond.identifier = ''
  cond.value = ''
  cond.comparator = ''
}

// 获取选项列表
const getOptions = (cond: any, item: any) => {
  const pk = getPk(item)
  if (!pk) return []
  const groups = stateMap.value.get(pk)
  if (!groups) return []
  
  if (!cond.type) return []
  
  let result: any[] = []
  groups.forEach((g: any) => {
    g.items.forEach((i: any) => {
      if (i.type === cond.type) {
        result.push(i)
      }
    })
  })
  return result
}

onUnmounted(() => {
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
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 0px;
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
</style>
