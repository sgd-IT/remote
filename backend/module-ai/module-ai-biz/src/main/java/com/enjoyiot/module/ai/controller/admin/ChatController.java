package com.enjoyiot.module.ai.controller.admin;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.ai.controller.admin.vo.ChatReqVO;
import com.enjoyiot.module.ai.controller.admin.vo.ChatRespVO;
import com.enjoyiot.module.ai.service.AiToolService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 对话")
@RestController
@RequestMapping("/ai/chat")
@Validated
@Slf4j
public class ChatController {

    @Value("${ai.llm.url:http://**:8801/v1/chat/completions}")
    private String llmUrl;

    @Value("${ai.llm.model:qwen3.5-35b}")
    private String llmModel;

    @Value("${ai.llm.api-key:**}")
    private String llmApiKey;

    @Value("${ai.llm.timeout-ms:120000}")
    private int timeoutMs;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private AiToolService aiToolService;

    @PostMapping("/completions")
    @Operation(summary = "AI 对话（非流式，支持工具调用）")
    public CommonResult<ChatRespVO> chat(@Valid @RequestBody ChatReqVO reqVO) {
        try {
            // 注入工具描述到 system prompt
            injectToolPrompt(reqVO);
            // 多轮工具调用循环（最多 5 轮）
            int maxRounds = 5;
            String finalContent = null;
            for (int round = 0; round < maxRounds; round++) {
                ObjectNode body = buildRequestBody(reqVO, false);
                String response = doPost(llmUrl, body);
                JsonNode root = objectMapper.readTree(response);
                JsonNode choice = root.path("choices").path(0);
                String content = choice.path("message").path("content").asText("");
                String finishReason = choice.path("finish_reason").asText(null);

                // 检查是否包含工具调用
                ToolCallResult toolResult = parseToolCalls(content);
                if (toolResult == null) {
                    // 没有工具调用，直接返回
                    return success(ChatRespVO.builder()
                            .content(content)
                            .model(root.path("model").asText(llmModel))
                            .streaming(false)
                            .finishReason(finishReason)
                            .build());
                }

                // 有工具调用，执行工具并将结果追加到消息列表
                // 先把 AI 的完整回复加入消息
                reqVO.getMessages().add(new ChatReqVO.ChatMessage() {{
                    setRole("assistant");
                    setContent(content);
                }});

                // 执行每个工具调用并加入结果
                for (ParsedToolCall tc : toolResult.calls) {
                    String toolRet = aiToolService.executeTool(tc.name, tc.arguments);
                    log.info("[chat] tool call: name={}, result={}", tc.name,
                            toolRet.length() > 200 ? toolRet.substring(0, 200) + "..." : toolRet);
                    reqVO.getMessages().add(new ChatReqVO.ChatMessage() {{
                        setRole("user");
                        setContent("工具 " + tc.name + " 的返回结果：\n" + toolRet);
                    }});
                }
                finalContent = content;
            }
            // 超过最大轮次，返回最后一次内容
            return success(ChatRespVO.builder()
                    .content(finalContent != null ? finalContent : "工具调用轮次超限，请简化问题后重试。")
                    .model(llmModel)
                    .streaming(false)
                    .finishReason("max_rounds")
                    .build());
        } catch (Exception e) {
            log.error("[chat] AI 对话异常", e);
            return CommonResult.error(500, "AI 对话服务异常: " + e.getMessage());
        }
    }

    @PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话（流式 SSE）")
    public void chatStream(@Valid @RequestBody ChatReqVO reqVO, HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            ObjectNode body = buildRequestBody(reqVO, true);
            HttpURLConnection conn = (HttpURLConnection) URI.create(llmUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + llmApiKey);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(timeoutMs);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
                os.flush();
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            response.getWriter().write("data: [DONE]\n\n");
                            response.getWriter().flush();
                            break;
                        }
                        // 透传原始 SSE 数据
                        response.getWriter().write(line + "\n\n");
                        response.getWriter().flush();
                    }
                }
            }
        } catch (Exception e) {
            log.error("[chatStream] AI 流式对话异常", e);
            try {
                response.getWriter().write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                response.getWriter().flush();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 构建发送给 LLM 的请求体
     */
    private ObjectNode buildRequestBody(ChatReqVO reqVO, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", llmModel);
        body.put("stream", stream);
        body.put("temperature", reqVO.getTemperature() != null ? reqVO.getTemperature() : 0.7);
        body.put("max_tokens", reqVO.getMaxTokens() != null ? reqVO.getMaxTokens() : 2048);

        ArrayNode messagesNode = objectMapper.createArrayNode();
        for (ChatReqVO.ChatMessage msg : reqVO.getMessages()) {
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.getRole());
            if (msg.getContentParts() != null && !msg.getContentParts().isEmpty()) {
                // 多模态消息
                ArrayNode contentNode = objectMapper.createArrayNode();
                for (ChatReqVO.ContentPart part : msg.getContentParts()) {
                    ObjectNode partNode = objectMapper.createObjectNode();
                    partNode.put("type", part.getType());
                    if ("text".equals(part.getType())) {
                        partNode.put("text", part.getText());
                    } else if ("image_url".equals(part.getType()) && part.getImageUrl() != null) {
                        ObjectNode imageUrlNode = objectMapper.createObjectNode();
                        imageUrlNode.put("url", part.getImageUrl().getUrl());
                        partNode.set("image_url", imageUrlNode);
                    }
                    contentNode.add(partNode);
                }
                msgNode.set("content", contentNode);
            } else {
                // 纯文本消息
                msgNode.put("content", msg.getContent() != null ? msg.getContent() : "");
            }
            messagesNode.add(msgNode);
        }
        body.set("messages", messagesNode);
        return body;
    }

    /**
     * 注入工具描述到 system prompt
     */
    private void injectToolPrompt(ChatReqVO reqVO) {
        if (reqVO.getMessages() == null || reqVO.getMessages().isEmpty()) return;
        ChatReqVO.ChatMessage first = reqVO.getMessages().get(0);
        if ("system".equals(first.getRole())) {
            String existingPrompt = first.getContent() != null ? first.getContent() : "";
            if (!existingPrompt.contains(AiToolService.TOOL_CALL_PREFIX)) {
                first.setContent(existingPrompt + AiToolService.buildToolPrompt());
            }
        } else {
            // 没有 system 消息，插入一条
            reqVO.getMessages().add(0, new ChatReqVO.ChatMessage() {{
                setRole("system");
                setContent("你是安为智慧消防远程值守系统的AI分析助手。" + AiToolService.buildToolPrompt());
            }});
        }
    }

    /**
     * 解析 AI 回复中的工具调用
     * @return 如果包含工具调用则返回结果，否则 null
     */
    private ToolCallResult parseToolCalls(String content) {
        if (content == null || !content.contains(AiToolService.TOOL_CALL_PREFIX)) return null;
        java.util.List<ParsedToolCall> calls = new java.util.ArrayList<>();
        int searchFrom = 0;
        while (true) {
            int start = content.indexOf(AiToolService.TOOL_CALL_PREFIX, searchFrom);
            if (start < 0) break;
            int end = content.indexOf(AiToolService.TOOL_CALL_SUFFIX, start);
            if (end < 0) break;
            String json = content.substring(start + AiToolService.TOOL_CALL_PREFIX.length(), end).trim();
            try {
                JsonNode node = objectMapper.readTree(json);
                String name = node.path("name").asText("");
                String args = node.has("arguments") ? objectMapper.writeValueAsString(node.get("arguments")) : "{}";
                if (!name.isEmpty()) {
                    calls.add(new ParsedToolCall(name, args));
                }
            } catch (Exception e) {
                log.warn("[parseToolCalls] JSON parse failed: {}", json, e);
            }
            searchFrom = end + AiToolService.TOOL_CALL_SUFFIX.length();
        }
        return calls.isEmpty() ? null : new ToolCallResult(calls);
    }

    /** 解析出的工具调用结果 */
    private static class ToolCallResult {
        final java.util.List<ParsedToolCall> calls;
        ToolCallResult(java.util.List<ParsedToolCall> calls) { this.calls = calls; }
    }

    /** 解析出的单个工具调用 */
    private static class ParsedToolCall {
        final String name;
        final String arguments;
        ParsedToolCall(String name, String arguments) { this.name = name; this.arguments = arguments; }
    }

    /**
     * 发送 HTTP POST 请求并返回响应体
     */
    private String doPost(String url, ObjectNode body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + llmApiKey);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(timeoutMs);

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
            throw new RuntimeException("LLM API 返回错误 HTTP " + code + ": " + sb);
        }
    }
}
