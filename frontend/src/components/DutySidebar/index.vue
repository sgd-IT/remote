<template>
  <nav class="sidebar-nav">
    <div class="sidebar-items" :class="{ hidden: collapsed }">
      <div
        v-for="item in navItems"
        :key="item.key"
        class="sidebar-item"
        :class="{ active: activeNav === item.key }"
        @click="navigateTo(item)"
      >
        <el-tooltip :content="item.label" placement="right" :show-after="300">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </el-tooltip>
        <div class="active-bar"></div>
      </div>
    </div>
    <div class="sidebar-bottom">
      <button class="sidebar-toggle" @click="collapsed = !collapsed">
        <el-icon :size="18" :class="{ rotated: collapsed }"><Fold /></el-icon>
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)

const navItems = [
  { key: 'duty-screen', label: '大屏值守台', icon: 'Monitor', path: '/duty-screen' },
  { key: 'remote-console', label: '远程控制台', icon: 'Grid', path: '/remote-console' },
  { key: 'ops-workbench', label: '运维工作台', icon: 'SetUp', path: '/ops-workbench' },
  { key: 'data-center', label: '数据中心', icon: 'TrendCharts', path: '/data-center' },
  { key: 'video-surveillance', label: '视频监控', icon: 'VideoCamera', path: '/video-surveillance' },
  { key: 'patrol', label: '查岗管理', icon: 'Avatar', path: '/eiot/firewatch/patrol' },
]

const activeNav = computed(() => {
  const path = route.path
  const item = navItems.find(n => path === n.path || (n.path !== '/duty-screen' && path.startsWith(n.path + '/')))
  return item?.key || 'duty-screen'
})

const navigateTo = (item: any) => {
  if (route.path !== item.path) {
    router.push(item.path)
  }
}
</script>

<style lang="scss" scoped>
$blue: #1890ff;

.sidebar-nav {
  position: absolute;
  left: 0;
  top: 56px;
  bottom: 0;
  width: 56px;
  z-index: 20;
  background: linear-gradient(180deg, rgba(8, 24, 48, 0.95) 0%, rgba(6, 18, 40, 0.95) 100%);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-right: 1px solid rgba(30, 58, 95, 0.3);
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: auto;

  .sidebar-items {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    width: 100%;
    flex: 1;
    padding-top: 12px;
    overflow: hidden;
    transition: opacity 0.2s ease, padding-top 0.3s ease;
    opacity: 1;

    &.hidden {
      opacity: 0;
      flex: 0;
      padding-top: 0;
      pointer-events: none;
    }
  }

  .sidebar-item {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 52px;
    position: relative;
    cursor: pointer;
    color: rgba(139, 157, 197, 0.55);
    transition: all 0.25s ease;

    .el-icon {
      font-size: 22px;
      transition: color 0.25s;
    }

    &:hover {
      color: rgba(79, 195, 247, 0.8);
      background: rgba(24, 144, 255, 0.06);
    }

    &.active {
      color: rgba(79, 195, 247, 1);

      .active-bar {
        opacity: 1;
      }
    }

    .active-bar {
      position: absolute;
      right: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 28px;
      background: $blue;
      border-radius: 3px 0 0 3px;
      box-shadow: 0 0 8px rgba($blue, 0.6), 0 0 16px rgba($blue, 0.2);
      opacity: 0;
      transition: opacity 0.3s;
    }
  }

  .sidebar-bottom {
    flex-shrink: 0;
    padding: 8px 0 12px;
    display: flex;
    justify-content: center;
    width: 100%;
  }

  .sidebar-toggle {
    width: 36px;
    height: 36px;
    background: transparent;
    border: 1px solid rgba(30, 58, 95, 0.3);
    border-radius: 8px;
    color: rgba(139, 157, 197, 0.5);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    transition: all 0.25s;

    .el-icon {
      transition: transform 0.3s;
    }

    .el-icon.rotated {
      transform: rotate(180deg);
    }

    &:hover {
      color: rgba(79, 195, 247, 0.9);
      border-color: rgba(79, 195, 247, 0.3);
      background: rgba(24, 144, 255, 0.08);
    }
  }
}
</style>
