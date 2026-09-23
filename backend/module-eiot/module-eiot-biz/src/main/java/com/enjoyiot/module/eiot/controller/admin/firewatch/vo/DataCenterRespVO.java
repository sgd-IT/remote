package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 数据中心统计数据 Response VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 数据中心统计数据 Response VO")
@Data
public class DataCenterRespVO {

    // ==================== 告警分析 ====================

    @Schema(description = "告警趋势")
    private List<AlarmTrendItem> alarmTrend;

    @Schema(description = "告警类型分布")
    private List<AlarmTypeItem> alarmTypeDistribution;

    @Schema(description = "各物业告警排名")
    private List<PropertyAlarmRankItem> propertyAlarmRank;

    @Schema(description = "24h告警时段热力数据")
    private List<HourlyHeatmapItem> hourlyHeatmap;

    @Schema(description = "告警等级分布")
    private List<AlarmLevelItem> alarmLevelDistribution;

    // ==================== 设备分析 ====================

    @Schema(description = "设备状态统计")
    private DeviceStatusItem deviceStatus;

    @Schema(description = "设备类型分布")
    private List<DeviceCategoryItem> deviceCategoryDistribution;

    @Schema(description = "各物业设备数量")
    private List<PropertyDeviceItem> propertyDeviceDistribution;

    // ==================== 值班统计 ====================

    @Schema(description = "值班统计摘要")
    private DutyStatsItem dutyStats;

    @Schema(description = "值班人员排班天数排名")
    private List<UserDutyRankItem> userDutyRank;

    // ==================== 巡检统计 ====================

    @Schema(description = "巡检统计摘要")
    private PatrolStatsItem patrolStats;

    @Schema(description = "巡检任务趋势")
    private List<PatrolTrendItem> patrolTrend;

    // ==================== 工单统计 ====================

    @Schema(description = "维修工单统计")
    private RepairStatsItem repairStats;

    @Schema(description = "隐患统计")
    private HazardStatsItem hazardStats;

    @Schema(description = "工单趋势（维修+隐患）")
    private List<WorkOrderTrendItem> workOrderTrend;

    // ==================== 内部 VO 类 ====================

    @Schema(description = "告警趋势项")
    @Data
    public static class AlarmTrendItem {
        @Schema(description = "日期 yyyy-MM-dd")
        private String date;
        @Schema(description = "告警数量")
        private Integer alarmCount;
        @Schema(description = "处置数量")
        private Integer handleCount;
    }

    @Schema(description = "告警类型项")
    @Data
    public static class AlarmTypeItem {
        @Schema(description = "告警类型名称")
        private String typeName;
        @Schema(description = "数量")
        private Integer count;
    }

    @Schema(description = "物业告警排名项")
    @Data
    public static class PropertyAlarmRankItem {
        @Schema(description = "物业名称")
        private String propertyName;
        @Schema(description = "告警数量")
        private Integer alarmCount;
    }

    @Schema(description = "24h热力项")
    @Data
    public static class HourlyHeatmapItem {
        @Schema(description = "小时 0-23")
        private Integer hour;
        @Schema(description = "告警数量")
        private Integer count;
    }

    @Schema(description = "告警等级项")
    @Data
    public static class AlarmLevelItem {
        @Schema(description = "等级编码")
        private String level;
        @Schema(description = "等级名称")
        private String levelName;
        @Schema(description = "数量")
        private Integer count;
    }

    @Schema(description = "设备状态项")
    @Data
    public static class DeviceStatusItem {
        @Schema(description = "总数")
        private Integer total;
        @Schema(description = "正常")
        private Integer normal;
        @Schema(description = "告警")
        private Integer alarm;
        @Schema(description = "故障")
        private Integer fault;
        @Schema(description = "离线")
        private Integer offline;
        @Schema(description = "未激活")
        private Integer inactive;
    }

    @Schema(description = "设备类型分布项")
    @Data
    public static class DeviceCategoryItem {
        @Schema(description = "分类名称")
        private String categoryName;
        @Schema(description = "数量")
        private Integer count;
    }

    @Schema(description = "物业设备数量项")
    @Data
    public static class PropertyDeviceItem {
        @Schema(description = "物业名称")
        private String propertyName;
        @Schema(description = "设备数量")
        private Integer deviceCount;
    }

    @Schema(description = "值班统计摘要项")
    @Data
    public static class DutyStatsItem {
        @Schema(description = "本月值班人次")
        private Integer monthDutyCount;
        @Schema(description = "签到率(%)")
        private Double checkInRate;
        @Schema(description = "准时率(%)")
        private Double onTimeRate;
        @Schema(description = "交接班记录数")
        private Integer handoverCount;
    }

    @Schema(description = "值班人员排名项")
    @Data
    public static class UserDutyRankItem {
        @Schema(description = "人员姓名")
        private String userName;
        @Schema(description = "排班天数")
        private Integer dutyDays;
    }

    @Schema(description = "巡检统计摘要项")
    @Data
    public static class PatrolStatsItem {
        @Schema(description = "总任务数")
        private Integer total;
        @Schema(description = "已完成数")
        private Integer completed;
        @Schema(description = "进行中数")
        private Integer inProgress;
        @Schema(description = "超期数")
        private Integer overdue;
        @Schema(description = "完成率(%)")
        private Double completeRate;
    }

    @Schema(description = "巡检趋势项")
    @Data
    public static class PatrolTrendItem {
        @Schema(description = "日期 yyyy-MM-dd")
        private String date;
        @Schema(description = "创建数量")
        private Integer createdCount;
        @Schema(description = "完成数量")
        private Integer completedCount;
    }

    @Schema(description = "维修工单统计项")
    @Data
    public static class RepairStatsItem {
        @Schema(description = "总数")
        private Integer total;
        @Schema(description = "待处理")
        private Integer pending;
        @Schema(description = "进行中")
        private Integer inProgress;
        @Schema(description = "已完成")
        private Integer completed;
        @Schema(description = "超期数")
        private Integer overdue;
        @Schema(description = "平均处置时长(小时)")
        private Double avgHandleHours;
    }

    @Schema(description = "隐患统计项")
    @Data
    public static class HazardStatsItem {
        @Schema(description = "总数")
        private Integer total;
        @Schema(description = "待整改")
        private Integer pending;
        @Schema(description = "已整改")
        private Integer rectified;
        @Schema(description = "已关闭")
        private Integer closed;
        @Schema(description = "整改率(%)")
        private Double rectificationRate;
    }

    @Schema(description = "工单趋势项")
    @Data
    public static class WorkOrderTrendItem {
        @Schema(description = "日期 yyyy-MM-dd")
        private String date;
        @Schema(description = "创建数量")
        private Integer createdCount;
        @Schema(description = "完成数量")
        private Integer completedCount;
    }
}
