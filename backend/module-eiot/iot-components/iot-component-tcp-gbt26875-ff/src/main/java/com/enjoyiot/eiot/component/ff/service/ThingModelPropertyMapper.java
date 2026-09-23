package com.enjoyiot.eiot.component.ff.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 协议解析数据 → 物模型属性标识符 映射器
 * <p>
 * 将 TypeHandler 输出的原始 key 映射到物模型定义的标识符，
 * 按 alarm_* / telemetry_* / info_* / diag_* 四组前缀归类。
 */
public final class ThingModelPropertyMapper {

    private ThingModelPropertyMapper() {}

    // ==================== TLD 1 系统状态位 → alarm_* ====================
    private static final Map<String, String> SYSTEM_STATUS_MAPPING = new HashMap<>();
    static {
        SYSTEM_STATUS_MAPPING.put("sys_normalMode",       "alarm_normal_monitor");
        SYSTEM_STATUS_MAPPING.put("sys_fireAlarm",        "alarm_fire");
        SYSTEM_STATUS_MAPPING.put("sys_fault",            "alarm_fault");
        SYSTEM_STATUS_MAPPING.put("sys_shielded",         "alarm_shield");
        SYSTEM_STATUS_MAPPING.put("sys_supervised",       "alarm_supervised");
        SYSTEM_STATUS_MAPPING.put("sys_activated",        "alarm_activated");
        SYSTEM_STATUS_MAPPING.put("sys_feedback",         "alarm_feedback");
        SYSTEM_STATUS_MAPPING.put("sys_delay",            "alarm_delay");
        SYSTEM_STATUS_MAPPING.put("sys_mainPowerFault",   "alarm_main_power");
        SYSTEM_STATUS_MAPPING.put("sys_backupPowerFault", "alarm_backup_power");
        SYSTEM_STATUS_MAPPING.put("sys_busFault",         "alarm_bus_fault");
        SYSTEM_STATUS_MAPPING.put("sys_manualMode",       "alarm_manual_mode");
        SYSTEM_STATUS_MAPPING.put("sys_configChanged",    "alarm_config_changed");
        SYSTEM_STATUS_MAPPING.put("sys_statusTime",       "alarm_status_time");
    }

    // ==================== TLD 21 装置状态 → alarm_* / telemetry_* ====================
    private static final Map<String, String> DEVICE_STATUS_MAPPING = new HashMap<>();
    static {
        DEVICE_STATUS_MAPPING.put("dev_fire_alarm",         "alarm_device_fire");
        DEVICE_STATUS_MAPPING.put("dev_fault",              "alarm_device_fault");
        DEVICE_STATUS_MAPPING.put("dev_main_power_fault",   "alarm_device_main_power");
        DEVICE_STATUS_MAPPING.put("dev_backup_power_fault", "alarm_device_backup_power");
        DEVICE_STATUS_MAPPING.put("dev_comm_fault",         "alarm_device_comm");
        DEVICE_STATUS_MAPPING.put("dev_line_fault",         "alarm_device_line");
        DEVICE_STATUS_MAPPING.put("dev_normal_monitor",     "alarm_normal_monitor");
        DEVICE_STATUS_MAPPING.put("dev_status_time",        "alarm_status_time");
    }

    // ==================== TLD 4/5/6/7/8/24/25/26/28 配置信息 → info_* ====================
    private static final Map<String, String> DEVICE_INFO_MAPPING = new HashMap<>();
    static {
        // TLD 25 装置版本
        DEVICE_INFO_MAPPING.put("dev_main_ver",    "info_firmware_ver_major");
        DEVICE_INFO_MAPPING.put("dev_sub_ver",     "info_firmware_ver_minor");
        // TLD 26 装置配置
        DEVICE_INFO_MAPPING.put("dev_config_desc", "info_config_desc");
        // TLD 28 装置时间
        DEVICE_INFO_MAPPING.put("device_time",     "info_system_time");
    }

    // ==================== 映射方法 ====================

    /**
     * TLD 1 系统状态 → alarm_* 属性
     */
    public static Map<String, Object> mapSystemStatus(Map<String, Object> entry) {
        Map<String, Object> result = new LinkedHashMap<>();
        entry.forEach((key, value) -> {
            if ("gbtType".equals(key)) return;
            String mapped = SYSTEM_STATUS_MAPPING.get(key);
            if (mapped != null) {
                result.put(mapped, value);
            }
        });
        return result;
    }

    /**
     * TLD 21 装置状态 → alarm_* 属性
     */
    public static Map<String, Object> mapDeviceStatus(Map<String, Object> entry) {
        Map<String, Object> result = new LinkedHashMap<>();
        entry.forEach((key, value) -> {
            if ("gbtType".equals(key)) return;
            String mapped = DEVICE_STATUS_MAPPING.get(key);
            if (mapped != null) {
                result.put(mapped, value);
            }
        });
        return result;
    }

    /**
     * TLD 4/5/6/7/8/24/25/26/28 → info_* 属性
     */
    public static Map<String, Object> mapDeviceInfo(Map<String, Object> entry) {
        Map<String, Object> result = new LinkedHashMap<>();
        entry.forEach((key, value) -> {
            if ("gbtType".equals(key)) return;
            String mapped = DEVICE_INFO_MAPPING.get(key);
            if (mapped != null) {
                result.put(mapped, value);
            } else if (!isInternalKey(key)) {
                // 未显式映射的 key → 加 info_ 前缀
                result.put("info_" + key.toLowerCase(), value);
            }
        });
        return result;
    }

    /**
     * TLD 138/140/142/143 → 混合 alarm_* + telemetry_*
     */
    public static Map<String, Object> mapExtendedData(Map<String, Object> entry, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();
        entry.forEach((key, value) -> {
            if ("gbtType".equals(key)) return;
            // 状态字段 → alarm_
            if (key.endsWith("_status") || key.equals("dsa_status") || key.equals("sa32_status") || key.equals("mc_status")) {
                result.put("alarm_extended_raw", value);
                return;
            }
            // 嵌套列表保留为 JSON
            if (value instanceof java.util.List) {
                result.put(prefix + "_" + key, value);
                return;
            }
            // 普通字段 → telemetry_
            if (key.contains("analog") || key.contains("energy")) {
                result.put("telemetry_" + key, value);
            } else {
                result.put(prefix + "_" + key, value);
            }
        });
        return result;
    }

    // Filter internal keys: sys_, comp_, oper_, ver_, sysConf_, compConf_, sysTime_, devOper_,
    // analog_, energy_, mc_, dsa_, sa32_, mct_, mcd_ etc.
    private static boolean isInternalKey(String key) {
        return key.startsWith("sys_") || key.startsWith("comp_") || key.startsWith("oper_")
                || key.startsWith("ver_") || key.startsWith("sysConf_") || key.startsWith("compConf_")
                || key.startsWith("sysTime_") || key.startsWith("devOper_")
                || key.startsWith("analog_") || key.startsWith("energy_")
                || key.startsWith("mc_") || key.startsWith("dsa_") || key.startsWith("sa32_")
                || key.startsWith("mct_") || key.startsWith("mcd_");
    }
}
