import * as FileApi from '@/api/infra/file'
import CryptoJS from 'crypto-js'
import { UploadRawFile, UploadRequestOptions } from 'element-plus/es/components/upload/src/upload'
import axios from 'axios'

/**
 * 获得上传 URL
 */
export const getUploadUrl = (): string => {
  return import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL + '/infra/file/upload'
}

export const useUpload = () => {
  // 后端上传地址
  const uploadUrl = getUploadUrl()
  // 是否使用前端直连上传
  const isClientUpload = UPLOAD_TYPE.CLIENT === import.meta.env.VITE_UPLOAD_TYPE
  // 重写ElUpload上传方法
  const httpRequest = async (options: UploadRequestOptions) => {
    // 模式一：前端直连上传（仅支持S3等云存储）
    if (isClientUpload) {
      try {
        // 1.1 生成文件名称
        const fileName = await generateFileName(options.file)
        // 1.2 获取文件预签名地址
        const presignedInfo = await FileApi.getFilePresignedUrl(fileName)
        // 1.3 上传文件（不能使用 ElUpload 的 ajaxUpload 方法：其使用 FormData 上传，Minio 不支持）
        await axios.put(presignedInfo.uploadUrl, options.file, {
          headers: {
            'Content-Type': options.file.type
          }
        })
        // 1.4. 记录文件信息到后端（异步，失败不阻断上传成功）
        createFile(presignedInfo, fileName, options.file)
        // 统一返回格式：data 字段为文件访问URL
        return { data: presignedInfo.url }
      } catch (err: any) {
        throw err?.response?.data || err?.message || err
      }
    }

    // 模式二：后端代理上传
    // updateFile 已封装为返回 URL 字符串，此处统一包装为 { data: url } 格式
    return new Promise((resolve, reject) => {
      FileApi.updateFile({ file: options.file, path: '' })
        .then((url) => {
          const realUrl: string = typeof url === 'string' ? url : (url?.data || url?.url || url)
          resolve({ data: realUrl })
        })
        .catch((err) => {
          reject(err?.response?.data || err?.message || err)
        })
    })
  }

  return {
    uploadUrl,
    httpRequest
  }
}

/**
 * 创建文件信息
 * @param vo 文件预签名信息
 * @param name 文件名称
 * @param file 文件
 */
function createFile(vo: FileApi.FilePresignedUrlRespVO, name: string, file: UploadRawFile) {
  const fileVo = {
    configId: vo.configId,
    url: vo.url,
    path: name,
    name: file.name,
    type: file.type,
    size: file.size
  }
  FileApi.createFile(fileVo)
  return fileVo
}

/**
 * 生成文件名称（使用算法SHA256）
 * @param file 要上传的文件
 */
async function generateFileName(file: UploadRawFile) {
  // 读取文件内容
  const data = await file.arrayBuffer()
  const wordArray = CryptoJS.lib.WordArray.create(data)
  // 计算SHA256
  const sha256 = CryptoJS.SHA256(wordArray).toString()
  // 拼接后缀
  const ext = file.name.substring(file.name.lastIndexOf('.'))
  return `${sha256}${ext}`
}

/**
 * 上传类型
 */
enum UPLOAD_TYPE {
  // 客户端直接上传（只支持S3服务）
  CLIENT = 'client',
  // 客户端发送到后端上传
  SERVER = 'server'
}
