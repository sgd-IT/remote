/**
 * 告警卡片统一映射工具
 * 首页 / 告警中心共用同一个 toCard，保证两张页面的卡片展示完全一致。
 *
 * 兼容两种数据来源，类型与级别分别归一（双读：alarm_kind 非空=新语义；为空=旧语义回退）：
 * - /alert/selectAlertRecordPage：字段为 name / level / alarmType(数字0-5) / alarmKind / createTime / alertTime / orderStatus / handleStatus / readFlg
 * - /eiot/firewatch/alarm-order/page：字段为 alarmDescription / alarmLocation / alarmType(英文) / alarmLevel(英文) / alarmTime / orderStatus
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

// 报警类型数字 → 英文类型标识（新语义 alarm_kind；与旧 level 类型编码"换列不换值"，仅多 6=信息）
const KIND_TO_TYPE = { 1: 'fire', 2: 'fault', 3: 'risk', 4: 'offline', 5: 'false_alarm', 6: 'info' }

// 旧数据回退：level 数字（旧语义=类型编码）→ 英文类型标识
const OLD_LEVEL_TO_TYPE = { '1': 'fire', '2': 'fault', '3': 'risk', '4': 'offline', '5': 'false_alarm' }

// alarm-order 出口 alarmLevel（英文）→ 数值等级（新语义 1/2/3；info 已废弃，不再换算）
const ALARM_LEVEL_TO_LEVEL = { critical: '1', major: '2', warning: '3' }

// 英文类型标识 → 告警类型中文（与 PC 端出口口径一致）
function alarmTypeName(type) {
  const m = { fire: '火警', fault: '故障', risk: '隐患', offline: '离线', false_alarm: '误报', info: '信息' }
  return m[type] || '其他'
}

// 数值等级 → 告警级别（保持既有 A/B/C 展示）
function levelName(level) {
  const m = { '1': 'A级（危）', '2': 'B级（中危）', '3': 'C级（低危）' }
  return m[level] || 'D级'
}

// 归一报警类型标识：alarmKind（新语义）→ alarmType 英文（alarm-order 出口）→ level 旧编码回退
function typeOf(item) {
  if (item.alarmKind !== undefined && item.alarmKind !== null) return KIND_TO_TYPE[item.alarmKind]
  if (typeof item.alarmType === 'string' && item.alarmType) return item.alarmType
  if (item.level !== undefined && item.level !== null && item.level !== '') return OLD_LEVEL_TO_TYPE[String(item.level)]
  return undefined
}

// 归一数值等级：level（记录明细，新语义等级）→ alarmLevel 英文换算（alarm-order 出口）
function levelOf(item) {
  if (item.level !== undefined && item.level !== null && item.level !== '') return String(item.level)
  if (item.alarmLevel && ALARM_LEVEL_TO_LEVEL[item.alarmLevel]) return ALARM_LEVEL_TO_LEVEL[item.alarmLevel]
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
  const type = typeOf(item)
  const level = levelOf(item)
  return {
    id: item.id,
    alarmNo: item.alarmNo || item.orderNo || item.id || '-',
    title: item.name || item.alarmLocation || item.alarmDescription || '未知告警',
    statusText: st.text,
    statusClass: st.cls,
    subText: [alarmTypeName(type), levelName(level), item.deviceName || '-'].join(' · '),
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