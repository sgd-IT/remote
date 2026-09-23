import request from '@/config/axios'

// IOT产品分类 VO
export interface CategoryVO {
  id: number // 分类id
  parentId: number // 父分类id
  name: string // 分类名称
  sort: number // 分类排序
  status: number // 开启状态
  imgUrl: string // 图片地址
  isSys: number // 是否系统通用（0-否，1-是）
}

// IOT产品分类 API
export const CategoryApi = {
  // 查询IOT产品分类列表
  getCategoryList: async (params) => {
    return await request.get({ url: `/eiot/category/list`, params })
  },

  // 查询IOT产品分类详情
  getCategory: async (id: number) => {
    return await request.get({ url: `/eiot/category/get?id=` + id })
  },

  // 新增IOT产品分类
  createCategory: async (data: CategoryVO) => {
    return await request.post({ url: `/eiot/category/create`, data })
  },

  // 修改IOT产品分类
  updateCategory: async (data: CategoryVO) => {
    return await request.put({ url: `/eiot/category/update`, data })
  },

  // 删除IOT产品分类
  deleteCategory: async (id: number) => {
    return await request.delete({ url: `/eiot/category/delete?id=` + id })
  },

  // 导出IOT产品分类 Excel
  exportCategory: async (params) => {
    return await request.download({ url: `/eiot/category/export-excel`, params })
  }
}
