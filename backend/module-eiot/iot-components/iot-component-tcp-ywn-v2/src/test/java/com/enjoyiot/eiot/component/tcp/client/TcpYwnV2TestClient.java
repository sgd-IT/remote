package com.enjoyiot.eiot.component.tcp.client;

import com.enjoyiot.eiot.component.tcp.ywn_v2.parser.MessageType;
import com.enjoyiot.eiot.component.tcp.ywn_v2.parser.YwnV2EventType;
import com.enjoyiot.eiot.component.tcp.ywn_v2.parser.YwnV2Frame;
import com.enjoyiot.eiot.component.tcp.ywn_v2.parser.YwnV2FrameCodec;
import com.enjoyiot.eiot.component.tcp.ywn_v2.protobuf.YwnV2Protocol;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * YWN V2 Protobuf 协议 — 模拟客户端测试
 *
 * 测试流程:
 *   1. 建立 TCP 连接
 *   2. 发送注册请求 (RegisterReq)
 *   3. 等待注册确认
 *   4. 循环: 发送心跳 → 等待确认 → 发送事件 → 等待确认
 *   5. 可接收服务端下发的控制指令
 *
 * 用法:
 *   java TcpYwnV2TestClient [host] [port] [gateway] [intervalSec] [mode]
 *   默认: host=127.0.0.1, port=6666, gateway=GATEWAY001, intervalSec=10, mode=both
 */
public class TcpYwnV2TestClient {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);

    private static int nextSeq() {
        return SEQUENCE.incrementAndGet() & 0x7FFFFFFF;
    }

    /** 发送帧 */
    private static void sendFrame(OutputStream out, YwnV2Frame frame) throws Exception {
        byte[] data = frame.toBytes();
        System.out.printf("[TX] %s (len=%d, hex=%s)%n", frame, data.length, bytesToHex(data, 64));
        out.write(data);
        out.flush();
    }

    /** 接收帧 */
    private static YwnV2Frame recvFrame(InputStream in) throws Exception {
        // 读取2字节数据长度 (大端) — Protobuf 载荷长度, 空载荷(如心跳)为0
        byte[] lenBytes = readFully(in, 2);
        int dataLen = ((lenBytes[0] & 0xFF) << 8) | (lenBytes[1] & 0xFF);
        if (dataLen > 65535) {
            throw new IllegalStateException("无效数据长度: " + dataLen);
        }

        // 读取剩余数据: 消息类型(2B) + 序列号(4B) + 载荷(dataLen)
        byte[] remaining = readFully(in, 6 + dataLen);

        // 组装完整帧: 数据长度(2B) + 消息类型(2B) + 序列号(4B) + 载荷
        ByteBuffer bb = ByteBuffer.allocate(8 + dataLen).order(ByteOrder.BIG_ENDIAN);
        bb.putShort((short) dataLen);
        bb.put(remaining);
        bb.flip();

        byte[] frameBytes = new byte[bb.remaining()];
        bb.get(frameBytes);

        YwnV2Frame frame = YwnV2FrameCodec.decode(io.vertx.core.buffer.Buffer.buffer(frameBytes));
        System.out.printf("[RX] %s (hex=%s)%n", frame, bytesToHex(frameBytes, 64));
        return frame;
    }

    /** 读取指定字节数 */
    private static byte[] readFully(InputStream in, int len) throws Exception {
        byte[] data = new byte[len];
        int off = 0;
        while (off < len) {
            int r = in.read(data, off, len - off);
            if (r < 0) throw new IllegalStateException("连接已关闭");
            off += r;
        }
        return data;
    }

    /** 读取确认帧并验证 */
    private static boolean expectAck(InputStream in, MessageType expectedType, int expectedSeq) throws Exception {
        YwnV2Frame frame = recvFrame(in);
        if (frame.getMessageType() != expectedType) {
            System.out.printf("[WARN] 期望消息类型 %s，实际收到 %s%n", expectedType, frame.getMessageType());
            return false;
        }
        YwnV2Protocol.Message ack = frame.parseAck();
        if (ack != null) {
            System.out.printf("[ACK] type=%s, seq=%d, ok=%s, msg=%s%n",
                    expectedType, frame.getSequenceNo(), ack.getOk(), ack.getMsg());
            return ack.getOk();
        }
        return true;
    }

    private static String bytesToHex(byte[] bytes, int maxLen) {
        StringBuilder sb = new StringBuilder();
        int len = Math.min(bytes.length, maxLen);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        if (bytes.length > maxLen) sb.append("...");
        return sb.toString().trim();
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8840;
        String gateway = args.length > 2 ? args[2] : "GATEWAY001";
        int intervalSec = args.length > 3 ? Integer.parseInt(args[3]) : 10;
        String mode = args.length > 4 ? args[4] : "both";

        System.out.println("=== YWN V2 Protobuf 模拟客户端 ===");
        System.out.printf("服务端: %s:%d%n", host, port);
        System.out.printf("网关编号: %s%n", gateway);
        System.out.printf("发送间隔: %d秒%n", intervalSec);
        System.out.printf("模式: %s%n", mode);

        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // ========== 1. 注册 ==========
            System.out.println("\n>>> 步骤1: 发送注册请求");
            YwnV2Protocol.RegisterReq regReq = YwnV2Protocol.RegisterReq.newBuilder()
                    .setGateway(gateway)
                    .build();
            YwnV2Frame regFrame = YwnV2Frame.builder()
                    .messageType(MessageType.REGISTER)
                    .sequenceNo(nextSeq())
                    .protobufData(regReq.toByteArray())
                    .build();
            sendFrame(out, regFrame);

            System.out.println(">>> 等待注册确认...");
            boolean regOk = expectAck(in, MessageType.REGISTER, regFrame.getSequenceNo());
            System.out.printf("注册结果: %s%n", regOk ? "成功" : "失败");
            if (!regOk) {
                System.out.println("注册失败，退出");
                return;
            }

            // ========== 2. 心跳 + 事件循环 ==========
            int loopCount = 0;
            boolean toggle = true;

            while (true) {
                loopCount++;

                if ("heartbeat".equalsIgnoreCase(mode) || ("both".equalsIgnoreCase(mode) && toggle)) {
                    // 发送心跳
                    System.out.printf("%n>>> 步骤2.%d: 发送心跳%n", loopCount);
                    YwnV2Frame hbFrame = YwnV2Frame.builder()
                            .messageType(MessageType.HEARTBEAT)
                            .sequenceNo(nextSeq())
                            .protobufData(new byte[0])
                            .build();
                    sendFrame(out, hbFrame);

                    try {
                        socket.setSoTimeout(3000);
                        expectAck(in, MessageType.HEARTBEAT, hbFrame.getSequenceNo());
                    } catch (Exception e) {
                        System.out.println("[WARN] 心跳确认超时: " + e.getMessage());
                    }
                } else {
                    // 发送事件
                    System.out.printf("%n>>> 步骤2.%d: 发送事件%n", loopCount);
                    int eventType = (loopCount % 15) + 1; // 轮流发送事件类型 1-15
                    YwnV2EventType evtType = YwnV2EventType.fromCode(eventType);

                    YwnV2Protocol.EventReq evtReq = YwnV2Protocol.EventReq.newBuilder()
                            .setEvent(eventType)
                            .setHost(1)
                            .setLoop(loopCount % 4 + 1)
                            .setOnce(loopCount % 10 + 1)
                            .setDevtype(0)
                            .setTwice("")
                            .setZongxian(true)
                            .setDeviceCode(String.format("DEV-%d-%d-%d", 1, loopCount % 4 + 1, loopCount % 10 + 1))
                            .build();
                    YwnV2Frame evtFrame = YwnV2Frame.builder()
                            .messageType(MessageType.EVENT)
                            .sequenceNo(nextSeq())
                            .protobufData(evtReq.toByteArray())
                            .build();
                    sendFrame(out, evtFrame);

                    try {
                        socket.setSoTimeout(3000);
                        expectAck(in, MessageType.EVENT, evtFrame.getSequenceNo());
                    } catch (Exception e) {
                        System.out.println("[WARN] 事件确认超时: " + e.getMessage());
                    }
                    System.out.printf("事件类型: %s%n", evtType != null ? evtType.getDescription() : "未知");
                }

                // 尝试接收服务端下发的控制指令
                try {
                    socket.setSoTimeout(500);
                    YwnV2Frame ctrlFrame = recvFrame(in);
                    if (ctrlFrame.getMessageType() == MessageType.CONTROL) {
                        YwnV2Protocol.ControlReq ctrlReq = ctrlFrame.parseControlReq();
                        if (ctrlReq != null) {
                            System.out.printf("[CTRL] 收到控制指令: ctrl=%d, host=%d, loop=%d, once=%d%n",
                                    ctrlReq.getCtrl(), ctrlReq.getHost(),
                                    ctrlReq.getLoop(), ctrlReq.getOnce());

                            // 回复控制确认
                            YwnV2Protocol.Message ctrlAck = YwnV2Protocol.Message.newBuilder()
                                    .setOk(true)
                                    .setMsg("执行成功")
                                    .build();
                            YwnV2Frame ackFrame = YwnV2Frame.builder()
                                    .messageType(MessageType.CONTROL)
                                    .sequenceNo(ctrlFrame.getSequenceNo())
                                    .protobufData(ctrlAck.toByteArray())
                                    .build();
                            sendFrame(out, ackFrame);
                        }
                    }
                } catch (Exception ignore) {
                    // 无下行数据，正常
                } finally {
                    socket.setSoTimeout(5000);
                }

                toggle = !toggle;
                Thread.sleep(intervalSec * 1000L);
            }
        }
    }
}