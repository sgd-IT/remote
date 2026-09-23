package com.enjoyiot.module.eiot.service.video.icc.dto;

import lombok.Data;

/**
 * ICC 对讲接口（StartTalk）响应 DTO
 */
@Data
public class IccTalkRespDTO {
    /** 对讲 RTSP 流地址 */
    private String url;
    /** 一次性 token，拼接为 url?token=xxx */
    private String token;
    /** 会话 ID，停止对讲时必须回传 */
    private String session;
    /** 音频类型：2-G711A 4-G711U（以 ICC 返回为准） */
    private String audioType;
    /** 音频位数 */
    private String audioBit;
    /** 采样率 */
    private String sampleRate;
    /** 是否国标广播设备：1-广播 */
    private Integer gbDevice;
}
