package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 火警处置工单创建 Request VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 火警处置工单创建 Request VO")
@Data
public class AlarmOrderCreateReqVO {

    @Schema(description = "关联告警记录ID")
    private Long alertRecordId;

    @Schema(description = "物业ID")
    private Long propertyId;

    @Schema(description = "物业名称")
    private String propertyName;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "告警类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "告警类型不能为空")
    private String alarmType;

    @Schema(description = "告警级别")
    private String alarmLevel;

    @Schema(description = "告警时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "告警时间不能为空")
    private LocalDateTime alarmTime;

    @Schema(description = "告警位置")
    private String alarmLocation;

    @Schema(description = "告警描述")
    private String alarmDescription;

    @Schema(description = "告警数据(JSON)")
    private String alarmData;

    @Schema(description = "视频设备ID")
    private Long videoDeviceId;

}
