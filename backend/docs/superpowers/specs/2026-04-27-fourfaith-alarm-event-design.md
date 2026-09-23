# 四信协议报警事件分层上报设计

## 背景

当前四信协议对接中，`triggerAbnormalEvents` 方法存在但从未被调用，导致标准类型的火警/故障/电源故障事件无法上报到平台。同时，部件级报警（Type 2）仅通过属性同步到子设备，缺少事件触发，无法在规则引擎中精确监听。

## 设计目标

1. 补全标准类型的报警事件上报（调用 `triggerAbnormalEvents`）
2. 部件级报警事件精确上报到子设备，便于规则引擎按部件维度触发
3. 父设备保留系统级/装置级/PRT/CRT 报警事件
4. 统一事件名称，降低平台规则配置成本

## 方案概述

### 事件上报分层策略

| 数据类型 | 报警内容 | 事件上报对象 | 事件标识符 |
|---------|---------|------------|----------|
| Type 1 系统状态 | 系统火警/故障/电源故障 | 父设备 | `fire_alarm_event` / `fault_event` / `power_fault_event` |
| Type 2 部件状态 | 部件火警/故障/电源故障 | 子设备 | `fire_alarm_event` / `fault_event` / `power_fault_event` |
| Type 21 装置状态 | 装置自身火警/故障 | 父设备 | `fire_alarm_event` / `fault_event` / `power_fault_event` |
| Type 0x09 PRT | PRT 报警 | 父设备 | `prt_alarm_event` |
| Type 0x0A CRT | CRT 联动 | 父设备 | `crt_alarm_event` |

### 关键设计点

1. **统一事件名**：父设备和子设备使用相同的事件标识符（`fire_alarm_event` 等），平台侧只需配置一套规则模板，通过 productKey 区分父/子设备。

2. **部件事件参数**：子设备事件参数包含部件详细信息（`comp_type`、`comp_addr`、`comp_desc`、`comp_system_type`、`comp_system_addr`），便于规则模板渲染时精确定位。

3. **CRT 条件触发**：`crt_alarm_event` 仅在 `crt_alarm_type != 0` 时触发，避免无报警也产生事件。

4. **触发位置**：
   - Type 1/21：`handleStandardUpload` 中属性上报后调用 `triggerAbnormalEvents`
   - Type 2：`handleComponentStatusUpload` 中子设备属性上报后，对异常部件触发事件
   - Type 0x09/0x0A：保持现有 `handlePrtUpload` / `handleCrtUpload` 逻辑，仅修复 CRT 条件

## 平台规则配置建议

- 父设备 productKey 规则：监听 `fire_alarm_event` → 覆盖系统/装置/PRT/CRT 报警
- 子设备 productKey 规则：监听 `fire_alarm_event` → 覆盖部件级报警
- 同一事件名使得通知模板可复用，通过事件参数中的 `event_level` 或 `comp_desc` 区分层级

## 待实现代码变更

1. `FourfaithGbtTcpComponent.handleStandardUpload`：补充 `triggerAbnormalEvents` 调用
2. `FourfaithGbtTcpComponent.handleComponentStatusUpload`：异常部件触发 `EventReport`
3. `FourfaithGbtTcpComponent.handleCrtUpload`：增加 `crt_alarm_type != 0` 条件判断
