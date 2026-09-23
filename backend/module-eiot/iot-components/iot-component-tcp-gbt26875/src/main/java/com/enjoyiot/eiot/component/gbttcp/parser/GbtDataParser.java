package com.enjoyiot.eiot.component.gbttcp.parser;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.*;

/**
 * GB/T 26875.3-2011 应用数据单元解析器
 *
 * 将各类型标志对应的二进制应用数据解析为 List&lt;Map&lt;String,Object&gt;&gt;
 * 每条记录表示一条信息体，包含解析后的字段。
 *
 * 属性 key 命名规则（按类型加前缀，避免不同消息类型间 key 冲突）：
 *   Type  1  系统状态      → sys_*
 *   Type  2  部件状态      → comp_*
 *   Type  3  模拟量值      → analog_*
 *   Type  4  操作信息      → oper_*
 *   Type  5  软件版本      → ver_*
 *   Type  6  系统说明      → sysConf_*
 *   Type  7  部件配置      → compConf_*
 *   Type  8  系统时间      → sysTime_*
 *   Type 21  装置运行状态  → dev_*
 *   Type 24  装置操作信息  → devOper_*
 *   Type 25  装置软件版本  → dev_main_ver / dev_sub_ver
 *   Type 26  装置配置描述  → devConf_*
 *   Type 28  装置系统时间  → device_time
 *   公共字段              → gbtType (无前缀)
 */
@Slf4j
public class GbtDataParser {

    private static final Charset GB18030 = Charset.forName("GB18030");

    /**
     * 解析应用数据单元
     *
     * @param data 完整的应用数据单元字节数组
     * @return 解析结果列表；空或无法识别时返回空列表
     */
    public static List<Map<String, Object>> parse(byte[] data) {
        if (data == null || data.length < 2) {
            return Collections.emptyList();
        }
        int typeFlag = data[0] & 0xFF;
        int count = data[1] & 0xFF;
        ByteBuffer buf = ByteBuffer.wrap(data, 2, data.length - 2).order(ByteOrder.LITTLE_ENDIAN);

        switch (typeFlag) {
            case GbtConstants.TYPE_UP_SYSTEM_STATUS:
                return parseSystemStatus(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_COMPONENT_STATUS:
                return parseComponentStatus(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_ANALOG_VALUE:
                return parseAnalogValue(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_OPERATION_INFO:
                return parseOperationInfo(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_SOFTWARE_VERSION:
                return parseSoftwareVersion(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_SYSTEM_CONFIG:
                return parseSystemConfig(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_COMPONENT_CONFIG:
                return parseComponentConfig(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_SYSTEM_TIME:
                return parseSystemTime(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_DEVICE_STATUS:
                return parseDeviceStatus(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_DEVICE_OPERATION:
                return parseDeviceOperation(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_DEVICE_VERSION:
                return parseDeviceVersion(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_DEVICE_CONFIG:
                return parseDeviceConfig(buf, count, typeFlag);
            case GbtConstants.TYPE_UP_DEVICE_TIME:
                return parseDeviceTime(buf, count, typeFlag);
            default:
                log.info("未识别的类型标志: {}", typeFlag);
                List<Map<String, Object>> list = new ArrayList<>();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("count", count);
                m.put("rawHex", GbtDataPackage.bytesToHex(data));
                list.add(m);
                return list;
        }
    }

    /**
     * 获取应用数据中的类型标志（第1字节）
     */
    public static int getTypeFlag(byte[] data) {
        return (data != null && data.length > 0) ? (data[0] & 0xFF) : -1;
    }

    // ==================== Type 1~8: 建筑消防设施 ====================

    /**
     * Type 1: 建筑消防设施系统状态  (前缀 sys_)
     * 每条: 系统类型(1) + 系统地址(1) + 系统状态(2,LE) + 时间标签(6) = 10字节
     *
     * 系统状态位定义 (图6):
     *   bit0:  =1正常运行 =0测试    bit1:  =1火警 =0无火警
     *   bit2:  =1故障 =0无故障      bit3:  =1屏蔽 =0无屏蔽
     *   bit4:  =1监管 =0无监管      bit5:  =1启动(开启) =0停止(关闭)
     *   bit6:  =1反馈 =0无反馈      bit7:  =1延时 =0未延时
     *   bit8:  =1主电故障           bit9:  =1备电故障
     *   bit10: =1总线故障           bit11: =1手动状态 =0自动状态
     *   bit12: 配置改变             bit13-15: 预留
     */
    private static List<Map<String, Object>> parseSystemStatus(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 10; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("sys_systemType", buf.get() & 0xFF);
            m.put("sys_systemAddr", buf.get() & 0xFF);
            int status = buf.getShort() & 0xFFFF;
            m.put("sys_status", status);
            m.put("sys_statusTime", readTimestampStr(buf));
            // 解析状态位 -> int 类型 (0=正常/否, 1=异常/是)
            m.put("sys_normalMode", (status & 0x0001) != 0 ? 1 : 0);         // bit0:  =1正常运行 =0测试
            m.put("sys_fireAlarm", (status & 0x0002) != 0 ? 1 : 0);          // bit1:  火警
            m.put("sys_fault", (status & 0x0004) != 0 ? 1 : 0);              // bit2:  故障
            m.put("sys_shielded", (status & 0x0008) != 0 ? 1 : 0);           // bit3:  屏蔽
            m.put("sys_supervised", (status & 0x0010) != 0 ? 1 : 0);         // bit4:  监管
            m.put("sys_activated", (status & 0x0020) != 0 ? 1 : 0);          // bit5:  启动
            m.put("sys_feedback", (status & 0x0040) != 0 ? 1 : 0);           // bit6:  反馈
            m.put("sys_delay", (status & 0x0080) != 0 ? 1 : 0);              // bit7:  延时
            m.put("sys_mainPowerFault", (status & 0x0100) != 0 ? 1 : 0);     // bit8:  主电故障
            m.put("sys_backupPowerFault", (status & 0x0200) != 0 ? 1 : 0);   // bit9:  备电故障
            m.put("sys_busFault", (status & 0x0400) != 0 ? 1 : 0);           // bit10: 总线故障
            m.put("sys_manualMode", (status & 0x0800) != 0 ? 1 : 0);         // bit11: 手动状态
            m.put("sys_configChanged", (status & 0x1000) != 0 ? 1 : 0);      // bit12: 配置改变
            list.add(m);
        }
        return list;
    }

    /**
     * Type 2: 建筑消防设施部件运行状态  (前缀 comp_)
     * 每条: 系统类型(1)+系统地址(1)+部件类型(1)+部件地址(4)+部件状态(2,LE)+部件说明(31)+时间标签(6) = 46字节
     *
     * 部件状态位定义 (图7):
     *   bit0:  =1正常运行 =0测试    bit1:  =1火警 =0无火警
     *   bit2:  =1故障 =0无故障      bit3:  =1屏蔽 =0无屏蔽
     *   bit4:  =1监管 =0无监管      bit5:  =1启动(开启) =0停止(关闭)
     *   bit6:  =1反馈 =0无反馈      bit7:  =1延时 =0未延时
     *   bit8:  =1电源故障 =0电源正常
     *   bit9-15: 预留
     */
    private static List<Map<String, Object>> parseComponentStatus(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 46; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("comp_systemType", buf.get() & 0xFF);
            m.put("comp_systemAddr", buf.get() & 0xFF);
            m.put("comp_type", buf.get() & 0xFF);
            m.put("comp_addr", readInt(buf));
            int status = buf.getShort() & 0xFFFF;
            m.put("comp_status", status);
            m.put("comp_desc", readGb18030String(buf, 31));
            m.put("comp_statusTime", readTimestampStr(buf));
            // 解析状态位 -> int 类型 (0=正常/否, 1=异常/是)
            m.put("comp_normalMode", (status & 0x0001) != 0 ? 1 : 0);    // bit0: =1正常运行 =0测试
            m.put("comp_fireAlarm", (status & 0x0002) != 0 ? 1 : 0);     // bit1: 火警
            m.put("comp_fault", (status & 0x0004) != 0 ? 1 : 0);         // bit2: 故障
            m.put("comp_shielded", (status & 0x0008) != 0 ? 1 : 0);      // bit3: 屏蔽
            m.put("comp_supervised", (status & 0x0010) != 0 ? 1 : 0);    // bit4: 监管
            m.put("comp_activated", (status & 0x0020) != 0 ? 1 : 0);     // bit5: 启动
            m.put("comp_feedback", (status & 0x0040) != 0 ? 1 : 0);      // bit6: 反馈
            m.put("comp_delay", (status & 0x0080) != 0 ? 1 : 0);         // bit7: 延时
            m.put("comp_powerFault", (status & 0x0100) != 0 ? 1 : 0);    // bit8: 电源故障
            list.add(m);
        }
        return list;
    }

    /**
     * Type 3: 建筑消防设施部件模拟量值  (前缀 analog_)
     * 每条: 系统类型(1)+系统地址(1)+部件类型(1)+部件地址(4)+模拟量类型(1)+模拟量值(2,LE,有符号)+时间标签(6) = 16字节
     */
    private static List<Map<String, Object>> parseAnalogValue(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 16; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("analog_systemType", buf.get() & 0xFF);
            m.put("analog_systemAddr", buf.get() & 0xFF);
            m.put("analog_compType", buf.get() & 0xFF);
            m.put("analog_compAddr", readInt(buf));
            int analogType = buf.get() & 0xFF;
            short rawValue = buf.getShort();
            m.put("analog_type", analogType);
            m.put("analog_typeName", GbtConstants.getAnalogTypeName(analogType));
            m.put("analog_rawValue", (int) rawValue);
            m.put("analog_value", rawValue * GbtConstants.getAnalogScale(analogType));
            m.put("analog_unit", GbtConstants.getAnalogUnit(analogType));
            m.put("analog_sampleTime", readTimestampStr(buf));
            list.add(m);
        }
        return list;
    }

    /**
     * Type 4: 建筑消防设施操作信息  (前缀 oper_)
     * 每条: 系统类型(1)+系统地址(1)+操作标志(1)+操作员编号(1)+时间标签(6) = 10字节
     */
    private static List<Map<String, Object>> parseOperationInfo(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 10; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("oper_systemType", buf.get() & 0xFF);
            m.put("oper_systemAddr", buf.get() & 0xFF);
            m.put("oper_type", buf.get() & 0xFF);
            m.put("oper_operatorNum", buf.get() & 0xFF);
            m.put("oper_recordTime", readTimestampStr(buf));
            list.add(m);
        }
        return list;
    }

    /**
     * Type 5: 建筑消防设施软件版本  (前缀 ver_)
     * 系统类型(1)+系统地址(1)+主版本号(1)+次版本号(1) = 4字节，无时间标签
     */
    private static List<Map<String, Object>> parseSoftwareVersion(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 4; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("ver_systemType", buf.get() & 0xFF);
            m.put("ver_systemAddr", buf.get() & 0xFF);
            m.put("ver_major", buf.get() & 0xFF);
            m.put("ver_minor", buf.get() & 0xFF);
            list.add(m);
        }
        return list;
    }

    /**
     * Type 6: 建筑消防设施系统说明描述  (前缀 sysConf_)
     * 每条: 系统类型(1)+系统地址(1)+说明长度(1,=L)+说明(L字节) = 3+L字节，无时间标签
     */
    private static List<Map<String, Object>> parseSystemConfig(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 3; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("sysConf_systemType", buf.get() & 0xFF);
            m.put("sysConf_systemAddr", buf.get() & 0xFF);
            int descLen = buf.get() & 0xFF;
            if (buf.remaining() < descLen) break;
            m.put("sysConf_desc", readGb18030String(buf, descLen));
            list.add(m);
        }
        return list;
    }

    /**
     * Type 7: 建筑消防设施部件配置描述  (前缀 compConf_)
     * 每条: 系统类型(1)+系统地址(1)+部件类型(1)+部件地址(4)+部件说明(31) = 38字节，无时间标签
     */
    private static List<Map<String, Object>> parseComponentConfig(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 38; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("compConf_systemType", buf.get() & 0xFF);
            m.put("compConf_systemAddr", buf.get() & 0xFF);
            m.put("compConf_compType", buf.get() & 0xFF);
            m.put("compConf_compAddr", readInt(buf));
            m.put("compConf_desc", readGb18030String(buf, 31));
            list.add(m);
        }
        return list;
    }

    /**
     * Type 8: 建筑消防设施系统时间  (前缀 sysTime_)
     * 系统类型(1)+系统地址(1)+系统时间(6) = 8字节
     */
    private static List<Map<String, Object>> parseSystemTime(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 8; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("sysTime_systemType", buf.get() & 0xFF);
            m.put("sysTime_systemAddr", buf.get() & 0xFF);
            m.put("sysTime_time", readTimestampStr(buf));
            list.add(m);
        }
        return list;
    }

    // ==================== Type 21~28: 用户信息传输装置 ====================

    /**
     * Type 21: 用户信息传输装置运行状态  (前缀 dev_)
     * 状态(1) + 时间标签(6) = 7字节
     *
     * 装置运行状态位定义 (图13):
     *   bit0: =1正常监视 =0测试     bit1: =1火警 =0无火警
     *   bit2: =1故障 =0无故障       bit3: =1主电故障 =0主电正常
     *   bit4: =1备电故障 =0备电正常  bit5: =1通信信道故障 =0通信正常
     *   bit6: =1监测连接线路故障 =0线路正常
     *   bit7: 预留
     */
    private static List<Map<String, Object>> parseDeviceStatus(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 7; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            int status = buf.get() & 0xFF;
            m.put("dev_status_raw", status);
            // 状态值: 枚举类型 String "1"=异常 "0"=正常
            m.put("dev_normal_monitor", (status & 0x01) != 0 ? 1 : 0);       // bit0: =1正常监视 =0测试
            m.put("dev_fire_alarm", (status & 0x02) != 0 ? 1 : 0);        // bit1: 火警
            m.put("dev_fault", (status & 0x04) != 0 ? 1 : 0);            // bit2: 故障
            m.put("dev_main_power_fault", (status & 0x08) != 0 ? 1 : 0);   // bit3: 主电故障
            m.put("dev_backup_power_fault", (status & 0x10) != 0 ? 1 : 0); // bit4: 备电故障
            m.put("dev_comm_fault", (status & 0x20) != 0 ? 1 : 0);     // bit5: 通信信道故障
            m.put("dev_line_fault", (status & 0x40) != 0 ? 1 : 0); // bit6: 监测连接线路故障
            m.put("dev_status_time", readTimestampStr(buf));
            list.add(m);
        }
        return list;
    }

    /**
     * Type 24: 用户信息传输装置操作信息  (前缀 devOper_)
     * 每条: 操作信息(1)+操作员编号(1)+记录时间(6) = 8字节
     */
    private static List<Map<String, Object>> parseDeviceOperation(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count && buf.remaining() >= 8; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("devOper_type", buf.get() & 0xFF);
            m.put("devOper_operatorNum", buf.get() & 0xFF);
            m.put("devOper_recordTime", readTimestampStr(buf));
            list.add(m);
        }
        return list;
    }

    /**
     * Type 25: 用户信息传输装置软件版本  (前缀 dev_)
     * 主版本号(1)+次版本号(1) = 2字节，无时间标签
     */
    private static List<Map<String, Object>> parseDeviceVersion(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (buf.remaining() >= 2) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("dev_main_ver", buf.get() & 0xFF);
            m.put("dev_sub_ver", buf.get() & 0xFF);
            list.add(m);
        }
        return list;
    }

    /**
     * Type 26: 用户信息传输装置配置描述  (前缀 devConf_)
     * 说明长度(1,=L)+说明(L字节)，无时间标签
     */
    private static List<Map<String, Object>> parseDeviceConfig(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (buf.remaining() >= 1) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            int descLen = buf.get() & 0xFF;
            if (buf.remaining() >= descLen) {
                m.put("dev_config_desc", readGb18030String(buf, descLen));
            }
            list.add(m);
        }
        return list;
    }

    /**
     * Type 28: 用户信息传输装置系统时间  (前缀 dev_)
     * 系统时间(6字节)
     */
    private static List<Map<String, Object>> parseDeviceTime(ByteBuffer buf, int count, int typeFlag) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (buf.remaining() >= 6) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            m.put("device_time", readTimestampStr(buf));
            list.add(m);
        }
        return list;
    }

    // ==================== 工具方法 ====================

    /**
     * 读取6字节时间标签并转为可读字符串
     */
    private static String readTimestampStr(ByteBuffer buf) {
        int sec = buf.get() & 0xFF;
        int min = buf.get() & 0xFF;
        int hour = buf.get() & 0xFF;
        int day = buf.get() & 0xFF;
        int month = buf.get() & 0xFF;
        int year = 2000 + (buf.get() & 0xFF);
        return String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, min, sec);
    }

    /**
     * 读取4字节整数（小端序）
     */
    private static int readInt(ByteBuffer buf) {
        return buf.getInt();
    }

    /**
     * 读取指定长度的GB18030编码字符串，去除尾部零字节
     */
    private static String readGb18030String(ByteBuffer buf, int length) {
        byte[] bytes = new byte[length];
        buf.get(bytes);
        // 找到第一个 0x00 位置作为有效数据结尾
        int end = length;
        for (int i = 0; i < length; i++) {
            if (bytes[i] == 0) {
                end = i;
                break;
            }
        }
        return new String(bytes, 0, end, GB18030).trim();
    }
}
