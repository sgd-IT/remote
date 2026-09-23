package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 值班管理创建 Request VO")
@Data
public class FireDutyScheduleCreateReqVO {

    @Schema(description = "所属物业ID", required = true)
    @NotNull(message = "所属物业ID不能为空")
    private Long propertyId;

    @Schema(description = "排班日期", required = true)
    @NotNull(message = "排班日期不能为空")
    private LocalDate scheduleDate;

    @Schema(description = "班次类型: day-白班, night-夜班, wholeday-全天班", required = true)
    @NotBlank(message = "班次类型不能为空")
    private String shiftType;

    @Schema(description = "班次名称")
    private String shiftName;

    @Schema(description = "开始时间(格式: HH:mm)", required = true)
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @Schema(description = "结束时间(格式: HH:mm)", required = true)
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @Schema(description = "值班人ID", required = true)
    @NotNull(message = "值班人不能为空")
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
