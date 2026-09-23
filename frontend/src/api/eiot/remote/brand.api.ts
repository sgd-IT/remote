// 品牌远程控制API
import request from '@/config/axios'

// ========== 品牌级联数据 ==========
export interface BrandCascadeVO {
  id: number
  name: string
  brandCode: string
  models?: BrandModelVO[]
}

export interface BrandModelVO {
  id: number
  model: string
  versions: string[]
}

export interface BrandModelFunctionVO {
  id: number
  brandModelId: number
  functionId: string
  name: string
  ruleNum: number
  awaitTime: number
  params: BrandModelFunctionParamVO[]
}

export interface BrandModelFunctionParamVO {
  id: number
  paramCode: string
  paramName: string
  paramType: string
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
