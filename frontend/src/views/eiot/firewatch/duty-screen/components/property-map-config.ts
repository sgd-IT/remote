export type CoordinateSource = 'WGS84' | 'GCJ02' | 'BD09' | 'UNKNOWN'
export type PropertyMapSource = {
  id: number | string
  code?: string | null
  name?: string | null
  propertyName?: string | null
  address?: string | null
  longitude?: number | string | null
  latitude?: number | string | null
  status: number
  riskLevel?: string | null
  color?: string | null
}
export type PropertyMapConfig = {
  schemaVersion: 1
  sourceCrs: CoordinateSource
  coordinateOverrides: Record<string, CoordinateSource>
  parkPropertyId: string | null
  parkPropertyCode: string | null
  refreshIntervalMs: number
  staleAfterMs: number
  requestTimeoutMs: number
}

const SOURCES: CoordinateSource[] = ['WGS84', 'GCJ02', 'BD09', 'UNKNOWN']
export const DEFAULT_PROPERTY_MAP_CONFIG: PropertyMapConfig = Object.freeze({ schemaVersion: 1, sourceCrs: 'UNKNOWN', coordinateOverrides: {}, parkPropertyId: null, parkPropertyCode: null, refreshIntervalMs: 30_000, staleAfterMs: 120_000, requestTimeoutMs: 10_000 })

const isId = (value: unknown): value is string => typeof value === 'string' && /^[1-9]\d*$/.test(value)
const isSource = (value: unknown): value is CoordinateSource => typeof value === 'string' && SOURCES.includes(value as CoordinateSource)
const bounded = (value: unknown, min: number, max: number): value is number => typeof value === 'number' && Number.isInteger(value) && value >= min && value <= max

export function parsePropertyMapConfig(value: unknown): PropertyMapConfig | null {
  if (!value || typeof value !== 'object') return null
  const config = value as Record<string, unknown>
  if (config.schemaVersion !== 1 || !isSource(config.sourceCrs) || !bounded(config.refreshIntervalMs, 10_000, 300_000) || !bounded(config.staleAfterMs, config.refreshIntervalMs, 600_000) || !bounded(config.requestTimeoutMs, 3_000, 30_000)) return null
  if (config.parkPropertyId !== null && !isId(config.parkPropertyId)) return null
  if (config.parkPropertyCode !== null && (typeof config.parkPropertyCode !== 'string' || config.parkPropertyCode.length > 120)) return null
  if (!config.coordinateOverrides || typeof config.coordinateOverrides !== 'object' || Array.isArray(config.coordinateOverrides)) return null
  const coordinateOverrides: Record<string, CoordinateSource> = {}
  for (const [id, source] of Object.entries(config.coordinateOverrides as Record<string, unknown>)) {
    if (!isId(id) || !isSource(source)) return null
    coordinateOverrides[id] = source
  }
  return { schemaVersion: 1, sourceCrs: config.sourceCrs, coordinateOverrides, parkPropertyId: config.parkPropertyId as string | null, parkPropertyCode: config.parkPropertyCode as string | null, refreshIntervalMs: config.refreshIntervalMs, staleAfterMs: config.staleAfterMs, requestTimeoutMs: config.requestTimeoutMs }
}

export async function loadPropertyMapConfig(fetcher: typeof fetch = fetch, baseUrl = import.meta.env.BASE_URL): Promise<PropertyMapConfig> {
  const base = baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
  const response = await fetcher(`${base}config/property-map.json`, { cache: 'no-cache', credentials: 'same-origin' })
  if (!response.ok) throw new Error(`property map configuration request failed: ${response.status}`)
  const config = parsePropertyMapConfig(await response.json())
  if (!config) throw new Error('property map configuration is invalid')
  return config
}
