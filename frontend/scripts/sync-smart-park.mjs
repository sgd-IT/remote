import { copyFile, mkdir, readFile, writeFile } from 'node:fs/promises'
import { constants } from 'node:fs'
import { createHash } from 'node:crypto'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = resolve(fileURLToPath(new URL('..', import.meta.url)))
const sourceRoot = resolve(process.env.SMART_PARK_SOURCE || 'D:/anwei-Smart-Park/app')
const release = process.env.SMART_PARK_RELEASE || process.argv[2] || 'v007-r001'
if (!/^[A-Za-z0-9._-]+$/.test(release) || release.includes('..') || release.includes('/') || release.includes('\\')) throw new Error(`非法园区 release：${release}`)

const destinationDir = join(projectRoot, 'public', 'smart-park', 'releases', release)
const manifestPath = join(projectRoot, 'public', 'smart-park', 'release-manifest.json')
if (release !== 'v007-r001') throw new Error(`不支持的园区 release：${release}`)
  const packageDir = resolve(process.env.SMART_PARK_PACKAGE_DIR || join(sourceRoot, '..', 'tasks', 'V 7.0', '界面精简与设备告警', '交付', 'V7.0-界面精简-20260914'))
  const packageManifest = JSON.parse(await readFile(join(packageDir, 'package-manifest.json'), 'utf8'))
  if (packageManifest.revision !== release || !Array.isArray(packageManifest.files)) throw new Error('V7 package manifest revision mismatch')
  const allowed = /^public\/(?:assets\/[^/]+|park\/v007\/v007-r001\/.+|smart-park\/releases\/v007-r001\/index\.html)$/
  const entries = []
  for (const item of packageManifest.files) {
    if (item.path === '集成说明.md') continue
    if (typeof item.path !== 'string' || !allowed.test(item.path) || item.path.includes('..') || !/^[a-f0-9]{64}$/.test(item.sha256)) throw new Error(`V7 package path/hash 非法：${item.path}`)
    const source = join(packageDir, item.path)
    const destination = join(projectRoot, item.path)
    const bytes = await readFile(source)
    const hash = createHash('sha256').update(bytes).digest('hex')
    if (bytes.length !== item.bytes || hash !== item.sha256) throw new Error(`V7 package hash mismatch: ${item.path}`)
    let exists = false
    try {
      const current = await readFile(destination)
      exists = true
      if (createHash('sha256').update(current).digest('hex') !== hash) throw new Error(`目标文件内容不同，拒绝覆盖：${destination}`)
    } catch (error) {
      if (error?.code !== 'ENOENT') throw error
    }
    entries.push({ source, destination, exists })
  }
  if (!entries.some(item => item.destination === join(destinationDir, 'index.html'))) throw new Error('V7 package 缺少园区入口')
  const contract = JSON.parse(await readFile(join(packageDir, 'public', 'park', 'v007', release, 'contracts', 'manifest.json'), 'utf8'))
  if (contract.revision !== release) throw new Error('V7 package 合同版本不匹配')
  for (const item of entries) {
    if (item.exists) continue
    await mkdir(dirname(item.destination), { recursive: true })
    await copyFile(item.source, item.destination, constants.COPYFILE_EXCL)
  }
  let currentManifest = null
  try {
    currentManifest = JSON.parse(await readFile(manifestPath, 'utf8'))
  } catch (error) {
    if (error?.code !== 'ENOENT' && !(error instanceof SyntaxError)) throw error
  }
  if (currentManifest?.schemaVersion !== 1 || currentManifest?.app !== 'smart-park' || currentManifest?.release !== release || currentManifest?.revision !== release || currentManifest?.entry !== `/smart-park/releases/${release}/index.html`) {
    await writeFile(manifestPath, `${JSON.stringify({ schemaVersion: 1, app: 'smart-park', release, revision: release, entry: `/smart-park/releases/${release}/index.html`, generatedAt: new Date().toISOString() }, null, 2)}\n`, 'utf8')
  }
  console.log(`智慧园区 V7 release ${release} 已同步，共 ${entries.length} 个文件`)
