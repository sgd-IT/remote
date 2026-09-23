package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * WSPlayer 实时预览流响应对象
 * <p>
 * 前端 WSPlayer 使用 realByUrl 播放：rtspURL = rtspUrl + "?token=" + token，
 * wsURL 由前端根据页面协议拼接（http 页面用 ws://{host}:9100，https 页面用 wss://{host}:9102）。
 */
@Data
@Schema(description = "WSPlayer 实时预览流响应对象")
public class WsPlayerStreamRespVO {

    @Schema(description = "RTSP 流地址（ICC StartVideo 返回，未拼 token，WSPlayer 内部会拼 ?token=）", example = "rtsp://host:9100/dss/monitor/param/cameraid=xxx")
    private String url;

    @Schema(description = "一次性 token，30 秒内有效且仅能使用一次", example = "1210")
    private String token;

    @Schema(description = "会话 ID", example = "1210")
    private String session;

    @Schema(description = "4 段式通道编码", example = "1000014$1$0$0")
    private String channelId;

    @Schema(description = "设备编码（对讲使用，通道编码第一个 $ 前内容）", example = "1000014")
    private String deviceCode;

    @Schema(description = "码流类型：1-主码流 2-辅码流1", example = "2")
    private String streamType;

    @Schema(description = "流服务器内网 IP（WSPlayer 连接使用）", example = "**")
    private String innerIp;

    @Schema(description = "是否支持 WSS 直连：1-支持 0-不支持", example = "0")
    private Integer wssDirect;

    @Schema(description = "网络标志（内外网映射信息）")
    private String netFlag;
}
