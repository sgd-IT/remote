package com.enjoyiot.eiot.component.tcp.demo.codec;

import com.enjoyiot.eiot.component.tcp.demo.model.YwnFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 控制器监控TCP协议V27 — 帧编码器
 *
 * 将 YwnFrame 对象序列化为字节数组写入 ByteBuf
 */
public class YwnFrameEncoder extends MessageToByteEncoder<YwnFrame> {

    private static final Logger log = LoggerFactory.getLogger(YwnFrameEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, YwnFrame frame, ByteBuf out) throws Exception {
        byte[] data = frame.toBytes();
        out.writeBytes(data);
        log.debug("编码帧: cmd={}, dev={}, len={}, hex={}",
                frame.getCommand(), frame.getDeviceNumber(), data.length, bytesToHex(data));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}
