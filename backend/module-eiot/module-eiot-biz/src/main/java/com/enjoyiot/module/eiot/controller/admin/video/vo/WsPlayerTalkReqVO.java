package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * WSPlayer 对讲请求对象
 */
@Data
@Schema(description = "WSPlayer 对讲请求对象")
public class WsPlayerTalkReqVO {

    @Schema(description = "设备编码（通道编码第一个 $ 前内容）", example = "1000014")
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    @Schema(description = "对讲类型：1-设备对讲 2-通道对讲", example = "1")
    private String talkType = "1";

    @Schema(description = "音频类型：2-G711A 4-G711U（默认 G711A，以 ICC 返回为准）", example = "2")
    private String audioType = "2";

    @Schema(description = "通道对讲时的通道序号（talkType=2 时选填）", example = "0")
    private String channelSeq;

    @Schema(description = "停止对讲时的会话 ID（StartTalk 返回）", example = "1061")
    private String session;
}
