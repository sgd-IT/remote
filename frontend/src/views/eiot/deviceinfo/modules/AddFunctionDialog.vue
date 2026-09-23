<template>
  <el-dialog
    :title="isEdit ? '编辑功能' : '新增功能'"
    v-model="visible"
    width="600px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      v-loading="loading"
    >
      <el-form-item label="选择功能" prop="brandModelFunctionId">
        <el-select
          v-model="form.brandModelFunctionId"
          placeholder="请选择功能"
          clearable
          filterable
          style="width: 100%"
          @change="handleFunctionChange"
        >
          <el-option
            v-for="item in functionList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          >
            <span>{{ item.name }}</span>
            <span class="text-gray-400 text-xs ml-2">{{ item.functionId }}</span>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="按钮名称" prop="buttonName">
        <el-input
          v-model="form.buttonName"
          placeholder="请输入按钮显示名称（默认使用功能名称）"
          clearable
          maxlength="20"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="功能类型" prop="functionType">
        <el-select v-model="form.functionType" placeholder="请选择功能类型" style="width: 100%">
          <el-option label="通用按钮" value="GENERAL" />
          <el-option label="总线按钮" value="BUS" />
          <el-option label="多线按钮" value="MULTI" />
        </el-select>
      </el-form-item>

      <el-form-item label="子设备" prop="subDeviceId">
        <el-select v-model="form.subDeviceId" clearable placeholder="不绑定子设备" style="width: 100%">
          <el-option
            v-for="item in childDevices"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>

      <!-- 参数填写区域 -->
      <template v-if="selectedFunction?.params && selectedFunction.params.length > 0">
        <el-divider content-position="left">参数配置</el-divider>
        <el-form-item
          v-for="param in selectedFunction!.params"
          :key="param.id"
          :label="param.paramName"
          :prop="'paramValues.' + param.paramCode"
        >
          <el-input
            v-model="form.paramValues[param.paramCode]"
            :placeholder="'请输入' + param.paramName"
            clearable
          />
        </el-form-item>
      </template>

      <el-empty v-if="selectedFunction && (!selectedFunction.params || selectedFunction.params.length === 0)" description="该功能无需配置参数" :image-size="60" />
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { getBrandModelFunctions, type BrandModelFunctionVO } from '@/api/eiot/remote/brand.api'
import { addDeviceFunction, updateDeviceFunction } from '@/api/eiot/remote/deviceRemote.api'
import { getChildrenDeviceList } from '@/api/eiot/deviceinfo/devices.api'

const props = defineProps({
  deviceId: {
    type: [Number, String],
    required: true
  },
  brandModelId: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['success'])

const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const functionList = ref<BrandModelFunctionVO[]>([])
const isEdit = ref(false)
const editId = ref<number | undefined>(undefined)

const form = reactive({
  brandModelFunctionId: undefined as number | undefined,
  buttonName: '',
  functionType: 'GENERAL' as 'BUS' | 'MULTI' | 'GENERAL',
  subDeviceId: undefined as number | undefined,
  paramValues: {} as Record<string, any>
})

const rules = {
  brandModelFunctionId: [{ required: true, message: '请选择功能', trigger: 'change' }],
  functionType: [{ required: true, message: '请选择功能类型', trigger: 'change' }]
}

const childDevices = ref<any[]>([])

const loadChildDevices = async () => {
  try {
    const res = await getChildrenDeviceList({ deviceId: props.deviceId, pageSize: 1000 })
    childDevices.value = res?.list || []
  } catch (e) {
    childDevices.value = []
  }
}

const selectedFunction = computed(() => {
  if (!form.brandModelFunctionId) return null
  return functionList.value.find(f => f.id === form.brandModelFunctionId) || null
})

const loadFunctions = async () => {
  loading.value = true
  try {
    const res = await getBrandModelFunctions(props.brandModelId)
    functionList.value = res || []
    await loadChildDevices()
  } catch (e) {
    message.error('加载功能列表失败')
  } finally {
    loading.value = false
  }
}

const handleFunctionChange = (val: number) => {
  // 切换功能时清空参数值
  form.paramValues = {}
  const func = functionList.value.find(f => f.id === val)
  if (func?.params) {
    func.params.forEach(p => {
      form.paramValues[p.paramCode] = ''
    })
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (!form.brandModelFunctionId) {
      message.warning('请选择功能')
      return
    }
    submitting.value = true
    try {
      const data = {
        brandModelFunctionId: form.brandModelFunctionId,
        buttonName: form.buttonName || undefined,
        paramValues: form.paramValues,
        functionType: form.functionType,
        subDeviceId: form.subDeviceId
      }
      if (isEdit.value && editId.value) {
        await updateDeviceFunction(props.deviceId, editId.value, data)
        message.success('修改成功')
      } else {
        await addDeviceFunction(props.deviceId, data)
        message.success('添加成功')
      }
      visible.value = false
      emit('success')
    } catch (e) {
      message.error(isEdit.value ? '修改失败' : '添加失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleClose = () => {
  form.brandModelFunctionId = undefined
  form.buttonName = ''
  form.functionType = 'GENERAL'
  form.subDeviceId = undefined
  form.paramValues = {}
  childDevices.value = []
  isEdit.value = false
  editId.value = undefined
}

const open = async (editData?: any) => {
  visible.value = true
  await loadFunctions()
  if (editData) {
    isEdit.value = true
    editId.value = editData.id
    form.brandModelFunctionId = editData.brandModelFunctionId
    form.buttonName = editData.buttonName || ''
    form.functionType = editData.functionType || 'GENERAL'
    form.subDeviceId = editData.subDeviceId
    if (editData.paramValues) {
      if (typeof editData.paramValues === 'string') {
        try { form.paramValues = JSON.parse(editData.paramValues) } catch { form.paramValues = {} }
      } else {
        form.paramValues = JSON.parse(JSON.stringify(editData.paramValues))
      }
    } else {
      form.paramValues = {}
    }
  }
}

defineExpose({ open })
</script>
