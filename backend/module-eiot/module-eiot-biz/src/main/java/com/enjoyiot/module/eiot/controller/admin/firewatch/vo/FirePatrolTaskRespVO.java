package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 巡检任务 Response VO")
@Data
@ExcelIgnoreUnannotated
public class FirePatrolTaskRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "任务编号")
    @ExcelProperty("任务编号")
    private String taskNo;

    @Schema(description = "所属物业ID")
    private Long propertyId;

    @Schema(description = "所属物业名称")
    @ExcelProperty("所属单位")
    private String propertyName;

    @Schema(description = "任务名称")
    @ExcelProperty("任务名称")
    private String taskName;

    @Schema(description = "任务类型")
    @ExcelProperty("任务类型")
    private String taskType;

    @Schema(description = "任务类型名称")
    private String taskTypeName;

    @Schema(description = "计划日期")
    @ExcelProperty("计划日期")
    private LocalDate planDate;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间")
    @ExcelProperty("实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    @ExcelProperty("实际结束时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "任务状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "巡检人ID")
    private Long patrolUserId;

    @Schema(description = "巡检人姓名")
    @ExcelProperty("巡检人")
    private String patrolUserName;

    @Schema(description = "巡检人电话")
    @ExcelProperty("联系电话")
    private String patrolUserPhone;

    @Schema(description = "巡检点位总数")
    @ExcelProperty("点位总数")
    private Integer totalPoints;

    @Schema(description = "已完成点位数")
    @ExcelProperty("已完成")
    private Integer completedPoints;

    @Schema(description = "正常点位数")
    @ExcelProperty("正常")
    private Integer normalPoints;

    @Schema(description = "异常点位数")
    @ExcelProperty("异常")
    private Integer abnormalPoints;

    @Schema(description = "完成进度")
    @ExcelProperty("进度(%)")
    private Integer progress;

    @Schema(description = "巡检结果")
    @ExcelProperty("巡检结果")
    private String result;

    @Schema(description = "结果名称")
    private String resultName;

    @Schema(description = "巡检总结")
    @ExcelProperty("巡检总结")
    private String summary;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
