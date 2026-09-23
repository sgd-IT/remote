package com.enjoyiot.eiot.component.udp.dispatcher;

import com.enjoyiot.eiot.component.udp.service.UdpComponent;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


@Slf4j
public class UdpPacketDispatcher {

    // 构造固定顺序的 Handler 列表
    private static final List<PacketHandler> HANDLERS = Arrays.asList(
            new JsonHandler(),
            new DahuaHandler(),
            new UwbHandler(),
            new HexHandler(),
            new AsciiHandler(),
            new BinaryHandler()
    );

    public static void dispatch(final Vertx vertx, final UdpComponent component,
                                final byte[] raw, final String senderAddr) {
        for (final PacketHandler h : HANDLERS) {
            if (h.matches(raw)) {
                log.info("[UDP协议] 路由到 {}: len={}", h.getClass().getSimpleName(), raw.length);
                vertx.executeBlocking(fut -> {
                    h.handle(vertx, component, raw, senderAddr);
                    fut.complete();
                }, false, null);
                return;
            }
        }
        log.info("[UDP协议] 无匹配Handler: len={}", raw.length);
    }

    /** 策略接口：匹配 & 处理 */
    public interface PacketHandler {
        boolean matches(byte[] raw);
        void handle(Vertx vertx, UdpComponent comp, byte[] raw, String senderAddr);
    }

    /** JSON 格式检测 & 处理 */
    private static class JsonHandler implements PacketHandler {
        @Override
        public boolean matches(byte[] raw) {
            int len = raw.length;
            return len >= 2
                    && ((raw[0] == '{' && raw[len - 1] == '}')
                    || (raw[0] == '[' && raw[len - 1] == ']'));
        }
        @Override
        public void handle(Vertx vertx, UdpComponent comp, byte[] raw, String senderAddr) {
            comp.handleJson(new String(raw, StandardCharsets.UTF_8));
        }
    }

    /** UWB 前缀检测 & 处理 */
    private static class UwbHandler implements PacketHandler {
        private static final byte[][] PREFIXES = Arrays.stream(
                        com.enjoyiot.eiot.component.udp.enums.TagsForwardedTypeEnum.values()
                ).map(e -> e.getPrefix().getBytes(StandardCharsets.UTF_8))
                .toArray(byte[][]::new);

        @Override
        public boolean matches(byte[] raw) {
            for (byte[] p : PREFIXES) {
                if (raw.length < p.length) continue;
                boolean ok = true;
                for (int i = 0; i < p.length; i++) {
                    if (raw[i] != p[i]) {
                        ok = false;
                        break;
                    }
                }
                if (ok) return true;
            }
            return false;
        }
        @Override
        public void handle(Vertx vertx, UdpComponent comp, byte[] raw, String senderAddr) {
            comp.handleUwb(new String(raw, StandardCharsets.UTF_8));
        }
    }

    /** HEX 文本检测 & 处理 */
    private static class HexHandler implements PacketHandler {
        @Override
        public boolean matches(byte[] raw) {
            if ((raw.length & 1) != 0) return false;
            for (byte b : raw) {
                int v = b & 0xFF;
                if (! ( (v >= '0' && v <= '9')
                        || (v >= 'A' && v <= 'F')
                        || (v >= 'a' && v <= 'f') ) ) {
                    return false;
                }
            }
            return true;
        }
        @Override
        public void handle(Vertx vertx, UdpComponent comp, byte[] raw, String senderAddr) {
            comp.handleHex(new String(raw, StandardCharsets.UTF_8));
        }
    }

    /** ASCII 文本检测 & 处理 */
    private static class AsciiHandler implements PacketHandler {
        @Override
        public boolean matches(byte[] raw) {
            for (byte b : raw) {
                int v = b & 0xFF;
                if (v == 0x0A || v == 0x0D) continue;
                if (v < 32 || v > 126) {
                    return false;
                }
            }
            return true;
        }
        @Override
        public void handle(Vertx vertx, UdpComponent comp, byte[] raw, String senderAddr) {
            comp.handleAscii(new String(raw, StandardCharsets.UTF_8));
        }
    }

    /** 二进制兜底处理 */
    private static class BinaryHandler implements PacketHandler {
        @Override
        public boolean matches(byte[] raw) {
            return true;
        }
        @Override
        public void handle(Vertx vertx, UdpComponent comp, byte[] raw, String senderAddr) {
            comp.handleBinary(raw);
        }
    }

    // ==================== 大华消防协议 Handler ====================

    /**
     * 大华消防协议 v1.04 @@...## 二进制帧检测与解析
     */
    @Slf4j
    private static class DahuaHandler implements PacketHandler {

        private static final byte FRAME_START = 0x40; // '@'
        private static final int HEADER_LEN = 27;      // @@(2) + header(25)
        private static final int MIN_FRAME_LEN = 30;   // header(27) + checksum(1) + ##(2)

        @Override
        public boolean matches(byte[] raw) {
            return raw.length >= MIN_FRAME_LEN
                    && raw[0] == FRAME_START && raw[1] == FRAME_START;
        }

        @Override
        public void handle(Vertx vertx, UdpComponent comp, byte[] raw,String senderAddr) {
            try {
                ByteBuffer buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);

                // 1. 跳过 @@
                buf.getShort(); // 2B

                // 2. 流水号 + 版本
                int serialNum = buf.getShort() & 0xFFFF;
                int version   = buf.getShort() & 0xFFFF;

                // 3. 时间标签(6B)
                byte[] timeLabel = new byte[6];
                buf.get(timeLabel);

                // 4. 源地址(6B) — 用于设备标识
                byte[] srcAddrBytes = new byte[6];
                buf.get(srcAddrBytes);
                String srcAddr = bytesToHex(srcAddrBytes);

                // 5. 目的地址(6B) — 用于确认帧回显
                byte[] dstAddrBytes = new byte[6];
                buf.get(dstAddrBytes);

                // 6. ADU长度(2B, LE) + 命令字(1B)
                int aduLen = buf.getShort() & 0xFFFF;
                int cmd    = buf.get() & 0xFF;

                // 7. 长度校验
                int expectedLen = HEADER_LEN + aduLen + 1 + 2; // header + adu + checksum + ##
                if (raw.length < expectedLen) {
                    log.warn("[UDP协议] 帧长度不足: actual={}, expected={}", raw.length, expectedLen);
                    return;
                }

                // 8. ADU(NB)
                byte[] adu = new byte[aduLen];
                buf.get(adu);

                // 9. 校验和
                byte receivedChecksum = buf.get();
                byte calculated = calcChecksum(raw, 2, HEADER_LEN + aduLen);
                if (receivedChecksum != calculated) {
                    log.warn("[UDP协议] 校验和不匹配: received=0x{}, calculated=0x{}",
                            Integer.toHexString(receivedChecksum & 0xFF),
                            Integer.toHexString(calculated & 0xFF));
                    return;
                }

                // 10. 提取 typeFlag（ADU 首字节，若 ADU 非空）
                int typeFlag = (aduLen > 0) ? (adu[0] & 0xFF) : 0;

                log.info("[UDP协议] 帧解析成功: src={}, cmd=0x{}, typeFlag=0x{}, aduLen={}, serial={}",
                        srcAddr, Integer.toHexString(cmd), Integer.toHexString(typeFlag), aduLen, serialNum);

                // 11. 交给组件进行协议级路由（传入流水号、版本号、目的地址字节、发送者地址）
                comp.handleDahua(srcAddr, cmd, typeFlag, adu, serialNum, version, dstAddrBytes, senderAddr);

            } catch (Exception e) {
                log.error("[UDP协议] 帧解析异常", e);
            }
        }

        /**
         * 计算校验和：从 startOffset 到 endOffset（不含）所有字节累加取低8位
         */
        private static byte calcChecksum(byte[] data, int start, int end) {
            int sum = 0;
            for (int i = start; i < end && i < data.length; i++) {
                sum += (data[i] & 0xFF);
            }
            return (byte) (sum & 0xFF);
        }

        /**
         * 字节数组转大写 Hex 字符串
         */
        private static String bytesToHex(byte[] data) {
            StringBuilder sb = new StringBuilder(data.length * 2);
            for (byte b : data) {
                sb.append(String.format("%02X", b & 0xFF));
            }
            return sb.toString();
        }
    }

}
