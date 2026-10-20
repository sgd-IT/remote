package com.enjoyiot.module.eiot.service.alarm;

import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
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

    /** 告警级别（critical/major/warning/info）——由定级层产出的等级（1/2/3）投影 */
    private String alarmLevel;

    /** 报警类型（新语义）：1=火警 2=故障 3=隐患 4=离线 5=误报 6=信息 */
    private Integer alarmKind;

    /** 确认档位（仅一级）：1=Ⅰ档 2=Ⅱ档 3=Ⅲ档 4=Ⅳ档 */
    private Integer tier;

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
     * 从 AlarmMessage + 入库后的告警记录构建 WebSocket 推送消息
     * <p>
     * 写侧三写（定稿 4.2 R8）：级别出口对齐四维——alarmLevel 由记录的新语义等级（1/2/3）投影，
     * 取代旧的"由输入级别字符串直接透传"；同时附带 alarmKind/tier 供前端使用
     *
     * @param message 原始告警消息
     * @param record  入库后的 eiot_alert_record 记录（含生成 ID）
     * @return WebSocket 推送消息
     */
    public static AlarmWebSocketMessage from(AlarmMessage message, AlertRecordDO record) {
        String timeStr = "";
        if (message.getAlarmTime() != null && message.getAlarmTime() > 0) {
            LocalDateTime dt = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(message.getAlarmTime()),
                    ZoneId.systemDefault());
            timeStr = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        AlarmWebSocketMessage wsMsg = AlarmWebSocketMessage.builder()
                .id(record.getId())
                .deviceId(message.getDeviceId())
                .deviceName(message.getDeviceName())
                .alarmTypeName(AlarmMessage.getAlarmTypeLabel(message.getAlarmType()))
                .alarmLevel(AlarmSemantics.toAlarmLevelText(record.getLevel()))
                .alarmKind(record.getAlarmKind())
                .tier(record.getTier())
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
