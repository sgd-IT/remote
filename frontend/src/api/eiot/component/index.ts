import request from '@/config/axios'

export interface ComponentVO {
  id: number
  name: string
  type: number
  config: string
  status: number
  remark: string
  createTime: string
}

export interface ComponentForm {
  id?: number
  name: string
  type: string
  config: string
  status: number
  remark: string
}

// 查询组件列表
export const getComponentList = (params: any) => {
  return request.get({ url: '/eiot/component/page', params })
}

// 查询组件详情
export const getComponent = (id: number) => {
  return request.get({ url: '/eiot/component/get', params: { id } })
}

// 新增组件
export const addComponent = (data: ComponentForm) => {
  return request.post({ url: '/eiot/component/create', data })
}

// 修改组件
export const updateComponent = (data: ComponentForm) => {
  return request.put({ url: '/eiot/component/update', data })
}

// 删除组件
export const delComponent = (id: number) => {
  return request.delete({ url: '/eiot/component/delete', params: { id } })
}
