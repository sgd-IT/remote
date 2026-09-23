# 智慧消防远程值守 — 视频监控模块设计文档

## 1. 概述

### 1.1 目的

本文档定义智慧消防远程值守系统中视频监控模块的技术设计方案，覆盖大华 ICC 开放平台对接、实时视频预览（RTSP）、部件-摄像头关联绑定，以及消防部件报警与摄像头的联动机制。

### 1.2 范围

- **第一阶段（本期）**：ICC 配置接入、摄像头手动管理、独立视频监控管理页（树形设备列表 + 1/4/9 分屏实时预览）、部件-摄像头关联绑定、火警处置页面报警联动视频弹窗
- **第二阶段（后续）**：录像回放、云台控制、视频 AI 报警接入、设备自动同步

### 1.3 术语

| 术语 | 说明 |
|------|------|
| ICC | 大华 Intelligent Command Center 综合管理平台 |
| RTSP | 实时流协议，ICC StartVideo 接口返回的流格式 |
| NVR | 网络视频录像机，接入 ICC 后其下 IPC 通道可被独立取流 |
| 部件 | 指消防子设备，如烟感、温感、手报等 |
| channelId | ICC 通道编码，如 `100001$1$0$0`，是 StartVideo 接口的最小取流单位 |

### 1.4 核心决策

| 决策项 | 选择 | 原因 |
|--------|------|------|
| 视频流格式 | RTSP 直传 | ICC StartVideo 接口返回 RTSP 地址；前端先用 `<video>` 标签尝试，后续迭代优化播放器 |
| 设备管理方式 | 纯手动配置 | 第一阶段快速验证核心链路，后台页面手动添加通道编码（channelId）即可取流 |
| 报警联动取流 | 前端实时获取 | ICC RTSP token 有效期仅约 30 秒，前端在播放前实时调接口获取最新地址 |
| 后端 ICC 代理 | 统一 `IccOpenApiClient` | 集中管理 Token 生命周期、RSA 加密、错误重试，便于后续切换其他视频平台 |

---

## 2. 背景与约束

### 2.1 现状

- 公司已搭建大华 ICC 开放平台（版本 5.0.17），现场摄像头（含 NVR 下挂载的 IPC）已通过 GB28181 或私有协议接入 ICC
- 项目前端基于 Vue 3 + Element Plus，后端基于 Java Spring Boot
- 火警处置中心页面（`alarm-order/index.vue`）右侧已有"现场视频"UI 占位，但未接入真实播放器
- `FireAlarmOrderServiceImpl.createAlarmOrder()` 中已预留 TODO "自动获取联动视频"

### 2.2 约束

- 不需要实现摄像头设备接入功能，设备已接入 ICC 平台
- 必须复用现有的多租户框架和权限体系
- 第一阶段以最小可行产品（MVP）为目标，优先打通端到端数据流

---

## 3. 架构设计

### 3.1 总体架构

采用**后端代理 + 前端 RTSP 播放**模式：后端封装大华 ICC OpenAPI，统一管理鉴权、流地址获取；前端通过后端获取 RTSP 播放地址后，使用浏览器播放器播放。

```
+-------------+      REST API       +----------------------+      ICC OpenAPI      +-------------+
|  前端(Vue)   |  <--------------->  |    视频服务后端       |  <----------------->  | 大华ICC平台  |
|             |                     |                      |                       |             |
| 视频管理页   |                     | ICC配置管理           |                       | Token服务    |
| 报警弹窗     |                     | 摄像头手动管理        |                       | StartVideo   |
| <video>播放 |  <--- RTSP地址      | IccOpenApiClient      |                       |             |
+-------------+                     | 部件-摄像头关联       |                       +-------------+
                                    | 实时预览服务          |
                                    +----------------------+
```

### 3.2 架构决策

**后端代理而非前端直连 ICC**：
- ICC 的 `client_secret` 和登录密码由后端统一管理和加密，不暴露到前端
- 前端只与自有后端交互，便于后续切换其他视频平台（如海康）
- 后端可在获取流地址时做权限校验（该用户是否有权查看该摄像头）

**统一 ICC Client 封装**：
- 所有与 ICC 的 HTTP 通信通过 `IccOpenApiClient` 一个出口
- Token 申请、刷新、RSA 加密、错误重试全部内聚在 Client 内部
- 业务 Service（如 `VideoLiveService`）不感知 ICC 的鉴权细节

---

## 4. 数据模型

### 4.1 `media_server` — ICC 平台配置

存储大华 ICC 平台连接信息，按租户隔离（每个租户一条配置）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `tenant_id` | BIGINT | 租户ID（多租户唯一）|
| `server_name` | VARCHAR(64) | 配置名称，如 `大华ICC生产环境` |
| `base_url` | VARCHAR(256) | ICC 平台基础地址，如 `https://**` |
| `client_id` | VARCHAR(128) | 合作方凭证 ID |
| `client_secret` | VARCHAR(256) | 合作方凭证密钥（加密存储）|
| `username` | VARCHAR(64) | ICC 平台登录用户名 |
| `password` | VARCHAR(512) | ICC 平台密码（RSA加密后存储）|
| `public_key` | VARCHAR(512) | RSA 公钥 |
| `enabled` | TINYINT(1) | 是否启用 |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |

### 4.2 `video_camera` — 摄像头管理

可播放的视频通道，后台手动维护。一条记录对应 ICC 中的一个**通道**（Channel），而非 NVR 设备。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `device_id` | VARCHAR(64) | 设备编码/录像机编码，对应 ICC 中的父设备。前端输入提示：`请输入设备编码，如 100001` |
| `channel_id` | VARCHAR(64) | 通道编码，对应 ICC 中的视频通道，**租户内唯一**。前端输入提示：`请输入通道编码，如 100001$1$0$0` |
| `channel_name` | VARCHAR(128) | 摄像头名称 |
| `status` | INT | 状态：1-在线，0-离线（手动维护或后续扩展心跳检测）|
| `longitude` | DOUBLE | 经度 |
| `latitude` | DOUBLE | 纬度 |
| `address` | VARCHAR(256) | 安装地址 |
| `dept_id` | BIGINT | 所属机构/企业 |
| `tenant_id` | BIGINT | 租户ID |
| `remark` | VARCHAR(256) | 备注 |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |

> **NVR 场景说明**：NVR 接入 ICC 后，其下每个 IPC 对应一个独立 Channel。如需播放 NVR 下的 4 路摄像头，需在 `video_camera` 表中添加 4 条记录：
> - 4 条记录的 `device_id` 相同（都是该 NVR 的设备编码）
> - 4 条记录的 `channel_id` 各不相同（分别是 4 个摄像头的通道编码）

### 4.3 `device_camera_relation` — 消防部件与摄像头关联

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `device_id` | BIGINT | 消防部件设备ID（`eiot_device_info.id`）|
| `camera_id` | BIGINT | 摄像头ID（`video_camera.id`）|
| `relation_type` | VARCHAR(32) | 关联类型：`manual`-手动绑定 |
| `bind_time` | DATETIME | 绑定时间 |
| `tenant_id` | BIGINT | 租户ID |

> **关联粒度**：按设备级关联，一个消防部件可绑定多个摄像头，一个摄像头也可被多个部件共用。

---

## 5. 后端服务设计

### 5.1 服务划分

| 服务 | 职责 | 文件路径 |
|------|------|----------|
| `IccOpenApiClient` | ICC HTTP 通信、Token 生命周期管理、RSA 加密、错误重试 | `service/video/icc/IccOpenApiClient.java` |
| `IccConfigService` | `media_server` CRUD、配置校验 | `service/video/IccConfigService.java` |
| `VideoCameraService` | 摄像头手动管理（增删改查、分页、树形） | `service/video/VideoCameraService.java` |
| `VideoLiveService` | 实时预览：调 ICC 获取 RTSP 地址 | `service/video/VideoLiveService.java` |
| `DeviceCameraRelationService` | 部件-摄像头绑定/解绑/查询 | `service/video/DeviceCameraRelationService.java` |

### 5.2 ICC Token 管理策略

ICC OpenAPI 使用 `accessToken` 进行接口鉴权，Token 有效期约 2 小时。

**管理策略**：
1. `IccOpenApiClient` 内存缓存当前有效的 `accessToken`、`refreshToken`、`expiresIn`、`magicId`
2. 每次调用 ICC API 前检查 Token 是否即将过期（提前 5 分钟刷新）
3. Token 失效时自动调用 `/evo-apigw/evo-oauth/1.0.0/oauth/extend/token` 重新获取
4. 使用同步锁防止并发刷新导致 Token 争抢
5. 若 ICC 返回 Token 失效，自动刷新并重试一次，仍失败则抛出 `ICC_TOKEN_EXPIRED` 业务异常

**Token 请求参数**：
- `grant_type`：固定值 `password`
- `username`：ICC 平台用户名
- `password`：RSA 加密后的密码
- `client_id`、`client_secret`：合作方凭证
- `public_key`：RSA 公钥

### 5.3 API 接口设计

#### ICC 配置管理

```
POST /admin-api/eiot/video/icc-config
  body: { serverName, baseUrl, clientId, clientSecret, username, password, publicKey, enabled }
  -> 保存或更新 ICC 平台配置（每个租户限一条）

GET /admin-api/eiot/video/icc-config
  -> 返回当前租户 ICC 配置
```

#### 摄像头管理

```
POST /admin-api/eiot/video/camera
  body: { deviceId, channelId, channelName, status, longitude, latitude, address, deptId, remark }
  -> 新增摄像头（deviceId 为设备/录像机编码，channelId 为通道编码）

PUT /admin-api/eiot/video/camera
  body: { id, deviceId, channelId, channelName, status, longitude, latitude, address, deptId, remark }
  -> 修改摄像头

DELETE /admin-api/eiot/video/camera/{id}
  -> 删除摄像头

GET /admin-api/eiot/video/camera/page
  params: { pageNo, pageSize, channelName?, status?, deptId? }
  -> 分页查询摄像头列表

GET /admin-api/eiot/video/camera/tree
  -> 按机构(dept_id)返回摄像头树形结构

GET /admin-api/eiot/video/icc-devices
  -> 从 ICC 平台获取设备/通道列表（供新增时参考选择）
  response: [
    {
      deviceId: "100001",
      deviceName: "NVR-001",
      channels: [
        { channelId: "100001$1$0$0", channelName: "大堂摄像头" },
        { channelId: "100001$1$0$1", channelName: "停车场摄像头" }
      ]
    }
  ]
```

#### 实时预览

```
POST /admin-api/eiot/video/live/url
  body: { cameraId }
  -> 返回 RTSP 播放地址
  response: { cameraId, channelName, rtspUrl, expireTime }
```

> 内部逻辑：
> 1. 校验当前用户是否有权查看该摄像头（通过 `dept_id` 数据权限）
> 2. 从 `video_camera` 表获取 `deviceId` 和 `channelId`
> 3. 调 `IccOpenApiClient.startVideo(deviceId, channelId)` 调用 ICC `/evo-apigw/admin/API/MTS/Video/StartVideo`
> 4. ICC 返回 `{ url, token }`，拼接完整 RTSP 地址：`url + "?token=" + token`
> 5. 返回给前端，`expireTime` 建议前端在 25 秒内完成播放（token 有效期约 30 秒）

#### 部件-摄像头关联

```
POST /admin-api/eiot/video/relation/bind
  body: { deviceId, cameraIds: [] }
  -> 绑定消防部件与一个或多个摄像头

GET /admin-api/eiot/video/relation/{deviceId}
  -> 查询指定部件关联的摄像头列表

DELETE /admin-api/eiot/video/relation/{id}
  -> 解除绑定
```

---

## 6. 前端设计

### 6.1 页面结构

#### 视频管理页（独立页面）

路由：`/eiot/video/monitor`

布局：**左侧设备树 + 右侧多画面分屏**

| 区域 | 内容 |
|------|------|
| 左侧（宽度 280px）| 机构树 + 摄像头设备树，支持搜索、按状态筛选 |
| 右侧顶部工具栏 | 分屏切换按钮（1画面 / 4画面 / 9画面）、全部关闭 |
| 右侧主区域 | 分屏播放器网格，每个格子可点击左侧摄像头加载视频 |

**交互逻辑**：
1. 用户从左侧设备树点击或拖拽摄像头到右侧某个分屏格子
2. 前端调 `/video/live/url` 获取 RTSP 地址
3. 在对应格子中初始化 `<video>` 播放器并开始播放
4. 切换分屏模式时，已播放的摄像头按顺序重新排列到新布局
5. 用户关闭某路视频时，停止播放并释放资源

#### 摄像头管理页（后台配置）

路由：`/eiot/video/camera`

提供摄像头的增删改查，新增/编辑表单中 **设备编码**（`deviceId`）和 **通道编码**（`channelId`）为两个独立字段：
- `deviceId`：设备/录像机编码（如 `100001`）
- `channelId`：通道编码（如 `100001$1$0$0`）
- `channelName`：摄像头名称

#### 部件-摄像头绑定页（后台配置）

路由：`/eiot/video/relation`

选择消防部件（来自 `eiot_device_info`），选择关联的摄像头（来自 `video_camera`），保存到 `device_camera_relation` 表。

#### 报警联动视频弹窗（火警处置页面）

集成到现有 `alarm-order/index.vue` 的"现场视频"区域：
- 当告警工单有关联摄像头时，"确取视频"按钮可用
- 点击后调 `/video/relation/{deviceId}` 获取关联摄像头列表
- 调 `/video/live/url` 获取 RTSP 地址并播放
- 支持显示"切换摄像头"下拉列表，切换关联的其他摄像头
- 视频区域支持全屏

### 6.2 组件设计

| 组件 | 文件路径 | 职责 |
|------|----------|------|
| `VideoMonitorPage` | `views/eiot/video/monitor/index.vue` | 视频管理页主容器 |
| `CameraTreePanel` | `views/eiot/video/monitor/CameraTreePanel.vue` | 左侧设备树面板 |
| `VideoPlayerGrid` | `views/eiot/video/monitor/VideoPlayerGrid.vue` | 右侧分屏播放器容器 |
| `VideoPlayer` | `components/VideoPlayer/index.vue` | 单路 RTSP 播放器（复用）|
| `CameraManagePage` | `views/eiot/video/camera/index.vue` | 摄像头后台管理页 |
| `RelationManagePage` | `views/eiot/video/relation/index.vue` | 部件-摄像头绑定页 |

### 6.3 播放器选型（第一阶段 MVP）

由于 ICC 返回 RTSP 地址，前端第一阶段采用浏览器原生 `<video>` 标签尝试播放：

```vue
<template>
  <video 
    v-if="rtspUrl" 
    :src="rtspUrl" 
    autoplay 
    controls 
    style="width:100%; height:100%;"
  ></video>
  <div v-else class="video-placeholder">
    视频加载中...
  </div>
</template>
```

> **重要说明**：Chrome/Edge 等浏览器原生 `<video>` 标签**不支持 RTSP 协议**。第一阶段先按此实现以快速验证数据链路是否打通。如果播放失败，后续迭代引入 `mpegts.js` 或专用播放器做 RTSP over WebSocket/WebRTC 转换。播放失败时前端应显示友好提示："当前浏览器不支持 RTSP 直接播放，请联系管理员升级播放器"。

---

## 7. 报警联动数据流

### 7.1 核心场景：部件报警 → 自动播放关联视频

```
[四信网关] --(GB/T 26875 报警报文)--> [iot-component-gbt26875-fourfaith-tcp]
                                               |
                                               v
                                    解析出部件报警（如：烟感003 火警）
                                               |
                                               v
                                    FireAlarmOrderService.createAlarmOrder()
                                               |
                                               v
                                    查询 device_camera_relation
                                    （device_id = 烟感003 对应的主设备ID）
                                               |
                                               v
                                    获取关联摄像头 camera_id 列表
                                    将 camera_ids 写入告警工单扩展字段
                                               |
                                               v
                                    [前端-火警处置页面]
                                               |
                                               v
                                    用户点击"确取视频"或自动弹窗
                                               |
                                               v
                                    GET /video/relation/{deviceId} 获取关联摄像头
                                               |
                                               v
                                    POST /video/live/url 实时获取 RTSP 地址
                                               |
                                               v
                                    VideoPlayer 初始化播放
```

### 7.2 现有代码适配

- `FireAlarmOrderDO` 已有 `video_device_id` 字段，需扩展为支持多摄像头关联（或每次通过 `device_camera_relation` 表实时查询）
- `FireAlarmOrderServiceImpl.createAlarmOrder()` 中 TODO "自动获取联动视频"改为：查询 `device_camera_relation` 关联表，将关联摄像头 ID 列表写入工单扩展字段

---

## 8. 错误处理

### 8.1 后端错误

| 场景 | 错误码 | 处理策略 |
|------|--------|----------|
| ICC Token 失效 | `ICC_TOKEN_EXPIRED` | `IccOpenApiClient` 自动刷新 Token 并重试一次，仍失败则返回错误 |
| ICC 平台不可达 | `ICC_UNAVAILABLE` | 返回友好提示"视频平台连接异常，请联系管理员" |
| ICC 返回 StartVideo 失败 | `ICC_START_VIDEO_FAILED` | 透传 ICC 错误描述，前端显示"视频取流失败" |
| 摄像头未配置 | `CAMERA_NOT_FOUND` | 返回错误，前端显示"摄像头未配置" |
| 无权查看摄像头 | `FORBIDDEN` | 返回 403，前端显示"无权查看该视频" |
| 流地址获取超时 | `STREAM_TIMEOUT` | 超时时间 10s，返回"视频加载超时，请重试" |

### 8.2 前端错误

| 场景 | 处理策略 |
|------|----------|
| RTSP 播放失败（浏览器不支持）| 显示"当前浏览器不支持 RTSP 直接播放"，提示联系管理员 |
| 获取 RTSP 地址失败 | 显示"视频取流失败"，提供"重试"按钮 |
| 分屏格子被占满 | 新点击的摄像头提示"请先关闭一路视频" |

---

## 9. 安全设计

1. **Token 隔离**：ICC `client_secret` 和登录密码仅存储在后端配置中，绝不暴露到前端
2. **密码加密**：`media_server` 表中 `password` 和 `client_secret` 使用 AES 加密存储
3. **权限校验**：获取视频流地址前，校验当前用户是否有权查看该摄像头（通过 `dept_id` 数据权限）
4. **播放地址时效**：ICC 返回的 RTSP token 有效期约 30 秒，过期后需重新申请

---

## 10. 交付阶段划分

### 10.1 第一阶段（本期目标）

| 序号 | 功能项 | 说明 |
|------|--------|------|
| 1 | ICC 配置管理 | 后台保存 ICC 平台地址、client_id、client_secret、用户名密码，支持 Token 自动刷新 |
| 2 | 摄像头手动管理 | 后台页面手动添加/编辑/删除摄像头（填 channelId、名称、所属机构）|
| 3 | 视频管理页 | 左侧树形设备列表 + 右侧 1/4/9 分屏 `<video>` 实时预览 |
| 4 | 部件-摄像头关联 | 后台管理页面，支持绑定/解绑消防部件与摄像头 |
| 5 | 报警联动视频弹窗 | 火警处置页面，报警时调关联摄像头列表并实时获取 RTSP 地址播放 |

### 10.2 第二阶段（后续规划）

| 序号 | 功能项 | 说明 |
|------|--------|------|
| 6 | 设备自动同步 | 从 ICC 自动同步 NVR/IPC 设备通道列表到本地 |
| 7 | 录像回放 | 按时间段查询录像、播放历史视频 |
| 8 | 云台控制 | 方向控制、变焦、预置位调用 |
| 9 | 视频 AI 报警 | 接入 ICC 智能分析报警（如火焰识别）作为系统报警来源 |
| 10 | 播放器升级 | 替换 `<video>` 为支持 RTSP 的专业播放器（如 RTSP over WebSocket）|

---

## 11. 依赖与风险

### 11.1 外部依赖

| 依赖 | 说明 |
|------|------|
| 大华 ICC 5.0.17 OpenAPI | 视频流获取（StartVideo 返回 RTSP）|
| ICC 平台网络可达 | 后端服务器需能访问 ICC 平台的 HTTP 端口 |
| RSA 公钥 | 获取 ICC Token 时需要 RSA 加密密码 |

### 11.2 风险与应对

| 风险 | 影响 | 应对策略 |
|------|------|----------|
| RTSP 在浏览器无法播放 | 视频无法观看 | 第一阶段仅验证数据链路打通；第二阶段引入专业播放器 |
| ICC API 文档与实际接口不符 | 开发阻塞 | 前期用 Postman 验证关键接口（取 Token、取流地址）|
| Token 并发刷新冲突 | 接口调用失败 | `IccOpenApiClient` 内使用同步锁保证单节点刷新 Token |
| 摄像头数量大（>1000）| 手动配置繁琐 | 第一阶段控制规模；第二阶段引入自动同步 |
