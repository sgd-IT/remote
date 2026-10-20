/**
 * 更新日志（数据源唯一入口）
 *
 * 发版时只需要在数组最前面追加一条记录，不需要改任何页面代码：
 * 页面的「最新更新」取第一条，登录弹窗也只比较第一条与浏览器本地记录的已读版本，
 * 记录变化后所有用户下次进入系统会自动看到一次更新提醒。
 *
 * type 取值：feature 新功能 / improve 体验优化 / fix 问题修复
 */

export type ChangelogItemType = 'feature' | 'improve' | 'fix'

export interface ChangelogItem {
  type: ChangelogItemType
  text: string
}

export interface ChangelogVersion {
  version: string
  date: string
  title: string
  items: ChangelogItem[]
}

export const CHANGELOG: ChangelogVersion[] = [
  {
    version: 'V1.2.0',
    date: '2026-10-19',
    title: '使用统计与排班体验升级',
    items: [
      { type: 'feature', text: '新增「使用统计」页面，可按调用次数最多 / 最少查看接口排行（仅超级管理员可见）' },
      { type: 'feature', text: '值班排班支持自动轮换，默认全天班并支持跨天展示' },
      { type: 'feature', text: '用户管理新增「登录平台」字段，便于区分账号登录来源' },
      { type: 'improve', text: '设备列表页整体改版，导出台账支持自定义列' },
      { type: 'improve', text: '报警弹窗、辅助判断面板与视频播放器统一主题色' },
      { type: 'fix', text: '稍后处置的告警不再重复弹窗打扰' },
      { type: 'fix', text: '值守大屏 3D 建模加载恢复正常' }
    ]
  },
  {
    version: 'V1.1.0',
    date: '2026-10-10',
    title: '数据中心图表化与告警分级',
    items: [
      { type: 'feature', text: '数据中心支持图表化分析，趋势与分布一眼看清' },
      { type: 'feature', text: '告警类型与等级全链路打通，统一按一级 / 二级 / 三级展示' },
      { type: 'feature', text: '报警弹窗抽为公共组件，值守大屏与监控中心共用' },
      { type: 'improve', text: '报警弹窗摄像头切换改为下拉浮层，画面切换更顺手' },
      { type: 'improve', text: '报警弹窗视频起播更快、画面更稳定' }
    ]
  },
  {
    version: 'V1.0.0',
    date: '2026-09-30',
    title: '多租户查看与月度报告',
    items: [
      { type: 'feature', text: '值守大屏支持租户切换，超级管理员可下拉查看各租户数据' },
      { type: 'feature', text: '数据中心增加月份选择，支持导出月度报告' },
      { type: 'feature', text: '用户管理支持归属租户' },
      { type: 'improve', text: '管理端告警列表显示所属租户并支持按租户筛选' },
      { type: 'fix', text: '值班记录筛选补充「早退」状态' }
    ]
  },
  {
    version: 'V0.9.0',
    date: '2026-08-28',
    title: '视频与呼叫能力完善',
    items: [
      { type: 'feature', text: '新增呼叫中心页面，支持座机号码管理与通话记录' },
      { type: 'feature', text: '视频监控与远程控制台支持批量播放' },
      { type: 'improve', text: '深蓝主题页面切换时不再白屏闪现' },
      { type: 'fix', text: '值班管理路由恢复，巡检任务支持每天重复' }
    ]
  }
]

/** 最新一条更新：始终取数组第一条，页面与弹窗都以此为准 */
export const CURRENT_VERSION = CHANGELOG[0]?.version ?? ''

export const CHANGE_TYPE_LABEL: Record<ChangelogItemType, string> = {
  feature: '新功能',
  improve: '优化',
  fix: '修复'
}
