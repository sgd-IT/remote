export const BASE_URL = process.env.NODE_ENV === 'development'
  ? 'http://192.168.250.130:48080/app-api'
  : 'https://fire-remote-admin.anxunwang.cn:18080/app-api'

// 并发 401 共享同一次刷新，避免刷新令牌被重复消费
let refreshingPromise = null

// 用 refreshToken 换新的 accessToken；失败时区分"确定失效"（后端明确拒绝）与网络异常
function refreshAccessToken() {
  const refreshToken = uni.getStorageSync('refreshToken')
  // 没存过刷新令牌：无需发请求，直接判定登录态失效
  if (!refreshToken) return Promise.reject({ definite: true })
  if (!refreshingPromise) {
    refreshingPromise = new Promise((resolve, reject) => {
      uni.request({
        url: BASE_URL + '/system/auth/refresh-token?refreshToken=' + encodeURIComponent(refreshToken),
        method: 'POST',
        header: {
          // 刷新走的也是租户隔离的接口，带上当前租户头
          'tenant-id': uni.getStorageSync('tenantId') || '1'
        },
        success(res) {
          const body = res.data || {}
          if (res.statusCode === 200 && body.code === 0 && body.data && body.data.accessToken) {
            return resolve(body.data)
          }
          // 后端明确拒绝（刷新令牌过期/无效）：确定要重新登录
          reject({ definite: true })
        },
        // 网络异常：保留登录态，交给下一次请求再试，避免把网络抖动误判成掉登录
        fail: reject
      })
    })
    const clear = () => { refreshingPromise = null }
    refreshingPromise.then(clear, clear)
  }
  return refreshingPromise
}

// 登录态彻底失效：清凭证回登录页；已在登录页时静默，避免残留请求反复 reLaunch 打断输入
function clearAuthToLogin() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('refreshToken')
  const pages = getCurrentPages()
  const route = String((pages[pages.length - 1] || {}).route || '')
  if (route === 'pages/login/index') return
  uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
  uni.reLaunch({ url: '/pages/login/index' })
}

// retried：已刷新重放过一次，防止刷新后仍 401 时无限循环
export default function request(options, retried) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        // 按当前登录用户所属租户传递租户头，未登录/未知时回退系统租户 1
        'tenant-id': uni.getStorageSync('tenantId') || '1',
        'Authorization': uni.getStorageSync('token') ? `Bearer ${uni.getStorageSync('token')}` : '',
        ...options.header
      },
      success(res) {
        if (res.statusCode !== 200) return reject(res)
        const { code, data, msg } = res.data
        if (code === 0) return resolve(data)
        if (code === 401) {
          // accessToken 过期（后端返回「账号未登录」）：先续期再重放一次，
          // refreshToken 也失效才踢回登录页；并发 401 共享同一次刷新
          if (retried) {
            clearAuthToLogin()
            return reject(res.data)
          }
          refreshAccessToken()
            .then(refreshed => {
              uni.setStorageSync('token', refreshed.accessToken)
              if (refreshed.refreshToken) uni.setStorageSync('refreshToken', refreshed.refreshToken)
              request(options, true).then(resolve, reject)
            })
            .catch(err => {
              if (err && err.definite) clearAuthToLogin()
              reject(res.data)
            })
          return
        }
        uni.showToast({ title: msg || '请求失败', icon: 'none' })
        reject(res.data)
      },
      fail: reject
    })
  })
}

export const get = (url, data) => request({ url, data })
export const post = (url, data) => request({ url, method: 'POST', data })
export const put = (url, data) => request({ url, method: 'PUT', data })
