package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.callrecord;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 话单通话记录 Response VO")
@Data
public class CallRecordRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "归属租户ID")
    private Long tenantId;

    @Schema(description = "通话唯一标志")
    private String sessionId;

    @Schema(description = "显示号码")
    private String displayNbr;

    @Schema(description = "主叫号码")
    private String callerNbr;

    @Schema(description = "被叫号码")
    private String calledNbr;

    @Schema(description = "业务号码")
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

    @Schema(description = "话单类型")
    private String serviceType;

    @Schema(description = "通话响应状态")
    private String disposition;

    @Schema(description = "混合录音文件地址")
    private String voiceAddress;

    @Schema(description = "主叫一方录音文件地址")
    private String voiceAddressIn;

    @Schema(description = "被叫一方录音文件地址")
    private String voiceAddressOut;

    @Schema(description = "分机号")
    private String extensionNum;

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

    @Schema(description = "匹配到的值班中心座机号码")
    private String matchPhone;

    @Schema(description = "通话方向：0=未匹配 1=呼出(座机为主叫) 2=呼入(座机为被叫)")
    private Integer callDirection;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
