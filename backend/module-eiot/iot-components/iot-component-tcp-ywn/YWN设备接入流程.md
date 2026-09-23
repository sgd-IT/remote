# YWN TCP V27 设备完整接入流程

## 一、接入流程总览

```
Step 1: 创建产品 (Product)         → 获取 productKey
Step 2: 配置物模型 (ThingModel)     → 定义属性/事件/服务
Step 3: 配置组件 (Component)        → 注册 ywn-tcp 组件，设置端口
Step 4: 注册设备 (Device)           → 预注册或自动注册
Step 5: 设备连接验证                 → TCP 连接，心跳，上线
Step 6: 数据验证                     → 属性/事件/状态前端可见
```

---

## 二、Step 1: 创建产品

### 2.1 通过前端创建

进入 **IoT平台 → 产品管理 → 新建产品**：

| 字段 | 值 | 说明 |
|------|-----|------|
| 产品名称 | YWN控制器 | 自定义 |
| 产品分类 | 消防设备 | 选择对应分类 |
| 节点类型 | 直连设备 | nodeType=2 |
| 协议代码 | ywn-tcp | 与组件 type 一致 |
| 保活时间 | 360 | 秒（YWN心跳周期3分钟，建议保活6分钟=360秒） |
| 是否透传 | 否 | false |
| 定位方式 | 无定位 | 0 |

创建后系统自动生成 `productKey`（16位随机字符串），**记录此值**。

### 2.2 通过 API 创建

```
POST /admin-api/eiot/product/create
```

```json
{
  "name": "YWN控制器",
  "categoryId": 1,
  "productKey": "自动生成或自定义",
  "status": 0,
  "nodeType": 2,
  "protocolCode": "ywn-tcp",
  "keepAliveTime": 360,
  "transparent": false,
  "locateType": 0
}
```

---

## 三、Step 2: 配置物模型

### 3.1 物模型总览

物模型与 `TcpYwnComponent` 代码中的 `report()` 调用**严格一一对应**：

| 类型 | 数量 | 来源代码 | 标识符 |
|------|------|---------|--------|
| 属性 (Property) | 18 个 | `PropertyReport.put()` | event_type_code, event_code, ... |
| 事件 (Event) | 5 个 | `EventReport.name()` | fire_alarm_event, fault_event, ... |
| 服务 (Service) | 15 个 | `serviceInvoke switch` | reset, silence, bus_start, ... |

### 3.2 完整物模型 JSON

进入 **产品管理 → 选择产品 → 物模型 → 编辑**，粘贴以下 JSON：

```json
{
  "properties": [
    {
      "identifier": "event_type_code",
      "name": "事件类型码",
      "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}},
      "accessMode": "r",
      "description": "YWN事件类型码(0x80帧上报)"
    },
    {
      "identifier": "event_code",
      "name": "事件代码",
      "dataType": {"type": "int32", "specs": {"min": 0, "max": 65535}},
      "accessMode": "r",
      "description": "YWN事件代码(如0x01=火警,0x50=故障)"
    },
    {
      "identifier": "event_name",
      "name": "事件名称",
      "dataType": {"type": "text", "specs": {"length": "64"}},
      "accessMode": "r",
      "description": "事件代码对应的中文描述"
    },
    {
      "identifier": "machine_no",
      "name": "机号",
      "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}},
      "accessMode": "r",
      "description": "控制器机号(0=不显示)"
    },
    {
      "identifier": "loop_no",
      "name": "回路号",
      "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}},
      "accessMode": "r",
      "description": "总线回路号(0=不显示)"
    },
    {
      "identifier": "address_no",
      "name": "地址号",
      "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}},
      "accessMode": "r",
      "description": "总线设备地址号(0=不显示)"
    },
    {
      "identifier": "channel_no",
      "name": "通道号",
      "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}},
      "accessMode": "r",
      "description": "通道号(0=不显示)"
    },
    {
      "identifier": "device_type",
      "name": "设备类型",
      "dataType": {"type": "int32", "specs": {"min": 0, "max": 65535}},
      "accessMode": "r",
      "description": "设备类型(0=不显示)"
    },
    {
      "identifier": "product_type",
      "name": "产品类型",
      "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}},
      "accessMode": "r",
      "description": "产品类型(0=不显示)"
    },
    {
      "identifier": "channel_type",
      "name": "通道类型",
      "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}},
      "accessMode": "r",
      "description": "通道类型(0=不显示)"
    },
    {
      "identifier": "description",
      "name": "事件描述",
      "dataType": {"type": "text", "specs": {"length": "128"}},
      "accessMode": "r",
      "description": "事件说明文字(GBK编码,最多20个汉字)"
    },
    {
      "identifier": "alarm_value_float",
      "name": "报警值(浮点)",
      "dataType": {"type": "float", "specs": {"min": "-999999", "max": "999999"}},
      "accessMode": "r",
      "description": "IEEE754浮点报警值(-1=不显示)"
    },
    {
      "identifier": "alarm_value_int",
      "name": "报警值(整型)",
      "dataType": {"type": "int32", "specs": {"min": "-1", "max": "32767"}},
      "accessMode": "r",
      "description": "整型报警值(实际值×100, -1=不显示)"
    },
    {
      "identifier": "event_time",
      "name": "事件时间",
      "dataType": {"type": "date"},
      "accessMode": "r",
      "description": "事件发生时间(yyyy-MM-dd HH:mm:ss)"
    },
    {
      "identifier": "iccid",
      "name": "ICCID",
      "dataType": {"type": "text", "specs": {"length": "20"}},
      "accessMode": "r",
      "description": "SIM卡ICCID号(以太网设备发'0')"
    },
    {
      "identifier": "imei",
      "name": "IMEI",
      "dataType": {"type": "text", "specs": {"length": "15"}},
      "accessMode": "r",
      "description": "设备IMEI号(以太网设备发'0')"
    },
    {
      "identifier": "hardware_version",
      "name": "硬件版本",
      "dataType": {"type": "text", "specs": {"length": "4"}},
      "accessMode": "r",
      "description": "硬件版本号(如V1.0)"
    },
    {
      "identifier": "software_version",
      "name": "软件版本",
      "dataType": {"type": "text", "specs": {"length": "4"}},
      "accessMode": "r",
      "description": "软件版本号(如V1.2)"
    }
  ],
  "events": [
    {
      "identifier": "fire_alarm_event",
      "name": "火警事件",
      "outputData": [
        {"identifier": "event_code", "name": "事件代码", "dataType": {"type": "int32"}},
        {"identifier": "event_name", "name": "事件名称", "dataType": {"type": "text"}},
        {"identifier": "machine_no", "name": "机号", "dataType": {"type": "int32"}},
        {"identifier": "loop_no", "name": "回路号", "dataType": {"type": "int32"}},
        {"identifier": "address_no", "name": "地址号", "dataType": {"type": "int32"}},
        {"identifier": "description", "name": "描述", "dataType": {"type": "text"}},
        {"identifier": "event_time", "name": "事件时间", "dataType": {"type": "date"}}
      ]
    },
    {
      "identifier": "fault_event",
      "name": "故障事件",
      "outputData": [
        {"identifier": "event_code", "name": "事件代码", "dataType": {"type": "int32"}},
        {"identifier": "event_name", "name": "事件名称", "dataType": {"type": "text"}},
        {"identifier": "machine_no", "name": "机号", "dataType": {"type": "int32"}},
        {"identifier": "loop_no", "name": "回路号", "dataType": {"type": "int32"}},
        {"identifier": "address_no", "name": "地址号", "dataType": {"type": "int32"}},
        {"identifier": "description", "name": "描述", "dataType": {"type": "text"}},
        {"identifier": "event_time", "name": "事件时间", "dataType": {"type": "date"}}
      ]
    },
    {
      "identifier": "feedback_event",
      "name": "反馈事件",
      "outputData": [
        {"identifier": "event_code", "name": "事件代码", "dataType": {"type": "int32"}},
        {"identifier": "event_name", "name": "事件名称", "dataType": {"type": "text"}},
        {"identifier": "machine_no", "name": "机号", "dataType": {"type": "int32"}},
        {"identifier": "loop_no", "name": "回路号", "dataType": {"type": "int32"}},
        {"identifier": "address_no", "name": "地址号", "dataType": {"type": "int32"}}
      ]
    },
    {
      "identifier": "controller_fault_event",
      "name": "控制器故障",
      "outputData": [
        {"identifier": "fault_type", "name": "故障类型", "dataType": {"type": "text"}},
        {"identifier": "event_time", "name": "事件时间", "dataType": {"type": "date"}}
      ]
    },
    {
      "identifier": "controller_fault_recovery_event",
      "name": "控制器故障恢复",
      "outputData": [
        {"identifier": "recovery_type", "name": "恢复类型", "dataType": {"type": "text"}},
        {"identifier": "event_time", "name": "事件时间", "dataType": {"type": "date"}}
      ]
    }
  ],
  "services": [
    {
      "identifier": "reset",
      "name": "设备复位",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "silence",
      "name": "消音",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "bus_start",
      "name": "启动总线设备",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false},
        {"identifier": "loopNo", "name": "回路号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false},
        {"identifier": "addressNo", "name": "地址号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "bus_stop",
      "name": "停止总线设备",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false},
        {"identifier": "loopNo", "name": "回路号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false},
        {"identifier": "addressNo", "name": "地址号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "shield",
      "name": "屏蔽",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false},
        {"identifier": "loopNo", "name": "回路号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false},
        {"identifier": "addressNo", "name": "地址号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "cancel_shield",
      "name": "取消屏蔽",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false},
        {"identifier": "loopNo", "name": "回路号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false},
        {"identifier": "addressNo", "name": "地址号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "manual_switch",
      "name": "手自动转换",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false},
        {"identifier": "mode", "name": "模式", "dataType": {"type": "enum", "specs": {"2": "手动", "3": "自动"}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "multi_line_start",
      "name": "多线盘启动",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false},
        {"identifier": "loopNo", "name": "盘号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false},
        {"identifier": "addressNo", "name": "路号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "multi_line_stop",
      "name": "多线盘停止",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false},
        {"identifier": "loopNo", "name": "盘号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false},
        {"identifier": "addressNo", "name": "路号", "dataType": {"type": "int32", "specs": {"min": 0, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "auto_allow",
      "name": "自动允许",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "auto_prohibit",
      "name": "自动禁止",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "manual_allow",
      "name": "手动允许",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "manual_prohibit",
      "name": "手动禁止",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "auto_manual_allow",
      "name": "自动手动允许",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    },
    {
      "identifier": "auto_manual_prohibit",
      "name": "自动手动禁止",
      "inputData": [
        {"identifier": "machineNo", "name": "机号", "dataType": {"type": "int32", "specs": {"min": 1, "max": 255}}, "required": false}
      ],
      "outputData": [
        {"identifier": "result", "name": "执行结果", "dataType": {"type": "text"}}
      ]
    }
  ]
}
```

### 3.3 物模型与代码对应关系

| 物模型类型 | 代码位置 | 标识符 | 数据流 |
|-----------|---------|--------|--------|
| 属性 | `handleEventUpload` → `PropertyReport.put("event_type_code", ...)` | event_type_code | 设备→平台 |
| 属性 | `handleDeviceInfoUpload` → `PropertyReport.put("iccid", ...)` | iccid | 设备→平台 |
| 事件 | `triggerAbnormalEvents` → `EventReport.name("fire_alarm_event")` | fire_alarm_event | 设备→平台 |
| 事件 | `handleControllerFault` → `EventReport.name("controller_fault_event")` | controller_fault_event | 设备→平台 |
| 服务 | `serviceInvoke` → `case "reset"` | reset | 平台→设备 |

---

## 四、Step 3: 配置组件

### 4.1 通过前端创建

进入 **系统管理 → 组件管理 → 新建组件**：

| 字段 | 值 |
|------|-----|
| 组件名称 | YWN控制器监控TCP协议V27组件 |
| 组件类型 | ywn-tcp |
| 状态 | 启用 (status=1) |
| 配置 | 见下方 JSON |

**配置 JSON：**

```json
{
  "host": "0.0.0.0",
  "port": 9200,
  "allowAutoRegister": false,
  "fallbackProductKey": ""
}
```

| 配置项 | 说明 |
|--------|------|
| `host` | 监听地址，`0.0.0.0` 表示监听所有网卡 |
| `port` | TCP 监听端口，不能与其他服务冲突 |
| `allowAutoRegister` | `false`=仅允许预注册设备；`true`=自动注册未知设备 |
| `fallbackProductKey` | 自动注册时使用的产品Key（仅 allowAutoRegister=true 时生效） |

### 4.2 通过 API 创建

```
POST /admin-api/eiot/component/create
```

```json
{
  "name": "YWN控制器监控TCP协议V27组件",
  "type": "ywn-tcp",
  "status": 1,
  "config": "{\"host\":\"0.0.0.0\",\"port\":9200,\"allowAutoRegister\":false,\"fallbackProductKey\":\"\"}"
}
```

> 组件创建后，`TcpYwnComponent.config()` 会在5秒内轮询到该记录，自动调用 `stateChange()` 启动 TCP Server。

### 4.3 自动注册模式配置（可选）

如果希望未知设备自动注册，修改配置：

```json
{
  "host": "0.0.0.0",
  "port": 9200,
  "allowAutoRegister": true,
  "fallbackProductKey": "Step1中获取的productKey"
}
```

---

## 五、Step 4: 注册设备

### 5.1 方式 A：预注册（推荐）

> 适用于 `allowAutoRegister=false` 模式

进入 **设备管理 → 新建设备**：

| 字段 | 值 | 说明 |
|------|-----|------|
| 产品 | YWN控制器 | 选择 Step 1 创建的产品 |
| 设备名称(dn) | 设备编号的十进制字符串 | 如 `"1"` 对应 YWN 协议设备编号 1 |
| 序列号 | 设备唯一序列号 | 自定义 |

> **dn 规则**：YWN 协议帧中设备编号是 2 字节小端整数（1-65535），`dn` 是该数字的十进制字符串形式。例如设备编号 1 → dn="1"，设备编号 256 → dn="256"。

### 5.2 方式 B：自动注册

> 适用于 `allowAutoRegister=true` 模式

无需手动创建设备。当设备首次发送任何帧（如心跳）时，组件会自动使用 `fallbackProductKey` 注册设备。

---

## 六、Step 5: 设备连接验证

### 6.1 建立 TCP 连接

使用 TCP 调试工具连接到 `服务器IP:9200`。

### 6.2 发送心跳帧

```
Hex: 82 01 00
```
- `82` = 心跳命令码 (HEARTBEAT)
- `01 00` = 设备编号 1（小端序）

**预期结果：**
- 服务端回复：`83 01 00`（心跳应答）
- 服务端日志：`[YWN] 设备已注册: 1`
- 设备状态变为在线

### 6.3 发送设备信息帧

```
Hex: 81 01 00 + 20字节ICCID + 15字节IMEI + 4字节HW版本 + 4字节SW版本
```

示例（ICCID="12345678901234567890", IMEI="000000000000000", HW="V1.0", SW="V1.2"）：

```
81 01 00 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36 37 38 39 30 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 56 31 2E 30 56 31 2E 32
```

**预期结果：**
- 服务端回复 ACK：`01 01 00`
- 前端设备详情页显示 ICCID/IMEI/硬件版本/软件版本

### 6.4 发送事件帧（火警示例）

构造一个 67 字节的事件帧（命令码 0x80 + 设备编号 2B + 64B 事件载荷），其中 eventCode=0x01（火警）。

**预期结果：**
- 服务端回复 ACK
- 前端设备日志页显示火警事件
- 大屏实时告警列表出现火警记录

### 6.5 持续心跳保活

每 3 分钟发送一次心跳帧 `82 01 00`，设备保持在线状态。

---

## 七、Step 6: 数据验证清单

### 7.1 设备状态验证

| 验证项 | 操作 | 预期 |
|--------|------|------|
| 设备在线 | 发送心跳后查看设备列表 | state=在线 |
| 设备离线 | 停止心跳6分钟后 | state=离线 |
| 重连恢复 | 离线后重新连接并发送心跳 | state=在线 |

### 7.2 属性数据验证

| 验证项 | 查询方式 | 预期 |
|--------|---------|------|
| 设备信息属性 | 设备详情页 → 属性 | 显示 ICCID/IMEI/版本 |
| 事件属性 | 设备详情页 → 属性 | 显示最新事件的 code/name/描述 |
| 历史曲线 | 设备详情页 → 属性历史 | 时序数据可查 |

### 7.3 事件日志验证

| 验证项 | 查询方式 | 预期 |
|--------|---------|------|
| 火警事件 | 设备日志页 → type=event | identifier=fire_alarm_event |
| 故障事件 | 设备日志页 → type=event | identifier=fault_event |
| 控制器故障 | 设备日志页 → type=event | identifier=controller_fault_event |
| 上下线记录 | 设备日志页 → type=state | identifier=online/offline |
| 服务调用记录 | 设备日志页 → type=service | identifier=xxx_reply, code=0/1 |

### 7.4 远程控制验证

| 验证项 | 操作 | 预期 |
|--------|------|------|
| 设备复位 | 远程控制 → 复位 | TCP 收到 0x02 帧，回复 code=0 |
| 消音 | 远程控制 → 消音 | TCP 收到 0x03 帧，回复 code=0 |
| 屏蔽 | 远程控制 → 屏蔽（填机号/回路/地址） | TCP 收到 0x07 帧 |
| 手自动转换 | 远程控制 → 手自动转换（选模式） | TCP 收到 0x06 帧 |
| 设备离线时控制 | 设备离线后发起控制 | 回复 code=1, error=设备不在线 |

---

## 八、数据流架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        YWN 控制器设备                         │
│  心跳(0x82) / 事件(0x80) / 设备信息(0x81) / 故障(0x86,0x87)  │
└──────────────────────────┬──────────────────────────────────┘
                           │ TCP 连接 (端口 9200)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              TcpYwnComponent (ywn-tcp 组件)                  │
│  帧解析 → 设备注册 → 属性上报 → 事件上报 → 状态变更           │
│  serviceInvoke → 下行控制帧 → 设备                           │
└──────────────────────────┬──────────────────────────────────┘
                           │ report() → ThingModelMessage
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              消息总线 (Vert.x EventBus)                      │
│              topic: "device_thing"                           │
└──────────────────────────┬──────────────────────────────────┘
                           │ 异步分发
           ┌───────────────┼───────────────┬───────────────────┐
           ▼               ▼               ▼                   ▼
  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
  │ LogHandler   │ │PropHandler   │ │StateHandler  │ │AlertChecker  │
  │              │ │              │ │              │ │              │
  │thing_model_  │ │Redis属性缓存 │ │eiot_device_  │ │eiot_device_  │
  │message 表    │ │+时序库历史   │ │info 表       │ │alert_record  │
  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
           │               │               │                   │
           └───────────────┼───────────────┴───────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     REST API 层                              │
│  /eiot/device/page          → 设备列表(在线状态)              │
│  /eiot/device/getDeviceWithProperty → 设备详情+属性           │
│  /eiot/device/deviceLogs/list → 事件/状态/服务日志            │
│  /eiot/firewatch/dashboard/*  → 大屏统计/实时告警             │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     Vue 前端页面                              │
│  设备列表 / 设备详情 / 设备日志 / 远程控制 / 监控大屏         │
└─────────────────────────────────────────────────────────────┘
```
