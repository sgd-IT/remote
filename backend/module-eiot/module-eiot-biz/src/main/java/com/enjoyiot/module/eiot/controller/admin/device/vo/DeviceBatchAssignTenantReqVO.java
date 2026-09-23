package com.enjoyiot.module.eiot.controller.admin.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 设备批量分配租户 Request VO")
@Data
public class DeviceBatchAssignTenantReqVO {

    @Schema(description = "设备ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2, 3]")
    @NotEmpty(message = "设备ID列表不能为空")
    private List<Long> deviceIds;

    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
}
