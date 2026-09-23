/**
 * 海康 dhflv / dhhls 播放器动态加载器
 *
 * 解决 Vite 开发服务器 sirv 中间件在 Node.js 18+ 上
 * 对 public/ 目录静态文件 Content-Length 计算错误（ERR_CONTENT_LENGTH_MISMATCH）
 * 导致全局 script 标签加载失败的问题。
 *
 * 改用 Vite ?url asset 管道引入，Content-Length 由 Vite asset 中间件正确计算。
 */
// Vite asset URL import - served through Vite asset pipeline
import dhflvUrl from '@/assets/lib/dhflv.min.js?url'
import dhhlsUrl from '@/assets/lib/dhhls.min.js?url'

const loadScript = (src: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    // 如果已经存在相同 src 的 script 标签，跳过
    const existing = document.querySelector(`script[src="${src}"]`)
    if (existing) {
      resolve()
      return
    }
    const el = document.createElement('script')
    el.type = 'text/javascript'
    el.src = src
    el.onload = () => resolve()
    el.onerror = () => reject(new Error(`Failed to load script: ${src}`))
    document.head.appendChild(el)
  })
}

let loadPromise: Promise<void> | null = null

/**
 * 确保 dhflv 和 dhhls 播放器脚本已加载到全局。
 * 多次调用安全，只会加载一次。
 */
export const ensurePlayersLoaded = (): Promise<void> => {
  if (!loadPromise) {
    loadPromise = Promise.all([
      loadScript(dhflvUrl),
      loadScript(dhhlsUrl)
    ]).then(() => {})
  }
  return loadPromise
}
