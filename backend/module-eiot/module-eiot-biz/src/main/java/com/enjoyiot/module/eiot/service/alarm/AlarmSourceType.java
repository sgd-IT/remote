package com.enjoyiot.module.eiot.service.alarm;

/**
 * 告警数据来源类型枚举
 * <p>
 * 标识触发告警的 IoT 设备对接方式，供 {@link AlarmStrategyFactory} 路由到对应的
 * {@link AlarmProcessingStrategy} 实现。
 *
 * @author EnjoyIot
 */
public enum AlarmSourceType {

    /**
     * TCP GBT26875 协议组件
     * <p>
     * 设备通过 TCP 长连接上报 GBT26875 协议帧，由
     * {@code GbtTcpComponent} / {@code GbtDataReporter} 解析后触发告警。
     */
    TCP_GBT26875("TCP_GBT26875", "GBT26875 TCP远程监控组件"),

    /**
     * AEP 平台 HTTP 推送
     * <p>
     * AEP 物联网平台通过 HTTPS 推送结构化 JSON 消息到
     * {@code AepCallbackService}，经消息类型路由后触发告警。
     */
    AEP_HTTP_PUSH("AEP_HTTP_PUSH", "AEP平台HTTP推送");

    private final String code;
    private final String description;

    AlarmSourceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据编码查找对应的来源类型
     *
     * @param code 来源编码
     * @return 匹配的 AlarmSourceType，未匹配则返回 null
     */
    public static AlarmSourceType fromCode(String code) {
        for (AlarmSourceType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
