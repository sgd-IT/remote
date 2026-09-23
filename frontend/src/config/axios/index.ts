import { service } from './service'

import { config } from './config'

const { default_headers } = config

const request = (option: any) => {
  const { headersType, headers, ...otherOption } = option
  // FormData 不手动设 Content-Type，让浏览器自动带 boundary；其他情况用指定的或默认类型
  const hasContentType = headersType || !(otherOption.data instanceof FormData)
  return service({
    ...otherOption,
    headers: {
      ...(hasContentType ? { 'Content-Type': headersType || default_headers } : {}),
      ...headers
    }
  })
}

const uploadInterceptor = (option: any) => {
  // 普通对象自动转为 FormData，确保文件正确序列化
  if (option.data && typeof option.data === 'object' && !(option.data instanceof FormData)) {
    const formData = new FormData()
    Object.keys(option.data).forEach(key => {
      const value = option.data[key]
      if (value !== null && value !== undefined) {
        formData.append(key, value)
      }
    })
    option.data = formData
  }
  // 非 FormData 时需要手动设置 Content-Type
  if (!(option.data instanceof FormData)) {
    option.headersType = 'multipart/form-data'
  }
  return option
}

export default {
  get: async <T = any>(option: any) => {
    const res = await request({ method: 'GET', ...option })
    return res.data as unknown as T
  },
  post: async <T = any>(option: any) => {
    const res = await request({ method: 'POST', ...option })
    return res.data as unknown as T
  },
  postOriginal: async (option: any) => {
    const res = await request({ method: 'POST', ...option })
    return res
  },
  delete: async <T = any>(option: any) => {
    const res = await request({ method: 'DELETE', ...option })
    return res.data as unknown as T
  },
  put: async <T = any>(option: any) => {
    const res = await request({ method: 'PUT', ...option })
    return res.data as unknown as T
  },
  download: async <T = any>(option: any) => {
    const res = await request({ method: 'GET', responseType: 'blob', ...option })
    return res as unknown as Promise<T>
  },
  upload: async <T = any>(option: any) => {
    option = uploadInterceptor(option)
    const res = await request({ method: 'POST', ...option })
    return res as unknown as Promise<T>
  }
}
