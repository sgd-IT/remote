package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 值班记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FireDutyRecordPageReqVO extends PageParam {

    @Schema(description = "关联排班ID")
    private Long scheduleId;

    @Schema(description = "所属物业ID")
    private Long propertyId;

    @Schema(description = "记录时间-开始")
    private LocalDateTime recordTimeStart;

    @Schema(description = "记录时间-结束")
    private LocalDateTime recordTimeEnd;

    @Schema(description = "记录类型")
    private String recordType;

    @Schema(description = "记录人姓名")
    private String recorderName;

    @Schema(description = "状态")
    private String status;

}
