import request from '@/config/axios'

enum Api {
  list = '/eiot/rule_engine/tasks/list',
  delete = '/eiot/rule_engine/task/delete',
  save = '/eiot/rule_engine/task/save',
  stop = '/eiot/rule_engine/task/pause',
  resume = '/eiot/rule_engine/task/resume',
  start = '/eiot/rule_engine/task/renew',
  logList = '/eiot/rule_engine/taskLogs/list',
  logClear = '/eiot/rule_engine/taskLogs/clear',
}

export interface ITaskVO {
  actions: string
  config?: string | any[]
  createTime: number
  remark: string
  expression: string
  id: string
  name: string
  reason: string
  state: string
  type: string
  uid: string
}
// 获取列表
export const getTaskList = (data) => {
  return request.post({
    url: Api.list,
    data,
  })
}

// 编辑、保存
export const saveTask = (data) => {
  return request.post({
    url: Api.save,
    data,
  })
}
// 删除
export const deleteTask = (id) => {
  return request.post({
    url: Api.delete,
    data: {id:id},
  })
}
// 停止
export const stopTask = (id) => {
  return request.post({
    url: Api.stop,
    data: {id:id},
  })
}
// 恢复
export const reloadTask = (id) => {
  return request.post({
    url: Api.resume,
    data: {id:id},
  })
}
// 启动
export const startTask = (id) => {
  return request.post({
    url: Api.start,
    data: {id:id},
  })
}

// 获取日志
export const getTaskLog = (data) => {
  return request.post({
    url: Api.logList,
    method: 'post',
    data,
  })
}
// 清空日志
export const clearTaskLog = (id) => {
  return request.post({
    url: Api.logClear,
    method: 'post',
    data: {id},
  })
}
