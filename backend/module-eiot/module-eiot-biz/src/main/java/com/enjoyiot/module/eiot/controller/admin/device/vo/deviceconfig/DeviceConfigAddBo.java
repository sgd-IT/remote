package com.enjoyiot.module.eiot.controller.admin.device.vo.deviceconfig;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


@Schema(description = "DeviceConfigAddBo")
@Data
public class DeviceConfigAddBo {

    private static final long serialVersionUID = -1L;

    @Schema(description = "设备配置json内容")
    @Size(max = 65535, message = "设备配置json内容长度不正确")
    private String config;

    @Schema(description = "设备id")
    @NotNull(message = "设备id不能为空")
    private Long deviceId;

}
