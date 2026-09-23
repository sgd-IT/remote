<template>
  <div>
    <vue-json-editor
      v-model:json="thingModel"
      :showBtns="false"
      :mode="'text'"
      :escapeControlCharacters="true"
      lang="zh"
      height="400"
      @change="change"
    />
    <div class="mt-[20px]">
      <el-button type="primary" @click="submitThingModelChange()">保存</el-button>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { propTypes } from '@/utils/propTypes'
import { toRaw } from 'vue'
import VueJsonEditor from 'vue3-ts-jsoneditor'
import { ElMessage } from 'element-plus'

import {ThingModelApi, ThingModelVO} from '@/api/eiot/thingmodel'

const props = defineProps({
  id: propTypes.string.def(''),
  model: propTypes.object.def({}),
})

const change =(e)=>{
  console.log('change:', e.text)
  thingModel.value = JSON.parse(e.text)
}


const thingModel = ref(props.model)
watch(() => props?.model, (newV) => {
  console.log('ll:', newV)
  console.log('ll:', thingModel.value)
  if (newV){
    thingModel.value = newV
  }else {
    thingModel.value = {}
  }
})

const submitThingModelChange = () => {
  if (!thingModel) {
    return
  }
  const data = {
    productKey: props.id,
    model:JSON.stringify(toRaw(thingModel.value)),
  } as ThingModelVO
  ThingModelApi.saveThingModel(data).then((res) => {
    if (res) {
      ElMessage.info({
        type: 'success',
        message: '保存成功',
      })
    } else {
      ElMessage({
        type: 'error',
        message: '保存失败',
      })
    }

  }).finally(()=>{
  })
}
</script>
