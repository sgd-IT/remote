// 虚拟设备Api
import request from '@/config/axios'

enum Api {
  list = '/eiot/virtual_device/list',
  add = '/eiot/virtual_device/add',
  delete = '/eiot/virtual_device/delete',
  batchDelete = '/eiot/virtual_device/batchDelete',
  detail = '/eiot/virtual_device/getDetail',
  update = '/eiot/virtual_device/update',
  run = '/eiot/virtual_device/run',
  set = '/eiot/virtual_device/setState',
  saveDevices = '/eiot/virtual_device/saveDevices',
  saveScript = '/eiot/virtual_device/saveScript',
  getLogs = '/eiot/virtual_device/logs/list',
}

export interface IVirtualDevicesVO {
  createTime: number
  devices: any[]
  id: string
  name: string
  productKey: string
  script: string
  state: string
  trigger: string
  triggerExpression: string
  type: string
  uid: string
}
export interface IChangeStateVO {
  id: string
  state: string
}
interface IPage {
  pageNum?: number
  pageSize?: number
}

// 将设备从组中移除
export const removeDeviceFromDeviceGroup = async (data) => {
  return await request.post({
    url: Api.deviceGroupsRemoveDevices,

    data,
  })
}

export const getVirtualDevicesList = async (data) => {
  return request.post({
    url: Api.list,
    data,
  })
}
// 删除
export const deleteVirtualDevices = (data: (string | number)[]) => {
  return request.post({
    url: Api.delete,
    method: 'post',
    data,
  })
}
// 删除
export const batchDeleteVirtualDevices = (data: (string | number)[]) => {
  return request.post({
    url: Api.batchDelete,
    method: 'post',
    data,
  })
}
// 编辑、保存
export const saveVirtualDevices = (data: IVirtualDevicesVO) => {
  return request.post({
    url: data.id ? Api.update : Api.add,
    method: 'post',
    data,
  })
}

// 获取详情
export const getVirtualDevicesDetail = (virtualDeviceId) => {
  return request.get({
    url: Api.detail,
    method: 'get',
    params:{
      id: virtualDeviceId
    }
  })
}

// 设置状态
export const setVirtualDeviceState = (data: IChangeStateVO) => {
  return request.post({
    url: Api.set,
    method: 'post',
    data,
  })
}

// 手动执行
export const runVirtualDevices = (virtualDeviceId: (string | number)[]) => {
  return request.post({
    url: Api.run,
    method: 'post',
    data: {
      id: virtualDeviceId
    },
  })
}

// 保存关联设备
export const saveVirtualDevicesBindDevices = (data) => {
  return request.post({
    url: Api.saveDevices,
    method: 'post',
    data,
  })
}


// 保存脚本
export const saveVirtualDevicesScript = (data) => {
  return request.post({
    url: Api.saveScript,
    method: 'post',
    data,
  })
}

// 取虚拟设备执行日志
export const getVirtualDevicesLogs =(data) => {
  return request.post({
    url: Api.getLogs,
    method: 'post',
    data,
  })
}