/**
 * 告警卡片统一映射工具
 * 首页 / 告警中心共用同一个 toCard，保证两张页面的卡片展示完全一致。
 *
 * 兼容两种数据来源（类型/级别统一归一化为数值等级，保证显示文案一致）：
 * - /alert/selectAlertRecordPage：字段为 name / level / createTime / alertTime / orderStatus / handleStatus / readFlg
 * - /eiot/firewatch/alarm-order/page：字段为 alarmDescription / alarmLocation / alarmType / alarmLevel / alarmTime / orderStatus
 */

// 工单状态（orderStatus）
const ORDER_STATUS_MAP = {
  pending: { text: '待处理', cls: 'warning' },
  confirmed: { text: '已确认', cls: 'info' },
  handling: { text: '处理中', cls: 'info' },
  processing: { text: '处理中', cls: 'info' },
  handled: { text: '已处置', cls: 'success' },
  resolved: { text: '已处置', cls: 'success' },
  closed: { text: '已关闭', cls: 'neutral' }
}

// 处置状态（handleStatus：0=待处置, 1=稍后处置, 2=处置中, 3=已处置）
const HANDLE_STATUS_LIST = [
  { text: '待处理', cls: 'warning' },
  { text: '稍后处置', cls: 'neutral' },
  { text: '处置中', cls: 'info' },
  { text: '已处置', cls: 'success' }
]

// alarm-order 的告警类型编码 → 数值等级（eiot_alert_record.level 语义）
const TYPE_TO_LEVEL = { fire: '1', fault: '2', risk: '3', offline: '4', false_alarm: '5' }

// alarm-order 的告警级别编码 → 数值等级
const ALARM_LEVEL_TO_LEVEL = { critical: '1', major: '2', warning: '3', info: '4' }

// 数值等级 → 告警类型（与首页保持一致）
function alarmTypeName(level) {
  const m = { '1': '火警', '2': '故障', '3': '监管', '4': '屏蔽', '5': '屏蔽' }
  return m[level] || '其他'
}

// 数值等级 → 告警级别（与首页保持一致）
function levelName(level) {
  const m = { '1': 'A级（危）', '2': 'B级（中危）', '3': 'C级（低危）' }
  return m[level] || 'D级'
}

// 归一化数值等级：优先取 level 字段，否则用 alarmType / alarmLevel 编码换算
function normLevel(item) {
  if (item.level !== undefined && item.level !== null && item.level !== '') {
    return String(item.level)
  }
  if (item.alarmType && TYPE_TO_LEVEL[item.alarmType]) {
    return TYPE_TO_LEVEL[item.alarmType]
  }
  if (item.alarmLevel && ALARM_LEVEL_TO_LEVEL[item.alarmLevel]) {
    return ALARM_LEVEL_TO_LEVEL[item.alarmLevel]
  }
  return undefined
}

// 状态推导：orderStatus → handleStatus → readFlg
function statusOf(item) {
  if (item.orderStatus) {
    return ORDER_STATUS_MAP[item.orderStatus] || { text: '未知状态', cls: 'warning' }
  }
  if (item.handleStatus !== null && item.handleStatus !== undefined) {
    return HANDLE_STATUS_LIST[item.handleStatus] || { text: '未知状态', cls: 'warning' }
  }
  return item.readFlg ? { text: '已处置', cls: 'success' } : { text: '待处理', cls: 'warning' }
}

// 时间格式化：兼容毫秒时间戳与后端 LocalDateTime 字符串（如 2026-08-12T10:18:26）
export function fmtTime(ts) {
  if (!ts) return '-'
  if (typeof ts === 'string') {
    const m = ts.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})/)
    if (m) return m[1] + '-' + m[2] + '-' + m[3] + ' ' + m[4] + ':' + m[5] + ':' + m[6]
  }
  const d = new Date(ts)
  if (isNaN(d.getTime())) return String(ts)
  const p = n => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) +
    ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
}

/**
 * 统一告警卡片映射
 * @returns {{id, alarmNo, title, statusText, statusClass, subText, timeText, deviceName, location}}
 */
export function toCard(item) {
  const st = statusOf(item)
  const level = normLevel(item)
  return {
    id: item.id,
    alarmNo: item.alarmNo || item.orderNo || item.id || '-',
    title: item.name || item.alarmLocation || item.alarmDescription || '未知告警',
    statusText: st.text,
    statusClass: st.cls,
    subText: [alarmTypeName(level), levelName(level), item.deviceName || '-'].join(' · '),
    timeText: fmtTime(item.createTime || item.alertTime || item.alarmTime),
    deviceName: item.deviceName || '',
    location: item.alarmLocation || item.deviceAddr || ''
  }
}

/**
 * 通用列表卡片映射（工作台各列表入口共用）
 * fields: { title, sub: [字段名...], status, time }，字段名均取自后端 *RespVO
 */
export function toListItem(item, fields) {
  const sub = (fields.sub || [])
    .map(k => item[k])
    .filter(v => v !== null && v !== undefined && v !== '')
  return {
    id: item.id,
    title: item[fields.title] || '未命名',
    subText: sub.join(' · '),
    statusText: fields.status ? (item[fields.status] || '') : '',
    timeText: fields.time ? fmtTime(item[fields.time]) : ''
  }
}