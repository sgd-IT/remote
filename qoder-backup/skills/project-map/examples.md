# project-map 示例：一次完整建图（精简版）

> 场景：多仓工作区，3 个 git 仓库（Java 后端 + Vue3 管理台 + uni-app 移动端），工作区根目录不是 git 仓库。

## 第 2 步产物：对话版地图（节选）

```markdown
【demo-workspace·project-map】

一、系统全景图
PC 管理台(Vue3) ──┐
                  ├─ HTTP /admin-api ──> Spring Boot(:48080) ──> MySQL / Redis / 时序库
移动端(uni-app) ──┘         ↑
                            └── WebSocket /infra/ws（实时推送）
外部集成：短信网关 / 地图服务 / AI 模型

三、业务域 × 端（节选）
| 业务域 | 后端入口 | 前端页面 | 移动端 |
|---|---|---|---|
| 告警处置 | firewatch/AlarmOrderController | firewatch/alarm-order | alarm-center / alarm-detail |
| 设备台账 | device/DeviceInfoController | eiot/deviceinfo | profile/device-list |

四、我要做 X → 改哪里（节选）
| 任务 | 改这里 | 注意 |
|---|---|---|
| 加一个告警字段 | 后端 Controller + 前端告警页 + API 类型文件 | 多仓都要动，先改后端契约 |
| 改数据库结构 | _sql/ 迁移目录 + 本地 SQL 脚本 | 目标库执行前先备份 |

六、已知坑（节选）
- 文档 X 引用的 planning 目录不存在（文档漂移，已记录待修）
- 巨型文件：alarm-order 页 3000+ 行，改动只读关键段，不通读
```

## 第 3 步产物：落盘清单（经用户确认后）

1. 三个仓根各一份 `AGENTS.md`（各 <40 行）
2. 主仓 `docs/INDEX.md`：业务域表 + 改哪里表 + 已知坑
3. `docs/adr/` 追溯性 ADR ×3（多套并存实现 / 归属粒度取舍 / 成功码约定）
4. 可选：1 张核心链路 Mermaid 时序图
5. 全部文件保持未提交状态，由用户决定何时提交

## 第 4 步验证记录（示例）

- 路径复核：INDEX.md 引用的 30 个路径全部存在 ✓
- 密钥扫描：无 ✓
- 30 秒自测："告警弹窗改哪个文件？"查表 10 秒定位 ✓
- git status：仅新增上述文件，无其他改动，未提交 ✓
