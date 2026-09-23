<template>
  <div class="quick-actions">
    <div class="quick-actions-grid">
      <div
        v-for="action in actions"
        :key="action.path"
        class="action-item"
        @click="handleClick(action)"
        tabindex="0"
        role="button"
        :aria-label="action.label"
        @keydown.enter="handleClick(action)"
      >
        <div class="action-icon" :style="{ backgroundColor: action.bgColor }">
          <Icon :icon="action.icon" :size="24" :color="action.iconColor" />
        </div>
        <div class="action-label">{{ action.label }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

defineOptions({ name: 'QuickActions' })

const router = useRouter()

interface QuickAction {
  label: string
  icon: string
  iconColor: string
  bgColor: string
  path: string
  permission?: string
}

const actions: QuickAction[] = [
  {
    label: '设备管理',
    icon: 'ep:device',
    iconColor: '#409eff',
    bgColor: '#ecf5ff',
    path: '/eiot/deviceinfo'
  },
  {
    label: '产品管理',
    icon: 'ep:box',
    iconColor: '#67c23a',
    bgColor: '#f0f9eb',
    path: '/eiot/product'
  },
  {
    label: '告警列表',
    icon: 'ep:warning',
    iconColor: '#e6a23c',
    bgColor: '#fdf6ec',
    path: '/eiot/alarm/list'
  },
  {
    label: '规则引擎',
    icon: 'ep:set-up',
    iconColor: '#909399',
    bgColor: '#f4f4f5',
    path: '/eiot/ruleinfo'
  },
  {
    label: 'OTA升级',
    icon: 'ep:upload',
    iconColor: '#f56c6c',
    bgColor: '#fef0f0',
    path: '/eiot/ota/upgradePack'
  },
  {
    label: '虚拟设备',
    icon: 'ep:cpu',
    iconColor: '#9c27b0',
    bgColor: '#f3e8ff',
    path: '/eiot/virtualDevice'
  },
  {
    label: '消息中心',
    icon: 'ep:message',
    iconColor: '#00bcd4',
    bgColor: '#e0f7fa',
    path: '/eiot/messageCenter/list'
  },
  {
    label: '定时任务',
    icon: 'ep:timer',
    iconColor: '#ff9800',
    bgColor: '#fff3e0',
    path: '/eiot/scheduledTask'
  }
]

const handleClick = (action: QuickAction) => {
  router.push(action.path)
}
</script>

<style scoped lang="scss">
.quick-actions {
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 24px;
  margin-top: 16px;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border-color: var(--el-color-primary-light-5);
    background: var(--el-fill-color);
  }

  &:active {
    transform: translateY(-2px);
  }

  &:focus {
    outline: 2px solid var(--el-color-primary);
    outline-offset: 2px;
  }
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s;

  .action-item:hover & {
    transform: scale(1.1);
  }
}

.action-label {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 500;
  text-align: center;
}

@media (max-width: 768px) {
  .quick-actions-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .action-item {
    padding: 16px 12px;

    .action-icon {
      width: 48px;
      height: 48px;
    }

    .action-label {
      font-size: 13px;
    }
  }
}
</style>
