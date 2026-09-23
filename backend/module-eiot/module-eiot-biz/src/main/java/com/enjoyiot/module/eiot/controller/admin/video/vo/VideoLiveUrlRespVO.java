package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "视频流地址响应对象（FLV + HLS 双协议）")
public class VideoLiveUrlRespVO {

    @Schema(description = "FLV 实时预览流地址", example = "http://host:7886/live/cameraid/xxx%240/substream/2.flv?token=xxx")
    private String flvUrl;

    @Schema(description = "HLS 流地址 (m3u8)，用于回放", example = "http://host:7086/live/.../m3u8?token=xxx")
    private String url;

    @Schema(description = "Token（HLS 协议已拼接到 URL）", example = "abc123")
    private String token;

    @Schema(description = "TrackId（RTSP 协议专用）", example = "track001")
    private String trackId;
}
