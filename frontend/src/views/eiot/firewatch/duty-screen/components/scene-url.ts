import type { SceneName } from './scene-types'
import { DEFAULT_MAP_RELEASE, DEFAULT_PARK_RELEASE } from './scene-types'

const RELEASE_PATTERN = /^[A-Za-z0-9._-]+$/

export function isSafeReleaseId(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0 && value.length <= 80 && RELEASE_PATTERN.test(value) && !value.includes('..')
}

export function safeReleaseId(value: unknown, fallback: string): string {
  return isSafeReleaseId(value) ? value : fallback
}

function sceneRelease(scene: SceneName): string {
  return scene === 'park'
    ? safeReleaseId(import.meta.env.VITE_SMART_PARK_RELEASE, DEFAULT_PARK_RELEASE)
    : safeReleaseId(import.meta.env.VITE_DONGGUAN_MAP_RELEASE, DEFAULT_MAP_RELEASE)
}

export function sceneUrl(scene: SceneName, baseUrl = import.meta.env.BASE_URL): string {
  const base = baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
  const release = sceneRelease(scene)
  if (scene === 'park') {
    return `${base}smart-park/releases/${release}/index.html?embedded=1&quality=HIGH#/campus`
  }
  return `${base}dongguan-map/releases/${release}/index.html?embedded=1#/dongguan`
}

export function sceneTimeoutMs(): number {
  const configured = Number(import.meta.env.VITE_SCENE_TIMEOUT_MS)
  return Number.isFinite(configured) ? Math.min(60_000, Math.max(5_000, configured)) : 20_000
}
