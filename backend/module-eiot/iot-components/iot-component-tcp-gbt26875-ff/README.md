# GBT26875-TCP 模块

## 概览
- 模块位置：`module-eiot/iot-components/iot-component-gbt26875-tcp`
- 目标：实现符合 **GB/T 26875.3-2011《城市消防远程监控系统 第3部分：报警传输网络通信协议》** 的 TCP 通信接入
- 技术栈：Spring + Vert.x TCP Server
- 组件架构：`GbtTcpStarter` 启动 → `GbtTcpVerticle` 管理 TCP 服务 → `GbtTcpComponent` 处理协议与平台集成 → `parser/*` 编解码

## 协议说明 (GB/T 26875.3-2011)

### 数据包结构
```
@@(2B) | 业务流水号(2B,LE) | 协议版本(2B) | 时间标签(6B)
| 源地址(6B,LE) | 目的地址(6B,LE) | 数据长度(2B,LE)
| 命令字节(1B) | 应用数据(≤1024B) | 校验和(1B) | ##(2B)
```
- 启动符：`@@` (0x40 0x40)
- 结束符：`##` (0x23 0x23)
- 多字节字段：低字节在前 (Little-Endian)
- 校验和：第3~27字节及应用数据单元的算术和，取低8位
- 最小包：30字节（无应用数据时）

### 命令字节 (表2)
| 命令 | 值 | 说明 |
|------|---|------|
| 控制 | 1 | 时间同步、初始化等 |
| 发送/确认 | 2 | 设备上传火警及运行状态信息 |
| 确认 | 3 | 确认回复 |
| 请求 | 4 | 查询设备状态 |
| 应答 | 5 | 返回查询信息 |
| 否认 | 6 | 否认回复 |

### 类型标志 (表3)
**上行（设备 → 监控中心）：**
| 类型标志 | 说明 |
|---------|------|
| 1 | 建筑消防设施系统状态 |
| 2 | 建筑消防设施部件运行状态 |
| 3 | 建筑消防设施部件模拟量值 |
| 4 | 建筑消防设施操作信息 |
| 5 | 建筑消防设施软件版本 |
| 6 | 建筑消防设施系统说明描述 |
| 7 | 建筑消防设施部件配置描述 |
| 8 | 建筑消防设施系统时间 |
| 21 | 用户信息传输装置运行状态 |
| 24 | 用户信息传输装置操作信息 |
| 25 | 用户信息传输装置软件版本 |
| 26 | 用户信息传输装置配置描述 |
| 28 | 用户信息传输装置系统时间 |

**下行（监控中心 → 设备）：**
| 类型标志 | 说明 |
|---------|------|
| 61~68 | 查询建筑消防设施各项信息 |
| 81~88 | 查询用户信息传输装置各项信息 |
| 89 | 初始化用户信息传输装置 |
| 90 | 同步用户信息传输装置时间 |
| 91 | 检查命令 |

## 关键代码

### 协议编解码 (`parser/` 包)
- `GbtConstants.java` — 协议常量（命令字节、类型标志、系统类型、模拟量类型等）
- `GbtDataPackage.java` — 标准数据包模型，含地址转换工具方法
- `GbtDataEncoder.java` — 编码器：GbtDataPackage → 二进制帧（含校验和计算）
- `GbtDataDecoder.java` — 解码器：二进制帧 → GbtDataPackage（含完整校验）
- `GbtDataReader.java` — 帧解析器：从 TCP 流中提取完整 `@@...##` 帧（四阶段状态机）
- `GbtDataParser.java` — 应用数据单元解析器：按类型标志将二进制数据解析为 `Map<String,Object>`

### 服务端处理 (`service/` 包)
- `GbtTcpComponent.java` — 核心组件
  - 连接处理与帧解析
  - 上行数据解析与平台上报（PropertyReport / EventReport）
  - 下行指令编码与发送（控制/查询/确认/否认）
  - 设备预注册检查与自动注册
  - 离线检测
- `GbtTcpVerticle.java` — Vert.x TCP 服务器生命周期管理
- `GbtTcpStarter.java` — Spring 启动入口

## 配置

`GbtTcpConfig` 包含以下字段：
```json
{
  "host": "0.0.0.0",
  "port": 8888,
  "centerAddr": "020000000001",
  "allowAutoRegister": false,
  "fallbackProductKey": "可选的备用产品Key"
}
```
- `host` / `port`：TCP 服务监听地址
- `centerAddr`：监控中心地址（12位十六进制，对应6字节），作为下发报文的源地址
- `allowAutoRegister`：是否允许自动注册设备（默认 false）
  - `false`：仅允许已在平台预注册的设备接入（**推荐用于多租户场景**）
  - `true`：允许未知设备自动注册到 `fallbackProductKey`
- `fallbackProductKey`：备用产品Key，仅在 `allowAutoRegister=true` 时生效

### 多租户设备接入流程

**推荐模式：设备预注册（allowAutoRegister=false）**

1. **在平台侧预先创建设备**
   - 为每个租户创建对应的产品（Product）
   - 在产品下创建设备，记录 `deviceName`（即设备地址，如 `010000000001`）
   - 设备会自动关联到对应租户和产品

2. **设备接入时自动识别**
   - 设备通过 TCP 连接到协议组件
   - 协议组件通过 `deviceName` 查询平台，获取设备所属的 `productKey` 和 `tenantId`
   - 使用查询到的信息进行数据上报和状态管理

3. **未注册设备被拒绝**
   - 如果设备未在平台预注册，连接会被拒绝
   - 日志会提示：`设备 XXX 未预注册且不允许自动注册,拒绝接入。请先在平台创建设备`

**兼容模式：自动注册（allowAutoRegister=true）**

适用于单租户或测试环境，所有设备自动注册到同一个产品。

## 通信流程

1. **设备连接** → TCP 建立连接
2. **设备上报数据** (command=2) → 服务端解析应用数据、上报平台、回复确认(command=3)
3. **平台下发属性设置** → 服务端编码为 GBT 控制命令(command=1) 或查询请求(command=4) 发送到设备
4. **设备确认/应答** (command=3/5) → 服务端处理
5. **离线检测** → 超过 120 秒无数据视为离线

### 属性设置 (propertySet) 参数约定
```json
{
  "gbtCommand": 4,
  "gbtTypeFlag": 61,
  "gbtBody": ""
}
```
- `gbtCommand`: 1=控制, 4=查询（默认4）
- `gbtTypeFlag`: 类型标志
- `gbtBody`: 可选，附加数据十六进制

### 服务调用 (serviceInvoke) 支持的服务
| 服务名 | 说明 |
|--------|------|
| `syncTime` | 同步设备时间 (type=90) |
| `initDevice` | 初始化设备 (type=89) |
| `inspect` | 检查命令 (type=91) |
| `query` | 通用查询，params.typeFlag 指定类型 |

### 属性获取 (propertyGet) 支持的 key
- `systemStatus` / `componentStatus` / `analogValue` / `operationInfo`
- `softwareVersion` / `systemConfig` / `componentConfig` / `systemTime`
- `deviceStatus` / `deviceOperation` / `deviceVersion` / `deviceConfig` / `deviceTime`

## 与平台集成

- 设备标识：源地址十六进制字符串作为 `deviceName`（如 `010000000001`）
- 数据上报：每条信息体解析为 `Map<String,Object>` 通过 `PropertyReport` 上报
- 火警事件：部件状态 bit0=1 时额外触发 `EventReport(name="fireAlarm")`
- 设备注册：
  - **多租户场景**：设备需预先在平台创建，协议组件通过查询识别租户和产品
  - **单租户场景**：可开启 `allowAutoRegister` 自动注册到备用产品
- 离线判定：心跳超时 + TCP 连接断开

## 客户端测试

测试客户端位于 `src/test/java/.../client/TcpTestClient.java`

参数：`host port [deviceAddr] [centerAddr] [intervalSec]`
- `deviceAddr`：设备地址（12位十六进制），默认 `010000000001`
- `centerAddr`：中心地址（12位十六进制），默认 `020000000001`
- `intervalSec`：上报间隔（秒），默认 `5`

行为：
- 首次发送用户信息传输装置运行状态 (Type 21) 完成接入
- 每隔 `intervalSec` 轮流发送系统状态(Type 1)、部件正常状态(Type 2)、部件火警(Type 2)、模拟量温度(Type 3)
- 接收并打印监控中心响应
