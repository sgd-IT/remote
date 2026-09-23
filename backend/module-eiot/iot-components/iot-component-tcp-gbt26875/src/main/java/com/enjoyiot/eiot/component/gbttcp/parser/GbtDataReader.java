package com.enjoyiot.eiot.component.gbttcp.parser;

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
                            // 帧头确认，读取控制单元剩余部分
                            phase = 2;
                            parser.fixedSizeMode(24);
                        } else {
                            // 不是 @@，回退扫描
                            phase = 0;
                        }
                        break;

                    case 2: // 收到 24 字节: serial(2)+version(2)+timestamp(6)+src(6)+dst(6)+length(2)
                        headerBuf = buffer.copy();
                        // 数据长度在本段末尾2字节（offset 22-23, 低字节在前）
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
                dataLength = 0;
                parser.fixedSizeMode(1);
            }
        });
        return parser;
    }
}
