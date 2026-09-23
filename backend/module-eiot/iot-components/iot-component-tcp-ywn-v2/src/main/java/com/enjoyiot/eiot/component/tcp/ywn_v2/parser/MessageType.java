package com.enjoyiot.eiot.component.tcp.ywn_v2.parser;

/**
 * YWN V2 协议 — 消息类型枚举
 *
 * 帧头格式: 数据长度(2B BE) + 消息类型(2B BE) + 序列号(4B BE) + Protobuf载荷
 * 所有消息均需确认，确认消息类型和序列号与原始消息一致
 */
public enum MessageType {

    /** 注册 (客户端→服务端) */
    REGISTER(1, "注册"),

    /** 心跳 (客户端→服务端) */
    HEARTBEAT(2, "心跳"),

    /** 事件上报 (客户端→服务端) */
    EVENT(3, "事件上报"),

    /** 控制下发 (服务端→客户端) */
    CONTROL(4, "控制下发");

    private final int code;
    private final String description;

    MessageType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static MessageType fromCode(int code) {
        for (MessageType t : values()) {
            if (t.code == code) return t;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%d(%s)", code, description);
    }
}