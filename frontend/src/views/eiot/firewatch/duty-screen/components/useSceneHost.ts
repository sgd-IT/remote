import { nextTick, onBeforeUnmount, onMounted, ref, watch, type Ref } from 'vue'
import { commandLayout, commandPoints, commandReset, commandZoomIn, commandZoomOut, isReadyForScene, isSceneMessage, type Viewport } from './scene-protocol'
import { sceneTimeoutMs, sceneUrl, safeReleaseId } from './scene-url'
import type { MapPoint, MapSelection, SceneFallbackReason, SceneHostError, SceneHostState, SceneName } from './scene-types'
import { DEFAULT_MAP_RELEASE, DEFAULT_PARK_RELEASE } from './scene-types'

type SceneHostOptions = {
  mapRelease?: string
  parkRelease?: string
  onSelection: (selection: MapSelection) => void
  snapshotId: Ref<string>
  viewport: Ref<Viewport | null>
  onPointClick: (pointId: string, kind: 'property' | 'device', snapshotId: string) => void
  onPointsApplied: (snapshotId: string, renderedPropertyIds: string[], excluded: { outOfBoundary: number; noSurface: number; invalid: number }, excludedIds?: { outOfBoundary: string[]; noSurface: string[]; invalid: string[] }) => void
  onLabelsApplied: (snapshotId: string, visiblePropertyIds: string[], clusteredPropertyIds: string[], hiddenPropertyIds: string[], offscreenPropertyIds: string[]) => void
  onPointsIssue: (snapshotId: string, message: string) => void
  onRequestBack: () => void
  onError: (error: SceneHostError) => void
}

export function useSceneHost(scene: Ref<SceneName>, points: Ref<MapPoint[]>, options: SceneHostOptions) {
  const iframe = ref<HTMLIFrameElement | null>(null)
  const iframeMounted = ref(false)
  const activeScene = ref<SceneName>(scene.value)
  const state = ref<SceneHostState>('loading')
  const fallbackReason = ref<SceneFallbackReason>('timeout')
  const generation = ref(0)
  let timeout: number | null = null
  let pointsTimeout: number | null = null
  let pendingSnapshotId: string | null = null
  let labelsAppliedSnapshotId: string | null = null
  let currentGeneration = 0
  let mounted = false

  const mapRelease = safeReleaseId(options.mapRelease ?? import.meta.env.VITE_DONGGUAN_MAP_RELEASE, DEFAULT_MAP_RELEASE)
  const parkRelease = safeReleaseId(options.parkRelease ?? import.meta.env.VITE_SMART_PARK_RELEASE, DEFAULT_PARK_RELEASE)
  const expectedParkRevision = parkRelease

  const clearTimeoutHandle = () => {
    if (timeout !== null) window.clearTimeout(timeout)
    timeout = null
  }
  const clearPointsTimeout = () => {
    if (pointsTimeout !== null) window.clearTimeout(pointsTimeout)
    pointsTimeout = null
    pendingSnapshotId = null
  }

  const post = (message: Parameters<Window['postMessage']>[0]) => {
    if (state.value !== 'ready' || !iframe.value?.contentWindow) return false
    iframe.value.contentWindow.postMessage(message, window.location.origin)
    return true
  }
  const postMapSnapshot = () => {
    if (activeScene.value !== 'city') return false
    const layout = options.viewport.value
    if (layout) post(commandLayout(layout))
    const snapshotId = options.snapshotId.value
    const sent = post(commandPoints(snapshotId, points.value))
    if (sent && pendingSnapshotId !== snapshotId) {
      clearPointsTimeout()
      pendingSnapshotId = snapshotId
      labelsAppliedSnapshotId = null
      pointsTimeout = window.setTimeout(() => {
        if (pendingSnapshotId !== snapshotId) return
        pointsTimeout = null
        options.onPointsIssue(snapshotId, '地图未在 8 秒内确认物业快照，请检查地图资源与点位协议')
      }, 8_000)
    }
    return sent
  }

  const fail = (reason: SceneFallbackReason, message?: string, retryable = true) => {
    state.value = 'fallback'
    fallbackReason.value = reason
    clearTimeoutHandle()
    options.onError({ scene: activeScene.value, reason, retryable, message })
  }

  const destroyScene = () => {
    clearTimeoutHandle()
    clearPointsTimeout()
    labelsAppliedSnapshotId = null
    currentGeneration += 1
    generation.value = currentGeneration
    state.value = 'loading'
    iframeMounted.value = false
    iframe.value = null
  }

  const mountScene = async () => {
    destroyScene()
    const localGeneration = currentGeneration
    activeScene.value = scene.value
    state.value = 'loading'
    await nextTick()
    if (!mounted || localGeneration !== currentGeneration) return
    iframeMounted.value = true
    await nextTick()
    if (!mounted || localGeneration !== currentGeneration) return
    timeout = window.setTimeout(() => {
      if (localGeneration === currentGeneration && state.value === 'loading') fail('timeout')
    }, sceneTimeoutMs())
  }

  const onMessage = (event: MessageEvent<unknown>) => {
    if (!mounted || event.origin !== window.location.origin || event.source !== iframe.value?.contentWindow || !isSceneMessage(event.data)) return
    const message = event.data
    if (message.type === 'dongguan-map-error' || message.type === 'smart-park-error') {
      fail(message.code, message.message, message.retryable)
      return
    }
    if (message.type === 'smart-park-ready' && message.revision !== expectedParkRevision) {
      fail('revision', `园区版本不匹配：${message.revision}` , false)
      return
    }
    if (isReadyForScene(activeScene.value, message, expectedParkRevision)) {
      state.value = 'ready'
      clearTimeoutHandle()
      if (activeScene.value === 'city') postMapSnapshot()
      return
    }
    if (activeScene.value === 'city' && message.type === 'dongguan-map-selection') options.onSelection(message.selection)
    if (activeScene.value === 'city' && message.type === 'dongguan-map-points-applied' && message.snapshotId === options.snapshotId.value) {
      if (pointsTimeout !== null) window.clearTimeout(pointsTimeout)
      pointsTimeout = null
      options.onPointsApplied(message.snapshotId, message.renderedPropertyIds, message.excluded, message.excludedIds)
      if (labelsAppliedSnapshotId !== message.snapshotId) pointsTimeout = window.setTimeout(() => {
        if (pendingSnapshotId === message.snapshotId) options.onPointsIssue(message.snapshotId, '地图已定位物业，但未在 8 秒内确认文字卡片布局')
      }, 8_000)
    }
    if (activeScene.value === 'city' && message.type === 'dongguan-map-labels-applied' && message.snapshotId === options.snapshotId.value) {
      labelsAppliedSnapshotId = message.snapshotId
      clearPointsTimeout()
      options.onLabelsApplied(message.snapshotId, message.visiblePropertyIds, message.clusteredPropertyIds, message.hiddenPropertyIds, message.offscreenPropertyIds)
    }
    if (activeScene.value === 'city' && message.type === 'dongguan-map-points-rejected' && message.snapshotId === options.snapshotId.value) { clearPointsTimeout(); options.onPointsIssue(message.snapshotId, message.reason) }
    if (activeScene.value === 'city' && message.type === 'dongguan-map-point-click' && message.snapshotId === options.snapshotId.value) options.onPointClick(message.pointId, message.kind, message.snapshotId)
    if (activeScene.value === 'park' && message.type === 'smart-park-request-back') options.onRequestBack()
  }

  const retry = () => { void mountScene() }
  const zoomIn = () => { post(commandZoomIn()) }
  const zoomOut = () => { post(commandZoomOut()) }
  const resetView = () => { post(commandReset()) }
  const setPoints = (value: MapPoint[]) => { if (activeScene.value === 'city') post(commandPoints(options.snapshotId.value, value)) }

  onMounted(() => {
    mounted = true
    window.addEventListener('message', onMessage)
    void mountScene()
  })
  watch(scene, () => { if (mounted) void mountScene() })
  watch([points, options.snapshotId, options.viewport], () => { if (activeScene.value === 'city' && state.value === 'ready') postMapSnapshot() }, { deep: true })
  onBeforeUnmount(() => {
    mounted = false
    window.removeEventListener('message', onMessage)
    destroyScene()
  })

  return { iframe, iframeMounted, activeScene, state, fallbackReason, sceneUrl, mapRelease, parkRelease, retry, zoomIn, zoomOut, resetView, setPoints }
}
