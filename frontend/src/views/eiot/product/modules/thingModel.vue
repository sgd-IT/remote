<template>
  <div>
    <div v-if="state.visible" v-loading="state.loading">
      <el-tabs type="border-card" v-model="activeTab" @tab-change="changeTab">
        <el-tab-pane label="功能列表" :name="0">
          <function-config v-if="activeTab === 0" :id="state.id" :model="state.model" />
        </el-tab-pane>
        <el-tab-pane label="TSL" :name="1">
          <tsl v-if="activeTab === 1" :id="state.id" :model="state.model" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ThingModelApi, ThingModelVO } from '@/api/eiot/thingmodel'
import { useEmitt } from '@/hooks/web/useEmitt'

import FunctionConfig from './functionConfig.vue'
import ModelNumberConfig from './modelNumberConfig.vue'
import Tsl from './tsl.vue'

const props = defineProps({
  productInfo: {
    type: Object,
    default: () => {}
  }
})

const activeTab = ref(0)
const state = reactive({
  loading: false,
  visible: false,
  id: '',
  model: {
    services: [],
    properties: [],
    events: []
  } as any
})
// 获取数据
const getInfo = () => {
  state.visible = true
  state.loading = true
  state.id = props.productInfo?.productKey
  getData()
}
const { emitter } = useEmitt({
  name: 'updateObjectModel',
  callback: () => getData()
})

const changeTab = (e) => {
  if (e != 2) {
    getData()
  }
}

const getData = () => {
  ThingModelApi.getThingModelByProductKey(state.id)
    .then((res) => {
      const data = res || {}
      if (data.model) {
        data.model.services = data?.model?.services || []
        data.model.properties = data?.model?.properties || []
        data.model.events = data?.model?.events || []
      }
      state.model = data.model || {
        services: [],
        properties: [],
        events: []
      }
    })
    .finally(() => {
      state.loading = false
    })
}

defineExpose({
  getInfo
})

onMounted(() => {
  // console.log(props.productInfo)
  getInfo()
})
</script>

<style scoped></style>
