package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 值班管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class FireDutyScheduleRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "所属物业ID")
    private Long propertyId;

    @Schema(description = "所属物业名称")
    @ExcelProperty("所属物业")
    private String propertyName;

    @Schema(description = "排班日期")
    @ExcelProperty("排班日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    @Schema(description = "班次类型")
    @ExcelProperty("班次类型")
    private String shiftType;

    @Schema(description = "班次类型名称")
    private String shiftTypeName;

    @Schema(description = "班次名称")
    @ExcelProperty("班次名称")
    private String shiftName;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    @ExcelProperty("结束时间")
    private String endTime;

    @Schema(description = "值班人ID")
    private Long dutyUserId;

    @Schema(description = "值班人姓名")
    @ExcelProperty("值班人")
    private String dutyUserName;

    @Schema(description = "值班人电话")
    @ExcelProperty("联系电话")
    private String dutyUserPhone;

    @Schema(description = "值班人岗位")
    @ExcelProperty("岗位")
    private String dutyUserPosition;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "签到时间")
    @ExcelProperty("签到时间")
    private LocalDateTime checkInTime;

    @Schema(description = "签退时间")
    @ExcelProperty("签退时间")
    private LocalDateTime checkOutTime;

    @Schema(description = "签到方式")
    private String checkInType;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
