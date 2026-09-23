import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { getProperty, getPropertyListForDutyScreen, type FirePropertyMapRespVO } from '@/api/eiot/firewatch/firewatch.api'
import { matchesParkEntry, normalizePropertyPoints } from './scene-data'
import { loadPropertyMapConfig, type PropertyMapConfig, type PropertyMapSource } from './property-map-config'
import { propertyId } from './property-coordinate'
import { propertyMapRefreshFailure } from './property-map-refresh'
import { createPropertyMapAppliedTracker } from './property-map-applied'
import { subscribePropertyMapChanges } from './property-map-events'
import { getAccessToken, getTenantId } from '@/utils/auth'

export type PropertyMapState = 'loading' | 'ready' | 'empty' | 'stale' | 'error' | 'forbidden' | 'config-error' | 'source-unknown'
export type PropertySnapshot = { snapshotId: string; fetchedAt: number; points: ReturnType<typeof normalizePropertyPoints>['points']; rawRows: PropertyMapSource[]; excluded: Record<string, number>; excludedIds: Record<string, string[]>; entryId: string | null }

function sourceRows(value: FirePropertyMapRespVO[]): PropertyMapSource[] {
  if (!Array.isArray(value)) throw new Error('property list response is not an array')
  return value
}

/** Owns the authoritative, complete property snapshot. It intentionally never falls back to dashboard/page data. */
export function usePropertyMapData(visible: Ref<boolean>) {
  const state = ref<PropertyMapState>('loading')
  const config = ref<PropertyMapConfig | null>(null)
  const snapshot = ref<PropertySnapshot | null>(null)
  const errorMessage = ref('')
  const loading = ref(false)
  const clock = ref(Date.now())
  let requestId = 0
  let generation = 0
  let interval: number | null = null
  let intervalDelay = 0
  let clockInterval: number | null = null
  let debounce: number | null = null
  let queued = false
  let disposed = false
  let sessionIdentity = `${getAccessToken() ?? ''}|${getTenantId() ?? ''}`
  const appliedTracker = createPropertyMapAppliedTracker()

  const points = computed(() => snapshot.value?.points ?? [])
  const snapshotId = computed(() => snapshot.value?.snapshotId ?? `property-${generation}-${requestId}`)
  const stale = computed(() => !snapshot.value || clock.value - snapshot.value.fetchedAt > (config.value?.staleAfterMs ?? 120_000))

  const clearAppliedWaiters = () => {
    appliedTracker.clear()
  }
  const invalidate = () => {
    generation += 1
    requestId += 1
    queued = false
    snapshot.value = null
    state.value = 'loading'
    clearAppliedWaiters()
  }
  const checkSessionChange = () => {
    const current = `${getAccessToken() ?? ''}|${getTenantId() ?? ''}`
    if (current === sessionIdentity) return false
    sessionIdentity = current
    invalidate()
    return true
  }
  const fetchConfig = async () => {
    try {
      const next = await loadPropertyMapConfig()
      if (config.value && JSON.stringify(config.value) !== JSON.stringify(next)) invalidate()
      config.value = next
      startInterval()
      return next
    } catch (error) {
      state.value = 'config-error'; errorMessage.value = error instanceof Error ? error.message : '物业地图配置无效'; return null
    }
  }
  const refresh = async (reason = 'poll'): Promise<void> => {
    if (disposed || !visible.value) return
    checkSessionChange()
    if (loading.value) { queued = true; return }
    loading.value = true
    let ownGeneration = generation
    let ownRequestId = requestId
    try {
      const activeConfig = await fetchConfig()
      if (!activeConfig || disposed || !visible.value) return
      ownGeneration = generation
      ownRequestId = ++requestId
      if (!snapshot.value) state.value = 'loading'
      const rows = sourceRows(await getPropertyListForDutyScreen(1, activeConfig.requestTimeoutMs))
      if (checkSessionChange() || disposed || ownGeneration !== generation || ownRequestId !== requestId) return
      const normalized = normalizePropertyPoints(rows, activeConfig)
      clock.value = Date.now()
      snapshot.value = { snapshotId: `property-${ownGeneration}-${ownRequestId}`, fetchedAt: clock.value, rawRows: rows, points: normalized.points, excluded: normalized.excluded, excludedIds: normalized.excludedIds, entryId: normalized.entryId }
      errorMessage.value = ''
      state.value = rows.length === 0 ? 'empty' : normalized.excluded['coordinate-source-unknown'] ? 'source-unknown' : normalized.points.length ? 'ready' : 'empty'
      if (reason === 'manual') console.info('[property-map] snapshot refreshed', { received: rows.length, rendered: normalized.points.length })
    } catch (error) {
      if (checkSessionChange() || disposed || ownGeneration !== generation || ownRequestId !== requestId) return
      const failure = propertyMapRefreshFailure(error, Boolean(snapshot.value))
      if (failure.clearSnapshot) { generation += 1; requestId += 1; queued = false; snapshot.value = null; clearAppliedWaiters() }
      state.value = failure.state; errorMessage.value = failure.message
    } finally {
      loading.value = false
      if (queued) { queued = false; void refresh('queued') }
    }
  }
  const scheduleRefresh = (reason = 'focus') => {
    if (disposed || !visible.value) return
    if (debounce !== null) window.clearTimeout(debounce)
    debounce = window.setTimeout(() => { debounce = null; void refresh(reason) }, 500)
  }
  const startInterval = () => {
    if (!visible.value || document.visibilityState !== 'visible') return
    const delay = config.value?.refreshIntervalMs ?? 30_000
    if (interval !== null && intervalDelay === delay) return
    stopInterval()
    intervalDelay = delay
    interval = window.setInterval(() => void refresh('poll'), delay)
    clockInterval = window.setInterval(() => {
      clock.value = Date.now()
      if (checkSessionChange()) scheduleRefresh('session')
      if (snapshot.value && clock.value - snapshot.value.fetchedAt > (config.value?.staleAfterMs ?? 120_000) && state.value === 'ready') state.value = 'stale'
    }, 1_000)
  }
  const stopInterval = () => {
    if (interval !== null) window.clearInterval(interval)
    if (clockInterval !== null) window.clearInterval(clockInterval)
    interval = null; clockInterval = null
    intervalDelay = 0
  }
  const onVisibility = () => {
    clock.value = Date.now()
    checkSessionChange()
    if (document.visibilityState === 'visible' && visible.value) { startInterval(); scheduleRefresh('visibility') }
    else stopInterval()
  }
  const onFocus = () => { if (document.visibilityState === 'visible') scheduleRefresh('focus') }
  const markApplied = (appliedSnapshotId: string, renderedPropertyIds: string[]) => appliedTracker.mark(appliedSnapshotId, renderedPropertyIds)
  const waitForApplied = (wantedSnapshotId: string, pointId: string, timeoutMs = 2_000) => appliedTracker.wait(wantedSnapshotId, pointId, timeoutMs)
  const verifyParkEntry = async (pointId: string): Promise<{ ok: boolean; snapshotId: string | null }> => {
    const active = snapshot.value; const activeConfig = config.value
    if (!active || !activeConfig || Date.now() - active.fetchedAt > activeConfig.staleAfterMs || active.entryId !== pointId || !pointId.startsWith('property:')) return { ok: false, snapshotId: null }
    const id = pointId.slice('property:'.length)
    if (!propertyId(id)) return { ok: false, snapshotId: null }
    const ownGeneration = generation
    try {
      const detail = await getProperty(id, activeConfig.requestTimeoutMs)
      if (disposed || ownGeneration !== generation || !detail || propertyId(detail.id) !== id || detail.status !== 1 || !matchesParkEntry(detail, id, activeConfig)) return { ok: false, snapshotId: null }
      const checked = normalizePropertyPoints([detail], activeConfig)
      if (!checked.points.some(point => point.id === pointId && point.navigable)) return { ok: false, snapshotId: null }
      await refresh('park-verify')
      return snapshot.value?.entryId === pointId ? { ok: true, snapshotId: snapshot.value.snapshotId } : { ok: false, snapshotId: null }
    } catch { return { ok: false, snapshotId: null } }
  }
  const clearForSessionChange = () => invalidate()

  watch(visible, isVisible => {
    if (isVisible) { startInterval(); void refresh('scene-return') } else stopInterval()
  }, { immediate: true })
  document.addEventListener('visibilitychange', onVisibility)
  window.addEventListener('focus', onFocus)
  const unsubscribeChanges = subscribePropertyMapChanges(() => scheduleRefresh('saved'))
  onBeforeUnmount(() => {
    disposed = true; stopInterval(); if (debounce !== null) window.clearTimeout(debounce)
    document.removeEventListener('visibilitychange', onVisibility); window.removeEventListener('focus', onFocus); clearAppliedWaiters()
    unsubscribeChanges()
  })
  return { state, config, snapshot, points, snapshotId, stale, errorMessage, refresh, scheduleRefresh, markApplied, waitForApplied, verifyParkEntry, clearForSessionChange }
}
