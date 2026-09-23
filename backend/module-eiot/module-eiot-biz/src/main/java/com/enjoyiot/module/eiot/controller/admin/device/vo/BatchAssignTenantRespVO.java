package com.enjoyiot.module.eiot.controller.admin.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 批量分配租户响应 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAssignTenantRespVO {

    @Schema(description = "成功分配租户的设备数量", example = "5")
    private Integer successCount;

    @Schema(description = "因已有租户而跳过的设备数量", example = "3")
    private Integer skippedCount;

    @Schema(description = "请求的设备总数", example = "8")
    private Integer totalCount;
}
