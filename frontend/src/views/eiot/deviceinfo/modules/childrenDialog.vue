<template>
  <el-dialog
    v-model="state.show"
    title="子设备列表"
    width="1100px"
    :close-on-press-escape="false"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
  >
    <yt-table
      v-if="state.show"
      :column="columns"
      :data="data"
      :total="state.total"
      :loading="state.loading"
      v-model:page="state.page"
      :view-btn="false"
      :edit-btn="false"
      :selection="false"
      :del-btn="false"
      @on-load="getData"
      menu-slot
    >
      <template #state="scope">
        <el-tag v-if="scope.row.state === 1" type="success" size="small">在线</el-tag>
        <el-tag v-else type="danger" size="small">离线</el-tag>
      </template>
      <template #menuSlot="scope">
        <el-popconfirm title="确认要解除与网关的关联吗？" @confirm="handleDelete(scope.row)">
          <template #reference>
            <el-button link type="danger" icon="Delete">解绑</el-button>
          </template>
        </el-popconfirm>
      </template>
    </yt-table>
  </el-dialog>
</template>
<script lang="ts" setup>
import { IColumn } from '@/components/common/types/tableCommon'
import {getChildrenDeviceList} from "@/api/eiot/deviceinfo/devices.api";
import {unbindDevice} from "@/api/eiot/deviceinfo/devices.api";

import { ElPopconfirm } from 'element-plus'
import YtTable from '@/components/common/yt-table'
defineOptions({name: 'ChildrenDialog'})


const state = reactive({
  show: false,
  row: {} as any,
  page: {
    pageSize: 100,
    pageNo: 1,
  },
  total: 0,
  loading: false,
  parentId: '',
})
const columns = ref<IColumn[]>([
  {
    label: '别名',
    key: 'name',
  },
  {
    label: '产品',
    key: 'productName',
    tableWidth: 120,
  },
  {
    label: '设备标识',
    key: 'dn',
    tableWidth: 150,
  },
  {
    label: '状态',
    key: 'state',
    slot: true,
    tableWidth: 80,
  },
  {
    label: '创建时间',
    key: 'createTime',
    type: 'date',
    sortable: true,
    tableWidth: 180,
  },
])
const data = ref([
])

const handleDelete = async (row: any) => {
  state.loading = true
  await unbindDevice({
    idList: [row.id],
  })
  ElMessage.success('解绑成功!')
  state.loading = false
  getData()
}

const getData = () => {
  state.parentId = state.row.id
  getChildrenDeviceList({
    ...state.page,
    parentId: state.parentId,
  })
    .then((res) => {
      // state.page.pageSize = res.data.length
      // state.page.pageNo = 1
      state.total = res.total
      // res.data.list.forEach((d: any) => {
      //   d['productName'] = d['product']?.name
      // })
      data.value = res.list
      console.log(res)
    })
    .finally(() => {
      state.loading = false
    })
}

const openDialog = (row: any) => {
  state.show = true
  state.row = row
  console.log('row', row)
  state.loading = true
  getData()
}

defineExpose({
  openDialog,
})
</script>
