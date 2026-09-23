import { access, readFile } from 'node:fs/promises'
import { constants } from 'node:fs'
import { join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(fileURLToPath(new URL('..', import.meta.url)))
const mapManifest = JSON.parse(await readFile(join(root, 'public', 'dongguan-map', 'release-manifest.json'), 'utf8'))
const parkManifest = JSON.parse(await readFile(join(root, 'public', 'smart-park', 'release-manifest.json'), 'utf8'))
const mapRelease = process.env.DONGGUAN_MAP_RELEASE || mapManifest.release
const parkRelease = process.env.SMART_PARK_RELEASE || parkManifest.release
const hostBase = process.env.SCENE_HOST_BASE || process.env.VITE_BASE_PATH || '/'
const safe = value => /^[A-Za-z0-9._-]+$/.test(value) && !value.includes('..') && !value.includes('/') && !value.includes('\\')
if (!safe(mapRelease) || !safe(parkRelease)) throw new Error('scene release id 非法')
if (typeof hostBase !== 'string' || !hostBase.startsWith('/') || hostBase.includes('..') || hostBase.includes('\\')) throw new Error('scene host base path 非法')
const normalizedHostBase = hostBase.endsWith('/') ? hostBase : `${hostBase}/`
if (!process.env.DONGGUAN_MAP_RELEASE && (mapManifest.schemaVersion !== 1 || mapManifest.app !== 'dongguan-map' || mapManifest.release !== mapRelease || mapManifest.entry !== `${normalizedHostBase}dongguan-map/releases/${mapRelease}/index.html`)) throw new Error('dongguan-map release manifest mismatch')
if (!process.env.SMART_PARK_RELEASE && (parkManifest.schemaVersion !== 1 || parkManifest.app !== 'smart-park' || parkManifest.release !== parkRelease || parkManifest.revision !== parkRelease || parkManifest.entry !== `${normalizedHostBase}smart-park/releases/${parkRelease}/index.html`)) throw new Error('smart-park release manifest mismatch')

async function checkIndex(app, release) {
  const dir = join(root, 'public', app, 'releases', release)
  const index = await readFile(join(dir, 'index.html'), 'utf8')
  const expected = `${normalizedHostBase}${app}/releases/${release}/`
  const rootAssets = app === 'smart-park' && release === 'v007-r001' && normalizedHostBase === '/' && index.includes('src="/assets/')
  if (!index.includes(expected) && !index.includes('src="./') && !index.includes("src='./") && !rootAssets) throw new Error(`${app} ${release} base path 校验失败`)
  await access(dir, constants.R_OK)
  console.log(`${app}/${release}: ok`)
}

await checkIndex('dongguan-map', mapRelease)
await checkIndex('smart-park', parkRelease)
if (parkRelease !== 'v007-r001') throw new Error(`不支持的园区 release：${parkRelease}`)
  if (normalizedHostBase !== '/') throw new Error('V7 园区资源合同要求站点根路径 /')
  const index = await readFile(join(root, 'public', 'smart-park', 'releases', parkRelease, 'index.html'), 'utf8')
  const contract = JSON.parse(await readFile(join(root, 'public', 'park', 'v007', parkRelease, 'contracts', 'manifest.json'), 'utf8'))
  if (contract.revision !== parkRelease || !Array.isArray(contract.assets) || contract.assets.length === 0) throw new Error('smart-park V7 contract mismatch')
  const urls = [...index.matchAll(/(?:src|href)="(\/assets\/[^"?#]+)"/g)].map(match => match[1])
  urls.push(...contract.assets.map(item => item.url))
  for (const url of urls) {
    if (typeof url !== 'string' || !/^\/(?:assets\/[^/]+|park\/v007\/v007-r001\/[A-Za-z0-9._/-]+)$/.test(url) || url.includes('..')) throw new Error(`V7 资源路径非法：${url}`)
    await access(join(root, 'public', url.slice(1)), constants.R_OK)
  }
console.log('scene release contract: ok')
