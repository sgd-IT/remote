import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { dirname, join, resolve } from 'node:path'

const dependencyRoot = process.env.PROPERTY_MAP_PLAYWRIGHT_ROOT || process.env.DONGGUAN_PLAYWRIGHT_ROOT
const baseUrl = process.env.PROPERTY_MAP_BROWSER_BASE_URL
if (!dependencyRoot || !baseUrl) {
  console.log(JSON.stringify({ suite: 'property-map-release-browser', status: 'NOT_RUN_ENV', reason: 'Playwright runtime and PROPERTY_MAP_BROWSER_BASE_URL are required' }))
  process.exit(0)
}

const require = createRequire(join(dependencyRoot, 'package.json'))
const { chromium } = require('playwright')
const browser = await chromium.launch({ headless: true, args: ['--enable-webgl', '--ignore-gpu-blocklist', '--use-angle=swiftshader'] })
const report = { suite: 'property-map-release-browser', status: 'FAIL', failedRequests: [], pageErrors: [] }
const reportPath = resolve(process.env.PROPERTY_MAP_RELEASE_BROWSER_REPORT || 'tasks/V2.0/execution/wgs84-r7/release-browser-report.json')
try {
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 } })
  page.on('pageerror', error => report.pageErrors.push(error.message))
  page.on('response', response => {
    if (response.status() >= 400) report.failedRequests.push({ status: response.status(), path: new URL(response.url()).pathname })
  })
  const releaseUrl = new URL('dongguan-map/releases/v002-property-r7/index.html', baseUrl)
  releaseUrl.search = '?embedded=1&diagnostics=1'
  releaseUrl.hash = '#/dongguan'
  const response = await page.goto(releaseUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 45_000 })
  assert.equal(response?.status(), 200)
  await page.locator('[data-dongguan-property-labels]').waitFor({ timeout: 90_000 })
  await page.waitForTimeout(2_000)
  assert.deepEqual(report.failedRequests, [])
  assert.deepEqual(report.pageErrors, [])
  report.status = 'PASS'
  report.pathname = new URL(page.url()).pathname
} catch (error) {
  report.error = error instanceof Error ? error.message : String(error)
  process.exitCode = 1
} finally {
  await browser.close()
  await mkdir(dirname(reportPath), { recursive: true })
  await writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`)
  console.log(JSON.stringify(report, null, 2))
}
