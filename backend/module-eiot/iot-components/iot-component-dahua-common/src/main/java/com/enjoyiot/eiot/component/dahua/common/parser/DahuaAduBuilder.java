package com.enjoyiot.eiot.component.dahua.common.parser;

import com.enjoyiot.eiot.component.dahua.common.constants.DahuaProtocolConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 大华协议 ADU（应用数据单元）组装器
 */
@Slf4j
public final class DahuaAduBuilder {

    private DahuaAduBuilder() {}

    // ==================== 时间标签构建 ====================

    /**
     * 构建当前时间的6字节时间标签
     * <p>
     * 格式: 秒(1B) | 分(1B) | 时(1B) | 日(1B) | 月(1B) | 年-2000(1B)
     */
    public static byte[] buildCurrentTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        return new byte[]{
                (byte) now.getSecond(),
                (byte) now.getMinute(),
                (byte) now.getHour(),
                (byte) now.getDayOfMonth(),
                (byte) now.getMonthValue(),
                (byte) (now.getYear() - 2000)
        };
    }

    // ==================== 时钟同步帧 (CMD=0x01, typeFlag=0x5A) ====================

    /**
     * 构建时钟同步 ADU
     * <pre>
     *   ADU = 类型标志(1B=0x5A) | 时间标签(6B)
     * </pre>
     *
     * @return 7字节 ADU
     */
    public static byte[] buildClockSyncAdu() {
        return buildClockSyncAdu(buildCurrentTimestamp());
    }

    /**
     * 构建时钟同步 ADU（指定时间标签）
     */
    public static byte[] buildClockSyncAdu(byte[] timestamp) {
        byte[] adu = new byte[1 + DahuaProtocolConstants.TIMESTAMP_LENGTH];
        adu[0] = (byte) DahuaProtocolConstants.TYPE_CLOCK_SYNC; // 0x5A
        System.arraycopy(timestamp, 0, adu, 1, DahuaProtocolConstants.TIMESTAMP_LENGTH);
        return adu;
    }

    // ==================== 设置参数帧 (CMD=0x04) ====================

    /**
     * 构建设置定长参数 ADU (typeFlag=0x64)
     * <pre>
     *   ADU = 0x64 | 信息体数目(1B=1) | 参数类型(1B) | 参数值(NB)
     * </pre>
     *
     * @param paramType 参数类型（0~255）
     * @param value     参数值字节
     * @return ADU 字节数组
     */
    public static byte[] buildSetParamFixedAdu(int paramType, byte[] value) {
        return buildParamAdu(
                DahuaProtocolConstants.TYPE_SET_PARAM_FIXED, paramType, value);
    }

    /**
     * 构建设置不定长参数 ADU (typeFlag=0x81)
     * <pre>
     *   ADU = 0x81 | 信息体数目(1B=1) | 参数类型(1B) | 参数值(NB)
     * </pre>
     *
     * @param paramType 参数类型（0~255）
     * @param value     参数值字节
     * @return ADU 字节数组
     */
    public static byte[] buildSetParamVariableAdu(int paramType, byte[] value) {
        return buildParamAdu(
                DahuaProtocolConstants.TYPE_SET_PARAM_VARIABLE, paramType, value);
    }

    /**
     * 构建参数设置帧 ADU（typeFlag + 参数类型 + 参数值，定长和不定长共用结构）
     */
    private static byte[] buildParamAdu(int typeFlag, int paramType, byte[] value) {
        byte[] v = (value != null) ? value : new byte[0];
        ByteBuffer buf = ByteBuffer.allocate(1 + 1 + 1 + v.length)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) typeFlag);
        buf.put((byte) 0x01);        // 信息体数目=1
        buf.put((byte) paramType);
        buf.put(v);
        return toArray(buf);
    }

    // ==================== 查询参数帧 (CMD=0x04) ====================

    /**
     * 构建查询定长参数 ADU (typeFlag=0x82)
     * <pre>
     *   ADU = 0x82 | 信息体数目(1B=1) | 参数类型(1B)
     * </pre>
     *
     * @param paramType 参数类型（0~255）
     * @return ADU 字节数组
     */
    public static byte[] buildQueryParamFixedAdu(int paramType) {
        return buildQueryParamAdu(
                DahuaProtocolConstants.TYPE_QUERY_PARAM_FIXED, paramType);
    }

    /**
     * 构建查询不定长参数 ADU (typeFlag=0x83)
     * <pre>
     *   ADU = 0x83 | 信息体数目(1B=1) | 参数类型(1B)
     * </pre>
     *
     * @param paramType 参数类型（0~255）
     * @return ADU 字节数组
     */
    public static byte[] buildQueryParamVariableAdu(int paramType) {
        return buildQueryParamAdu(
                DahuaProtocolConstants.TYPE_QUERY_PARAM_VARIABLE, paramType);
    }

    /**
     * 构建参数查询帧 ADU（typeFlag + 参数类型）
     */
    private static byte[] buildQueryParamAdu(int typeFlag, int paramType) {
        ByteBuffer buf = ByteBuffer.allocate(1 + 1 + 1)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) typeFlag);
        buf.put((byte) 0x01);        // 信息体数目=1
        buf.put((byte) paramType);
        return toArray(buf);
    }

    // ==================== 远程消音帧 (CMD=0x04, typeFlag=0xF9) ====================

    /**
     * 构建远程消音 ADU
     * <pre>
     *   ADU = 0xF9 | 信息体数目(1B=1)
     *        | 系统类型(1B) | 系统地址(1B) | 部件类型(1B) | 部件地址(4B,LE)
     * </pre>
     *
     * @param sysType  系统类型（如 0x01=火灾报警控制器）
     * @param sysAddr  系统地址
     * @param compType 部件类型
     * @param compAddr 部件地址（4字节，LE），通常为 nodeAddr(2B) + loopAddr(2B)
     * @return ADU 字节数组
     */
    public static byte[] buildRemoteMuteAdu(int sysType, int sysAddr, int compType, byte[] compAddr) {
        ByteBuffer buf = ByteBuffer.allocate(1 + 1 + 1 + 1 + 1 + DahuaProtocolConstants.COMPONENT_ADDR_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) DahuaProtocolConstants.TYPE_REMOTE_MUTE); // 0xF9
        buf.put((byte) 0x01);         // 信息体数目=1
        buf.put((byte) sysType);
        buf.put((byte) sysAddr);
        buf.put((byte) compType);
        if (compAddr != null && compAddr.length >= DahuaProtocolConstants.COMPONENT_ADDR_LENGTH) {
            buf.put(compAddr, 0, DahuaProtocolConstants.COMPONENT_ADDR_LENGTH);
        } else {
            buf.putInt(0);
        }
        return toArray(buf);
    }

    /**
     * 构建远程消音 ADU（使用 int 表示部件地址）
     *
     * @param compAddrInt 部件地址（整数，按 LE 写入4字节）
     */
    public static byte[] buildRemoteMuteAdu(int sysType, int sysAddr, int compType, int compAddrInt) {
        ByteBuffer buf = ByteBuffer.allocate(1 + 1 + 1 + 1 + 1 + 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) DahuaProtocolConstants.TYPE_REMOTE_MUTE);
        buf.put((byte) 0x01);
        buf.put((byte) sysType);
        buf.put((byte) sysAddr);
        buf.put((byte) compType);
        buf.putInt(compAddrInt);
        return toArray(buf);
    }

    // ==================== 查询通用帧 (CMD=0x04, typeFlag=0x88) ====================

    /**
     * 通用查询信息对象（用于 0x88 查询通用帧）
     * <pre>
     *   部件类型(1B) | 部件地址(4B,LE) | 数据类型(1B)
     * </pre>
     */
    public static class QueryInfoObject {
        /** 部件类型 */
        public final int componentType;
        /** 部件地址（4字节 LE），如 nodeAddr(2B) + loopAddr(2B) */
        public final byte[] componentAddr;
        /** 数据类型（0~9: 注册/事件/模拟量/参数/操作/升级/能力/确认/加密） */
        public final int dataType;

        /** 每个信息对象固定10字节 */
        private static final int INFO_OBJ_SIZE = 1 + DahuaProtocolConstants.COMPONENT_ADDR_LENGTH + 1;

        public QueryInfoObject(int componentType, byte[] componentAddr, int dataType) {
            this.componentType = componentType;
            this.componentAddr = componentAddr;
            this.dataType = dataType;
        }

        void writeTo(ByteBuffer buf) {
            buf.put((byte) componentType);
            if (componentAddr != null && componentAddr.length >= DahuaProtocolConstants.COMPONENT_ADDR_LENGTH) {
                buf.put(componentAddr, 0, DahuaProtocolConstants.COMPONENT_ADDR_LENGTH);
            } else {
                buf.putInt(0);
            }
            buf.put((byte) dataType);
        }
    }

    /**
     * 构建查询通用帧 ADU (typeFlag=0x88)
     * <pre>
     *   ADU = 0x88 | 信息体数目(1B) | [部件类型(1) | 部件地址(4) | 数据类型(1)] × N
     * </pre>
     *
     * @param infoObjects 查询信息对象列表
     * @return ADU 字节数组
     */
    public static byte[] buildQueryUniversalAdu(List<QueryInfoObject> infoObjects) {
        if (infoObjects == null || infoObjects.isEmpty()) {
            log.warn("[解析器] 查询通用帧信息对象为空");
            return new byte[]{(byte) DahuaProtocolConstants.TYPE_QUERY_UNIVERSAL, 0x00};
        }

        ByteBuffer buf = ByteBuffer.allocate(1 + 1 + infoObjects.size() * 6)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) DahuaProtocolConstants.TYPE_QUERY_UNIVERSAL);
        buf.put((byte) infoObjects.size());
        for (QueryInfoObject obj : infoObjects) {
            obj.writeTo(buf);
        }
        return toArray(buf);
    }

    // ==================== 设置/配置通用帧 (CMD=0x04, typeFlag=0x89) ====================

    /**
     * 通用配置信息对象（用于 0x89 设置通用帧）
     * <pre>
     *   部件类型(1B) | 部件地址(4B,LE) | 数据类型(1B)
     *   | 数据内容长度(2B,LE) | 数据内容(NB)
     * </pre>
     */
    public static class ConfigInfoObject {
        /** 部件类型 */
        public final int componentType;
        /** 部件地址（4字节 LE） */
        public final byte[] componentAddr;
        /** 数据类型 */
        public final int dataType;
        /** 数据内容 */
        public final byte[] dataContent;

        public ConfigInfoObject(int componentType, byte[] componentAddr, int dataType, byte[] dataContent) {
            this.componentType = componentType;
            this.componentAddr = componentAddr;
            this.dataType = dataType;
            this.dataContent = (dataContent != null) ? dataContent : new byte[0];
        }

        /**
         * 单个信息对象字节数
         */
        int size() {
            return 1 + DahuaProtocolConstants.COMPONENT_ADDR_LENGTH + 1 + 2 + dataContent.length;
        }

        void writeTo(ByteBuffer buf) {
            buf.put((byte) componentType);
            if (componentAddr != null && componentAddr.length >= DahuaProtocolConstants.COMPONENT_ADDR_LENGTH) {
                buf.put(componentAddr, 0, DahuaProtocolConstants.COMPONENT_ADDR_LENGTH);
            } else {
                buf.putInt(0);
            }
            buf.put((byte) dataType);
            buf.putShort((short) dataContent.length);
            if (dataContent.length > 0) {
                buf.put(dataContent);
            }
        }
    }

    /**
     * 构建设置/配置通用帧 ADU (typeFlag=0x89)
     * <pre>
     *   ADU = 0x89 | 信息体数目(1B)
     *        | [部件类型(1) | 部件地址(4) | 数据类型(1) | 数据长度(2) | 数据内容(N)] × N
     * </pre>
     *
     * @param infoObjects 配置信息对象列表
     * @return ADU 字节数组
     */
    public static byte[] buildConfigUniversalAdu(List<ConfigInfoObject> infoObjects) {
        if (infoObjects == null || infoObjects.isEmpty()) {
            log.warn("[解析器] 配置通用帧信息对象为空");
            return new byte[]{(byte) DahuaProtocolConstants.TYPE_CONFIG_UNIVERSAL, 0x00};
        }

        int totalSize = 1 + 1; // typeFlag + count
        for (ConfigInfoObject obj : infoObjects) {
            totalSize += obj.size();
        }

        ByteBuffer buf = ByteBuffer.allocate(totalSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) DahuaProtocolConstants.TYPE_CONFIG_UNIVERSAL);
        buf.put((byte) infoObjects.size());
        for (ConfigInfoObject obj : infoObjects) {
            obj.writeTo(buf);
        }
        return toArray(buf);
    }

    // ==================== 确认/否认帧 (CMD=0x03/0x06) ====================

    /**
     * 构建确认帧 ADU（空 ADU，仅命令字区分）
     * <pre>
     *   ADU = 空（0字节）
     * </pre>
     */
    public static byte[] buildConfirmAdu() {
        return new byte[0];
    }

    /**
     * 构建否认帧 ADU（空 ADU，仅命令字区分）
     */
    public static byte[] buildDenyAdu() {
        return new byte[0];
    }

    // ==================== 工具方法 ====================

    /**
     * 构建信息体数目为1的通用 ADU（typeFlag + count=1 + body）
     *
     * @param typeFlag 类型标志
     * @param body     信息体数据（不含 typeFlag 和 count）
     * @return ADU 字节数组
     */
    public static byte[] buildSimpleAdu(int typeFlag, byte[] body) {
        byte[] b = (body != null) ? body : new byte[0];
        byte[] adu = new byte[2 + b.length];
        adu[0] = (byte) typeFlag;
        adu[1] = (byte) 0x01;
        System.arraycopy(b, 0, adu, 2, b.length);
        return adu;
    }

    /**
     * 将 ByteBuffer 已写入部分转为字节数组
     */
    private static byte[] toArray(ByteBuffer buf) {
        byte[] bytes = new byte[buf.position()];
        buf.flip();
        buf.get(bytes);
        return bytes;
    }

    /**
     * 获取当前时间的6字节时间标签（实例方法别名，便于注入场景使用）
     */
    public byte[] currentTimestamp() {
        return buildCurrentTimestamp();
    }
}
