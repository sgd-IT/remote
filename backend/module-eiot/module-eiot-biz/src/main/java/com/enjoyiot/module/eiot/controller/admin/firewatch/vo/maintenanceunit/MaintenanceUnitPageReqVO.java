package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 维保单位分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceUnitPageReqVO extends PageParam {

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "资质等级: 甲级/乙级/丙级")
    private String qualificationLevel;
}
