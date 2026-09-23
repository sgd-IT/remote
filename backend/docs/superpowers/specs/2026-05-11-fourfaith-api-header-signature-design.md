# 四信 API 请求头鉴权签名设计

## 背景

`FourfaithRemoteControlClient` 在调用四信 `createHardwareCommand` 接口时，请求头中的 `XF-API-requestid`、`XF-API-timestamp`、`XF-API-signature` 均为占位值，需按四信实际鉴权规则生成。

## 目标

修正请求头生成逻辑，使每次请求都携带合法的四信鉴权 Header。

## 方案

采用**方法内直接生成**（方案 A），改动范围最小，当前模块仅有一个 HTTP 调用入口，无需过早抽象。

## 签名规则

- **appid**：从配置读取 `apiProperties.getAppId()`
- **requestid**：使用 `IdUtil.simpleUUID()` 生成（32 位无横线小写 UUID）
- **timestamp**：使用 Unix 秒级时间戳，`System.currentTimeMillis() / 1000`，转为字符串
- **signature**：`MD5(appSecret + "_" + requestid + "_" + timestamp)`，使用 Hutool `SecureUtil.md5(...)` 计算

## 改动文件

- `module-eiot/iot-components/iot-component-gbt26875-fourfaith-tcp/src/main/java/com/enjoyiot/eiot/component/gbt/fourfaith/tcp/service/FourfaithRemoteControlClient.java`

## 关键代码变更

```java
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

## 依赖

依赖项目已引入的 Hutool 库（`cn.hutool.core.util.IdUtil`、`cn.hutool.crypto.SecureUtil`）。
