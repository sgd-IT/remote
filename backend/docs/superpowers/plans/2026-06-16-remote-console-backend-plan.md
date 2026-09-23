# 远程控制台后端接口与后台管理实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现前端远程控制台所需的全部后端接口，并提供远程控制操作日志的后台管理页面。

**Architecture:** 基于现有 `DeviceFunctionController` 和 `DeviceInstructionServiceImpl` 扩展，新增 `RemoteConsoleController` 提供控制台只读接口，新增 `RemoteOperationLog` 表记录所有远程控制操作，后台管理页通过 `RemoteOperationLogController` 提供分页和导出。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, MySQL, Vue 3, TypeScript, Element Plus

---

## 文件结构总览

### 后端新增文件

| 文件 | 职责 |
|------|------|
| `RemoteOperationLogDO.java` | 操作日志数据对象 |
| `RemoteOperationLogMapper.java` | 操作日志 Mapper |
| `RemoteOperationLogRespVO.java` | 操作日志响应 VO |
| `RemoteOperationLogPageReqVO.java` | 操作日志分页请求 VO |
| `RemoteConsoleDeviceTreeRespVO.java` | 设备树响应 VO |
| `RemoteConsoleDeviceTreeItemRespVO.java` | 设备树设备项 VO |
| `RemoteConsoleDeviceDetailRespVO.java` | 设备详情响应 VO |
| `RemoteConsoleDeviceParamRespVO.java` | 设备参数响应 VO |
| `RemoteConsoleDeviceStatusStatsRespVO.java` | 状态统计响应 VO |
| `SwitchDeviceModeReqVO.java` | 模式切换请求 VO |
| `RemoteConsoleController.java` | 远程控制台 Controller |
| `RemoteOperationLogController.java` | 操作日志管理 Controller |
| `RemoteConsoleService.java` | 远程控制台 Service 接口 |
| `RemoteConsoleServiceImpl.java` | 远程控制台 Service 实现 |
| `RemoteOperationLogService.java` | 操作日志 Service 接口 |
| `RemoteOperationLogServiceImpl.java` | 操作日志 Service 实现 |
| `RemoteOperationLogConvert.java` | 操作日志转换器 |
| `_sql/mysql/remote/remote_operation_log.sql` | 建表 SQL |

### 后端修改文件

| 文件 | 修改内容 |
|------|---------|
| `DeviceInstructionServiceImpl.java` | 在 `sendInstruction()` 前后增加操作日志记录 |

### 前端新增/修改文件

| 文件 | 职责 |
|------|------|
| `remoteConsole.api.ts` | 调整接口定义 |
| `DeviceTree.vue` | 移除 Mock 数据 |
| `ControlPanel.vue` | 移除 Mock 数据 |
| `operation-log/index.vue` | 后台管理页面 |
| `remaining.ts` | 注册后台管理路由 |

---

## Task 1: 创建操作日志数据库表

**Files:**
- Create: `anwei-fire-remote-monitor/_sql/mysql/remote/remote_operation_log.sql`

- [ ] **Step 1: 编写建表 SQL**

```sql
CREATE TABLE IF NOT EXISTS `eiot_remote_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '设备名称',
  `property_id` BIGINT DEFAULT NULL COMMENT '企业/物业ID',
  `property_name` VARCHAR(64) DEFAULT NULL COMMENT '企业/物业名称',
  `operation_type` VARCHAR(32) NOT NULL COMMENT '操作类型：MUTE/RESET/SWITCH_MODE/START/STOP',
  `operation_name` VARCHAR(64) NOT NULL COMMENT '操作名称',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作人姓名',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '复核人ID',
  `reviewer_name` VARCHAR(64) DEFAULT NULL COMMENT '复核人姓名',
  `command_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=失败 1=成功 2=执行中',
  `result_msg` VARCHAR(512) DEFAULT NULL COMMENT '结果说明',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数JSON',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '操作IP',
  `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id_operation_time` (`device_id`, `operation_time`),
  KEY `idx_tenant_id_operation_time` (`tenant_id`, `operation_time`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='远程控制操作日志';
```

- [ ] **Step 2: 在本地或目标环境执行 SQL**

Run: 通过 MySQL 客户端执行该 SQL 文件
Expected: 表创建成功

- [ ] **Step 3: Commit**

```bash
git add anwei-fire-remote-monitor/_sql/mysql/remote/remote_operation_log.sql
git commit -m "feat(remote): add remote operation log table sql"
```

---

## Task 2: 创建操作日志 DO 和 Mapper

**Files:**
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/remote/RemoteOperationLogDO.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/remote/RemoteOperationLogMapper.java`

- [ ] **Step 1: 创建 DO**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.dal.dataobject.remote;

import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("eiot_remote_operation_log")
@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteOperationLogDO extends BaseDO {

    private Long id;
    private Long tenantId;
    private Long deviceId;
    private String deviceName;
    private Long propertyId;
    private String propertyName;
    private String operationType;
    private String operationName;
    private Long operatorId;
    private String operatorName;
    private Long reviewerId;
    private String reviewerName;
    private Integer commandStatus;
    private String resultMsg;
    private String requestParams;
    private String ipAddress;
    private LocalDateTime operationTime;
}
```

- [ ] **Step 2: 创建 Mapper**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.dal.mysql.remote;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RemoteOperationLogMapper extends BaseMapperX<RemoteOperationLogDO> {
}
```

- [ ] **Step 3: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/remote/RemoteOperationLogDO.java
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/remote/RemoteOperationLogMapper.java
git commit -m "feat(remote): add RemoteOperationLog DO and Mapper"
```

---

## Task 3: 创建响应 VO 和请求 VO

**Files:**
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteOperationLogRespVO.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteOperationLogPageReqVO.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteConsoleDeviceTreeRespVO.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteConsoleDeviceTreeItemRespVO.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteConsoleDeviceDetailRespVO.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteConsoleDeviceParamRespVO.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/RemoteConsoleDeviceStatusStatsRespVO.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/SwitchDeviceModeReqVO.java`

- [ ] **Step 1: 创建 RemoteOperationLogRespVO**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "远程控制操作日志响应 VO")
public class RemoteOperationLogRespVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "企业/物业ID")
    private Long propertyId;

    @Schema(description = "企业/物业名称")
    private String propertyName;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作名称")
    private String operationName;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "复核人ID")
    private Long reviewerId;

    @Schema(description = "复核人姓名")
    private String reviewerName;

    @Schema(description = "命令状态：0=失败 1=成功 2=执行中")
    private Integer commandStatus;

    @Schema(description = "结果说明")
    private String resultMsg;

    @Schema(description = "请求参数JSON")
    private String requestParams;

    @Schema(description = "操作IP")
    private String ipAddress;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
}
```

- [ ] **Step 2: 创建 RemoteOperationLogPageReqVO**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "远程控制操作日志分页请求 VO")
public class RemoteOperationLogPageReqVO extends PageParam {

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "企业/物业ID")
    private Long propertyId;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "命令状态")
    private Integer commandStatus;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
```

- [ ] **Step 3: 创建设备树 VO**

`RemoteConsoleDeviceTreeItemRespVO.java`:

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "远程控制台设备树设备项 VO")
public class RemoteConsoleDeviceTreeItemRespVO {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "在线状态：0=离线 1=在线")
    private Integer state;

    @Schema(description = "告警状态：alarm/fault/online/offline")
    private String alarmStatus;
}
```

`RemoteConsoleDeviceTreeRespVO.java`:

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "远程控制台设备树响应 VO")
public class RemoteConsoleDeviceTreeRespVO {

    @Schema(description = "企业/物业ID")
    private Long id;

    @Schema(description = "企业/物业名称")
    private String name;

    @Schema(description = "风险等级：danger/warning/normal")
    private String riskLevel;

    @Schema(description = "设备数量")
    private Integer deviceCount;

    @Schema(description = "设备列表")
    private List<RemoteConsoleDeviceTreeItemRespVO> devices;
}
```

- [ ] **Step 4: 创建设备详情 VO**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "远程控制台设备详情响应 VO")
public class RemoteConsoleDeviceDetailRespVO {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "在线状态：0=离线 1=在线")
    private Integer state;

    @Schema(description = "告警状态")
    private String alarmStatus;

    @Schema(description = "控制模式：manual/auto")
    private String mode;
}
```

- [ ] **Step 5: 创建设备参数 VO**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "远程控制台设备参数响应 VO")
public class RemoteConsoleDeviceParamRespVO {

    @Schema(description = "参数编码")
    private String code;

    @Schema(description = "参数名称")
    private String name;

    @Schema(description = "参数值")
    private Object value;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "图标")
    private String icon;
}
```

- [ ] **Step 6: 创建状态统计 VO**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "远程控制台设备状态统计响应 VO")
public class RemoteConsoleDeviceStatusStatsRespVO {

    @Schema(description = "设备总数")
    private Integer total;

    @Schema(description = "在线设备数")
    private Integer online;

    @Schema(description = "告警设备数")
    private Integer alarm;

    @Schema(description = "故障设备数")
    private Integer fault;

    @Schema(description = "离线设备数")
    private Integer offline;
}
```

- [ ] **Step 7: 创建模式切换请求 VO**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "切换手自动模式请求 VO")
public class SwitchDeviceModeReqVO {

    @NotBlank(message = "模式不能为空")
    @Schema(description = "模式：manual/auto")
    private String mode;

    @Schema(description = "切换原因")
    private String reason;
}
```

- [ ] **Step 8: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/vo/
git commit -m "feat(remote): add remote console response VOs"
```

---

## Task 4: 创建转换器

**Files:**
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/convert/remote/RemoteOperationLogConvert.java`

- [ ] **Step 1: 创建转换器**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.convert.remote;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface RemoteOperationLogConvert {

    RemoteOperationLogConvert INSTANCE = Mappers.getMapper(RemoteOperationLogConvert.class);

    RemoteOperationLogRespVO convert(RemoteOperationLogDO bean);

    List<RemoteOperationLogRespVO> convertList(List<RemoteOperationLogDO> list);

    PageResult<RemoteOperationLogRespVO> convertPage(PageResult<RemoteOperationLogDO> page);
}
```

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/convert/remote/RemoteOperationLogConvert.java
git commit -m "feat(remote): add RemoteOperationLogConvert"
```

---

## Task 5: 创建操作日志 Service

**Files:**
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteOperationLogService.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteOperationLogServiceImpl.java`

- [ ] **Step 1: 创建 Service 接口**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.service.remote;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;

import java.util.List;

public interface RemoteOperationLogService {

    /**
     * 创建操作日志
     */
    Long createLog(RemoteOperationLogDO log);

    /**
     * 分页查询操作日志
     */
    PageResult<RemoteOperationLogRespVO> getOperationLogPage(RemoteOperationLogPageReqVO pageReqVO);

    /**
     * 查询设备的最近操作日志
     */
    List<RemoteOperationLogRespVO> getRecentLogsByDeviceId(Long deviceId, Integer limit);
}
```

- [ ] **Step 2: 创建 Service 实现**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.service.remote;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogRespVO;
import com.enjoyiot.module.eiot.convert.remote.RemoteOperationLogConvert;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;
import com.enjoyiot.module.eiot.dal.mysql.remote.RemoteOperationLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RemoteOperationLogServiceImpl implements RemoteOperationLogService {

    @Resource
    private RemoteOperationLogMapper remoteOperationLogMapper;

    @Override
    public Long createLog(RemoteOperationLogDO log) {
        remoteOperationLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public PageResult<RemoteOperationLogRespVO> getOperationLogPage(RemoteOperationLogPageReqVO pageReqVO) {
        LambdaQueryWrapperX<RemoteOperationLogDO> wrapper = new LambdaQueryWrapperX<>()
                .eqIfPresent(RemoteOperationLogDO::getDeviceId, pageReqVO.getDeviceId())
                .eqIfPresent(RemoteOperationLogDO::getPropertyId, pageReqVO.getPropertyId())
                .eqIfPresent(RemoteOperationLogDO::getOperationType, pageReqVO.getOperationType())
                .eqIfPresent(RemoteOperationLogDO::getOperatorId, pageReqVO.getOperatorId())
                .eqIfPresent(RemoteOperationLogDO::getCommandStatus, pageReqVO.getCommandStatus())
                .betweenIfPresent(RemoteOperationLogDO::getOperationTime, pageReqVO.getStartTime(), pageReqVO.getEndTime())
                .orderByDesc(RemoteOperationLogDO::getOperationTime);
        PageResult<RemoteOperationLogDO> pageResult = remoteOperationLogMapper.selectPage(pageReqVO, wrapper);
        return RemoteOperationLogConvert.INSTANCE.convertPage(pageResult);
    }

    @Override
    public List<RemoteOperationLogRespVO> getRecentLogsByDeviceId(Long deviceId, Integer limit) {
        LambdaQueryWrapperX<RemoteOperationLogDO> wrapper = new LambdaQueryWrapperX<>()
                .eq(RemoteOperationLogDO::getDeviceId, deviceId)
                .orderByDesc(RemoteOperationLogDO::getOperationTime)
                .last("LIMIT " + limit);
        List<RemoteOperationLogDO> list = remoteOperationLogMapper.selectList(wrapper);
        return RemoteOperationLogConvert.INSTANCE.convertList(list);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteOperationLogService.java
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteOperationLogServiceImpl.java
git commit -m "feat(remote): add RemoteOperationLogService"
```

---

## Task 6: 创建远程控制台 Service

**Files:**
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteConsoleService.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteConsoleServiceImpl.java`

- [ ] **Step 1: 创建 Service 接口**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.service.remote;

import com.enjoyiot.module.eiot.controller.admin.remote.vo.*;

import java.util.List;

public interface RemoteConsoleService {

    /**
     * 获取设备树
     */
    List<RemoteConsoleDeviceTreeRespVO> getDeviceTree();

    /**
     * 获取设备详情
     */
    RemoteConsoleDeviceDetailRespVO getDeviceDetail(Long deviceId);

    /**
     * 获取设备参数
     */
    List<RemoteConsoleDeviceParamRespVO> getDeviceParams(Long deviceId);

    /**
     * 获取设备状态统计
     */
    RemoteConsoleDeviceStatusStatsRespVO getDeviceStatusStats();

    /**
     * 切换手自动模式
     */
    Boolean switchDeviceMode(Long deviceId, SwitchDeviceModeReqVO reqVO);
}
```

- [ ] **Step 2: 创建 Service 实现（占位实现）**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.service.remote;

import cn.hutool.json.JSONUtil;
import com.enjoyiot.eiot.api.device.DeviceService;
import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.framework.common.exception.ServiceException;
import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.DeviceProperty;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;
import com.enjoyiot.module.eiot.service.device.DeviceInfoService;
import com.enjoyiot.module.eiot.service.device.DeviceInstructionService;
import com.enjoyiot.module.eiot.service.device.DeviceManagerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class RemoteConsoleServiceImpl implements RemoteConsoleService {

    @Resource
    private DeviceInfoService deviceInfoService;

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceManagerService deviceManagerService;

    @Resource
    private RemoteOperationLogService remoteOperationLogService;

    @Resource
    private DeviceInstructionService deviceInstructionService;

    @Override
    public List<RemoteConsoleDeviceTreeRespVO> getDeviceTree() {
        // TODO: 按租户/企业分组查询设备，实现前确认 deviceInfoService 是否支持列表查询
        // 当前返回空列表，由实现者补充
        return Collections.emptyList();
    }

    @Override
    public RemoteConsoleDeviceDetailRespVO getDeviceDetail(Long deviceId) {
        DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo(deviceId);
        if (deviceInfo == null) {
            throw new ServiceException(500, "设备不存在");
        }
        RemoteConsoleDeviceDetailRespVO vo = new RemoteConsoleDeviceDetailRespVO();
        vo.setId(deviceInfo.getId());
        vo.setName(deviceInfo.getName());
        vo.setState(deviceInfo.getState());
        vo.setMode(getDeviceControlMode(deviceId));
        vo.setAlarmStatus(getAlarmStatus(deviceInfo));
        return vo;
    }

    @Override
    public List<RemoteConsoleDeviceParamRespVO> getDeviceParams(Long deviceId) {
        // 从设备属性读取预定义参数
        List<RemoteConsoleDeviceParamRespVO> result = new ArrayList<>();
        addParam(result, deviceId, "temperature", "当前温度", "°C", "🌡️");
        addParam(result, deviceId, "humidity", "当前湿度", "%", "💧");
        addParam(result, deviceId, "voltage", "电压", "V", "⚡");
        addParam(result, deviceId, "battery", "电池电量", "%", "🔋");
        addParam(result, deviceId, "signal", "信号强度", "dBm", "📶");
        addParam(result, deviceId, "runtime", "运行时长", "h", "⏱️");
        return result;
    }

    @Override
    public RemoteConsoleDeviceStatusStatsRespVO getDeviceStatusStats() {
        // TODO: 根据实际设备统计实现
        RemoteConsoleDeviceStatusStatsRespVO vo = new RemoteConsoleDeviceStatusStatsRespVO();
        vo.setTotal(0);
        vo.setOnline(0);
        vo.setAlarm(0);
        vo.setFault(0);
        vo.setOffline(0);
        return vo;
    }

    @Override
    public Boolean switchDeviceMode(Long deviceId, SwitchDeviceModeReqVO reqVO) {
        DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo(deviceId);
        if (deviceInfo == null) {
            throw new ServiceException(500, "设备不存在");
        }

        // TODO: 调用设备服务下发模式切换命令
        boolean success = true;

        // 记录操作日志
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        RemoteOperationLogDO log = new RemoteOperationLogDO();
        log.setTenantId(deviceInfo.getTenantId());
        log.setDeviceId(deviceId);
        log.setDeviceName(deviceInfo.getName());
        log.setOperationType("SWITCH_MODE");
        log.setOperationName("切换" + ("auto".equals(reqVO.getMode()) ? "自动" : "手动") + "模式");
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setCommandStatus(success ? 1 : 0);
        log.setResultMsg(success ? "切换成功" : "切换失败");
        log.setRequestParams(JSONUtil.toJsonStr(reqVO));
        log.setOperationTime(LocalDateTime.now());
        remoteOperationLogService.createLog(log);

        return success;
    }

    private void addParam(List<RemoteConsoleDeviceParamRespVO> result, Long deviceId, String code, String name, String unit, String icon) {
        // TODO: 从 deviceManagerService 读取设备属性
        RemoteConsoleDeviceParamRespVO vo = new RemoteConsoleDeviceParamRespVO();
        vo.setCode(code);
        vo.setName(name);
        vo.setValue(null);
        vo.setUnit(unit);
        vo.setIcon(icon);
        result.add(vo);
    }

    private String getDeviceControlMode(Long deviceId) {
        // TODO: 从设备属性读取 control_mode
        return "manual";
    }

    private String getAlarmStatus(DeviceInfo deviceInfo) {
        // TODO: 根据设备实际告警状态判断
        if (deviceInfo.getState() == null || deviceInfo.getState() == 0) {
            return "offline";
        }
        return "online";
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteConsoleService.java
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteConsoleServiceImpl.java
git commit -m "feat(remote): add RemoteConsoleService skeleton"
```

---

## Task 7: 创建 Controller

**Files:**
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/RemoteConsoleController.java`
- Create: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/RemoteOperationLogController.java`

- [ ] **Step 1: 创建 RemoteConsoleController**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.*;
import com.enjoyiot.module.eiot.service.remote.RemoteConsoleService;
import com.enjoyiot.module.eiot.service.remote.RemoteOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 远程控制台")
@RestController
@RequestMapping("/eiot/remote-console")
public class RemoteConsoleController {

    @Resource
    private RemoteConsoleService remoteConsoleService;

    @Resource
    private RemoteOperationLogService remoteOperationLogService;

    @GetMapping("/device-tree")
    @Operation(summary = "获取设备树")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<List<RemoteConsoleDeviceTreeRespVO>> getDeviceTree() {
        return success(remoteConsoleService.getDeviceTree());
    }

    @GetMapping("/device/{deviceId}/detail")
    @Operation(summary = "获取设备详情")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<RemoteConsoleDeviceDetailRespVO> getDeviceDetail(
            @PathVariable("deviceId") Long deviceId) {
        return success(remoteConsoleService.getDeviceDetail(deviceId));
    }

    @GetMapping("/device/{deviceId}/params")
    @Operation(summary = "获取设备参数")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<List<RemoteConsoleDeviceParamRespVO>> getDeviceParams(
            @PathVariable("deviceId") Long deviceId) {
        return success(remoteConsoleService.getDeviceParams(deviceId));
    }

    @GetMapping("/device/{deviceId}/operation-logs")
    @Operation(summary = "获取设备操作日志")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<List<RemoteOperationLogRespVO>> getDeviceOperationLogs(
            @PathVariable("deviceId") Long deviceId,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return success(remoteOperationLogService.getRecentLogsByDeviceId(deviceId, limit));
    }

    @GetMapping("/device-status-stats")
    @Operation(summary = "获取设备状态统计")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<RemoteConsoleDeviceStatusStatsRespVO> getDeviceStatusStats() {
        return success(remoteConsoleService.getDeviceStatusStats());
    }

    @PostMapping("/device/{deviceId}/mode")
    @Operation(summary = "切换手自动模式")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:control')")
    public CommonResult<Boolean> switchDeviceMode(
            @PathVariable("deviceId") Long deviceId,
            @Valid @RequestBody SwitchDeviceModeReqVO reqVO) {
        return success(remoteConsoleService.switchDeviceMode(deviceId, reqVO));
    }
}
```

- [ ] **Step 2: 创建 RemoteOperationLogController**

```java
/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.remote;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.excel.core.util.ExcelUtils;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogRespVO;
import com.enjoyiot.module.eiot.service.remote.RemoteOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 远程控制日志")
@RestController
@RequestMapping("/eiot/remote-console/operation-log")
public class RemoteOperationLogController {

    @Resource
    private RemoteOperationLogService remoteOperationLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询操作日志")
    @PreAuthorize("@ss.hasPermission('iot:remote-operation-log:query')")
    public CommonResult<PageResult<RemoteOperationLogRespVO>> getOperationLogPage(
            @Valid RemoteOperationLogPageReqVO pageReqVO) {
        return success(remoteOperationLogService.getOperationLogPage(pageReqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出操作日志")
    @PreAuthorize("@ss.hasPermission('iot:remote-operation-log:export')")
    public void exportOperationLog(HttpServletResponse response,
                                    @Valid RemoteOperationLogPageReqVO pageReqVO) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<RemoteOperationLogRespVO> pageResult = remoteOperationLogService.getOperationLogPage(pageReqVO);
        ExcelUtils.write(response, "远程控制操作日志.xlsx", "日志",
                RemoteOperationLogRespVO.class, pageResult.getList());
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/remote/
git commit -m "feat(remote): add RemoteConsoleController and RemoteOperationLogController"
```

---

## Task 8: 增强指令下发日志记录

**Files:**
- Modify: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInstructionServiceImpl.java`

- [ ] **Step 1: 注入 RemoteOperationLogService**

在 `DeviceInstructionServiceImpl` 中新增：

```java
@Resource
private RemoteOperationLogService remoteOperationLogService;
```

- [ ] **Step 2: 在 sendInstruction 方法中记录日志**

在方法开头（参数校验后）添加日志记录，在方法返回前更新日志结果。

```java
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

    DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo(reqVO.getDeviceId());
    Long operatorId = SecurityFrameworkUtils.getLoginUserId();
    String operatorName = SecurityFrameworkUtils.getLoginUserNickname();

    // 创建操作日志
    RemoteOperationLogDO operationLog = new RemoteOperationLogDO();
    operationLog.setTenantId(deviceInfo != null ? deviceInfo.getTenantId() : null);
    operationLog.setDeviceId(reqVO.getDeviceId());
    operationLog.setDeviceName(deviceInfo != null ? deviceInfo.getName() : "");
    operationLog.setOperationType(mapOperationType(functionDef.getName(), reqVO.getCommandType()));
    operationLog.setOperationName(buildOperationName(functionDef.getName(), reqVO.getCommandType()));
    operationLog.setOperatorId(operatorId);
    operationLog.setOperatorName(operatorName);
    operationLog.setCommandStatus(2); // 执行中
    operationLog.setRequestParams(JSONUtil.toJsonStr(reqVO));
    operationLog.setOperationTime(LocalDateTime.now());
    remoteOperationLogService.createLog(operationLog);

    // ... 原有业务逻辑 ...

    InstructionSendRespVO resp;
    try {
        resp = doSendInstruction(reqVO, deviceFunction, functionDef);
    } catch (Exception e) {
        resp = buildResp(false, "指令下发异常: " + e.getMessage());
    }

    // 更新日志结果
    operationLog.setCommandStatus(resp.getSuccess() ? 1 : 0);
    operationLog.setResultMsg(resp.getMessage());
    remoteOperationLogMapper.updateById(operationLog);

    return resp;
}
```

- [ ] **Step 3: 提取原 sendInstruction 核心逻辑到 doSendInstruction**

将原 `sendInstruction` 方法体（从参数校验后到返回前）抽取为 `private InstructionSendRespVO doSendInstruction(InstructionSendReqVO reqVO, DeviceFunctionDO deviceFunction, BrandModelFunctionDO functionDef)`。

- [ ] **Step 4: 添加辅助方法**

```java
private String mapOperationType(String functionName, String commandType) {
    if (functionName.contains("消音")) return "MUTE";
    if (functionName.contains("复位")) return "RESET";
    if (functionName.contains("模式")) return "SWITCH_MODE";
    if ("start".equalsIgnoreCase(commandType) || "启动".equals(commandType)) return "START";
    if ("stop".equalsIgnoreCase(commandType) || "停止".equals(commandType)) return "STOP";
    return "OTHER";
}

private String buildOperationName(String functionName, String commandType) {
    if (functionName.contains("消音")) return "消音";
    if (functionName.contains("复位")) return "复位";
    if (functionName.contains("模式")) return "切换模式";
    if ("start".equalsIgnoreCase(commandType) || "启动".equals(commandType)) return functionName + "-启动";
    if ("stop".equalsIgnoreCase(commandType) || "停止".equals(commandType)) return functionName + "-停止";
    return functionName;
}
```

- [ ] **Step 5: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInstructionServiceImpl.java
git commit -m "feat(remote): log instruction send operations"
```

---

## Task 9: 实现设备树和状态统计

**Files:**
- Modify: `anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteConsoleServiceImpl.java`

- [ ] **Step 1: 实现 getDeviceTree**

根据项目实际情况，查询 `DeviceInfo` 列表，按 `tenantId` 分组。示例逻辑：

```java
@Override
public List<RemoteConsoleDeviceTreeRespVO> getDeviceTree() {
    // 查询当前租户下的所有设备（或忽略租户查询所有设备）
    List<DeviceInfo> devices = deviceInfoService.getDeviceInfoList(); // 需要确认方法名
    Map<Long, List<DeviceInfo>> grouped = devices.stream()
            .collect(Collectors.groupingBy(d -> d.getTenantId() != null ? d.getTenantId() : 0L));

    List<RemoteConsoleDeviceTreeRespVO> result = new ArrayList<>();
    grouped.forEach((tenantId, list) -> {
        RemoteConsoleDeviceTreeRespVO node = new RemoteConsoleDeviceTreeRespVO();
        node.setId(tenantId);
        node.setName(getTenantName(tenantId));
        node.setDeviceCount(list.size());
        node.setRiskLevel(calculateRiskLevel(list));
        node.setDevices(list.stream().map(this::convertDeviceItem).collect(Collectors.toList()));
        result.add(node);
    });
    return result;
}

private RemoteConsoleDeviceTreeItemRespVO convertDeviceItem(DeviceInfo device) {
    RemoteConsoleDeviceTreeItemRespVO item = new RemoteConsoleDeviceTreeItemRespVO();
    item.setId(device.getId());
    item.setName(device.getName());
    item.setState(device.getState());
    item.setAlarmStatus(getAlarmStatus(device));
    return item;
}

private String calculateRiskLevel(List<DeviceInfo> devices) {
    boolean hasAlarm = devices.stream().anyMatch(d -> "alarm".equals(getAlarmStatus(d)));
    if (hasAlarm) return "danger";
    boolean hasFault = devices.stream().anyMatch(d -> "fault".equals(getAlarmStatus(d)));
    if (hasFault) return "warning";
    return "normal";
}
```

- [ ] **Step 2: 实现 getDeviceStatusStats**

```java
@Override
public RemoteConsoleDeviceStatusStatsRespVO getDeviceStatusStats() {
    List<DeviceInfo> devices = deviceInfoService.getDeviceInfoList();
    RemoteConsoleDeviceStatusStatsRespVO vo = new RemoteConsoleDeviceStatusStatsRespVO();
    vo.setTotal(devices.size());
    vo.setOnline((int) devices.stream().filter(d -> d.getState() != null && d.getState() == 1).count());
    vo.setOffline((int) devices.stream().filter(d -> d.getState() == null || d.getState() == 0).count());
    // TODO: 告警/故障需要从告警记录或设备属性中统计
    vo.setAlarm(0);
    vo.setFault(0);
    return vo;
}
```

- [ ] **Step 3: Commit**

```bash
git add anwei-fire-remote-monitor/module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/remote/RemoteConsoleServiceImpl.java
git commit -m "feat(remote): implement device tree and status stats"
```

---

## Task 10: 编译后端项目

**Files:**
- All modified Java files

- [ ] **Step 1: 运行 Maven 编译**

Run:
```bash
cd anwei-fire-remote-monitor
mvn clean compile -pl module-eiot/module-eiot-biz -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 修复编译错误**

根据编译错误修复：
- 缺少的 import
- 方法不存在（如 `getDeviceInfoList`）
- 类型不匹配

- [ ] **Step 3: Commit 修复**

```bash
git add -A
git commit -m "fix(remote): fix compile errors"
```

---

## Task 11: 调整前端 API 层

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/api/eiot/remote/remoteConsole.api.ts`

- [ ] **Step 1: 调整接口定义**

```typescript
// 远程控制台 API
import request from '@/config/axios'

// ... 类型定义保持不变 ...

/** 获取设备树（按企业/物业分组） */
export const getDeviceTree = async (): Promise<DeviceTreeNodeVO[]> => {
  return await request.get({ url: '/eiot/remote-console/device-tree' })
}

/** 获取设备详情 */
export const getDeviceDetail = async (deviceId: number): Promise<DeviceDetailVO> => {
  return await request.get({ url: `/eiot/remote-console/device/${deviceId}/detail` })
}

/** 获取设备实时参数 */
export const getDeviceParams = async (deviceId: number): Promise<DeviceParamVO[]> => {
  return await request.get({ url: `/eiot/remote-console/device/${deviceId}/params` })
}

/** 获取设备操作日志 */
export const getOperationLogs = async (deviceId: number, limit: number = 20) => {
  return await request.get({
    url: `/eiot/remote-console/device/${deviceId}/operation-logs`,
    params: { limit }
  })
}

/** 获取设备状态统计 */
export const getDeviceStatusStats = async (): Promise<DeviceStatusStatsVO> => {
  return await request.get({ url: '/eiot/remote-console/device-status-stats' })
}

/** 切换手自动模式 */
export const switchDeviceMode = async (deviceId: number, mode: 'manual' | 'auto', reason?: string) => {
  return await request.post({
    url: `/eiot/remote-console/device/${deviceId}/mode`,
    data: { mode, reason }
  })
}

/** 后台管理 - 操作日志分页 */
export const getOperationLogPage = async (params: any) => {
  return await request.get({ url: '/eiot/remote-console/operation-log/page', params })
}

/** 后台管理 - 操作日志导出 */
export const exportOperationLog = async (params: any) => {
  return await request.download({ url: '/eiot/remote-console/operation-log/export', params })
}
```

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/api/eiot/remote/remoteConsole.api.ts
git commit -m "feat(remote): update frontend api definitions"
```

---

## Task 12: 移除前端 Mock 数据

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/DeviceTree.vue`
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/ControlPanel.vue`

- [ ] **Step 1: 修改 DeviceTree.vue**

在 `fetchData` 中，API 失败时不再调用 `loadMockData()`，而是显示空状态：

```typescript
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDeviceTree()
    if (res && Array.isArray(res) && res.length > 0) {
      treeData.value = res
      if (res.length > 0) {
        expandedMap[res[0].id] = true
      }
    }
  } catch (err) {
    console.error('获取设备树失败:', err)
  } finally {
    loading.value = false
  }
}
```

删除 `loadMockData()` 方法。

- [ ] **Step 2: 修改 ControlPanel.vue**

在 `fetchFunctions`、`fetchParams`、`fetchLogs`、`fetchStats` 中，API 失败时不再使用 Mock 数据：

```typescript
const fetchFunctions = async () => {
  functionsLoading.value = true
  try {
    const res = await request.get({ url: `/eiot/device/${props.deviceId}/function` })
    const list = Array.isArray(res) ? res : (res as any)?.list || []
    deviceFunctions.value = list
  } catch (err) {
    console.error('获取设备功能失败:', err)
    deviceFunctions.value = []
  } finally {
    functionsLoading.value = false
  }
}
```

删除 `getMockFunctions()`、`getMockParams()`、`getMockLogs()` 方法。

- [ ] **Step 3: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/DeviceTree.vue
git add anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/remote-console/components/ControlPanel.vue
git commit -m "feat(remote): remove mock data from remote console frontend"
```

---

## Task 13: 创建后台管理页面

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/remote/operation-log/index.vue`

- [ ] **Step 1: 创建页面**

页面包含：
- 搜索表单：时间范围、企业、设备、操作类型、操作人、结果状态
- 数据表格：展示操作日志
- 导出按钮

```vue
<template>
  <div class="operation-log-container">
    <el-card>
      <template #header>
        <span>远程控制操作日志</span>
      </template>

      <el-form :model="queryParams" ref="queryFormRef" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="操作时间">
              <el-date-picker
                v-model="dateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="操作类型">
              <el-select v-model="queryParams.operationType" placeholder="全部" clearable>
                <el-option label="消音" value="MUTE" />
                <el-option label="复位" value="RESET" />
                <el-option label="切换模式" value="SWITCH_MODE" />
                <el-option label="启动" value="START" />
                <el-option label="停止" value="STOP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="结果状态">
              <el-select v-model="queryParams.commandStatus" placeholder="全部" clearable>
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
                <el-option label="执行中" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item>
              <el-button type="primary" @click="handleQuery">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
              <el-button type="success" @click="handleExport">导出</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-table v-loading="loading" :data="logList" border>
        <el-table-column prop="operationTime" label="操作时间" width="180" />
        <el-table-column prop="propertyName" label="企业/物业" width="150" />
        <el-table-column prop="deviceName" label="设备" width="150" />
        <el-table-column prop="operationName" label="操作" width="120" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="reviewerName" label="复核人" width="120" />
        <el-table-column prop="commandStatus" label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.commandStatus)">
              {{ getStatusText(row.commandStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultMsg" label="结果说明" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="操作IP" width="140" />
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { getOperationLogPage } from '@/api/eiot/remote/remoteConsole.api'

const loading = ref(false)
const total = ref(0)
const logList = ref<any[]>([])
const dateRange = ref<string[]>([])

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  operationType: undefined,
  commandStatus: undefined,
  startTime: undefined,
  endTime: undefined
})

const getList = async () => {
  loading.value = true
  try {
    queryParams.startTime = dateRange.value?.[0]
    queryParams.endTime = dateRange.value?.[1]
    const res = await getOperationLogPage(queryParams)
    logList.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  dateRange.value = []
  queryParams.operationType = undefined
  queryParams.commandStatus = undefined
  queryParams.startTime = undefined
  queryParams.endTime = undefined
  handleQuery()
}

const handleExport = () => {
  // TODO: 实现导出
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'danger', 1: 'success', 2: 'warning' }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '失败', 1: '成功', 2: '执行中' }
  return map[status] || '未知'
}

onMounted(() => {
  getList()
})
</script>
```

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/views/eiot/remote/operation-log/index.vue
git commit -m "feat(remote): add operation log admin page"
```

---

## Task 14: 注册后台管理路由

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/router/modules/remaining.ts`

- [ ] **Step 1: 在 remaining.ts 中添加路由**

在 `/eiot` 路由 children 中追加：

```typescript
{
  path: '/eiot/remote/operation-log',
  name: 'RemoteOperationLog',
  meta: {
    title: '远程控制日志',
    noCache: true,
    hidden: true,
    activeMenu: '/eiot/remote'
  },
  component: () => import('@/views/eiot/remote/operation-log/index.vue')
}
```

- [ ] **Step 2: Commit**

```bash
git add anwei-fire-remote-monitor-vue/src/router/modules/remaining.ts
git commit -m "feat(remote): register operation log route"
```

---

## Task 15: 前端编译验证

**Files:**
- All modified frontend files

- [ ] **Step 1: 运行 TypeScript 检查**

Run:
```bash
cd anwei-fire-remote-monitor-vue
npx vue-tsc --noEmit --pretty 2>&1 | Select-String "remote-console\|remote/operation-log"
```

Expected: 无新增错误

- [ ] **Step 2: 启动 dev server 验证**

Run:
```bash
npx vite --port 5173
```

Expected: 正常启动，页面可访问

- [ ] **Step 3: Commit 修复**

```bash
git add -A
git commit -m "fix(remote): fix frontend compile errors"
```

---

## Task 16: 联调测试

**Files:**
- Backend and frontend

- [ ] **Step 1: 启动后端服务**

Run:
```bash
cd anwei-fire-remote-monitor/server
mvn spring-boot:run
```

- [ ] **Step 2: 测试设备树接口**

Use Postman or curl:
```bash
curl http://localhost:48080/eiot/remote-console/device-tree
```

Expected: 返回设备树数据

- [ ] **Step 3: 测试操作日志记录**

执行一次远程控制操作后，查询数据库：
```sql
SELECT * FROM eiot_remote_operation_log ORDER BY id DESC LIMIT 5;
```

Expected: 有新记录产生

- [ ] **Step 4: 测试后台管理页面**

访问 `http://localhost:5173/#/eiot/remote/operation-log`

Expected: 页面展示操作日志列表

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(remote): complete remote console backend and admin page"
```

---

## 自查清单

- [ ] 所有接口路径与前端 API 定义一致
- [ ] `RemoteOperationLogDO` 继承正确的 BaseDO
- [ ] `DeviceInstructionServiceImpl` 中日志记录不破坏原有逻辑
- [ ] 后台管理页面路由已注册
- [ ] 前端 Mock 数据已移除
- [ ] 编译通过
