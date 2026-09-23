package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "远程控制台设备树设备项 VO")
public class RemoteConsoleDeviceTreeItemRespVO {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "品牌代码（主机品牌）")
    private String brandCode;

    @Schema(description = "设备型号（主机型号）")
    private String model;

    @Schema(description = "在线状态：0=离线 1=在线")
    private Integer state;

    @Schema(description = "告警状态：alarm/fault/online/offline")
    private String alarmStatus;
}
