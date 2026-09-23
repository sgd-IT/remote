package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维保计划创建 Request VO")
@Data
public class PlanCreateReqVO {

    @Schema(description = "计划编号", required = true)
    @NotBlank(message = "计划编号不能为空")
    private String planNo;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "物业ID")
    private Long propertyId;

    @Schema(description = "物业名称")
    private String propertyName;

    @Schema(description = "计划类型: monthly-月度, quarterly-季度, yearly-年度")
    private String planType;

    @Schema(description = "计划日期")
    private LocalDateTime planDate;

    @Schema(description = "执行人")
    private String executePerson;

    @Schema(description = "执行单位")
    private String executeUnit;

    @Schema(description = "备注")
    private String remark;
}
