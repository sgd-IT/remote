// 通道配置Api
import request, {IPageResponse} from '@/config/axios'

enum Api {
  list = '/notify/channel/getList',
  getConfigList = '/notify/channel/config/getList',
  getConfigAll = '/notify/channel/config/getAll',
  addConfig = '/notify/channel/config/add',
  updateConfig = '/notify/channel/config/updateById',
  getConfigById = '/notify/channel/config/getById',
  delConfigById = '/notify/channel/config/delById',
  getMsgList = '/notify/message/getList',
}
export interface IChannelConfigsVO {
  id?: number
  channelId: number
  title: string
  param: string
  createTime: number
}
export interface IChannelsVO {
  id?: number
  code: string
  title: string
  icon: string
  createTime: number
}
export interface INotifyMessagesVO {
  id?: number
  content: string
  messageType: string
  status: boolean
  createTime: number
  updateAt: number
}

interface IPage {
  pageNum?: number
  pageSize?: number
}

// 获取通道列表
export const getChannelsList = async (): Promise<IPageResponse<IChannelsVO>> => {
  return await request.post({
    url: Api.list,
    method: 'post',
    data: {},
  })
}

// 获取通道配置分页列表
export const getConfigList = async(data?: IPage): Promise<IPageResponse<IChannelConfigsVO>> => {
  return await request.post({
    url: Api.getConfigList,
    method: 'post',
    data,
  })
}

// 获取通道配置列表
export const getConfigAll = async (): Promise<IChannelConfigsVO[]> => {
  return await request.post({
    url: Api.getConfigAll,
    method: 'post',
    data: {}
  })
}

// 保存修改通道配置
export const addConfig = async (data: IChannelConfigsVO) => {
  return await request.post({
    url: Api.addConfig,
    method: 'post',
    data: data,
  })
}

export const updateConfig = async (data: IChannelConfigsVO) => {
  return await request.post({
    url: Api.updateConfig,
    method: 'post',
    data: data,
  })
}

// 根据ID获取通道配置
export const getConfigById = async (data: string | number): Promise<IChannelConfigsVO> => {
  return await request.post({
    url: Api.getConfigById,
    method: 'post',
    data: data,
  })
}

// 删除通道配置
export const delConfigById = async (id: string | number) => {
  return await request.post({
    url: Api.delConfigById,
    method: 'post',
    data: {id},
  })
}

// 获取消息分页列表
export const getMsgs = async (data?: IPage): Promise<IPageResponse<INotifyMessagesVO>> => {
  return await request.post({
    url: Api.getMsgList,
    method: 'post',
    data: data,
  })
}
