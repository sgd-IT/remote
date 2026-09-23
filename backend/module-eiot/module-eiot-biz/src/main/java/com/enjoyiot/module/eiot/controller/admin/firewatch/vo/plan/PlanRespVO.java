package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维保计划 Response VO")
@Data
@ExcelIgnoreUnannotated
public class PlanRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "计划编号")
    @ExcelProperty("计划编号")
    private String planNo;

    @Schema(description = "计划名称")
    @ExcelProperty("计划名称")
    private String planName;

    @Schema(description = "物业ID")
    private Long propertyId;

    @Schema(description = "物业名称")
    @ExcelProperty("所属企业")
    private String propertyName;

    @Schema(description = "计划类型: monthly-月度, quarterly-季度, yearly-年度")
    @ExcelProperty("计划类型")
    private String planType;

    @Schema(description = "计划类型名称")
    private String planTypeName;

    @Schema(description = "计划日期")
    @ExcelProperty("计划日期")
    private LocalDateTime planDate;

    @Schema(description = "执行人")
    @ExcelProperty("执行人")
    private String executePerson;

    @Schema(description = "执行单位")
    @ExcelProperty("执行单位")
    private String executeUnit;

    @Schema(description = "计划状态: pending-待执行, executing-执行中, completed-已完成, overdue-已逾期")
    @ExcelProperty("计划状态")
    private String planStatus;

    @Schema(description = "计划状态名称")
    private String planStatusName;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
