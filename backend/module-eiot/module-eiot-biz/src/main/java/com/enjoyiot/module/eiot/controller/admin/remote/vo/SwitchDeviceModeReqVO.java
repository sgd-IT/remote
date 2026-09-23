package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "切换手自动模式请求 VO")
public class SwitchDeviceModeReqVO {

    @NotBlank(message = "模式不能为空")
    @Schema(description = "模式：manual/auto")
    private String mode;

    @Schema(description = "切换原因")
    private String reason;
}
