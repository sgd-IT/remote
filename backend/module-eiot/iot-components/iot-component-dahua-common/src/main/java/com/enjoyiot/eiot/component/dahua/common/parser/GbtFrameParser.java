package com.enjoyiot.eiot.component.dahua.common.parser;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * GB/T 26875.3-2011 传输帧解析工具（TCP/UDP 共用）
 */
@Slf4j
public final class GbtFrameParser {

    /** 启动符 '@' */
    public static final byte START_BYTE = 0x40;
    /** 结束符 '#' */
    public static final byte END_BYTE = 0x23;
    /** 控制头长度: @@(2)+serial(2)+ver(2)+ts(6)+src(6)+dst(6)+len(2) = 26 */
    public static final int HEADER_SIZE = 26;
    /** 帧尾: checksum(1)+##(2) = 3 */
    public static final int TAIL_SIZE = 3;
    /** 协议规定应用数据单元最大长度 */
    public static final int MAX_ADU_LENGTH = 512;
    /** 数据长度字段偏移 */
    public static final int LENGTH_OFFSET = 22;

    private GbtFrameParser() {}

    /**
     * 检测结果是否为 GBT26875 帧（@@ 起始 + ## 结尾）
     *
     * @param raw 原始字节
     * @return true 如果帧边界标识匹配
     */
    public static boolean isGbtFrame(byte[] raw) {
        return raw != null && raw.length >= HEADER_SIZE + TAIL_SIZE
                && raw[0] == START_BYTE && raw[1] == START_BYTE
                && raw[raw.length - 2] == END_BYTE && raw[raw.length - 1] == END_BYTE;
    }

    /**
     * 从完整 GBT26875 帧中提取结构化结果
     *
     * @param raw 完整帧字节（含 @@ 和 ##）
     * @return 解析结果；帧格式不合法时返回 null
     */
    public static Result parse(byte[] raw) {
        if (raw == null || raw.length < HEADER_SIZE + TAIL_SIZE) {
            return null;
        }
        // 验证帧头帧尾
        if (raw[0] != START_BYTE || raw[1] != START_BYTE) {
            return null;
        }
        if (raw[raw.length - 2] != END_BYTE || raw[raw.length - 1] != END_BYTE) {
            return null;
        }

        ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);

        // 跳启动符
        buf.position(2);
        int serial = buf.getShort() & 0xFFFF;

        // 协议版本
        int majorVer = buf.get() & 0xFF;
        int minorVer = buf.get() & 0xFF;

        // 时间标签 (6B)
        byte[] timestamp = new byte[6];
        buf.get(timestamp);

        // 源地址 (6B, BE)
        byte[] srcAddr = new byte[6];
        buf.get(srcAddr);

        // 目的地址 (6B, BE)
        byte[] dstAddr = new byte[6];
        buf.get(dstAddr);

        // 应用数据单元长度 (2B, LE)
        int dataLength = buf.getShort() & 0xFFFF;

        // 协议合规性检查
        if (dataLength > MAX_ADU_LENGTH) {
            log.warn("[GBT帧] 应用数据单元长度{}超过协议上限{}，帧可能不合规",
                    dataLength, MAX_ADU_LENGTH);
        }

        // 验证帧总长度一致性
        int expectedFrameLen = HEADER_SIZE + 1 + dataLength + TAIL_SIZE;
        if (expectedFrameLen != raw.length) {
            log.warn("[GBT帧] 帧长度不一致: 期望{}实际{}，数据长度字段={}",
                    expectedFrameLen, raw.length, dataLength);
            // 不直接返回 null，尝试按数据长度字段提取（容错）
        }

        // 命令字
        int command = buf.get() & 0xFF;

        // 应用数据单元
        if (buf.remaining() < dataLength + TAIL_SIZE) {
            log.warn("[GBT帧] 帧数据不足: 剩余{}B，需要{}B(data)+{}B(tail)",
                    buf.remaining(), dataLength, TAIL_SIZE);
            return null;
        }

        byte[] appData = new byte[dataLength];
        buf.get(appData);

        // 校验和 + ## 跳过（不校验 CRC，由上层决定是否需要）

        Result result = new Result();
        result.serialNumber = serial;
        result.majorVersion = majorVer;
        result.minorVersion = minorVer;
        result.timestamp = timestamp;
        result.sourceAddr = srcAddr;
        result.destAddr = dstAddr;
        result.command = command;
        result.appData = appData;
        result.dataLength = dataLength;

        return result;
    }

    /**
     * 从源地址字节提取设备标识（取后 12 位 ASCII/HEX）
     */
    public static String extractDeviceAddr(byte[] srcAddr) {
        if (srcAddr == null || srcAddr.length < 6) return "unknown";
        StringBuilder sb = new StringBuilder(12);
        // 取后 6 字节（实际地址字段就是 6B）
        for (byte b : srcAddr) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * GBT26875 帧解析结果
     */
    public static class Result {
        public int serialNumber;
        public int majorVersion;
        public int minorVersion;
        public byte[] timestamp;
        public byte[] sourceAddr;
        public byte[] destAddr;
        public int command;
        public byte[] appData;
        public int dataLength;

        /** 获取设备地址字符串 */
        public String getDeviceAddr() {
            return extractDeviceAddr(sourceAddr);
        }

        @Override
        public String toString() {
            return String.format("GbtFrame{sn=%d, ver=%d.%02d, cmd=0x%02X, "
                            + "src=%s, dataLen=%d, aduLen=%d}",
                    serialNumber, majorVersion, minorVersion, command,
                    getDeviceAddr(), dataLength,
                    appData != null ? appData.length : 0);
        }
    }
}
