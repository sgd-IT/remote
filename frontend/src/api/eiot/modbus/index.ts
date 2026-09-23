// modbusInfo api
import request from '@/config/axios'

enum Api {
  list = '/eiot/modbus/list',
  add = '/eiot/modbus/add',
  edit = '/eiot/modbus/edit',
  delete = '/eiot/modbus/deleteModbus',
  exportDeviceTemplate = '/eiot/modbus/exportData',
  objectModelList = '/eiot/modbus/getThingModelByProductKey',
  saveObjectModel = '/eiot/modbus/thingModel/save',
  syncToProduct = '/eiot/modbus/syncToProduct',
}
export interface ModBusInfoVO {
  id?: number
  name: string
  productKey: string
  remark: string
}
interface IPage {
  pageNo?: number
  pageSize?: number
}

// 物联网产品 API
export const ModBusInfoApi = {
    // 获取列表
    getModbusList : async (data?: IPage) => {
        return await request.post({url: Api.list, data});
    },

    // 编辑、保存
    saveModbus : async (data: ModBusInfoVO) => {
        return await request.post({url: !data.id ? Api.add : Api.edit, data});
    },
    deleteModbus : async (data) => {
        return await request.post({url: Api.delete + `?id=` + data});
    },

    exportDeviceTemplate : () => {
        return request.download({url: Api.exportDeviceTemplate});
    },
    // 获取物模型
    getObjectModel : async (data: string | number) => {
        return await request.post({url: Api.objectModelList + `?pk=` + data});
    },

    // 保存物模型
    saveObjectModel : async (data) => {
        return await request.post({url: Api.saveObjectModel, data});
    },

    syncToProduct : async (data) => {
        return await request.postOriginal({url: Api.syncToProduct, data});
    },
}