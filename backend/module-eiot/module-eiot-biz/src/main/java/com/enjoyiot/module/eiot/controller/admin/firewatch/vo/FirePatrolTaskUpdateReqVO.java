package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 巡检任务更新 Request VO")
@Data
public class FirePatrolTaskUpdateReqVO {

    @Schema(description = "任务ID", required = true)
    @NotNull(message = "任务ID不能为空")
    private Long id;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "计划日期")
    private LocalDate planDate;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "巡检人ID")
    private Long patrolUserId;

    @Schema(description = "巡检人姓名")
    private String patrolUserName;

    @Schema(description = "巡检人电话")
    private String patrolUserPhone;

    @Schema(description = "备注")
    private String remark;

}
