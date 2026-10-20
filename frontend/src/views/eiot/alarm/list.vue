<template>
  <yt-crud
    ref="crudRef"
    :data="data"
    :column="column"
    :fun-props="{
      addBtn: false
    }"
    :table-props="{
      Selection: false,
      delBtn: false,
      editBtn: false
    }"
    @onLoad="getData"
    :loading="state.loading"
    :total="state.total"
    v-model:page="state.page"
    v-model:query="state.query"
  >
    <template #alertImage="scope">
      <el-image
        v-if="scope.row.alertImage"
        :src="scope.row.alertImage"
        :preview-src-list="[scope.row.alertImage]"
        preview-teleported
        fit="cover"
        style="width: 60px; height: 40px; border-radius: 2px"
      />
      <span v-else>—</span>
    </template>
  </yt-crud>
</template>
<script lang="ts" setup>
import { IColumn } from '@/components/common/types/tableCommon'
import { getMsgList } from '@/api/eiot/alarm/alarm.api'
import { getTenantPage } from '@/api/system/tenant'

import YtCrud from '@/components/common/yt-crud.vue'

// 租户下拉选项（搜索筛选用）
const tenantOptions = ref<any[]>([])

const column = ref<IColumn[]>([
  {
    label: '租户名称',
    key: 'tenantId',
    hide: true, // 仅作搜索项，表格中的租户列用下方 tenantName
    search: true,
    type: 'select',
    componentProps: {
      clearable: true,
      style: 'width: 180px',
      labelAlias: 'name',
      valueAlias: 'id',
      options: tenantOptions
    }
  },
  {
    label: '租户名称',
    key: 'tenantName',
    tableWidth: 160
  },
  {
    label: '配置名称',
    key: 'name',
    search: true,
    rules: [{ required: true, message: '告警名称不能为空' }]
  },
  {
    label: '产品名称',
    key: 'productName',
    tableWidth: 150
  },
  {
    label: '设备名称',
    key: 'deviceName',
    tableWidth: 150
  },
  {
    // 搜索列（仅筛选用，挂 level 参数；选项为新语义等级 1/2/3，混存期旧数据按旧编码命中）
    label: '告警等级',
    key: 'level',
    hide: true,
    search: true,
    type: 'select',
    componentProps: {
      style: 'width: 150px',
      options: [
        {
          label: '一级',
          value: 1
        },
        {
          label: '二级',
          value: 2
        },
        {
          label: '三级',
          value: 3
        }
      ]
    }
  },
  {
    label: '告警等级',
    key: 'levelText',
    tableWidth: 120
  },
  {
    label: '告警类型',
    key: 'typeText',
    tableWidth: 150
  },
  {
    label: '是否已读',
    key: 'readFlg',
    tableWidth: 120,
    type: 'switch'
  },
  {
    label: '告警时间',
    key: 'alertTime',
    type: 'date',
    tableWidth: 180
  },
  {
    label: '告警内容',
    key: 'details',
    tableWidth: 250,
    componentProps: {
      type: 'textarea',
      rows: 4
    }
  },
  {
    label: '报警图片',
    key: 'alertImage',
    tableWidth: 110,
    slot: true
  }
])

const KIND_TEST_MAP: Record<string, string> = {
  '1': '火警',
  '2': '故障',
  '3': '隐患',
  '4': '离线',
  '5': '误报',
  '6': '信息'
}
const OLD_KIND_TEST_MAP: Record<string, string> = {
  '1': '火警',
  '2': '故障',
  '3': '离线',
  '4': '反馈',
  '5': '其他'
}
const LEVEL_TEXT_MAP: Record<string, string> = { '1': '一级', '2': '二级', '3': '三级' }

const decorateRow = (row: any) => {
  const isNew = row.alarmKind !== null && row.alarmKind !== undefined
  return {
    ...row,
    typeText: isNew ? (KIND_TEST_MAP[String(row.alarmKind)] || '') : (OLD_KIND_TEST_MAP[String(row.level)] || ''),
    levelText: isNew ? (LEVEL_TEXT_MAP[String(row.level)] || '—') : '—',
  }
}

const data = ref([])
const state = reactive({
  page: {
    pageSize: 10,
    pageNum: 1
  },
  total: 0,
  loading: false,
  query: {}
})

const getData = () => {
  state.loading = true
  getMsgList({
    ...state.page,
    ...state.query
  })
    .then((res) => {
      data.value = (res.list || []).map(decorateRow)
      state.total = res.total
    })
    .finally(() => {
      state.loading = false
    })
}

// 加载租户下拉选项
const loadTenantOptions = async () => {
  const res = await getTenantPage({ pageNo: 1, pageSize: 100 })
  tenantOptions.value = res?.list || []
  const col = column.value.find((item) => item.key === 'tenantId')
  if (col && col.componentProps) {
    col.componentProps.options = tenantOptions.value
  }
}
loadTenantOptions()
</script>
