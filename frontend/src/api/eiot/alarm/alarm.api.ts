// 告警Api
import request from '@/config/axios'

enum Api {
  configList = '/alert/selectAlertConfigPage',
  configUpdate = '/alert/updateAlertConfig',
  configAdd = '/alert/createAlertConfig',
  configDelete = '/alert/deleteAlertConfigById',
  msgList = '/alert/selectAlertRecordPage',
}

// 获取告警配置列表
export const getConfigList = (data) => {
  return request.post({
    url: Api.configList,
    method: 'post',
    data,
  })
}
// 告警配置保存
export const saveConfig = (data) => {
  return request.post({
    url: data.id ? Api.configUpdate : Api.configAdd,
    method: 'post',
    data,
  })
}
// 告警配置删除
export const deleteConfig = (id) => {
  return request.post({
    url: Api.configDelete,
    method: 'post',
    data:{id},
  })
}
// 告警消息
export const getMsgList = (data) => {
  return request.post({
    url: Api.msgList,
    method: 'post',
    data,
  })
}
