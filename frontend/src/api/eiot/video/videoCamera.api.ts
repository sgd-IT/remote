import request from '@/config/axios'

enum Api {
  create = '/eiot/video/camera/create',
  update = '/eiot/video/camera/update',
  delete = '/eiot/video/camera/delete',
  get = '/eiot/video/camera/get',
  list = '/eiot/video/camera/list',
  page = '/eiot/video/camera/page',
  tree = '/eiot/video/camera/tree',
  liveUrl = '/eiot/video/camera/live/url',
  iccDevices = '/eiot/video/camera/icc-devices',
  batchCreate = '/eiot/video/camera/batch-create',
  tenantOptions = '/eiot/video/camera/tenant-options'
}

export const createVideoCamera = (data: any) => {
  return request.post({ url: Api.create, data })
}

export const updateVideoCamera = (data: any) => {
  return request.put({ url: Api.update, data })
}

export const deleteVideoCamera = (id: number) => {
  return request.delete({ url: Api.delete, params: { id } })
}

export const getVideoCamera = (id: number) => {
  return request.get({ url: Api.get, params: { id } })
}

export const getVideoCameraList = () => {
  return request.get({ url: Api.list })
}

export const getVideoCameraPage = (params: any) => {
  return request.get({ url: Api.page, params })
}

export const getLiveUrl = (cameraId: number): Promise<{ url: string; flvUrl: string }> => {
  return request.get({ url: Api.liveUrl, params: { cameraId } })
}

export const getIccDevices = () => {
  return request.get({ url: Api.iccDevices })
}

/** 批量创建摄像头（ICC 自动获取，仅平台租户），可指定归属租户 */
export const batchCreateVideoCamera = (data: { tenantId: number; cameras: any[] }) => {
  return request.post({ url: Api.batchCreate, data })
}

/** 获取可分配摄像头的租户列表（仅平台租户） */
export const getTenantOptions = () => {
  return request.get({ url: Api.tenantOptions })
}

/** 获取摄像头树（按企业分组） */
export const getCameraTree = () => {
  return request.get({ url: Api.tree })
}
