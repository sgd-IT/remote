package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.enjoyiot.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 火警处置工单分页 Request VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 火警处置工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AlarmOrderPageReqVO extends PageParam {

    @Schema(description = "工单编号")
    private String orderNo;

    @Schema(description = "物业ID")
    private Long propertyId;

    @Schema(description = "物业名称")
    private String propertyName;

    @Schema(description = "告警类型")
    private String alarmType;

    @Schema(description = "告警级别")
    private String alarmLevel;

    @Schema(description = "工单状态")
    private String orderStatus;

    @Schema(description = "设备类型(即category_id)")
    private String deviceType;

    @Schema(description = "告警开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime alarmTimeBegin;

    @Schema(description = "告警结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime alarmTimeEnd;

}
