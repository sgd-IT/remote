import request from '@/config/axios'

// 产品显示模型 VO
export interface ShowModelVO {
  id: number // 显示id
  cnf: string // 显示内容
  typ: number // 类型 0:详情页, 1配置页
  name: string // 配置名称
  remark: string // 备注
  modelCode: string // 模型code
  status: number // 状态（0正常 1禁用）

  productKey: string //
}

// 产品显示模型 API
export const ShowModelApi = {
  // 查询产品显示模型分页
  getShowModelPage: async (params: any) => {
    return await request.get({ url: `/eiot/show-model/page`, params })
  },

  // 查询产品显示模型详情
  getShowModel: async (id: number) => {
    return await request.get({ url: `/eiot/show-model/get?id=` + id })
  },

  // 新增产品显示模型
  createShowModel: async (data: ShowModelVO) => {
    return await request.post({ url: `/eiot/show-model/create`, data })
  },

  // 修改产品显示模型
  updateShowModel: async (data: ShowModelVO) => {
    return await request.put({ url: `/eiot/show-model/update`, data })
  },

  // 删除产品显示模型
  deleteShowModel: async (id: number) => {
    return await request.delete({ url: `/eiot/show-model/delete?id=` + id })
  },

  // 导出产品显示模型 Excel
  exportShowModel: async (params) => {
    return await request.download({ url: `/eiot/show-model/export-excel`, params })
  }
}
