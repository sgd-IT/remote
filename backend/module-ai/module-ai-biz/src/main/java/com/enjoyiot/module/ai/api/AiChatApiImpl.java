package com.enjoyiot.module.ai.api;

import com.enjoyiot.module.ai.service.LlmClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 对话 API 实现（供其他模块进程内调用）
 * <p>
 * 与 HTTP 接口 /ai/chat/completions 共用 {@link LlmClient} 的 LLM 配置与调用逻辑；
 * 报告/摘要类场景采用偏保守的生成参数，不注入工具调用提示词。
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class AiChatApiImpl implements AiChatApi {

    /** 报告分析场景：偏低温度以保证内容稳定性 */
    private static final double REPORT_TEMPERATURE = 0.5;
    /** 报告分析场景：两节内容的最大输出 token */
    private static final int REPORT_MAX_TOKENS = 2500;

    @Resource
    private LlmClient llmClient;

    @Override
    public String chatOnce(String systemPrompt, String userPrompt) {
        try {
            long start = System.currentTimeMillis();
            String content = llmClient.chatOnce(systemPrompt, userPrompt, REPORT_TEMPERATURE, REPORT_MAX_TOKENS);
            log.info("[AiChatApi] 单轮对话完成: 提示词长度={}, 输出长度={}, 耗时={}ms",
                    userPrompt != null ? userPrompt.length() : 0,
                    content != null ? content.length() : 0,
                    System.currentTimeMillis() - start);
            return content;
        } catch (Exception e) {
            throw new IllegalStateException("AI 对话服务调用失败: " + e.getMessage(), e);
        }
    }
}
