package com.enjoyiot.module.eiot.service.video.icc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONUtil;
import com.enjoyiot.module.eiot.dal.dataobject.video.IccMediaServerDO;
import com.enjoyiot.module.eiot.service.video.icc.dto.IccStartVideoRespDTO;
import com.enjoyiot.module.eiot.service.video.icc.dto.IccTalkRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class IccOpenApiClient {

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile String magicId;
    private volatile Instant tokenExpireTime;
    private final Object tokenLock = new Object();

    public String getAccessToken(IccMediaServerDO config) {
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

    /**
     * hutool JSONUtil.toBean 会把 JSON 中的 null 解析为 JSONNull 对象，
     * 统一转换为 null，避免 (String) 强转抛 ClassCastException。
     */
    private static String strOrNull(Object v) {
        return (v == null || v instanceof JSONNull) ? null : String.valueOf(v);
    }

    private static Integer intOrNull(Object v) {
        return (v instanceof Number) ? ((Number) v).intValue() : null;
    }

    private void refreshToken(IccMediaServerDO config) {
        try {
            String publicKey = getPublicKey(config.getBaseUrl());
            String encryptedPassword = doEncryptPassword(config.getPassword(), publicKey);
            Map<String, Object> body = new HashMap<>();
            body.put("grant_type", "password");
            body.put("username", config.getUsername());
            body.put("password", encryptedPassword);
            body.put("client_id", config.getClientId());
            body.put("client_secret", config.getClientSecret());
            body.put("public_key", publicKey);

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
            this.accessToken = strOrNull(data.get("access_token"));
            this.refreshToken = strOrNull(data.get("refresh_token"));
            this.magicId = strOrNull(data.get("magicId"));
            Object expObj = data.get("expires_in");
            Long expiresIn = expObj instanceof Number ? ((Number) expObj).longValue() : 604799L;
            this.tokenExpireTime = Instant.now().plusSeconds(expiresIn);

            log.info("[IccOpenApiClient] Token刷新成功，有效期{}秒", expiresIn);
        } catch (Exception e) {
            log.error("[IccOpenApiClient] Token刷新失败", e);
            throw new RuntimeException("ICC平台连接异常，请联系管理员", e);
        }
    }

    public String getPublicKey(String baseUrl) {
        String url = baseUrl + "/evo-apigw/evo-oauth/1.0.0/oauth/public-key";
        HttpResponse response = HttpRequest.get(url)
                .timeout(10000)
                .execute();
        String respBody = response.body();
        log.info("[IccOpenApiClient] PublicKey response: {}", respBody);

        Map<String, Object> result = JSONUtil.toBean(respBody, Map.class);
        Boolean success = (Boolean) result.get("success");
        if (!Boolean.TRUE.equals(success)) {
            String errMsg = (String) result.getOrDefault("errMsg", "Unknown error");
            throw new RuntimeException("ICC获取公钥失败: " + errMsg);
        }

        Map<String, Object> data = (Map<String, Object>) result.get("data");
        return strOrNull(data.get("publicKey"));
    }

    private String doEncryptPassword(String password, String publicKey) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoded));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);

        byte[] inputArray = password.getBytes(StandardCharsets.UTF_8);
        int inputLength = inputArray.length;
        int maxEncryptBlock = 117;
        int offSet = 0;
        byte[] resultBytes = {};
        byte[] cache;

        while (inputLength - offSet > 0) {
            if (inputLength - offSet > maxEncryptBlock) {
                cache = cipher.doFinal(inputArray, offSet, maxEncryptBlock);
                offSet += maxEncryptBlock;
            } else {
                cache = cipher.doFinal(inputArray, offSet, inputLength - offSet);
                offSet = inputLength;
            }
            resultBytes = Arrays.copyOf(resultBytes, resultBytes.length + cache.length);
            System.arraycopy(cache, 0, resultBytes, resultBytes.length - cache.length, cache.length);
        }
        return Base64.getEncoder().encodeToString(resultBytes);
    }

    public IccStartVideoRespDTO startVideo(IccMediaServerDO config, String deviceId, String channelId, String streamType) {
        String token = getAccessToken(config);
        String url = config.getBaseUrl() + "/evo-apigw/admin/API/MTS/Video/StartVideo";

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("channelId", channelId);
        data.put("dataType", "1");
        data.put("streamType", streamType != null && !streamType.isEmpty() ? streamType : "2");

        Map<String, Object> body = new HashMap<>();
        body.put("data", data);

        log.info("[IccOpenApiClient] StartVideo request: url={}, body={}", url, JSONUtil.toJsonStr(body));

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "bearer " + token)
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
        dto.setUrl(strOrNull(respData.get("url")));
        dto.setToken(strOrNull(respData.get("token")));
        dto.setTrackId(strOrNull(respData.get("trackId")));
        dto.setSession(strOrNull(respData.get("session")));
        dto.setInnerIp(strOrNull(respData.get("innerIp")));
        dto.setWssDirect(intOrNull(respData.get("wssDirect")));
        dto.setNetFlag(strOrNull(respData.get("netFlag")));
        return dto;
    }

    /**
     * 开始语音对讲
     * <p>
     * 接口：POST /evo-apigw/admin/API/MTS/Audio/StartTalk
     * <p>
     * deviceCode 为通道编码第一个 $ 前的内容（设备编码）。
     * audioType 优先请求 2（G711A），以 ICC 返回的 audioType/sampleRate/gbDevice 为准。
     */
    public IccTalkRespDTO startTalk(IccMediaServerDO config, String deviceCode, String talkType, String audioType) {
        String token = getAccessToken(config);
        String url = config.getBaseUrl() + "/evo-apigw/admin/API/MTS/Audio/StartTalk";

        Map<String, Object> data = new HashMap<>();
        data.put("deviceCode", deviceCode);
        data.put("talkType", talkType);
        data.put("audioType", audioType);
        data.put("audioBit", "16");
        data.put("sampleRate", "8000");

        Map<String, Object> body = new HashMap<>();
        body.put("data", data);

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "bearer " + token)
                .body(JSONUtil.toJsonStr(body))
                .timeout(10000)
                .execute();

        String respBody = response.body();
        log.info("[IccOpenApiClient] StartTalk response: {}", respBody);

        Map<String, Object> result = JSONUtil.toBean(respBody, Map.class);
        Integer code = ((Number) result.get("code")).intValue();
        if (code != 1000) {
            String desc = (String) result.getOrDefault("desc", "Unknown error");
            throw new RuntimeException("ICC开始对讲失败: " + desc);
        }

        Map<String, Object> respData = (Map<String, Object>) result.get("data");
        IccTalkRespDTO dto = new IccTalkRespDTO();
        dto.setUrl(strOrNull(respData.get("url")));
        dto.setToken(strOrNull(respData.get("token")));
        dto.setSession(strOrNull(respData.get("session")));
        dto.setAudioType(strOrNull(respData.get("audioType")));
        dto.setAudioBit(strOrNull(respData.get("audioBit")));
        dto.setSampleRate(strOrNull(respData.get("sampleRate")));
        dto.setGbDevice(intOrNull(respData.get("gbDevice")));
        return dto;
    }

    /**
     * 停止语音对讲
     * <p>
     * 接口：POST /evo-apigw/admin/API/MTS/Audio/StopTalk
     * <p>
     * session 为 StartTalk 返回的会话 ID，必须回传。
     */
    public boolean stopTalk(IccMediaServerDO config, String deviceCode, String talkType, String session) {
        String token = getAccessToken(config);
        String url = config.getBaseUrl() + "/evo-apigw/admin/API/MTS/Audio/StopTalk";

        Map<String, Object> data = new HashMap<>();
        data.put("deviceCode", deviceCode);
        data.put("talkType", talkType);
        data.put("session", session);

        Map<String, Object> body = new HashMap<>();
        body.put("data", data);

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "bearer " + token)
                .body(JSONUtil.toJsonStr(body))
                .timeout(10000)
                .execute();

        String respBody = response.body();
        log.info("[IccOpenApiClient] StopTalk response: {}", respBody);

        Map<String, Object> result = JSONUtil.toBean(respBody, Map.class);
        Integer code = ((Number) result.get("code")).intValue();
        if (code != 1000) {
            String desc = (String) result.getOrDefault("desc", "Unknown error");
            throw new RuntimeException("ICC停止对讲失败: " + desc);
        }
        return true;
    }

    /**
     * 获取实时视频流地址
     * <p>
     * 接口：POST /evo-apigw/admin/API/video/stream/realtime
     * <p>
     * channelId 格式为 ICC 4 段式：deviceId$1$0$channelNo
     * HLS 协议返回的 URL 需手动拼接 access_token
     */
    public String getRealtimeStreamUrl(IccMediaServerDO config, String channelCode, String streamType, String scheme) {
        String accessToken = getAccessToken(config);

        // realMonitor 接口当前 ICC 版本不支持（返回 404），暂时注释，直接走旧接口
        // String url = config.getBaseUrl() + "/evo-apigw/admin/videoService/realMonitor";
        // Map<String, Object> body = new HashMap<>();
        // body.put("channelCode", channelCode);
        // body.put("streamType", streamType);
        // body.put("scheme", scheme);
        //
        // HttpResponse response = HttpRequest.post(url)
        //         .header("Content-Type", "application/json")
        //         .header("Authorization", "bearer " + accessToken)
        //         .body(JSONUtil.toJsonStr(body))
        //         .timeout(10000)
        //         .execute();
        //
        // String respBody = response.body();
        // log.info("[IccOpenApiClient] realMonitor request: channelCode={}, scheme={}, status={}, response: {}",
        //         channelCode, scheme, response.getStatus(), respBody);
        //
        // if (response.getStatus() == 200 && respBody.trim().startsWith("{")) {
        //     return parseRealMonitorResponse(respBody, accessToken, channelCode, scheme);
        // }
        //
        // log.warn("[IccOpenApiClient] realMonitor 接口不可用 (HTTP {}), 降级到旧 stream/realtime 接口", response.getStatus());

        return getRealtimeStreamUrlLegacy(config, accessToken, channelCode, streamType, scheme);
    }

    // realMonitor 接口解析（当前 ICC 版本不支持，暂时注释）
    // private String parseRealMonitorResponse(String respBody, String accessToken, String channelCode, String scheme) {
    //     Map<String, Object> result = JSONUtil.toBean(respBody, Map.class);
    //     String code = result.get("code") != null ? String.valueOf(result.get("code")) : "";
    //     if (!"0".equals(code)) {
    //         String desc = (String) result.getOrDefault("desc", result.getOrDefault("errMsg", "Unknown error"));
    //         throw new RuntimeException("ICC获取实时流地址失败: " + desc);
    //     }
    //
    //     Map<String, Object> respData = (Map<String, Object>) result.get("data");
    //     String streamUrl = (String) respData.get("url");
    //
    //     String dataToken = (String) respData.get("token");
    //     String token = StrUtil.isNotBlank(dataToken) ? dataToken : accessToken;
    //     String separator = streamUrl.contains("?") ? "&" : "?";
    //     streamUrl = streamUrl + separator + "token=" + token;
    //
    //     log.info("[IccOpenApiClient] realMonitor 成功, channelCode={}, scheme={}, url={}", channelCode, scheme, streamUrl);
    //     return streamUrl;
    // }

    /**
     * 旧版获取实时流接口：POST /evo-apigw/admin/API/video/stream/realtime
     * <p>
     * 参数格式：{data: {channelId, streamType, type}}，code=1000 表示成功
     */
    private String getRealtimeStreamUrlLegacy(IccMediaServerDO config, String accessToken,
                                                String channelId, String streamType, String type) {
        String url = config.getBaseUrl() + "/evo-apigw/admin/API/video/stream/realtime";

        Map<String, Object> data = new HashMap<>();
        data.put("channelId", channelId);
        data.put("streamType", streamType);
        data.put("type", type);

        Map<String, Object> body = new HashMap<>();
        body.put("data", data);

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "bearer " + accessToken)
                .body(JSONUtil.toJsonStr(body))
                .timeout(10000)
                .execute();

        String respBody = response.body();
        log.info("[IccOpenApiClient] stream/realtime legacy request: channelId={}, type={}, status={}, response: {}",
                channelId, type, response.getStatus(), respBody);

        if (response.getStatus() != 200 || !respBody.trim().startsWith("{")) {
            throw new RuntimeException("ICC 实时流接口不可用 (HTTP " + response.getStatus() + ")，两个接口均无法访问");
        }

        Map<String, Object> result = JSONUtil.toBean(respBody, Map.class);
        Integer code = ((Number) result.get("code")).intValue();
        if (code != 1000) {
            String desc = (String) result.getOrDefault("desc", "Unknown error");
            throw new RuntimeException("ICC获取实时流地址失败: " + desc);
        }

        Map<String, Object> respData = (Map<String, Object>) result.get("data");
        String streamUrl = strOrNull(respData.get("url"));
        if (streamUrl == null) {
            throw new RuntimeException("ICC获取实时流地址失败: url 为空");
        }
        streamUrl = streamUrl + "?token=" + accessToken;

        log.info("[IccOpenApiClient] stream/realtime legacy 成功, channelId={}, type={}, url={}", channelId, type, streamUrl);
        return streamUrl;
    }

    /**
     * 构建 FLV 实时预览流地址
     * <p>
     * URL 格式：http://{host}:7886/live/cameraid/{deviceCode}%24{channelSeq}/substream/{streamType}.flv?token={accessToken}
     * 强制使用 HTTP 协议（端口 7886），避免自签名证书导致浏览器拦截
     */
    public String buildFlvStreamUrl(IccMediaServerDO config, String channelCode, String streamType) {
        String accessToken = getAccessToken(config);
        try {
            java.net.URI uri = new java.net.URI(config.getBaseUrl());
            String host = uri.getHost();

            String deviceCode = channelCode.substring(0, channelCode.indexOf("$"));
            String channelSeq = channelCode.substring(channelCode.lastIndexOf("$") + 1);
            String encodedDevice = URLEncoder.encode(deviceCode, StandardCharsets.UTF_8);

            String flvUrl = String.format("http://%s:7886/live/cameraid/%s%%24%s/substream/%s.flv?token=%s",
                    host, encodedDevice, channelSeq, streamType, accessToken);

            log.info("[IccOpenApiClient] FLV URL: channelCode={}, streamType={}, url={}", channelCode, streamType, flvUrl);
            return flvUrl;
        } catch (Exception e) {
            log.error("[IccOpenApiClient] FLV URL 构建失败, baseUrl={}", config.getBaseUrl(), e);
            throw new RuntimeException("FLV 流地址构建失败: " + e.getMessage());
        }
    }
// ... existing code ...

    public List<Map<String, Object>> getDeviceList(IccMediaServerDO config) {
        String token = getAccessToken(config);
        String url = config.getBaseUrl() + "/evo-apigw/evo-brm/1.2.0/device/subsystem/page";

        Map<String, Object> body = new HashMap<>();
        body.put("pageNum", 1);
        body.put("pageSize", 1000);

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "bearer " + token)
                .body(JSONUtil.toJsonStr(body))
                .timeout(10000)
                .execute();

        String respBody = response.body();
        log.info("[IccOpenApiClient] GetDeviceList response status: {}, body: {}", response.getStatus(), respBody);

        if (response.getStatus() != 200 || !respBody.trim().startsWith("{")) {
            throw new RuntimeException("ICC设备列表接口不可用 (HTTP " + response.getStatus() + ")，请确认ICC平台地址和API路径正确");
        }

        Map<String, Object> result = JSONUtil.toBean(respBody, Map.class);
        Boolean success = (Boolean) result.get("success");
        if (!Boolean.TRUE.equals(success)) {
            String errMsg = (String) result.getOrDefault("errMsg", "Unknown error");
            throw new RuntimeException("ICC获取设备列表失败: " + errMsg);
        }

        Map<String, Object> respData = (Map<String, Object>) result.get("data");
        if (respData == null) {
            return java.util.Collections.emptyList();
        }

        List<Map<String, Object>> devices = (List<Map<String, Object>>) respData.get("pageData");
        return devices != null ? devices : java.util.Collections.emptyList();
    }
}
