import type { MapPoint, MapSelection, SceneName } from './scene-types'

export type Viewport = { width: number; height: number; safeRect: { left: number; top: number; right: number; bottom: number } }
export type MapCommand =
  | { type: 'dongguan-map-command'; command: 'zoomIn' | 'zoomOut' | 'reset' }
  | { type: 'dongguan-map-command'; command: 'focus' | 'select'; townId: string }
  | { type: 'dongguan-map-command'; command: 'points'; protocolVersion: '2'; snapshotId: string; points: MapPoint[] }
  | { type: 'dongguan-map-command'; command: 'layout'; protocolVersion: '2'; viewport: Viewport }

export type SceneReadyMessage =
  | { type: 'dongguan-map-ready'; protocolVersion: '2'; capabilities: string[] }
  | { type: 'smart-park-ready'; revision: string }
export type SceneErrorMessage = { type: 'dongguan-map-error' | 'smart-park-error'; code: 'webgl' | 'resource' | 'context-lost' | 'runtime'; retryable: boolean; message?: string }
export type SceneInboundMessage =
  | SceneReadyMessage
  | SceneErrorMessage
  | { type: 'dongguan-map-selection'; selection: MapSelection }
  | { type: 'dongguan-map-points-applied'; protocolVersion: '2'; snapshotId: string; renderedPropertyIds: string[]; excluded: { outOfBoundary: number; noSurface: number; invalid: number }; excludedIds?: { outOfBoundary: string[]; noSurface: string[]; invalid: string[] } }
  | { type: 'dongguan-map-labels-applied'; protocolVersion: '2'; snapshotId: string; visiblePropertyIds: string[]; clusteredPropertyIds: string[]; hiddenPropertyIds: string[]; offscreenPropertyIds: string[] }
  | { type: 'dongguan-map-points-rejected'; protocolVersion: '2'; snapshotId: string; reason: string }
  | { type: 'dongguan-map-point-click'; protocolVersion: '2'; snapshotId: string; pointId: string; kind: 'property' | 'device' }
  | { type: 'smart-park-request-back' }

const MAX_TEXT = 2_048
const isRecord = (value: unknown): value is Record<string, unknown> => Boolean(value) && typeof value === 'object' && !Array.isArray(value)
const isText = (value: unknown, max = MAX_TEXT): value is string => typeof value === 'string' && value.length > 0 && value.length <= max
const isFiniteCount = (value: unknown): value is number => typeof value === 'number' && Number.isSafeInteger(value) && value >= 0 && value <= 10_000
const isIdList = (value: unknown): value is string[] => Array.isArray(value) && value.length <= 10_000 && value.every(item => isText(item, 120))
const isSelection = (value: unknown): value is MapSelection => value === null || (isRecord(value) && isText(value.id, 80) && isText(value.name, MAX_TEXT))

export function isSceneMessage(value: unknown): value is SceneInboundMessage {
  if (!isRecord(value) || !isText(value.type, 80)) return false
  switch (value.type) {
    case 'dongguan-map-ready': return value.protocolVersion === '2' && Array.isArray(value.capabilities) && value.capabilities.length <= 20 && value.capabilities.every(item => isText(item, 80))
    case 'smart-park-ready': return isText(value.revision, 80)
    case 'dongguan-map-error':
    case 'smart-park-error': return (value.code === 'webgl' || value.code === 'resource' || value.code === 'context-lost' || value.code === 'runtime') && typeof value.retryable === 'boolean' && (value.message === undefined || isText(value.message))
    case 'dongguan-map-selection': return isSelection(value.selection)
    case 'dongguan-map-points-applied': return value.protocolVersion === '2' && isText(value.snapshotId, 80) && isIdList(value.renderedPropertyIds) && isRecord(value.excluded) && isFiniteCount(value.excluded.outOfBoundary) && isFiniteCount(value.excluded.noSurface) && isFiniteCount(value.excluded.invalid) && (value.excludedIds === undefined || (isRecord(value.excludedIds) && isIdList(value.excludedIds.outOfBoundary) && isIdList(value.excludedIds.noSurface) && isIdList(value.excludedIds.invalid)))
    case 'dongguan-map-labels-applied': return value.protocolVersion === '2' && isText(value.snapshotId, 80) && isIdList(value.visiblePropertyIds) && isIdList(value.clusteredPropertyIds) && isIdList(value.hiddenPropertyIds) && isIdList(value.offscreenPropertyIds)
    case 'dongguan-map-points-rejected': return value.protocolVersion === '2' && isText(value.snapshotId, 80) && isText(value.reason, 160)
    case 'dongguan-map-point-click': return value.protocolVersion === '2' && isText(value.snapshotId, 80) && isText(value.pointId, 120) && (value.kind === 'property' || value.kind === 'device')
    case 'smart-park-request-back': return true
    default: return false
  }
}

export function isReadyForScene(scene: SceneName, message: SceneInboundMessage, expectedRevision: string): boolean {
  return scene === 'city' ? message.type === 'dongguan-map-ready' && message.capabilities.includes('property-labels') && message.capabilities.includes('layout-safe-rect') && message.capabilities.includes('snapshot-report') : message.type === 'smart-park-ready' && message.revision === expectedRevision
}
export const commandZoomIn = (): MapCommand => ({ type: 'dongguan-map-command', command: 'zoomIn' })
export const commandZoomOut = (): MapCommand => ({ type: 'dongguan-map-command', command: 'zoomOut' })
export const commandReset = (): MapCommand => ({ type: 'dongguan-map-command', command: 'reset' })
// postMessage cannot clone Vue's reactive proxies. Copy only protocol fields at this boundary.
export const commandPoints = (snapshotId: string, points: MapPoint[]): MapCommand => ({
  type: 'dongguan-map-command', command: 'points', protocolVersion: '2', snapshotId,
  points: points.map(point => ({
    id: point.id, kind: point.kind, name: point.name, address: point.address,
    lng: point.lng, lat: point.lat, color: point.color, statusText: point.statusText,
    navigable: point.navigable, townId: point.townId
  }))
})
export const commandLayout = (viewport: Viewport): MapCommand => ({
  type: 'dongguan-map-command', command: 'layout', protocolVersion: '2',
  viewport: {
    width: viewport.width, height: viewport.height,
    safeRect: { left: viewport.safeRect.left, top: viewport.safeRect.top, right: viewport.safeRect.right, bottom: viewport.safeRect.bottom }
  }
})
