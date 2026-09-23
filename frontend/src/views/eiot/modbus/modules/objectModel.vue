<template>
  <el-dialog
    title="点位表"
    width="70%"
    v-model="state.visible"
    top="20px"
    :close-on-press-escape="false"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
  >
    <div v-if="state.visible" v-loading="state.loading">
      <function-config :id="state.id" :model="state.model" />
    </div>
  </el-dialog>
</template>
<script lang="ts" setup>
import { useEmitt } from '@/hooks/web/useEmitt'
import { ModBusInfoApi } from '@/api/eiot/modbus'
import FunctionConfig from './functionConfig.vue'


const activeTab = ref(0)
const state = reactive({
  loading: false,
  visible: false,
  id: '',
  model: {
    services: [],
    properties: [],
    events: [],
  } as any,
})
// 获取数据
const getInfo = (key: string) => {
  state.visible = true
  state.loading = true
  state.id = key
  getData()
}
const { emitter } = useEmitt({
  name: 'updateObjectModelX',
  callback: () => getData()
})
const getData = async () => {
  try {
    const res = await ModBusInfoApi.getObjectModel(state.id);
    const data = res || {}
    if (data.model) {
      data.model.services = data.model.services || []
      data.model.properties = data.model.properties || []
      data.model.events = data.model.events || []
    }
    state.model = data.model || {
      services: [],
      properties: [],
      events: [],
    }
  } finally {
    state.loading = false
  }
}
defineExpose({
  getInfo,
})
</script>

<!-- <style lang="less" scoped>

</style> -->
