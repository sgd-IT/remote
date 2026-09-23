import request from '@/config/axios'

// 产品物模型 VO
export interface ThingModelVO {
  id: number // 物模型id
  productKey: string // 产品key
  model: string // 物模型
}

// 产品物模型 API
export const ThingModelApi = {
  // 查询产品物模型分页
  getThingModelPage: async (params: any) => {
    return await request.get({ url: `/eiot/thing-model/page`, params })
  },

  // 查询产品物模型详情
  getThingModel: async (id: number) => {
    return await request.get({ url: `/eiot/thing-model/get?id=` + id })
  },

  getThingModelByProductKey: async (productKey: string) => {
    return await request.get({ url: `/eiot/thing-model/get?productKey=` + productKey })
  },


  // 修改产品物模型
  saveThingModel: async (data: ThingModelVO) => {
    return await request.put({ url: `/eiot/thing-model/save`, data })
  },

  // 删除产品物模型
  deleteThingModel: async (id: number) => {
    return await request.delete({ url: `/eiot/thing-model/delete?id=` + id })
  },

  // 导出产品物模型 Excel
  exportThingModel: async (params) => {
    return await request.download({ url: `/eiot/thing-model/export-excel`, params })
  }
}
