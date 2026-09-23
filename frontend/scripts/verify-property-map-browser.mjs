import { createRequire } from 'node:module'
import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'
import { join, resolve } from 'node:path'

const dependencyRoot = process.env.PROPERTY_MAP_PLAYWRIGHT_ROOT || process.env.DONGGUAN_PLAYWRIGHT_ROOT
const targetUrl = process.env.PROPERTY_MAP_BROWSER_URL
const interactiveLogin = process.env.PROPERTY_MAP_INTERACTIVE_LOGIN === '1'
const chromeProfile = process.env.PROPERTY_MAP_CHROME_PROFILE
const parkCycles = Number(process.env.PROPERTY_MAP_PARK_CYCLES || 20)
assert.ok(Number.isSafeInteger(parkCycles) && parkCycles >= 1 && parkCycles <= 20, 'PROPERTY_MAP_PARK_CYCLES must be an integer from 1 to 20')
const targetName = '东莞南城碧桂园湾区国际'
if (!dependencyRoot || !targetUrl) {
  console.log(JSON.stringify({ suite: 'property-map-browser', status: 'NOT_RUN_ENV', reason: 'PROPERTY_MAP_PLAYWRIGHT_ROOT/DONGGUAN_PLAYWRIGHT_ROOT and PROPERTY_MAP_BROWSER_URL are required for authenticated browser validation' }))
  process.exit(0)
}
const require = createRequire(join(dependencyRoot, 'package.json'))
const { chromium } = require('playwright')
const artifactDir = resolve(process.env.PROPERTY_MAP_SCREENSHOT_DIR || 'tasks/V2.0/execution/wgs84-r7/screenshots/real')
const reportPath = resolve(process.env.PROPERTY_MAP_BROWSER_REPORT || 'tasks/V2.0/execution/wgs84-r7/browser-report.json')
await mkdir(artifactDir, { recursive: true })
const browser = chromeProfile
  ? await chromium.launchPersistentContext(chromeProfile, { headless: !interactiveLogin, executablePath: process.env.DONGGUAN_BROWSER_PATH || undefined, viewport: { width: 1600, height: 900 }, deviceScaleFactor: 1 })
  : await chromium.launch({ headless: !interactiveLogin, executablePath: process.env.DONGGUAN_BROWSER_PATH || undefined })
const report = { suite: 'property-map-browser', status: 'FAIL', basicStatus: 'NOT_RUN', entryStatus: 'NOT_RUN', cycles: [], returnControls: [], errors: [], requests: [], deviceLocationRequests: 0, propertyPointCommands: 0 }
try {
  const page = chromeProfile ? await browser.newPage() : await browser.newPage({ viewport: { width: 1600, height: 900 }, deviceScaleFactor: 1 })
  await page.addInitScript(() => {
    window.__propertyMapPointCommands = []
    window.addEventListener('message', event => {
      const message = event.data
      if (message?.type === 'dongguan-map-command' && message.command === 'points') {
        window.__propertyMapPointCommands.push({
          snapshotId: message.snapshotId,
          kinds: Array.isArray(message.points) ? message.points.map(point => point.kind) : [],
          ids: Array.isArray(message.points) ? message.points.map(point => point.id) : []
        })
      }
    })
  })
  await page.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: 45_000 })
  await page.waitForTimeout(1_000)
  const login = page.locator('.login-btn')
  if (interactiveLogin && new URL(page.url()).hash.includes('/login')) {
    console.log('请在打开的测试浏览器中自行登录；脚本最多等待 5 分钟，不读取或保存登录凭据。')
    await page.waitForURL(url => !url.hash.includes('/login'), { timeout: 300_000 }).catch(() => undefined)
  } else if (interactiveLogin && await login.count()) {
    await page.waitForURL(url => !url.hash.includes('/login'), { timeout: 300_000 }).catch(() => undefined)
  }
  if (new URL(page.url()).hash.includes('/login')) {
    report.status = 'NOT_RUN_ENV'
    report.reason = 'No authenticated test session is available'
    await page.screenshot({ path: join(artifactDir, 'authentication-blocked-1600x900.png') })
  } else {
    page.on('pageerror', error => report.errors.push(error.message))
    page.on('console', message => { if (message.type() === 'error') report.errors.push(message.text()) })
    page.on('request', request => { if (new URL(request.url()).pathname.endsWith('/eiot/firewatch/dashboard/device-locations')) report.deviceLocationRequests += 1 })
    page.on('response', response => { if (response.status() >= 400 && /property|dongguan-map|smart-park/.test(response.url())) report.requests.push({ status: response.status(), url: new URL(response.url()).pathname }) })
    const listResponsePromise = page.waitForResponse(response => {
      const url = new URL(response.url())
      return url.pathname.endsWith('/eiot/firewatch/property/list') && url.searchParams.get('status') === '1'
    }, { timeout: 30_000 }).catch(() => null)
    await page.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: 45_000 })
    const listResponse = await listResponsePromise
    if (!listResponse && (new URL(page.url()).hash.includes('/login') || await login.count())) {
      report.status = 'NOT_RUN_ENV'
      report.reason = 'No authenticated test session is available; the application returned to login before the property request'
      await page.screenshot({ path: join(artifactDir, 'authentication-blocked-1600x900.png') })
    } else {
    const listBody = await listResponse?.json().catch(() => null)
    assert.ok(listResponse, 'Authorized property list did not respond')
    assert.equal(listBody?.code, 0, 'Authorized property list returned a business error')
    assert.ok(Array.isArray(listBody?.data), 'Property list response is not a complete array')
    const rows = listBody.data
    const matches = rows.filter(row => row.name === targetName || row.propertyName === targetName)
    report.authorizedPropertyCount = rows.length
    report.targetMatchCount = matches.length
    report.propertyCoordinates = rows.map(row => ({ id: String(row.id), status: row.status, longitude: row.longitude, latitude: row.latitude }))
    if (matches.length === 1) report.target = { id: String(matches[0].id), code: matches[0].code, longitude: matches[0].longitude, latitude: matches[0].latitude, status: matches[0].status }
    const configResponse = await page.request.get(new URL('config/property-map.json', page.url().split('#')[0]).toString())
    const config = configResponse.ok() ? await configResponse.json() : null
    await page.waitForSelector('iframe.scene-host__frame', { timeout: 30_000 })
    let status = await page.locator('.property-map-status').innerText().catch(() => '')
    const mapFrame = () => page.frameLocator('iframe.scene-host__frame')
    const frame = mapFrame()
    await frame.locator('[data-dongguan-property-labels]').waitFor({ timeout: 30_000 })
    await page.waitForFunction(() => document.querySelector('iframe.scene-host__frame')?.contentWindow?.__propertyMapPointCommands?.length > 0, null, { timeout: 30_000 })
    const pointCommands = await frame.locator('body').evaluate(() => window.__propertyMapPointCommands)
    report.propertyPointCommands = pointCommands.length
    assert.equal(report.deviceLocationRequests, 0, 'The duty screen still requests device map locations')
    assert.ok(pointCommands.every(command => command.kinds.every(kind => kind === 'property')), 'The city map received a device point')
    assert.ok(config && config.sourceCrs === 'WGS84', 'Active property map configuration is not WGS84')
    const candidateIds = rows.filter(row => row.status === 1 && row.longitude != null && row.latitude != null).map(row => `property:${row.id}`)
    const sentIds = new Set(pointCommands.flatMap(command => command.ids))
    const sentCandidates = candidateIds.filter(id => sentIds.has(id))
    if (!candidateIds.length) {
      report.basicStatus = 'NOT_RUN_DATA'
      report.reason = 'Authorized list has no enabled property with WGS84 coordinates'
      report.status = 'PARTIAL'
    } else {
      assert.ok(sentCandidates.length, 'Enabled properties with coordinates were not sent to the map')
      await page.waitForFunction(ids => {
        const element = document.querySelector('iframe.scene-host__frame')?.contentDocument?.querySelector('[data-dongguan-property-labels]')
        const layout = JSON.parse(element?.getAttribute('data-dongguan-property-layout') || '[]')
        return layout.some(card => ids.some(id => card.id === id || card.clusterIds.includes(id)))
      }, sentCandidates, { timeout: 30_000 })
      const basicLayout = await frame.locator('[data-dongguan-property-labels]').evaluate(element => JSON.parse(element.getAttribute('data-dongguan-property-layout') || '[]'))
      const basicRow = rows.find(row => basicLayout.some(card => card.id === `property:${row.id}` || card.clusterIds.includes(`property:${row.id}`)))
      assert.ok(basicRow, 'No authorized property is visible as a card or cluster')
      const basicId = `property:${basicRow.id}`
      const basicCard = basicLayout.find(card => card.id === basicId || card.clusterIds.includes(basicId))
      const expectedName = String(basicRow.name ?? basicRow.propertyName ?? '').trim().slice(0, 512) || '未命名物业'
      const expectedAddress = String(basicRow.address ?? '').trim().slice(0, 2048) || '地址未维护'
      if (basicCard.clusterIds.length) {
        await frame.locator(`[data-card-id="${basicCard.id}"] .property-label__main`).click()
        const member = frame.locator(`.property-label__list-item[data-point-id="${basicId}"]`)
        assert.equal(await member.locator('strong').innerText(), expectedName)
        assert.equal(await member.locator('span').innerText(), expectedAddress)
        await frame.getByRole('button', { name: '关闭聚合物业列表' }).click()
      } else {
        const card = frame.locator(`[data-card-id="${basicId}"]`)
        assert.equal(await card.locator('.property-label__title').innerText(), expectedName)
        assert.equal(await card.locator('.property-label__address').innerText(), expectedAddress)
      }
      report.basicStatus = 'PASS'
      report.basicPropertyId = String(basicRow.id)
      status = await page.locator('.property-map-status').innerText().catch(() => status)
      report.mapStatus = status
      const diagnosticText = await page.locator('.property-map-status pre').textContent().catch(() => null)
      if (diagnosticText) report.diagnostic = JSON.parse(diagnosticText)
      await page.screenshot({ path: join(artifactDir, 'real-ordinary-property-1600x900.png'), clip: { x: 420, y: 60, width: 750, height: 820 } })
    const targetSource = config?.coordinateOverrides?.[String(matches[0]?.id)] ?? config?.sourceCrs
    if (targetSource === 'UNKNOWN' || !config.parkPropertyId || !config.parkPropertyCode || matches.length !== 1) {
      report.entryStatus = 'NOT_RUN_CONFIG'
      report.status = 'PARTIAL'
      report.reason = status || 'Real park entry identity is not configured; ordinary property rendering passed independently'
    } else {
      const target = matches[0]
      assert.equal(String(target.id), config.parkPropertyId, 'Configured park entry ID does not match the authorized list')
      assert.equal(target.code, config.parkPropertyCode, 'Configured park entry code does not match the authorized list')
      assert.equal(target.status, 1, 'Configured park entry is disabled')
      const pointId = `property:${target.id}`
      assert.ok(pointCommands.some(command => command.ids.includes(pointId)), 'The target property was not sent to the map')
      const readLayout = currentFrame => currentFrame.locator('[data-dongguan-property-labels]').evaluate(element => JSON.parse(element.getAttribute('data-dongguan-property-layout') || '[]'))
      await page.waitForFunction(id => {
        const element = document.querySelector('iframe.scene-host__frame')?.contentDocument?.querySelector('[data-dongguan-property-labels]')
        const layout = JSON.parse(element?.getAttribute('data-dongguan-property-layout') || '[]')
        return layout.some(card => card.id === id || card.clusterIds.includes(id))
      }, pointId, { timeout: 30_000 })
      const targetLayout = (await readLayout(frame)).find(card => card.id === pointId || card.clusterIds.includes(pointId))
      assert.ok(targetLayout, 'The real entry is not visible as a card or cluster')
      const expectedAddress = String(target.address ?? '').trim().slice(0, 2048) || '地址未维护'
      assert.equal(await frame.getByRole('dialog', { name: '聚合物业列表' }).count(), 0, 'A cluster dialog opened without a click')
      if (targetLayout.clusterIds.length) {
        await frame.locator(`[data-card-id="${targetLayout.id}"] .property-label__main`).click()
        const member = frame.locator(`.property-label__list-item[data-point-id="${pointId}"]`)
        assert.equal(await member.locator('strong').innerText(), targetName)
        assert.equal(await member.locator('span').innerText(), expectedAddress)
        await frame.getByRole('button', { name: '关闭聚合物业列表' }).click()
      } else {
        const targetCard = frame.locator(`.property-label__main[aria-label="进入园区：${targetName}"]`)
        assert.equal(await targetCard.locator('.property-label__title').innerText(), targetName)
        assert.equal(await targetCard.locator('.property-label__address').innerText(), expectedAddress)
      }
      assert.equal(await page.locator('.left-panel').count(), 1)
      assert.equal(await page.locator('.right-panel').count(), 1)
      await page.screenshot({ path: join(artifactDir, 'real-city-1600x900.png'), clip: { x: 420, y: 60, width: 750, height: 820 } })
      const original = new URL(page.url())
      const [routePath, routeQuery = ''] = original.hash.split('?')
      const routeParams = new URLSearchParams(routeQuery)
      routeParams.set('e2e-preserve', '1')
      original.hash = `${routePath}?${routeParams}`
      await page.goto(original.toString())
      for (let cycle = 1; cycle <= parkCycles; cycle += 1) {
        await page.waitForSelector('iframe.scene-host__frame')
        const cityFrame = mapFrame()
        await cityFrame.locator('[data-dongguan-property-labels]').waitFor({ timeout: 30_000 })
        await page.waitForFunction(id => {
          const element = document.querySelector('iframe.scene-host__frame')?.contentDocument?.querySelector('[data-dongguan-property-labels]')
          const layout = JSON.parse(element?.getAttribute('data-dongguan-property-layout') || '[]')
          return layout.some(card => card.id === id || card.clusterIds.includes(id))
        }, pointId, { timeout: 30_000 })
        const layout = await readLayout(cityFrame)
        const entry = layout.find(card => card.id === pointId || card.clusterIds.includes(pointId))
        const detailPromise = cycle === 1 ? page.waitForResponse(response => {
          const url = new URL(response.url())
          return url.pathname.endsWith('/eiot/firewatch/property/get') && url.searchParams.get('id') === String(target.id)
        }, { timeout: 30_000 }) : null
        if (entry.clusterIds.length) {
          await cityFrame.locator(`[data-card-id="${entry.id}"] .property-label__main`).click()
          await cityFrame.locator(`.property-label__list-item[data-point-id="${pointId}"]`).click()
        } else if (cycle === 2) {
          const box = await page.locator('iframe.scene-host__frame').boundingBox()
          if (!box) throw new Error('Real entry anchor layout is unavailable')
          await page.mouse.click(box.x + entry.anchor[0], box.y + entry.anchor[1])
        } else {
          await cityFrame.locator(`.property-label__main[aria-label="进入园区：${targetName}"]`).click()
        }
        if (detailPromise) {
          const detailBody = await (await detailPromise).json()
          assert.equal(detailBody?.code, 0, 'Entry detail request failed')
          assert.equal(String(detailBody.data?.id), String(target.id), 'Entry detail identity changed')
          assert.equal(detailBody.data?.code, target.code, 'Entry detail code changed')
          assert.equal(detailBody.data?.status, 1, 'Entry detail is disabled')
        }
        await page.waitForURL(url => { const query = new URLSearchParams(url.hash.split('?')[1] || ''); return query.get('scene') === 'park' && query.get('e2e-preserve') === '1' }, { timeout: 30_000 })
        await page.waitForFunction(() => document.querySelectorAll('iframe.scene-host__frame').length === 1 && !document.querySelector('.scene-host__loading'))
        if (cycle === 1) await page.screenshot({ path: join(artifactDir, 'real-park-1600x900.png'), clip: { x: 420, y: 60, width: 750, height: 820 } })
        const parkReturn = mapFrame().locator('.park-return[data-action="presentation-back"]')
        if (await parkReturn.count() && await parkReturn.isVisible()) {
          await parkReturn.click({ timeout: 30_000 })
          report.returnControls.push('park')
        } else {
          await page.locator('.park-return-to-city').click({ timeout: 30_000 })
          report.returnControls.push('host')
        }
        await page.waitForURL(url => { const query = new URLSearchParams(url.hash.split('?')[1] || ''); return query.get('scene') === 'city' && query.get('e2e-preserve') === '1' }, { timeout: 30_000 })
        report.cycles.push({ cycle, iframeCount: await page.locator('iframe.scene-host__frame').count(), heap: await page.evaluate(() => performance.memory?.usedJSHeapSize ?? null) })
      }
      await page.screenshot({ path: join(artifactDir, 'real-city-returned-1600x900.png'), clip: { x: 420, y: 60, width: 750, height: 820 } })
      if (report.cycles.some(cycle => cycle.iframeCount !== 1)) throw new Error('More than one scene iframe remained after a cycle')
      assert.equal(report.deviceLocationRequests, 0, 'A device-location request appeared during city/park cycles')
      if (report.errors.length || report.requests.length) throw new Error('Browser errors or critical request failures were observed')
      report.entryStatus = 'PASS'
      report.status = 'PASS'
    }
    }
    }
  }
} catch (error) {
  report.errors.push(error instanceof Error ? error.stack : String(error))
  process.exitCode = 1
} finally {
  await mkdir(resolve(reportPath, '..'), { recursive: true })
  await writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`)
  await browser.close()
  console.log(JSON.stringify(report, null, 2))
}
