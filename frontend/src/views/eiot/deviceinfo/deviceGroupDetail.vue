<template>
  <yt-crud
    ref="crudRef" :data="data" :column="column"
    :loading="state.loading"
    :total="state.total"
    v-model:page="state.page"
    v-model:query="state.query"
    :tableProps=" {
      selection: true,
      viewBtn: false,
      editBtn: false,
      delBtn: false,
      menuSlot: true,
      menuWidth: 300,
    }"
    :funProps="{
      addBtn: false,
      delBtn: true,
      delBtnText: '批量退组'
    }"
    @on-load="getData"
    @delFun="onDelete"
  >
      <template #menuSlot="scope">
        <el-button type="text" size="small" @click="removeFromGroup(scope.row.id)">退组</el-button>
      </template>

      <template #online="scope">
        <el-tag class="state" v-if="scope.row.state === 1" type="success" size="small">在线</el-tag>
        <el-tag class="state" v-else type="danger" size="small">离线</el-tag>
      </template>

      <template #group="scope">
        <el-tag v-for="i,k in scope.row.group" :key="k">{{ i.name }}</el-tag>
      </template>
  </yt-crud>
</template>
<script lang="ts" setup>
import { IColumn } from '@/components/common/types/tableCommon'

import YtCrud from '@/components/common/yt-crud.vue'
import { DeviceInfoApi, DeviceInfoVO } from '@/api/eiot/deviceinfo'
import {  removeDeviceFromDeviceGroup } from '@/api/eiot/deviceinfo/devices.api'
import { ProductApi } from '@/api/eiot/product/index'

const route = useRoute()
const { id } = route.params

const column = ref<IColumn[]>([
  {
    label: '设备ID',
    key: 'id',
  },
  {
    label: '产品',
    key: 'productKey',
    type: 'select',
    search: true,
    colSpan: 12,
    tableWidth: 120,
    editDisabled: true,
    componentProps: {
      labelAlias: 'name',
      valueAlias: 'productKey',
      style: 'width: 120px',
      options: [],
    },
  },
  {
    label: '设备标识',
    key: 'dn',
    tableWidth: 240,
    search: true
  },
  {
    label: '分组',
    key: 'group',
    tableWidth: 240,
    slot: true,
  },
  {
    label: '状态',
    key: 'online',
    type: 'select',
    componentProps: {
      options: [
        {
          label: '在线',
          value: true,
        },
        {
          label: '离线',
          value: false,
        },
      ],
    },
    search: false,
    tableWidth: 80,
    slot: true,
  },
  {
    label: '创建时间',
    key: 'createTime',
    tableWidth: 180,
    sortable: true,
    type: 'date',
  },
])

const data = ref()

const state = reactive({
  page: {
    pageSize: 10,
    pageNo: 1,
  },
  total: 0,
  loading: false,
  query: {
    groupId: id
  },
})

// 产品字典
const productOptions = ref<IProductsVO[]>([])
const getDict = () => {
  ProductApi.getProductPage({
    pageNo: 1,
  }).then((res) => {
    productOptions.value = res.list || []
    column.value.forEach((item) => {
      if (item.key === 'productKey') {
        item.componentProps.options = productOptions.value
      }
    })
  })
}
getDict()

const getData = () => {
  state.loading = true
  DeviceInfoApi.getDeviceInfoPage({
    ...state.page,
    ...state.query}).then(res => {
      data.value = res.list
      state.total = res.total
  }).finally(() => state.loading = false)
}


// removeFromGroup 退组
const removeFromGroup = (deviceId: string) => {

  removeDeviceFromDeviceGroup({groupId: id, deviceIds: [deviceId]}).then(res => {
    // 退组成功

      ElMessage.success('退组成功!')
      getData()

  })
}


// 删除
const onDelete = async (row: any) => {
  state.loading = true
  if (row instanceof Array) {
    await removeDeviceFromDeviceGroup({group: id, deviceIds: row.map((m) => m.id)})
  } else {
    await removeDeviceFromDeviceGroup({group: id, deviceIds: [row.id]})
  }
  ElMessage.success('退组成功!')
  state.loading = false
  getData()
}

</script>

<!-- <style lang="scss" scoped>

</style> -->
