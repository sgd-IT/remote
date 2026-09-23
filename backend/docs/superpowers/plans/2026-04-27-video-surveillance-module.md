# 视频监控模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现智慧消防远程值守系统视频监控模块第一阶段（ICC配置管理、摄像头手动管理、1/4/9分屏视频管理页、部件-摄像头关联绑定、报警联动视频弹窗）。

**Architecture:** 后端封装大华ICC OpenAPI（统一IccOpenApiClient管理Token和RSA加密），前端通过后端获取RTSP地址后用`<video>`标签播放。摄像头采用纯手动配置（逐通道）。

**Tech Stack:** Java Spring Boot + MyBatis-Plus + Vue 3 + Element Plus + TypeScript

**用户约束：** 不要主动提交代码，所有代码变更需用户确认后再提交。

---

## 文件结构总览

### 后端（module-eiot-biz）

| 文件 | 操作 | 说明 |
|------|------|------|
| `dal/dataobject/video/MediaServerDO.java` | 创建 | ICC平台配置DO |
| `dal/dataobject/video/VideoCameraDO.java` | 创建 | 摄像头管理DO |
| `dal/dataobject/video/DeviceCameraRelationDO.java` | 创建 | 部件-摄像头关联DO |
| `dal/mysql/video/MediaServerMapper.java` | 创建 | ICC配置Mapper |
| `dal/mysql/video/VideoCameraMapper.java` | 创建 | 摄像头Mapper |
| `dal/mysql/video/DeviceCameraRelationMapper.java` | 创建 | 关联Mapper |
| `service/video/icc/IccOpenApiClient.java` | 创建 | ICC HTTP客户端 |
| `service/video/icc/dto/IccTokenRespDTO.java` | 创建 | ICC Token响应DTO |
| `service/video/icc/dto/IccStartVideoRespDTO.java` | 创建 | ICC取流响应DTO |
| `service/video/IccConfigService.java` | 创建 | ICC配置Service接口 |
| `service/video/IccConfigServiceImpl.java` | 创建 | ICC配置Service实现 |
| `service/video/VideoCameraService.java` | 创建 | 摄像头Service接口 |
| `service/video/VideoCameraServiceImpl.java` | 创建 | 摄像头Service实现 |
| `service/video/VideoLiveService.java` | 创建 | 实时预览Service接口 |
| `service/video/VideoLiveServiceImpl.java` | 创建 | 实时预览Service实现 |
| `service/video/DeviceCameraRelationService.java` | 创建 | 关联Service接口 |
| `service/video/DeviceCameraRelationServiceImpl.java` | 创建 | 关联Service实现 |
| `controller/admin/video/VideoController.java` | 创建 | 视频模块Controller |
| `controller/admin/video/vo/*` | 创建 | 请求/响应VO（约8个）|

### 前端（anwei-fire-remote-monitor-vue）

| 文件 | 操作 | 说明 |
|------|------|------|
| `api/eiot/video/video.api.ts` | 创建 | 视频模块前端API |
| `views/eiot/video/camera/index.vue` | 创建 | 摄像头管理页 |
| `views/eiot/video/relation/index.vue` | 创建 | 部件-摄像头绑定页 |
| `views/eiot/video/monitor/index.vue` | 创建 | 视频管理页主容器 |
| `views/eiot/video/monitor/CameraTreePanel.vue` | 创建 | 左侧设备树面板 |
| `views/eiot/video/monitor/VideoPlayerGrid.vue` | 创建 | 右侧分屏播放器容器 |
| `components/VideoPlayer/index.vue` | 创建 | 单路RTSP播放器组件 |
| `views/eiot/firewatch/alarm-order/index.vue` | 修改 | 集成报警联动视频弹窗 |

### SQL

| 文件 | 操作 | 说明 |
|------|------|------|
| `_sql/mysql/update.sql` | 追加 | 新建表SQL |

---

## Task 1: 数据库建表

**Files:**
- Modify: `_sql/mysql/update.sql`

- [ ] **Step 1: 追加3张新表的SQL**

在 `_sql/mysql/update.sql` 末尾追加：

```sql
-- ----------------------------
-- Table structure for media_server
-- ----------------------------
DROP TABLE IF EXISTS `media_server`;
CREATE TABLE `media_server` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `server_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
    `base_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ICC平台基础地址',
    `client_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '合作方凭证ID',
    `client_secret` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '合作方凭证密钥',
    `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ICC登录用户名',
    `password` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ICC登录密码（RSA加密）',
    `public_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'RSA公钥',
    `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_tenant_id` (`tenant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ICC平台配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for video_camera
-- ----------------------------
DROP TABLE IF EXISTS `video_camera`;
CREATE TABLE `video_camera` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `channel_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ICC通道编码',
    `channel_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '摄像头名称',
    `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-在线，0-离线',
    `longitude` double DEFAULT NULL COMMENT '经度',
    `latitude` double DEFAULT NULL COMMENT '纬度',
    `address` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '安装地址',
    `dept_id` bigint DEFAULT NULL COMMENT '所属机构',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_channel_id_tenant` (`channel_id`, `tenant_id`) USING BTREE,
    KEY `idx_dept_id` (`dept_id`) USING BTREE,
    KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '摄像头管理表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for device_camera_relation
-- ----------------------------
DROP TABLE IF EXISTS `device_camera_relation`;
CREATE TABLE `device_camera_relation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `device_id` bigint NOT NULL COMMENT '消防部件设备ID',
    `camera_id` bigint NOT NULL COMMENT '摄像头ID',
    `relation_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'manual' COMMENT '关联类型：manual-手动绑定',
    `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_device_camera` (`device_id`, `camera_id`) USING BTREE,
    KEY `idx_device_id` (`device_id`) USING BTREE,
    KEY `idx_camera_id` (`camera_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消防部件与摄像头关联表' ROW_FORMAT = DYNAMIC;
```

- [ ] **Step 2: 验证SQL语法**

在本地MySQL或开发环境执行该SQL，确认3张表创建成功。

---

## Task 2: 后端 DO（Data Object）

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/video/MediaServerDO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/video/VideoCameraDO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/video/DeviceCameraRelationDO.java`

- [ ] **Step 1: 创建 MediaServerDO.java**

```java
package com.enjoyiot.module.eiot.dal.dataobject.video;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

@TableName("media_server")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaServerDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String serverName;
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;
    private String publicKey;
    private Boolean enabled;
}
```

- [ ] **Step 2: 创建 VideoCameraDO.java**

```java
package com.enjoyiot.module.eiot.dal.dataobject.video;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

@TableName("video_camera")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoCameraDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String channelId;
    private String channelName;
    private Integer status;
    private Double longitude;
    private Double latitude;
    private String address;
    private Long deptId;
    private String remark;
}
```

- [ ] **Step 3: 创建 DeviceCameraRelationDO.java**

```java
package com.enjoyiot.module.eiot.dal.dataobject.video;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

@TableName("device_camera_relation")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCameraRelationDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deviceId;
    private Long cameraId;
    private String relationType;
    private LocalDateTime bindTime;
}
```

---

## Task 3: 后端 Mapper

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/video/MediaServerMapper.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/video/VideoCameraMapper.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/mysql/video/DeviceCameraRelationMapper.java`

- [ ] **Step 1: 创建 MediaServerMapper.java**

```java
package com.enjoyiot.module.eiot.dal.mysql.video;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.video.MediaServerDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MediaServerMapper extends BaseMapperX<MediaServerDO> {
}
```

- [ ] **Step 2: 创建 VideoCameraMapper.java**

```java
package com.enjoyiot.module.eiot.dal.mysql.video;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VideoCameraMapper extends BaseMapperX<VideoCameraDO> {

    @Select("SELECT * FROM video_camera WHERE tenant_id = #{tenantId} AND dept_id = #{deptId} AND (deleted = 0 OR deleted IS NULL)")
    List<VideoCameraDO> selectListByDeptId(Long tenantId, Long deptId);
}
```

- [ ] **Step 3: 创建 DeviceCameraRelationMapper.java**

```java
package com.enjoyiot.module.eiot.dal.mysql.video;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.video.DeviceCameraRelationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceCameraRelationMapper extends BaseMapperX<DeviceCameraRelationDO> {

    @Select("SELECT * FROM device_camera_relation WHERE device_id = #{deviceId} AND (deleted = 0 OR deleted IS NULL)")
    List<DeviceCameraRelationDO> selectListByDeviceId(@Param("deviceId") Long deviceId);
}
```

---

## Task 4: IccOpenApiClient（核心组件）

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/icc/IccOpenApiClient.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/icc/dto/IccTokenRespDTO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/icc/dto/IccStartVideoRespDTO.java`

- [ ] **Step 1: 创建 IccTokenRespDTO.java**

```java
package com.enjoyiot.module.eiot.service.video.icc.dto;

import lombok.Data;

@Data
public class IccTokenRespDTO {
    private String accessToken;
    private String tokenType;
    private String refreshToken;
    private Long expiresIn;
    private String userId;
    private String magicId;
}
```

- [ ] **Step 2: 创建 IccStartVideoRespDTO.java**

```java
package com.enjoyiot.module.eiot.service.video.icc.dto;

import lombok.Data;

@Data
public class IccStartVideoRespDTO {
    private String url;
    private String token;
    private String trackId;
}
```

- [ ] **Step 3: 创建 IccOpenApiClient.java**

```java
package com.enjoyiot.module.eiot.service.video.icc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.enjoyiot.module.eiot.dal.dataobject.video.MediaServerDO;
import com.enjoyiot.module.eiot.service.video.icc.dto.IccStartVideoRespDTO;
import com.enjoyiot.module.eiot.service.video.icc.dto.IccTokenRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class IccOpenApiClient {

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile String magicId;
    private volatile Instant tokenExpireTime;
    private final Object tokenLock = new Object();

    public String getAccessToken(MediaServerDO config) {
        if (isTokenValid()) {
            return accessToken;
        }
        synchronized (tokenLock) {
            if (isTokenValid()) {
                return accessToken;
            }
            refreshToken(config);
            return accessToken;
        }
    }

    private boolean isTokenValid() {
        if (StrUtil.isBlank(accessToken) || tokenExpireTime == null) {
            return false;
        }
        return Instant.now().isBefore(tokenExpireTime.minusSeconds(300));
    }

    private void refreshToken(MediaServerDO config) {
        try {
            String encryptedPassword = encryptPassword(config.getPassword(), config.getPublicKey());
            Map<String, Object> body = new HashMap<>();
            body.put("grant_type", "password");
            body.put("username", config.getUsername());
            body.put("password", encryptedPassword);
            body.put("client_id", config.getClientId());
            body.put("client_secret", config.getClientSecret());
            body.put("public_key", config.getPublicKey());

            String url = config.getBaseUrl() + "/evo-apigw/evo-oauth/1.0.0/oauth/extend/token";
            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(body))
                    .timeout(10000)
                    .execute();

            String respBody = response.body();
            log.info("[IccOpenApiClient] Token response: {}", respBody);

            Map<String, Object> result = JSONUtil.toBean(respBody, Map.class);
            Boolean success = (Boolean) result.get("success");
            if (!Boolean.TRUE.equals(success)) {
                String errMsg = (String) result.getOrDefault("errMsg", "Unknown error");
                throw new RuntimeException("ICC Token获取失败: " + errMsg);
            }

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            this.accessToken = (String) data.get("access_token");
            this.refreshToken = (String) data.get("refresh_token");
            this.magicId = (String) data.get("magicId");
            Long expiresIn = ((Number) data.get("expires_in")).longValue();
            this.tokenExpireTime = Instant.now().plusSeconds(expiresIn);

            log.info("[IccOpenApiClient] Token刷新成功，有效期{}秒", expiresIn);
        } catch (Exception e) {
            log.error("[IccOpenApiClient] Token刷新失败", e);
            throw new RuntimeException("ICC平台连接异常，请联系管理员", e);
        }
    }

    private String encryptPassword(String password, String publicKey) {
        RSA rsa = new RSA(null, publicKey);
        byte[] encrypted = rsa.encrypt(password.getBytes(StandardCharsets.UTF_8), null);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public IccStartVideoRespDTO startVideo(MediaServerDO config, String channelId) {
        String token = getAccessToken(config);
        String url = config.getBaseUrl() + "/evo-apigw/admin/API/MTS/Video/StartVideo";

        Map<String, Object> data = new HashMap<>();
        data.put("channelId", channelId);
        data.put("dataType", "1");
        data.put("streamType", "1");

        Map<String, Object> body = new HashMap<>();
        body.put("data", data);

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body(JSONUtil.toJsonStr(body))
                .timeout(10000)
                .execute();

        String respBody = response.body();
        log.info("[IccOpenApiClient] StartVideo response: {}", respBody);

        Map<String, Object> result = JSONUtil.toBean(respBody, Map.class);
        Integer code = ((Number) result.get("code")).intValue();
        if (code != 1000) {
            String desc = (String) result.getOrDefault("desc", "Unknown error");
            throw new RuntimeException("ICC取流失败: " + desc);
        }

        Map<String, Object> respData = (Map<String, Object>) result.get("data");
        IccStartVideoRespDTO dto = new IccStartVideoRespDTO();
        dto.setUrl((String) respData.get("url"));
        dto.setToken((String) respData.get("token"));
        dto.setTrackId((String) respData.get("trackId"));
        return dto;
    }
}
```

- [ ] **Step 4: 编译验证**

运行 `mvn compile -pl module-eiot/module-eiot-biz -am`，确认 IccOpenApiClient 编译通过。

---

## Task 5: Service 层 — ICC配置 + 摄像头管理

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/IccConfigService.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/IccConfigServiceImpl.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/VideoCameraService.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/VideoCameraServiceImpl.java`

- [ ] **Step 1: 创建 IccConfigService.java**

```java
package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.module.eiot.dal.dataobject.video.MediaServerDO;

public interface IccConfigService {
    Long saveOrUpdateConfig(MediaServerDO config);
    MediaServerDO getCurrentTenantConfig();
}
```

- [ ] **Step 2: 创建 IccConfigServiceImpl.java**

```java
package com.enjoyiot.module.eiot.service.video;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.dal.dataobject.video.MediaServerDO;
import com.enjoyiot.module.eiot.dal.mysql.video.MediaServerMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class IccConfigServiceImpl implements IccConfigService {

    @Resource
    private MediaServerMapper mediaServerMapper;

    @Override
    public Long saveOrUpdateConfig(MediaServerDO config) {
        Long tenantId = TenantContextHolder.getTenantId();
        config.setTenantId(tenantId);
        LambdaQueryWrapper<MediaServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MediaServerDO::getTenantId, tenantId);
        MediaServerDO exist = mediaServerMapper.selectOne(wrapper);
        if (exist != null) {
            config.setId(exist.getId());
            mediaServerMapper.updateById(config);
            return exist.getId();
        }
        mediaServerMapper.insert(config);
        return config.getId();
    }

    @Override
    public MediaServerDO getCurrentTenantConfig() {
        Long tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<MediaServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MediaServerDO::getTenantId, tenantId);
        return mediaServerMapper.selectOne(wrapper);
    }
}
```

- [ ] **Step 3: 创建 VideoCameraService.java**

```java
package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoCameraPageReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;

import java.util.List;

public interface VideoCameraService {
    Long createCamera(VideoCameraDO camera);
    void updateCamera(VideoCameraDO camera);
    void deleteCamera(Long id);
    VideoCameraDO getCamera(Long id);
    PageResult<VideoCameraDO> pageCameras(VideoCameraPageReqVO pageReqVO);
    List<VideoCameraDO> listCamerasByDeptId(Long deptId);
}
```

- [ ] **Step 4: 创建 VideoCameraServiceImpl.java**

```java
package com.enjoyiot.module.eiot.service.video;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoCameraPageReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.dal.mysql.video.VideoCameraMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoCameraServiceImpl implements VideoCameraService {

    @Resource
    private VideoCameraMapper videoCameraMapper;

    @Override
    public Long createCamera(VideoCameraDO camera) {
        camera.setTenantId(TenantContextHolder.getTenantId());
        videoCameraMapper.insert(camera);
        return camera.getId();
    }

    @Override
    public void updateCamera(VideoCameraDO camera) {
        videoCameraMapper.updateById(camera);
    }

    @Override
    public void deleteCamera(Long id) {
        videoCameraMapper.deleteById(id);
    }

    @Override
    public VideoCameraDO getCamera(Long id) {
        return videoCameraMapper.selectById(id);
    }

    @Override
    public PageResult<VideoCameraDO> pageCameras(VideoCameraPageReqVO pageReqVO) {
        LambdaQueryWrapperX<VideoCameraDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(VideoCameraDO::getChannelName, pageReqVO.getChannelName())
               .eqIfPresent(VideoCameraDO::getStatus, pageReqVO.getStatus())
               .eqIfPresent(VideoCameraDO::getDeptId, pageReqVO.getDeptId())
               .orderByDesc(VideoCameraDO::getCreateTime);
        return videoCameraMapper.selectPage(pageReqVO, wrapper);
    }

    @Override
    public List<VideoCameraDO> listCamerasByDeptId(Long deptId) {
        return videoCameraMapper.selectListByDeptId(TenantContextHolder.getTenantId(), deptId);
    }
}
```

---

## Task 6: Service 层 — 实时预览 + 部件关联

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/VideoLiveService.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/VideoLiveServiceImpl.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/DeviceCameraRelationService.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/DeviceCameraRelationServiceImpl.java`

- [ ] **Step 1: 创建 VideoLiveService.java**

```java
package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoLiveUrlRespVO;

public interface VideoLiveService {
    VideoLiveUrlRespVO getLiveUrl(Long cameraId);
}
```

- [ ] **Step 2: 创建 VideoLiveServiceImpl.java**

```java
package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoLiveUrlRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.video.MediaServerDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.dal.mysql.video.VideoCameraMapper;
import com.enjoyiot.module.eiot.service.video.icc.IccOpenApiClient;
import com.enjoyiot.module.eiot.service.video.icc.dto.IccStartVideoRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VideoLiveServiceImpl implements VideoLiveService {

    @Resource
    private IccConfigService iccConfigService;

    @Resource
    private VideoCameraMapper videoCameraMapper;

    @Resource
    private IccOpenApiClient iccOpenApiClient;

    @Override
    public VideoLiveUrlRespVO getLiveUrl(Long cameraId) {
        VideoCameraDO camera = videoCameraMapper.selectById(cameraId);
        if (camera == null) {
            throw new IllegalArgumentException("摄像头不存在");
        }
        MediaServerDO config = iccConfigService.getCurrentTenantConfig();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            throw new IllegalStateException("ICC平台未配置或未启用");
        }
        IccStartVideoRespDTO startVideoResp = iccOpenApiClient.startVideo(config, camera.getChannelId());
        VideoLiveUrlRespVO resp = new VideoLiveUrlRespVO();
        resp.setCameraId(cameraId);
        resp.setChannelName(camera.getChannelName());
        resp.setRtspUrl(startVideoResp.getUrl() + "?token=" + startVideoResp.getToken());
        resp.setExpireTime(LocalDateTime.now().plusSeconds(30));
        return resp;
    }
}
```

- [ ] **Step 3: 创建 DeviceCameraRelationService.java**

```java
package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;

import java.util.List;

public interface DeviceCameraRelationService {
    void bindCameras(Long deviceId, List<Long> cameraIds);
    List<VideoCameraDO> getCamerasByDeviceId(Long deviceId);
    void unbind(Long id);
}
```

- [ ] **Step 4: 创建 DeviceCameraRelationServiceImpl.java**

```java
package com.enjoyiot.module.eiot.service.video;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.dal.dataobject.video.DeviceCameraRelationDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.dal.mysql.video.DeviceCameraRelationMapper;
import com.enjoyiot.module.eiot.dal.mysql.video.VideoCameraMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceCameraRelationServiceImpl implements DeviceCameraRelationService {

    @Resource
    private DeviceCameraRelationMapper relationMapper;

    @Resource
    private VideoCameraMapper videoCameraMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindCameras(Long deviceId, List<Long> cameraIds) {
        Long tenantId = TenantContextHolder.getTenantId();
        // 先删除旧绑定
        LambdaQueryWrapper<DeviceCameraRelationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceCameraRelationDO::getDeviceId, deviceId);
        relationMapper.delete(wrapper);
        // 插入新绑定
        for (Long cameraId : cameraIds) {
            DeviceCameraRelationDO relation = new DeviceCameraRelationDO();
            relation.setDeviceId(deviceId);
            relation.setCameraId(cameraId);
            relation.setRelationType("manual");
            relation.setBindTime(LocalDateTime.now());
            relation.setTenantId(tenantId);
            relationMapper.insert(relation);
        }
    }

    @Override
    public List<VideoCameraDO> getCamerasByDeviceId(Long deviceId) {
        List<DeviceCameraRelationDO> relations = relationMapper.selectListByDeviceId(deviceId);
        List<Long> cameraIds = relations.stream().map(DeviceCameraRelationDO::getCameraId).collect(Collectors.toList());
        if (cameraIds.isEmpty()) {
            return List.of();
        }
        return videoCameraMapper.selectBatchIds(cameraIds);
    }

    @Override
    public void unbind(Long id) {
        relationMapper.deleteById(id);
    }
}
```

---

## Task 7: Controller + VO

**Files:**
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/VideoController.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/IccConfigSaveReqVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/IccConfigRespVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/VideoCameraSaveReqVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/VideoCameraPageReqVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/VideoCameraRespVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/VideoLiveUrlReqVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/VideoLiveUrlRespVO.java`
- Create: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/DeviceCameraBindReqVO.java`

- [ ] **Step 1: 创建 IccConfigSaveReqVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理后台 - ICC配置保存 Request VO")
@Data
public class IccConfigSaveReqVO {
    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置名称不能为空")
    private String serverName;

    @Schema(description = "ICC平台基础地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "基础地址不能为空")
    private String baseUrl;

    @Schema(description = "client_id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "client_id不能为空")
    private String clientId;

    @Schema(description = "client_secret", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "client_secret不能为空")
    private String clientSecret;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "RSA公钥", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公钥不能为空")
    private String publicKey;

    @Schema(description = "是否启用")
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
```

- [ ] **Step 2: 创建 IccConfigRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ICC配置 Response VO")
@Data
public class IccConfigRespVO {
    private Long id;
    private String serverName;
    private String baseUrl;
    private String clientId;
    private String username;
    private String publicKey;
    private Boolean enabled;
}
```

- [ ] **Step 3: 创建 VideoCameraSaveReqVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理后台 - 摄像头保存 Request VO")
@Data
public class VideoCameraSaveReqVO {
    private Long id;

    @Schema(description = "通道编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "通道编码不能为空")
    private String channelId;

    @Schema(description = "摄像头名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "摄像头名称不能为空")
    private String channelName;

    @Schema(description = "状态：1-在线，0-离线")
    private Integer status;

    private Double longitude;
    private Double latitude;
    private String address;

    @Schema(description = "所属机构")
    private Long deptId;

    private String remark;
}
```

- [ ] **Step 4: 创建 VideoCameraPageReqVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.video.vo;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 摄像头分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class VideoCameraPageReqVO extends PageParam {
    private String channelName;
    private Integer status;
    private Long deptId;
}
```

- [ ] **Step 5: 创建 VideoCameraRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 摄像头 Response VO")
@Data
public class VideoCameraRespVO {
    private Long id;
    private String channelId;
    private String channelName;
    private Integer status;
    private Double longitude;
    private Double latitude;
    private String address;
    private Long deptId;
    private String remark;
    private LocalDateTime createTime;
}
```

- [ ] **Step 6: 创建 VideoLiveUrlReqVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理后台 - 实时预览地址 Request VO")
@Data
public class VideoLiveUrlReqVO {
    @Schema(description = "摄像头ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "摄像头ID不能为空")
    private Long cameraId;
}
```

- [ ] **Step 7: 创建 VideoLiveUrlRespVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 实时预览地址 Response VO")
@Data
public class VideoLiveUrlRespVO {
    private Long cameraId;
    private String channelName;
    private String rtspUrl;
    private LocalDateTime expireTime;
}
```

- [ ] **Step 8: 创建 DeviceCameraBindReqVO.java**

```java
package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 部件摄像头绑定 Request VO")
@Data
public class DeviceCameraBindReqVO {
    @Schema(description = "设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    @Schema(description = "摄像头ID列表")
    private List<Long> cameraIds;
}
```

- [ ] **Step 9: 创建 VideoController.java**

```java
package com.enjoyiot.module.eiot.controller.admin.video;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.video.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.video.MediaServerDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.service.video.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 视频监控")
@RestController
@RequestMapping("/eiot/video")
@Validated
public class VideoController {

    @Resource
    private IccConfigService iccConfigService;

    @Resource
    private VideoCameraService videoCameraService;

    @Resource
    private VideoLiveService videoLiveService;

    @Resource
    private DeviceCameraRelationService deviceCameraRelationService;

    // ========== ICC 配置 ==========

    @PostMapping("/icc-config")
    @Operation(summary = "保存ICC配置")
    @PreAuthorize("@ss.hasPermission('eiot:video:icc-config:save')")
    public CommonResult<Long> saveIccConfig(@Valid @RequestBody IccConfigSaveReqVO reqVO) {
        MediaServerDO config = new MediaServerDO();
        config.setServerName(reqVO.getServerName());
        config.setBaseUrl(reqVO.getBaseUrl());
        config.setClientId(reqVO.getClientId());
        config.setClientSecret(reqVO.getClientSecret());
        config.setUsername(reqVO.getUsername());
        config.setPassword(reqVO.getPassword());
        config.setPublicKey(reqVO.getPublicKey());
        config.setEnabled(reqVO.getEnabled());
        return success(iccConfigService.saveOrUpdateConfig(config));
    }

    @GetMapping("/icc-config")
    @Operation(summary = "获取ICC配置")
    @PreAuthorize("@ss.hasPermission('eiot:video:icc-config:query')")
    public CommonResult<IccConfigRespVO> getIccConfig() {
        MediaServerDO config = iccConfigService.getCurrentTenantConfig();
        if (config == null) {
            return success(null);
        }
        IccConfigRespVO vo = new IccConfigRespVO();
        vo.setId(config.getId());
        vo.setServerName(config.getServerName());
        vo.setBaseUrl(config.getBaseUrl());
        vo.setClientId(config.getClientId());
        vo.setUsername(config.getUsername());
        vo.setPublicKey(config.getPublicKey());
        vo.setEnabled(config.getEnabled());
        return success(vo);
    }

    // ========== 摄像头管理 ==========

    @PostMapping("/camera")
    @Operation(summary = "新增摄像头")
    @PreAuthorize("@ss.hasPermission('eiot:video:camera:create')")
    public CommonResult<Long> createCamera(@Valid @RequestBody VideoCameraSaveReqVO reqVO) {
        VideoCameraDO camera = new VideoCameraDO();
        camera.setChannelId(reqVO.getChannelId());
        camera.setChannelName(reqVO.getChannelName());
        camera.setStatus(reqVO.getStatus());
        camera.setLongitude(reqVO.getLongitude());
        camera.setLatitude(reqVO.getLatitude());
        camera.setAddress(reqVO.getAddress());
        camera.setDeptId(reqVO.getDeptId());
        camera.setRemark(reqVO.getRemark());
        return success(videoCameraService.createCamera(camera));
    }

    @PutMapping("/camera")
    @Operation(summary = "修改摄像头")
    @PreAuthorize("@ss.hasPermission('eiot:video:camera:update')")
    public CommonResult<Boolean> updateCamera(@Valid @RequestBody VideoCameraSaveReqVO reqVO) {
        VideoCameraDO camera = new VideoCameraDO();
        camera.setId(reqVO.getId());
        camera.setChannelId(reqVO.getChannelId());
        camera.setChannelName(reqVO.getChannelName());
        camera.setStatus(reqVO.getStatus());
        camera.setLongitude(reqVO.getLongitude());
        camera.setLatitude(reqVO.getLatitude());
        camera.setAddress(reqVO.getAddress());
        camera.setDeptId(reqVO.getDeptId());
        camera.setRemark(reqVO.getRemark());
        videoCameraService.updateCamera(camera);
        return success(true);
    }

    @DeleteMapping("/camera/{id}")
    @Operation(summary = "删除摄像头")
    @Parameter(name = "id", description = "摄像头ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:video:camera:delete')")
    public CommonResult<Boolean> deleteCamera(@PathVariable("id") Long id) {
        videoCameraService.deleteCamera(id);
        return success(true);
    }

    @GetMapping("/camera/page")
    @Operation(summary = "分页查询摄像头")
    @PreAuthorize("@ss.hasPermission('eiot:video:camera:query')")
    public CommonResult<PageResult<VideoCameraDO>> pageCameras(@Valid VideoCameraPageReqVO pageReqVO) {
        return success(videoCameraService.pageCameras(pageReqVO));
    }

    @GetMapping("/camera/tree")
    @Operation(summary = "按机构查询摄像头树")
    @PreAuthorize("@ss.hasPermission('eiot:video:camera:query')")
    public CommonResult<List<VideoCameraDO>> cameraTree(@RequestParam(value = "deptId", required = false) Long deptId) {
        return success(videoCameraService.listCamerasByDeptId(deptId));
    }

    // ========== 实时预览 ==========

    @PostMapping("/live/url")
    @Operation(summary = "获取实时预览RTSP地址")
    @PreAuthorize("@ss.hasPermission('eiot:video:live:query')")
    public CommonResult<VideoLiveUrlRespVO> getLiveUrl(@Valid @RequestBody VideoLiveUrlReqVO reqVO) {
        return success(videoLiveService.getLiveUrl(reqVO.getCameraId()));
    }

    // ========== 部件-摄像头关联 ==========

    @PostMapping("/relation/bind")
    @Operation(summary = "绑定部件与摄像头")
    @PreAuthorize("@ss.hasPermission('eiot:video:relation:bind')")
    public CommonResult<Boolean> bindRelation(@Valid @RequestBody DeviceCameraBindReqVO reqVO) {
        deviceCameraRelationService.bindCameras(reqVO.getDeviceId(), reqVO.getCameraIds());
        return success(true);
    }

    @GetMapping("/relation/{deviceId}")
    @Operation(summary = "查询部件关联的摄像头")
    @Parameter(name = "deviceId", description = "设备ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:video:relation:query')")
    public CommonResult<List<VideoCameraDO>> getRelationCameras(@PathVariable("deviceId") Long deviceId) {
        return success(deviceCameraRelationService.getCamerasByDeviceId(deviceId));
    }

    @DeleteMapping("/relation/{id}")
    @Operation(summary = "解除绑定")
    @Parameter(name = "id", description = "绑定记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:video:relation:unbind')")
    public CommonResult<Boolean> unbindRelation(@PathVariable("id") Long id) {
        deviceCameraRelationService.unbind(id);
        return success(true);
    }
}
```

---

## Task 8: 前端 API 层

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/api/eiot/video/video.api.ts`

- [ ] **Step 1: 创建 video.api.ts**

```typescript
import request from '@/config/axios'

enum Api {
  iccConfig = '/eiot/video/icc-config',
  cameraCreate = '/eiot/video/camera',
  cameraUpdate = '/eiot/video/camera',
  cameraDelete = '/eiot/video/camera',
  cameraPage = '/eiot/video/camera/page',
  cameraTree = '/eiot/video/camera/tree',
  liveUrl = '/eiot/video/live/url',
  relationBind = '/eiot/video/relation/bind',
  relationList = '/eiot/video/relation',
  relationUnbind = '/eiot/video/relation',
}

// ICC配置
export const saveIccConfig = (data: any) => {
  return request.post({ url: Api.iccConfig, data })
}

export const getIccConfig = () => {
  return request.get({ url: Api.iccConfig })
}

// 摄像头管理
export const createCamera = (data: any) => {
  return request.post({ url: Api.cameraCreate, data })
}

export const updateCamera = (data: any) => {
  return request.put({ url: Api.cameraUpdate, data })
}

export const deleteCamera = (id: number) => {
  return request.delete({ url: Api.cameraDelete + '/' + id })
}

export const getCameraPage = (params: any) => {
  return request.get({ url: Api.cameraPage, params })
}

export const getCameraTree = (deptId?: number) => {
  return request.get({ url: Api.cameraTree, params: { deptId } })
}

// 实时预览
export const getLiveUrl = (cameraId: number) => {
  return request.post({ url: Api.liveUrl, data: { cameraId } })
}

// 部件-摄像头关联
export const bindDeviceCamera = (data: any) => {
  return request.post({ url: Api.relationBind, data })
}

export const getRelationCameras = (deviceId: number) => {
  return request.get({ url: Api.relationList + '/' + deviceId })
}

export const unbindRelation = (id: number) => {
  return request.delete({ url: Api.relationUnbind + '/' + id })
}
```

---

## Task 9: 前端页面 — 摄像头管理页

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/video/camera/index.vue`

- [ ] **Step 1: 创建摄像头管理页**

创建一个标准的 Element Plus 表格页面，包含：
- 顶部搜索栏（名称、状态、机构）
- 新增/编辑弹窗（表单字段：channelId、channelName、status、longitude、latitude、address、deptId、remark）
- 表格展示（分页）
- 操作列（编辑、删除）

页面结构参照现有 `views/eiot/firewatch/property/index.vue` 或其他 CRUD 页面的模式编写，使用 `<script setup>` + TypeScript。

核心逻辑：
- 调 `getCameraPage` 获取分页数据
- 调 `createCamera` / `updateCamera` / `deleteCamera` 做增删改
- 使用 `ElMessage` 提示操作结果

---

## Task 10: 前端页面 — 视频管理页 + VideoPlayer 组件

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/components/VideoPlayer/index.vue`
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/video/monitor/CameraTreePanel.vue`
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/video/monitor/VideoPlayerGrid.vue`
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/video/monitor/index.vue`

- [ ] **Step 1: 创建 VideoPlayer 组件**

```vue
<template>
  <div class="video-player-wrapper">
    <video
      v-if="rtspUrl"
      :src="rtspUrl"
      autoplay
      controls
      class="video-element"
      @error="handleError"
    ></video>
    <div v-else-if="loading" class="video-placeholder">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>视频加载中...</span>
    </div>
    <div v-else-if="error" class="video-placeholder error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" @click="retry">重试</el-button>
    </div>
    <div v-else class="video-placeholder">
      <el-icon><VideoCamera /></el-icon>
      <span>请点击左侧摄像头加载视频</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getLiveUrl } from '@/api/eiot/video/video.api'

const props = defineProps<{
  cameraId?: number
}>()

const rtspUrl = ref('')
const loading = ref(false)
const error = ref('')

const loadVideo = async () => {
  if (!props.cameraId) return
  loading.value = true
  error.value = ''
  rtspUrl.value = ''
  try {
    const res = await getLiveUrl(props.cameraId)
    rtspUrl.value = res.data.rtspUrl
  } catch (e: any) {
    error.value = e.message || '视频取流失败'
  } finally {
    loading.value = false
  }
}

const retry = () => {
  loadVideo()
}

const handleError = () => {
  error.value = '当前浏览器不支持 RTSP 直接播放，请联系管理员升级播放器'
}

watch(() => props.cameraId, (newVal) => {
  if (newVal) {
    loadVideo()
  } else {
    rtspUrl.value = ''
    error.value = ''
  }
}, { immediate: true })
</script>

<style scoped>
.video-player-wrapper {
  width: 100%;
  height: 100%;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.video-element {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.video-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 14px;
}
.video-placeholder.error {
  color: #f56c6c;
}
</style>
```

- [ ] **Step 2: 创建 VideoPlayerGrid 组件**

分屏容器，支持 1/4/9 画面切换：
- 用 CSS Grid 布局（1x1、2x2、3x3）
- 每个格子是一个 `VideoPlayer` 组件
- 顶部工具栏：分屏切换按钮、全部关闭按钮
- 点击格子可激活，从左侧树拖拽/点击摄像头到格子中加载

- [ ] **Step 3: 创建 CameraTreePanel 组件**

左侧设备树：
- 调 `getCameraTree` 获取按机构分组的摄像头列表
- 支持搜索过滤
- 点击摄像头时，将 cameraId 发送到父组件（视频管理页）

- [ ] **Step 4: 创建视频管理页主容器**

组合 `CameraTreePanel` + `VideoPlayerGrid`，左右布局（左侧 280px）。

---

## Task 11: 前端页面 — 部件绑定页

**Files:**
- Create: `anwei-fire-remote-monitor-vue/src/views/eiot/video/relation/index.vue`

- [ ] **Step 1: 创建部件-摄像头绑定页**

双列表选择器布局：
- 左侧：消防部件列表（从现有设备接口获取 `eiot_device_info` 列表）
- 右侧：已选部件的关联摄像头列表
- 中间/下方：摄像头选择器（从 `video_camera` 获取）+ 绑定按钮
- 调 `bindDeviceCamera` 保存绑定关系

---

## Task 12: 报警联动集成到火警处置页面

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/alarm-order/index.vue`
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/firewatch/FireAlarmOrderServiceImpl.java`

- [ ] **Step 1: 修改 FireAlarmOrderServiceImpl.createAlarmOrder()**

在创建工单时，查询 `device_camera_relation` 获取关联摄像头，将 `videoDeviceId` 设为第一个关联摄像头的 ID（或保持现有字段，前端通过 deviceId 实时查询）。

```java
// 在 createAlarmOrder 方法中，在 fireAlarmOrderMapper.insert(order) 之前添加：
// TODO: 如需预写入videoDeviceId，注入 DeviceCameraRelationService 查询 deviceId 对应的 cameraId
// 当前设计采用前端实时获取，此行可保持现有逻辑不变
```

- [ ] **Step 2: 在 alarm-order/index.vue 中集成视频弹窗**

1. 在 "确取视频" 按钮的点击事件中，调 `getRelationCameras(alarm.deviceId)` 获取关联摄像头列表
2. 如果有多个关联摄像头，显示下拉选择
3. 选择后调 `getLiveUrl(cameraId)` 获取 RTSP 地址
4. 弹出 `VideoPlayer` 组件播放视频（或内嵌到右侧"现场视频"区域）
5. 在 `<script setup>` 中导入 `VideoPlayer` 组件和 `getRelationCameras`、`getLiveUrl` API

---

## 自查清单

### Spec 覆盖检查

| Spec 需求 | 对应任务 |
|-----------|----------|
| `media_server` 表 | Task 1 |
| `video_camera` 表 | Task 1 |
| `device_camera_relation` 表 | Task 1 |
| IccOpenApiClient + Token管理 | Task 4 |
| ICC配置管理API | Task 7 (VideoController /icc-config) |
| 摄像头手动管理CRUD | Task 5 + Task 7 (VideoController /camera) |
| 实时预览获取RTSP地址 | Task 6 + Task 7 (VideoController /live/url) |
| 部件-摄像头关联绑定 | Task 6 + Task 7 (VideoController /relation) |
| 视频管理页1/4/9分屏 | Task 10 |
| 报警联动视频弹窗 | Task 12 |
| RTSP播放（`<video>`尝试） | Task 10 (VideoPlayer组件) |
| NVR逐通道配置支持 | Task 1 (video_camera表设计) + Task 9 |

### 占位符检查

- [ ] 无 "TBD" / "TODO" / "implement later" / "fill in details"
- [ ] 无 "Add appropriate error handling" 等模糊描述
- [ ] 所有代码步骤包含完整代码

### 类型一致性检查

- [ ] `VideoCameraDO.status` 为 `Integer`（1-在线，0-离线）
- [ ] `IccOpenApiClient.startVideo()` 返回 `IccStartVideoRespDTO`
- [ ] `VideoLiveService.getLiveUrl()` 返回 `VideoLiveUrlRespVO`
- [ ] `DeviceCameraRelationService.getCamerasByDeviceId()` 返回 `List<VideoCameraDO>`
- [ ] Controller 路径前缀统一为 `/eiot/video`

---

## 执行交接

**Plan complete and saved to `docs/superpowers/plans/2026-04-27-video-surveillance-module.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** - Dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints for review

**Which approach?**
