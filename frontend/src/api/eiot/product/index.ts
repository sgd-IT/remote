import request from '@/config/axios'

enum Api {


  productConfigEdit = '/eiot/product/config/edit',
  productConfigGet = '/eiot/product/config/getDetail',

  productShowModelEdit = '/eiot/show-model/save',
}
// 物联网产品 VO
export interface ProductVO {
  id: number // 产品id
  name: string // 产品名称
  categoryId: number // 产品分类id
  productKey: string // productKey
  mcuCode: string // mcu code
  remark1: string // 功能介绍
  imgUrl: string // 图片url
  remark: string // 备注
  status: number // 状态（0启用 1禁用）
  nodeType: number // 设备类型(0 网关设备, 1 网关子设备, 2 直连设备, 3 非联网设备 )
  protocolCode: string // 协议code
  keepAliveTime: number // 保活时间
  productSecret: string // 产品密钥
  transparent: boolean // 是否透传
  dnTyp: number // 唯一标识类型(1序列号 2蓝牙mac)
  locateType: number // 定位方式(0无定位, 1自动上报 2手动定位)
}

// 物联网产品 API
export const ProductApi = {
  // 查询物联网产品分页
  getProductPage: async (params: any) => {
    return await request.get({ url: `/eiot/product/page`, params })
  },

  // 查询物联网产品详情
  getProduct: async (id: number) => {
    return await request.get({ url: `/eiot/product/get?id=` + id })
  },

  // 查询物联网产品详情
  getByPk: async (pk: String) => {
    return await request.get({ url: `/eiot/product/getByPk?pk=` + pk })
  },

  // 新增物联网产品
  createProduct: async (data: ProductVO) => {
    return await request.post({ url: `/eiot/product/create`, data })
  },

  // 修改物联网产品
  updateProduct: async (data: ProductVO) => {
    return await request.put({ url: `/eiot/product/update`, data })
  },

  // 删除物联网产品
  deleteProduct: async (id: number) => {
    return await request.delete({ url: `/eiot/product/delete?id=` + id })
  },

  // 导出物联网产品 Excel
  exportProduct: async (params) => {
    return await request.download({ url: `/eiot/product/export-excel`, params })
  },

  getShowModel: async (productKey)=> {
    return await request.get({ url: `/eiot/show-model/getByProductKey?productKey=`+productKey })
  },

    editProductConfig : async (data) => {
    return await request.post({
      url: Api.productConfigEdit,

      data,
    })
  },

    getProductConfig : async (data) => {
    return await request.post({
      url: Api.productConfigGet,
      data,
    })
  },



// 显示模型编辑
  editProductShowModel :async (data) => {
    return await request.post({
      url: Api.productShowModelEdit,
      data,
    })
  }

}
