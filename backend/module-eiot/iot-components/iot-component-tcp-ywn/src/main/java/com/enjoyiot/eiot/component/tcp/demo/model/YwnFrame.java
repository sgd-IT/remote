package com.enjoyiot.eiot.component.tcp.demo.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * 控制器监控TCP协议V27 — 帧模型
 *
 * 统一帧格式: 命令码(1B) + 设备编号(2B LE) + 载荷(变长)
 * 各命令帧长度由 YwnCommand.frameLength 定义
 *
 * 控制类命令载荷(15字节帧):
 * | 偏移 | 字段     | 字节数 |
 * |------|---------|--------|
 * | 0    | 命令码   | 1      |
 * | 1-2  | 设备编号  | 2 (LE) |
 * | 3    | 机号     | 1      |
 * | 4    | 回路号   | 1      |
 * | 5    | 地址号   | 1      |
 * | 6-14 | 备用     | 9 (填0) |
 *
 * 手自动转换(命令码0x06)载荷:
 * | 偏移 | 字段      | 字节数 |
 * |------|----------|--------|
 * | 0    | 命令码    | 1      |
 * | 1-2  | 设备编号   | 2 (LE) |
 * | 3    | 机号      | 1      |
 * | 4    | 模式      | 1 (2=手动, 3=自动) |
 * | 5-14 | 备用      | 10 (填0) |
 */
public class YwnFrame {

    private YwnCommand command;
    private int deviceNumber;   // 1-65535
    private byte[] payload;     // 命令码和设备编号之后的载荷

    public YwnFrame() {}

    public YwnFrame(YwnCommand command, int deviceNumber, byte[] payload) {
        this.command = command;
        this.deviceNumber = deviceNumber;
        this.payload = payload;
    }

    // ========== 工厂方法 ==========

    /** 创建应答帧 (3字节) */
    public static YwnFrame ack(int deviceNumber) {
        return new YwnFrame(YwnCommand.ACK, deviceNumber, new byte[0]);
    }

    /** 创建心跳帧 (3字节) */
    public static YwnFrame heartbeat(int deviceNumber) {
        return new YwnFrame(YwnCommand.HEARTBEAT, deviceNumber, new byte[0]);
    }

    /** 创建心跳应答帧 (3字节) */
    public static YwnFrame heartbeatAck(int deviceNumber) {
        return new YwnFrame(YwnCommand.HEARTBEAT_ACK, deviceNumber, new byte[0]);
    }

    /** 创建设备复位帧 */
    public static YwnFrame reset(int deviceNumber, int machineNo) {
        byte[] payload = new byte[12]; // 15 - 3(命令码+设备编号)
        payload[0] = (byte) machineNo;
        return new YwnFrame(YwnCommand.RESET, deviceNumber, payload);
    }

    /** 创建消音帧 */
    public static YwnFrame silence(int deviceNumber, int machineNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        return new YwnFrame(YwnCommand.SILENCE, deviceNumber, payload);
    }

    /** 创建启动总线设备帧 */
    public static YwnFrame busStart(int deviceNumber, int machineNo, int loopNo, int addressNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        payload[1] = (byte) loopNo;
        payload[2] = (byte) addressNo;
        return new YwnFrame(YwnCommand.BUS_START, deviceNumber, payload);
    }

    /** 创建停止总线设备帧 */
    public static YwnFrame busStop(int deviceNumber, int machineNo, int loopNo, int addressNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        payload[1] = (byte) loopNo;
        payload[2] = (byte) addressNo;
        return new YwnFrame(YwnCommand.BUS_STOP, deviceNumber, payload);
    }

    /** 创建手自动转换帧 (2=手动, 3=自动) */
    public static YwnFrame manualAutoSwitch(int deviceNumber, int machineNo, int mode) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        payload[1] = (byte) mode;  // 2=手动, 3=自动
        return new YwnFrame(YwnCommand.MANUAL_AUTO_SWITCH, deviceNumber, payload);
    }

    /** 创建屏蔽帧 */
    public static YwnFrame shield(int deviceNumber, int machineNo, int loopNo, int addressNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        payload[1] = (byte) loopNo;
        payload[2] = (byte) addressNo;
        return new YwnFrame(YwnCommand.SHIELD, deviceNumber, payload);
    }

    /** 创建取消屏蔽帧 */
    public static YwnFrame cancelShield(int deviceNumber, int machineNo, int loopNo, int addressNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        payload[1] = (byte) loopNo;
        payload[2] = (byte) addressNo;
        return new YwnFrame(YwnCommand.CANCEL_SHIELD, deviceNumber, payload);
    }

    /** 创建多线盘启动帧 */
    public static YwnFrame multiLineStart(int deviceNumber, int machineNo, int diskNo, int roadNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        payload[1] = (byte) diskNo;
        payload[2] = (byte) roadNo;
        return new YwnFrame(YwnCommand.MULTI_LINE_START, deviceNumber, payload);
    }

    /** 创建多线盘停止帧 */
    public static YwnFrame multiLineStop(int deviceNumber, int machineNo, int diskNo, int roadNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        payload[1] = (byte) diskNo;
        payload[2] = (byte) roadNo;
        return new YwnFrame(YwnCommand.MULTI_LINE_STOP, deviceNumber, payload);
    }

    /** 创建自动允许帧 */
    public static YwnFrame autoAllow(int deviceNumber, int machineNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        return new YwnFrame(YwnCommand.AUTO_ALLOW, deviceNumber, payload);
    }

    /** 创建自动禁止帧 */
    public static YwnFrame autoProhibit(int deviceNumber, int machineNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        return new YwnFrame(YwnCommand.AUTO_PROHIBIT, deviceNumber, payload);
    }

    /** 创建手动允许帧 */
    public static YwnFrame manualAllow(int deviceNumber, int machineNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        return new YwnFrame(YwnCommand.MANUAL_ALLOW, deviceNumber, payload);
    }

    /** 创建手动禁止帧 */
    public static YwnFrame manualProhibit(int deviceNumber, int machineNo) {
        byte[] payload = new byte[12];
        payload[0] = (byte) machineNo;
        return new YwnFrame(YwnCommand.MANUAL_PROHIBIT, deviceNumber, payload);
    }

    /** 创建控制器故障帧 (3字节) */
    public static YwnFrame controllerFault(int deviceNumber) {
        return new YwnFrame(YwnCommand.CONTROLLER_FAULT, deviceNumber, new byte[0]);
    }

    /** 创建控制器故障恢复帧 (3字节) */
    public static YwnFrame controllerFaultRecovery(int deviceNumber) {
        return new YwnFrame(YwnCommand.CONTROLLER_FAULT_RECOVERY, deviceNumber, new byte[0]);
    }

    /** 创建事件上传帧 */
    public static YwnFrame eventUpload(int deviceNumber, YwnEvent event) {
        return new YwnFrame(YwnCommand.EVENT_UPLOAD, deviceNumber, event.toBytes());
    }

    /** 创建设备信息上传帧 */
    public static YwnFrame deviceInfoUpload(YwnDeviceInfo deviceInfo) {
        return new YwnFrame(YwnCommand.DEVICE_INFO_UPLOAD, deviceInfo.getDeviceNumber(),
                Arrays.copyOfRange(deviceInfo.toFrameBytes(), 3, 46)); // 跳过命令码+设备编号
    }

    // ========== 解析 ==========

    /** 从整帧字节数组解析 */
    public static YwnFrame fromBytes(byte[] data) {
        if (data == null || data.length < 3) {
            throw new IllegalArgumentException("帧数据至少3字节");
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        YwnFrame frame = new YwnFrame();

        int cmdCode = buf.get() & 0xFF;
        frame.command = YwnCommand.fromCode(cmdCode);
        frame.deviceNumber = buf.getShort() & 0xFFFF;

        int payloadLen = data.length - 3;
        if (payloadLen > 0) {
            frame.payload = new byte[payloadLen];
            buf.get(frame.payload);
        } else {
            frame.payload = new byte[0];
        }

        return frame;
    }

    /** 序列化为整帧字节数组 */
    public byte[] toBytes() {
        int frameLen = command.getFrameLength();
        ByteBuffer buf = ByteBuffer.allocate(frameLen).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) command.getCode());
        buf.putShort((short) deviceNumber);

        if (payload != null && payload.length > 0) {
            buf.put(payload);
        }
        // 补零到帧长
        int remaining = frameLen - buf.position();
        if (remaining > 0) {
            buf.put(new byte[remaining]);
        }
        return buf.array();
    }

    // ========== 事件解析便捷方法 ==========

    /** 如果是事件上传帧，解析事件数据 */
    public YwnEvent parseEvent() {
        if (command != YwnCommand.EVENT_UPLOAD || payload == null || payload.length != 64) {
            return null;
        }
        return YwnEvent.fromBytes(payload);
    }

    // ========== Getters & Setters ==========

    public YwnCommand getCommand() { return command; }
    public void setCommand(YwnCommand command) { this.command = command; }
    public int getDeviceNumber() { return deviceNumber; }
    public void setDeviceNumber(int deviceNumber) { this.deviceNumber = deviceNumber; }
    public byte[] getPayload() { return payload; }
    public void setPayload(byte[] payload) { this.payload = payload; }

    /** 获取载荷中的机号(偏移0) */
    public int getMachineNo() {
        return (payload != null && payload.length > 0) ? payload[0] & 0xFF : 0;
    }

    /** 获取载荷中的回路号(偏移1) */
    public int getLoopNo() {
        return (payload != null && payload.length > 1) ? payload[1] & 0xFF : 0;
    }

    /** 获取载荷中的地址号(偏移2) */
    public int getAddressNo() {
        return (payload != null && payload.length > 2) ? payload[2] & 0xFF : 0;
    }

    /** 获取载荷中的手自动模式(偏移1, 仅命令0x06) */
    public int getManualAutoMode() {
        return (payload != null && payload.length > 1) ? payload[1] & 0xFF : 0;
    }

    @Override
    public String toString() {
        return String.format("Frame{cmd=%s, dev=%d, payloadLen=%d}",
                command, deviceNumber, payload != null ? payload.length : 0);
    }

    /** 转hex字符串(用于日志) */
    public String toHexString() {
        byte[] data = toBytes();
        StringBuilder sb = new StringBuilder(data.length * 3);
        for (byte b : data) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}
