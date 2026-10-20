// 设备远程控制API
import request from '@/config/axios'

// ========== 设备远程配置 ==========
export interface DeviceRemoteConfigVO {
  id?: number
  deviceId: number
  brandModelId?: number
  brandName?: string
  model?: string
  version?: string
  hasPassword?: boolean
  createTime?: string
  updateTime?: string
}

export interface DeviceRemoteConfigReqVO {
  brandModelId: number
  confirmPassword?: string
}

// ========== 设备功能 ==========
export interface DeviceFunctionVO {
  id: string | number
  deviceId: string | number
  brandModelFunctionId: number
  functionName: string
  buttonName: string
  sortOrder: number
  paramValues: string | Record<string, any>
  enabled: boolean
  subDeviceId?: string | number
  functionType?: 'BUS' | 'MULTI' | 'GENERAL'
  createTime?: string
  updateTime?: string
}

export interface DeviceFunctionSaveReqVO {
  brandModelFunctionId: number
  buttonName?: string
  paramValues: Record<string, any>
  subDeviceId?: number
  functionType: 'BUS' | 'MULTI' | 'GENERAL'
}

// ========== 指令下发 ==========
export interface InstructionSendReqVO {
  deviceId: string | number
  deviceFunctionId: string | number
  password?: string
  commandType?: string
}

export interface InstructionSendRespVO {
  success: boolean
  message: string
  taskId?: string
}

// ========== 告警记录 ==========
export interface AlertRecordVO {
  id: number
  /** 告警名称（火警/故障等） */
  name?: string
  /** 告警等级/类型编码（双读）：新数据=等级 1/2/3（1 最急）；旧数据=类型编码 1-5 */
  level?: string
  /** 报警类型（新语义）：1=火警 2=故障 3=隐患 4=离线 5=误报 6=信息；NULL=过渡期旧数据 */
  alarmKind?: number
  alertTime: number
  /** 告警详情 */
  details?: string
  /** 报警类型：0=主机报警 1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警 */
  alarmType?: number
  /** 是否已读 */
  readFlg?: boolean
  tenantId?: number
  deviceId?: number
  productKey?: string
  deviceName?: string
  productName?: string
  /** 设备位置（暂未提供，占位） */
  address?: string
}

export const getAlertRecordPage = async (params: any) => {
  return await request.post({ url: '/alert/selectAlertRecordPage', data: params })
}

export const confirmAlertRecord = async (id: number | string) => {
  return await request.post({ url: '/alert/confirmAlertRecord', data: { id } })
}

/** 获取设备远程配置 */
export const getDeviceRemoteConfig = async (deviceId: string | number) => {
  return await request.get({
    url: `/eiot/device/${deviceId}/remote-config`
  })
}

/** 保存设备远程配置 */
export const saveDeviceRemoteConfig = async (deviceId: string | number, data: DeviceRemoteConfigReqVO) => {
  return await request.post({
    url: `/eiot/device/${deviceId}/remote-config`,
    data
  })
}

/** 获取设备已绑定功能列表 */
export const getDeviceFunctions = async (deviceId: string | number) => {
  return await request.get({
    url: `/eiot/device/${deviceId}/function`
  })
}

/** 新增设备功能 */
export const addDeviceFunction = async (deviceId: string | number, data: DeviceFunctionSaveReqVO) => {
  return await request.post({
    url: `/eiot/device/${deviceId}/function`,
    data
  })
}

/** 修改设备功能 */
export const updateDeviceFunction = async (deviceId: string | number, id: string | number, data: DeviceFunctionSaveReqVO) => {
  return await request.put({
    url: `/eiot/device/${deviceId}/function/${id}`,
    data
  })
}

/** 删除设备功能 */
export const deleteDeviceFunction = async (deviceId: string | number, id: string | number) => {
  return await request.delete({
    url: `/eiot/device/${deviceId}/function/${id}`
  })
}

// ========== 设备实时属性 ==========
export interface DevicePropertyVO {
  identifier: string
  name?: string
  value?: string
  time?: string
}

export interface DeviceInfoWithPropertyVO {
  deviceId: number
  productKey: string
  dn: string
  identifier2property: Record<string, DevicePropertyVO>
  online: boolean
  onlineTime?: number
  offlineTime?: number
}

/** 查询设备实时属性（含状态聚合属性） */
export const getDeviceWithProperty = async (deviceId: string | number): Promise<DeviceInfoWithPropertyVO> => {
  return await request.post({
    url: '/eiot/device/getDeviceWithProperty',
    data: { deviceId }
  })
}

/** 下发控制指令 */
export const sendInstruction = async (data: InstructionSendReqVO) => {
  return await request.post({
    url: '/eiot/device/instruction/send',
    data
  })
}
