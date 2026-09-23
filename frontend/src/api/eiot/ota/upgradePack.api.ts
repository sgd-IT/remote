import request from '@/config/axios'

enum Api {
  list = '/eiot/ota/package/getList',
  del = '/eiot/ota/package/delById',
  add = '/eiot/ota/package/add',
  upgrade = '/eiot/ota/device/upgrade',
  upload = '/eiot/ota/package/upload',
  devices = '/eiot/device/list',
  lot = '/eiot/ota/device/info',
  result = '/eiot/ota/device/detail',
}

// 获取列表
export const getUpgradePack = (data) => {
  return request.post({
    url: Api.list,
    data,
  })
}

// 删除
export const delUpgradePack = (data) => {
  return request.post({
    url: Api.del,
    data,
  })
}
// 新增
export const addUpgradePack = (data) => {
  return request.post({
    url: Api.add,
    data,
  })
}
// 升级
export const upgradeUpgradePack = (data) => {
  return request.post({
    url: Api.upgrade,
    data,
  })
}
// 上传
export const uploadUpgradePack = (data) => {
  return request.post({
    url: Api.upload,
    data,
  })
}
// 结果
export const resultUpgradePack = (data) => {
  return request.post({
    url: Api.result,
    data,
  })
}

// 获取升级包设备
// export const getDevices = (data) => {
//   return request.post({
//     url: Api.devices,
//     data,
//   })
// }

// 查询设备信息分页
export const getDevices= (params: any) => {
  return  request.get({ url: `/eiot/device/page`, params })
}
// 设备升级批次查询
export const getUpgradePackLot = (data) => {
  return request.post({
    url: Api.lot,
    data,
  })
}
