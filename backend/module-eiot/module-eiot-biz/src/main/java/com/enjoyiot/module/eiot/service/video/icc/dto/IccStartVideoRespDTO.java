package com.enjoyiot.module.eiot.service.video.icc.dto;

import lombok.Data;

@Data
public class IccStartVideoRespDTO {
    private String url;
    private String token;
    private String trackId;
    private String session;
    /** 流服务器内网 IP（WSPlayer 连接使用） */
    private String innerIp;
    /** 是否支持 WSS 直连：1-支持 0-不支持 */
    private Integer wssDirect;
    /** 网络标志（内外网映射信息） */
    private String netFlag;
}
