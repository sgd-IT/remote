package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 维保计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PlanPageReqVO extends PageParam {

    @Schema(description = "计划编号")
    private String planNo;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "计划类型: monthly-月度, quarterly-季度, yearly-年度")
    private String planType;

    @Schema(description = "计划状态: pending-待执行, executing-执行中, completed-已完成, overdue-已逾期")
    private String planStatus;

    @Schema(description = "执行单位")
    private String executeUnit;

    @Schema(description = "物业名称")
    private String propertyName;
}
