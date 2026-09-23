import { createRequire } from 'node:module'
import { join } from 'node:path'

const dependencyRoot = process.env.PROPERTY_MAP_PLAYWRIGHT_ROOT || process.env.DONGGUAN_PLAYWRIGHT_ROOT
if (!dependencyRoot) throw new Error('PROPERTY_MAP_PLAYWRIGHT_ROOT or DONGGUAN_PLAYWRIGHT_ROOT is required')
const require = createRequire(join(dependencyRoot, 'package.json'))
const { chromium } = require('playwright')
const browser = await chromium.launch({ headless: true, executablePath: process.env.DONGGUAN_BROWSER_PATH || undefined })
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const pageErrors = []
  page.on('pageerror', error => pageErrors.push(error.message))
  await page.goto(process.env.PROPERTY_MAP_BROWSER_URL || 'http://127.0.0.1:5173/#/duty-screen?scene=city', { waitUntil: 'domcontentloaded', timeout: 45_000 })
  await page.waitForTimeout(1_500)
  const login = page.locator('.login-btn')
  await login.waitFor({ state: 'visible', timeout: 10_000 }).catch(() => undefined)
  let attemptedLogin = false
  let defaultsFilled = false
  if (await login.count()) {
    defaultsFilled = await page.evaluate(() => Boolean(document.querySelector('input[type="text"]')?.value && document.querySelector('input[type="password"]')?.value))
    if (defaultsFilled) {
      attemptedLogin = true
      await login.click()
      await page.waitForURL(url => !url.hash.includes('/login'), { timeout: 30_000 }).catch(() => undefined)
    }
  }
  const result = await page.evaluate(async () => {
    const cached = (key) => {
      const raw = localStorage.getItem(key)
      if (!raw) return null
      try {
        const parsed = JSON.parse(raw)
        return parsed?.v ?? parsed?.value ?? parsed
      } catch { return raw }
    }
    const token = cached('ACCESS_TOKEN')
    const tenantId = cached('tenantId')
    if (!token) return { authenticated: false, status: null, code: null, targetMatches: [] }
    const headers = { Authorization: `Bearer ${token}`, 'Cache-Control': 'no-cache', Pragma: 'no-cache' }
    if (tenantId) headers['tenant-id'] = String(tenantId)
    const response = await fetch('/admin-api/eiot/firewatch/property/list?status=1', { headers })
    const body = await response.json().catch(() => ({}))
    const rows = Array.isArray(body?.data) ? body.data : []
    return {
      authenticated: true,
      status: response.status,
      code: body?.code ?? null,
      rowCount: rows.length,
      targetMatches: rows.filter(row => row?.name === '东莞南城碧桂园湾区国际' || row?.propertyName === '东莞南城碧桂园湾区国际').map(row => ({ id: row.id, code: row.code, name: row.name ?? row.propertyName, address: row.address, longitude: row.longitude, latitude: row.latitude, status: row.status })),
    }
  })
  const bodyText = (await page.locator('body').innerText().catch(() => '')).slice(0, 500)
  console.log(JSON.stringify({ suite: 'property-map-session-probe', attemptedLogin, defaultsFilled, loginButtonCount: await login.count(), route: new URL(page.url()).hash.split('?')[0], pageErrors, bodyText, ...result }, null, 2))
} finally {
  await browser.close()
}
