
# 智慧消防远程值守系统 - API接口设计文档（上：F1-F5）

> 版本：V1.0  
> 制定日期：2026-06-15  
> 设计依据：GB/T 26875.1-2026、GB 25506-2010、GB 25201-2010、T/CFPA 052-2026、T/GDCAA 016-2025

---

## 文档目录

1. [全局规范](#1-全局规范)
2. [公共数据结构](#2-公共数据结构)
3. [F1 火警监测与处置](#3-f1-火警监测与处置)
4. [F2 远程控制](#4-f2-远程控制)
5. [F3 监测预警](#5-f3-监测预警)
6. [F4 运维管理](#6-f4-运维管理)
7. [F5 数据与报表](#7-f5-数据与报表)

---

## 1. 全局规范

### 1.1 URL规范

| 项目 | 规范 | 示例 |
|------|------|------|
| 基础路径 | `/api/v1` | - |
| 资源命名 | RESTful风格，资源名使用复数 | `/alarms`、`/devices`、`/work-orders` |
| 路径分隔 | 使用 `-` 连接多词资源 | `/fire-alerts`、`/control-commands` |
| 动作表达 | 使用HTTP方法表达动作 | GET查询、POST创建、PUT更新、DELETE删除 |
| 嵌套资源 | 不超过2级嵌套 | `/alarms/{id}/process-steps` |
| 版本控制 | URL路径版本 | `/api/v1/alarms` |

### 1.2 HTTP方法规范

| 方法 | 用途 | 幂等性 | 典型场景 |
|------|------|--------|---------|
| GET | 查询资源 | 幂等 | 查询告警列表、获取设备状态 |
| POST | 创建资源 | 非幂等 | 创建告警确认、提交工单 |
| PUT | 完整更新资源 | 幂等 | 更新设备信息、确认处置结果 |
| PATCH | 部分更新资源 | 非幂等 | 更新工单状态 |
| DELETE | 删除资源 | 幂等 | 删除草稿记录（受保护数据禁止删除） |

### 1.3 认证规范

| 项目 | 规范 | 说明 |
|------|------|------|
| 认证方式 | Bearer Token (JWT) | - |
| Token位置 | HTTP Header `Authorization` | - |
| Token格式 | `Bearer {jwt_token}` | - |
| Token有效期 | 建议8小时 | - |
| 刷新机制 | 提供Refresh Token | - |
| 敏感操作 | 需要二次验证 | 控制指令下发等 |

### 1.4 分页规范

| 项目 | 参数名 | 类型 | 默认值 | 说明 |
|------|--------|------|--------|------|
| 页码 | `page` | Integer | 1 | 从1开始 |
| 每页条数 | `page_size` | Integer | 20 | 最大100 |
| 排序字段 | `sort_by` | String | `create_time` | - |
| 排序方向 | `sort_order` | String | `desc` | `asc`/`desc` |

### 1.5 时间格式

| 项目 | 格式 | 示例 | 时区 |
|------|------|------|------|
| 日期时间 | ISO 8601 | `2026-06-15T10:30:00+08:00` | Asia/Shanghai (UTC+8) |
| 日期 | ISO 8601 Date | `2026-06-15` | - |
| 时间 | ISO 8601 Time | `10:30:00` | - |

### 1.6 字段命名规范

| 项目 | 规范 | 示例 |
|------|------|------|
| 字段命名 | snake_case | `alarm_no`、`handle_status` |
| 布尔值 | 使用 0/1 | - |
| 枚举值 | 使用整数 | - |
| 主键 | 使用 `id` | - |
| 外键 | 使用 `{resource}_id` | `device_id`、`building_id` |
| 创建时间 | `create_time` | - |
| 更新时间 | `update_time` | - |

### 1.7 响应格式

#### 1.7.1 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 1.7.2 分页响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [ ... ],
    "total": 100,
    "page": 1,
    "page_size": 20,
    "total_pages": 5
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 1.7.3 错误响应

```json
{
  "code": 40001,
  "message": "火警已确认，无法重复操作",
  "data": null,
  "timestamp": "2026-06-15T10:30:00+08:00",
  "request_id": "req_abc123"
}
```

### 1.8 错误码规范

| 错误码范围 | 模块 | 示例 |
|-----------|------|------|
| 0 | 成功 | - |
| 1xxxx | 通用错误 | 10001=参数错误、10002=未授权、10003=禁止操作 |
| 2xxxx | F1 火警监测错误 | 20001=火警已确认、20002=火警已闭环、20003=告警不存在 |
| 3xxxx | F2 远程控制错误 | 30001=双人复核未通过、30002=授权码无效、30003=设备离线 |
| 4xxxx | F3 监测预警错误 | 40001=监测点不存在、40002=阈值配置错误 |
| 5xxxx | F4 运维管理错误 | 50001=工单不存在、50002=工单状态不允许此操作 |
| 6xxxx | F5 数据报表错误 | 60001=报表生成失败、60002=数据签名校验失败 |

### 1.9 权限要求标注

| 权限等级 | 说明 | 典型角色 |
|---------|------|---------|
| 公开 | 无需认证 | - |
| 已认证 | 需要登录 | 任意登录用户 |
| 普通操作 | 基本操作权限 | ROLE_WATCHER (值守员) |
| 操作权限 | 设备控制权限 | ROLE_SUPERVISOR (值守主管) |
| 管理权限 | 系统配置权限 | ROLE_ADMIN (技术负责人) |

---

## 2. 公共数据结构

### 2.1 分页参数 (PaginationParams)

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| page_size | Integer | 否 | 每页条数，默认20，最大100 |
| sort_by | String | 否 | 排序字段，默认create_time |
| sort_order | String | 否 | 排序方向，asc/desc，默认desc |

### 2.2 分页响应 (PaginationResponse)

| 参数名 | 类型 | 说明 |
|--------|------|------|
| list | Array | 数据列表 |
| total | Integer | 总记录数 |
| page | Integer | 当前页码 |
| page_size | Integer | 每页条数 |
| total_pages | Integer | 总页数 |

### 2.3 时间范围参数 (TimeRangeParams)

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| start_time | DateTime | 否 | 开始时间（ISO 8601） |
| end_time | DateTime | 否 | 结束时间（ISO 8601） |

### 2.4 操作人信息 (OperatorInfo)

| 参数名 | 类型 | 说明 |
|--------|------|------|
| operator_id | String | 操作人ID |
| operator_name | String | 操作人姓名 |
| operator_role | String | 操作人角色 |

### 2.5 文件上传响应 (FileUploadResponse)

| 参数名 | 类型 | 说明 |
|--------|------|------|
| file_id | String | 文件ID |
| file_url | String | 文件访问URL |
| file_name | String | 文件名 |
| file_size | Long | 文件大小（字节） |
| mime_type | String | MIME类型 |

### 2.6 签名信息 (SignatureInfo)

| 参数名 | 类型 | 说明 |
|--------|------|------|
| hash_value | String | 数据哈希值 (SHA-256) |
| sign_time | DateTime | 签名时间 |
| sign_method | String | 签名方法 |
| prev_hash | String | 前一条记录哈希值 |

---

## 3. F1 火警监测与处置

### 3.1 接口列表

| 接口编号 | 接口名称 | HTTP方法 | 路径 | 权限要求 |
|---------|---------|----------|------|---------|
| F1-001 | 获取告警列表 | GET | /fire-alerts | 已认证 |
| F1-002 | 获取告警详情 | GET | /fire-alerts/{alarm_no} | 已认证 |
| F1-003 | 确认告警（核实） | POST | /fire-alerts/{alarm_no}/verify | 普通操作 |
| F1-004 | 确认火警（启动处置） | POST | /fire-alerts/{alarm_no}/confirm-fire | 普通操作 |
| F1-005 | 确认误报 | POST | /fire-alerts/{alarm_no}/confirm-false-alarm | 普通操作 |
| F1-006 | 获取处置流程列表 | GET | /fire-alerts/{alarm_no}/process-steps | 已认证 |
| F1-007 | 执行处置步骤 | POST | /fire-alerts/{alarm_no}/process-steps | 普通操作 |
| F1-008 | 获取通知记录列表 | GET | /fire-alerts/{alarm_no}/notifications | 已认证 |
| F1-009 | 手动发送通知 | POST | /fire-alerts/{alarm_no}/notifications | 普通操作 |
| F1-010 | 拨打119 | POST | /fire-alerts/{alarm_no}/call-119 | 操作权限 |
| F1-011 | 启动应急预案 | POST | /fire-alerts/{alarm_no}/emergency-plan | 操作权限 |
| F1-012 | 闭环告警 | POST | /fire-alerts/{alarm_no}/close | 普通操作 |
| F1-013 | 获取告警统计 | GET | /fire-alerts/statistics | 已认证 |
| F1-014 | 获取告警趋势 | GET | /fire-alerts/trends | 已认证 |
| F1-015 | 获取未处置告警数 | GET | /fire-alerts/unhandled-count | 已认证 |
| F1-016 | 获取故障列表 | GET | /faults | 已认证 |
| F1-017 | 获取故障详情 | GET | /faults/{fault_no} | 已认证 |
| F1-018 | 确认故障 | POST | /faults/{fault_no}/confirm | 普通操作 |
| F1-019 | 获取预警列表 | GET | /warnings | 已认证 |
| F1-020 | 获取预警详情 | GET | /warnings/{warning_no} | 已认证 |
| F1-021 | 处理预警 | POST | /warnings/{warning_no}/handle | 普通操作 |

---

### F1-001 获取告警列表

**接口编号**：F1-001  
**接口名称**：获取告警列表  
**路径**：`GET /api/v1/fire-alerts`  
**权限要求**：已认证

#### 请求参数

| 参数位置 | 参数名 | 类型 | 必填 | 说明 | 校验规则 |
|----------|--------|------|------|------|---------|
| Query | page | Integer | 否 | 页码 | 默认1，最小1 |
| Query | page_size | Integer | 否 | 每页条数 | 默认20，最大100 |
| Query | signal_type | Integer | 否 | 信号类型 | 1=火警/2=故障/3=监管/4=屏蔽/5=联动 |
| Query | handle_status | Integer | 否 | 处置状态 | 1=待核实/2=核实中/3=处置中/4=已闭环 |
| Query | verify_result | Integer | 否 | 核实结果 | 1=真实火警/2=误报/3=未核实 |
| Query | org_code | String | 否 | 联网单位编码 | - |
| Query | building_id | String | 否 | 建筑ID | - |
| Query | start_time | DateTime | 否 | 开始时间 | ISO 8601格式 |
| Query | end_time | DateTime | 否 | 结束时间 | ISO 8601格式 |
| Query | keyword | String | 否 | 关键词搜索 | 搜索位置、设备编码 |
| Query | sort_by | String | 否 | 排序字段 | 默认alarm_time |
| Query | sort_order | String | 否 | 排序方向 | asc/desc，默认desc |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "alarm_no": "FA20260615100001",
        "org_code": "440106",
        "org_name": "XX商场",
        "building_id": "B001",
        "building_name": "1号楼",
        "alarm_time": "2026-06-15T10:00:00+08:00",
        "receive_time": "2026-06-15T10:00:08+08:00",
        "signal_type": 1,
        "signal_type_name": "火警",
        "device_type": 40,
        "device_type_name": "感烟探测器",
        "device_code": "L01-001",
        "location": "3层301商铺",
        "verify_result": null,
        "verify_time": null,
        "handle_status": 1,
        "handle_status_name": "待核实",
        "handler_id": null,
        "handler_name": null,
        "ai_analysis_result": {
          "fire_probability": 0.85,
          "smoke_probability": 0.92,
          "person_count": 0
        },
        "related_video_ids": ["V001", "V002"]
      }
    ],
    "total": 150,
    "page": 1,
    "page_size": 20,
    "total_pages": 8
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 10001 | 参数错误 |

#### 合规标注

- GB/T 26875.1-2026 6.5：火警传输延迟≤10s
- GB 25506-2010 4.3：火警处置法定流程

---

### F1-002 获取告警详情

**接口编号**：F1-002  
**接口名称**：获取告警详情  
**路径**：`GET /api/v1/fire-alerts/{alarm_no}`  
**权限要求**：已认证

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_no | String | 是 | 告警编号 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "alarm_no": "FA20260615100001",
    "org_code": "440106",
    "org_name": "XX商场",
    "building_id": "B001",
    "building_name": "1号楼",
    "alarm_time": "2026-06-15T10:00:00+08:00",
    "receive_time": "2026-06-15T10:00:08+08:00",
    "signal_type": 1,
    "signal_type_name": "火警",
    "device_type": 40,
    "device_type_name": "感烟探测器",
    "device_code": "L01-001",
    "location": "3层301商铺",
    "verify_result": 1,
    "verify_result_name": "真实火警",
    "verify_time": "2026-06-15T10:00:35+08:00",
    "verify_method": 1,
    "verify_method_name": "视频复核",
    "false_alarm_reason": null,
    "handler_id": "U001",
    "handler_name": "张三",
    "handle_start_time": "2026-06-15T10:00:35+08:00",
    "handle_end_time": null,
    "handle_status": 3,
    "handle_status_name": "处置中",
    "handle_result": null,
    "notify_119": true,
    "notify_119_time": "2026-06-15T10:01:30+08:00",
    "related_video_ids": ["V001", "V002", "V003", "V004", "V005"],
    "ai_analysis_result": {
      "fire_probability": 0.85,
      "smoke_probability": 0.92,
      "person_count": 0,
      "analysis_time": "2026-06-15T10:00:12+08:00"
    },
    "historical_alarms": [
      {
        "alarm_no": "FA20260610100002",
        "alarm_time": "2026-06-10T10:00:00+08:00",
        "signal_type": 1,
        "verify_result": 2,
        "false_alarm_reason": "装修粉尘"
      }
    ],
    "signature": {
      "hash_value": "a1b2c3d4...",
      "sign_time": "2026-06-15T10:00:08+08:00",
      "prev_hash": "x1x2x3x4..."
    }
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 20003 | 告警不存在 |

---

### F1-003 确认告警（核实）

**接口编号**：F1-003  
**接口名称**：确认告警（核实）  
**路径**：`POST /api/v1/fire-alerts/{alarm_no}/verify`  
**权限要求**：普通操作  
**合规标注**：T/CFPA 052-2026 4.3 - 火警30s内双重核实

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_no | String | 是 | 告警编号 |

#### 请求体

```json
{
  "verify_result": 1,
  "verify_method": 1,
  "false_alarm_reason": null,
  "remark": "视频复核确认烟雾"
}
```

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |
|--------|------|------|------|---------|
| verify_result | Integer | 是 | 核实结果 | 1=真实火警/2=误报/3=未核实 |
| verify_method | Integer | 是 | 核实方式 | 1=视频复核/2=电话确认/3=现场确认 |
| false_alarm_reason | String | 条件必填 | 误报原因 | verify_result=2时必填 |
| remark | String | 否 | 备注 | 最大500字符 |

#### 响应结构

```json
{
  "code": 0,
  "message": "火警核实成功，请继续处置",
  "data": {
    "alarm_no": "FA20260615100001",
    "verify_result": 1,
    "verify_time": "2026-06-15T10:00:35+08:00",
    "handler_id": "U001",
    "handler_name": "张三",
    "handle_status": 3,
    "next_action": "confirm-fire"
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 20001 | 火警已确认，无法重复操作 |
| 20003 | 告警不存在 |
| 20004 | 告警状态不允许此操作 |
| 10001 | 参数错误（误报原因为空） |

#### 业务规则

- 核实操作必须在告警发生后30秒内完成
- 误报原因必须选择预设选项或填写自定义原因
- 核实结果一旦确认不可修改

---

### F1-004 确认火警（启动处置）

**接口编号**：F1-004  
**接口名称**：确认火警（启动处置）  
**路径**：`POST /api/v1/fire-alerts/{alarm_no}/confirm-fire`  
**权限要求**：普通操作  
**合规标注**：GB 25506-2010 4.3 - 火警处置法定流程

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_no | String | 是 | 告警编号 |

#### 请求体

```json
{
  "confirm_remark": "视频确认真实火警",
  "auto_execute_plan": true
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| confirm_remark | String | 否 | 确认备注 |
| auto_execute_plan | Boolean | 否 | 是否自动执行应急预案，默认false |

#### 响应结构

```json
{
  "code": 0,
  "message": "火警已确认，启动处置流程",
  "data": {
    "alarm_no": "FA20260615100001",
    "handle_status": 3,
    "handle_status_name": "处置中",
    "process_steps": [
      {
        "step_no": 1,
        "step_name": "远程切换主机至自动模式",
        "step_status": "pending",
        "require_privilege": "操作权限"
      },
      {
        "step_no": 2,
        "step_name": "拨打119报警",
        "step_status": "pending",
        "require_privilege": "普通操作"
      },
      {
        "step_no": 3,
        "step_name": "通知联网单位消控室",
        "step_status": "pending",
        "require_privilege": "普通操作"
      },
      {
        "step_no": 4,
        "step_name": "通知业主消防安全负责人",
        "step_status": "pending",
        "require_privilege": "普通操作"
      },
      {
        "step_no": 5,
        "step_name": "5分钟内确认处理结果",
        "step_status": "pending",
        "require_privilege": "普通操作"
      }
    ],
    "time_remaining": 300
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务规则

- 确认火警后自动进入处置流程
- 系统自动启动30秒倒计时用于核实
- 5分钟内必须确认处理结果（T/GDCAA 016-2025 7.1）

---

### F1-005 确认误报

**接口编号**：F1-005  
**接口名称**：确认误报  
**路径**：`POST /api/v1/fire-alerts/{alarm_no}/confirm-false-alarm`  
**权限要求**：普通操作  
**合规标注**：GB 25201-2010 - 误报原因必填

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_no | String | 是 | 告警编号 |

#### 请求体

```json
{
  "false_alarm_reason": "cooking_smoke",
  "false_alarm_reason_text": "厨房油烟触发",
  "remark": "已通知业主"
}
```

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |
|--------|------|------|------|---------|
| false_alarm_reason | String | 是 | 误报原因代码 | 必填，预设选项 |
| false_alarm_reason_text | String | 否 | 误报原因说明 | 最大256字符 |
| remark | String | 否 | 备注 | 最大500字符 |

#### 误报原因代码

| 代码 | 说明 |
|------|------|
| cooking_smoke | 烹饪油烟 |
| dust | 装修粉尘 |
| steam | 水蒸气 |
| cigarette | 香烟烟雾 |
| device_aging | 设备老化 |
| wrong_location | 安装位置不当 |
| drill_test | 消防演练 |
| device_test | 设备测试 |
| maintenance | 维修施工 |
| other | 其他原因 |

#### 响应结构

```json
{
  "code": 0,
  "message": "误报已确认",
  "data": {
    "alarm_no": "FA20260615100001",
    "verify_result": 2,
    "false_alarm_reason": "cooking_smoke",
    "false_alarm_reason_text": "烹饪油烟",
    "handle_status": 4,
    "handle_status_name": "已闭环",
    "auto_mute": true,
    "auto_reset": true,
    "notification_sent": true
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务规则

- 误报原因必填，不可为空
- 同一设备月累计误报≥3次，自动标记为"高频误报设备"
- 高频误报设备自动生成维保建议

---

### F1-007 执行处置步骤

**接口编号**：F1-007  
**接口名称**：执行处置步骤  
**路径**：`POST /api/v1/fire-alerts/{alarm_no}/process-steps`  
**权限要求**：普通操作（部分步骤需要操作权限）  
**合规标注**：GB 25506-2010 5.5 - 法定处置流程

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_no | String | 是 | 告警编号 |

#### 请求体

```json
{
  "step_no": 2,
  "action": "execute",
  "result": "拨打成功",
  "remark": "已联系消防部门"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| step_no | Integer | 是 | 步骤序号 |
| action | String | 是 | 执行动作：execute(执行)/skip(跳过)/fail(失败) |
| result | String | 条件必填 | 执行结果 |
| remark | String | 否 | 备注 |

#### 响应结构

```json
{
  "code": 0,
  "message": "步骤执行成功",
  "data": {
    "process_id": "PROC20260615001",
    "alarm_no": "FA20260615100001",
    "step_no": 2,
    "step_name": "拨打119报警",
    "start_time": "2026-06-15T10:01:00+08:00",
    "end_time": "2026-06-15T10:01:30+08:00",
    "result": "拨打成功",
    "operator_id": "U001",
    "operator_name": "张三",
    "next_step_no": 3,
    "all_steps_completed": false
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务规则

- 每步操作自动记录时间戳和操作人
- 步骤不可跳过，除非特殊情况并记录原因
- 所有步骤完成后自动进入闭环状态

---

### F1-010 拨打119

**接口编号**：F1-010  
**接口名称**：拨打119  
**路径**：`POST /api/v1/fire-alerts/{alarm_no}/call-119`  
**权限要求**：操作权限  
**合规标注**：GB 25506-2010 - 火灾报警

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_no | String | 是 | 告警编号 |

#### 请求体

```json
{
  "remark": "3层商铺火灾确认，请求支援"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| remark | String | 否 | 备注说明 |

#### 响应结构

```json
{
  "code": 0,
  "message": "119已拨打",
  "data": {
    "alarm_no": "FA20260615100001",
    "call_id": "CALL20260615001",
    "call_time": "2026-06-15T10:01:30+08:00",
    "call_duration": 45,
    "recording_url": "https://storage.example.com/recordings/2026/06/15/CALL20260615001.mp3",
    "call_status": "completed",
    "call_result": "已接通，消防已派车"
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务规则

- 拨打119自动填充地址信息
- 通话全程录音，保存期限≥5年
- 记录拨打时间、通话时长、录音地址

---

### F1-012 闭环告警

**接口编号**：F1-012  
**接口名称**：闭环告警  
**路径**：`POST /api/v1/fire-alerts/{alarm_no}/close`  
**权限要求**：普通操作  
**合规标注**：T/CFPA 052-2026 - 处置记录不可篡改

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_no | String | 是 | 告警编号 |

#### 请求体

```json
{
  "close_reason": "火灾已扑灭",
  "close_remark": "现场已清理完毕"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| close_reason | String | 是 | 闭环原因 |
| close_remark | String | 否 | 备注 |

#### 响应结构

```json
{
  "code": 0,
  "message": "告警已闭环",
  "data": {
    "alarm_no": "FA20260615100001",
    "handle_status": 4,
    "handle_status_name": "已闭环",
    "handle_end_time": "2026-06-15T11:30:00+08:00",
    "total_duration": 5400,
    "archive_status": "auto_archived",
    "signature": {
      "hash_value": "e5f6g7h8...",
      "sign_time": "2026-06-15T11:30:00+08:00"
    }
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务规则

- 闭环后告警记录自动归档
- 归档后数据禁止修改、删除
- 自动计算处置总时长

---

### F1-013 获取告警统计

**接口编号**：F1-013  
**接口名称**：获取告警统计  
**路径**：`GET /api/v1/fire-alerts/statistics`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| stat_type | String | 否 | 统计类型：today/week/month/year，默认today |
| org_code | String | 否 | 联网单位编码 |
| building_id | String | 否 | 建筑ID |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "stat_type": "today",
    "stat_date": "2026-06-15",
    "fire_alert_count": 5,
    "fire_alert_handled": 5,
    "fire_alert_pending": 0,
    "fire_alert_handled_rate": 100.00,
    "fault_count": 12,
    "fault_handled": 10,
    "fault_pending": 2,
    "false_alarm_count": 8,
    "avg_response_time": 28,
    "avg_handling_time": 180,
    "signal_type_distribution": [
      {"type": 1, "name": "火警", "count": 5},
      {"type": 2, "name": "故障", "count": 12},
      {"type": 3, "name": "监管", "count": 3}
    ],
    "handle_status_distribution": [
      {"status": 1, "name": "待核实", "count": 0},
      {"status": 2, "name": "核实中", "count": 0},
      {"status": 3, "name": "处置中", "count": 0},
      {"status": 4, "name": "已闭环", "count": 28}
    ]
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

---

### F1-015 获取未处置告警数

**接口编号**：F1-015  
**接口名称**：获取未处置告警数  
**路径**：`GET /api/v1/fire-alerts/unhandled-count`  
**权限要求**：已认证

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total_unhandled": 3,
    "fire_alerts": 1,
    "faults": 2,
    "warnings": 0,
    "critical_list": [
      {
        "alarm_no": "FA20260615100002",
        "signal_type": 1,
        "signal_type_name": "火警",
        "alarm_time": "2026-06-15T10:15:00+08:00",
        "waiting_time": 900
      }
    ]
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

---

## 4. F2 远程控制

### 4.1 接口列表

| 接口编号 | 接口名称 | HTTP方法 | 路径 | 权限要求 |
|---------|---------|----------|------|---------|
| F2-001 | 获取设备状态列表 | GET | /devices/status | 已认证 |
| F2-002 | 获取设备状态详情 | GET | /devices/{device_id}/status | 已认证 |
| F2-003 | 下发消音命令 | POST | /control-commands/mute | 普通操作 |
| F2-004 | 下发复位命令 | POST | /control-commands/reset | 普通操作 |
| F2-005 | 切换手自动模式 | POST | /control-commands/switch-mode | 操作权限 |
| F2-006 | 启动设备 | POST | /control-commands/start | 操作权限+授权 |
| F2-007 | 停止设备 | POST | /control-commands/stop | 操作权限+授权 |
| F2-008 | 创建双人复核任务 | POST | /review-tasks | 操作权限 |
| F2-009 | 获取复核任务列表 | GET | /review-tasks | 已认证 |
| F2-010 | 获取复核任务详情 | GET | /review-tasks/{task_id} | 已认证 |
| F2-011 | 执行复核确认 | POST | /review-tasks/{task_id}/approve | 操作权限 |
| F2-012 | 执行复核拒绝 | POST | /review-tasks/{task_id}/reject | 操作权限 |
| F2-013 | 获取控制命令记录 | GET | /control-commands | 已认证 |
| F2-014 | 获取控制命令详情 | GET | /control-commands/{command_id} | 已认证 |
| F2-015 | 验证授权码 | POST | /control-commands/verify-auth | 操作权限 |
| F2-016 | 获取通讯保持状态 | GET | /control-commands/{command_id}/heartbeat | 已认证 |
| F2-017 | 获取操作日志 | GET | /operation-logs | 已认证 |

---

### F2-001 获取设备状态列表

**接口编号**：F2-001  
**接口名称**：获取设备状态列表  
**路径**：`GET /api/v1/devices/status`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| device_type | Integer | 否 | 设备类型 |
| run_status | Integer | 否 | 运行状态 |
| building_id | String | 否 | 建筑ID |
| keyword | String | 否 | 关键词搜索 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "device_id": "D001",
        "device_name": "消火栓泵#1",
        "device_type": 5,
        "device_type_name": "消火栓系统",
        "location": "地下室泵房",
        "control_mode": "manual",
        "control_mode_name": "手动",
        "run_status": 0,
        "run_status_name": "停止",
        "alarm_status": 0,
        "alarm_status_name": "正常",
        "fault_status": 0,
        "fault_status_name": "正常",
        "last_control_time": "2026-06-15T09:30:00+08:00",
        "last_control_user": "李四",
        "online_status": 1,
        "online_status_name": "在线"
      }
    ],
    "total": 500,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

---

### F2-003 下发消音命令

**接口编号**：F2-003  
**接口名称**：下发消音命令  
**路径**：`POST /api/v1/control-commands/mute`  
**权限要求**：普通操作  
**合规标注**：GB 25506-2010 5.5.2 - 消音功能

#### 请求体

```json
{
  "device_id": "D001",
  "alarm_no": "FA20260615100001",
  "remark": "误报消音"
}
```

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |
|--------|------|------|------|---------|
| device_id | String | 是 | 设备ID | - |
| alarm_no | String | 否 | 关联告警编号 | - |
| remark | String | 否 | 操作备注 | 最大256字符 |

#### 响应结构

```json
{
  "code": 0,
  "message": "消音命令下发成功",
  "data": {
    "command_id": "CMD20260615001",
    "device_id": "D001",
    "command_type": "MUTE",
    "command_status": "executed",
    "sent_time": "2026-06-15T10:02:00+08:00",
    "response_time": "2026-06-15T10:02:03+08:00",
    "execution_result": "消音成功",
    "operator_id": "U001",
    "operator_name": "张三",
    "signature": {
      "hash_value": "cmd_hash_001",
      "sign_time": "2026-06-15T10:02:03+08:00"
    }
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 30003 | 设备离线，命令无法下发 |
| 30004 | 设备不支持此命令 |
| 10002 | 未授权 |

#### 业务规则

- 消音仅屏蔽声音，不清除报警状态
- 消音后新报警自动恢复声音
- 消音操作生成不可删除的操作日志

---

### F2-006 启动设备

**接口编号**：F2-006  
**接口名称**：启动设备  
**路径**：`POST /api/v1/control-commands/start`  
**权限要求**：操作权限 + 授权码验证 + 双人复核  
**合规标注**：T/CFPA 052-2026 6.2.3 - 高风险操作双人复核

#### 请求体

```json
{
  "device_id": "PUMP001",
  "device_type": "fire_pump",
  "operation": "start",
  "authorization_code": "AUTH123456",
  "review_task_id": "RT20260615001",
  "remark": "火灾确认，启动消火栓泵"
}
```

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |
|--------|------|------|------|---------|
| device_id | String | 是 | 设备ID | - |
| device_type | String | 是 | 设备类型 | fire_pump/sprinkler_pump/fan/siren |
| operation | String | 是 | 操作类型 | start |
| authorization_code | String | 是 | 业主授权码 | 6位数字，一次性有效 |
| review_task_id | String | 是 | 双人复核任务ID | - |
| remark | String | 否 | 操作备注 | 最大256字符 |

#### 响应结构

```json
{
  "code": 0,
  "message": "启动命令下发成功",
  "data": {
    "command_id": "CMD20260615002",
    "device_id": "PUMP001",
    "command_type": "START",
    "command_status": "executing",
    "sent_time": "2026-06-15T10:03:00+08:00",
    "expected_response_time": "2026-06-15T10:03:30+08:00",
    "heartbeat_interval": 10,
    "heartbeat_timeout": 60,
    "operator_id": "U001",
    "operator_name": "张三",
    "reviewer_id": "U002",
    "reviewer_name": "李四",
    "authorization_verified": true
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 30001 | 双人复核未通过 |
| 30002 | 授权码无效或已过期 |
| 30003 | 设备离线 |
| 30005 | 授权码与复核任务不匹配 |

#### 业务规则

- 启泵必须获得业主授权码
- 执行双人复核：一人操作一人监督
- 启泵命令需持续通讯保持
- 运行状态实时回传监控

---

### F2-008 创建双人复核任务

**接口编号**：F2-008  
**接口名称**：创建双人复核任务  
**路径**：`POST /api/v1/review-tasks`  
**权限要求**：操作权限  
**合规标注**：T/CFPA 052-2026 6.2.5 - 双人复核机制

#### 请求体

```json
{
  "task_type": "device_control",
  "task_detail": {
    "device_id": "PUMP001",
    "device_name": "消火栓泵#1",
    "operation": "start",
    "operation_desc": "启动消火栓泵"
  },
  "reason": "火灾确认，需要启动消防泵",
  "validity_minutes": 5
}
```

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |
|--------|------|------|------|---------|
| task_type | String | 是 | 任务类型 | device_control/system_config |
| task_detail | Object | 是 | 任务详情 | 包含设备ID、操作类型等 |
| reason | String | 是 | 操作原因 | 最大500字符 |
| validity_minutes | Integer | 否 | 有效期（分钟） | 默认5，最大10 |

#### 响应结构

```json
{
  "code": 0,
  "message": "复核任务已创建",
  "data": {
    "task_id": "RT20260615001",
    "task_type": "device_control",
    "task_status": "pending",
    "creator_id": "U001",
    "creator_name": "张三",
    "task_detail": {
      "device_id": "PUMP001",
      "device_name": "消火栓泵#1",
      "operation": "start",
      "operation_desc": "启动消火栓泵"
    },
    "reason": "火灾确认，需要启动消防泵",
    "validity_time": "2026-06-15T10:08:00+08:00",
    "remaining_seconds": 300,
    "created_time": "2026-06-15T10:03:00+08:00"
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 业务规则

- 操作员与复核员不可为同一人
- 复核有效期5分钟，超时需重新发起
- 复核拒绝需记录拒绝原因

---

### F2-011 执行复核确认

**接口编号**：F2-011  
**接口名称**：执行复核确认  
**路径**：`POST /api/v1/review-tasks/{task_id}/approve`  
**权限要求**：操作权限  
**合规标注**：T/CFPA 052-2026 6.2.5

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| task_id | String | 是 | 复核任务ID |

#### 请求体

```json
{
  "password": "xxxx",
  "verify_code": "123456",
  "remark": "复核通过，确认执行"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| password | String | 是 | 复核密码 |
| verify_code | String | 是 | 动态验证码 |
| remark | String | 否 | 复核备注 |

#### 响应结构

```json
{
  "code": 0,
  "message": "复核确认通过",
  "data": {
    "task_id": "RT20260615001",
    "task_status": "approved",
    "reviewer_id": "U002",
    "reviewer_name": "李四",
    "review_time": "2026-06-15T10:04:00+08:00",
    "approve_result": "复核通过",
    "signature": {
      "hash_value": "review_hash_001"
    }
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

---

## 5. F3 监测预警

### 5.1 接口列表

| 接口编号 | 接口名称 | HTTP方法 | 路径 | 权限要求 |
|---------|---------|----------|------|---------|
| F3-001 | 获取监测点列表 | GET | /monitoring-points | 已认证 |
| F3-002 | 获取监测点详情 | GET | /monitoring-points/{point_id} | 已认证 |
| F3-003 | 创建监测点 | POST | /monitoring-points | 管理权限 |
| F3-004 | 更新监测点 | PUT | /monitoring-points/{point_id} | 管理权限 |
| F3-005 | 删除监测点 | DELETE | /monitoring-points/{point_id} | 管理权限 |
| F3-006 | 获取实时状态 | GET | /monitoring-points/{point_id}/realtime | 已认证 |
| F3-007 | 获取批量实时状态 | POST | /monitoring-points/batch-realtime | 已认证 |
| F3-008 | 获取历史状态 | GET | /monitoring-points/{point_id}/history | 已认证 |
| F3-009 | 获取在线率统计 | GET | /stats/online-rate | 已认证 |
| F3-010 | 获取在线率趋势 | GET | /stats/online-rate/trends | 已认证 |
| F3-011 | 获取完好率统计 | GET | /stats/health-rate | 已认证 |
| F3-012 | 获取AI分析结果 | GET | /ai-analysis/results | 已认证 |
| F3-013 | 获取AI分析详情 | GET | /ai-analysis/{analysis_id} | 已认证 |
| F3-014 | 获取预警规则列表 | GET | /warning-rules | 已认证 |
| F3-015 | 获取预警规则详情 | GET | /warning-rules/{rule_id} | 已认证 |
| F3-016 | 创建预警规则 | POST | /warning-rules | 管理权限 |
| F3-017 | 更新预警规则 | PUT | /warning-rules/{rule_id} | 管理权限 |
| F3-018 | 删除预警规则 | DELETE | /warning-rules/{rule_id} | 管理权限 |
| F3-019 | 验证AI分析结果 | POST | /ai-analysis/{analysis_id}/verify | 普通操作 |

---

### F3-001 获取监测点列表

**接口编号**：F3-001  
**接口名称**：获取监测点列表  
**路径**：`GET /api/v1/monitoring-points`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| point_id | String | 否 | 监测点编码 |
| unit_code | String | 否 | 联网单位编码 |
| system_type | String | 否 | 消防系统类型 |
| device_type | String | 否 | 设备类型编码 |
| status | Integer | 否 | 状态：1=正常/2=故障/3=停用 |
| online_status | Integer | 否 | 在线状态：1=在线/0=离线 |
| keyword | String | 否 | 关键词搜索 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "point_id": "4401060101010001",
        "unit_code": "440106",
        "unit_name": "XX商场",
        "system_type": "01",
        "system_type_name": "火灾探测报警系统",
        "device_type": "40",
        "device_type_name": "感烟探测器",
        "device_serial": "000001",
        "location": "3层301商铺",
        "status": 1,
        "status_name": "正常",
        "online_status": 1,
        "online_status_name": "在线",
        "last_report_time": "2026-06-15T10:29:55+08:00",
        "install_date": "2024-01-15",
        "warranty_expire": "2027-01-15"
      }
    ],
    "total": 10000,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- GB/T 26875.1-2026 附录B.2：设施编码规则（20位）
- GB/T 26875.1-2026 6.3.2：地址编码层级规范

---

### F3-006 获取实时状态

**接口编号**：F3-006  
**接口名称**：获取实时状态  
**路径**：`GET /api/v1/monitoring-points/{point_id}/realtime`  
**权限要求**：已认证

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| point_id | String | 是 | 监测点编码（20位） |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "point_id": "4401060101010001",
    "device_name": "感烟探测器",
    "device_type": "40",
    "location": "3层301商铺",
    "status_type": 1,
    "status_type_name": "正常",
    "status_value": {
      "smoke_level": 0.02,
      "temperature": 25.3,
      "sensitivity": "正常"
    },
    "alarm_level": null,
    "alarm_level_name": null,
    "collect_time": "2026-06-15T10:29:55+08:00",
    "receive_time": "2026-06-15T10:29:55+08:00",
    "signal_strength": -65,
    "power_status": "正常"
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- GB/T 26875.1-2026 B.3.1：状态数据格式
- GB 50440-2007 5.2.3：火警信息接收延迟≤10秒

---

### F3-009 获取在线率统计

**接口编号**：F3-009  
**接口名称**：获取在线率统计  
**路径**：`GET /api/v1/stats/online-rate`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| stat_type | String | 否 | 统计类型：daily/weekly/monthly，默认daily |
| stat_date | Date | 否 | 统计日期，默认当天 |
| unit_code | String | 否 | 联网单位编码 |
| device_type | String | 否 | 设备类型 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "stat_type": "daily",
    "stat_date": "2026-06-15",
    "unit_code": "440106",
    "unit_name": "XX商场",
    "total_count": 500,
    "online_count": 485,
    "offline_count": 15,
    "online_rate": 97.00,
    "threshold": 95.00,
    "is_alarm": false,
    "alarm_level": null,
    "device_type_stats": [
      {
        "device_type": "40",
        "device_type_name": "感烟探测器",
        "total_count": 200,
        "online_count": 195,
        "online_rate": 97.50
      }
    ],
    "building_stats": [
      {
        "building_id": "B001",
        "building_name": "1号楼",
        "total_count": 100,
        "online_count": 98,
        "online_rate": 98.00
      }
    ]
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- T/GDCAA 016-2025 6.3.4：在线率阈值95%
- GB 50440-2007 5.3.2：低于阈值时生成预警

---

### F3-012 获取AI分析结果

**接口编号**：F3-012  
**接口名称**：获取AI分析结果  
**路径**：`GET /api/v1/ai-analysis/results`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| analysis_type | String | 否 | 分析类型：channel_blocking/absence/risk_prediction |
| camera_id | String | 否 | 摄像头ID |
| is_valid | Integer | 否 | 是否有效：1=有效/0=误报 |
| start_time | DateTime | 否 | 开始时间 |
| end_time | DateTime | 否 | 结束时间 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "analysis_id": "AI20260615100001",
        "analysis_type": "channel_blocking",
        "analysis_type_name": "消防通道堵塞",
        "camera_id": "CAM001",
        "camera_name": "地下车库A区",
        "unit_name": "XX商场",
        "location_desc": "地下车库A区主通道",
        "analysis_result": {
          "is_blocked": true,
          "occupancy_rate": 65.5,
          "confidence": 0.95,
          "block_objects": [
            {"type": "car", "confidence": 0.95, "bbox": [120, 80, 200, 150]}
          ]
        },
        "alarm_threshold": 40.0,
        "screenshot_url": "https://storage.example.com/ai/20260615/001.jpg",
        "analyze_time": "2026-06-15T10:00:05+08:00",
        "is_valid": 1,
        "is_valid_name": "有效"
      }
    ],
    "total": 50,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- T/GDCAA 016-2025 6.3.5：AI预警准确率≥85%
- T/CFPA 052-2026 5.3：算法模型在线更新

---

### F3-016 创建预警规则

**接口编号**：F3-016  
**接口名称**：创建预警规则  
**路径**：`POST /api/v1/warning-rules`  
**权限要求**：管理权限

#### 请求体

```json
{
  "rule_name": "管网压力过低预警",
  "rule_type": "threshold",
  "target_type": "device",
  "target_device_types": ["05", "06"],
  "condition": {
    "metric": "pressure",
    "operator": "lt",
    "threshold": 0.5,
    "unit": "MPa",
    "duration_seconds": 60
  },
  "alarm_level": 2,
  "notify_channels": ["sms", "app", "phone"],
  "notify_roles": ["duty_watcher", "supervisor"],
  "is_enabled": true,
  "remark": "当管网压力低于0.5MPa持续60秒时触发"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| rule_name | String | 是 | 规则名称 |
| rule_type | String | 是 | 规则类型：threshold/time_window/trend |
| target_type | String | 是 | 目标类型：device/device_type/building |
| target_device_types | Array | 条件 | 目标设备类型列表 |
| condition | Object | 是 | 触发条件 |
| alarm_level | Integer | 是 | 告警级别：1=低/2=中/3=高/4=紧急 |
| notify_channels | Array | 是 | 通知渠道：sms/app/phone/email |
| notify_roles | Array | 是 | 通知角色 |
| is_enabled | Boolean | 否 | 是否启用，默认true |
| remark | String | 否 | 备注 |

#### 条件对象 (condition)

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| metric | String | 是 | 监控指标：pressure/temperature/smoke_level |
| operator | String | 是 | 操作符：gt/lt/eq/gte/lte |
| threshold | Number | 是 | 阈值 |
| unit | String | 是 | 单位 |
| duration_seconds | Integer | 否 | 持续时间（秒） |

#### 响应结构

```json
{
  "code": 0,
  "message": "预警规则创建成功",
  "data": {
    "rule_id": "RULE20260615001",
    "rule_name": "管网压力过低预警",
    "rule_type": "threshold",
    "is_enabled": true,
    "created_by": "U001",
    "created_by_name": "管理员",
    "created_time": "2026-06-15T10:30:00+08:00"
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

---

## 6. F4 运维管理

### 6.1 接口列表

| 接口编号 | 接口名称 | HTTP方法 | 路径 | 权限要求 |
|---------|---------|----------|------|---------|
| F4-001 | 获取设备资产列表 | GET | /device-assets | 已认证 |
| F4-002 | 获取设备资产详情 | GET | /device-assets/{device_id} | 已认证 |
| F4-003 | 创建设备资产 | POST | /device-assets | 管理权限 |
| F4-004 | 更新设备资产 | PUT | /device-assets/{device_id} | 管理权限 |
| F4-005 | 删除设备资产 | DELETE | /device-assets/{device_id} | 管理权限 |
| F4-006 | 获取设备分类统计 | GET | /device-assets/stats | 已认证 |
| F4-007 | 获取设备生命周期 | GET | /device-assets/{device_id}/lifecycle | 已认证 |
| F4-008 | 更新设备生命周期状态 | POST | /device-assets/{device_id}/lifecycle | 管理权限 |
| F4-009 | 获取备品备件列表 | GET | /spare-parts | 已认证 |
| F4-010 | 获取备品备件详情 | GET | /spare-parts/{spare_id} | 已认证 |
| F4-011 | 创建备品备件 | POST | /spare-parts | 管理权限 |
| F4-012 | 更新备品备件 | PUT | /spare-parts/{spare_id} | 管理权限 |
| F4-013 | 备品入库 | POST | /spare-parts/{spare_id}/stock-in | 管理权限 |
| F4-014 | 备品出库 | POST | /spare-parts/{spare_id}/stock-out | 管理权限 |
| F4-015 | 获取工单列表 | GET | /work-orders | 已认证 |
| F4-016 | 获取工单详情 | GET | /work-orders/{work_order_id} | 已认证 |
| F4-017 | 创建工单 | POST | /work-orders | 普通操作 |
| F4-018 | 派发工单 | POST | /work-orders/{work_order_id}/dispatch | 普通操作 |
| F4-019 | 接收工单 | POST | /work-orders/{work_order_id}/accept | 操作权限 |
| F4-020 | 拒绝工单 | POST | /work-orders/{work_order_id}/reject | 操作权限 |
| F4-021 | 开始处理 | POST | /work-orders/{work_order_id}/process | 操作权限 |
| F4-022 | 提交维保记录 | POST | /work-orders/{work_order_id}/maintenance-record | 操作权限 |
| F4-023 | 提交维保报告 | POST | /work-orders/{work_order_id}/report | 操作权限 |
| F4-024 | 验收工单 | POST | /work-orders/{work_order_id}/verify | 普通操作 |
| F4-025 | 评价工单 | POST | /work-orders/{work_order_id}/rate | 普通操作 |
| F4-026 | 关闭工单 | POST | /work-orders/{work_order_id}/close | 普通操作 |
| F4-027 | 获取巡检路线列表 | GET | /inspection-routes | 已认证 |
| F4-028 | 获取巡检路线详情 | GET | /inspection-routes/{route_id} | 已认证 |
| F4-029 | 创建巡检路线 | POST | /inspection-routes | 管理权限 |
| F4-030 | 更新巡检路线 | PUT | /inspection-routes/{route_id} | 管理权限 |
| F4-031 | 删除巡检路线 | DELETE | /inspection-routes/{route_id} | 管理权限 |
| F4-032 | 获取巡检任务列表 | GET | /inspection-tasks | 已认证 |
| F4-033 | 获取巡检任务详情 | GET | /inspection-tasks/{task_id} | 已认证 |
| F4-034 | 开始巡检 | POST | /inspection-tasks/{task_id}/start | 普通操作 |
| F4-035 | 点位签到 | POST | /inspection-tasks/{task_id}/checkin | 普通操作 |
| F4-036 | 提交巡检记录 | POST | /inspection-tasks/{task_id}/submit | 普通操作 |
| F4-037 | 获取值班排班列表 | GET | /duty-schedules | 已认证 |
| F4-038 | 获取排班详情 | GET | /duty-schedules/{schedule_id} | 已认证 |
| F4-039 | 创建排班 | POST | /duty-schedules | 管理权限 |
| F4-040 | 更新排班 | PUT | /duty-schedules/{schedule_id} | 管理权限 |
| F4-041 | 删除排班 | DELETE | /duty-schedules/{schedule_id} | 管理权限 |
| F4-042 | 获取交接班记录 | GET | /shift-handoffs | 已认证 |
| F4-043 | 创建交接班记录 | POST | /shift-handoffs | 普通操作 |
| F4-044 | 确认交接班 | POST | /shift-handoffs/{handoff_id}/confirm | 普通操作 |
| F4-045 | 获取证照列表 | GET | /certificates | 已认证 |
| F4-046 | 获取证照详情 | GET | /certificates/{cert_id} | 已认证 |
| F4-047 | 创建证照 | POST | /certificates | 管理权限 |
| F4-048 | 更新证照 | PUT | /certificates/{cert_id} | 管理权限 |
| F4-049 | 获取证照到期提醒 | GET | /certificates/expiring | 已认证 |

---

### F4-001 获取设备资产列表

**接口编号**：F4-001  
**接口名称**：获取设备资产列表  
**路径**：`GET /api/v1/device-assets`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| device_id | String | 否 | 设备编码 |
| device_name | String | 否 | 设备名称 |
| device_type | Integer | 否 | 设备种类（1-15） |
| status | String | 否 | 设备状态 |
| building_id | String | 否 | 建筑ID |
| manufacturer | String | 否 | 生产厂家 |
| keyword | String | 否 | 关键词搜索 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "device_id": "DA001",
        "device_name": "感烟探测器#001",
        "device_type": 1,
        "device_type_name": "火灾探测报警系统",
        "device_model": "JTW-ZD-401",
        "manufacturer": "某某消防",
        "serial_number": "SN20240001",
        "address_code": "4401060101010001",
        "install_location": "3层301商铺",
        "install_date": "2024-01-15",
        "activate_date": "2024-02-01",
        "warranty_expire": "2027-02-01",
        "service_life": 10,
        "lifecycle_status": "RUNNING",
        "lifecycle_status_name": "运行",
        "building_id": "B001",
        "building_name": "1号楼",
        "floor": "3F",
        "create_time": "2024-01-20T10:00:00+08:00"
      }
    ],
    "total": 1000,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- T/GDCAA 016-2025 5.2.2：设备15类分类标准
- GB/T 26875.1-2026 6.3：地址编码规范

---

### F4-015 获取工单列表

**接口编号**：F4-015  
**接口名称**：获取工单列表  
**路径**：`GET /api/v1/work-orders`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| work_order_id | String | 否 | 工单编号 |
| work_order_type | Integer | 否 | 工单类型：1=故障/2=定期 |
| priority | String | 否 | 优先级：P1/P2/P3 |
| status | String | 否 | 工单状态 |
| building_id | String | 否 | 建筑ID |
| device_id | String | 否 | 设备ID |
| assign_unit | String | 否 | 派发单位 |
| start_time | DateTime | 否 | 开始时间 |
| end_time | DateTime | 否 | 结束时间 |
| keyword | String | 否 | 关键词搜索 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "work_order_id": "WO20260615001",
        "work_order_type": 1,
        "work_order_type_name": "故障",
        "trigger_type": 1,
        "trigger_type_name": "自动",
        "source_id": "FA20260615100002",
        "priority": "P1",
        "priority_name": "紧急",
        "building_id": "B001",
        "building_name": "1号楼",
        "device_id": "D001",
        "device_name": "消火栓泵#1",
        "description": "水泵启动异常",
        "assign_unit": "某某维保公司",
        "assign_time": "2026-06-15T09:00:00+08:00",
        "due_time": "2026-06-15T21:00:00+08:00",
        "status": "PROCESSING",
        "status_name": "处理中",
        "overdue_flag": false,
        "create_by": "U001",
        "create_by_name": "张三",
        "create_time": "2026-06-15T09:00:00+08:00"
      }
    ],
    "total": 50,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- T/GDCAA 016-2025 6.1.3：故障维保2小时响应，24小时修复
- GB 25201-2010 8.2：定期维保计划

---

### F4-027 获取巡检路线列表

**接口编号**：F4-027  
**接口名称**：获取巡检路线列表  
**路径**：`GET /api/v1/inspection-routes`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| route_id | String | 否 | 路线编号 |
| route_name | String | 否 | 路线名称 |
| building_id | String | 否 | 建筑ID |
| status | Integer | 否 | 状态：1=启用/0=停用 |
| frequency | Integer | 否 | 频次：1=日/2=周/3=月 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "route_id": "IR20260615001",
        "route_name": "1号楼日常巡检路线",
        "building_id": "B001",
        "building_name": "1号楼",
        "floor_range": "1-10层",
        "checkpoint_count": 50,
        "estimated_duration": 60,
        "checkpoint_time_limit": 3,
        "frequency": 1,
        "frequency_name": "每日",
        "executors": [
          {"user_id": "U001", "user_name": "张三"},
          {"user_id": "U002", "user_name": "李四"}
        ],
        "status": 1,
        "status_name": "启用",
        "last_execution": {
          "task_id": "IT20260614001",
          "execution_time": "2026-06-14T09:00:00+08:00",
          "inspector_name": "张三",
          "coverage_rate": 100.00
        }
      }
    ],
    "total": 20,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- T/GDCAA 016-2025 7.1.1：巡检路线覆盖消防设施
- T/GDCAA 016-2025 7.1.2：巡检点位二维码配置

---

### F4-037 获取值班排班列表

**接口编号**：F4-037  
**接口名称**：获取值班排班列表  
**路径**：`GET /api/v1/duty-schedules`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| schedule_id | String | 否 | 排班编号 |
| building_id | String | 否 | 建筑ID |
| shift_type | Integer | 否 | 班次：1=早/2=中/3=夜 |
| schedule_date | Date | 否 | 值班日期 |
| start_date | Date | 否 | 开始日期 |
| end_date | Date | 否 | 结束日期 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "schedule_id": "DS20260615001",
        "building_id": "B001",
        "building_name": "1号楼消控室",
        "shift_type": 1,
        "shift_type_name": "早班",
        "shift_time": "08:00-16:00",
        "duration": 8,
        "schedule_date": "2026-06-15",
        "schedule_status": 1,
        "schedule_status_name": "已确认",
        "shift_leader": {
          "user_id": "U001",
          "user_name": "张三",
          "cert_status": 1,
          "cert_status_name": "有效"
        },
        "shift_members": [
          {"user_id": "U001", "user_name": "张三", "cert_level": "四级"},
          {"user_id": "U002", "user_name": "李四", "cert_level": "四级"}
        ],
        "create_by": "U003",
        "create_time": "2026-06-10T10:00:00+08:00"
      }
    ],
    "total": 30,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- GB 25506-2010 5.1.1：三班八小时轮值
- GB 25506-2010 5.1.2：每班时长不超过8小时
- GB 25506-2010 4.2：持证消防设施操作员

---

### F4-042 获取交接班记录

**接口编号**：F4-042  
**接口名称**：获取交接班记录  
**路径**：`GET /api/v1/shift-handoffs`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| handoff_id | String | 否 | 交接编号 |
| building_id | String | 否 | 建筑ID |
| shift_date | Date | 否 | 交接班日期 |
| start_time | DateTime | 否 | 开始时间 |
| end_time | DateTime | 否 | 结束时间 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "handoff_id": "SH20260615001",
        "shift_out_id": "U001",
        "shift_out_name": "张三",
        "shift_in_id": "U002",
        "shift_in_name": "李四",
        "handoff_time": "2026-06-15T08:00:00+08:00",
        "pending_issues": [
          {
            "issue_id": "PI001",
            "issue_type": "fault",
            "description": "3层感烟探测器频繁误报",
            "priority": "P2",
            "status": "处理中"
          }
        ],
        "system_status": {
          "fire_alarm": "正常",
          "water_system": "正常",
          "power_system": "正常"
        },
        "today_alerts_count": 5,
        "today_fires_count": 0,
        "shift_out_signature": "https://storage.example.com/signatures/shift_out.png",
        "shift_in_signature": "https://storage.example.com/signatures/shift_in.png",
        "status": "completed"
      }
    ],
    "total": 100,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- GB 25506-2010 5.2.3：交接班双方签字确认
- GB 25506-2010 5.2.1：遗留问题移交记录
- GB 25506-2010 9.1：交接班记录保存≥3年

---

## 7. F5 数据与报表

### 7.1 接口列表

| 接口编号 | 接口名称 | HTTP方法 | 路径 | 权限要求 |
|---------|---------|----------|------|---------|
| F5-001 | 获取值班记录台账 | GET | /ledgers/duty-records | 已认证 |
| F5-002 | 获取火警处置台账 | GET | /ledgers/fire-records | 已认证 |
| F5-003 | 获取故障整改台账 | GET | /ledgers/fault-records | 已认证 |
| F5-004 | 获取巡检记录台账 | GET | /ledgers/inspection-records | 已认证 |
| F5-005 | 获取操作日志台账 | GET | /ledgers/operation-logs | 已认证 |
| F5-006 | 获取培训演练台账 | GET | /ledgers/training-records | 已认证 |
| F5-007 | 获取运行数据台账 | GET | /ledgers/runtime-data | 已认证 |
| F5-008 | 获取日报 | GET | /reports/daily | 已认证 |
| F5-009 | 生成日报 | POST | /reports/daily/generate | 管理权限 |
| F5-010 | 获取月报 | GET | /reports/monthly | 已认证 |
| F5-011 | 生成月报 | POST | /reports/monthly/generate | 管理权限 |
| F5-012 | 获取季报 | GET | /reports/quarterly | 已认证 |
| F5-013 | 生成季报 | POST | /reports/quarterly/generate | 管理权限 |
| F5-014 | 获取年报 | GET | /reports/annual | 已认证 |
| F5-015 | 生成年报 | POST | /reports/annual/generate | 管理权限 |
| F5-016 | 审核报表 | POST | /reports/{report_id}/approve | 管理权限 |
| F5-017 | 签发报表 | POST | /reports/{report_id}/sign | 管理权限 |
| F5-018 | 导出报表 | GET | /reports/{report_id}/export | 已认证 |
| F5-019 | 验证数据签名 | POST | /data-signature/verify | 已认证 |
| F5-020 | 获取数据签名详情 | GET | /data-signature/{data_id} | 已认证 |
| F5-021 | 获取备份记录 | GET | /backup-records | 管理权限 |
| F5-022 | 手动备份 | POST | /backup-records/backup | 管理权限 |
| F5-023 | 恢复备份 | POST | /backup-records/{backup_id}/restore | 管理权限 |
| F5-024 | 获取数据完整性报告 | GET | /data-integrity/report | 管理权限 |

---

### F5-001 获取值班记录台账

**接口编号**：F5-001  
**接口名称**：获取值班记录台账  
**路径**：`GET /api/v1/ledgers/duty-records`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| record_id | String | 否 | 记录编号 |
| building_id | String | 否 | 建筑ID |
| shift_type | Integer | 否 | 班次：1=早/2=中/3=夜 |
| record_date | Date | 否 | 值班日期 |
| start_date | Date | 否 | 开始日期 |
| end_date | Date | 否 | 结束日期 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "record_id": "DR20260615001",
        "building_id": "B001",
        "building_name": "1号楼消控室",
        "record_date": "2026-06-15",
        "shift_type": 1,
        "shift_type_name": "早班",
        "shift_leader": {
          "user_id": "U001",
          "user_name": "张三"
        },
        "shift_members": [
          {"user_id": "U001", "user_name": "张三"},
          {"user_id": "U002", "user_name": "李四"}
        ],
        "duty_start_time": "2026-06-15T08:00:00+08:00",
        "duty_end_time": "2026-06-15T16:00:00+08:00",
        "handoff_records": [
          {
            "handoff_time": "2026-06-15T08:00:00+08:00",
            "shift_out_name": "王五",
            "shift_in_name": "张三"
          }
        ],
        "device_status_summary": {
          "fire_alarm": "正常",
          "sprinkler": "正常",
          "power": "正常"
        },
        "daily_alerts_count": 5,
        "daily_fires_count": 0,
        "sign_by": "U001",
        "sign_time": "2026-06-15T16:00:00+08:00",
        "archive_status": 1,
        "archive_status_name": "已归档",
        "archive_time": "2026-06-15T23:59:00+08:00",
        "create_time": "2026-06-15T16:00:00+08:00"
      }
    ],
    "total": 365,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- GB 25506-2010 5.3：值班记录每日归档
- GB 25506-2010 9.1：保存期限≥3年
- T/CFPA 052-2026 7.2：归档后不可修改

---

### F5-008 获取日报

**接口编号**：F5-008  
**接口名称**：获取日报  
**路径**：`GET /api/v1/reports/daily`  
**权限要求**：已认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| report_date | Date | 否 | 报表日期，默认当天 |
| building_id | String | 否 | 建筑ID |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "report_id": "DRPT20260615B001",
    "report_type": "daily",
    "report_date": "2026-06-15",
    "building_id": "B001",
    "building_name": "1号楼",
    "report_status": "generated",
    "report_status_name": "已生成",
    "content": {
      "alert_stats": {
        "fire_alerts": 5,
        "fault_alerts": 12,
        "warnings": 3,
        "handled_count": 18,
        "pending_count": 2,
        "response_time_avg": 28
      },
      "device_stats": {
        "total_devices": 500,
        "online_count": 485,
        "online_rate": 97.00,
        "fault_count": 2
      },
      "duty_stats": {
        "shift_1": {"leader": "张三", "status": "正常"},
        "shift_2": {"leader": "李四", "status": "正常"},
        "shift_3": {"leader": "王五", "status": "正常"}
      },
      "inspection_stats": {
        "planned": 3,
        "completed": 3,
        "coverage_rate": 100.00
      },
      "work_order_stats": {
        "new_orders": 5,
        "completed": 4,
        "pending": 1
      },
      "abnormal_events": [
        {
          "event_type": "设备故障",
          "device": "3层感烟探测器",
          "description": "频繁误报",
          "status": "处理中"
        }
      ]
    },
    "signature": {
      "hash_value": "daily_hash_001",
      "sign_time": "2026-06-16T00:05:00+08:00"
    },
    "created_time": "2026-06-16T00:00:00+08:00"
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- T/GDCAA 016-2025 10.2.1：日报内容要求
- T/GDCAA 016-2025 10.2.3：自动推送责任人

---

### F5-019 验证数据签名

**接口编号**：F5-019  
**接口名称**：验证数据签名  
**路径**：`POST /api/v1/data-signature/verify`  
**权限要求**：已认证

#### 请求体

```json
{
  "data_type": "fire_alert",
  "data_id": "FA20260615100001",
  "verify_chain": true
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| data_type | String | 是 | 数据类型：fire_alert/control_command/operation_log |
| data_id | String | 是 | 数据ID |
| verify_chain | Boolean | 否 | 是否验证链式完整性，默认false |

#### 响应结构

```json
{
  "code": 0,
  "message": "数据签名验证通过",
  "data": {
    "data_type": "fire_alert",
    "data_id": "FA20260615100001",
    "is_valid": true,
    "signature": {
      "hash_value": "alarm_hash_001",
      "sign_time": "2026-06-15T10:00:08+08:00",
      "sign_method": "SHA-256"
    },
    "chain_valid": true,
    "prev_hash": "alarm_hash_000",
    "integrity_level": "high",
    "verify_time": "2026-06-15T10:30:00+08:00"
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- T/CFPA 052-2026 8.3：数据签名防篡改
- T/CFPA 052-2026 8.5：完整性定期校验

---

### F5-021 获取备份记录

**接口编号**：F5-021  
**接口名称**：获取备份记录  
**路径**：`GET /api/v1/backup-records`  
**权限要求**：管理权限

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| page_size | Integer | 否 | 每页条数 |
| backup_type | String | 否 | 备份类型：full/incremental/archive |
| status | Integer | 否 | 状态：1=成功/2=失败/3=进行中 |
| start_time | DateTime | 否 | 开始时间 |
| end_time | DateTime | 否 | 结束时间 |

#### 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "backup_id": "BK20260601001",
        "backup_type": "full",
        "backup_type_name": "全量备份",
        "backup_time": "2026-06-01T02:00:00+08:00",
        "status": 1,
        "status_name": "成功",
        "file_size": "50GB",
        "duration": 3600,
        "data_types": ["fire_alert", "control_command", "operation_log"],
        "storage_location": "异地存储",
        "checksum": "abc123...",
        "restore_point": true,
        "restore_point_time": "2026-06-01T02:00:00+08:00",
        "created_by": "system",
        "created_time": "2026-06-01T02:00:00+08:00"
      }
    ],
    "total": 100,
    "page": 1,
    "page_size": 20
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

#### 合规标注

- T/CFPA 052-2026 9.1：月全量+周增量备份
- T/CFPA 052-2026 9.2：异地存储要求

---

## 附录A：错误码完整列表

### 通用错误 (1xxxx)

| 错误码 | 说明 |
|--------|------|
| 10000 | 成功 |
| 10001 | 参数错误 |
| 10002 | 未授权访问 |
| 10003 | 禁止操作 |
| 10004 | 资源不存在 |
| 10005 | 资源已存在 |
| 10006 | 操作失败 |
| 10007 | 系统繁忙 |

### F1 火警监测错误 (2xxxx)

| 错误码 | 说明 |
|--------|------|
| 20001 | 火警已确认，无法重复操作 |
| 20002 | 火警已闭环，无法修改 |
| 20003 | 告警不存在 |
| 20004 | 告警状态不允许此操作 |
| 20005 | 核实超时 |
| 20006 | 通知发送失败 |
| 20007 | 119拨打失败 |
| 20008 | 应急预案执行失败 |
| 20009 | 误报原因不能为空 |
| 20010 | 处置步骤执行失败 |

### F2 远程控制错误 (3xxxx)

| 错误码 | 说明 |
|--------|------|
| 30001 | 双人复核未通过 |
| 30002 | 授权码无效或已过期 |
| 30003 | 设备离线，命令无法下发 |
| 30004 | 设备不支持此命令 |
| 30005 | 授权码与复核任务不匹配 |
| 30006 | 复核任务已过期 |
| 30007 | 操作员与复核员不能相同 |
| 30008 | 通讯保持超时 |
| 30009 | 控制命令执行失败 |
| 30010 | 设备状态查询失败 |

### F3 监测预警错误 (4xxxx)

| 错误码 | 说明 |
|--------|------|
| 40001 | 监测点不存在 |
| 40002 | 阈值配置错误 |
| 40003 | 预警规则不存在 |
| 40004 | AI分析结果不存在 |
| 40005 | 设备类型不支持 |
| 40006 | 数据采集失败 |
| 40007 | 统计计算失败 |

### F4 运维管理错误 (5xxxx)

| 错误码 | 说明 |
|--------|------|
| 50001 | 工单不存在 |
| 50002 | 工单状态不允许此操作 |
| 50003 | 设备资产不存在 |
| 50004 | 设备生命周期状态不允许转换 |
| 50005 | 排班冲突 |
| 50006 | 人员资质不符合要求 |
| 50007 | 交接班未完成 |
| 50008 | 证照已过期 |
| 50009 | 备品库存不足 |
| 50010 | 巡检点位不匹配 |

### F5 数据报表错误 (6xxxx)

| 错误码 | 说明 |
|--------|------|
| 60001 | 报表生成失败 |
| 60002 | 数据签名校验失败 |
| 60003 | 报表不存在 |
| 60004 | 报表已锁定，无法修改 |
| 60005 | 备份失败 |
| 60006 | 恢复失败 |
| 60007 | 数据完整性校验失败 |
| 60008 | 导出失败 |
| 60009 | 数据已归档，无法修改 |

---

## 附录B：数据类型与保护级别

| 数据类型 | 保护级别 | 存储要求 | 说明 |
|---------|---------|---------|------|
| 火警记录 | 最高 | 永久 | T/CFPA 052-2026 7.1 |
| 控制指令 | 最高 | 永久 | T/CFPA 052-2026 7.1 |
| 操作日志 | 高 | ≥5年 | GB 25201-2010 7.3 |
| 告警记录 | 高 | ≥5年 | T/GDCAA 016-2025 9.7 |
| 值班记录 | 高 | ≥3年 | GB 25506-2010 9.1 |
| 维保记录 | 中 | ≥3年 | GB 25201-2010 9.2 |
| 巡检记录 | 中 | ≥3年 | GB 25201-2010 9.2 |
| 运行数据 | 中 | ≥5年 | GB/T 26875.1-2026 10.3 |

---

## 附录C：版本历史

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|---------|--------|
| V1.0 | 2026-06-15 | 初始版本，覆盖F1-F5模块共约100个接口 | API设计团队 |

---

**文档结束**


---


> **适用范围**：F6 视频联动、F7 查岗管理、F8 GIS态势感知、F9 权限与安全、F10 移动端、WebSocket实时接口  
> **标准依据**：GB/T 26875.1-2026、GB 25506-2010、GB 25201-2010、T/CFPA 052-2026、T/GDCAA 016-2025

---

## 目录

1. [全局接口规范](#1-全局接口规范)
2. [F6 视频联动模块](#2-f6-视频联动模块)
3. [F7 查岗管理模块](#3-f7-查岗管理模块)
4. [F8-gis态势感知模块](#4-f8-gis态势感知模块)
5. [F9-权限与安全模块](#5-f9-权限与安全模块)
6. [F10-移动端专用模块](#6-f10-移动端专用模块)
7. [WebSocket实时接口](#7-websocket实时接口)
8. [错误码汇总表](#8-错误码汇总表)

---

## 1. 全局接口规范

### 1.1 基础规范

| 规范项 | 规则 |
|--------|------|
| 基础路径 | `/api/v1` |
| 接口风格 | RESTful风格，资源名使用复数形式 |
| 认证方式 | Bearer Token (JWT) |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 时间格式 | ISO 8601 (如：`2026-06-15T10:30:00+08:00`) |
| 字段命名 | snake_case (全小写，单词间用下划线分隔) |

### 1.2 请求规范

#### 请求头 (Request Headers)

| Header | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {access_token}` |
| Content-Type | 是 | `application/json` |
| Accept | 否 | `application/json` |
| X-Request-ID | 否 | 请求唯一标识，用于链路追踪 |
| X-Terminal-Type | 移动端必填 | 终端类型：`web/ios/android/harmony` |
| X-App-Version | 移动端必填 | APP版本号 |

#### 请求参数位置

| 位置 | 说明 | 示例 |
|------|------|------|
| Path | URL路径参数 | `/api/v1/devices/{device_id}` |
| Query | URL查询参数 | `/api/v1/devices?page=1&page_size=20` |
| Body | 请求体参数 | POST/PUT/PATCH请求的JSON body |

### 1.3 响应规范

#### 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | integer | 业务状态码，0=成功，其他=失败 |
| message | string | 状态描述信息 |
| data | object/null | 响应数据 |
| timestamp | string | 响应时间戳 |

#### 分页响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "page_size": 20,
    "total_pages": 5
  },
  "timestamp": "2026-06-15T10:30:00+08:00"
}
```

### 1.4 分页规范

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | integer | 1 | 当前页码 |
| page_size | integer | 20 | 每页条数，最大100 |

### 1.5 通用字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 实体唯一标识(UUID) |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |
| is_deleted | boolean | 逻辑删除标记 |

---

## 2. F6 视频联动模块

### 2.1 视频设备管理

#### API-F6-001: 创建视频设备

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-001 |
| 接口名称 | 创建视频设备 |
| 路径 | `/video/devices` |
| HTTP方法 | POST |
| 权限要求 | `video:device:create` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 校验规则 | 说明 |
|--------|------|------|----------|------|
| device_code | string | 是 | 长度≤32, 唯一 | 设备编码(20位标准编码) |
| device_name | string | 是 | 长度≤100 | 设备名称 |
| device_type | integer | 是 | 范围[1-5] | 设备类型: 1-枪机 2-球机 3-半球 4-NVR 5-视频服务器 |
| manufacturer | string | 否 | 长度≤50 | 厂商 |
| model | string | 否 | 长度≤50 | 型号 |
| ip_address | string | 是 | 有效IPv4/IPv6 | IP地址 |
| port | integer | 是 | 范围[1-65535] | 端口 |
| username | string | 是 | 长度≤50 | 登录用户名(加密存储) |
| password | string | 是 | 长度≤200 | 登录密码(加密存储) |
| channel_no | integer | 否 | ≥1 | 通道号 |
| rtsp_url | string | 否 | 有效RTSP URL | RTSP地址 |
| unit_id | string | 是 | 存在性校验 | 所属单位ID |
| building_id | string | 否 | 存在性校验 | 所属建筑ID |
| location | string | 否 | 长度≤200 | 安装位置 |
| longitude | decimal | 否 | 范围[-180,180] | 经度 |
| latitude | decimal | 否 | 范围[-90,90] | 纬度 |
| video_type | integer | 是 | 范围[1-5] | 视频类型: 1-消控室 2-消防通道 3-安全出口 4-重点区域 5-安防共享 |

**响应结构 (data)**

```json
{
  "device_id": "string",
  "device_code": "string",
  "device_name": "string",
  "device_type": 1,
  "is_online": false,
  "create_time": "2026-06-15T10:30:00+08:00"
}
```

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 60101 | 设备编码已存在 |
| 60102 | 设备类型不支持 |
| 60103 | RTSP地址格式错误 |
| 60104 | 单位不存在 |

**合规标注**: T/CFPA 052-2026 5.4.1, GA/T 1400 B.2

---

#### API-F6-002: 查询视频设备列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-002 |
| 接口名称 | 查询视频设备列表 |
| 路径 | `/video/devices` |
| HTTP方法 | GET |
| 权限要求 | `video:device:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| unit_id | string | 否 | - | 单位ID筛选 |
| building_id | string | 否 | - | 建筑ID筛选 |
| device_type | integer | 否 | - | 设备类型 |
| video_type | integer | 否 | - | 视频类型 |
| is_online | boolean | 否 | - | 在线状态 |
| keyword | string | 否 | - | 关键字搜索(名称/编码) |

**响应结构 (data.list)**

```json
[{
  "device_id": "string",
  "device_code": "string",
  "device_name": "string",
  "device_type": 1,
  "manufacturer": "string",
  "model": "string",
  "ip_address": "string",
  "port": 554,
  "unit_id": "string",
  "building_id": "string",
  "location": "string",
  "video_type": 1,
  "is_online": true,
  "last_heartbeat": "2026-06-15T10:30:00+08:00",
  "create_time": "2026-06-15T10:30:00+08:00"
}]
```

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 60201 | 查询参数错误 |

---

#### API-F6-003: 查询视频设备详情

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-003 |
| 接口名称 | 查询视频设备详情 |
| 路径 | `/video/devices/{device_id}` |
| HTTP方法 | GET |
| 权限要求 | `video:device:read` |

**请求参数 (Path)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| device_id | string | 是 | 设备ID |

**响应结构 (data)**

```json
{
  "device_id": "string",
  "device_code": "string",
  "device_name": "string",
  "device_type": 2,
  "manufacturer": "string",
  "model": "string",
  "ip_address": "string",
  "port": 554,
  "username": "string",
  "rtsp_url": "string",
  "unit_id": "string",
  "unit_name": "string",
  "building_id": "string",
  "building_name": "string",
  "location": "string",
  "longitude": 113.358642,
  "latitude": 23.132654,
  "video_type": 1,
  "is_online": true,
  "last_heartbeat": "2026-06-15T10:30:00+08:00",
  "stream_urls": {
    "hls": "string",
    "websocket": "string",
    "rtmp": "string"
  },
  "capabilities": {
    "ptz": true,
    "preset": true,
    "talkback": false,
    "recording": true
  },
  "create_time": "2026-06-15T10:30:00+08:00",
  "update_time": "2026-06-15T10:30:00+08:00"
}
```

---

#### API-F6-004: 更新视频设备

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-004 |
| 接口名称 | 更新视频设备 |
| 路径 | `/video/devices/{device_id}` |
| HTTP方法 | PUT |
| 权限要求 | `video:device:update` |

**请求参数 (Path)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| device_id | string | 是 | 设备ID |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 校验规则 | 说明 |
|--------|------|------|----------|------|
| device_name | string | 否 | 长度≤100 | 设备名称 |
| ip_address | string | 否 | 有效IPv4/IPv6 | IP地址 |
| port | integer | 否 | 范围[1-65535] | 端口 |
| username | string | 否 | 长度≤50 | 登录用户名 |
| password | string | 否 | 长度≤200 | 登录密码 |
| location | string | 否 | 长度≤200 | 安装位置 |
| video_type | integer | 否 | 范围[1-5] | 视频类型 |

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 60401 | 设备不存在 |
| 60402 | 更新内容为空 |

---

#### API-F6-005: 删除视频设备

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-005 |
| 接口名称 | 删除视频设备 |
| 路径 | `/video/devices/{device_id}` |
| HTTP方法 | DELETE |
| 权限要求 | `video:device:delete` |

**请求参数 (Path)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| device_id | string | 是 | 设备ID |

**响应结构 (data)**

```json
{
  "device_id": "string"
}
```

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 60501 | 设备不存在 |
| 60502 | 设备有关联录像不可删除 |

---

### 2.2 视频通道关联配置

#### API-F6-006: 创建联动配置

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-006 |
| 接口名称 | 创建联动配置 |
| 路径 | `/video/linkages` |
| HTTP方法 | POST |
| 权限要求 | `video:linkage:create` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_type | string | 是 | 告警类型: fire_alarm/fault/supervision/warning |
| unit_id | string | 否 | 单位ID (NULL表示全辖通用) |
| building_id | string | 否 | 建筑编码 |
| floor_code | string | 否 | 楼层编码 |
| camera_ids | array[string] | 是 | 关联摄像头ID列表 |
| priority | integer | 否 | 关联优先级, 默认1 |
| link_count | integer | 否 | 关联数量, 默认3-5 |
| enable_ai | boolean | 否 | 是否启用AI分析, 默认false |
| ai_types | array[string] | 否 | AI分析类型: fire/smoke/person |
| is_active | boolean | 否 | 是否启用, 默认true |

**响应结构 (data)**

```json
{
  "link_id": "string",
  "alarm_type": "fire_alarm",
  "camera_count": 3,
  "create_time": "2026-06-15T10:30:00+08:00"
}
```

**合规标注**: T/CFPA 052-2026 5.4.2

---

#### API-F6-007: 查询联动配置列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-007 |
| 接口名称 | 查询联动配置列表 |
| 路径 | `/video/linkages` |
| HTTP方法 | GET |
| 权限要求 | `video:linkage:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| alarm_type | string | 否 | - | 告警类型筛选 |
| unit_id | string | 否 | - | 单位ID筛选 |
| is_active | boolean | 否 | - | 启用状态 |

---

#### API-F6-008: 更新联动配置

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-008 |
| 接口名称 | 更新联动配置 |
| 路径 | `/video/linkages/{link_id}` |
| HTTP方法 | PUT |
| 权限要求 | `video:linkage:update` |

---

#### API-F6-009: 删除联动配置

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-009 |
| 接口名称 | 删除联动配置 |
| 路径 | `/video/linkages/{link_id}` |
| HTTP方法 | DELETE |
| 权限要求 | `video:linkage:delete` |

---

### 2.3 实时预览

#### API-F6-010: 获取实时预览地址

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-010 |
| 接口名称 | 获取实时预览地址 |
| 路径 | `/video/devices/{device_id}/stream` |
| HTTP方法 | GET |
| 权限要求 | `video:stream:read` |

**请求参数 (Path)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| device_id | string | 是 | 设备ID |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| stream_type | string | 否 | websocket | 流类型: hls/websocket/rtmp |
| quality | string | 否 | auto | 画质: auto/fluent/HD/ultra |

**响应结构 (data)**

```json
{
  "device_id": "string",
  "stream_url": "string",
  "stream_type": "websocket",
  "expires_in": 3600,
  "ws_url": "string"
}
```

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 61001 | 设备不在线 |
| 61002 | 流服务异常 |

**合规标注**: T/CFPA 052-2026 5.4.5 (延迟要求≤500ms)

---

#### API-F6-011: 云台控制

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-011 |
| 接口名称 | 云台控制 |
| 路径 | `/video/devices/{device_id}/ptz` |
| HTTP方法 | POST |
| 权限要求 | `video:ptz:control` |

**请求参数 (Path)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| device_id | string | 是 | 设备ID |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| action | string | 是 | 操作: start/stop |
| command | string | 是 | 控制命令: UP/DOWN/LEFT/RIGHT/ZOOM_IN/ZOOM_OUT/PRESET_CALL |
| speed | integer | 否 | 速度, 范围[1-64], 默认32 |
| preset_no | integer | 否 | 预置点编号 |

**响应结构 (data)**

```json
{
  "device_id": "string",
  "action": "start",
  "command": "UP",
  "result": "success"
}
```

---

#### API-F6-012: 批量获取预览地址

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-012 |
| 接口名称 | 批量获取预览地址 |
| 路径 | `/video/devices/batch-stream` |
| HTTP方法 | POST |
| 权限要求 | `video:stream:read` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| device_ids | array[string] | 是 | 设备ID列表, 最大36个 |
| stream_type | string | 否 | 流类型 |
| quality | string | 否 | 画质 |

**响应结构 (data.list)**

```json
[{
  "device_id": "string",
  "device_name": "string",
  "stream_url": "string",
  "is_online": true
}]
```

**合规标注**: T/CFPA 052-2026 5.4.5 (单用户同时播放≤36路)

---

### 2.4 录像回放

#### API-F6-013: 查询录像列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-013 |
| 接口名称 | 查询录像列表 |
| 路径 | `/video/records` |
| HTTP方法 | GET |
| 权限要求 | `video:record:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| device_id | string | 否 | - | 设备ID |
| building_id | string | 否 | - | 建筑ID |
| record_type | integer | 否 | - | 录像类型: 1-连续 2-告警触发 3-手动 |
| start_time | datetime | 否 | - | 开始时间 |
| end_time | datetime | 否 | - | 结束时间 |
| is_locked | boolean | 否 | - | 锁定状态 |

---

#### API-F6-014: 获取录像回放地址

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-014 |
| 接口名称 | 获取录像回放地址 |
| 路径 | `/video/records/{record_id}/playback` |
| HTTP方法 | GET |
| 权限要求 | `video:record:read` |

**请求参数 (Path)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| record_id | string | 是 | 录像记录ID |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| start_time | datetime | 否 | 录像开始时间 | 回放开始时间 |
| end_time | datetime | 否 | 录像结束时间 | 回放结束时间 |
| stream_type | string | 否 | hls | 流类型: hls/rtmp/websocket |

**响应结构 (data)**

```json
{
  "record_id": "string",
  "device_id": "string",
  "start_time": "2026-06-15T10:00:00+08:00",
  "end_time": "2026-06-15T11:00:00+08:00",
  "duration": 3600,
  "stream_url": "string",
  "watermark": {
    "show": true,
    "text": "测试单位"
  }
}
```

**合规标注**: T/GDCAA 016-2025 8.2.3 (录像下载必须添加水印)

---

#### API-F6-015: 录像切片下载

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-015 |
| 接口名称 | 录像切片下载 |
| 路径 | `/video/records/slice` |
| HTTP方法 | POST |
| 权限要求 | `video:record:download` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| device_id | string | 是 | 设备ID |
| start_time | datetime | 是 | 切片开始时间 |
| end_time | datetime | 是 | 切片结束时间 |
| add_watermark | boolean | 否 | 是否添加水印, 默认true |

**响应结构 (data)**

```json
{
  "task_id": "string",
  "status": "processing",
  "estimated_time": 60
}
```

---

#### API-F6-016: 按告警查询关联录像

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-016 |
| 接口名称 | 按告警查询关联录像 |
| 路径 | `/video/records/alarm/{alarm_id}` |
| HTTP方法 | GET |
| 权限要求 | `video:record:read` |

**请求参数 (Path)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_id | string | 是 | 告警ID |

**响应结构 (data.list)**

```json
[{
  "record_id": "string",
  "device_id": "string",
  "device_name": "string",
  "start_time": "2026-06-15T10:00:00+08:00",
  "end_time": "2026-06-15T10:30:00+08:00",
  "is_locked": true,
  "lock_reason": "fire_alarm"
}]
```

**合规标注**: T/GDCAA 016-2025 8.2.2 (火警相关录像保留≥5年)

---

### 2.5 AI标注与告警联动

#### API-F6-017: 获取AI分析结果

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-017 |
| 接口名称 | 获取AI分析结果 |
| 路径 | `/video/ai-result` |
| HTTP方法 | GET |
| 权限要求 | `video:ai:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| device_id | string | 是 | 设备ID |
| stream_time | datetime | 是 | 视频流时间点 |

**响应结构 (data)**

```json
{
  "device_id": "string",
  "stream_time": "2026-06-15T10:30:00+08:00",
  "results": [{
    "type": "fire",
    "confidence": 0.95,
    "detected": true,
    "bbox": {
      "x": 100,
      "y": 150,
      "width": 80,
      "height": 60
    },
    "description": "检测到火焰"
  }, {
    "type": "smoke",
    "confidence": 0.78,
    "detected": true,
    "bbox": null,
    "description": "检测到烟雾"
  }]
}
```

**合规标注**: T/GDCAA 016-2025 6.3.5 (火焰识别准确率≥90%)

---

#### API-F6-018: 查询告警联动记录

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-018 |
| 接口名称 | 查询告警联动记录 |
| 路径 | `/video/linkage-records` |
| HTTP方法 | GET |
| 权限要求 | `video:linkage:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| alarm_id | string | 否 | - | 告警ID |
| alarm_type | string | 否 | - | 告警类型 |
| start_time | datetime | 否 | - | 开始时间 |
| end_time | datetime | 否 | - | 结束时间 |

**响应结构 (data.list)**

```json
[{
  "linkage_id": "string",
  "alarm_id": "string",
  "alarm_type": "fire_alarm",
  "camera_id": "string",
  "camera_name": "string",
  "pop_time": "2026-06-15T10:30:05+08:00",
  "ai_result": {
    "fire": 0.95,
    "smoke": 0.78
  },
  "screenshot_url": "string",
  "ai_confidence": 0.95,
  "verify_status": 2,
  "verify_result": "false_alarm",
  "verify_user": "string",
  "verify_time": "2026-06-15T10:31:00+08:00"
}]
```

**合规标注**: T/CFPA 052-2026 5.4.2, 5.4.4

---

#### API-F6-019: 提交视频复核结果

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-019 |
| 接口名称 | 提交视频复核结果 |
| 路径 | `/video/linkage-records/{linkage_id}/verify` |
| HTTP方法 | POST |
| 权限要求 | `video:verify:submit` |

**请求参数 (Path)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| linkage_id | string | 是 | 联动记录ID |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| verify_result | string | 是 | 复核结论: true_alarm/false_alarm/device_fault/unable_confirm |
| verify_desc | string | 否 | 复核描述 |
| false_alarm_reason | string | 否 | 误报原因(verify_result=fault时必填) |

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 61901 | 联动记录不存在 |
| 61902 | 告警已处置完成 |

**合规标注**: T/GDCAA 016-2025 8.2.4 (复核记录需完整保留)

---

#### API-F6-020: 生成取证包

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-020 |
| 接口名称 | 生成取证包 |
| 路径 | `/video/evidence` |
| HTTP方法 | POST |
| 权限要求 | `video:evidence:create` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_id | string | 是 | 关联告警ID |
| evidence_type | integer | 是 | 类型: 1-火警 2-故障 3-公安调取 |
| camera_ids | array[string] | 否 | 涉及摄像头ID列表 |
| start_time | datetime | 是 | 开始时间 |
| end_time | datetime | 是 | 结束时间 |

**响应结构 (data)**

```json
{
  "evidence_id": "string",
  "alarm_id": "string",
  "file_count": 5,
  "total_size": 524288000,
  "watermark": "测试单位 2026-06-15",
  "download_url": "string",
  "expires_in": 86400
}
```

---

### 2.6 录像标记

#### API-F6-021: 创建录像标记

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-021 |
| 接口名称 | 创建录像标记 |
| 路径 | `/video/tags` |
| HTTP方法 | POST |
| 权限要求 | `video:tag:create` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| alarm_id | string | 否 | 关联告警ID |
| record_id | string | 是 | 录像记录ID |
| tag_type | string | 是 | 标记类型: fire_alarm/fault/warning |
| start_time | datetime | 是 | 标记开始时间 |
| end_time | datetime | 是 | 标记结束时间 |
| description | string | 否 | 标记描述 |

---

#### API-F6-022: 查询录像标记

| 属性 | 值 |
|------|---|
| 接口编号 | API-F6-022 |
| 接口名称 | 查询录像标记 |
| 路径 | `/video/records/{record_id}/tags` |
| HTTP方法 | GET |
| 权限要求 | `video:tag:read` |

---

## 3. F7 查岗管理模块

### 3.1 查岗配置管理

#### API-F7-001: 创建查岗配置

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-001 |
| 接口名称 | 创建查岗配置 |
| 路径 | `/roll-call/configs` |
| HTTP方法 | POST |
| 权限要求 | `rollcall:config:create` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| unit_id | string | 否 | 单位ID (NULL表示全局配置) |
| roll_call_type | integer | 是 | 查岗类型: 1-手动 2-定时 3-AI |
| is_enabled | boolean | 否 | 是否启用, 默认true |
| check_times | array | 否 | 定时查岗时段 (roll_call_type=2时必填) |
| timeout_seconds | integer | 否 | 超时时间(秒), 默认300 |
| escalation_config | object | 否 | 升级配置 |
| ai_enabled | boolean | 否 | AI查岗是否启用 (roll_call_type=3时) |
| ai_threshold_minutes | integer | 否 | AI离岗阈值(分钟) |

**check_times 格式示例**

```json
["00:00", "04:00", "08:00", "12:00", "16:00", "20:00"]
```

**escalation_config 格式示例**

```json
{
  "night": {
    "level1_timeout": 180,
    "level2_timeout": 300,
    "notify_types": ["app", "sms", "call"]
  },
  "day": {
    "level1_timeout": 300,
    "level2_timeout": 600,
    "notify_types": ["app", "sms"]
  }
}
```

**合规标注**: T/GDCAA 016-2025 6.4.1, 6.4.2, 6.4.3, 6.4.4

---

#### API-F7-002: 查询查岗配置列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-002 |
| 接口名称 | 查询查岗配置列表 |
| 路径 | `/roll-call/configs` |
| HTTP方法 | GET |
| 权限要求 | `rollcall:config:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| unit_id | string | 否 | - | 单位ID筛选 |
| roll_call_type | integer | 否 | - | 查岗类型 |

---

#### API-F7-003: 查询查岗配置详情

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-003 |
| 接口名称 | 查询查岗配置详情 |
| 路径 | `/roll-call/configs/{config_id}` |
| HTTP方法 | GET |
| 权限要求 | `rollcall:config:read` |

---

#### API-F7-004: 更新查岗配置

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-004 |
| 接口名称 | 更新查岗配置 |
| 路径 | `/roll-call/configs/{config_id}` |
| HTTP方法 | PUT |
| 权限要求 | `rollcall:config:update` |

---

#### API-F7-005: 删除查岗配置

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-005 |
| 接口名称 | 删除查岗配置 |
| 路径 | `/roll-call/configs/{config_id}` |
| HTTP方法 | DELETE |
| 权限要求 | `rollcall:config:delete` |

---

### 3.2 查岗任务

#### API-F7-006: 发起查岗

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-006 |
| 接口名称 | 发起查岗 |
| 路径 | `/roll-call/tasks` |
| HTTP方法 | POST |
| 权限要求 | `rollcall:task:create` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| task_type | integer | 是 | 任务类型: 1-手动 2-定时 3-AI |
| unit_ids | array[string] | 是 | 单位ID列表 |
| room_ids | array[string] | 否 | 消控室ID列表 |
| duty_person_ids | array[string] | 否 | 值班员ID列表 |
| check_content | string | 否 | 查岗说明 |
| timeout_seconds | integer | 否 | 超时时间(秒), 默认300 |

**响应结构 (data)**

```json
{
  "task_ids": ["string", "string"],
  "send_count": 2,
  "send_time": "2026-06-15T10:30:00+08:00"
}
```

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 70601 | 查岗对象为空 |
| 70602 | 查岗对象不存在 |

**合规标注**: T/GDCAA 016-2025 6.4.1

---

#### API-F7-007: 查询查岗任务列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-007 |
| 接口名称 | 查询查岗任务列表 |
| 路径 | `/roll-call/tasks` |
| HTTP方法 | GET |
| 权限要求 | `rollcall:task:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| task_type | integer | 否 | - | 任务类型: 1-手动 2-定时 3-AI |
| unit_id | string | 否 | - | 单位ID筛选 |
| response_status | integer | 否 | - | 响应状态: 1-待响应 2-有效响应 3-超时 4-脱岗 |
| task_status | integer | 否 | - | 任务状态: 1-进行中 2-已完成 3-已取消 |
| start_time | datetime | 否 | - | 开始时间 |
| end_time | datetime | 否 | - | 结束时间 |

---

#### API-F7-008: 查询查岗任务详情

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-008 |
| 接口名称 | 查询查岗任务详情 |
| 路径 | `/roll-call/tasks/{task_id}` |
| HTTP方法 | GET |
| 权限要求 | `rollcall:task:read` |

**响应结构 (data)**

```json
{
  "task_id": "string",
  "task_type": 1,
  "unit_id": "string",
  "unit_name": "string",
  "room_id": "string",
  "room_name": "string",
  "duty_person_id": "string",
  "duty_person_name": "string",
  "check_user_id": "string",
  "check_user_name": "string",
  "check_content": "string",
  "check_time": "2026-06-15T10:30:00+08:00",
  "deadline_time": "2026-06-15T10:35:00+08:00",
  "response_status": 2,
  "response_time": "2026-06-15T10:30:15+08:00",
  "response_type": "screen",
  "escalation_count": 0,
  "task_status": 2,
  "complete_time": "2026-06-15T10:30:15+08:00",
  "remark": "string",
  "escalation_records": [{
    "escalation_id": "string",
    "escalation_level": 1,
    "escalate_to_user_name": "string",
    "notify_type": "app",
    "notify_time": "2026-06-15T10:35:00+08:00",
    "response_status": 1
  }]
}
```

---

#### API-F7-009: 取消查岗任务

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-009 |
| 接口名称 | 取消查岗任务 |
| 路径 | `/roll-call/tasks/{task_id}/cancel` |
| HTTP方法 | POST |
| 权限要求 | `rollcall:task:cancel` |

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 70901 | 任务不存在 |
| 70902 | 任务已响应无法取消 |
| 70903 | 任务已完成无法取消 |

---

### 3.3 查岗响应

#### API-F7-010: 提交查岗响应

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-010 |
| 接口名称 | 提交查岗响应 |
| 路径 | `/roll-call/tasks/{task_id}/response` |
| HTTP方法 | POST |
| 权限要求 | `rollcall:response:submit` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| response_type | string | 是 | 响应方式: screen/app/face/sms |
| duty_person_id | string | 是 | 值班员ID |

**响应结构 (data)**

```json
{
  "task_id": "string",
  "response_status": 2,
  "response_time": "2026-06-15T10:30:15+08:00",
  "is_effective": true
}
```

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 71001 | 任务不存在 |
| 71002 | 任务已响应 |
| 71003 | 响应超时 |

**合规标注**: T/GDCAA 016-2025 6.4.2 (响应时间≤60秒视为有效)

---

#### API-F7-011: 批量响应(值守屏端)

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-011 |
| 接口名称 | 批量响应 |
| 路径 | `/roll-call/tasks/batch-response` |
| HTTP方法 | POST |
| 权限要求 | `rollcall:response:submit` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| task_ids | array[string] | 是 | 任务ID列表 |
| response_type | string | 是 | 响应方式 |
| duty_person_id | string | 是 | 值班员ID |

---

### 3.4 脱岗记录

#### API-F7-012: 查询脱岗记录列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-012 |
| 接口名称 | 查询脱岗记录列表 |
| 路径 | `/roll-call/absent-records` |
| HTTP方法 | GET |
| 权限要求 | `rollcall:absent:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| absent_type | integer | 否 | - | 脱岗类型: 1-临时离岗 2-睡岗 3-脱岗 4-无人值班 |
| unit_id | string | 否 | - | 单位ID筛选 |
| confirm_status | integer | 否 | - | 确认状态: 1-待确认 2-已确认 3-误报 |
| rectify_status | integer | 否 | - | 整改状态: 1-待整改 2-整改中 3-已整改 |
| start_time | datetime | 否 | - | 开始时间 |
| end_time | datetime | 否 | - | 结束时间 |

---

#### API-F7-013: 脱岗确认

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-013 |
| 接口名称 | 脱岗确认 |
| 路径 | `/roll-call/absent-records/{absent_id}/confirm` |
| HTTP方法 | POST |
| 权限要求 | `rollcall:absent:confirm` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| confirm_status | integer | 是 | 确认状态: 2-已确认 3-误报 |
| confirm_user_id | string | 是 | 确认人ID |
| confirm_remark | string | 否 | 确认备注 |

---

#### API-F7-014: 提交整改完成

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-014 |
| 接口名称 | 提交整改完成 |
| 路径 | `/roll-call/absent-records/{absent_id}/rectify` |
| HTTP方法 | POST |
| 权限要求 | `rollcall:rectify:submit` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| rectify_evidence_urls | array[string] | 是 | 整改证明材料URL列表 |
| rectify_remark | string | 否 | 整改说明 |

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 71401 | 脱岗记录不存在 |
| 71402 | 整改证据不能为空 |

**合规标注**: T/GDCAA 016-2025 6.4.6 (脱岗整改时限≤24小时)

---

### 3.5 升级记录

#### API-F7-015: 查询升级记录

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-015 |
| 接口名称 | 查询升级记录 |
| 路径 | `/roll-call/escalation-records` |
| HTTP方法 | GET |
| 权限要求 | `rollcall:escalation:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| task_id | string | 否 | - | 查岗任务ID |
| escalation_level | integer | 否 | - | 升级级别: 1/2/3 |
| response_status | integer | 否 | - | 响应状态: 1-待响应 2-已响应 |

---

#### API-F7-016: 手动升级

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-016 |
| 接口名称 | 手动升级 |
| 路径 | `/roll-call/tasks/{task_id}/escalate` |
| HTTP方法 | POST |
| 权限要求 | `rollcall:escalation:manual` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| escalation_level | integer | 是 | 升级级别: 1/2/3 |
| escalate_to_user_id | string | 否 | 指定升级对象ID |

---

### 3.6 查岗统计

#### API-F7-017: 查询查岗统计

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-017 |
| 接口名称 | 查询查岗统计 |
| 路径 | `/roll-call/statistics` |
| HTTP方法 | GET |
| 权限要求 | `rollcall:statistics:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| stat_date | date | 否 | 当天 | 统计日期 |
| unit_id | string | 否 | - | 单位ID (为空则汇总) |
| period | string | 否 | day | 统计周期: day/week/month |

**响应结构 (data)**

```json
{
  "stat_date": "2026-06-15",
  "unit_id": "string",
  "unit_name": "string",
  "total_checks": 100,
  "effective_responses": 95,
  "timeout_count": 3,
  "absent_count": 2,
  "ai_detected_count": 1,
  "response_rate": 95.00,
  "absent_rate": 2.00,
  "rectify_completed": 2,
  "rectify_pending": 0
}
```

**合规标注**: T/GDCAA 016-2025 6.4.7

---

#### API-F7-018: 导出查岗台账

| 属性 | 值 |
|------|---|
| 接口编号 | API-F7-018 |
| 接口名称 | 导出查岗台账 |
| 路径 | `/roll-call/ledger/export` |
| HTTP方法 | GET |
| 权限要求 | `rollcall:ledger:export` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| start_date | date | 是 | - | 开始日期 |
| end_date | date | 是 | - | 结束日期 |
| unit_id | string | 否 | - | 单位ID |
| format | string | 否 | pdf | 导出格式: pdf/excel |

**响应**: 文件流

**合规标注**: T/GDCAA 016-2025 8.2.5

---

## 4. F8 GIS态势感知模块

### 4.1 建筑标记

#### API-F8-001: 创建建筑标记

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-001 |
| 接口名称 | 创建建筑标记 |
| 路径 | `/gis/buildings` |
| HTTP方法 | POST |
| 权限要求 | `gis:building:create` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| building_id | string | 是 | 建筑ID |
| building_name | string | 是 | 建筑名称 |
| longitude | decimal | 是 | 经度 |
| latitude | decimal | 是 | 纬度 |
| address | string | 是 | 详细地址 |
| district | string | 是 | 所属行政区 |
| building_type | integer | 否 | 建筑类型 |
| area | decimal | 否 | 建筑面积(平方米) |
| floor_count | integer | 否 | 楼层数 |

**响应结构 (data)**

```json
{
  "marker_id": "string",
  "building_id": "string",
  "marker_color": "#00FF00",
  "create_time": "2026-06-15T10:30:00+08:00"
}
```

**合规标注**: T/GDCAA 016-2025 11.1.1, 11.1.2

---

#### API-F8-002: 查询建筑标记列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-002 |
| 接口名称 | 查询建筑标记列表 |
| 路径 | `/gis/buildings` |
| HTTP方法 | GET |
| 权限要求 | `gis:building:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 100 | 每页条数(地图场景建议≤100) |
| district | string | 否 | - | 行政区筛选 |
| risk_level | integer | 否 | - | 风险等级: 1-高 2-中 3-低 |
| keyword | string | 否 | - | 关键字搜索 |

**响应结构 (data.list)**

```json
[{
  "marker_id": "string",
  "building_id": "string",
  "building_name": "string",
  "longitude": 113.358642,
  "latitude": 23.132654,
  "address": "string",
  "district": "string",
  "device_count": 50,
  "online_count": 48,
  "online_rate": 96.00,
  "risk_level": 2,
  "risk_index": 45.5,
  "marker_color": "#FFFF00"
}]
```

---

#### API-F8-003: 查询建筑标记详情

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-003 |
| 接口名称 | 查询建筑标记详情 |
| 路径 | `/gis/buildings/{marker_id}` |
| HTTP方法 | GET |
| 权限要求 | `gis:building:read` |

---

#### API-F8-004: 更新建筑标记

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-004 |
| 接口名称 | 更新建筑标记 |
| 路径 | `/gis/buildings/{marker_id}` |
| HTTP方法 | PUT |
| 权限要求 | `gis:building:update` |

---

#### API-F8-005: 删除建筑标记

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-005 |
| 接口名称 | 删除建筑标记 |
| 路径 | `/gis/buildings/{marker_id}` |
| HTTP方法 | DELETE |
| 权限要求 | `gis:building:delete` |

---

### 4.2 设备标记

#### API-F8-006: 查询设备标记列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-006 |
| 接口名称 | 查询设备标记列表 |
| 路径 | `/gis/devices` |
| HTTP方法 | GET |
| 权限要求 | `gis:device:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 200 | 每页条数 |
| building_id | string | 否 | - | 建筑ID筛选 |
| device_type | integer | 否 | - | 设备类型 |
| status | integer | 否 | - | 设备状态: 1-在线 2-离线 3-故障 |
| keyword | string | 否 | - | 关键字搜索 |

---

#### API-F8-007: 查询设备标记详情

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-007 |
| 接口名称 | 查询设备标记详情 |
| 路径 | `/gis/devices/{marker_id}` |
| HTTP方法 | GET |
| 权限要求 | `gis:device:read` |

---

#### API-F8-008: 设备标记聚合查询

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-008 |
| 接口名称 | 设备标记聚合查询 |
| 路径 | `/gis/devices/cluster` |
| HTTP方法 | GET |
| 权限要求 | `gis:device:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| center | string | 是 | 视野中心点: "lng,lat" |
| zoom | integer | 是 | 缩放级别: 1-18 |
| bounds | string | 否 | 视野边界: "sw_lng,sw_lat,ne_lng,ne_lat" |
| cluster_radius | integer | 否 | 聚合半径(像素), 默认50 |

**响应结构 (data.list)**

```json
[{
  "cluster_id": "string",
  "is_cluster": true,
  "count": 25,
  "center": {
    "longitude": 113.358642,
    "latitude": 23.132654
  },
  "status_distribution": {
    "online": 20,
    "offline": 3,
    "fault": 2
  },
  "has_alert": true,
  "max_alert_level": 2,
  "devices": null
}, {
  "cluster_id": "string",
  "is_cluster": false,
  "count": 1,
  "center": {
    "longitude": 113.358700,
    "latitude": 23.132700
  },
  "status_distribution": null,
  "has_alert": false,
  "max_alert_level": null,
  "device": {
    "marker_id": "string",
    "device_id": "string",
    "device_name": "string",
    "device_type": 1,
    "status": 1,
    "status_color": "#00FF00"
  }
}]
```

**聚合策略说明**

| 缩放级别 | 聚合策略 |
|----------|----------|
| 1-5 | 区域聚合, 显示行政区/大片区 |
| 6-10 | 建筑聚合, 同建筑设备合并 |
| 11-15 | 楼层聚合, 同楼层设备合并 |
| 16-18 | 设备独立显示 |

**合规标注**: T/GDCAA 016-2025 11.5.1, 11.5.2, 11.5.3, 11.5.4

---

### 4.3 告警标记

#### API-F8-009: 查询告警标记列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-009 |
| 接口名称 | 查询告警标记列表 |
| 路径 | `/gis/alerts` |
| HTTP方法 | GET |
| 权限要求 | `gis:alert:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 100 | 每页条数 |
| bounds | string | 否 | - | 视野边界 |
| alert_level | integer | 否 | - | 告警级别: 1/2/3 |
| alert_status | integer | 否 | - | 告警状态: 1-未处置 2-处置中 3-已闭环 |
| alert_type | string | 否 | - | 告警类型 |
| start_time | datetime | 否 | - | 开始时间 |
| end_time | datetime | 否 | - | 结束时间 |

**响应结构 (data.list)**

```json
[{
  "marker_id": "string",
  "alert_id": "string",
  "alert_type": "fire_alarm",
  "alert_level": 1,
  "building_id": "string",
  "building_name": "string",
  "device_id": "string",
  "device_name": "string",
  "longitude": 113.358642,
  "latitude": 23.132654,
  "alert_time": "2026-06-15T10:30:00+08:00",
  "alert_status": 1,
  "blink_color": "#FF0000",
  "blink_interval": 500,
  "is_blinking": true
}]
```

**合规标注**: T/GDCAA 016-2025 11.4.1, 11.4.2, 11.4.3

---

#### API-F8-010: 查询告警标记详情

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-010 |
| 接口名称 | 查询告警标记详情 |
| 路径 | `/gis/alerts/{marker_id}` |
| HTTP方法 | GET |
| 权限要求 | `gis:alert:read` |

---

### 4.4 热力图

#### API-F8-011: 查询热力图数据

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-011 |
| 接口名称 | 查询热力图数据 |
| 路径 | `/gis/heatmap` |
| HTTP方法 | GET |
| 权限要求 | `gis:heatmap:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bounds | string | 是 | 视野边界: "sw_lng,sw_lat,ne_lng,ne_lat" |
| grid_size | integer | 否 | 网格大小(米), 默认100 |
| calculate_time | datetime | 否 | 计算时间, 默认最新 |

**响应结构 (data)**

```json
{
  "calculate_time": "2026-06-15T00:00:00+08:00",
  "grids": [{
    "longitude": 113.358642,
    "latitude": 23.132654,
    "heat_value": 85.5,
    "risk_index": 72.3,
    "risk_level": 1
  }]
}
```

**合规标注**: T/GDCAA 016-2025 11.3.1, 11.3.2, 11.3.3

---

#### API-F8-012: 查询风险指数详情

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-012 |
| 接口名称 | 查询风险指数详情 |
| 路径 | `/gis/heatmap/building/{building_id}` |
| HTTP方法 | GET |
| 权限要求 | `gis:heatmap:read` |

**响应结构 (data)**

```json
{
  "building_id": "string",
  "risk_index": 72.3,
  "risk_level": 1,
  "device_fault_rate": 8.5,
  "alert_frequency": 15,
  "avg_repair_time": 4.2,
  "device_age_rate": 35.0,
  "inspection_rate": 92.0,
  "calculate_time": "2026-06-15T00:00:00+08:00"
}
```

---

### 4.5 区域统计

#### API-F8-013: 查询区域态势统计

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-013 |
| 接口名称 | 查询区域态势统计 |
| 路径 | `/gis/districts/statistics` |
| HTTP方法 | GET |
| 权限要求 | `gis:district:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| district_level | integer | 否 | - | 行政区级别: 1-省 2-市 3-区 4-街道 |
| parent_code | string | 否 | - | 上级行政区编码 |

**响应结构 (data.list)**

```json
[{
  "district_id": "string",
  "district_name": "string",
  "building_count": 100,
  "device_count": 5000,
  "online_count": 4800,
  "offline_count": 150,
  "fault_count": 50,
  "online_rate": 96.00,
  "today_alert_count": 5,
  "today_fire_count": 0,
  "week_alert_count": 30,
  "month_alert_count": 120,
  "risk_level": 2,
  "risk_index": 45.5,
  "update_time": "2026-06-15T10:30:00+08:00"
}]
```

**合规标注**: T/GDCAA 016-2025 11.6.1, 11.6.2, 11.6.3

---

#### API-F8-014: 查询态势概览

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-014 |
| 接口名称 | 查询态势概览 |
| 路径 | `/gis/overview` |
| HTTP方法 | GET |
| 权限要求 | `gis:overview:read` |

**响应结构 (data)**

```json
{
  "today_alert_count": 15,
  "today_fire_count": 1,
  "pending_alert_count": 3,
  "device_online_rate": 96.5,
  "risk_distribution": {
    "high": 10,
    "medium": 45,
    "low": 145
  },
  "alert_trend": [{
    "date": "2026-06-15",
    "count": 15
  }],
  "update_time": "2026-06-15T10:30:00+08:00"
}
```

---

### 4.6 告警弹窗数据

#### API-F8-015: 获取告警弹窗数据

| 属性 | 值 |
|------|---|
| 接口编号 | API-F8-015 |
| 接口名称 | 获取告警弹窗数据 |
| 路径 | `/gis/alerts/{alert_id}/popup` |
| HTTP方法 | GET |
| 权限要求 | `gis:alert:read` |

**响应结构 (data)**

```json
{
  "alert_id": "string",
  "alert_type": "fire_alarm",
  "alert_level": 1,
  "alert_level_color": "#FF0000",
  "building_name": "string",
  "device_name": "string",
  "location": "string",
  "alert_time": "2026-06-15T10:30:00+08:00",
  "alert_desc": "感烟探测器报警",
  "longitude": 113.358642,
  "latitude": 23.132654,
  "action_buttons": [{
    "action": "verify",
    "label": "确认处置",
    "icon": "check"
  }, {
    "action": "navigate",
    "label": "导航",
    "icon": "location"
  }, {
    "action": "dispatch",
    "label": "派发工单",
    "icon": "edit"
  }],
  "linkage_cameras": [{
    "camera_id": "string",
    "camera_name": "string",
    "stream_url": "string"
  }],
  "ai_result": {
    "fire": 0.95,
    "smoke": 0.78
  }
}
```

**合规标注**: T/GDCAA 016-2025 11.7.1, 11.7.2, 11.7.3, 11.7.4

---

## 5. F9 权限与安全模块

### 5.1 认证接口

#### API-F9-001: 用户登录

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-001 |
| 接口名称 | 用户登录 |
| 路径 | `/auth/login` |
| HTTP方法 | POST |
| 权限要求 | 无 |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 校验规则 | 说明 |
|--------|------|------|----------|------|
| username | string | 是 | 长度≤32 | 用户名/手机号 |
| password | string | 是 | 长度≥8 | 密码 |
| captcha | string | 否 | 4位字符 | 图形验证码(失败3次后必填) |
| captcha_key | string | 否 | UUID | 验证码key |

**响应结构 (data)**

```json
{
  "access_token": "string",
  "refresh_token": "string",
  "token_type": "Bearer",
  "expires_in": 7200,
  "user_info": {
    "user_id": "string",
    "user_code": "string",
    "user_name": "string",
    "role_id": "string",
    "role_name": "string",
    "role_level": 1,
    "unit_id": "string",
    "unit_name": "string",
    "cert_status": "valid",
    "cert_expire": "2026-12-31"
  }
}
```

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 90101 | 用户名或密码错误 |
| 90102 | 账号已锁定 |
| 90103 | 账号已停用 |
| 90104 | 证书已过期 |
| 90105 | 验证码错误 |

**合规标注**: GB 25201-2010 6.2, T/GDCAA 016-2025 5.3

---

#### API-F9-002: 刷新Token

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-002 |
| 接口名称 | 刷新Token |
| 路径 | `/auth/refresh` |
| HTTP方法 | POST |
| 权限要求 | 无 |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| refresh_token | string | 是 | 刷新Token |

**响应结构 (data)**

```json
{
  "access_token": "string",
  "refresh_token": "string",
  "expires_in": 7200
}
```

---

#### API-F9-003: 用户登出

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-003 |
| 接口名称 | 用户登出 |
| 路径 | `/auth/logout` |
| HTTP方法 | POST |
| 权限要求 | 已登录 |

---

#### API-F9-004: 获取图形验证码

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-004 |
| 接口名称 | 获取图形验证码 |
| 路径 | `/auth/captcha` |
| HTTP方法 | GET |
| 权限要求 | 无 |

**响应结构 (data)**

```json
{
  "captcha_key": "string",
  "captcha_image": "data:image/png;base64,..."
}
```

---

### 5.2 用户管理

#### API-F9-005: 创建用户

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-005 |
| 接口名称 | 创建用户 |
| 路径 | `/users` |
| HTTP方法 | POST |
| 权限要求 | `user:create` (L3) |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 校验规则 | 说明 |
|--------|------|------|----------|------|
| user_code | string | 是 | 长度≤32, 唯一 | 用户编码 |
| user_name | string | 是 | 长度≤64 | 用户姓名 |
| id_card | string | 是 | 18位身份证 | 身份证号(加密存储) |
| phone | string | 是 | 11位手机号 | 手机号 |
| email | string | 否 | 有效邮箱格式 | 邮箱 |
| unit_id | string | 是 | 存在性校验 | 所属单位ID |
| role_id | string | 是 | 存在性校验 | 角色ID |
| password | string | 是 | 长度≥8, 复杂度校验 | 初始密码 |

**响应结构 (data)**

```json
{
  "user_id": "string",
  "user_code": "string",
  "user_name": "string",
  "account_status": "normal",
  "create_time": "2026-06-15T10:30:00+08:00"
}
```

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 90501 | 用户编码已存在 |
| 90502 | 身份证号已注册 |
| 90503 | 手机号已注册 |
| 90504 | 角色不存在 |
| 90505 | 密码复杂度不足 |

**合规标注**: T/CFPA 052-2026 6.1.3, 6.1.4, GB 25201-2010 6.2

---

#### API-F9-006: 查询用户列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-006 |
| 接口名称 | 查询用户列表 |
| 路径 | `/users` |
| HTTP方法 | GET |
| 权限要求 | `user:read` (L3) |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| unit_id | string | 否 | - | 单位ID筛选 |
| role_id | string | 否 | - | 角色ID筛选 |
| role_level | integer | 否 | - | 角色等级 |
| account_status | string | 否 | - | 账号状态: normal/locked/disabled |
| cert_status | string | 否 | - | 证书状态: valid/expiring/expired |
| keyword | string | 否 | - | 关键字搜索(姓名/编码/手机号) |

---

#### API-F9-007: 查询用户详情

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-007 |
| 接口名称 | 查询用户详情 |
| 路径 | `/users/{user_id}` |
| HTTP方法 | GET |
| 权限要求 | `user:read` (L3) |

**响应结构 (data)**

```json
{
  "user_id": "string",
  "user_code": "string",
  "user_name": "string",
  "id_card": "**************1234",
  "phone": "138****1234",
  "email": "string",
  "unit_id": "string",
  "unit_name": "string",
  "role_id": "string",
  "role_name": "string",
  "role_level": 1,
  "certificate_id": "string",
  "certificate_type": "string",
  "certificate_level": "string",
  "certificate_expire": "2026-12-31",
  "cert_status": "valid",
  "account_status": "normal",
  "password_expire": "2026-09-15",
  "last_login_time": "2026-06-15T10:30:00+08:00",
  "last_login_ip": "192.168.1.100",
  "fail_login_count": 0,
  "create_time": "2026-06-15T10:30:00+08:00"
}
```

---

#### API-F9-008: 更新用户

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-008 |
| 接口名称 | 更新用户 |
| 路径 | `/users/{user_id}` |
| HTTP方法 | PUT |
| 权限要求 | `user:update` (L3) |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| user_name | string | 否 | 用户姓名 |
| phone | string | 否 | 手机号 |
| email | string | 否 | 邮箱 |
| unit_id | string | 否 | 所属单位ID |
| role_id | string | 否 | 角色ID |

---

#### API-F9-009: 删除用户

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-009 |
| 接口名称 | 删除用户 |
| 路径 | `/users/{user_id}` |
| HTTP方法 | DELETE |
| 权限要求 | `user:delete` (L3) |

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 90901 | 用户不存在 |
| 90902 | 用户有关联数据无法删除 |

---

#### API-F9-010: 修改密码

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-010 |
| 接口名称 | 修改密码 |
| 路径 | `/users/{user_id}/password` |
| HTTP方法 | PUT |
| 权限要求 | `user:password` (本人或L3) |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| old_password | string | 否 | 原密码(修改他人密码时无需) |
| new_password | string | 是 | 新密码 |
| confirm_password | string | 是 | 确认密码 |

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 91001 | 原密码错误 |
| 91002 | 新密码与原密码相同 |
| 91003 | 密码复杂度不足 |
| 91004 | 两次密码输入不一致 |

---

#### API-F9-011: 重置密码

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-011 |
| 接口名称 | 重置密码 |
| 路径 | `/users/{user_id}/reset-password` |
| HTTP方法 | POST |
| 权限要求 | `user:reset-password` (L3) |

**响应结构 (data)**

```json
{
  "user_id": "string",
  "temp_password": "string",
  "expire_seconds": 86400
}
```

---

#### API-F9-012: 锁定/解锁用户

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-012 |
| 接口名称 | 锁定/解锁用户 |
| 路径 | `/users/{user_id}/lock` |
| HTTP方法 | POST |
| 权限要求 | `user:lock` (L3) |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| action | string | 是 | 操作: lock/unlock |
| reason | string | 否 | 锁定原因 |

---

### 5.3 角色管理

#### API-F9-013: 创建角色

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-013 |
| 接口名称 | 创建角色 |
| 路径 | `/roles` |
| HTTP方法 | POST |
| 权限要求 | `role:create` (L3) |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| role_code | string | 是 | 角色编码(唯一) |
| role_name | string | 是 | 角色名称 |
| role_level | integer | 是 | 角色等级: 1-普通 2-操作 3-管理 |
| role_desc | string | 否 | 角色描述 |
| permissions | array[string] | 否 | 权限编码列表 |
| data_scope | object | 否 | 数据权限范围配置 |

---

#### API-F9-014: 查询角色列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-014 |
| 接口名称 | 查询角色列表 |
| 路径 | `/roles` |
| HTTP方法 | GET |
| 权限要求 | `role:read` (L3) |

---

#### API-F9-015: 查询角色详情

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-015 |
| 接口名称 | 查询角色详情 |
| 路径 | `/roles/{role_id}` |
| HTTP方法 | GET |
| 权限要求 | `role:read` (L3) |

---

#### API-F9-016: 更新角色

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-016 |
| 接口名称 | 更新角色 |
| 路径 | `/roles/{role_id}` |
| HTTP方法 | PUT |
| 权限要求 | `role:update` (L3) |

---

#### API-F9-017: 删除角色

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-017 |
| 接口名称 | 删除角色 |
| 路径 | `/roles/{role_id}` |
| HTTP方法 | DELETE |
| 权限要求 | `role:delete` (L3) |

**业务错误码**

| 错误码 | 说明 |
|--------|------|
| 91701 | 系统预置角色不可删除 |
| 91702 | 角色有关联用户无法删除 |

---

### 5.4 权限管理

#### API-F9-018: 查询权限列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-018 |
| 接口名称 | 查询权限列表 |
| 路径 | `/permissions` |
| HTTP方法 | GET |
| 权限要求 | `permission:read` (L3) |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| module_id | string | 否 | - | 所属模块筛选 |
| permission_type | string | 否 | - | 权限类型: menu/button/api/data |
| risk_level | integer | 否 | - | 风险等级: 1/2/3 |

---

#### API-F9-019: 查询用户权限

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-019 |
| 接口名称 | 查询用户权限 |
| 路径 | `/users/{user_id}/permissions` |
| HTTP方法 | GET |
| 权限要求 | 已登录用户本人或L3 |

**响应结构 (data)**

```json
{
  "user_id": "string",
  "role_id": "string",
  "role_level": 1,
  "permissions": ["alarm:read", "alarm:verify", "device:read"],
  "data_scope": {
    "scope_type": "unit",
    "unit_ids": ["string"]
  },
  "effective_permissions": ["alarm:read", "alarm:verify", "device:read", "video:read"]
}
```

---

#### API-F9-020: 分配用户角色

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-020 |
| 接口名称 | 分配用户角色 |
| 路径 | `/users/{user_id}/roles` |
| HTTP方法 | PUT |
| 权限要求 | `user:assign-role` (L3) |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| role_id | string | 是 | 角色ID |

---

### 5.5 证书管理

#### API-F9-021: 查询证书列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-021 |
| 接口名称 | 查询证书列表 |
| 路径 | `/certificates` |
| HTTP方法 | GET |
| 权限要求 | `certificate:read` (L3) |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| user_id | string | 否 | - | 用户ID筛选 |
| cert_type | string | 否 | - | 证书类型 |
| verify_status | string | 否 | - | 核验状态 |
| expire_within_days | integer | 否 | - | 即将过期天数 |

---

#### API-F9-022: 创建证书记录

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-022 |
| 接口名称 | 创建证书记录 |
| 路径 | `/certificates` |
| HTTP方法 | POST |
| 权限要求 | `certificate:create` (L3) |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| user_id | string | 是 | 用户ID |
| cert_number | string | 是 | 证书编号 |
| cert_type | string | 是 | 证书类型 |
| cert_level | string | 是 | 证书等级 |
| issue_org | string | 是 | 发证机构 |
| issue_date | date | 是 | 发证日期 |
| expire_date | date | 是 | 有效期至 |
| cert_file | string | 否 | 证书文件URL |

---

#### API-F9-023: 核验证书

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-023 |
| 接口名称 | 核验证书 |
| 路径 | `/certificates/{cert_id}/verify` |
| HTTP方法 | POST |
| 权限要求 | `certificate:verify` (L3) |

**响应结构 (data)**

```json
{
  "cert_id": "string",
  "verify_status": "verified",
  "verify_result": {
    "is_valid": true,
    "issue_org_match": true,
    "expire_check": "valid"
  },
  "verify_time": "2026-06-15T10:30:00+08:00"
}
```

---

#### API-F9-024: 更新证书

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-024 |
| 接口名称 | 更新证书 |
| 路径 | `/certificates/{cert_id}` |
| HTTP方法 | PUT |
| 权限要求 | `certificate:update` (L3) |

---

### 5.6 安全审计

#### API-F9-025: 查询安全日志

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-025 |
| 接口名称 | 查询安全日志 |
| 路径 | `/security/logs` |
| HTTP方法 | GET |
| 权限要求 | `security:log:read` (L2/L3) |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| log_type | string | 否 | - | 日志类型: login/operation/control |
| user_id | string | 否 | - | 用户ID筛选 |
| operation | string | 否 | - | 操作类型 |
| result | string | 否 | - | 操作结果: success/fail |
| start_time | datetime | 否 | - | 开始时间 |
| end_time | datetime | 否 | - | 结束时间 |

**响应结构 (data.list)**

```json
[{
  "log_id": "string",
  "log_type": "control",
  "user_id": "string",
  "user_name": "string",
  "user_role": "值守主管",
  "operation": "pump_start",
  "operation_module": "F2-远程控制",
  "operation_detail": {
    "device_name": "消火栓泵",
    "building_name": "1号楼"
  },
  "target_object": "消火栓泵-1#",
  "ip_address": "192.168.1.100",
  "terminal_info": "Chrome/Win10",
  "session_id": "string",
  "result": "success",
  "failure_reason": null,
  "log_time": "2026-06-15T10:30:00+08:00",
  "verify_status": "verified",
  "reviewer_name": "string"
}]
```

**合规标注**: T/CFPA 052-2026 8.1, GB 25201-2010 7.3, T/GDCAA 016-2025 8.2.4

---

#### API-F9-026: 导出安全日志

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-026 |
| 接口名称 | 导出安全日志 |
| 路径 | `/security/logs/export` |
| HTTP方法 | GET |
| 权限要求 | `security:log:export` (L3) |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| start_time | datetime | 是 | - | 开始时间 |
| end_time | datetime | 是 | - | 结束时间 |
| log_type | string | 否 | - | 日志类型 |
| format | string | 否 | pdf | 导出格式: pdf/excel |

---

#### API-F9-027: 验证日志完整性

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-027 |
| 接口名称 | 验证日志完整性 |
| 路径 | `/security/logs/verify` |
| HTTP方法 | POST |
| 权限要求 | `security:log:verify` (L3) |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| start_time | datetime | 否 | 验证起始时间 |
| end_time | datetime | 否 | 验证结束时间 |

**响应结构 (data)**

```json
{
  "verify_range": {
    "start_time": "2026-06-01T00:00:00+08:00",
    "end_time": "2026-06-15T23:59:59+08:00"
  },
  "total_records": 10000,
  "verified_records": 9998,
  "error_records": 2,
  "chain_integrity": "valid",
  "verify_time": "2026-06-15T10:30:00+08:00"
}
```

---

### 5.7 当前用户

#### API-F9-028: 获取当前用户信息

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-028 |
| 接口名称 | 获取当前用户信息 |
| 路径 | `/auth/me` |
| HTTP方法 | GET |
| 权限要求 | 已登录 |

---

#### API-F9-029: 修改当前用户信息

| 属性 | 值 |
|------|---|
| 接口编号 | API-F9-029 |
| 接口名称 | 修改当前用户信息 |
| 路径 | `/auth/me` |
| HTTP方法 | PUT |
| 权限要求 | 已登录 |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | string | 否 | 手机号 |
| email | string | 否 | 邮箱 |

---

## 6. F10 移动端专用模块

### 6.1 移动端认证

#### API-F10-001: 移动端登录

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-001 |
| 接口名称 | 移动端登录 |
| 路径 | `/mobile/auth/login` |
| HTTP方法 | POST |
| 权限要求 | 无 |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名/手机号 |
| password | string | 是 | 密码 |
| device_id | string | 是 | 设备ID |
| device_type | string | 是 | 设备类型: ios/android/harmony |
| device_info | object | 是 | 设备信息 |

**device_info 格式**

```json
{
  "os_version": "15.0",
  "app_version": "1.0.0",
  "device_model": "iPhone 14 Pro",
  "manufacturer": "Apple"
}
```

**合规标注**: T/CFPA 052-2026 12.14.1

---

#### API-F10-002: 推送Token注册

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-002 |
| 接口名称 | 推送Token注册 |
| 路径 | `/mobile/push/register` |
| HTTP方法 | POST |
| 权限要求 | 已登录 |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| push_token | string | 是 | 推送Token |
| push_platform | string | 是 | 推送平台: jiguang/umeng/huawei/xiaomi |
| device_id | string | 是 | 设备ID |

---

#### API-F10-003: 更新推送配置

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-003 |
| 接口名称 | 更新推送配置 |
| 路径 | `/mobile/push/config` |
| HTTP方法 | PUT |
| 权限要求 | 已登录 |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fire_alarm_enabled | boolean | 否 | 火警推送开关 |
| fault_enabled | boolean | 否 | 故障推送开关 |
| warning_enabled | boolean | 否 | 预警推送开关 |
| rollcall_enabled | boolean | 否 | 查岗推送开关 |
| quiet_hours_start | string | 否 | 免打扰开始时间 |
| quiet_hours_end | string | 否 | 免打扰结束时间 |

---

### 6.2 移动值守

#### API-F10-004: 开始值守

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-004 |
| 接口名称 | 开始值守 |
| 路径 | `/mobile/duty/start` |
| HTTP方法 | POST |
| 权限要求 | `mobile:duty:create` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| duty_type | integer | 是 | 值守类型: 1-远程 2-现场 |
| building_id | string | 否 | 值守建筑ID(现场值守时必填) |
| location | object | 是 | 值守位置(GPS) |

**location 格式**

```json
{
  "longitude": 113.358642,
  "latitude": 23.132654,
  "accuracy": 10.5
}
```

**响应结构 (data)**

```json
{
  "duty_id": "string",
  "start_time": "2026-06-15T10:30:00+08:00",
  "duty_status": 1
}
```

**合规标注**: T/GDCAA 016-2025 12.1.1, 12.1.2

---

#### API-F10-005: 结束值守

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-005 |
| 接口名称 | 结束值守 |
| 路径 | `/mobile/duty/{duty_id}/end` |
| HTTP方法 | POST |
| 权限要求 | `mobile:duty:create` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| location | object | 否 | 结束位置(GPS) |
| notes | string | 否 | 备注 |

---

#### API-F10-006: 查询值守记录

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-006 |
| 接口名称 | 查询值守记录 |
| 路径 | `/mobile/duty/records` |
| HTTP方法 | GET |
| 权限要求 | `mobile:duty:read` |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| start_date | date | 否 | - | 开始日期 |
| end_date | date | 否 | - | 结束日期 |

---

### 6.3 现场取证

#### API-F10-007: 上传取证数据

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-007 |
| 接口名称 | 上传取证数据 |
| 路径 | `/mobile/evidence` |
| HTTP方法 | POST |
| 权限要求 | `mobile:evidence:upload` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| evidence_type | integer | 是 | 取证类型: 1-照片 2-视频 3-录音 |
| business_type | string | 是 | 业务类型: alarm/work_order/inspection |
| business_id | string | 是 | 业务ID |
| latitude | decimal | 是 | 纬度 |
| longitude | decimal | 是 | 经度 |
| location_accuracy | decimal | 是 | 定位精度(米) |
| capture_time | datetime | 是 | 采集时间 |
| device_info | string | 是 | 设备信息 |
| hash_value | string | 是 | 文件哈希值 |
| file | file | 是 | 文件( multipart/form-data) |

**响应结构 (data)**

```json
{
  "evidence_id": "string",
  "file_url": "string",
  "upload_status": "uploaded",
  "upload_time": "2026-06-15T10:30:00+08:00"
}
```

**合规标注**: T/CFPA 052-2026 12.12.1, 12.12.2, 12.12.3, 12.12.4, 12.12.5, 12.12.6, 12.12.7

---

#### API-F10-008: 批量上传取证数据

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-008 |
| 接口名称 | 批量上传取证数据 |
| 路径 | `/mobile/evidence/batch` |
| HTTP方法 | POST |
| 权限要求 | `mobile:evidence:upload` |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| business_type | string | 是 | 业务类型 |
| business_id | string | 是 | 业务ID |
| evidence_list | array | 是 | 取证数据列表 |

**evidence_list 格式**

```json
[{
  "evidence_type": 1,
  "latitude": 23.132654,
  "longitude": 113.358642,
  "capture_time": "2026-06-15T10:30:00+08:00",
  "hash_value": "string"
}]
```

---

### 6.4 离线数据同步

#### API-F10-009: 提交离线数据

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-009 |
| 接口名称 | 提交离线数据 |
| 路径 | `/mobile/offline/sync` |
| HTTP方法 | POST |
| 权限要求 | 已登录 |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| data_type | string | 是 | 数据类型: duty/evidence/maintenance/inspection |
| data_list | array | 是 | 离线数据列表 |
| sync_id | string | 是 | 同步批次ID |
| sync_time | datetime | 是 | 客户端同步时间 |

**data_list 格式示例 (值守记录)**

```json
[{
  "client_id": "string",
  "server_id": null,
  "data": {
    "duty_type": 1,
    "building_id": "string",
    "start_time": "2026-06-15T10:00:00+08:00",
    "end_time": "2026-06-15T18:00:00+08:00",
    "start_location": {...},
    "end_location": {...},
    "alerts_handled": 5,
    "notes": "string"
  },
  "client_hash": "string"
}]
```

**响应结构 (data)**

```json
{
  "sync_id": "string",
  "sync_results": [{
    "client_id": "string",
    "server_id": "string",
    "status": "success",
    "conflict": null
  }, {
    "client_id": "string",
    "server_id": null,
    "status": "conflict",
    "conflict": {
      "reason": "data_modified",
      "server_data": {...},
      "client_data": {...},
      "resolution": "server_wins"
    }
  }],
  "sync_time": "2026-06-15T10:30:00+08:00"
}
```

**冲突处理策略**

| 冲突类型 | 处理规则 | 说明 |
|----------|----------|------|
| data_modified | server_wins | 服务端数据优先 |
| duplicate_submit | server_wins | 重复提交,采用服务端数据 |
| time_conflict | latest_wins | 以时间戳最新为准 |
| location_conflict | server_wins | 位置信息以服务端为准 |

**合规标注**: T/CFPA 052-2026 12.13.1, 12.13.2, 12.13.3, 12.13.4

---

#### API-F10-010: 查询离线待同步数据

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-010 |
| 接口名称 | 查询离线待同步数据 |
| 路径 | `/mobile/offline/pending` |
| HTTP方法 | GET |
| 权限要求 | 已登录 |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| data_type | string | 否 | - | 数据类型筛选 |
| sync_status | integer | 否 | - | 同步状态: 1-待同步 4-同步失败 |

**响应结构 (data.list)**

```json
[{
  "cache_id": "string",
  "data_type": "evidence",
  "business_id": "string",
  "sync_status": 4,
  "last_error": "网络超时",
  "sync_retry_count": 3,
  "cached_time": "2026-06-15T10:00:00+08:00",
  "expire_time": "2026-06-22T10:00:00+08:00"
}]
```

---

#### API-F10-011: 删除已同步数据

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-011 |
| 接口名称 | 删除已同步数据 |
| 路径 | `/mobile/offline/{cache_id}` |
| HTTP方法 | DELETE |
| 权限要求 | 已登录 |

---

### 6.5 角色切换

#### API-F10-012: 查询用户角色列表

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-012 |
| 接口名称 | 查询用户角色列表 |
| 路径 | `/mobile/roles` |
| HTTP方法 | GET |
| 权限要求 | 已登录 |

**响应结构 (data.list)**

```json
[{
  "role_id": "string",
  "role_name": "值守员",
  "role_level": 1,
  "unit_id": "string",
  "unit_name": "string",
  "is_current": true,
  "permissions": ["alarm:read", "device:read"]
}]
```

---

#### API-F10-013: 切换当前角色

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-013 |
| 接口名称 | 切换当前角色 |
| 路径 | `/mobile/roles/switch` |
| HTTP方法 | POST |
| 权限要求 | 已登录 |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| role_id | string | 是 | 目标角色ID |
| unit_id | string | 是 | 目标单位ID |

**响应结构 (data)**

```json
{
  "role_id": "string",
  "role_name": "值守主管",
  "role_level": 2,
  "unit_id": "string",
  "unit_name": "string",
  "permissions": ["alarm:read", "alarm:verify", "control:authorize"],
  "token": "string"
}
```

---

### 6.6 缓存管理

#### API-F10-014: 获取缓存数据

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-014 |
| 接口名称 | 获取缓存数据 |
| 路径 | `/mobile/cache` |
| HTTP方法 | GET |
| 权限要求 | 已登录 |

**请求参数 (Query)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cache_type | string | 是 | 缓存类型: buildings/devices/emergency_plans |

**响应结构 (data)**

```json
{
  "cache_type": "emergency_plans",
  "data": [...],
  "version": "2026061501",
  "update_time": "2026-06-15T00:00:00+08:00",
  "expire_time": "2026-06-22T00:00:00+08:00"
}
```

---

#### API-F10-015: 同步确认

| 属性 | 值 |
|------|---|
| 接口编号 | API-F10-015 |
| 接口名称 | 同步确认 |
| 路径 | `/mobile/sync/confirm` |
| HTTP方法 | POST |
| 权限要求 | 已登录 |

**请求参数 (Body)**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sync_id | string | 是 | 同步批次ID |
| confirmed_items | array | 是 | 已确认处理的项目ID列表 |

---

## 7. WebSocket实时接口

### 7.1 连接规范

#### WS-F1: 建立连接

| 属性 | 值 |
|------|---|
| 连接地址 | `wss://{host}/api/v1/ws` |
| 协议版本 | v1 |
| 心跳间隔 | 30秒 |
| 心跳消息 | `{"type":"ping"}` / `{"type":"pong"}` |

**连接认证**

连接时需在URL或首条消息中携带Token:

```
wss://{host}/api/v1/ws?token={access_token}
```

或首条消息:

```json
{
  "type": "auth",
  "token": "Bearer {access_token}"
}
```

**认证响应**

```json
{
  "type": "auth_result",
  "code": 0,
  "message": "success",
  "data": {
    "user_id": "string",
    "user_name": "string",
    "role_level": 1,
    "subscribed_topics": ["alarm", "device_status"]
  }
}
```

---

### 7.2 订阅机制

#### 订阅消息

```json
{
  "type": "subscribe",
  "topics": ["alarm", "device_status", "roll_call"]
}
```

#### 取消订阅

```json
{
  "type": "unsubscribe",
  "topics": ["alarm"]
}
```

#### 可订阅主题

| 主题名 | 说明 | 权限要求 |
|--------|------|----------|
| alarm | 告警推送 | 已登录 |
| alarm_status | 告警状态变更 | 已登录 |
| device_status | 设备状态变更 | 已登录 |
| roll_call | 查岗通知 | 已登录 |
| system | 系统通知 | 已登录 |

---

### 7.3 消息格式

#### 统一消息格式

```json
{
  "type": "alarm",          // 消息类型
  "id": "msg_001",          // 消息唯一ID
  "timestamp": "2026-06-15T10:30:00+08:00",  // 消息时间
  "priority": "high",       // 优先级: high/normal/low
  "payload": {}             // 消息内容
}
```

---

### 7.4 告警推送

#### WS-ALA-001: 新告警推送

**触发条件**: 系统接收到新的消防告警

```json
{
  "type": "alarm",
  "id": "msg_001",
  "timestamp": "2026-06-15T10:30:00+08:00",
  "priority": "high",
  "payload": {
    "alarm_id": "string",
    "alarm_no": "ALA20260615001",
    "alarm_type": "fire_alarm",
    "signal_type": 1,
    "alarm_level": 1,
    "org_code": "string",
    "org_name": "string",
    "building_id": "string",
    "building_name": "string",
    "device_id": "string",
    "device_name": "string",
    "location": "3#2层餐饮区",
    "alarm_time": "2026-06-15T10:30:00+08:00",
    "handle_status": 1,
    "ai_result": {
      "fire": 0.95,
      "smoke": 0.78
    },
    "linkage_cameras": [{
      "camera_id": "string",
      "camera_name": "string",
      "stream_url": "string"
    }]
  }
}
```

**合规标注**: T/CFPA 052-2026 5.4.2 (告警到推送≤10秒)

---

#### WS-ALA-002: 告警状态变更推送

**触发条件**: 告警处置状态发生变化

```json
{
  "type": "alarm_status",
  "id": "msg_002",
  "timestamp": "2026-06-15T10:35:00+08:00",
  "priority": "normal",
  "payload": {
    "alarm_id": "string",
    "alarm_no": "ALA20260615001",
    "old_status": 2,
    "new_status": 3,
    "change_time": "2026-06-15T10:35:00+08:00",
    "operator_id": "string",
    "operator_name": "string",
    "remark": "已确认误报"
  }
}
```

**状态码对照**

| 值 | 状态 |
|----|------|
| 1 | 待核实 |
| 2 | 核实中 |
| 3 | 处置中 |
| 4 | 已闭环 |

---

### 7.5 设备状态变更推送

#### WS-DEV-001: 设备状态变更

**触发条件**: 设备在线/离线/故障状态发生变化

```json
{
  "type": "device_status",
  "id": "msg_003",
  "timestamp": "2026-06-15T10:40:00+08:00",
  "priority": "low",
  "payload": {
    "device_id": "string",
    "device_name": "string",
    "device_type": 1,
    "building_id": "string",
    "building_name": "string",
    "old_status": 1,
    "new_status": 3,
    "fault_code": "E001",
    "fault_desc": "通信故障",
    "change_time": "2026-06-15T10:40:00+08:00"
  }
}
```

**设备状态码对照**

| 值 | 状态 | 说明 |
|----|------|------|
| 1 | 在线 | 正常运行 |
| 2 | 离线 | 设备断线 |
| 3 | 故障 | 设备异常 |

---

#### WS-DEV-002: 设备数据上报

**触发条件**: 设备上传监测数据

```json
{
  "type": "device_data",
  "id": "msg_004",
  "timestamp": "2026-06-15T10:41:00+08:00",
  "priority": "low",
  "payload": {
    "device_id": "string",
    "data_type": "pressure",
    "value": 0.85,
    "unit": "MPa",
    "collect_time": "2026-06-15T10:41:00+08:00"
  }
}
```

---

### 7.6 查岗通知推送

#### WS-RC-001: 查岗请求推送

**触发条件**: 发起查岗任务

```json
{
  "type": "roll_call",
  "id": "msg_005",
  "timestamp": "2026-06-15T10:45:00+08:00",
  "priority": "high",
  "payload": {
    "task_id": "string",
    "task_type": 1,
    "check_content": "夜间值班抽查",
    "check_time": "2026-06-15T10:45:00+08:00",
    "deadline_time": "2026-06-15T10:50:00+08:00",
    "timeout_seconds": 300,
    "response_url": "/api/v1/roll-call/tasks/string/response"
  }
}
```

---

#### WS-RC-002: 查岗升级推送

**触发条件**: 查岗超时无应答触发升级

```json
{
  "type": "roll_call_escalation",
  "id": "msg_006",
  "timestamp": "2026-06-15T10:50:00+08:00",
  "priority": "high",
  "payload": {
    "task_id": "string",
    "escalation_id": "string",
    "escalation_level": 1,
    "escalate_to_user_id": "string",
    "escalate_to_user_name": "string",
    "escalate_to_role": "消防安全管理人",
    "original_task": {
      "unit_name": "string",
      "room_name": "string",
      "duty_person_name": "string",
      "check_time": "2026-06-15T10:45:00+08:00",
      "deadline_time": "2026-06-15T10:50:00+08:00"
    }
  }
}
```

---

### 7.7 系统通知

#### WS-SYS-001: 系统消息推送

```json
{
  "type": "system",
  "id": "msg_007",
  "timestamp": "2026-06-15T10:55:00+08:00",
  "priority": "normal",
  "payload": {
    "title": "系统维护通知",
    "content": "系统将于2026-06-16 02:00-04:00进行维护",
    "action_type": "url",
    "action_url": "/notice/123"
  }
}
```

---

### 7.8 心跳与重连

#### 心跳机制

| 参数 | 值 |
|------|---|
| 心跳间隔 | 30秒 |
| 超时时间 | 60秒 (连续2次未收到pong) |
| ping消息 | `{"type":"ping","timestamp":1234567890}` |
| pong响应 | `{"type":"pong","server_time":1234567890}` |

#### 重连策略

| 策略 | 参数 |
|------|------|
| 首次重连延迟 | 1秒 |
| 最大重连延迟 | 30秒 |
| 重连倍数 | 2倍 |
| 最大重连次数 | 10次 |
| 重连公式 | delay = min(30, 1 * 2^n) |

#### 重连消息格式

```json
{
  "type": "reconnect",
  "reason": "network_error",
  "last_message_id": "msg_006",
  "reconnect_time": "2026-06-15T10:55:30+08:00"
}
```

---

### 7.9 错误处理

#### 错误消息格式

```json
{
  "type": "error",
  "id": "msg_err_001",
  "timestamp": "2026-06-15T10:30:00+08:00",
  "payload": {
    "code": 40001,
    "message": "订阅主题不存在",
    "detail": "topic 'xxx' is not supported"
  }
}
```

#### 错误码对照

| 错误码 | 说明 |
|--------|------|
| 40001 | 订阅主题不存在 |
| 40002 | 消息格式错误 |
| 40003 | 权限不足 |
| 40004 | Token无效 |
| 40005 | 连接已关闭 |
| 40006 | 消息过大 |

---

## 8. 错误码汇总表

### 8.1 系统级错误码

| 错误码 | 说明 | HTTP状态码 |
|--------|------|-------------|
| 0 | 成功 | 200 |
| 10001 | 系统内部错误 | 500 |
| 10002 | 服务暂不可用 | 503 |
| 10003 | 请求参数错误 | 400 |
| 10004 | 请求方法不支持 | 405 |
| 10005 | 资源不存在 | 404 |
| 10006 | 资源已存在 | 409 |
| 10007 | 认证失败 | 401 |
| 10008 | 权限不足 | 403 |
| 10009 | 请求频率超限 | 429 |
| 10010 | Token无效或已过期 | 401 |

### 8.2 F6 视频联动模块错误码

| 错误码 | 说明 |
|--------|------|
| 60101 | 设备编码已存在 |
| 60102 | 设备类型不支持 |
| 60103 | RTSP地址格式错误 |
| 60104 | 单位不存在 |
| 60201 | 查询参数错误 |
| 60401 | 设备不存在 |
| 60402 | 更新内容为空 |
| 60501 | 设备不存在 |
| 60502 | 设备有关联录像不可删除 |
| 61001 | 设备不在线 |
| 61002 | 流服务异常 |
| 61901 | 联动记录不存在 |
| 61902 | 告警已处置完成 |

### 8.3 F7 查岗管理模块错误码

| 错误码 | 说明 |
|--------|------|
| 70601 | 查岗对象为空 |
| 70602 | 查岗对象不存在 |
| 70901 | 任务不存在 |
| 70902 | 任务已响应无法取消 |
| 70903 | 任务已完成无法取消 |
| 71001 | 任务不存在 |
| 71002 | 任务已响应 |
| 71003 | 响应超时 |
| 71401 | 脱岗记录不存在 |
| 71402 | 整改证据不能为空 |

### 8.4 F9 权限与安全模块错误码

| 错误码 | 说明 |
|--------|------|
| 90101 | 用户名或密码错误 |
| 90102 | 账号已锁定 |
| 90103 | 账号已停用 |
| 90104 | 证书已过期 |
| 90105 | 验证码错误 |
| 90501 | 用户编码已存在 |
| 90502 | 身份证号已注册 |
| 90503 | 手机号已注册 |
| 90504 | 角色不存在 |
| 90505 | 密码复杂度不足 |
| 90901 | 用户不存在 |
| 90902 | 用户有关联数据无法删除 |
| 91001 | 原密码错误 |
| 91002 | 新密码与原密码相同 |
| 91003 | 密码复杂度不足 |
| 91004 | 两次密码输入不一致 |
| 91701 | 系统预置角色不可删除 |
| 91702 | 角色有关联用户无法删除 |

### 8.5 WebSocket错误码

| 错误码 | 说明 |
|--------|------|
| 40001 | 订阅主题不存在 |
| 40002 | 消息格式错误 |
| 40003 | 权限不足 |
| 40004 | Token无效 |
| 40005 | 连接已关闭 |
| 40006 | 消息过大 |

---

## 附录A: 接口统计

| 模块 | 接口数量 | 说明 |
|------|---------|------|
| F6 视频联动 | 22 | 设备管理、通道配置、实时预览、录像回放、AI标注、取证 |
| F7 查岗管理 | 18 | 配置管理、任务管理、响应、脱岗、升级、统计 |
| F8 GIS态势感知 | 15 | 建筑标记、设备标记、告警标记、热力图、区域统计 |
| F9 权限与安全 | 29 | 认证、用户管理、角色管理、权限管理、证书管理、审计 |
| F10 移动端 | 15 | 认证、推送、值守、取证、离线同步、角色切换 |
| WebSocket | 8 | 告警推送、设备状态、查岗通知、系统消息 |
| **合计** | **107** | |

---

## 附录B: 变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|---------|--------|
| V1.0 | 2026-06-15 | 初始版本，完成F6-F10及WebSocket接口设计 | API设计团队 |

---

**文档结束**

---

> 本内容由 Coze AI 生成，请遵循相关法律法规及《人工智能生成合成内容标识办法》使用与传播。
