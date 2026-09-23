package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 子设备统计响应 VO（设备控制室专用）
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 子设备统计 Response VO")
@Data
public class SubDeviceStatsRespVO {

    @Schema(description = "子设备总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long total;

    @Schema(description = "在线子设备数", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    private Long online;

}
