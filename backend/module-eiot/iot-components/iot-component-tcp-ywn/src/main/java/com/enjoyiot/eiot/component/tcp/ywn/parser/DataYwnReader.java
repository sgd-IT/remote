package com.enjoyiot.eiot.component.tcp.ywn.parser;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * YWN控制器监控TCP协议V27 — 帧拆包器
 *
 * 协议无长度前缀，帧长度由命令码决定:
 * - 命令码 1B → 查表获取整帧长度 → 读取剩余字节
 * - 未识别的命令码 → 跳过1字节继续寻找有效帧头
 *
 * 两阶段状态机:
 *   Phase 0 — 读1字节命令码，查表确定帧长
 *   Phase 1 — 读取 frameLength-1 字节剩余数据，组装完整帧
 */
@Slf4j
public class DataYwnReader {

    public static RecordParser getParser(Consumer<Buffer> receiveHandler) {
        RecordParser parser = RecordParser.newFixed(1);
        parser.setOutput(new Handler<Buffer>() {
            int phase = 0;
            int frameLength = 0;
            byte cmdByte = 0;

            @Override
            public void handle(Buffer buffer) {
                switch (phase) {
                    case 0: // 读取1字节命令码
                        cmdByte = buffer.getByte(0);
                        int cmdCode = cmdByte & 0xFF;
                        frameLength = YwnCommand.getFrameLengthByCode(cmdCode);

                        if (frameLength < 0) {
                            // 未识别命令码，跳过此字节继续扫描
                            log.warn("[YWN] 未识别的命令码: 0x{}, 跳过", String.format("%02X", cmdCode));
                            // 保持 phase=0, fixedSizeMode=1
                            break;
                        }

                        if (frameLength == 1) {
                            // 只有命令码本身(不应出现，最小帧3字节)
                            log.warn("[YWN] 异常帧长度: cmd=0x{}, len={}", String.format("%02X", cmdCode), frameLength);
                            break;
                        }

                        // 读取剩余字节
                        phase = 1;
                        parser.fixedSizeMode(frameLength - 1);
                        break;

                    case 1: // 收到剩余数据，组装完整帧
                        Buffer fullFrame = Buffer.buffer(frameLength);
                        fullFrame.appendByte(cmdByte);
                        fullFrame.appendBuffer(buffer);

                        try {
                            receiveHandler.accept(fullFrame);
                        } catch (Exception e) {
                            log.error("[YWN] 处理数据包异常", e);
                        }

                        // 复位状态机
                        resetState();
                        break;

                    default:
                        resetState();
                        break;
                }
            }

            private void resetState() {
                phase = 0;
                frameLength = 0;
                cmdByte = 0;
                parser.fixedSizeMode(1);
            }
        });
        return parser;
    }
}
