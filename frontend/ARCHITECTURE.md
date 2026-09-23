# 智慧消防前端架构

## 文档职责

本文描述 `anwei-fire-remote-monitor-vue` 在智慧消防系统中的边界、主要子系统和理解路径。文件级实现、依赖版本、测试现状与风险清单以 [`.planning/codebase/`](.planning/codebase/) 为准；跨前后端契约见 [`SYSTEM_INTEGRATION.md`](SYSTEM_INTEGRATION.md)。

## 系统定位与边界

本仓库是面向运营和管理人员的 Vue 3 单页应用，负责页面呈现、交互编排、客户端状态、前端路由以及浏览器侧实时连接。它不拥有业务数据、认证裁决、租户隔离、设备协议解析或第三方服务密钥。

职责边界如下：

- 前端拥有：页面与组件、菜单呈现、客户端路由、表单校验、请求适配、浏览器缓存、地图/视频等浏览器运行时集成。
- 后端拥有：身份与权限的最终判定、业务规则、数据库与时序数据、设备接入、告警处理、跨租户隔离和服务端第三方集成。
- 共享契约：REST 路径与数据结构、认证及租户头、动态菜单结构、WebSocket 消息、AI 流式响应、媒体与文件访问约定。

配套后端仓库位于 `D:\anwei-fire-remote-monitor`。任何共享契约变更都必须同时检查两个仓库。

## 架构分区

| 分区 | 稳定入口 | 责任 |
|---|---|---|
| 应用启动 | `index.html`、`src/main.ts`、`src/App.vue` | 创建并挂载应用，安装全局插件、状态和路由 |
| 路由与权限 | `src/router/`、`src/permission.ts`、`src/utils/routerHelper.ts` | 登录守卫、后端菜单转路由、布局与页面缓存 |
| 业务页面 | `src/views/` | 按 `eiot`、`system`、`infra`、`ai`、`report` 等领域承载页面流程 |
| 后端适配 | `src/api/`、`src/config/axios/` | 集中定义端点、认证/租户头、错误处理和令牌刷新 |
| 共享状态 | `src/store/`、`src/hooks/` | 跨页面状态、缓存和可复用响应式行为 |
| 共享界面 | `src/components/`、`src/layout/`、`src/plugins/` | 通用组件、应用外壳和全局插件 |
| 浏览器集成 | `src/icc/`、`public/static/WSPlayer/`、`public/dongguan-map/` | 视频、地图、Worker、WASM 和大型静态资源 |
| 构建配置 | `package.json`、`vite.config.ts`、`build/`、`types/` | 开发/构建入口、插件和类型声明 |

## 关键运行链路

### 启动与导航

1. `index.html` 加载 `src/main.ts`。
2. `src/main.ts` 安装 Vue 插件、Pinia、Router、全局组件和指令。
3. `src/permission.ts` 在导航时处理登录态、用户信息和字典初始化。
4. 后端菜单由 `src/store/modules/permission.ts` 与 `src/utils/routerHelper.ts` 转为动态路由。
5. `src/layout/` 渲染菜单、标签页和当前业务页面。

### 业务请求

1. `src/views/` 中的页面调用同领域的 `src/api/` 模块。
2. API 模块统一委托 `src/config/axios/`。
3. 客户端加入认证与租户上下文，并按统一结果码处理响应。
4. 页面或 Pinia store 更新状态并驱动界面。

不要在页面中创建绕过统一客户端的 Axios 实例；浏览器 WebSocket、媒体 SDK 或流式 Fetch 是明确的例外，但仍需遵守认证、租户和错误处理契约。

### 实时与媒体

消防值守与监控页面通过 `/infra/ws` 接收实时消息，并以轮询作为部分场景的补充。视频播放依赖后端媒体接口和仓库内的供应商运行时资源。修改连接重试、事件结构、资源路径或媒体鉴权时，应作为跨系统变更处理。

## 设计与放置规则

- 路由级业务流程放在 `src/views/<domain>/`，对应 HTTP 操作放在 `src/api/<domain>/`。
- 仅在多个页面复用时提升到 `src/components/`、`src/hooks/` 或 `src/store/`。
- 认证、租户头、响应解包和令牌刷新统一留在 `src/config/axios/`。
- 前端权限只控制可见性和导航体验，授权安全边界始终由后端执行。
- 大型静态地图/播放器资源必须按完整版本管理；不要只替换供应商资源中的单个文件。
- 新功能应沿用既有领域目录，不要把业务逻辑堆入 `src/main.ts`、`src/layout/` 或通用工具目录。

## 建议理解路径

1. 先读本文，确认仓库职责和目标变更所属分区。
2. 涉及后端接口、认证、实时消息或部署时，读 [`SYSTEM_INTEGRATION.md`](SYSTEM_INTEGRATION.md)。
3. 再读 [`.planning/codebase/STRUCTURE.md`](.planning/codebase/STRUCTURE.md) 和 [`.planning/codebase/ARCHITECTURE.md`](.planning/codebase/ARCHITECTURE.md)。
4. 根据任务补读 `STACK.md`、`INTEGRATIONS.md`、`CONVENTIONS.md`、`TESTING.md` 或 `CONCERNS.md`。
5. 最后进入对应源码目录，以当前源码和配置验证文档中的事实。

## 架构维护

当仓库职责、主要分区、全局数据流或前后端边界改变时更新本文。文件级结构、依赖版本或局部实现变化只更新 `.planning/codebase/`；跨仓库契约变化更新 `SYSTEM_INTEGRATION.md`。
