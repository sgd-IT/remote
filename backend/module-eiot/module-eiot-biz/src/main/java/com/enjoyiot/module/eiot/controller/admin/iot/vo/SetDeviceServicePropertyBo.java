package com.enjoyiot.module.eiot.controller.admin.iot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * @author: regan
 * @description:
 * @date:created in 2023/6/17 12:17
 * @modificed by:
 */
@Schema(description = "SetDeviceServicePropertyBo")
@Data
public class SetDeviceServicePropertyBo {
    @Schema(description="设备id",required = true)
    @NotNull(message = "设备id不许为空")
    private Long deviceId;
    @Schema(description="参数")
    private Map<String, Object> args;

}
