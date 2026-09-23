package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维保合同更新 Request VO")
@Data
public class ContractUpdateReqVO {

    @Schema(description = "合同ID", required = true)
    @NotNull(message = "合同ID不能为空")
    private Long id;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "合同名称")
    private String contractName;

    @Schema(description = "物业ID")
    private Long propertyId;

    @Schema(description = "物业名称")
    private String propertyName;

    @Schema(description = "维保单位")
    private String maintenanceUnit;

    @Schema(description = "维保联系人")
    private String maintenanceContact;

    @Schema(description = "维保联系电话")
    private String maintenancePhone;

    @Schema(description = "合同开始日期")
    private LocalDateTime startDate;

    @Schema(description = "合同结束日期")
    private LocalDateTime endDate;

    @Schema(description = "合同金额")
    private BigDecimal contractAmount;

    @Schema(description = "合同状态: valid-有效, expired-已到期, renewing-续签中")
    private String contractStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "合同文件URL")
    private String contractFile;
}
