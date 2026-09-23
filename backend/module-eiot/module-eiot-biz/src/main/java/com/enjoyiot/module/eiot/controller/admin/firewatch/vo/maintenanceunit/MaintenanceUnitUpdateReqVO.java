package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 维保单位更新 Request VO")
@Data
public class MaintenanceUnitUpdateReqVO {

    @Schema(description = "主键ID", required = true)
    @NotNull(message = "主键ID不能为空")
    private Long id;

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "资质等级: 甲级/乙级/丙级")
    private String qualificationLevel;

    @Schema(description = "备注")
    private String remark;
}
