<template>
  <div class="password-strength">
    <div class="strength-bars">
      <div
        v-for="i in 4"
        :key="i"
        class="strength-bar"
        :class="{ active: i <= level, [`level-${level}`]: i <= level }"
      ></div>
    </div>
    <div class="strength-text">
      <span :class="`text-level-${level}`">{{ strengthLabel }}</span>
      <span v-if="level <= 2 && level > 0" class="strength-tip">{{ tip }}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

const props = defineProps<{
  password: string
}>()

const level = computed(() => {
  const pwd = props.password
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 6) score++
  if (pwd.length >= 10) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd) && /[^a-zA-Z0-9]/.test(pwd)) score++
  return Math.min(score, 4)
})

const strengthLabel = computed(() => {
  const labels = ['', '密码强度：弱', '密码强度：弱', '密码强度：中', '密码强度：强']
  return labels[level.value]
})

const tip = '建议包含大小写字母和数字'
</script>

<style lang="scss" scoped>
.password-strength {
  margin-bottom: 14px;
}
.strength-bars {
  display: flex;
  gap: 3px;
  margin-bottom: 4px;
}
.strength-bar {
  flex: 1;
  height: 3px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
  transition: background 0.3s;
  &.active {
    &.level-1, &.level-2 { background: #ff5252; }
    &.level-3 { background: #ffb74d; }
    &.level-4 { background: #00e676; }
  }
}
.strength-text {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}
.text-level-1, .text-level-2 { color: #ff5252; }
.text-level-3 { color: #ffb74d; }
.text-level-4 { color: #00e676; }
.strength-tip {
  color: rgba(255, 255, 255, 0.2);
}
</style>
