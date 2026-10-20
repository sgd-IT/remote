package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 月度报告数据 VO（聚合结果，供 PDF 渲染使用）
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 月度报告数据 VO")
@Data
public class MonthlyReportDataVO {

    @Schema(description = "报告月份（yyyy-MM）")
    private String month;

    @Schema(description = "统计周期文本（如 9月1日—9月30日）")
    private String periodText;

    @Schema(description = "生成时间文本")
    private String generatedAt;

    @Schema(description = "联网单位数")
    private Integer tenantCount;

    @Schema(description = "物业项目数")
    private Integer propertyCount;

    // ==================== 一、本月概览 ====================

    @Schema(description = "告警总数")
    private Integer totalAlarms;

    @Schema(description = "环比变化文本（如 上升 12.5% / 下降 3.2% / —）")
    private String momText;

    @Schema(description = "火警数")
    private Integer fireAlarms;

    @Schema(description = "故障数")
    private Integer faultAlarms;

    @Schema(description = "隐患数")
    private Integer riskAlarms;

    @Schema(description = "离线数")
    private Integer offlineAlarms;

    @Schema(description = "误报数")
    private Integer falseAlarmAlarms;

    @Schema(description = "已处置数")
    private Integer handledAlarms;

    @Schema(description = "处置率(%)")
    private Double handleRate;

    @Schema(description = "平均响应时间（秒），无数据为 null")
    private Integer avgResponseTime;

    // ==================== 设备快照 ====================

    @Schema(description = "设备总数")
    private Integer deviceTotal;

    @Schema(description = "在线设备数")
    private Integer deviceOnline;

    @Schema(description = "离线设备数")
    private Integer deviceOffline;

    @Schema(description = "故障设备数")
    private Integer deviceFault;

    @Schema(description = "未激活设备数")
    private Integer deviceInactive;

    @Schema(description = "在线率(%)")
    private Double deviceOnlineRate;

    // ==================== 二、告警事件分析 ====================

    @Schema(description = "按类型统计（数量/占比/已处置/未处置）")
    private List<TypeStatItem> typeStats;

    @Schema(description = "每日趋势")
    private List<TrendItem> dailyTrend;

    @Schema(description = "高峰时段 TOP5")
    private List<HourStatItem> hourStats;

    @Schema(description = "高发单位 TOP5")
    private List<TenantStatItem> tenantStats;

    // ==================== 三、设备运行状态 ====================

    @Schema(description = "离线设备清单（最多 30 条）")
    private List<OfflineDeviceItem> offlineDevices;

    @Schema(description = "重复告警设备 TOP5")
    private List<DeviceAlertStatItem> repeatAlarmDevices;

    @Schema(description = "误报高发设备 TOP5")
    private List<DeviceAlertStatItem> falseAlarmDevices;

    // ==================== 四、重点告警明细 ====================

    @Schema(description = "重点告警明细（火警或未处置，最多 50 条）")
    private List<KeyAlarmItem> keyAlarms;

    // ==================== 五、隐患与整改 ====================

    @Schema(description = "本月发现隐患数")
    private Integer hazardFound;

    @Schema(description = "已闭环数")
    private Integer hazardClosed;

    @Schema(description = "未闭环数")
    private Integer hazardOpen;

    @Schema(description = "超期未整改数")
    private Integer hazardOverdue;

    @Schema(description = "隐患明细（最多 20 条）")
    private List<HazardItem> hazardItems;

    // ==================== AI 分析（结论 / 问题与建议 / 下月工作计划） ====================

    @Schema(description = "AI 生成的一句话结论（为空时概览结论行显示 —）")
    private String aiConclusionText;

    @Schema(description = "AI 生成的问题与建议文本（为空时渲染留白区）")
    private String aiSuggestionText;

    @Schema(description = "AI 生成的下月工作计划文本（为空时渲染留白区）")
    private String aiPlanText;

    // ==================== 内部 VO ====================

    @Schema(description = "告警类型统计项")
    @Data
    public static class TypeStatItem {
        @Schema(description = "类型名称")
        private String typeName;
        @Schema(description = "数量")
        private Integer count;
        @Schema(description = "占比文本（如 32.5%）")
        private String percent;
        @Schema(description = "已处置数")
        private Integer handledCount;
        @Schema(description = "未处置数")
        private Integer pendingCount;
    }

    @Schema(description = "每日趋势项")
    @Data
    public static class TrendItem {
        @Schema(description = "日期标签（如 01）")
        private String label;
        @Schema(description = "告警数")
        private Integer alarmCount;
        @Schema(description = "已处置数")
        private Integer handleCount;
    }

    @Schema(description = "时段统计项")
    @Data
    public static class HourStatItem {
        @Schema(description = "时段标签（如 08:00-09:00）")
        private String hourLabel;
        @Schema(description = "数量")
        private Integer count;
    }

    @Schema(description = "单位统计项")
    @Data
    public static class TenantStatItem {
        @Schema(description = "单位名称")
        private String tenantName;
        @Schema(description = "数量")
        private Integer count;
    }

    @Schema(description = "离线设备项")
    @Data
    public static class OfflineDeviceItem {
        @Schema(description = "设备名称")
        private String deviceName;
        @Schema(description = "设备编号")
        private String deviceNo;
        @Schema(description = "所属单位")
        private String propertyName;
        @Schema(description = "安装位置")
        private String location;
    }

    @Schema(description = "设备告警统计项")
    @Data
    public static class DeviceAlertStatItem {
        @Schema(description = "设备名称")
        private String deviceName;
        @Schema(description = "数量")
        private Integer count;
    }

    @Schema(description = "重点告警明细项")
    @Data
    public static class KeyAlarmItem {
        @Schema(description = "告警时间")
        private String alarmTime;
        @Schema(description = "设备名称")
        private String deviceName;
        @Schema(description = "告警类型")
        private String alarmTypeName;
        @Schema(description = "安装位置")
        private String location;
        @Schema(description = "处置状态")
        private String statusName;
        @Schema(description = "处置人")
        private String handler;
        @Schema(description = "处置结果（动作 + 备注）")
        private String result;
    }

    @Schema(description = "隐患明细项")
    @Data
    public static class HazardItem {
        @Schema(description = "隐患编号")
        private String hazardNo;
        @Schema(description = "隐患等级")
        private String levelName;
        @Schema(description = "隐患描述")
        private String desc;
        @Schema(description = "位置")
        private String location;
        @Schema(description = "发现日期")
        private String foundDate;
        @Schema(description = "整改期限")
        private String deadline;
        @Schema(description = "状态")
        private String statusName;
    }
}
