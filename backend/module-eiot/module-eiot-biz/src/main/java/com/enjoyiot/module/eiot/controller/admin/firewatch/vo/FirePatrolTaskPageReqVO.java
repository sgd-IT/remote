package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Schema(description = "管理后台 - 巡检任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FirePatrolTaskPageReqVO extends PageParam {

    @Schema(description = "所属物业ID")
    private Long propertyId;

    @Schema(description = "计划日期-开始")
    private LocalDate planDateStart;

    @Schema(description = "计划日期-结束")
    private LocalDate planDateEnd;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "巡检人姓名")
    private String patrolUserName;

    @Schema(description = "巡检结果")
    private String result;

}
