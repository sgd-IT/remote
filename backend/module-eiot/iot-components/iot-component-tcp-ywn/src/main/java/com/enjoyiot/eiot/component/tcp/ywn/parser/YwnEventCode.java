package com.enjoyiot.eiot.component.tcp.ywn.parser;

/**
 * 控制器监控TCP协议V27 — 事件代码枚举
 *
 * 事件代码占2字节(事件数据偏移4-5)，常见值：
 * 火警类: 0x01~0x0F
 * 故障类: 0x40~0x5F
 * 反馈类: 0x1A~0x2F
 * 屏蔽类: 0x30~0x3F
 * 恢复类: 在原事件码基础上+偏移
 */
public enum YwnEventCode {

    // ========== 火警类 ==========
    FIRE_ALARM(0x01, "火警"),
    FIRE_ALARM_LINKAGE(0x02, "火警联动"),
    FIRE_ALARM_START(0x03, "火警启动"),
    FIRE_ALARM_FEEDBACK(0x04, "火警反馈"),

    // ========== 反馈类 ==========
    BUS_DEVICE_FEEDBACK(0x1A, "总线设备反馈"),
    BUS_DEVICE_FEEDBACK_CANCEL(0x1C, "反馈撤销"),

    // ========== 屏蔽类 ==========
    BUS_DEVICE_SHIELDED(0x30, "总线设备屏蔽"),

    // ========== 故障类 ==========
    BUS_DEVICE_FAULT(0x50, "总线设备故障"),
    BUS_DEVICE_FAULT_RECOVERY(0x64, "故障恢复"),

    // ========== 启动类 ==========
    BUS_DEVICE_STARTED(0x10, "总线设备启动"),
    BUS_DEVICE_STOPPED(0x12, "总线设备停止"),

    // ========== 其他 ==========
    MANUAL_AUTO_SWITCH(0x70, "手自动转换"),
    OFFLINE_PERSON(0x7E, "离人报警"),
    SUPERVISORY(0x7F, "监管报警");

    private final int code;
    private final String description;

    YwnEventCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static YwnEventCode fromCode(int code) {
        for (YwnEventCode ec : values()) {
            if (ec.code == code) return ec;
        }
        return null;
    }

    /** 判断是否为火警类事件 */
    public boolean isFireAlarm() {
        return code >= 0x01 && code <= 0x0F;
    }

    /** 判断是否为故障类事件 */
    public boolean isFault() {
        return code >= 0x40 && code <= 0x5F;
    }

    /** 判断是否为恢复类事件 */
    public boolean isRecovery() {
        return code == 0x64 || code == 0x1C;
    }

    @Override
    public String toString() {
        return String.format("0x%02X(%s)", code, description);
    }
}
