package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维修工单创建 Request VO")
@Data
public class RepairOrderCreateReqVO {

    @Schema(description = "所属租户ID", required = true)
    @NotNull(message = "所属租户不能为空")
    private Long tenantId;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "故障类型")
    private String faultType;

    @Schema(description = "故障描述", required = true)
    @NotBlank(message = "故障描述不能为空")
    private String faultDescription;

    @Schema(description = "故障图片(JSON数组)")
    private String faultImages;

    @Schema(description = "紧急程度: urgent-紧急, major-较大, normal-一般")
    private String urgencyLevel;

    @Schema(description = "排查截止时间")
    private LocalDateTime deadline;

    @Schema(description = "报修来源: platform-平台推送, phone-电话通知, manual-手动登记")
    private String reportSource;

    @Schema(description = "维保人")
    private String repairPerson;

    @Schema(description = "维保单位")
    private String repairCompany;

    @Schema(description = "维保人电话")
    private String repairPersonPhone;

}
