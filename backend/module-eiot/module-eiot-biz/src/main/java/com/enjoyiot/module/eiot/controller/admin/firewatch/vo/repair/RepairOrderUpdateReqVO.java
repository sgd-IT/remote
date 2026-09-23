package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维修工单更新 Request VO")
@Data
public class RepairOrderUpdateReqVO {

    @Schema(description = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long id;

    @Schema(description = "故障类型")
    private String faultType;

    @Schema(description = "故障描述")
    private String faultDescription;

    @Schema(description = "故障图片(JSON数组)")
    private String faultImages;

    @Schema(description = "紧急程度")
    private String urgencyLevel;

    @Schema(description = "排查截止时间")
    private LocalDateTime deadline;
}
