<template>
  <el-input class="cu-input" size="small" placeholder="请选择产品" v-model.trim="name" @clear="onClear" clearable @click="handleSelect">
    <template #append>
      <el-button type="primary" @click="handleSelect">选择</el-button>
    </template>
  </el-input>
  <el-dialog
    title="选择产品"
    v-model="dialogState.show"
    width="1200px"
    :close-on-press-escape="false"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
  >
    <yt-crud
      v-if="dialogState.show"
      ref="crudRef"
      :data="data"
      :column="column"
      :table-props="{
        menu: false,
        selection: multiple,
      }"
      :fun-props="{
        hide: true
      }"
      :loading="state.loading"
      :total="state.total"
      v-model:page="state.page"
      v-model:query="state.query"
      @on-load="getData"
      @row-click="rowClick"
    />
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogState.show = false">取消</el-button>
        <el-button type="primary" @click="handleMultiple">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script lang="ts" setup>
import { propTypes } from '@/utils/propTypes'
import { IColumn } from '../common/types/tableCommon'
import { ProductApi } from '@/api/eiot/product/index'

import YtCrud from '@/components/common/yt-crud.vue'
import {CategoryApi} from "@/api/eiot/category";

const props = defineProps({
  id: propTypes.string.def(''),
  pk: propTypes.string.def(''),
  // 是否多选
  multiple: propTypes.bool.def(false),
})
const emits = defineEmits(['onSelect', 'update:id', 'update:pk'])
const state = reactive({
  page: {
    pageSize: 10,
    pageNo: 1,
  },
  total: 0,
  loading: false,
  query: {},
})
// 单击
const crudRef = ref()

const rowClick = (row: any) => {
  if (props.multiple) {
    crudRef.value.getTableRef().tableRef.toggleRowSelection(toRaw(row), undefined)
    return
  }
  emits('onSelect', row)
  emits('update:id', row.id)
  emits('update:pk', row.productKey)
  dialogState.data = row
  dialogState.show = false
}
// 多选
const handleMultiple = () => {
  const rows = crudRef.value.getTableRef().tableRef.getSelectionRows()
  dialogState.data = {
    id: rows.map((m: any) => m.id),
    name: rows.map((m: any) => m.name).join(','),
  }
  emits('update:id', dialogState.data.id)
  dialogState.show = false
}

// 清空
const onClear = () => {
  emits('update:id', '')
  emits('update:pk', '')
  dialogState.data = {}
}
const dialogState = reactive({
  show: false,
  data: {} as any,
})

const handleSelect = () => {
  dialogState.show = true
}
// 分类字典
const cateOptions = ref<any[]>([])
const getDict = () => {
  CategoryApi.getCategoryList(null).then((res) => {
    cateOptions.value = res || []
    column.value.forEach((item) => {
      if (item.key === 'categoryId') {
        item.componentProps.options = cateOptions
      }
    })
  })
}
getDict()
const column = ref<IColumn[]>([
  {
    label: 'ProductKey',
    key: 'productKey',
    rules: [{ required: true, message: 'ProductKey不能为空' }],
  },
  {
    label: '产品名称',
    search: true,
    key: 'name',
    rules: [{ required: true, message: '产品名称不能为空' }],
  },
  {
    label: '品类',
    key: 'categoryId',
    search: true,
    type: 'select',
    tableWidth: 150,
    componentProps: {
      labelAlias: 'name',
      valueAlias: 'id',
      options: cateOptions.value,
    },
    rules: [{ required: true, message: '品类不能为空' }],
  },
  {
    label: '节点类型',
    key: 'nodeType',
    type: 'radio',
    componentProps: {
      defaultValue: 0,
      options: [
        {
          value: 0,
          label: '网关设备',
        },
        {
          value: 1,
          label: '网关子设备',
        },
        {
          value: 2,
          label: '直连设备',
        },
        {
          value: 3,
          label: '视频设备',
        },
      ],
    },
  },
  {
    label: '透传设备',
    key: 'transparent',
    type: 'switch',
    tableWidth: 80,
    componentProps: {
      defaultValue: 'false',
      options: [
        {
          value: 'true',
          label: '是',
        },
        {
          value: 'false',
          label: '否',
        },
      ],
    },
  },
])
const data = ref<any[]>([])
const getData = () => {
  state.loading = true
  ProductApi.getProductPage({
    ...state.query,
    ...state.page,
  }).then((res) => {
    data.value = res.list || []
    state.total = res.total
  })
  state.loading = false
}
const allData = ref<any[]>([])
const getAllData = () => {
  ProductApi.getProductPage({
    pageNo: 1,
    pageSize: 100,
  }).then((res) => {
     allData.value = res.list || []
  })
}
getAllData()
const getName = (str, type: 'id' | 'productKey') => {
  return allData.value.find((f) => f[type] === str)?.name || ''
}
const name = computed(() => {
  console.log(allData.value)
  if (props.id) return getName(props.id, 'id')
  if (props.pk) return getName(props.pk, 'productKey')
  return dialogState.data?.name || ''
})
</script>

<style lang="scss" scoped>
.cu-input {
  cursor: pointer;
}
::v-deep(.el-input-group__append) {
  background-color: var(--el-color-primary);
  color: #fff;
}
</style>
