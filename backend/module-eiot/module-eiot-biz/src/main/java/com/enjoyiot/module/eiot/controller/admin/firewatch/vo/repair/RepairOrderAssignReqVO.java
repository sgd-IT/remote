package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维修工单派单 Request VO")
@Data
public class RepairOrderAssignReqVO {

    @Schema(description = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long id;

    @Schema(description = "排查截止时间")
    private LocalDateTime deadline;
}
