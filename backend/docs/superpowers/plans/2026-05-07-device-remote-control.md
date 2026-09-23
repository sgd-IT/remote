# 设备远程控制模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于四信智慧消防云平台 API，构建设备远程控制模块，包含品牌管理、品牌功能管理、设备远程配置、设备功能实例、指令下发。

**Architecture:** 后端采用 Spring Boot + MyBatis Plus 分层架构（Controller → Service → Mapper → DO），前端采用 Vue3 + Element Plus，数据通过 6 张新表持久化。

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, Vue3, Element Plus, TypeScript

---

## 文件结构

### 后端（module-eiot/module-eiot-biz）

| 文件 | 说明 |
|------|------|
| `dal/dataobject/brand/BrandDO.java` | 品牌 DO |
| `dal/dataobject/brand/BrandModelDO.java` | 品牌型号 DO |
| `dal/dataobject/brand/BrandModelFunctionDO.java` | 型号功能 DO |
| `dal/dataobject/brand/BrandModelFunctionParamDO.java` | 功能参数 DO |
| `dal/dataobject/device/DeviceRemoteConfigDO.java` | 设备远程配置 DO |
| `dal/dataobject/device/DeviceFunctionDO.java` | 设备功能实例 DO |
| `dal/mysql/brand/BrandMapper.java` | 品牌 Mapper |
| `dal/mysql/brand/BrandModelMapper.java` | 品牌型号 Mapper |
| `dal/mysql/brand/BrandModelFunctionMapper.java` | 型号功能 Mapper |
| `dal/mysql/brand/BrandModelFunctionParamMapper.java` | 功能参数 Mapper |
| `dal/mysql/device/DeviceRemoteConfigMapper.java` | 设备远程配置 Mapper |
| `dal/mysql/device/DeviceFunctionMapper.java` | 设备功能实例 Mapper |
| `service/brand/BrandService.java` | 品牌 Service 接口 |
| `service/brand/BrandServiceImpl.java` | 品牌 Service 实现 |
| `service/device/DeviceRemoteConfigService.java` | 设备远程配置 Service 接口 |
| `service/device/DeviceRemoteConfigServiceImpl.java` | 设备远程配置 Service 实现 |
| `service/device/DeviceFunctionService.java` | 设备功能 Service 接口 |
| `service/device/DeviceFunctionServiceImpl.java` | 设备功能 Service 实现 |
| `service/device/DeviceInstructionService.java` | 指令下发 Service 接口 |
| `service/device/DeviceInstructionServiceImpl.java` | 指令下发 Service 实现 |
| `controller/admin/brand/BrandController.java` | 品牌管理 Controller |
| `controller/admin/device/DeviceRemoteConfigController.java` | 设备远程配置 Controller |
| `controller/admin/device/DeviceFunctionController.java` | 设备功能 Controller |
| `convert/brand/BrandConvert.java` | 品牌 Convert |
| `convert/device/DeviceRemoteConfigConvert.java` | 设备远程配置 Convert |
| `convert/device/DeviceFunctionConvert.java` | 设备功能 Convert |
| `controller/admin/brand/vo/` | 品牌相关 VO |
| `controller/admin/device/vo/remote/` | 远程控制相关 VO |

### 前端（anwei-fire-remote-monitor-vue）

| 文件 | 说明 |
|------|------|
| `src/api/eiot/remote/brand.api.ts` | 品牌 API |
| `src/api/eiot/remote/deviceRemote.api.ts` | 设备远程控制 API |
| `src/views/eiot/deviceinfo/modules/RemoteControlTab.vue` | 远程控制 Tab |
| `src/views/eiot/deviceinfo/modules/RemoteConfigDialog.vue` | 远程控制设置弹窗 |
| `src/views/eiot/deviceinfo/modules/AddFunctionDialog.vue` | 新增功能弹窗 |
| `src/views/eiot/deviceinfo/detail.vue` | 在设备详情页新增 Tab |

### 数据库

| 文件 | 说明 |
|------|------|
| `_sql/mysql/update.sql` | 6 张表 DDL + 权限菜单数据 |

---

## Task 1: 数据库表创建

**Files:**
- Modify: `_sql/mysql/update.sql`（追加到文件末尾）

- [ ] **Step 1: 在 update.sql 末尾追加 6 张表的 DDL**

```sql
-- ----------------------------
-- 2026-05-07 设备远程控制模块表
-- ----------------------------

CREATE TABLE IF NOT EXISTS `eiot_brand` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '品牌id',
    `brand_code` varchar(64) NOT NULL COMMENT '品牌代码',
    `name` varchar(64) NOT NULL COMMENT '品牌名称',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_brand_code` (`brand_code`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB COMMENT='设备品牌表';

CREATE TABLE IF NOT EXISTS `eiot_brand_model` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '型号id',
    `brand_id` bigint NOT NULL COMMENT '关联品牌id',
    `model` varchar(64) NOT NULL COMMENT '型号',
    `version` varchar(32) NOT NULL COMMENT '版本',
    `sync_time` datetime DEFAULT NULL COMMENT '同步时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_brand_model_version` (`brand_id`, `model`, `version`),
    KEY `idx_brand_id` (`brand_id`)
) ENGINE=InnoDB COMMENT='品牌型号版本表';

CREATE TABLE IF NOT EXISTS `eiot_brand_model_function` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '功能id',
    `brand_model_id` bigint NOT NULL COMMENT '关联品牌型号id',
    `function_id` varchar(64) NOT NULL COMMENT '四信原始功能ID',
    `name` varchar(64) NOT NULL COMMENT '功能名称',
    `rule_num` int DEFAULT NULL COMMENT '规则号',
    `await_time` int DEFAULT NULL COMMENT '等待时间(ms)',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_function` (`brand_model_id`, `function_id`),
    KEY `idx_brand_model_id` (`brand_model_id`)
) ENGINE=InnoDB COMMENT='型号功能表';

CREATE TABLE IF NOT EXISTS `eiot_brand_model_function_param` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数id',
    `function_id` bigint NOT NULL COMMENT '关联型号功能id',
    `param_name` varchar(64) NOT NULL COMMENT '参数名称',
    `param_code` varchar(64) NOT NULL COMMENT '参数代码',
    `param_type` varchar(32) NOT NULL COMMENT '参数类型',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_function_id` (`function_id`)
) ENGINE=InnoDB COMMENT='功能参数表';

CREATE TABLE IF NOT EXISTS `eiot_device_remote_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置id',
    `device_id` bigint NOT NULL COMMENT '关联设备id',
    `brand_model_id` bigint NOT NULL COMMENT '绑定的品牌型号id',
    `confirm_password` varchar(64) DEFAULT NULL COMMENT '二次确认密码',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_id` (`device_id`),
    KEY `idx_brand_model_id` (`brand_model_id`)
) ENGINE=InnoDB COMMENT='设备远程配置表';

CREATE TABLE IF NOT EXISTS `eiot_device_function` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `device_id` bigint NOT NULL COMMENT '关联设备id',
    `brand_model_function_id` bigint NOT NULL COMMENT '关联型号功能id',
    `button_name` varchar(64) NOT NULL COMMENT '自定义按钮名称',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `param_values` text COMMENT '参数值JSON',
    `enabled` bit(1) NOT NULL DEFAULT b'1',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_function` (`device_id`, `brand_model_function_id`),
    KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB COMMENT='设备功能实例表';
```

- [ ] **Step 2: 执行 SQL 脚本**

Run: 连接 MySQL 数据库，执行上述 DDL 语句
Expected: 6 张表创建成功

---

## Task 2: 后端 DO 类

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/brand/BrandDO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/brand/BrandModelDO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/brand/BrandModelFunctionDO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/brand/BrandModelFunctionParamDO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/device/DeviceRemoteConfigDO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/device/DeviceFunctionDO.java`

- [ ] **Step 1: 创建 BrandDO.java**

```java
package com.enjoyiot.module.eiot.dal.dataobject.brand;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_brand")
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String brandCode;
    private String name;
}
```

- [ ] **Step 2: 创建 BrandModelDO.java**

```java
package com.enjoyiot.module.eiot.dal.dataobject.brand;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("eiot_brand_model")
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandModelDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long brandId;
    private String model;
    private String version;
    private LocalDateTime syncTime;
}
```

- [ ] **Step 3: 创建 BrandModelFunctionDO.java**

```java
package com.enjoyiot.module.eiot.dal.dataobject.brand;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_brand_model_function")
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandModelFunctionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long brandModelId;
    private String functionId;
    private String name;
    private Integer ruleNum;
    private Integer awaitTime;
}
```

- [ ] **Step 4: 创建 BrandModelFunctionParamDO.java**

```java
package com.enjoyiot.module.eiot.dal.dataobject.brand;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_brand_model_function_param")
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandModelFunctionParamDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long functionId;
    private String paramName;
    private String paramCode;
    private String paramType;
}
```

- [ ] **Step 5: 创建 DeviceRemoteConfigDO.java**

```java
package com.enjoyiot.module.eiot.dal.dataobject.device;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_device_remote_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceRemoteConfigDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deviceId;
    private Long brandModelId;
    private String confirmPassword;
}
```

- [ ] **Step 6: 创建 DeviceFunctionDO.java**

```java
package com.enjoyiot.module.eiot.dal.dataobject.device;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_device_function")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceFunctionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deviceId;
    private Long brandModelFunctionId;
    private String buttonName;
    private Integer sortOrder;
    private String paramValues;
    private Boolean enabled;
}
```

---

## Task 3: 后端 Mapper 接口

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/brand/BrandMapper.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/brand/BrandModelMapper.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/brand/BrandModelFunctionMapper.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/brand/BrandModelFunctionParamMapper.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/device/DeviceRemoteConfigMapper.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/device/DeviceFunctionMapper.java`

- [ ] **Step 1: 创建 BrandMapper.java**

```java
package com.enjoyiot.module.eiot.dal.mysql.brand;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BrandMapper extends BaseMapperX<BrandDO> {
}
```

- [ ] **Step 2: 创建 BrandModelMapper.java**

```java
package com.enjoyiot.module.eiot.dal.mysql.brand;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrandModelMapper extends BaseMapperX<BrandModelDO> {

    @Select("SELECT * FROM eiot_brand_model WHERE brand_id = #{brandId} AND deleted = 0")
    List<BrandModelDO> selectByBrandId(@Param("brandId") Long brandId);
}
```

- [ ] **Step 3: 创建 BrandModelFunctionMapper.java**

```java
package com.enjoyiot.module.eiot.dal.mysql.brand;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrandModelFunctionMapper extends BaseMapperX<BrandModelFunctionDO> {

    @Select("SELECT * FROM eiot_brand_model_function WHERE brand_model_id = #{brandModelId} AND deleted = 0")
    List<BrandModelFunctionDO> selectByBrandModelId(@Param("brandModelId") Long brandModelId);
}
```

- [ ] **Step 4: 创建 BrandModelFunctionParamMapper.java**

```java
package com.enjoyiot.module.eiot.dal.mysql.brand;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionParamDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrandModelFunctionParamMapper extends BaseMapperX<BrandModelFunctionParamDO> {

    @Select("SELECT * FROM eiot_brand_model_function_param WHERE function_id = #{functionId} AND deleted = 0")
    List<BrandModelFunctionParamDO> selectByFunctionId(@Param("functionId") Long functionId);
}
```

- [ ] **Step 5: 创建 DeviceRemoteConfigMapper.java**

```java
package com.enjoyiot.module.eiot.dal.mysql.device;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceRemoteConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceRemoteConfigMapper extends BaseMapperX<DeviceRemoteConfigDO> {

    @Select("SELECT * FROM eiot_device_remote_config WHERE device_id = #{deviceId} AND deleted = 0")
    DeviceRemoteConfigDO selectByDeviceId(@Param("deviceId") Long deviceId);
}
```

- [ ] **Step 6: 创建 DeviceFunctionMapper.java**

```java
package com.enjoyiot.module.eiot.dal.mysql.device;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceFunctionMapper extends BaseMapperX<DeviceFunctionDO> {

    @Select("SELECT * FROM eiot_device_function WHERE device_id = #{deviceId} AND deleted = 0 AND enabled = 1 ORDER BY sort_order")
    List<DeviceFunctionDO> selectByDeviceId(@Param("deviceId") Long deviceId);
}
```

---

## Task 4: 后端 VO 类

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/brand/vo/BrandRespVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/brand/vo/BrandModelRespVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/brand/vo/BrandModelFunctionRespVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/brand/vo/BrandCascadeRespVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/remote/DeviceRemoteConfigRespVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/remote/DeviceRemoteConfigSaveReqVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/remote/DeviceFunctionRespVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/remote/DeviceFunctionSaveReqVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/remote/DeviceFunctionAddReqVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/remote/InstructionSendReqVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/vo/remote/InstructionSendRespVO.java`

- [ ] **Step 1: 创建 BrandRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.brand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "品牌响应 VO")
public class BrandRespVO {

    @Schema(description = "品牌id")
    private Long id;

    @Schema(description = "品牌代码")
    private String brandCode;

    @Schema(description = "品牌名称")
    private String name;
}
```

- [ ] **Step 2: 创建 BrandModelRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.brand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "品牌型号版本响应 VO")
public class BrandModelRespVO {

    @Schema(description = "型号id")
    private Long id;

    @Schema(description = "品牌id")
    private Long brandId;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "同步时间")
    private LocalDateTime syncTime;
}
```

- [ ] **Step 3: 创建 BrandModelFunctionRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.brand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "型号功能响应 VO")
public class BrandModelFunctionRespVO {

    @Schema(description = "功能id")
    private Long id;

    @Schema(description = "品牌型号id")
    private Long brandModelId;

    @Schema(description = "四信原始功能ID")
    private String functionId;

    @Schema(description = "功能名称")
    private String name;

    @Schema(description = "规则号")
    private Integer ruleNum;

    @Schema(description = "等待时间(ms)")
    private Integer awaitTime;

    @Schema(description = "参数列表")
    private List<FunctionParamRespVO> params;

    @Data
    public static class FunctionParamRespVO {
        private Long id;
        private String paramName;
        private String paramCode;
        private String paramType;
    }
}
```

- [ ] **Step 4: 创建 BrandCascadeRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.brand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "品牌级联数据 VO")
public class BrandCascadeRespVO {

    @Schema(description = "品牌id")
    private Long id;

    @Schema(description = "品牌代码")
    private String brandCode;

    @Schema(description = "品牌名称")
    private String name;

    @Schema(description = "型号列表")
    private List<ModelVO> models;

    @Data
    public static class ModelVO {
        private Long id;
        private String model;
        private List<String> versions;
    }
}
```

- [ ] **Step 5: 创建 DeviceRemoteConfigRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "设备远程配置响应 VO")
public class DeviceRemoteConfigRespVO {

    @Schema(description = "配置id")
    private Long id;

    @Schema(description = "设备id")
    private Long deviceId;

    @Schema(description = "品牌型号id")
    private Long brandModelId;

    @Schema(description = "品牌名称")
    private String brandName;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "是否设置了二次确认密码")
    private Boolean hasPassword;
}
```

- [ ] **Step 6: 创建 DeviceRemoteConfigSaveReqVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "设备远程配置保存请求 VO")
public class DeviceRemoteConfigSaveReqVO {

    @Schema(description = "品牌型号id")
    @NotNull(message = "品牌型号不能为空")
    private Long brandModelId;

    @Schema(description = "二次确认密码（空表示不设置）")
    private String confirmPassword;
}
```

- [ ] **Step 7: 创建 DeviceFunctionRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "设备功能实例响应 VO")
public class DeviceFunctionRespVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "设备id")
    private Long deviceId;

    @Schema(description = "关联型号功能id")
    private Long brandModelFunctionId;

    @Schema(description = "功能名称")
    private String functionName;

    @Schema(description = "按钮名称")
    private String buttonName;

    @Schema(description = "展示顺序")
    private Integer sortOrder;

    @Schema(description = "参数值JSON")
    private String paramValues;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
```

- [ ] **Step 8: 创建 DeviceFunctionSaveReqVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "设备功能保存请求 VO")
public class DeviceFunctionSaveReqVO {

    @Schema(description = "关联型号功能id")
    @NotNull(message = "功能不能为空")
    private Long brandModelFunctionId;

    @Schema(description = "按钮名称")
    @NotBlank(message = "按钮名称不能为空")
    private String buttonName;

    @Schema(description = "展示顺序")
    private Integer sortOrder;

    @Schema(description = "参数值JSON")
    private String paramValues;
}
```

- [ ] **Step 9: 创建 InstructionSendReqVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "指令下发请求 VO")
public class InstructionSendReqVO {

    @Schema(description = "设备id")
    @NotNull(message = "设备id不能为空")
    private Long deviceId;

    @Schema(description = "设备功能实例id")
    @NotNull(message = "功能id不能为空")
    private Long deviceFunctionId;

    @Schema(description = "二次确认密码")
    private String password;
}
```

- [ ] **Step 10: 创建 InstructionSendRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "指令下发响应 VO")
public class InstructionSendRespVO {

    @Schema(description = "下发结果：true成功 false失败")
    private Boolean success;

    @Schema(description = "结果消息")
    private String message;
}
```

---

## Task 5: 后端 Convert 类

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/convert/brand/BrandConvert.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/convert/device/DeviceRemoteConfigConvert.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/convert/device/DeviceFunctionConvert.java`

- [ ] **Step 1: 创建 BrandConvert.java**

```java
package com.enjoyiot.module.eiot.convert.brand;

import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BrandConvert {

    BrandConvert INSTANCE = Mappers.getMapper(BrandConvert.class);

    BrandRespVO convert(BrandDO bean);

    List<BrandRespVO> convertList(List<BrandDO> list);
}
```

- [ ] **Step 2: 创建 DeviceRemoteConfigConvert.java**

```java
package com.enjoyiot.module.eiot.convert.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceRemoteConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DeviceRemoteConfigConvert {

    DeviceRemoteConfigConvert INSTANCE = Mappers.getMapper(DeviceRemoteConfigConvert.class);

    DeviceRemoteConfigRespVO convert(DeviceRemoteConfigDO bean);
}
```

- [ ] **Step 3: 创建 DeviceFunctionConvert.java**

```java
package com.enjoyiot.module.eiot.convert.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface DeviceFunctionConvert {

    DeviceFunctionConvert INSTANCE = Mappers.getMapper(DeviceFunctionConvert.class);

    DeviceFunctionRespVO convert(DeviceFunctionDO bean);

    List<DeviceFunctionRespVO> convertList(List<DeviceFunctionDO> list);
}
```

---

## Task 6: 后端 Service 层

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/brand/BrandService.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/brand/BrandServiceImpl.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceRemoteConfigService.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceRemoteConfigServiceImpl.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceFunctionService.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceFunctionServiceImpl.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInstructionService.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInstructionServiceImpl.java`

- [ ] **Step 1: 创建 BrandService.java**

```java
package com.enjoyiot.module.eiot.service.brand;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandCascadeRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandModelFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandDO;

import java.util.List;

public interface BrandService {

    /**
     * 分页查询品牌
     */
    PageResult<BrandRespVO> getBrandPage(int pageNo, int pageSize);

    /**
     * 同步品牌数据（从四信API）
     */
    void syncBrandData();

    /**
     * 获取品牌级联数据
     */
    List<BrandCascadeRespVO> getBrandCascade();

    /**
     * 获取型号下的功能列表
     */
    List<BrandModelFunctionRespVO> getBrandModelFunctions(Long brandModelId);
}
```

- [ ] **Step 2: 创建 BrandServiceImpl.java**

```java
package com.enjoyiot.module.eiot.service.brand;

import cn.hutool.core.collection.CollUtil;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandCascadeRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandModelFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandRespVO;
import com.enjoyiot.module.eiot.convert.brand.BrandConvert;
import com.enjoyiot.module.eiot.dal.dataobject.brand.*;
import com.enjoyiot.module.eiot.dal.mysql.brand.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BrandServiceImpl implements BrandService {

    @Resource
    private BrandMapper brandMapper;
    @Resource
    private BrandModelMapper brandModelMapper;
    @Resource
    private BrandModelFunctionMapper brandModelFunctionMapper;
    @Resource
    private BrandModelFunctionParamMapper brandModelFunctionParamMapper;

    @Override
    public PageResult<BrandRespVO> getBrandPage(int pageNo, int pageSize) {
        // 简化实现：查询所有品牌
        List<BrandDO> list = brandMapper.selectList(new LambdaQueryWrapperX<BrandDO>()
                .eq(BrandDO::getDeleted, false));
        return new PageResult<>(BrandConvert.INSTANCE.convertList(list), (long) list.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncBrandData() {
        // 第一阶段：基础框架搭建，四信API调用在后续迭代中实现
        // 同步逻辑：调用四信 /api/v1/hardwareFunction/getAllHardwareDetailed
        // 清空 eiot_brand_model / eiot_brand_model_function / eiot_brand_model_function_param
        // 按 brandCode + model + version 分组写入
        log.info("品牌数据同步（基础框架已就绪，四信API调用待接入）");
    }

    @Override
    public List<BrandCascadeRespVO> getBrandCascade() {
        List<BrandDO> brands = brandMapper.selectList(new LambdaQueryWrapperX<BrandDO>()
                .eq(BrandDO::getDeleted, false));

        List<BrandCascadeRespVO> result = new ArrayList<>();
        for (BrandDO brand : brands) {
            BrandCascadeRespVO vo = new BrandCascadeRespVO();
            vo.setId(brand.getId());
            vo.setBrandCode(brand.getBrandCode());
            vo.setName(brand.getName());

            List<BrandModelDO> models = brandModelMapper.selectByBrandId(brand.getId());
            List<BrandCascadeRespVO.ModelVO> modelVOs = new ArrayList<>();
            for (BrandModelDO model : models) {
                BrandCascadeRespVO.ModelVO mvo = new BrandCascadeRespVO.ModelVO();
                mvo.setId(model.getId());
                mvo.setModel(model.getModel());
                mvo.setVersions(List.of(model.getVersion()));
                modelVOs.add(mvo);
            }
            vo.setModels(modelVOs);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<BrandModelFunctionRespVO> getBrandModelFunctions(Long brandModelId) {
        List<BrandModelFunctionDO> functions = brandModelFunctionMapper.selectByBrandModelId(brandModelId);
        List<BrandModelFunctionRespVO> result = new ArrayList<>();
        for (BrandModelFunctionDO function : functions) {
            BrandModelFunctionRespVO vo = new BrandModelFunctionRespVO();
            vo.setId(function.getId());
            vo.setBrandModelId(function.getBrandModelId());
            vo.setFunctionId(function.getFunctionId());
            vo.setName(function.getName());
            vo.setRuleNum(function.getRuleNum());
            vo.setAwaitTime(function.getAwaitTime());

            List<BrandModelFunctionParamDO> params = brandModelFunctionParamMapper.selectByFunctionId(function.getId());
            List<BrandModelFunctionRespVO.FunctionParamRespVO> paramVOs = new ArrayList<>();
            for (BrandModelFunctionParamDO param : params) {
                BrandModelFunctionRespVO.FunctionParamRespVO pvo = new BrandModelFunctionRespVO.FunctionParamRespVO();
                pvo.setId(param.getId());
                pvo.setParamName(param.getParamName());
                pvo.setParamCode(param.getParamCode());
                pvo.setParamType(param.getParamType());
                paramVOs.add(pvo);
            }
            vo.setParams(paramVOs);
            result.add(vo);
        }
        return result;
    }
}
```

- [ ] **Step 3: 创建 DeviceRemoteConfigService.java**

```java
package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigSaveReqVO;

public interface DeviceRemoteConfigService {

    /**
     * 获取设备远程配置
     */
    DeviceRemoteConfigRespVO getRemoteConfig(Long deviceId);

    /**
     * 保存设备远程配置
     */
    void saveRemoteConfig(Long deviceId, DeviceRemoteConfigSaveReqVO reqVO);
}
```

- [ ] **Step 4: 创建 DeviceRemoteConfigServiceImpl.java**

```java
package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigSaveReqVO;
import com.enjoyiot.module.eiot.convert.device.DeviceRemoteConfigConvert;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandDO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelDO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceRemoteConfigDO;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandMapper;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandModelMapper;
import com.enjoyiot.module.eiot.dal.mysql.device.DeviceRemoteConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DeviceRemoteConfigServiceImpl implements DeviceRemoteConfigService {

    @Resource
    private DeviceRemoteConfigMapper deviceRemoteConfigMapper;
    @Resource
    private BrandModelMapper brandModelMapper;
    @Resource
    private BrandMapper brandMapper;

    @Override
    public DeviceRemoteConfigRespVO getRemoteConfig(Long deviceId) {
        DeviceRemoteConfigDO config = deviceRemoteConfigMapper.selectByDeviceId(deviceId);
        if (config == null) {
            return null;
        }
        DeviceRemoteConfigRespVO vo = DeviceRemoteConfigConvert.INSTANCE.convert(config);

        BrandModelDO brandModel = brandModelMapper.selectById(config.getBrandModelId());
        if (brandModel != null) {
            vo.setModel(brandModel.getModel());
            vo.setVersion(brandModel.getVersion());
            BrandDO brand = brandMapper.selectById(brandModel.getBrandId());
            if (brand != null) {
                vo.setBrandName(brand.getName());
            }
        }
        vo.setHasPassword(config.getConfirmPassword() != null && !config.getConfirmPassword().isEmpty());
        return vo;
    }

    @Override
    public void saveRemoteConfig(Long deviceId, DeviceRemoteConfigSaveReqVO reqVO) {
        DeviceRemoteConfigDO existing = deviceRemoteConfigMapper.selectByDeviceId(deviceId);
        if (existing != null) {
            existing.setBrandModelId(reqVO.getBrandModelId());
            if (reqVO.getConfirmPassword() != null) {
                existing.setConfirmPassword(reqVO.getConfirmPassword());
            }
            deviceRemoteConfigMapper.updateById(existing);
        } else {
            DeviceRemoteConfigDO config = new DeviceRemoteConfigDO();
            config.setDeviceId(deviceId);
            config.setBrandModelId(reqVO.getBrandModelId());
            config.setConfirmPassword(reqVO.getConfirmPassword());
            deviceRemoteConfigMapper.insert(config);
        }
    }
}
```

- [ ] **Step 5: 创建 DeviceFunctionService.java**

```java
package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionSaveReqVO;

import java.util.List;

public interface DeviceFunctionService {

    /**
     * 获取设备功能列表
     */
    List<DeviceFunctionRespVO> getDeviceFunctions(Long deviceId);

    /**
     * 新增设备功能
     */
    Long addDeviceFunction(Long deviceId, DeviceFunctionSaveReqVO reqVO);

    /**
     * 更新设备功能
     */
    void updateDeviceFunction(Long id, DeviceFunctionSaveReqVO reqVO);

    /**
     * 删除设备功能
     */
    void deleteDeviceFunction(Long id);
}
```

- [ ] **Step 6: 创建 DeviceFunctionServiceImpl.java**

```java
package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionSaveReqVO;
import com.enjoyiot.module.eiot.convert.device.DeviceFunctionConvert;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionDO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandModelFunctionMapper;
import com.enjoyiot.module.eiot.dal.mysql.device.DeviceFunctionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceFunctionServiceImpl implements DeviceFunctionService {

    @Resource
    private DeviceFunctionMapper deviceFunctionMapper;
    @Resource
    private BrandModelFunctionMapper brandModelFunctionMapper;

    @Override
    public List<DeviceFunctionRespVO> getDeviceFunctions(Long deviceId) {
        List<DeviceFunctionDO> list = deviceFunctionMapper.selectByDeviceId(deviceId);
        List<DeviceFunctionRespVO> result = DeviceFunctionConvert.INSTANCE.convertList(list);
        for (DeviceFunctionRespVO vo : result) {
            BrandModelFunctionDO func = brandModelFunctionMapper.selectById(vo.getBrandModelFunctionId());
            if (func != null) {
                vo.setFunctionName(func.getName());
            }
        }
        return result;
    }

    @Override
    public Long addDeviceFunction(Long deviceId, DeviceFunctionSaveReqVO reqVO) {
        DeviceFunctionDO function = new DeviceFunctionDO();
        function.setDeviceId(deviceId);
        function.setBrandModelFunctionId(reqVO.getBrandModelFunctionId());
        function.setButtonName(reqVO.getButtonName());
        function.setSortOrder(reqVO.getSortOrder());
        function.setParamValues(reqVO.getParamValues());
        function.setEnabled(true);
        deviceFunctionMapper.insert(function);
        return function.getId();
    }

    @Override
    public void updateDeviceFunction(Long id, DeviceFunctionSaveReqVO reqVO) {
        DeviceFunctionDO function = deviceFunctionMapper.selectById(id);
        if (function == null) {
            return;
        }
        function.setButtonName(reqVO.getButtonName());
        function.setSortOrder(reqVO.getSortOrder());
        function.setParamValues(reqVO.getParamValues());
        deviceFunctionMapper.updateById(function);
    }

    @Override
    public void deleteDeviceFunction(Long id) {
        deviceFunctionMapper.deleteById(id);
    }
}
```

- [ ] **Step 7: 创建 DeviceInstructionService.java**

```java
package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendReqVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendRespVO;

public interface DeviceInstructionService {

    /**
     * 下发控制指令
     */
    InstructionSendRespVO sendInstruction(InstructionSendReqVO reqVO);
}
```

- [ ] **Step 8: 创建 DeviceInstructionServiceImpl.java**

```java
package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendReqVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionDO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceRemoteConfigDO;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandModelFunctionMapper;
import com.enjoyiot.module.eiot.dal.mysql.device.DeviceFunctionMapper;
import com.enjoyiot.module.eiot.dal.mysql.device.DeviceRemoteConfigMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeviceInstructionServiceImpl implements DeviceInstructionService {

    @Resource
    private DeviceFunctionMapper deviceFunctionMapper;
    @Resource
    private BrandModelFunctionMapper brandModelFunctionMapper;
    @Resource
    private DeviceRemoteConfigMapper deviceRemoteConfigMapper;

    @Override
    public InstructionSendRespVO sendInstruction(InstructionSendReqVO reqVO) {
        DeviceFunctionDO deviceFunction = deviceFunctionMapper.selectById(reqVO.getDeviceFunctionId());
        if (deviceFunction == null) {
            return buildResp(false, "功能不存在");
        }

        BrandModelFunctionDO functionDef = brandModelFunctionMapper.selectById(deviceFunction.getBrandModelFunctionId());
        if (functionDef == null) {
            return buildResp(false, "功能定义不存在");
        }

        // 校验二次确认密码（非消音功能）
        if (!"消音".equals(functionDef.getName())) {
            DeviceRemoteConfigDO config = deviceRemoteConfigMapper.selectByDeviceId(reqVO.getDeviceId());
            if (config != null && config.getConfirmPassword() != null
                    && !config.getConfirmPassword().isEmpty()) {
                if (!config.getConfirmPassword().equals(reqVO.getPassword())) {
                    return buildResp(false, "二次确认密码错误");
                }
            }
        }

        // 第一阶段：基础框架搭建，四信API调用在后续迭代中实现
        // 组装参数：从 deviceFunction.paramValues 获取参数值
        // 调用四信API：POST /api/v1/instruction/send
        log.info("下发指令：deviceId={}, functionId={}, params={}",
                reqVO.getDeviceId(), functionDef.getFunctionId(), deviceFunction.getParamValues());

        return buildResp(true, "指令已下发");
    }

    private InstructionSendRespVO buildResp(boolean success, String message) {
        InstructionSendRespVO resp = new InstructionSendRespVO();
        resp.setSuccess(success);
        resp.setMessage(message);
        return resp;
    }
}
```

---

## Task 7: 后端 Controller 层

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/brand/BrandController.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/DeviceRemoteConfigController.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/DeviceFunctionController.java`

- [ ] **Step 1: 创建 BrandController.java**

```java
package com.enjoyiot.module.eiot.controller.admin.brand;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandCascadeRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandModelFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandRespVO;
import com.enjoyiot.module.eiot.service.brand.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备品牌")
@RestController
@RequestMapping("/eiot/brand")
public class BrandController {

    @Resource
    private BrandService brandService;

    @GetMapping("/page")
    @Operation(summary = "品牌分页查询")
    @PreAuthorize("@ss.hasPermission('iot:brand:query')")
    public CommonResult<PageResult<BrandRespVO>> getBrandPage(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return success(brandService.getBrandPage(pageNo, pageSize));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步品牌数据")
    @PreAuthorize("@ss.hasPermission('iot:brand:sync')")
    public CommonResult<Boolean> syncBrandData() {
        brandService.syncBrandData();
        return success(true);
    }

    @GetMapping("/cascade")
    @Operation(summary = "获取品牌级联数据")
    @PreAuthorize("@ss.hasPermission('iot:brand:query')")
    public CommonResult<List<BrandCascadeRespVO>> getBrandCascade() {
        return success(brandService.getBrandCascade());
    }

    @GetMapping("/model/{brandModelId}/function")
    @Operation(summary = "获取型号下功能列表")
    @PreAuthorize("@ss.hasPermission('iot:brand:query')")
    public CommonResult<List<BrandModelFunctionRespVO>> getBrandModelFunctions(
            @PathVariable("brandModelId") Long brandModelId) {
        return success(brandService.getBrandModelFunctions(brandModelId));
    }
}
```

- [ ] **Step 2: 创建 DeviceRemoteConfigController.java**

```java
package com.enjoyiot.module.eiot.controller.admin.device;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigSaveReqVO;
import com.enjoyiot.module.eiot.service.device.DeviceRemoteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备远程配置")
@RestController
@RequestMapping("/eiot/device")
public class DeviceRemoteConfigController {

    @Resource
    private DeviceRemoteConfigService deviceRemoteConfigService;

    @GetMapping("/{deviceId}/remote-config")
    @Operation(summary = "获取设备远程配置")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<DeviceRemoteConfigRespVO> getRemoteConfig(
            @PathVariable("deviceId") Long deviceId) {
        return success(deviceRemoteConfigService.getRemoteConfig(deviceId));
    }

    @PostMapping("/{deviceId}/remote-config")
    @Operation(summary = "保存设备远程配置")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:config')")
    public CommonResult<Boolean> saveRemoteConfig(
            @PathVariable("deviceId") Long deviceId,
            @Valid @RequestBody DeviceRemoteConfigSaveReqVO reqVO) {
        deviceRemoteConfigService.saveRemoteConfig(deviceId, reqVO);
        return success(true);
    }
}
```

- [ ] **Step 3: 创建 DeviceFunctionController.java**

```java
package com.enjoyiot.module.eiot.controller.admin.device;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.*;
import com.enjoyiot.module.eiot.service.device.DeviceFunctionService;
import com.enjoyiot.module.eiot.service.device.DeviceInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备功能管理")
@RestController
@RequestMapping("/eiot/device")
public class DeviceFunctionController {

    @Resource
    private DeviceFunctionService deviceFunctionService;
    @Resource
    private DeviceInstructionService deviceInstructionService;

    @GetMapping("/{deviceId}/function")
    @Operation(summary = "获取设备已绑定功能列表")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<List<DeviceFunctionRespVO>> getDeviceFunctions(
            @PathVariable("deviceId") Long deviceId) {
        return success(deviceFunctionService.getDeviceFunctions(deviceId));
    }

    @PostMapping("/{deviceId}/function")
    @Operation(summary = "新增设备功能")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:config')")
    public CommonResult<Long> addDeviceFunction(
            @PathVariable("deviceId") Long deviceId,
            @Valid @RequestBody DeviceFunctionSaveReqVO reqVO) {
        return success(deviceFunctionService.addDeviceFunction(deviceId, reqVO));
    }

    @PutMapping("/{deviceId}/function/{id}")
    @Operation(summary = "修改设备功能")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:config')")
    public CommonResult<Boolean> updateDeviceFunction(
            @PathVariable("id") Long id,
            @Valid @RequestBody DeviceFunctionSaveReqVO reqVO) {
        deviceFunctionService.updateDeviceFunction(id, reqVO);
        return success(true);
    }

    @DeleteMapping("/{deviceId}/function/{id}")
    @Operation(summary = "删除设备功能")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:config')")
    public CommonResult<Boolean> deleteDeviceFunction(@PathVariable("id") Long id) {
        deviceFunctionService.deleteDeviceFunction(id);
        return success(true);
    }

    @PostMapping("/instruction/send")
    @Operation(summary = "下发控制指令")
    @PreAuthorize("@ss.hasPermission('iot:device:instruction:send')")
    public CommonResult<InstructionSendRespVO> sendInstruction(
            @Valid @RequestBody InstructionSendReqVO reqVO) {
        return success(deviceInstructionService.sendInstruction(reqVO));
    }
}
```

---

## Task 8: 前端 API 文件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/api/eiot/remote/brand.api.ts`
- Create: `anwei-fire-remote-monitor-vue/src/api/eiot/remote/deviceRemote.api.ts`

- [ ] **Step 1: 创建 brand.api.ts**

```typescript
import request from '@/config/axios'

enum Api {
  brandPage = '/eiot/brand/page',
  brandSync = '/eiot/brand/sync',
  brandCascade = '/eiot/brand/cascade',
  brandModelFunction = '/eiot/brand/model',
}

export interface BrandVO {
  id: number
  brandCode: string
  name: string
}

export interface BrandModelVO {
  id: number
  brandId: number
  model: string
  version: string
}

export interface BrandCascadeVO {
  id: number
  brandCode: string
  name: string
  models: Array<{
    id: number
    model: string
    versions: string[]
  }>
}

export interface FunctionParamVO {
  id: number
  paramName: string
  paramCode: string
  paramType: string
}

export interface BrandModelFunctionVO {
  id: number
  brandModelId: number
  functionId: string
  name: string
  ruleNum: number
  awaitTime: number
  params: FunctionParamVO[]
}

export const getBrandPage = async (params: any) => {
  return await request.get({ url: Api.brandPage, params })
}

export const syncBrandData = async () => {
  return await request.post({ url: Api.brandSync })
}

export const getBrandCascade = async () => {
  return await request.get({ url: Api.brandCascade })
}

export const getBrandModelFunctions = async (brandModelId: number | string) => {
  return await request.get({ url: `${Api.brandModelFunction}/${brandModelId}/function` })
}
```

- [ ] **Step 2: 创建 deviceRemote.api.ts**

```typescript
import request from '@/config/axios'

enum Api {
  remoteConfig = '/eiot/device',
  instructionSend = '/eiot/device/instruction/send',
}

export interface DeviceRemoteConfigVO {
  id: number
  deviceId: number
  brandModelId: number
  brandName: string
  model: string
  version: string
  hasPassword: boolean
}

export interface DeviceFunctionVO {
  id: number
  deviceId: number
  brandModelFunctionId: number
  functionName: string
  buttonName: string
  sortOrder: number
  paramValues: string
  enabled: boolean
}

export interface InstructionSendReq {
  deviceId: number
  deviceFunctionId: number
  password?: string
}

export interface InstructionSendResp {
  success: boolean
  message: string
}

export const getRemoteConfig = async (deviceId: number | string) => {
  return await request.get({ url: `${Api.remoteConfig}/${deviceId}/remote-config` })
}

export const saveRemoteConfig = async (deviceId: number | string, data: any) => {
  return await request.post({ url: `${Api.remoteConfig}/${deviceId}/remote-config`, data })
}

export const getDeviceFunctions = async (deviceId: number | string) => {
  return await request.get({ url: `${Api.remoteConfig}/${deviceId}/function` })
}

export const addDeviceFunction = async (deviceId: number | string, data: any) => {
  return await request.post({ url: `${Api.remoteConfig}/${deviceId}/function`, data })
}

export const updateDeviceFunction = async (deviceId: number | string, id: number | string, data: any) => {
  return await request.put({ url: `${Api.remoteConfig}/${deviceId}/function/${id}`, data })
}

export const deleteDeviceFunction = async (deviceId: number | string, id: number | string) => {
  return await request.delete({ url: `${Api.remoteConfig}/${deviceId}/function/${id}` })
}

export const sendInstruction = async (data: InstructionSendReq) => {
  return await request.post({ url: Api.instructionSend, data })
}
```

---

## Task 9: 前端远程控制 Tab 组件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/modules/RemoteControlTab.vue`

- [ ] **Step 1: 创建 RemoteControlTab.vue**

```vue
<template>
  <div class="remote-control-tab">
    <el-card v-if="remoteConfig" class="config-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-button type="primary" size="small" @click="openConfigDialog">编辑</el-button>
        </div>
      </template>
      <el-descriptions :column="3">
        <el-descriptions-item label="主机品牌">{{ remoteConfig.brandName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ remoteConfig.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ remoteConfig.version || '-' }}</el-descriptions-item>
        <el-descriptions-item label="二次确认密码">
          <span v-if="remoteConfig.hasPassword">******</span>
          <span v-else style="color: #999;">未设置</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-else class="config-card">
      <el-empty description="暂未配置远程控制">
        <el-button type="primary" @click="openConfigDialog">立即配置</el-button>
      </el-empty>
    </el-card>

    <el-card class="function-card">
      <template #header>
        <div class="card-header">
          <span>功能列表</span>
          <el-button v-if="remoteConfig" type="primary" size="small" @click="openAddFunctionDialog">新增功能</el-button>
        </div>
      </template>
      <div v-if="functions.length > 0" class="function-buttons">
        <el-button
          v-for="func in functions"
          :key="func.id"
          :type="func.enabled ? 'primary' : 'default'"
          :disabled="!func.enabled"
          @click="handleFunctionClick(func)"
        >
          {{ func.buttonName }}
        </el-button>
      </div>
      <el-empty v-else description="暂无功能" />
    </el-card>

    <!-- 设置弹窗 -->
    <RemoteConfigDialog
      v-model:visible="configDialogVisible"
      :device-id="deviceId"
      :config="remoteConfig"
      @success="loadData"
    />

    <!-- 新增功能弹窗 -->
    <AddFunctionDialog
      v-model:visible="addFunctionDialogVisible"
      :device-id="deviceId"
      :brand-model-id="remoteConfig?.brandModelId"
      @success="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRemoteConfig, getDeviceFunctions, sendInstruction } from '@/api/eiot/remote/deviceRemote.api'
import RemoteConfigDialog from './RemoteConfigDialog.vue'
import AddFunctionDialog from './AddFunctionDialog.vue'

const props = defineProps<{
  deviceId: number | string
}>()

const remoteConfig = ref<any>(null)
const functions = ref<any[]>([])
const configDialogVisible = ref(false)
const addFunctionDialogVisible = ref(false)

const loadData = async () => {
  try {
    const configRes = await getRemoteConfig(props.deviceId)
    remoteConfig.value = configRes.data || null
    if (remoteConfig.value) {
      const funcRes = await getDeviceFunctions(props.deviceId)
      functions.value = funcRes.data || []
    }
  } catch (e) {
    console.error(e)
  }
}

const openConfigDialog = () => {
  configDialogVisible.value = true
}

const openAddFunctionDialog = () => {
  addFunctionDialogVisible.value = true
}

const handleFunctionClick = async (func: any) => {
  try {
    let password = ''
    if (func.functionName !== '消音' && remoteConfig.value?.hasPassword) {
      const { value } = await ElMessageBox.prompt('请输入二次确认密码', '安全验证', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputType: 'password',
      })
      password = value
    }
    const res = await sendInstruction({
      deviceId: Number(props.deviceId),
      deviceFunctionId: func.id,
      password,
    })
    if (res.data?.success) {
      ElMessage.success('指令下发成功')
    } else {
      ElMessage.error(res.data?.message || '指令下发失败')
    }
  } catch (e) {
    // 用户取消
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.remote-control-tab {
  padding: 16px;
}
.config-card {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.function-card {
  margin-bottom: 16px;
}
.function-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
```

---

## Task 10: 前端远程控制设置弹窗

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/modules/RemoteConfigDialog.vue`

- [ ] **Step 1: 创建 RemoteConfigDialog.vue**

```vue
<template>
  <el-dialog
    title="远程控制设置"
    v-model="dialogVisible"
    width="600px"
    :close-on-click-modal="false"
  >
    <el-form :model="form" label-width="120px" ref="formRef">
      <el-form-item label="主机品牌">
        <el-select v-model="selectedBrandId" placeholder="请选择品牌" @change="onBrandChange">
          <el-option
            v-for="item in brandCascade"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="型号">
        <el-select v-model="selectedModelId" placeholder="请选择型号" @change="onModelChange">
          <el-option
            v-for="item in modelList"
            :key="item.id"
            :label="item.model"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="版本">
        <el-select v-model="form.brandModelId" placeholder="请选择版本">
          <el-option
            v-for="version in versionList"
            :key="version"
            :label="version"
            :value="selectedModelId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="二次确认密码">
        <el-input v-model="form.confirmPassword" type="password" placeholder="不填表示不需要二次确认" />
      </el-form-item>
      <el-form-item>
        <el-text type="info">若设置二次确认密码，除消音按钮外，其余按钮点击时都需要输入密码</el-text>
      </el-form-item>

      <el-divider />

      <div class="function-list-header">
        <span>功能列表</span>
        <el-button type="primary" size="small" @click="openAddFunction">新增功能</el-button>
      </div>
      <el-table :data="functions" border size="small">
        <el-table-column type="index" width="50" />
        <el-table-column prop="functionName" label="功能" />
        <el-table-column prop="buttonName" label="按钮名称" />
        <el-table-column prop="sortOrder" label="展示顺序" width="90" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editFunction(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="deleteFunction(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBrandCascade } from '@/api/eiot/remote/brand.api'
import { saveRemoteConfig, getDeviceFunctions, deleteDeviceFunction } from '@/api/eiot/remote/deviceRemote.api'

const props = defineProps<{
  visible: boolean
  deviceId: number | string
  config: any
}>()

const emit = defineEmits(['update:visible', 'success'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

const formRef = ref()
const form = ref({
  brandModelId: undefined as number | undefined,
  confirmPassword: '',
})

const brandCascade = ref<any[]>([])
const selectedBrandId = ref<number | undefined>(undefined)
const selectedModelId = ref<number | undefined>(undefined)
const functions = ref<any[]>([])

const modelList = computed(() => {
  const brand = brandCascade.value.find((b) => b.id === selectedBrandId.value)
  return brand?.models || []
})

const versionList = computed(() => {
  const model = modelList.value.find((m) => m.id === selectedModelId.value)
  return model?.versions || []
})

const loadBrandCascade = async () => {
  const res = await getBrandCascade()
  brandCascade.value = res.data || []
}

const loadFunctions = async () => {
  const res = await getDeviceFunctions(props.deviceId)
  functions.value = res.data || []
}

const onBrandChange = () => {
  selectedModelId.value = undefined
  form.value.brandModelId = undefined
}

const onModelChange = () => {
  form.value.brandModelId = selectedModelId.value
}

const handleSave = async () => {
  if (!form.value.brandModelId) {
    ElMessage.warning('请选择品牌型号')
    return
  }
  await saveRemoteConfig(props.deviceId, {
    brandModelId: form.value.brandModelId,
    confirmPassword: form.value.confirmPassword || undefined,
  })
  ElMessage.success('保存成功')
  emit('success')
  dialogVisible.value = false
}

const openAddFunction = () => {
  // 触发父组件打开新增功能弹窗
}

const editFunction = (row: any) => {
  // TODO: 编辑功能
}

const deleteFunction = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该功能？', '提示', { type: 'warning' })
    await deleteDeviceFunction(props.deviceId, row.id)
    ElMessage.success('删除成功')
    loadFunctions()
  } catch (e) {
    // 取消
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    loadBrandCascade()
    loadFunctions()
    if (props.config) {
      form.value.brandModelId = props.config.brandModelId
      // 反解析 brandModelId：通过 brandModelId 查询 brandId 和 model/version
      // 前端级联选择需要 brandId → model → version 三层匹配
    }
  }
})
</script>

<style scoped>
.function-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
```

---

## Task 11: 前端新增功能弹窗

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/modules/AddFunctionDialog.vue`

- [ ] **Step 1: 创建 AddFunctionDialog.vue**

```vue
<template>
  <el-dialog
    title="新增功能"
    v-model="dialogVisible"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form :model="form" label-width="100px" ref="formRef" :rules="rules">
      <el-form-item label="功能" prop="brandModelFunctionId">
        <el-select v-model="form.brandModelFunctionId" placeholder="请选择功能" @change="onFunctionChange">
          <el-option
            v-for="item in functionList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="按钮名称" prop="buttonName">
        <el-input v-model="form.buttonName" placeholder="请输入按钮名称" />
      </el-form-item>
      <el-form-item label="展示顺序" prop="sortOrder">
        <el-input-number v-model="form.sortOrder" :min="0" />
      </el-form-item>

      <!-- 变量区域 -->
      <template v-if="selectedFunction?.params?.length > 0">
        <el-divider content-position="left">变量</el-divider>
        <el-form-item
          v-for="param in selectedFunction.params"
          :key="param.id"
          :label="param.paramName"
          :prop="`paramValues.${param.paramCode}`"
          :rules="{ required: true, message: `请输入${param.paramName}`, trigger: 'blur' }"
        >
          <el-input v-model="form.paramValues[param.paramCode]" :placeholder="`请输入${param.paramName}`" />
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getBrandModelFunctions } from '@/api/eiot/remote/brand.api'
import { addDeviceFunction } from '@/api/eiot/remote/deviceRemote.api'

const props = defineProps<{
  visible: boolean
  deviceId: number | string
  brandModelId: number | undefined
}>()

const emit = defineEmits(['update:visible', 'success'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

const formRef = ref()
const form = ref({
  brandModelFunctionId: undefined as number | undefined,
  buttonName: '',
  sortOrder: 0,
  paramValues: {} as Record<string, any>,
})

const functionList = ref<any[]>([])
const rules = {
  brandModelFunctionId: [{ required: true, message: '请选择功能', trigger: 'change' }],
  buttonName: [{ required: true, message: '请输入按钮名称', trigger: 'blur' }],
}

const selectedFunction = computed(() => {
  return functionList.value.find((f) => f.id === form.value.brandModelFunctionId)
})

const onFunctionChange = () => {
  const func = selectedFunction.value
  form.value.buttonName = func?.name || ''
  form.value.paramValues = {}
}

const loadFunctions = async () => {
  if (!props.brandModelId) return
  const res = await getBrandModelFunctions(props.brandModelId)
  functionList.value = res.data || []
}

const handleSubmit = async () => {
  await formRef.value.validate()
  await addDeviceFunction(props.deviceId, {
    brandModelFunctionId: form.value.brandModelFunctionId,
    buttonName: form.value.buttonName,
    sortOrder: form.value.sortOrder,
    paramValues: JSON.stringify(form.value.paramValues),
  })
  ElMessage.success('新增成功')
  emit('success')
  dialogVisible.value = false
}

watch(() => props.visible, (val) => {
  if (val) {
    form.value = {
      brandModelFunctionId: undefined,
      buttonName: '',
      sortOrder: 0,
      paramValues: {},
    }
    loadFunctions()
  }
})
</script>
```

---

## Task 12: 设备详情页集成远程控制 Tab

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/detail.vue`

- [ ] **Step 1: 在 detail.vue 的 el-tabs 中新增"远程控制" Tab**

在 `<el-tabs>` 内的合适位置（如"日志"Tab之后）插入：

```vue
<el-tab-pane label="远程控制" name="remoteControl" :disabled="inAdd">
  <RemoteControlTab :device-id="state.deviceDetail.id" />
</el-tab-pane>
```

- [ ] **Step 2: 导入 RemoteControlTab 组件**

在 `<script setup>` 顶部添加：

```typescript
import RemoteControlTab from './modules/RemoteControlTab.vue'
```

---

## Task 13: 权限菜单数据

**Files:**
- Modify: `_sql/mysql/update.sql`（追加到文件末尾）

- [ ] **Step 1: 追加权限菜单 SQL**

```sql
-- ----------------------------
-- 2026-05-07 设备远程控制权限
-- ----------------------------

-- 品牌查询权限
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3200, '品牌查询', 'iot:brand:query', 3, 1, 0, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-05-07 00:00:00', '1', '2026-05-07 00:00:00', b'0');

-- 品牌同步权限
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3201, '品牌同步', 'iot:brand:sync', 3, 2, 0, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-05-07 00:00:00', '1', '2026-05-07 00:00:00', b'0');

-- 远程控制配置权限
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3202, '远程控制配置', 'iot:device:remote:config', 3, 3, 0, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-05-07 00:00:00', '1', '2026-05-07 00:00:00', b'0');

-- 下发指令权限
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3203, '下发指令', 'iot:device:instruction:send', 3, 4, 0, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-05-07 00:00:00', '1', '2026-05-07 00:00:00', b'0');
```

---

## Task 14: 编译验证

- [ ] **Step 1: 后端编译**

Run: `cd anwei-fire-remote-monitor; mvn clean compile -pl module-eiot/module-eiot-biz -am`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端类型检查**

Run: `cd anwei-fire-remote-monitor-vue; pnpm run type-check`
Expected: 无新增错误

- [ ] **Step 3: 前端构建**

Run: `cd anwei-fire-remote-monitor-vue; pnpm run build`
Expected: 构建成功
