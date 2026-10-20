/**
 * 数据中心 Mock 数据
 *
 * @deprecated 第二轮退场：后端 15 个统计接口已全部真实化，本文件与各组件的 fallback 均为过渡期兜底，
 * 逐 Tab 核对通过后统一删除（连同 index.vue 的“开发中”标识与提示条）。保留期间的作用：
 * 接口异常/空数据时页面仍可渲染。
 */

// ==================== 告警分析 ====================

export const mockAlarmTrend = Array.from({ length: 30 }, (_, i) => {
  const d = new Date()
  d.setDate(d.getDate() - 29 + i)
  const date = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const base = 15 + Math.round(Math.sin(i * 0.4) * 8)
  return { date, alarmCount: base + Math.round(Math.random() * 10), handleCount: base + Math.round(Math.random() * 6) - 2 }
})

export const mockAlarmTypeDistribution = [
  { typeName: '烟感报警', count: 128 },
  { typeName: '温感报警', count: 96 },
  { typeName: '手报报警', count: 73 },
  { typeName: '电源故障', count: 58 },
  { typeName: '通信故障', count: 45 },
  { typeName: '部件故障', count: 37 },
  { typeName: '气体泄漏', count: 22 },
  { typeName: '消防水压', count: 18 },
]

export const mockAlarmLevelDistribution = [
  { level: 'critical', levelName: '严重', count: 42 },
  { level: 'major', levelName: '较大', count: 87 },
  { level: 'warning', levelName: '一般', count: 215 },
  { level: 'info', levelName: '提示', count: 133 },
]

export const mockPropertyAlarmRank = [
  { propertyName: '碧桂园·天玺', alarmCount: 89 },
  { propertyName: '万科·翡翠湾', alarmCount: 72 },
  { propertyName: '恒大·御景半岛', alarmCount: 65 },
  { propertyName: '保利·和光尘樾', alarmCount: 53 },
  { propertyName: '中海·锦城', alarmCount: 47 },
  { propertyName: '龙湖·春江天境', alarmCount: 38 },
  { propertyName: '融创·玖樾府', alarmCount: 31 },
  { propertyName: '华润·橡树湾', alarmCount: 24 },
  { propertyName: '绿地·海珀云庭', alarmCount: 18 },
  { propertyName: '招商·雍景湾', alarmCount: 12 },
]

export const mockAlarmHourlyDistribution = Array.from({ length: 24 }, (_, i) => {
  // 模拟真实分布：夜间少、白天多、傍晚高峰
  const hourFactors = [2, 1, 1, 0, 0, 1, 3, 6, 8, 10, 12, 9, 11, 13, 10, 8, 14, 16, 12, 9, 7, 5, 4, 3]
  return { hour: i, count: hourFactors[i] + Math.round(Math.random() * 3) }
})

// ==================== 设备分析 ====================

export const mockDeviceStatus = {
  total: 1286,
  normal: 1043,
  alarmDeviceCountInRange: 67,
  fault: 89,
  offline: 52,
  inactive: 35,
}

export const mockDeviceCategoryDistribution = [
  { categoryName: '烟感探测器', count: 412 },
  { categoryName: '温感探测器', count: 287 },
  { categoryName: '手动报警按钮', count: 156 },
  { categoryName: '消防栓', count: 134 },
  { categoryName: '喷淋系统', count: 98 },
  { categoryName: '气体灭火', count: 67 },
  { categoryName: '声光报警器', count: 53 },
  { categoryName: '消防广播', count: 42 },
  { categoryName: '排烟风机', count: 37 },
]

export const mockPropertyDeviceDistribution = [
  { propertyName: '碧桂园·天玺', deviceCount: 234 },
  { propertyName: '万科·翡翠湾', deviceCount: 198 },
  { propertyName: '恒大·御景半岛', deviceCount: 176 },
  { propertyName: '保利·和光尘樾', deviceCount: 152 },
  { propertyName: '中海·锦城', deviceCount: 134 },
  { propertyName: '龙湖·春江天境', deviceCount: 112 },
  { propertyName: '融创·玖樾府', deviceCount: 98 },
  { propertyName: '华润·橡树湾', deviceCount: 87 },
  { propertyName: '绿地·海珀云庭', deviceCount: 54 },
  { propertyName: '招商·雍景湾', deviceCount: 41 },
]

// ==================== 值班统计 ====================

export const mockDutyStats = {
  monthDutyCount: 86,
  checkInRate: 94.2,
  onTimeRate: 88.5,
  handoverCount: 43,
}

export const mockUserDutyRank = [
  { userName: '张建国', dutyDays: 18 },
  { userName: '李明辉', dutyDays: 16 },
  { userName: '王秀英', dutyDays: 15 },
  { userName: '赵志强', dutyDays: 14 },
  { userName: '陈丽萍', dutyDays: 13 },
  { userName: '刘伟', dutyDays: 12 },
  { userName: '杨芳', dutyDays: 10 },
  { userName: '周军', dutyDays: 9 },
]

// ==================== 巡检统计 ====================

export const mockPatrolStats = {
  total: 234,
  completed: 189,
  inProgress: 28,
  overdue: 17,
  completeRate: 80.8,
}

export const mockPatrolTrend = Array.from({ length: 30 }, (_, i) => {
  const d = new Date()
  d.setDate(d.getDate() - 29 + i)
  const date = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return { date, createdCount: 6 + Math.round(Math.random() * 6), completedCount: 5 + Math.round(Math.random() * 5) }
})

// ==================== 工单统计 ====================

export const mockRepairStats = {
  total: 156,
  pending: 23,
  inProgress: 18,
  completed: 102,
  overdue: 13,
}

export const mockHazardStats = {
  total: 87,
  pending: 12,
  rectified: 52,
  closed: 23,
  hazardOverdue: 9,
  rectificationRate: 86.2,
}

export const mockWorkOrderTrend = Array.from({ length: 30 }, (_, i) => {
  const d = new Date()
  d.setDate(d.getDate() - 29 + i)
  const date = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return { date, createdCount: 3 + Math.round(Math.random() * 5), completedCount: 2 + Math.round(Math.random() * 4) }
})
