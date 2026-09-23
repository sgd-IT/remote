package com.enjoyiot.eiot.component.tcp.ywn.parser;

/**
 * 控制器监控TCP协议V27 — 命令类型枚举
 *
 * 帧格式：命令码(1B) + 设备编号(2B LE) + 载荷(变长)
 * 控制类命令统一15字节帧，上传类命令按实际载荷计算
 */
public enum YwnCommand {

    // ========== 服务端/设备双向 ==========
    ACK(0x01, 3, "应答", Direction.BOTH),

    // ========== 下发控制命令 (服务端→设备) ==========
    RESET(0x02, 15, "设备复位", Direction.DOWN),
    SILENCE(0x03, 15, "消音", Direction.DOWN),
    BUS_START(0x04, 15, "启动总线设备", Direction.DOWN),
    BUS_STOP(0x05, 15, "停止总线设备", Direction.DOWN),
    MANUAL_AUTO_SWITCH(0x06, 15, "手自动转换", Direction.DOWN),
    SHIELD(0x07, 15, "屏蔽", Direction.DOWN),
    CANCEL_SHIELD(0x08, 15, "取消屏蔽", Direction.DOWN),
    SELF_CHECK(0x09, 15, "自检(预留)", Direction.DOWN),
    MULTI_LINE_START(0x0A, 15, "多线盘启动", Direction.DOWN),
    MULTI_LINE_STOP(0x0B, 15, "多线盘停止", Direction.DOWN),
    AUTO_ALLOW(0x0C, 15, "自动允许", Direction.DOWN),
    AUTO_PROHIBIT(0x0D, 15, "自动禁止", Direction.DOWN),
    MANUAL_ALLOW(0x0E, 15, "手动允许", Direction.DOWN),
    MANUAL_PROHIBIT(0x0F, 15, "手动禁止", Direction.DOWN),
    AUTO_MANUAL_ALLOW(0x10, 15, "自动手动允许", Direction.DOWN),
    AUTO_MANUAL_PROHIBIT(0x11, 15, "自动手动禁止", Direction.DOWN),

    // ========== 上传命令 (设备→服务端) ==========
    EVENT_UPLOAD(0x80, 67, "事件上传", Direction.UP),
    DEVICE_INFO_UPLOAD(0x81, 46, "设备信息上传", Direction.UP),
    HEARTBEAT(0x82, 3, "心跳", Direction.UP),
    HEARTBEAT_ACK(0x83, 3, "心跳应答", Direction.DOWN),
    CONTROLLER_FAULT(0x86, 3, "控制器故障", Direction.UP),
    CONTROLLER_FAULT_RECOVERY(0x87, 3, "控制器故障恢复", Direction.UP);

    private final int code;
    private final int frameLength;  // 整帧长度(含命令码1B)
    private final String description;
    private final Direction direction;

    YwnCommand(int code, int frameLength, String description, Direction direction) {
        this.code = code;
        this.frameLength = frameLength;
        this.description = description;
        this.direction = direction;
    }

    public int getCode() { return code; }
    public int getFrameLength() { return frameLength; }
    public String getDescription() { return description; }
    public Direction getDirection() { return direction; }

    /** 根据命令码查找枚举 */
    public static YwnCommand fromCode(int code) {
        for (YwnCommand cmd : values()) {
            if (cmd.code == code) return cmd;
        }
        return null;
    }

    /** 根据命令码获取帧长度，未知命令返回 -1 */
    public static int getFrameLengthByCode(int code) {
        YwnCommand cmd = fromCode(code);
        return cmd != null ? cmd.frameLength : -1;
    }

    public enum Direction {
        UP,     // 设备→服务端
        DOWN,   // 服务端→设备
        BOTH    // 双向
    }

    @Override
    public String toString() {
        return String.format("0x%02X(%s)", code, description);
    }
}
