package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "远程控制台设备参数响应 VO")
public class RemoteConsoleDeviceParamRespVO {

    @Schema(description = "参数编码")
    private String code;

    @Schema(description = "参数名称")
    private String name;

    @Schema(description = "参数值")
    private Object value;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "图标")
    private String icon;
}
