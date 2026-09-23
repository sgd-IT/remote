package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 报警记录页统计 Response VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 报警记录统计 Response VO")
@Data
public class AlarmRecordStatisticsRespVO {

    @Schema(description = "报警总数")
    private Integer totalAlarms;

    @Schema(description = "火警数量")
    private Integer fireAlarms;

    @Schema(description = "故障数量")
    private Integer faultAlarms;

    @Schema(description = "隐患数量")
    private Integer riskAlarms;

    @Schema(description = "离线数量")
    private Integer offlineAlarms;

    @Schema(description = "误报数量")
    private Integer falseAlarmAlarms;

    @Schema(description = "已处置数量")
    private Integer handledAlarms;

    @Schema(description = "处置率(%)")
    private Double handleRate;

    @Schema(description = "平均响应时间(秒)")
    private Integer avgResponseTime;

    @Schema(description = "报警类型分布")
    private List<AlarmTypeCountVO> alarmTypeDistribution;

    @Schema(description = "报警类型数量 VO")
    @Data
    public static class AlarmTypeCountVO {
        @Schema(description = "告警类型编码")
        private String alarmType;
        @Schema(description = "告警类型名称")
        private String typeName;
        @Schema(description = "数量")
        private Integer count;
        @Schema(description = "已处置数量")
        private Integer handledCount;
    }
}
