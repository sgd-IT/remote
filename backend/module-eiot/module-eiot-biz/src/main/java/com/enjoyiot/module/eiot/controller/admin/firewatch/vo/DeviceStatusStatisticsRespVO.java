package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设备状态统计 Response VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 设备状态统计 Response VO")
@Data
public class DeviceStatusStatisticsRespVO {

    @Schema(description = "设备总数")
    private Integer total;

    @Schema(description = "在线设备数")
    private Integer online;

    @Schema(description = "正常设备数")
    private Integer normal;

    @Schema(description = "离线设备数")
    private Integer offline;

    @Schema(description = "告警设备数")
    private Integer alarm;

    @Schema(description = "故障设备数")
    private Integer fault;

    @Schema(description = "未激活设备数")
    private Integer inactive;

    @Schema(description = "在线率(%)")
    private Double onlineRate;

    @Schema(description = "告警率(%)")
    private Double alarmRate;

    @Schema(description = "故障率(%)")
    private Double faultRate;

}
