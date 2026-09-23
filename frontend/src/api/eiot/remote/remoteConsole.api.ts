// 远程控制台 API
import request from '@/config/axios'

// ========== 类型定义 ==========

/** 设备树节点 - 企业/物业 */
export interface DeviceTreeNodeVO {
  id: string | number
  name: string
  riskLevel: 'danger' | 'warning' | 'normal'
  deviceCount: number
  devices: DeviceTreeItemVO[]
}

/** 设备树节点 - 设备 */
export interface DeviceTreeItemVO {
  id: string | number
  name: string
  productKey?: string
  /** 品牌代码（主机品牌） */
  brandCode?: string
  /** 设备型号（主机型号） */
  model?: string
  state: number // 0=离线 1=在线
  alarmStatus?: 'alarm' | 'fault' | 'online' | 'offline'
  icon?: string
}

/** 设备实时参数 */
export interface DeviceParamVO {
  code: string
  name: string
  value: number | string
  unit: string
  icon?: string
}

/** 操作日志 */
export interface OperationLogVO {
  id: number
  deviceId?: number
  deviceName?: string
  /** 操作类型: MUTE/RESET/SWITCH_MODE/START/STOP/OTHER */
  operationType?: string
  /** 操作名称 */
  operationName?: string
  /** 按钮名称 */
  buttonName?: string
  /** 功能类型: BUS(总线)/MULTI(多线)/GENERAL(通用) */
  functionType?: string
  operatorId?: number
  /** 操作人姓名 */
  operatorName?: string
  reviewerId?: number
  reviewerName?: string
  /** 命令状态: 0=失败 1=成功 2=执行中 */
  commandStatus?: number
  /** 结果说明 */
  resultMsg?: string
  /** 操作时间 */
  operationTime?: string
  /** 兼容旧字段 */
  time?: string
  content?: string
  type?: string
}

/** 设备状态统计 */
export interface DeviceStatusStatsVO {
  total: number
  online: number
  alarm: number
  fault: number
  offline: number
}

/** 设备详情 */
export interface DeviceDetailVO {
  id: string | number
  name: string
  productKey?: string
  state: number
  alarmStatus?: string
  params: DeviceParamVO[]
  stats?: DeviceStatusStatsVO
  mode?: 'manual' | 'auto'
}

/** 摄像头信息 */
export interface CameraVO {
  id: string | number
  channelId?: string
  channelName?: string
  status?: number
  address?: string
}

/** 右上角展示人员 */
export interface ConsolePersonVO {
  userId?: number
  userName?: string
  userPhone?: string
  position?: string
  source?: string
}

// ========== API 接口 ==========

/** 获取设备树（按企业/物业分组） */
export const getDeviceTree = async (): Promise<DeviceTreeNodeVO[]> => {
  return await request.get({ url: '/eiot/remote-console/device-tree' })
}

/** 获取设备详情 */
export const getDeviceDetail = async (deviceId: number): Promise<DeviceDetailVO> => {
  return await request.get({ url: `/eiot/remote-console/device/${deviceId}/detail` })
}

/** 获取设备实时参数 */
export const getDeviceParams = async (deviceId: number): Promise<DeviceParamVO[]> => {
  return await request.get({ url: `/eiot/remote-console/device/${deviceId}/params` })
}

/** 获取设备操作日志 */
export const getOperationLogs = async (deviceId: string | number, limit: number = 20) => {
  return await request.get({
    url: `/eiot/remote-console/device/${deviceId}/operation-logs`,
    params: { limit }
  })
}

/** 获取设备状态统计 */
export const getDeviceStatusStats = async (): Promise<DeviceStatusStatsVO> => {
  return await request.get({ url: '/eiot/remote-console/device-status-stats' })
}

/** 获取右上角展示人员：展示岗位人员（按岗位顺序）+ 当前值班人员（去重） */
export const getConsolePersons = async (tenantId: string | number): Promise<ConsolePersonVO[]> => {
  return await request.get({
    url: '/eiot/remote-console/console-persons',
    params: { tenantId }
  })
}

/** 切换手自动模式 */
export const switchDeviceMode = async (deviceId: number, mode: 'manual' | 'auto', reason?: string) => {
  return await request.post({
    url: `/eiot/remote-console/device/${deviceId}/mode`,
    data: { mode, reason }
  })
}

/** 获取设备绑定的摄像头列表 */
export const getDeviceCameras = async (deviceId: string | number): Promise<CameraVO[]> => {
  return await request.get({ url: '/eiot/video/relation/list', params: { deviceId } })
}

/** 获取摄像头实时流地址 */
export const getCameraLiveUrl = async (cameraId: string | number): Promise<{ url: string }> => {
  return await request.get({ url: '/eiot/video/camera/live/url', params: { cameraId } })
}

/** 后台管理 - 操作日志分页 */
export const getOperationLogPage = async (params: any) => {
  return await request.get({ url: '/eiot/remote-console/operation-log/page', params })
}

/** 后台管理 - 操作日志导出 */
export const exportOperationLog = async (params: any) => {
  return await request.download({ url: '/eiot/remote-console/operation-log/export', params })
}

/** 智能空开控制（开闸/合闸） */
export const valveControl = async (action: 'open' | 'close', deviceId: string | number) => {
  return await request.post({
    url: `/eiot/remote-console/valve/${action}`,
    data: { deviceId }
  })
}
