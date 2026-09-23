<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="产品名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入产品名称" />
      </el-form-item>
      <el-form-item label="产品分类id" prop="categoryId">
        <el-input v-model="formData.categoryId" placeholder="请输入产品分类id" />
      </el-form-item>
      <el-form-item label="productKey" prop="productKey">
        <el-input v-model="formData.productKey" placeholder="请输入productKey" />
      </el-form-item>
      <el-form-item label="mcu code" prop="mcuCode">
        <el-select v-model="formData.mcuCode" clearable class="!w-240px">
          <el-option v-for="dict in getDictOptions(DICT_TYPE.EIOT_MUC_CODE)" :key="dict.value"
                     :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="功能介绍" prop="remark1">
        <el-input v-model="formData.remark1" placeholder="请输入功能介绍" />
      </el-form-item>
      <el-form-item label="图片url" prop="imgUrl">
        <el-input v-model="formData.imgUrl" placeholder="请输入图片url" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
                    :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="设备类型" prop="nodeType">
        <el-select
          v-model="formData.nodeType"
          class="!w-280px"
          clearable
          placeholder="请选择设备类型"
        >
          <el-option
            v-for="dict in getDictOptions(DICT_TYPE.EIOT_NODE_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="协议code" prop="protocolCode">
        <el-select
          v-model="formData.protocolCode"
          class="!w-280px"
          clearable
          placeholder="请选择协议类型"
        >
          <el-option
            v-for="dict in getDictOptions(DICT_TYPE.EIOT_PROTOCOL_CODE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="保活时间" prop="keepAliveTime">
        <el-input-number v-model="formData.keepAliveTime"/>
      </el-form-item>
      <el-form-item label="产品密钥" prop="productSecret">
        <el-input v-model="formData.productSecret" placeholder="请输入产品密钥" />
      </el-form-item>
      <el-form-item label="是否透传" prop="transparent">
        <el-radio-group v-model="formData.transparent">
          <el-radio v-for="dict in getBoolDictOptions(DICT_TYPE.INFRA_BOOLEAN_STRING)"
           :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="唯一标识类型" prop="dnTyp">
        <el-input v-model="formData.dnTyp" placeholder="请输入唯一标识类型(1序列号 2蓝牙mac)" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ProductApi, ProductVO } from '@/api/eiot/product'
import {getBoolDictOptions, DICT_TYPE, getIntDictOptions, getDictOptions} from "@/utils/dict";

/** 物联网产品 表单 */
defineOptions({ name: 'EiotProductForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  name: undefined,
  categoryId: undefined,
  productKey: undefined,
  mcuCode: undefined,
  remark1: undefined,
  imgUrl: undefined,
  remark: undefined,
  status: undefined,
  nodeType: undefined,
  protocolCode: undefined,
  keepAliveTime: undefined,
  productSecret: undefined,
  transparent: undefined,
  dnTyp: undefined
})
const formRules = reactive({
  name: [{ required: true, message: '产品名称不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '产品分类id不能为空', trigger: 'blur' }],
  productKey: [{ required: true, message: 'productKey不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态（0启用 1禁用）不能为空', trigger: 'blur' }],
  nodeType: [{ required: true, message: '设备类型(0 网关设备, 1 网关子设备, 2 直连设备, 3 非联网设备 )不能为空', trigger: 'change' }],
  protocolCode: [{ required: true, message: '协议code不能为空', trigger: 'blur' }],
  keepAliveTime: [{ required: true, message: '保活时间不能为空', trigger: 'blur' }],
  productSecret: [{ required: true, message: '产品密钥不能为空', trigger: 'blur' }],
  transparent: [{ required: true, message: '是否透传不能为空', trigger: 'blur' }],
  dnTyp: [{ required: true, message: '唯一标识类型(1序列号 2蓝牙mac)不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref

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
      formData.value = await ProductApi.getProduct(id)
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
    const data = formData.value as unknown as ProductVO
    if (formType.value === 'create') {
      await ProductApi.createProduct(data)
      message.success(t('common.createSuccess'))
    } else {
      await ProductApi.updateProduct(data)
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
    name: undefined,
    categoryId: undefined,
    productKey: undefined,
    mcuCode: undefined,
    remark1: undefined,
    imgUrl: undefined,
    remark: undefined,
    status: undefined,
    nodeType: undefined,
    protocolCode: undefined,
    keepAliveTime: undefined,
    productSecret: undefined,
    transparent: undefined,
    dnTyp: undefined
  }
  formRef.value?.resetFields()
}
</script>
