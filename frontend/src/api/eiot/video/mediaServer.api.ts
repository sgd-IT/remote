import request from '@/config/axios'

enum Api {
  getConfig = '/eiot/video/media-server/get',
  saveConfig = '/eiot/video/media-server/save',
  deleteConfig = '/eiot/video/media-server/delete'
}

export const getMediaServerConfig = () => {
  return request.get({ url: Api.getConfig })
}

export const saveMediaServerConfig = (data: any) => {
  return request.post({ url: Api.saveConfig, data })
}

export const deleteMediaServerConfig = (id: number) => {
  return request.delete({ url: Api.deleteConfig, params: { id } })
}
