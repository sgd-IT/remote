package com.enjoyiot.eiot.component.tcp.ywn_v2.parser;

/**
 * YWN V2 协议 — 控制类型枚举（共10种）
 */
public enum YwnV2ControlType {

    RESET(1, "复位"),
    MUTE(2, "消音"),
    START(3, "启动"),
    STOP(4, "停止"),
    SHIELD(5, "屏蔽"),
    UNSHIELD(6, "取消屏蔽"),
    AUTO_ALLOW(7, "全部自动"),
    AUTO_DENY(8, "禁止自动"),
    MANUAL_ALLOW(9, "允许手动"),
    MANUAL_DENY(10, "禁止手动");

    private final int code;
    private final String description;

    YwnV2ControlType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static YwnV2ControlType fromCode(int code) {
        for (YwnV2ControlType t : values()) {
            if (t.code == code) return t;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%d(%s)", code, description);
    }
}