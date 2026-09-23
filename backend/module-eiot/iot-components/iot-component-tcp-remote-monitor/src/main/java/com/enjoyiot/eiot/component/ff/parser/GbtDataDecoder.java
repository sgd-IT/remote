package com.enjoyiot.eiot.component.ff.parser;

import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

/**
 * GB/T 26875.3-2011 数据解码器
 * 将标准协议二进制帧解码为 GbtDataPackage
 */
@Slf4j
public class GbtDataDecoder {

    /**
     * 解码完整数据包（从 @@ 到 ##）
     *
     * @param buffer 完整帧数据
     * @return 解析成功返回 GbtDataPackage，校验失败返回 null
     */
    public static GbtDataPackage decode(Buffer buffer) {
        // 心跳包特殊处理：12字节简化帧（@@ + srcAddr(6) + commModule(1) + sysType(1) + ##）
        if (buffer.length() == GbtConstants.HEARTBEAT_PACKET_SIZE
                && buffer.getByte(0) == GbtConstants.START_BYTE
                && buffer.getByte(1) == GbtConstants.START_BYTE
                && buffer.getByte(10) == GbtConstants.END_BYTE
                && buffer.getByte(11) == GbtConstants.END_BYTE) {
            return GbtDataPackage.parseHeartbeat(buffer);
        }

        if (buffer.length() < GbtConstants.MIN_PACKET_SIZE) {
            log.warn("数据包长度不足: {} < {}", buffer.length(), GbtConstants.MIN_PACKET_SIZE);
            return null;
        }

        // 验证启动符 @@
        if (buffer.getByte(0) != GbtConstants.START_BYTE
                || buffer.getByte(1) != GbtConstants.START_BYTE) {
            log.warn("无效的启动符");
            return null;
        }

        // 验证结束符 ##
        int len = buffer.length();
        if (buffer.getByte(len - 2) != GbtConstants.END_BYTE
                || buffer.getByte(len - 1) != GbtConstants.END_BYTE) {
            log.warn("无效的结束符");
            return null;
        }

        // 业务流水号（offset 2-3, 低字节在前）
        int serialNumber = buffer.getShortLE(2) & 0xFFFF;

        // 协议版本号（offset 4=主版本, 5=用户版本）
        byte majorVersion = buffer.getByte(4);
        byte userVersion = buffer.getByte(5);

        // 时间标签（offset 6-11, 共6字节）
        byte[] timestamp = buffer.getBytes(6, 12);

        // 源地址（offset 12-17, 共6字节, 线上低字节在前；设备编号为字节逆序，见 GbtDataPackage.reverseHexAddr）
        byte[] sourceAddr = buffer.getBytes(12, 18);

        // 目的地址（offset 18-23, 共6字节, 线上低字节在前；设备编号为字节逆序，见 GbtDataPackage.reverseHexAddr）
        byte[] destAddr = buffer.getBytes(18, 24);

        // 应用数据单元长度（offset 24-25, 低字节在前）
        int dataLength = buffer.getShortLE(24) & 0xFFFF;

        // 命令字节（offset 26）
        byte command = buffer.getByte(26);

        // 长度一致性校验
        int expectedLength = GbtConstants.MIN_PACKET_SIZE + dataLength;
        if (buffer.length() != expectedLength) {
            log.warn("数据包长度不匹配: expected={}, actual={}", expectedLength, buffer.length());
            return null;
        }

        // 应用数据单元（offset 27 ~ 27+dataLength-1）
        byte[] applicationData = dataLength > 0 ? buffer.getBytes(27, 27 + dataLength) : new byte[0];

        // 校验和验证（offset 27+dataLength）
        byte receivedChecksum = buffer.getByte(27 + dataLength);
        int sum = 0;
        for (int i = 2; i < 27 + dataLength; i++) {
            sum += (buffer.getByte(i) & 0xFF);
        }
        byte expectedChecksum = (byte) (sum & 0xFF);
        if (receivedChecksum != expectedChecksum) {
            log.warn("校验和错误: expected=0x{}, received=0x{}",
                    String.format("%02X", expectedChecksum & 0xFF),
                    String.format("%02X", receivedChecksum & 0xFF));
            return null;
        }

        return GbtDataPackage.builder()
                .serialNumber(serialNumber)
                .majorVersion(majorVersion)
                .userVersion(userVersion)
                .timestamp(timestamp)
                .sourceAddr(sourceAddr)
                .destAddr(destAddr)
                .command(command)
                .applicationData(applicationData)
                .build();
    }
}
