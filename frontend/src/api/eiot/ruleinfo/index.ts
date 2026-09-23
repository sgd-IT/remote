import request from '@/config/axios'

// 规则引擎 VO
export interface RuleInfoVO {
  id: number // 规则id
  name: string // 规则名称
  listeners: string // 监听器
  filters: string // 过滤器
  action: string // 动作
  typ: string // 类型(1数据流转 2场景联动)
  status: number // 状态(0启用 1禁用)
  desc: string // 描述
  deptId: number // 机构id
}

// 规则引擎 API
export const RuleInfoApi = {
  // 查询规则引擎分页
  getRuleInfoPage: async (params: any) => {
    return await request.get({ url: `/eiot/rule-info/page`, params })
  },

  // 查询规则引擎详情
  getRuleInfo: async (id: number) => {
    return await request.get({ url: `/eiot/rule-info/get?id=` + id })
  },

  // 新增规则引擎
  createRuleInfo: async (data: RuleInfoVO) => {
    return await request.post({ url: `/eiot/rule-info/create`, data })
  },

  // 修改规则引擎
  updateRuleInfo: async (data: RuleInfoVO) => {
    return await request.put({ url: `/eiot/rule-info/update`, data })
  },

  // 删除规则引擎
  deleteRuleInfo: async (id: number) => {
    return await request.delete({ url: `/eiot/rule-info/delete?id=` + id })
  },

  // 导出规则引擎 Excel
  exportRuleInfo: async (params) => {
    return await request.download({ url: `/eiot/rule-info/export-excel`, params })
  }
}
