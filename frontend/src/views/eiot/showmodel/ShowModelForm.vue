<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="模型code" prop="modelCode">
        <el-input v-model="formData.modelCode" placeholder="请输入模型code" />
      </el-form-item>
      <el-form-item label="配置名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入配置名称" />
      </el-form-item>
      <el-form-item label="类型 " prop="typ">
        <el-select v-model="formData.typ">
          <el-option v-for="dict in getIntDictOptions(DICT_TYPE.EIOT_SHOW_MODEL_TYP)" :key="dict.value" :value="dict.value"
                     :label="dict.label"/>
        </el-select>
      </el-form-item>
      <el-form-item label="显示内容" prop="cnf">
        <el-input v-model="formData.cnf" placeholder="请输入显示内容" />
      </el-form-item>


      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value"
                    :value="dict.value" :label="dict.label"/>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ShowModelApi, ShowModelVO } from '@/api/eiot/showmodel'
import {getIntDictOptions,DICT_TYPE} from "@/utils/dict";

/** 产品显示模型 表单 */
defineOptions({ name: 'ShowModelForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  cnf: undefined,
  typ: undefined,
  name: undefined,
  remark: undefined,
  modelCode: undefined,
  status: undefined
})
const formRules = reactive({
  cnf: [{ required: true, message: '显示内容不能为空', trigger: 'blur' }],
  typ: [{ required: true, message: '类型 不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '配置名称不能为空', trigger: 'blur' }],
  modelCode: [{ required: true, message: '模型code不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'blur' }]
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
      formData.value = await ShowModelApi.getShowModel(id)
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
    const data = formData.value as unknown as ShowModelVO
    if (formType.value === 'create') {
      await ShowModelApi.createShowModel(data)
      message.success(t('common.createSuccess'))
    } else {
      await ShowModelApi.updateShowModel(data)
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
    cnf: undefined,
    typ: undefined,
    name: undefined,
    remark: undefined,
    modelCode: undefined,
    status: undefined
  }
  formRef.value?.resetFields()
}
</script>
