# GB/T 26875.3 协议对接 TDengine 存储重构设计

## 1. 概述

### 1.1 问题背景

现有 GB/T 26875.3-2011（城市消防远程监控系统 报警传输网络通信协议）对接方案中，物模型定义了 30+ 个属性，其中 13 个 `text`/`date` 类型字段被映射为 `NCHAR(4096)`，导致 TDengine 创建超级表时行长度超过 65531 字节限制，报错 `code:9790, desc:Row length exceeds max length 65531`。

### 1.2 设计目标

1. 解决 TDengine 行长度超限问题
2. 支持每个部件在 GIS 地图上独立显示
3. 支持部件级告警变色
4. 保持与平台设备管理、规则引擎的兼容性

### 1.3 设计决策

- **采用双 productKey 架构**：父设备（用传装置）和部件子设备各自有独立的 productKey
- **部件子设备 productKey 规则**：`{父设备productKey}sub`
- **系统子设备不在 GIS 上独立显示**：系统状态聚合到父设备
- **FieldParser 默认 NCHAR 长度**：全局从 4096 改为 256
- **部件子设备不定义独立事件**：复用父设备的 fire_alarm_event、fault_event 等

---

## 2. 架构设计

```
GB/T 26875 TCP 协议组件 (GbtTcpComponent)
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
  父设备 productKey        部件子设备 productKey
  (product_property_父)    (product_property_部件)
        │                       │
   ┌────┴────┐             ┌────┴────┐
   ▼         ▼             ▼         ▼
device_   device_       device_   device_
property_1 property_2   property_3 property_4...
(父设备)   (父设备)      (部件1)    (部件2)
```

---

## 3. 物模型设计

### 3.1 父设备物模型（GB/T 26875 用传装置）

**文件**: `doc/gbt26875_thing_model.json`

**属性列表**（20 个字段）：

| 标识符 | 类型 | 说明 |
|--------|------|------|
| seq_no | int32 | 业务流水号 |
| cmd_byte | enum | 命令字节 |
| packet_time | date | 报文时间 |
| last_fire_alarm | enum | 最新火警状态（0=正常,1=火警） |
| last_fault | enum | 最新故障状态（0=正常,1=故障） |
| comp_fire_count | int32 | 部件火警数量 |
| last_temperature | float | 最新温度值 |
| last_pressure | float | 最新压力值 |
| dev_normal_monitor | enum | 运行模式（0=测试,1=正常） |
| dev_fire_alarm | enum | 装置火警 |
| dev_fault | enum | 装置故障 |
| dev_main_power_fault | enum | 主电状态 |
| dev_backup_power_fault | enum | 备电状态 |
| dev_comm_fault | enum | 通信状态 |
| dev_line_fault | enum | 监测线路状态 |
| dev_status_raw | int32 | 装置状态原始值 |
| dev_status_time | date | 装置状态时间 |
| dev_main_ver | int32 | 装置主版本 |
| dev_sub_ver | int32 | 装置次版本 |
| device_time | date | 装置系统时间 |

**事件列表**：fire_alarm_event、fault_event、power_fault_event、offline_event

**服务列表**：sync_device_time、inspection_cmd、init_device、read_system_status、read_component_status

### 3.2 部件子设备物模型（GB/T 26875 消防部件）

**文件**: `doc/gbt26875_component_thing_model.json`

**属性列表**（16 个字段）：

| 标识符 | 类型 | 说明 |
|--------|------|------|
| comp_system_type | int32 | 系统类型 |
| comp_system_addr | int32 | 系统地址 |
| comp_type | int32 | 部件类型 |
| comp_addr | int32 | 部件地址 |
| comp_status | int32 | 部件状态原始值 |
| comp_desc | text(64) | 部件说明 |
| comp_normal_mode | enum | 正常模式 |
| comp_fire_alarm | enum | 部件火警 |
| comp_fault | enum | 部件故障 |
| comp_shielded | enum | 屏蔽状态 |
| comp_supervised | enum | 监管状态 |
| comp_activated | enum | 启动状态 |
| comp_feedback | enum | 反馈状态 |
| comp_delay | enum | 延时状态 |
| comp_power_fault | enum | 电源故障 |
| comp_status_time | date | 状态时间 |

---

## 4. TDengine 表结构设计

### 4.1 父设备超级表

```sql
CREATE STABLE IF NOT EXISTS product_property_父pk (
    time timestamp,
    seq_no INT,
    cmd_byte TINYINT,
    packet_time NCHAR(64),
    last_fire_alarm TINYINT,
    last_fault TINYINT,
    comp_fire_count INT,
    last_temperature FLOAT,
    last_pressure FLOAT,
    dev_normal_monitor TINYINT,
    dev_fire_alarm TINYINT,
    dev_fault TINYINT,
    dev_main_power_fault TINYINT,
    dev_backup_power_fault TINYINT,
    dev_comm_fault TINYINT,
    dev_line_fault TINYINT,
    dev_status_raw INT,
    dev_status_time NCHAR(64),
    dev_main_ver INT,
    dev_sub_ver INT,
    device_time NCHAR(64)
) TAGS (device_id NCHAR(50));
```

**行长度估算**: 4×256(NCHAR) + 16×4(数值) ≈ 1088 字节 << 65531

### 4.2 部件子设备超级表

```sql
CREATE STABLE IF NOT EXISTS product_property_部件pk (
    time timestamp,
    comp_system_type TINYINT,
    comp_system_addr TINYINT,
    comp_type TINYINT,
    comp_addr INT,
    comp_status SMALLINT,
    comp_desc NCHAR(64),
    comp_normal_mode TINYINT,
    comp_fire_alarm TINYINT,
    comp_fault TINYINT,
    comp_shielded TINYINT,
    comp_supervised TINYINT,
    comp_activated TINYINT,
    comp_feedback TINYINT,
    comp_delay TINYINT,
    comp_power_fault TINYINT,
    comp_status_time NCHAR(64)
) TAGS (device_id NCHAR(50));
```

**行长度估算**: 2×256(NCHAR) + 14×4(数值) ≈ 568 字节 << 65531

---

## 5. 数据流设计

### 5.1 Type 1 - 建筑消防设施系统状态

```
系统状态条目 × N
  ├── 提取聚合状态
  │     ├── last_fire_alarm = 任一系统火警 ? 1 : 0
  │     └── last_fault = 任一系统故障 ? 1 : 0
  └── 给父设备上报：last_fire_alarm, last_fault
```

**不创建系统子设备**，只更新父设备聚合状态。

### 5.2 Type 2 - 建筑消防设施部件运行状态

```
部件状态条目 × N
  ├── 对每个部件：
  │     ├── 生成子设备 dn = "{父地址}_{系统类型}_{系统地址}_{部件类型}_{部件地址}"
  │     ├── 确保部件子设备已注册（productKey = 父pk + "sub"）
  │     └── 给部件子设备上报属性：comp_fire_alarm, comp_fault, comp_desc, ...
  └── 给父设备上报：comp_fire_count（火警部件总数）
```

### 5.3 Type 3 - 部件模拟量值

```
模拟量条目 × N
  ├── 给父设备上报：last_temperature, last_pressure
  └── （可选）给对应部件子设备上报模拟量相关属性
```

### 5.4 Type 21 - 装置运行状态

```
装置状态条目
  └── 给父设备上报：dev_fire_alarm, dev_fault, dev_main_power_fault, ...
      └── 触发事件（如有异常）
```

---

## 6. 自动注册逻辑

### 6.1 部件子设备命名规则

```
部件子设备 deviceName = {父设备地址}_{系统类型}_{系统地址}_{部件类型}_{部件地址}

示例：
  父设备地址 = "1234567890AB"
  系统类型 = 1（火灾报警系统）
  系统地址 = 2
  部件类型 = 25（火灾探测器）
  部件地址 = 100
  
  部件子设备 deviceName = "1234567890AB_1_2_25_100"
```

### 6.2 部件子设备 productKey 规则

```
部件子设备 productKey = {父设备productKey}sub

示例：
  父设备 productKey = "7ehb4fkesadn4a3k"
  部件子设备 productKey = "7ehb4fkesadn4a3ksub"
```

### 6.3 注册流程

```java
RegisterDevice.builder()
    .productKey(parentPk + "sub")  // 自动推导
    .deviceName(parentAddr + "_" + sysType + "_" + sysAddr + "_" + compType + "_" + compAddr)
    .parentId(parentDeviceId)
    .build();
```

---

## 7. 代码改造点

### 7.1 FieldParser.java

**修改**: NCHAR 默认长度从 4096 改为 256

```java
// 第75-78行
if ("NCHAR".equals(fType) && len < 1) {
    // len = 4096;  // 旧值
    len = 256;      // 新值
}
```

**影响范围**: 全局生效，所有产品的 text/date 字段默认长度均变为 256。

### 7.2 gbt26875_thing_model.json

**修改**:
- 移除所有 `_json` 大字段（system_status_json, component_status_json 等）
- 保留聚合字段（last_fire_alarm, last_fault, comp_fire_count 等）
- 保留装置级扁平字段（dev_fire_alarm, dev_fault 等）

### 7.3 新增 gbt26875_component_thing_model.json

**内容**: 部件子设备物模型定义（16 个字段）

### 7.4 GbtTcpConfig.java

**无需新增配置项**，部件子设备 productKey 自动推导。

### 7.5 GbtTcpComponent.java

**新增方法**:
- `generateComponentDeviceName(...)`: 生成部件子设备 deviceName
- `ensureComponentDeviceRegistered(...)`: 确保部件子设备已注册
- `reportToComponentDevice(...)`: 给部件子设备上报属性

**修改方法**:
- `handleDataUpload(...)`: 增加 Type 2 数据分流逻辑
- `extractKeyStatus(...)`: 调整聚合状态计算

---

## 8. 测试策略

### 8.1 测试1：FieldParser 默认长度

验证 text/date 类型字段的默认 NCHAR 长度为 256。

### 8.2 测试2：部件子设备 DN 生成

验证部件子设备命名规则符合 `{父地址}_{系统类型}_{系统地址}_{部件类型}_{部件地址}`。

### 8.3 测试3：数据分流

- Type 1 数据只更新父设备，不创建子设备
- Type 2 数据会创建部件子设备并上报属性

### 8.4 测试4：聚合状态

验证多个部件的火警状态正确聚合为 `last_fire_alarm` 和 `comp_fire_count`。

### 8.5 测试5：TDengine 建表

验证父设备和部件子设备的超级表创建成功，不抛出 9790 错误。

---

## 9. 风险与回滚

### 9.1 风险

1. **FieldParser 全局修改影响其他产品**：如果其他产品确实需要 NCHAR(4096)，需要后续支持按字段配置长度
2. **部件子设备数量过多**：一个父设备下可能有几百个部件子设备，需关注 MySQL 设备表性能
3. **自动注册并发问题**：多条 Type 2 数据同时到达时，可能并发注册同一部件子设备

### 9.2 回滚方案

1. 保留旧的 `gbt26875_thing_model.json` 备份
2. FieldParser 修改可快速回滚（改回 4096）
3. 部件子设备注册失败时降级为只更新父设备聚合状态

---

## 10. 实施顺序

1. **Step 1**: 修改 FieldParser（修复行超长根因）
2. **Step 2**: 更新父设备物模型（精简字段）
3. **Step 3**: 新增部件子设备物模型
4. **Step 4**: 改造 GbtTcpComponent（子设备注册 + 数据分流）
5. **Step 5**: 测试验证（TDengine 建表 + 数据流 + 聚合状态）
