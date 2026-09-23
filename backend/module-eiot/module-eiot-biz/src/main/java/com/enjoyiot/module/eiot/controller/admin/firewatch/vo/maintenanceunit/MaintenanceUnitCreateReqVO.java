package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 维保单位创建 Request VO")
@Data
public class MaintenanceUnitCreateReqVO {

    @Schema(description = "单位名称", required = true)
    @NotBlank(message = "单位名称不能为空")
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
