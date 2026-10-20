<template>
  <div></div>
</template>
<script lang="ts" setup>
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'

defineOptions({ name: 'HomeRedirect' })

const { replace } = useRouter()
const { wsCache } = useCache()

// 与登录页保持同一口径：super_admin/monitor_role/fire_watcher → 值守端大屏，其余企业账号 → 监控中心大屏
const getHomePath = () => {
  try {
    const userInfo = wsCache.get(CACHE_KEY.USER)
    if (
      userInfo?.roles?.some(
        (r: string) => r === 'super_admin' || r === 'monitor_role' || r === 'fire_watcher'
      )
    ) {
      return '/duty-screen'
    }
  } catch {
    // ignore
  }
  return '/monitoring-center'
}

replace({ path: getHomePath() })
</script>
