// 设备Api
import request from '@/config/axios'

enum Api {
  devicesList = '/eiot/device/page',
  devicesDel = '/eiot/device/delete',
  devicesBatchDel = '/eiot/device/batchDelete',
  devicesAdd = '/eiot/device/add',
  devicesUpdate = '/eiot/device/save',
  devicesBind = '/eiot/device/bind',
  devicesUnbind = '/eiot/device/unbind',
  devicesDetail = '/eiot/device/detail',
  devicesTagAdd = '/eiot/device/tag/add',
  deviceConfigSave = '/eiot/device/config/save',
  deviceSimulateSend = '/eiot/device/simulateSend',
  devicePropertyLogs = '/eiot/device/deviceProperty/log/list',
  deviceConfigGet = '/eiot/device/config/get',
  deviceConfigSend = '/eiot/device/config/send',
  propertySet = '/eiot/device/service/property/set',
  propertyGet = '/eiot/device/service/property/get',
  deviceLogs = '/eiot/device/deviceLogs/list',
  serviceInvoke = '/eiot/device/service/invoke',
  childrenDeviceList = '/eiot/device/children/list',
  unbindDeviceList = '/eiot/device/children/unbindList',
  childrenDevice = '',
  getParentDevices = '/eiot/device/getParentDevices',

  deviceGroupsList = '/eiot/device/groups/list',
  deviceGroupsAdd = '/eiot/device/group/add',
  deviceGroupsUpdate = '/eiot/device/group/edit',
  deviceGroupsDel = '/eiot/device/group/delete',
  deviceGroupsClear = '/eiot/device/group/clear',
  deviceGroupsAddDevices = '/eiot/device/group/addDevices',
  deviceGroupsRemoveDevices = '/eiot/device/group/removeDevices',

}

export interface IDeviceGroupVo {
  id: number | string
  createTime: number
  deviceQty: number
  name: string
  remark: string
  uid: string
}


// 解绑
export const getDevicesList = async (data) => {
  return await request.get({
    url: Api.devicesList,
    params: data,
  })
}

// 解绑
export const bindDevice = async (data) => {
  return await request.post({
    url: Api.devicesBind,

    data,
  })
}

// 解绑
export const unbindDevice = async (data) => {
  return await request.post({
    url: Api.devicesUnbind,

    data,
  })
}

// 添加设备标签
export const devicesTagAdd = async (data) => {
  return await request.post({
    url: Api.devicesTagAdd,

    data,
  })
}

// 模拟上报
export const deviceSimulateSend = async (data) => {
  return await request.post({
    url: Api.deviceSimulateSend,

    data,
  })
}
// 设备属性设置
export const propertySet = async (data) => {
  return await request.post({
    url: Api.propertySet,

    data,
  })
}

export const propertyGet = async (data) => {
  return await request.post({
    url: Api.propertyGet,

    data,
  })
}

// 设备日志查询
export const deviceLogs = async (data) => {
  return await request.post({
    url: Api.deviceLogs,
    data,
  })
}
// 设备服务调用
export const serviceInvoke = async (data) => {
  return await request.post({
    url: Api.serviceInvoke,

    data,
  })
}
// 获取子设备列表
export const getChildrenDeviceList = async (data) => {
  return await request.post({
    url: Api.childrenDeviceList,

    data,
  })
}

// 获取未绑定的子设备列表
export const getUnbindDeviceList = async (data) => {
  return await request.post({
    url: Api.unbindDeviceList,

    data,
  })
}
// 设备配置保存
export const saveDeviceConfig = async (data) => {
  return await request.post({
    url: Api.deviceConfigSave,

    data,
  })
}
// 获取设备配置
export const getDeviceConfig = async (id) => {
  return await request.post({
    url: Api.deviceConfigGet,
    data: {deviceId: id},
  })
}
// 设备配置下发
export const sendDeviceConfig = async (data) => {
  return await request.post({
    url: Api.deviceConfigSend,

    data,
  })
}

// 获取属性日志
export const devicePropertyLogs = async (data) => {
  return await request.post({
    url: Api.devicePropertyLogs,

    data,
  })
}

// 获取网关设备
export const getParentDevices = async () => {
  return await request.post({
    url: Api.getParentDevices,

  })
}

// 获取设备分组列表
export const getDeviceGroupsList = async (data)=> {
  return await request.post({
    url: Api.deviceGroupsList,

    data,
  })
}

// 编辑、保存设备分组
export const saveDeviceGroup = async (data: IDeviceGroupVo) => {
  return await request.post({
    url: data.id ? Api.deviceGroupsUpdate : Api.deviceGroupsAdd,

    data,
  })
}
// 获取导入设备模板
export const getDeviceTemplate= async ()=>{
  return await request.download({
    url: "/eiot/device/exportData"
  })
}

// 获取导入分组模板
export const getGroupTemplate= async ()=>{
  return await request.download({
    url: "/eiot/device/group/exportData"
  })
}
// 删除设备分组
export const deleteDeviceGroup = async (data) => {
  return await request.post({
    url: Api.deviceGroupsDel,
    data,
  })
}

// 删除设备分组
export const clearDeviceGroup = async (data: string) => {
  return await request.post({
    url: Api.deviceGroupsClear,

    data,
  })
}

// 清空组下所有设备
export const addDevicesGroup = async (data: string) => {
  return await request.post({
    url: Api.deviceGroupsClear,

    data,
  })
}

// 添加设备到组
export const addDeviceToDeviceGroup = async (data) => {
  return await request.post({
    url: Api.deviceGroupsAddDevices,

    data,
  })
}

// 将设备从组中移除
export const removeDeviceFromDeviceGroup = async (data) => {
  return await request.post({
    url: Api.deviceGroupsRemoveDevices,

    data,
  })
}
