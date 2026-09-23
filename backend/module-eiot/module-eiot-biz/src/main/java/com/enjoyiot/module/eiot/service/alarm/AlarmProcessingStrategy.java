package com.enjoyiot.module.eiot.service.alarm;

import java.util.List;
import java.util.Map;

/**
 * 告警处理策略接口（策略模式）
 * <p>
 * 每种 IoT 对接方式（TCP GBT26875、AEP HTTP 推送等）需提供一个实现类，
 * 负责将原始数据解析为标准化的 {@link AlarmMessage} 列表。
 * <p>
 * 注册方式：实现本接口并标注 Spring {@code @Component}，由 {@link AlarmStrategyFactory}
 * 自动发现和注入。
 *
 * @author EnjoyIot
 */
public interface AlarmProcessingStrategy {

    /**
     * 返回本策略支持的告警来源类型
     * <p>
     * 工厂 {@link AlarmStrategyFactory} 通过此方法建立 type → strategy 的映射关系。
     *
     * @return 支持的来源类型枚举
     */
    AlarmSourceType supportedSourceType();

    /**
     * 从原始上下文数据中解析告警消息列表
     * <p>
     * 一次协议帧 / HTTP 推送可能包含多条告警（如 GBT26875 TLD=1 的系统状态
     * 可同时触发火警+电源故障），因此返回列表而非单条。
     *
     * @param context 原始告警上下文，包含设备信息、原始数据、事件参数等
     * @return 解析后的标准化告警消息列表（可能为空列表，但不会为 null）
     */
    List<AlarmMessage> parse(AlarmParseContext context);

    /**
     * 告警解析上下文
     * <p>
     * 封装不同来源的原始数据，避免策略接口与具体协议耦合。
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class AlarmParseContext {

        /** 告警来源类型 */
        private AlarmSourceType sourceType;

        /** 设备ID（已关联到本地设备表的ID） */
        private Long deviceId;

        /** 设备名称或编号 */
        private String deviceName;

        /** 产品Key */
        private String productKey;

        /** 事件/消息类型名称（如 fire_alarm_event / fault_event / dataReport） */
        private String eventName;

        /** 事件参数（由协议解析器产出的结构化数据） */
        private Map<String, Object> eventParams;

        /** 消息发生时间戳（毫秒） */
        private Long occurredTime;

        /** 原始数据（JSON 字符串或原始报文 HEX，供追溯） */
        private String rawData;

        /** 扩展属性（各策略实现可存放自定义上下文） */
        private Map<String, Object> extra;
    }
}
