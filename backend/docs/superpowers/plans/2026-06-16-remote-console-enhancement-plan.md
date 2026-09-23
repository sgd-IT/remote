# 远程控制台增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在监控大屏增加远程控制入口，远程控制台左侧视频列表改为展示设备绑定的摄像头，并按功能类型正确分组展示通用/总线/多线控制按钮。

**Architecture:** 复用现有后端视频与设备功能接口；前端调整 `VideoPanel` 接收摄像头列表、`QuickControl` 按 `GENERAL` 类型渲染、`DeviceCardGrid` 按 `BUS`/`MULTI` 过滤；在大屏顶部新增跳转入口。

**Tech Stack:** Vue 3, TypeScript, Element Plus, SCSS

---

## 文件结构总览

| 文件 | 职责 |
|------|------|
| `src/api/eiot/remote/remoteConsole.api.ts` | 新增摄像头 VO 与 API 封装 |
| `src/views/eiot/firewatch/large-screen/index.vue` | 新增远程控制入口按钮 |
| `src/views/eiot/firewatch/remote-console/index.vue` | 加载摄像头、功能列表，管理选中状态 |
| `src/views/eiot/firewatch/remote-console/components/VideoPanel.vue` | 接收摄像头列表，主视频 + 摄像头列表 |
| `src/views/eiot/firewatch/remote-console/components/QuickControl.vue` | 按 GENERAL 类型渲染功能按钮 |
| `src/views/eiot/firewatch/remote-console/components/DeviceCardGrid.vue` | 按 BUS/MULTI 类型过滤功能卡片 |

---

## Task 1: 扩展远程控制台 API 类型

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/api/eiot/remote/remoteConsole.api.ts`

- [ ] **Step 1: 新增 CameraVO 与 API**

在 `remoteConsole.api.ts` 现有类型之后追加：

```typescript
/** 摄像头信息 */
export interface CameraVO {
  id: number
  channelId?: string
  channelName?: string
  status?: number
  address?: string
}

/** 获取设备绑定的摄像头列表 */
export const getDeviceCameras = async (deviceId: number): Promise<CameraVO[]> => {
  return await request.get({ url: '/eiot/video/relation/list', params: { deviceId } })
}

/** 获取摄像头实时流地址 */
export const getCameraLiveUrl = async (cameraId: number): Promise<{ url: string }> => {
  return await request.get({ url: '/eiot/video/camera/live/url', params: { cameraId } })
}
```

- [ ] **Step 2: Commit**

```bash
cd anwei-fire-remote-monitor-vue
git add src/api/eiot/remote/remoteConsole.api.ts
git commit -m "feat(remote): add camera API types"
```

---

## Task 2: 大屏顶部新增远程控制入口

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/large-screen/index.vue`

- [ ] **Step 1: 引入 SwitchButton 图标**

在 `<script setup>` 的 Element Plus 图标导入处增加：

```typescript
import { SwitchButton } from '@element-plus/icons-vue'
```

（若已有集中导入，则在对应解构列表中加入 `SwitchButton`。）

- [ ] **Step 2: 新增跳转方法**

在 `<script setup>` 中添加：

```typescript
const goToRemoteConsole = () => {
  router.push('/eiot/firewatch/remote-console')
}
```

- [ ] **Step 3: 在标题栏右侧新增按钮**

在 `header-right` div 中、现有“进入管理后台”按钮之前插入：

```vue
<el-button type="warning" class="remote-btn" @click="goToRemoteConsole">
  <el-icon><SwitchButton /></el-icon>
  远程控制
</el-button>
```

- [ ] **Step 4: Commit**

```bash
cd anwei-fire-remote-monitor-vue
git add src/views/eiot/firewatch/large-screen/index.vue
git commit -m "feat(remote): add remote console entry on large screen"
```

---

## Task 3: 改造 VideoPanel 接收摄像头列表

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/VideoPanel.vue`

- [ ] **Step 1: 修改 Props 与内部状态**

将 `<script>` 中关于 `treeData`/`flatDevices`/`selectedId` 的逻辑替换为摄像头列表：

```typescript
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { getCameraLiveUrl, type CameraVO } from '@/api/eiot/remote/remoteConsole.api'

const props = defineProps<{
  cameras: CameraVO[]
}>()

const emit = defineEmits<{
  (e: 'select', camera: CameraVO): void
}>()

const selectedCameraId = ref<number>(0)
const currentTime = ref('')
let timer: ReturnType<typeof setInterval> | null = null

const currentCamera = computed(() => {
  return props.cameras.find(c => c.id === selectedCameraId.value) || null
})

const liveUrl = ref('')
const loadingUrl = ref(false)

const selectCamera = (camera: CameraVO) => {
  selectedCameraId.value = camera.id
  emit('select', camera)
  loadLiveUrl(camera.id)
}

const loadLiveUrl = async (cameraId: number) => {
  if (!cameraId) {
    liveUrl.value = ''
    return
  }
  loadingUrl.value = true
  try {
    const res = await getCameraLiveUrl(cameraId)
    liveUrl.value = res?.url || ''
  } catch (e) {
    liveUrl.value = ''
  } finally {
    loadingUrl.value = false
  }
}

watch(() => props.cameras, (list) => {
  if (list.length > 0) {
    if (!selectedCameraId.value || !list.find(c => c.id === selectedCameraId.value)) {
      selectCamera(list[0])
    }
  } else {
    selectedCameraId.value = 0
    liveUrl.value = ''
  }
}, { immediate: true, deep: true })

const updateTime = () => {
  const now = new Date()
  currentTime.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
```

- [ ] **Step 2: 修改模板**

将模板中依赖 `treeData` 的部分替换为摄像头展示：

```vue
<template>
  <div class="video-panel">
    <div class="video-monitor">
      <div class="video-display">
        <div v-if="liveUrl && !loadingUrl" class="video-player">
          <video
            :src="liveUrl"
            controls
            autoplay
            muted
            style="width: 100%; height: 100%; object-fit: cover;"
          />
        </div>
        <div v-else class="video-placeholder">
          <div class="video-rec-indicator"><div class="rec-dot"></div><span>REC</span></div>
          <div class="video-camera-icon">📹</div>
          <div class="video-camera-name">{{ currentCamera?.channelName || '请选择摄像头' }}</div>
          <div class="video-timestamp">{{ currentTime }}</div>
        </div>
      </div>
      <div class="video-controls">
        <div class="video-control-btn" title="播放/暂停">▶</div>
        <div class="video-control-btn" title="截图">📷</div>
        <div class="video-control-btn" title="全屏">⛶</div>
        <div class="video-control-btn" title="静音">🔊</div>
      </div>
    </div>

    <div class="video-list-container">
      <div class="video-list-header"><span>📹</span><span>设备视频列表</span></div>
      <div class="video-list">
        <div
          v-for="camera in cameras"
          :key="camera.id"
          class="video-list-item"
          :class="{ active: selectedCameraId === camera.id, offline: camera.status === 0 }"
          @click="selectCamera(camera)"
        >
          <div class="video-thumbnail" :class="{ offline: camera.status === 0 }">📷</div>
          <div class="video-info">
            <div class="video-name">{{ camera.channelName || '未命名摄像头' }}</div>
            <div class="video-status" :class="camera.status === 1 ? 'status-online' : 'status-offline'">
              <span class="status-dot" :class="camera.status === 1 ? 'online' : 'offline'"></span>
              <span>{{ camera.status === 1 ? '在线' : '离线' }}</span>
            </div>
          </div>
        </div>
        <div v-if="cameras.length === 0" class="empty-state">
          <div class="empty-icon">📦</div>
          <div class="empty-text">暂无关联摄像头</div>
        </div>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 3: 调整样式**

为视频播放器占位和空状态补充样式：

```scss
.video-player {
  width: 100%;
  height: 100%;
  background: #000;
}

.video-list .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}

.video-list .empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
  opacity: 0.5;
}

.video-list .empty-text {
  color: #8ba3c7;
  font-size: 13px;
}
```

- [ ] **Step 4: Commit**

```bash
cd anwei-fire-remote-monitor-vue
git add src/views/eiot/firewatch/remote-console/components/VideoPanel.vue
git commit -m "feat(remote): VideoPanel uses bound camera list"
```

---

## Task 4: 改造 QuickControl 按 GENERAL 类型渲染

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/QuickControl.vue`

- [ ] **Step 1: 重写组件**

替换整个 `<template>` 与 `<script>` 为：

```vue
<template>
  <div class="button-grid-container">
    <div v-if="generalFunctions.length > 0" class="button-grid">
      <button
        v-for="fn in generalFunctions"
        :key="fn.id"
        class="grid-btn"
        :class="getBtnClass(fn)"
        @click="handleClick(fn)"
      >
        <span class="btn-icon">⚡</span>
        <span class="btn-text">{{ fn.buttonName || fn.functionName }}</span>
      </button>
    </div>
    <div v-else class="empty-state">
      <div class="empty-icon">📦</div>
      <div class="empty-text">暂无通用功能</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { DeviceFunctionVO } from '@/api/eiot/remote/deviceRemote.api'

const props = defineProps<{
  functions: DeviceFunctionVO[]
}>()

const emit = defineEmits<{
  (e: 'action', fn: DeviceFunctionVO): void
}>()

const generalFunctions = computed(() => {
  return props.functions.filter(f => !f.functionType || f.functionType === 'GENERAL')
})

const getBtnClass = (fn: DeviceFunctionVO) => {
  const name = (fn.buttonName || fn.functionName || '').toLowerCase()
  if (name.includes('消音') || name.includes('静音')) return 'grid-btn-mute'
  if (name.includes('复位') || name.includes('重置')) return 'grid-btn-reset'
  if (name.includes('启动') || name.includes('开始') || name.includes('开')) return 'grid-btn-start'
  if (name.includes('停止') || name.includes('关闭') || name.includes('关')) return 'grid-btn-stop'
  return 'grid-btn-default'
}

const handleClick = (fn: DeviceFunctionVO) => {
  emit('action', fn)
}
</script>
```

- [ ] **Step 2: 补充默认按钮样式**

在 `<style>` 末尾增加：

```scss
.grid-btn-default {
  background: linear-gradient(135deg, #2c3e50 0%, #1a252f 100%);
  border: 1px solid rgba(0, 180, 255, 0.3);
  box-shadow: 0 0 8px rgba(0, 180, 255, 0.1);
}

.grid-btn-default:hover {
  background: linear-gradient(135deg, #34495e 0%, #22303d 100%);
  border-color: rgba(0, 180, 255, 0.5);
  box-shadow: 0 0 15px rgba(0, 180, 255, 0.25);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
  opacity: 0.5;
}

.empty-text {
  color: #8ba3c7;
  font-size: 13px;
}
```

- [ ] **Step 3: Commit**

```bash
cd anwei-fire-remote-monitor-vue
git add src/views/eiot/firewatch/remote-console/components/QuickControl.vue
git commit -m "feat(remote): QuickControl renders GENERAL functions"
```

---

## Task 5: 调整 DeviceCardGrid 按 BUS/MULTI 过滤

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/DeviceCardGrid.vue`

- [ ] **Step 1: 修改过滤逻辑**

将 `filteredFunctions` computed 改为：

```typescript
const filteredFunctions = computed(() => {
  const type = activeTab.value === 'bus' ? 'BUS' : 'MULTI'
  return props.functions.filter(f => f.functionType === type)
})
```

（其余模板与样式不变，保持卡片“启动/停止”按钮。）

- [ ] **Step 2: Commit**

```bash
cd anwei-fire-remote-monitor-vue
git add src/views/eiot/firewatch/remote-console/components/DeviceCardGrid.vue
git commit -m "feat(remote): DeviceCardGrid filters by BUS/MULTI type"
```

---

## Task 6: 重构远程控制台主页面

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/index.vue`

- [ ] **Step 1: 引入摄像头 API**

在 `<script setup>` 导入中增加：

```typescript
import { getDeviceCameras, getCameraLiveUrl, type CameraVO } from '@/api/eiot/remote/remoteConsole.api'
```

- [ ] **Step 2: 新增摄像头状态与方法**

在 `index.vue` 的 script 中：

```typescript
const cameras = ref<CameraVO[]>([])

const loadCameras = async (deviceId: number) => {
  if (!deviceId) {
    cameras.value = []
    return
  }
  try {
    const res = await getDeviceCameras(deviceId)
    cameras.value = res || []
  } catch (e) {
    cameras.value = []
  }
}

const handleSelectDevice = (device: DeviceTreeItemVO) => {
  selectedDeviceId.value = device.id
  selectedDeviceInfo.value = device
  loadCameras(device.id)
  loadDeviceFunctions(device.id)
}
```

- [ ] **Step 3: 调整模板绑定**

将模板中：

```vue
<VideoPanel v-model="selectedDeviceId" :tree-data="treeData" @select="handleSelectDevice" />
```

改为：

```vue
<VideoPanel :cameras="cameras" />
```

将：

```vue
<QuickControl :functions="deviceFunctions" @action="handleQuickAction" />
```

改为：

```vue
<QuickControl :functions="deviceFunctions" @action="handleGeneralAction" />
```

- [ ] **Step 4: 调整事件处理**

将 `handleQuickAction` 和 `handleCardAction` 统一为：

```typescript
const handleGeneralAction = (fn: DeviceFunctionVO) => {
  const actionName = fn.buttonName || fn.functionName || '执行'
  openVerify(actionName, fn, '1')
}

const handleCardAction = (fn: DeviceFunctionVO, actionName: string) => {
  openVerify(actionName, fn, actionName === '启动' ? '1' : '0')
}
```

删除旧的 `handleQuickAction` 方法。

- [ ] **Step 5: Commit**

```bash
cd anwei-fire-remote-monitor-vue
git add src/views/eiot/firewatch/remote-console/index.vue
git commit -m "feat(remote): load cameras and wire function actions in remote console"
```

---

## Task 7: 前端编译验证

**Files:**
- All modified frontend files

- [ ] **Step 1: TypeScript 检查**

```bash
cd anwei-fire-remote-monitor-vue
npx vue-tsc --noEmit --pretty 2>&1 | Select-String "remote-console|large-screen|remoteConsole\.api"
```

Expected: 无新增错误。

- [ ] **Step 2: Vite 构建**

```bash
cd anwei-fire-remote-monitor-vue
npx vite build 2>&1 | Select-Object -Last 30
```

Expected: Build successful.

- [ ] **Step 3: Commit 修复**

```bash
cd anwei-fire-remote-monitor-vue
git add -A
git commit -m "fix(remote): fix frontend compile errors"
```

---

## Task 8: 联调测试

**Files:**
- Backend and frontend

- [ ] **Step 1: 确认后端服务已启动**

确保后端在 `http://localhost:48080` 运行。

- [ ] **Step 2: 启动前端 dev server**

```bash
cd anwei-fire-remote-monitor-vue
npx vite --port 5173
```

- [ ] **Step 3: 访问并验证**

1. 访问监控大屏 `/#/eiot/firewatch/large-screen`，确认顶部有“远程控制”按钮，点击可跳转。
2. 访问 `/#/eiot/firewatch/remote-console`，登录后确认左侧视频列表展示设备绑定摄像头。
3. 选中设备后，右侧 QuickControl 展示 `GENERAL` 类型功能按钮。
4. 底部总线/多线 Tab 分别展示 `BUS`/`MULTI` 类型功能。
5. 点击任意控制按钮弹出双人复核弹窗。

- [ ] **Step 4: Commit**

```bash
cd anwei-fire-remote-monitor-vue
git add -A
git commit -m "feat(remote): complete remote console enhancement integration"
```

---

## 自查清单

- [ ] `remoteConsole.api.ts` 已新增 `CameraVO`、`getDeviceCameras`、`getCameraLiveUrl`
- [ ] 监控大屏顶部已新增“远程控制”入口
- [ ] `VideoPanel` 已改为接收 `cameras` 列表并展示主视频 + 摄像头列表
- [ ] `QuickControl` 已按 `GENERAL` 类型渲染功能按钮
- [ ] `DeviceCardGrid` 已按 `BUS`/`MULTI` 类型过滤
- [ ] 所有控制操作仍走双人复核流程
- [ ] 前端编译通过
