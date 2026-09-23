package com.enjoyiot.module.eiot.service.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket 告警推送消息 DTO
 * <p>
 * 用于通过 WebSocket 向前端实时推送告警信息，字段结构与前端
 * {@code AlarmItem} 接口对齐，确保前后端字段名一致。
 * <p>
 * 推送方式：{@link AlarmRecordService} → WebSocketSenderApi.sendObject()
 * → 消息类型 "alarm-push" → 前端 useAlarmPopup.composable 接收
 *
 * @author EnjoyIot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmWebSocketMessage {

    /** 告警记录ID */
    private Long id;

    /** 设备ID */
    private Long deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 告警类型名称（中文：火警/故障/离线等） */
    private String alarmTypeName;

    /** 告警级别（critical/major/warning/info） */
    private String alarmLevel;

    /** 告警描述 */
    private String alarmDescription;

    /** 告警位置 */
    private String alarmLocation;

    /** 告警时间（格式：yyyy-MM-dd HH:mm:ss） */
    private String alarmTime;

    /** 告警来源描述 */
    private String sourceDescription;

    /** 产品Key */
    private String productKey;

    /**
     * 报警类型：0=主机报警 1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警
     * 用于前端大屏按报警类型规则生成语音播报文本
     */
    private Integer alarmType;

    /** 单位名称（租户名称），用于语音播报前缀 */
    private String tenantName;

    /** 子设备名称（主机报警的部件描述，如存在），用于语音播报 */
    private String subDeviceName;

    // ==================== 工厂方法 ====================

    /**
     * 从 AlarmMessage + 入库后的 ID 构建 WebSocket 推送消息
     *
     * @param message  原始告警消息
     * @param recordId 入库后的 eiot_alert_record.id
     * @return WebSocket 推送消息
     */
    public static AlarmWebSocketMessage from(AlarmMessage message, Long recordId) {
        String timeStr = "";
        if (message.getAlarmTime() != null && message.getAlarmTime() > 0) {
            LocalDateTime dt = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(message.getAlarmTime()),
                    ZoneId.systemDefault());
            timeStr = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        AlarmWebSocketMessage wsMsg = AlarmWebSocketMessage.builder()
                .id(recordId)
                .deviceId(message.getDeviceId())
                .deviceName(message.getDeviceName())
                .alarmTypeName(AlarmMessage.getAlarmTypeLabel(message.getAlarmType()))
                .alarmLevel(message.getAlarmLevel())
                .alarmDescription(message.getAlarmDescription())
                .alarmLocation(message.getAlarmLocation())
                .alarmTime(timeStr)
                .sourceDescription(message.getSourceType() != null
                        ? message.getSourceType().getDescription() : "")
                .productKey(message.getProductKey())
                // 规则引擎产生的告警均为主机报警（alarmType=0）
                .alarmType(0)
                // 子设备名称：取部件描述（GB/T 26875 协议特有）
                .subDeviceName(message.getComponentInfo() != null
                        ? message.getComponentInfo().getComponentDesc() : "")
                .build();
        return wsMsg;
    }
}
