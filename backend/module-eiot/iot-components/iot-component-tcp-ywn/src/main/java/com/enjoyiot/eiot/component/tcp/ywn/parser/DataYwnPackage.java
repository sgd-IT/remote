package com.enjoyiot.eiot.component.tcp.ywn.parser;

import cn.hutool.core.util.HexUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * YWN控制器监控TCP协议V27 — 帧数据模型
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
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DataYwnPackage {

    // ========== 上行命令码 (设备→服务端) ==========
    public static final int CMD_ACK = 0x01;
    public static final int CMD_EVENT_UPLOAD = 0x80;
    public static final int CMD_DEVICE_INFO_UPLOAD = 0x81;
    public static final int CMD_HEARTBEAT = 0x82;
    public static final int CMD_HEARTBEAT_ACK = 0x83;
    public static final int CMD_CONTROLLER_FAULT = 0x86;
    public static final int CMD_CONTROLLER_FAULT_RECOVERY = 0x87;

    // ========== 下行控制命令码 (服务端→设备) ==========
    public static final int CMD_RESET = 0x02;
    public static final int CMD_SILENCE = 0x03;
    public static final int CMD_BUS_START = 0x04;
    public static final int CMD_BUS_STOP = 0x05;
    public static final int CMD_MANUAL_AUTO_SWITCH = 0x06;
    public static final int CMD_SHIELD = 0x07;
    public static final int CMD_CANCEL_SHIELD = 0x08;
    public static final int CMD_SELF_CHECK = 0x09;
    public static final int CMD_MULTI_LINE_START = 0x0A;
    public static final int CMD_MULTI_LINE_STOP = 0x0B;
    public static final int CMD_AUTO_ALLOW = 0x0C;
    public static final int CMD_AUTO_PROHIBIT = 0x0D;
    public static final int CMD_MANUAL_ALLOW = 0x0E;
    public static final int CMD_MANUAL_PROHIBIT = 0x0F;
    public static final int CMD_AUTO_MANUAL_ALLOW = 0x10;
    public static final int CMD_AUTO_MANUAL_PROHIBIT = 0x11;

    /** 命令码 */
    private int command;

    /** 设备编号 (1-65535, LE) */
    private int deviceNumber;

    /** 载荷(命令码和设备编号之后的数据) */
    private byte[] payload;

    // ========== 便捷方法 ==========

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

    /** 如果是事件上传帧(0x80)，解析事件数据 */
    public YwnEvent parseEvent() {
        if (command != CMD_EVENT_UPLOAD || payload == null || payload.length != 64) {
            return null;
        }
        return YwnEvent.fromBytes(payload);
    }

    /** 整帧长度 */
    public int getFrameLength() {
        YwnCommand cmd = YwnCommand.fromCode(command);
        if (cmd != null) {
            return cmd.getFrameLength();
        }
        return 3 + (payload != null ? payload.length : 0);
    }

    /** 转为整帧字节数组 */
    public byte[] toBytes() {
        int frameLen = getFrameLength();
        ByteBuffer buf = ByteBuffer.allocate(frameLen).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) command);
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

    /** 转为hex字符串(用于日志) */
    public String toHexString() {
        byte[] data = toBytes();
        StringBuilder sb = new StringBuilder(data.length * 3);
        for (int i = 0; i < Math.min(data.length, 32); i++) {
            sb.append(String.format("%02X ", data[i] & 0xFF));
        }
        if (data.length > 32) sb.append("...");
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        YwnCommand cmd = YwnCommand.fromCode(command);
        return String.format("YwnPkg{cmd=%s, dev=%d, payloadLen=%d}",
                cmd != null ? cmd : String.format("0x%02X", command),
                deviceNumber, payload != null ? payload.length : 0);
    }

    public static class BufferSerializer extends JsonSerializer<byte[]> {
        @Override
        public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(HexUtil.encodeHexStr(value));
        }
    }
}
