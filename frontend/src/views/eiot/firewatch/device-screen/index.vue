<template>
  <div class="device-screen">
    <header class="header">
      <div class="header-left">
        <h1 class="title">设备控制室</h1>
        <p class="subtitle">DEVICE CONTROL ROOM</p>
      </div>
      <div class="header-center">
        <div class="datetime">
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
          <span class="week">{{ currentWeek }}</span>
        </div>
      </div>
      <div class="header-right">
        <div class="exit-btn" @click="goBack" title="退出">
          <el-icon><Close /></el-icon>
        </div>
      </div>
    </header>

    <main class="main-body">
      <div class="content-area">
        <!-- 左侧企业树（全国省市区 + 企业，默认折叠，带搜索） -->
        <aside class="tenant-panel">
          <div class="tenant-panel-header">
            <span class="section-title"><el-icon><OfficeBuilding /></el-icon>企业列表</span>
            <span class="panel-count" v-if="propertyList.length">{{ propertyList.length }}</span>
          </div>
          <div class="tree-search">
            <el-input
              v-model="searchKeyword"
              placeholder="输入企业名称搜索"
              clearable
              :prefix-icon="Search"
              size="small" />
          </div>
          <div class="tree-wrap" v-loading="treeLoading">
            <el-tree
              ref="treeRef"
              :data="propertyTree"
              :props="treeProps"
              node-key="key"
              :highlight-current="true"
              :expand-on-click-node="true"
              :filter-node-method="filterNode"
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <span class="tree-node" :class="data.type">
                  <el-icon v-if="data.type === 'province'" class="node-ico"><Location /></el-icon>
                  <el-icon v-else-if="data.type === 'city'" class="node-ico"><Position /></el-icon>
                  <el-icon v-else-if="data.type === 'district'" class="node-ico"><MapLocation /></el-icon>
                  <el-icon v-else class="node-ico"><OfficeBuilding /></el-icon>
                  <span class="node-label" :title="node.label">{{ node.label }}</span>
                  <span class="node-count" v-if="data.type === 'district' && data.children?.length">({{ data.children.length }})</span>
                </span>
              </template>
            </el-tree>
            <el-empty v-if="!treeLoading && !propertyList.length" description="暂无企业数据" :image-size="60" />
          </div>
        </aside>

        <!-- 右侧父设备列表 -->
        <div class="device-section">
          <div class="device-section-header">
            <span class="section-title">
              <el-icon><Monitor /></el-icon>
              {{ selectedProperty ? selectedProperty.name + ' · 父设备' : '父设备列表' }}
            </span>
            <div class="result-info" v-if="selectedProperty">
              共 <b>{{ parentDevices.length }}</b> 台父设备
            </div>
          </div>

          <div class="device-card-grid" v-loading="deviceLoading">
            <DeviceCard
              v-for="device in parentDevices"
              :key="device.id"
              :device="device"
              :selected="selectedDeviceId === device.id"
              :show-history="false"
              :show-actions="false"
              horizontal
              @select="selectDevice"
              @tab-change="handleTabChange" />
            <div class="empty-state" v-if="!deviceLoading && !parentDevices.length">
              <el-icon :size="44"><Monitor /></el-icon>
              <span>{{ selectedProperty ? '该企业暂无父设备' : '请从左侧选择企业' }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 子设备弹窗 -->
    <SubDeviceModal
      v-model="subDeviceVisible"
      :parent-id="subDeviceParentId"
      :parent-name="subDeviceParentName"
      :line-type-filter="subDeviceLineTypeFilter"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  Monitor, OfficeBuilding, Close,
  Location, Position, MapLocation, Search
} from '@element-plus/icons-vue'
import { getAreaTree } from '@/api/system/area'
import {
  getPropertyList, getParentDevices,
  type FirePropertyItem, type FireDeviceItem
} from '@/api/eiot/firewatch/fireDevice.api'
import DeviceCard from '../alarm-screen/components/DeviceCard.vue'
import SubDeviceModal from './components/SubDeviceModal.vue'

defineOptions({ name: 'FireWatchDeviceScreen' })

const router = useRouter()

// 时间
const currentDate = ref(''),
  currentTime = ref(''),
  currentWeek = ref('')
const updateDateTime = () => {
  const now = new Date()
  const w = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  currentDate.value = now
    .toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
    .replace(/\//g, '-')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  currentWeek.value = w[now.getDay()]
}

// 退出：优先返回上一级，兜底跳转大屏
const goBack = () => {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push('/duty-screen')
  }
}

// ============ 状态映射 ============
const STATE_MAP: Record<string, string> = { '0': 'offline', '1': 'normal', '3': 'inactive', '4': 'fault' }
const STATUS_TEXT: Record<string, string> = { normal: '正常', alarm: '告警', fault: '故障', offline: '离线', inactive: '未激活' }

// ============ 企业树（全国省市区 + 企业） ============
const propertyList = ref<FirePropertyItem[]>([])
const treeLoading = ref(false)
const treeProps = { label: 'label', children: 'children' }
const treeRef = ref()
const searchKeyword = ref('')

interface TreeNode {
  key: string
  label: string
  type: 'province' | 'city' | 'district' | 'property'
  data?: FirePropertyItem
  children?: TreeNode[]
}

// 区域树（来自后端 /system/area/tree，与租户管理用同一数据源，保证命名一致）
const areaTree = ref<any[]>([])

// 区域名称规范化：去掉末尾行政区划后缀（省/市/区/县/街道/镇/乡等）
// 用于匹配不同数据源的区域名称（如物业手输"南城街道" vs 区域树"南城区" → 均规范化为"南城"）
const normalizeArea = (name: string): string => {
  if (!name) return ''
  let n = name.trim()
  const suffixes = ['特别行政区', '自治区', '自治州', '自治县', '地区', '街道', '林区', '省', '市', '区', '县', '镇', '乡', '盟']
  let changed = true
  while (changed) {
    changed = false
    for (const s of suffixes) {
      if (n.length > s.length && n.endsWith(s)) {
        n = n.slice(0, -s.length)
        changed = true
        break
      }
    }
  }
  return n
}

// 物业按 province>city>district 规范化文本索引（用于挂到区域树下）
const propertyIndex = computed(() => {
  const idx = new Map<string, Map<string, Map<string, FirePropertyItem[]>>>()
  propertyList.value.forEach(p => {
    const prov = normalizeArea(p.province || '')
    const city = normalizeArea(p.city || '')
    const dist = normalizeArea(p.district || '')
    if (!prov) return
    if (!idx.has(prov)) idx.set(prov, new Map())
    const cm = idx.get(prov)!
    if (!cm.has(city)) cm.set(city, new Map())
    const dm = cm.get(city)!
    if (!dm.has(dist)) dm.set(dist, [])
    dm.get(dist)!.push(p)
  })
  return idx
})

// 区域树（后端 areaTree：{id,name,children}），区县下挂载匹配的企业；无企业的区县显示为空叶子
const propertyTree = computed<TreeNode[]>(() => {
  const idx = propertyIndex.value
  const buildDistricts = (distNodes: any[], prefix: string, provNorm: string, cityNorm: string): TreeNode[] =>
    (distNodes || []).map((d, di) => {
      const dNorm = normalizeArea(d.name)
      const props = idx.get(provNorm)?.get(cityNorm)?.get(dNorm) || []
      return {
        key: `${prefix}-d${di}`,
        label: d.name,
        type: 'district' as const,
        children: props.map(p => ({
          key: `prop-${p.id}`,
          label: p.name,
          type: 'property' as const,
          data: p
        }))
      }
    })
  const buildCities = (cityNodes: any[], prefix: string, provNorm: string): TreeNode[] =>
    (cityNodes || []).map((c, ci) => {
      const cNorm = normalizeArea(c.name)
      return {
        key: `${prefix}-c${ci}`,
        label: c.name,
        type: 'city' as const,
        children: buildDistricts(c.children, `${prefix}-c${ci}`, provNorm, cNorm)
      }
    })
  return (areaTree.value || []).map((p, pi) => ({
    key: `p${pi}`,
    label: p.name,
    type: 'province' as const,
    children: buildCities(p.children, `p${pi}`, normalizeArea(p.name))
  }))
})

// el-tree 过滤：企业节点匹配关键字；省市区节点由 el-tree 自动保留有匹配后代的路径
const filterNode = (value: string, data: TreeNode) => {
  if (!value) return true
  if (data.type === 'property') {
    return (data.label || '').toLowerCase().includes(String(value).toLowerCase())
  }
  return false
}

// 搜索时自动展开匹配路径
watch(searchKeyword, (val) => {
  treeRef.value?.filter(val)
  if (val) {
    nextTick(() => {
      const store = (treeRef.value as any)?.store
      if (!store?.root) return
      const hasVisibleDesc = (n: any): boolean =>
        (n.childNodes || []).some((c: any) => c.visible || hasVisibleDesc(c))
      const expandVisible = (node: any) => {
        if (!node) return
        if (node.visible && hasVisibleDesc(node) && !node.expanded) {
          node.expand?.(() => {}, false)
        }
        ;(node.childNodes || []).forEach(expandVisible)
      }
      expandVisible(store.root)
    })
  }
})

const handleNodeClick = (data: TreeNode) => {
  if (data.type === 'property' && data.data) {
    selectedProperty.value = data.data
    loadParentDevices(data.data.id)
  }
}

// ============ 父设备列表（DeviceCard 格式） ============
const selectedProperty = ref<FirePropertyItem | null>(null)
const parentDevices = ref<any[]>([])
const deviceLoading = ref(false)
const selectedDeviceId = ref<number | string>('')

const selectDevice = (device: any) => {
  selectedDeviceId.value = selectedDeviceId.value === device.id ? '' : device.id
}

// 把后端 FireDeviceItem 转为 DeviceCard 期望的数据结构
const toDeviceCardData = (d: FireDeviceItem, propertyName: string) => {
  const status = STATE_MAP[String(d.state)] || 'offline'
  return {
    id: d.id,
    name: d.name || d.dn || '未知设备',
    fullName: d.name || d.dn || '未知设备',
    brand: propertyName || '',
    dn: d.dn || '',
    status,
    statusText: STATUS_TEXT[status] || '离线',
    addr: d.addr || '-',
    address: d.addr || '-',
    propertyName: propertyName || '-',
    productKey: d.productKey || '',
    serialNo: d.serialNo || '-',
    deviceType: d.deviceType || '',
    tenantId: d.tenantId,
    fireCount: 0,
    faultCount: status === 'fault' ? 1 : 0,
    alarmText: status === 'fault' ? '设备故障' : '',
    activeTab: ''
  }
}

const loadParentDevices = async (propertyId: number) => {
  deviceLoading.value = true
  parentDevices.value = []
  selectedDeviceId.value = ''
  try {
    const res = await getParentDevices(propertyId)
    const list = Array.isArray(res) ? res : []
    const pname = selectedProperty.value?.name || ''
    parentDevices.value = list.map((d: FireDeviceItem) => toDeviceCardData(d, pname))
  } catch (e: any) {
    console.error('[DeviceScreen] load parent devices error:', e?.message || e)
    parentDevices.value = []
  } finally {
    deviceLoading.value = false
  }
}

// DeviceCard tab 切换：sub → 弹出子设备弹窗；bus → 弹出多线/总线子设备弹窗
const handleTabChange = (device: any, tab: string) => {
  device.activeTab = tab
  if (tab === 'sub') {
    openSubDevices(device, false)
  } else if (tab === 'bus') {
    openSubDevices(device, true)
  }
}

// ============ 子设备弹窗 ==========
const subDeviceVisible = ref(false)
const subDeviceParentId = ref<number | null>(null)
const subDeviceParentName = ref<string>('')
const subDeviceLineTypeFilter = ref<boolean>(false)

const openSubDevices = (device: any, lineTypeFilter = false) => {
  subDeviceParentId.value = device.id
  subDeviceParentName.value = device.name || device.dn || '未知设备'
  subDeviceLineTypeFilter.value = lineTypeFilter
  subDeviceVisible.value = true
}

// ============ 状态持久化（远程控制台返回时恢复选中企业与设备） ============
const STATE_KEY = 'device_screen_state'
const saveState = () => {
  if (selectedProperty.value) {
    sessionStorage.setItem(STATE_KEY, JSON.stringify({
      property: selectedProperty.value,
      deviceId: selectedDeviceId.value,
      propertyKey: `prop-${selectedProperty.value.id}`
    }))
  } else {
    sessionStorage.removeItem(STATE_KEY)
  }
}

// ============ 生命周期 ============
let clockTimer: any = null
onMounted(async () => {
  updateDateTime()
  clockTimer = setInterval(updateDateTime, 1000)
  // 区域树（与租户管理同一数据源）+ 物业列表并行加载
  treeLoading.value = true
  try {
    const [areaRes, propRes] = await Promise.all([
      getAreaTree().catch((e: any) => {
        console.error('[DeviceScreen] load area tree error:', e?.message || e)
        return []
      }),
      getPropertyList()
    ])
    areaTree.value = Array.isArray(areaRes) ? areaRes : []
    propertyList.value = Array.isArray(propRes) ? propRes : []
  } catch (e: any) {
    console.error('[DeviceScreen] init error:', e?.message || e)
    propertyList.value = []
  } finally {
    treeLoading.value = false
  }
  // 恢复上次选中状态（从远程控制台返回）
  try {
    const raw = sessionStorage.getItem(STATE_KEY)
    if (raw) {
      const s = JSON.parse(raw)
      if (s.property?.id) {
        selectedProperty.value = s.property
        await loadParentDevices(s.property.id)
        if (s.deviceId) selectedDeviceId.value = s.deviceId
        nextTick(() => { treeRef.value?.setCurrentKey(s.propertyKey) })
      }
    }
  } catch (e) {
    console.error('[DeviceScreen] restore state error:', e)
  }
})
onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  saveState()
})
</script>

<style lang="scss" scoped>
@use 'sass:color';
$bg1: #0a1628; $bg2: #0d1f3c; $bg3: #0f2747; $bd: #1e3a5f; $t1: #fff; $t2: #8b9dc5;
$blue: #1890ff; $red: #ff4d4f; $orange: #fa8c16; $green: #52c41a; $cyan: #4FC3F7;

// ====== HUD 卡片 mixin ======
%hud-card {
  position: relative;
  background: linear-gradient(170deg, rgba(18,37,67,0.97) 0%, rgba(18,35,61,0.97) 35%, rgba(21,44,79,0.97) 100%);
  border: 1px solid rgba(79,195,247,0.1);
  border-radius: 8px;
  box-shadow:
    0 2px 10px rgba(0,0,0,0.18),
    0 0 1px rgba(79,195,247,0.08),
    inset 0 1px 0 rgba(79,195,247,0.04),
    inset 0 0 40px rgba(79,195,247,0.012);
  &::before {
    content: '';
    position: absolute; top: -1px; left: -1px;
    width: 20px; height: 20px;
    border-top: 2px solid rgba(79,195,247,0.5);
    border-left: 2px solid rgba(79,195,247,0.5);
    border-radius: 8px 0 0 0;
    box-shadow: 0 0 8px rgba(79,195,247,0.15);
    z-index: 2;
  }
  &::after {
    content: '';
    position: absolute; bottom: -1px; right: -1px;
    width: 20px; height: 20px;
    border-bottom: 2px solid rgba(79,195,247,0.5);
    border-right: 2px solid rgba(79,195,247,0.5);
    border-radius: 0 0 8px 0;
    box-shadow: 0 0 8px rgba(79,195,247,0.15);
    z-index: 2;
  }
}

.device-screen {
  width: 100vw; height: 100vh;
  display: flex; flex-direction: column;
  background: linear-gradient(160deg, $bg1 0%, $bg2 50%, $bg3 100%);
  font-family: 'Microsoft YaHei', sans-serif;
  color: $t1; overflow: hidden;
}

// ====== Header ======
.header {
  height: 56px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; position: relative; z-index: 10;
  background: linear-gradient(170deg, rgba(18,37,67,0.97) 0%, rgba(18,35,61,0.97) 35%, rgba(21,44,79,0.97) 100%);
  border-bottom: 1px solid rgba(79,195,247,0.1);
  box-shadow: 0 2px 10px rgba(0,0,0,0.18), 0 0 1px rgba(79,195,247,0.08), inset 0 1px 0 rgba(79,195,247,0.04);
  .header-left {
    .title {
      font-size: 22px; font-weight: bold; margin: 0;
      background: linear-gradient(90deg, #fff, #7ec8f8, #54a0ff);
      -webkit-background-clip: text; -webkit-text-fill-color: transparent;
      letter-spacing: 3px;
    }
    .subtitle { font-size: 10px; color: $t2; letter-spacing: 1px; margin-top: 1px; opacity: 0.7; }
  }
  .header-center {
    position: absolute; left: 50%; transform: translateX(-50%);
    .datetime {
      display: flex; align-items: baseline; gap: 16px;
      .date { color: $t2; font-size: 14px; font-weight: 500; letter-spacing: 0.5px; }
      .time { font-size: 22px; font-weight: 800; color: $blue; letter-spacing: 1px; font-variant-numeric: tabular-nums; }
      .week { color: $t2; font-size: 14px; font-weight: 500; }
    }
  }
  .header-right {
    display: flex; align-items: center;
    .exit-btn {
      display: flex; align-items: center; justify-content: center;
      width: 32px; height: 32px; cursor: pointer;
      background: rgba(10,22,40,0.6); border: 1px solid rgba(79,195,247,0.2); border-radius: 4px;
      color: $t2; font-size: 16px;
      &:hover { border-color: $red; color: $red; }
    }
  }
}

// ====== 主体 ======
.main-body {
  flex: 1; min-height: 0; display: flex; flex-direction: column;
  padding: 10px; overflow: hidden;
}

// ====== 双栏内容区 ======
.content-area {
  flex: 1; min-height: 0; display: flex; gap: 10px; overflow: hidden;
}

// ====== 左侧企业树面板 ======
.tenant-panel {
  @extend %hud-card;
  width: 340px; flex-shrink: 0;
  display: flex; flex-direction: column;
  overflow: hidden;
  .tenant-panel-header {
    display: flex; align-items: center; justify-content: space-between;
    padding: 12px 16px; flex-shrink: 0;
    border-bottom: 1px solid rgba(79,195,247,0.12);
    position: relative;
    &::before {
      content: ''; position: absolute; left: 0; top: 20%; bottom: 20%;
      width: 3px; background: rgba(79,195,247,0.9);
      border-radius: 0 2px 2px 0; box-shadow: 0 0 6px rgba(79,195,247,0.5);
    }
    .section-title {
      display: flex; align-items: center; gap: 8px;
      font-size: 15px; font-weight: bold; color: #fff; letter-spacing: 0.5px;
      .el-icon { color: rgba(79,195,247,0.9); font-size: 16px; }
    }
    .panel-count {
      font-size: 12px; font-weight: 600; color: $cyan;
      padding: 1px 8px; border-radius: 10px;
      background: rgba($cyan, 0.15); border: 1px solid rgba($cyan, 0.25);
      min-width: 24px; text-align: center;
    }
  }
  .tree-search {
    padding: 8px 12px; flex-shrink: 0;
    border-bottom: 1px solid rgba(79,195,247,0.08);
    :deep(.el-input__wrapper) {
      background: rgba(10,22,40,0.6);
      box-shadow: 0 0 0 1px rgba(79,195,247,0.2) inset;
    }
    :deep(.el-input__inner) {
      color: rgba($t1, 0.9);
      &::placeholder { color: rgba($t2, 0.6); }
    }
    :deep(.el-input__prefix) { color: $t2; }
  }
  .tree-wrap {
    flex: 1; min-height: 0; overflow-y: auto; padding: 8px;
    scrollbar-width: none;
    &::-webkit-scrollbar { display: none; }
    :deep(.el-tree) {
      background: transparent; color: rgba($t1, 0.85);
      --el-tree-node-hover-bg-color: rgba($blue, 0.08);
      --el-tree-text-color: rgba($t1, 0.85);
      --el-tree-expand-icon-color: rgba(79, 195, 247, 0.7);
    }
    :deep(.el-tree-node__content) {
      height: 34px; background: transparent;
      &:hover { background: rgba($blue, 0.08); }
    }
    :deep(.el-tree-node__content > .el-tree-node__expand-icon) {
      color: rgba(79, 195, 247, 0.7);
      &.is-leaf { color: transparent; }
    }
    :deep(.el-tree-node.is-current > .el-tree-node__content) {
      background: rgba($cyan, 0.1);
    }
    .tree-node {
      display: flex; align-items: center; gap: 6px; flex: 1;
      padding: 2px 4px; min-width: 0;
      .node-ico {
        color: rgba(79, 195, 247, 0.6); font-size: 14px; flex-shrink: 0;
      }
      .node-label {
        font-size: 13px; color: rgba($t1, 0.85);
        overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
      }
      .node-count {
        font-size: 11px; color: $cyan; flex-shrink: 0;
        background: rgba($cyan, 0.12); padding: 0 5px; border-radius: 8px;
      }
      &.property {
        .node-ico { color: rgba($cyan, 0.9); }
        .node-label { color: rgba($t1, 0.95); font-weight: 500; }
        &:hover .node-label { color: $cyan; }
      }
      &.province { .node-label { font-weight: 600; color: rgba($t1, 0.95); } }
      &.city { .node-label { color: rgba($t1, 0.8); } }
      &.district { .node-label { color: $t2; } }
    }
  }
}

// ====== 父设备列表面板 ======
.device-section {
  @extend %hud-card;
  flex: 1; min-width: 0; display: flex; flex-direction: column;
  overflow: hidden;
}
.device-section-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; flex-shrink: 0;
  border-bottom: 1px solid rgba(79,195,247,0.12);
  position: relative;
  &::before {
    content: ''; position: absolute; left: 0; top: 20%; bottom: 20%;
    width: 3px; background: rgba(79,195,247,0.9);
    border-radius: 0 2px 2px 0; box-shadow: 0 0 6px rgba(79,195,247,0.5);
  }
  .section-title {
    display: flex; align-items: center; gap: 8px;
    font-size: 15px; font-weight: bold; color: #fff; letter-spacing: 0.5px;
    .el-icon { color: rgba(79,195,247,0.9); font-size: 16px; }
  }
  .result-info { font-size: 12px; color: $t2; b { color: $cyan; font-weight: 600; } }
}

// ====== 设备卡片网格 ======
.device-card-grid {
  flex: 1; min-height: 0; overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  align-content: start;
  padding: 14px;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
  // 设备卡片深色化（与主体深色风格一致）
  :deep(.device-card) {
    background: linear-gradient(170deg, rgba(18,37,67,0.97) 0%, rgba(18,35,61,0.97) 35%, rgba(21,44,79,0.97) 100%);
    border: 1px solid rgba(79,195,247,0.18) !important;
    box-shadow: 0 2px 10px rgba(0,0,0,0.25), inset 0 1px 0 rgba(79,195,247,0.05) !important;
    &:hover, &.selected {
      border-color: rgba(79,195,247,0.4) !important;
      box-shadow: 0 5px 18px rgba(0,0,0,0.35), 0 0 14px rgba(79,195,247,0.12) !important;
    }
    .dc-title { color: #fff !important; }
    .dc-content { color: #c7d6f0 !important; }
    .dc-card-smoke { color: #c7d6f0 !important; }
    .dc-btn-primary {
      background: rgba(10,22,40,0.6) !important;
      border: 1px solid rgba(79,195,247,0.3) !important;
      color: #7ec8f8 !important;
      &:hover {
        background: rgba(24,144,255,0.15) !important;
        border-color: rgba(79,195,247,0.5) !important;
      }
    }
    .dc-btn-ghost {
      background: rgba(10,22,40,0.6) !important;
      border: 1px solid rgba(79,195,247,0.2) !important;
      color: #8b9dc5 !important;
      &:hover {
        background: rgba(24,144,255,0.12) !important;
        border-color: rgba(79,195,247,0.35) !important;
        color: #fff !important;
      }
      &.active {
        background: rgba(24,144,255,0.25) !important;
        border-color: rgba(24,144,255,0.4) !important;
        color: #fff !important;
      }
    }
    .dc-meta-label { color: #5a7099 !important; }
    .dc-meta-val { color: #c7d6f0 !important; }
    .dc-loc-icon { color: #5a7099 !important; }
    .dc-history-btn {
      color: #5a7099 !important;
      &:hover { color: #7ec8f8 !important; }
    }
    .dc-indicator-dots span { background: rgba(79,195,247,0.2) !important; }
    .dc-bar { background: #7ec8f8 !important; }
    .dc-device-img img {
      filter: invert(1) hue-rotate(180deg);
    }
  }
}

// ====== 空状态 ======
.empty-state {
  grid-column: 1 / -1;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 60px 20px; gap: 12px; color: $t2;
  .el-icon { opacity: 0.4; }
  span { font-size: 14px; }
}
</style>

