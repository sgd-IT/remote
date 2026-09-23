package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维保合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractPageReqVO extends PageParam {

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "合同名称")
    private String contractName;

    @Schema(description = "维保单位")
    private String maintenanceUnit;

    @Schema(description = "合同状态: valid-有效, expired-已到期, renewing-续签中")
    private String contractStatus;

    @Schema(description = "物业名称")
    private String propertyName;

    @Schema(description = "创建时间-开始")
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间-结束")
    private LocalDateTime createTimeEnd;
}
