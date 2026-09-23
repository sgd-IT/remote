import request from '@/config/axios'

enum Api {
  bind = '/eiot/video/relation/bind',
  unbind = '/eiot/video/relation/unbind',
  list = '/eiot/video/relation/list',
  relations = '/eiot/video/relation/relations'
}

export const bindDeviceCamera = (deviceId: number, cameraId: number) => {
  return request.post({ url: Api.bind, params: { deviceId, cameraId } })
}

export const unbindDeviceCamera = (id: number) => {
  return request.delete({ url: Api.unbind, params: { id } })
}

export const getCamerasByDevice = (deviceId: number) => {
  return request.get({ url: Api.list, params: { deviceId } })
}

export const getRelationsByDevice = (deviceId: number) => {
  return request.get({ url: Api.relations, params: { deviceId } })
}
