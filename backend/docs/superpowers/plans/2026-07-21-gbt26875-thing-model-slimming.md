# GBT26875 物模型瘦身 + 只写变化值 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 GBT26875 物模型从 60 属性瘦身到 29 属性，消除旧值重复写入和 PRT/CRT 双重存储。

**Architecture:** 修改物模型 JSON 移除 telemetry_* 和 diag_* 属性；修改数据上报层让 TLD 3 不再存储、TLD 9/10 纯事件化；修改 TDengine 持久化层只写实际收到的属性列。

**Tech Stack:** Java 17, Spring Boot, TDengine, Maven

**Spec:** `docs/superpowers/specs/2026-07-21-gbt26875-thing-model-slimming-design.md`

---

## 文件结构

| # | 文件路径 | 职责 | 改动类型 |
|---|---------|------|---------|
| 1 | `doc/gbt26875/gbt26875_thing_model.json` | GBT26875 物模型定义 | 修改：移除 31 个属性 |
| 2 | `module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/.../ThingModelPropertyMapper.java` | 协议数据→物模型标识符映射 | 修改：移除 2 个方法 + 1 个映射表 |
| 3 | `module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/.../FourfaithGbtDataReporter.java` | TLD 数据上报处理器 | 修改：TLD 3 空操作、TLD 9/10 纯事件、清理死代码 |
| 4 | `module-eiot/module-eiot-temporal/iot-temporal-serviceimpl-td/.../DevicePropertyDataImpl.java` | TDengine 属性持久化 | 修改：addProperties 不合并旧值 + 查询加 IS NOT NULL |

---

### Task 1: 物模型 JSON 瘦身（移除 telemetry_* 和 diag_*）

**Files:**
- Modify: `doc/gbt26875/gbt26875_thing_model.json`

- [ ] **Step 1: 移除 telemetry_* 属性（19 个）**

从 `gbt26875_thing_model.json` 的 `properties` 数组中，删除以下 19 个条目（第 29-48 行）：
- `telemetry_temperature`、`telemetry_pressure`、`telemetry_humidity`、`telemetry_gas_concentration`
- `telemetry_voltage`、`telemetry_current`、`telemetry_wind_speed`、`telemetry_signal`、`telemetry_battery`
- `telemetry_residual_current`、`telemetry_water_level`
- `telemetry_ua_voltage`、`telemetry_ub_voltage`、`telemetry_uc_voltage`
- `telemetry_ia_current`、`telemetry_ib_current`、`telemetry_ic_current`
- `telemetry_sample_time`、`telemetry_total_energy`

删除后 properties 数组只剩 alarm_*（25 个）+ info_*（4 个）= 29 个。

- [ ] **Step 2: 移除 diag_* 属性（12 个）**

从 `gbt26875_thing_model.json` 的 `properties` 数组中，删除以下 12 个条目（原第 54-65 行）：
- `diag_prt_channel`、`diag_prt_host_no`、`diag_prt_alarm_type`、`diag_prt_device_name`、`diag_prt_device_location`、`diag_prt_alarm_info`、`diag_prt_time`
- `diag_crt_channel`、`diag_crt_host_no`、`diag_crt_alarm_type`、`diag_crt_event_json`、`diag_crt_time`

- [ ] **Step 3: 验证 JSON 格式正确**

确认 properties 数组恰好包含 29 个条目：
- alarm_* 共 25 个（alarm_fire 到 alarm_device_line）
- info_* 共 4 个（info_firmware_ver_major 到 info_system_time）
- events 和 services 数组不变

最终 JSON 验证：
```bash
cd d:\project\anwei-remote-monitor\anwei-fire-remote-monitor
python -c "import json; d=json.load(open('doc/gbt26875/gbt26875_thing_model.json')); print(f'properties: {len(d[\"properties\"])}'); [print(f'  {p[\"identifier\"]}') for p in d['properties']]"
```

Expected output: `properties: 29`，所有标识符以 `alarm_` 或 `info_` 开头。

- [ ] **Step 4: Commit**

```bash
git add doc/gbt26875/gbt26875_thing_model.json
git commit -m "feat(gbt26875): 物模型瘦身 - 移除 telemetry_*(19) 和 diag_*(12) 属性，保留 29 个核心属性"
```

---

### Task 2: 清理 ThingModelPropertyMapper 死代码

**Files:**
- Modify: `module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/ff/tcp/service/ThingModelPropertyMapper.java`

- [ ] **Step 1: 移除 ANALOG_TELEMETRY_MAPPING 映射表**

删除 `ThingModelPropertyMapper.java` 第 39-59 行：
```java
// ==================== TLD 3 模拟量 typeValue → telemetry_* ====================
private static final Map<Integer, String> ANALOG_TELEMETRY_MAPPING = new HashMap<>();
static {
    ANALOG_TELEMETRY_MAPPING.put(AnalogTypeDef.TEMPERATURE.getTypeValue(),    "telemetry_temperature");
    // ... 所有 17 行映射 ...
    ANALOG_TELEMETRY_MAPPING.put(AnalogTypeDef.IC.getTypeValue(),             "telemetry_ic_current");
}
```

- [ ] **Step 2: 移除 mapAnalogValue 方法**

删除 `ThingModelPropertyMapper.java` 第 104-121 行：
```java
/**
 * TLD 3 模拟量 → telemetry_* 属性
 */
public static Map<String, Object> mapAnalogValue(Map<String, Object> entry) {
    // ... 方法体 ...
}
```

- [ ] **Step 3: 移除 mapDiagnostic 方法**

删除 `ThingModelPropertyMapper.java` 第 156-166 行：
```java
/**
 * TLD 9/10 PRT/CRT → diag_* 属性
 */
public static Map<String, Object> mapDiagnostic(String prefix, Map<String, Object> entry) {
    // ... 方法体 ...
}
```

- [ ] **Step 4: 移除 AnalogTypeDef 导入**

删除第 3 行的 import（因为 mapAnalogValue 移除后不再需要）：
```java
import parser.com.enjoyiot.eiot.component.tcp.gbt.ff.AnalogTypeDef;
```

- [ ] **Step 5: 验证编译通过**

```bash
cd d:\project\anwei-remote-monitor\anwei-fire-remote-monitor
mvn compile -pl module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp -am -q
```

Expected: `BUILD SUCCESS`（注意：FourfaithGbtDataReporter 中仍调用 mapAnalogValue 和 mapDiagnostic，会在 Task 3 修复，此处如果编译失败属预期）

> **注意**：Task 2 和 Task 3 应连续执行，Task 2 移除 Mapper 方法后 Task 3 同步移除调用方，最终一起编译通过。

- [ ] **Step 6: Commit**

```bash
git add module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/ff/tcp/service/ThingModelPropertyMapper.java
git commit -m "refactor(gbt26875): 移除 ThingModelPropertyMapper 中 mapAnalogValue 和 mapDiagnostic 方法及 ANALOG_TELEMETRY_MAPPING"
```

---

### Task 3: 修改 FourfaithGbtDataReporter（TLD 3/9/10 + 清理死代码）

**Files:**
- Modify: `module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/ff/tcp/service/FourfaithGbtDataReporter.java`

- [ ] **Step 1: TLD 3 handleAnalogValue 改为空操作**

将 `handleAnalogValue` 方法（第 200-205 行）替换为：
```java
/**
 * TLD 3 模拟量 → 暂不存储（遥测数据后续可扩展独立窄表）
 */
private void handleAnalogValue(List<Map<String, Object>> entries, String deviceAddr, String pk) {
    log.debug("[四信-GBT26875] TLD 3 模拟量数据暂不存储: 设备={}, 记录数={}", deviceAddr, entries.size());
}
```

- [ ] **Step 2: TLD 9 handlePrtUpload 移除 reportProperty，仅保留 reportEvent**

将 `handlePrtUpload` 方法（第 256-273 行）替换为：
```java
/**
 * TLD 9 PRT → 仅 reportEvent（纯事件存储，不再作为属性上报）
 */
private void handlePrtUpload(List<Map<String, Object>> entries, String deviceAddr, String pk) {
    for (Map<String, Object> entry : entries) {
        int alarmType = (Integer) entry.getOrDefault("prt_alarm_type", 0);
        if (alarmType != 0) {
            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("alarm_type", alarmType);
            eventParams.put("device_name", entry.get("prt_device_name"));
            eventParams.put("location", entry.get("prt_device_location"));
            eventParams.put("info", entry.get("prt_alarm_info"));
            eventParams.put("event_time", entry.get("prt_time"));
            eventParams.put("source", "PRT");
            reportEvent(pk, deviceAddr, "prt_alarm_event", eventParams);
        }
    }
}
```

- [ ] **Step 3: TLD 10 handleCrtUpload 移除 reportProperty，仅保留 reportEvent**

将 `handleCrtUpload` 方法（第 278-295 行）替换为：
```java
/**
 * TLD 10 CRT → 仅 reportEvent（纯事件存储，不再作为属性上报）
 */
private void handleCrtUpload(List<Map<String, Object>> entries, String deviceAddr, String pk) {
    for (Map<String, Object> entry : entries) {
        int crtAlarmType = (Integer) entry.getOrDefault("crt_alarm_type", 0);
        if (crtAlarmType != 0) {
            String eventName = mapCrtAlarmTypeToEvent(crtAlarmType);
            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("alarm_type", crtAlarmType);
            eventParams.put("alarm_type_desc", getCrtAlarmTypeDesc(crtAlarmType));
            eventParams.put("event_json", parseEventJsonToList((String) entry.get("crt_event_json")));
            eventParams.put("event_time", entry.get("crt_time"));
            eventParams.put("source", "CRT");
            reportEvent(pk, deviceAddr, eventName, eventParams);
        }
    }
}
```

- [ ] **Step 4: 移除 reportThrottled 方法和遥测节流相关字段**

删除以下内容（因为 handleAnalogValue 不再调用 reportThrottled，成为死代码）：

1. 第 37-41 行：`telemetryThrottleMap` 字段和 `TELEMETRY_THROTTLE_MS` 常量
```java
/** 遥测节流：标识符 → 上次上报时间戳（毫秒） */
private final ConcurrentHashMap<String, Long> telemetryThrottleMap = new ConcurrentHashMap<>();

/** 遥测节流窗口（毫秒），默认 5 分钟 */
private static final long TELEMETRY_THROTTLE_MS = 5 * 60 * 1000L;
```

2. 第 427-449 行：`reportThrottled` 方法整体
```java
/**
 * 带节流的属性上报：同一标识符 5 分钟内不重复上报
 */
private void reportThrottled(String pk, String dn, Map<String, Object> params) {
    // ... 方法体 ...
}
```

- [ ] **Step 5: 验证编译通过**

```bash
cd d:\project\anwei-remote-monitor\anwei-fire-remote-monitor
mvn compile -pl module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp -am -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/ff/tcp/service/FourfaithGbtDataReporter.java
git commit -m "feat(gbt26875): TLD 3 不再存储遥测属性; TLD 9/10 改为纯事件存储; 移除遥测节流死代码"
```

---

### Task 4: 修改 DevicePropertyDataImpl（只写变化值 + IS NOT NULL 过滤）

**Files:**
- Modify: `module-eiot/module-eiot-temporal/iot-temporal-serviceimpl-td/src/main/java/com/enjoyiot/eiot/temporal/td/service/DevicePropertyDataImpl.java`

- [ ] **Step 1: 修改 addProperties 方法，不合并旧属性**

将 `addProperties` 方法（第 84-122 行）替换为：
```java
@Override
public void addProperties(Long deviceId, Map<String, DevicePropertyCache> properties, long time) {
    DeviceInfo device = deviceApi.getDeviceInfoFromCache(deviceId);

    if (device == null) {
        return;
    }

    // 直接使用传入的属性，不合并旧属性缓存（只写变化值）
    StringBuilder sbFieldNames = new StringBuilder();
    StringBuilder sbFieldPlaces = new StringBuilder();
    List<Object> args = new ArrayList<>();
    args.add(time);

    // 组织 SQL：只写实际收到的属性列
    properties.forEach((key, val) -> {
        if (key.contains(".")) {
            key = "`" + key + "`";
        }
        sbFieldNames.append(key)
                .append(",");
        sbFieldPlaces.append("?,");
        args.add(val.getValue());
    });
    sbFieldNames.deleteCharAt(sbFieldNames.length() - 1);
    sbFieldPlaces.deleteCharAt(sbFieldPlaces.length() - 1);

    String sql = String.format("INSERT INTO %s (time,%s) USING %s TAGS ('%s') VALUES (?,%s);",
            Constants.getDevicePropertyTableName(deviceId),
            sbFieldNames,
            Constants.getProductPropertySTableName(device.getProductKey()),
            deviceId,
            sbFieldPlaces);

    tdTemplate.update(sql, args.toArray());
}
```

**关键变更**：移除了 `deviceApi.getPropertiesFromCache(deviceId)` 和 `oldProperties.putAll(properties)` 两行，直接用 `properties` 参数写入。

- [ ] **Step 2: 修改 findDevicePropertyHistory 方法，加 IS NOT NULL 过滤**

将 `findDevicePropertyHistory` 方法中的 SQL 查询（第 67-71 行）修改为：
```java
List<TbDeviceProperty> deviceProperties = tdTemplate.query(String.format(
                "select time,%s as `value`,device_id from %s where device_id=? and time>=? and time<=? " +
                        "and %s is not null " +
                        "order by time asc" ,
                filedName.toLowerCase(), tbName, filedName.toLowerCase()),
        new BeanPropertyRowMapper<>(TbDeviceProperty.class),
        deviceId, start, end
);
```

**关键变更**：在 `WHERE` 条件中增加 `AND {field} IS NOT NULL`，只返回有值的行（即实际发生变化的记录），过滤掉未上报的属性行。

- [ ] **Step 3: 验证编译通过**

```bash
cd d:\project\anwei-remote-monitor\anwei-fire-remote-monitor
mvn compile -pl module-eiot/module-eiot-temporal/iot-temporal-serviceimpl-td -am -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add module-eiot/module-eiot-temporal/iot-temporal-serviceimpl-td/src/main/java/com/enjoyiot/eiot/temporal/td/service/DevicePropertyDataImpl.java
git commit -m "feat(temporal): addProperties 只写变化值不合并旧属性; findDevicePropertyHistory 加 IS NOT NULL 过滤"
```

---

### Task 5: 全量编译验证

**Files:** 无新增改动，仅验证

- [ ] **Step 1: 全量编译**

```bash
cd d:\project\anwei-remote-monitor\anwei-fire-remote-monitor
mvn compile -q
```

Expected: `BUILD SUCCESS`

如有编译错误，根据错误信息定位并修复。可能的风险点：
- 其他模块引用了 `ThingModelPropertyMapper.mapAnalogValue()` 或 `mapDiagnostic()`
- 其他模块引用了 `telemetryThrottleMap` 或 `reportThrottled()`

- [ ] **Step 2: 检查是否有其他引用**

如果编译失败，在项目中搜索以下方法引用：
- `mapAnalogValue` → 如有其他调用方，同步移除
- `mapDiagnostic` → 如有其他调用方，同步移除
- `reportThrottled` → 如有其他调用方，保留或同步清理

- [ ] **Step 3: Commit（如有修复）**

```bash
git add -A
git commit -m "fix: 修复物模型瘦身改造引起的编译错误"
```

---

## 自审清单

### 1. Spec 覆盖度

| Spec 需求 | Task | 状态 |
|----------|------|------|
| 移除 telemetry_* (19个) 属性 | Task 1 | ✅ |
| 移除 diag_* (12个) 属性 | Task 1 | ✅ |
| TLD 3 不再上报属性 | Task 3 | ✅ |
| TLD 9/10 仅 reportEvent | Task 3 | ✅ |
| addProperties 不合并旧属性 | Task 4 | ✅ |
| findDevicePropertyHistory 加 IS NOT NULL | Task 4 | ✅ |
| 移除 mapAnalogValue 方法 | Task 2 | ✅ |
| 移除 mapDiagnostic 方法 | Task 2 | ✅ |
| 遥测节流死代码清理 | Task 3 | ✅ |

### 2. 无占位符

所有 Task 的每个 Step 都包含具体代码或命令，无 TBD/TODO。

### 3. 类型一致性

- `addProperties` 签名 `void addProperties(Long, Map<String, DevicePropertyCache>, long)` → 未改变
- `findDevicePropertyHistory` 签名 → 未改变
- `reportProperty` / `reportEvent` → 未改变
- 物模型属性 identifier 与 Mapper 中 SYSTEM_STATUS_MAPPING / DEVICE_STATUS_MAPPING / DEVICE_INFO_MAPPING 保持一致
