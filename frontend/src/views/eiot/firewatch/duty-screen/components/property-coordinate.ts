import type { CoordinateSource } from './property-map-config'

const DECIMAL = /^[+-]?(?:\d+(?:\.\d+)?|\.\d+)$/
const PI = Math.PI
const A = 6378245.0
const EE = Number('0.00669342162296594323')

export function parseDecimal(value: unknown, min: number, max: number): number | null {
  if (typeof value !== 'number' && typeof value !== 'string') return null
  const source = typeof value === 'string' ? value.trim() : value
  if (source === '' || (typeof source === 'string' && !DECIMAL.test(source))) return null
  const number = Number(source)
  return Number.isFinite(number) && number >= min && number <= max ? number : null
}

export function propertyId(value: unknown): string | null {
  if (typeof value === 'number') return Number.isSafeInteger(value) && value > 0 ? String(value) : null
  return typeof value === 'string' && /^[1-9]\d*$/.test(value.trim()) ? value.trim() : null
}

function outOfChina(lng: number, lat: number) { return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271 }
function transformLat(lng: number, lat: number) { return -100 + 2 * lng + 3 * lat + .2 * lat * lat + .1 * lng * lat + .2 * Math.sqrt(Math.abs(lng)) + (20 * Math.sin(6 * lng * PI) + 20 * Math.sin(2 * lng * PI)) * 2 / 3 + (20 * Math.sin(lat * PI) + 40 * Math.sin(lat / 3 * PI)) * 2 / 3 + (160 * Math.sin(lat / 12 * PI) + 320 * Math.sin(lat * PI / 30)) * 2 / 3 }
function transformLng(lng: number, lat: number) { return 300 + lng + 2 * lat + .1 * lng * lng + .1 * lng * lat + .1 * Math.sqrt(Math.abs(lng)) + (20 * Math.sin(6 * lng * PI) + 20 * Math.sin(2 * lng * PI)) * 2 / 3 + (20 * Math.sin(lng * PI) + 40 * Math.sin(lng / 3 * PI)) * 2 / 3 + (150 * Math.sin(lng / 12 * PI) + 300 * Math.sin(lng / 30 * PI)) * 2 / 3 }
function wgsToGcj(lng: number, lat: number) {
  if (outOfChina(lng, lat)) return { lng, lat }
  let dLat = transformLat(lng - 105, lat - 35); let dLng = transformLng(lng - 105, lat - 35)
  const radLat = lat / 180 * PI; let magic = Math.sin(radLat); magic = 1 - EE * magic * magic; const sqrtMagic = Math.sqrt(magic)
  dLat = dLat * 180 / ((A * (1 - EE)) / (magic * sqrtMagic) * PI); dLng = dLng * 180 / (A / sqrtMagic * Math.cos(radLat) * PI)
  return { lng: lng + dLng, lat: lat + dLat }
}

function gcjToWgs(lng: number, lat: number) {
  if (outOfChina(lng, lat)) return { lng, lat }
  let guessLng = lng; let guessLat = lat
  for (let index = 0; index < 12; index += 1) {
    const converted = wgsToGcj(guessLng, guessLat)
    guessLng -= converted.lng - lng; guessLat -= converted.lat - lat
  }
  return { lng: guessLng, lat: guessLat }
}

function bdToGcj(lng: number, lat: number) {
  const x = lng - .0065; const y = lat - .006
  const z = Math.sqrt(x * x + y * y) - .00002 * Math.sin(y * PI)
  const theta = Math.atan2(y, x) - .000003 * Math.cos(x * PI)
  return { lng: z * Math.cos(theta), lat: z * Math.sin(theta) }
}

export function convertToWgs84(lng: number, lat: number, source: CoordinateSource): { lng: number; lat: number } | null {
  if (source === 'UNKNOWN') return null
  if (source === 'WGS84') return { lng, lat }
  const gcj = source === 'BD09' ? bdToGcj(lng, lat) : { lng, lat }
  return gcjToWgs(gcj.lng, gcj.lat)
}
