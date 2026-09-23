export const BASE_URL = process.env.NODE_ENV === 'development'
  ? 'http://**:48080/admin-api'
  : 'https://**:18080/admin-api'

export default function request(options) {
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
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/index' })
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
