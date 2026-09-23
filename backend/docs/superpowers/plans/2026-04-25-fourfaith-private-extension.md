# 四信私有协议扩展 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在四信专用 TCP 模块中实现心跳包兼容、PRT/CRT 私有协议解析、远程控制下行（含异步结果回传）、点位表子设备属性匹配。

**Architecture:** 保持单模块内分层：Reader 负责帧解析（新增心跳嗅探）、Parser 负责应用数据解析（新增 Type 0x09/0x0A/0x85）、Component 负责类型路由与异步请求管理（PendingRequestManager）、RemoteControlClient 负责四信 HTTP API 对接。

**Tech Stack:** Java 17, Spring Boot, Vert.x (TCP), Hutool, Maven

---

## File Structure

| 文件 | 职责 | 操作 |
|------|------|------|
| `FourfaithGbtDataReader.java` | TCP 字节流帧解析器（新增心跳嗅探） | 修改 |
| `FourfaithGbtDataPackage.java` | 数据包模型（支持心跳包简化格式） | 修改 |
| `FourfaithGbtConstants.java` | 协议常量（新增私有类型、心跳包常量） | 修改 |
| `FourfaithGbtDataParser.java` | 应用数据单元解析（新增 PRT/CRT/TLD-133） | 修改 |
| `FourfaithRemoteResponseParser.java` | TLD-133 远程应答解析器 | 新增 |
| `FourfaithRemoteControlClient.java` | 四信 HTTP API 客户端 | 新增 |
| `PendingRequest.java` | 异步请求上下文数据模型 | 新增 |
| `PendingRequestManager.java` | 待响应请求缓存管理器 | 新增 |
| `FourfaithGbtTcpComponent.java` | 核心组件（类型路由 + 异步服务调用 + 心跳处理） | 修改 |
| `FourfaithGbtTcpConfig.java` | TCP 配置模型（新增 API/型号配置） | 修改 |
| `FourfaithGbtDataEncoder.java` | 数据编码器（新增 Type 0x81 组装） | 修改 |
| `gbt26875_thing_model.json` | 父设备物模型（新增 10 属性 + 2 事件 + 1 服务） | 修改 |
| `fourfaith_component_thing_model.json` | 四信部件子设备物模型（独立，新增 5 点位属性） | 新增 |

---

## Task 1: FourfaithGbtConstants — 新增私有协议常量

**Files:**
- Modify: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/parser/FourfaithGbtConstants.java`
- Test: `src/test/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/parser/FourfaithGbtConstantsTest.java`

- [ ] **Step 1: 新增私有类型常量**

在现有常量类中追加：

```java
// ===== 四信私有类型标志 =====
public static final int TYPE_PRT = 0x09;           // PRT 打印信息
public static final int TYPE_CRT = 0x0A;           // CRT 图形显示信息
public static final int TYPE_REMOTE_CONTROL = 0x81; // 远程控制下发 (TLD-129)
public static final int TYPE_REMOTE_RESPONSE = 0x85; // 远程应答 (TLD-133)

// ===== 心跳包常量 =====
public static final int HEARTBEAT_PACKET_SIZE = 12;  // 心跳包总字节数
public static final int HEARTBEAT_BODY_SIZE = 10;    // @@ 之后字节数
public static final byte HEARTBEAT_END_BYTE = 0x23;  // '#' 结束符

// ===== 远程控制常量 =====
public static final int REMOTE_CONTROL_TYPE_RESET = 10;
public static final int REMOTE_CONTROL_TYPE_MUTE = 20;
public static final int REMOTE_CONTROL_TYPE_SWITCH = 30;
public static final int REMOTE_CONTROL_TYPE_BUS_SWITCH = 40;
public static final int REMOTE_CONTROL_TYPE_MULTI_SWITCH = 41;

// ===== 远程应答常量 =====
public static final int REMOTE_RESPONSE_STATUS_SUCCESS_MASK = 0x80;
public static final int REMOTE_RESPONSE_STATUS_EXECUTING_MASK = 0x40;
public static final int REMOTE_RESPONSE_STATUS_TIMEOUT_MASK = 0x3F;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/.../FourfaithGbtConstants.java
git commit -m "feat(fourfaith): add private protocol constants (PRT/CRT/heartbeat/remote)"
```

---

## Task 2: FourfaithGbtDataReader — 心跳包嗅探改造

**Files:**
- Modify: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/parser/FourfaithGbtDataReader.java`
- Modify: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/parser/FourfaithGbtDataPackage.java`
- Test: `src/test/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/parser/FourfaithGbtDataReaderTest.java`

- [ ] **Step 1: 编写心跳包识别测试**

```java
@Test
void testHeartbeatDetection() {
    // 构造心跳包: @@ + 源地址(6) + 通讯模块(1) + 系统类型(1) + ##
    byte[] heartbeat = new byte[] {
        0x40, 0x40,                       // @@
        0x10, 0x46, (byte)0x94, 0x50, 0x61, (byte)0xFF, // 源地址
        0x04,                             // 通讯模块
        (byte)0x88,                       // 系统类型
        0x23, 0x23                        // ##
    };
    
    AtomicBoolean detected = new AtomicBoolean(false);
    RecordParser parser = FourfaithGbtDataReader.getParser(buf -> {
        // 心跳包应被识别并回调
        detected.set(true);
        assertEquals(12, buf.length());
        assertEquals((byte)0x40, buf.getByte(0));
        assertEquals((byte)0x40, buf.getByte(1));
    });
    
    for (byte b : heartbeat) {
        parser.handle(Buffer.buffer(new byte[]{b}));
    }
    
    assertTrue(detected.get(), "心跳包应被识别");
}
```

Run: `mvn test -Dtest=FourfaithGbtDataReaderTest#testHeartbeatDetection`
Expected: FAIL（心跳嗅探逻辑尚未实现）

- [ ] **Step 2: 实现心跳包嗅探逻辑**

改造 `FourfaithGbtDataReader.getParser()` 的 Phase 2：

```java
case 2: // 收到 24 字节控制头（或心跳包前部）
    // 先尝试作为心跳包识别：读取 10 字节检查是否为 ## 结尾
    if (isHeartbeatPacket(buffer)) {
        // 是心跳包，组装简化帧
        Buffer heartbeatPacket = buildHeartbeatPacket(buffer);
        receiveHandler.accept(heartbeatPacket);
        resetState();
        break;
    }
    // 不是心跳包，按标准帧处理
    headerBuf = buffer.copy();
    dataLength = headerBuf.getShortLE(22) & 0xFFFF;
    // ... 原有逻辑
    break;

private boolean isHeartbeatPacket(Buffer buf) {
    // buf 在 Phase 2 时已经读取了 10 字节（@@ 之后的全部）
    return buf.length() >= 10
        && buf.getByte(8) == FourfaithGbtConstants.HEARTBEAT_END_BYTE
        && buf.getByte(9) == FourfaithGbtConstants.HEARTBEAT_END_BYTE;
}

private Buffer buildHeartbeatPacket(Buffer body) {
    Buffer packet = Buffer.buffer(FourfaithGbtConstants.HEARTBEAT_PACKET_SIZE);
    packet.appendByte(FourfaithGbtConstants.START_BYTE);
    packet.appendByte(FourfaithGbtConstants.START_BYTE);
    packet.appendBuffer(body); // 10 字节
    return packet;
}
```

> **注意**：Phase 2 的 fixedSizeMode 需要从 24 改为 10，在 `isHeartbeatPacket` 返回 false 时再读取剩余 14 字节。

- [ ] **Step 3: FourfaithGbtDataPackage 支持心跳包**

新增静态工厂方法：

```java
public static FourfaithGbtDataPackage parseHeartbeat(Buffer buf) {
    // buf: @@ + 源地址(6) + 通讯模块(1) + 系统类型(1) + ##
    byte[] srcAddr = buf.getBytes(2, 8);
    byte commModule = buf.getByte(8);
    byte sysType = buf.getByte(9);
    
    return FourfaithGbtDataPackage.builder()
        .sourceAddr(srcAddr)
        .command((byte)0xFF) // 心跳包特殊命令标记
        .heartbeat(true)
        .build();
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn test -Dtest=FourfaithGbtDataReaderTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/.../FourfaithGbtDataReader.java
git add src/main/java/.../FourfaithGbtDataPackage.java
git add src/test/java/.../FourfaithGbtDataReaderTest.java
git commit -m "feat(fourfaith): heartbeat packet detection and parsing"
```

---

## Task 3: FourfaithGbtDataParser — PRT/CRT/TLD-133 解析

**Files:**
- Modify: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/parser/FourfaithGbtDataParser.java`
- Create: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/parser/private/FourfaithRemoteResponseParser.java`
- Test: `src/test/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/parser/FourfaithGbtDataParserTest.java`

- [ ] **Step 1: 新增 PRT 解析方法**

```java
public static Map<String, Object> parsePrt(byte[] appData) {
    Map<String, Object> result = new LinkedHashMap<>();
    ByteBuffer buf = ByteBuffer.wrap(appData);
    
    result.put("prt_channel", buf.get() & 0xFF);
    result.put("prt_host_no", buf.get() & 0xFF);
    buf.position(buf.position() + 2); // 跳过预留 2 字节
    result.put("prt_alarm_type", buf.get() & 0xFF);
    
    // 设备名称
    int nameLen = buf.get() & 0xFF;
    byte[] nameBytes = new byte[nameLen];
    buf.get(nameBytes);
    result.put("prt_device_name", new String(nameBytes, StandardCharsets.UTF_8).trim());
    
    // 设备位置
    int locLen = buf.get() & 0xFF;
    byte[] locBytes = new byte[locLen];
    buf.get(locBytes);
    result.put("prt_device_location", new String(locBytes, StandardCharsets.UTF_8).trim());
    
    // 报警信息
    int infoLen = buf.get() & 0xFF;
    byte[] infoBytes = new byte[infoLen];
    buf.get(infoBytes);
    result.put("prt_alarm_info", new String(infoBytes, StandardCharsets.UTF_8).trim());
    
    return result;
}
```

- [ ] **Step 2: 新增 CRT 解析方法**

```java
public static Map<String, Object> parseCrt(byte[] appData) {
    Map<String, Object> result = new LinkedHashMap<>();
    ByteBuffer buf = ByteBuffer.wrap(appData);
    
    result.put("crt_channel", buf.get() & 0xFF);
    result.put("crt_host_no", buf.get() & 0xFF);
    buf.get(); // 跳过预留 1 字节
    int fieldCount = buf.get() & 0xFF;
    result.put("crt_alarm_type", buf.get() & 0xFF);
    
    List<Map<String, Object>> eventFields = new ArrayList<>();
    for (int i = 0; i < fieldCount; i++) {
        int type = buf.get() & 0xFF;
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("seq", i + 1);
        
        if (type == 0) {
            field.put("type", 0);
            field.put("value", null);
        } else if (type == 1) {
            int len = buf.get() & 0xFF;
            byte[] strBytes = new byte[len];
            buf.get(strBytes);
            field.put("type", 1);
            field.put("value", new String(strBytes, StandardCharsets.UTF_8).trim());
        } else if (type == 2) {
            field.put("type", 2);
            field.put("value", buf.getInt());
        }
        eventFields.add(field);
    }
    
    result.put("crt_event_json", JSONUtil.toJsonStr(eventFields));
    return result;
}
```

- [ ] **Step 3: 新增 TLD-133 解析器**

```java
@Slf4j
public class FourfaithRemoteResponseParser {
    
    public static Map<String, Object> parse(byte[] appData) {
        if (appData == null || appData.length < 10) {
            log.warn("TLD-133 数据长度不足: {}", appData != null ? appData.length : 0);
            return Collections.emptyMap();
        }
        
        ByteBuffer buf = ByteBuffer.wrap(appData);
        Map<String, Object> result = new LinkedHashMap<>();
        
        result.put("resp_sys_type", buf.get() & 0xFF);
        result.put("resp_sys_addr", buf.get() & 0xFF);
        result.put("resp_comp_type", buf.get() & 0xFF);
        result.put("resp_comp_addr", buf.getInt());
        
        int status = buf.get() & 0xFF;
        boolean success = (status & FourfaithGbtConstants.REMOTE_RESPONSE_STATUS_SUCCESS_MASK) != 0;
        boolean executing = (status & FourfaithGbtConstants.REMOTE_RESPONSE_STATUS_EXECUTING_MASK) != 0;
        int timeoutMin = status & FourfaithGbtConstants.REMOTE_RESPONSE_STATUS_TIMEOUT_MASK;
        
        result.put("resp_success", success ? 1 : 0);
        result.put("resp_executing", executing ? 1 : 0);
        result.put("resp_timeout_min", timeoutMin);
        result.put("resp_status_code", status);
        
        int respLen = buf.getShort() & 0xFFFF;
        if (respLen > 0 && buf.remaining() >= respLen) {
            byte[] respBytes = new byte[respLen];
            buf.get(respBytes);
            result.put("resp_content", new String(respBytes, StandardCharsets.UTF_8).trim());
        }
        
        return result;
    }
}
```

- [ ] **Step 4: 编写 PRT/CRT/TLD-133 单元测试**

使用 `F-PRC200消防协议解析.txt` 中的示例字节流构造测试数据。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/.../parser/
git add src/test/java/.../parser/
git commit -m "feat(fourfaith): add PRT/CRT/TLD-133 parsing"
```

---

## Task 4: PendingRequest + PendingRequestManager — 异步请求管理

**Files:**
- Create: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/model/PendingRequest.java`
- Create: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/PendingRequestManager.java`
- Test: `src/test/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/PendingRequestManagerTest.java`

- [ ] **Step 1: 编写 PendingRequest 数据模型**

```java
@Data
@Builder
public class PendingRequest {
    private String invokeId;      // 平台服务调用ID
    private String productKey;    // 设备 productKey
    private String deviceName;    // 设备 deviceName
    private int serialNumber;     // 业务流水号（关联键）
    private String serviceName;   // 服务名，如 remote_control
    private long sendTime;        // 发送时间戳
}
```

- [ ] **Step 2: 编写 PendingRequestManager**

```java
@Slf4j
@Component
public class PendingRequestManager {
    
    private final Map<Integer, PendingRequest> pendingMap = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 30000; // 30秒超时
    
    public void put(int serialNumber, PendingRequest request) {
        pendingMap.put(serialNumber, request);
        log.info("[PendingRequest] 注册请求: SN={}, service={}, invokeId={}",
            serialNumber, request.getServiceName(), request.getInvokeId());
    }
    
    public PendingRequest get(int serialNumber) {
        return pendingMap.get(serialNumber);
    }
    
    public PendingRequest remove(int serialNumber) {
        PendingRequest req = pendingMap.remove(serialNumber);
        if (req != null) {
            long elapsed = System.currentTimeMillis() - req.getSendTime();
            log.info("[PendingRequest] 移除请求: SN={}, elapsed={}ms", serialNumber, elapsed);
        }
        return req;
    }
    
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void timeoutCheck() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, PendingRequest>> it = pendingMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PendingRequest> entry = it.next();
            PendingRequest req = entry.getValue();
            if (now - req.getSendTime() > TIMEOUT_MS) {
                it.remove();
                log.warn("[PendingRequest] 请求超时: SN={}, service={}",
                    req.getSerialNumber(), req.getServiceName());
                // TODO: 调用回调上报超时结果（由 Component 层处理）
            }
        }
    }
    
    public int size() {
        return pendingMap.size();
    }
}
```

- [ ] **Step 3: 编写单元测试**

```java
@Test
void testPutAndRemove() {
    PendingRequest req = PendingRequest.builder()
        .invokeId("INV-001")
        .serialNumber(100)
        .serviceName("remote_control")
        .sendTime(System.currentTimeMillis())
        .build();
    
    manager.put(100, req);
    assertEquals(1, manager.size());
    
    PendingRequest removed = manager.remove(100);
    assertNotNull(removed);
    assertEquals("INV-001", removed.getInvokeId());
    assertEquals(0, manager.size());
}

@Test
void testTimeout() throws InterruptedException {
    // 使用反射或测试配置将超时设为 100ms
    PendingRequest req = PendingRequest.builder()
        .invokeId("INV-002")
        .serialNumber(200)
        .sendTime(System.currentTimeMillis() - 35000) // 35秒前
        .build();
    
    manager.put(200, req);
    manager.timeoutCheck();
    assertEquals(0, manager.size());
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/.../model/PendingRequest.java
git add src/main/java/.../service/PendingRequestManager.java
git add src/test/java/.../service/PendingRequestManagerTest.java
git commit -m "feat(fourfaith): add PendingRequestManager for async service invocation"
```

---

## Task 5: FourfaithGbtTcpComponent — 心跳处理 + 类型路由 + 异步服务调用

**Files:**
- Modify: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithGbtTcpComponent.java`
- Test: `src/test/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithGbtTcpComponentTest.java`

- [ ] **Step 1: 注入 PendingRequestManager**

```java
@Autowired
private PendingRequestManager pendingRequestManager;
```

- [ ] **Step 2: processPacket 增加心跳包分支**

```java
private void processPacket(FourfaithGbtDataPackage pkg, ...) {
    String deviceAddr = pkg.getSourceAddrHex();
    byte command = pkg.getCommand();
    
    // 心跳包特殊处理
    if (pkg.isHeartbeat()) {
        handleHeartbeat(deviceAddr);
        return;
    }
    
    clientMap.put(deviceAddr, client);
    heartbeatDevice.put(deviceAddr, System.currentTimeMillis());
    ensureDeviceRegistered(deviceAddr);
    
    switch (command) {
        // ... 原有分支
    }
}

private void handleHeartbeat(String deviceAddr) {
    heartbeatDevice.put(deviceAddr, System.currentTimeMillis());
    log.warn("[GBT26875] 收到心跳包: 设备={}", deviceAddr);
    sendHeartbeatReply(deviceAddr);
}

private void sendHeartbeatReply(String deviceAddr) {
    byte[] replyAddr = new byte[FourfaithGbtConstants.ADDRESS_LENGTH]; // 全 0
    Buffer reply = Buffer.buffer(FourfaithGbtConstants.HEARTBEAT_PACKET_SIZE);
    reply.appendByte(FourfaithGbtConstants.START_BYTE);
    reply.appendByte(FourfaithGbtConstants.START_BYTE);
    reply.appendBytes(replyAddr); // 6 字节全 0
    reply.appendByte((byte) 0);   // 通讯模块 0
    reply.appendByte((byte) 0);   // 系统类型 0
    reply.appendBytes(FourfaithGbtConstants.END_BYTES); // ##
    
    NetSocket socket = clientMap.get(deviceAddr);
    if (socket != null) {
        socket.write(reply);
    }
}
```

- [ ] **Step 3: handleDataUpload 增加 Type 0x09 / 0x0A 分支**

```java
private void handleDataUpload(FourfaithGbtDataPackage pkg, String deviceAddr) {
    byte[] appData = pkg.getApplicationData();
    if (appData == null || appData.length < 2) return;
    
    int typeFlag = FourfaithGbtDataParser.getTypeFlag(appData);
    String pk = dnToPk.get(deviceAddr);
    if (pk == null) return;
    
    switch (typeFlag) {
        case FourfaithGbtConstants.TYPE_PRT:
            handlePrtUpload(appData, deviceAddr, pk);
            break;
        case FourfaithGbtConstants.TYPE_CRT:
            handleCrtUpload(appData, deviceAddr, pk);
            break;
        // ... 原有分支
    }
}

private void handlePrtUpload(byte[] appData, String deviceAddr, String pk) {
    Map<String, Object> props = FourfaithGbtDataParser.parsePrt(appData);
    props.put("prt_time", new Date());
    
    report(PropertyReport.builder()
        .productKey(pk)
        .deviceName(deviceAddr)
        .params(props)
        .build());
    
    int alarmType = (Integer) props.getOrDefault("prt_alarm_type", 0);
    if (alarmType != 0) {
        Map<String, Object> eventParams = new LinkedHashMap<>();
        eventParams.put("alarm_type", alarmType);
        eventParams.put("device_name", props.get("prt_device_name"));
        eventParams.put("location", props.get("prt_device_location"));
        eventParams.put("info", props.get("prt_alarm_info"));
        eventParams.put("event_time", props.get("prt_time"));
        eventParams.put("source", "PRT");
        
        report(EventReport.builder()
            .productKey(pk)
            .deviceName(deviceAddr)
            .identifier("prt_alarm_event")
            .params(eventParams)
            .build());
    }
}

private void handleCrtUpload(byte[] appData, String deviceAddr, String pk) {
    Map<String, Object> props = FourfaithGbtDataParser.parseCrt(appData);
    props.put("crt_time", new Date());
    
    report(PropertyReport.builder()
        .productKey(pk)
        .deviceName(deviceAddr)
        .params(props)
        .build());
    
    // CRT 部件关联逻辑
    String eventJson = (String) props.get("crt_event_json");
    String linkedDn = resolveCrtComponent(deviceAddr, pk, eventJson);
    
    // 触发 crt_alarm_event
    Map<String, Object> eventParams = new LinkedHashMap<>();
    eventParams.put("alarm_type", props.get("crt_alarm_type"));
    eventParams.put("event_json", eventJson);
    eventParams.put("event_time", props.get("crt_time"));
    eventParams.put("source", "CRT");
    eventParams.put("linked_device_name", linkedDn);
    
    report(EventReport.builder()
        .productKey(pk)
        .deviceName(deviceAddr)
        .identifier("crt_alarm_event")
        .params(eventParams)
        .build());
}
```

- [ ] **Step 4: CRT 部件关联方法**

```java
private String resolveCrtComponent(String parentAddr, String parentPk, String eventJson) {
    try {
        JSONArray fields = JSONUtil.parseArray(eventJson);
        // 字段2 = 设备描述/位置
        if (fields.size() < 2) return null;
        JSONObject field2 = fields.getJSONObject(1);
        String desc = field2.getStr("value", "");
        
        // 解析 &&QN01,机器11,回路3A,部件2CD
        // 提取 hostNo, circuitNo, addressNo
        String[] parts = desc.split(",");
        if (parts.length < 4) return null;
        
        String circuitNo = extractValue(parts[2], "回路");  // "3A"
        String addressNo = extractValue(parts[3], "部件");  // "2CD"
        
        // 查询子设备匹配 point_circuit_no + point_address_no
        String linkedDn = findComponentByPointAttr(parentAddr, circuitNo, addressNo);
        if (linkedDn != null) {
            return linkedDn;
        }
        
        // 未命中，自动创建子设备
        int compAddr = Integer.parseInt(addressNo, 16); // "2CD" -> 717
        String compDn = generateComponentDeviceName(parentAddr, 0, 0, 1, compAddr);
        ensureComponentDeviceRegistered(parentAddr, parentPk, 0, 0, 1, compAddr);
        
        // 回填点位属性（通过属性上报）
        Map<String, Object> pointProps = new LinkedHashMap<>();
        pointProps.put("point_circuit_no", circuitNo);
        pointProps.put("point_address_no", addressNo);
        report(PropertyReport.builder()
            .productKey(parentPk + "sub")
            .deviceName(compDn)
            .params(pointProps)
            .build());
        
        return compDn;
    } catch (Exception e) {
        log.warn("CRT 部件关联失败: {}", e.getMessage());
        return null;
    }
}

private String extractValue(String part, String prefix) {
    int idx = part.indexOf(prefix);
    return idx >= 0 ? part.substring(idx + prefix.length()).trim() : part.trim();
}

private String findComponentByPointAttr(String parentAddr, String circuitNo, String addressNo) {
    // 通过设备管理接口查询子设备列表并匹配属性
    // 具体实现依赖平台设备管理 API
    // 返回匹配到的子设备 deviceName，未命中返回 null
    return null; // TODO: 接入实际设备查询接口
}
```

- [ ] **Step 5: CMD_CONFIRM 增强 + CMD_RESPONSE 关联**

```java
case FourfaithGbtConstants.CMD_CONFIRM:
    int confirmSn = pkg.getSerialNumber();
    PendingRequest req = pendingRequestManager.get(confirmSn);
    if (req != null) {
        log.info("[GBT26875] 收到确认: 设备={}, SN={}, 服务={}",
            deviceAddr, confirmSn, req.getServiceName());
    } else {
        log.info("[GBT26875] 收到确认: 设备={}, SN={}", deviceAddr, confirmSn);
    }
    break;

case FourfaithGbtConstants.CMD_RESPONSE:
    // 设备对我方查询/控制的应答（cmd=05, TLD-133）
    handleRemoteResponse(pkg, deviceAddr);
    break;
```

- [ ] **Step 6: 远程应答处理**

```java
private void handleRemoteResponse(FourfaithGbtDataPackage pkg, String deviceAddr) {
    int serialNumber = pkg.getSerialNumber();
    PendingRequest req = pendingRequestManager.remove(serialNumber);
    
    byte[] appData = pkg.getApplicationData();
    if (appData == null || appData.length < 2) return;
    
    int typeFlag = FourfaithGbtDataParser.getTypeFlag(appData);
    if (typeFlag == FourfaithGbtConstants.TYPE_REMOTE_RESPONSE) {
        Map<String, Object> resp = FourfaithRemoteResponseParser.parse(appData);
        
        if (req != null && "remote_control".equals(req.getServiceName())) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("success", resp.get("resp_success"));
            output.put("result_code", resp.get("resp_status_code"));
            output.put("result_desc", ((Integer)resp.get("resp_success") == 1)
                ? "执行成功" : "执行失败");
            
            report(InvokeReply.builder()
                .productKey(req.getProductKey())
                .deviceName(req.getDeviceName())
                .invokeId(req.getInvokeId())
                .outputData(output)
                .build());
        }
    }
}
```

- [ ] **Step 7: serviceInvoke 增加 remote_control**

```java
case "remote_control": {
    Map<String, ?> params = action.getParams();
    int controlType = getIntParam(params, "control_type", 0);
    String hostNo = getStringParam(params, "host_no", "");
    String circuitNo = getStringParam(params, "circuit_no", "");
    String addressNo = getStringParam(params, "address_no", "");
    
    // 调用四信 API 获取控制指令
    FourfaithRemoteControlClient.CommandResult result = 
        remoteControlClient.createCommand(controlType, hostNo, circuitNo, addressNo);
    
    if (!result.isSuccess()) {
        // API 调用失败，立即回传
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("success", 0);
        output.put("result_desc", result.getErrorMsg());
        report(InvokeReply.builder()
            .productKey(pk)
            .deviceName(dn)
            .invokeId(action.getInvokeId())
            .outputData(output)
            .build());
        break;
    }
    
    // 注册到 PendingRequestManager
    int sn = nextSerial();
    pendingRequestManager.put(sn, PendingRequest.builder()
        .invokeId(action.getInvokeId())
        .productKey(pk)
        .deviceName(dn)
        .serialNumber(sn)
        .serviceName("remote_control")
        .sendTime(System.currentTimeMillis())
        .build());
    
    // 发送 Type 0x81 控制帧
    byte[] commandBytes = HexUtil.decodeHex(result.getCommandCode());
    byte[] appData = buildType81AppData(commandBytes);
    sendControl(dn, FourfaithGbtConstants.TYPE_REMOTE_CONTROL, appData);
    break;
}
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/.../service/FourfaithGbtTcpComponent.java
git commit -m "feat(fourfaith): heartbeat handling, PRT/CRT routing, async remote control"
```

---

## Task 6: FourfaithRemoteControlClient — 四信 HTTP API 客户端

**Files:**
- Create: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithRemoteControlClient.java`
- Test: `src/test/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithRemoteControlClientTest.java`

- [ ] **Step 1: 编写 HTTP API 客户端**

```java
@Slf4j
@Component
public class FourfaithRemoteControlClient {
    
    @Autowired
    private FourfaithGbtTcpConfig config;
    
    @Data
    @Builder
    public static class CommandResult {
        private boolean success;
        private String commandCode;
        private String errorMsg;
    }
    
    public CommandResult createCommand(int controlType, String hostNo, 
                                       String circuitNo, String addressNo) {
        try {
            String url = config.getFourfaithApiUrl() + "/api/v1/hardwareFunction/createHardwareCommand";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("brandCode", config.getBrandCode());
            requestBody.put("model", config.getModel());
            requestBody.put("version", config.getVersion());
            
            Map<String, Object> funcMap = new HashMap<>();
            funcMap.put("name", getControlTypeName(controlType));
            
            Map<String, String> valueMap = new HashMap<>();
            valueMap.put("hostNo", hostNo);
            valueMap.put("circuitNo", circuitNo);
            valueMap.put("addressNo", addressNo);
            funcMap.put("functionValueMap", valueMap);
            
            requestBody.put("functionValueList", Collections.singletonList(funcMap));
            
            String response = HttpUtil.post(url, JSONUtil.toJsonStr(requestBody), 
                config.getFourfaithApiTimeoutMs());
            
            JSONObject json = JSONUtil.parseObj(response);
            int code = json.getInt("code", -1);
            if (code != 0) {
                return CommandResult.builder()
                    .success(false)
                    .errorMsg(json.getStr("message", "API调用失败"))
                    .build();
            }
            
            // 提取 commandCode
            JSONArray dataArr = json.getJSONArray("data");
            if (dataArr == null || dataArr.isEmpty()) {
                return CommandResult.builder()
                    .success(false)
                    .errorMsg("API返回数据为空")
                    .build();
            }
            
            JSONObject firstData = dataArr.getJSONObject(0);
            JSONArray cmdList = firstData.getJSONArray("functionCommandList");
            if (cmdList == null || cmdList.isEmpty()) {
                return CommandResult.builder()
                    .success(false)
                    .errorMsg("指令列表为空")
                    .build();
            }
            
            String commandCode = cmdList.getJSONObject(0).getStr("commandCode");
            return CommandResult.builder()
                .success(true)
                .commandCode(commandCode)
                .build();
                
        } catch (Exception e) {
            log.error("四信API调用异常", e);
            return CommandResult.builder()
                .success(false)
                .errorMsg("API调用异常: " + e.getMessage())
                .build();
        }
    }
    
    private String getControlTypeName(int type) {
        switch (type) {
            case 10: return "复位";
            case 20: return "消音";
            case 30: return "开关";
            case 40: return "总线开关";
            case 41: return "多线开关";
            default: return "未知";
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/.../service/FourfaithRemoteControlClient.java
git add src/test/java/.../service/FourfaithRemoteControlClientTest.java
git commit -m "feat(fourfaith): add FourfaithRemoteControlClient for HTTP API"
```

---

## Task 7: FourfaithGbtTcpConfig + 物模型 JSON

**Files:**
- Modify: `src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/model/FourfaithGbtTcpConfig.java`
- Create: `doc/fourfaith_component_thing_model.json`
- Modify: `doc/gbt26875_thing_model.json`

- [ ] **Step 1: 扩展配置模型**

```java
@Data
public class FourfaithGbtTcpConfig {
    // ... 原有字段
    
    // 四信 API 配置
    private String fourfaithApiUrl;
    private String fourfaithApiAppId;
    private String fourfaithApiAppSecret;
    private Integer fourfaithApiTimeoutMs = 10000;
    
    // 设备型号配置
    private String brandCode;
    private String model;
    private String version;
}
```

- [ ] **Step 2: 创建 fourfaith_component_thing_model.json**

基于 `doc/gbt26875_component_thing_model.json` 复制并新增 5 个点位属性。

- [ ] **Step 3: Commit**

```bash
git add src/main/java/.../model/FourfaithGbtTcpConfig.java
git add doc/fourfaith_component_thing_model.json
git add doc/gbt26875_thing_model.json
git commit -m "feat(fourfaith): extend config and thing models"
```

---

## Task 8: 集成测试与验证

**Files:**
- Test: `src/test/java/com/enjoyiot/eiot/component/gbt/fourfaith/client/TcpTestClient.java`

- [ ] **Step 1: TcpTestClient 增加 PRT/CRT/心跳包测试用例**

- [ ] **Step 2: 端到端测试**

```bash
mvn test -pl module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp
```

- [ ] **Step 3: Commit**

```bash
git commit -m "test(fourfaith): add integration tests for PRT/CRT/heartbeat/remote-control"
```

---

## Spec Coverage Check

| Spec 章节 | 对应 Task | 状态 |
|-----------|----------|------|
| 4. 心跳包兼容 | Task 2 | ✅ |
| 5.1 PRT 解析 | Task 3 | ✅ |
| 5.2 CRT 解析 | Task 3 | ✅ |
| 5.3 CRT 部件关联 | Task 5 Step 4 | ✅ |
| 6. 远程控制下行 | Task 6 + Task 5 Step 7 | ✅ |
| 7. 远程应答解析 | Task 3 Step 3 + Task 5 Step 6 | ✅ |
| 8. 异步服务调用关联 | Task 4 + Task 5 Step 5/6/7 | ✅ |
| 9. 配置项与物模型 | Task 7 | ✅ |
| 10. 数据流设计 | Task 5 | ✅ |

---

## Execution Options

**Plan saved to:** `docs/superpowers/plans/2026-04-25-fourfaith-private-extension.md`

**Two execution options:**

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
