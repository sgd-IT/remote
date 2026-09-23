package com.enjoyiot.module.eiot.service.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一告警消息模型
 * <p>
 * 作为所有 IoT 对接方式（TCP GBT26875、AEP HTTP 推送等）产生的告警数据的统一中间格式。
 * 每种对接方式的 {@link AlarmProcessingStrategy} 实现负责将原始数据解析为本对象，
 * 再由 {@link AlarmRecordService} 统一落库和推送。
 *
 * @author EnjoyIot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmMessage {

    // ═══════════════════════════════════════════════════════════════
    // 基础标识字段
    // ═══════════════════════════════════════════════════════════════

    /**
     * 告警来源类型（TCP_GBT26875 / AEP_HTTP_PUSH）
     */
    private AlarmSourceType sourceType;

    /**
     * 告警类型编码（fire / fault / power_fault / offline / shield / supervise）
     */
    private String alarmType;

    /**
     * 告警等级（critical / major / warning / info）
     */
    private String alarmLevel;

    // ═══════════════════════════════════════════════════════════════
    // 关联设备字段
    // ═══════════════════════════════════════════════════════════════

    /**
     * 设备ID（eiot_device_info.id）
     */
    private Long deviceId;

    /**
     * 设备名称（eiot_device_info.name 或设备编号）
     */
    private String deviceName;

    /**
     * 产品Key（eiot_product.product_key）
     */
    private String productKey;

    // ═══════════════════════════════════════════════════════════════
    // 告警内容字段
    // ═══════════════════════════════════════════════════════════════

    /**
     * 告警发生时间戳（毫秒）
     */
    private Long alarmTime;

    /**
     * 告警位置描述（楼层、房间、点位等）
     */
    private String alarmLocation;

    /**
     * 告警描述（人类可读的告警摘要）
     */
    private String alarmDescription;

    // ═══════════════════════════════════════════════════════════════
    // 扩展字段
    // ═══════════════════════════════════════════════════════════════

    /**
     * 原始告警数据（JSON 格式，用于追溯和详情展示）
     */
    private String rawData;

    /**
     * 部件信息（仅 TCP GBT26875 场景使用，AEP 场景为 null）
     */
    private ComponentInfo componentInfo;

    /**
     * 扩展属性（供后续扩展使用，如视频设备ID、关联物业ID等）
     */
    @Builder.Default
    private Map<String, Object> extra = new LinkedHashMap<>();


    /**
     * 部件信息（GB/T 26875 协议特有）
     * <p>
     * 描述消防系统中引发告警的具体部件（如烟感、温感、手报等）。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentInfo {
        /** 系统类型编码（如 1=火灾报警系统） */
        private Integer systemType;
        /** 系统地址 */
        private Integer systemAddr;
        /** 部件类型编码（如 40=感烟探测器） */
        private Integer componentType;
        /** 部件地址 */
        private Integer componentAddr;
        /** 部件类型名称 */
        private String componentTypeName;
        /** 部件描述 */
        private String componentDesc;
    }

    /**
     * 生成告警名称（用于存入 eiot_alert_record.name）
     */
    public String buildAlarmName() {
        StringBuilder sb = new StringBuilder();
        sb.append(getAlarmTypeLabel(alarmType));
        if (deviceName != null) {
            sb.append(" - ").append(deviceName);
        }
        return sb.toString();
    }

    /**
     * 生成告警详情（用于存入 eiot_alert_record.details）
     */
    public String buildDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append(alarmDescription != null ? alarmDescription : buildAlarmName());
        if (alarmLocation != null) {
            sb.append(" | 位置: ").append(alarmLocation);
        }
        if (componentInfo != null && componentInfo.componentDesc != null) {
            sb.append(" | 部件: ").append(componentInfo.componentDesc);
        }
        sb.append(" | 来源: ").append(sourceType.getDescription());
        return sb.toString();
    }

    /**
     * 告警类型编码 → 中文标签映射
     */
    public static String getAlarmTypeLabel(String alarmType) {
        if (alarmType == null) return "未知告警";
        switch (alarmType) {
            case "fire":          return "火警";
            case "fault":         return "故障";
            case "power_fault":   return "电源故障";
            case "offline":       return "设备离线";
            case "shield":        return "屏蔽";
            case "supervise":     return "监管";
            case "info":          return "信息";
            case "risk":          return "隐患";
            case "smoke":         return "烟感报警";
            case "temperature":   return "温感报警";
            case "manual":        return "手动报警";
            case "composite":     return "复合报警";
            default:              return alarmType;
        }
    }
}
