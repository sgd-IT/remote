// 设备影子 API
import request from '@/config/axios'

export interface DeviceShadowVO {
  id: number
  deviceId: number
  productKey: string
  dn: string
  desired: Record<string, any>
  reported: Record<string, any>
  delta: Record<string, any>
  metadata: {
    desired?: Record<string, { timestamp: number }>
    reported?: Record<string, { timestamp: number }>
  }
  version: number
  lastDesiredTime: string
  lastReportedTime: string
  createTime: string
  updateTime: string
}

// 获取设备影子（用于设备详情页）
export const getDeviceShadow = (deviceId: number) => {
  return request.get<DeviceShadowVO>({
    url: '/eiot/device-shadow/get',
    params: { deviceId }
  })
}

// 更新设备影子（下发期望状态）
export const updateDeviceShadow = (
  deviceId: number,
  desired: Record<string, any>,
  version: number
) => {
  return request.post({
    url: '/eiot/device-shadow/update',
    params: { deviceId, version },
    data: desired
  })
}
