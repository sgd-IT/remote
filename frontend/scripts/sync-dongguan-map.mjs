import { access, cp, mkdtemp, mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { constants } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const projectRoot = resolve(fileURLToPath(new URL('..', import.meta.url)))
const sourceRoot = resolve(process.env.DONGGUAN_MAP_SOURCE || 'D:/sc-datav')
const release = process.env.DONGGUAN_MAP_RELEASE || process.argv[2] || 'v001'
const releasePattern = /^[A-Za-z0-9._-]+$/
if (!releasePattern.test(release) || release.includes('..') || release.includes('/') || release.includes('\\')) throw new Error(`非法地图 release：${release}`)
const hostBase = process.env.DONGGUAN_MAP_HOST_BASE || process.env.VITE_BASE_PATH || '/'
if (typeof hostBase !== 'string' || !hostBase.startsWith('/') || hostBase.includes('..') || hostBase.includes('\\')) throw new Error(`非法宿主 base path：${hostBase}`)
const normalizedHostBase = hostBase.endsWith('/') ? hostBase : `${hostBase}/`
const releaseBase = `${normalizedHostBase}dongguan-map/releases/${release}/`

const viteCli = join(sourceRoot, 'node_modules', 'vite', 'bin', 'vite.js')
const releasesDir = join(projectRoot, 'public', 'dongguan-map', 'releases')
const destinationDir = join(releasesDir, release)
const metadataPath = join(projectRoot, 'public', 'dongguan-map', 'release-manifest.json')
const stagingDir = await mkdtemp(join(projectRoot, '.tmp-dongguan-map-'))

const run = (command, args, options) => new Promise((resolveRun, rejectRun) => {
  const child = spawn(command, args, { stdio: 'inherit', ...options })
  child.once('error', rejectRun)
  child.once('exit', code => code === 0 ? resolveRun() : rejectRun(new Error(`${command} ${args.join(' ')} failed with exit code ${code}`)))
})

try {
  await access(viteCli, constants.R_OK)
  try { await access(destinationDir, constants.F_OK); throw new Error(`release 已存在，拒绝覆盖：${destinationDir}`) } catch (error) {
    if (error?.code !== 'ENOENT') throw error
  }
  await run(process.execPath, [viteCli, 'build', '--outDir', stagingDir], {
    cwd: sourceRoot,
    env: { ...process.env, VITE_BASE_PATH: releaseBase }
  })
  await access(join(stagingDir, 'index.html'), constants.R_OK)
  await mkdir(releasesDir, { recursive: true })
  await cp(stagingDir, destinationDir, { recursive: true, errorOnExist: true, force: false })
  const manifest = { schemaVersion: 1, app: 'dongguan-map', release, entry: `${releaseBase}index.html`, generatedAt: new Date().toISOString() }
  await mkdir(dirname(metadataPath), { recursive: true })
  await writeFile(metadataPath, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8')
  const index = await readFile(join(destinationDir, 'index.html'), 'utf8')
  if (!index.includes(releaseBase)) throw new Error('地图构建产物 base path 校验失败')
  console.log(`东莞三维地图 release ${release} 已同步至 ${destinationDir}`)
} finally {
  await rm(stagingDir, { recursive: true, force: true })
}
