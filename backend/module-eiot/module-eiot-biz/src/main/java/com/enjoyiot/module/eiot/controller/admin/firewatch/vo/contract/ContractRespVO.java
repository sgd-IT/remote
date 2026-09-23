package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维保合同 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ContractRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "合同编号")
    @ExcelProperty("合同编号")
    private String contractNo;

    @Schema(description = "合同名称")
    @ExcelProperty("合同名称")
    private String contractName;

    @Schema(description = "物业ID")
    private Long propertyId;

    @Schema(description = "物业名称")
    @ExcelProperty("所属企业")
    private String propertyName;

    @Schema(description = "维保单位")
    @ExcelProperty("维保单位")
    private String maintenanceUnit;

    @Schema(description = "维保联系人")
    @ExcelProperty("维保联系人")
    private String maintenanceContact;

    @Schema(description = "维保联系电话")
    @ExcelProperty("维保联系电话")
    private String maintenancePhone;

    @Schema(description = "合同开始日期")
    @ExcelProperty("合同开始日期")
    private LocalDateTime startDate;

    @Schema(description = "合同结束日期")
    @ExcelProperty("合同结束日期")
    private LocalDateTime endDate;

    @Schema(description = "合同金额")
    @ExcelProperty("合同金额")
    private BigDecimal contractAmount;

    @Schema(description = "合同状态: valid-有效, expired-已到期, renewing-续签中")
    @ExcelProperty("合同状态")
    private String contractStatus;

    @Schema(description = "合同状态名称")
    private String contractStatusName;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "合同文件URL")
    private String contractFile;
}
