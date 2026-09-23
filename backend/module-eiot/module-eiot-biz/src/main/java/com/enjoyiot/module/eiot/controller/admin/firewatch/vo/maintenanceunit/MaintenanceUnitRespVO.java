package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维保单位 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MaintenanceUnitRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "单位名称")
    @ExcelProperty("单位名称")
    private String unitName;

    @Schema(description = "联系人")
    @ExcelProperty("联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    @ExcelProperty("联系电话")
    private String contactPhone;

    @Schema(description = "地址")
    @ExcelProperty("地址")
    private String address;

    @Schema(description = "资质等级: 甲级/乙级/丙级")
    @ExcelProperty("资质等级")
    private String qualificationLevel;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
