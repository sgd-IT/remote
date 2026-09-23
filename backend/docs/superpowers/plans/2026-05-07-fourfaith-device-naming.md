# 四信子设备 dn 命名及 CRT 关联调整实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将四信子设备 dn 从国标格式改为7位部件地址格式，并调整 CRT 报警关联逻辑为 parentId + dn 联合查询。

**Architecture:** 在 DeviceApi 层新增 `getDeviceByParentIdAndDn` 接口，四信组件中简化 dn 生成、补全 CRT 主机号提取、改为 parentId + dn 查询子设备，去掉物模型中冗余的三个地址属性。

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, EasyExcel

---

## File Structure

| 文件 | 职责 |
|------|------|
| `EiotDeviceInfoMapper.java` | 新增 `selectByParentIdAndDn` 数据库查询 |
| `DeviceInfoService.java` / `Impl` | 新增 Service 层方法 |
| `DeviceApi.java` / `Impl` | 新增 `getDeviceByParentIdAndDn` API 接口 |
| `FourfaithGbtTcpComponent.java` | dn 生成简化、CRT 解析补全主机号、关联逻辑改为 parentId+dn 查询、注释自动注册 |
| `fourfaith_component_thing_model.json` | 去掉 `host_no` / `circuit_no` / `address_no` 三个属性 |

---

### Task 1: Mapper 层新增 selectByParentIdAndDn

**Files:**
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/deviceinfo/EiotDeviceInfoMapper.java`

- [ ] **Step 1: 新增 selectByParentIdAndDn 方法**

在 `EiotDeviceInfoMapper` 接口中，在 `selectByDn` 方法下方新增：

```java
/**
 * 根据父设备ID和设备名称(dn)查询子设备
 *
 * @param parentId 父设备ID
 * @param dn 设备名称
 * @return 子设备信息，未找到返回 null
 */
default EiotDeviceInfoDO selectByParentIdAndDn(Long parentId, String dn) {
    return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EiotDeviceInfoDO>()
            .eq(EiotDeviceInfoDO::getParentId, parentId)
            .eq(EiotDeviceInfoDO::getDn, dn)
            .last("LIMIT 1"));
}
```

- [ ] **Step 2: Commit**

```bash
git add module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/deviceinfo/EiotDeviceInfoMapper.java
git commit -m "feat: add selectByParentIdAndDn to EiotDeviceInfoMapper"
```

---

### Task 2: Service 层新增 getDeviceByParentIdAndDn

**Files:**
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInfoService.java`
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInfoServiceImpl.java`

- [ ] **Step 1: 在 DeviceInfoService 接口新增方法**

```java
/**
 * 根据父设备ID和设备名称查询子设备
 *
 * @param parentId 父设备ID
 * @param dn 设备名称
 * @return 子设备信息
 */
DeviceInfo getDeviceByParentIdAndDn(Long parentId, String dn);
```

- [ ] **Step 2: 在 DeviceInfoServiceImpl 实现方法**

在 `DeviceInfoServiceImpl` 中，在 `getDeviceByDn` 方法下方新增：

```java
@Override
@TenantIgnore
public DeviceInfo getDeviceByParentIdAndDn(Long parentId, String dn) {
    EiotDeviceInfoDO deviceDO = deviceInfoMapper.selectByParentIdAndDn(parentId, dn);
    return DeviceInfoConvert.INSTANCE.convert(deviceDO);
}
```

- [ ] **Step 3: Commit**

```bash
git add module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInfoService.java
git add module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInfoServiceImpl.java
git commit -m "feat: add getDeviceByParentIdAndDn to DeviceInfoService"
```

---

### Task 3: DeviceApi 新增 getDeviceByParentIdAndDn

**Files:**
- Modify: `module-eiot/module-eiot-api/src/main/java/com/enjoyiot/module/eiot/api/device/DeviceApi.java`
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/api/device/DeviceApiImpl.java`

- [ ] **Step 1: 在 DeviceApi 接口新增方法**

在 `getDeviceByDn` 方法下方新增：

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

- [ ] **Step 2: 在 DeviceApiImpl 实现方法**

在 `getDeviceByDn` 方法下方新增：

```java
@Override
public DeviceInfo getDeviceByParentIdAndDn(Long parentId, String dn) {
    return TenantUtils.executeIgnoreResult(() -> deviceInfoService.getDeviceByParentIdAndDn(parentId, dn));
}
```

- [ ] **Step 3: Commit**

```bash
git add module-eiot/module-eiot-api/src/main/java/com/enjoyiot/module/eiot/api/device/DeviceApi.java
git add module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/api/device/DeviceApiImpl.java
git commit -m "feat: add getDeviceByParentIdAndDn to DeviceApi"
```

---

### Task 4: 修改 FourfaithGbtTcpComponent - dn 生成规则

**Files:**
- Modify: `module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithGbtTcpComponent.java`

- [ ] **Step 1: 修改 generateComponentDeviceName 方法**

将方法签名和实现改为只接收部件地址一个参数：

```java
/**
 * 生成部件子设备 deviceName
 * 规则: 部件地址(7位十六进制)
 */
static String generateComponentDeviceName(String partAddr) {
    return partAddr.toLowerCase();
}
```

- [ ] **Step 2: 修改 ensureComponentDeviceRegistered 方法签名和调用**

修改 `ensureComponentDeviceRegistered` 方法，改为接收 `partAddr` 而不是 sysType/sysAddr/compType/compAddr：

```java
/**
 * 确保部件子设备已注册
 *
 * @param parentAddr 父设备地址(hex)
 * @param parentPk   父设备 productKey
 * @param partAddr   部件地址(7位十六进制)
 * @return 部件子设备 productKey（= parentPk + "sub"）
 */
String ensureComponentDeviceRegistered(String parentAddr, String parentPk, String partAddr) {
    String compDn = generateComponentDeviceName(partAddr);

    // 已缓存
    String cachedPk = compDnToPk.get(compDn);
    if (cachedPk != null) {
        return cachedPk;
    }

    // 查询是否已注册
    DeviceInfo existing = deviceApi.getDeviceByDn(compDn);
    if (existing != null) {
        String pk = existing.getProductKey();
        compDnToPk.put(compDn, pk);
        return pk;
    }

    // 自动注册
    String compPk = parentPk + "sub";
    DeviceInfo parentDevice = deviceApi.getDeviceByDn(parentAddr);
    Long parentId = parentDevice != null ? parentDevice.getId() : null;

    RegisterDevice req = RegisterDevice.builder()
        .productKey(compPk)
        .deviceName(compDn)
        .parentId(parentId)
        .build();
    DeviceInfo registered = deviceApi.registerDevice(req);
    if (registered != null) {
        compDnToPk.put(compDn, compPk);
        log.info("[GBT26875] 部件子设备注册成功: dn={}, pk={}, parent={}", compDn, compPk, parentAddr);
        return compPk;
    }
    return null;
}
```

- [ ] **Step 3: 修改所有调用 ensureComponentDeviceRegistered 和 generateComponentDeviceName 的地方**

搜索当前文件中所有调用 `generateComponentDeviceName` 和 `ensureComponentDeviceRegistered` 的地方，改为新签名。

当前调用点：

1. 行 390-394（国标 Type2 部件状态上报）：
```java
// 修改前
String compPk = ensureComponentDeviceRegistered(deviceAddr, pk, sysType, sysAddr, compType, compAddr);
String compDn = generateComponentDeviceName(deviceAddr, sysType, sysAddr, compType, compAddr);

// 修改后 - 国标 Type2 场景需要单独评估，此处先注释或标记
```

> **注意**：国标 Type2 部件状态上报中使用的是 `sysType/sysAddr/compType/compAddr`，这与四信7位部件地址不直接对应。如果四信设备也会上报国标 Type2 数据，需要建立映射关系。当前建议：先保留 Type2 场景的原有逻辑，仅修改 CRT 关联场景。

2. 行 607-608（CRT 自动创建子设备）：
```java
// 修改前
String compDn = generateComponentDeviceName(parentAddr, 0, 0, 1, compAddr);
String compPk = ensureComponentDeviceRegistered(parentAddr, parentPk, 0, 0, 1, compAddr);

// 修改后（此处在 Task 5 中会注释掉自动注册逻辑）
```

- [ ] **Step 4: Commit**

```bash
git add module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithGbtTcpComponent.java
git commit -m "refactor: simplify component deviceName to 7-digit partAddr"
```

---

### Task 5: 修改 FourfaithGbtTcpComponent - CRT 解析和关联逻辑

**Files:**
- Modify: `module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithGbtTcpComponent.java`

- [ ] **Step 1: 新增 extractCrtPartAddr 方法**

在 `extractValue` 方法下方新增：

```java
/**
 * 从 CRT 设备位置字符串中提取部件地址
 * 格式: &&QN01,机器11,回路3A,部件2CD
 *
 * @param desc 设备位置字符串
 * @return 7位部件地址(小写十六进制)，解析失败返回 null
 */
private String extractCrtPartAddr(String desc) {
    if (desc == null || desc.isEmpty()) {
        return null;
    }
    String[] parts = desc.split(",");
    if (parts.length < 4) {
        return null;
    }

    String hostNo = extractValue(parts[1], "机器");
    String circuitNo = extractValue(parts[2], "回路");
    String addressNo = extractValue(parts[3], "部件");

    return (hostNo + circuitNo + addressNo).toLowerCase();
}
```

- [ ] **Step 2: 修改 findComponentByCrtDesc 方法**

找到当前的 CRT 关联方法（可能在 `handleCrtAlarm` 或类似方法中），改为使用 `parentId + dn` 查询。

当前逻辑大致在 590-625 行附近。修改为：

```java
private String findComponentByCrtDesc(String parentAddr, String desc) {
    try {
        String partAddr = extractCrtPartAddr(desc);
        if (partAddr == null) {
            log.warn("CRT 设备位置字符串解析失败: {}", desc);
            return null;
        }

        // 查父设备获取 parentId
        DeviceInfo parentDevice = deviceApi.getDeviceByDn(parentAddr);
        if (parentDevice == null || parentDevice.getId() == null) {
            log.warn("CRT 父设备未找到: {}", parentAddr);
            return null;
        }

        // 用 parentId + dn 查子设备
        DeviceInfo child = deviceApi.getDeviceByParentIdAndDn(parentDevice.getId(), partAddr);
        if (child != null) {
            return child.getDeviceName();
        }

        // 未命中：记录日志，不自动注册（依赖 Excel 预导入）
        log.warn("CRT 子设备未找到: parent={}, partAddr={}, 请先通过Excel导入点位", parentAddr, partAddr);
        return null;

    } catch (Exception e) {
        log.warn("CRT 部件关联失败: {}", e.getMessage());
        return null;
    }
}
```

- [ ] **Step 3: 注释掉 CRT 未命中时的自动注册逻辑**

如果当前代码在 `findComponentByCrtDesc` 或相关方法中有自动创建子设备的逻辑，将其注释掉或删除。

当前自动注册逻辑大致在 605-619 行附近：
```java
// 未命中，自动创建子设备
// int compAddr = Integer.parseInt(addressNo, 16);
// String compDn = generateComponentDeviceName(parentAddr, 0, 0, 1, compAddr);
// String compPk = ensureComponentDeviceRegistered(parentAddr, parentPk, 0, 0, 1, compAddr);
// ...
```

将上述自动注册代码块注释掉。

- [ ] **Step 4: 删除 point_circuit_no / point_address_no 属性上报**

如果当前代码中有向子设备上报 `point_circuit_no` 和 `point_address_no` 属性的逻辑，删除或注释掉。

当前逻辑在 611-618 行附近：
```java
// 删除以下代码
// Map<String, Object> pointProps = new LinkedHashMap<>();
// pointProps.put("point_circuit_no", circuitNo);
// pointProps.put("point_address_no", addressNo);
// report(PropertyReport.builder()...);
```

- [ ] **Step 5: Commit**

```bash
git add module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithGbtTcpComponent.java
git commit -m "refactor: CRT关联改为parentId+dn查询，补全主机号提取，禁用自动注册"
```

---

### Task 6: 修改物模型 - 去掉三个地址属性

**Files:**
- Modify: `doc/fourfaith/fourfaith_component_thing_model.json`

- [ ] **Step 1: 删除 host_no / circuit_no / address_no 属性**

从 `properties` 数组中删除以下三个对象：

```json
{ "identifier": "host_no", "name": "主机号", ... }
{ "identifier": "circuit_no", "name": "回路号", ... }
{ "identifier": "address_no", "name": "地址号", ... }
```

删除后的 `properties` 数组保持其他属性不变。

- [ ] **Step 2: Commit**

```bash
git add doc/fourfaith/fourfaith_component_thing_model.json
git commit -m "chore: remove host_no/circuit_no/address_no from component thing model"
```

---

### Task 7: 修改 Excel 导入 dn 生成规则（如已存在导入功能）

**Files:**
- 需根据实际情况确定

> **说明**：如果项目中已存在四信点位表 Excel 导入功能，需要修改子设备 dn 生成规则为 `hostNo + circuitNo + addressNo` 拼接。如果导入功能尚未实现，则此 Task 跳过。

- [ ] **Step 1: 定位 Excel 导入相关类**

搜索包含 `fourfaith`、`点位表`、`设备导入` 等关键字的 Service/Controller/VO 类。

- [ ] **Step 2: 修改 dn 生成逻辑**

将子设备注册时的 dn 从其他格式改为：

```java
String partAddr = (hostNo + circuitNo + addressNo).toLowerCase();
RegisterDevice req = RegisterDevice.builder()
    .productKey(parentPk + "sub")
    .deviceName(partAddr)  // dn = 7位部件地址
    .name(deviceName)       // 点位名称（展示名）
    .parentId(parentId)
    .build();
```

- [ ] **Step 3: Commit**

```bash
git add <修改的Excel导入文件>
git commit -m "refactor: Excel导入子设备dn改为7位部件地址"
```

---

## Self-Review Checklist

**1. Spec coverage:**
- [x] 子设备 dn 规则变更 → Task 4
- [x] parentId + dn 查询 → Task 1-3
- [x] CRT 主机号提取 → Task 5 Step 1
- [x] CRT 关联逻辑改为 parentId+dn → Task 5 Step 2
- [x] CRT 未命中不自动注册 → Task 5 Step 3
- [x] 物模型去掉三个属性 → Task 6
- [x] Excel 导入 dn 调整 → Task 7

**2. Placeholder scan:**
- [x] 无 TBD/TODO
- [x] 无 "implement later"
- [x] 无 "appropriate error handling" 等模糊描述
- [x] Task 7 明确标注了条件判断（如已存在导入功能）

**3. Type consistency:**
- [x] `getDeviceByParentIdAndDn` 签名在 Task 1/2/3 中一致
- [x] `generateComponentDeviceName` 签名在 Task 4 中统一为 `String partAddr`
- [x] `extractCrtPartAddr` 返回类型统一为 `String`
