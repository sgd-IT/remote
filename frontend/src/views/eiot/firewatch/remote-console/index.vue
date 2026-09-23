<template>
  <div class="remote-console">
    <header class="header">
      <div class="header-title">广东安为安全科技有限公司</div>
      <div class="header-actions">
        <div class="header-btn close" @click="goBackToDuty" title="退出">
          <el-icon><Close /></el-icon>
        </div>
      </div>
    </header>

    <SubHeader
      :device-name="selectedDeviceInfo?.name"
      :company-name="selectedEnterpriseName"
      :point-total="pointTotal"
      :point-online="pointOnline"
      :duty-persons="dutyPersons"
      :brand="selectedDeviceInfo?.brandCode || '--'"
      :model="selectedDeviceInfo?.model || '--'"
    />

    <div class="main-container">
      <div class="left-panel">
        <VideoPanel :cameras="displayCameras" />
      </div>
      <div class="center-panel">
        <!-- 智能空开控制 -->
        <div class="valve-control-section">
          <div class="valve-section-title">智能空开</div>
          <div class="valve-buttons">
            <button class="valve-btn valve-open" :disabled="!selectedDeviceId" @click="handleValveControl('open')">开闸</button>
            <button class="valve-btn valve-close" :disabled="!selectedDeviceId" @click="handleValveControl('close')">合闸</button>
          </div>
        </div>
        <MainKeyboard :functions="deviceFunctions" :device-id="selectedDeviceId" @action="handleGeneralAction" />
        <DeviceCardGrid :functions="deviceFunctions" @action="handleCardAction" />
      </div>
      <div class="right-panel">
        <AlarmLogPanel :device-id="selectedDeviceId" />
        <OperationLogPanel :device-id="selectedDeviceId" />
      </div>
    </div>

    <VerifyModal v-model="verifyVisible" :action-name="currentActionName" @confirm="handleVerifyConfirm" />

    <!-- 滚动告警弹窗 -->
    <Teleport to="body">
      <div v-if="currentScrollAlert" class="rc-scroll-alert" :class="'level-' + getAlertLevel(currentScrollAlert)">
        <div class="rc-scroll-alert-header">
          <span class="rc-scroll-alert-badge" :class="'sev-' + getAlertLevel(currentScrollAlert)">{{ getAlertLevelText(currentScrollAlert) }}</span>
          <span class="rc-scroll-alert-device">{{ currentScrollAlert.deviceName || '未知设备' }}</span>
          <span class="rc-scroll-alert-count">{{ scrollAlertIndex + 1 }}/{{ pendingAlerts.length }}</span>
        </div>
        <div class="rc-scroll-alert-body">
          <span class="rc-scroll-alert-content">{{ currentScrollAlert.details || currentScrollAlert.name || '设备告警' }}</span>
        </div>
        <div class="rc-scroll-alert-footer">
          <span class="rc-scroll-alert-time">{{ formatAlertTime(currentScrollAlert.alertTime) }}</span>
          <button class="rc-scroll-alert-btn" @click="handleAlertFromPanel(currentScrollAlert)">去处置</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import SubHeader from './components/SubHeader.vue'
import VideoPanel from './components/VideoPanel.vue'
import MainKeyboard from './components/MainKeyboard.vue'
import DeviceCardGrid from './components/DeviceCardGrid.vue'
import AlarmLogPanel from './components/AlarmLogPanel.vue'
import OperationLogPanel from './components/OperationLogPanel.vue'
import VerifyModal from './components/modals/VerifyModal.vue'
import { getDeviceTree, getDeviceCameras, valveControl, getConsolePersons, type CameraVO, type DeviceTreeItemVO, type DeviceTreeNodeVO } from '@/api/eiot/remote/remoteConsole.api'
import { getDeviceFunctions, sendInstruction, type DeviceFunctionVO } from '@/api/eiot/remote/deviceRemote.api'
import { getSubDeviceStats } from '@/api/eiot/firewatch/fireDevice.api'
import { getUnprocessedAlertsByParentDevice, handleAlertRecord } from '@/api/eiot/firewatch/firewatch.api'

const router = useRouter()
const route = useRoute()

const treeData = ref<DeviceTreeNodeVO[]>([])
const selectedDeviceId = ref<string | number>(0)
const selectedDeviceInfo = ref<DeviceTreeItemVO | null>(null)
const selectedEnterpriseName = ref<string>('--')
const deviceFunctions = ref<DeviceFunctionVO[]>([])
const cameras = ref<CameraVO[]>([])
const pointTotal = ref(0)
const pointOnline = ref(0)

// ====== 右上角展示人员（展示岗位人员 + 当前值班人员，随选中单位切换） ======
const dutyPersons = ref<any[]>([])

const loadCurrentDuty = async (tenantId?: string | number) => {
  if (!tenantId) {
    dutyPersons.value = []
    return
  }
  try {
    const numTenantId = Number(tenantId)
    // 后端已按展示顺序返回：1. 展示岗位人员（按岗位顺序）；2. 当前值班人员（去重）
    const list = (await getConsolePersons(numTenantId)) || []
    dutyPersons.value = list.map((item: any) => ({
      dutyUserName: item.userName,
      dutyUserPhone: item.userPhone,
      dutyUserPosition: item.position || ''
    }))
  } catch (e) {
    dutyPersons.value = []
  }
}

const verifyVisible = ref(false)
const currentActionName = ref('确认执行此操作？')
const pendingAction = ref<{ fn: DeviceFunctionVO; commandType: string } | null>(null)

// ====== 滚动告警弹窗 ======
const pendingAlerts = ref<any[]>([])
let alertPollingTimer: ReturnType<typeof setInterval> | null = null
let alertScrollTimer: ReturnType<typeof setInterval> | null = null
const scrollAlertIndex = ref(0)

const currentScrollAlert = computed(() => {
  if (pendingAlerts.value.length === 0) return null
  return pendingAlerts.value[scrollAlertIndex.value % pendingAlerts.value.length]
})

const startAlertScroll = () => {
  if (alertScrollTimer) clearInterval(alertScrollTimer)
  alertScrollTimer = setInterval(() => {
    if (pendingAlerts.value.length > 1) {
      scrollAlertIndex.value = (scrollAlertIndex.value + 1) % pendingAlerts.value.length
    }
  }, 5000)
}

// 告警设备摄像头（从 alertDeviceId 加载）
const alertDeviceCameras = ref<CameraVO[]>([])
const displayCameras = computed(() => {
  // 如果有告警设备摄像头，优先展示
  if (alertDeviceCameras.value.length > 0) return alertDeviceCameras.value
  return cameras.value
})

const getAlertLevel = (alert: any) => {
  const level = alert?.level
  if (level === '1' || level === 'critical') return 'critical'
  if (level === '2' || level === 'major') return 'major'
  if (level === '3' || level === 'warning') return 'warning'
  return 'info'
}

const getAlertLevelText = (alert: any) => {
  const map: Record<string, string> = { critical: '紧急', major: '较重', warning: '一般', info: '提示' }
  return map[getAlertLevel(alert)] || '未知'
}

const formatAlertTime = (t: any) => {
  if (!t) return ''
  if (typeof t === 'number') return new Date(t).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  return String(t).replace('T', ' ').substring(0, 19)
}

const handleAlertFromPanel = async (alert: any) => {
  if (alert?.id) {
    try {
      await handleAlertRecord(alert.id)
    } catch (e) {
      console.warn('处置告警失败:', e)
    }
  }
  // 从列表中移除
  pendingAlerts.value = pendingAlerts.value.filter(a => a.id !== alert.id)
  // 修正滚动索引
  if (scrollAlertIndex.value >= pendingAlerts.value.length) {
    scrollAlertIndex.value = 0
  }
  // 如果报警设备与当前选中设备不同，加载报警设备的摄像头
  const alertDeviceId = alert?.deviceId
  if (alertDeviceId && String(alertDeviceId) !== String(selectedDeviceId.value)) {
    loadAlertDeviceCameras(alertDeviceId)
  }
}

const loadAlertDeviceCameras = async (deviceId: string | number) => {
  if (!deviceId) return
  try {
    alertDeviceCameras.value = await getDeviceCameras(deviceId) || []
  } catch (e) {
    alertDeviceCameras.value = []
  }
}

// 轮询当前设备+子设备的未处置告警
const fetchDeviceAlerts = async () => {
  if (!selectedDeviceId.value) return
  try {
    // 后端按父设备ID查询，自动覆盖父设备自身 + 其子设备的告警
    const res = await getUnprocessedAlertsByParentDevice(Number(selectedDeviceId.value), 5)
    const alerts = (Array.isArray(res) ? res : res?.data) || []
    pendingAlerts.value = alerts
    // 启动/重启滚动定时器
    if (alerts.length > 0) startAlertScroll()
  } catch (e) {
    // 静默失败
  }
}

const findDeviceNode = (deviceId: string | number) => {
  for (const node of treeData.value) {
    const device = node.devices?.find(d => String(d.id) === String(deviceId))
    if (device) return { node, device }
  }
  return null
}

const applySelectedDevice = (device: DeviceTreeItemVO, enterpriseName?: string, nodeId?: string | number) => {
  selectedDeviceId.value = device.id
  selectedDeviceInfo.value = device
  selectedEnterpriseName.value = enterpriseName || '--'
  loadCameras(device.id)
  loadDeviceFunctions(device.id)
  loadSubDeviceStats(device.id)
  // 设备树节点 id 为租户ID，按租户加载当前值班人员
  loadCurrentDuty(nodeId)
}

const loadDeviceTree = async () => {
  try {
    const res = await getDeviceTree()
    treeData.value = res || []
    const queryDeviceId = route.query.deviceId as string | undefined
    const alertDeviceId = route.query.alertDeviceId as string | undefined

    if (queryDeviceId) {
      const found = findDeviceNode(queryDeviceId)
      if (found) {
        applySelectedDevice(found.device, found.node.name, found.node.id)
        // 如果有报警设备ID，加载其摄像头
        if (alertDeviceId) {
          loadAlertDeviceCameras(alertDeviceId)
        }
        return
      }
      // 路由传入了 deviceId 但树中找不到（可能是子设备），直接使用传入的 ID
      console.warn('[RemoteConsole] 设备', queryDeviceId, '未在树中找到，直接使用传入ID')
      selectedDeviceId.value = queryDeviceId
      selectedDeviceInfo.value = { id: queryDeviceId, name: '报警设备', state: 0 }
      selectedEnterpriseName.value = '--'
      loadCameras(queryDeviceId)
      loadDeviceFunctions(queryDeviceId)
      loadSubDeviceStats(queryDeviceId)
      if (alertDeviceId) {
        loadAlertDeviceCameras(alertDeviceId)
      }
      return
    }
    const firstNode = res?.[0]
    const first = firstNode?.devices?.[0]
    if (first) applySelectedDevice(first, firstNode?.name, firstNode?.id)
  } catch (e) {
    treeData.value = []
  }
}

const loadDeviceFunctions = async (deviceId: string | number) => {
  if (!deviceId) { deviceFunctions.value = []; return }
  try { deviceFunctions.value = await getDeviceFunctions(deviceId) || [] }
  catch (e) { deviceFunctions.value = [] }
}

const loadCameras = async (deviceId: string | number) => {
  if (!deviceId) { cameras.value = []; return }
  try { cameras.value = await getDeviceCameras(deviceId) || [] }
  catch (e) { cameras.value = [] }
}

const loadSubDeviceStats = async (deviceId: string | number) => {
  if (!deviceId) { pointTotal.value = 0; pointOnline.value = 0; return }
  try {
    // 后端统计接口：一次返回子设备总数与在线数，避免全量拉取
    const stats = await getSubDeviceStats(Number(deviceId))
    pointTotal.value = stats?.total ?? 0
    pointOnline.value = stats?.online ?? 0
  } catch (e) {
    pointTotal.value = 0
    pointOnline.value = 0
  }
}

const refreshData = () => {
  if (selectedDeviceId.value) {
    loadCameras(selectedDeviceId.value)
    loadDeviceFunctions(selectedDeviceId.value)
  }
}

const openVerify = (actionName: string, fn: DeviceFunctionVO, commandType: string) => {
  currentActionName.value = `确认${actionName}？`
  pendingAction.value = { fn, commandType }
  verifyVisible.value = true
}

const handleGeneralAction = (fn: DeviceFunctionVO) => {
  const actionName = fn.buttonName || fn.functionName || '执行'
  openVerify(actionName, fn, '1')
}

const handleCardAction = (fn: DeviceFunctionVO, _panelType: 'MULTI' | 'BUS', actionType: 'start' | 'stop') => {
  const actionName = actionType === 'start' ? '启动' : '停止'
  openVerify(`${fn.buttonName || fn.functionName} - ${actionName}`, fn, actionType === 'start' ? '1' : '0')
}

const goBackToDuty = () => {
  // 独立页面（/remote-console）退出时优先返回上一层级（如设备控制室、告警大屏等来源页面），
  // 无历史记录可回退时兜底到大屏首页；弹窗内嵌场景保持兜底跳转
  if (route.path === '/remote-console' && window.history.state?.back) {
    router.back()
  } else {
    router.push('/duty-screen')
  }
}

const handleVerifyConfirm = async (data: { password: string }) => {
  if (!pendingAction.value) {
    ElMessage.success('验证通过')
    return
  }
  const { fn, commandType } = pendingAction.value
  if (!selectedDeviceId.value) {
    ElMessage.warning('请先选择设备')
    return
  }
  try {
    const res = await sendInstruction({
      deviceId: selectedDeviceId.value,
      deviceFunctionId: fn.id,
      password: data.password,
      commandType
    })
    if (res?.success) ElMessage.success('指令下发成功')
    else ElMessage.warning(res?.message || '指令下发失败')
  } catch (e: any) {
    ElMessage.error(e?.message || '指令下发失败')
  }
}

// 智能空开控制（开闸/合闸）
const handleValveControl = async (action: 'open' | 'close') => {
  if (!selectedDeviceId.value) {
    ElMessage.warning('请先选择设备')
    return
  }
  const actionName = action === 'open' ? '开闸' : '合闸'
  try {
    await ElMessageBox.confirm(`确认执行${actionName}操作？`, '操作确认', {
      confirmButtonText: '确认执行',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    const res = await valveControl(action, selectedDeviceId.value)
    if (res === true) ElMessage.success(`${actionName}成功`)
    else ElMessage.warning(`${actionName}失败`)
  } catch (e: any) {
    ElMessage.error(e?.message || `${actionName}失败`)
  }
}

onMounted(() => {
  loadDeviceTree()
  // 启动告警轮询
  setTimeout(() => {
    fetchDeviceAlerts()
    alertPollingTimer = setInterval(fetchDeviceAlerts, 10000)
  }, 2000)
})

onUnmounted(() => {
  if (alertPollingTimer) clearInterval(alertPollingTimer)
  if (alertScrollTimer) clearInterval(alertScrollTimer)
})
</script>

<style lang="scss" scoped>
@import './styles/common.scss';

.remote-console {
  display: flex; flex-direction: column; height: 100vh; background: $bg-primary; overflow: hidden;
  color: $text-primary; font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  @include scrollbar;
}

.header {
  height: 52px; background: $bg-panel; border-bottom: 1px solid $border-color;
  display: flex; align-items: center; justify-content: space-between; padding: 0 24px; flex-shrink: 0;
}
.header-title { font-size: 18px; font-weight: bold; letter-spacing: 2px; color: $text-primary; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.header-btn {
  display: flex; align-items: center; gap: 6px; padding: 6px 14px;
  background: rgba(255,255,255,0.05); border: 1px solid $border-color; border-radius: 4px;
  color: $text-secondary; font-size: 13px; cursor: pointer;
  &:hover { border-color: $primary; color: $primary; }
  &.close:hover { border-color: $danger; color: $danger; }
}

.main-container {
  flex: 1; display: flex; overflow: hidden; min-height: 0;
}
.left-panel { width: 22%; min-width: 260px; overflow: hidden; }
.center-panel {
  flex: 1; display: flex; flex-direction: column; gap: 16px;
  padding: 16px; overflow-y: auto; min-width: 0;
}
.right-panel { width: 22%; min-width: 260px; display: flex; flex-direction: column; overflow: hidden; }

/* 智能空开控制区域 */
.valve-control-section {
  background: $bg-panel;
  border-radius: 8px;
  padding: 12px 16px;
  border: 1px solid $border-color;
  display: flex;
  align-items: center;
  gap: 16px;
}
.valve-section-title {
  font-size: 15px;
  font-weight: 500;
  color: $text-primary;
  white-space: nowrap;
}
.valve-buttons {
  display: flex;
  gap: 12px;
}
.valve-btn {
  padding: 8px 28px;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  &.valve-open {
    background: darken($success, 18%);
    border-color: darken($success, 12%);
    &:hover:not(:disabled) {
      background: darken($success, 10%);
      box-shadow: 0 0 10px rgba($success, 0.35);
    }
  }
  &.valve-close {
    background: darken($danger, 18%);
    border-color: darken($danger, 12%);
    &:hover:not(:disabled) {
      background: darken($danger, 10%);
      box-shadow: 0 0 10px rgba($danger, 0.35);
    }
  }
  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}
</style>

<!-- 滚动告警弹窗 + 弹窗深色主题（非 scoped） -->
<style lang="scss">
@import './styles/common.scss';
.rc-scroll-alert {
  position: fixed;
  top: 56px;
  right: 20px;
  width: 320px;
  background: #0f2747;
  border: 1px solid rgba(59, 130, 246, 0.4);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  z-index: 9998;
  overflow: hidden;
  animation: rcScrollSlideIn 0.3s ease;

  &.level-critical { border-left: 3px solid #ff4d4f; }
  &.level-major { border-left: 3px solid #fa8c16; }
  &.level-warning { border-left: 3px solid #fadb14; }
  &.level-info { border-left: 3px solid #3b82f6; }
}

@keyframes rcScrollSlideIn {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}

.rc-scroll-alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 77, 79, 0.08);
  border-bottom: 1px solid rgba(30, 58, 95, 0.8);
}

.rc-scroll-alert-badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 8px;
  font-weight: 600;
  &.sev-critical { background: rgba(255,77,79,0.2); color: #ff8a8a; }
  &.sev-major { background: rgba(250,140,22,0.2); color: #ffa940; }
  &.sev-warning { background: rgba(250,219,20,0.2); color: #fadb14; }
  &.sev-info { background: rgba(24,144,255,0.2); color: #40a9ff; }
}

.rc-scroll-alert-device {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rc-scroll-alert-count {
  font-size: 11px;
  color: #6b7b9a;
  flex-shrink: 0;
}

.rc-scroll-alert-body {
  padding: 8px 12px;
}

.rc-scroll-alert-content {
  font-size: 12px;
  color: #8b9dc5;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rc-scroll-alert-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  border-top: 1px solid rgba(30, 58, 95, 0.5);
}

.rc-scroll-alert-time {
  font-size: 11px;
  color: #6b7b9a;
}

.rc-scroll-alert-btn {
  padding: 3px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #ff4d4f, #ff6b6b);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: linear-gradient(135deg, #ff7875, #ff8a8a); }
}

/* ====== 弹窗深色主题 ====== */
.el-dialog.remote-console-modal {
  background: linear-gradient(170deg, #0e2c4e 0%, #0a2442 50%, #081e38 100%);
  border: 1px solid rgba(0, 180, 255, 0.2);
  border-radius: 10px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.6), 0 0 1px rgba(0, 180, 255, 0.15);
  overflow: hidden;

  .el-dialog__header {
    background: rgba(0, 180, 255, 0.06);
    border-bottom: 1px solid rgba(30, 58, 95, 0.6);
    padding: 14px 20px;
    margin: 0;
  }
  .el-dialog__title {
    color: $text-primary;
    font-size: 15px;
    font-weight: 600;
    letter-spacing: 1px;
  }
  .el-dialog__headerbtn {
    color: $text-secondary;
    &:hover { color: $danger; }
  }
  .el-dialog__body {
    padding: 18px 20px;
    color: $text-secondary;
  }
  .el-dialog__footer {
    padding: 12px 20px;
    border-top: 1px solid rgba(30, 58, 95, 0.5);
  }
  .el-form-item__label {
    color: $text-secondary !important;
  }
  .el-input__wrapper {
    background: rgba(15, 39, 71, 0.8) !important;
    border: 1px solid rgba(30, 58, 95, 0.6) !important;
    box-shadow: none !important;
    border-radius: 6px;
    &:hover, &.is-focus {
      border-color: rgba(0, 180, 255, 0.5) !important;
      box-shadow: 0 0 6px rgba(0, 180, 255, 0.15) !important;
    }
  }
  .el-input__inner {
    color: #fff !important;
    &::placeholder { color: rgba(139, 157, 197, 0.5) !important; }
  }
  .el-input__prefix .el-icon,
  .el-input__suffix .el-icon {
    color: $text-secondary;
  }
  .el-button {
    border-radius: 6px;
  }
  .el-button--default {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(30, 58, 95, 0.6);
    color: $text-secondary;
    &:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(0, 180, 255, 0.4);
      color: #fff;
    }
  }
  .el-button--primary {
    background: linear-gradient(135deg, $primary, lighten($primary, 10%));
    border-color: transparent;
    &:hover {
      background: linear-gradient(135deg, lighten($primary, 8%), lighten($primary, 18%));
    }
  }
  .el-select .el-input__wrapper,
  .el-textarea__inner {
    background: rgba(15, 39, 71, 0.8) !important;
    border: 1px solid rgba(30, 58, 95, 0.6) !important;
    box-shadow: none !important;
    color: #fff !important;
    border-radius: 6px;
    &:hover, &:focus {
      border-color: rgba(0, 180, 255, 0.5) !important;
    }
  }
  .el-table {
    --el-table-bg-color: transparent;
    --el-table-tr-bg-color: transparent;
    --el-table-header-bg-color: rgba(0, 180, 255, 0.06);
    --el-table-text-color: #{$text-secondary};
    --el-table-header-text-color: #{$text-primary};
    --el-table-border-color: rgba(30, 58, 95, 0.4);
  }
}
</style>
