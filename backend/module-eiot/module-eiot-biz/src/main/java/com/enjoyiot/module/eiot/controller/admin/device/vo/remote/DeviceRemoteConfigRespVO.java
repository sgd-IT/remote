package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "设备远程配置响应 VO")
public class DeviceRemoteConfigRespVO {

    @Schema(description = "配置id")
    private Long id;

    @Schema(description = "设备id")
    private Long deviceId;

    @Schema(description = "品牌型号id")
    private Long brandModelId;

    @Schema(description = "品牌名称")
    private String brandName;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "是否设置了二次确认密码")
    private Boolean hasPassword;
}
