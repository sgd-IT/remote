package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "远程控制台设备状态统计响应 VO")
public class RemoteConsoleDeviceStatusStatsRespVO {

    @Schema(description = "设备总数")
    private Integer total;

    @Schema(description = "在线设备数")
    private Integer online;

    @Schema(description = "告警设备数")
    private Integer alarm;

    @Schema(description = "故障设备数")
    private Integer fault;

    @Schema(description = "离线设备数")
    private Integer offline;
}
