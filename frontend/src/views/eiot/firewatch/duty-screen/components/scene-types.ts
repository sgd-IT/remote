export type SceneName = 'city' | 'park'

export type SceneHostState = 'loading' | 'ready' | 'fallback'

export type SceneFallbackReason =
  | 'timeout'
  | 'resource'
  | 'webgl'
  | 'context-lost'
  | 'runtime'
  | 'protocol'
  | 'revision'

export type MapPointKind = 'property' | 'device'

export type MapPoint = {
  id: string
  kind: MapPointKind
  name: string
  address?: string
  lng: number
  lat: number
  color: `#${string}`
  statusText?: string
  navigable: boolean
  townId?: string
}

export type MapSelection = { id: string; name: string } | null

export type SceneHostError = {
  scene: SceneName
  reason: SceneFallbackReason
  retryable: boolean
  message?: string
}

export const DEFAULT_MAP_RELEASE = 'v002-property-r7'
export const DEFAULT_PARK_RELEASE = 'v007-r001'
