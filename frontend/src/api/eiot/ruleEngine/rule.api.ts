import request from '@/config/axios'

enum Api {
  list = '/eiot/rule_engine/page',
  saveRule = '/eiot/rule_engine/save',
  delete = '/eiot/rule_engine/delete',
  resume = '/eiot/rule_engine/resume',
  pause = '/eiot/rule_engine/pause',
  logList = '/eiot/rule_engine/ruleLog/list',
  logClear = '/eiot/rule_engine/ruleLog/clear',
}

// 获取列表
export const getRuleList = (data) => {
  return request.post({
    url: Api.list,
    data,
  })
}
// 编辑规则
export const saveRule = (data) => {
  return request.post({
    url: Api.saveRule,
    data,
  })
}
// 删除规则
export const deleteRule = (id) => {
  return request.post({
    url: Api.delete,
    data:{id: id},
  })
}
// 停止
export const pauseRule = (id) => {
  return request.post({
    url: Api.pause,
    data:{id: id},

  })
}

// 恢复
export const resumeRule = (id) => {
  return request.post({
    url: Api.resume,
    data:{id: id},

  })
}

// 获取日志
export const getRulesLog = (data) => {
  return request.post({
    url: Api.logList,
    data,
  })
}
// 清空日志
export const clearRulesLog = (id) => {
  return request.post({
    url: Api.logClear,
    data:{id: id},
  })
}
