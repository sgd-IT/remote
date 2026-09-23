# GBT26875 物模型瘦身 + 只写变化值存储改造设计

## 1. 背景与动机

### 1.1 现状问题

GBT26875 用户信息传输装置的物模型定义了 **60 个属性**，当前存储存在以下问题：

1. **旧值重复写入**：`DevicePropertyDataImpl.addProperties()` 中 `oldProperties.putAll(properties)` 导致每次写入全部 60 列（大部分是从缓存取的旧值），假设每分钟上报 1 个属性变化，一天 86400 个值中只有 1440 个是新值
2. **物模型属性过多**：60 列中 `telemetry_*`（19 列）和 `diag_*`（12 列）每次只上报 1-5 列，其余为 NULL
3. **双重存储**：TLD 9/10 PRT/CRT 诊断数据既作为属性存入 `product_property_*` 表，又作为事件存入 `thing_model_message` 表

### 1.2 改造目标

- 物模型属性从 60 列瘦身到 **29 列**（减少 52%）
- 消除旧值重复写入（只写实际收到的属性列）
- 消除 PRT/CRT 双重存储（改为纯事件）
- 遥测数据暂不存储（后续可扩展独立窄表）

## 2. 关键设计决策

### 2.1 物模型属性瘦身

**决策**：移除 `telemetry_*`（19个）和 `diag_*`（12个）属性，只保留 `alarm_*`（25个）+ `info_*`（4个）= **29 个属性**。

**理由**：
- `telemetry_*`（温度/压力/电压等）是时序数据流，不适合物模型属性存储
- `diag_*`（PRT/CRT 诊断记录）是事件记录，已在 `thing_model_message` 表中存储
- `alarm_*` 和 `info_*` 是真正的"设备当前状态"，适合物模型属性

### 2.2 只写变化值（不合并旧属性）

**决策**：修改 `addProperties` 逻辑，不合并旧属性缓存，只写入实际收到的属性列。

**理由**：
- 旧逻辑 `oldProperties.putAll(properties)` 每次写 60 列（59 列旧值 + 1 列新值），大量重复
- 新逻辑只写实际收到的属性列，零旧值重复
- 未上报的属性列为 NULL，查询时加 `IS NOT NULL` 过滤

### 2.3 遥测数据暂不存储

**决策**：TLD 3 模拟量遥测数据暂不存储。

**理由**：当前业务不需要遥测历史查询。后续可新增独立的 `gbt_telemetry` 窄表。

### 2.4 诊断数据事件化

**决策**：TLD 9/10 PRT/CRT 诊断数据只通过 `reportEvent` 存入 `thing_model_message` 事件表，不再作为属性上报。

**理由**：PRT/CRT 是历史事件记录，不是设备当前状态属性。已有事件存储机制，无需重复。

### 2.5 部件属性范围

**决策**：部件（TLD 2）只保留状态和基础信息属性，作为子设备存储。

**理由**：部件状态是子设备级别的属性，当前 comp_* 属性均为状态和基础信息，全部保留。

### 2.6 适用范围

**决策**：本次改造仅影响 GBT26875 一个产品，不影响其他产品物模型。

**影响**：`addProperties` 和 `findDevicePropertyHistory` 的改动影响所有使用 TDengine 的产品，但对其他产品同样是优化（减少旧值重复写入）。

## 3. 物模型属性清单（29 列）

### 3.1 系统级告警属性（alarm_*，19 个）

| # | identifier | name | 类型 | 来源 TLD |
|---|-----------|------|------|---------|
| 1 | alarm_fire | 系统火警 | enum | TLD 1 bit1 |
| 2 | alarm_fault | 系统故障 | enum | TLD 1 bit2 |
| 3 | alarm_main_power | 系统主电状态 | enum | TLD 1 bit8 |
| 4 | alarm_backup_power | 系统备电状态 | enum | TLD 1 bit9 |
| 5 | alarm_bus_fault | 总线故障 | enum | TLD 1 bit10 |
| 6 | alarm_comm | 通信状态 | enum | TLD 21 bit5 |
| 7 | alarm_line | 监测线路状态 | enum | TLD 21 bit6 |
| 8 | alarm_test_mode | 测试模式 | enum | TLD 1/21 bit0 |
| 9 | alarm_silence | 消音状态 | enum | - |
| 10 | alarm_mute | 静音状态 | enum | - |
| 11 | alarm_shield | 屏蔽状态 | enum | TLD 1 bit3 |
| 12 | alarm_supervised | 监管状态 | enum | TLD 1 bit4 |
| 13 | alarm_activated | 启动状态 | enum | TLD 1 bit5 |
| 14 | alarm_feedback | 反馈状态 | enum | TLD 1 bit6 |
| 15 | alarm_delay | 延时状态 | enum | TLD 1 bit7 |
| 16 | alarm_manual_mode | 手动模式 | enum | TLD 1 bit11 |
| 17 | alarm_config_changed | 配置变更标志 | enum | TLD 1 bit12 |
| 18 | alarm_status_time | 告警状态时间 | date | TLD 1 |
| 19 | alarm_raw_value | 状态原始值 | int32 | TLD 1 |

### 3.2 装置级告警属性（alarm_device_*，6 个）

| # | identifier | name | 类型 | 来源 TLD |
|---|-----------|------|------|---------|
| 1 | alarm_device_fire | 装置火警 | enum | TLD 21 bit1 |
| 2 | alarm_device_fault | 装置故障 | enum | TLD 21 bit2 |
| 3 | alarm_device_main_power | 装置主电状态 | enum | TLD 21 bit3 |
| 4 | alarm_device_backup_power | 装置备电状态 | enum | TLD 21 bit4 |
| 5 | alarm_device_comm | 装置通信状态 | enum | TLD 21 bit5 |
| 6 | alarm_device_line | 装置线路状态 | enum | TLD 21 bit6 |

### 3.3 设备信息属性（info_*，4 个）

| # | identifier | name | 类型 | 来源 TLD |
|---|-----------|------|------|---------|
| 1 | info_firmware_ver_major | 固件主版本 | int32 | TLD 25 |
| 2 | info_firmware_ver_minor | 固件次版本 | int32 | TLD 25 |
| 3 | info_config_desc | 装置配置描述 | text | TLD 26 |
| 4 | info_system_time | 装置系统时间 | date | TLD 28 |

## 4. TLD 数据存储路由

| TLD | 数据类型 | 存储位置 | 处理方式 |
|-----|---------|---------|---------|
| TLD 1 系统状态 | 设备属性 | product_property_{pk} 表 alarm_* 列 | 只写实际上报的列 |
| TLD 21 装置状态 | 设备属性 | product_property_{pk} 表 alarm_device_* 列 | 只写实际上报的列 |
| TLD 2 部件状态 | 子设备属性 | 子设备 property 表 comp_* 列 | 只写状态+基础信息 |
| TLD 25/26/28 设备信息 | 设备属性 | product_property_{pk} 表 info_* 列 | 变化检测后写入 |
| TLD 3 模拟量遥测 | 时序数据 | ❌ 暂不存储 | 代码中移除属性上报 |
| TLD 9 PRT | 事件记录 | thing_model_message 事件表 | 仅 reportEvent |
| TLD 10 CRT | 事件记录 | thing_model_message 事件表 | 仅 reportEvent |

## 5. 写入逻辑变更

### 5.1 addProperties 方法改造

**当前逻辑**（DevicePropertyDataImpl.java L85-122）：
```java
Map<String, DevicePropertyCache> oldProperties = deviceApi.getPropertiesFromCache(deviceId);
oldProperties.putAll(properties);  // 合并全部旧属性
// INSERT 全部 60 列
```

**新逻辑**：
```java
// 不合并旧属性，直接用 properties 参数写入
Map<String, DevicePropertyCache> writeProperties = properties;
// INSERT 只写实际收到的列（1-25 列）
```

### 5.2 TLD 3 遥测数据处理

**当前**：`handleAnalogValue()` → `ThingModelPropertyMapper.mapAnalogValue()` → `reportProperty()`
**新**：`handleAnalogValue()` → 直接 return（不再上报属性）

### 5.3 TLD 9/10 诊断数据处理

**当前**：`handlePrtUpload()` → `reportProperty()` + `reportEvent()`
**新**：`handlePrtUpload()` → 仅 `reportEvent()`

**当前**：`handleCrtUpload()` → `reportProperty()` + `reportEvent()`
**新**：`handleCrtUpload()` → 仅 `reportEvent()`

## 6. 查询逻辑变更

### 6.1 findDevicePropertyHistory 方法改造

**当前 SQL**：
```sql
SELECT time, {field} AS `value`, device_id 
FROM {tableName} 
WHERE device_id = ? AND time >= ? AND time <= ? 
ORDER BY time ASC
```

**新 SQL**：
```sql
SELECT time, {field} AS `value`, device_id 
FROM {tableName} 
WHERE device_id = ? AND time >= ? AND time <= ? 
AND {field} IS NOT NULL
ORDER BY time ASC
```

增加 `AND {field} IS NOT NULL` 条件，只返回有值的行（即实际发生变化的记录）。

## 7. 数据完整性保障

| 数据类型 | 存储位置 | 完整性 | 说明 |
|---------|---------|--------|------|
| 系统状态 alarm_* | product_property 宽表 | ✅ 完整 | 每次 TLD 1 上报写入，只写有值列 |
| 装置状态 alarm_device_* | product_property 宽表 | ✅ 完整 | TLD 21 上报时写入；未上报时为 NULL，查询过滤 |
| 部件状态 comp_* | 子设备 property 表 | ✅ 完整 | 变化检测后写入 |
| 设备信息 info_* | product_property 宽表 | ✅ 完整 | 变化检测后写入 |
| 遥测数据 telemetry_* | ❌ 暂不存储 | ⚠️ | 后续可新增 gbt_telemetry 窄表 |
| PRT 事件 | thing_model_message | ✅ 完整 | 每条 PRT 一行事件记录 |
| CRT 事件 | thing_model_message | ✅ 完整 | 每条 CRT 一行事件记录 |

## 8. 改动文件清单

| # | 文件 | 改动内容 | 影响范围 |
|---|------|---------|---------|
| 1 | `doc/gbt26875/gbt26875_thing_model.json` | 移除 telemetry_* (19个) + diag_* (12个) 属性 | 仅 GBT26875 物模型 |
| 2 | `FourfaithGbtDataReporter.java` | TLD 3 不再上报属性；TLD 9/10 移除 reportProperty 只保留 reportEvent | 仅 GBT26875 组件 |
| 3 | `ThingModelPropertyMapper.java` | 移除 mapAnalogValue 和 mapDiagnostic 方法 | 仅 GBT26875 组件 |
| 4 | `DevicePropertyDataImpl.java` | addProperties 不合并旧属性，只写传入的属性列 | 所有 TDengine 产品 |
| 5 | `DevicePropertyDataImpl.java` | findDevicePropertyHistory 加 `AND field IS NOT NULL` 条件 | 所有 TDengine 产品 |

## 9. 存储效率对比

| 指标 | 改造前 | 改造后 |
|------|--------|--------|
| 属性表列数 | 60 列 | 29 列（减少 52%） |
| 每次写入列数 | 60 列（合并旧值） | 只写实际收到的列（1-25 列） |
| 旧值重复写入 | 严重（59/60 是旧值） | 零重复 |
| PRT/CRT 存储 | 双重（属性+事件） | 单一（仅事件） |
| 遥测稀疏 | 19 列只写 1-2 列 | 不存储 |

## 10. 风险与回滚

### 10.1 风险

- `addProperties` 改动影响所有 TDengine 产品：其他产品的属性上报也会变为只写变化值
- `findDevicePropertyHistory` 加 IS NOT NULL：其他产品查询也需适配

### 10.2 回滚方案

- 恢复 `oldProperties.putAll(properties)` 合并逻辑
- 移除 `IS NOT NULL` 过滤条件
- 物模型恢复 telemetry_* 和 diag_* 属性
