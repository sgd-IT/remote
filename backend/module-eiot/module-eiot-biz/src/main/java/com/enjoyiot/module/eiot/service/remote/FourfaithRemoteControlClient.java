package com.enjoyiot.module.eiot.service.remote;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 四信 HTTP API 客户端
 *
 * 对接四信 createHardwareCommand 接口，将控制类型转换为设备可执行的指令码。
 */
@Slf4j
@Component
public class FourfaithRemoteControlClient {

    @Autowired
    private FourfaithApiProperties apiProperties;

    public FourfaithApiProperties getApiProperties() {
        return apiProperties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandResult {
        private boolean success;
        private String commandCode;
        private String errorMsg;
    }

    /**
     * 生成鉴权 Headers
     */
    private Map<String, String> buildAuthHeaders() {
        String appId = apiProperties.getAppId();
        String appSecret = apiProperties.getAppSecret();
        String requestId = IdUtil.fastSimpleUUID();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = DigestUtil.md5Hex(appSecret + "_" + requestId + "_" + timestamp);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("XF-API-appid", appId);
        headers.put("XF-API-requestid", requestId);
        headers.put("XF-API-timestamp", timestamp);
        headers.put("XF-API-signature", signature);
        return headers;
    }

    /**
     * 发送 POST 请求
     */
    private JSONObject post(String path, Object body) {
        String url = apiProperties.getUrl() + path;
        String bodyJson = JSONUtil.toJsonStr(body);
        Map<String, String> headers = buildAuthHeaders();

        HttpRequest request = HttpRequest.post(url)
            .timeout(apiProperties.getTimeoutMs())
            .body(bodyJson);
        headers.forEach(request::header);

        String response = request.execute().body();
        log.warn("四信API请求: url={}, body={}, response={}", url, bodyJson, response);
        return JSONUtil.parseObj(response);
    }

    /**
     * 获取所有设备功能指令清单
     */
    public JSONArray getAllHardwareDetailed() {
        JSONObject json = post("/api/v1/hardwareFunction/getAllHardwareDetailed", new HashMap<>());
        int code = json.getInt("code", -1);
        if (code != 0) {
            log.error("获取设备功能清单失败: {}", json.getStr("message"));
            return null;
        }
        return json.getJSONArray("data");
    }

    /**
     * 创建硬件控制指令
     *
     * @param functionId       四信功能ID
     * @param functionType     功能类型 (10:复位,20:消音,30:开关,40:总线开关,41:多线开关)
     * @param commandType      指令类型 (0:关,1:开)
     * @param functionValueMap 功能参数值 Map（key=paramCode, value=参数值）
     * @param brandCode        品牌代码
     * @param model            设备型号
     * @param version          协议版本
     * @return 指令创建结果
     */
    public CommandResult createCommand(String functionId, String functionType, String commandType,
                                       Map<String, String> functionValueMap,
                                       String brandCode, String model, String version) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("brandCode", brandCode);
            requestBody.put("model", model);
            requestBody.put("version", version);
    
            Map<String, Object> funcMap = new HashMap<>();
            funcMap.put("id", functionId);
            funcMap.put("functionValueMap", functionValueMap);
    
            requestBody.put("functionValueList", Collections.singletonList(funcMap));
    
            JSONObject json = post("/api/v1/hardwareFunction/createHardwareCommand", requestBody);
            int code = json.getInt("code", -1);
            if (code != 0) {
                return CommandResult.builder()
                    .success(false)
                    .errorMsg(json.getStr("message", "API调用失败"))
                    .build();
            }
    
            JSONArray dataArr = json.getJSONArray("data");
            if (dataArr == null || dataArr.isEmpty()) {
                return CommandResult.builder()
                    .success(false)
                    .errorMsg("API返回数据为空")
                    .build();
            }
    
            // 根据功能类型找到匹配的 data 项
            JSONObject matchedData = null;
            if (StrUtil.isNotBlank(functionType)) {
                for (int i = 0; i < dataArr.size(); i++) {
                    JSONObject item = dataArr.getJSONObject(i);
                    if (functionType.equals(item.getStr("type"))) {
                        matchedData = item;
                        break;
                    }
                }
            }
            // 未找到匹配则取第一条（兼容策略）
            if (matchedData == null) {
                return CommandResult.builder()
                        .success(false)
                        .errorMsg("功能类型为空")
                        .build();
            }
    
            JSONArray cmdList = matchedData.getJSONArray("functionCommandList");
            if (cmdList == null || cmdList.isEmpty()) {
                return CommandResult.builder()
                    .success(false)
                    .errorMsg("指令列表为空")
                    .build();
            }
    
            // 根据指令类型过滤匹配的 commandCode
            if (StrUtil.isNotBlank(commandType)) {
                for (int i = 0; i < cmdList.size(); i++) {
                    JSONObject cmd = cmdList.getJSONObject(i);
                    if (commandType.equals(cmd.getStr("type"))) {
                        String commandCode = cmd.getStr("commandCode");
                        return CommandResult.builder()
                            .success(true)
                            .commandCode(commandCode)
                            .build();
                    }
                }
                return CommandResult.builder()
                    .success(false)
                    .errorMsg("未找到指令类型为 [" + commandType + "] 的指令")
                    .build();
            }else {
                return CommandResult.builder()
                    .success(false)
                    .errorMsg("指令类型为空")
                    .build();
            }
    
        } catch (Exception e) {
            log.error("四信API调用异常", e);
            return CommandResult.builder()
                .success(false)
                .errorMsg("API调用异常: " + e.getMessage())
                .build();
        }
    }
}
