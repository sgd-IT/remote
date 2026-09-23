// 消防设备控制室 API（企业父设备 / 子设备查询）
import request from '@/config/axios'

/** 物业/企业项（用于企业树展示） */
export interface FirePropertyItem {
  id: number
  name: string
  propertyName?: string
  province?: string
  city?: string
  district?: string
  tenantId?: number
}

/** 设备项（父设备/子设备通用） */
export interface FireDeviceItem {
  id: number
  name: string
  dn: string
  state: number
  addr?: string
  tenantId?: number
  propertyName?: string
  productKey?: string
  serialNo?: string
  /** 设备类型：transfer(用传设备), camera(摄像头) 等 */
  deviceType?: string
  /** 线路类型：0=多线, 1=总线 */
  lineType?: number
}

/** 分页结果（与后端 PageResult 对应） */
export interface FirePageResult<T> {
  list: T[]
  total: number
}

/** 子设备统计（total=总数, online=在线数） */
export interface SubDeviceStats {
  total: number
  online: number
}

enum Api {
  // 物业列表（复用已有接口，admin 看全部、企业看自己）
  propertyList = '/eiot/firewatch/property/list',
  // 企业父设备列表（parentId 为空）
  parentDevices = '/eiot/firewatch/device/list',
  // 父设备绑定的子设备列表（分页，可选 lineTypes 过滤）
  subDevices = '/eiot/firewatch/device/sub-devices',
  // 按线路类型查询子设备（分页）
  subDevicesByLineType = '/eiot/firewatch/device/sub-devices-by-line-type',
  // 子设备统计（总数/在线数）
  subDeviceStats = '/eiot/firewatch/device/sub-devices/stats'
}

/** 获取企业/物业列表（不分页） */
export const getPropertyList = (status?: number): Promise<FirePropertyItem[]> =>
  request.get({ url: Api.propertyList, params: { status } })

/** 按 propertyId 获取该企业的父设备列表 */
export const getParentDevices = (propertyId: number): Promise<FireDeviceItem[]> =>
  request.get({ url: Api.parentDevices, params: { propertyId } })

/**
 * 按 parentId 分页获取父设备绑定的子设备
 * @param lineTypes 线路类型列表（0=多线, 1=总线），不传则不过滤
 */
export const getSubDevices = (
  parentId: number,
  pageNo = 1,
  pageSize = 10,
  lineTypes?: number[]
): Promise<FirePageResult<FireDeviceItem>> =>
  request.get({
    url: Api.subDevices,
    params: { parentId, pageNo, pageSize, lineTypes: lineTypes?.length ? lineTypes.join(',') : undefined }
  })

/** 按 parentId 和 lineType 分页获取子设备（0=多线, 1=总线） */
export const getSubDevicesByLineType = (
  parentId: number,
  lineType: number,
  pageNo = 1,
  pageSize = 10
): Promise<FirePageResult<FireDeviceItem>> =>
  request.get({ url: Api.subDevicesByLineType, params: { parentId, lineType, pageNo, pageSize } })

/** 获取父设备下的子设备统计（总数/在线数） */
export const getSubDeviceStats = (parentId: number): Promise<SubDeviceStats> =>
  request.get({ url: Api.subDeviceStats, params: { parentId } })
