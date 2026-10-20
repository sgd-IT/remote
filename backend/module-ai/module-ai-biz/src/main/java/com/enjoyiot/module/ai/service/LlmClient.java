package com.enjoyiot.module.ai.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * LLM HTTP 客户端（ChatController 与模块间 AI 对话 API 共用）
 * <p>
 * 统一维护 ai.llm.* 配置、鉴权头与超时设置。
 *
 * @author EnjoyIot
 */
@Slf4j
@Component
public class LlmClient {

    @Value("${ai.llm.url:http://14.22.81.203:8801/v1/chat/completions}")
    private String llmUrl;

    @Value("${ai.llm.model:qwen3.5-35b}")
    private String llmModel;

    @Value("${ai.llm.api-key:qwen-35b-a3b}")
    private String llmApiKey;

    @Value("${ai.llm.timeout-ms:120000}")
    private int timeoutMs;

    @Resource
    private ObjectMapper objectMapper;

    public String getLlmModel() {
        return llmModel;
    }

    /**
     * 构建请求体骨架（model / stream / temperature / max_tokens）
     */
    public ObjectNode createBody(boolean stream, Double temperature, Integer maxTokens) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", llmModel);
        body.put("stream", stream);
        body.put("temperature", temperature != null ? temperature : 0.7);
        body.put("max_tokens", maxTokens != null ? maxTokens : 2048);
        return body;
    }

    /**
     * 打开指向 LLM 对话接口的 HTTP 连接（已设置鉴权与超时，尚未发送请求）
     */
    public HttpURLConnection openConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(llmUrl).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + llmApiKey);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(timeoutMs);
        return conn;
    }

    /**
     * 发送非流式对话请求，返回原始响应 JSON 字符串；非 2xx 抛出异常
     */
    public String post(ObjectNode body) throws IOException {
        HttpURLConnection conn = openConnection();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(body));
            os.flush();
        }

        int code = conn.getResponseCode();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            if (code >= 200 && code < 300) {
                return sb.toString();
            }
            throw new IOException("LLM API 返回错误 HTTP " + code + ": " + sb);
        }
    }

    /**
     * 单轮对话：system + user 两条消息，返回 choices[0].message.content
     */
    public String chatOnce(String systemPrompt, String userPrompt,
                           double temperature, int maxTokens) throws IOException {
        ObjectNode body = createBody(false, temperature, maxTokens);
        ArrayNode messagesNode = objectMapper.createArrayNode();
        if (StrUtil.isNotBlank(systemPrompt)) {
            ObjectNode sysNode = objectMapper.createObjectNode();
            sysNode.put("role", "system");
            sysNode.put("content", systemPrompt);
            messagesNode.add(sysNode);
        }
        ObjectNode userNode = objectMapper.createObjectNode();
        userNode.put("role", "user");
        userNode.put("content", userPrompt != null ? userPrompt : "");
        messagesNode.add(userNode);
        body.set("messages", messagesNode);

        String response = post(body);
        JsonNode root = objectMapper.readTree(response);
        return root.path("choices").path(0).path("message").path("content").asText("");
    }
}
