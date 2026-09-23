import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import ts from 'typescript'

const root = resolve(fileURLToPath(new URL('..', import.meta.url)))
const cache = new Map()
async function loadTs(relative) {
  const filename = resolve(root, relative)
  if (cache.has(filename)) return cache.get(filename)
  const source = (await readFile(filename, 'utf8')).replaceAll('import.meta.env.BASE_URL', '"/"')
  const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText
  const module = { exports: {} }; cache.set(filename, module.exports)
  const localRequire = (request) => {
    if (request.startsWith('.')) {
      const target = resolve(dirname(filename), request.endsWith('.ts') ? request : `${request}.ts`)
      const relativeTarget = target.slice(root.length + 1).replaceAll('\\', '/')
      if (!cache.has(target)) throw new Error(`async module dependency must be preloaded: ${relativeTarget}`)
      return cache.get(target)
    }
    throw new Error(`unexpected runtime dependency: ${request}`)
  }
  vm.runInNewContext(output, { module, exports: module.exports, require: localRequire, console, Math, Number, String, Object, JSON, RegExp, Set, Map, Error, Date, Promise, setTimeout, clearTimeout })
  cache.set(filename, module.exports)
  return module.exports
}

// Preload actual modules in dependency order; no shadow implementation is used.
await loadTs('src/views/eiot/firewatch/duty-screen/components/property-coordinate.ts')
await loadTs('src/views/eiot/firewatch/duty-screen/components/scene-types.ts')
await loadTs('src/views/eiot/firewatch/duty-screen/components/property-map-refresh.ts')
await loadTs('src/views/eiot/firewatch/duty-screen/components/property-map-applied.ts')
const coordinate = await loadTs('src/views/eiot/firewatch/duty-screen/components/property-coordinate.ts')
const sceneData = await loadTs('src/views/eiot/firewatch/duty-screen/components/scene-data.ts')
const protocol = await loadTs('src/views/eiot/firewatch/duty-screen/components/scene-protocol.ts')
const refresh = await loadTs('src/views/eiot/firewatch/duty-screen/components/property-map-refresh.ts')
const applied = await loadTs('src/views/eiot/firewatch/duty-screen/components/property-map-applied.ts')

const results = []
const pending = []
const test = (id, run) => { try { run(); results.push({ id, status: 'PASS' }) } catch (error) { results.push({ id, status: 'FAIL', message: error instanceof Error ? error.message : String(error) }) } }
const testAsync = (id, run) => pending.push(Promise.resolve().then(run).then(() => results.push({ id, status: 'PASS' }), error => results.push({ id, status: 'FAIL', message: error instanceof Error ? error.message : String(error) })))
const config = { schemaVersion: 1, sourceCrs: 'WGS84', coordinateOverrides: {}, parkPropertyId: '9001', parkPropertyCode: 'park-real-code', refreshIntervalMs: 30000, staleAfterMs: 120000, requestTimeoutMs: 10000 }
const row = (id, longitude = 113.75, latitude = 23.02, extra = {}) => ({ id, code: `code-${id}`, name: `fixture-${id}`, address: `fixture address ${id}`, longitude, latitude, status: 1, ...extra })

test('UT-001', () => {
  assert.equal(coordinate.parseDecimal('113.5', -180, 180), 113.5); assert.equal(coordinate.parseDecimal('', -180, 180), null); assert.equal(coordinate.parseDecimal(null, -180, 180), null); assert.equal(coordinate.parseDecimal('113abc', -180, 180), null); assert.equal(coordinate.parseDecimal(181, -180, 180), null); assert.equal(coordinate.parseDecimal('23.1', -90, 90), 23.1)
})
test('UT-002', () => {
  const gcj = coordinate.convertToWgs84(116.41024449916938, 39.91640428150164, 'GCJ02'); const bd = coordinate.convertToWgs84(116.41662724378733, 39.922699552216216, 'BD09')
  assert.ok(gcj && Math.abs(gcj.lng - 116.404) < 1e-6 && Math.abs(gcj.lat - 39.915) < 1e-6); assert.ok(bd && Math.abs(bd.lng - 116.4039209645) < 1e-6 && Math.abs(bd.lat - 39.9151996376) < 1e-6); assert.equal(coordinate.convertToWgs84(113, 23, 'UNKNOWN'), null)
})
test('UT-003', () => {
  const normal = sceneData.normalizePropertyPoints([row('1'), row('2')], config); assert.deepEqual([...normal.points.map((point) => point.id)], ['property:1', 'property:2'])
  assert.ok(normal.points.every(point => point.kind === 'property' && point.color === '#43dcff'))
  const sameCoordinate = sceneData.normalizePropertyPoints([row('3'), row('4')], config); assert.equal(sameCoordinate.points.length, 2)
  const conflict = sceneData.normalizePropertyPoints([row('5'), row('5', 113.8, 23.1)], config); assert.equal(conflict.points.length, 0); assert.equal(conflict.excluded['duplicate-id-conflict'], 1)
  const repeatedConflict = sceneData.normalizePropertyPoints([row('5'), row('5', 113.8, 23.1), row('5')], config); assert.equal(repeatedConflict.points.length, 0); assert.deepEqual([...repeatedConflict.excludedIds['duplicate-id-conflict']], ['5'])
})
test('UT-004', () => {
  const park = row('9001', 113.75, 23.02, { code: 'park-real-code' }); assert.equal(sceneData.matchesParkEntry(park, '9001', config), true); assert.equal(sceneData.matchesParkEntry({ ...park, code: 'other' }, '9001', config), false); assert.equal(sceneData.matchesParkEntry(park, '9002', config), false)
  assert.equal(sceneData.normalizePropertyPoints([park], config).points[0].color, '#ffd166')
  assert.equal(sceneData.matchesParkEntry(park, '9001', { ...config, parkPropertyCode: null }), false)
  assert.equal(sceneData.matchesParkEntry(park, '9001', { ...config, parkPropertyId: null }), false)
})
test('IT-001', () => {
  const rows = Array.from({ length: 500 }, (_, index) => row(String(index + 1), 113.60 + index / 10000, 22.85 + index / 10000)); const snapshot = sceneData.normalizePropertyPoints(rows, { ...config, parkPropertyId: null, parkPropertyCode: null }); assert.equal(snapshot.points.length, 500); assert.equal(sceneData.normalizePropertyPoints([], config).points.length, 0); assert.equal(snapshot.points.some((point) => point.id === 'property:101'), true)
})
test('IT-002', () => {
  const before = sceneData.normalizePropertyPoints([row('42', 113.70, 23.00)], config).points
  const updated = sceneData.normalizePropertyPoints([row('42', 113.80, 23.10)], config).points
  const removed = sceneData.normalizePropertyPoints([], config).points
  assert.equal(before.length, 1); assert.equal(updated.length, 1); assert.notDeepEqual(updated[0], before[0]); assert.equal(removed.length, 0)
})
test('IT-003', () => {
  assert.equal(protocol.isSceneMessage({ type: 'dongguan-map-ready', protocolVersion: '2', capabilities: ['property-labels', 'layout-safe-rect', 'snapshot-report'] }), true)
  assert.equal(protocol.isSceneMessage({ type: 'dongguan-map-ready', protocolVersion: '1' }), false)
  assert.equal(protocol.isSceneMessage({ type: 'dongguan-map-point-click', protocolVersion: '2', snapshotId: 'property-1-1', pointId: 'property:1', kind: 'property' }), true)
  assert.equal(protocol.isSceneMessage({ type: 'dongguan-map-point-click', protocolVersion: '2', snapshotId: 'x', pointId: 'property:1', kind: 'park-entry' }), false)
  assert.equal(protocol.isSceneMessage({ type: 'dongguan-map-points-applied', protocolVersion: '2', snapshotId: 'x', renderedPropertyIds: ['property:1'], excluded: { outOfBoundary: 0, noSurface: 0, invalid: 0 } }), true)
  assert.equal(protocol.isSceneMessage({ type: 'dongguan-map-points-rejected', protocolVersion: '2', snapshotId: 'x', reason: 'invalid points' }), true)
  assert.equal(protocol.isSceneMessage({ type: 'dongguan-map-labels-applied', protocolVersion: '2', snapshotId: 'x', visiblePropertyIds: ['property:1'], clusteredPropertyIds: [], hiddenPropertyIds: [], offscreenPropertyIds: [] }), true)
})
test('IT-006', () => {
  const proxyPoint = new Proxy({ id: 'property:1', kind: 'property', name: 'fixture', address: 'address', lng: 113.7, lat: 23, color: '#43dcff', navigable: false }, {})
  const proxyPoints = new Proxy([proxyPoint], {})
  const proxyViewport = new Proxy({ width: 1600, height: 900, safeRect: new Proxy({ left: 100, top: 100, right: 1500, bottom: 800 }, {}) }, {})
  assert.throws(() => structuredClone(proxyPoints), /could not be cloned/i)
  const pointsCommand = structuredClone(protocol.commandPoints('property-1-1', proxyPoints))
  const layoutCommand = structuredClone(protocol.commandLayout(proxyViewport))
  assert.equal(pointsCommand.points[0].id, 'property:1')
  assert.equal(layoutCommand.viewport.safeRect.right, 1500)
})
test('IT-004', () => {
  const stale = refresh.propertyMapRefreshFailure(new Error('network'), true); assert.equal(stale.state, 'stale'); assert.equal(stale.clearSnapshot, false)
  const initial = refresh.propertyMapRefreshFailure(new Error('network'), false); assert.equal(initial.state, 'error'); assert.equal(initial.clearSnapshot, false)
  const denied = refresh.propertyMapRefreshFailure({ response: { status: 403 } }, true); assert.equal(denied.state, 'forbidden'); assert.equal(denied.clearSnapshot, true)
})
test('SEC-001', () => {
  const unsafe = sceneData.normalizePropertyPoints([row('77', 113.7, 23, { name: '<img src=x onerror=1>', address: '<script>alert(1)</script>' })], config).points[0]; assert.equal(unsafe.name, '<img src=x onerror=1>'); assert.equal(unsafe.address, '<script>alert(1)</script>')
})

testAsync('IT-005', async () => {
  const tracker = applied.createPropertyMapAppliedTracker(1000, 4)
  tracker.mark('property-early', ['property:9001'])
  assert.equal(await tracker.wait('property-early', 'property:9001', 10), true)
  assert.equal(await tracker.wait('property-early', 'property:other', 5), false)
  const active = tracker.wait('property-late', 'property:9001', 50)
  tracker.mark('property-late', ['property:9001'])
  assert.equal(await active, true)
  tracker.clear()
})

await Promise.all(pending)
console.log(JSON.stringify({ suite: 'property-map', results }, null, 2))
if (results.some((result) => result.status === 'FAIL')) process.exitCode = 1
