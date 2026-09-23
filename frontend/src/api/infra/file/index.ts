import request from '@/config/axios'

export interface FilePageReqVO extends PageParam {
  path?: string
  type?: string
  createTime?: Date[]
}

// 文件预签名地址 Response VO
export interface FilePresignedUrlRespVO {
  // 文件配置编号
  configId: number
  // 文件上传 URL
  uploadUrl: string
  // 文件 URL
  url: string
}

// 查询文件列表
export const getFilePage = (params: FilePageReqVO) => {
  return request.get({ url: '/infra/file/page', params })
}

// 删除文件
export const deleteFile = (id: number) => {
  return request.delete({ url: '/infra/file/delete?id=' + id })
}

// 获取文件预签名地址
export const getFilePresignedUrl = (path: string) => {
  return request.get<FilePresignedUrlRespVO>({
    url: '/infra/file/presigned-url',
    params: { path }
  })
}

// 创建文件
export const createFile = (data: any) => {
  return request.post({ url: '/infra/file/create', data })
}

// 上传文件（返回文件URL字符串）
export const updateFile = (data: any) => {
  // 将普通对象转为 FormData，确保 multipart/form-data 正确传输
  const formData = new FormData()
  if (data && typeof data === 'object') {
    Object.keys(data).forEach(key => {
      const value = data[key]
      if (value !== null && value !== undefined) {
        formData.append(key, value)
      }
    })
  }

  return request.upload({ url: '/infra/file/upload', data: formData }).then((res: any) => {
    // 兼容多种后端响应格式，提取文件URL字符串
    if (res && typeof res === 'object') {
      return res.data || res.url || res
    }
    return res
  })
}
