// 数据中心 API
import request from '@/config/axios'

enum Api {
  // 告警分析
  alarmTrend = '/eiot/firewatch/data-center/alarm-trend',
  alarmTypeDistribution = '/eiot/firewatch/data-center/alarm-type-distribution',
  alarmPropertyRank = '/eiot/firewatch/data-center/alarm-property-rank',
  alarmHourlyHeatmap = '/eiot/firewatch/data-center/alarm-hourly-heatmap',
  alarmLevelDistribution = '/eiot/firewatch/data-center/alarm-level-distribution',

  // 设备分析
  deviceStatus = '/eiot/firewatch/data-center/device-status',
  deviceCategory = '/eiot/firewatch/data-center/device-category',
  devicePropertyDistribution = '/eiot/firewatch/data-center/device-property-distribution',

  // 值班统计
  dutyStats = '/eiot/firewatch/data-center/duty-stats',
  dutyUserRank = '/eiot/firewatch/data-center/duty-user-rank',

  // 巡检统计
  patrolStats = '/eiot/firewatch/data-center/patrol-stats',
  patrolTrend = '/eiot/firewatch/data-center/patrol-trend',

  // 工单统计
  repairStats = '/eiot/firewatch/data-center/repair-stats',
  hazardStats = '/eiot/firewatch/data-center/hazard-stats',
  workOrderTrend = '/eiot/firewatch/data-center/work-order-trend',
}

// ==================== 告警分析 ====================

export const getAlarmTrend = (days: number = 30, propertyId?: number) => {
  return request.get({ url: Api.alarmTrend, params: { days, propertyId } })
}

export const getAlarmTypeDistribution = (propertyId?: number) => {
  return request.get({ url: Api.alarmTypeDistribution, params: { propertyId } })
}

export const getAlarmPropertyRank = (days: number = 30) => {
  return request.get({ url: Api.alarmPropertyRank, params: { days } })
}

export const getAlarmHourlyHeatmap = (propertyId?: number) => {
  return request.get({ url: Api.alarmHourlyHeatmap, params: { propertyId } })
}

export const getAlarmLevelDistribution = (propertyId?: number) => {
  return request.get({ url: Api.alarmLevelDistribution, params: { propertyId } })
}

// ==================== 设备分析 ====================

export const getDeviceStatus = (propertyId?: number) => {
  return request.get({ url: Api.deviceStatus, params: { propertyId } })
}

export const getDeviceCategory = (propertyId?: number) => {
  return request.get({ url: Api.deviceCategory, params: { propertyId } })
}

export const getDevicePropertyDistribution = () => {
  return request.get({ url: Api.devicePropertyDistribution })
}

// ==================== 值班统计 ====================

export const getDutyStats = (propertyId?: number) => {
  return request.get({ url: Api.dutyStats, params: { propertyId } })
}

export const getDutyUserRank = (year: number, month: number, propertyId?: number) => {
  return request.get({ url: Api.dutyUserRank, params: { year, month, propertyId } })
}

// ==================== 巡检统计 ====================

export const getPatrolStats = (propertyId?: number) => {
  return request.get({ url: Api.patrolStats, params: { propertyId } })
}

export const getPatrolTrend = (days: number = 30, propertyId?: number) => {
  return request.get({ url: Api.patrolTrend, params: { days, propertyId } })
}

// ==================== 工单统计 ====================

export const getRepairStats = (propertyId?: number) => {
  return request.get({ url: Api.repairStats, params: { propertyId } })
}

export const getHazardStats = (propertyId?: number) => {
  return request.get({ url: Api.hazardStats, params: { propertyId } })
}

export const getWorkOrderTrend = (days: number = 30, propertyId?: number) => {
  return request.get({ url: Api.workOrderTrend, params: { days, propertyId } })
}
