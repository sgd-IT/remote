package com.enjoyiot.module.eiot.controller.admin.callpush.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 话单推送请求 VO
 * <p>
 * 字段与《话单推送接口文档 V2.6》1.1.2 发送参数说明一一对应。
 * 能力开放平台 → 服务商（本系统），HTTP POST + JSON。
 *
 * @author EnjoyIot
 */
@Schema(description = "话单推送请求 VO（能力开放平台 → 服务商）")
@Data
public class CallRecordPushReqVO {

    @Schema(description = "通话唯一标志", required = true)
    private String sessionid;

    @Schema(description = "显示号码")
    private String displayNbr;

    @Schema(description = "主叫号码")
    private String callerNbr;

    @Schema(description = "被叫号码")
    private String calledNbr;

    @Schema(description = "业务号码", required = true)
    private String serviceNbr;

    @Schema(description = "业务号码B")
    private String serviceNbrB;

    @Schema(description = "拨打时间(YYYYMMDDHH24MISS)")
    private String dialTime;

    @Schema(description = "振铃时间(YYYYMMDDHH24MISS)")
    private String ringTime;

    @Schema(description = "开始时间(YYYYMMDDHH24MISS)")
    private String startTime;

    @Schema(description = "结束时间(YYYYMMDDHH24MISS)")
    private String endTime;

    @Schema(description = "通话时长(秒)")
    private String duration;

    @Schema(description = "通话时长(分钟)")
    private String minDuration;

    @Schema(description = "话单类型(yly-caller/yly-callee/yzj-caller/yzj-callee/recyzj-caller/recyzj-callee/gzh-caller/gzh-callee)")
    private String serviceType;

    @Schema(description = "通话响应状态(NO ANSWER/FAILED/BUSY/ANSWERED/CONGESTION)")
    private String disposition;

    @Schema(description = "混合录音文件地址")
    private String voiceAddress;

    @Schema(description = "主叫一方录音文件地址")
    private String voiceAddressIn;

    @Schema(description = "被叫一方录音文件地址")
    private String voiceAddressOut;

    @Schema(description = "混合录音文件md5值")
    private String md5Value;

    @Schema(description = "主叫录音文件md5值")
    private String md5ValueIn;

    @Schema(description = "被叫录音文件md5值")
    private String md5ValueOut;

    @Schema(description = "分机号")
    private String extensionNum;

    @Schema(description = "用户自定义数据")
    private String userData;

    @Schema(description = "上平台时间(YYYY-MM-DD HH:MM:SS)")
    private String callIn;

    @Schema(description = "应答时刻(YYYY-MM-DD HH:MM:SS)")
    private String answer;

    @Schema(description = "挂机时刻(YYYY-MM-DD HH:MM:SS)")
    private String hungUp;

    @Schema(description = "挂断事件发起方(0主叫 1被叫)")
    private String hungUpOriginator;

    @Schema(description = "响铃时刻(YYYY-MM-DD HH:MM:SS)")
    private String startRing;
}
