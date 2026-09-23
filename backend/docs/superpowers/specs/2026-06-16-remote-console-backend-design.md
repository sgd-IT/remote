# 远程控制台后端接口与后台管理设计文档

| 项目 | 内容 |
|------|------|
| 文档编号 | SPEC-REMOTE-CONSOLE-001 |
| 版本 | V1.0 |
| 日期 | 2026-06-16 |
| 状态 | 待评审 |

---

## 1. 背景与目标

### 1.1 背景
前端已实现远程控制台页面（`views/eiot/firewatch/remote-console`），但后端对应接口尚未实现，访问时返回 `{"code":500,"data":null,"msg":"系统异常"`。

### 1.2 目标
实现前端远程控制台所需的全部后端接口，并提供后台管理页面用于操作日志审计，满足 GB 25506-2010、T/CFPA 052-2026、T/GDCAA 016-2025 等标准对远程控制操作留痕的要求。

---

## 2. 范围

### 2.1 本次实现
- 前端远程控制台只读接口（设备树、设备详情、设备参数、状态统计、操作日志）
- 手自动模式切换接口
- 指令下发日志增强（复用现有 `/eiot/device/instruction/send`）
- 后台管理页面：远程控制操作日志审计

### 2.2 本次不实现
- 完整的双人复核任务流（创建/审批/超时）
- 业主授权码系统
- 通讯保持心跳机制
- 系统参数修改功能

---

## 3. 数据模型

### 3.1 新增表

#### `eiot_remote_operation_log` 远程控制操作日志

| 字段名 | 类型 | 说明 | 是否必填 |
|--------|------|------|---------|
| id | BIGINT | 主键 | 是 |
| tenant_id | BIGINT | 租户ID | 是 |
| device_id | BIGINT | 设备ID | 是 |
| device_name | VARCHAR(64) | 设备名称 | 是 |
| property_id | BIGINT | 所属企业/物业ID | 否 |
| property_name | VARCHAR(64) | 企业/物业名称 | 否 |
| operation_type | VARCHAR(32) | 操作类型：MUTE/RESET/SWITCH_MODE/START/STOP | 是 |
| operation_name | VARCHAR(64) | 操作名称：消音/复位/切换模式/启动/停止 | 是 |
| operator_id | BIGINT | 操作人ID | 是 |
| operator_name | VARCHAR(64) | 操作人姓名 | 是 |
| reviewer_id | BIGINT | 复核人ID（高风险操作） | 否 |
| reviewer_name | VARCHAR(64) | 复核人姓名 | 否 |
| command_status | TINYINT | 0=失败 1=成功 2=执行中 | 是 |
| result_msg | VARCHAR(512) | 结果说明 | 否 |
| request_params | TEXT | 请求参数JSON | 否 |
| ip_address | VARCHAR(45) | 操作IP | 否 |
| operation_time | DATETIME | 操作时间 | 是 |
| create_time | DATETIME | 创建时间 | 是 |

**索引**：
- `idx_device_id_operation_time`：用于按设备查询最近操作日志
- `idx_tenant_id_operation_time`：用于后台管理分页查询
- `idx_operator_id`：用于按操作人筛选

### 3.2 复用现有表

- `eiot_device_info`：设备基础信息
- `eiot_device_function`：设备已绑定功能
- `eiot_device_property` / 物模型属性：设备实时参数、手自动模式
- `system_tenant`：租户/企业信息

---

## 4. 接口设计

### 4.1 接口清单

| 接口 | 路径 | HTTP | 权限 |
|------|------|------|------|
| 获取设备树 | `/eiot/remote-console/device-tree` | GET | `iot:device:query` |
| 获取设备详情 | `/eiot/remote-console/device/{deviceId}/detail` | GET | `iot:device:query` |
| 获取设备参数 | `/eiot/remote-console/device/{deviceId}/params` | GET | `iot:device:query` |
| 获取设备操作日志 | `/eiot/remote-console/device/{deviceId}/operation-logs` | GET | `iot:device:query` |
| 获取设备状态统计 | `/eiot/remote-console/device-status-stats` | GET | `iot:device:query` |
| 切换手自动模式 | `/eiot/remote-console/device/{deviceId}/mode` | POST | `iot:device:remote:control` |
| 下发控制指令 | `/eiot/device/instruction/send` | POST | `iot:device:instruction:send` |
| 后台-操作日志分页 | `/eiot/remote-console/operation-log/page` | GET | `iot:remote-operation-log:query` |
| 后台-操作日志导出 | `/eiot/remote-console/operation-log/export` | GET | `iot:remote-operation-log:export` |

### 4.2 接口详情

#### 4.2.1 获取设备树

**路径**：`GET /eiot/remote-console/device-tree`

**响应**：
```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "name": "万达商业广场",
      "riskLevel": "danger",
      "deviceCount": 8,
      "devices": [
        {
          "id": 101,
          "name": "1号主机",
          "state": 1,
          "alarmStatus": "alarm"
        }
      ]
    }
  ]
}
```

**实现说明**：
- 按 `tenant_id`（企业/物业）分组查询设备
- `riskLevel` 根据企业下设备的告警/故障数量实时计算：
  - 有告警设备 → `danger`
  - 有故障设备 → `warning`
  - 全部正常 → `normal`

#### 4.2.2 获取设备详情

**路径**：`GET /eiot/remote-console/device/{deviceId}/detail`

**响应**：
```json
{
  "code": 0,
  "data": {
    "id": 101,
    "name": "1号主机",
    "state": 1,
    "alarmStatus": "alarm",
    "mode": "manual",
    "params": []
  }
}
```

**实现说明**：
- `mode` 从设备物模型属性 `control_mode` 读取，不存在时默认 `manual`

#### 4.2.3 获取设备参数

**路径**：`GET /eiot/remote-console/device/{deviceId}/params`

**响应**：
```json
{
  "code": 0,
  "data": [
    {
      "code": "temperature",
      "name": "当前温度",
      "value": 23.5,
      "unit": "°C",
      "icon": "🌡️"
    }
  ]
}
```

**实现说明**：
- 从设备属性表中读取预定义参数：温度、湿度、电压、电池电量、信号强度、运行时长
- 参数不存在时使用默认值或返回空列表

#### 4.2.4 获取设备操作日志

**路径**：`GET /eiot/remote-console/device/{deviceId}/operation-logs?pageNo=1&pageSize=20`

**响应**：
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "time": "2026-06-15T10:25:18",
        "content": "执行了消音操作",
        "operatorName": "李明华"
      }
    ],
    "total": 100
  }
}
```

#### 4.2.5 获取设备状态统计

**路径**：`GET /eiot/remote-console/device-status-stats`

**响应**：
```json
{
  "code": 0,
  "data": {
    "total": 31,
    "online": 3,
    "alarm": 2,
    "fault": 1,
    "offline": 25
  }
}
```

#### 4.2.6 切换手自动模式

**路径**：`POST /eiot/remote-console/device/{deviceId}/mode`

**请求体**：
```json
{
  "mode": "auto",
  "reason": "火灾确认，切换至自动模式"
}
```

**实现说明**：
- 校验用户权限
- 记录操作日志
- 调用设备服务下发模式切换命令
- 返回命令下发结果

#### 4.2.7 下发控制指令（复用增强）

**路径**：`POST /eiot/device/instruction/send`（已存在）

**增强内容**：
- 在 `DeviceInstructionServiceImpl.sendInstruction()` 执行前后增加操作日志记录
- 记录内容：操作人、复核人、设备、功能、结果、请求参数

### 4.3 后台管理接口

#### 4.3.1 操作日志分页

**路径**：`GET /eiot/remote-console/operation-log/page`

**请求参数**：
| 参数名 | 类型 | 说明 |
|--------|------|------|
| pageNo | Integer | 页码 |
| pageSize | Integer | 每页条数 |
| deviceId | Long | 设备ID |
| propertyId | Long | 企业/物业ID |
| operationType | String | 操作类型 |
| operatorId | Long | 操作人ID |
| commandStatus | Integer | 命令状态 |
| startTime | DateTime | 开始时间 |
| endTime | DateTime | 结束时间 |

#### 4.3.2 操作日志导出

**路径**：`GET /eiot/remote-console/operation-log/export`

**说明**：导出 Excel，字段同分页列表。

---

## 5. 权限设计

### 5.1 新增权限

| 权限编码 | 权限名称 | 说明 |
|----------|----------|------|
| `iot:device:remote:control` | 设备远程控制 | 执行手自动切换等远程控制 |
| `iot:remote-operation-log:query` | 远程控制日志查询 | 查看操作日志 |
| `iot:remote-operation-log:export` | 远程控制日志导出 | 导出操作日志 |

### 5.2 角色绑定建议

| 角色 | 权限 |
|------|------|
| 值守员（ROLE_WATCHER） | `iot:device:query`、`iot:device:instruction:send`（消音/复位） |
| 值守主管（ROLE_SUPERVISOR） | 值守员权限 + `iot:device:remote:control` |
| 技术负责人（ROLE_ADMIN） | 主管权限 + `iot:remote-operation-log:query`、`iot:remote-operation-log:export` |

---

## 6. 前端调整

### 6.1 移除 Mock 兜底

当前前端组件在 API 失败时会回退到 Mock 数据。后端接口实现后，应调整前端代码：
- `DeviceTree.vue`：移除 `loadMockData()`，直接展示接口返回数据
- `ControlPanel.vue`：移除 `getMockParams()`、`getMockLogs()`、`getMockFunctions()`
- 保留错误处理和空状态提示

### 6.2 请求静默模式

开发阶段为避免后端 500 错误频繁弹窗，已给 API 请求添加了 `silent: true`。接口实现稳定后，可移除该标记或改为仅对已知可失败场景使用。

---

## 7. 实现文件清单

### 7.1 后端文件

| 文件路径 | 说明 |
|---------|------|
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/remote/RemoteOperationLogDO.java` | 操作日志 DO |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/remote/RemoteOperationLogMapper.java` | 操作日志 Mapper |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteOperationLogRespVO.java` | 日志响应 VO |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteConsoleDeviceTreeRespVO.java` | 设备树 VO |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteConsoleDeviceDetailRespVO.java` | 设备详情 VO |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteConsoleDeviceParamRespVO.java` | 设备参数 VO |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteConsoleDeviceStatusStatsRespVO.java` | 状态统计 VO |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/SwitchDeviceModeReqVO.java` | 模式切换请求 VO |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/RemoteConsoleController.java` | 远程控制台 Controller |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteConsoleService.java` | 远程控制台 Service 接口 |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteConsoleServiceImpl.java` | 远程控制台 Service 实现 |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteOperationLogService.java` | 日志 Service 接口 |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteOperationLogServiceImpl.java` | 日志 Service 实现 |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/convert/remote/RemoteOperationLogConvert.java` | 日志转换器 |
| `_sql/mysql/remote/remote_operation_log.sql` | 建表 SQL |

### 7.2 前端文件

| 文件路径 | 说明 |
|---------|------|
| `anwei-fire-remote-monitor-vue/src/api/eiot/remote/remoteConsole.api.ts` | 已存在，按需调整 |
| `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/DeviceTree.vue` | 移除 Mock |
| `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/ControlPanel.vue` | 移除 Mock |
| `anwei-fire-remote-monitor-vue/src/views/eiot/remote/operation-log/index.vue` | 新增后台管理页面 |
| `anwei-fire-remote-monitor-vue/src/router/modules/remaining.ts` | 注册后台管理路由 |

---

## 8. 验收标准

### 8.1 接口验收
- [ ] 设备树接口返回正确的企业和设备分组
- [ ] 设备参数接口返回预定义参数
- [ ] 操作日志接口按时间倒序返回
- [ ] 状态统计接口数据准确
- [ ] 切换模式接口记录操作日志
- [ ] 指令下发接口记录操作日志

### 8.2 后台管理验收
- [ ] 操作日志列表分页正常
- [ ] 按时间、企业、设备、操作类型、操作人筛选正常
- [ ] 导出 Excel 正常

### 8.3 合规验收
- [ ] 所有远程控制操作均写入 `eiot_remote_operation_log`
- [ ] 日志表不提供 update/delete 接口
- [ ] 日志保留时间配置为 ≥5 年

---

## 9. 风险与依赖

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| 设备属性表中没有 `control_mode` 字段 | 模式显示异常 | 默认返回 manual，并在文档中说明依赖设备上报 |
| 企业/物业与设备的关联关系不清晰 | 设备树分组错误 | 实现前确认 `eiot_device_info` 中 `tenant_id` 即为物业/企业 |
| 现有 `DeviceInstructionServiceImpl` 改动影响其他功能 | 指令下发异常 | 仅增加日志记录，不改变原有业务逻辑 |

---

## 10. 附录

### 10.1 参考资料
- PRD-02 远程控制模块
- 系统设计/03-功能模块设计.md
- API-接口设计文档.md 第 4 章 F2 远程控制
- 现有代码：`DeviceFunctionController.java`、`DeviceInstructionServiceImpl.java`
