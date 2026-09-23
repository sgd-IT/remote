<template>
  <div></div>
</template>
<script lang="ts" setup>
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'

defineOptions({ name: 'HomeRedirect' })

const { replace } = useRouter()
const { wsCache } = useCache()

// 根据用户角色决定跳转目标：super_admin/monitor_role → /duty-screen(值守端大屏，查看所有企业)，企业账号 → /monitoring-center(监控中心大屏，仅本企业)
const getHomePath = () => {
  try {
    const userInfo = wsCache.get(CACHE_KEY.USER)
    if (userInfo?.roles?.some((r: string) => r === 'super_admin' || r === 'monitor_role')) {
      return '/duty-screen'
    }
  } catch {
    // ignore
  }
  return '/monitoring-center'
}

replace({ path: getHomePath() })
</script>
