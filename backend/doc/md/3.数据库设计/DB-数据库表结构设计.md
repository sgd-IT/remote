
# 智慧消防远程值守系统 - 数据库表结构设计

> 版本：V1.0  
> 制定日期：2026-06-15  
> 设计依据：GB/T 26875.1-2026、GB 25506-2010、GB 25201-2010、T/CFPA 052-2026、T/GDCAA 016-2025  
> 数据库选型：MySQL 8.0+ / PostgreSQL 14+

---

## 1. 设计概述

### 1.1 设计目标

本数据库设计文档基于智慧消防远程值守系统的10份产品需求文档（PRD），完成完整的关系型数据库表结构设计，确保：

- 覆盖全部10个功能模块（F1-F10）的数据实体
- 满足GB/T 26875.1-2026等标准的合规要求
- 实现数据的完整性、一致性和可追溯性
- 支持数据不可篡改的安全需求

### 1.2 命名规范

| 规范项 | 规则 |
|--------|------|
| 表名 | `tb_{模块}_{业务实体}`，使用小写，单词间用下划线分隔 |
| 字段名 | `snake_case`，全小写，单词间用下划线分隔 |
| 主键 | `id`（BigInt）或业务主键 |
| 外键 | `{表名}_{参照表主键}` |
| 索引 | `idx_{表名}_{字段名}`，联合索引加下划线连接 |
| 唯一索引 | `uk_{表名}_{字段名组合}` |
| 时间字段 | `*_time`（DateTime类型），`_*_date`（Date类型） |

### 1.3 字段类型规范

| 类型 | 说明 | 适用场景 |
|------|------|---------|
| BIGINT | 主键、ID | 整数型标识 |
| VARCHAR(n) | 可变长度字符串 | 短文本、编码、名称 |
| TEXT | 长文本 | 描述、备注、JSON |
| DECIMAL(p,s) | 精确数值 | 金额、百分比、坐标 |
| DATETIME | 日期时间 | 时间戳（Asia/Shanghai） |
| DATE | 日期 | 日期类型数据 |
| TINYINT | 微整型 | 状态、枚举值（0-255） |
| JSON | JSON数据 | 复杂对象、列表 |
| GEO | Point/Polygon | 地理坐标（PostGIS） |

### 1.4 时区规范

- 所有时间字段统一使用时区：**Asia/Shanghai（UTC+8）**
- 时间格式：ISO 8601 `YYYY-MM-DD HH:mm:ss`
- 存储使用datetime类型（MySQL）或timestamp with time zone（PostgreSQL）

---

## 2. ER关系总览

### 2.1 核心实体关系图（文字版）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           核心实体关系图                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐               │
│  │   组织架构    │     │    用户      │     │    角色      │               │
│  │ org_unit     │────→│ sys_user     │←────│ sys_role     │               │
│  └──────────────┘     └──────┬───────┘     └──────────────┘               │
│         │                    │                                           │
│         │                    ▼                                           │
│         │            ┌──────────────┐                                    │
│         │            │  用户角色关系  │                                    │
│         │            │sys_user_role  │                                    │
│         │            └──────────────┘                                    │
│         │                    │                                           │
│         ▼                    ▼                                           │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐               │
│  │   建筑物      │←───│  监测点      │────→│  设备台账    │               │
│  │   building   │     │monitor_point │     │device_asset  │               │
│  └──────────────┘     └──────────────┘     └──────────────┘               │
│         │                    │                    │                       │
│         │                    ▼                    ▼                       │
│         │            ┌──────────────┐     ┌──────────────┐               │
│         │            │  告警记录    │────→│  处置流程    │               │
│         │            │alarm_record  │     │handle_process│               │
│         │            └──────┬───────┘     └──────────────┘               │
│         │                   │                                             │
│         │                   ▼                                             │
│         │            ┌──────────────┐     ┌──────────────┐               │
│         │            │  通知记录    │     │  复核记录    │               │
│         │            │notify_record│     │review_record │               │
│         │            └──────────────┘     └──────────────┘               │
│         │                                                             │
│         ▼                                                             │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐               │
│  │   视频设备   │←───│  视频通道    │────→│  录像记录    │               │
│  │video_device  │     │video_linkage│     │video_record  │               │
│  └──────────────┘     └──────────────┘     └──────────────┘               │
│         │                                                             │
│         └─────────────────┬───────────────────────────────────────────┘   │
│                           │                                               │
│                           ▼                                               │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐               │
│  │   查岗任务   │←───│  查岗配置    │────→│  脱岗记录    │               │
│  │roll_call_task│    │roll_call_cfg│     │absent_record │               │
│  └──────────────┘     └──────────────┘     └──────────────┘               │
│         │                                                             │
│         └─────────────────┬───────────────────────────────────────────┘   │
│                           │                                               │
│                           ▼                                               │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐               │
│  │   维保工单   │←───│  巡检路线    │────→│  值班排班    │               │
│  │ work_order   │     │inspection_rt │     │ duty_schedule│               │
│  └──────────────┘     └──────────────┘     └──────────────┘               │
│         │                                                             │
│         ▼                                                             │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐               │
│  │  维保记录    │     │  交接班记录  │     │  数据签名    │               │
│  │maint_record  │     │ shift_handoff│     │ data_signature│              │
│  └──────────────┘     └──────────────┘     └──────────────┘               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心外键关系说明

| 关系 | 说明 |
|------|------|
| org_unit → building | 建筑物属于组织单位 |
| building → monitor_point | 监测点属于建筑物 |
| building → video_device | 视频设备属于建筑物 |
| device_asset → building | 设备属于建筑物 |
| alarm_record → building | 告警属于建筑物 |
| alarm_record → monitor_point | 告警关联监测点 |
| handle_process → alarm_record | 处置流程属于告警 |
| notify_record → alarm_record | 通知记录属于告警 |
| work_order → device_asset | 维保工单关联设备 |
| inspection_task → inspection_route | 巡检任务属于路线 |
| duty_schedule → building | 排班属于建筑物 |

---

## 3. 系统基础模块（F9 - 权限与安全）

### 3.1 组织架构表（org_unit）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| unit_code | VARCHAR | 32 | 是 | - | 单位编码 | GB/T 26875.1-2026 附录B |
| unit_name | VARCHAR | 200 | 是 | - | 单位名称 | GB/T 26875.1-2026 附录B |
| unit_type | TINYINT | - | 是 | - | 单位类型（1-重点单位/2-一般单位/3-九小场所） | T/GDCAA 016-2025 |
| parent_id | BIGINT | - | 否 | NULL | 上级单位ID | - |
| district_code | VARCHAR | 20 | 否 | - | 行政区划编码 | - |
| address | VARCHAR | 500 | 否 | - | 详细地址 | - |
| longitude | DECIMAL | 10,6 | 否 | - | 经度 | - |
| latitude | DECIMAL | 10,6 | 否 | - | 纬度 | - |
| risk_level | TINYINT | - | 是 | 2 | 风险等级（1-高/2-中/3-低） | T/GDCAA 016-2025 |
| is_active | TINYINT | - | 是 | 1 | 启用状态（1-是/0-否） | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |
| update_time | DATETIME | - | 是 | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_unit_code` (`unit_code`)
- KEY `idx_parent_id` (`parent_id`)
- KEY `idx_risk_level` (`risk_level`)

---

### 3.2 用户信息表（sys_user）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| user_code | VARCHAR | 32 | 是 | - | 用户编码 | - |
| user_name | VARCHAR | 64 | 是 | - | 用户姓名 | GB 25201-2010 |
| id_card | VARCHAR | 18 | 是 | - | 身份证号（加密） | T/CFPA 052-2026 |
| phone | VARCHAR | 11 | 是 | - | 手机号 | T/CFPA 052-2026 |
| email | VARCHAR | 128 | 否 | - | 邮箱 | GB 25201-2010 |
| unit_id | BIGINT | - | 是 | - | 所属单位ID | - |
| certificate_id | VARCHAR | 64 | 否 | - | 证书编号 | T/CFPA 052-2026 |
| certificate_type | VARCHAR | 32 | 否 | - | 证书类型 | T/CFPA 052-2026 |
| certificate_level | VARCHAR | 8 | 否 | - | 证书等级（四级/三级/二级） | T/CFPA 052-2026 |
| certificate_expire | DATE | - | 否 | - | 证书有效期 | 消防法第21条 |
| cert_status | TINYINT | - | 是 | 1 | 证书状态（1-有效/2-临期/3-过期） | T/CFPA 052-2026 |
| account_status | TINYINT | - | 是 | 1 | 账号状态（1-正常/2-锁定/3-停用） | T/GDCAA 016-2025 |
| password_hash | VARCHAR | 128 | 是 | - | 密码哈希（bcrypt） | GB/T 22239-2019 |
| password_expire | DATE | - | 是 | - | 密码有效期 | GB 25201-2010 |
| last_login_time | DATETIME | - | 否 | - | 最后登录时间 | GB 25201-2010 |
| last_login_ip | VARCHAR | 45 | 否 | - | 最后登录IP | T/GDCAA 016-2025 |
| fail_login_count | INT | - | 是 | 0 | 连续登录失败次数 | T/GDCAA 016-2025 |
| lock_expire | DATETIME | - | 否 | - | 锁定过期时间 | T/GDCAA 016-2025 |
| is_deleted | TINYINT | - | 是 | 0 | 逻辑删除标记 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | GB 25201-2010 |
| update_time | DATETIME | - | 是 | - | 更新时间 | GB 25201-2010 |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_user_code` (`user_code`)
- UNIQUE KEY `uk_id_card` (`id_card`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_cert_status` (`cert_status`)
- KEY `idx_cert_expire` (`certificate_expire`)

---

### 3.3 角色信息表（sys_role）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| role_code | VARCHAR | 16 | 是 | - | 角色编码 | T/CFPA 052-2026 |
| role_name | VARCHAR | 32 | 是 | - | 角色名称 | T/CFPA 052-2026 |
| role_level | INT | - | 是 | - | 角色等级（1-普通/2-操作/3-管理） | T/CFPA 052-2026 |
| role_desc | VARCHAR | 256 | 否 | - | 角色描述 | GB 25201-2010 |
| is_system | TINYINT | - | 是 | 0 | 是否系统预置（1-是/0-否） | T/CFPA 052-2026 |
| permissions | JSON | - | 是 | - | 权限列表 | T/CFPA 052-2026 |
| data_scope | JSON | - | 是 | - | 数据权限范围 | T/GDCAA 016-2025 |
| is_active | TINYINT | - | 是 | 1 | 启用状态 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | GB 25201-2010 |
| update_time | DATETIME | - | 是 | - | 更新时间 | GB 25201-2010 |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_role_code` (`role_code`)
- KEY `idx_role_level` (`role_level`)

**预置角色**：
- `ROLE_WATCHER`（L1 普通值守员）
- `ROLE_SUPERVISOR`（L2 值守主管）
- `ROLE_ADMIN`（L3 技术负责人）

---

### 3.4 用户角色关系表（sys_user_role）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| user_id | BIGINT | - | 是 | - | 用户ID | T/CFPA 052-2026 |
| role_id | BIGINT | - | 是 | - | 角色ID | T/CFPA 052-2026 |
| create_time | DATETIME | - | 是 | - | 创建时间 | GB 25201-2010 |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
- KEY `idx_role_id` (`role_id`)

---

### 3.5 权限信息表（sys_permission）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| permission_code | VARCHAR | 64 | 是 | - | 权限编码 | T/CFPA 052-2026 |
| permission_name | VARCHAR | 64 | 是 | - | 权限名称 | T/CFPA 052-2026 |
| permission_type | VARCHAR | 16 | 是 | - | 权限类型（menu/button/api/data） | T/CFPA 052-2026 |
| module_id | VARCHAR | 32 | 是 | - | 所属模块 | T/CFPA 052-2026 |
| risk_level | INT | - | 是 | 1 | 风险等级（1-低/2-中/3-高） | T/GDCAA 016-2025 |
| require_review | TINYINT | - | 是 | 0 | 是否需要复核（1-是/0-否） | T/CFPA 052-2026 |
| min_role_level | INT | - | 是 | 1 | 最低角色等级 | T/CFPA 052-2026 |
| is_active | TINYINT | - | 是 | 1 | 启用状态 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | GB 25201-2010 |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_permission_code` (`permission_code`)
- KEY `idx_module_id` (`module_id`)
- KEY `idx_risk_level` (`risk_level`)

---

### 3.6 安全审计日志表（sys_security_log）

> **重要**：此表数据禁止修改和删除

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| log_id | VARCHAR | 64 | 是 | - | 日志唯一标识 | T/CFPA 052-2026 |
| log_type | VARCHAR | 16 | 是 | - | 日志类型 | GB 25201-2010 |
| user_id | VARCHAR | 32 | 是 | - | 操作用户ID | T/CFPA 052-2026 |
| user_name | VARCHAR | 64 | 是 | - | 操作用户姓名 | GB 25201-2010 |
| user_role | VARCHAR | 16 | 是 | - | 操作用户角色 | T/CFPA 052-2026 |
| operation | VARCHAR | 64 | 是 | - | 操作类型 | T/CFPA 052-2026 |
| operation_module | VARCHAR | 50 | 否 | - | 操作模块 | - |
| operation_detail | JSON | - | 是 | - | 操作详情 | T/GDCAA 016-2025 |
| target_object | VARCHAR | 128 | 否 | - | 操作对象 | GB 25201-2010 |
| ip_address | VARCHAR | 45 | 是 | - | 操作终端IP | T/GDCAA 016-2025 |
| terminal_info | VARCHAR | 128 | 否 | - | 终端信息 | GB 25201-2010 |
| session_id | VARCHAR | 64 | 是 | - | 会话ID | - |
| result | VARCHAR | 16 | 是 | - | 操作结果（success/fail） | GB 25201-2010 |
| failure_reason | VARCHAR | 256 | 否 | - | 失败原因 | T/GDCAA 016-2025 |
| prev_hash | VARCHAR | 64 | 是 | - | 前一条日志哈希 | T/CFPA 052-2026 |
| hash_value | VARCHAR | 64 | 是 | - | 本条日志哈希（SHA-256） | T/CFPA 052-2026 |
| log_time | DATETIME | - | 是 | - | 操作时间 | T/CFPA 052-2026 |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_log_id` (`log_id`)
- KEY `idx_user_id` (`user_id`)
- KEY `idx_log_time` (`log_time`)
- KEY `idx_operation` (`operation`)
- KEY `idx_hash_chain` (`prev_hash`, `hash_value`)

**分区策略**：
```sql
PARTITION BY RANGE (TO_DAYS(log_time)) (
    PARTITION p_2026_q1 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p_2026_q2 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p_2026_q3 VALUES LESS THAN (TO_DAYS('2026-10-01')),
    PARTITION p_2026_q4 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
)
```

---

### 3.7 证书信息表（sys_certificate）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| cert_id | VARCHAR | 64 | 是 | - | 证书记录ID | T/CFPA 052-2026 |
| user_id | VARCHAR | 32 | 是 | - | 用户ID | T/CFPA 052-2026 |
| cert_number | VARCHAR | 64 | 是 | - | 证书编号 | T/CFPA 052-2026 |
| cert_type | VARCHAR | 32 | 是 | - | 证书类型 | T/CFPA 052-2026 |
| cert_level | VARCHAR | 8 | 是 | - | 证书等级 | T/CFPA 052-2026 |
| issue_org | VARCHAR | 128 | 是 | - | 发证机构 | T/CFPA 052-2026 |
| issue_date | DATE | - | 是 | - | 发证日期 | 消防法第21条 |
| expire_date | DATE | - | 是 | - | 有效期至 | 消防法第21条 |
| verify_status | VARCHAR | 16 | 是 | - | 核验状态 | T/CFPA 052-2026 |
| verify_result | JSON | - | 否 | - | 核验结果 | T/CFPA 052-2026 |
| cert_file | VARCHAR | 256 | 否 | - | 证书文件路径 | T/CFPA 052-2026 |
| is_active | TINYINT | - | 是 | 1 | 启用状态 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | GB 25201-2010 |
| update_time | DATETIME | - | 是 | - | 更新时间 | GB 25201-2010 |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_cert_number` (`cert_number`)
- KEY `idx_user_id` (`user_id`)
- KEY `idx_expire_date` (`expire_date`)
- KEY `idx_verify_status` (`verify_status`)

---

## 4. 火警监测与处置模块（F1）

### 4.1 告警记录表（alarm_record）

> **重要**：此表数据禁止修改和删除

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| alarm_no | VARCHAR | 32 | 是 | - | 告警编号 | GB/T 26875.1-2026 |
| org_code | VARCHAR | 32 | 是 | - | 联网单位编码 | GB/T 26875.1-2026 附录A |
| org_name | VARCHAR | 128 | 是 | - | 联网单位名称 | GB/T 26875.1-2026 |
| building_id | VARCHAR | 32 | 是 | - | 建筑ID | - |
| building_name | VARCHAR | 128 | 否 | - | 建筑名称 | - |
| alarm_time | DATETIME | - | 是 | - | 原始报警时间 | GB/T 26875.3 |
| receive_time | DATETIME | - | 是 | - | 平台接收时间 | GB/T 26875.1-2026 |
| signal_type | TINYINT | - | 是 | - | 信号类型（1-火警/2-故障/3-监管/4-屏蔽/5-联动） | GB/T 26875.1-2026 附录A |
| device_type | TINYINT | - | 是 | - | 设备类型 | GB/T 26875.1-2026 附录A |
| device_code | VARCHAR | 64 | 是 | - | 设备地址编码（回路+点位） | GB/T 26875.1-2026 附录A |
| location | VARCHAR | 256 | 是 | - | 报警部位描述 | GB 25506-2010 |
| verify_result | TINYINT | - | 否 | - | 核实结果（1-真实火警/2-误报/3-未核实） | - |
| verify_time | DATETIME | - | 否 | - | 核实完成时间 | - |
| verify_method | TINYINT | - | 否 | - | 核实方式（1-视频复核/2-电话确认/3-现场确认） | T/CFPA 052-2026 |
| false_alarm_reason | VARCHAR | 256 | 否 | - | 误报原因 | GB 25201-2010 |
| handler_id | VARCHAR | 32 | 否 | - | 处置人ID | - |
| handler_name | VARCHAR | 64 | 否 | - | 处置人姓名 | - |
| handle_start_time | DATETIME | - | 否 | - | 处置开始时间 | - |
| handle_end_time | DATETIME | - | 否 | - | 处置结束时间 | - |
| handle_status | TINYINT | - | 是 | 1 | 处置状态（1-待核实/2-核实中/3-处置中/4-已闭环） | - |
| handle_result | TEXT | - | 否 | - | 处置结果 | - |
| notify_119 | TINYINT | - | 否 | 0 | 是否拨打119（1-是/0-否） | GB 25506-2010 |
| notify_119_time | DATETIME | - | 否 | - | 拨打119时间 | - |
| related_video_ids | JSON | - | 否 | - | 关联视频录像ID | T/CFPA 052-2026 |
| ai_analysis_result | JSON | - | 否 | - | AI分析结果 | T/CFPA 052-2026 |
| is_archived | TINYINT | - | 是 | 0 | 是否归档（1-是/0-否） | T/CFPA 052-2026 |
| archive_time | DATETIME | - | 否 | - | 归档时间 | - |
| hash_value | VARCHAR | 64 | 是 | - | 数据哈希值 | T/CFPA 052-2026 |
| is_deleted | TINYINT | - | 是 | 0 | 逻辑删除标记（仅标记，不可物理删除） | T/CFPA 052-2026 |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |
| update_time | DATETIME | - | 是 | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_alarm_no` (`alarm_no`)
- KEY `idx_org_code` (`org_code`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_alarm_time` (`alarm_time`)
- KEY `idx_signal_type` (`signal_type`)
- KEY `idx_handle_status` (`handle_status`)
- KEY `idx_verify_result` (`verify_result`)

**分区策略**：按`alarm_time`按月分区

---

### 4.2 处置流程记录表（handle_process）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| process_id | VARCHAR | 32 | 是 | - | 处置记录ID | - |
| alarm_no | VARCHAR | 32 | 是 | - | 关联告警编号 | T/CFPA 052-2026 |
| step_no | INT | - | 是 | - | 步骤序号 | T/CFPA 052-2026 |
| step_name | VARCHAR | 64 | 是 | - | 步骤名称 | T/CFPA 052-2026 |
| operator_id | VARCHAR | 32 | 是 | - | 操作人ID | T/CFPA 052-2026 |
| operator_name | VARCHAR | 64 | 是 | - | 操作人姓名 | GB 25201-2010 |
| start_time | DATETIME | - | 是 | - | 开始时间 | T/CFPA 052-2026 |
| end_time | DATETIME | - | 否 | - | 完成时间 | - |
| result | VARCHAR | 512 | 否 | - | 执行结果 | - |
| remark | VARCHAR | 512 | 否 | - | 备注 | - |
| is_auto | TINYINT | - | 是 | 0 | 是否自动执行（1-是/0-否） | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_alarm_no` (`alarm_no`)
- KEY `idx_operator_id` (`operator_id`)
- KEY `idx_start_time` (`start_time`)

---

### 4.3 通知记录表（notify_record）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| notify_id | VARCHAR | 32 | 是 | - | 通知记录ID | - |
| alarm_no | VARCHAR | 32 | 是 | - | 关联告警编号 | T/CFPA 052-2026 |
| notify_type | TINYINT | - | 是 | - | 通知类型（1-语音/2-短信/3-APP推送/4-电话） | T/CFPA 052-2026 |
| target_role | TINYINT | - | 是 | - | 通知对象角色（1-119/2-消控室/3-业主/4-微消站/5-负责人） | T/CFPA 052-2026 |
| target_name | VARCHAR | 64 | 是 | - | 被通知人姓名 | - |
| target_phone | VARCHAR | 20 | 是 | - | 被通知人电话 | - |
| send_time | DATETIME | - | 是 | - | 发送时间 | T/CFPA 052-2026 |
| deliver_status | TINYINT | - | 是 | - | 送达状态（1-已发送/2-已送达/3-已读/4-发送失败） | - |
| read_time | DATETIME | - | 否 | - | 阅读时间 | - |
| call_duration | INT | - | 否 | - | 通话时长（秒，仅语音） | - |
| recording_url | VARCHAR | 256 | 否 | - | 通话录音地址 | T/CFPA 052-2026 |
| is_deleted | TINYINT | - | 是 | 0 | 逻辑删除标记 | T/CFPA 052-2026 |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_alarm_no` (`alarm_no`)
- KEY `idx_target_phone` (`target_phone`)
- KEY `idx_send_time` (`send_time`)

---

## 5. 远程控制模块（F2）

### 5.1 控制命令记录表（control_command）

> **重要**：此表数据禁止修改和删除

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| command_id | VARCHAR | 64 | 是 | - | 命令唯一标识 | T/CFPA 052-2026 |
| alarm_no | VARCHAR | 32 | 否 | - | 关联告警编号 | - |
| device_id | VARCHAR | 32 | 是 | - | 目标设备ID | GB/T 26875.3 |
| device_name | VARCHAR | 100 | 是 | - | 设备名称 | GB 50116-2013 |
| device_type | VARCHAR | 32 | 是 | - | 设备类型编码 | GB 50116-2013 |
| command_type | VARCHAR | 16 | 是 | - | 命令类型（START/STOP/SET/SILENCE/RESET/SWITCH） | T/GDCAA 016-2025 |
| command_data | JSON | - | 是 | - | 命令参数 | GB/T 26875.3 |
| protocol_format | VARCHAR | 16 | 是 | - | 协议格式版本 | GB/T 26875.3 |
| operator_id | VARCHAR | 32 | 是 | - | 操作员ID | T/CFPA 052-2026 |
| operator_name | VARCHAR | 64 | 是 | - | 操作员姓名 | GB 25201-2010 |
| reviewer_id | VARCHAR | 32 | 否 | - | 复核员ID（高风险必填） | T/CFPA 052-2026 |
| reviewer_name | VARCHAR | 64 | 否 | - | 复核员姓名 | - |
| authorization_code | VARCHAR | 32 | 否 | - | 业主授权码 | T/GDCAA 016-2025 |
| command_status | VARCHAR | 16 | 是 | - | 命令状态（pending/sending/confirmed/success/fail/timeout） | GB/T 26875.3 |
| sent_time | DATETIME | - | 是 | - | 命令下发时间 | T/CFPA 052-2026 |
| response_time | DATETIME | - | 否 | - | 设备响应时间 | GB/T 26875.3 |
| completed_time | DATETIME | - | 否 | - | 命令完成时间 | T/CFPA 052-2026 |
| failure_reason | VARCHAR | 256 | 否 | - | 失败原因 | T/GDCAA 016-2025 |
| result_feedback | JSON | - | 否 | - | 执行结果反馈 | - |
| hash_value | VARCHAR | 64 | 是 | - | 日志哈希值 | T/CFPA 052-2026 |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_command_id` (`command_id`)
- KEY `idx_device_id` (`device_id`)
- KEY `idx_alarm_no` (`alarm_no`)
- KEY `idx_operator_id` (`operator_id`)
- KEY `idx_command_type` (`command_type`)
- KEY `idx_sent_time` (`sent_time`)
- KEY `idx_command_status` (`command_status`)

**分区策略**：按`sent_time`按月分区

---

### 5.2 设备状态表（device_status）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| device_id | VARCHAR | 32 | 是 | - | 设备ID | GB/T 26875.3 |
| device_name | VARCHAR | 64 | 是 | - | 设备名称 | GB 50116-2013 |
| device_type | VARCHAR | 32 | 是 | - | 设备类型 | GB 50116-2013 |
| building_id | VARCHAR | 32 | 是 | - | 所属建筑ID | - |
| location_code | VARCHAR | 32 | 是 | - | 位置编码 | GB/T 26875.3 |
| control_mode | VARCHAR | 16 | 是 | - | 控制模式（manual/auto） | GB 25506-2010 |
| run_status | VARCHAR | 16 | 是 | - | 运行状态（running/stopped/fault） | GB 50116-2013 |
| alarm_status | VARCHAR | 16 | 是 | - | 报警状态 | GB 50116-2013 |
| fault_status | VARCHAR | 16 | 是 | - | 故障状态 | GB 50116-2013 |
| last_control_time | DATETIME | - | 是 | - | 最后控制时间 | T/GDCAA 016-2025 |
| last_control_user | VARCHAR | 32 | 是 | - | 最后控制人 | T/CFPA 052-2026 |
| last_heartbeat | DATETIME | - | 是 | - | 最后心跳时间 | GB/T 26875.3 |
| update_time | DATETIME | - | 是 | - | 状态更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_device_id` (`device_id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_device_type` (`device_type`)
- KEY `idx_run_status` (`run_status`)

**数据留存**：完整数据保留90天，之后转归档

---

### 5.3 复核记录表（review_record）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| review_id | VARCHAR | 64 | 是 | - | 复核记录ID | T/CFPA 052-2026 |
| task_id | VARCHAR | 64 | 是 | - | 关联任务ID（控制命令ID等） | T/CFPA 052-2026 |
| task_type | VARCHAR | 32 | 是 | - | 任务类型 | - |
| reviewer_id | VARCHAR | 32 | 是 | - | 复核员ID | T/CFPA 052-2026 |
| reviewer_name | VARCHAR | 64 | 是 | - | 复核员姓名 | GB 25201-2010 |
| reviewer_role | VARCHAR | 16 | 是 | - | 复核员角色 | T/GDCAA 016-2025 |
| review_action | VARCHAR | 16 | 是 | - | 复核动作（APPROVE/REJECT） | T/CFPA 052-2026 |
| review_comment | VARCHAR | 256 | 否 | - | 复核备注 | T/GDCAA 016-2025 |
| review_time | DATETIME | - | 是 | - | 复核时间 | T/CFPA 052-2026 |
| review_result | VARCHAR | 16 | 是 | - | 复核结果 | T/CFPA 052-2026 |
| reject_reason | VARCHAR | 256 | 否 | - | 拒绝原因 | T/GDCAA 016-2025 |
| is_deleted | TINYINT | - | 是 | 0 | 逻辑删除标记 | T/CFPA 052-2026 |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_task_id` (`task_id`)
- KEY `idx_reviewer_id` (`reviewer_id`)
- KEY `idx_review_time` (`review_time`)

---

## 6. 监测预警模块（F3）

### 6.1 监测点基础信息表（monitor_point）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| point_id | VARCHAR | 20 | 是 | - | 监测点编码（20位） | GB/T 26875.1-2026 附录B.2 |
| unit_code | VARCHAR | 6 | 是 | - | 联网单位编码（6位） | GB/T 26875.1-2026 B.2.1 |
| system_type | VARCHAR | 4 | 是 | - | 消防系统类型（4位） | GB/T 26875.1-2026 B.2.2 |
| device_type | VARCHAR | 4 | 是 | - | 设备类型编码（4位） | GB/T 26875.1-2026 B.2.3 |
| device_serial | VARCHAR | 6 | 是 | - | 设备序号（6位） | GB/T 26875.1-2026 B.2.4 |
| unit_id | BIGINT | - | 是 | - | 所属单位ID | - |
| building_id | VARCHAR | 32 | 是 | - | 所属建筑ID | - |
| location | VARCHAR | 200 | 是 | - | 安装位置描述 | - |
| floor | VARCHAR | 20 | 否 | - | 所属楼层 | - |
| status | TINYINT | - | 是 | 1 | 状态（1-正常/2-故障/3-停用） | - |
| online_threshold | INT | - | 是 | 95 | 在线率阈值（%） | T/GDCAA 016-2025 6.3.4 |
| is_active | TINYINT | - | 是 | 1 | 启用状态 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |
| update_time | DATETIME | - | 是 | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_point_id` (`point_id`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_system_type` (`system_type`)
- KEY `idx_device_type` (`device_type`)

---

### 6.2 实时状态数据表（device_realtime_status）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| point_id | VARCHAR | 20 | 是 | - | 监测点编码 | GB/T 26875.1-2026 附录B |
| unit_code | VARCHAR | 6 | 是 | - | 单位编码（冗余用于查询） | - |
| status_value | JSON | - | 是 | - | 状态值（JSON格式） | GB/T 26875.1-2026 B.3.1 |
| status_type | TINYINT | - | 是 | 1 | 状态类型（1-正常/2-告警/3-故障） | - |
| alarm_level | TINYINT | - | 否 | - | 告警级别（1-一般/2-严重/3-紧急） | GB 50440-2007 5.3 |
| collect_time | DATETIME | - | 是 | - | 采集时间 | GB/T 26875.1-2026 B.3.1 |
| receive_time | DATETIME | - | 是 | - | 接收时间 | GB/T 26875.1-2026 B.3.1 |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_point_id` (`point_id`)
- KEY `idx_unit_code` (`unit_code`)
- KEY `idx_collect_time` (`collect_time`)
- KEY `idx_status_type` (`status_type`)

**分区策略**：按`collect_time`按天分区，保留30天后转归档

---

### 6.3 设备在线率统计表（device_online_stats）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| stat_date | DATE | - | 是 | - | 统计日期 | - |
| unit_id | BIGINT | - | 是 | - | 单位ID | - |
| point_id | VARCHAR | 20 | 否 | - | 监测点编码（全辖统计时为空） | - |
| device_type | VARCHAR | 4 | 是 | - | 设备类型 | GB/T 26875.1-2026 B.2.3 |
| online_count | INT | - | 是 | 0 | 在线设备数 | - |
| total_count | INT | - | 是 | 0 | 设备总数 | - |
| online_rate | DECIMAL | 5,2 | 是 | 0 | 在线率（%） | T/GDCAA 016-2025 6.3.4 |
| fault_count | INT | - | 是 | 0 | 故障设备数 | - |
| fault_rate | DECIMAL | 5,2 | 是 | 0 | 故障率（%） | - |
| is_alarm | TINYINT | - | 是 | 0 | 是否告警（1-是/0-否） | GB 50440-2007 5.3.2 |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_stat_date` (`stat_date`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_point_id` (`point_id`)
- KEY `idx_is_alarm` (`is_alarm`)

**数据留存**：保留≥3年

---

### 6.4 AI预警分析表（ai_analysis_result）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| analysis_id | VARCHAR | 32 | 是 | - | 分析编号 | - |
| analysis_type | VARCHAR | 20 | 是 | - | 分析类型（channel_blocking/absent/risk_prediction） | T/CFPA 052-2026 5.3 |
| camera_id | VARCHAR | 50 | 是 | - | 摄像头ID | - |
| unit_id | BIGINT | - | 是 | - | 单位ID | - |
| building_id | VARCHAR | 32 | 否 | - | 建筑ID | - |
| location_desc | VARCHAR | 200 | 否 | - | 位置描述 | - |
| analysis_result | JSON | - | 是 | - | 分析结果（JSON，含置信度） | T/CFPA 052-2026 5.3 |
| detect_objects | JSON | - | 否 | - | 检测物体列表（JSON） | - |
| occupancy_rate | DECIMAL | 5,2 | 否 | - | 占用率（通道堵塞场景，%） | T/GDCAA 016-2025 6.3.5 |
| alarm_threshold | DECIMAL | 5,2 | 否 | - | 触发阈值 | - |
| screenshot_url | VARCHAR | 500 | 否 | - | 告警截图URL | T/GDCAA 016-2025 8.2.3 |
| video_clip_url | VARCHAR | 500 | 否 | - | 关联录像片段URL | - |
| model_version | VARCHAR | 32 | 否 | - | AI模型版本 | T/CFPA 052-2026 5.3 |
| confidence | DECIMAL | 5,4 | 否 | - | 置信度 | - |
| analyze_time | DATETIME | - | 是 | - | 分析时间 | - |
| is_valid | TINYINT | - | 是 | 1 | 是否有效（1-有效/0-误报） | - |
| is_deleted | TINYINT | - | 是 | 0 | 逻辑删除标记 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_analysis_id` (`analysis_id`)
- KEY `idx_camera_id` (`camera_id`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_analyze_time` (`analyze_time`)
- KEY `idx_analysis_type` (`analysis_type`)

---

## 7. 运维管理模块（F4）

### 7.1 设备资产表（device_asset）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| device_id | VARCHAR | 32 | 是 | - | 设备唯一编码 | T/GDCAA 016-2025 5.2.1 |
| device_name | VARCHAR | 100 | 是 | - | 设备名称 | - |
| device_type | TINYINT | - | 是 | - | 设备种类（1-15类） | T/GDCAA 016-2025 附录A |
| device_model | VARCHAR | 50 | 否 | - | 设备型号 | - |
| manufacturer | VARCHAR | 100 | 否 | - | 生产厂家 | - |
| serial_number | VARCHAR | 64 | 否 | - | 序列号 | - |
| address_code | VARCHAR | 20 | 是 | - | 地址编码 | GB/T 26875.1-2026 6.3 |
| install_location | VARCHAR | 200 | 是 | - | 安装位置 | GB 25201-2010 6.2 |
| building_id | VARCHAR | 32 | 是 | - | 所属建筑 | - |
| floor | VARCHAR | 20 | 否 | - | 所属楼层 | - |
| area_id | VARCHAR | 32 | 否 | - | 所属区域 | - |
| install_date | DATE | - | 是 | - | 安装日期 | - |
| activate_date | DATE | - | 是 | - | 激活日期 | - |
| warranty_expire | DATE | - | 是 | - | 质保到期 | - |
| service_life | INT | - | 是 | 0 | 设计使用年限 | - |
| lifecycle_status | TINYINT | - | 是 | 1 | 生命周期状态（1-入库/2-安装/3-激活/4-运行/5-维保/6-故障/7-报废） | GB 25201-2010 7.1 |
| is_active | TINYINT | - | 是 | 1 | 启用状态 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |
| update_time | DATETIME | - | 是 | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_device_id` (`device_id`)
- KEY `idx_device_type` (`device_type`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_lifecycle_status` (`lifecycle_status`)
- KEY `idx_warranty_expire` (`warranty_expire`)

---

### 7.2 备品备件表（spare_part）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| spare_id | VARCHAR | 32 | 是 | - | 备件编码 | - |
| spare_name | VARCHAR | 100 | 是 | - | 备件名称 | - |
| device_type | TINYINT | - | 是 | - | 适配设备类型 | T/GDCAA 016-2025 附录A |
| quantity | INT | - | 是 | 0 | 当前库存数量 | T/GDCAA 016-2025 5.4.3 |
| min_quantity | INT | - | 是 | 0 | 最低库存预警值 | T/GDCAA 016-2025 5.4.3 |
| unit | VARCHAR | 20 | 是 | - | 计量单位 | - |
| location | VARCHAR | 100 | 否 | - | 存放位置 | - |
| unit_price | DECIMAL | 10,2 | 否 | - | 单价 | - |
| supplier | VARCHAR | 100 | 否 | - | 供应商 | - |
| expire_date | DATE | - | 否 | - | 有效期 | - |
| is_active | TINYINT | - | 是 | 1 | 启用状态 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |
| update_time | DATETIME | - | 是 | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_spare_id` (`spare_id`)
- KEY `idx_device_type` (`device_type`)

---

### 7.3 维保工单表（work_order）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| work_order_id | VARCHAR | 32 | 是 | - | 工单编号 | T/GDCAA 016-2025 6.1.1 |
| work_order_type | TINYINT | - | 是 | - | 工单类型（1-故障/2-定期） | - |
| trigger_type | TINYINT | - | 是 | - | 触发类型（1-自动/2-手动） | - |
| source_id | VARCHAR | 32 | 否 | - | 触发源ID（如告警ID） | - |
| priority | TINYINT | - | 是 | 2 | 优先级（1-P1/2-P2/3-P3） | - |
| building_id | VARCHAR | 32 | 是 | - | 所属建筑 | - |
| device_id | VARCHAR | 32 | 是 | - | 关联设备 | - |
| description | TEXT | - | 是 | - | 问题描述 | - |
| photos | JSON | - | 否 | - | 问题照片 | - |
| assign_unit | VARCHAR | 32 | 是 | - | 派发单位 | - |
| assign_time | DATETIME | - | 是 | - | 派发时间 | - |
| due_time | DATETIME | - | 是 | - | 要求完成时间 | - |
| status | TINYINT | - | 是 | 1 | 工单状态（1-待派发/2-已派发/3-已接收/4-处理中/5-已完成/6-已验收/7-已关闭/8-已驳回） | - |
| create_by | VARCHAR | 32 | 是 | - | 创建人 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |
| update_time | DATETIME | - | 是 | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_work_order_id` (`work_order_id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_device_id` (`device_id`)
- KEY `idx_status` (`status`)
- KEY `idx_assign_time` (`assign_time`)
- KEY `idx_due_time` (`due_time`)

---

### 7.4 维保记录表（maintenance_record）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| record_id | VARCHAR | 32 | 是 | - | 记录编号 | - |
| work_order_id | VARCHAR | 32 | 是 | - | 关联工单 | T/GDCAA 016-2025 6.2 |
| engineer_id | VARCHAR | 32 | 是 | - | 维保人员 | T/GDCAA 016-2025 6.2 |
| engineer_name | VARCHAR | 64 | 是 | - | 维保人员姓名 | - |
| arrive_time | DATETIME | - | 是 | - | 到达时间 | T/GDCAA 016-2025 6.2.1 |
| arrive_location | JSON | - | yes | - | 到达位置（GPS） | T/GDCAA 016-2025 6.2.1 |
| operation_type | TINYINT | - | 是 | - | 操作类型（1-检修/2-更换/3-调试/4-清洁） | - |
| operation_desc | TEXT | - | 是 | - | 操作描述 | - |
| photos | JSON | - | yes | - | 现场照片（≥2张） | T/GDCAA 016-2025 6.2.2 |
| videos | JSON | - | 否 | - | 现场视频 | - |
| replace_parts | JSON | - | 否 | - | 更换配件清单 | - |
| complete_time | DATETIME | - | yes | - | 完成时间 | - |
| signature_url | VARCHAR | 256 | - | - | 业主签字URL | GB 25201-2010 8.4 |
| is_deleted | TINYINT | - | 是 | 0 | 逻辑删除标记 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_work_order_id` (`work_order_id`)
- KEY `idx_engineer_id` (`engineer_id`)
- KEY `idx_arrive_time` (`arrive_time`)

---

### 7.5 巡检路线表（inspection_route）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| route_id | VARCHAR | 32 | 是 | - | 路线编号 | T/GDCAA 016-2025 7.1.1 |
| route_name | VARCHAR | 100 | 是 | - | 路线名称 | - |
| building_id | VARCHAR | 32 | yes | - | 所属建筑 | - |
| floor_range | VARCHAR | 50 | 否 | - | 涉及楼层范围 | - |
| checkpoints | JSON | - | yes | - | 巡检点位列表 | - |
| estimated_duration | INT | - | yes | - | 预计巡检时长（分钟） | - |
| checkpoint_time_limit | INT | - | yes | - | 单点巡检时限（分钟） | - |
| frequency | TINYINT | - | yes | - | 巡检频次（1-日/2-周/3-月） | GB 25201-2010 6.1 |
| executors | JSON | - | yes | - | 执行人员列表 | - |
| status | TINYINT | - | yes | 1 | 路线状态（1-启用/0-停用） | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |
| update_time | DATETIME | - | yes | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_route_id` (`route_id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_frequency` (`frequency`)

---

### 7.6 巡检任务表（inspection_task）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| task_id | VARCHAR | 32 | 是 | - | 任务ID | - |
| route_id | VARCHAR | 32 | 是 | - | 巡检路线ID | T/GDCAA 016-2025 7.2 |
| building_id | VARCHAR | 32 | 是 | - | 建筑ID | - |
| plan_date | DATE | - | 是 | - | 计划日期 | - |
| plan_start_time | DATETIME | - | 是 | - | 计划开始时间 | - |
| plan_end_time | DATETIME | - | 是 | - | 计划结束时间 | - |
| actual_start_time | DATETIME | - | 否 | - | 实际开始时间 | - |
| actual_end_time | DATETIME | - | 否 | - | 实际结束时间 | - |
| assignee_id | VARCHAR | 32 | 是 | - | 执行人ID | T/GDCAA 016-2025 7.2 |
| assignee_name | VARCHAR | 64 | 是 | - | 执行人姓名 | - |
| checkpoint_count | INT | - | 是 | 0 | 应检点数 | - |
| checked_count | INT | - | 是 | 0 | 实检点数 | - |
| skipped_count | INT | - | 是 | 0 | 漏检点数 | - |
| normal_count | INT | - | 是 | 0 | 正常数 | - |
| abnormal_count | INT | - | 是 | 0 | 异常数 | - |
| status | TINYINT | - | 是 | 1 | 任务状态（1-待执行/2-执行中/3-已完成/4-已取消） | - |
| is_deleted | TINYINT | - | 是 | 0 | 逻辑删除标记 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_task_id` (`task_id`)
- KEY `idx_route_id` (`route_id`)
- KEY `idx_plan_date` (`plan_date`)
- KEY `idx_assignee_id` (`assignee_id`)
- KEY `idx_status` (`status`)

---

### 7.7 值班排班表（duty_schedule）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| schedule_id | VARCHAR | 32 | 是 | - | 排班编号 | GB 25506-2010 5.1 |
| unit_id | BIGINT | - | 是 | - | 单位ID | - |
| room_id | VARCHAR | 32 | 是 | - | 消控室ID | - |
| duty_person_id | VARCHAR | 32 | 是 | - | 值班员ID | GB 25506-2010 5.1 |
| duty_person_name | VARCHAR | 50 | 是 | - | 值班员姓名 | - |
| cert_number | VARCHAR | 50 | 是 | - | 证书编号 | GB 25506-2010 5.1.2 |
| cert_level | VARCHAR | 20 | 是 | - | 证书等级 | - |
| schedule_date | DATE | - | 是 | - | 值班日期 | - |
| shift_type | VARCHAR | 20 | 是 | - | 班次类型（早班/中班/夜班） | GB 25506-2010 5.1.1 |
| shift_start_time | TIME | - | 是 | - | 上班时间 | - |
| shift_end_time | TIME | - | 是 | - | 下班时间 | - |
| is_on_duty | TINYINT | - | 是 | 1 | 是否值班（1-是/0-否） | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_schedule_date` (`schedule_date`)
- KEY `idx_duty_person_id` (`duty_person_id`)
- KEY `idx_unit_id` (`unit_id`)

---

### 7.8 交接班记录表（shift_handoff）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| handoff_id | VARCHAR | 32 | 是 | - | 交接编号 | GB 25506-2010 5.2 |
| unit_id | BIGINT | - | 是 | - | 单位ID | - |
| room_id | VARCHAR | 32 | 是 | - | 消控室ID | - |
| shift_out_id | VARCHAR | 32 | 是 | - | 交班人员ID | - |
| shift_out_name | VARCHAR | 50 | 是 | - | 交班人员姓名 | - |
| shift_in_id | VARCHAR | 32 | 是 | - | 接班人员ID | - |
| shift_in_name | VARCHAR | 50 | 是 | - | 接班人员姓名 | - |
| handoff_time | DATETIME | - | 是 | - | 交接时间 | GB 25506-2010 5.2.3 |
| pending_issues | JSON | - | yes | - | 遗留问题清单 | GB 25506-2010 5.2.1 |
| system_status | JSON | - | yes | - | 系统状态核对 | GB 25506-2010 5.2.2 |
| device_status_summary | JSON | - | yes | - | 设备状态汇总 | - |
| today_alerts_count | INT | - | yes | 0 | 当日告警数量 | - |
| today_fires_count | INT | - | yes | 0 | 当日火灾数量 | - |
| unread_messages | JSON | - | 否 | - | 待办消息 | - |
| shift_out_signature_url | VARCHAR | 256 | yes | - | 交班签字URL | GB 25506-2010 5.2.3 |
| shift_in_signature_url | VARCHAR | 256 | yes | - | 接班签字URL | GB 25506-2010 5.2.3 |
| supervisor_signature_url | VARCHAR | 256 | 否 | - | 监督签字URL | - |
| notes | TEXT | - | 否 | - | 备注 | - |
| is_deleted | TINYINT | - | 是 | 0 | 逻辑删除标记 | - |
| create_time | DATETIME | - | 是 | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_handoff_time` (`handoff_time`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_shift_out_id` (`shift_out_id`)

**数据留存**：保留≥3年

---

### 7.9 设备生命周期变更表（device_lifecycle_log）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| log_id | VARCHAR | 32 | 是 | - | 记录ID | - |
| device_id | VARCHAR | 32 | 是 | - | 设备编码 | GB 25201-2010 7.1 |
| from_status | TINYINT | - | 是 | - | 原状态 | GB 25201-2010 7.1 |
| to_status | TINYINT | - | 是 | - | 新状态 | GB 25201-2010 7.1 |
| change_reason | TEXT | - | yes | - | 变更原因 | GB 25201-2010 7.1 |
| operator_id | VARCHAR | 32 | yes | - | 操作人 | - |
| operator_name | VARCHAR | 64 | yes | - | 操作人姓名 | - |
| approval_id | VARCHAR | 32 | 否 | - | 审批ID（报废必填） | GB 25201-2010 7.3 |
| change_time | DATETIME | - | yes | - | 变更时间 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_device_id` (`device_id`)
- KEY `idx_change_time` (`change_time`)

---

## 8. 数据与报表模块（F5）

### 8.1 值班记录台账表（duty_record_archive）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| record_id | VARCHAR | 32 | 是 | - | 记录编号 | GB 25506-2010 5.2 |
| building_id | VARCHAR | 32 | 是 | - | 建筑编号 | - |
| record_date | DATE | - | 是 | - | 值班日期 | GB 25506-2010 5.3 |
| shift_type | VARCHAR | 20 | 是 | - | 班次（早/中/夜） | - |
| shift_leader | VARCHAR | 32 | 是 | - | 值班负责人ID | - |
| shift_leader_name | VARCHAR | 64 | 是 | - | 值班负责人姓名 | - |
| shift_members | JSON | - | yes | - | 值班人员列表 | - |
| duty_start_time | DATETIME | - | yes | - | 上班时间 | - |
| duty_end_time | DATETIME | - | yes | - | 下班时间 | - |
| handoff_records | JSON | - | yes | - | 交接班记录列表 | GB 25506-2010 5.2 |
| device_status_summary | JSON | - | yes | - | 设备状态汇总 | - |
| daily_alerts_count | INT | - | yes | 0 | 当日告警数 | - |
| daily_fires_count | INT | - | yes | 0 | 当日火灾数 | - |
| abnormal_events | JSON | - | 否 | - | 异常事件记录 | - |
| notes | TEXT | - | 否 | - | 备注 | - |
| sign_by | VARCHAR | 32 | yes | - | 签字确认人 | - |
| sign_time | DATETIME | - | yes | - | 签字时间 | - |
| attachment_urls | JSON | - | 否 | - | 附件URL列表 | - |
| archive_status | TINYINT | - | yes | 0 | 归档状态（0-未归档/1-已归档） | T/CFPA 052-2026 7.2 |
| archive_time | DATETIME | - | 否 | - | 归档时间 | - |
| hash_value | VARCHAR | 64 | yes | - | 归档哈希值 | T/CFPA 052-2026 |
| is_deleted | TINYINT | - | yes | 0 | 逻辑删除标记（归档后不可删除） | T/CFPA 052-2026 7.2 |
| create_time | DATETIME | - | yes | - | 创建时间 | - |
| update_time | DATETIME | - | yes | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_record_id` (`record_id`)
- KEY `idx_record_date` (`record_date`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_archive_status` (`archive_status`)

---

### 8.2 火警处置记录台账表（fire_alert_archive）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| fire_record_id | VARCHAR | 32 | 是 | - | 记录编号 | GB/T 26875.1-2026 9.1 |
| alert_id | VARCHAR | 32 | 是 | - | 关联告警ID | - |
| building_id | VARCHAR | 32 | 是 | - | 建筑编号 | - |
| fire_location | VARCHAR | 200 | yes | - | 火灾位置 | - |
| alert_time | DATETIME | - | yes | - | 告警时间 | - |
| confirm_time | DATETIME | - | yes | - | 确认时间 | - |
| confirm_method | TINYINT | - | yes | - | 确认方式（1-视频/2-现场/3-复核） | - |
| fire_type | TINYINT | - | yes | - | 火警类型（1-真实/2-误报/3-测试） | T/CFPA 052-2026 7.3 |
| dispatch_time | DATETIME | - | 否 | - | 力量调度时间 | - |
| dispatch_unit | VARCHAR | 100 | 否 | - | 调度单位 | - |
| arrival_time | DATETIME | - | 否 | - | 到场时间 | - |
| fire_extinguish_time | DATETIME | - | 否 | - | 灭火完成时间 | - |
| evacuation_time | DATETIME | - | 否 | - | 疏散完成时间 | - |
| casualties | INT | - | 否 | 0 | 伤亡人数 | - |
| property_loss | DECIMAL | 15,2 | 否 | 0 | 财产损失（元） | - |
| cause_analysis | TEXT | - | 否 | - | 原因分析 | - |
| handling_process | TEXT | - | yes | - | 处置过程 | GB/T 26875.1-2026 9.2 |
| responsible_person | VARCHAR | 32 | yes | - | 处置责任人 | - |
| responsible_person_name | VARCHAR | 64 | yes | - | 处置责任人姓名 | - |
| supervisor | VARCHAR | 32 | 否 | - | 监督人 | - |
| attach_files | JSON | - | 否 | - | 附件（照片/视频/音频） | T/CFPA 052-2026 7.3.1 |
| archive_status | TINYINT | - | yes | 0 | 归档状态 | T/CFPA 052-2026 7.2 |
| archive_time | DATETIME | - | 否 | - | 归档时间 | - |
| hash_value | VARCHAR | 64 | yes | - | 归档哈希值 | T/CFPA 052-2026 |
| is_permanent | TINYINT | - | yes | 0 | 是否永久保存（真实火警=1） | T/CFPA 052-2026 7.3 |
| is_deleted | TINYINT | - | yes | 0 | 逻辑删除标记 | T/CFPA 052-2026 7.2 |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_fire_record_id` (`fire_record_id`)
- KEY `idx_alert_id` (`alert_id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_alert_time` (`alert_time`)
- KEY `idx_fire_type` (`fire_type`)
- KEY `idx_is_permanent` (`is_permanent`)

---

### 8.3 故障整改台账表（fault_record_archive）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| fault_record_id | VARCHAR | 32 | 是 | - | 记录编号 | T/GDCAA 016-2025 9.2 |
| device_id | VARCHAR | 32 | 是 | - | 设备编号 | - |
| building_id | VARCHAR | 32 | 是 | - | 建筑编号 | - |
| fault_type | TINYINT | - | yes | - | 故障类型 | - |
| fault_level | TINYINT | - | yes | - | 故障等级（1-P1/2-P2/3-P3） | - |
| fault_time | DATETIME | - | yes | - | 故障发生时间 | - |
| fault_description | TEXT | - | yes | - | 故障描述 | - |
| fault_photos | JSON | - | 否 | - | 故障照片 | - |
| discover_method | TINYINT | - | yes | - | 发现方式（1-自动/2-人工） | - |
| work_order_id | VARCHAR | 32 | yes | - | 关联工单编号 | T/GDCAA 016-2025 9.2.2 |
| repair_unit | VARCHAR | 100 | yes | - | 维修单位 | - |
| repair_start_time | DATETIME | - | yes | - | 维修开始时间 | - |
| repair_end_time | DATETIME | - | 否 | - | 维修结束时间 | - |
| repair_result | TINYINT | - | yes | - | 维修结果（1-已修复/2-未修复/3-需更换） | - |
| repair_parts | JSON | - | 否 | - | 更换配件 | - |
| repair_person | VARCHAR | 32 | yes | - | 维修人员 | - |
| acceptance_person | VARCHAR | 32 | yes | - | 验收人 | - |
| acceptance_time | DATETIME | - | yes | - | 验收时间 | - |
| acceptance_result | TINYINT | - | yes | - | 验收结果 | - |
| recurrence_prevention | TEXT | - | 否 | - | 防范措施 | - |
| archive_status | TINYINT | - | yes | 0 | 归档状态 | T/CFPA 052-2026 7.2 |
| archive_time | DATETIME | - | 否 | - | 归档时间 | - |
| hash_value | VARCHAR | 64 | yes | - | 归档哈希值 | T/CFPA 052-2026 |
| is_deleted | TINYINT | - | yes | 0 | 逻辑删除标记 | T/CFPA 052-2026 7.2 |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_fault_record_id` (`fault_record_id`)
- KEY `idx_device_id` (`device_id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_fault_time` (`fault_time`)
- KEY `idx_work_order_id` (`work_order_id`)

---

### 8.4 数据签名表（data_signature）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| signature_id | VARCHAR | 64 | yes | - | 签名ID | T/CFPA 052-2026 8.3 |
| data_type | VARCHAR | 32 | yes | - | 数据类型（alarm/control/log/archive） | T/CFPA 052-2026 8.3.1 |
| data_id | VARCHAR | 64 | yes | - | 原始数据ID | - |
| data_hash | VARCHAR | 64 | yes | - | 数据内容哈希（SHA-256） | T/CFPA 052-2026 8.3.1 |
| signature_method | VARCHAR | 32 | yes | - | 签名方法（MD5/SHA-256/国密SM3） | T/CFPA 052-2026 8.3.1 |
| signature_value | VARCHAR | 256 | yes | - | 签名值 | T/CFPA 052-2026 8.3.1 |
| sign_time | DATETIME | - | yes | - | 签名时间 | - |
| sign_by | VARCHAR | 32 | yes | - | 签名人 | - |
| valid_status | TINYINT | - | yes | 1 | 有效状态（1-有效/0-已失效） | - |
| chain_index | BIGINT | - | yes | - | 区块链链上索引（如有） | T/CFPA 052-2026 8.3.2 |
| chain_tx_hash | VARCHAR | 128 | 否 | - | 区块链交易哈希 | T/CFPA 052-2026 8.3.2 |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_data_id` (`data_id`)
- KEY `idx_data_type` (`data_type`)
- KEY `idx_sign_time` (`sign_time`)
- KEY `idx_chain_tx_hash` (`chain_tx_hash`)

---

### 8.5 备份记录表（backup_record）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| backup_id | VARCHAR | 64 | yes | - | 备份记录ID | T/CFPA 052-2026 9.1 |
| backup_type | TINYINT | - | yes | - | 备份类型（1-全量/2-增量/3-实时） | T/CFPA 052-2026 9.1 |
| data_types | JSON | - | yes | - | 备份数据类型列表 | T/CFPA 052-2026 9.1 |
| backup_start_time | DATETIME | - | yes | - | 备份开始时间 | - |
| backup_end_time | DATETIME | - | 否 | - | 备份结束时间 | - |
| backup_status | TINYINT | - | yes | - | 备份状态（1-进行中/2-成功/3-失败） | - |
| backup_size | BIGINT | - | 否 | - | 备份大小（字节） | - |
| backup_path | VARCHAR | 500 | 否 | - | 备份存储路径 | T/CFPA 052-2026 9.2 |
| storage_location | VARCHAR | 100 | yes | - | 存储位置（本地/异地/云端） | T/CFPA 052-2026 9.2 |
| retention_days | INT | - | yes | - | 保留天数 | T/CFPA 052-2026 9.1 |
| verify_status | TINYINT | - | yes | - | 校验状态（1-待校验/2-通过/3-失败） | T/GDCAA 016-2025 9.8.3 |
| verify_time | DATETIME | - | 否 | - | 校验时间 | - |
| operator_id | VARCHAR | 32 | yes | - | 操作人 | - |
| operator_name | VARCHAR | 64 | yes | - | 操作人姓名 | - |
| error_message | TEXT | - | 否 | - | 错误信息 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_backup_start_time` (`backup_start_time`)
- KEY `idx_backup_type` (`backup_type`)
- KEY `idx_backup_status` (`backup_status`)

---

## 9. 视频联动模块（F6）

### 9.1 视频设备信息表（video_device）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| device_id | VARCHAR | 20 | 是 | - | 设备编码（20位） | GA/T 1400 B.2 |
| device_name | VARCHAR | 100 | yes | - | 设备名称 | - |
| device_type | TINYINT | - | yes | - | 类型（1-枪机/2-球机/3-半球/4-NVR/5-视频服务器） | - |
| manufacturer | VARCHAR | 50 | 否 | - | 厂商 | - |
| model | VARCHAR | 50 | 否 | - | 型号 | - |
| ip_address | VARCHAR | 50 | yes | - | IP地址 | - |
| port | INT | - | yes | - | 端口 | - |
| username | VARCHAR | 50 | yes | - | 登录用户名（加密存储） | - |
| password | VARCHAR | 200 | yes | - | 登录密码（加密存储） | - |
| channel_no | INT | - | yes | - | 通道号 | - |
| rtsp_url | VARCHAR | 500 | yes | - | RTSP地址 | - |
| unit_id | BIGINT | - | yes | - | 所属单位ID | - |
| building_id | VARCHAR | 32 | yes | - | 所属建筑ID | - |
| location | VARCHAR | 200 | yes | - | 安装位置 | - |
| longitude | DECIMAL | 10,6 | 否 | - | 经度 | - |
| latitude | DECIMAL | 10,6 | 否 | - | 纬度 | - |
| video_type | TINYINT | - | yes | - | 视频类型（1-消控室/2-消防通道/3-安全出口/4-重点区域/5-安防共享） | T/GDCAA 016-2025 7.1.1 |
| protocol | VARCHAR | 20 | yes | - | 接入协议（GB28181/RTSP/ONVIF） | T/GDCAA 016-2025 7.1.2 |
| resolution | VARCHAR | 20 | yes | - | 分辨率 | - |
| is_online | TINYINT | - | yes | 0 | 在线状态（1-在线/0-离线） | - |
| last_heartbeat | DATETIME | - | yes | - | 最后心跳时间 | - |
| is_active | TINYINT | - | yes | 1 | 启用状态 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |
| update_time | DATETIME | - | yes | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_device_id` (`device_id`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_video_type` (`video_type`)
- KEY `idx_is_online` (`is_online`)

---

### 9.2 视频通道关联表（video_linkage）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| link_id | BIGINT | - | yes | - | 联动规则ID | - |
| alarm_type | VARCHAR | 20 | yes | - | 告警类型 | GB/T 26875.1-2026 表A.1 |
| unit_id | BIGINT | - | 否 | - | 单位ID（NULL表示全辖通用） | - |
| building_code | VARCHAR | 20 | 否 | - | 建筑编码 | - |
| floor_code | VARCHAR | 10 | 否 | - | 楼层编码 | - |
| camera_ids | JSON | - | yes | - | 关联摄像头ID列表 | T/CFPA 052-2026 5.4.2 |
| camera_names | JSON | - | yes | - | 摄像头名称列表 | - |
| priority | INT | - | yes | 1 | 关联优先级（1-最高） | - |
| link_count | INT | - | yes | 3 | 关联数量（默认3-5） | T/CFPA 052-2026 5.4.2 |
| enable_ai | TINYINT | - | yes | 1 | 是否启用AI分析（1-是/0-否） | - |
| ai_types | VARCHAR | 100 | 否 | - | AI分析类型（火焰/烟雾/人形等） | T/CFPA 052-2026 5.3 |
| is_active | TINYINT | - | yes | 1 | 是否启用 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |
| update_time | DATETIME | - | yes | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_alarm_type` (`alarm_type`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_building_code` (`building_code`)

---

### 9.3 视频录像记录表（video_record）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| record_id | VARCHAR | 32 | 是 | - | 录像记录ID | - |
| device_id | VARCHAR | 20 | yes | - | 设备编码 | - |
| camera_id | VARCHAR | 20 | yes | - | 摄像头编码 | - |
| record_type | TINYINT | - | yes | - | 录像类型（1-连续/2-告警触发/3-手动） | - |
| start_time | DATETIME | - | yes | - | 开始时间 | T/GDCAA 016-2025 8.2.2 |
| end_time | DATETIME | - | yes | - | 结束时间 | - |
| duration | INT | - | yes | 0 | 时长（秒） | - |
| file_path | VARCHAR | 500 | yes | - | 存储路径 | - |
| file_size | BIGINT | - | yes | 0 | 文件大小（字节） | - |
| storage_level | TINYINT | - | yes | 1 | 存储层级（1-热/2-温/3-冷） | - |
| retention_days | INT | - | yes | 30 | 留存天数 | T/GDCAA 016-2025 8.2.2 |
| expire_time | DATETIME | - | yes | - | 过期时间 | - |
| is_locked | TINYINT | - | yes | 0 | 是否锁定（1-锁定/0-未锁定） | T/GDCAA 016-2025 8.2.2 |
| lock_reason | VARCHAR | 200 | 否 | - | 锁定原因 | - |
| lock_expire_time | DATETIME | - | 否 | - | 锁定过期时间 | - |
| hash_value | VARCHAR | 64 | yes | - | 文件哈希值（完整性校验） | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_device_id` (`device_id`)
- KEY `idx_camera_id` (`camera_id`)
- KEY `idx_start_time` (`start_time`)
- KEY `idx_end_time` (`end_time`)
- KEY `idx_record_type` (`record_type`)
- KEY `idx_is_locked` (`is_locked`)

**分区策略**：按`start_time`按月分区

---

### 9.4 告警联动记录表（alarm_video_linkage_record）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| linkage_record_id | VARCHAR | 32 | yes | - | 联动记录ID | - |
| alarm_id | VARCHAR | 32 | yes | - | 告警ID | T/CFPA 052-2026 5.4.2 |
| alarm_type | VARCHAR | 20 | yes | - | 告警类型 | - |
| alarm_level | TINYINT | - | yes | - | 告警级别 | - |
| camera_id | VARCHAR | 20 | yes | - | 关联摄像头ID | T/CFPA 052-2026 5.4.2 |
| camera_name | VARCHAR | 100 | yes | - | 摄像头名称 | - |
| pop_time | DATETIME | - | yes | - | 弹出时间 | T/CFPA 052-2026 5.4.2 |
| ai_result | JSON | - | 否 | - | AI分析结果（JSON） | T/CFPA 052-2026 5.3 |
| ai_confidence | DECIMAL | 5,4 | 否 | - | AI置信度 | - |
| screenshot_url | VARCHAR | 500 | 否 | - | 截图URL | T/GDCAA 016-2025 8.2.3 |
| verify_status | TINYINT | - | yes | 0 | 复核状态（0-待复核/1-已复核） | T/CFPA 052-2026 5.4.4 |
| verify_result | VARCHAR | 20 | 否 | - | 复核结论 | T/CFPA 052-2026 5.4.4 |
| verify_user | VARCHAR | 50 | 否 | - | 复核人 | T/CFPA 052-2026 5.4.4 |
| verify_time | DATETIME | - | 否 | - | 复核时间 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_alarm_id` (`alarm_id`)
- KEY `idx_camera_id` (`camera_id`)
- KEY `idx_pop_time` (`pop_time`)
- KEY `idx_verify_status` (`verify_status`)

---

## 10. 查岗管理模块（F7）

### 10.1 查岗配置表（roll_call_config）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| config_id | BIGINT | - | yes | - | 配置ID | - |
| unit_id | BIGINT | - | 否 | - | 单位ID（NULL表示全局配置） | - |
| roll_call_type | TINYINT | - | yes | - | 查岗类型（1-手动/2-定时/3-AI） | T/GDCAA 016-2025 6.4.1 |
| is_enabled | TINYINT | - | yes | 1 | 是否启用（1-是/0-否） | - |
| check_times | VARCHAR | 200 | 否 | - | 定时查岗时段（JSON数组） | T/GDCAA 016-2025 6.4.2 |
| timeout_seconds | INT | - | yes | 300 | 超时时间（秒），默认300（5分钟） | T/GDCAA 016-2025 6.4.3 |
| escalation_config | JSON | - | yes | - | 升级配置（JSON） | T/GDCAA 016-2025 6.4.4 |
| ai_enabled | TINYINT | - | yes | 0 | AI查岗是否启用 | T/GDCAA 016-2025 6.4.5 |
| ai_threshold_minutes | INT | - | yes | 3 | AI离岗阈值（分钟） | T/GDCAA 016-2025 6.4.5 |
| ai_whitelist_enabled | TINYINT | - | yes | 1 | AI白名单是否启用（如厕等） | T/GDCAA 016-2025 6.4.5 |
| create_user | VARCHAR | 50 | yes | - | 创建人 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |
| update_time | DATETIME | - | yes | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_roll_call_type` (`roll_call_type`)
- KEY `idx_is_enabled` (`is_enabled`)

---

### 10.2 查岗任务表（roll_call_task）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| task_id | VARCHAR | 32 | yes | - | 查岗任务ID | - |
| task_type | TINYINT | - | yes | - | 任务类型（1-手动/2-定时/3-AI） | T/GDCAA 016-2025 6.4.1 |
| unit_id | BIGINT | - | yes | - | 单位ID | - |
| unit_name | VARCHAR | 200 | yes | - | 单位名称 | - |
| room_id | VARCHAR | 32 | yes | - | 消控室ID | - |
| room_name | VARCHAR | 100 | yes | - | 消控室名称 | - |
| duty_person_id | VARCHAR | 32 | yes | - | 值班员ID | - |
| duty_person_name | VARCHAR | 50 | yes | - | 值班员姓名 | - |
| check_user_id | VARCHAR | 50 | yes | - | 查岗发起人ID | - |
| check_user_name | VARCHAR | 50 | yes | - | 查岗发起人姓名 | - |
| check_content | VARCHAR | 500 | 否 | - | 查岗说明 | - |
| check_time | DATETIME | - | yes | - | 查岗发起时间 | T/GDCAA 016-2025 8.2.4 |
| deadline_time | DATETIME | - | yes | - | 应答截止时间 | T/GDCAA 016-2025 6.4.3 |
| response_status | TINYINT | - | yes | 1 | 响应状态（1-待响应/2-有效响应/3-超时/4-脱岗） | T/GDCAA 016-2025 6.4.3 |
| response_time | DATETIME | - | 否 | - | 响应时间 | - |
| response_type | VARCHAR | 20 | 否 | - | 响应方式（screen/app/face/sms） | T/GDCAA 016-2025 6.4.2 |
| escalation_count | INT | - | yes | 0 | 升级次数 | T/GDCAA 016-2025 6.4.4 |
| escalation_records | JSON | - | 否 | - | 升级记录（JSON数组） | T/GDCAA 016-2025 6.4.4 |
| task_status | TINYINT | - | yes | 1 | 任务状态（1-进行中/2-已完成/3-已取消） | - |
| complete_time | DATETIME | - | 否 | - | 完成时间 | - |
| remark | TEXT | - | 否 | - | 备注 | - |
| is_deleted | TINYINT | - | yes | 0 | 逻辑删除标记 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_task_id` (`task_id`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_duty_person_id` (`duty_person_id`)
- KEY `idx_check_time` (`check_time`)
- KEY `idx_response_status` (`response_status`)
- KEY `idx_task_status` (`task_status`)

**数据留存**：保留≥5年

---

### 10.3 脱岗记录表（absent_record）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| absent_id | VARCHAR | 32 | yes | - | 脱岗记录ID | - |
| task_id | VARCHAR | 32 | 否 | - | 关联查岗任务ID | - |
| absent_type | TINYINT | - | yes | - | 脱岗类型（1-临时离岗/2-睡岗/3-脱岗/4-无人值班） | T/GDCAA 016-2025 6.4.5 |
| unit_id | BIGINT | - | yes | - | 单位ID | - |
| unit_name | VARCHAR | 200 | yes | - | 单位名称 | - |
| room_id | VARCHAR | 32 | yes | - | 消控室ID | - |
| duty_person_id | VARCHAR | 32 | yes | - | 值班员ID | - |
| duty_person_name | VARCHAR | 50 | yes | - | 值班员姓名 | - |
| camera_id | VARCHAR | 20 | yes | - | 监控摄像头ID | T/GDCAA 016-2025 6.4.5 |
| absent_start_time | DATETIME | - | yes | - | 脱岗开始时间 | T/GDCAA 016-2025 8.2.4 |
| absent_end_time | DATETIME | - | 否 | - | 脱岗结束时间 | - |
| absent_duration | INT | - | yes | 0 | 脱岗时长（秒） | - |
| ai_detect_time | DATETIME | - | yes | - | AI检测时间 | T/GDCAA 016-2025 6.4.5 |
| screenshot_urls | JSON | - | yes | - | 脱岗截图（JSON数组） | T/GDCAA 016-2025 8.2.3 |
| video_clip_urls | JSON | - | 否 | - | 关联录像片段（JSON） | T/GDCAA 016-2025 8.2.3 |
| confirm_status | TINYINT | - | yes | 1 | 确认状态（1-待确认/2-已确认/3-误报） | - |
| confirm_user | VARCHAR | 50 | 否 | - | 确认人 | - |
| confirm_time | DATETIME | - | 否 | - | 确认时间 | - |
| confirm_remark | VARCHAR | 500 | 否 | - | 确认备注 | - |
| rectify_status | TINYINT | - | yes | 1 | 整改状态（1-待整改/2-整改中/3-已整改） | T/GDCAA 016-2025 6.4.6 |
| rectify_deadline | DATETIME | - | yes | - | 整改截止时间 | - |
| rectify_complete_time | DATETIME | - | 否 | - | 整改完成时间 | - |
| rectify_evidence_urls | JSON | - | 否 | - | 整改证明材料（JSON） | T/GDCAA 016-2025 6.4.6 |
| is_closed | TINYINT | - | yes | 0 | 是否闭环（1-是/0-否） | - |
| is_deleted | TINYINT | - | yes | 0 | 逻辑删除标记 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_absent_id` (`absent_id`)
- KEY `idx_task_id` (`task_id`)
- KEY `idx_unit_id` (`unit_id`)
- KEY `idx_duty_person_id` (`duty_person_id`)
- KEY `idx_absent_start_time` (`absent_start_time`)
- KEY `idx_rectify_status` (`rectify_status`)
- KEY `idx_is_closed` (`is_closed`)

**数据留存**：保留≥5年

---

### 10.4 升级通知记录表（escalation_record）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| escalation_id | VARCHAR | 32 | yes | - | 升级记录ID | - |
| task_id | VARCHAR | 32 | yes | - | 关联查岗任务ID | - |
| escalation_level | INT | - | yes | - | 升级级别（1/2/3） | T/GDCAA 016-2025 6.4.4 |
| escalate_to_role | VARCHAR | 50 | yes | - | 升级对象角色 | T/GDCAA 016-2025 6.4.4 |
| escalate_to_user_id | BIGINT | - | yes | - | 升级对象用户ID | - |
| escalate_to_user_name | VARCHAR | 50 | yes | - | 升级对象姓名 | - |
| escalate_to_phone | VARCHAR | 20 | yes | - | 升级对象电话 | - |
| notify_type | VARCHAR | 20 | yes | - | 通知方式（app/sms/call） | - |
| notify_content | VARCHAR | 500 | yes | - | 通知内容 | - |
| notify_status | TINYINT | - | yes | - | 通知状态（1-已发送/2-已送达/3-失败） | - |
| notify_time | DATETIME | - | yes | - | 通知时间 | - |
| read_status | TINYINT | - | yes | 1 | 阅读状态（1-未读/2-已读） | - |
| read_time | DATETIME | - | 否 | - | 阅读时间 | - |
| response_status | TINYINT | - | yes | 1 | 响应状态（1-待响应/2-已响应） | - |
| response_content | VARCHAR | 500 | 否 | - | 响应内容 | - |
| response_time | DATETIME | - | 否 | - | 响应时间 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_task_id` (`task_id`)
- KEY `idx_escalate_to_user_id` (`escalate_to_user_id`)
- KEY `idx_notify_time` (`notify_time`)

---

### 10.5 查岗统计表（roll_call_statistics）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| stat_id | BIGINT | - | yes | - | 统计ID | - |
| stat_date | DATE | - | yes | - | 统计日期 | T/GDCAA 016-2025 6.4.7 |
| unit_id | BIGINT | - | 否 | - | 单位ID（汇总统计时为NULL） | - |
| unit_name | VARCHAR | 200 | 否 | - | 单位名称 | - |
| total_checks | INT | - | yes | 0 | 查岗总次数 | T/GDCAA 016-2025 6.4.7 |
| effective_responses | INT | - | yes | 0 | 有效响应次数 | T/GDCAA 016-2025 6.4.7 |
| timeout_count | INT | - | yes | 0 | 超时次数 | T/GDCAA 016-2025 6.4.7 |
| absent_count | INT | - | yes | 0 | 脱岗次数 | T/GDCAA 016-2025 6.4.7 |
| ai_detected_count | INT | - | yes | 0 | AI检测脱岗次数 | T/GDCAA 016-2025 6.4.7 |
| response_rate | DECIMAL | 5,2 | yes | 0 | 响应率（%） | T/GDCAA 016-2025 6.4.7 |
| absent_rate | DECIMAL | 5,2 | yes | 0 | 脱岗率（%） | T/GDCAA 016-2025 6.4.7 |
| rectify_completed | INT | - | yes | 0 | 已整改次数 | T/GDCAA 016-2025 6.4.7 |
| rectify_pending | INT | - | yes | 0 | 待整改次数 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_stat_date` (`stat_date`)
- KEY `idx_unit_id` (`unit_id`)

---

## 11. GIS态势感知模块（F8）

### 11.1 建筑标注表（building_marker）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| marker_id | VARCHAR | 32 | yes | - | 标注ID | - |
| building_id | VARCHAR | 32 | yes | - | 建筑ID | T/GDCAA 016-2025 11.1.1 |
| building_name | VARCHAR | 100 | yes | - | 建筑名称 | - |
| longitude | DECIMAL | 10,6 | yes | - | 经度 | T/GDCAA 016-2025 11.1.2 |
| latitude | DECIMAL | 10,6 | yes | - | 纬度 | T/GDCAA 016-2025 11.1.2 |
| address | VARCHAR | 200 | yes | - | 详细地址 | - |
| district | VARCHAR | 50 | yes | - | 所属行政区 | - |
| building_type | TINYINT | - | yes | - | 建筑类型 | - |
| area | DECIMAL | 10,2 | 否 | - | 建筑面积（平方米） | - |
| floor_count | INT | - | 否 | - | 楼层数 | - |
| device_count | INT | - | yes | 0 | 设备总数 | - |
| online_count | INT | - | yes | 0 | 在线设备数 | - |
| online_rate | DECIMAL | 5,2 | yes | 0 | 在线率（%） | - |
| risk_level | TINYINT | - | yes | 2 | 风险等级（1-高/2-中/3-低） | T/GDCAA 016-2025 11.1.3 |
| risk_index | DECIMAL | 5,2 | yes | 0 | 风险指数（0-100） | T/GDCAA 016-2025 11.3 |
| marker_color | VARCHAR | 20 | yes | - | 标注颜色（根据风险等级） | T/GDCAA 016-2025 11.1.5 |
| marker_icon | VARCHAR | 100 | 否 | - | 标注图标URL | - |
| update_time | DATETIME | - | yes | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_building_id` (`building_id`)
- KEY `idx_district` (`district`)
- KEY `idx_risk_level` (`risk_level`)
- KEY `idx_risk_index` (`risk_index`)

---

### 11.2 设备标注表（device_marker）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| marker_id | VARCHAR | 32 | yes | - | 标注ID | - |
| device_id | VARCHAR | 32 | yes | - | 设备ID | T/GDCAA 016-2025 11.2.1 |
| device_name | VARCHAR | 100 | yes | - | 设备名称 | - |
| device_type | TINYINT | - | yes | - | 设备类型（1-15类） | T/GDCAA 016-2025 11.2.2 |
| building_id | VARCHAR | 32 | yes | - | 所属建筑ID | - |
| floor | VARCHAR | 20 | yes | - | 所属楼层 | - |
| longitude | DECIMAL | 10,6 | yes | - | 经度 | - |
| latitude | DECIMAL | 10,6 | yes | - | 纬度 | - |
| position_desc | VARCHAR | 200 | 否 | - | 位置描述 | - |
| status | TINYINT | - | yes | 1 | 设备状态（1-在线/2-离线/3-故障） | T/GDCAA 016-2025 11.2.3 |
| status_color | VARCHAR | 20 | yes | - | 状态颜色 | - |
| update_time | DATETIME | - | yes | - | 更新时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_device_id` (`device_id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_device_type` (`device_type`)
- KEY `idx_status` (`status`)

---

### 11.3 告警标注表（alert_marker）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| marker_id | VARCHAR | 32 | yes | - | 标注ID | - |
| alert_id | VARCHAR | 32 | yes | - | 告警编号 | T/GDCAA 016-2025 11.4.1 |
| alert_type | TINYINT | - | yes | - | 告警类型 | - |
| building_id | VARCHAR | 32 | yes | - | 建筑ID | - |
| building_name | VARCHAR | 100 | yes | - | 建筑名称 | - |
| device_id | VARCHAR | 32 | yes | - | 设备ID | - |
| device_name | VARCHAR | 100 | yes | - | 设备名称 | - |
| longitude | DECIMAL | 10,6 | yes | - | 经度 | - |
| latitude | DECIMAL | 10,6 | yes | - | 纬度 | - |
| alert_time | DATETIME | - | yes | - | 告警时间 | - |
| alert_level | TINYINT | - | yes | - | 告警级别（1-一级/2-二级/3-三级） | - |
| alert_status | TINYINT | - | yes | 1 | 告警状态（1-未处置/2-处置中/3-已闭环） | - |
| blink_color | VARCHAR | 20 | yes | - | 闪烁颜色 | T/GDCAA 016-2025 11.4.3 |
| blink_interval | INT | - | yes | 500 | 闪烁间隔（毫秒） | T/GDCAA 016-2025 11.4.3 |
| is_blinking | TINYINT | - | yes | 1 | 是否闪烁（1-是/0-否） | T/GDCAA 016-2025 11.4.4 |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_alert_id` (`alert_id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_alert_time` (`alert_time`)
- KEY `idx_alert_level` (`alert_level`)
- KEY `idx_alert_status` (`alert_status`)

---

### 11.4 风险热力图表（risk_heatmap）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| heatmap_id | VARCHAR | 32 | yes | - | 热力图ID | - |
| building_id | VARCHAR | 32 | yes | - | 建筑ID | T/GDCAA 016-2025 11.3 |
| risk_index | DECIMAL | 5,2 | yes | 0 | 风险指数（0-100） | T/GDCAA 016-2025 11.3.2 |
| device_fault_rate | DECIMAL | 5,2 | yes | 0 | 设备故障率（%） | - |
| alert_frequency | INT | - | yes | 0 | 告警频率 | - |
| avg_repair_time | DECIMAL | 10,2 | yes | 0 | 平均修复时长（小时） | - |
| device_age_rate | DECIMAL | 5,2 | yes | 0 | 设备老化度（%） | - |
| inspection_rate | DECIMAL | 5,2 | yes | 0 | 巡检覆盖率（%） | - |
| risk_level | TINYINT | - | yes | 3 | 风险等级（1-高/2-中/3-低） | T/GDCAA 016-2025 11.3.2 |
| heat_value | DECIMAL | 10,2 | yes | 0 | 热力图权重值 | - |
| calculate_time | DATETIME | - | yes | - | 计算时间 | T/GDCAA 016-2025 11.3.1 |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_building_id` (`building_id`)
- KEY `idx_risk_level` (`risk_level`)
- KEY `idx_calculate_time` (`calculate_time`)

---

## 12. 移动端模块（F10）

### 12.1 值守记录表（mobile_duty_record）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| duty_id | VARCHAR | 32 | yes | - | 值守记录ID | T/GDCAA 016-2025 12.1.1 |
| user_id | VARCHAR | 32 | yes | - | 值守人员ID | - |
| user_name | VARCHAR | 64 | yes | - | 值守人员姓名 | - |
| duty_type | TINYINT | - | yes | - | 值守类型（1-远程/2-现场） | - |
| building_id | VARCHAR | 32 | 否 | - | 值守建筑 | - |
| start_time | DATETIME | - | yes | - | 值守开始时间 | T/GDCAA 016-2025 12.1.2 |
| end_time | DATETIME | - | 否 | - | 值守结束时间 | - |
| start_location | JSON | - | yes | - | 值守开始位置（GPS） | T/GDCAA 016-2025 12.1.2 |
| end_location | JSON | - | 否 | - | 值守结束位置（GPS） | - |
| duty_status | TINYINT | - | yes | 1 | 值守状态（1-进行中/2-已完成） | - |
| alerts_handled | INT | - | yes | 0 | 处理告警数 | - |
| notes | TEXT | - | 否 | - | 备注 | - |
| is_synced | TINYINT | - | yes | 1 | 是否已同步（1-是/0-否） | T/CFPA 052-2026 12.10.4 |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_duty_id` (`duty_id`)
- KEY `idx_user_id` (`user_id`)
- KEY `idx_start_time` (`start_time`)

---

### 12.2 取证数据表（evidence_data）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| evidence_id | VARCHAR | 32 | yes | - | 取证ID | T/CFPA 052-2026 12.12.6 |
| evidence_type | TINYINT | - | yes | - | 取证类型（1-照片/2-视频/3-录音） | - |
| business_type | VARCHAR | 32 | yes | - | 业务类型（alarm/work_order/inspection） | - |
| business_id | VARCHAR | 32 | yes | - | 业务ID | - |
| file_url | VARCHAR | 500 | yes | - | 文件URL | - |
| file_size | BIGINT | - | yes | 0 | 文件大小（字节） | - |
| file_format | VARCHAR | 20 | yes | - | 文件格式 | - |
| latitude | DECIMAL | 10,7 | yes | - | 纬度 | T/CFPA 052-2026 12.12.4 |
| longitude | DECIMAL | 10,7 | yes | - | 经度 | T/CFPA 052-2026 12.12.4 |
| location_accuracy | DECIMAL | 5,2 | yes | - | 定位精度（米） | - |
| capture_time | DATETIME | - | yes | - | 采集时间 | T/CFPA 052-2026 12.12.5 |
| device_info | VARCHAR | 200 | yes | - | 设备信息 | - |
| hash_value | VARCHAR | 64 | yes | - | 文件哈希值（防篡改） | T/CFPA 052-2026 12.12.6 |
| user_id | VARCHAR | 32 | yes | - | 采集人ID | - |
| user_name | VARCHAR | 64 | yes | - | 采集人姓名 | - |
| upload_status | TINYINT | - | yes | 1 | 上传状态（1-待上传/2-上传中/3-已上传/4-上传失败） | T/CFPA 052-2026 12.12.7 |
| upload_time | DATETIME | - | 否 | - | 上传时间 | - |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_evidence_id` (`evidence_id`)
- KEY `idx_business_id` (`business_id`)
- KEY `idx_capture_time` (`capture_time`)
- KEY `idx_user_id` (`user_id`)
- KEY `idx_upload_status` (`upload_status`)

---

### 12.3 离线数据缓存表（offline_cache）

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 | 合规来源 |
|--------|------|------|------|--------|------|----------|
| id | BIGINT | - | 是 | - | 主键 | - |
| cache_id | VARCHAR | 64 | yes | - | 缓存ID | - |
| data_type | VARCHAR | 32 | yes | - | 数据类型 | T/CFPA 052-2026 12.13 |
| business_id | VARCHAR | 64 | yes | - | 业务ID | - |
| data_content | JSON | - | yes | - | 数据内容 | T/CFPA 052-2026 12.13.2 |
| user_id | VARCHAR | 32 | yes | - | 用户ID | - |
| device_id | VARCHAR | 64 | yes | - | 设备ID | - |
| cached_time | DATETIME | - | yes | - | 缓存时间 | T/CFPA 052-2026 12.13.1 |
| sync_status | TINYINT | - | yes | 1 | 同步状态（1-待同步/2-同步中/3-已同步/4-同步失败） | T/CFPA 052-2026 12.13.2 |
| sync_time | DATETIME | - | 否 | - | 同步时间 | - |
| sync_retry_count | INT | - | yes | 0 | 同步重试次数 | - |
| last_error | TEXT | - | 否 | - | 最后错误信息 | T/CFPA 052-2026 12.13.4 |
| expire_time | DATETIME | - | yes | - | 过期时间（缓存7天后过期） | T/CFPA 052-2026 12.13 |
| create_time | DATETIME | - | yes | - | 创建时间 | - |

**索引**：
- PRIMARY KEY (`id`)
- KEY `idx_cache_id` (`cache_id`)
- KEY `idx_user_id` (`user_id`)
- KEY `idx_sync_status` (`sync_status`)
- KEY `idx_expire_time` (`expire_time`)

---

## 13. GB/T 26875.1-2026 合规对照表

### 13.1 附录A - 用户信息传输装置数据项

| 数据项编号 | 数据项名称 | 对应表字段 | 字段名称 | 必填 | 说明 |
|-----------|-----------|-----------|---------|------|------|
| A.2.1 | 单位代码 | org_unit | unit_code | 是 | 6位行政区划代码 |
| A.2.2 | 单位名称 | org_unit | unit_name | 是 | 联网单位全称 |
| A.3.1 | 建筑编码 | building | building_code | 是 | 20位标准编码 |
| A.3.2 | 建筑名称 | building | building_name | 是 | 建筑物名称 |
| A.4.1 | 传输装置编号 | monitor_point | point_id | 是 | 20位监测点编码 |
| A.4.2 | 传输装置状态 | device_realtime_status | status_type | 是 | 在线/离线/故障 |
| A.5.1 | 火警时间 | alarm_record | alarm_time | 是 | 精确到秒 |
| A.5.2 | 火警类型 | alarm_record | signal_type | 是 | 火警/故障/监管等 |
| A.5.3 | 报警设备编码 | alarm_record | device_code | 是 | 回路+点位 |
| A.5.4 | 报警部位 | alarm_record | location | 是 | 楼层+区域+位置 |
| A.6.1 | 设备类型 | monitor_point | device_type | 是 | 附录A设备类型表 |
| A.6.2 | 设备状态 | device_status | run_status | 是 | 运行/停止/故障 |
| A.7.1 | 系统时间 | sys_standard | server_time | 是 | 北京时间，误差≤5s |
| A.8.1 | 操作员编号 | sys_user | user_code | 是 | 唯一识别码 |
| A.8.2 | 操作员姓名 | sys_user | user_name | 是 | 真实姓名 |
| A.8.3 | 操作时间 | sys_security_log | log_time | 是 | 操作时间戳 |

**注**：附录A数据项共45项，以上为主要强制数据项对照

---

### 13.2 附录B - 建筑消防设施系统数据项

| 数据项编号 | 数据项名称 | 对应表字段 | 字段名称 | 必填 | 说明 |
|-----------|-----------|-----------|---------|------|------|
| B.2.1 | 联网单位编码 | org_unit | unit_code | 是 | 6位标准编码 |
| B.2.2 | 消防系统类型 | monitor_point | system_type | 是 | 附录B系统类型表 |
| B.2.3 | 设备类型编码 | monitor_point | device_type | 是 | 4位类型编码 |
| B.2.4 | 设备序号 | monitor_point | device_serial | 是 | 6位序号 |
| B.3.1 | 状态数据 | device_realtime_status | status_value | 是 | JSON格式状态值 |
| B.3.2 | 采集时间 | device_realtime_status | collect_time | 是 | UTC+8 |
| B.3.3 | 接收时间 | device_realtime_status | receive_time | 是 | 系统接收时间 |
| B.4.1 | 管网压力 | device_realtime_status | pressure_value | 否 | MPa，精度0.1 |
| B.4.2 | 水位高度 | device_realtime_status | water_level | 否 | m，精度0.1 |
| B.4.3 | 温度值 | device_realtime_status | temperature | 否 | ℃，精度0.1 |
| B.4.4 | 电气参数 | device_realtime_status | power_data | 否 | 电压/电流/功率 |
| B.5.1 | 操作记录 | sys_security_log | operation | 是 | 操作类型 |
| B.5.2 | 操作人员 | sys_security_log | user_id | 是 | 操作人ID |
| B.5.3 | 操作时间 | sys_security_log | log_time | 是 | 操作时间 |

**注**：附录B数据项共58项，以上为主要强制数据项对照

---

## 14. 数据不可篡改方案

### 14.1 方案概述

根据T/CFPA 052-2026和T/GDCAA 016-2025要求，对火警数据、控制指令、操作日志等核心数据实施不可篡改保护。

### 14.2 技术方案

#### 14.2.1 哈希链式存储

```
数据写入流程：
1. 数据写入数据库
2. 计算数据内容哈希：SHA-256(数据内容 + 时间戳 + 操作人ID)
3. 获取上一条记录的哈希值(prev_hash)
4. 将当前哈希和prev_hash存入data_signature表
5. 使用数据库触发器防止UPDATE和DELETE操作

链式验证：
- 任意记录变更 → 哈希值不匹配 → 验证失败
- 任意记录删除 → 链断裂 → 验证失败
```

#### 14.2.2 区块链存证（可选）

对于极高敏感数据（火警记录、控制指令），可将哈希值同步上链：

```
上链流程：
1. 数据写入本地数据库
2. 计算数据哈希
3. 将哈希值打包为区块链交易
4. 提交至联盟链（如蚂蚁链、腾讯区块链）
5. 返回链上交易哈希(tx_hash)和区块索引(chain_index)
6. 将tx_hash和chain_index存入data_signature表

验证流程：
1. 查询本地记录，获取chain_index
2. 调用区块链API，验证链上哈希与本地哈希一致
```

#### 14.2.3 数据库安全加固

```sql
-- 创建受保护表的安全规则
CREATE TRIGGER prevent_alarm_update
BEFORE UPDATE ON alarm_record
FOR EACH ROW
BEGIN
    -- 仅允许特定字段更新
    IF OLD.handle_status != NEW.handle_status OR 
       OLD.verify_result != NEW.verify_result THEN
        -- 允许更新处置相关字段
        SET NEW.update_time = NOW();
        SET NEW.hash_value = SHA2(CONCAT(NEW.*, NOW(), NEW.handler_id), 256);
    ELSE
        -- 拒绝其他更新
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '核心字段禁止修改';
    END IF;
END;

-- 禁止物理删除（逻辑删除除外）
CREATE TRIGGER prevent_alarm_delete
BEFORE DELETE ON alarm_record
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '告警记录禁止删除';
END;
```

### 14.3 受保护数据类型

| 数据类型 | 保护级别 | 保护措施 | 留存期限 | 合规来源 |
|---------|---------|---------|---------|---------|
| 火警记录 | 最高 | 哈希链+区块链存证 | 永久 | T/CFPA 052-2026 7.1 |
| 控制指令 | 最高 | 哈希链+区块链存证 | 永久 | T/CFPA 052-2026 7.1 |
| 操作日志 | 高 | 哈希链 | ≥5年 | GB 25201-2010 7.3 |
| 告警记录 | 高 | 哈希链 | ≥5年 | T/GDCAA 016-2025 9.7 |
| 值班记录 | 高 | 哈希链 | ≥3年 | GB 25506-2010 9.1 |
| 维保记录 | 中 | 哈希链 | ≥3年 | GB 25201-2010 9.2 |

### 14.4 完整性校验机制

```sql
-- 定期完整性校验存储过程
CREATE PROCEDURE verify_data_integrity(IN p_table_name VARCHAR(64), OUT p_result TINYINT)
BEGIN
    DECLARE v_hash VARCHAR(64);
    DECLARE v_prev_hash VARCHAR(64);
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_error_count INT DEFAULT 0;
    DECLARE done INT DEFAULT FALSE;
    
    DECLARE cur CURSOR FOR 
        SELECT hash_value, prev_hash FROM sys_security_log 
        ORDER BY log_time;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_hash, v_prev_hash;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET v_count = v_count + 1;
        
        -- 验证哈希链连续性
        IF v_count > 1 AND v_prev_hash != (SELECT hash_value FROM sys_security_log WHERE log_time < DATE_SUB((SELECT log_time FROM sys_security_log WHERE hash_value = v_prev_hash), INTERVAL 1 SECOND) ORDER BY log_time DESC LIMIT 1) THEN
            SET v_error_count = v_error_count + 1;
            -- 记录异常
            INSERT INTO integrity_error_log (table_name, hash_value, error_type, create_time)
            VALUES (p_table_name, v_hash, 'CHAIN_BREAK', NOW());
        END IF;
    END LOOP;
    CLOSE cur;
    
    -- 返回结果
    IF v_error_count = 0 THEN
        SET p_result = 1; -- 验证通过
    ELSE
        SET p_result = 0; -- 验证失败
    END IF;
END;
```

---

## 15. 索引策略

### 15.1 索引设计原则

| 原则 | 说明 |
|------|------|
| 主键索引 | 所有表必须有主键，使用BIGINT自增 |
| 外键索引 | 所有外键字段必须建立索引 |
| 查询索引 | 频繁查询条件字段建立索引 |
| 联合索引 | 多个字段联合查询时建立联合索引 |
| 唯一索引 | 唯一性字段建立唯一索引 |
| 覆盖索引 | 优先使用覆盖索引减少回表 |

### 15.2 核心高频查询索引

| 查询场景 | 表名 | 索引字段 | 索引类型 |
|---------|------|---------|---------|
| 按单位查告警 | alarm_record | unit_code, alarm_time | idx |
| 按时间查告警 | alarm_record | alarm_time, handle_status | idx |
| 火警实时监测 | alarm_record | signal_type, alarm_time, handle_status | idx |
| 设备状态查询 | device_status | device_id, update_time | uk |
| 告警联动查询 | alarm_video_linkage_record | alarm_id, pop_time | idx |
| 操作日志查询 | sys_security_log | user_id, log_time | idx |
| 查岗任务查询 | roll_call_task | unit_id, check_time, response_status | idx |

### 15.3 索引维护策略

```sql
-- 每周分析索引统计
ANALYZE TABLE alarm_record;
ANALYZE TABLE sys_security_log;
ANALYZE TABLE device_realtime_status;

-- 每月重建碎片化索引
OPTIMIZE TABLE alarm_record;
OPTIMIZE TABLE sys_security_log;
```

---

## 16. 分区策略

### 16.1 高频写入表分区

| 表名 | 分区字段 | 分区类型 | 保留策略 |
|------|---------|---------|---------|
| alarm_record | alarm_time | 按月RANGE | 全部保留，禁止删除 |
| sys_security_log | log_time | 按月RANGE | 保留5年后转归档 |
| device_realtime_status | collect_time | 按天RANGE | 30天后转归档 |
| video_record | start_time | 按月RANGE | 按留存期限自动删除 |
| control_command | sent_time | 按月RANGE | 全部保留，禁止删除 |

### 16.2 分区SQL示例

```sql
-- 告警记录表按月分区
ALTER TABLE alarm_record
PARTITION BY RANGE (TO_DAYS(alarm_time)) (
    PARTITION p_2026_01 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION p_2026_02 VALUES LESS THAN (TO_DAYS('2026-03-01')),
    PARTITION p_2026_03 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p_2026_04 VALUES LESS THAN (TO_DAYS('2026-05-01')),
    PARTITION p_2026_05 VALUES LESS THAN (TO_DAYS('2026-06-01')),
    PARTITION p_2026_06 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p_2026_07 VALUES LESS THAN (TO_DAYS('2026-08-01')),
    PARTITION p_2026_08 VALUES LESS THAN (TO_DAYS('2026-09-01')),
    PARTITION p_2026_09 VALUES LESS THAN (TO_DAYS('2026-10-01')),
    PARTITION p_2026_10 VALUES LESS THAN (TO_DAYS('2026-11-01')),
    PARTITION p_2026_11 VALUES LESS THAN (TO_DAYS('2026-12-01')),
    PARTITION p_2026_12 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 添加自动分区存储过程（建议使用事件调度）
DELIMITER //
CREATE EVENT auto_partition_event
ON SCHEDULE EVERY 1 MONTH
STARTS DATE_ADD(LAST_DAY(CURRENT_DATE), INTERVAL 1 DAY)
DO
BEGIN
    DECLARE next_month DATE;
    DECLARE partition_name VARCHAR(20);
    DECLARE partition_value DATE;
    
    SET next_month = DATE_ADD(LAST_DAY(CURRENT_DATE), INTERVAL 1 DAY);
    SET partition_name = CONCAT('p_', DATE_FORMAT(next_month, '%Y_%m'));
    SET partition_value = DATE_ADD(next_month, INTERVAL 1 MONTH);
    
    SET @sql = CONCAT(
        'ALTER TABLE alarm_record REORGANIZE PARTITION p_future INTO (',
        'PARTITION ', partition_name, ' VALUES LESS THAN (TO_DAYS(''', partition_value, ''')),',
        'PARTITION p_future VALUES LESS THAN MAXVALUE)'
    );
    
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END//
DELIMITER ;
```

---

## 17. 版本规划

### 17.1 V1.0（基础版本）

**功能范围**：
- 系统基础模块（F9）完整表结构
- 火警监测与处置模块（F1）完整表结构
- 远程控制模块（F2）完整表结构
- 监测预警模块（F3）核心表结构

**优先级**：P0

### 17.2 V1.1（增强版本）

**功能范围**：
- 运维管理模块（F4）完整表结构
- 数据与报表模块（F5）核心表结构
- 视频联动模块（F6）核心表结构

**优先级**：P0

### 17.3 V2.0（完整版本）

**功能范围**：
- 查岗管理模块（F7）完整表结构
- GIS态势感知模块（F8）完整表结构
- 移动端模块（F10）完整表结构
- 数据不可篡改方案完整实现
- 区块链存证功能

**优先级**：P1

---

## 18. 附录

### 18.1 设备15类分类编码（T/GDCAA 016-2025 附录A）

| 编码 | 设备类型 |
|------|---------|
| 01 | 火灾探测报警系统 |
| 02 | 消防联动控制系统 |
| 03 | 消防广播系统 |
| 04 | 消防电话系统 |
| 05 | 消火栓系统 |
| 06 | 自动喷水灭火系统 |
| 07 | 气体灭火系统 |
| 08 | 泡沫灭火系统 |
| 09 | 干粉灭火系统 |
| 10 | 防烟排烟系统 |
| 11 | 防火门监控系统 |
| 12 | 消防电源监控系统 |
| 13 | 电气火灾监控系统 |
| 14 | 消防应急照明和疏散指示系统 |
| 15 | 微型消防站设备 |

### 18.2 告警类型编码（GB/T 26875.1-2026 表A.1）

| 编码 | 告警类型 |
|------|---------|
| 1 | 火警 |
| 2 | 故障 |
| 3 | 监管 |
| 4 | 屏蔽 |
| 5 | 联动 |
| 6 | 预警 |

### 18.3 变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|---------|--------|
| V1.0 | 2026-06-15 | 初始版本，完整覆盖10个PRD模块 | 数据库设计团队 |

---

**文档结束**

---

> 本内容由 Coze AI 生成，请遵循相关法律法规及《人工智能生成合成内容标识办法》使用与传播。
