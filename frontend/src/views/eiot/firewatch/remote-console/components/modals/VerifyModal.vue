<template>
  <el-dialog
    v-model="visible"
    title="操作确认"
    width="360px"
    :close-on-click-modal="false"
    custom-class="remote-console-modal"
    append-to-body
    destroy-on-close
  >
    <div class="verify-body">
      <div class="verify-tip"><span class="verify-icon">⚠️</span>{{ actionName }}</div>
      <el-form :model="form" label-width="80px" size="small">
        <el-form-item label="操作指令">
          <el-input v-model="form.password" type="password" placeholder="请输入操作指令" show-password />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button size="small" @click="visible = false">取消</el-button>
      <el-button size="small" type="primary" @click="handleConfirm">确认执行</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, watch, computed } from 'vue'

const props = defineProps<{
  modelValue: boolean
  actionName: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'confirm', data: { password: string }): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const form = ref({ password: '' })

watch(() => props.modelValue, (val) => {
  if (val) form.value = { password: '' }
})

const handleConfirm = () => {
  if (!form.value.password) return
  emit('confirm', { ...form.value })
  visible.value = false
}
</script>

<style lang="scss" scoped>
.verify-body {
  padding: 0 8px;
}

.verify-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #000000;
  font-size: 14px;
  margin-bottom: 14px;
  padding: 8px 12px;
  background: rgba(230, 162, 60, 0.12);
  border: 1px solid rgba(230, 162, 60, 0.3);
  border-radius: 4px;
}

.verify-icon {
  font-size: 18px;
  flex-shrink: 0;
}
</style>
