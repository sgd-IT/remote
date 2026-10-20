import request from '@/config/axios'

export interface ApiAccessLogVO {
  id: number
  traceId: string
  userId: number
  userType: number
  applicationName: string
  requestMethod: string
  requestParams: string
  responseBody: string
  requestUrl: string
  userIp: string
  userAgent: string
  operateModule: string
  operateName: string
  operateType: number
  beginTime: Date
  endTime: Date
  duration: number
  resultCode: number
  resultMsg: string
  createTime: Date
}

// 查询列表API 访问日志
export const getApiAccessLogPage = (params: PageParam) => {
  return request.get({ url: '/infra/api-access-log/page', params })
}

// API 使用统计（仅超级管理员）：接口调用次数口径
export interface ApiAccessLogStatisticsVO {
  summary: { totalCount: number; userCount: number; urlCount: number; dailyAvg: number }
  trend: Array<{ date: string; callCount: number }>
  urlRank: Array<{ requestUrl: string; callCount: number; userCount: number }>
  urlRankBottom: Array<{ requestUrl: string; callCount: number; userCount: number }>
  userRank: Array<{ userId: number; nickname?: string; callCount: number }>
}

export const getApiAccessLogStatistics = (params: {
  beginTime?: string
  endTime?: string
  top?: number
}) => {
  return request.get<ApiAccessLogStatisticsVO>({ url: '/infra/api-access-log/statistics', params })
}

// 导出API 访问日志
export const exportApiAccessLog = (params) => {
  return request.download({ url: '/infra/api-access-log/export-excel', params })
}
