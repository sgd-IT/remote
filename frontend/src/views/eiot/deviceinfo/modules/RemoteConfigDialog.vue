<template>
  <el-dialog
    title="远程控制设置"
    v-model="visible"
    width="500px"
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
      <el-form-item label="品牌型号" prop="brandModelId">
        <el-cascader
          v-model="cascaderValue"
          :options="brandOptions"
          :props="cascaderProps"
          placeholder="请选择品牌 / 型号 / 版本"
          clearable
          filterable
          style="width: 100%"
          @change="handleCascaderChange"
        />
      </el-form-item>
      <el-form-item v-if="selectedModelName">
        <el-alert type="info" :closable="false" class="w-full">
          <template #default>
            <div>已选择：{{ selectedBrandName }} / {{ selectedModelName }}</div>
          </template>
        </el-alert>
      </el-form-item>
      <el-form-item label="二次确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请输入二次确认密码（留空表示不设置）"
          show-password
          clearable
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { getBrandCascade, type BrandCascadeVO } from '@/api/eiot/remote/brand.api'
import { saveDeviceRemoteConfig } from '@/api/eiot/remote/deviceRemote.api'
import { type DeviceRemoteConfigVO } from '@/api/eiot/remote/deviceRemote.api'

const props = defineProps({
  deviceId: {
    type: [Number, String],
    required: true
  },
  remoteConfig: {
    type: Object as () => DeviceRemoteConfigVO | null,
    default: null
  }
})

const emit = defineEmits(['success'])

const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  brandModelId: undefined as number | undefined,
  confirmPassword: ''
})

const cascaderValue = ref<number[]>([])
const brandOptions = ref<any[]>([])
const selectedBrandName = ref('')
const selectedModelName = ref('')

const rules = {
  brandModelId: [{ required: true, message: '请选择品牌型号', trigger: 'change' }]
}

const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  emitPath: true
}

const loadBrandCascade = async () => {
  loading.value = true
  try {
    const res = await getBrandCascade()
    const data = res || []
    brandOptions.value = data.map((brand: BrandCascadeVO) => ({
      id: brand.id,
      name: brand.name,
      children: (brand.models || []).map((model: any) => ({
        id: model.id,
        name: `${brand.name} - ${model.model}${model.versions && model.versions.length > 0 ? ' (' + model.versions.join(', ') + ')' : ''}`,
        rawModel: model.model,
        rawVersion: model.versions ? model.versions[0] : '',
        brandName: brand.name
      }))
    }))
  } catch (e) {
    message.error('加载品牌数据失败')
  } finally {
    loading.value = false
  }
}

const handleCascaderChange = (val: number[]) => {
  if (val && val.length === 2) {
    const brandId = val[0]
    const modelId = val[1]
    form.brandModelId = modelId

    const brand = brandOptions.value.find((b: any) => b.id === brandId)
    if (brand) {
      selectedBrandName.value = brand.name
      const model = brand.children?.find((m: any) => m.id === modelId)
      if (model) {
        selectedModelName.value = model.rawModel + (model.rawVersion ? ' / ' + model.rawVersion : '')
      }
    }
  } else {
    form.brandModelId = undefined
    selectedBrandName.value = ''
    selectedModelName.value = ''
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (!form.brandModelId) {
      message.warning('请选择品牌型号')
      return
    }
    submitting.value = true
    try {
      await saveDeviceRemoteConfig(props.deviceId, {
        brandModelId: form.brandModelId,
        confirmPassword: form.confirmPassword
      })
      message.success('保存成功')
      visible.value = false
      emit('success')
    } catch (e) {
      message.error('保存失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleClose = () => {
  cascaderValue.value = []
  form.brandModelId = undefined
  form.confirmPassword = ''
  selectedBrandName.value = ''
  selectedModelName.value = ''
}

const open = async () => {
  visible.value = true
  await loadBrandCascade()
  // 回显已有配置
  const config = props.remoteConfig
  if (config?.brandModelId) {
    for (const brand of brandOptions.value) {
      const model = brand.children?.find((m: any) => m.id === config.brandModelId)
      if (model) {
        cascaderValue.value = [brand.id, config.brandModelId]
        selectedBrandName.value = brand.name
        selectedModelName.value = model.rawModel + (model.rawVersion ? ' / ' + model.rawVersion : '')
        form.brandModelId = config.brandModelId
        break
      }
    }
  }
  // 密码不回显明文，保持空（表示不修改）
  form.confirmPassword = ''
}

defineExpose({ open })
</script>
