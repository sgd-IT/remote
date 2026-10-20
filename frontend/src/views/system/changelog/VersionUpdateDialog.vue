<script lang="ts" setup>
import { CHANGELOG, CHANGE_TYPE_LABEL, type ChangelogItemType } from './data'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import { useRouter } from 'vue-router'

defineProps<{ modelValue: boolean }>()
const emit = defineEmits(['update:modelValue'])

const { wsCache } = useCache()
const { push } = useRouter()

const latest = CHANGELOG[0]

// 弹窗只放最新版本的前几条，完整历史留在更新日志页
const previewItems = latest?.items.slice(0, 5) ?? []

const tagTypeMap: Record<ChangelogItemType, 'primary' | 'success' | 'warning'> = {
  feature: 'primary',
  improve: 'success',
  fix: 'warning'
}

// 关闭弹窗（点 X / 遮罩 / 按钮）即视为已读，下次登录不再弹同一版本
const markRead = () => {
  if (latest) {
    wsCache.set(CACHE_KEY.CHANGELOG_READ_VERSION, latest.version)
  }
}

const viewAll = () => {
  markRead()
  emit('update:modelValue', false)
  push('/changelog')
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    width="540px"
    align-center
    class="version-update-dialog"
    @update:model-value="emit('update:modelValue', $event)"
    @closed="markRead"
  >
    <template #header>
      <div class="dialog-header">
        <span class="dialog-title">系统已更新</span>
        <span class="dialog-version">{{ latest?.version }}</span>
      </div>
    </template>

    <div class="dialog-body">
      <div class="dialog-meta">{{ latest?.date }} · {{ latest?.title }}</div>
      <ul class="dialog-items">
        <li v-for="(item, index) in previewItems" :key="index" class="dialog-item">
          <el-tag :type="tagTypeMap[item.type]" size="small" effect="plain">
            {{ CHANGE_TYPE_LABEL[item.type] }}
          </el-tag>
          <span class="item-text">{{ item.text }}</span>
        </li>
      </ul>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">我知道了</el-button>
      <el-button type="primary" @click="viewAll">查看完整更新</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dialog-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.dialog-version {
  padding: 1px 10px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 999px;
}

.dialog-meta {
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.dialog-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.dialog-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.item-text {
  font-size: 14px;
  line-height: 22px;
  color: var(--el-text-color-regular);
}
</style>
