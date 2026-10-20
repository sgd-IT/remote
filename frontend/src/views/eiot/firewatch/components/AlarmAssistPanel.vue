<template>
  <div class="assist-section">
    <div class="assist-heading">
      <strong>辅助判断</strong>
      <button class="assist-link contact-link">联系现场核实</button>
      <button
        class="assist-link"
        :aria-label="expanded ? '收起辅助信息' : '展开辅助信息'"
        @click="expanded = !expanded"
      >
        {{ expanded ? '收起' : '展开' }} {{ expanded ? '⌃' : '⌄' }}
      </button>
    </div>
    <div v-if="expanded" class="evidence-detail">
      <div v-if="loading" class="assist-tip">正在加载辅助信息…</div>
      <div v-else-if="loadFailed" class="assist-tip assist-tip-error">
        辅助信息加载失败，请稍后重试
      </div>
      <div v-else-if="!data" class="assist-tip">暂无辅助信息</div>
      <div v-else-if="!data.alarmDevice" class="assist-tip">该告警未关联设备</div>
      <template v-else>
        <div class="assist-space-line">
          <span class="assist-space-label">安装空间</span>
          <span class="assist-space-value">{{ spaceText }}</span>
        </div>
        <div v-for="row in rows" :key="row.deviceId" class="dark-evidence-row">
          <div class="resource-main">
            <svg
              class="device-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path :d="iconOf(row.type)" />
            </svg>
            <div>
              <div class="resource-name-line">
                <span class="resource-name">{{ row.name || '未命名设备' }}</span>
                <span v-if="row.isAlarmDevice" class="alarm-device-tag">报警设备</span>
              </div>
              <small>{{ row.location || '位置未配置' }}</small>
            </div>
          </div>
          <div class="evidence-row-right">
            <span class="evidence-state" :class="stateClassOf(row)">{{ stateTextOf(row) }}</span>
            <small>{{ timeTextOf(row) }}</small>
            <div v-if="row.cameras.length || row.type === 'transfer'" class="evidence-links">
              <button v-if="row.cameras.length" class="assist-link" @click="goVideo(row)">
                实时预览
              </button>
              <button v-if="row.type === 'transfer'" class="assist-link" @click="goConsole(row)">
                设备控制室
              </button>
            </div>
          </div>
        </div>
        <div v-if="!hasRelated" class="assist-tip">同区域暂无其他设备</div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { formatDate } from '@/utils/formatTime'
import {
  getAlarmAssist,
  type AlarmAssistCamera,
  type AlarmAssistData,
  type AlarmAssistDevice
} from '@/api/eiot/firewatch/alarmAssist.api'

const props = defineProps<{ alarmId?: number | string | null }>()
// 数据加载结果上抛：弹窗侧据此渲染主图区「实时预览」入口（取决于报警设备 ownCameras）
const emit = defineEmits<{
  loaded: [data: AlarmAssistData | null]
  /** 行内「实时预览」：把该设备行的摄像头交给弹窗侧打开实时视频弹层（与主图区入口统一） */
  viewVideo: [payload: { cameras: AlarmAssistCamera[]; title: string }]
}>()

const router = useRouter()
const expanded = ref(true)
const loading = ref(false)
const loadFailed = ref(false)
const data = ref<AlarmAssistData | null>(null)

// 请求序号：快速切换告警时丢弃过期响应，防止旧数据覆盖新数据
let requestSeq = 0

const load = async () => {
  const id = Number(props.alarmId)
  if (!props.alarmId || Number.isNaN(id)) {
    data.value = null
    loading.value = false
    loadFailed.value = false
    emit('loaded', null)
    return
  }
  const seq = ++requestSeq
  loading.value = true
  loadFailed.value = false
  try {
    const res = await getAlarmAssist(id)
    if (seq !== requestSeq) return
    data.value = res || null
    emit('loaded', data.value)
  } catch (e) {
    if (seq !== requestSeq) return
    console.warn('[AlarmAssistPanel] 辅助判断加载失败:', e)
    loadFailed.value = true
    emit('loaded', null)
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

// 弹窗展示某条告警时按 alarmId 拉取一次（不做随轮询刷新）；失败保留区块仅作提示
watch(() => props.alarmId, () => load(), { immediate: true })

interface AssistRow extends AlarmAssistDevice {
  isAlarmDevice: boolean
}

// 状态展示（互斥优先级，颜色随文案）：报警中(红) > 离线(黄) > 可查看 > 未报警(灰)
const stateTextOf = (row: AssistRow) =>
  row.alarming ? '报警中' : row.online === false ? '离线' : row.cameras.length > 0 ? '可查看' : '未报警'
const stateClassOf = (row: AssistRow) => {
  const text = stateTextOf(row)
  return text === '报警中' ? 'danger' : text === '离线' ? 'warn' : ''
}
// 列表排序与状态优先级同序：报警中 → 离线 → 可查看 → 未报警；同级保持原相对顺序
const STATE_RANK: Record<string, number> = { 报警中: 0, 离线: 1, 可查看: 2, 未报警: 3 }

const rows = computed<AssistRow[]>(() => {
  const d = data.value
  if (!d) return []
  const list: AssistRow[] = []
  // 报警设备自身也入列并标注；其视频入口用 ownCameras（报警设备 VO 不含 cameras 字段）
  if (d.alarmDevice) {
    list.push({
      ...d.alarmDevice,
      cameras: d.ownCameras || [],
      isAlarmDevice: true
    })
  }
  for (const item of d.relatedDevices || []) {
    list.push({ ...item, cameras: item.cameras || [], isAlarmDevice: false })
  }
  // Array.sort 稳定：同状态保持原相对顺序（报警设备自身在同状态中仍居首）
  return list.sort(
    (a, b) => (STATE_RANK[stateTextOf(a)] ?? 9) - (STATE_RANK[stateTextOf(b)] ?? 9)
  )
})

const hasRelated = computed(() => (data.value?.relatedDevices?.length ?? 0) > 0)
const spaceText = computed(() => data.value?.space?.path || '未配置安装空间')

// 设备类型 → 图标路径；未登记类型回退通用图标
const ICON_PATHS: Record<string, string> = {
  camera: 'M3 6h12v12H3z M15 10l6-3v10l-6-3',
  smoke: 'M5 13a7 4 0 0 1 14 0v3a7 4 0 0 1-14 0z M8 5c-2 2 2 2 0 4 M12 3c-2 2 2 2 0 4 M16 5c-2 2 2 2 0 4',
  temp_humidity: 'M9 5a3 3 0 0 1 6 0v9a5 5 0 1 1-6 0z M12 8v9 M17 6h3 M17 10h3',
  temperature: 'M9 5a3 3 0 0 1 6 0v9a5 5 0 1 1-6 0z M12 8v9 M17 6h3 M17 10h3',
  water: 'M12 3C9 8 5 11 5 15a7 7 0 0 0 14 0c0-4-4-7-7-12z M8 15a4 4 0 0 0 4 4',
  door_magnet: 'M6 3h12v18H6z M15 12h.01',
  transfer: 'M4 7h16v10H4z M8 11h8 M8 13h5'
}
const DEFAULT_ICON = 'M4 4h16v16H4z M9 9h6v6H9z'
const iconOf = (type?: string) => ICON_PATHS[type || ''] || DEFAULT_ICON

const timeTextOf = (row: AssistRow) =>
  row.dataTime ? formatDate(new Date(row.dataTime), 'HH:mm:ss') : '—'

// 行内能力链接：实时预览 → 视频弹层（与主图区入口统一，可多路切换）；
// 设备控制室仅"用传设备"行显示（远程控制只对用传设备有意义），跳转带该用传设备ID
const goVideo = (row: AssistRow) => {
  if (!row.cameras.length) return
  emit('viewVideo', { cameras: row.cameras, title: row.name || '' })
}
const goConsole = (row: AssistRow) => {
  if (!row.transferDeviceId) return
  router.push({
    path: '/remote-console',
    query: { deviceId: String(row.transferDeviceId), alertDeviceId: String(row.deviceId) }
  })
}
</script>

<style scoped lang="scss">
/* ====== 报警弹窗辅助判断（真实数据：安装空间/同区域设备/视频与控制室入口） ====== */
.assist-section {
  border-top: 1px solid rgba(30, 58, 95, 0.8);
  padding: 15px 20px 12px;
  background: rgba(30, 58, 95, 0.16);
}

.assist-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #fff;
  flex-wrap: nowrap;

  strong {
    white-space: nowrap;
  }
  .assist-link {
    margin-left: 0;
    white-space: nowrap;
  }
  .contact-link {
    margin-left: auto;
  }
}

// 行内链接（实时预览/设备控制室）：与弹窗主图区入口同色系，hover 统一变亮
.assist-link {
  border: 0;
  padding: 2px 0;
  background: transparent;
  color: #4fc3f7;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s;
  &:hover {
    color: #fff;
  }
}

.evidence-detail {
  border-top: 1px solid #284264;
  margin: 12px 0 0;
  padding-top: 0;
  /* 列表区定高（约原高度减 30px）：超出走内部滚动，滚动条默认隐藏、悬停才显现 */
  max-height: 350px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;

  &:hover {
    scrollbar-color: rgba(79, 195, 247, 0.35) transparent;
  }

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: transparent;
    border-radius: 2px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &:hover::-webkit-scrollbar-thumb {
    background: rgba(79, 195, 247, 0.35);
  }
}

.assist-space-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 9px 0 2px;

  .assist-space-label {
    flex: none;
    font-size: 12px;
    color: #8b9dc5;
  }

  .assist-space-value {
    font-size: 12px;
    color: #fff;
  }
}

.assist-tip {
  padding: 10px 0 2px;
  font-size: 12px;
  color: #8b9dc5;

  &.assist-tip-error {
    color: #ff8a8a;
  }
}

.dark-evidence-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 9px 0;
  border-bottom: 1px solid rgba(44, 79, 133, 0.4);

  &:last-child {
    border-bottom: 0;
  }

  small {
    display: block;
    font-size: 11px;
    color: #8b9dc5;
    margin-top: 4px;
  }
}

.resource-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;

  .device-icon {
    color: #4fc3f7;
  }

  small {
    line-height: 1.5;
    max-width: 480px;
  }
}

.resource-name-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resource-name {
  color: #e7f1ff;
  font-size: 13px;
  font-weight: 500;
}

.alarm-device-tag {
  flex: none;
  padding: 0 6px;
  border: 1px solid rgba(79, 195, 247, 0.5);
  border-radius: 3px;
  font-size: 11px;
  line-height: 16px;
  color: #4fc3f7;
}

.evidence-row-right {
  text-align: right;
  flex-shrink: 0;
}

.evidence-links {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

.evidence-state {
  font-size: 12px;
  color: #a9bbd6;

  &.danger {
    color: #ff8a8a;
  }

  &.warn {
    color: #ffc477;
  }
}

.device-icon {
  width: 22px;
  height: 22px;
  flex: none;
  vertical-align: middle;
}
</style>
