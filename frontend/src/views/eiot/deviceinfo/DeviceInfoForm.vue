<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="设备唯一标识" prop="dn">
        <el-input v-model="formData.dn" placeholder="请输入设备唯一标识" />
      </el-form-item>
      <el-form-item label="产品key" prop="productKey">
        <el-input v-model="formData.productKey" placeholder="请输入产品key" />
      </el-form-item>
      <el-form-item label="机构id" prop="deptId">
        <el-input v-model="formData.deptId" placeholder="请输入机构id" />
      </el-form-item>

      <el-form-item label="设备状态" prop="status">
        <el-select
          v-model="formData.status"
          placeholder="请选择设备状态"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.EIOT_DEVICE_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="别名" prop="name">
        <el-input v-model="formData.name" placeholder="请输入别名" />
      </el-form-item>

      <el-form-item label="设备序列号" prop="serialNo">
        <el-input v-model="formData.serialNo" placeholder="请输入设备序列号" />
      </el-form-item>
      <el-form-item label="设备型号" prop="model">
        <el-input v-model="formData.model" placeholder="请输入设备型号" />
      </el-form-item>
      <el-form-item label="品牌代码" prop="brandCode">
        <el-input v-model="formData.brandCode" placeholder="请输入品牌代码（如 fourfaith）" />
      </el-form-item>
      <el-form-item label="协议版本" prop="apiVersion">
        <el-input v-model="formData.apiVersion" placeholder="请输入API协议版本（如 1.0）" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="设备位置">
        <Map
          :is-write="true"
          :click-map="true"
          :center="mapCenter"
          :address="formAddress"
          @locate-change="handleLocateChange"
          @address-change="handleAddressChange"
        />
      </el-form-item>
      <el-form-item label="经度">
        <el-input v-model="formData.lon" disabled />
      </el-form-item>
      <el-form-item label="纬度">
        <el-input v-model="formData.lat" disabled />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { DeviceInfoApi, DeviceInfoVO } from '@/api/eiot/deviceinfo'
import {getIntDictOptions, DICT_TYPE} from "@/utils/dict";
import Map from '@/components/Map/index.vue';

/** 设备信息 表单 */
defineOptions({ name: 'DeviceInfoForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  dn: undefined,
  productKey: undefined,
  deptId: undefined,
  propertys: undefined,
  status: undefined,
  name: undefined,
  serialNo: undefined,
  model: undefined,
  brandCode: undefined,
  apiVersion: undefined,
  remark: undefined,
  lat: undefined,
  lon: undefined,
  addr: undefined
})
const formRules = reactive({
  dn: [{ required: true, message: '设备唯一标识不能为空', trigger: 'blur' }],
  productKey: [{ required: true, message: '产品key不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '设备状态不能为空', trigger: 'blur' }],
  serialNo: [{ required: true, message: '设备序列号不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref

const mapCenter = computed(() => {
  if (formData.value.lat !== undefined && formData.value.lat !== null && formData.value.lat !== '' &&
      formData.value.lon !== undefined && formData.value.lon !== null && formData.value.lon !== '') {
    // AMap 使用 "经度,纬度" 格式
    return `${formData.value.lon},${formData.value.lat}`
  }
  return ''
})

const formAddress = computed(() => {
  return formData.value.addr || ''
})

const handleLocateChange = (lnglat: string[]) => {
  if (lnglat && lnglat.length >= 2) {
    // AMap 返回 [经度, 纬度]
    formData.value.lon = lnglat[0]
    formData.value.lat = lnglat[1]
  }
}

const handleAddressChange = (address: string) => {
  formData.value.addr = address
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await DeviceInfoApi.getDeviceInfo(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  await formRef.value.validate()
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as DeviceInfoVO
    if (formType.value === 'create') {
      await DeviceInfoApi.createDeviceInfo(data)
      message.success(t('common.createSuccess'))
    } else {
      await DeviceInfoApi.updateDeviceInfo(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    dn: undefined,
    productKey: undefined,
    deptId: undefined,
    propertys: undefined,
    status: undefined,
    name: undefined,
    serialNo: undefined,
    model: undefined,
    brandCode: undefined,
    apiVersion: undefined,
    remark: undefined,
    lat: undefined,
    lon: undefined,
    addr: undefined
  }
  formRef.value?.resetFields()
}
</script>
