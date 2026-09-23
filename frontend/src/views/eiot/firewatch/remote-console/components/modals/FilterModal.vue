<template>
  <el-dialog
    v-model="visible"
    title="记录筛选"
    width="500px"
    :close-on-click-modal="false"
    custom-class="remote-console-modal"
    append-to-body
    destroy-on-close
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="时间范围">
        <el-select v-model="form.timeRange" style="width: 100%">
          <el-option label="最近1小时" value="1h" />
          <el-option label="最近24小时" value="24h" />
          <el-option label="最近7天" value="7d" />
          <el-option label="自定义范围" value="custom" />
        </el-select>
      </el-form-item>
      <el-form-item label="设备类型">
        <el-select v-model="form.deviceType" style="width: 100%">
          <el-option label="全部设备" value="" />
          <el-option label="水泵" value="pump" />
          <el-option label="喷淋" value="spray" />
          <el-option label="排烟" value="smoke" />
          <el-option label="防火卷帘" value="shutter" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作类型">
        <el-select v-model="form.operationType" style="width: 100%">
          <el-option label="全部操作" value="" />
          <el-option label="启动" value="start" />
          <el-option label="停止" value="stop" />
          <el-option label="复位" value="reset" />
          <el-option label="消音" value="mute" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="form.keyword" placeholder="输入搜索关键字" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" @click="handleApply">应用筛选</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const form = reactive({
  timeRange: '24h',
  deviceType: '',
  operationType: '',
  keyword: ''
})

const handleReset = () => {
  form.timeRange = '24h'
  form.deviceType = ''
  form.operationType = ''
  form.keyword = ''
}

const handleApply = () => {
  ElMessage.success('筛选条件已应用')
  visible.value = false
}
</script>
