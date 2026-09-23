# 远程控制台前端重构设计

## 1. 目标

完全替换现有 `/eiot/firewatch/remote-console` 远程控制台页面，按照新原型实现视频监控 + 集中控制 + 设备卡片网格的交互布局；同时扩展设备功能模型，支持在设备详情远程控制中为功能绑定子设备并标记功能类型，远程控制台根据功能类型渲染对应按钮区域。

## 2. 范围

### 2.1 前端范围

- 完全替换 `src/views/eiot/firewatch/remote-console/` 下现有页面与组件。
- 新增/调整必要的 API 类型与请求封装。
- 调整设备详情页中“远程控制”功能的新增/编辑表单。

### 2.2 后端范围

- `eiot_device_function` 表新增 `sub_device_id`（可为空）和 `function_type` 字段。
- 对应 DO、VO、ReqVO、Service、Controller 调整。
- 保持向后兼容：已有功能默认归为 `通用按钮` 类型，`sub_device_id` 为空。

## 3. 后端变更

### 3.1 数据库表变更

```sql
ALTER TABLE `eiot_device_function`
    ADD COLUMN `sub_device_id` BIGINT DEFAULT NULL COMMENT '绑定的子设备ID',
    ADD COLUMN `function_type` VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '功能类型：BUS(总线按钮)/MULTI(多线按钮)/GENERAL(通用按钮)';
```

### 3.2 实体与接口调整

- `DeviceFunctionDO` 新增 `subDeviceId`、`functionType` 字段。
- `DeviceFunctionRespVO` / `DeviceFunctionCreateReqVO` / `DeviceFunctionUpdateReqVO` 新增同名字段。
- `DeviceFunctionServiceImpl` 增删改查透传新字段。
- 提供子设备列表接口复用现有 `POST /eiot/device/children/list`。

## 4. 前端架构

### 4.1 页面结构

```
RemoteConsole/
├── index.vue                 # 页面容器，管理当前选中企业/设备/子设备状态
├── components/
│   ├── VideoPanel.vue        # 左侧 38% 视频区（主视频 + 视频列表）
│   ├── FunctionBar.vue       # 顶部功能按钮栏（记录筛选/操作日志/告警处理/通话录音）
│   ├── EventTable.vue        # 右上数据表格（设备告警事件）
│   ├── QuickControl.vue      # 右侧 2×2 快捷按钮（复位/消音/启动/停止）
│   ├── DeviceCardGrid.vue    # 底部 Tab + 设备卡片网格
│   └── modals/
│       ├── LogModal.vue      # 操作日志弹窗
│       ├── AlarmModal.vue    # 告警处理弹窗
│       ├── CallModal.vue     # 通话录音弹窗（静态）
│       ├── FilterModal.vue   # 记录筛选弹窗（静态）
│       └── VerifyModal.vue   # 双人复核弹窗
```

### 4.2 数据映射

| 原型区域 | 数据来源 | 说明 |
|---|---|---|
| 左侧视频列表 | `GET /eiot/remote-console/device-tree` | 按企业分组，点击后在主视频区显示占位画面 |
| 右上告警表格 | `POST /alert/selectAlertRecordPage` | 按当前选中企业/设备过滤 |
| 操作日志弹窗 | `GET /eiot/remote-console/device/{id}/operation-logs` | 复用已有接口 |
| 告警处理弹窗 | `POST /alert/selectAlertRecordPage` | 展示未处理告警，支持确认处理 |
| 底部设备卡片 | `GET /eiot/device/{id}/function` | 根据 `functionType` 分组到总线/多线/通用；几号机选择器静态 |
| 快捷按钮 | `GET /eiot/device/{id}/function` | 按名称匹配“复位/消音/启动/停止”，优先取对应 functionType 为 GENERAL 的功能 |

### 4.3 功能类型渲染规则

- `BUS`（总线按钮）：展示在“总线”Tab 下，作为设备卡片操作按钮。
- `MULTI`（多线按钮）：展示在“多线”Tab 下，作为设备卡片操作按钮。
- `GENERAL`（通用按钮）：用于顶部 2×2 快捷按钮区（消音/复位等）和通用操作区。

当某类型无功能时，显示空状态。

### 4.4 交互流程

1. 进入页面默认加载设备树，第一个企业展开、第一台设备自动选中。
2. 选中设备后，右侧加载告警表格、设备功能列表。
3. 点击顶部“确认密码”或任意控制按钮，先弹出双人复核弹窗。
4. 复核通过后调用指令下发接口，成功后刷新操作日志与告警表格。
5. 通话录音、记录筛选弹窗按原型静态数据展示。

### 4.5 设备详情远程控制表单调整

在原有新增/编辑功能表单中增加：

- **子设备**（可选）：下拉选择当前设备的子设备，调用 `POST /eiot/device/children/list`。
- **功能类型**：下拉选择 `总线按钮` / `多线按钮` / `通用按钮`，存储为 `BUS` / `MULTI` / `GENERAL`。

## 5. API 新增与调整

### 5.1 后端新增/调整

- 修改 `DeviceFunctionController` 创建/更新接口接收新字段。
- 修改 `DeviceFunctionDO`、`DeviceFunctionRespVO`、`DeviceFunctionCreateReqVO`、`DeviceFunctionUpdateReqVO`。
- 复用 `POST /eiot/device/children/list` 获取子设备列表。

### 5.2 前端新增/调整

- `remoteConsole.api.ts`：已有接口可复用，类型按需扩展。
- 新增 `deviceRemote.api.ts` 或复用现有告警 API 调用。
- 设备功能相关 API 类型扩展 `subDeviceId` 和 `functionType`。

## 6. 样式规范

- 沿用原型深色科技风格：`#0A1628` 背景、`#00B4FF` 主色、`rgba(0,180,255,0.15)` 边框。
- 使用 SCSS + scoped，尽量使用 flex/grid 布局。
- 滚动条、卡片悬浮、按钮激活态与原型保持一致。

## 7. 兼容性

- 已有功能无 `functionType` 时，后端默认 `GENERAL`，前端按通用按钮处理。
- 设备详情远程控制功能新增字段不影响旧数据展示与编辑（编辑时为空则展示空）。

## 8. 验收标准

- [ ] 新远程控制台页面可访问，布局与原型一致。
- [ ] 左侧视频列表可展示设备树数据，点击可切换主视频区。
- [ ] 右上告警表格展示真实告警事件。
- [ ] 操作日志、告警处理弹窗展示真实数据。
- [ ] 底部总线/多线 Tab 按功能类型正确分组展示设备卡片。
- [ ] 所有控制操作均走双人复核流程。
- [ ] 设备详情远程控制新增/编辑功能支持选择子设备和功能类型。
- [ ] 前端构建通过，无新增 TypeScript 错误。
