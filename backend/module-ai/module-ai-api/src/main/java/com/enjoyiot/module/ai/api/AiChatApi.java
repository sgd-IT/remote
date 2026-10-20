package com.enjoyiot.module.ai.api;

/**
 * AI 对话 API（模块间进程内调用）
 * <p>
 * 与 HTTP 接口 /ai/chat/completions 共用同一套 LLM 配置（ai.llm.*），
 * 供月度报告等后端场景直接获取模型输出。
 *
 * @author EnjoyIot
 */
public interface AiChatApi {

    /**
     * 单轮非流式对话（system + user 两条消息）
     *
     * @param systemPrompt 系统提示词，可为空
     * @param userPrompt   用户提示词
     * @return 模型回复内容
     */
    String chatOnce(String systemPrompt, String userPrompt);
}
