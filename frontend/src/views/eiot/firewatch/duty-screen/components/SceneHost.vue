<template>
  <div class="scene-host">
    <iframe
      v-if="host.iframeMounted.value && host.state.value !== 'fallback'"
      ref="iframeElement"
      class="scene-host__frame"
      :src="host.sceneUrl(host.activeScene.value)"
      :title="host.activeScene.value === 'park' ? '安为智慧园区三维场景' : '东莞市三维地图'"
      allow="fullscreen"
    ></iframe>
    <StaticSceneFallback
      v-if="host.state.value === 'fallback'"
      :scene="host.activeScene.value"
      :reason="host.fallbackReason.value"
      @retry="host.retry"
      @back="emit('request-back')"
    />
    <div v-else-if="host.state.value === 'loading'" class="scene-host__loading" role="status">正在加载{{ host.activeScene.value === 'park' ? '智慧园区' : '东莞三维地图' }}…</div>
  </div>
</template>

<script setup lang="ts">
import { computed, toRef } from 'vue'
import StaticSceneFallback from './StaticSceneFallback.vue'
import { useSceneHost } from './useSceneHost'
import type { MapPoint, MapSelection, SceneName } from './scene-types'
import type { Viewport } from './scene-protocol'

const props = withDefaults(defineProps<{ scene: SceneName; points: MapPoint[]; snapshotId?: string; viewport?: Viewport | null; mapRelease?: string; parkRelease?: string }>(), { points: () => [], snapshotId: 'property-0-0', viewport: null })
const emit = defineEmits<{
  selection: [selection: MapSelection]
  pointClick: [pointId: string, kind: 'property' | 'device', snapshotId: string]
  pointsApplied: [snapshotId: string, renderedPropertyIds: string[], excluded: { outOfBoundary: number; noSurface: number; invalid: number }, excludedIds?: { outOfBoundary: string[]; noSurface: string[]; invalid: string[] }]
  labelsApplied: [snapshotId: string, visiblePropertyIds: string[], clusteredPropertyIds: string[], hiddenPropertyIds: string[], offscreenPropertyIds: string[]]
  pointsIssue: [snapshotId: string, message: string]
  'request-back': []
  error: [error: { scene: SceneName; reason: string; retryable: boolean; message?: string }]
}>()

const host = useSceneHost(toRef(props, 'scene'), toRef(props, 'points'), {
  mapRelease: props.mapRelease,
  parkRelease: props.parkRelease,
  snapshotId: computed(() => props.snapshotId),
  viewport: computed(() => props.viewport),
  onSelection: selection => emit('selection', selection),
  onPointClick: (pointId, kind, snapshotId) => emit('pointClick', pointId, kind, snapshotId),
  onPointsApplied: (snapshotId, renderedPropertyIds, excluded, excludedIds) => emit('pointsApplied', snapshotId, renderedPropertyIds, excluded, excludedIds),
  onLabelsApplied: (snapshotId, visiblePropertyIds, clusteredPropertyIds, hiddenPropertyIds, offscreenPropertyIds) => emit('labelsApplied', snapshotId, visiblePropertyIds, clusteredPropertyIds, hiddenPropertyIds, offscreenPropertyIds),
  onPointsIssue: (snapshotId, message) => emit('pointsIssue', snapshotId, message),
  onRequestBack: () => emit('request-back'),
  onError: error => emit('error', error)
})

// Keep the public ref stable for the duty screen controls.
const iframeElement = host.iframe

defineExpose({ zoomIn: host.zoomIn, zoomOut: host.zoomOut, resetView: host.resetView, retry: host.retry, setPoints: host.setPoints })
</script>

<style scoped>
.scene-host { position: relative; width: 100%; height: 100%; min-height: 300px; overflow: hidden; background: #06162d; }
.scene-host__frame { display: block; width: 100%; height: 100%; border: 0; background: #06162d; }
.scene-host__loading { position: absolute; inset: 0; display: grid; place-items: center; color: #a7c8ed; background: radial-gradient(circle at 50% 40%, rgba(26, 77, 133, .8), rgba(5, 16, 35, .98) 72%); }
</style>
