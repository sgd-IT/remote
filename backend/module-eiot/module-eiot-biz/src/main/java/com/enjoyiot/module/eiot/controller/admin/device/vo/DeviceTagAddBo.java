package com.enjoyiot.module.eiot.controller.admin.device.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "DeviceTagAddBo")
@Data
public class DeviceTagAddBo {

    @Schema(description = "设备")
    private String deviceId;

    @Schema(description = "tag id")

    private String id;
    @Schema(description = "tag名称")

    private String name;
    @Schema(description = "tag值")

    private String value;
}
