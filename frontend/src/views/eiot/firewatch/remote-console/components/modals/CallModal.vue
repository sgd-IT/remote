<template>
  <el-dialog
    v-model="visible"
    title="通话录音"
    width="600px"
    :close-on-click-modal="false"
    custom-class="remote-console-modal"
    append-to-body
    destroy-on-close
  >
    <div class="call-list">
      <div v-for="(item, index) in callList" :key="index" class="call-item">
        <div class="call-icon">📱</div>
        <div class="call-info">
          <div class="call-number">{{ item.number }}</div>
          <div class="call-time">{{ item.time }}</div>
        </div>
        <div class="call-duration">{{ item.duration }}</div>
        <button class="play-btn">▶ 播放</button>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

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

const callList = [
  { number: '138****1234', time: '2024-01-15 14:30:00', duration: '02:35' },
  { number: '139****5678', time: '2024-01-15 14:15:00', duration: '01:42' },
  { number: '137****9012', time: '2024-01-15 13:50:00', duration: '03:18' }
]
</script>

<style lang="scss" scoped>
.call-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  background: rgba(13, 30, 56, 0.6);
  border: 1px solid rgba(0, 180, 255, 0.1);
  border-radius: 6px;
  margin-bottom: 10px;
}

.call-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 180, 255, 0.1);
  border-radius: 50%;
  font-size: 18px;
}

.call-info {
  flex: 1;
}

.call-number {
  color: #e8f0fe;
  font-size: 14px;
  margin-bottom: 4px;
}

.call-time {
  color: #8ba3c7;
  font-size: 12px;
}

.call-duration {
  color: #8ba3c7;
  font-size: 13px;
}

.play-btn {
  padding: 8px 16px;
  background: rgba(0, 180, 255, 0.15);
  border: 1px solid rgba(0, 180, 255, 0.3);
  border-radius: 4px;
  color: #00b4ff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.play-btn:hover {
  background: rgba(0, 180, 255, 0.25);
}
</style>
