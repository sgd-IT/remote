package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 今日警情统计 Response VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 今日警情统计 Response VO")
@Data
public class TodayAlarmStatisticsRespVO {

    @Schema(description = "今日告警总数")
    private Integer totalAlarms;

    @Schema(description = "已处置数量")
    private Integer handledCount;

    @Schema(description = "待处理数量")
    private Integer pendingCount;

    @Schema(description = "误报数量")
    private Integer falseAlarmCount;

    @Schema(description = "处置率(%)")
    private Double handleRate;

    @Schema(description = "平均响应时间(秒)")
    private Integer avgResponseTime;

    @Schema(description = "火警告警数")
    private Integer fireAlarmCount;

    @Schema(description = "故障告警数")
    private Integer faultAlarmCount;

}
