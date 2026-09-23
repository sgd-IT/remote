<template>
  <div class="log-panel">
    <div class="panel-title">警情日志</div>
    <div v-loading="loading" class="log-list">
      <div
        v-for="item in list"
        :key="item.id"
        class="log-item"
        :class="getLevelClass(item.level)"
      >
        <div class="log-header">
          <div class="log-time">{{ formatTime(item.alertTime) }}</div>
          <div class="log-badge" :class="getLevelClass(item.level)">
            {{ getAlarmTypeText(item) }}
          </div>
        </div>
        <div class="log-device">{{ item.deviceName || item.productName || '-' }}</div>
        <div class="log-location">
          <span class="loc-label">位置</span>
          <span class="loc-value">{{ item.address || '-' }}</span>
        </div>
        <!-- 日志详情 -->
        <div class="log-details" :title="item.details">{{ item.details || '-' }}</div>
      </div>
      <div v-if="!loading && list.length === 0" class="empty">暂无警情记录</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { getAlertRecordPage, type AlertRecordVO } from '@/api/eiot/remote/deviceRemote.api'

const props = defineProps<{ deviceId: string | number }>()
const list = ref<AlertRecordVO[]>([])
const loading = ref(false)

const fetch = async () => {
  loading.value = true
  try {
    const res = await getAlertRecordPage({ pageNo: 1, pageSize: 20, deviceId: props.deviceId })
    list.value = res?.list || []
  } finally {
    loading.value = false
  }
}

const formatTime = (t?: number) => {
  if (!t) return '-'
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

/**
 * level 映射（与报警列表一致）：
 * 1 = 火警 → 红
 * 2 = 故障 → 橙
 * 3 = 离线 → 黄
 * 4 = 反馈 → 蓝
 * 5 = 其他 → 灰
 */
const levelMap: Record<string, string> = {
  '1': 'fire',
  '2': 'fault',
  '3': 'offline',
  '4': 'feedback',
  '5': 'other'
}

const typeTextMap: Record<string, string> = {
  fire: '火警',
  fault: '故障',
  offline: '离线',
  feedback: '反馈',
  other: '其他'
}

/** 报警类型映射（alarmType 字段）：0=主机报警 1=高温 2=热成像 3=烟火 4=离岗 5=消防通道占用 */
const alarmTypeTextMap: Record<number, string> = {
  0: '主机报警',
  1: '高温事件报警',
  2: '热成像事件报警',
  3: '烟火事件报警',
  4: '离岗事件报警',
  5: '消防通道占用事件报警'
}

const getLevelClass = (level?: string): string => {
  if (!level) return 'level-default'
  return `level-${levelMap[String(level)] || 'default'}`
}

/**
 * 报警类型显示：优先使用 alarmType 字段，旧数据无该字段时回退按 level 判断
 */
const getAlarmTypeText = (item: AlertRecordVO): string => {
  if (item.alarmType !== undefined && item.alarmType !== null && alarmTypeTextMap[item.alarmType]) {
    return alarmTypeTextMap[item.alarmType]
  }
  if (item.level) {
    const cat = levelMap[String(item.level)]
    if (cat && typeTextMap[cat]) return typeTextMap[cat]
  }
  return '告警'
}

watch(() => props.deviceId, fetch, { immediate: true })
</script>

<style lang="scss" scoped>
@import '../styles/common.scss';

.log-panel {
  background: $bg-panel;
  border-left: 1px solid $border-color;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-title {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 500;
  color: $text-primary;
  border-bottom: 1px solid $border-color;
}

.log-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  scrollbar-width: none;
  -ms-overflow-style: none;
  &::-webkit-scrollbar {
    display: none;
  }
}

.log-item {
  padding: 12px 12px;
  border-radius: 6px;
  margin-bottom: 10px;
  border: 1px solid transparent;
  border-left: 3px solid transparent;
  background: rgba(255, 255, 255, 0.03);
}

/* 等级背景色：红→橙→黄→蓝→灰 */
.log-item.level-fire {
  background: rgba($danger, 0.15);
  border-color: rgba($danger, 0.35);
  border-left-color: $danger;
}

.log-item.level-fault {
  background: rgba($warning, 0.15);
  border-color: rgba($warning, 0.35);
  border-left-color: $warning;
}

.log-item.level-offline {
  background: rgba(#fadb14, 0.12);
  border-color: rgba(#fadb14, 0.3);
  border-left-color: #fadb14;
}

.log-item.level-feedback {
  background: rgba(#1890ff, 0.12);
  border-color: rgba(#1890ff, 0.3);
  border-left-color: #1890ff;
}

.log-item.level-other {
  background: rgba($text-muted, 0.12);
  border-color: rgba($text-muted, 0.25);
  border-left-color: $text-muted;
}

.log-item.level-default {
  background: rgba(255, 255, 255, 0.04);
  border-left-color: $text-muted;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.log-time {
  font-size: 12px;
  color: $text-muted;
}

.log-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  background: $text-muted;
}

.log-badge.level-fire {
  background: $danger;
}

.log-badge.level-fault {
  background: $warning;
}

.log-badge.level-offline {
  background: #fadb14;
  color: #333;
}

.log-badge.level-feedback {
  background: #1890ff;
}

.log-badge.level-other {
  background: $text-muted;
}

.log-device {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 4px;
}

.log-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.loc-label {
  color: $text-muted;
}

.loc-value {
  color: $text-secondary;
}

/* 日志详情：最多两行，超出省略，悬停查看完整内容 */
.log-details {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed rgba($text-muted, 0.25);
  font-size: 12px;
  color: $text-secondary;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.empty {
  text-align: center;
  color: $text-secondary;
  padding: 30px 0;
}
</style>
