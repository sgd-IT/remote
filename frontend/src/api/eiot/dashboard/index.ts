import request from '@/config/axios'
import { ProductApi } from '@/api/eiot/product/index'

export interface DeviceStats {
  totalDevices: number
  onlineDevices: number
  offlineDevices: number
  alertDevices: number
}

export interface AlertStats {
  totalAlerts: number
  todayAlerts: number
  criticalAlerts: number
  warningAlerts: number
}

export interface ProductStats {
  totalProducts: number
  activeProducts: number
}

export interface DeviceLocation {
  id: number | string
  name: string
  serialNo: string
  lat: number
  lng: number
  status: 'online' | 'offline' | 'error'
  productKey: number | string
  productName: string
  address?: string
}

export interface DashboardStats {
  deviceStats: DeviceStats
  alertStats: AlertStats
  productStats: ProductStats
  deviceLocations: DeviceLocation[]
}

export const getDashboardStats = async (): Promise<DashboardStats> => {
  return await request.get({ url: '/eiot/dashboard/stats' })
}

export const getDeviceStats = async (): Promise<DeviceStats> => {
  return await request.get({ url: '/eiot/dashboard/device-stats' })
}

export const getAlertStats = async (): Promise<AlertStats> => {
  return await request.get({ url: '/eiot/dashboard/alert-stats' })
}

export const getProductStats = async (): Promise<ProductStats> => {
  return await request.get({ url: '/eiot/dashboard/product-stats' })
}

export interface GetDeviceLocationsParams {
  pageNum?: number
  pageSize?: number
}

export const getDeviceLocations = async (params: GetDeviceLocationsParams = {}): Promise<DeviceLocation[]> => {
  const { pageNum = 1, pageSize = 100 } = params
  return await request.get({ 
    url: '/eiot/dashboard/device-locations', 
    params: { pageNum, pageSize } 
  })
}

export const getProducts = async (): Promise<any[]> => {
  try {
    const response = await ProductApi.getProductPage({ pageNum: 1, pageSize: 100})
    return response.rows || []
  } catch (error) {
    console.error('Failed to fetch products:', error)
    return []
  }
}
