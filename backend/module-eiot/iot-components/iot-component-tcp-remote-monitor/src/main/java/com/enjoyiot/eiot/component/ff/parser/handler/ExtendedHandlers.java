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
 * 拓展版新增类型处理器
 * <p>
 * TLD 138: 上报电能量
 * TLD 140: 上报多通道设备状态、模拟量
 * TLD 142: 上报设备状态、模拟量（合并包）
 * TLD 143: 上报32bit设备状态、模拟量
 * TLD 147: 上报多通道设备各个通道的部件类型
 * TLD 148: 上报多通道设备各个通道数据
 */
@Slf4j
public class ExtendedHandlers {

    private ExtendedHandlers() {}

    // ==================== TLD 138: 上报电能量 ====================
    // 系统类型(1) + 系统地址(1) + 部件类型(1) + 部件地址(4) + 电能数量(1) + [电能类型(1)+电能值(4)] × n
    public static class EnergyReportHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_ENERGY; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 8; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("energy_systemType", buf.get() & 0xFF);
                m.put("energy_systemAddr", buf.get() & 0xFF);
                m.put("energy_compType", buf.get() & 0xFF);
                m.put("energy_compAddr", HandlerUtils.readIntLE(buf));
                int energyCount = buf.get() & 0xFF;
                m.put("energy_count", energyCount);

                List<Map<String, Object>> energyList = new ArrayList<>();
                for (int j = 0; j < energyCount && buf.remaining() >= 5; j++) {
                    Map<String, Object> e = new LinkedHashMap<>();
                    int eType = buf.get() & 0xFF;
                    long eValue = buf.getInt() & 0xFFFFFFFFL;
                    e.put("energy_type", eType);
                    e.put("energy_type_desc", getEnergyTypeDesc(eType));
                    e.put("energy_value", eValue); // 精度0.01KWh
                    e.put("energy_kwh", eValue * 0.01);
                    energyList.add(e);
                }
                m.put("energy_list", energyList);
                list.add(m);
            }
            return list;
        }

        private String getEnergyTypeDesc(int type) {
            switch (type) {
                case 1: return "A相电能";
                case 2: return "B相电能";
                case 3: return "C相电能";
                case 4: return "总相电能";
                default: return "未知(" + type + ")";
            }
        }
    }

    // ==================== TLD 140: 上报多通道设备状态、模拟量 ====================
    // 系统类型(1) + 系统地址(1) + 部件类型(1) + 部件地址(4) + 通道号(1) + 状态(2) + 模拟量个数(1) + [类型(1)+值(2)] × n
    public static class MultiChannelStatusHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_MULTI_CHAN_STATUS; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 11; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("mc_systemType", buf.get() & 0xFF);
                m.put("mc_systemAddr", buf.get() & 0xFF);
                m.put("mc_compType", buf.get() & 0xFF);
                m.put("mc_compAddr", HandlerUtils.readIntLE(buf));
                m.put("mc_channelNo", buf.get() & 0xFF);
                int status = buf.getShort() & 0xFFFF;
                m.put("mc_status", status);
                int analogCount = buf.get() & 0xFF;
                m.put("mc_analogCount", analogCount);

                List<Map<String, Object>> analogs = new ArrayList<>();
                for (int j = 0; j < analogCount && buf.remaining() >= 3; j++) {
                    Map<String, Object> a = new LinkedHashMap<>();
                    int aType = buf.get() & 0xFF;
                    short aVal = buf.getShort();
                    a.put("analog_type", aType);
                    a.put("analog_typeName", AnalogTypeDef.getName(aType));
                    a.put("analog_rawValue", (int) aVal);
                    a.put("analog_value", aVal * AnalogTypeDef.getScale(aType));
                    a.put("analog_unit", AnalogTypeDef.getUnit(aType));
                    analogs.add(a);
                }
                m.put("mc_analogs", analogs);
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 142: 上报设备状态、模拟量（合并包） ====================
    // 系统类型(1) + 系统地址(1) + 部件类型(1) + 部件地址(4) + 状态(2) + 模拟量个数(1) + [类型(1)+值(2)] × n
    public static class DeviceStatusAnalogHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_DEVICE_STATUS_ANALOG; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 10; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("dsa_systemType", buf.get() & 0xFF);
                m.put("dsa_systemAddr", buf.get() & 0xFF);
                m.put("dsa_compType", buf.get() & 0xFF);
                m.put("dsa_compAddr", HandlerUtils.readIntLE(buf));
                int status = buf.getShort() & 0xFFFF;
                m.put("dsa_status", status);
                int analogCount = buf.get() & 0xFF;
                m.put("dsa_analogCount", analogCount);

                List<Map<String, Object>> analogs = new ArrayList<>();
                for (int j = 0; j < analogCount && buf.remaining() >= 3; j++) {
                    Map<String, Object> a = new LinkedHashMap<>();
                    int aType = buf.get() & 0xFF;
                    short aVal = buf.getShort();
                    a.put("analog_type", aType);
                    a.put("analog_typeName", AnalogTypeDef.getName(aType));
                    a.put("analog_rawValue", (int) aVal);
                    a.put("analog_value", aVal * AnalogTypeDef.getScale(aType));
                    a.put("analog_unit", AnalogTypeDef.getUnit(aType));
                    analogs.add(a);
                }
                m.put("dsa_analogs", analogs);
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 143: 上报32bit设备状态、模拟量 ====================
    // 系统类型(1) + 系统地址(1) + 部件类型(1) + 部件地址(4) + 状态(4,DWORD) + 模拟量个数(1) + [类型(1)+值(2)] × n
    public static class StatusAnalog32Handler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_STATUS_ANALOG_32BIT; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 12; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("sa32_systemType", buf.get() & 0xFF);
                m.put("sa32_systemAddr", buf.get() & 0xFF);
                m.put("sa32_compType", buf.get() & 0xFF);
                m.put("sa32_compAddr", HandlerUtils.readIntLE(buf));
                long status = buf.getInt() & 0xFFFFFFFFL;
                m.put("sa32_status", status);
                m.put("sa32_status_hex", String.format("0x%08X", status));
                int analogCount = buf.get() & 0xFF;
                m.put("sa32_analogCount", analogCount);

                List<Map<String, Object>> analogs = new ArrayList<>();
                for (int j = 0; j < analogCount && buf.remaining() >= 3; j++) {
                    Map<String, Object> a = new LinkedHashMap<>();
                    int aType = buf.get() & 0xFF;
                    short aVal = buf.getShort();
                    a.put("analog_type", aType);
                    a.put("analog_typeName", AnalogTypeDef.getName(aType));
                    a.put("analog_rawValue", (int) aVal);
                    a.put("analog_value", aVal * AnalogTypeDef.getScale(aType));
                    a.put("analog_unit", AnalogTypeDef.getUnit(aType));
                    analogs.add(a);
                }
                m.put("sa32_analogs", analogs);
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 147: 上报多通道设备各个通道的部件类型 ====================
    // 系统类型(1) + 系统地址(1) + 部件类型(1) + 部件地址(4) + DI使能(4) + DO使能(4) + 通道数量(1) + [通道号(1)+部件类型(1)] × n
    public static class MultiChannelCompTypeHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_MULTI_CHAN_COMP_TYPE; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 15; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("mct_systemType", buf.get() & 0xFF);
                m.put("mct_systemAddr", buf.get() & 0xFF);
                m.put("mct_compType", buf.get() & 0xFF);
                m.put("mct_compAddr", HandlerUtils.readIntLE(buf));
                long diEnable = buf.getInt() & 0xFFFFFFFFL;
                long doEnable = buf.getInt() & 0xFFFFFFFFL;
                m.put("mct_diEnable", diEnable);
                m.put("mct_diEnable_hex", String.format("0x%08X", diEnable));
                m.put("mct_doEnable", doEnable);
                m.put("mct_doEnable_hex", String.format("0x%08X", doEnable));
                int chanCount = buf.get() & 0xFF;
                m.put("mct_channelCount", chanCount);

                List<Map<String, Object>> channels = new ArrayList<>();
                for (int j = 0; j < chanCount && buf.remaining() >= 2; j++) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("channelNo", buf.get() & 0xFF);
                    c.put("compType", buf.get() & 0xFF);
                    channels.add(c);
                }
                m.put("mct_channels", channels);
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 148: 上报多通道设备各个通道数据 ====================
    // 系统类型(1) + 系统地址(1) + 部件类型(1) + 部件地址(4) + DI状态(4) + DO状态(4) + 通道数量(1)
    // + [通道号(1)+部件类型(1)+状态(2)+模拟量类型(1)+模拟量数量(1)+模拟量值(2)×n] × N
    public static class MultiChannelDataHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_MULTI_CHAN_DATA; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 15; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("mcd_systemType", buf.get() & 0xFF);
                m.put("mcd_systemAddr", buf.get() & 0xFF);
                m.put("mcd_compType", buf.get() & 0xFF);
                m.put("mcd_compAddr", HandlerUtils.readIntLE(buf));
                long diStatus = buf.getInt() & 0xFFFFFFFFL;
                long doStatus = buf.getInt() & 0xFFFFFFFFL;
                m.put("mcd_diStatus", diStatus);
                m.put("mcd_diStatus_hex", String.format("0x%08X", diStatus));
                m.put("mcd_doStatus", doStatus);
                m.put("mcd_doStatus_hex", String.format("0x%08X", doStatus));
                int chanCount = buf.get() & 0xFF;
                m.put("mcd_channelCount", chanCount);

                List<Map<String, Object>> channels = new ArrayList<>();
                for (int j = 0; j < chanCount && buf.remaining() >= 6; j++) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("channelNo", buf.get() & 0xFF);
                    c.put("compType", buf.get() & 0xFF);
                    int chanStatus = buf.getShort() & 0xFFFF;
                    c.put("status", chanStatus);
                    int analogType = buf.get() & 0xFF;
                    c.put("analogType", analogType);
                    int analogCount = buf.get() & 0xFF;
                    c.put("analogCount", analogCount);

                    List<Map<String, Object>> analogs = new ArrayList<>();
                    for (int k = 0; k < analogCount && buf.remaining() >= 2; k++) {
                        Map<String, Object> a = new LinkedHashMap<>();
                        short aVal = buf.getShort();
                        a.put("analog_rawValue", (int) aVal);
                        a.put("analog_value", aVal * AnalogTypeDef.getScale(analogType));
                        a.put("analog_unit", AnalogTypeDef.getUnit(analogType));
                        analogs.add(a);
                    }
                    c.put("analogs", analogs);
                    channels.add(c);
                }
                m.put("mcd_channels", channels);
                list.add(m);
            }
            return list;
        }
    }
}
