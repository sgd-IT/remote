package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Schema(description = "管理后台 - 值班管理分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FireDutySchedulePageReqVO extends PageParam {

    @Schema(description = "所属物业ID")
    private Long propertyId;

    @Schema(description = "排班日期-开始")
    private LocalDate scheduleDateStart;

    @Schema(description = "排班日期-结束")
    private LocalDate scheduleDateEnd;

    @Schema(description = "班次类型")
    private String shiftType;

    @Schema(description = "值班人姓名")
    private String dutyUserName;

    @Schema(description = "值班人电话")
    private String dutyUserPhone;

    @Schema(description = "状态")
    private String status;

}
