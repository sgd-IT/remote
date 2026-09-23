package com.enjoyiot.eiot.component.ff.parser;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * GB/T 26875.3-2011 帧解析器
 *
 * 从 TCP 字节流中提取完整的 @@...## 数据帧。
 * 使用四阶段状态机：
 *   Phase 0 — 逐字节扫描，等待第一个 0x40 (@)
 *   Phase 1 — 验证第二个 0x40 (@)，确认帧头
 *   Phase 2 — 读取固定 24 字节控制头（流水号→数据长度）
 *   Phase 3 — 读取 command(1) + data(L) + checksum(1) + ##(2) = L+4 字节
 */
@Slf4j
public class GbtDataReader {

    public static RecordParser getParser(Consumer<Buffer> receiveHandler) {
        RecordParser parser = RecordParser.newFixed(1);
        parser.setOutput(new Handler<Buffer>() {
            int phase = 0;
            int dataLength = 0;
            Buffer headerBuf = null;
            Buffer prefixBuf = null; // 缓存心跳包嗅探阶段的数据

            @Override
            public void handle(Buffer buffer) {
                switch (phase) {
                    case 0: // 扫描第一个 @
                        if (buffer.getByte(0) == GbtConstants.START_BYTE) {
                            phase = 1;
                        }
                        break;

                    case 1: // 验证第二个 @
                        if (buffer.getByte(0) == GbtConstants.START_BYTE) {
                            // 帧头确认，先读 10 字节判断是否为心跳包
                            phase = 2;
                            parser.fixedSizeMode(GbtConstants.HEARTBEAT_BODY_SIZE);
                        } else {
                            // 不是 @@，回退扫描
                            phase = 0;
                        }
                        break;

                    case 2: // 收到 10 字节：serial(2)+version(2)+timestamp(6) 或 心跳包 body
                        if (buffer.length() >= GbtConstants.HEARTBEAT_BODY_SIZE
                                && buffer.getByte(8) == GbtConstants.END_BYTE
                                && buffer.getByte(9) == GbtConstants.END_BYTE) {
                            // 心跳包：@@(2) + srcAddr(6) + commModule(1) + sysType(1) + ##(2) = 12
                            Buffer hb = Buffer.buffer(GbtConstants.HEARTBEAT_PACKET_SIZE);
                            hb.appendByte(GbtConstants.START_BYTE);
                            hb.appendByte(GbtConstants.START_BYTE);
                            hb.appendBuffer(buffer);
                            receiveHandler.accept(hb);
                            resetState();
                            break;
                        }
                        // 不是心跳包，缓存这 10 字节作为 header 前部
                        prefixBuf = buffer.copy();
                        phase = 21; // 使用 21 代替 2_1（Java 标识符限制）
                        parser.fixedSizeMode(14); // 读取 header 剩余 14 字节
                        break;

                    case 21: // 收到 header 剩余 14 字节：src(6)+dst(6)+length(2)
                        headerBuf = Buffer.buffer(24);
                        headerBuf.appendBuffer(prefixBuf); // 10 字节
                        headerBuf.appendBuffer(buffer);     // 14 字节
                        dataLength = headerBuf.getShortLE(22) & 0xFFFF;
                        if (dataLength > GbtConstants.MAX_DATA_LENGTH) {
                            log.warn("数据长度超限: {}，丢弃并重新同步", dataLength);
                            resetState();
                            break;
                        }
                        phase = 3;
                        // 后续需读取: command(1) + data(dataLength) + checksum(1) + ##(2)
                        parser.fixedSizeMode(dataLength + 4);
                        break;

                    case 3: // 收到命令字节 + 应用数据 + 校验和 + 结束符
                        // 组装完整帧
                        Buffer fullPacket = Buffer.buffer(GbtConstants.MIN_PACKET_SIZE + dataLength);
                        fullPacket.appendByte(GbtConstants.START_BYTE);
                        fullPacket.appendByte(GbtConstants.START_BYTE);
                        fullPacket.appendBuffer(headerBuf);
                        fullPacket.appendBuffer(buffer);

                        // 国标框架心跳（CMD=128/129）：标准帧格式，命令字节在 buffer[0]
                        byte cmdByte = buffer.getByte(0);
                        if (cmdByte == GbtConstants.CMD_HEARTBEAT_ACK
                                || cmdByte == GbtConstants.CMD_HEARTBEAT_NO_ACK) {
                            log.info("检测到国标框架心跳: CMD={}", cmdByte);
                        }

                        try {
                            receiveHandler.accept(fullPacket);
                        } catch (Exception e) {
                            log.error("处理GBT数据包异常", e);
                        }

                        resetState();
                        break;

                    default:
                        resetState();
                        break;
                }
            }

            private void resetState() {
                phase = 0;
                headerBuf = null;
                prefixBuf = null;
                dataLength = 0;
                parser.fixedSizeMode(1);
            }
        });
        return parser;
    }
}
