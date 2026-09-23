package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 值班记录创建 Request VO")
@Data
public class FireDutyRecordCreateReqVO {

    @Schema(description = "关联排班ID", required = true)
    @NotNull(message = "排班ID不能为空")
    private Long scheduleId;

    @Schema(description = "所属物业ID", required = true)
    @NotNull(message = "所属物业ID不能为空")
    private Long propertyId;

    @Schema(description = "记录时间")
    private LocalDateTime recordTime;

    @Schema(description = "记录类型: patrol-巡查记录, alarm-告警处理, handover-交接班, other-其他", required = true)
    @NotBlank(message = "记录类型不能为空")
    private String recordType;

    @Schema(description = "记录标题", required = true)
    @NotBlank(message = "记录标题不能为空")
    private String title;

    @Schema(description = "记录内容", required = true)
    @NotBlank(message = "记录内容不能为空")
    private String content;

    @Schema(description = "关联设备ID")
    private Long deviceId;

    @Schema(description = "关联设备名称")
    private String deviceName;

    @Schema(description = "关联工单ID")
    private Long orderId;

    @Schema(description = "记录人ID")
    private Long recorderId;

    @Schema(description = "记录人姓名")
    private String recorderName;

    @Schema(description = "附件(JSON数组)")
    private String attachments;

    @Schema(description = "状态: normal-正常, abnormal-异常")
    private String status;

    @Schema(description = "备注")
    private String remark;

}
