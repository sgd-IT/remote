# 远程控制台前端重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按照新原型完全替换远程控制台页面，并扩展设备功能模型支持子设备绑定与功能类型（总线/多线/通用）。

**Architecture:** 后端扩展 `eiot_device_function` 表及对应 DO/VO/Controller；前端将远程控制台拆分为 VideoPanel、EventTable、QuickControl、DeviceCardGrid、Modals 等组件，按功能类型渲染不同区域。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Vue 3, TypeScript, Element Plus, SCSS

---

## 文件结构总览

### 后端新增/修改

| 文件 | 职责 |
|------|------|
| `_sql/mysql/remote/device_function_add_columns.sql` | 设备功能表加字段 |
| `dal/dataobject/device/DeviceFunctionDO.java` | 新增 subDeviceId、functionType |
| `controller/admin/device/vo/DeviceFunctionRespVO.java` | 新增响应字段 |
| `controller/admin/device/vo/DeviceFunctionCreateReqVO.java` | 新增请求字段 |
| `controller/admin/device/vo/DeviceFunctionUpdateReqVO.java` | 新增请求字段 |
| `service/device/DeviceFunctionService.java` / `Impl` | 透传新字段 |
| `controller/admin/device/DeviceFunctionController.java` | 透传新字段 |

### 前端新增/修改

| 文件 | 职责 |
|------|------|
| `src/api/eiot/remote/deviceRemote.api.ts` | 设备功能、告警 API 类型扩展 |
| `src/api/eiot/remote/remoteConsole.api.ts` | 复用已有接口，按需扩展类型 |
| `src/views/eiot/firewatch/remote-console/index.vue` | 页面容器 |
| `src/views/eiot/firewatch/remote-console/components/VideoPanel.vue` | 左侧视频区 |
| `src/views/eiot/firewatch/remote-console/components/FunctionBar.vue` | 顶部功能栏 |
| `src/views/eiot/firewatch/remote-console/components/EventTable.vue` | 右上告警事件表 |
| `src/views/eiot/firewatch/remote-console/components/QuickControl.vue` | 2×2 快捷按钮 |
| `src/views/eiot/firewatch/remote-console/components/DeviceCardGrid.vue` | 底部 Tab + 设备卡片 |
| `src/views/eiot/firewatch/remote-console/components/modals/*.vue` | 各类弹窗 |
| `src/views/eiot/deviceinfo/detail.vue`（或远程控制子组件） | 新增子设备、功能类型字段 |

---

## Task 1: 设备功能表加字段

**Files:**
- Create: `anwei-fire-remote-monitor/_sql/mysql/remote/device_function_add_columns.sql`

- [ ] **Step 1: 编写加字段 SQL**

```sql
ALTER TABLE `eiot_device_function`
    ADD COLUMN `sub_device_id` BIGINT DEFAULT NULL COMMENT '绑定的子设备ID',
    ADD COLUMN `function_type` VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '功能类型：BUS(总线按钮)/MULTI(多线按钮)/GENERAL(通用按钮)';
```

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor/_sql/mysql/remote/device_function_add_columns.sql
git commit -m "feat(remote): add sub_device_id and function_type to device_function"
```

---

## Task 2: 更新 DeviceFunctionDO

**Files:**
- Modify: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/device/DeviceFunctionDO.java`

- [ ] **Step 1: 新增字段**

在 `DeviceFunctionDO` 类中 `paramValues` 字段后添加：

```java
/**
 * 绑定的子设备ID
 */
private Long subDeviceId;

/**
 * 功能类型：BUS/MULTI/GENERAL
 */
private String functionType;
```

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/device/DeviceFunctionDO.java
git commit -m "feat(remote): add subDeviceId and functionType to DeviceFunctionDO"
```

---

## Task 3: 更新设备功能 VO

**Files:**
- Modify: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/DeviceFunctionRespVO.java`
- Modify: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/DeviceFunctionCreateReqVO.java`
- Modify: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/DeviceFunctionUpdateReqVO.java`

- [ ] **Step 1: 修改响应 VO**

在 `DeviceFunctionRespVO` 中新增：

```java
@Schema(description = "绑定的子设备ID")
private Long subDeviceId;

@Schema(description = "功能类型：BUS(总线按钮)/MULTI(多线按钮)/GENERAL(通用按钮)")
private String functionType;
```

- [ ] **Step 2: 修改创建请求 VO**

在 `DeviceFunctionCreateReqVO` 中新增：

```java
@Schema(description = "绑定的子设备ID")
private Long subDeviceId;

@Schema(description = "功能类型：BUS/MULTI/GENERAL", requiredMode = Schema.RequiredMode.REQUIRED)
@NotBlank(message = "功能类型不能为空")
private String functionType;
```

- [ ] **Step 3: 修改更新请求 VO**

在 `DeviceFunctionUpdateReqVO` 中新增：

```java
@Schema(description = "绑定的子设备ID")
private Long subDeviceId;

@Schema(description = "功能类型：BUS/MULTI/GENERAL", requiredMode = Schema.RequiredMode.REQUIRED)
@NotBlank(message = "功能类型不能为空")
private String functionType;
```

- [ ] **Step 4: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/
git commit -m "feat(remote): add subDeviceId and functionType to DeviceFunction VOs"
```

---

## Task 4: 更新 DeviceFunctionService

**Files:**
- Modify: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceFunctionServiceImpl.java`

- [ ] **Step 1: 透传新字段**

在创建/更新逻辑中确保 `subDeviceId` 和 `functionType` 被正确设置到 DO。例如：

```java
// 创建时
DeviceFunctionDO deviceFunction = DeviceFunctionConvert.INSTANCE.convert(createReqVO);
deviceFunction.setFunctionType(createReqVO.getFunctionType());
deviceFunction.setSubDeviceId(createReqVO.getSubDeviceId());
deviceFunctionMapper.insert(deviceFunction);

// 更新时
DeviceFunctionDO updateObj = DeviceFunctionConvert.INSTANCE.convert(updateReqVO);
updateObj.setFunctionType(updateReqVO.getFunctionType());
updateObj.setSubDeviceId(updateReqVO.getSubDeviceId());
deviceFunctionMapper.updateById(updateObj);
```

（如 `DeviceFunctionConvert` 已自动映射同名字段，可跳过手动设置，但需确认 MapStruct 版本能映射 `subDeviceId`/`functionType`。）

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceFunctionServiceImpl.java
git commit -m "feat(remote): pass subDeviceId and functionType in DeviceFunctionService"
```

---

## Task 5: 后端编译验证

**Files:**
- All modified Java files

- [ ] **Step 1: 编译 module-eiot-biz**

Run:
```bash
cd anwei-fire-remote-monitor
mvn clean compile -pl module-eiot/module-eiot-biz -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Commit 修复（如有）**

```bash
git add -A
git commit -m "fix(remote): compile fixes for device function fields"
```

---

## Task 6: 扩展前端 API 类型

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/api/eiot/remote/deviceRemote.api.ts`
- Modify: `anwei-fire-remote-monitor-vue/src/api/eiot/remote/remoteConsole.api.ts`

- [ ] **Step 1: 扩展 DeviceFunctionVO**

在 `deviceRemote.api.ts` 中 `DeviceFunctionVO` 接口新增：

```typescript
export interface DeviceFunctionVO {
  id: number
  deviceId: number
  brandModelFunctionId: number
  buttonName?: string
  functionName?: string
  paramValues?: string
  enabled?: boolean
  sortOrder?: number
  subDeviceId?: number
  functionType?: 'BUS' | 'MULTI' | 'GENERAL'
}
```

- [ ] **Step 2: 扩展告警记录类型并新增告警查询 API**

在 `deviceRemote.api.ts` 中新增：

```typescript
export interface AlertRecordVO {
  id: number
  alertTime: number
  deviceId: number
  deviceName?: string
  content: string
  level?: 'HIGH' | 'MEDIUM' | 'LOW'
  status?: number
  addr?: string
}

export const getAlertRecordPage = async (params: any) => {
  return await request.post({ url: '/alert/selectAlertRecordPage', data: params })
}
```

- [ ] **Step 3: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/api/eiot/remote/deviceRemote.api.ts
git commit -m "feat(remote): extend DeviceFunctionVO and add alert API"
```

---

## Task 7: 创建设备详情远程控制表单字段

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/detail.vue`（或其中远程控制子组件，根据实际文件结构调整）

- [ ] **Step 1: 定位远程控制功能表单**

找到设备详情页中新增/编辑设备功能的表单区域。

- [ ] **Step 2: 新增子设备选择器**

在表单中增加：

```vue
<el-form-item label="子设备">
  <el-select v-model="form.subDeviceId" clearable placeholder="不绑定">
    <el-option
      v-for="item in childDevices"
      :key="item.id"
      :label="item.name"
      :value="item.id"
    />
  </el-select>
</el-form-item>
```

并在打开表单时调用：

```typescript
const childDevices = ref<any[]>([])
const fetchChildDevices = async (deviceId: number) => {
  const res = await request.post({
    url: '/eiot/device/children/list',
    data: { deviceId, pageSize: 1000 }
  })
  childDevices.value = res?.list || []
}
```

- [ ] **Step 3: 新增功能类型选择器**

```vue
<el-form-item label="功能类型">
  <el-select v-model="form.functionType" placeholder="请选择">
    <el-option label="通用按钮" value="GENERAL" />
    <el-option label="总线按钮" value="BUS" />
    <el-option label="多线按钮" value="MULTI" />
  </el-select>
</el-form-item>
```

- [ ] **Step 4: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/detail.vue
git commit -m "feat(remote): add subDevice and functionType fields in device function form"
```

---

## Task 8: 创建远程控制台基础组件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/modals/VerifyModal.vue`
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/modals/LogModal.vue`
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/modals/AlarmModal.vue`
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/modals/CallModal.vue`
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/modals/FilterModal.vue`

- [ ] **Step 1: 创建 VerifyModal.vue**

实现双人复核弹窗：输入复核人账号、密码，确认后 emit `confirm`。

```vue
<template>
  <el-dialog v-model="visible" title="操作确认" width="400px" :close-on-click-modal="false">
    <div class="verify-body">
      <div class="verify-tip">{{ actionName }}</div>
      <el-form :model="form" label-width="80px">
        <el-form-item label="复核账号">
          <el-input v-model="form.verifierAccount" placeholder="请输入复核账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确认执行</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  actionName: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'confirm', data: { verifierAccount: string; password: string }): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const form = ref({ verifierAccount: '', password: '' })

watch(() => props.modelValue, (val) => {
  if (val) form.value = { verifierAccount: '', password: '' }
})

const handleConfirm = () => {
  if (!form.value.verifierAccount || !form.value.password) return
  emit('confirm', { ...form.value })
  visible.value = false
}
</script>
```

- [ ] **Step 2: 创建 LogModal.vue**

使用 `getOperationLogs` 展示当前设备操作日志。

- [ ] **Step 3: 创建 AlarmModal.vue**

使用 `getAlertRecordPage` 展示未处理告警，支持确认处理（静态确认动作）。

- [ ] **Step 4: 创建 CallModal.vue / FilterModal.vue**

按原型静态数据实现。

- [ ] **Step 5: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/modals/
git commit -m "feat(remote): add remote console modal components"
```

---

## Task 9: 创建 VideoPanel 组件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/VideoPanel.vue`

- [ ] **Step 1: 实现左侧视频区**

```vue
<template>
  <div class="video-panel">
    <div class="video-monitor">
      <div class="video-display">
        <div class="video-placeholder">
          <div class="video-rec-indicator"><div class="rec-dot"></div><span>REC</span></div>
          <div class="video-camera-icon">📹</div>
          <div class="video-camera-name">{{ currentCamera?.name || '请选择设备' }}</div>
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
          v-for="device in flatDevices"
          :key="device.id"
          class="video-list-item"
          :class="{ active: selectedId === device.id, offline: device.state === 0 }"
          @click="selectDevice(device)"
        >
          <div class="video-thumbnail" :class="{ offline: device.state === 0 }">{{ getIcon(device) }}</div>
          <div class="video-info">
            <div class="video-name">{{ device.name }}</div>
            <div class="video-status" :class="device.state === 1 ? 'status-online' : 'status-offline'">
              <span class="status-dot" :class="device.state === 1 ? 'online' : 'offline'"></span>
              <span>{{ device.state === 1 ? '在线' : '离线' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
```

Props: `treeData: DeviceTreeNodeVO[]`, `modelValue: number`（选中设备ID）。
通过 `treeData` 扁平化得到 `flatDevices`。

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/VideoPanel.vue
git commit -m "feat(remote): add VideoPanel component"
```

---

## Task 10: 创建 FunctionBar 组件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/FunctionBar.vue`

- [ ] **Step 1: 实现顶部功能栏**

四个按钮：记录筛选、操作日志、告警处理、通话录音。点击 emit 对应事件。

```vue
<template>
  <div class="function-bar">
    <button class="func-btn" @click="$emit('filter')"><span>🔍</span><span>记录筛选</span></button>
    <button class="func-btn" @click="$emit('log')"><span>📋</span><span>操作日志</span></button>
    <button class="func-btn" @click="$emit('alarm')"><span>🚨</span><span>告警处理</span></button>
    <button class="func-btn" @click="$emit('call')"><span>📞</span><span>通话录音</span></button>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/FunctionBar.vue
git commit -m "feat(remote): add FunctionBar component"
```

---

## Task 11: 创建 EventTable 组件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/EventTable.vue`

- [ ] **Step 1: 实现告警事件表格**

列：时间、设备、内容、位置信息、设备编号、操作。

```vue
<template>
  <div class="table-container">
    <div class="table-header">
      <div class="table-header-cell">时间</div>
      <div class="table-header-cell">设备</div>
      <div class="table-header-cell">内容</div>
      <div class="table-header-cell">位置信息</div>
      <div class="table-header-cell">设备编号</div>
      <div class="table-header-cell">操作</div>
    </div>
    <div class="table-body">
      <div v-for="item in list" :key="item.id" class="table-row">
        <div class="table-cell">{{ formatTime(item.alertTime) }}</div>
        <div class="table-cell">{{ item.deviceName }}</div>
        <div class="table-cell">{{ item.content }}</div>
        <div class="table-cell">{{ item.addr || '-' }}</div>
        <div class="table-cell">{{ item.deviceId }}</div>
        <div class="table-cell"><button class="table-action">处理</button></div>
      </div>
      <div v-if="!list.length" class="empty-state"><div class="empty-icon">📦</div><div class="empty-text">暂无数据</div></div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/EventTable.vue
git commit -m "feat(remote): add EventTable component"
```

---

## Task 12: 创建 QuickControl 组件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/QuickControl.vue`

- [ ] **Step 1: 实现 2×2 快捷按钮**

根据 `deviceFunctions` 中 `functionType === 'GENERAL'` 且名称匹配“复位/消音/启动/停止”的功能渲染按钮。

```vue
<template>
  <div class="button-grid">
    <button class="grid-btn grid-btn-reset" @click="handleClick('复位')">
      <span class="btn-icon">↻</span><span class="btn-text">复位</span>
    </button>
    <button class="grid-btn grid-btn-mute" @click="handleClick('消音')">
      <span class="btn-icon">🔇</span><span class="btn-text">消音</span>
    </button>
    <button class="grid-btn grid-btn-start" @click="handleClick('启动')">
      <span class="btn-icon">▶</span><span class="btn-text">启动</span>
    </button>
    <button class="grid-btn grid-btn-stop" @click="handleClick('停止')">
      <span class="btn-icon">■</span><span class="btn-text">停止</span>
    </button>
  </div>
</template>
```

点击时 emit `action` 事件，携带匹配到的功能对象和动作名。

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/QuickControl.vue
git commit -m "feat(remote): add QuickControl component"
```

---

## Task 13: 创建 DeviceCardGrid 组件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/DeviceCardGrid.vue`

- [ ] **Step 1: 实现 Tab + 设备卡片网格**

Tab：总线、多线。几号机选择器静态。

```vue
<template>
  <div class="control-section">
    <div class="tab-bar">
      <div class="tab-item" :class="{ active: activeTab === 'bus' }" @click="activeTab = 'bus'">总线</div>
      <div class="tab-item" :class="{ active: activeTab === 'multi' }" @click="activeTab = 'multi'">多线</div>
      <div class="tab-tools">
        <div class="machine-select">
          <span>几号机:</span>
          <el-select v-model="machineNo" size="small">
            <el-option label="1号机" value="1" />
            <el-option label="2号机" value="2" />
            <el-option label="3号机" value="3" />
          </el-select>
        </div>
      </div>
    </div>
    <div class="device-card-grid">
      <div v-for="fn in filteredFunctions" :key="fn.id" class="device-card">
        <div class="device-card-header">
          <span class="device-card-title">{{ fn.buttonName || fn.functionName }}</span>
          <span class="device-card-icon">⚙️</span>
        </div>
        <div class="device-status-tag" :class="fn.enabled ? 'status-running' : 'status-stopped'">
          <span class="status-dot"></span>
          <span>{{ fn.enabled ? '运行中' : '停止' }}</span>
        </div>
        <div class="device-card-actions">
          <button class="card-action-btn card-btn-start" @click="emitAction(fn, '启动')">启动</button>
          <button class="card-action-btn card-btn-stop" @click="emitAction(fn, '停止')">停止</button>
        </div>
      </div>
    </div>
  </div>
</template>
```

`filteredFunctions` 根据 `activeTab` 过滤 `functionType` 为 `BUS` 或 `MULTI` 的功能。

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/DeviceCardGrid.vue
git commit -m "feat(remote): add DeviceCardGrid component"
```

---

## Task 14: 重构远程控制台主页面

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/index.vue`
- Delete: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/DeviceTree.vue`
- Delete: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/ControlPanel.vue`
- Delete: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/VerifyDialog.vue`（如 VerifyModal 已替代）

- [ ] **Step 1: 重写 index.vue**

```vue
<template>
  <div class="remote-console">
    <header class="header">
      <div class="header-title">广东安为</div>
      <div class="header-right" @click="verifyVisible = true">
        <span>确认密码</span>
        <span>🔐</span>
      </div>
    </header>
    <div class="main-container">
      <VideoPanel v-model="selectedDeviceId" :tree-data="treeData" @select="handleSelectDevice" />
      <div class="right-panel">
        <FunctionBar @filter="filterVisible = true" @log="logVisible = true" @alarm="alarmVisible = true" @call="callVisible = true" />
        <div class="table-section">
          <EventTable :device-id="selectedDeviceId" />
          <QuickControl :functions="deviceFunctions" @action="handleQuickAction" />
        </div>
        <DeviceCardGrid :functions="deviceFunctions" @action="handleCardAction" />
      </div>
    </div>

    <VerifyModal v-model="verifyVisible" :action-name="currentActionName" @confirm="handleVerifyConfirm" />
    <LogModal v-model="logVisible" :device-id="selectedDeviceId" />
    <AlarmModal v-model="alarmVisible" :device-id="selectedDeviceId" />
    <CallModal v-model="callVisible" />
    <FilterModal v-model="filterVisible" />
  </div>
</template>
```

- [ ] **Step 2: 删除旧组件**

删除 `DeviceTree.vue`、`ControlPanel.vue`、`VerifyDialog.vue`。

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(remote): rebuild remote console main page with new prototype"
```

---

## Task 15: 样式整合与全局样式文件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/styles/common.scss`

- [ ] **Step 1: 提取公共 SCSS 变量/工具类**

```scss
$bg-primary: #0A1628;
$bg-panel: #0D1E38;
$border-color: rgba(0, 180, 255, 0.15);
$primary: #00B4FF;
$success: #00E676;
$warning: #FF8C00;
$danger: #FF4D4F;
$text-primary: #E8F0FE;
$text-secondary: #8BA3C7;
```

各组件通过 `@use './styles/common.scss' as *;` 引入。

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/styles/common.scss
git commit -m "feat(remote): add common styles for remote console"
```

---

## Task 16: 前端编译验证

**Files:**
- All modified frontend files

- [ ] **Step 1: TypeScript 检查**

Run:
```bash
cd anwei-fire-remote-monitor-vue
npx vue-tsc --noEmit --pretty 2>&1 | Select-String "remote-console"
```

Expected: 无新增错误。

- [ ] **Step 2: Vite 构建**

Run:
```bash
cd anwei-fire-remote-monitor-vue
npx vite build 2>&1 | Select-Object -Last 30
```

Expected: Build successful.

- [ ] **Step 3: Commit 修复**

```bash
git add -A
git commit -m "fix(remote): fix frontend compile errors"
```

---

## Task 17: 联调测试

**Files:**
- Backend and frontend

- [ ] **Step 1: 确认后端服务已启动**

确保后端在 `http://localhost:48080` 运行，且设备功能表字段已更新。

- [ ] **Step 2: 访问新页面**

启动前端 dev server：
```bash
cd anwei-fire-remote-monitor-vue
npx vite --port 5173
```

访问 `http://localhost:5173/#/eiot/firewatch/remote-console`

- [ ] **Step 3: 验证关键路径**

- 左侧视频列表展示设备树数据
- 选中设备后右侧加载告警表格
- 操作日志/告警处理弹窗有数据
- 总线/多线 Tab 按功能类型分组
- 控制按钮触发双人复核弹窗

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(remote): complete remote console redesign integration"
```

---

## 自查清单

- [ ] `eiot_device_function` 表已新增 `sub_device_id` 和 `function_type`
- [ ] 后端 DO/VO/Service/Controller 已透传新字段
- [ ] 前端设备详情远程控制表单已新增子设备和功能类型
- [ ] 新远程控制台页面布局与原型一致
- [ ] 左侧视频列表使用设备树数据
- [ ] 告警表格和操作日志弹窗接入真实接口
- [ ] 总线/多线/通用按钮按功能类型正确分组
- [ ] 所有控制操作走双人复核流程
- [ ] 前端编译通过
