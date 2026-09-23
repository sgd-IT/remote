package com.enjoyiot.eiot.component.dahua.common.parser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 大华消防协议 v1.04 通用数据帧（0x87）TLV 结构工具
 */
public final class DahuaTlv {

    private DahuaTlv() {}

    /**
     * TLV 节点
     */
    public static final class Node {
        /** 数据类型（2B,LE）。一级为0~9（注册包/事件/模拟量/参数/操作/升级/能力/确认/加密） */
        public final int type;
        /** Length 原始值（2B,LE）：嵌套时为子节点个数，叶子时为value字节数 */
        public final int length;
        /** true=Length为子节点个数（嵌套），false=Length为value字节数（叶子） */
        public final boolean nested;
        /** 嵌套时的子节点列表 */
        public final List<Node> children;
        /** 叶子时的原始值字节 */
        public final byte[] value;

        private Node(int type, int length, boolean nested, List<Node> children, byte[] value) {
            this.type = type;
            this.length = length;
            this.nested = nested;
            this.children = children;
            this.value = value;
        }

        /** 叶子节点快捷取值：2字节小端无符号 */
        public int valueAsU16LE() {
            if (value == null || value.length < 2) return -1;
            return (value[0] & 0xFF) | ((value[1] & 0xFF) << 8);
        }

        /** 叶子节点快捷取值：4字节小端无符号 */
        public long valueAsU32LE() {
            if (value == null || value.length < 4) return -1;
            return (value[0] & 0xFFL) | ((value[1] & 0xFFL) << 8)
                    | ((value[2] & 0xFFL) << 16) | ((value[3] & 0xFFL) << 24);
        }

        @Override
        public String toString() {
            return nested
                    ? "Tlv{type=" + type + ", children=" + children.size() + "}"
                    : "Tlv{type=" + type + ", len=" + length + ", value=" + bytesToHex(value) + "}";
        }
    }

    // ==================== 解析 ====================

    /**
     * 顺序解析TLV序列（用于0x87信息对象的"数据TLV结构"整体字节）
     *
     * @param data 数据内容字节（不含数据总长度字段）
     * @return 一级TLV节点列表
     */
    public static List<Node> parse(byte[] data) {
        List<Node> result = new ArrayList<>();
        if (data == null || data.length == 0) {
            return result;
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        while (buf.remaining() >= 4) {
            Node node = parseNode(buf, 0);
            if (node == null) {
                break;
            }
            result.add(node);
        }
        return result;
    }

    /**
     * 解析单个TLV节点（递归）。
     * <p>
     * 协议中 Length 有二义性（叶子=字节数 / 嵌套=子节点数），按层级消解：
     * <ul>
     *   <li>depth=0（一级数据类型）：优先按嵌套解析（L=二级类型个数），失败回退叶子；</li>
     *   <li>depth≥1（二级及以下类型）：优先按叶子解析（L=值字节数），仅当 L 超过
     *       剩余字节时才尝试嵌套，避免把固定长度的值字节误认为下级TLV。</li>
     * </ul>
     */
    private static Node parseNode(ByteBuffer buf, int depth) {
        if (buf.remaining() < 4) {
            return null;
        }
        int type = buf.getShort() & 0xFFFF;
        int length = buf.getShort() & 0xFFFF;
        int savePos = buf.position();

        if (depth == 0) {
            // 一级优先嵌套
            List<Node> children = parseChildren(buf, length, depth);
            if (children != null) {
                return new Node(type, length, true, children, null);
            }
            buf.position(savePos);
            return readLeaf(buf, type, length);
        }

        // 二级及以下优先叶子
        if (length <= buf.remaining()) {
            return readLeaf(buf, type, length);
        }
        // L 超过剩余字节：不可能是叶子字节数，尝试嵌套
        List<Node> children = parseChildren(buf, length, depth);
        if (children != null) {
            return new Node(type, length, true, children, null);
        }
        buf.position(savePos);
        return readLeaf(buf, type, buf.remaining());
    }

    /**
     * 读取叶子节点：Length=值字节数（截断到剩余字节）
     */
    private static Node readLeaf(ByteBuffer buf, int type, int length) {
        int len = Math.min(length, buf.remaining());
        byte[] value = new byte[len];
        buf.get(value);
        return new Node(type, len, false, null, value);
    }

    /**
     * 尝试解析 count 个下级TLV节点，任一失败返回 null 并回滚位置。
     */
    private static List<Node> parseChildren(ByteBuffer buf, int count, int parentDepth) {
        if (count <= 0) {
            return count == 0 ? new ArrayList<>() : null;
        }
        // 快速失败：count个节点至少需要 count*4 字节
        if ((long) count * 4 > buf.remaining()) {
            return null;
        }
        int save = buf.position();
        List<Node> nodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Node n = parseNode(buf, parentDepth + 1);
            if (n == null) {
                buf.position(save);
                return null;
            }
            nodes.add(n);
        }
        return nodes;
    }

    // ==================== 组装 ====================

    /**
     * 组装叶子TLV：Type(2B,LE) | Length=字节数(2B,LE) | Value
     */
    public static byte[] leaf(int type, byte[] value) {
        byte[] v = (value != null) ? value : new byte[0];
        ByteBuffer buf = ByteBuffer.allocate(4 + v.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) type);
        buf.putShort((short) v.length);
        buf.put(v);
        return toBytes(buf);
    }

    /**
     * 组装嵌套TLV：Type(2B,LE) | Length=子节点数(2B,LE) | 子TLV序列
     */
    public static byte[] nested(int type, List<byte[]> childTlvs) {
        int bodyLen = 0;
        for (byte[] c : childTlvs) {
            bodyLen += c.length;
        }
        ByteBuffer buf = ByteBuffer.allocate(4 + bodyLen).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) type);
        buf.putShort((short) childTlvs.size());
        for (byte[] c : childTlvs) {
            buf.put(c);
        }
        return toBytes(buf);
    }

    /**
     * 组装叶子TLV（2字节小端值）
     */
    public static byte[] leafU16LE(int type, int value) {
        return leaf(type, new byte[]{(byte) (value & 0xFF), (byte) ((value >> 8) & 0xFF)});
    }

    /**
     * 组装叶子TLV（4字节小端值）
     */
    public static byte[] leafU32LE(int type, long value) {
        return leaf(type, new byte[]{
                (byte) (value & 0xFF), (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF), (byte) ((value >> 24) & 0xFF)});
    }

    private static byte[] toBytes(ByteBuffer buf) {
        byte[] out = new byte[buf.position()];
        buf.flip();
        buf.get(out);
        return out;
    }

    // ==================== 工具 ====================

    public static String bytesToHex(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
