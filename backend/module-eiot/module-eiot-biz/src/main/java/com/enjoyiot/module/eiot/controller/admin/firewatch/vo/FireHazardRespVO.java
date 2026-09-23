package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 隐患 Response VO")
@Data
@ExcelIgnoreUnannotated
public class FireHazardRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "隐患编号")
    @ExcelProperty("隐患编号")
    private String hazardNo;

    @Schema(description = "所属物业ID")
    private Long propertyId;

    @Schema(description = "所属物业名称")
    @ExcelProperty("所属单位")
    private String propertyName;

    @Schema(description = "关联设备ID")
    private Long deviceId;

    @Schema(description = "关联设备名称")
    @ExcelProperty("关联设备")
    private String deviceName;

    @Schema(description = "隐患等级")
    @ExcelProperty("隐患等级")
    private String hazardLevel;

    @Schema(description = "隐患等级名称")
    private String hazardLevelName;

    @Schema(description = "隐患描述")
    @ExcelProperty("隐患描述")
    private String hazardDesc;

    @Schema(description = "隐患位置")
    @ExcelProperty("隐患位置")
    private String location;

    @Schema(description = "发现人")
    @ExcelProperty("发现人")
    private String foundUserName;

    @Schema(description = "发现日期")
    @ExcelProperty("发现日期")
    private LocalDate foundDate;

    @Schema(description = "整改截止日期")
    @ExcelProperty("整改期限")
    private LocalDate deadline;

    @Schema(description = "整改建议")
    @ExcelProperty("整改建议")
    private String suggestion;

    @Schema(description = "现场照片(JSON数组)")
    private String photos;

    @Schema(description = "隐患状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "整改人ID")
    private Long rectificationUserId;

    @Schema(description = "整改人姓名")
    @ExcelProperty("整改人")
    private String rectificationUserName;

    @Schema(description = "整改措施")
    @ExcelProperty("整改措施")
    private String measure;

    @Schema(description = "整改进度(%)")
    @ExcelProperty("整改进度(%)")
    private Integer rectificationProgress;

    @Schema(description = "整改记录(JSON数组)")
    private String rectificationRecords;

    @Schema(description = "验收结果")
    private String reviewResult;

    @Schema(description = "验收意见")
    @ExcelProperty("验收意见")
    private String reviewRemark;

    @Schema(description = "验收时间")
    @ExcelProperty("验收时间")
    private LocalDateTime reviewTime;

    @Schema(description = "关闭原因")
    private String closeRemark;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
