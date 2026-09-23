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
  label: '告警等级',
  key: 'level',
  tableWidth: 120,
  search: true,
  type: 'select',
  rules: [{ required: true, message: '请选择告警等级' }],
  componentProps: {
    style: 'width: 150px',
    options: [{
      label: '火警',
      value: 1,
    }, {
      label: '故障',
      value: 2,
    }, {
      label: '离线',
      value: 3,
    }, {
      label: '反馈',
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
  label: '规则',
  key: 'ruleInfoId',
  tableWidth: 120,
  type: 'select',
  rules: [{ required: true, message: '请选择规则' }],
  componentProps: {
    labelAlias: 'name',
    valueAlias: 'id',
    options: []
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
