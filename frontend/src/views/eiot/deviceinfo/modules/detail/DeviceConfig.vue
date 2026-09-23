<template>
  <div>
    <el-form ref="configForm">
      <div v-for="item in allConfigItems" :key="item.value" :label="item.label">
        <span>{{ item.label }}：</span>
        <span> {{ item.value }}</span>
        <span style="display: inline-block; margin-left: 8px">
          <el-switch :key="item" :label="item" v-model="productConfigCheck[item.value]" disabled />
        </span>
        <vue-json-editor
          v-if="productConfigCheck[item.value]"
          v-model:json="editConfig[item.value]"
          :showBtns="false"
          :mode="'text'"
          :escapeControlCharacters="true"
          lang="zh"
          height="400"
          @change="changeConfigItem($event, item)"
        />
      </div>
    </el-form>
    <!--    <vue-json-editor v-model:json="config" :showBtns="false" :escapeControlCharacters="true" :mode="'text'" lang="zh" height="600" @change="change" />-->
    <!--    <el-button @click="saveConfig" type="primary" style="margin-top: 15px;">生成配置</el-button>-->
    <el-button @click="saveConfig" type="primary" style="margin-top: 15px">保存</el-button>
    <el-button @click="sendConfig" type="primary" style="margin-top: 15px">下发</el-button>
  </div>
</template>

<script setup lang="ts">
import VueJsonEditor from 'vue3-ts-jsoneditor'
import {
  getDeviceConfig,
  sendDeviceConfig,
  saveDeviceConfig
} from '@/api/eiot/deviceinfo/devices.api'
import { ElNotification } from 'element-plus'

import { ProductApi } from '@/api/eiot/product'
import { toRaw } from 'vue'

const props = defineProps({
  deviceInfo: {
    type: Object,
    default: () => ({
      deviceId: ''
    })
  }
})

const editConfig = ref({
  basic: undefined,
  interface: undefined,
  model: undefined
})

const allConfigItems = [
  { value: 'basic', label: '基础配置' },
  { value: 'interface', label: '接口配置' },
  { value: 'model', label: '物模型配置' },
  { value: 'subBasic', label: '子设备基础配置' },
  { value: 'subInterface', label: '子设备接口配置' },
  { value: 'subModel', label: '子设备物模型配置' }
]

const changeConfigItem = (e, item) => {
  if (e.text) {
    editConfig.value[item.value] = JSON.parse(e.text)
  } else {
    editConfig.value[item.value] = null
  }
}

const changeConfigItemSwitch = (val, item) => {
  // console.log('change switch ', val, item)
  // console.log('productConfigCheck', productConfigCheck.value)
}

const getConfig = () => {
  getDeviceConfig(props.deviceInfo.deviceId).then((rst) => {
    if (rst) {
      editConfig.value = rst.data?.config ? JSON.parse(rst.data?.config) : ''
    }
  })
}

const saveConfig = () => {
  console.log(editConfig.value)
  console.log(JSON.stringify(editConfig.value))
  saveDeviceConfig({
    deviceId: props.deviceInfo.deviceId,
    config: editConfig.value ? JSON.stringify(editConfig.value) : ''
  }).then((res) => {
    ElNotification({
      title: '成功',
      message: '修改成功',
      type: 'success'
    })
  })
}

const sendConfig = () => {
  sendDeviceConfig(props.deviceInfo.deviceId).then((res) => {
    if (res.code === 200) {
      ElNotification({
        title: '成功',
        message: '下发成功',
        type: 'success'
      })
    } else {
      ElNotification({
        title: '提示',
        message: res.message,
        type: 'error'
      })
    }
  })
}

const productConfigCheck = ref({})

const configInit = () => {
  ProductApi.getProductConfig(props.deviceInfo.deviceDetail.productKey).then((res) => {
    if (res) {
      const configItems = res.data?.configItems ? res.data?.configItems : ''
      console.log(configItems)
      for (const item of allConfigItems) {
        productConfigCheck.value[item.value] = configItems.includes(item.value)
      }
      console.log(productConfigCheck.value)
    }
  })
}
onMounted(() => {
  configInit()
  getConfig()
})
</script>

<style lang="scss" scoped>
::v-deep(.jse-menu) {
  .jse-group-button {
    &:nth-child(1),
    &:nth-child(3) {
      display: none;
    }
  }
}
</style>
