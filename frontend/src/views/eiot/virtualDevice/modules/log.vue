<template>
  <el-table
    size="small"
    :data="logs"
    highlight-current-row
  >
    <el-table-column sortable label="时间" prop="logAt">
      <template #default="scope">
        <div>{{ dayjs(scope.row.logAt).format('YYYY-MM-DD HH:mm:ss') }}</div>
      </template>
    </el-table-column>

    <el-table-column sortable label="结果" prop="result"/>
  </el-table>
  <Pagination :total="state.total" :limit="state.page.pageSize" :page="state.page.pageNo" @pagination="getPageEvents"/>
</template>

<script lang="ts" setup>
import { getVirtualDevicesLogs } from '@/api/eiot/deviceinfo/virtualDevices.api'
import dayjs from 'dayjs'

const route = useRoute()
const { id } = route.params

const logs = ref()

const state = reactive({
  total: 0,
  page: {
    pageSize: 10,
    pageNo: 1,
  },
})

const getLogs = () => {
  getVirtualDevicesLogs({virtualDeviceId: id, ...state.page}).then((res) => {
    logs.value = res.list
    state.total = res.total
  })
}
defineExpose({ getLogs })

getLogs()

const getPageEvents = (e) => {
  state.page.pageSize = e.limit
  state.page.pageNo = e.page
  getLogs()
}

</script>
