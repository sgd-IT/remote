import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, join, resolve } from 'node:path'

const dependencyRoot = process.env.PROPERTY_MAP_PLAYWRIGHT_ROOT
const profile = process.env.PROPERTY_MAP_CHROME_PROFILE
const targetUrl = process.env.PROPERTY_MAP_BROWSER_URL
const sampleId = process.env.PROPERTY_MAP_SAMPLE_ID
if (!dependencyRoot || !profile || !targetUrl || !sampleId) {
  console.log(JSON.stringify({ suite: 'property-map-three-polls', status: 'NOT_RUN_ENV', reason: 'Playwright root, Chrome profile, browser URL and sample ID are required' }))
  process.exit(0)
}

const require = createRequire(join(dependencyRoot, 'package.json'))
const { chromium } = require('playwright')
const reportPath = resolve(process.env.PROPERTY_MAP_THREE_POLLS_REPORT || 'tasks/V2.0/execution/wgs84-r7/three-polls-report.json')
const screenshotPath = resolve('tasks/V2.0/execution/wgs84-r7/screenshots/real/authorized-property-3polls-1600x900.png')
const report = { suite: 'property-map-three-polls', status: 'FAIL', sampleId, responses: [], commands: [], stages: [], errors: [] }
const context = await chromium.launchPersistentContext(profile, { headless: true, executablePath: process.env.DONGGUAN_BROWSER_PATH || undefined, viewport: { width: 1600, height: 900 }, deviceScaleFactor: 1 })
try {
  const page = await context.newPage()
  await page.addInitScript(() => {
    window.__propertyMapStageReports = []
    window.__propertyMapPointCommands = []
    window.addEventListener('message', event => {
      const message = event.data
      if (message?.type === 'dongguan-map-command' && message.command === 'points') {
        window.__propertyMapPointCommands.push({ snapshotId: message.snapshotId, ids: message.points?.map(point => point.id) ?? [] })
      }
      if (['dongguan-map-points-applied', 'dongguan-map-labels-applied', 'dongguan-map-points-rejected'].includes(message?.type)) {
        window.__propertyMapStageReports.push({
          type: message.type, snapshotId: message.snapshotId,
          renderedPropertyIds: message.renderedPropertyIds, excluded: message.excluded, excludedIds: message.excludedIds,
          visiblePropertyIds: message.visiblePropertyIds, clusteredPropertyIds: message.clusteredPropertyIds,
          hiddenPropertyIds: message.hiddenPropertyIds, offscreenPropertyIds: message.offscreenPropertyIds,
          reason: message.reason
        })
      }
    })
  })
  const listResponses = []
  page.on('response', response => {
    const url = new URL(response.url())
    if (url.pathname.endsWith('/eiot/firewatch/property/list') && url.searchParams.get('status') === '1') listResponses.push(response)
  })
  page.on('pageerror', error => report.errors.push(error.message))
  await page.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: 45_000 })
  await page.waitForSelector('iframe.scene-host__frame', { timeout: 30_000 })
  const frame = page.frameLocator('iframe.scene-host__frame')
  await frame.locator('[data-dongguan-property-labels]').waitFor({ timeout: 45_000 })
  await page.waitForFunction(id => {
    const element = document.querySelector('iframe.scene-host__frame')?.contentDocument?.querySelector('[data-dongguan-property-labels]')
    const layout = JSON.parse(element?.getAttribute('data-dongguan-property-layout') || '[]')
    return layout.some(card => card.id === `property:${id}` || card.clusterIds?.includes(`property:${id}`))
  }, sampleId, { timeout: 45_000 })
  const deadline = Date.now() + 105_000
  while (listResponses.length < 3 && Date.now() < deadline) await page.waitForTimeout(1_000)
  assert.ok(listResponses.length >= 3, `Only ${listResponses.length} real property list responses arrived within 105 seconds`)
  for (const response of listResponses.slice(0, 3)) {
    const body = await response.json()
    const rows = body?.data
    const sample = Array.isArray(rows) ? rows.find(row => String(row.id) === sampleId) : null
    report.responses.push({ httpStatus: response.status(), code: body?.code, count: Array.isArray(rows) ? rows.length : null, samplePresent: Boolean(sample), sampleStatus: sample?.status, longitude: sample?.longitude, latitude: sample?.latitude })
  }
  assert.ok(report.responses.every(item => item.httpStatus === 200 && item.code === 0 && item.samplePresent), 'A property list cycle failed or omitted the sample')
  await page.waitForTimeout(2_000)
  report.iframePath = new URL(await page.locator('iframe.scene-host__frame').getAttribute('src'), page.url()).pathname
  report.commands = await frame.locator('body').evaluate(() => window.__propertyMapPointCommands || [])
  report.stages = await page.evaluate(() => window.__propertyMapStageReports || [])
  const commandIds = [...new Set(report.commands.map(command => command.snapshotId))]
  assert.ok(commandIds.length >= 3, `Only ${commandIds.length} point snapshots reached the iframe`)
  assert.ok(report.stages.some(stage => stage.type === 'dongguan-map-points-applied' && stage.renderedPropertyIds?.includes(`property:${sampleId}`)), 'The sample has no successful geometry receipt')
  assert.ok(report.stages.some(stage => stage.type === 'dongguan-map-labels-applied' && [stage.visiblePropertyIds, stage.clusteredPropertyIds, stage.hiddenPropertyIds].some(ids => ids?.includes(`property:${sampleId}`))), 'The sample has no accessible label receipt')
  assert.deepEqual(report.errors, [])
  await mkdir(dirname(screenshotPath), { recursive: true })
  await page.screenshot({ path: screenshotPath, timeout: 90_000, clip: { x: 420, y: 60, width: 750, height: 820 } })
  report.status = 'PASS'
} catch (error) {
  report.error = error instanceof Error ? error.message : String(error)
  process.exitCode = 1
} finally {
  await context.close()
  await mkdir(dirname(reportPath), { recursive: true })
  await writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`)
  console.log(JSON.stringify({ ...report, commands: report.commands.length, stages: report.stages.length }, null, 2))
}
