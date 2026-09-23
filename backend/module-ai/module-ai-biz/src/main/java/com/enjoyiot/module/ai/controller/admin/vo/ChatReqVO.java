package com.enjoyiot.module.ai.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - AI 对话请求 Request VO")
@Data
public class ChatReqVO {

    @Schema(description = "对话消息列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "消息列表不能为空")
    private List<ChatMessage> messages;

    @Schema(description = "是否流式输出", example = "false")
    private Boolean stream = false;

    @Schema(description = "多样性/随机性，越小越保守", example = "0.7")
    private Double temperature = 0.7;

    @Schema(description = "输出最大Tokens", example = "2048")
    private Integer maxTokens = 2048;

    @Schema(description = "AI 对话请求消息")
    @Data
    public static class ChatMessage {

        @Schema(description = "角色: system/user/assistant", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "角色不能为空")
        private String role;

        @Schema(description = "文本内容（纯文本对话时使用）")
        private String content;

        @Schema(description = "多模态内容列表（图片+文本混合时使用，优先于 content）")
        private List<ContentPart> contentParts;
    }

    @Schema(description = "多模态内容片段")
    @Data
    public static class ContentPart {

        @Schema(description = "内容类型: text / image_url", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "内容类型不能为空")
        private String type;

        @Schema(description = "文本内容（type=text 时使用）")
        private String text;

        @Schema(description = "图片URL（type=image_url 时使用）")
        private ImageUrl imageUrl;
    }

    @Schema(description = "图片URL")
    @Data
    public static class ImageUrl {

        @Schema(description = "图片URL地址", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "图片URL不能为空")
        private String url;
    }
}
