# YWN 改造上线操作与测试流程

> 适用版本：状态聚合 + 厂商配置表远程控制改造（2026-07）
> 涉及改造：
> 1. **状态聚合** — `TcpYwnComponent` 事件上报时派生 7 个布尔状态属性，复位/取消屏蔽指令下发后主动清零；物模型服务收敛为单一 `remote_control`
> 2. **远程控制厂商配置表** — 新增 `eiot_protocol_vendor` / `eiot_vendor_command` 两表，指令下发按厂商 `invoke_type` 策略路由（REMOTE_API=四信 / LOCAL_COMMAND=YWN）

---

## 第一部分 上线操作流程

> 按顺序执行，每步完成后再进行下一步。

### 1. 编译打包验证

```powershell
$env:JAVA_HOME = "D:\Java\jdk-21.0.6"
$env:Path = "D:\Java\jdk-21.0.6\bin;$env:Path"

# 业务模块（含厂商配置表、策略化指令下发）
mvn -q compile -pl module-eiot/module-eiot-biz -am -DskipTests

# YWN 组件（状态派生 + 单一 remote_control 服务）
mvn -q compile -pl module-eiot/iot-components/iot-component-tcp-ywn -am -DskipTests
```

两条命令均须退出码为 0。

### 2. 数据库变更

执行脚本：`_sql/mysql/remote/protocol_vendor_command.sql`

脚本内容：

| 序号 | 变更 | 说明 |
|------|------|------|
| 1 | `CREATE TABLE eiot_protocol_vendor` | 协议厂商配置表（invoke_type 决定下发策略） |
| 2 | `CREATE TABLE eiot_vendor_command` | 厂商指令字典表（命令码 + param_schema 槽位模板） |
| 3 | `ALTER TABLE eiot_brand ADD vendor_code` | 品牌关联厂商 |
| 4 | `ALTER TABLE eiot_brand_model_function ADD vendor_command_key` | 功能关联厂商指令 |
| 5 | 厂商种子数据 | FOURFAITH（REMOTE_API）、YWN（LOCAL_COMMAND） |
| 6 | 存量品牌回填 | `UPDATE eiot_brand SET vendor_code='FOURFAITH' WHERE vendor_code IS NULL` |
| 7 | YWN 指令字典 9 条 | reset / silence / manual_auto_switch / bus_switch / shield_switch / multi_line_switch / auto_switch / manual_allow_switch / auto_manual_switch |

> ⚠️ **重复执行风险**：`CREATE TABLE IF NOT EXISTS` 可重复执行，但 **ALTER TABLE 加列与 INSERT 种子数据不可重复执行**（会报列已存在 / 唯一键冲突）。执行前先确认：
> ```sql
> SHOW COLUMNS FROM eiot_brand LIKE 'vendor_code';          -- 已有列则跳过 ALTER
> SELECT * FROM eiot_protocol_vendor;                        -- 已有数据则跳过 INSERT
> ```

执行后验证：

```sql
SELECT vendor_code, invoke_type, service_name FROM eiot_protocol_vendor;
-- 预期 2 行: FOURFAITH/REMOTE_API, YWN/LOCAL_COMMAND

SELECT command_key, code_start, code_stop FROM eiot_vendor_command WHERE vendor_code='YWN';
-- 预期 9 行, 命令码: reset=2, silence=3, bus 4/5, manual_auto=6, shield 7/8,
--                    multi_line 10/11, auto 12/13, manual_allow 14/15, auto_manual 16/17
```

### 3. 应用配置确认（随代码发布，无需手工修改）

`server/src/main/resources/application.yaml` 的 `ignore-tables` 已包含：

```yaml
      - eiot_protocol_vendor
      - eiot_vendor_command
```

两张新表为全局配置表（BaseDO），不做租户隔离。若使用生产独立配置文件（如 `deploy/config/server/application-prod.yaml`），需确认同步。

### 4. 物模型更新（关键步骤）

将 `doc/ywn/ywn_thing_model.json` 更新到 YWN 产品的物模型：

- **properties**：必须包含 7 个状态属性 —— `fire_status`、`fault_status`、`feedback_status`、`shield_status`、`start_status`、`supervisory_status`、`controller_fault_status`
- **services**：单一 `remote_control` 服务（入参 `command_code`[2~17] / `machineNo` / `param1` / `param2`）

> ⚠️ **必须先导入物模型再测试状态属性**：属性缓存写入前会按物模型过滤（`DevicePropertyHandler`），物模型中不存在的属性 key 会被丢弃，状态属性将无法缓存。

更新后清理物模型缓存（Redis `iot:thing_model` 相关 key）或重启 server 服务生效。

### 5. 组件配置

组件管理界面确认 / 新增 `ywn-tcp` 组件并启用：

```json
{
  "host": "0.0.0.0",
  "port": 8839,
  "allowAutoRegister": false,
  "fallbackProductKey": ""
}
```

> `port` 需与测试客户端 `YwnClient` 连接端口一致（YwnClient 默认 8839，也可通过启动参数指定）。

### 6. 业务配置（管理后台 / SQL）

#### 6.1 YWN 品牌关联厂商

```sql
UPDATE eiot_brand SET vendor_code = 'YWN' WHERE brand_code = '<YWN品牌编码>';
```

> 品牌 `vendor_code` 为空时后端默认按 FOURFAITH（REMOTE_API）处理 —— 存量四信品牌不受影响。

#### 6.2 品牌型号功能配置 vendor_command_key

为 YWN 品牌型号下的每个功能按钮设置 `eiot_brand_model_function.vendor_command_key`：

| vendor_command_key | 功能名称 | 类型 | 命令码(启/停) | param_schema 参数 |
|--------------------|---------|------|--------------|-------------------|
| reset | 复位 | GENERAL | 2 / — | machineNo |
| silence | 消音 | GENERAL | 3 / — | machineNo |
| manual_auto_switch | 手自动转换 | GENERAL | 6 / — | machineNo, mode→param1（2=手动 3=自动） |
| bus_switch | 总线开关 | BUS | 4 / 5 | machineNo, loopNo→param1, addrNo→param2 |
| shield_switch | 屏蔽/取消屏蔽 | GENERAL | 7 / 8 | machineNo, loopNo→param1, addrNo→param2 |
| multi_line_switch | 多线开关 | MULTI | 10 / 11 | machineNo, panelNo→param1, lineNo→param2 |
| auto_switch | 自动允许/禁止 | GENERAL | 12 / 13 | machineNo |
| manual_allow_switch | 手动允许/禁止 | GENERAL | 14 / 15 | machineNo |
| auto_manual_switch | 自动手动允许/禁止 | GENERAL | 16 / 17 | machineNo |

#### 6.3 设备远程配置

`eiot_device_remote_config`：设备关联品牌型号（`brand_model_id`），并设置二次确认密码（`confirm_password`，消音操作免密）。

#### 6.4 设备功能实例参数预设

`eiot_device_function.param_values` 按上表 param_schema 的参数 code 预设 JSON，例如总线开关按钮：

```json
{"machineNo": 1, "loopNo": 1, "addressNo": 1}
```

> 参数 code 须与指令字典 `param_schema` 中的 `code` 一致（machineNo/loopNo/addrNo/panelNo/lineNo/mode），缺省时使用 schema 中的 `default` 值。

---

## 第二部分 测试流程

### 测试工具

项目内置交互式测试客户端（模拟 YWN 网关设备）：

- 类：`module-eiot/iot-components/iot-component-tcp-ywn/src/main/java/com/enjoyiot/eiot/component/tcp/demo/client/YwnClient.java`
- 启动：IDE 直接运行 `main`，参数 `<host> <port> <deviceNumber>`（默认 `127.0.0.1 8839 1001`）
- 连接后自动上报设备信息（0x81）并周期发送心跳；收到下行控制帧时**打印命令码与参数**并自动回 ACK

菜单命令：

```
1  - 上报火警 (0x01, 机1/回路1/地址1)      6  - 上报设备信息
2  - 上报故障 (0x50)                       7  - 发送心跳
3  - 上报故障恢复 (0x64)                   8  - 上报控制器故障 (0x86)
4  - 上报反馈 (0x1A)                       9  - 上报控制器故障恢复 (0x87)
5  - 上报反馈撤销 (0x1C)                   10 - 自定义事件(code/machine/loop/addr)
0  - 退出
```

状态验证方式（任选其一）：

- **API**：`POST /admin-api/eiot/device/getDeviceWithProperty`（返回属性最新值）
- **Redis**：查看 `iot:deviceProperty:{deviceId}` 属性快照
- **前端**：设备详情页当前属性

### A. 四信回归测试（向后兼容验证）

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| A1 | 存量四信设备远程控制 | 前端远程控制页对四信设备发起复位/消音 | 走 `RemoteApiInstructionSender`（日志"指令生成成功"），指令下发成功，操作日志正常 |
| A2 | vendor_code 为空兜底 | 将某四信品牌 `vendor_code` 置 NULL 后下发指令 | 后端默认按 FOURFAITH 处理，行为与改造前一致 |
| A3 | 操作日志 | 查询 `eiot_remote_operation_log` | 记录完整（操作类型/操作人/命令状态） |

### B. 状态聚合测试

前置：YwnClient 已连接并注册在线（设备编号与预注册 dn 一致）。**按顺序执行**，每步后查属性缓存。

| # | 用例 | YwnClient 操作 | 预期属性变化 |
|---|------|---------------|-------------|
| B1 | 火警置位 | 菜单 `1`（事件码 0x01） | `fire_status=true`，`event_code=1` |
| B2 | **故障不覆盖火警（核心验证点）** | 菜单 `2`（事件码 0x50） | `fault_status=true`，`event_code=80`；**`fire_status` 仍为 true** |
| B3 | 故障恢复 | 菜单 `3`（事件码 0x64） | `fault_status=false`；`fire_status` 不变 |
| B4 | 反馈置位 | 菜单 `4`（事件码 0x1A） | `feedback_status=true` |
| B5 | 反馈撤销 | 菜单 `5`（事件码 0x1C） | `feedback_status=false` |
| B6 | 屏蔽置位 | 菜单 `10`，事件码 `30`（hex） | `shield_status=true` |
| B7 | 启动置位 | 菜单 `10`，事件码 `10`（hex） | `start_status=true` |
| B8 | 停止清零 | 菜单 `10`，事件码 `12`（hex） | `start_status=false` |
| B9 | 监管置位 | 菜单 `10`，事件码 `7F`（hex） | `supervisory_status=true` |
| B10 | 控制器故障 | 菜单 `8`（0x86 帧） | `controller_fault_status=true` |
| B11 | 控制器故障恢复 | 菜单 `9`（0x87 帧） | `controller_fault_status=false` |
| B12 | 状态持久性 | 重启 server 后查属性缓存 | 各状态属性值不丢失（Redis 持久，组件无状态） |

> 事件码派生规则（`TcpYwnComponent.appendStatusChanges`）：
> 火警类 0x01~0x0F → fire_status=true；故障类 0x40~0x5F → fault_status=true；
> 0x1A/0x04 → feedback_status=true，0x1C → false；0x30~0x3F → shield_status=true；
> 0x10/0x03 → start_status=true，0x12 → false；0x64 → fault_status=false；0x7F → supervisory_status=true

### C. 远程控制测试（LOCAL_COMMAND 链路）

前置：完成第一部分 6.1~6.4 配置，YwnClient 在线。
发起方式：前端远程控制页按钮，或 API `POST /admin-api/eiot/device/instruction/send`（body: `deviceId` / `deviceFunctionId` / `password` / `commandType`）。
核对方式：YwnClient 控制台打印收到的下行帧（命令码 + machineNo/loopNo/addressNo）。

#### C1. 九条指令命令码核对

| # | 指令 | commandType | YwnClient 预期收到 | 参数核对 |
|---|------|-------------|--------------------|---------|
| C1-1 | reset 复位 | — | cmd=0x02 | machineNo |
| C1-2 | silence 消音 | — | cmd=0x03 | machineNo |
| C1-3 | manual_auto_switch 手自动转换 | — | cmd=0x06 | param1=mode（2手动/3自动） |
| C1-4 | bus_switch 启动 | 1/start | cmd=0x04 | param1=回路, param2=地址 |
| C1-5 | bus_switch 停止 | 0/stop | cmd=0x05 | 同上 |
| C1-6 | shield_switch 屏蔽 | 1/start | cmd=0x07 | param1=回路, param2=地址 |
| C1-7 | shield_switch 取消屏蔽 | 0/stop | cmd=0x08 | 同上 |
| C1-8 | multi_line_switch 启动 | 1/start | cmd=0x0A | param1=盘号, param2=路号 |
| C1-9 | multi_line_switch 停止 | 0/stop | cmd=0x0B | 同上 |
| C1-10 | auto_switch 允许 | 1/start | cmd=0x0C | machineNo |
| C1-11 | auto_switch 禁止 | 0/stop | cmd=0x0D | machineNo |
| C1-12 | manual_allow_switch 允许 | 1/start | cmd=0x0E | machineNo |
| C1-13 | manual_allow_switch 禁止 | 0/stop | cmd=0x0F | machineNo |
| C1-14 | auto_manual_switch 允许 | 1/start | cmd=0x10 | machineNo |
| C1-15 | auto_manual_switch 禁止 | 0/stop | cmd=0x11 | machineNo |

> commandType 解析规则（`LocalCommandInstructionSender.resolveCommandCode`）：
> `code_stop` 为 NULL 的指令（复位/消音/手自动转换）忽略 commandType 直接用 `code_start`；
> 有启停语义时 `'0'/stop/停止` → code_stop，其余 → code_start。

#### C2. 指令驱动状态清零

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| C2-1 | 复位清零 4 状态 | ① YwnClient 依次造状态：菜单1(火警)、菜单10-0x10(启动)、菜单4(反馈)、菜单10-0x7F(监管) ② 下发复位指令 | `fire_status/start_status/feedback_status/supervisory_status` 均变 false；`fault_status/shield_status` 不受影响 |
| C2-2 | 取消屏蔽清零 | ① 菜单10-0x30 置屏蔽 ② 下发 shield_switch 停止（取消屏蔽 0x08） | `shield_status=false` |

#### C3. 日志核对

| 验证项 | 位置 | 预期 |
|--------|------|------|
| 操作日志 | `eiot_remote_operation_log` | 每次下发一条记录，`command_status`=1（成功），操作类型/名称正确 |
| 服务调用消息 | `thing_model_message` / 设备日志页 | `type=service, identifier=remote_control_reply, code=0` |
| 后端日志 | server 控制台 | `本地命令下发: deviceId=..., vendor=YWN, commandKey=..., args={command_code=..., machineNo=..., param1=..., param2=...}` |

### D. 异常用例

| # | 用例 | 操作 | 预期返回 |
|---|------|------|---------|
| D1 | 功能未配置指令标识 | 将功能 `vendor_command_key` 置 NULL 后下发 | "功能未配置厂商指令标识(vendorCommandKey)" |
| D2 | 指令字典缺失 | `vendor_command_key` 填不存在的 key | "厂商指令字典不存在: YWN/xxx" |
| D3 | 设备离线下发 | YwnClient 退出后下发指令 | 组件回 `ServiceReply code=1`，`data={"error":"设备不在线"}` |
| D4 | 非法命令码 | API 直接 invokeService 传 `command_code=99` | 组件回 code=1，"非法命令码: 99"（合法范围 2~17） |
| D5 | 二次密码错误 | 复位操作输错密码 | "二次确认密码错误"，指令不下发 |
| D6 | 消音免密 | 消音操作不输密码 | 正常下发（消音免二次确认） |
| D7 | 未知服务名 | invokeService 传非 `remote_control` 服务名 | 组件回 code=1，"未知服务: xxx, 仅支持 remote_control" |
| D8 | 未配置远程控制 | 设备无 `eiot_device_remote_config` 记录时下发 | "设备未配置远程控制" |

---

## 第三部分 验收清单

### 上线操作

| 项目 | 通过 | 备注 |
|------|:---:|------|
| 编译验证（module-eiot-biz + tcp-ywn） | ☐ | |
| SQL 脚本执行 & 两表数据核对 | ☐ | |
| ignore-tables 配置确认 | ☐ | |
| 物模型导入（7 状态属性 + remote_control 服务） | ☐ | |
| ywn-tcp 组件启用 | ☐ | |
| YWN 品牌 vendor_code 配置 | ☐ | |
| 功能 vendor_command_key 配置（9 条） | ☐ | |
| 设备远程配置 + 功能参数预设 | ☐ | |

### 功能测试

| 项目 | 通过 | 备注 |
|------|:---:|------|
| A1~A3 四信回归 | ☐ | |
| B1 火警置位 | ☐ | |
| B2 故障不覆盖火警 ★ | ☐ | 核心验证点 |
| B3~B5 故障恢复/反馈置位撤销 | ☐ | |
| B6~B9 屏蔽/启动/停止/监管 | ☐ | |
| B10~B11 控制器故障/恢复 | ☐ | |
| B12 重启后状态持久 | ☐ | |
| C1 九条指令命令码核对（15 个用例） | ☐ | |
| C2 复位/取消屏蔽驱动清零 | ☐ | |
| C3 操作日志/服务消息核对 | ☐ | |
| D1~D8 异常用例 | ☐ | |

---

## 附录 关键代码/文件索引

| 内容 | 位置 |
|------|------|
| 物模型定义 | `doc/ywn/ywn_thing_model.json` |
| SQL 脚本 | `_sql/mysql/remote/protocol_vendor_command.sql` |
| 状态派生/清零/服务收敛 | `.../tcp/ywn/service/TcpYwnComponent.java`（appendStatusChanges / reportCommandDrivenStatus / serviceInvoke） |
| 命令码常量 | `.../tcp/ywn/parser/DataYwnPackage.java` |
| 事件码枚举 | `.../tcp/ywn/parser/YwnEventCode.java` |
| 策略路由入口 | `.../service/device/DeviceInstructionServiceImpl.java` |
| 四信策略 | `.../service/device/instruction/RemoteApiInstructionSender.java` |
| YWN 本地命令策略 | `.../service/device/instruction/LocalCommandInstructionSender.java` |
| 测试客户端 | `.../tcp/demo/client/YwnClient.java` |
| 指令下发 API | `POST /admin-api/eiot/device/instruction/send`（DeviceFunctionController） |
