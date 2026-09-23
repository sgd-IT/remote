import request from '@/config/axios'

// 设备信息 VO
export interface DeviceInfoVO {
  id: number // 设备id
  dn: string // 设备唯一标识
  productKey: string // 产品key
  deptId: number // 机构id
  propertys: string // 设备属性
  name: string // 别名
  state: number // 在线(0:否, 1:在线, 2未激活, 3禁用)
  offlineTime: number // 离线时间
  onlineTime: number // 在线时间
  serialNo: string // 设备序列号
  remark: string // 备注
  lat: number // 经纬度
  lon: number // 经纬度
}

// 设备信息 API
export const DeviceInfoApi = {
  // 查询设备信息分页
  getDeviceInfoPage: async (params: any) => {
    return await request.get({ url: `/eiot/device/page`, params })
  },

  // 查询设备信息详情
  getDeviceInfo: async (id: number) => {
    return await request.get({ url: `/eiot/device/get?id=` + id })
  },

  // 查询设备信息详情
  getDeviceProperties: async (id: number) => {
    return await request.post({ url: `/eiot/device/getDeviceWithProperty`, data:{deviceId:id}})
  },

  // 新增设备信息
  createDeviceInfo: async (data: DeviceInfoVO) => {
    return await request.post({ url: `/eiot/device/create`, data })
  },

  // 修改设备信息
  updateDeviceInfo: async (data: DeviceInfoVO) => {
    return await request.put({ url: `/eiot/device/update`, data })
  },

  // 删除设备信息
  deleteDeviceInfo: async (id: number) => {
    return await request.delete({ url: `/eiot/device/delete?id=` + id })
  },

  // 导出设备信息 Excel
  exportDeviceInfo: async (params) => {
    return await request.download({ url: `/eiot/device/export-excel`, params })
  },

  getDeviceBySerialNumber: async (serialNo:string)=>{
    return await request.get({ url: `/eiot/device/getDeviceBySerialNumber/` + serialNo })
  },

  deleteByIds: async (ids: any[]) =>{
    return await request.delete({ url: `/eiot/device/deleteBatch?ids=` + ids })
  }
  ,
  genSerialNo: async (nodeType: Number)=>{
    return await request.get({ url: `/eiot/device/genSerialNO?nodeType=`+nodeType })
  },

  // 批量分配租户
  batchAssignTenant: async (data: { deviceIds: number[]; tenantId: number }) => {
    return await request.post({ url: `/eiot/device/batchAssignTenant`, data })
  }
}
