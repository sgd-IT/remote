# 四信子设备 dn 命名及 CRT 关联设计调整

## 1. 概述

### 1.1 背景

四信 F-PRC200 私有协议扩展中，子设备（部件）的标识规则需要与四信点位表结构对齐。四信侧将**主机号(2位) + 回路号(2位) + 地址号(3位)** 拼接为7位十六进制字符串作为**部件地址**，用于唯一标识主机下的一个子设备。

当前设计采用国标 GB/T 26875 的子设备命名规则 `{父地址}_{sysType}_{sysAddr}_{compType}_{compAddr}`，与四信点位表结构不匹配，导致：
- CRT 报警无法正确关联到预导入的子设备
- 子设备 dn 中未包含主机号和回路号，存在命名冲突风险

### 1.2 设计目标

1. 子设备 dn 与四信部件地址对齐（7位十六进制）
2. CRT 报警能通过部件地址精准关联到子设备
3. Excel 导入的子设备与 CRT 报警创建的使用同一套 dn 规则
4. CRT 未命中子设备时不自动注册（依赖 Excel 预导入）

---

## 2. 子设备 dn 规则

### 2.1 变更内容

| 项目 | 变更前 | 变更后 |
|------|--------|--------|
| **dn 格式** | `{父地址}_{sysType}_{sysAddr}_{compType}_{compAddr}` | `部件地址(7位十六进制)` |
| **示例** | `1046945061FF_0_0_1_717` | `114a1cd` |
| **生成方式** | 国标四段拼接 | `hostNo(2位) + circuitNo(2位) + addressNo(3位)` |

### 2.2 部件地址结构

```
部件地址 = 主机号(2位十六进制) + 回路号(2位十六进制) + 地址号(3位十六进制)
         = 总长7位，不足前面补0

示例：
  主机号 = 11  → "11"
  回路号 = 4A  → "4a"
  地址号 = 1CD → "1cd"
  部件地址 = "114a1cd"
```

### 2.3 生成方法

```java
static String generateComponentDeviceName(String partAddr) {
    return partAddr.toLowerCase();  // 统一小写
}
```

---

## 3. 子设备查询方式

### 3.1 查询流程

```
收到 CRT 报文 (Type 0x0A)
  ├── 从报文头获取父设备地址 sourceAddr
  ├── deviceApi.getDeviceByDn(sourceAddr) → 父设备信息 → parentId
  ├── 解析 CRT 设备位置字符串
  │     ├── 提取主机号: "11"
  │     ├── 提取回路号: "4A"
  │     ├── 提取地址号: "1CD"
  │     └── 拼接部件地址: partAddr = "114a1cd"
  └── deviceApi.getDeviceByParentIdAndDn(parentId, partAddr)
        ├── 命中 → 给该子设备上报属性 / 触发 crt_alarm_event
        └── 未命中 → 记录 warn 日志（子设备未预注册，不自动创建）
```

### 3.2 新增接口

**DeviceApi.java**
```java
/**
 * 根据父设备ID和设备名称查询子设备
 *
 * @param parentId 父设备ID
 * @param dn 设备名称（部件地址）
 * @return 子设备信息，未找到返回 null
 */
DeviceInfo getDeviceByParentIdAndDn(Long parentId, String dn);
```

**EiotDeviceInfoMapper.java**
```java
default EiotDeviceInfoDO selectByParentIdAndDn(Long parentId, String dn) {
    return selectOne(new LambdaQueryWrapper<EiotDeviceInfoDO>()
            .eq(EiotDeviceInfoDO::getParentId, parentId)
            .eq(EiotDeviceInfoDO::getDn, dn)
            .last("LIMIT 1"));
}
```

---

## 4. CRT 解析调整

### 4.1 设备位置字符串格式

来自四信文档 `F-PRC200消防协议解析.txt`：

```
事件字段2（设备位置）: &&QN01,机器11,回路3A,部件2CD
```

### 4.2 提取逻辑

当前代码只提取回路号和地址号，需要**补全主机号提取**：

```java
private String extractCrtPartAddr(String desc) {
    // desc 示例: "&&QN01,机器11,回路3A,部件2CD"
    String[] parts = desc.split(",");
    if (parts.length < 4) return null;

    String hostNo = extractValue(parts[1], "机器");      // "11"
    String circuitNo = extractValue(parts[2], "回路");   // "3A"
    String addressNo = extractValue(parts[3], "部件");   // "2CD"

    return (hostNo + circuitNo + addressNo).toLowerCase();  // "113a2cd"
}

private String extractValue(String part, String prefix) {
    int idx = part.indexOf(prefix);
    return idx >= 0 ? part.substring(idx + prefix.length()).trim() : part.trim();
}
```

### 4.3 关联逻辑

```java
String partAddr = extractCrtPartAddr(desc);
if (partAddr == null) return null;

// 查父设备
DeviceInfo parentDevice = deviceApi.getDeviceByDn(parentAddr);
if (parentDevice == null || parentDevice.getId() == null) return null;

// 用 parentId + dn 查子设备
DeviceInfo child = deviceApi.getDeviceByParentIdAndDn(parentDevice.getId(), partAddr);
if (child != null) {
    return child.getDeviceName();  // 命中
}

// 未命中：记录日志，不自动注册
log.warn("CRT 子设备未找到: parent={}, partAddr={}, 请先通过Excel导入点位", parentAddr, partAddr);
return null;
```

---

## 5. Excel 导入调整

### 5.1 导入规则

| Excel 列 | 用途 |
|---------|------|
| **主机号** | 2位十六进制，如 `11` |
| **回路号** | 2位十六进制，如 `4A` |
| **地址号** | 3位十六进制，如 `1CD` |
| **设备名称** | 点位名称，如 "1层电梯间烟感" |

### 5.2 dn 生成

```java
String hostNo = row.getHostNo();        // "11"
String circuitNo = row.getCircuitNo();  // "4A"
String addressNo = row.getAddressNo();  // "1CD"
String partAddr = (hostNo + circuitNo + addressNo).toLowerCase();  // "114a1cd"

// 子设备 dn = partAddr
String dn = partAddr;
```

### 5.3 注册参数

```java
RegisterDevice req = RegisterDevice.builder()
    .productKey(parentPk + "sub")
    .deviceName(dn)           // 7位部件地址
    .name(row.getDeviceName()) // 点位名称（设备展示名）
    .parentId(parentId)
    .build();
```

---

## 6. 物模型调整

### 6.1 精简内容

**去掉以下3个属性**：
- `point_host_no`
- `point_circuit_no`
- `point_address_no`

**原因**：dn 本身就是部件地址（7位十六进制），已经包含了主机号、回路号、地址号的完整信息，无需再拆分为三个独立属性。

### 6.2 保留属性

国标基础属性（状态、类型、位置等）保持不变。

---

## 7. 改动文件清单

| 文件 | 改动内容 |
|------|---------|
| `FourfaithGbtTcpComponent.java` | dn 生成规则简化、CRT 关联逻辑改为 parentId+dn 查询、主机号提取补全、未命中时注释自动注册 |
| `DeviceApi.java` | 新增 `getDeviceByParentIdAndDn` 接口 |
| `DeviceApiImpl.java` | 实现 `getDeviceByParentIdAndDn` |
| `DeviceInfoService.java` | 新增 Service 层方法 |
| `DeviceInfoServiceImpl.java` | 实现 Service 层方法 |
| `EiotDeviceInfoMapper.java` | 新增 `selectByParentIdAndDn` |
| `fourfaith_component_thing_model.json` | 去掉 `point_host_no` / `point_circuit_no` / `point_address_no` |
| Excel 导入相关类 | dn 生成改为7位部件地址 |

---

## 8. 数据流

### 8.1 CRT 报警关联流程

```
设备上传 Type 0x0A (CRT)
  └── FourfaithGbtDataReader → FourfaithGbtDataParser.parseCrt()
      └── 解析 crt_event_json
          └── 提取字段2（设备位置字符串）
              └── extractCrtPartAddr() → 部件地址 partAddr
                  └── FourfaithGbtTcpComponent.handleCrtAlarm()
                      ├── deviceApi.getDeviceByDn(parentAddr) → parentId
                      ├── deviceApi.getDeviceByParentIdAndDn(parentId, partAddr)
                      │     ├── 命中 → 给子设备上报属性 / 触发 crt_alarm_event
                      │     └── 未命中 → warn 日志（子设备未预注册）
                      └── 上报父设备属性 crt_event_json
```

### 8.2 Excel 导入流程

```
用户上传点位表 Excel
  └── 解析每行数据
      ├── 提取 hostNo / circuitNo / addressNo / deviceName
      ├── 拼接部件地址: partAddr = hostNo + circuitNo + addressNo
      ├── dn = partAddr
      └── 注册子设备
            ├── productKey = parentPk + "sub"
            ├── deviceName = dn
            ├── name = deviceName（点位名称）
            └── parentId = 父设备id
```

---

## 9. 错误处理

| 场景 | 处理策略 |
|------|---------|
| CRT 设备位置字符串格式异常 | 记录 warn 日志，跳过该次关联 |
| CRT 子设备未找到 | 记录 warn 日志（含 parentAddr 和 partAddr），不自动注册 |
| 父设备未找到 | 记录 warn 日志，跳过该次关联 |
| Excel 导入 dn 重复 | 记录 warn 日志，跳过重复行 |

---

## 10. 测试要点

| 测试项 | 方法 |
|--------|------|
| 部件地址拼接 | 验证 hostNo(2) + circuitNo(2) + addressNo(3) = 7位小写十六进制 |
| CRT 主机号提取 | 验证 "机器11" → "11" |
| parentId + dn 查询 | Mock 父设备 + 子设备，验证命中/未命中 |
| Excel 导入 dn 生成 | 验证三列拼接结果与预期一致 |
| 子设备注册 | 验证 deviceName = 部件地址，name = 点位名称 |
