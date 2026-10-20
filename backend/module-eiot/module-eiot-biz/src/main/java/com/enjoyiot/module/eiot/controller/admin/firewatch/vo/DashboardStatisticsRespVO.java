package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 监控大屏统计数据 Response VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 监控大屏统计数据 Response VO")
@Data
public class DashboardStatisticsRespVO {

    @Schema(description = "设备状态统计")
    private DeviceStatusStatisticsRespVO deviceStatus;

    @Schema(description = "今日警情统计")
    private TodayAlarmStatisticsRespVO todayAlarm;

    @Schema(description = "风险单位分布")
    private List<RiskDistributionVO> riskDistribution;

    @Schema(description = "实时告警列表")
    private List<AlarmOrderRespVO> realtimeAlarms;

    @Schema(description = "告警趋势")
    private List<AlarmTrendVO> alarmTrend;

    @Schema(description = "设备类型分布")
    private List<DeviceCategoryVO> deviceCategoryDistribution;

    @Schema(description = "告警类型分布")
    private List<AlarmTypeDistributionVO> alarmTypeDistribution;

    @Schema(description = "安全天数")
    private Integer safeDays;

    @Schema(description = "设备健康率(%)")
    private Double deviceHealthRate;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "隐患待整改数")
    private Integer pendingIssues;

    @Schema(description = "联网单位总数")
    private Integer propertyCount;

    // ==================== 大屏扩展数据（巡检/值班/物业） ====================

    @Schema(description = "巡检任务摘要")
    private PatrolSummaryVO patrolSummary;

    @Schema(description = "当前值班列表")
    private List<DutyScheduleItemVO> currentDutyList;

    @Schema(description = "物业列表（大屏地图/卡片用）")
    private List<PropertyItemVO> properties;

    @Schema(description = "风险单位分布 VO")
    @Data
    public static class RiskDistributionVO {
        @Schema(description = "风险等级")
        private String riskLevel;
        @Schema(description = "风险等级名称")
        private String riskLevelName;
        @Schema(description = "数量")
        private Integer count;
    }

    @Schema(description = "告警趋势 VO")
    @Data
    public static class AlarmTrendVO {
        @Schema(description = "日期")
        private String date;
        @Schema(description = "告警数量")
        private Integer alarmCount;
        @Schema(description = "处置数量")
        private Integer handleCount;
    }

    @Schema(description = "设备类型分布 VO")
    @Data
    public static class DeviceCategoryVO {
        @Schema(description = "设施分类编码")
        private String categoryCode;
        @Schema(description = "设施分类名称")
        private String categoryName;
        @Schema(description = "设备数量")
        private Integer count;
    }

    @Schema(description = "告警类型分布 VO")
    @Data
    public static class AlarmTypeDistributionVO {
        @Schema(description = "告警类型编码")
        private String alarmType;
        @Schema(description = "告警类型名称")
        private String typeName;
        @Schema(description = "数量")
        private Integer count;
    }

    // ==================== 巡检任务摘要 ====================

    @Schema(description = "巡检任务摘要 VO")
    @Data
    public static class PatrolSummaryVO {
        @Schema(description = "总任务数")
        private Integer total;
        @Schema(description = "已完成数")
        private Integer completed;
        @Schema(description = "进行中/待执行数")
        private Integer pending;
        @Schema(description = "巡检任务列表（最近几条）")
        private List<PatrolTaskItemVO> tasks;
    }

    @Schema(description = "巡检任务项 VO")
    @Data
    public static class PatrolTaskItemVO {
        @Schema(description = "任务ID")
        private Long id;
        @Schema(description = "任务名称")
        private String taskName;
        @Schema(description = "巡检人姓名")
        private String inspectorName;
        @Schema(description = "计划日期")
        private String planTime;
        @Schema(description = "状态")
        private String status;
    }

    // ==================== 值班管理项 ====================

    @Schema(description = "值班管理项 VO")
    @Data
    public static class DutyScheduleItemVO {
        @Schema(description = "排班ID")
        private Long id;
        @Schema(description = "值班人姓名")
        private String userName;
        @Schema(description = "班次名称")
        private String shiftName;
        @Schema(description = "开始时间")
        private String startTime;
        @Schema(description = "结束时间")
        private String endTime;
        @Schema(description = "状态")
        private String status;
    }

    // ==================== 物业摘要项 ====================

    @Schema(description = "物业摘要项 VO")
    @Data
    public static class PropertyItemVO {
        @Schema(description = "物业ID")
        private Long id;
        @Schema(description = "物业名称")
        private String name;
        @Schema(description = "区域")
        private String district;
        @Schema(description = "风险等级")
        private String riskLevel;
        @Schema(description = "设备数量")
        private Integer deviceCount;
        @Schema(description = "经度")
        private Double longitude;
        @Schema(description = "纬度")
        private Double latitude;
        @Schema(description = "地址")
        private String address;
        @Schema(description = "联系人")
        private String contactName;
        @Schema(description = "联系电话")
        private String contactPhone;
    }

}
