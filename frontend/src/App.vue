<script lang="ts" setup>
import { isDark } from '@/utils/is'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/modules/app'
import { useDesign } from '@/hooks/web/useDesign'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import routerSearch from '@/components/RouterSearch/index.vue'

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
