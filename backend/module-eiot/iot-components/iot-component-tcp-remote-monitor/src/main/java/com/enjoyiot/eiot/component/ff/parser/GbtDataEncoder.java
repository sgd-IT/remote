package com.enjoyiot.eiot.component.ff.parser;

import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.Arrays;

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

        // 5. 源地址（6字节，线上低字节在前；设备编号为字节逆序，见 GbtDataPackage.reverseHexAddr）
        byte[] src = data.getSourceAddr() != null ? data.getSourceAddr() : new byte[GbtConstants.ADDRESS_LENGTH];
        buffer.appendBytes(src);

        // 6. 目的地址（6字节，线上低字节在前；设备编号为字节逆序，见 GbtDataPackage.reverseHexAddr）
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

        // 10. 校验和：从业务流水号首字节（offset 2）至应用数据单元末尾所有字节的算术和，取低8位
        //     （不含启动符@@与校验位本身，与 GbtDataDecoder 的校验范围对称）
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

    /**
     * 构建 Type 0x81 (TLD-129) 远程控制应用数据单元
     *
     * 结构：类型标志(1) + 信息体数目(1) + 系统类型标志(1) + 系统地址(1)
     *       + 部件类型(1) + 部件地址(4,LE) + 指令长度(2,LE) + 指令内容(N) + 时间标签(6)
     *
     * @param commandBytes 指令内容字节
     * @param deviceAddr   设备编号（大端格式，6字节，如 FF6150944610），取前8个字符（高4字节）作为部件地址
     */
    public static byte[] buildType81AppData(byte[] commandBytes, String partAddrHex) {
        ByteBuffer buf = ByteBuffer.allocate(1 + 1 + 1 + 1 + 1 + 4 + 2 + commandBytes.length + 6)
            .order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) GbtConstants.TYPE_DOWN_REMOTE_CONTROL); // 类型标志
        buf.put((byte) 0x01); // 信息体数目
        buf.put((byte) 0x88); // 系统类型标志 定义固定 0x88
        buf.put((byte) 0x02); // 系统地址 定义固定 0x02
        buf.put((byte) 0x83); // 部件类型 定义固定 0x83

        int partAddr = Integer.parseUnsignedInt(partAddrHex, 16);
        buf.putInt(partAddr);

        buf.putShort((short) commandBytes.length);
        buf.put(commandBytes);
        buf.put(buildCurrentTimestamp());
        return Arrays.copyOf(buf.array(), buf.position());
    }
}
