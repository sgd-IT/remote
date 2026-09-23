package com.enjoyiot.eiot.component.tcp.ywn_v2.parser;

/**
 * YWN V2 协议 — 事件类型枚举（共32种）
 *
 * 1-15:  基本事件
 * 16-30: 恢复事件（对应基本事件+15）
 * 31-32: 特殊事件
 */
public enum YwnV2EventType {

    // ========== 基本事件 (1-15) ==========
    ALARM(1, "火警"),
    FAULT(2, "故障"),
    SHIELD(3, "屏蔽"),
    SUPERVISION(4, "监管"),
    START(5, "启动"),
    FEEDBACK(6, "反馈"),
    DELAY(7, "延时状态"),
    MAIN_POWER(8, "主电故障"),
    EXTRA_POWER(9, "备电故障"),
    EVENT_10(10, "预留事件10"),
    HAND_AUTO(11, "手动状态"),
    EVENT_12(12, "预留事件12"),
    RESET(13, "复位"),
    EVENT_14(14, "预留事件14"),
    EVENT_15(15, "预留事件15"),

    // ========== 恢复事件 (16-30) ==========
    ALARM_RECOVER(16, "火警恢复"),
    FAULT_RECOVER(17, "故障恢复"),
    SHIELD_RECOVER(18, "屏蔽恢复"),
    SUPERVISION_RECOVER(19, "监管恢复"),
    START_RECOVER(20, "启动恢复"),
    FEEDBACK_RECOVER(21, "反馈恢复"),
    DELAY_RECOVER(22, "延时状态恢复"),
    MAIN_POWER_RECOVER(23, "主电故障恢复"),
    EXTRA_POWER_RECOVER(24, "备电故障恢复"),
    EVENT_10_RECOVER(25, "预留10恢复"),
    HAND_AUTO_RECOVER(26, "自动状态"),
    EVENT_12_RECOVER(27, "预留12恢复"),
    RESET_RECOVER(28, "复位恢复"),
    EVENT_14_RECOVER(29, "预留14恢复"),
    EVENT_15_RECOVER(30, "预留15恢复"),

    // ========== 特殊事件 (31-32) ==========
    NOBODY(31, "无人状态"),
    NOBODY_RECOVER(32, "无人恢复");

    private final int code;
    private final String description;

    YwnV2EventType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static YwnV2EventType fromCode(int code) {
        for (YwnV2EventType t : values()) {
            if (t.code == code) return t;
        }
        return null;
    }

    /** 是否为恢复类事件 */
    public boolean isRecovery() {
        return code >= 16 && code <= 30;
    }

    /** 是否为火警类事件 */
    public boolean isFireAlarm() {
        return code == 1;
    }

    /** 是否为故障类事件 */
    public boolean isFault() {
        return code == 2 || code == 8 || code == 9;
    }

    /** 是否为屏蔽类事件 */
    public boolean isShield() {
        return code == 3;
    }

    /** 是否为启动类事件 */
    public boolean isStart() {
        return code == 5;
    }

    /** 是否为反馈类事件 */
    public boolean isFeedback() {
        return code == 6;
    }

    /** 是否为监管类事件 */
    public boolean isSupervision() {
        return code == 4;
    }

    @Override
    public String toString() {
        return String.format("%d(%s)", code, description);
    }
}