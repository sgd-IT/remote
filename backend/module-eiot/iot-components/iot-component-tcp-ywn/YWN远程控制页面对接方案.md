# YWN 设备与远程控制页面对接方案

## 现状分析

### 当前远程控制页面架构

```
index.vue
├── MainKeyboard.vue        → 12个指示灯(硬编码) + 8个控制按钮(名称匹配)
├── DeviceCardGrid.vue      → 多线盘(MULTI) + 总线盘(BUS) 卡片网格
├── AlarmLogPanel.vue       → 查询告警记录 (POST /alert/selectAlertRecordPage)
├── OperationLogPanel.vue   → 查询操作日志
└── VideoPanel.vue          → 视频监控
```

### 当前数据流

```
前端 getDeviceFunctions(deviceId)
    → GET /eiot/device/{deviceId}/function
    → 返回 eiot_device_function 表记录
    → 每条记录有 functionType: GENERAL / MULTI / BUS

前端 sendInstruction({deviceId, deviceFunctionId, password, commandType})
    → POST /eiot/device/instruction/send
    → DeviceInstructionServiceImpl
        → 查 BrandModelFunction → 调四信HTTP API → 得到 command_code
        → deviceCtrlService.invokeService(deviceId, "remote_control", {command_code})
        → ThingComponent.serviceInvoke()
```

### YWN 与现有架构的差距

| 问题 | 现状 | YWN 需要 |
|------|------|---------|
| 指令下发 | 经过四信HTTP API转换 | YWN直接用服务名(reset/silence等) |
| 指示灯 | 12个硬编码，active固定 | 需从YWN事件属性动态获取 |
| 报警状态 | getAlarmStatus返回TODO | 需从event_code判断火警/故障 |
| 报警记录 | DeviceAlertChecker需配置 | 需为YWN配置告警条件 |
| 设备功能 | eiot_device_function表配置 | 需插入YWN对应功能记录 |

---

## 方案一：事件状态映射到指示灯

### 1.1 映射关系

YWN 事件上传(0x80)后，`TcpYwnComponent` 会将事件属性写入 Redis 缓存。从缓存读取 `event_code` 即可判断当前状态。

| 指示灯 | YWN event_code 范围 | 判断条件 | 对应事件 |
|--------|---------------------|---------|---------|
| 火警 | 0x01-0x0F | `isFireAlarm()` | fire_alarm_event |
| 监管 | 0x7F | `event_code == 0x7F` | SUPERVISORY |
| 反馈 | 0x1A, 0x1C | `event_code == 0x1A \|\| 0x1C` | feedback_event |
| 主电 | — | 始终亮(假设有电) | — |
| 启动 | 0x10, 0x12 | `event_code == 0x10 \|\| 0x12` | BUS_DEVICE_STARTED |
| 故障 | 0x40-0x5F | `isFault()` | fault_event |
| 屏蔽 | 0x30-0x3F | `event_code in [0x30, 0x3F]` | BUS_DEVICE_SHIELDED |
| 备电 | — | 始终亮(假设有电) | — |
| 手动 | — | 从 control_mode 属性判断 | — |
| 自动 | — | 从 control_mode 属性判断 | — |
| 喷洒 | — | YWN协议无此状态 | — |
| 消音 | — | 收到消音命令后亮，下次事件后灭 | — |

### 1.2 后端修改：RemoteConsoleServiceImpl

在 `getDeviceDetail` 中增加 YWN 设备的指示灯状态计算：

```java
// RemoteConsoleServiceImpl.java - getDeviceDetail 方法中添加

// 获取设备属性缓存
Map<String, DevicePropertyCache> properties = deviceInfoService.getPropertiesFromCache(deviceId);

// 计算 YWN 指示灯状态
if (properties != null && properties.containsKey("event_code")) {
    int eventCode = Integer.parseInt(properties.get("event_code").getValue().toString());
    Map<String, Boolean> indicators = new LinkedHashMap<>();

    indicators.put("fire_alarm", eventCode >= 0x01 && eventCode <= 0x0F);
    indicators.put("supervisory", eventCode == 0x7F);
    indicators.put("feedback", eventCode == 0x1A || eventCode == 0x1C);
    indicators.put("main_power", true);  // 默认有电
    indicators.put("started", eventCode == 0x10 || eventCode == 0x12);
    indicators.put("fault", eventCode >= 0x40 && eventCode <= 0x5F);
    indicators.put("shielded", eventCode >= 0x30 && eventCode <= 0x3F);
    indicators.put("backup_power", true);
    indicators.put("manual", "manual".equals(getControlMode(properties)));
    indicators.put("auto", "auto".equals(getControlMode(properties)));
    indicators.put("spray", false);  // YWN无喷洒状态
    indicators.put("muted", false);  // 可从操作日志判断

    vo.setIndicators(indicators);
}
```

### 1.3 前端修改：MainKeyboard.vue

将硬编码的 indicators 改为从 props 接收：

```vue
<script setup lang="ts">
const props = defineProps<{
  functions: DeviceFunctionVO[]
  indicators?: Record<string, boolean>  // 新增：从父组件传入
}>()

const indicatorList = computed(() => {
  const defs = [
    { name: '火警', key: 'fire_alarm',    type: 'danger' },
    { name: '监管', key: 'supervisory',   type: 'primary' },
    { name: '反馈', key: 'feedback',      type: 'warning' },
    { name: '主电', key: 'main_power',    type: 'success' },
    { name: '启动', key: 'started',       type: 'primary' },
    { name: '故障', key: 'fault',         type: 'danger' },
    { name: '屏蔽', key: 'shielded',      type: 'warning' },
    { name: '备电', key: 'backup_power',  type: 'success' },
    { name: '手动', key: 'manual',        type: 'primary' },
    { name: '自动', key: 'auto',          type: 'success' },
    { name: '喷洒', key: 'spray',         type: 'primary' },
    { name: '消音', key: 'muted',         type: 'warning' }
  ]
  return defs.map(d => ({
    ...d,
    active: props.indicators?.[d.key] ?? false
  }))
})
</script>
```

### 1.4 index.vue 传递指示灯数据

```vue
<MainKeyboard
  :functions="deviceFunctions"
  :indicators="deviceIndicators"
  @action="handleGeneralAction"
/>

<script setup lang="ts">
const deviceIndicators = ref<Record<string, boolean>>({})

const loadDeviceDetail = async (deviceId: string | number) => {
  try {
    const detail = await getDeviceDetail(deviceId)
    deviceIndicators.value = detail?.indicators || {}
  } catch (e) {
    deviceIndicators.value = {}
  }
}

// 在 applySelectedDevice 中调用
const applySelectedDevice = (device, enterpriseName?) => {
  selectedDeviceId.value = device.id
  selectedDeviceInfo.value = device
  selectedEnterpriseName.value = enterpriseName || '--'
  loadCameras(device.id)
  loadDeviceFunctions(device.id)
  loadDeviceDetail(device.id)  // 新增：加载设备详情(含指示灯)
}
</script>
```

---

## 方案二：报警记录触发

### 2.1 报警记录生成机制

系统已有完整的告警链路：

```
TcpYwnComponent.report(EventReport)
    → ThingModelMessage(type=event, identifier=fire_alarm_event)
    → DeviceAlertChecker.handle(message)
        → 查询 eiot_device_alert_config 表中的告警配置
        → ConditionEvaluator.evaluate(conditions, message)
        → 条件满足 → DeviceAlertAction.doAlert()
            → 写入 eiot_device_alert_record 表
```

**关键：需要配置 `eiot_device_alert_config` 表记录**，否则 `DeviceAlertChecker` 不会触发告警。

### 2.2 告警配置 SQL

为 YWN 产品配置 3 条告警规则：

```sql
-- 假设 YWN 产品的 productKey = 'ywnProductKey'
-- 假设租户ID = 1

-- 规则1：火警事件 → HIGH 级别告警
INSERT INTO eiot_device_alert_config
(tenant_id, product_key, name, level, conditions, trigger_options, enabled, create_time, update_time, deleted)
VALUES (
  1,
  'ywnProductKey',
  'YWN火警告警',
  'HIGH',
  '[
    {
      "type": "event",
      "identifier": "fire_alarm_event",
      "params": {
        "event_code": {"operator": ">=", "value": 1, "type": "int"},
        "event_code": {"operator": "<=", "value": 15, "type": "int"}
      }
    }
  ]',
  '{"enableRecover": true}',
  1,
  NOW(),
  NOW(),
  0
);

-- 规则2：故障事件 → MEDIUM 级别告警
INSERT INTO eiot_device_alert_config
(tenant_id, product_key, name, level, conditions, trigger_options, enabled, create_time, update_time, deleted)
VALUES (
  1,
  'ywnProductKey',
  'YWN故障告警',
  'MEDIUM',
  '[
    {
      "type": "event",
      "identifier": "fault_event"
    }
  ]',
  '{"enableRecover": true}',
  1,
  NOW(),
  NOW(),
  0
);

-- 规则3：控制器故障 → MEDIUM 级别告警
INSERT INTO eiot_device_alert_config
(tenant_id, product_key, name, level, conditions, trigger_options, enabled, create_time, update_time, deleted)
VALUES (
  1,
  'ywnProductKey',
  'YWN控制器故障',
  'MEDIUM',
  '[
    {
      "type": "event",
      "identifier": "controller_fault_event"
    }
  ]',
  '{"enableRecover": true}',
  1,
  NOW(),
  NOW(),
  0
);
```

### 2.3 报警记录展示

`AlarmLogPanel.vue` 已经调用 `getAlertRecordPage`，告警记录写入后自动可见：

```typescript
// AlarmLogPanel.vue 已有的查询逻辑
const res = await getAlertRecordPage({
  pageNo: 1,
  pageSize: 20,
  deviceId: props.deviceId
})
```

告警级别映射（已有）：
- `HIGH` → 红色标签 "火警"
- `MEDIUM` → 橙色标签 "动作"
- `LOW` → �色标签 "一般"

### 2.4 告警恢复

当 YWN 上报恢复类事件（event_code=0x64 故障恢复）时，如果告警配置中 `enableRecover=true`，`DeviceAlertChecker` 会自动调用 `doRecover()` 将对应告警记录状态改为 `recover`。

---

## 方案三：远程控制按钮映射

### 3.1 问题分析

当前 `sendInstruction` 流程依赖四信 HTTP API 转换指令码，YWN 不需要这个环节。有两种方案：

| 方案 | 说明 | 优点 | 缺点 |
|------|------|------|------|
| A. 直接调 invokeService | 前端直接调 `/eiot/device/service/invoke` | 最简单，无需改后端 | 绕过密码校验和操作日志 |
| B. 扩展 sendInstruction | 后端按协议分支处理 | 保留密码校验和日志 | 需改后端代码 |

**推荐方案 B**：在 `DeviceInstructionServiceImpl` 中增加协议分支。

### 3.2 方案 B：后端修改

#### 3.2.1 修改 DeviceInstructionServiceImpl

```java
// DeviceInstructionServiceImpl.java - doSendInstruction 方法中添加协议分支

private InstructionSendRespVO doSendInstruction(
        InstructionSendReqVO reqVO, DeviceFunctionDO deviceFunction,
        BrandModelFunctionDO functionDef) {

    DeviceInfo device = deviceInfoService.getDeviceInfo(reqVO.getDeviceId());

    // === YWN 协议分支：直接调用服务，不经过四信API ===
    if ("ywn-tcp".equals(device.getProtocolCode())
            || isYwnProduct(device.getProductKey())) {
        return sendYwnInstruction(reqVO, deviceFunction, functionDef, device);
    }

    // === 原有四信流程 ===
    // ... 密码校验 ...
    // ... 四信API调用 ...
    // ... invokeService("remote_control", {command_code}) ...
}

/**
 * YWN 指令下发：直接用物模型服务名调用
 */
private InstructionSendRespVO sendYwnInstruction(
        InstructionSendReqVO reqVO, DeviceFunctionDO deviceFunction,
        BrandModelFunctionDO functionDef, DeviceInfo device) {

    // 1. 密码校验（消音跳过）
    String functionName = functionDef.getName();
    if (!"消音".equals(functionName)) {
        DeviceRemoteConfigDO config = getRemoteConfig(reqVO.getDeviceId());
        if (config != null && config.getConfirmPassword() != null) {
            if (!config.getConfirmPassword().equals(reqVO.getPassword())) {
                return InstructionSendRespVO.fail("密码验证失败");
            }
        }
    }

    // 2. 从 deviceFunction.paramValues 解析 YWN 服务参数
    Map<String, Object> args = new LinkedHashMap<>();
    if (deviceFunction.getParamValues() != null) {
        args = JsonUtils.parseObject(deviceFunction.getParamValues(), Map.class);
    }

    // 3. 直接调用 YWN 服务（service name = functionDef 的 functionId 字段）
    //    functionId 在 YWN 场景下存储的是物模型服务标识符(如 "reset", "silence")
    String serviceName = functionDef.getFunctionId();  // "reset" / "silence" / "bus_start" 等
    deviceCtrlService.invokeService(reqVO.getDeviceId(), serviceName, args);

    return InstructionSendRespVO.success("指令下发成功");
}
```

#### 3.2.2 设备功能数据配置

在 `eiot_brand_model_function` 和 `eiot_device_function` 表中配置 YWN 功能。

**品牌型号定义**（只需配置一次）：

```sql
-- 1. 创建 YWN 品牌
INSERT INTO eiot_brand (brand_code, name, create_time, update_time, deleted, tenant_id)
VALUES ('YWN', 'YWN控制器', NOW(), NOW(), 0, 1);

-- 2. 创建 YWN 型号
INSERT INTO eiot_brand_model (brand_id, model, version, create_time, update_time, deleted, tenant_id)
VALUES ((SELECT id FROM eiot_brand WHERE brand_code='YWN'), 'V27', 'V27', NOW(), NOW(), 0, 1);

-- 3. 创建 15 个功能定义
-- function_id 字段在 YWN 场景下存储物模型服务标识符
INSERT INTO eiot_brand_model_function (brand_model_id, function_id, name, create_time, update_time, deleted, tenant_id)
VALUES
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'reset',               '复位',       NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'silence',              '消音',       NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'bus_start',            '总线启动',   NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'bus_stop',             '总线停止',   NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'shield',               '屏蔽',       NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'cancel_shield',        '解除屏蔽',   NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'manual_switch',        '手自动转换', NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'multi_line_start',     '多线启动',   NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'multi_line_stop',      '多线停止',   NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'auto_allow',           '自动允许',   NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'auto_prohibit',        '自动禁止',   NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'manual_allow',         '手动允许',   NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'manual_prohibit',      '手动禁止',   NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'auto_manual_allow',    '自手动允许', NOW(), NOW(), 0, 1),
((SELECT id FROM eiot_brand_model WHERE model='V27'), 'auto_manual_prohibit', '自手动禁止', NOW(), NOW(), 0, 1);
```

#### 3.2.3 MainKeyboard 控制按钮映射

现有 `MainKeyboard.vue` 的控制按钮通过名称匹配 `generalFunctions`：

```javascript
// MainKeyboard.vue 现有逻辑
const defs = [
  { name: '消音', icon: '🔇' },
  { name: '复位', icon: '↻' },
  { name: '自检', icon: '✓' },
  { name: '手动禁止', icon: '✋' },
  { name: '自动禁止', icon: '🚫' },
  { name: '喷洒禁止', icon: '💧' },
  { name: '点位屏蔽', icon: '🛡' },
  { name: '解除屏蔽', icon: '🔓' }
]
// 匹配逻辑：buttonName 或 functionName 包含定义名称
```

**对应的 eiot_device_function 记录**（每个设备需配置）：

```sql
-- 假设设备 ID = 100，品牌型号 ID = 上面的 V27 型号 ID
-- functionType = 'GENERAL' 的功能 → MainKeyboard 的控制按钮

INSERT INTO eiot_device_function
(device_id, brand_model_function_id, button_name, sort_order, param_values, enabled, function_type, create_time, update_time, deleted, tenant_id)
VALUES
-- 消音 → silence 服务
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='silence'), '消音', 1, '{"machineNo":1}', 1, 'GENERAL', NOW(), NOW(), 0, 1),
-- 复位 → reset 服务
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='reset'), '复位', 2, '{"machineNo":1}', 1, 'GENERAL', NOW(), NOW(), 0, 1),
-- 手动禁止 → manual_prohibit 服务
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='manual_prohibit'), '手动禁止', 4, '{"machineNo":1}', 1, 'GENERAL', NOW(), NOW(), 0, 1),
-- 自动禁止 → auto_prohibit 服务
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='auto_prohibit'), '自动禁止', 5, '{"machineNo":1}', 1, 'GENERAL', NOW(), NOW(), 0, 1),
-- 点位屏蔽 → shield 服务
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='shield'), '点位屏蔽', 7, '{"machineNo":1,"loopNo":0,"addressNo":0}', 1, 'GENERAL', NOW(), NOW(), 0, 1),
-- 解除屏蔽 → cancel_shield 服务
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='cancel_shield'), '解除屏蔽', 8, '{"machineNo":1,"loopNo":0,"addressNo":0}', 1, 'GENERAL', NOW(), NOW(), 0, 1);
```

> `param_values` 中的参数名必须与 `TcpYwnComponent.serviceInvoke()` 中的 `getIntParam(params, "machineNo", ...)` 一致。

---

## 方案四：多线盘/总线盘按钮映射

### 4.1 页面渲染机制

```
DeviceCardGrid.vue
├── PanelSection title="多线盘" type="MULTI" :functions="multiFunctions"
│   └── 渲染为 5列网格，至少20个卡位
│       每个卡片有 "启动" 和 "停止" 两个按钮
│
└── PanelSection title="总线盘" type="BUS" :functions="busFunctions"
    └── 渲染为 5列网格，至少20个卡位
        每个卡片有 "启动" 和 "停止" 两个按钮
```

### 4.2 多线盘按钮数据

`functionType='MULTI'` 的设备功能记录会被 `DeviceCardGrid` 渲染到多线盘区域：

```sql
-- 多线盘功能（每个按钮对应一个多线盘路号）
-- 第1路
INSERT INTO eiot_device_function
(device_id, brand_model_function_id, button_name, sort_order, param_values, enabled, function_type, create_time, update_time, deleted, tenant_id)
VALUES
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='multi_line_start'), '1号多线盘-启动', 1, '{"machineNo":1,"loopNo":1,"addressNo":1}', 1, 'MULTI', NOW(), NOW(), 0, 1),

-- 第2路
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='multi_line_start'), '2号多线盘-启动', 2, '{"machineNo":1,"loopNo":1,"addressNo":2}', 1, 'MULTI', NOW(), NOW(), 0, 1),

-- 第3路
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='multi_line_start'), '3号多线盘-启动', 3, '{"machineNo":1,"loopNo":1,"addressNo":3}', 1, 'MULTI', NOW(), NOW(), 0, 1);

-- 注意：PanelSection 的 DeviceCard 组件对每张卡片同时提供"启动"和"停止"按钮
-- 启动 → commandType='1' → 后端用 multi_line_start 服务
-- 停止 → commandType='0' → 后端用 multi_line_stop 服务
```

### 4.3 总线盘按钮数据

`functionType='BUS'` 的设备功能记录会被渲染到总线盘区域：

```sql
-- 总线盘功能（每个按钮对应一个总线设备地址）
-- 1回路1地址
INSERT INTO eiot_device_function
(device_id, brand_model_function_id, button_name, sort_order, param_values, enabled, function_type, create_time, update_time, deleted, tenant_id)
VALUES
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='bus_start'), '1回路-1号', 1, '{"machineNo":1,"loopNo":1,"addressNo":1}', 1, 'BUS', NOW(), NOW(), 0, 1),

-- 1回路2地址
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='bus_start'), '1回路-2号', 2, '{"machineNo":1,"loopNo":1,"addressNo":2}', 1, 'BUS', NOW(), NOW(), 0, 1),

-- 2回路1地址
(100, (SELECT id FROM eiot_brand_model_function WHERE function_id='bus_start'), '2回路-1号', 3, '{"machineNo":1,"loopNo":2,"addressNo":1}', 1, 'BUS', NOW(), NOW(), 0, 1);
```

### 4.4 启动/停止按钮处理

`index.vue` 中的 `handleCardAction` 已有启动/停止逻辑：

```typescript
// index.vue 现有逻辑
const handleCardAction = (fn: DeviceFunctionVO, _panelType: 'MULTI' | 'BUS') => {
  const isStart = (fn.buttonName || fn.functionName || '').includes('启动') ||
                  (fn.buttonName || fn.functionName || '').includes('开始') ||
                  (fn.buttonName || fn.functionName || '').includes('开')
  const actionName = isStart ? '启动' : '停止'
  openVerify(`${fn.buttonName} - ${actionName}`, fn, isStart ? '1' : '0')
}
```

**后端需要修改**：根据 `commandType`（'1'=启动, '0'=停止）选择不同的 YWN 服务名：

```java
// DeviceInstructionServiceImpl.java - sendYwnInstruction 方法中

// 根据 commandType 切换启动/停止服务
String serviceName = functionDef.getFunctionId();  // 基础服务名
if ("multi_line_start".equals(serviceName) && "0".equals(reqVO.getCommandType())) {
    serviceName = "multi_line_stop";
}
if ("bus_start".equals(serviceName) && "0".equals(reqVO.getCommandType())) {
    serviceName = "bus_stop";
}

deviceCtrlService.invokeService(reqVO.getDeviceId(), serviceName, args);
```

### 4.5 完整的数据流

```
用户点击多线盘第1路"启动"按钮
    ↓
index.vue: handleCardAction(fn, 'MULTI')
    → isStart = true, commandType = '1'
    → openVerify → VerifyModal 弹出
    ↓
用户输入密码确认
    ↓
sendInstruction({ deviceId:100, deviceFunctionId:xxx, password:'xxx', commandType:'1' })
    ↓
POST /eiot/device/instruction/send
    ↓
DeviceInstructionServiceImpl.doSendInstruction()
    → 检测到 ywn-tcp 协议 → 走 sendYwnInstruction 分支
    → 密码校验通过
    → functionDef.functionId = "multi_line_start"
    → commandType = '1' → serviceName = "multi_line_start"
    → args = { machineNo:1, loopNo:1, addressNo:1 }
    ↓
deviceCtrlService.invokeService(100, "multi_line_start", {machineNo:1, loopNo:1, addressNo:1})
    ↓
ThingModelMessage(type=service, identifier=multi_line_start)
    → componentManager.sendToDevice(message)
    → Redis路由: str:iot:device:router:{pk}:{dn} → ywn-tcp_xxx
    ↓
TcpYwnComponent.onMessage()
    → TYPE_SERVICE → doServiceInvoke()
    → serviceInvoke(ServiceInvoke)
    → case "multi_line_start":
        → DataYwnEncoder.encodeControl(CMD_MULTI_LINE_START, deviceNumber, 1, 1, 1)
        → 生成 15 字节下行帧: 0x0A + deviceNumber(2B LE) + 载荷(12B)
    → sendMsg(dn, buffer)
    → TCP 发送到 YWN 控制器
    ↓
YWN 控制器收到帧 → 执行多线盘启动 → 回复 ACK (0x01)
    ↓
TcpYwnComponent.handleControlAck()
    → 日志记录
    ↓
ServiceReply(code=0) → 写入 thing_model_message 表
    ↓
前端操作日志面板可见
```

---

## 总结：完整对接清单

| 序号 | 改动项 | 文件/位置 | 说明 |
|------|--------|----------|------|
| 1 | 添加 server 依赖 | server/pom.xml | iot-component-tcp-ywn（已完成） |
| 2 | 创建产品 | 系统前端 | nodeType=2, protocolCode=ywn-tcp |
| 3 | 配置物模型 | 系统前端 | 导入 ywn_thing_model.json |
| 4 | 配置组件 | 系统前端 | type=ywn-tcp, port=9200 |
| 5 | 注册设备 | 系统前端 | dn=设备编号字符串 |
| 6 | 创建品牌型号 | SQL | eiot_brand + eiot_brand_model |
| 7 | 创建功能定义 | SQL | eiot_brand_model_function（15条） |
| 8 | 配置设备功能 | SQL/前端 | eiot_device_function（每设备） |
| 9 | 配置告警规则 | SQL | eiot_device_alert_config（3条） |
| 10 | 后端：YWN指令分支 | DeviceInstructionServiceImpl | 增加 sendYwnInstruction 方法 |
| 11 | 后端：指示灯状态 | RemoteConsoleServiceImpl | getDeviceDetail 增加 indicators |
| 12 | 前端：指示灯动态化 | MainKeyboard.vue | 从 props 读取 indicators |
| 13 | 前端：传递指示灯 | index.vue | 调用 getDeviceDetail 获取 |
