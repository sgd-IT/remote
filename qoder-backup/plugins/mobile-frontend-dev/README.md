# 移动端前端开发套件 (mobile-frontend-dev)

## 功能

将两个 skill 打包为一个插件，在移动端和前端开发时**同时触发**：

1. **ui-ux-pro-max** — UI/UX 设计智能指南
   - 50+ 设计风格、97 配色方案、57 字体搭配、99 UX 准则、25 图表类型
   - 覆盖 React、Next.js、Vue、Svelte、SwiftUI、React Native、Flutter、Tailwind、shadcn/ui 9 大技术栈
   - 含无障碍、触控交互、性能、布局响应式等优先级规则

2. **uniapp** — uni-app 跨平台开发框架参考
   - 基于 uni-app 官方文档生成（2026-01-29）
   - 核心配置：pages.json、manifest.json、条件编译、分包
   - 完整 API 参考：路由、网络、存储、媒体、设备、WebSocket 等 50+ 参考文档

## 触发条件

当进行以下开发工作时，两个 skill 同时激活：
- 移动端 App 开发（uni-app）
- 微信小程序开发
- Vue 前端页面开发
- UI/UX 设计与组件实现

## 来源

- ui-ux-pro-max: github.com/brixtonpham/claude-config (skills/0-ui-ux-pro-max)
- uniapp: github.com/antfu/skills (基于 https://gitcode.com/dcloud/unidocs-zh 生成, author: hairy)

## 省略的文件

- ui-ux-pro-max 的 `scripts/search.py` 及 CSV 数据文件未包含在源码包中（原仓库路径 skills/ui-ux-pro-max/scripts/），SKILL.md 中的 CLI 搜索命令不可用，但内联的设计规则和检查清单完整保留
- uniapp 的 `GENERATION.md`（生成说明，非功能文件）
