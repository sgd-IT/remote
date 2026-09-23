package com.enjoyiot.module.eiot.service.alarm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AEP 平台 HTTP 推送告警解析策略
 *
 * @author sgd
 */
@Slf4j
@Component
public class AepHttpPushAlarmStrategy implements AlarmProcessingStrategy {

    /** AEP eventType: 信息（不产生告警） */
    private static final int EVENT_TYPE_INFO = 1;
    /** AEP eventType: 警告 */
    private static final int EVENT_TYPE_WARNING = 2;
    /** AEP eventType: 故障 */
    private static final int EVENT_TYPE_FAULT = 3;

    /** 上下文中存放 AEP eventType 的 key */
    static final String CTX_KEY_EVENT_TYPE = "aepEventType";
    /** 上下文中存放 AEP messageType 的 key */
    static final String CTX_KEY_MESSAGE_TYPE = "aepMessageType";
    /** 上下文中存放 dataReport 属性快照的 key */
    static final String CTX_KEY_PAYLOAD = "aepPayload";
    /** AEP messageType: 事件上报 */
    static final String MSG_EVENT_REPORT = "eventReport";
    /** AEP messageType: 数据变化上报 */
    static final String MSG_DATA_REPORT = "dataReport";

    @Override
    public AlarmSourceType supportedSourceType() {
        return AlarmSourceType.AEP_HTTP_PUSH;
    }

    @Override
    public List<AlarmMessage> parse(AlarmParseContext context) {
        if (context == null || context.getEventParams() == null) {
            return Collections.emptyList();
        }

        Map<String, Object> params = context.getEventParams();
        String messageType = (String) params.get(CTX_KEY_MESSAGE_TYPE);
        Integer eventType = toInteger(params.get(CTX_KEY_EVENT_TYPE));

        log.debug("[AEP告警解析] messageType={}, eventType={}, deviceId={}",
                messageType, eventType, context.getDeviceId());

        // ── eventReport: 根据 eventType 生成告警 ──
        if (MSG_EVENT_REPORT.equals(messageType)) {
            return parseEventReport(context, eventType);
        }

        // ── dataReport: 按属性值判定告警 ──
        if (MSG_DATA_REPORT.equals(messageType)) {
            return parseDataReport(context);
        }

        // 其他类型（commandResponse）不在此策略中处理
        return Collections.emptyList();
    }

    // ==================== eventReport 解析 ====================

    /**
     * 解析 AEP eventReport 消息
     * <p>
     * eventType=1(信息) → 不告警，返回空列表
     * eventType=2(警告) → alarmType=risk, alarmLevel=warning
     * eventType=3(故障) → alarmType=fault, alarmLevel=major
     */
    private List<AlarmMessage> parseEventReport(AlarmParseContext context, Integer eventType) {
        if (eventType == null) {
            log.warn("[AEP告警解析] eventReport 缺少 eventType, deviceId={}", context.getDeviceId());
            return Collections.emptyList();
        }

        String alarmType;
        String alarmLevel;

        switch (eventType) {
            case EVENT_TYPE_INFO:
                // 信息类事件不生成告警
                log.debug("[AEP告警解析] eventReport eventType=1(信息)，不生成告警, deviceId={}",
                        context.getDeviceId());
                return Collections.emptyList();
            case EVENT_TYPE_WARNING:
                alarmType = "risk";
                alarmLevel = "warning";
                break;
            case EVENT_TYPE_FAULT:
                alarmType = "fault";
                alarmLevel = "major";
                break;
            default:
                log.warn("[AEP告警解析] eventReport 未知 eventType={}, deviceId={}",
                        eventType, context.getDeviceId());
                alarmType = "risk";
                alarmLevel = "warning";
        }

        AlarmMessage msg = buildAlarmMessage(context, alarmType, alarmLevel,
                getEventTypeLabel(eventType) + "事件");
        return Collections.singletonList(msg);
    }

    // ==================== dataReport 解析 ====================

    /**
     * 解析 AEP dataReport 属性快照（NB 烟感周期/变化上报）
     * <p>
     * 告警映射（仅报报警态，解除态不产生告警）：
     * smoke_state=0 → fire/critical；error=1 烟感故障 / =3 低压故障；
     * fault ∈ {1,3,4,5,6,7,9} → fault/major；tamper_alarm=1 → risk/warning；
     * humidity_state=1 → risk；temperature_state=1 → temperature；disable_status=1 → shield/info。
     * <p>
     * 注意 AEP 定义里 smoke_state 的 <b>0 才是报警态</b>（1=正常）；其余枚举均为 <b>1=报警态</b>
     * （tamper_alarm 0=装上/1=拆除、temperature_state 0=正常/1=过高、humidity_state、disable_status）。
     * 设备处于屏蔽状态时只出 shield 告警，屏蔽中的探测器不应产生火警。
     * <p>
     * ponytail: 持续报警态下设备每个上报周期都会重推同一属性，仅靠 AlarmRecordService 的
     * 5 分钟去重窗口压制；要彻底消噪得加告警自动关闭/按状态变化触发，那时再改。
     */
    private List<AlarmMessage> parseDataReport(AlarmParseContext context) {
        if (!(context.getEventParams().get(CTX_KEY_PAYLOAD) instanceof Map<?, ?> payload)) {
            log.warn("[AEP告警解析] dataReport 缺少 payload 或非对象, deviceId={}", context.getDeviceId());
            return Collections.emptyList();
        }

        Integer disabled = toInteger(payload.get("disable_status"));
        if (disabled != null && disabled == 1) {
            return Collections.singletonList(
                    buildAlarmMessage(context, "shield", "info", "设备已屏蔽"));
        }

        List<AlarmMessage> alarms = new ArrayList<>();
        Integer smoke = toInteger(payload.get("smoke_state"));
        if (smoke != null && smoke == 0) {
            alarms.add(buildAlarmMessage(context, "fire", "critical", "烟雾报警"));
        }
        Integer error = toInteger(payload.get("error"));
        if (error != null) {
            if (error == 1) {
                alarms.add(buildAlarmMessage(context, "fault", "major", "烟感故障"));
            } else if (error == 3) {
                alarms.add(buildAlarmMessage(context, "power_fault", "major", "低压故障"));
            }
        }
        Integer fault = toInteger(payload.get("fault"));
        String faultLabel = fault == null ? null : getFaultLabel(fault);
        if (faultLabel != null) {
            alarms.add(buildAlarmMessage(context, "fault", "major", faultLabel));
        }
        Integer tamper = toInteger(payload.get("tamper_alarm"));
        if (tamper != null && tamper == 1) {
            alarms.add(buildAlarmMessage(context, "risk", "warning", "防拆报警"));
        }
        Integer humidity = toInteger(payload.get("humidity_state"));
        if (humidity != null && humidity == 1) {
            alarms.add(buildAlarmMessage(context, "risk", "warning", "湿度过高"));
        }
        Integer temperature = toInteger(payload.get("temperature_state"));
        if (temperature != null && temperature == 1) {
            alarms.add(buildAlarmMessage(context, "temperature", "warning", "温度过高"));
        }
        return alarms;
    }

    /**
     * AEP 异常告警码 → 中文描述；2/8/10 是故障解除，返回 null 不告警
     */
    private String getFaultLabel(int fault) {
        switch (fault) {
            case 1: return "迷宫污染故障";
            case 3: return "本底值学习故障";
            case 4: return "电路失效故障";
            case 5: return "温度传感器故障";
            case 6: return "温湿度传感器故障";
            case 7: return "低压故障";
            case 9: return "蜂鸣器故障";
            default: return null;
        }
    }

    // ==================== AlarmMessage 构建 ====================

    /**
     * 构建统一的 AlarmMessage
     */
    private AlarmMessage buildAlarmMessage(AlarmParseContext context,
                                           String alarmType, String alarmLevel,
                                           String alarmDescPrefix) {
        Long alarmTime = context.getOccurredTime() != null
                ? context.getOccurredTime()
                : System.currentTimeMillis();

        StringBuilder description = new StringBuilder(alarmDescPrefix);
        if (context.getDeviceName() != null) {
            description.append(" - ").append(context.getDeviceName());
        }

        return AlarmMessage.builder()
                .sourceType(AlarmSourceType.AEP_HTTP_PUSH)
                .alarmType(alarmType)
                .alarmLevel(alarmLevel)
                .deviceId(context.getDeviceId())
                .deviceName(context.getDeviceName())
                .productKey(context.getProductKey())
                .alarmTime(alarmTime)
                .alarmDescription(description.toString())
                .rawData(context.getRawData())
                .build();
    }

    // ==================== 工具方法 ====================

    /**
     * 安全转换为 Integer
     */
    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * eventType → 中文标签
     */
    private String getEventTypeLabel(int eventType) {
        switch (eventType) {
            case EVENT_TYPE_INFO:    return "信息";
            case EVENT_TYPE_WARNING: return "警告";
            case EVENT_TYPE_FAULT:   return "故障";
            default:                 return "未知(" + eventType + ")";
        }
    }
}
