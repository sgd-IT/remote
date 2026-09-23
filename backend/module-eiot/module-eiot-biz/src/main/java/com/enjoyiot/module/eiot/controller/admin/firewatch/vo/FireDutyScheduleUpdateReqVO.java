package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 值班管理更新 Request VO")
@Data
public class FireDutyScheduleUpdateReqVO {

    @Schema(description = "排班ID", required = true)
    @NotNull(message = "排班ID不能为空")
    private Long id;

    @Schema(description = "所属物业ID")
    private Long propertyId;

    @Schema(description = "排班日期")
    private LocalDate scheduleDate;

    @Schema(description = "班次类型: day-白班, night-夜班, wholeday-全天班")
    private String shiftType;

    @Schema(description = "班次名称")
    private String shiftName;

    @Schema(description = "开始时间(格式: HH:mm)")
    private String startTime;

    @Schema(description = "结束时间(格式: HH:mm)")
    private String endTime;

    @Schema(description = "值班人ID")
    private Long dutyUserId;

    @Schema(description = "值班人姓名")
    private String dutyUserName;

    @Schema(description = "值班人电话")
    private String dutyUserPhone;

    @Schema(description = "值班人岗位")
    private String dutyUserPosition;

    @Schema(description = "备注")
    private String remark;

}
