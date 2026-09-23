package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 值班记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class FireDutyRecordRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "关联排班ID")
    private Long scheduleId;

    @Schema(description = "所属物业ID")
    private Long propertyId;

    @Schema(description = "所属物业名称")
    @ExcelProperty("所属物业")
    private String propertyName;

    @Schema(description = "记录时间")
    @ExcelProperty("记录时间")
    private LocalDateTime recordTime;

    @Schema(description = "记录类型")
    @ExcelProperty("记录类型")
    private String recordType;

    @Schema(description = "记录类型名称")
    private String recordTypeName;

    @Schema(description = "记录标题")
    @ExcelProperty("标题")
    private String title;

    @Schema(description = "记录内容")
    @ExcelProperty("内容")
    private String content;

    @Schema(description = "关联设备ID")
    private Long deviceId;

    @Schema(description = "关联设备名称")
    @ExcelProperty("关联设备")
    private String deviceName;

    @Schema(description = "关联工单ID")
    private Long orderId;

    @Schema(description = "记录人ID")
    private Long recorderId;

    @Schema(description = "记录人姓名")
    @ExcelProperty("记录人")
    private String recorderName;

    @Schema(description = "附件(JSON数组)")
    private String attachments;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
