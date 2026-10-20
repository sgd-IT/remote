# 四信私有用户传输装置（F-PRC200）私有协议扩展设计

## 1. 概述

### 1.1 背景

现有 `iot-component-gbt26875-fourfaith-tcp` 模块已实现 GB/T 26875.3-2011 标准协议接入（Type 1~8、21~28）。四信提供的 F-PRC200 私有用户传输装置在国标基础上扩展了以下私有类型：

- **上行**：Type 0x09（PRT 打印信息）、Type 0x0A（CRT 图形显示信息）
- **下行**：Type 0x81（TLD-129 远程控制）、Type 0x85（TLD-133 远程应答）
- **外部 API**：四信智慧消防云平台设备指令 API（获取控制指令）

### 1.2 设计目标

1. 支持解析并上报 PRT/CRT 私有报警信息
2. 支持 CRT 信息关联到已有部件子设备（公式映射）
3. 支持通过四信云平台 API 获取指令并下发远程控制
4. 支持解析远程应答获取执行结果
5. 保持 TDengine 行长度不超限

### 1.3 设备标识规则

**用传设备（父设备）唯一标识**：
- 源地址 6 字节 hex 字符串（`四信ID(1B) + 设备类型(1B) + 设备编号(4B)`）
- 示例：`1046945061FF`

**部件子设备命名规则**：
- `{父地址}_{sysType}_{sysAddr}_{compType}_{compAddr}`
- 示例：`1046945061FF_00_00_01_1003`

### 1.4 适用范围

- **模块**：`iot-component-gbt26875-fourfaith-tcp`（四信专用模块，已与国标主干分离）
- **设备**：四信 F-PRC200 系列私有用户传输装置
- **场景**：借测验证及后续正式接入

---

## 2. 文档依据

| 文档 | 文件名 | 用途 |
|------|--------|------|
| F-PRC200 消防协议解析 | `F-PRC200消防协议解析(1).txt` | PRT/CRT 报文示例及解析说明 |
| 下发主机远程控制对接说明 | `01-下发主机远程控制对接说明及示例V1.1(1).docx` | Type 0x81 帧示例及流程说明 |
| 四信设备指令 API | `四信智慧消防云平台-设备指令API_V1.0.pdf` | HTTP API 接口定义（获取/生成控制指令） |
| 城市消防远程监控系统协议拓展版 | `城市消防远程监控系统协议_拓展版_V1.53-20230608.pdf` | TLD-9/10/129/133 数据结构定义 |

---

## 3. 架构设计

保持单模块，在包结构上分层：

```
iot-component-gbt26875-fourfaith-tcp
├── parser/
│   ├── FourfaithGbtDataParser.java          ← 追加 Type 0x09/0x0A/0x81/0x85 解析
│   ├── FourfaithGbtConstants.java           ← 追加私有类型常量
│   ├── FourfaithGbtDataReader.java          ← 追加心跳包嗅探逻辑
│   └── private/
│       ├── FourfaithPrtParser.java          ← PRT 专用解析（可选提取）
│       ├── FourfaithCrtParser.java          ← CRT 专用解析（可选提取）
│       └── FourfaithRemoteResponseParser.java ← TLD-133 应答解析
├── service/
│   ├── FourfaithGbtTcpComponent.java        ← 增加类型路由 + 异步请求管理
│   ├── FourfaithRemoteControlClient.java    ← 四信 HTTP API 客户端（新增）
│   └── PendingRequestManager.java           ← 异步服务调用请求缓存（新增）
├── model/
│   ├── FourfaithGbtTcpConfig.java           ← 追加 API 及型号配置
│   └── PendingRequest.java                  ← 请求上下文（新增）
└── client/
    └── FourfaithGbtVertxTcpClient.java      ← 现有，无需改动
```

**设计原则**：
- `FourfaithGbtTcpComponent` 只做**类型路由和调度**
- PRT/CRT 解析逻辑放在 `FourfaithGbtDataParser`（Parser 本职扩展）
- 远程控制 HTTP 调用封装在 `FourfaithRemoteControlClient`（独立可测）
- TLD-133 应答解析独立为 `FourfaithRemoteResponseParser`
- 心跳包识别在 `FourfaithGbtDataReader` 中完成（不污染业务层）
- 异步请求关联由 `PendingRequestManager` 统一管理

---

## 4. 心跳包兼容

### 4.1 问题背景

四信 F-PRC200 心跳包采用**简化格式**，与标准 GB/T 26875 数据帧格式不同：

**标准数据帧**（`FourfaithGbtDataReader` 当前支持）：
```
@@ + 24字节控制头 + command(1) + data(L) + checksum(1) + ##
```

**心跳包格式**（`F-PRC200消防协议解析.txt` 第42行）：
```
40 40 10 46 94 50 61 FF 04 88 23 23
@@   源地址(6B)   通讯模块 系统类型  ##
```
- 总长度仅 **12字节**
- `@@` 之后只有 8 字节（无流水号、版本号、时间标签、目的地址、数据长度）

**如果不处理**：Reader 在 Phase 2 尝试读取 24 字节控制头时，读到 `##` 导致帧同步丢失，后续数据包全部解析失败，设备被误判离线。

### 4.2 解决方案

修改 `FourfaithGbtDataReader` 状态机，在 Phase 2 增加**心跳包嗅探**：

```
Phase 0: 扫描第一个 @
Phase 1: 验证第二个 @
    │
    ▼
Phase 2: 改为先读 10 字节（源地址 6B + 通讯模块 1B + 系统类型 1B + 结束符 2B）
    │
    ├── 检查 offset 8~9 是否为 ##（0x23 0x23）
    │       │
    │       ├── 是 → 心跳包（总长度 12 字节）
    │       │       └── 提取 offset 0~5 作为 sourceAddr
    │       │       └── 提取 offset 6 作为 commModule
    │       │       └── 提取 offset 7 作为 sysType
    │       │       └── 回调 receiveHandler.accept(heartbeatPacket)
    │       │       └── resetState → 回到 Phase 0
    │       │
    │       └── 否 → 标准数据帧
    │               └── 缓存这 10 字节作为 header 前部
    │               └── 继续读取剩余 14 字节凑齐 24 字节 header
    │               └── 进入 Phase 3 正常解析
```

**心跳包应答格式**（txt 文档第50行）：
```
40 40 00 00 00 00 00 00 00 00 23 23
@@   源地址(6字节，全0)  通讯模块(0) 系统类型(0)  ##
```

### 4.3 心跳包处理流程

```
FourfaithGbtDataReader 识别到心跳包
  └── receiveHandler 传递给 FourfaithGbtTcpComponent
      ├── 更新 heartbeatDevice 最后活跃时间
      ├── 发送心跳应答（固定格式）
      └── 不触发 ensureDeviceRegistered（心跳包无应用数据）
```

### 4.4 影响范围

| 文件 | 改动 |
|------|------|
| `FourfaithGbtDataReader.java` | Phase 2 增加心跳嗅探逻辑；新增心跳包识别常量 |
| `FourfaithGbtTcpComponent.java` | `processPacket` 增加心跳包分支处理；新增 `sendHeartbeatReply()` |
| `FourfaithGbtDataPackage.java` | 支持构建简化格式的心跳包（无控制头字段） |

---

## 5. 私有上行协议扩展（PRT / CRT）

### 5.1 PRT 解析（Type 0x09 / TLD-9）

**数据结构**（PDF 表 8.2.1.9.1）：

| 位置 | 内容 | 类型 | 长度 | 说明 |
|------|------|------|------|------|
| 0 | 通道号 | BYTE | 1 | 0~128 |
| 1 | 消控主机编号 | BYTE | 1 | 0~255 |
| 2 | 预留 | WORD | 2 | 固定 0x3333 |
| 4 | 报警类型 | BYTE | 1 | 参考报警类型状态图 |
| 5 | 设备名称长度 | BYTE | 1 | 0~255 |
| 6 | 设备名称 | STRING | n | GB18030 编码 |
| 6+n | 设备位置长度 | BYTE | 1 | 0~255 |
| 7+n | 设备位置 | STRING | m | GB18030 编码 |
| 7+n+m | 报警信息长度 | BYTE | 1 | 0~255 |
| 8+n+m | 报警信息 | STRING | k | GB18030 编码 |

**字段映射**（前缀 `prt_`）：

| 字段名 | 类型 | 物模型属性 |
|--------|------|-----------|
| prt_channel | int32 | `prt_channel` |
| prt_host_no | int32 | `prt_host_no` |
| prt_alarm_type | int32 | `prt_alarm_type` |
| prt_device_name | text | `prt_device_name` |
| prt_device_location | text | `prt_device_location` |
| prt_alarm_info | text | `prt_alarm_info` |
| prt_time | date | `prt_time` |

**上报策略**：
- 属性上报到**父设备**
- 当 `prt_alarm_type != 0` 时，触发 `prt_alarm_event`
- 不触发标准 `fire_alarm_event`（避免重复）

### 5.2 CRT 解析（Type 0x0A / TLD-10）

**数据结构**（PDF 表 8.2.1.10.1）：

| 位置 | 内容 | 类型 | 长度 | 说明 |
|------|------|------|------|------|
| 0 | 通道号 | BYTE | 1 | 0~128 |
| 1 | 消控主机编号 | BYTE | 1 | 0~255 |
| 2 | 预留 | BYTE | 1 | 固定 0x33 |
| 3 | 类型个数 | BYTE | 1 | 2~10，事件字段个数 |
| 4 | 报警类型 | BYTE | 1 | 参考系统状态图 |
| 5 | 事件字段1表示类型 | BYTE | 1 | 0=未启用, 1=字符串, 2=数值 |
| 6 | 事件字段1应用数据 | BYTE[n] | n | 根据表示类型选择数据结构 |
| ... | 事件字段2~n | - | - | 同上 |

**表示类型结构**（PDF 表 8.2.1.10.2）：

| 值 | 说明 |
|----|------|
| 0 | 未启用，无相关应用数据 |
| 1 | 字符串（长度1B + 内容nB，GB18030） |
| 2 | 数值（DWORD 4，小端模式） |

**事件字段语义**（固定顺序）：

| 字段序号 | 语义 | 示例内容 |
|---------|------|---------|
| 1 | 设备名称 | 可能为空 |
| 2 | 设备描述（位置） | `&&QN01,机器11,回路3A,部件2CD` |
| 3 | 设备信息 | `本机火警` |
| 4 | 设备编号 | 可能为空 |
| 5 | 设备其他 | 可能为空 |

**字段映射**（前缀 `crt_`）：

| 字段名 | 类型 | 物模型属性 |
|--------|------|-----------|
| crt_channel | int32 | `crt_channel` |
| crt_host_no | int32 | `crt_host_no` |
| crt_event_json | text | `crt_event_json` |
| crt_time | date | `crt_time` |

**crt_event_json 内部结构示例**：

```json
[
  {"seq":1,"name":"设备名称","type":1,"value":""},
  {"seq":2,"name":"设备描述","type":1,"value":"&&QN01,机器11,回路3A,部件2CD"},
  {"seq":3,"name":"设备信息","type":1,"value":"本机火警"},
  {"seq":4,"name":"设备编号","type":1,"value":""}
]
```

**上报策略**：
- 属性上报到**父设备**
- 当报警类型不为 0 或包含有效报警信息时，触发 `crt_alarm_event`
- 不触发标准 `fire_alarm_event`

### 5.3 CRT 部件关联（子设备属性匹配）

**目标**：将 CRT 解析出的 `主机号+回路号+地址号` 与部件子设备的点位属性匹配，实现报警关联到具体部件。

**匹配逻辑**：

```java
// 从 CRT 字段2解析出三元组
String hostNo = "11";      // 机器号
String circuitNo = "3A";   // 回路号
String addressNo = "2CD";  // 地址号

// 查询该父设备下所有子设备，匹配点位属性
List<Device> children = queryChildren(parentDn);
for (Device child : children) {
    if (circuitNo.equals(child.getProperty("point_circuit_no"))
        && addressNo.equals(child.getProperty("point_address_no"))) {
        // 命中，给该子设备上报属性/事件
        return child.getDeviceName();
    }
}
```

**关联流程**：

```
收到 CRT 数据
  └── 解析 crt_event_json
      └── 提取字段2（设备描述/位置）
          └── 字符串解析提取 hostNo + circuitNo + addressNo
              └── 查询该父设备下的子设备列表
                  ├── 命中（point_circuit_no + point_address_no 匹配）
                  │     └── 给该子设备更新属性/触发事件
                  │     └── crt_alarm_event.linked_device_name = 子设备DN
                  └── 未命中
                        └── 给父设备上报 crt_event_json
                        └── 自动创建子设备（deviceName 按规则生成）
                        └── 回填属性：point_host_no / point_circuit_no / point_address_no
                        └── crt_alarm_event.linked_device_name = 新子设备DN
```

**点位属性来源**：

| 属性 | 来源 | 说明 |
|------|------|------|
| `point_host_no` | CRT 解析回填 / Excel 导入 | 主机号 |
| `point_circuit_no` | CRT 解析回填 / Excel 导入 | 回路号 |
| `point_address_no` | CRT 解析回填 / Excel 导入 | 地址号 |
| `point_name` | Excel 导入 | 点位名称（设备名称）|
| `point_location` | Excel 导入 | 点位位置 |

> Excel 导入时只需关注**设备名称**和**设备唯一标识**，回路号/地址号由 CRT 首次报警时自动解析回填。如果 Excel 中已包含回路号/地址号，导入时一并写入即可。

---

## 6. 远程控制下行（Type 0x81 / TLD-129）

### 6.1 业务流程

```
值守端/平台
  └── 调用服务 remote_control (control_type, host_no, circuit_no, address_no, switch_type)
      └── FourfaithGbtTcpComponent.serviceInvoke()
          └── FourfaithRemoteControlClient
              ├── Step 1: 调用四信 API 获取控制指令
              │     POST {fourfaithApiUrl}/api/v1/hardwareFunction/createHardwareCommand
              │     Body: {brandCode, model, version, functionValueList:[{...}]}
              │     Response: {data:[{functionCommandList:[{commandCode, type, seq}]}]}
              │
              ├── Step 2: commandCode (hex字符串) → byte[]
              │
              └── Step 3: 组装 Type 0x81 帧并下发
                    系统类型(1B) + 系统地址(1B) + 部件类型(1B) + 部件地址(4B)
                    + 命令长度(2B) + 命令内容(nB) + 时间标签(6B)

设备收到
  ├── 回复收到 (cmd=03)
  └── 执行完成后回复结果 (cmd=05, TLD-133)
      └── FourfaithRemoteResponseParser 解析并上报平台
```

### 6.2 四信 HTTP API 对接

**接口 1：获取设备功能清单**

```
POST /api/v1/hardwareFunction/getHardwareFunctionList
Content-Type: application/json

Request:
{
  "brandCode": "QIN_NIAO",
  "model": "QINNIAO",
  "version": "1.0"
}

Response:
{
  "code": 0,
  "data": [
    {
      "name": "复位",
      "type": "10",
      "functionCodeList": [
        {"name": "主机号", "code": "hostNo", "type": "Long"},
        {"name": "回路号", "code": "circuitNo", "type": "String"}
      ]
    }
  ]
}
```

**接口 2：生成设备功能指令**

```
POST /api/v1/hardwareFunction/createHardwareCommand
Content-Type: application/json

Request:
{
  "brandCode": "QIN_NIAO",
  "model": "QINNIAO",
  "version": "1.0",
  "functionValueList": [
    {
      "name": "复位",
      "functionValueMap": {
        "addressNo": "10",
        "hostNo": "20",
        "circuitNo": "AA"
      }
    }
  ]
}

Response:
{
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "name": "复位",
      "type": "10",
      "ruleNum": 1,
      "awaitTime": 2000,
      "functionCommandList": [
        {
          "commandCode": "852A3D3E83EA2771AD60335E3EFB61A1...",
          "type": "1",
          "seq": 1
        }
      ]
    }
  ]
}
```

**控制类型枚举**（来自 API PDF）：

| type 值 | 含义 |
|---------|------|
| 10 | 复位 |
| 20 | 消音 |
| 30 | 开关 |
| 40 | 总线开关 |
| 41 | 多线开关 |

### 6.3 Type 0x81 帧结构（TLD-129）

| 字段 | 长度 | 说明 |
|------|------|------|
| 类型标志 | 1B | 0x81（129） |
| 信息体数目 | 1B | 0x01 |
| 系统类型标志 | 1B | 固定 0x88（【待确认】docx 示例值） |
| 系统地址 | 1B | **PDF 定义固定 0x00**，docx 示例为 0x02，以 PDF 为准 |
| 部件类型 | 1B | 固定 0x83（【待确认】docx 示例值） |
| 部件地址 | 4B | 设备地址前 4 字节（小端序） |
| 命令长度 | 2B | 平台指令长度（小端序） |
| 命令内容 | nB | 四信 API 返回的 commandCode 字节数组 |
| 时间标签 | 6B | 当前时间（秒/分/时/日/月/年） |

> **【待四信确认】** 系统类型标志、系统地址、部件类型的固定值需四信确认。当前按 PDF 定义实现（系统地址=0x00）。

### 6.4 批量指令支持

四信 API 返回的 `functionCommandList` 可能包含多条指令。若有多条：
- 按 `seq` 顺序逐条下发
- 每条之间间隔 `awaitTime` 毫秒
- 每条独立组装 Type 0x81 帧
- **批量指令异步关联策略**：仅将最后一条指令的业务流水号注册到 `PendingRequestManager`，设备回复的 TLD-133 即代表整批指令的最终执行结果

---

## 7. 远程应答解析（Type 0x85 / TLD-133 / cmd=05）

### 7.1 数据结构（PDF 表 8.2.1.16.1）

| 位置 | 内容 | 类型 | 长度 | 说明 |
|------|------|------|------|------|
| 0 | 系统类型标志 | BYTE | 1 | - |
| 1 | 系统地址 | BYTE | 1 | 0x00，固定值 |
| 2 | 部件类型 | BYTE | 1 | - |
| 3 | 部件地址 | DWORD | 4 | 小端模式，设备编码 |
| 7 | 指令状态 | BYTE | 1 | 见下表 |
| 8 | 应答长度 | WORD | 2 | 小端模式 |
| 10 | 应答内容 | STRING | n | GB18030 编码 |

### 7.2 指令状态定义（PDF 表 8.2.1.16.2）

| 位 | 含义 |
|----|------|
| Bit7 | 执行结果指示位：1=成功，0=失败 |
| Bit6 | 执行状态：1=执行中，0=非执行中 |
| Bit5~0 | 执行超时时长，单位分钟（最大 63） |

### 7.3 处理逻辑

```java
int status = buf.get() & 0xFF;
boolean success = (status & 0x80) != 0;
boolean executing = (status & 0x40) != 0;
int timeoutMin = status & 0x3F;

// 通过业务流水号找到原始请求上下文
int serialNumber = pkg.getSerialNumber();
PendingRequest req = pendingRequestManager.remove(serialNumber);
if (req != null) {
    // 构建服务调用结果
    Map<String, Object> output = new LinkedHashMap<>();
    output.put("success", success ? 1 : 0);
    output.put("result_code", status);
    output.put("result_desc", success ? "执行成功" : "执行失败");
    
    // 回传给平台
    report(InvokeReply.builder()
        .productKey(req.getProductKey())
        .deviceName(req.getDeviceName())
        .invokeId(req.getInvokeId())
        .outputData(output)
        .build());
}
```

---

## 8. 异步服务调用关联机制

### 8.1 问题背景

现有 `serviceInvoke` 是**即发即走**模式：

```java
case "syncTime": {
    sendControl(dn, TYPE_DOWN_SYNC_TIME, ts);
    break;  // ← 没有等待设备应答
}
```

但远程控制需要**异步等待**设备执行结果：

```
平台调用 remote_control
  └── serviceInvoke() 下发 Type 0x81 (流水号=SN001)
      └── 设备回复 cmd=03（确认收到）← 仅表示收到
      └── 设备执行完成后回复 cmd=05（TLD-133）← 这才是结果
          └── 需要将结果回传给平台
```

**缺失**：`cmd=05` 与原始 `serviceInvoke` 之间的关联机制。

### 8.2 设计方案

引入 `PendingRequestManager` 统一管理待响应请求：

```java
@Component
public class PendingRequestManager {
    // 业务流水号 → 请求上下文
    private final Map<Integer, PendingRequest> pendingMap = new ConcurrentHashMap<>();
    
    public void put(int serialNumber, PendingRequest request) { ... }
    public PendingRequest remove(int serialNumber) { ... }
    public void timeoutCheck() { ... }  // 定时清理超时请求
}

@Data
public class PendingRequest {
    private String invokeId;      // 平台服务调用ID
    private String productKey;    // 设备 productKey
    private String deviceName;    // 设备 deviceName
    private int serialNumber;     // 业务流水号（关联键）
    private String serviceName;   // remote_control
    private long sendTime;        // 发送时间
}
```

### 8.3 远程控制全流程（含异步关联）

```
平台调用 remote_control (invokeId=INV-001)
  └── serviceInvoke(action)
      ├── SN = nextSerial()
      ├── pendingRequestManager.put(SN, PendingRequest(
      │       invokeId=action.getInvokeId(),
      │       productKey=pk, deviceName=dn,
      │       serialNumber=SN, serviceName="remote_control",
      │       sendTime=System.currentTimeMillis()
      │   ))
      ├── sendControl(device, 0x81, appData)  // 帧中携带 SN
      └── 启动超时定时器（30秒）

设备回复 cmd=03 (SN)
  └── 仅日志记录"已确认"

设备回复 cmd=05 (SN, TLD-133)
  └── processPacket()
      └── handleRemoteResponse()
          ├── req = pendingRequestManager.remove(SN)
          ├── 解析 TLD-133 → success/result_code/result_desc
          └── report(InvokeReply.builder()
                  .invokeId(req.getInvokeId())
                  .outputData(output)
                  .build())

超时（30秒未收到 cmd=05）
  └── pendingRequestManager.timeoutCheck()
      ├── req = pendingMap.remove(SN)
      └── report(InvokeReply.builder()
              .invokeId(req.getInvokeId())
              .outputData({success:0, result_desc:"执行超时"})
              .build())
```

### 8.4 CMD_CONFIRM（cmd=03）处理增强

现有代码对 `CMD_CONFIRM` 仅打印日志。增强后：

```java
case FourfaithGbtConstants.CMD_CONFIRM:
    int confirmSn = pkg.getSerialNumber();
    PendingRequest req = pendingRequestManager.get(confirmSn);
    if (req != null) {
        log.info("[GBT26875] 收到确认: 设备={}, 流水号={}, 服务={}",
            deviceAddr, confirmSn, req.getServiceName());
        // 可选：更新请求状态为"已确认"
    }
    break;
```

### 8.5 影响范围

| 文件 | 改动 |
|------|------|
| `FourfaithGbtTcpComponent.java` | `serviceInvoke` 增加请求缓存；`processPacket` CMD_CONFIRM/CMD_RESPONSE 增强关联逻辑 |
| `PendingRequestManager.java` | 新增：请求上下文缓存 + 超时清理 |
| `PendingRequest.java` | 新增：请求上下文数据模型 |

---

## 9. 配置项与物模型变更

### 9.1 配置项扩展（`FourfaithGbtTcpConfig`）

```json
{
  "host": "0.0.0.0",
  "port": 8888,
  "centerAddr": "020000000001",
  "allowAutoRegister": false,
  "fallbackProductKey": "",

  "fourfaithApiUrl": "http://122.112.139.158:25290",
  "fourfaithApiAppId": "从四信商务申请",
  "fourfaithApiAppSecret": "从四信商务申请",
  "fourfaithApiTimeoutMs": 10000,

  "brandCode": "QIN_NIAO",
  "model": "QINNIAO",
  "version": "1.0"
}
```

| 新增字段 | 必填 | 说明 |
|---------|------|------|
| `fourfaithApiUrl` | 是 | 四信 API 基础地址 |
| `fourfaithApiAppId` | 是 | API 账号 |
| `fourfaithApiAppSecret` | 是 | API 密钥 |
| `fourfaithApiTimeoutMs` | 否 | HTTP 超时，默认 10000ms |
| `brandCode` | 是 | 设备品牌编码 |
| `model` | 是 | 设备型号 |
| `version` | 是 | 设备版本 |

### 9.2 父设备物模型变更（`gbt26875_thing_model.json`）

**新增属性**（10 个）：

```json
[
  { "identifier": "prt_channel", "name": "PRT通道号", "dataType": { "type": "int32" }, "accessMode": "r" },
  { "identifier": "prt_host_no", "name": "PRT消控主机编号", "dataType": { "type": "int32" }, "accessMode": "r" },
  { "identifier": "prt_alarm_type", "name": "PRT报警类型", "dataType": { "type": "int32" }, "accessMode": "r" },
  { "identifier": "prt_device_name", "name": "PRT报警设备名称", "dataType": { "type": "text", "specs": { "length": "64" } }, "accessMode": "r" },
  { "identifier": "prt_device_location", "name": "PRT报警设备位置", "dataType": { "type": "text", "specs": { "length": "128" } }, "accessMode": "r" },
  { "identifier": "prt_alarm_info", "name": "PRT报警信息", "dataType": { "type": "text", "specs": { "length": "64" } }, "accessMode": "r" },
  { "identifier": "prt_time", "name": "PRT时间", "dataType": { "type": "date" }, "accessMode": "r" },
  { "identifier": "crt_channel", "name": "CRT通道号", "dataType": { "type": "int32" }, "accessMode": "r" },
  { "identifier": "crt_host_no", "name": "CRT消控主机编号", "dataType": { "type": "int32" }, "accessMode": "r" },
  { "identifier": "crt_event_json", "name": "CRT事件JSON", "dataType": { "type": "text", "specs": { "length": "256" } }, "accessMode": "r" }
]
```

**新增事件**（2 个）：

```json
[
  {
    "identifier": "prt_alarm_event",
    "name": "PRT打印报警事件",
    "outputData": [
      { "identifier": "alarm_type", "name": "报警类型", "dataType": { "type": "int32" } },
      { "identifier": "device_name", "name": "设备名称", "dataType": { "type": "text" } },
      { "identifier": "location", "name": "设备位置", "dataType": { "type": "text" } },
      { "identifier": "info", "name": "报警信息", "dataType": { "type": "text" } },
      { "identifier": "event_time", "name": "事件时间", "dataType": { "type": "date" } },
      { "identifier": "source", "name": "来源", "dataType": { "type": "text" } }
    ]
  },
  {
    "identifier": "crt_alarm_event",
    "name": "CRT图形显示报警事件",
    "outputData": [
      { "identifier": "alarm_type", "name": "报警类型", "dataType": { "type": "int32" } },
      { "identifier": "loop_no", "name": "回路号", "dataType": { "type": "text" } },
      { "identifier": "component_no", "name": "部件号", "dataType": { "type": "text" } },
      { "identifier": "event_json", "name": "事件原始JSON", "dataType": { "type": "text" } },
      { "identifier": "event_time", "name": "事件时间", "dataType": { "type": "date" } },
      { "identifier": "source", "name": "来源", "dataType": { "type": "text" } },
      { "identifier": "linked_device_name", "name": "关联部件DN", "dataType": { "type": "text" } }
    ]
  }
]
```

**新增服务**（1 个）：

```json
{
  "identifier": "remote_control",
  "name": "远程控制下发",
  "inputData": [
    { "identifier": "control_type", "name": "控制类型", "dataType": { "type": "enum", "specs": { "10": "复位", "20": "消音", "30": "开关", "40": "总线开关", "41": "多线开关" } }, "required": true },
    { "identifier": "host_no", "name": "主机号", "dataType": { "type": "text" }, "required": true },
    { "identifier": "circuit_no", "name": "回路号", "dataType": { "type": "text" }, "required": true },
    { "identifier": "address_no", "name": "地址号", "dataType": { "type": "text" }, "required": true },
    { "identifier": "switch_type", "name": "开关类型", "dataType": { "type": "enum", "specs": { "0": "关", "1": "开" } }, "required": false }
  ],
  "outputData": [
    { "identifier": "success", "name": "是否成功", "dataType": { "type": "enum", "specs": { "0": "失败", "1": "成功" } } },
    { "identifier": "result_code", "name": "结果码", "dataType": { "type": "int32" } },
    { "identifier": "result_desc", "name": "结果描述", "dataType": { "type": "text" } }
  ]
}
```

### 9.3 TDengine 行长度验算

新增属性对行长度影响：

```
新增: 3个 text(64→NCHAR64) + 1个 text(128→NCHAR128) + 1个 text(256→NCHAR256)
      + 5个 int32(各4字节) + 2个 date(各64字节)

新增长度: 64 + 128 + 256 + 5×4 + 2×64 = 596 字节
原有长度: ~1088 字节
总计: ~1684 字节 << 65531（安全）
```

### 9.4 部件子设备物模型（独立）

四信模块已与国标主干分离，部件子设备使用**独立物模型**，不共用 `gbt26875_component_thing_model.json`。

**文件**：`doc/fourfaith_component_thing_model.json`

**基于国标部件物模型（16 属性）新增 5 个点位属性**：

```json
[
  { "identifier": "point_host_no", "name": "主机号", "dataType": { "type": "text", "specs": { "length": "16" } }, "accessMode": "r" },
  { "identifier": "point_circuit_no", "name": "回路号", "dataType": { "type": "text", "specs": { "length": "16" } }, "accessMode": "r" },
  { "identifier": "point_address_no", "name": "地址号", "dataType": { "type": "text", "specs": { "length": "16" } }, "accessMode": "r" },
  { "identifier": "point_name", "name": "点位名称", "dataType": { "type": "text", "specs": { "length": "64" } }, "accessMode": "r" },
  { "identifier": "point_location", "name": "点位位置", "dataType": { "type": "text", "specs": { "length": "128" } }, "accessMode": "r" }
]
```

> 字段名以实际 Excel 为准，导入时做字段映射。

**四信部件子设备 productKey**：`{父设备productKey}sub`

---

## 10. 数据流设计

### 10.1 PRT 数据流

```
设备上传 Type 0x09 (cmd=02)
  └── FourfaithGbtDataReader → FourfaithGbtDataDecoder
      └── FourfaithGbtDataParser.parsePrt()
          └── Map<String, Object> (prt_* 字段)
              └── FourfaithGbtTcpComponent.handleDataUpload()
                  ├── 属性上报 → 父设备 (prt_channel, prt_device_name, ...)
                  └── 若 prt_alarm_type != 0
                      └── 触发 prt_alarm_event
```

### 10.2 CRT 数据流

```
设备上传 Type 0x0A (cmd=02)
  └── FourfaithGbtDataReader → FourfaithGbtDataDecoder
      └── FourfaithGbtDataParser.parseCrt()
          └── Map<String, Object> (crt_* 字段 + crt_event_json)
              └── FourfaithGbtTcpComponent.handleDataUpload()
                  ├── 属性上报 → 父设备 (crt_channel, crt_event_json)
                  ├── 解析字段2提取 hostNo + circuitNo + addressNo
                  │     └── addressNo 按 16 进制字符串解析为 compAddr：
                  │         例：`"2CD"` → `Integer.parseInt("2CD", 16)` = `717`
                  │     └── 查询该父设备下的子设备列表
                  │         ├── 命中（point_circuit_no + point_address_no 匹配）
                  │         │     └── 给该子设备更新属性 / 触发事件
                  │         └── 未命中
                  │             └── 自动创建子设备（deviceName 按规则生成，compAddr = 上述解析值）
                  │             └── 回填属性：point_host_no / point_circuit_no / point_address_no
                  └── 触发 crt_alarm_event
                      └── linked_device_name = 命中/新建时填充 DN
```

### 10.3 远程控制数据流

```
平台调用 remote_control 服务 (invokeId=INV-001)
  └── FourfaithGbtTcpComponent.serviceInvoke()
      ├── SN = nextSerial()
      ├── pendingRequestManager.put(SN, PendingRequest(invokeId=INV-001, ...))
      └── FourfaithRemoteControlClient
          ├── createHardwareCommand(API)
          │     └── 若 API 超时或返回 code != 0
          │         └── 立即 report(InvokeReply) 回传失败结果，流程终止
          │     └── 返回 commandCode (hex)
          ├── hexStringToBytes(commandCode)
          ├── buildType81AppData(sysType, sysAddr, compType, compAddr, commandBytes)
          └── sendControl(deviceAddr, 0x81, appData)  // 帧中携带 SN
                └── 设备收到
                    ├── 回复 cmd=03（收到确认，流水号=SN）
                    │     └── 日志记录，可选更新请求状态
                    └── 执行完成后回复 cmd=05（TLD-133，流水号=SN）
                        └── FourfaithRemoteResponseParser.parse()
                            ├── req = pendingRequestManager.remove(SN)
                            ├── 解析结果 → success/result_code/result_desc
                            └── report(InvokeReply(invokeId=req.invokeId, outputData))

超时（30秒未收到 cmd=05）
  └── pendingRequestManager.timeoutCheck()
      └── report(InvokeReply(invokeId=INV-001, {success:0, result_desc:"执行超时"}))
```

---

## 11. 错误处理

| 场景 | 处理策略 |
|------|---------|
| 心跳包解析 | 更新活跃时间，回复心跳应答，不触发注册逻辑 |
| PRT/CRT 解析失败（长度不足） | 记录 warn 日志，丢弃该帧，不影响后续数据处理 |
| CRT 子设备匹配失败 | 自动创建子设备，回填 point_host_no / point_circuit_no / point_address_no |
| 四信 API 调用超时 | 返回 `success=0, result_desc="获取控制指令超时"` |
| 四信 API 返回错误（code != 0） | 透传 message 到 result_desc |
| 设备离线时下发控制 | 返回 `success=0, result_desc="设备离线"` |
| 设备否认（cmd=06） | 返回 `success=0, result_desc="设备拒绝执行"` |
| TLD-133 执行结果 bit7=0 | 返回 `success=0, result_desc="执行失败"` |
| 远程控制超时（30秒无 cmd=05） | `PendingRequestManager` 超时清理，回传 `success=0, result_desc="执行超时"` |
| 收到未知流水号的 cmd=05 | 记录 warn 日志，可能请求已超时或被清理 |

---

## 12. 测试策略

### 12.1 单元测试

| 测试项 | 方法 |
|--------|------|
| 心跳包识别 | 构造 12 字节心跳包，验证 Reader 正确识别并回调 |
| 心跳包应答 | 验证心跳应答格式（源地址全0，通讯模块0，系统类型0） |
| PRT 解析 | 用文档样例字节流断言各字段值 |
| CRT 解析 | 用文档样例断言类型个数、事件字段、表示类型判断 |
| CRT 子设备匹配 | Mock 子设备属性，验证命中/未命中流程 |
| Type 0x81 组装 | 断言输出字节流与文档示例一致 |
| TLD-133 解析 | 构造成功/失败/执行中三种状态断言 |
| PendingRequest 缓存 | 验证 put/remove/timeout 生命周期 |
| 异步关联 | 模拟下发→cmd=03→cmd=05 全流程，验证 InvokeReply 正确回传 |

### 12.2 集成测试

| 测试项 | 方法 |
|--------|------|
| 心跳包在线保持 | TcpTestClient 定时发送心跳包，验证设备不被误判离线 |
| PRT 数据流 | TcpTestClient 发送 PRT 样例，验证属性+事件 |
| CRT 关联流程 | 预置子设备带点位属性，验证 CRT 报警正确关联 |
| 远程控制全流程 | Mock 四信 API + TcpTestClient，验证下发→确认→结果→InvokeReply |
| TDengine 建表 | 启动组件验证超级表创建成功 |

### 12.3 借测验收标准

1. 四信设备上线后，心跳包正常收发，设备不被误判离线
2. 能正常接收并解析 PRT/CRT 数据
3. 值守端能看到 `prt_device_name`、`prt_alarm_info`、`crt_event_json`
4. PRT/CRT 报警时，分别触发 `prt_alarm_event` / `crt_alarm_event`
5. CRT 报警能正确关联到部件子设备（Excel 导入或自动创建）
6. 值守端发起 `remote_control`，设备能收到并执行，结果通过 `InvokeReply` 正确回传

---

## 13. 风险与待确认项

### 13.1 待确认项清单

| 序号 | 待确认项 | 作用/影响 | 当前假设 | 确认后动作 |
|------|---------|----------|---------|-----------|
| 1 | **TLD-129 固定字段值** | Type 0x81 远程控制帧中 `系统类型标志`、`系统地址`、`部件类型` 三个字段的固定值直接决定设备能否识别并执行控制指令。值错误会导致远程控制下发后设备无响应或拒绝执行。 | 按 PDF 文档定义：`系统地址=0x00`；docx 示例中 `系统类型标志=0x88`、`部件类型=0x83`（详见 6.3 节） | 根据四信反馈修正 `FourfaithGbtDataEncoder.buildType81AppData()` 中的固定字节 |
| 2 | **四信 API 账号** | 远程控制功能依赖四信云平台 `createHardwareCommand` 接口获取指令码。无账号则无法调用 API，远程控制功能完全不可用。 | 当前配置项预留了 `fourfaithApiAppId` / `fourfaithApiAppSecret`，但无实际值 | 向四信商务申请正式环境账号，填入 `application.yml` 或设备配置 |
| 3 | **控制类型扩展** | 控制类型（复位/消音/开关等）当前硬编码为 `10/20/30/40/41`。若四信后续新增控制类型，需改代码重新发布。 | 当前枚举值来自四信 API PDF 文档（见 6.2 节） | 确认为最终枚举值；若后续可能扩展，改为配置表或从 `getHardwareFunctionList` 接口动态获取 |
| 4 | **批量指令间隔** | 四信 API 返回的 `functionCommandList` 可能包含多条指令，需按 `awaitTime` 间隔逐条下发。间隔过短可能导致设备丢包，过长则影响响应速度。 | 当前代码已读取 `awaitTime` 字段，但默认值和实测有效性未知 | 借测时实测验证间隔合理性，必要时增加重试或状态检查机制 |
| 5 | **Excel 字段映射** | 点位表 Excel 导入时需将字段（如回路号、地址号、点位名称）映射到物模型属性。字段名不一致会导致导入失败或数据错位。 | 设计文档假设字段名为 `point_circuit_no`、`point_address_no`、`point_name`、`point_location`（见 5.3 节） | 拿到四信实际 Excel 后，确认表头字段名，调整导入 VO 的 `@ExcelProperty` 注解 |

### 13.2 文档中其他待确认标记

- **6.3 节**：`系统类型标志` 和 `部件类型` 标注为 **【待确认】**，docx 示例值可能与 PDF 定义不一致，需以四信最终答复为准。
- **6.3 节**：`系统地址` PDF 定义固定 `0x00`，但 docx 示例为 `0x02`，需确认以哪个为准。

---

## 14. 实施顺序

1. **Step 1**: 改造 `FourfaithGbtDataReader`（心跳包嗅探 + 心跳应答）
2. **Step 2**: 扩展 `FourfaithGbtDataParser`（Type 0x09 + 0x0A 解析）
3. **Step 3**: 新增 `FourfaithRemoteResponseParser`（TLD-133 解析）
4. **Step 4**: 新增 `FourfaithRemoteControlClient`（HTTP API 封装）
5. **Step 5**: 新增 `PendingRequestManager` + `PendingRequest`（异步关联）
6. **Step 6**: 扩展 `FourfaithGbtTcpComponent`（类型路由 + 异步服务调用 + 心跳处理）
7. **Step 7**: 扩展配置项和物模型（含独立 component 物模型）
8. **Step 8**: 编写单元测试和集成测试
9. **Step 9**: 借测验证（需四信提供 API 账号和点位表 Excel）
