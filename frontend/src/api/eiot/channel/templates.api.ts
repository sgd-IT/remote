// 模板配置Api
import request, {IPageResponse} from '@/config/axios'

enum Api {
  list = '/notify/channel/template/getList',
  add = '/notify/channel/template/add',
  update = '/notify/channel/template/updateById',
  del = '/notify/channel/template/delById',
}
export interface IChannelTemplateVo {
  id?: number
  channelConfigId: number
  title: string
  content: string
  createTime: number
  status?: number // 状态：0-待审核，1-审核成功，2-审核失败
  templateCode?: string // 模板编号
  channelCode?: string // 通道编号
  smsTemplateCode?: string // 系统短信模板编码（关联 system_sms_template.code）
}
interface IPage {
  pageNum?: number
  pageSize?: number
}
// 获取模板配置列表
export const getTemplatesList = async (data?: IPage): Promise<IPageResponse<IChannelTemplateVo>> => {
  return await request.post({
    url: Api.list,
    method: 'post',
    data: data,
  })
}

// 保存、编辑模板配置
export const saveTemplate = async (data: IChannelTemplateVo) => {
  return await request.post({
    url: Api.add,
    method: 'post',
    data: data,
  })
}

// 保存、编辑模板配置
export const updateTemplate = async (data: IChannelTemplateVo) => {
  return await request.post({
    url: Api.update,
    method: 'post',
    data: data,
  })
}


// 删除模板配置
export const deleteTemplate = async (id: (string | number)[]) => {
  return await request.post({
    url: Api.del,
    method: 'post',
    data:{id},
  })
}
