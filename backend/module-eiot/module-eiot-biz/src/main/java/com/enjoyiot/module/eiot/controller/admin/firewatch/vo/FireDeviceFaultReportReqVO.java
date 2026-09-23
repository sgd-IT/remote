package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 消防设备故障报修 Request VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 消防设备故障报修 Request VO")
@Data
public class FireDeviceFaultReportReqVO {

    @Schema(description = "设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    @Schema(description = "故障类型")
    private String faultType;

    @Schema(description = "故障描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "故障描述不能为空")
    private String faultDescription;

    @Schema(description = "故障图片(JSON数组)")
    private String faultImages;

    @Schema(description = "报修人")
    private String reporter;

    @Schema(description = "报修人电话")
    private String reporterPhone;

    @Schema(description = "紧急程度: urgent-紧急, major-较大, normal-一般")
    private String urgencyLevel;

}
