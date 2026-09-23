package com.enjoyiot.module.ai.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - AI 对话响应 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRespVO {

    @Schema(description = "AI 回复内容")
    private String content;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "是否流式片段")
    private Boolean streaming;

    @Schema(description = "完成原因: stop / length / null(流式中间片段)")
    private String finishReason;
}
