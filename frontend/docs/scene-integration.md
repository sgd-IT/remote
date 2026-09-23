# Duty-screen 场景集成

值守大屏由宿主 Vue 页面编排业务数据，地图和园区作为同源、按场景懒加载的静态 iframe 子应用运行。宿主保留统一 Axios、租户/认证上下文和既有 WebSocket；子应用只负责渲染和回传协议事件。

## 场景与入口

- `/#/duty-screen?scene=city`：东莞三维地图，默认场景。
- `/#/duty-screen?scene=park`：安为智慧园区。
- 大屏右上角地图图标菜单可切换东莞地图与 V7 园区，左右业务面板保持显示；V7 内的“地图全貌”返回大屏中的东莞地图。
- 缺失或非法 `scene` 只规范化该参数，其他 query 保留。
- 城市地图仅接收当前账号有权查询、已核实坐标来源的真实物业点；设备统计与告警留在业务面板，不向城市地图绘制设备柱。
- 从物业点进入园区仍须绑定真实 ID 与 code；地图图标菜单中的“V7 园区”仅切换静态三维场景，不声称当前物业与园区已绑定。坐标来源未核实或入口身份未绑定时不展示伪造的物业点入口。

## 静态 release

- 地图入口：`/dongguan-map/releases/v002-property-r7/index.html?embedded=1#/dongguan`
- 园区入口：`/smart-park/releases/v007-r001/index.html?embedded=1&quality=HIGH#/campus`
- V7 静态包包含 `public/assets`、`public/park/v007/v007-r001` 和园区入口；资源合同使用站点根路径 `/park`，部署时须保持同源根路径可访问。
- 发布脚本会校验 V7 交付包的文件哈希，并拒绝覆盖内容不同的既有文件；场景校验检查版本和资源合同。
- `pnpm sync:smart-park` 默认读取 `D:/anwei-Smart-Park/tasks/V 7.0/界面精简与设备告警/交付/V7.0-界面精简-20260914`；交付包位于其他位置时设置 `SMART_PARK_PACKAGE_DIR`。

```bash
pnpm sync:dongguan-map
pnpm sync:smart-park
pnpm verify:scene-release
```

本地使用 `pnpm serve:prod` 预览生产构建时，Vite 将 `/admin-api` 和 `/infra/ws` 代理到 `http://127.0.0.1:48080`；后端地址不同可设置 `PREVIEW_BACKEND_URL`。生产部署仍需由站点反向代理提供同一路径。

## 协议边界

宿主只接受同源且 `event.source` 等于当前 iframe 的 ready、error、selection、point-click 和 request-back 消息；过期场景、版本不匹配或非法字段只进入降级态，不清空业务 HUD。宿主向地图发送 zoom/reset/points 命令，points 仅包含物业点；向园区不发送业务数据。

浏览器级 WebGL 上下文丢失、资源失败、超时和协议/版本异常仍需在目标部署环境执行手工验收；仓库当前没有自动化前端 E2E 运行器。
