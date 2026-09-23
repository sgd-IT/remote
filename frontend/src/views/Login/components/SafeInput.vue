<template>
  <div class="safe-input-wrapper">
    <div class="input-label" v-if="label">{{ label }}</div>
    <div
      class="safe-input"
      :class="{ focused: isFocused, error: hasError }"
    >
      <span class="input-icon" v-if="prefixIcon">
        <Icon :icon="prefixIcon" :size="14" />
      </span>
      <input
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @keyup.enter="$emit('enter')"
      />
      <span
        class="input-icon suffix"
        v-if="showPasswordToggle"
        @click="togglePassword"
      >
        <Icon :icon="showPassword ? 'ep:view' : 'ep:hide'" :size="14" />
      </span>
    </div>
    <div class="input-error" v-if="hasError && errorMessage">{{ errorMessage }}</div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { Icon } from '@/components/Icon'

const props = defineProps<{
  modelValue: string
  label?: string
  placeholder?: string
  type?: string
  prefixIcon?: string
  showPasswordToggle?: boolean
  hasError?: boolean
  errorMessage?: string
}>()

defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'enter'): void
}>()

const isFocused = ref(false)
const showPassword = ref(false)

const inputType = computed(() => {
  if (props.type === 'password' && props.showPasswordToggle) {
    return showPassword.value ? 'text' : 'password'
  }
  return props.type || 'text'
})

const togglePassword = () => {
  showPassword.value = !showPassword.value
}
</script>

<style lang="scss" scoped>
.safe-input-wrapper {
  margin-bottom: 16px;
  position: relative;
}
.input-label {
  color: rgba(255, 255, 255, 0.45);
  font-size: 13px;
  margin-bottom: 6px;
}
.safe-input {
  width: 100%;
  height: 44px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  gap: 10px;
  transition: border-color 0.3s;
  box-sizing: border-box;

  input {
    flex: 1;
    background: transparent;
    border: none;
    outline: none;
    color: #fff;
    font-size: 15px;
    &::placeholder {
      color: rgba(255, 255, 255, 0.18);
    }
    &:-webkit-autofill,
    &:-webkit-autofill:hover,
    &:-webkit-autofill:focus,
    &:-webkit-autofill:active {
      -webkit-box-shadow: 0 0 0 1000px rgba(13, 27, 42, 0.95) inset !important;
      -webkit-text-fill-color: #fff !important;
      caret-color: #fff !important;
      transition: background-color 5000s ease-in-out 0s;
    }
  }

  &.focused {
    border-color: rgba(255, 255, 255, 0.06);
  }

  &.error {
    border-color: rgba(255, 82, 82, 0.4);
  }
}
.input-icon {
  color: rgba(255, 255, 255, 0.25);
  font-size: 16px;
  display: flex;
  align-items: center;
  &.suffix {
    cursor: pointer;
    &:hover {
      color: rgba(255, 255, 255, 0.45);
    }
  }
}
.input-error {
  color: #ff5252;
  font-size: 12px;
  margin-top: 4px;
}
</style>
