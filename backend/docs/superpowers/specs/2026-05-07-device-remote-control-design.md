# 设备远程控制模块设计文档

## 目标

基于四信智慧消防云平台 API，构建设备远程控制模块。支持：品牌管理、品牌功能管理、父设备绑定品牌型号版本、功能自定义配置、控制指令下发。

## 业务规则

1. **品牌数据**：从四信 API `/api/v1/hardwareFunction/getAllHardwareDetailed` 同步到本地数据库
2. **功能用途**：下发控制指令
3. **参数规则**：
   - 新增功能时，根据所选功能的 `functionCodeList` 动态显示参数输入框，用户手动填写参数值（如主机号、回路号、地址号）
   - 参数值以 JSON 格式存储在 `eiot_device_function.param_values` 中
   - 父设备功能（复位、消音）无参数，无需填写
   - 下发指令时，直接使用预填的参数值，无需再次选择子设备或填写参数
4. **权限**：新增独立权限点 `iot:brand:sync`、`iot:device:remote:config`、`iot:device:instruction:send`
5. **UI 入口**：设备详情页新增"远程控制"Tab，支持弹窗配置

## 数据模型

所有表均包含标准字段：`deleted`（软删除）、`tenant_id`（租户隔离）、`create_time`、`update_time`、`creator`、`updater`。

### 1. 品牌表 `eiot_brand` — 品牌基础信息

```sql
CREATE TABLE `eiot_brand` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '品牌id',
    `brand_code` varchar(64) NOT NULL COMMENT '品牌代码（如 QIN_NIAO）',
    `name` varchar(64) NOT NULL COMMENT '品牌名称（如"青鸟主机"）',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_brand_code` (`brand_code`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB COMMENT='设备品牌表';
```

### 2. 品牌型号表 `eiot_brand_model` — 同一品牌下的不同型号版本

```sql
CREATE TABLE `eiot_brand_model` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '型号id',
    `brand_id` bigint NOT NULL COMMENT '关联品牌id',
    `model` varchar(64) NOT NULL COMMENT '型号（如 JB-TG-JBF-11SF）',
    `version` varchar(32) NOT NULL COMMENT '版本（如 V1.0.0.0）',
    `sync_time` datetime DEFAULT NULL COMMENT '同步时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_brand_model_version` (`brand_id`, `model`, `version`),
    KEY `idx_brand_id` (`brand_id`)
) ENGINE=InnoDB COMMENT='品牌型号版本表';
```

### 3. 型号功能表 `eiot_brand_model_function` — 功能绑定到型号

```sql
CREATE TABLE `eiot_brand_model_function` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '功能id',
    `brand_model_id` bigint NOT NULL COMMENT '关联品牌型号id',
    `function_id` varchar(64) NOT NULL COMMENT '四信原始功能ID',
    `name` varchar(64) NOT NULL COMMENT '功能名称（如"总线开关"）',
    `rule_num` int DEFAULT NULL COMMENT '规则号',
    `await_time` int DEFAULT NULL COMMENT '等待时间(ms)',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_function` (`brand_model_id`, `function_id`),
    KEY `idx_brand_model_id` (`brand_model_id`)
) ENGINE=InnoDB COMMENT='型号功能表';
```

### 4. 功能参数表 `eiot_brand_model_function_param`

```sql
CREATE TABLE `eiot_brand_model_function_param` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数id',
    `function_id` bigint NOT NULL COMMENT '关联型号功能id（eiot_brand_model_function.id）',
    `param_name` varchar(64) NOT NULL COMMENT '参数名称（如"回路号"）',
    `param_code` varchar(64) NOT NULL COMMENT '参数代码（如 loopNo）',
    `param_type` varchar(32) NOT NULL COMMENT '参数类型（String/Long）',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_function_id` (`function_id`)
) ENGINE=InnoDB COMMENT='功能参数表';
```

### 5. 设备远程配置表 `eiot_device_remote_config` — 绑定到 `brand_model_id`

```sql
CREATE TABLE `eiot_device_remote_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置id',
    `device_id` bigint NOT NULL COMMENT '关联设备id',
    `brand_model_id` bigint NOT NULL COMMENT '绑定的品牌型号id',
    `confirm_password` varchar(64) DEFAULT NULL COMMENT '二次确认密码（加密存储）',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_id` (`device_id`),
    KEY `idx_brand_model_id` (`brand_model_id`)
) ENGINE=InnoDB COMMENT='设备远程配置表';
```

### 6. 设备功能实例表 `eiot_device_function` — 关联到 `brand_model_function_id`

```sql
CREATE TABLE `eiot_device_function` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `device_id` bigint NOT NULL COMMENT '关联设备id',
    `brand_model_function_id` bigint NOT NULL COMMENT '关联型号功能id',
    `button_name` varchar(64) NOT NULL COMMENT '自定义按钮名称',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `param_values` text COMMENT '参数值JSON（如 {"hostId":1,"loopNo":2,"partNo":3}）',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_function` (`device_id`, `brand_model_function_id`),
    KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB COMMENT='设备功能实例表';
```

## 后端 API 设计

### 品牌管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/eiot/brand/page` | 品牌分页查询 | `iot:brand:query` |
| POST | `/eiot/brand/sync` | 从四信同步品牌数据 | `iot:brand:sync` |
| GET | `/eiot/brand-model/{brandModelId}/function` | 获取型号下功能列表 | `iot:brand:query` |
| GET | `/eiot/brand/cascade` | 获取品牌级联数据（品牌→型号→版本） | `iot:brand:query` |

### 设备远程配置

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/eiot/device/{deviceId}/remote-config` | 获取设备远程配置 | `iot:device:query` |
| POST | `/eiot/device/{deviceId}/remote-config` | 保存设备远程配置 | `iot:device:remote:config` |

### 设备功能管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/eiot/device/{deviceId}/function` | 获取设备已绑定功能列表 | `iot:device:query` |
| POST | `/eiot/device/{deviceId}/function` | 新增设备功能 | `iot:device:remote:config` |
| PUT | `/eiot/device/{deviceId}/function/{id}` | 修改设备功能（按钮名称、顺序） | `iot:device:remote:config` |
| DELETE | `/eiot/device/{deviceId}/function/{id}` | 删除设备功能 | `iot:device:remote:config` |

### 指令下发

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/eiot/device/instruction/send` | 下发控制指令 | `iot:device:instruction:send` |

**指令下发请求体：**
```json
{
  "deviceId": 123,
  "deviceFunctionId": 789,
  "password": "123456"
}
```
- `deviceFunctionId`：设备功能实例id，后端根据此id获取功能定义和预填参数值
- `password`：配置了二次确认密码且功能非消音时必填

## 前端设计

### 设备详情页 - 远程控制 Tab

**布局：**
1. **基本信息卡片**
   - 主机品牌：显示品牌名称
   - 型号：显示型号
   - 版本：显示版本
   - 二次确认密码：显示 `******`
   - 操作：编辑按钮（打开"远程控制设置"弹窗）

2. **功能按钮区域**
   - 按 `sort_order` 排序展示已启用的功能按钮
   - 每个按钮显示 `button_name`
   - 点击按钮触发指令下发流程

### 远程控制设置弹窗

参照 UI 截图设计：

**基本信息区域：**
- 主机品牌：`el-select`（从 `/eiot/brand/cascade` 获取品牌列表）
- 型号：`el-select`（级联，选择品牌后加载该品牌下的型号列表）
- 版本：`el-select`（级联，选择型号后加载该型号下的版本列表）
- 二次确认密码：`el-input type="password"`
- 说明文字：若设置二次确认密码，除消音按钮外，其余按钮点击时都需要输入密码

**功能设置区域：**
- 功能列表表格：序号 | 功能 | 按钮名称 | 展示顺序 | 操作（编辑/删除）
- 新增功能按钮：打开"新增功能"弹窗

**新增功能弹窗**（参照截图）：
- 功能：`el-select`（从当前品牌型号的功能列表中选择，如"多线"）
- 按钮名称：`el-input`（默认带入功能名称，可修改）
- 展示顺序：`el-input-number`
- **变量区域**：选择功能后，根据该功能的 `functionCodeList` 动态生成参数输入框
  - 如"多线"功能显示：主机号、回路号、地址号
  - 如"复位"功能无参数，不显示变量区域
- 底部：取消 | 提交

**底部操作：** 关闭 | 保存

### 指令下发交互

1. 用户点击功能按钮
2. 若配置了二次确认密码且功能不是消音 → 弹窗输入密码
3. 后端根据 `deviceFunctionId` 获取功能定义和预填参数值
4. 组装指令参数，调用四信 API 下发
5. 返回下发结果给前端

## 品牌同步机制

1. 管理后台提供"同步品牌数据"按钮
2. 调用四信 API `/api/v1/hardwareFunction/getAllHardwareDetailed`
3. 清空并重新写入本地 `eiot_brand`、`eiot_brand_model`、`eiot_brand_model_function`、`eiot_brand_model_function_param`
4. 记录同步时间到 `eiot_brand_model.sync_time`
5. 同步过程中若设备已绑定的品牌被删除，保留 `eiot_device_remote_config` 和 `eiot_device_function` 记录（软关联），前端展示时提示"品牌数据已更新，请重新配置"

## 权限点汇总

| 权限标识 | 名称 | 说明 |
|---------|------|------|
| `iot:brand:query` | 品牌查询 | 查看品牌列表和功能 |
| `iot:brand:sync` | 品牌同步 | 从四信同步品牌数据 |
| `iot:device:remote:config` | 远程控制配置 | 配置设备品牌、型号、版本、功能 |
| `iot:device:instruction:send` | 下发指令 | 发送控制指令 |

## 与现有系统集成

1. **设备表 `eiot_device_info`**：已有 `brand_code`、`api_version` 字段，新模块通过 `brand_model_id` 精确关联到品牌-型号-版本组合（`eiot_brand_model` 表的一条记录即一个型号-版本），不影响现有字段
2. **设备父子关系**：复用现有 `parent_id` 关联
3. **前端路由**：在设备详情页 `/eiot/device/deviceDetail/:id` 新增"远程控制"Tab
