# 四信 API 请求头鉴权签名实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修正 FourfaithRemoteControlClient 中的请求头生成逻辑，将占位值替换为按四信规则生成的动态鉴权 Header。

**Architecture:** 在 `createCommand` 方法内部直接生成 `requestId`、`timestamp` 和 `signature`，使用 Hutool 提供的工具类，改动范围最小。

**Tech Stack:** Java, Hutool (IdUtil, SecureUtil), Spring Boot

---

### Task 1: 修改 FourfaithRemoteControlClient 请求头生成逻辑

**Files:**
- Modify: `module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithRemoteControlClient.java`

- [ ] **Step 1: 读取当前文件内容并确认导入项**

文件路径：`module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithRemoteControlClient.java`

确认已有 Hutool 的 `HttpRequest`、`JSONUtil` 导入。需要新增的导入：
```java
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
```

- [ ] **Step 2: 在 `createCommand` 方法中插入签名生成代码**

定位到原代码第 79-89 行：
```java
String bodyJson = JSONUtil.toJsonStr(requestBody);

// 构造请求，补充鉴权 Header（占位，需根据四信实际鉴权方式调整）
HttpRequest request = HttpRequest.post(url)
    .header("Content-Type", "application/json")
    .header("XF-API-appid", apiProperties.getAppId())
    .header("XF-API-requestid", apiProperties.getAppId())
    .header("XF-API-timestamp", apiProperties.getAppId())
    .header("XF-API-signature", apiProperties.getAppSecret())
    .timeout(apiProperties.getTimeoutMs())
    .body(bodyJson);
```

替换为：
```java
String bodyJson = JSONUtil.toJsonStr(requestBody);

String requestId = IdUtil.simpleUUID();
String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
String signature = SecureUtil.md5(apiProperties.getAppSecret() + "_" + requestId + "_" + timestamp);

HttpRequest request = HttpRequest.post(url)
    .header("Content-Type", "application/json")
    .header("XF-API-appid", apiProperties.getAppId())
    .header("XF-API-requestid", requestId)
    .header("XF-API-timestamp", timestamp)
    .header("XF-API-signature", signature)
    .timeout(apiProperties.getTimeoutMs())
    .body(bodyJson);
```

- [ ] **Step 3: 添加必要的 import 语句**

在文件现有的 import 区域（第 1-16 行附近），在 Hutool 相关导入下方添加：
```java
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
```

- [ ] **Step 4: 编译验证**

在项目根目录（`anwei-fire-remote-monitor`）执行 Maven 编译，确认该模块编译通过：
```bash
cd d:\project\anwei-remote-monitor\anwei-fire-remote-monitor
mvn compile -pl module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp -am
```
预期结果：`BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithRemoteControlClient.java
git commit -m "feat: 四信API请求头按规则生成动态鉴权签名"
```

---

## Self-Review

**1. Spec coverage:**
- requestid 使用 UUID 生成 -> Task 1 Step 2 (`IdUtil.simpleUUID()`)
- timestamp 使用 Unix 秒级时间戳 -> Task 1 Step 2 (`System.currentTimeMillis() / 1000`)
- signature 使用 MD5(appSecret + "_" + requestid + "_" + timestamp) -> Task 1 Step 2 (`SecureUtil.md5(...)`)
- 仅修改 FourfaithRemoteControlClient.java -> Task 1 范围确认

**2. Placeholder scan:** 无 TBD/TODO/"implement later" 等占位内容。

**3. Type consistency：** `IdUtil.simpleUUID()` 返回 `String`，`SecureUtil.md5()` 返回 `String`，与请求头 `header()` 方法参数类型一致。
