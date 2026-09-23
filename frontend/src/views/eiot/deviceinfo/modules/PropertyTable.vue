<template>
  <el-dialog v-model="dialogVisible" title="历史数据" width="1000">
    <div class="px-[30px]">
      <div class="my-[20px]">
        <el-date-picker
          v-model="state.historyTime"
          type="datetimerange"
          :picker-options="state.pickerOptions"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          align="right"
          @change="timeRangeChange"
        />
      </div>
      <div>
        <el-table :data="state.data" lazy highlight-current-row v-loading="loading" border element-loading-text="拼命加载中">
          <el-table-column align="center" prop="time" label="时间">
            <template #default="scope">
              <div>{{ dayjs(scope.row.time).format('YYYY-MM-DD HH:mm:ss') }}</div>
            </template>
          </el-table-column>
          <el-table-column align="center" prop="value" label="数值" />
        </el-table>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import {devicePropertyLogs} from '@/api/eiot/deviceinfo/devices.api'
import dayjs from 'dayjs'

const dialogVisible = ref(false)
const loading = ref(false)

const state = ref<any>({
  historyTime: undefined,
  data: [],
  deviceId: undefined,
  currHistoryProperty: undefined
})

const getData = async () => {
  loading.value = true
  await devicePropertyLogs({
    deviceId: state.value.deviceId,
    name: state.value.currHistoryProperty.identifier,
    start: state.value.historyTime[0].getTime(),
    end: state.value.historyTime[1].getTime(),
  }).then((res) => {
    state.value.data = res
  }).finally(() => {
    loading.value = false
  })
}

/** 打开弹窗 */
const open = async (row: any) => {
  dialogVisible.value = true
  console.log(row)
  if (row) {
    state.value = row
    await getData()
  }
}

const timeRangeChange = () => {
  getData()
}

defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>

<style scoped></style>
