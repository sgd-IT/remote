package com.enjoyiot.eiot.component.dahua.common.parser;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * GB/T 26875.3-2011 传输帧编码器（TCP/UDP 共用）
 */
@Slf4j
public final class GbtFrameBuilder {

    private GbtFrameBuilder() {}

    /**
     * 编码完整 GBT26875 帧
     */
    public static byte[] buildFrame(int serialNumber, int version, byte[] timestamp,
                                    byte[] sourceAddr, byte[] destAddr,
                                    int command, byte[] adu) {
        byte[] ts = (timestamp != null) ? timestamp : DahuaAduBuilder.buildCurrentTimestamp();
        byte[] src = (sourceAddr != null) ? sourceAddr : new byte[6];
        byte[] dst = (destAddr != null) ? destAddr : new byte[6];
        byte[] appData = (adu != null) ? adu : new byte[0];

        // @@(2) + serial(2) + ver(2) + ts(6) + src(6) + dst(6) + aduLen(2) + cmd(1) + adu(N) + checksum(1) + ##(2)
        int totalLen = 2 + 2 + 2 + 6 + 6 + 6 + 2 + 1 + appData.length + 1 + 2;
        ByteBuffer buf = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN);

        // 1. 启动符 @@
        buf.put((byte) 0x40);
        buf.put((byte) 0x40);

        // 2. 业务流水号（2B, LE）
        buf.putShort((short) serialNumber);

        // 3. 协议版本号（2B, LE）
        buf.putShort((short) version);

        // 4. 时间标签（6B）
        buf.put(ts, 0, 6);

        // 5. 源地址（6B）
        buf.put(src, 0, 6);

        // 6. 目的地址（6B）
        buf.put(dst, 0, 6);

        // 7. ADU 长度（2B, LE）
        buf.putShort((short) appData.length);

        // 8. 命令字（1B）
        buf.put((byte) command);

        // 9. ADU
        if (appData.length > 0) {
            buf.put(appData);
        }

        // 10. 校验和：从流水号（offset=2）到 ADU 末尾所有字节累加取低8位
        byte[] data = buf.array();
        int sum = 0;
        for (int i = 2; i < buf.position(); i++) {
            sum += (data[i] & 0xFF);
        }
        buf.put((byte) (sum & 0xFF));

        // 11. 结束符 ##
        buf.put((byte) 0x23);
        buf.put((byte) 0x23);

        byte[] result = new byte[buf.position()];
        buf.flip();
        buf.get(result);
        return result;
    }

    /**
     * 构建确认帧（CMD=0x03，空 ADU）
     * <p>
     * 用于平台收到注册帧/设备上报帧后向终端发送确认。
     *
     * @param serialNumber 原帧流水号（回显）
     * @param version      协议版本号
     * @param sourceAddr   平台源地址（6B）
     * @param destAddr     设备目的地址（6B，通常为原帧的源地址）
     * @return 完整确认帧字节数组
     */
    public static byte[] buildConfirmFrame(int serialNumber, int version,
                                           byte[] sourceAddr, byte[] destAddr) {
        return buildFrame(serialNumber, version, null, sourceAddr, destAddr,
                0x03, new byte[0]);
    }

    /**
     * 构建否认帧（CMD=0x06，空 ADU）
     */
    public static byte[] buildDenyFrame(int serialNumber, int version,
                                        byte[] sourceAddr, byte[] destAddr) {
        return buildFrame(serialNumber, version, null, sourceAddr, destAddr,
                0x06, new byte[0]);
    }

    /**
     * 十六进制字符串 → 6字节地址数组（不足12位前补0）
     *
     * @param hex 设备地址 hex 字符串（如 "FF6140019917"）
     * @return 6字节地址数组
     */
    public static byte[] hexToAddr(String hex) {
        byte[] addr = new byte[6];
        if (hex == null || hex.isEmpty()) {
            return addr;
        }
        hex = hex.replaceAll("\\s+", "").toUpperCase();
        while (hex.length() < 12) {
            hex = "0" + hex;
        }
        for (int i = 0; i < 6; i++) {
            addr[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return addr;
    }
}
