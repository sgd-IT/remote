package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 巡检任务创建 Request VO")
@Data
public class FirePatrolTaskCreateReqVO {

    @Schema(description = "所属物业ID", required = true)
    @NotNull(message = "所属物业ID不能为空")
    private Long propertyId;

    @Schema(description = "任务名称", required = true)
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @Schema(description = "任务类型: routine-日常巡检, special-专项检查, maintenance-维保检查", required = true)
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @Schema(description = "计划日期")
    private LocalDate planDate;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "巡检人ID", required = true)
    @NotNull(message = "巡检人不能为空")
    private Long patrolUserId;

    @Schema(description = "巡检人姓名")
    private String patrolUserName;

    @Schema(description = "巡检人电话")
    private String patrolUserPhone;

    @Schema(description = "巡检设备ID列表")
    private List<Long> deviceIds;

    @Schema(description = "备注")
    private String remark;

}
