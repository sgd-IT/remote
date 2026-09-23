package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "远程控制台设备详情响应 VO")
public class RemoteConsoleDeviceDetailRespVO {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "在线状态：0=离线 1=在线")
    private Integer state;

    @Schema(description = "告警状态")
    private String alarmStatus;

    @Schema(description = "控制模式：manual/auto")
    private String mode;
}
