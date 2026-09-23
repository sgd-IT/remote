package com.enjoyiot.eiot.component.tcp.ywn.parser;

import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

/**
 * YWN控制器监控TCP协议V27 — 帧解码器
 *
 * 将完整帧 Buffer 解码为 DataYwnPackage:
 * - byte[0]:    命令码 (1B)
 * - byte[1-2]:  设备编号 (2B, LE)
 * - byte[3..]:  载荷
 */
@Slf4j
public class DataYwnDecoder {

    /**
     * 解码完整帧数据
     *
     * @param buffer 完整帧数据(由 DataYwnReader 拆包)
     * @return 解码后的 DataYwnPackage，数据不足返回 null
     */
    public static DataYwnPackage decode(Buffer buffer) {
        if (buffer == null || buffer.length() < 3) {
            log.warn("[YWN] 帧数据不足: {} 字节", buffer != null ? buffer.length() : 0);
            return null;
        }

        int cmdCode = buffer.getByte(0) & 0xFF;
        int deviceNumber = buffer.getShortLE(1) & 0xFFFF;

        byte[] payload;
        if (buffer.length() > 3) {
            payload = buffer.getBytes(3, buffer.length());
        } else {
            payload = new byte[0];
        }

        return DataYwnPackage.builder()
                .command(cmdCode)
                .deviceNumber(deviceNumber)
                .payload(payload)
                .build();
    }
}
