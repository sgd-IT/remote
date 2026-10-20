<template>
  <yt-crud
    ref="crudRef"
    :data="data"
    :column="column"
    :table-props="{
        selection: false,
      }"
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
    <template #pushPostIds="scope">
      <span>{{ getPostNames(scope.row.pushPostIds) }}</span>
    </template>
    <template #algorithmAlertType="scope">
      <span>{{ getAlgorithmAlertTypeName(scope.row.algorithmAlertType) }}</span>
    </template>
  </yt-crud>
</template>

<script lang="ts" setup>
import { nextTick } from 'vue'
import { IColumn } from '@/components/common/types/tableCommon'
import { getConfigList, saveConfig, deleteConfig } from '@/api/eiot/alarm/alarm.api'
import { getRuleList } from '@/api/eiot/ruleEngine/rule.api'
import YtCrud from '@/components/common/yt-crud.vue'
import { getConfigAll } from '@/api/eiot/channel/configs.api'
import {getTemplatesList} from "@/api/eiot/channel/templates.api";
import { getSimplePostList, PostVO } from '@/api/system/post'

const state = reactive({
  page: {
    pageSize: 10,
    pageNo: 1,
  },
  total: 0,
  loading: false,
  query: {},
})
const column = ref<IColumn[]>([{
  label: '配置名称',
  key: 'name',
  search: true,
  rules: [{ required: true, message: '配置名称不能为空' }],
}, {
  // 主机事件类别标签按库内事实校正（1火警/2故障/3电源故障/4离线/5其他，与后端归一映射一致）
  label: '告警类型',
  key: 'level',
  tableWidth: 120,
  search: true,
  type: 'select',
  rules: [{ required: true, message: '请选择告警类型' }],
  componentProps: {
    style: 'width: 150px',
    options: [{
      label: '火警',
      value: 1,
    }, {
      label: '故障',
      value: 2,
    }, {
      label: '电源故障',
      value: 3,
    }, {
      label: '离线',
      value: 4,
    }, {
      label: '其他',
      value: 5,
    }],
  }
}, {
  label: '消息模板',
  key: 'messageTemplateId',
  type: 'select',
  rules: [{ required: true, message: '请选择消息模板' }],
  tableWidth: 120,
  componentProps: {
    labelAlias: 'title',
    valueAlias: 'id',
    options: []
  }
}, {
  label: '算法报警类型',
  key: 'algorithmAlertType',
  type: 'select',
  slot: true,
  tableWidth: 150,
  componentProps: {
    clearable: true,
    placeholder: '不选则需绑定规则',
    options: [{
      label: '高温事件报警',
      value: 1,
    }, {
      label: '热成像事件报警',
      value: 2,
    }, {
      label: '烟火事件报警',
      value: 3,
    }, {
      label: '离岗事件报警',
      value: 4,
    }, {
      label: '消防通道占用事件报警',
      value: 5,
    }]
  }
}, {
  label: '规则',
  key: 'ruleInfoId',
  tableWidth: 120,
  type: 'select',
  componentProps: {
    labelAlias: 'name',
    valueAlias: 'id',
    options: [],
    clearable: true,
    placeholder: '选择算法报警类型后可不选'
  }
}, {
  label: '消息推送岗位',
  key: 'pushPostIds',
  slot: true,
  tableWidth: 150,
  type: 'select',
  componentProps: {
    multiple: true,
    labelAlias: 'name',
    valueAlias: 'id',
    maxCollapseTags: 3,
    collapseTags: true,
    options: []
  }
}, {
  label: '是否启用',
  key: 'status',
  tableWidth: 120,
  type: 'switch',
  slot: true,
  componentProps: {
    activeValue: 0,
    inactiveValue: 1,
  }
}, {
  label: '告警描述',
  key: 'description',
  hide: true,
  componentProps: {
    type: 'textarea',
    rows: 4,
  }
}])

const isInitialized = ref(false) // 标记是否已初始化完成

// 岗位列表（用于表格列渲染）
const postOptions = ref<PostVO[]>([])
const getPostNames = (ids: number[]) => {
  if (!ids || ids.length === 0) return '--'
  return ids.map(id => postOptions.value.find(p => p.id === id)?.name || id).join('、')
}

// 算法报警类型显示映射
const algorithmAlertTypeNames: Record<number, string> = {
  1: '高温事件报警',
  2: '热成像事件报警',
  3: '烟火事件报警',
  4: '离岗事件报警',
  5: '消防通道占用事件报警',
}
const getAlgorithmAlertTypeName = (v?: number) => (v ? algorithmAlertTypeNames[v] || String(v) : '--')

const getDict = async () => {
  let options: any[] = []
  const res1 = await getRuleList({
    pageSize: 100,
    pageNo: 1,
    typ: 'scene',
  })
  const res2 = await getRuleList({
    pageSize: 100,
    pageNo: 1,
    typ: 'flow',
  })
  options = [...res1.list, ...res2.list]
  column.value.forEach(item => {
    if (item.key === 'ruleInfoId') {
      item.componentProps.options = options
    }
  })

  getTemplatesList({}).then(res => {
    console.log("gettem:",res)
    column.value.forEach(item => {
    if (item.key === 'messageTemplateId') {
      item.componentProps.options = res.list
    }
  })
  })

  // 加载消息推送岗位列表
  getSimplePostList().then(posts => {
    postOptions.value = posts
    column.value.forEach(item => {
      if (item.key === 'pushPostIds') {
        item.componentProps.options = posts
      }
    })
  })
}
getDict()
const getData =  () => {
  state.loading = true
  getConfigList({
    ...state.page,
    ...state.query,
  }).then((res) => {
    data.value = res.list
    state.total = res.total
    // 数据加载完成后，标记为已初始化
    nextTick(() => {
      isInitialized.value = true
    })
  }).finally(() => {
    state.loading = false
  })
}
// 保存数据
const onSave = ({type, data, cancel}: any) => {
  // 消息推送岗位最多支持选择 3 个
  if (data?.pushPostIds && data.pushPostIds.length > 3) {
    ElMessage.warning('消息推送岗位最多支持选择 3 个')
    return
  }
  // 算法报警类型与规则联动：未选择算法报警类型（通用规则配置）时，规则必填；选择算法报警类型时规则可不选
  if (!data?.algorithmAlertType && !data?.ruleInfoId) {
    ElMessage.warning('未选择算法报警类型时，规则不能为空')
    return
  }
  state.loading = true
  saveConfig(toRaw(data)).then(res => {
      cancel()
      getData()
  }).finally(() => {
    state.loading = false
  })


}
// 删除
const handleDelete =  (row: any) => {
  state.loading = true
  deleteConfig(row.id).then(res => {
      ElMessage.success('删除成功!')
      getData()
  }).finally(() => {
    state.loading = false
  })
}
// 状态变更（实时保存）
const handleStatusChange = (row: any) => {
  // 确保有 id 且已初始化完成才保存（避免初始化时误触发创建）
  if (!row || !row.id || !isInitialized.value) {
    return
  }
  state.loading = true
  saveConfig(toRaw(row)).then(res => {
    ElMessage.success('状态更新成功!')
  }).catch(err => {
    // 保存失败，恢复原值
    row.status = row.status === 0 ? 1 : 0
    ElMessage.error('状态更新失败，请重试')
  }).finally(() => {
    state.loading = false
  })
}
const data = ref([])
</script>

<!-- <style lang="scss" scoped>

</style> -->
