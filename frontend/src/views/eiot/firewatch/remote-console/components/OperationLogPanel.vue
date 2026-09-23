<template>
  <div class="log-panel">
    <div class="panel-title">操作日志</div>
    <div v-loading="loading" class="log-list">
      <div v-for="item in list" :key="item.id" class="log-item" :class="item.statusClass">
        <!-- 第一行：时间 + 状态 -->
        <div class="log-row-1">
          <span class="log-time">{{ item.time }}</span>
          <span class="log-status" :class="item.statusClass">{{ item.statusText }}</span>
        </div>
        <!-- 第二行：操作人 + 按钮 + 标签 -->
        <div class="log-row-2">
          <span class="log-operator">
            <el-icon :size="13" style="vertical-align:middle"><User /></el-icon>
            <span>{{ item.operatorName || '-' }}</span>
          </span>
          <span class="log-dot" v-if="item.buttonName"></span>
          <span class="log-btn-name" v-if="item.buttonName">{{ item.buttonName }}</span>
          <span class="log-tag" :class="getFunctionTypeClass(item.functionType)" v-if="item.functionType">
            {{ getFunctionTypeText(item.functionType) }}
          </span>
          <span class="log-tag log-tag-action" v-if="item.actionText" :class="item.actionClass">
            {{ item.actionText }}
          </span>
          <span class="log-op-name" v-if="item.operationName">{{ item.operationName }}</span>
        </div>
      </div>
      <div v-if="!loading && list.length === 0" class="empty">暂无操作记录</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { User } from '@element-plus/icons-vue'
import { getOperationLogs } from '@/api/eiot/remote/remoteConsole.api'

const props = defineProps<{ deviceId: string | number }>()
const list = ref<any[]>([])
const loading = ref(false)

const formatTime = (value: any) => {
  if (!value) return '-'
  try {
    const date = value instanceof Date ? value : new Date(value)
    if (isNaN(date.getTime())) return String(value)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
  } catch {
    return String(value)
  }
}

/**
 * 功能类型显示文本：BUS=总线, MULTI=多线, GENERAL=通用
 */
const getFunctionTypeText = (ft?: string): string => {
  if (!ft) return ''
  const map: Record<string, string> = { BUS: '总线', MULTI: '多线', GENERAL: '通用' }
  return map[ft] || ft
}

const getFunctionTypeClass = (ft?: string): string => {
  if (!ft) return ''
  return `ft-${ft.toLowerCase()}`
}

/**
 * 判断是否为总线/多线，且操作类型为启动/停止
 */
const getActionInfo = (functionType?: string, operationType?: string): { text: string; cls: string } => {
  if (!functionType || !operationType) return { text: '', cls: '' }
  const isBusOrMulti = functionType === 'BUS' || functionType === 'MULTI'
  if (!isBusOrMulti) return { text: '', cls: '' }
  if (operationType === 'START') return { text: '启动', cls: 'action-start' }
  if (operationType === 'STOP') return { text: '停止', cls: 'action-stop' }
  return { text: '', cls: '' }
}

const fetch = async () => {
  if (!props.deviceId) return
  loading.value = true
  try {
    const res = await getOperationLogs(props.deviceId, 20)
    const raw = Array.isArray(res) ? res : res?.list || []
    list.value = raw.map((it: any) => {
      const s = it.commandStatus
      const action = getActionInfo(it.functionType, it.operationType)
      return {
        id: it.id,
        time: formatTime(it.operationTime),
        operatorName: it.operatorName || '-',
        operationName: it.operationName || '',
        buttonName: it.buttonName || '',
        functionType: it.functionType || '',
        operationType: it.operationType || '',
        actionText: action.text,
        actionClass: action.cls,
        statusText: s === 1 ? '成功' : s === 0 ? '失败' : '执行中',
        statusClass: s === 1 ? 'success' : s === 0 ? 'danger' : 'warning'
      }
    })
  } finally {
    loading.value = false
  }
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
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  scrollbar-width: none;
  -ms-overflow-style: none;
  &::-webkit-scrollbar { display: none; }
}

.log-item {
  border-radius: 8px;
  background: rgba($bg-panel, 0.5);
  border: 1px solid rgba($border-color, 0.5);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: background 0.2s;
  &:hover {
    background: rgba($bg-panel, 0.75);
  }
}

/* 第一行：时间 + 状态 */
.log-row-1 {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.log-time {
  font-size: 13px;
  color: $text-secondary;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.3px;
}

.log-status {
  font-size: 12px;
  padding: 2px 12px;
  border-radius: 10px;
  color: #fff;
  font-weight: 500;
  background: $text-muted;
  &.success { background: $success; }
  &.danger { background: $danger; }
  &.warning { background: $warning; }
}

/* 第二行：操作人 + 按钮 + 标签 */
.log-row-2 {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.log-operator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: $text-primary;
}

.log-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: $text-muted;
}

.log-btn-name {
  font-size: 13px;
  color: $text-primary;
  font-weight: 500;
}

.log-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  border: 1px solid transparent;
  font-weight: 500;
}

.log-tag.ft-bus {
  color: #1890ff;
  background: rgba(#1890ff, 0.15);
  border-color: rgba(#1890ff, 0.35);
}

.log-tag.ft-multi {
  color: #fa8c16;
  background: rgba(#fa8c16, 0.15);
  border-color: rgba(#fa8c16, 0.35);
}

.log-tag.ft-general {
  color: $text-secondary;
  background: rgba($text-muted, 0.15);
  border-color: rgba($text-muted, 0.25);
}

.log-tag-action {
  color: #fff;
  &.action-start { background: $success; border-color: transparent; }
  &.action-stop { background: $danger; border-color: transparent; }
}

.log-op-name {
  font-size: 12px;
  color: $text-secondary;
}

.empty {
  text-align: center;
  color: $text-secondary;
  padding: 40px 0;
}
</style>
