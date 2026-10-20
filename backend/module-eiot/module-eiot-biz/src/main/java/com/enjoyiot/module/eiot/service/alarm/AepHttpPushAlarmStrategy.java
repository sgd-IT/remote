package com.enjoyiot.module.eiot.service.alarm;

import cn.hutool.json.JSONUtil;
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
    /** 上下文中存放 eventReport 事件内容的 key（与 AepCallbackServiceImpl 的 put key 一致） */
    static final String CTX_KEY_EVENT_CONTENT = "eventContent";
    /** AEP messageType: 事件上报 */
    static final String MSG_EVENT_REPORT = "eventReport";
    /** AEP messageType: 数据变化上报 */
    static final String MSG_DATA_REPORT = "dataReport";

    /** dataReport/eventReport 共用的告警属性判定键（用于区分"状态正常的恢复态"与"无法解析的事件内容"） */
    private static final List<String> ALARM_ATTRIBUTE_KEYS = List.of(
            "disable_status", "smoke_state", "error", "fault",
            "tamper_alarm", "humidity_state", "temperature_state");

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
     * 优先解析 {@code eventContent} 中的具体事件内容（与 dataReport 共用 {@link #alarmsFromAttributes} 判定）：
     * 识别出报警 → 产出具体描述（如"防拆报警"/"烟感报警"）；识别为恢复态 → 不生成告警；
     * 无法识别 → 回退按 eventType 泛化描述。
     * <p>
     * 泛化回退口径：eventType=1(信息) → 不告警，返回空列表
     * eventType=2(警告) → alarmType=risk, alarmLevel=warning
     * eventType=3(故障) → alarmType=fault, alarmLevel=major
     */
    private List<AlarmMessage> parseEventReport(AlarmParseContext context, Integer eventType) {
        if (eventType == null) {
            log.warn("[AEP告警解析] eventReport 缺少 eventType, deviceId={}", context.getDeviceId());
            return Collections.emptyList();
        }

        // 信息类事件不生成告警
        if (eventType == EVENT_TYPE_INFO) {
            log.debug("[AEP告警解析] eventReport eventType=1(信息)，不生成告警, deviceId={}",
                    context.getDeviceId());
            return Collections.emptyList();
        }

        // 优先按 eventContent 判定具体事件（如 {"tamper_alarm":1,"serviceid":1005} → 防拆报警）
        Map<?, ?> content = toContentMap(context.getEventParams().get(CTX_KEY_EVENT_CONTENT));
        if (content != null && !content.isEmpty()) {
            List<AlarmMessage> specific = alarmsFromAttributes(context, content);
            if (!specific.isEmpty()) {
                log.debug("[AEP告警解析] eventReport 解析出具体事件 {} 条, deviceId={}, content={}",
                        specific.size(), context.getDeviceId(), content);
                return specific;
            }
            if (hasAlarmAttribute(content)) {
                // 恢复态事件（如 tamper_alarm=0 / smoke_state=1）：不生成告警，与 dataReport 口径一致
                log.debug("[AEP告警解析] eventReport 为恢复态事件，不生成告警, deviceId={}, content={}",
                        context.getDeviceId(), content);
                return Collections.emptyList();
            }
        }

        // 回退：无法解析具体内容时按 eventType 泛化生成（保持原行为）
        String alarmType;
        String alarmLevel;

        switch (eventType) {
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
     * 判定逻辑与 eventReport 的 eventContent 共用 {@link #alarmsFromAttributes}
     */
    private List<AlarmMessage> parseDataReport(AlarmParseContext context) {
        if (!(context.getEventParams().get(CTX_KEY_PAYLOAD) instanceof Map<?, ?> payload)) {
            log.warn("[AEP告警解析] dataReport 缺少 payload 或非对象, deviceId={}", context.getDeviceId());
            return Collections.emptyList();
        }
        return alarmsFromAttributes(context, payload);
    }

    /**
     * 从属性集判定告警（dataReport 属性快照与 eventReport 事件内容共用同一套判定）
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
    private List<AlarmMessage> alarmsFromAttributes(AlarmParseContext context, Map<?, ?> attrs) {
        Integer disabled = toInteger(attrs.get("disable_status"));
        if (disabled != null && disabled == 1) {
            return Collections.singletonList(
                    buildAlarmMessage(context, "shield", "info", "设备已屏蔽"));
        }

        List<AlarmMessage> alarms = new ArrayList<>();
        Integer smoke = toInteger(attrs.get("smoke_state"));
        if (smoke != null && smoke == 0) {
            alarms.add(buildAlarmMessage(context, "fire", "critical", "烟感报警"));
        }
        Integer error = toInteger(attrs.get("error"));
        if (error != null) {
            if (error == 1) {
                alarms.add(buildAlarmMessage(context, "fault", "major", "烟感故障"));
            } else if (error == 3) {
                alarms.add(buildAlarmMessage(context, "power_fault", "major", "低压故障"));
            }
        }
        Integer fault = toInteger(attrs.get("fault"));
        String faultLabel = fault == null ? null : getFaultLabel(fault);
        if (faultLabel != null) {
            alarms.add(buildAlarmMessage(context, "fault", "major", faultLabel));
        }
        Integer tamper = toInteger(attrs.get("tamper_alarm"));
        if (tamper != null && tamper == 1) {
            alarms.add(buildAlarmMessage(context, "risk", "warning", "防拆报警"));
        }
        Integer humidity = toInteger(attrs.get("humidity_state"));
        if (humidity != null && humidity == 1) {
            alarms.add(buildAlarmMessage(context, "risk", "warning", "湿度过高"));
        }
        Integer temperature = toInteger(attrs.get("temperature_state"));
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

        return AlarmMessage.builder()
                .sourceType(AlarmSourceType.AEP_HTTP_PUSH)
                .alarmType(alarmType)
                .alarmLevel(alarmLevel)
                .deviceId(context.getDeviceId())
                .deviceName(context.getDeviceName())
                .productKey(context.getProductKey())
                .alarmTime(alarmTime)
                // 描述保持纯事件名（如"烟感报警"）；设备名/租户名由名称与详情各自拼接，不再拼进描述
                .alarmDescription(alarmDescPrefix)
                .rawData(context.getRawData())
                .build();
    }

    // ==================== 工具方法 ====================

    /**
     * eventContent 归一为 Map（兼容对象与 JSON 字符串两种形态；无法解析返回 null）
     */
    private Map<?, ?> toContentMap(Object eventContent) {
        if (eventContent instanceof Map<?, ?> map) {
            return map;
        }
        if (eventContent instanceof String str && str.trim().startsWith("{")) {
            try {
                return JSONUtil.parseObj(str);
            } catch (Exception e) {
                log.warn("[AEP告警解析] eventContent 字符串解析失败: {}", str);
            }
        }
        return null;
    }

    /**
     * 属性集中是否含任一已知告警属性（区分"状态正常的恢复态"与"无法解析的事件内容"）
     */
    private boolean hasAlarmAttribute(Map<?, ?> attrs) {
        for (String key : ALARM_ATTRIBUTE_KEYS) {
            if (attrs.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

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
