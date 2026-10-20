// 消防值守系统 API
import request from '@/config/axios'

export type FirePropertyMapRespVO = {
  id: number | string
  code?: string | null
  name?: string | null
  propertyName?: string | null
  address?: string | null
  longitude?: number | string | null
  latitude?: number | string | null
  status: number
  riskLevel?: string | null
  color?: string | null
}

enum Api {
  // 监控大屏
  dashboardStatistics = '/eiot/firewatch/dashboard/statistics',
  deviceStatus = '/eiot/firewatch/dashboard/device-status',
  todayAlarm = '/eiot/firewatch/dashboard/today-alarm',
  realtimeAlarms = '/eiot/firewatch/dashboard/realtime-alarms',
  riskDistribution = '/eiot/firewatch/dashboard/risk-distribution',
  alarmTrend = '/eiot/firewatch/dashboard/alarm-trend',
  deviceCategory = '/eiot/firewatch/dashboard/device-category',

  // 火警处置
  alarmOrderCreate = '/eiot/firewatch/alarm-order/create',
  alarmOrderHandle = '/eiot/firewatch/alarm-order/handle',
  alarmOrderConfirm = '/eiot/firewatch/alarm-order/confirm',
  alarmOrderFalseAlarm = '/eiot/firewatch/alarm-order/false-alarm',
  alarmOrderRealFire = '/eiot/firewatch/alarm-order/real-fire',
  alarmOrderClose = '/eiot/firewatch/alarm-order/close',
  alarmOrderResetDevice = '/eiot/firewatch/alarm-order/reset-device',
  alarmOrderMute = '/eiot/firewatch/alarm-order/mute',
  alarmOrderCall = '/eiot/firewatch/alarm-order/call',
  alarmOrderNotify = '/eiot/firewatch/alarm-order/notify',
  alarmOrderGet = '/eiot/firewatch/alarm-order/get',
  alarmOrderPage = '/eiot/firewatch/alarm-order/page',
  alarmOrderStatistics = '/eiot/firewatch/alarm-order/statistics',

  // 值班管理
  dutyScheduleCreate = '/eiot/firewatch/duty-schedule/create',
  dutyScheduleBatchCreate = '/eiot/firewatch/duty-schedule/batch-create',
  dutyScheduleUpdate = '/eiot/firewatch/duty-schedule/update',
  dutyScheduleDelete = '/eiot/firewatch/duty-schedule/delete',
  dutyScheduleGet = '/eiot/firewatch/duty-schedule/get',
  dutySchedulePage = '/eiot/firewatch/duty-schedule/page',
  dutyScheduleListByDateRange = '/eiot/firewatch/duty-schedule/list-by-date-range',
  dutyScheduleListByDate = '/eiot/firewatch/duty-schedule/list-by-date',
  dutyScheduleCurrentDuty = '/eiot/firewatch/duty-schedule/current-duty',
  dutyScheduleCheckIn = '/eiot/firewatch/duty-schedule/check-in',
  dutyScheduleCheckOut = '/eiot/firewatch/duty-schedule/check-out',
  dutyScheduleChangeDuty = '/eiot/firewatch/duty-schedule/change-duty',
  dutyScheduleStatistics = '/eiot/firewatch/duty-schedule/statistics',
  dutyScheduleUserDutyDays = '/eiot/firewatch/duty-schedule/user-duty-days',
  dutyScheduleAutoSchedule = '/eiot/firewatch/duty-schedule/auto-schedule',
  dutyScheduleImportTemplate = '/eiot/firewatch/duty-schedule/get-import-template',
  dutyScheduleImport = '/eiot/firewatch/duty-schedule/import-excel',

  // 值班记录
  dutyRecordCreate = '/eiot/firewatch/duty-record/create',
  dutyRecordUpdate = '/eiot/firewatch/duty-record/update',
  dutyRecordDelete = '/eiot/firewatch/duty-record/delete',
  dutyRecordGet = '/eiot/firewatch/duty-record/get',
  dutyRecordPage = '/eiot/firewatch/duty-record/page',
  dutyRecordListBySchedule = '/eiot/firewatch/duty-record/list-by-schedule',
  dutyRecordListByTimeRange = '/eiot/firewatch/duty-record/list-by-time-range',
  dutyRecordHandover = '/eiot/firewatch/duty-record/handover',
  dutyRecordPatrol = '/eiot/firewatch/duty-record/patrol',
  dutyRecordStatistics = '/eiot/firewatch/duty-record/statistics',

  // 巡检任务
  patrolTaskCreate = '/eiot/firewatch/patrol-task/create',
  patrolTaskUpdate = '/eiot/firewatch/patrol-task/update',
  patrolTaskDelete = '/eiot/firewatch/patrol-task/delete',
  patrolTaskGet = '/eiot/firewatch/patrol-task/get',
  patrolTaskPage = '/eiot/firewatch/patrol-task/page',
  patrolTaskStart = '/eiot/firewatch/patrol-task/start',
  patrolTaskComplete = '/eiot/firewatch/patrol-task/complete',
  patrolTaskCancel = '/eiot/firewatch/patrol-task/cancel',
  patrolTaskPoints = '/eiot/firewatch/patrol-task/points',
  patrolPointCheck = '/eiot/firewatch/patrol-task/point/check',
  patrolTaskPointsAdd = '/eiot/firewatch/patrol-task/points/add',
  patrolTaskStatistics = '/eiot/firewatch/patrol-task/statistics',
  patrolTaskUserPending = '/eiot/firewatch/patrol-task/user-pending-tasks',
  patrolTaskTodayPendingCount = '/eiot/firewatch/patrol-task/today-pending-count',
  patrolTaskCheckOverdue = '/eiot/firewatch/patrol-task/check-overdue',
  patrolTaskExport = '/eiot/firewatch/patrol-task/export',
  patrolTaskImportTemplate = '/eiot/firewatch/patrol-task/get-import-template',
  patrolTaskImport = '/eiot/firewatch/patrol-task/import',

  // 维修工单
  repairOrderCreate = '/eiot/firewatch/repair-order/create',
  repairOrderCreateFromAlert = '/eiot/firewatch/repair-order/create-from-alert',
  repairOrderUpdate = '/eiot/firewatch/repair-order/update',
  repairOrderAssign = '/eiot/firewatch/repair-order/assign',
  repairOrderComplete = '/eiot/firewatch/repair-order/complete',
  repairOrderClose = '/eiot/firewatch/repair-order/close',
  repairOrderGet = '/eiot/firewatch/repair-order/get',
  repairOrderPage = '/eiot/firewatch/repair-order/page',
  repairOrderStatistics = '/eiot/firewatch/repair-order/statistics',

  // 隐患管理
  hazardCreate = '/eiot/firewatch/hazard/create',
  hazardUpdate = '/eiot/firewatch/hazard/update',
  hazardDelete = '/eiot/firewatch/hazard/delete',
  hazardGet = '/eiot/firewatch/hazard/get',
  hazardPage = '/eiot/firewatch/hazard/page',
  hazardSubmitRectification = '/eiot/firewatch/hazard/submit-rectification',
  hazardReviewRectification = '/eiot/firewatch/hazard/review-rectification',
  hazardClose = '/eiot/firewatch/hazard/close',
  hazardStatistics = '/eiot/firewatch/hazard/statistics',
  hazardExport = '/eiot/firewatch/hazard/export',
  hazardImportTemplate = '/eiot/firewatch/hazard/get-import-template',
  hazardImport = '/eiot/firewatch/hazard/import',

  // 物业单位
  propertyCreate = '/eiot/firewatch/property/create',
  propertyUpdate = '/eiot/firewatch/property/update',
  propertyDelete = '/eiot/firewatch/property/delete',
  propertyGet = '/eiot/firewatch/property/get',
  propertyPage = '/eiot/firewatch/property/page',
  propertyList = '/eiot/firewatch/property/list',
  propertyStatus = '/eiot/firewatch/property/status',
  propertyStatistics = '/eiot/firewatch/property/statistics',
}

// ==================== 监控大屏 API ====================

// 获取大屏统计数据
export const getDashboardStatistics = () => {
  return request.get({ url: Api.dashboardStatistics })
}

// 获取设备状态统计
export const getDeviceStatus = () => {
  return request.get({ url: Api.deviceStatus })
}

// 获取今日警情统计
export const getTodayAlarm = () => {
  return request.get({ url: Api.todayAlarm })
}

// 获取实时告警列表
export const getRealtimeAlarms = (limit: number = 10) => {
  return request.get({ url: Api.realtimeAlarms, params: { limit } })
}

// 获取风险单位分布
export const getRiskDistribution = () => {
  return request.get({ url: Api.riskDistribution })
}

// 获取告警趋势
export const getAlarmTrend = (days: number = 7) => {
  return request.get({ url: Api.alarmTrend, params: { days } })
}

// 获取设备类型分布
export const getDeviceCategory = () => {
  return request.get({ url: Api.deviceCategory })
}

// ==================== 火警处置 API ====================

// 创建火警工单
export const createAlarmOrder = (data: any) => {
  return request.post({ url: Api.alarmOrderCreate, data })
}

// 处置火警工单
export const handleAlarmOrder = (data: any) => {
  return request.put({ url: Api.alarmOrderHandle, data })
}

// 确认火警
export const confirmAlarm = (id: number, handler?: string, remark?: string) => {
  return request.put({ url: Api.alarmOrderConfirm, params: { id, handler, remark } })
}

// 标记误报
export const markFalseAlarm = (id: number, handler?: string, remark?: string) => {
  return request.put({ url: Api.alarmOrderFalseAlarm, params: { id, handler, remark } })
}

// 确认真实火警
export const confirmRealFire = (id: number, handler?: string, remark?: string) => {
  return request.put({ url: Api.alarmOrderRealFire, params: { id, handler, remark } })
}

// 关闭工单
export const closeAlarmOrder = (id: number, handler?: string, remark?: string) => {
  return request.put({ url: Api.alarmOrderClose, params: { id, handler, remark } })
}

// 复位设备
export const resetDevice = (id: number) => {
  return request.put({ url: Api.alarmOrderResetDevice, params: { id } })
}

// 消音
export const muteAlarm = (id: number) => {
  return request.put({ url: Api.alarmOrderMute, params: { id } })
}

// 拨打电话
export const makePhoneCall = (id: number, phone: string) => {
  return request.post({ url: Api.alarmOrderCall, params: { id, phone } })
}

// 发送通知
export const sendNotification = (id: number, channels?: string[]) => {
  return request.post({ url: Api.alarmOrderNotify, params: { id, channels } })
}

// 获取工单详情
export const getAlarmOrder = (id: number) => {
  return request.get({ url: Api.alarmOrderGet, params: { id } })
}

// 分页查询工单
export const getAlarmOrderPage = (params: any) => {
  return request.get({ url: Api.alarmOrderPage, params })
}

// 获取报警记录统计
export const getAlarmRecordStatistics = () => {
  return request.get({ url: Api.alarmOrderStatistics })
}

// ==================== 值班管理 API ====================

// 创建排班
export const createDutySchedule = (data: any) => {
  return request.post({ url: Api.dutyScheduleCreate, data })
}

// 批量创建排班
export const batchCreateDutySchedule = (data: any[]) => {
  return request.post({ url: Api.dutyScheduleBatchCreate, data })
}

// 更新排班
export const updateDutySchedule = (data: any) => {
  return request.put({ url: Api.dutyScheduleUpdate, data })
}

// 删除排班
export const deleteDutySchedule = (id: number) => {
  return request.delete({ url: Api.dutyScheduleDelete, params: { id } })
}

// 获取排班详情
export const getDutySchedule = (id: number) => {
  return request.get({ url: Api.dutyScheduleGet, params: { id } })
}

// 分页查询排班
export const getDutySchedulePage = (params: any) => {
  return request.get({ url: Api.dutySchedulePage, params })
}

// 按日期范围查询排班
export const getDutyScheduleByDateRange = (startDate: string, endDate: string, propertyId?: number) => {
  return request.get({ url: Api.dutyScheduleListByDateRange, params: { startDate, endDate, propertyId } })
}

// 按日期查询排班
export const getDutyScheduleByDate = (date: string, propertyId?: number) => {
  return request.get({ url: Api.dutyScheduleListByDate, params: { date, propertyId } })
}

// 获取当前值班人员
// 支持按物业ID（propertyId）或租户ID（tenantId，后端自动转换为物业ID）查询
export const getCurrentDuty = (propertyId?: number, tenantId?: number) => {
  return request.get({ url: Api.dutyScheduleCurrentDuty, params: { propertyId, tenantId } })
}

// 签到
export const checkIn = (id: number, checkInType: string) => {
  return request.put({ url: Api.dutyScheduleCheckIn, params: { id, checkInType } })
}

// 签退
export const checkOut = (id: number) => {
  return request.put({ url: Api.dutyScheduleCheckOut, params: { id } })
}

// 换班
export const changeDuty = (id: number, newUserId: number, reason?: string) => {
  return request.put({ url: Api.dutyScheduleChangeDuty, params: { id, newUserId, reason } })
}

// 获取排班统计
export const getDutyScheduleStatistics = (propertyId?: number, startDate?: string, endDate?: string) => {
  return request.get({ url: Api.dutyScheduleStatistics, params: { propertyId, startDate, endDate } })
}

// 获取用户值班天数
export const getUserDutyDays = (userId: number, year: number, month: number) => {
  return request.get({ url: Api.dutyScheduleUserDutyDays, params: { userId, year, month } })
}

// 自动排班
export const autoSchedule = (propertyId: number, year: number, month: number, dutyUsers: number[]) => {
  return request.post({ url: Api.dutyScheduleAutoSchedule, params: { propertyId, year, month }, data: dutyUsers })
}

// 下载排班导入模板
export const downloadDutyScheduleTemplate = () => {
  return request.download({ url: Api.dutyScheduleImportTemplate })
}

// 导入排班
export const importDutySchedule = (data: FormData) => {
  return request.upload({ url: Api.dutyScheduleImport, data })
}

// ==================== 值班记录 API ====================

// 创建记录
export const createDutyRecord = (data: any) => {
  return request.post({ url: Api.dutyRecordCreate, data })
}

// 更新记录
export const updateDutyRecord = (data: any) => {
  return request.put({ url: Api.dutyRecordUpdate, data })
}

// 删除记录
export const deleteDutyRecord = (id: number) => {
  return request.delete({ url: Api.dutyRecordDelete, params: { id } })
}

// 获取记录详情
export const getDutyRecord = (id: number) => {
  return request.get({ url: Api.dutyRecordGet, params: { id } })
}

// 分页查询记录
export const getDutyRecordPage = (params: any) => {
  return request.get({ url: Api.dutyRecordPage, params })
}

// 按排班查询记录
export const getDutyRecordBySchedule = (scheduleId: number) => {
  return request.get({ url: Api.dutyRecordListBySchedule, params: { scheduleId } })
}

// 按时间范围查询记录
export const getDutyRecordByTimeRange = (startTime: string, endTime: string, propertyId?: number) => {
  return request.get({ url: Api.dutyRecordListByTimeRange, params: { startTime, endTime, propertyId } })
}

// 创建交接班记录
export const createHandoverRecord = (scheduleId: number, handoverContent: string, nextDutyUserId?: number) => {
  return request.post({ url: Api.dutyRecordHandover, params: { scheduleId, handoverContent, nextDutyUserId } })
}

// 创建巡查记录
export const createPatrolRecord = (scheduleId: number, content: string, status?: string, attachments?: string) => {
  return request.post({ url: Api.dutyRecordPatrol, params: { scheduleId, content, status, attachments } })
}

// 获取记录统计
export const getDutyRecordStatistics = (startTime: string, endTime: string, propertyId?: number) => {
  return request.get({ url: Api.dutyRecordStatistics, params: { startTime, endTime, propertyId } })
}

// ==================== 巡检任务 API ====================

// 创建任务
export const createPatrolTask = (data: any) => {
  return request.post({ url: Api.patrolTaskCreate, data })
}

// 更新任务
export const updatePatrolTask = (data: any) => {
  return request.put({ url: Api.patrolTaskUpdate, data })
}

// 删除任务
export const deletePatrolTask = (id: number) => {
  return request.delete({ url: Api.patrolTaskDelete, params: { id } })
}

// 获取任务详情
export const getPatrolTask = (id: number) => {
  return request.get({ url: Api.patrolTaskGet, params: { id } })
}

// 分页查询任务
export const getPatrolTaskPage = (params: any) => {
  return request.get({ url: Api.patrolTaskPage, params })
}

// 开始任务
export const startPatrolTask = (id: number) => {
  return request.put({ url: Api.patrolTaskStart, params: { id } })
}

// 完成任务
export const completePatrolTask = (id: number, summary?: string) => {
  return request.put({ url: Api.patrolTaskComplete, params: { id, summary } })
}

// 取消任务
export const cancelPatrolTask = (id: number, reason?: string) => {
  return request.put({ url: Api.patrolTaskCancel, params: { id, reason } })
}

// 获取任务点位列表
export const getPatrolTaskPoints = (taskId: number) => {
  return request.get({ url: Api.patrolTaskPoints, params: { taskId } })
}

// 检查点位
export const checkPatrolPoint = (data: any) => {
  return request.put({ url: Api.patrolPointCheck, data })
}

// 添加巡检点位
export const addPatrolTaskPoints = (taskId: number, deviceIds: number[]) => {
  return request.post({ url: Api.patrolTaskPointsAdd, params: { taskId }, data: deviceIds })
}

// 获取任务统计
export const getPatrolTaskStatistics = (startDate: string, endDate: string, propertyId?: number, tenantId?: number) => {
  return request.get({ url: Api.patrolTaskStatistics, params: { startDate, endDate, propertyId, tenantId } })
}

// 获取用户待执行任务
export const getUserPendingTasks = (userId: number) => {
  return request.get({ url: Api.patrolTaskUserPending, params: { userId } })
}

// 获取今日待执行任务数
export const getTodayPendingTaskCount = () => {
  return request.get({ url: Api.patrolTaskTodayPendingCount })
}

// 检测并标记超期任务
export const checkOverdueTasks = () => {
  return request.put({ url: Api.patrolTaskCheckOverdue })
}

// 导出巡检任务
export const exportPatrolTask = (params: any) => {
  return request.download({ url: Api.patrolTaskExport, params })
}

// 下载巡检任务导入模板
export const downloadPatrolTaskTemplate = () => {
  return request.download({ url: Api.patrolTaskImportTemplate })
}

// 导入巡检任务
export const importPatrolTask = (data: FormData) => {
  return request.upload({ url: Api.patrolTaskImport, data })
}

// ==================== 维修工单 API ====================

// 创建维修工单
export const createRepairOrder = (data: any) => {
  return request.post({ url: Api.repairOrderCreate, data })
}

// 从故障告警生成维修工单
export const createRepairOrderFromAlert = (alertRecordId: number) => {
  return request.post({ url: Api.repairOrderCreateFromAlert, params: { alertRecordId } })
}

// 更新维修工单
export const updateRepairOrder = (data: any) => {
  return request.put({ url: Api.repairOrderUpdate, data })
}

// 派单
export const assignRepairOrder = (data: any) => {
  return request.put({ url: Api.repairOrderAssign, data })
}

// 完成维修
export const completeRepairOrder = (id: number, completeRemark?: string, completeImages?: string) => {
  return request.put({ url: Api.repairOrderComplete, params: { id, completeRemark, completeImages } })
}

// 关闭工单
export const closeRepairOrder = (id: number) => {
  return request.put({ url: Api.repairOrderClose, params: { id } })
}

// 获取工单详情
export const getRepairOrder = (id: number) => {
  return request.get({ url: Api.repairOrderGet, params: { id } })
}

// 分页查询工单
export const getRepairOrderPage = (params: any) => {
  return request.get({ url: Api.repairOrderPage, params })
}

// 获取工单统计
export const getRepairOrderStatistics = () => {
  return request.get({ url: Api.repairOrderStatistics })
}

// 签单
export const signRepairOrder = (id: number) => {
  return request.put({ url: '/eiot/firewatch/repair-order/sign', params: { id } })
}

// 开始维修
export const startRepairOrder = (id: number) => {
  return request.put({ url: '/eiot/firewatch/repair-order/start', params: { id } })
}

// 评价工单
export const rateRepairOrder = (id: number, satisfaction: number, suggestion?: string) => {
  return request.put({ url: '/eiot/firewatch/repair-order/rate', params: { id, satisfaction, suggestion } })
}

// 获取超期工单数
export const getOverdueRepairOrderCount = () => {
  return request.get({ url: '/eiot/firewatch/repair-order/overdue' })
}

// ==================== 隐患管理 API ====================

// 创建隐患
export const createHazard = (data: any) => {
  return request.post({ url: Api.hazardCreate, data })
}

// 更新隐患
export const updateHazard = (data: any) => {
  return request.put({ url: Api.hazardUpdate, data })
}

// 删除隐患
export const deleteHazard = (id: number) => {
  return request.delete({ url: Api.hazardDelete, params: { id } })
}

// 获取隐患详情
export const getHazard = (id: number) => {
  return request.get({ url: Api.hazardGet, params: { id } })
}

// 分页查询隐患
export const getHazardPage = (params: any) => {
  return request.get({ url: Api.hazardPage, params })
}

// 提交整改进度
export const submitHazardRectification = (data: any) => {
  return request.put({ url: Api.hazardSubmitRectification, data })
}

// 验收复查
export const reviewHazardRectification = (id: number, result: string, remark?: string) => {
  return request.put({ url: Api.hazardReviewRectification, params: { id, result, remark } })
}

// 关闭隐患
export const closeHazard = (id: number, remark?: string) => {
  return request.put({ url: Api.hazardClose, params: { id, remark } })
}

// 获取隐患统计
export const getHazardStatistics = (params: any) => {
  return request.get({ url: Api.hazardStatistics, params })
}

// 导出隐患
export const exportHazard = (params: any) => {
  return request.download({ url: Api.hazardExport, params })
}

// 下载隐患导入模板
export const downloadHazardTemplate = () => {
  return request.download({ url: Api.hazardImportTemplate })
}

// 导入隐患
export const importHazard = (data: FormData) => {
  return request.upload({ url: Api.hazardImport, data })
}

// ==================== 物业单位 API ====================

// 创建物业
export const createProperty = (data: any) => {
  return request.post({ url: Api.propertyCreate, data })
}

// 更新物业
export const updateProperty = (data: any) => {
  return request.put({ url: Api.propertyUpdate, data })
}

// 删除物业
export const deleteProperty = (id: number) => {
  return request.delete({ url: Api.propertyDelete, params: { id } })
}

// 获取物业详情
export const getProperty = (id: number | string, timeout?: number) => {
  return request.get<FirePropertyMapRespVO | null>({ url: Api.propertyGet, params: { id }, timeout })
}

// 分页查询物业
export const getPropertyPage = (params: any) => {
  return request.get({ url: Api.propertyPage, params })
}

// 获取物业列表
export const getPropertyList = (status?: number) => {
  return request.get({ url: Api.propertyList, params: { status } })
}

// 值守大屏使用与当前登录用户权限和租户一致的完整物业集合。
export const getPropertyListForDutyScreen = (status: number = 1, timeout: number = 10_000) => {
  return request.get<FirePropertyMapRespVO[]>({ url: Api.propertyList, params: { status }, timeout })
}

// 更新物业状态
export const updatePropertyStatus = (id: number, status: number) => {
  return request.put({ url: Api.propertyStatus, params: { id, status } })
}

// 获取物业统计
export const getPropertyStatistics = (id: number) => {
  return request.get({ url: Api.propertyStatistics, params: { id } })
}

// ==================== 维护保养仪表板 API ====================

// 获取维护保养仪表板统计数据
export const getMaintenanceDashboard = () => {
  return request.get({ url: '/eiot/firewatch/maintenance/dashboard' })
}

// 发送催促执行通知
export const sendUrgeNotification = () => {
  return request.post({ url: '/eiot/firewatch/maintenance/send-urge' })
}

// ==================== 维保合同管理 API ====================

// 创建维保合同
export const createContract = (data: any) => {
  return request.post({ url: '/eiot/firewatch/contract/create', data })
}

// 更新维保合同
export const updateContract = (data: any) => {
  return request.put({ url: '/eiot/firewatch/contract/update', data })
}

// 删除维保合同
export const deleteContract = (id: number) => {
  return request.delete({ url: '/eiot/firewatch/contract/delete', params: { id } })
}

// 获取合同详情
export const getContract = (id: number) => {
  return request.get({ url: '/eiot/firewatch/contract/get', params: { id } })
}

// 分页查询合同
export const getContractPage = (params: any) => {
  return request.get({ url: '/eiot/firewatch/contract/page', params })
}

// 获取即将到期合同
export const getExpiringContracts = () => {
  return request.get({ url: '/eiot/firewatch/contract/expiring' })
}

// ==================== 维保计划管理 API ====================

// 创建维保计划
export const createPlan = (data: any) => {
  return request.post({ url: '/eiot/firewatch/plan/create', data })
}

// 更新维保计划
export const updatePlan = (data: any) => {
  return request.put({ url: '/eiot/firewatch/plan/update', data })
}

// 删除维保计划
export const deletePlan = (id: number) => {
  return request.delete({ url: '/eiot/firewatch/plan/delete', params: { id } })
}

// 获取计划详情
export const getPlan = (id: number) => {
  return request.get({ url: '/eiot/firewatch/plan/get', params: { id } })
}

// 分页查询计划
export const getPlanPage = (params: any) => {
  return request.get({ url: '/eiot/firewatch/plan/page', params })
}

// 完成维保计划
export const completePlan = (id: number) => {
  return request.put({ url: '/eiot/firewatch/plan/complete', params: { id } })
}

// 获取逾期计划
export const getOverduePlans = () => {
  return request.get({ url: '/eiot/firewatch/plan/overdue' })
}

// 导出维保计划 Excel
export const exportPlanExcel = (params: any) => {
  return request.download({ url: '/eiot/firewatch/plan/export-excel', params })
}

// ==================== 维保单位管理 API ====================

// 创建维保单位
export const createMaintenanceUnit = (data: any) => {
  return request.post({ url: '/eiot/firewatch/maintenance-unit/create', data })
}

// 更新维保单位
export const updateMaintenanceUnit = (data: any) => {
  return request.put({ url: '/eiot/firewatch/maintenance-unit/update', data })
}

// 删除维保单位
export const deleteMaintenanceUnit = (id: number) => {
  return request.delete({ url: '/eiot/firewatch/maintenance-unit/delete', params: { id } })
}

// 获取维保单位详情
export const getMaintenanceUnit = (id: number) => {
  return request.get({ url: '/eiot/firewatch/maintenance-unit/get', params: { id } })
}

// 分页查询维保单位
export const getMaintenanceUnitPage = (params: any) => {
  return request.get({ url: '/eiot/firewatch/maintenance-unit/page', params })
}

// 获取维保单位列表（不分页，用于下拉选择）
export const getMaintenanceUnitList = () => {
  return request.get({ url: '/eiot/firewatch/maintenance-unit/list' })
}

// ==================== 告警消息 API ====================

// 查询告警消息分页
export const getAlertRecordPage = (data: any) => {
  return request.post({ url: '/alert/selectAlertRecordPage', data })
}

// 未处置告警回捞窗口：当天 0 点起的分钟数（避免历史未处置数据债涌上大屏；服务重启/页面刷新后仍能恢复当天待处置告警）
const minutesSinceDayStart = () =>
  Math.max(1, Math.ceil((Date.now() - new Date().setHours(0, 0, 0, 0)) / 60000))

// 查询未处置告警（默认回捞窗口：当天 0 点起）
export const getRecentUnprocessedAlerts = (minutes: number = minutesSinceDayStart()) => {
  return request.get({ url: '/alert/recentUnprocessedAlerts', params: { minutes } })
}

// 确认处理告警记录
export const confirmAlertRecord = (id: number) => {
  return request.post({ url: '/alert/confirmAlertRecord', data: { id } })
}

// 稍后处置告警记录
export const deferAlertRecord = (id: number) => {
  return request.post({ url: '/alert/deferAlertRecord', data: { id } })
}

// 去处置告警记录（状态改为处置中）
export const handleAlertRecord = (id: number) => {
  return request.post({ url: '/alert/handleAlertRecord', data: { id } })
}

// 查询指定父设备（含其子设备）未处置告警（默认回捞窗口：当天 0 点起）
export const getUnprocessedAlertsByParentDevice = (parentDeviceId: number, minutes: number = minutesSinceDayStart()) => {
  return request.get({ url: '/alert/unprocessedAlertsByDevices', params: { parentDeviceId, minutes } })
}

// 查找报警设备对应的用传设备ID
export const findTransferDeviceId = (alertDeviceId: number) => {
  return request.get({ url: '/alert/findTransferDeviceId', params: { alertDeviceId } })
}
