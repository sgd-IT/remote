package com.enjoyiot.eiot.component.dahua.common.parser;

import com.enjoyiot.eiot.component.dahua.common.constants.DahuaProtocolConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 大华协议 v1.04 ADU 组装器（V2，平台下行帧）
 */
@Slf4j
public final class DahuaAduBuilderV2 {

    private DahuaAduBuilderV2() {
    }

    // ==================== 0x87 通用数据帧下行 ====================

    /**
     * 通用数据帧下行信息对象（图2.2.4.10-1 信息对象）
     */
    public static class UniversalInfoObject {
        public final int systemType;
        public final int systemAddr;
        public final int componentType;
        /**
         * 部件地址4字节LE：回路(2B)+节点(2B)
         */
        public final byte[] componentAddr;
        /**
         * 已组装好的数据TLV字节（可用 {@link DahuaTlv} 组装）
         */
        public final byte[] dataTlv;
        /**
         * 时间标签6字节，null 则用当前时间
         */
        public final byte[] timestamp;

        public UniversalInfoObject(int systemType, int systemAddr, int componentType,
                                   byte[] componentAddr, byte[] dataTlv, byte[] timestamp) {
            this.systemType = systemType;
            this.systemAddr = systemAddr;
            this.componentType = componentType;
            this.componentAddr = componentAddr;
            this.dataTlv = (dataTlv != null) ? dataTlv : new byte[0];
            this.timestamp = timestamp;
        }

        int size() {
            return 3 + DahuaProtocolConstants.COMPONENT_ADDR_LENGTH + 2 + dataTlv.length
                    + DahuaProtocolConstants.TIMESTAMP_LENGTH;
        }

        void writeTo(ByteBuffer buf) {
            buf.put((byte) systemType);
            buf.put((byte) systemAddr);
            buf.put((byte) componentType);
            putCompAddr(buf, componentAddr);
            buf.putShort((short) dataTlv.length);
            buf.put(dataTlv);
            byte[] ts = (timestamp != null) ? timestamp : DahuaAduBuilder.buildCurrentTimestamp();
            buf.put(ts, 0, DahuaProtocolConstants.TIMESTAMP_LENGTH);
        }
    }

    /**
     * 组装 0x87 通用数据帧 ADU（CMD=0x02 下发）
     * <pre>
     *   ADU = 0x87 | 信息对象数目(1B) | 信息对象 × N
     * </pre>
     */
    public static byte[] buildUniversalDataAdu(List<UniversalInfoObject> infoObjects) {
        if (infoObjects == null || infoObjects.isEmpty()) {
            log.warn("[解析器] 通用数据帧信息对象为空");
            return new byte[]{(byte) DahuaProtocolConstants.TYPE_UNIVERSAL_DATA, 0x00};
        }
        int total = 2;
        for (UniversalInfoObject o : infoObjects) {
            total += o.size();
        }
        ByteBuffer buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) DahuaProtocolConstants.TYPE_UNIVERSAL_DATA);
        buf.put((byte) infoObjects.size());
        for (UniversalInfoObject o : infoObjects) {
            o.writeTo(buf);
        }
        return toBytes(buf);
    }

    /**
     * 快捷组装：下发参数（定长）通用帧，TLV = 参数类型(3) > [参数子类型(value 4B LE)]
     *
     * @param paramType  定长参数类型字（表定长参数类型表）
     * @param paramValue 参数值（4字节LE）
     */
    public static byte[] buildUniversalSetParamFixedAdu(int sysType, int sysAddr, int compType,
                                                        byte[] compAddr, int paramType, long paramValue) {
        byte[] leaf = DahuaTlv.leafU32LE(paramType, paramValue);
        byte[] tlv = DahuaTlv.nested(DahuaProtocolConstants.DATA_TYPE_PARAM_FIXED,
                java.util.Collections.singletonList(leaf));
        UniversalInfoObject obj = new UniversalInfoObject(sysType, sysAddr, compType, compAddr, tlv, null);
        return buildUniversalDataAdu(java.util.Collections.singletonList(obj));
    }

    /**
     * 快捷组装：下发操作命令通用帧，TLV = 操作类型(5) > [操作子类型(value 2B LE)]
     */
    public static byte[] buildUniversalOperationAdu(int sysType, int sysAddr, int compType,
                                                    byte[] compAddr, int opType, int opValue) {
        byte[] leaf = DahuaTlv.leafU16LE(opType, opValue);
        byte[] tlv = DahuaTlv.nested(DahuaProtocolConstants.DATA_TYPE_OPERATION,
                java.util.Collections.singletonList(leaf));
        UniversalInfoObject obj = new UniversalInfoObject(sysType, sysAddr, compType, compAddr, tlv, null);
        return buildUniversalDataAdu(java.util.Collections.singletonList(obj));
    }

    // ==================== 功能配置帧 (CMD=0x81, typeFlag=0xFA) ====================

    /**
     * 功能配置信息对象（图2.2.6.1-1，13B/对象）
     */
    public static byte[] buildFunctionConfigAdu(int sysType, int sysAddr, int compType,
                                                byte[] compAddr, int opType, int opValue) {
        ByteBuffer buf = ByteBuffer.allocate(2 + 13).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) DahuaProtocolConstants.TYPE_FUNCTION_CONFIG); // 0xFA
        buf.put((byte) 0x01);
        buf.put((byte) sysType);
        buf.put((byte) sysAddr);
        buf.put((byte) compType);
        putCompAddr(buf, compAddr);
        buf.putShort((short) opType);   // 操作类型 2B LE，见表3.2.5-1
        buf.putShort((short) opValue);  // 参数值 2B LE
        buf.putShort((short) 0);        // 预留
        return toBytes(buf);
    }

    // ==================== 升级请求帧 (CMD=0x04, typeFlag=0xC8) ====================

    /**
     * 升级请求帧 ADU（图2.2.7.1-1）
     * <pre>
     *   0xC8 | 信息对象数目(1B=1)
     *   | 系统类型(1B) | 系统地址(1B) | 部件类型(1B) | 部件地址(4B,LE,按bit表示目标)
     *   | 软件类型(1B) | 软件版本(4B: 预留|预留|主版本|次版本)
     *   | 地址类型(1B, FTP=0x00) | 地址信息(不定长)
     *   地址信息 = 地址长度(1B) | 用户名长度(1B) | 用户密码长度(1B) | 预留(1B)
     *             | IP地址 | 用户名 | 用户密码 | 文件路径 | 文件名（各项分号结尾）
     * </pre>
     */
    public static byte[] buildUpgradeRequestAdu(int sysType, int sysAddr, int compType,
                                                byte[] compAddr, int softType,
                                                int majorVer, int minorVer,
                                                String ftpIp, String ftpUser, String ftpPassword,
                                                String filePath, String fileName) {
        byte[] ip = semi(ftpIp);
        byte[] user = semi(ftpUser);
        byte[] pwd = semi(ftpPassword);
        byte[] path = semi(filePath);
        byte[] name = semi(fileName);
        int addrInfoLen = 4 + ip.length + user.length + pwd.length + path.length + name.length;

        ByteBuffer buf = ByteBuffer.allocate(2 + 3 + 4 + 1 + 4 + 1 + addrInfoLen)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0xC8);
        buf.put((byte) 0x01);
        buf.put((byte) sysType);
        buf.put((byte) sysAddr);
        buf.put((byte) compType);
        putCompAddr(buf, compAddr);
        buf.put((byte) softType);
        buf.put((byte) 0);            // 预留
        buf.put((byte) 0);            // 预留
        buf.put((byte) majorVer);     // 主版本号
        buf.put((byte) minorVer);     // 次版本号
        buf.put((byte) 0x00);         // 地址类型 FTP
        // 地址信息
        buf.put((byte) ip.length);
        buf.put((byte) user.length);
        buf.put((byte) pwd.length);
        buf.put((byte) 0);            // 预留
        buf.put(ip);
        buf.put(user);
        buf.put(pwd);
        buf.put(path);
        buf.put(name);
        return toBytes(buf);
    }

    // ==================== 查询能力集 (CMD=0x04, typeFlag=0x8B) ====================

    /**
     * 查询能力集 ADU（图2.2.5.8-1，结构同查询通用帧）
     * <pre>
     *   0x8B | 信息对象数目(1B) | [部件类型(1B) | 部件地址(4B,LE) | 数据类型(1B)] × N
     * </pre>
     *
     * @param componentType 部件类型（255=批量能力上报）
     * @param dataType      能力数据类型：0=单通道能力集, 1=全能力集
     */
    public static byte[] buildQueryCapabilityAdu(int componentType, byte[] compAddr, int dataType) {
        ByteBuffer buf = ByteBuffer.allocate(2 + 6).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) DahuaProtocolConstants.TYPE_QUERY_CAPABILITY); // 0x8B
        buf.put((byte) 0x01);
        buf.put((byte) componentType);
        putCompAddr(buf, compAddr);
        buf.put((byte) dataType);
        return toBytes(buf);
    }

    // ==================== 工具 ====================

    private static void putCompAddr(ByteBuffer buf, byte[] compAddr) {
        if (compAddr != null && compAddr.length >= DahuaProtocolConstants.COMPONENT_ADDR_LENGTH) {
            buf.put(compAddr, 0, DahuaProtocolConstants.COMPONENT_ADDR_LENGTH);
        } else {
            buf.putInt(0);
        }
    }

    /**
     * 字符串转字节并以分号(0x3B)结尾；空串则仅分号
     */
    private static byte[] semi(String s) {
        byte[] str = (s == null) ? new byte[0] : s.getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[str.length + 1];
        System.arraycopy(str, 0, out, 0, str.length);
        out[out.length - 1] = ';';
        return out;
    }

    private static byte[] toBytes(ByteBuffer buf) {
        byte[] out = new byte[buf.position()];
        buf.flip();
        buf.get(out);
        return out;
    }
}
