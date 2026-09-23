# 远程控制台增强设计

## 1. 目标

在现有远程控制台页面基础上完成三项增强：

1. 在监控大屏（`large-screen`）顶部增加“远程控制”入口，一键跳转远程控制台。
2. 远程控制台左侧视频列表改为展示当前选中设备绑定的摄像头，支持主视频播放与列表切换。
3. 修复/优化功能按钮分组逻辑：
   - `GENERAL` 类型功能作为通用按钮展示在右侧 2×2 快捷控制区（显示真实功能名称）。
   - `BUS` 类型功能展示在底部“总线”Tab 下。
   - `MULTI` 类型功能展示在底部“多线”Tab 下。

## 2. 范围

### 2.1 前端范围

- `src/views/eiot/firewatch/large-screen/index.vue`：新增远程控制入口按钮。
- `src/views/eiot/firewatch/remote-console/index.vue`：调整数据加载逻辑，加载摄像头与功能列表。
- `src/views/eiot/firewatch/remote-console/components/VideoPanel.vue`：改为接收摄像头列表，支持主视频与摄像头列表。
- `src/views/eiot/firewatch/remote-console/components/QuickControl.vue`：改为按 `GENERAL` 类型渲染功能按钮。
- `src/views/eiot/firewatch/remote-console/components/DeviceCardGrid.vue`：按 `BUS`/`MULTI` 类型过滤功能卡片。
- `src/api/eiot/remote/remoteConsole.api.ts`：新增摄像头相关 API 封装。

### 2.2 后端范围

- 复用现有接口，无需新增后端接口：
  - `GET /eiot/video/relation/list?deviceId={id}`：获取设备绑定的摄像头。
  - `GET /eiot/video/camera/live/url?cameraId={id}`：获取摄像头实时流地址。
  - `GET /eiot/device/{id}/function`：获取设备功能列表。
  - `POST /eiot/device/instruction/send`：下发控制指令。

## 3. 设计方案

### 3.1 监控大屏入口

在 `large-screen/index.vue` 顶部标题栏右侧、现有“进入管理后台”按钮旁边新增“远程控制”按钮：

```vue
<el-button type="warning" class="remote-btn" @click="goToRemoteConsole">
  <el-icon><SwitchButton /></el-icon>
  远程控制
</el-button>
```

点击后跳转：

```typescript
const goToRemoteConsole = () => {
  router.push('/eiot/firewatch/remote-console')
}
```

### 3.2 视频列表与摄像头绑定

#### 数据流

1. 进入远程控制台后，左侧仍保留设备树/企业列表用于选择设备（保持现有交互）。
2. 选中设备后，父组件 `index.vue` 同时调用：
   - `GET /eiot/video/relation/list?deviceId={id}` 获取摄像头列表。
   - `GET /eiot/device/{id}/function` 获取功能列表。
3. 将摄像头列表 `cameras` 传入 `VideoPanel`。

#### VideoPanel 调整

Props 变更：

```typescript
interface CameraVO {
  id: number
  channelId?: string
  channelName?: string
  status?: number
  address?: string
}

const props = defineProps<{
  cameras: CameraVO[]
}>()
```

行为：

- 默认选中第一个摄像头作为主视频。
- 主视频区调用 `GET /eiot/video/camera/live/url?cameraId={id}` 获取 flv 地址并播放。
- 若 ICC 未配置或获取地址失败，使用占位画面兜底。
- 下方摄像头列表展示 `channelName`，点击切换主视频。
- 无摄像头时显示占位画面和“暂无关联摄像头”提示。

### 3.3 功能按钮按类型分组

#### QuickControl（通用按钮区）

- 过滤 `functionType === 'GENERAL'` 或 `functionType` 为空的功能。
- 每个功能渲染为一个按钮，按钮文字为 `buttonName || functionName`。
- 按钮排列使用自适应网格（每行 2 列或更多，根据数量自动换行）。
- 点击按钮后触发 `action` 事件，携带功能对象，由父组件弹出双人复核并下发指令。
- 无 GENERAL 功能时显示空状态。

#### DeviceCardGrid（总线/多线卡片区）

- “总线”Tab 过滤 `functionType === 'BUS'`。
- “多线”Tab 过滤 `functionType === 'MULTI'`。
- 每个卡片展示功能名称、运行状态，并保留“启动/停止”两个按钮。
- 启动按钮下发 `commandType = '1'`，停止按钮下发 `commandType = '0'`。
- 某类型无功能时显示空状态。

### 3.4 数据类型扩展

在 `remoteConsole.api.ts` 新增：

```typescript
export interface CameraVO {
  id: number
  channelId?: string
  channelName?: string
  status?: number
  address?: string
}

export const getDeviceCameras = async (deviceId: number): Promise<CameraVO[]> => {
  return await request.get({ url: '/eiot/video/relation/list', params: { deviceId } })
}

export const getCameraLiveUrl = async (cameraId: number): Promise<{ url: string }> => {
  return await request.get({ url: '/eiot/video/camera/live/url', params: { cameraId } })
}
```

## 4. 兼容性

- 旧功能数据 `functionType` 为空时，数据库默认值为 `GENERAL`，后端返回空时前端按 `GENERAL` 处理。
- 设备未绑定摄像头时，左侧视频区显示占位，不影响其他功能。
- 主视频流地址获取失败时降级为占位画面。

## 5. 验收标准

- [ ] 监控大屏顶部出现“远程控制”按钮，点击后跳转到远程控制台。
- [ ] 远程控制台左侧视频列表展示当前设备绑定的摄像头，点击可切换主视频。
- [ ] 主视频区尝试播放摄像头实时流，失败时显示占位画面。
- [ ] 右侧通用按钮区展示所有 `GENERAL` 类型功能，按钮名称为真实功能名称。
- [ ] 底部“总线”Tab 展示所有 `BUS` 类型功能，“多线”Tab 展示所有 `MULTI` 类型功能。
- [ ] 所有控制操作仍走双人复核流程。
- [ ] 前端编译通过，无新增 TypeScript 错误。
