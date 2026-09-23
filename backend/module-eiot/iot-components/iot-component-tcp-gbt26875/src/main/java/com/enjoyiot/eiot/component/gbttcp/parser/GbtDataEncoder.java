package com.enjoyiot.eiot.component.gbttcp.parser;

import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * GB/T 26875.3-2011 数据编码器
 * 将 GbtDataPackage 编码为标准协议二进制帧
 */
@Slf4j
public class GbtDataEncoder {

    /**
     * 编码数据包
     */
    public static Buffer encode(GbtDataPackage data) {
        byte[] appData = data.getApplicationData() != null ? data.getApplicationData() : new byte[0];
        int dataLength = appData.length;

        Buffer buffer = Buffer.buffer(GbtConstants.MIN_PACKET_SIZE + dataLength);

        // 1. 启动符 @@
        buffer.appendByte(GbtConstants.START_BYTE);
        buffer.appendByte(GbtConstants.START_BYTE);

        // 2. 业务流水号（2字节，低字节在前）
        buffer.appendShortLE((short) data.getSerialNumber());

        // 3. 协议版本号（主版本1字节 + 用户版本1字节）
        buffer.appendByte(data.getMajorVersion());
        buffer.appendByte(data.getUserVersion());

        // 4. 时间标签（6字节：秒、分、时、日、月、年）
        byte[] ts = data.getTimestamp() != null ? data.getTimestamp() : buildCurrentTimestamp();
        buffer.appendBytes(ts);

        // 5. 源地址（6字节，低字节在前）
        byte[] src = data.getSourceAddr() != null ? data.getSourceAddr() : new byte[GbtConstants.ADDRESS_LENGTH];
        buffer.appendBytes(src);

        // 6. 目的地址（6字节，低字节在前）
        byte[] dst = data.getDestAddr() != null ? data.getDestAddr() : new byte[GbtConstants.ADDRESS_LENGTH];
        buffer.appendBytes(dst);

        // 7. 应用数据单元长度（2字节，低字节在前）
        buffer.appendShortLE((short) dataLength);

        // 8. 命令字节（1字节）
        buffer.appendByte(data.getCommand());

        // 9. 应用数据单元
        if (dataLength > 0) {
            buffer.appendBytes(appData);
        }

        // 10. 校验和：第3~27字节及应用数据单元的算术和，取低8位
        //     即 buffer[2] ~ buffer[current_length-1]
        int sum = 0;
        for (int i = 2; i < buffer.length(); i++) {
            sum += (buffer.getByte(i) & 0xFF);
        }
        buffer.appendByte((byte) (sum & 0xFF));

        // 11. 结束符 ##
        buffer.appendByte(GbtConstants.END_BYTE);
        buffer.appendByte(GbtConstants.END_BYTE);

        return buffer;
    }

    /**
     * 构建当前时间的时间标签
     * 格式：秒(1B) | 分(1B) | 时(1B) | 日(1B) | 月(1B) | 年(1B, year-2000)
     */
    public static byte[] buildCurrentTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        return new byte[]{
                (byte) now.getSecond(),
                (byte) now.getMinute(),
                (byte) now.getHour(),
                (byte) now.getDayOfMonth(),
                (byte) now.getMonthValue(),
                (byte) (now.getYear() - 2000)
        };
    }
}
