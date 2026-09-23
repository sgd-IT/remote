import type { MapPoint } from './scene-types'
import type { CoordinateSource, PropertyMapConfig, PropertyMapSource } from './property-map-config'
import { convertToWgs84, parseDecimal, propertyId } from './property-coordinate'

function text(value: unknown, fallback: string, max = 120): string {
  const result = String(value ?? '').trim()
  return result ? result.slice(0, max) : fallback
}

export type PropertyNormalization = { points: MapPoint[]; excluded: Record<string, number>; excludedIds: Record<string, string[]>; entryId: string | null }

function increment(excluded: Record<string, number>, excludedIds: Record<string, string[]>, reason: string, id?: string) {
  excluded[reason] = (excluded[reason] ?? 0) + 1
  if (id && (excludedIds[reason]?.length ?? 0) < 100) (excludedIds[reason] ??= []).push(id)
}

function resolvedCoordinateSource(id: string, config: PropertyMapConfig): CoordinateSource {
  return config.coordinateOverrides[id] ?? config.sourceCrs
}

export function normalizePropertyPoints(rows: PropertyMapSource[], config: PropertyMapConfig): PropertyNormalization {
  const excluded: Record<string, number> = {}
  const excludedIds: Record<string, string[]> = {}
  const entries = new Map<string, MapPoint>()
  const conflictedIds = new Set<string>()
  let entryId: string | null = null
  for (const row of rows) {
    const id = propertyId(row.id)
    if (!id) { increment(excluded, excludedIds, 'invalid-id'); continue }
    if (conflictedIds.has(id)) continue
    if (row.status !== 1) { increment(excluded, excludedIds, 'disabled', id); continue }
    const name = text(row.name ?? row.propertyName, '未命名物业', 512)
    const address = text(row.address, '地址未维护', 2048)
    const longitude = parseDecimal(row.longitude, -180, 180)
    const latitude = parseDecimal(row.latitude, -90, 90)
    if (longitude === null || latitude === null) { increment(excluded, excludedIds, 'invalid-coordinate', id); continue }
    const source = resolvedCoordinateSource(id, config)
    if (source === 'UNKNOWN') { increment(excluded, excludedIds, 'coordinate-source-unknown', id); continue }
    const converted = convertToWgs84(longitude, latitude, source)
    if (!converted) { increment(excluded, excludedIds, 'invalid-coordinate', id); continue }
    const isEntry = matchesParkEntry(row, id, config)
    const point: MapPoint = {
      id: `property:${id}`,
      kind: 'property',
      name,
      address,
      lng: converted.lng,
      lat: converted.lat,
      color: isEntry ? '#ffd166' : '#43dcff',
      statusText: text(row.riskLevel, '正常', 80),
      navigable: isEntry
    }
    const prior = entries.get(point.id)
    if (prior && JSON.stringify(prior) !== JSON.stringify(point)) { entries.delete(point.id); conflictedIds.add(id); increment(excluded, excludedIds, 'duplicate-id-conflict', id); continue }
    entries.set(point.id, point)
    if (isEntry) entryId = point.id
  }
  if (entryId && !entries.has(entryId)) entryId = null
  return { points: [...entries.values()].sort((a, b) => a.id.localeCompare(b.id)), excluded, excludedIds, entryId }
}

export function matchesParkEntry(row: PropertyMapSource, id: string, config: PropertyMapConfig): boolean {
  const code = typeof row.code === 'string' ? row.code.trim() : ''
  return Boolean(config.parkPropertyId && config.parkPropertyCode && config.parkPropertyId === id && config.parkPropertyCode === code)
}

export function findPoint(points: MapPoint[], id: string): MapPoint | undefined {
  return points.find(point => point.id === id)
}
