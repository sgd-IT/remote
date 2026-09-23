package com.enjoyiot.eiot.component.ff.parser.handler;

import com.enjoyiot.eiot.component.ff.parser.AnalogTypeDef;
import com.enjoyiot.eiot.component.ff.parser.GbtConstants;
import com.enjoyiot.eiot.component.ff.parser.HandlerUtils;
import com.enjoyiot.eiot.component.ff.parser.TypeHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准建筑消防设施类型处理器（TLD 1~8）+ 用户信息传输装置类型（TLD 21~28）
 * <p>
 * 合并实现以减少类文件数量，通过 getTypeFlag() 区分具体类型。
 */
@Slf4j
public class StandardHandlers {

    private StandardHandlers() {}

    // ==================== TLD 1: 系统状态 ====================
    public static class SystemStatusHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_SYSTEM_STATUS; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 10; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("sys_systemType", buf.get() & 0xFF);
                m.put("sys_systemAddr", buf.get() & 0xFF);
                int status = buf.getShort() & 0xFFFF;
                m.put("sys_status", status);
                m.put("sys_statusTime", HandlerUtils.readTimestampStr(buf));
                m.put("sys_normalMode", (status & 0x0001) != 0 ? 1 : 0);
                m.put("sys_fireAlarm", (status & 0x0002) != 0 ? 1 : 0);
                m.put("sys_fault", (status & 0x0004) != 0 ? 1 : 0);
                m.put("sys_shielded", (status & 0x0008) != 0 ? 1 : 0);
                m.put("sys_supervised", (status & 0x0010) != 0 ? 1 : 0);
                m.put("sys_activated", (status & 0x0020) != 0 ? 1 : 0);
                m.put("sys_feedback", (status & 0x0040) != 0 ? 1 : 0);
                m.put("sys_delay", (status & 0x0080) != 0 ? 1 : 0);
                m.put("sys_mainPowerFault", (status & 0x0100) != 0 ? 1 : 0);
                m.put("sys_backupPowerFault", (status & 0x0200) != 0 ? 1 : 0);
                m.put("sys_busFault", (status & 0x0400) != 0 ? 1 : 0);
                m.put("sys_manualMode", (status & 0x0800) != 0 ? 1 : 0);
                m.put("sys_configChanged", (status & 0x1000) != 0 ? 1 : 0);
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 2: 部件运行状态 ====================
    public static class ComponentStatusHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_COMPONENT_STATUS; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 46; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("comp_systemType", buf.get() & 0xFF);
                m.put("comp_systemAddr", buf.get() & 0xFF);
                m.put("comp_type", buf.get() & 0xFF);
                m.put("comp_addr", HandlerUtils.readIntLE(buf));
                int status = buf.getShort() & 0xFFFF;
                m.put("comp_status", status);
                m.put("comp_desc", HandlerUtils.readGb18030String(buf, 31));
                m.put("comp_statusTime", HandlerUtils.readTimestampStr(buf));
                m.put("comp_normalMode", (status & 0x0001) != 0 ? 1 : 0);
                m.put("comp_fireAlarm", (status & 0x0002) != 0 ? 1 : 0);
                m.put("comp_fault", (status & 0x0004) != 0 ? 1 : 0);
                m.put("comp_shielded", (status & 0x0008) != 0 ? 1 : 0);
                m.put("comp_supervised", (status & 0x0010) != 0 ? 1 : 0);
                m.put("comp_activated", (status & 0x0020) != 0 ? 1 : 0);
                m.put("comp_feedback", (status & 0x0040) != 0 ? 1 : 0);
                m.put("comp_delay", (status & 0x0080) != 0 ? 1 : 0);
                m.put("comp_powerFault", (status & 0x0100) != 0 ? 1 : 0);
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 3: 部件模拟量值 ====================
    public static class AnalogValueHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_ANALOG_VALUE; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 16; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("analog_systemType", buf.get() & 0xFF);
                m.put("analog_systemAddr", buf.get() & 0xFF);
                m.put("analog_compType", buf.get() & 0xFF);
                m.put("analog_compAddr", HandlerUtils.readIntLE(buf));
                int analogType = buf.get() & 0xFF;
                short rawValue = buf.getShort();
                m.put("analog_type", analogType);
                m.put("analog_typeName", AnalogTypeDef.getName(analogType));
                m.put("analog_rawValue", (int) rawValue);
                m.put("analog_value", rawValue * AnalogTypeDef.getScale(analogType));
                m.put("analog_unit", AnalogTypeDef.getUnit(analogType));
                m.put("analog_sampleTime", HandlerUtils.readTimestampStr(buf));
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 4: 操作信息 ====================
    public static class OperationInfoHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_OPERATION_INFO; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 10; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("oper_systemType", buf.get() & 0xFF);
                m.put("oper_systemAddr", buf.get() & 0xFF);
                m.put("oper_type", buf.get() & 0xFF);
                m.put("oper_operatorNum", buf.get() & 0xFF);
                m.put("oper_recordTime", HandlerUtils.readTimestampStr(buf));
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 5: 软件版本 ====================
    public static class SoftwareVersionHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_SOFTWARE_VERSION; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
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
    }

    // ==================== TLD 6: 系统配置 ====================
    public static class SystemConfigHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_SYSTEM_CONFIG; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 3; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("sysConf_systemType", buf.get() & 0xFF);
                m.put("sysConf_systemAddr", buf.get() & 0xFF);
                int descLen = buf.get() & 0xFF;
                if (buf.remaining() < descLen) break;
                m.put("sysConf_desc", HandlerUtils.readGb18030String(buf, descLen));
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 7: 部件配置 ====================
    public static class ComponentConfigHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_COMPONENT_CONFIG; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 38; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("compConf_systemType", buf.get() & 0xFF);
                m.put("compConf_systemAddr", buf.get() & 0xFF);
                m.put("compConf_compType", buf.get() & 0xFF);
                m.put("compConf_compAddr", HandlerUtils.readIntLE(buf));
                m.put("compConf_desc", HandlerUtils.readGb18030String(buf, 31));
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 8: 系统时间 ====================
    public static class SystemTimeHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_SYSTEM_TIME; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 8; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("sysTime_systemType", buf.get() & 0xFF);
                m.put("sysTime_systemAddr", buf.get() & 0xFF);
                m.put("sysTime_time", HandlerUtils.readTimestampStr(buf));
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 21: 装置运行状态 ====================
    public static class DeviceStatusHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_DEVICE_STATUS; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 7; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                int status = buf.get() & 0xFF;
                m.put("dev_status_raw", status);
                m.put("dev_normal_monitor", (status & 0x01) != 0 ? 1 : 0);
                m.put("dev_fire_alarm", (status & 0x02) != 0 ? 1 : 0);
                m.put("dev_fault", (status & 0x04) != 0 ? 1 : 0);
                m.put("dev_main_power_fault", (status & 0x08) != 0 ? 1 : 0);
                m.put("dev_backup_power_fault", (status & 0x10) != 0 ? 1 : 0);
                m.put("dev_comm_fault", (status & 0x20) != 0 ? 1 : 0);
                m.put("dev_line_fault", (status & 0x40) != 0 ? 1 : 0);
                m.put("dev_status_time", HandlerUtils.readTimestampStr(buf));
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 24: 装置操作信息 ====================
    public static class DeviceOperationHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_DEVICE_OPERATION; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 8; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("devOper_type", buf.get() & 0xFF);
                m.put("devOper_operatorNum", buf.get() & 0xFF);
                m.put("devOper_recordTime", HandlerUtils.readTimestampStr(buf));
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 25: 装置软件版本 ====================
    public static class DeviceVersionHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_DEVICE_VERSION; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
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
    }

    // ==================== TLD 26: 装置配置描述 ====================
    public static class DeviceConfigHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_DEVICE_CONFIG; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            if (buf.remaining() >= 1) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                int descLen = buf.get() & 0xFF;
                if (buf.remaining() >= descLen) {
                    m.put("dev_config_desc", HandlerUtils.readGb18030String(buf, descLen));
                }
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 28: 装置系统时间 ====================
    public static class DeviceTimeHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_DEVICE_TIME; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            if (buf.remaining() >= 6) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("device_time", HandlerUtils.readTimestampStr(buf));
                list.add(m);
            }
            return list;
        }
    }
}
