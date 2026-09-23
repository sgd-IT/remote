import type { PropertyMapState } from './usePropertyMapData'

export type PropertyMapRefreshFailure = {
  state: Extract<PropertyMapState, 'forbidden' | 'stale' | 'error'>
  clearSnapshot: boolean
  message: string
}

/** Pure failure policy: retain a known-good snapshot for transient errors, never for denied access. */
export function propertyMapRefreshFailure(error: unknown, hasSnapshot: boolean): PropertyMapRefreshFailure {
  const status = (error as { response?: { status?: unknown } })?.response?.status
  if (status === 401 || status === 403) return { state: 'forbidden', clearSnapshot: true, message: '无权查看物业位置' }
  return hasSnapshot
    ? { state: 'stale', clearSnapshot: false, message: '物业位置更新失败，显示上次数据' }
    : { state: 'error', clearSnapshot: false, message: '物业位置加载失败' }
}
