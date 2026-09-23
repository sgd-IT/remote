package com.enjoyiot.module.eiot.controller.admin.iot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;


@Data
public class DeviceIdReqVo {
    @Schema(description = "设备id", required = true)
    @NotNull(message = "设备id不许为空")
    private Long deviceId;

}
