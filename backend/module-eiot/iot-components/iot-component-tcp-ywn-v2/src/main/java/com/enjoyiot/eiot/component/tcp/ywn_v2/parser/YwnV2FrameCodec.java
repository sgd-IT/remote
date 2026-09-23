package com.enjoyiot.eiot.component.tcp.ywn_v2.parser;

import com.enjoyiot.eiot.component.tcp.ywn_v2.protobuf.YwnV2Protocol;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * YWN V2 协议 — 帧编解码器（编码+解码+拆包）
 *
 * 帧格式: 数据长度(2B BE) + 消息类型(2B BE) + 序列号(4B BE) + Protobuf载荷
 * 数据长度 = Protobuf 载荷长度(协议文档语义: 数据包总长 - 协议头8B)，空载荷(如心跳)为0
 *
 * 拆包策略: 基于长度前缀
 *   Phase 0 — 读取2字节数据长度(载荷长度)
 *   Phase 1 — 读取 6+dataLength 字节(消息类型2B+序列号4B+载荷)，组装完整帧
 */
@Slf4j
public class YwnV2FrameCodec {

    /** 序列号生成器（递增） */
    private static final AtomicInteger SEQUENCE_GENERATOR = new AtomicInteger(1);

    /** 生成下一个序列号 */
    public static int nextSequenceNo() {
        return SEQUENCE_GENERATOR.incrementAndGet() & 0x7FFFFFFF;
    }

    // ==================== 编码 ====================

    /**
     * 将 YwnV2Frame 编码为 Vertx Buffer
     */
    public static Buffer encode(YwnV2Frame frame) {
        return Buffer.buffer(frame.toBytes());
    }

    /**
     * 构建确认帧
     */
    public static Buffer encodeAck(MessageType msgType, int sequenceNo, boolean ok, String msg) {
        YwnV2Protocol.Message ack = YwnV2Protocol.Message.newBuilder()
                .setOk(ok)
                .setMsg(msg != null ? msg : "")
                .build();
        YwnV2Frame frame = YwnV2Frame.builder()
                .messageType(msgType)
                .sequenceNo(sequenceNo)
                .protobufData(ack.toByteArray())
                .build();
        return encode(frame);
    }

    /**
     * 构建控制下发帧
     */
    public static Buffer encodeControl(int controlType, int host, int loop, int once,
                                        int devtype, String twice, boolean zongxian) {
        YwnV2Protocol.ControlReq req = YwnV2Protocol.ControlReq.newBuilder()
                .setCtrl(controlType)
                .setHost(host)
                .setLoop(loop)
                .setOnce(once)
                .setDevtype(devtype)
                .setTwice(twice != null ? twice : "")
                .setZongxian(zongxian)
                .build();
        YwnV2Frame frame = YwnV2Frame.builder()
                .messageType(MessageType.CONTROL)
                .sequenceNo(nextSequenceNo())
                .protobufData(req.toByteArray())
                .build();
        return encode(frame);
    }

    // ==================== 解码 ====================

    /**
     * 从完整帧 Buffer 解码为 YwnV2Frame
     *
     * @param buffer 完整帧数据（由拆包器组装，包含数据长度+消息类型+序列号+载荷）
     * @return 解码后的帧，数据不足或异常返回 null
     */
    public static YwnV2Frame decode(Buffer buffer) {
        if (buffer == null || buffer.length() < 8) {
            log.warn("[YWN-V2] 帧数据不足: {} 字节", buffer != null ? buffer.length() : 0);
            return null;
        }

        try {
            // 大端序解析
            ByteBuffer bb = ByteBuffer.wrap(buffer.getBytes()).order(ByteOrder.BIG_ENDIAN);

            int dataLength = bb.getShort() & 0xFFFF;
            int msgTypeCode = bb.getShort() & 0xFFFF;
            int sequenceNo = bb.getInt();

            // 校验数据长度字段与实际载荷长度一致性(宽容处理, 仅告警)
            int actualPayloadLen = buffer.length() - YwnV2Frame.HEADER_LENGTH;
            if (dataLength != actualPayloadLen) {
                log.warn("[YWN-V2] 数据长度字段({})与实际载荷长度({})不一致", dataLength, actualPayloadLen);
            }

            MessageType msgType = MessageType.fromCode(msgTypeCode);
            if (msgType == null) {
                log.warn("[YWN-V2] 未知消息类型: {}", msgTypeCode);
            }

            // 剩余字节为 Protobuf 载荷
            byte[] protobufData = new byte[buffer.length() - 8];
            bb.get(protobufData);

            return YwnV2Frame.builder()
                    .messageType(msgType)
                    .sequenceNo(sequenceNo)
                    .protobufData(protobufData.length > 0 ? protobufData : new byte[0])
                    .build();
        } catch (Exception e) {
            log.error("[YWN-V2] 帧解码异常", e);
            return null;
        }
    }

    // ==================== 拆包器 ====================

    /**
     * 创建基于长度前缀的帧拆包器
     *
     * 两阶段:
     *   Phase 0 — 读取2字节数据长度(载荷长度, 可为0表示空载荷)
     *   Phase 1 — 读取 6+dataLength 字节(消息类型2B+序列号4B+载荷)，组装完整帧
     */
    public static RecordParser createParser(Consumer<YwnV2Frame> frameHandler) {
        RecordParser parser = RecordParser.newFixed(2); // 先读2字节数据长度
        parser.setOutput(new Handler<Buffer>() {
            int phase = 0;
            int dataLength = 0;

            @Override
            public void handle(Buffer buffer) {
                switch (phase) {
                    case 0: {
                        // 读取2字节数据长度 (大端) — Protobuf 载荷长度, 空载荷(如心跳)为0
                        dataLength = ((buffer.getByte(0) & 0xFF) << 8) | (buffer.getByte(1) & 0xFF);
                        if (dataLength > 65535) {
                            log.warn("[YWN-V2] 数据长度超限: {}", dataLength);
                            break;
                        }
                        phase = 1;
                        // 还需读取 消息类型(2B) + 序列号(4B) + 载荷(dataLength)
                        parser.fixedSizeMode(6 + dataLength);
                        break;
                    }
                    case 1: {
                        // 组装完整帧: 数据长度(2B) + 消息类型(2B) + 序列号(4B) + 载荷(dataLength)
                        Buffer fullFrame = Buffer.buffer(8 + dataLength);
                        fullFrame.appendShort((short) dataLength);
                        fullFrame.appendBuffer(buffer);

                        try {
                            YwnV2Frame frame = decode(fullFrame);
                            if (frame != null) {
                                frameHandler.accept(frame);
                            }
                        } catch (Exception e) {
                            log.error("[YWN-V2] 处理帧异常", e);
                        }
                        resetState();
                        break;
                    }
                    default:
                        resetState();
                        break;
                }
            }

            private void resetState() {
                phase = 0;
                dataLength = 0;
                parser.fixedSizeMode(2);
            }
        });
        return parser;
    }
}