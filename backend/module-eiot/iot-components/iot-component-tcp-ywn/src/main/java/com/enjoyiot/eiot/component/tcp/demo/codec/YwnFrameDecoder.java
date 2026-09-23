package com.enjoyiot.eiot.component.tcp.demo.codec;

import com.enjoyiot.eiot.component.tcp.demo.model.YwnCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 控制器监控TCP协议V27 — 帧解码器
 *
 * 协议无长度前缀，帧长度由命令码决定:
 * - 命令码 1B → 查表获取整帧长度 → 读取剩余字节
 * - 未识别的命令码 → 跳过1字节继续寻找有效帧头
 */
public class YwnFrameDecoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(YwnFrameDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() >= 1) {
            // 标记当前读位置
            in.markReaderIndex();

            // 读取命令码
            int cmdCode = in.readByte() & 0xFF;
            YwnCommand command = YwnCommand.fromCode(cmdCode);

            if (command == null) {
                // 未识别命令码，跳过此字节，继续寻找有效帧头
                log.warn("未识别的命令码: 0x{}, 跳过", Integer.toHexString(cmdCode));
                continue;
            }

            int frameLen = command.getFrameLength();

            // 检查剩余可读字节数是否满足整帧
            if (in.readableBytes() < frameLen - 1) {
                // 数据不足，重置读位置等待更多数据
                in.resetReaderIndex();
                return;
            }

            // 读取整帧数据
            byte[] frameData = new byte[frameLen];
            frameData[0] = (byte) cmdCode;

            // 读取命令码之后的剩余字节
            in.readBytes(frameData, 1, frameLen - 1);

            out.add(frameData);
            log.debug("解码帧: cmd={}, len={}, hex={}", command, frameLen, bytesToHex(frameData));
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < Math.min(bytes.length, 32); i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        if (bytes.length > 32) sb.append("...");
        return sb.toString().trim();
    }
}
