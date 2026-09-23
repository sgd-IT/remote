package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维修工单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class RepairOrderRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "工单编号")
    @ExcelProperty("工单编号")
    private String orderNo;

    @Schema(description = "关联告警记录ID")
    private Long alertRecordId;

    @Schema(description = "来源")
    @ExcelProperty("来源")
    private String source;

    @Schema(description = "来源名称")
    private String sourceName;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备编码")
    @ExcelProperty("设备编码")
    private String deviceCode;

    @Schema(description = "设备名称")
    @ExcelProperty("设备名称")
    private String deviceName;

    @Schema(description = "物业ID")
    private Long propertyId;

    @Schema(description = "物业名称")
    @ExcelProperty("所属企业")
    private String propertyName;

    @Schema(description = "故障类型")
    @ExcelProperty("故障类型")
    private String faultType;

    @Schema(description = "故障描述")
    @ExcelProperty("故障描述")
    private String faultDescription;

    @Schema(description = "故障图片(JSON数组)")
    private String faultImages;

    @Schema(description = "紧急程度")
    @ExcelProperty("紧急程度")
    private String urgencyLevel;

    @Schema(description = "紧急程度名称")
    private String urgencyLevelName;

    @Schema(description = "工单状态")
    @ExcelProperty("工单状态")
    private String orderStatus;

    @Schema(description = "状态名称")
    private String orderStatusName;

    @Schema(description = "指派人")
    @ExcelProperty("指派人")
    private String assignee;

    @Schema(description = "指派人电话")
    @ExcelProperty("指派人电话")
    private String assigneePhone;

    @Schema(description = "派单时间")
    @ExcelProperty("派单时间")
    private LocalDateTime assignTime;

    @Schema(description = "排查截止时间")
    @ExcelProperty("排查截止时间")
    private LocalDateTime deadline;

    @Schema(description = "签单时间")
    @ExcelProperty("签单时间")
    private LocalDateTime signTime;

    @Schema(description = "维保人")
    @ExcelProperty("维保人")
    private String repairPerson;

    @Schema(description = "维保人电话")
    @ExcelProperty("维保人电话")
    private String repairPersonPhone;

    @Schema(description = "维保单位")
    @ExcelProperty("维保单位")
    private String repairCompany;

    @Schema(description = "报修来源: platform-平台推送, phone-电话通知, manual-手动登记")
    @ExcelProperty("报修来源")
    private String reportSource;

    @Schema(description = "报修来源名称")
    private String reportSourceName;

    @Schema(description = "完成时间")
    @ExcelProperty("完成时间")
    private LocalDateTime completeTime;

    @Schema(description = "完成备注")
    @ExcelProperty("完成备注")
    private String completeRemark;

    @Schema(description = "完成图片(JSON数组)")
    private String completeImages;

    @Schema(description = "满意度评分(1-5)")
    @ExcelProperty("满意度评分")
    private Integer satisfaction;

    @Schema(description = "评价建议")
    @ExcelProperty("评价建议")
    private String suggestion;

    @Schema(description = "报修时间")
    @ExcelProperty("报修时间")
    private LocalDateTime reportTime;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "故障单位")
    @ExcelProperty("故障单位")
    private String faultUnit;

}
