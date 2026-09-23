// 品牌管理API
import request from '@/config/axios'

export interface BrandVO {
  id: number
  brandCode: string
  name: string
  createTime: string
  updateTime: string
}

export interface BrandModelVO {
  id: number
  brandId: number
  model: string
  version: string
  createTime: string
  updateTime: string
}

export interface BrandModelFunctionVO {
  id: number
  brandModelId: number
  functionId: string
  name: string
  ruleNum: number
  awaitTime: number
}

/** 获取品牌分页列表 */
export const getBrandPage = async (params: any) => {
  return await request.get({
    url: '/eiot/brand/page',
    params
  })
}

/** 同步品牌数据 */
export const syncBrandData = async () => {
  return await request.post({
    url: '/eiot/brand/sync'
  })
}

/** 获取品牌级联数据 */
export const getBrandCascade = async () => {
  return await request.get({
    url: '/eiot/brand/cascade'
  })
}

/** 获取型号下功能列表 */
export const getBrandModelFunctions = async (brandModelId: number) => {
  return await request.get({
    url: `/eiot/brand/model/${brandModelId}/function`
  })
}
