package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "远程控制操作日志响应 VO")
public class RemoteOperationLogRespVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "企业/物业ID")
    private Long propertyId;

    @Schema(description = "企业/物业名称")
    private String propertyName;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作名称")
    private String operationName;

    @Schema(description = "按钮名称")
    private String buttonName;

    @Schema(description = "功能类型：BUS(总线)/MULTI(多线)/GENERAL(通用)")
    private String functionType;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "复核人ID")
    private Long reviewerId;

    @Schema(description = "复核人姓名")
    private String reviewerName;

    @Schema(description = "命令状态：0=失败 1=成功 2=执行中")
    private Integer commandStatus;

    @Schema(description = "结果说明")
    private String resultMsg;

    @Schema(description = "请求参数JSON")
    private String requestParams;

    @Schema(description = "操作IP")
    private String ipAddress;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
}
