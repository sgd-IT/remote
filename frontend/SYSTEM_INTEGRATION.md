# 智慧消防前后端集成说明（前端视角）

## 文档职责

本文记录 `anwei-fire-remote-monitor-vue` 与 `anwei-fire-remote-monitor` 之间稳定的协作边界。它不替代接口文档，也不复制两个仓库的内部实现。

## 系统关系

```text
浏览器前端
  ├─ REST / 流式 HTTP ──> Spring Boot 后端 ──> 业务模块 / MySQL / Redis / TDengine
  ├─ WebSocket ─────────> 后端实时推送 ──────> 告警、通知与值守屏
  └─ 地图/视频运行时 ───> 地图服务、后端媒体接口及供应商播放器资源
                                      └──────> IoT 消息、规则、设备协议与第三方平台
```

前端负责消费和呈现，后端负责业务权威状态与设备/外部系统接入。浏览器中的权限判断、缓存和数据校验不能替代服务端控制。

## 共享契约入口

| 契约 | 前端入口 | 后端入口/责任 |
|---|---|---|
| API 基址 | `src/config/axios/config.ts` 的 `VITE_BASE_URL + VITE_API_URL` | 管理端与应用端分别使用 `/admin-api`、`/app-api` 前缀 |
| 请求封装 | `src/config/axios/index.ts`、`service.ts` | 控制器返回统一 `CommonResult` 结果 |
| 身份认证 | `Authorization: Bearer ...`，令牌由 `src/utils/auth.ts` 管理 | `module-system` 签发/刷新令牌，Security 过滤器完成认证 |
| 租户上下文 | 请求头 `tenant-id` | Tenant 过滤器和持久化拦截器执行隔离 |
| 令牌刷新 | `/system/auth/refresh-token` | `module-system` 的认证控制器和 OAuth2 服务 |
| 动态菜单 | `src/store/modules/permission.ts`、`src/utils/routerHelper.ts` | 后端用户/权限服务返回菜单与权限标识 |
| WebSocket | `/infra/ws?token=...` | `framework/spring-boot-starter-websocket/` 与 `module-infra` 推送实现 |
| AI 流式响应 | `src/api/ai/chat/index.ts` | `module-ai` 暴露流式聊天并代理模型服务 |
| 文件上传 | `src/api/infra/file/`、`src/components/UploadFile/` | `module-infra` 管理文件与存储提供方 |
| 视频与设备 | `src/api/eiot/`、`src/icc/`、视频组件 | `module-eiot` 负责设备、媒体、规则、协议和厂商平台 |

## 变更协同规则

- REST 路径、方法、参数、分页或响应字段变化：先修改后端契约与实现，再同步前端 `src/api/` 类型和调用方。
- `CommonResult`、状态码或错误语义变化：同步检查统一 Axios 拦截器，禁止各页面自行兼容不同响应壳。
- 登录、刷新、登出或租户传递变化：同时检查前端缓存/拦截器/路由守卫与后端 Security/OAuth2/Tenant 链路。
- 菜单组件字符串、权限标识或路由元数据变化：同时验证后端菜单输出和前端动态路由解析。
- WebSocket 路径、鉴权或消息字段变化：同步修改服务端握手/发送端及所有前端订阅者，并验证断线重连和重复消息。
- AI 流式协议变化：同步验证 HTTP 状态、分块/SSE 格式、取消与超时行为。
- 视频、设备命令或协议变化：服务端设备/媒体契约先明确，再调整前端控制和播放流程；不要让浏览器承担设备身份或密钥裁决。
- 部署路径、反向代理或环境变量变化：同步检查前端 Vite 模式、后端 profile 和 `deploy/` 下的代理配置。

## 安全边界

- 所有 `VITE_*` 值都会进入浏览器构建产物，只能保存公开配置。
- 服务端密钥、厂商凭据、数据库和消息系统凭据不得复制到前端、本文或规划文档。
- WebSocket 查询参数中的 token、浏览器存储令牌以及日志中的请求/响应内容按敏感数据处理。
- 前端按钮/路由权限仅是交互控制；后端控制器和服务必须独立完成授权及租户校验。

## 联调最小闭环

1. 确认前端环境指向目标后端，后端 profile 的数据库、Redis 与必要时序/消息依赖可用。
2. 验证登录、令牌刷新、用户信息、菜单加载和租户切换。
3. 验证一个普通查询和一个写操作，确认结果壳、错误提示与权限一致。
4. 若涉及实时功能，验证 WebSocket 建连、消息格式、断线重连和页面卸载清理。
5. 若涉及设备、视频、AI 或文件，使用对应真实/受控依赖完成专项联调。
6. 分别执行两个仓库适用的静态检查、构建和测试；测试现状见各自 `.planning/codebase/TESTING.md`。

## 文档同步责任

跨前后端契约变更时，应在同一次工作中同步检查并更新两个仓库的 `SYSTEM_INTEGRATION.md`。单仓内部实现重构且契约不变时，无需改本文。

## V2.0 东莞物业地图复核

城市地图复用 `GET /eiot/firewatch/property/list?status=1` 与 `GET /eiot/firewatch/property/get?id=...`；前端只通过统一 Axios 传递当前会话的认证与租户上下文。`get` 的服务端授权和租户拦截必须持续生效，因为它被用于园区入口点击前复核；不得以大屏便利性调用 `list-all` 或伪造 `tenant-id`。地图 iframe 只接收脱敏展示快照，永不接收 token 或 tenantId。

物业 create/update 的坐标契约为 WGS84 十进制度 `[longitude, latitude]`：两字段同时有值且分别位于 [-180,180]、[-90,90]，或同时为空；update 省略两字段表示不修改，显式传两字段 null 表示同时清空。服务端校验是最终约束。普通物业显示不依赖园区入口 ID/code；地图协议 2 对每个快照分别回报几何结果、标签/聚合可访问结果或拒绝，宿主对两阶段设超时并校验当前快照。

宿主发往地图 iframe 的点位与布局命令在协议边界复制为可结构化克隆的普通对象，不能直接传 Vue Proxy。园区 `v007-r001` 的嵌入场景没有旧版 iframe 返回按钮，宿主提供同页返回东莞地图按钮并保留路由查询参数；服务端授权仍以详情复核为准。
