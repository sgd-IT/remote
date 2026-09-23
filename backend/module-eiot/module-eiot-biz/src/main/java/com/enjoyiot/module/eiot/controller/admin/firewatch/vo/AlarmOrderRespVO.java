package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 火警处置工单 Response VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 火警处置工单 Response VO")
@Data
public class AlarmOrderRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联告警记录ID")
    private Long alertRecordId;

    @Schema(description = "工单编号")
    private String orderNo;

    @Schema(description = "物业ID")
    private Long propertyId;

    @Schema(description = "物业名称")
    private String propertyName;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备类型编码")
    private String deviceType;

    @Schema(description = "设备类型名称")
    private String deviceTypeName;

    @Schema(description = "告警类型")
    private String alarmType;

    @Schema(description = "告警类型名称")
    private String alarmTypeName;

    @Schema(description = "告警级别")
    private String alarmLevel;

    @Schema(description = "告警级别名称")
    private String alarmLevelName;

    @Schema(description = "告警时间")
    private LocalDateTime alarmTime;

    @Schema(description = "告警位置")
    private String alarmLocation;

    @Schema(description = "告警描述")
    private String alarmDescription;

    @Schema(description = "工单状态")
    private String orderStatus;

    @Schema(description = "工单状态名称")
    private String orderStatusName;

    @Schema(description = "处理人")
    private String handler;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "处理类型")
    private String handleType;

    @Schema(description = "处理备注")
    private String handleRemark;

    @Schema(description = "响应时长(秒)")
    private Integer responseDuration;

    @Schema(description = "视频URL")
    private String videoUrl;

    @Schema(description = "AI建议")
    private String aiSuggestion;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "报警类型分类：0=主机报警 1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警")
    private Integer alarmCategory;

    /**
     * 获取告警类型名称
     */
    public String getAlarmTypeName() {
        if (alarmType == null) return null;
        switch (alarmType) {
            case "fire": return "火警";
            case "fault": return "故障";
            case "risk": return "隐患";
            case "offline": return "离线";
            case "false_alarm": return "误报";
            case "smoke": return "烟感报警";
            case "temperature": return "温感报警";
            case "manual": return "手动报警";
            case "composite": return "复合报警";
            default: return alarmType;
        }
    }

    /**
     * 获取告警级别名称
     */
    public String getAlarmLevelName() {
        if (alarmLevel == null) return null;
        switch (alarmLevel) {
            case "critical": return "严重";
            case "major": return "较大";
            case "warning": return "一般";
            case "info": return "提示";
            default: return alarmLevel;
        }
    }

    /**
     * 获取工单状态名称
     */
    public String getOrderStatusName() {
        if (orderStatus == null) return null;
        switch (orderStatus) {
            case "pending": return "待处理";
            case "handling": return "处理中";
            case "handled": return "已处置";
            case "closed": return "已关闭";
            default: return orderStatus;
        }
    }

}
