<template>
  <div class="function-bar">
    <button class="func-btn" @click="$emit('filter')">
      <span>🔍</span>
      <span>记录筛选</span>
    </button>
    <button class="func-btn" @click="$emit('log')">
      <span>📋</span>
      <span>操作日志</span>
    </button>
    <button class="func-btn" @click="$emit('alarm')">
      <span>🚨</span>
      <span>告警处理</span>
    </button>
    <button class="func-btn" @click="$emit('call')">
      <span>📞</span>
      <span>通话录音</span>
    </button>
  </div>
</template>

<script lang="ts" setup>
defineEmits<{
  (e: 'filter'): void
  (e: 'log'): void
  (e: 'alarm'): void
  (e: 'call'): void
}>()
</script>

<style lang="scss" scoped>
.function-bar {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 180, 255, 0.08);
}

.func-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(13, 30, 56, 0.85);
  border: 1px solid rgba(0, 180, 255, 0.25);
  border-radius: 4px;
  color: #e8f0fe;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.func-btn:hover {
  border-color: rgba(0, 180, 255, 0.4);
  box-shadow: 0 0 12px rgba(0, 180, 255, 0.15);
}

.func-btn.active {
  background: rgba(0, 180, 255, 0.15);
  border-color: #00b4ff;
}
</style>
