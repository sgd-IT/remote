## anwei-fire-remote-monitor-mobile

智慧消防远程值守移动端

## 开发与发布速查

### 关键原则

本项目为 HBuilderX 工程结构（根目录无 package.json，无 CLI 编译链）：**编译只能由 HBuilderX 完成**，微信开发者工具只认编译产物——代码改动不经 HBuilderX 编译，产物不会更新。

### 两条编译链

| 场景 | HBuilderX 操作 | 产物目录 | 接口 BASE_URL |
| --- | --- | --- | --- |
| 日常调试 | 运行 → 运行到小程序模拟器 | `unpackage/dist/dev/mp-weixin` | 内网 `http://192.168.250.130:48080/admin-api` |
| 发行上传 | 发行 → 小程序-微信（仅适用于 uni-app） | `unpackage/dist/build/mp-weixin` | 生产 `https://fire-remote-admin.anxunwang.cn:18080/admin-api` |

### 常用命令（复制到 PowerShell 执行）

```powershell
& "D:\Tool\微信web开发者工具\cli.bat" open --project "d:\code\remote-monitor\anwei-fire-remote-monitor-mobile\unpackage\dist\build\mp-weixin"
```

- 作用：把微信开发者工具切到 build 产物目录（发行上传用）；**不需要先打开 HBuilderX**，工具未启动会自动拉起
- 打开调试窗口同理，把命令中的 `build` 换成 `dev` 即可
- 只负责「开窗口」，不负责编译：代码改动必须先经 HBuilderX 运行/发行
- 微信开发者工具安装路径：`D:\Tool\微信web开发者工具\`（CLI 为同目录 `cli.bat`）

### 发行上传流程

1. HBuilderX：发行 → 小程序-微信，填版本号与描述（自动编译 build 产物并尝试打开开发者工具）
2. 工具窗口未自动切到 build 目录时，执行「常用命令」节命令
3. 确认工具左上角项目路径为 `...\unpackage\dist\build\mp-weixin`
4. 自检产物 `utils/request.js`：BASE_URL 应为生产域名（若为内网 IP 说明拿错了包）
5. 工具右上角「上传」填版本号 → 微信公众平台（mp.weixin.qq.com）提交审核

### 常见问题

| 现象 | 原因 | 处置 |
| --- | --- | --- |
| 800059 `app.js file not found` | 开发者工具打开的是源码根目录 | 源码根目录没有 app.js（只在产物里）；执行「常用命令」节命令切换窗口 |
| 「在该目录下未找到 app.json」 | 根目录 project.config.json 的 miniprogramRoot 被同步进产物 | 根目录永远不加 miniprogramRoot；产物里若出现，删除该字段 |
| HBuilderX 无法调起开发者工具 / ERR_INVALID_ARG_TYPE | 工具服务端口未开启 | 工具：设置 → 安全设置 → 服务端口 → 开启 |

