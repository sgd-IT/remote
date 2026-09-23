import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'
import ts from 'typescript'

const source = await readFile(new URL('../src/views/eiot/firewatch/duty-screen/components/usePropertyMapData.ts', import.meta.url), 'utf8')
const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText

const listeners = new Map()
const intervals = new Map()
const timeouts = new Map()
let nextTimer = 1
let now = 1_000
let rows = [{ id: 1, status: 1, longitude: 113.7, latitude: 23, name: '第一物业' }]
let fetches = 0
let configFetches = 0
let failConfig = true
let accessToken = 'session-a'
let tenantId = 'tenant-a'
let changeListener = null
let unmount = null
let visibleWatcher = null
const visible = { value: true }
const config = { schemaVersion: 1, sourceCrs: 'WGS84', coordinateOverrides: {}, parkPropertyId: null, parkPropertyCode: null, refreshIntervalMs: 30_000, staleAfterMs: 120_000, requestTimeoutMs: 10_000 }
const add = (type, listener) => listeners.set(type, listener)
const remove = (type) => listeners.delete(type)
const windowMock = {
  setInterval: (fn, delay) => { const id = nextTimer++; intervals.set(id, { fn, delay }); return id },
  clearInterval: (id) => intervals.delete(id),
  setTimeout: (fn, delay) => { const id = nextTimer++; timeouts.set(id, { fn, delay }); return id },
  clearTimeout: (id) => timeouts.delete(id),
  addEventListener: add,
  removeEventListener: remove,
}
const documentMock = { visibilityState: 'visible', addEventListener: add, removeEventListener: remove }
const modules = {
  vue: {
    ref: (value) => ({ value }),
    computed: (getter) => ({ get value() { return getter() } }),
    watch: (_source, fn, options) => { visibleWatcher = fn; if (options?.immediate) fn(visible.value) },
    onBeforeUnmount: (fn) => { unmount = fn },
  },
  '@/api/eiot/firewatch/firewatch.api': {
    getPropertyListForDutyScreen: async () => { fetches++; return rows },
    getProperty: async () => null,
  },
  './scene-data': {
    normalizePropertyPoints: (items) => ({ points: items.filter((item) => item.status === 1 && item.longitude != null && item.latitude != null).map((item) => ({ id: `property:${item.id}`, kind: 'property', name: item.name, lng: item.longitude, lat: item.latitude, navigable: false })), excluded: {}, excludedIds: {}, entryId: null }),
    matchesParkEntry: () => false,
  },
  './property-map-config': { loadPropertyMapConfig: async () => { configFetches++; if (failConfig) throw new Error('temporary config failure'); return { ...config } } },
  './property-coordinate': { propertyId: (value) => String(value) },
  './property-map-refresh': { propertyMapRefreshFailure: (_error, hasSnapshot) => ({ state: hasSnapshot ? 'stale' : 'error', message: 'refresh failed', clearSnapshot: false }) },
  './property-map-applied': { createPropertyMapAppliedTracker: () => ({ clear() {}, mark() {}, wait: async () => false }) },
  './property-map-events': { subscribePropertyMapChanges: (fn) => { changeListener = fn; return () => { changeListener = null } } },
  '@/utils/auth': { getAccessToken: () => accessToken, getTenantId: () => tenantId },
}
const module = { exports: {} }
vm.runInNewContext(js, { module, exports: module.exports, require: (id) => {
  if (!(id in modules)) throw new Error(`Unexpected dependency: ${id}`)
  return modules[id]
}, window: windowMock, document: documentMock, Date: class extends Date { static now() { return now } }, console, setTimeout, clearTimeout })
const propertyMap = module.exports.usePropertyMapData(visible)
const settle = async () => { for (let index = 0; index < 8; index++) await Promise.resolve() }
const fireTimeout = (delay) => {
  const entry = [...timeouts].find(([, timer]) => timer.delay === delay)
  assert.ok(entry, `Missing ${delay} ms timeout`)
  timeouts.delete(entry[0]); entry[1].fn()
}

await settle()
assert.equal(fetches, 0, 'invalid initial config blocks property requests')
assert.equal(propertyMap.state.value, 'config-error')
assert.equal([...intervals.values()].filter((timer) => timer.delay === 30_000).length, 1, 'config failure does not stop polling')
failConfig = false
;[...intervals.values()].find((timer) => timer.delay === 30_000).fn()
await settle()
assert.equal(fetches, 1, 'initial property request')
assert.equal(propertyMap.state.value, 'ready')
assert.equal([...intervals.values()].filter((timer) => timer.delay === 30_000).length, 1, 'polling starts before asynchronous config completes')
assert.equal(propertyMap.points.value[0].lng, 113.7)

rows = [{ ...rows[0], longitude: 113.8 }]
;[...intervals.values()].find((timer) => timer.delay === 30_000).fn()
await settle()
assert.equal(fetches, 2)
assert.equal(propertyMap.points.value[0].lng, 113.8, 'same ID moves on the next poll')
assert.ok(configFetches >= 2, 'active config is reloaded')

rows = []
changeListener()
fireTimeout(500)
await settle()
assert.equal(propertyMap.state.value, 'empty', 'save notification removes deleted properties')
assert.equal(propertyMap.points.value.length, 0)

rows = [{ id: 2, status: 1, longitude: 113.9, latitude: 23.1, name: '第二物业' }]
accessToken = 'session-b'; tenantId = 'tenant-b'
;[...intervals.values()].find((timer) => timer.delay === 1_000).fn()
assert.equal(propertyMap.snapshot.value, null, 'session change clears the old snapshot immediately')
fireTimeout(500)
await settle()
assert.equal(propertyMap.points.value[0].id, 'property:2')

now += 121_000
;[...intervals.values()].find((timer) => timer.delay === 1_000).fn()
assert.equal(propertyMap.stale.value, true)
assert.equal(propertyMap.state.value, 'stale')

visible.value = false; visibleWatcher(false)
assert.equal(intervals.size, 0, 'hidden city releases both timers')
visible.value = true; visibleWatcher(true)
await settle()
assert.equal([...intervals.values()].filter((timer) => timer.delay === 30_000).length, 1)
unmount()
assert.equal(intervals.size, 0)
assert.equal(changeListener, null)
console.log(JSON.stringify({ suite: 'property-map-lifecycle', status: 'PASS', fetches, configFetches }))
