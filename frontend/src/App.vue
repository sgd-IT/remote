<script lang="ts" setup>
import { isDark } from '@/utils/is'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/modules/app'
import { useDesign } from '@/hooks/web/useDesign'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import routerSearch from '@/components/RouterSearch/index.vue'
import VersionUpdateDialog from '@/views/system/changelog/VersionUpdateDialog.vue'
import { CHANGELOG } from '@/views/system/changelog/data'

defineOptions({ name: 'APP' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('app')
const appStore = useAppStore()
const currentSize = computed(() => appStore.getCurrentSize)
const greyMode = computed(() => appStore.getGreyMode)
const { wsCache } = useCache()
const route = useRoute()

/**
 * 深蓝主题页面（大屏/设备控制室/远程控制中心等，路由 meta.darkBlue = true）：
 * 同步 html/body class，使页面跳转期间与数据加载期间背景保持深蓝，避免白色闪现
 */
const DARK_BLUE_CLASS = 'dark-blue-screen'
const syncDarkBlueBodyClass = () => {
  const darkBlue = !!route.meta?.darkBlue
  document.documentElement.classList.toggle(DARK_BLUE_CLASS, darkBlue)
  document.body.classList.toggle(DARK_BLUE_CLASS, darkBlue)
}
watch(() => route.meta?.darkBlue, syncDarkBlueBodyClass, { immediate: true })

/**
 * 更新提醒弹窗
 * 登录后落在的首页可能是大屏（不经过 Layout），所以放在 App 全局兜底；
 * 路由一旦从登录页切到业务页就检查一次，同一会话内只检查一次，避免反复打断操作。
 */
const versionDialogVisible = ref(false)
let versionChecked = false
const checkVersionUpdate = () => {
  if (versionChecked) return
  // 未登录页面不检查，避免登录页提前看到内部更新内容
  if (['/login', '/sso', '/social-login'].some((path) => route.path.startsWith(path))) return
  if (!wsCache.get(CACHE_KEY.USER)) return
  versionChecked = true
  const latest = CHANGELOG[0]
  if (latest && wsCache.get(CACHE_KEY.CHANGELOG_READ_VERSION) !== latest.version) {
    versionDialogVisible.value = true
  }
}
watch(() => route.path, checkVersionUpdate, { immediate: true })

// 根据浏览器当前主题设置系统主题色
const setDefaultTheme = () => {
  let isDarkTheme = wsCache.get(CACHE_KEY.IS_DARK)
  if (isDarkTheme === null) {
    isDarkTheme = isDark()
  }
  appStore.setIsDark(false)
  // appStore.setIsDark(isDarkTheme)
}
setDefaultTheme()
</script>
<template>
  <ConfigGlobal :size="currentSize">
    <RouterView :class="greyMode ? `${prefixCls}-grey-mode` : ''" />
    <routerSearch />
    <VersionUpdateDialog v-model="versionDialogVisible" />
  </ConfigGlobal>
</template>
<style lang="scss">
$prefix-cls: #{$namespace}-app;

.size {
  width: 100%;
  height: 100%;
}

html,
body {
  @extend .size;

  padding: 0 !important;
  margin: 0;
  overflow: hidden;

  #app {
    @extend .size;
  }
}

.#{$prefix-cls}-grey-mode {
  filter: grayscale(100%);
}
</style>
