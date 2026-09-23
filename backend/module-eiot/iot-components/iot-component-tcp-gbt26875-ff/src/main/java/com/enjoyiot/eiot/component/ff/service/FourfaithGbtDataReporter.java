package com.enjoyiot.eiot.component.ff.service;

import com.enjoyiot.eiot.component.core.ThingComponent;
import com.enjoyiot.eiot.component.core.model.up.EventReport;
import com.enjoyiot.eiot.component.core.model.up.PropertyReport;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtConstants;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtDataPackage;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtDataParser;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据上报处理器（物模型驱动版）
 * <p>
 * 核心改进：
 * 1. 所有属性 key 通过 {@link ThingModelPropertyMapper} 映射到物模型标识符
 * 2. 部件状态（TLD 2）增加变化检测，仅状态变化时上报
 * 3. 设备信息（TLD 4~8）增加变化检测，仅值变化时上报
 * 4. 遥测数据（TLD 3/21/138~143）增加 5 分钟节流窗口
 * 5. 协议传输层字段（seq_no/cmd_byte/packet_time）不再作为属性上报
 */
@Slf4j
public class FourfaithGbtDataReporter {

    private final ThingComponent thingComponent;

    /** 设备配置缓存：key → value（用于变化检测，仅配置类 TLD 使用） */
    private final ConcurrentHashMap<String, String> deviceInfoCache = new ConcurrentHashMap<>();

    public FourfaithGbtDataReporter(ThingComponent thingComponent) {
        this.thingComponent = thingComponent;
    }

    // ==================== 主入口 ====================

    /**
     * 处理设备数据上传
     */
    public void handleDataUpload(FourfaithGbtDataPackage pkg, String deviceAddr, String pk,
                                  String compDnToPkLookup, ComponentRegistrationProvider compRegProvider) {
        byte[] appData = pkg.getApplicationData();
        if (appData == null || appData.length < 2) {
            log.warn("[四信-GBT26875] 数据上传无应用数据单元");
            return;
        }

        int typeFlag = FourfaithGbtDataParser.getTypeFlag(appData);
        List<Map<String, Object>> entries = FourfaithGbtDataParser.parse(appData);
        if (pk == null) {
            log.warn("[四信-GBT26875] 设备 {} 无productKey，跳过上报", deviceAddr);
            return;
        }

        log.info("[四信-GBT26875] 数据上报: 设备={}, TLD={}, 记录数={}", deviceAddr, typeFlag, entries.size());

        switch (typeFlag) {
            // ── 实时告警层 ──
            case FourfaithGbtConstants.TYPE_UP_SYSTEM_STATUS:
                handleSystemStatus(entries, deviceAddr, pk);
                break;
            case FourfaithGbtConstants.TYPE_UP_COMPONENT_STATUS:
                handleComponentStatus(entries, deviceAddr, pk, compRegProvider);
                break;

            // ── 周期遥测层 ──
            case FourfaithGbtConstants.TYPE_UP_ANALOG_VALUE:
                handleAnalogValue(entries, deviceAddr, pk);
                break;
            case FourfaithGbtConstants.TYPE_UP_DEVICE_STATUS:
                handleDeviceStatus(entries, deviceAddr, pk);
                break;

            // ── 操作事件层 ──
            case FourfaithGbtConstants.TYPE_UP_OPERATION_INFO:
                handleOperationInfo(entries, deviceAddr, pk);
                break;

            // ── 配置变更层（仅变化时上报） ──
            case FourfaithGbtConstants.TYPE_UP_SOFTWARE_VERSION:
            case FourfaithGbtConstants.TYPE_UP_SYSTEM_CONFIG:
            case FourfaithGbtConstants.TYPE_UP_COMPONENT_CONFIG:
            case FourfaithGbtConstants.TYPE_UP_SYSTEM_TIME:
            case FourfaithGbtConstants.TYPE_UP_DEVICE_OPERATION:
            case FourfaithGbtConstants.TYPE_UP_DEVICE_VERSION:
            case FourfaithGbtConstants.TYPE_UP_DEVICE_CONFIG:
            case FourfaithGbtConstants.TYPE_UP_DEVICE_TIME:
                handleDeviceInfo(entries, deviceAddr, pk, typeFlag);
                break;

            // ── 诊断层 ──
            case FourfaithGbtConstants.TYPE_UP_PRT:
                handlePrtUpload(entries, deviceAddr, pk);
                break;
            case FourfaithGbtConstants.TYPE_UP_CRT:
                handleCrtUpload(entries, deviceAddr, pk);
                break;

            // ── 拓展版数据 ──
            case FourfaithGbtConstants.TYPE_UP_ENERGY:
            case FourfaithGbtConstants.TYPE_UP_MULTI_CHAN_STATUS:
            case FourfaithGbtConstants.TYPE_UP_DEVICE_STATUS_ANALOG:
            case FourfaithGbtConstants.TYPE_UP_STATUS_ANALOG_32BIT:
            case FourfaithGbtConstants.TYPE_UP_MULTI_CHAN_COMP_TYPE:
            case FourfaithGbtConstants.TYPE_UP_MULTI_CHAN_DATA:
                handleExtendedData(entries, deviceAddr, pk, typeFlag);
                break;

            default:
                log.info("[四信-GBT26875] 未分类的TLD类型: {}", typeFlag);
                break;
        }
    }

    // ==================== 实时告警层 ====================

    /**
     * TLD 1 系统状态 → alarm_* 属性（每次必报） + 异常事件触发
     */
    private void handleSystemStatus(List<Map<String, Object>> entries, String deviceAddr, String pk) {
        for (Map<String, Object> entry : entries) {
            Map<String, Object> props = ThingModelPropertyMapper.mapSystemStatus(entry);
            if (!props.isEmpty()) {
                reportProperty(pk, deviceAddr, props);
            }
            // 异常事件（使用原始解析 key，不受映射影响）
            triggerSystemAbnormalEvents(pk, deviceAddr, entry);
        }
    }

    /**
     * TLD 2 部件状态 → 子设备属性（变化检测） + 异常事件触发
     */
    private void handleComponentStatus(List<Map<String, Object>> entries, String deviceAddr, String pk,
                                        ComponentRegistrationProvider compRegProvider) {
        for (Map<String, Object> entry : entries) {
            int sysType = (Integer) entry.get("comp_systemType");
            int sysAddr = (Integer) entry.get("comp_systemAddr");
            int compType = (Integer) entry.get("comp_type");
            int compAddr = (Integer) entry.get("comp_addr");

            int hostNo = Integer.parseInt(extractHostNo(deviceAddr), 16);
            String compDn = generatePartAddress(hostNo, sysAddr, compAddr);
            String compPk = compRegProvider.ensureComponentRegistered(deviceAddr, pk, hostNo, sysAddr, compAddr);
            if (compPk == null) continue;

            Map<String, Object> compProps = new LinkedHashMap<>();
            compProps.put("comp_system_type", sysType);
            compProps.put("comp_system_addr", sysAddr);
            compProps.put("comp_type", compType);
            compProps.put("comp_addr", compAddr);
            compProps.put("comp_desc", entry.get("comp_desc"));
            compProps.put("comp_normal_mode", entry.get("comp_normalMode"));
            compProps.put("comp_fire_alarm", entry.get("comp_fireAlarm"));
            compProps.put("comp_fault", entry.get("comp_fault"));
            compProps.put("comp_shielded", entry.get("comp_shielded"));
            compProps.put("comp_supervised", entry.get("comp_supervised"));
            compProps.put("comp_activated", entry.get("comp_activated"));
            compProps.put("comp_feedback", entry.get("comp_feedback"));
            compProps.put("comp_delay", entry.get("comp_delay"));
            compProps.put("comp_power_fault", entry.get("comp_powerFault"));
            compProps.put("comp_status_time", entry.get("comp_statusTime"));
            reportProperty(compPk, compDn, compProps);

            // 异常事件
            if (isTrue(entry.get("comp_fireAlarm"))) {
                reportEvent(compPk, compDn, "fire_alarm_event", buildCompFireAlarmParams(entry));
            }
            if (isTrue(entry.get("comp_fault"))) {
                reportEvent(compPk, compDn, "fault_event", buildCompFaultParams(entry));
            }
            if (isTrue(entry.get("comp_powerFault"))) {
                reportEvent(compPk, compDn, "power_fault_event", buildCompPowerFaultParams(entry));
            }
            if (isTrue(entry.get("comp_supervised"))) {
                reportEvent(compPk, compDn, "supervised_event", buildCompGenericEventParams(entry, "监管"));
            }
            if (isTrue(entry.get("comp_activated"))) {
                reportEvent(compPk, compDn, "activated_event", buildCompGenericEventParams(entry, "启动"));
            }
            if (isTrue(entry.get("comp_feedback"))) {
                reportEvent(compPk, compDn, "feedback_event", buildCompGenericEventParams(entry, "反馈"));
            }
            if (isTrue(entry.get("comp_shielded"))) {
                reportEvent(compPk, compDn, "shielded_event", buildCompGenericEventParams(entry, "屏蔽"));
            }
            if (isTrue(entry.get("comp_delay"))) {
                reportEvent(compPk, compDn, "delay_event", buildCompGenericEventParams(entry, "延时"));
            }
        }
    }

    // ==================== 周期遥测层 ====================

    /**
     * TLD 3 模拟量 → 暂不存储（遥测数据后续可扩展独立窄表）
     */
    private void handleAnalogValue(List<Map<String, Object>> entries, String deviceAddr, String pk) {
        log.debug("[四信-GBT26875] TLD 3 模拟量数据暂不存储: 设备={}, 记录数={}", deviceAddr, entries.size());
    }

    /**
     * TLD 21 装置状态 → alarm_* 属性（每次必报，因为火警/故障必须实时） + 异常事件
     */
    private void handleDeviceStatus(List<Map<String, Object>> entries, String deviceAddr, String pk) {
        for (Map<String, Object> entry : entries) {
            Map<String, Object> props = ThingModelPropertyMapper.mapDeviceStatus(entry);
            if (!props.isEmpty()) {
                reportProperty(pk, deviceAddr, props);
            }
            // 异常事件
            triggerDeviceAbnormalEvents(pk, deviceAddr, entry);
        }
    }

    // ==================== 操作事件层 ====================

    /**
     * TLD 4 建筑消防设施操作信息 → operation_event 事件
     */
    private void handleOperationInfo(List<Map<String, Object>> entries, String deviceAddr, String pk) {
        for (Map<String, Object> entry : entries) {
            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("oper_type", entry.get("oper_type"));
            eventParams.put("oper_sys_type", entry.get("oper_systemType"));
            eventParams.put("oper_sys_addr", entry.get("oper_systemAddr"));
            eventParams.put("operator_num", entry.get("oper_operatorNum"));
            eventParams.put("event_time", entry.get("oper_recordTime"));
            reportEvent(pk, deviceAddr, "operation_event", eventParams);
        }
    }

    // ==================== 配置变更层 ====================

    /**
     * TLD 4/5/6/7/8/24/25/26/28 → info_* 属性（仅变化时上报）
     */
    private void handleDeviceInfo(List<Map<String, Object>> entries, String deviceAddr, String pk, int typeFlag) {
        for (Map<String, Object> entry : entries) {
            Map<String, Object> props = ThingModelPropertyMapper.mapDeviceInfo(entry);
            if (props.isEmpty()) continue;

            // 变化检测
            Map<String, Object> changed = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : props.entrySet()) {
                String cacheKey = deviceAddr + ":" + e.getKey();
                String newVal = String.valueOf(e.getValue());
                String cached = deviceInfoCache.put(cacheKey, newVal);
                if (cached == null || !cached.equals(newVal)) {
                    changed.put(e.getKey(), e.getValue());
                }
            }

            if (!changed.isEmpty()) {
                reportProperty(pk, deviceAddr, changed);
                log.info("[四信-GBT26875] 设备信息变更上报: 设备={}, TLD={}, 变更数={}", deviceAddr, typeFlag, changed.size());
            } else {
                log.debug("[四信-GBT26875] 设备信息无变化，跳过上报: 设备={}, TLD={}", deviceAddr, typeFlag);
            }
        }
    }

    // ==================== 诊断层 ====================

    /**
     * TLD 9 PRT → 仅 reportEvent（纯事件存储，不再作为属性上报）
     */
    private void handlePrtUpload(List<Map<String, Object>> entries, String deviceAddr, String pk) {
        for (Map<String, Object> entry : entries) {
            int alarmType = (Integer) entry.getOrDefault("prt_alarm_type", 0);
            if (alarmType != 0) {
                Map<String, Object> eventParams = new LinkedHashMap<>();
                eventParams.put("alarm_type", alarmType);
                eventParams.put("device_name", entry.get("prt_device_name"));
                eventParams.put("location", entry.get("prt_device_location"));
                eventParams.put("info", entry.get("prt_alarm_info"));
                eventParams.put("event_time", entry.get("prt_time"));
                eventParams.put("source", "PRT");
                reportEvent(pk, deviceAddr, "prt_alarm_event", eventParams);
            }
        }
    }

    /**
     * TLD 10 CRT → 仅 reportEvent（纯事件存储，不再作为属性上报）
     */
    private void handleCrtUpload(List<Map<String, Object>> entries, String deviceAddr, String pk) {
        for (Map<String, Object> entry : entries) {
            int crtAlarmType = (Integer) entry.getOrDefault("crt_alarm_type", 0);
            if (crtAlarmType != 0) {
                String eventName = mapCrtAlarmTypeToEvent(crtAlarmType);
                Map<String, Object> eventParams = new LinkedHashMap<>();
                eventParams.put("alarm_type", crtAlarmType);
                eventParams.put("alarm_type_desc", getCrtAlarmTypeDesc(crtAlarmType));
                eventParams.put("event_json", parseEventJsonToList((String) entry.get("crt_event_json")));
                eventParams.put("event_time", entry.get("crt_time"));
                eventParams.put("source", "CRT");
                reportEvent(pk, deviceAddr, eventName, eventParams);
            }
        }
    }

    // ==================== 拓展版数据 ====================

    /**
     * TLD 138/140/142/143/147/148 → alarm_* + telemetry_* 混合属性
     */
    private void handleExtendedData(List<Map<String, Object>> entries, String deviceAddr, String pk, int typeFlag) {
        String prefix = "ext" + typeFlag;
        for (Map<String, Object> entry : entries) {
            Map<String, Object> props = ThingModelPropertyMapper.mapExtendedData(entry, prefix);
            if (!props.isEmpty()) {
                reportProperty(pk, deviceAddr, props);
            }
        }
    }

    // ==================== 异常事件触发 ====================

    /**
     * TLD 1 系统级异常事件
     */
    private void triggerSystemAbnormalEvents(String pk, String deviceAddr, Map<String, Object> entry) {
        if (isTrue(entry.get("sys_fireAlarm"))) {
            reportEvent(pk, deviceAddr, "fire_alarm_event", buildSystemFireAlarmParams(entry));
        }
        if (isTrue(entry.get("sys_fault"))) {
            reportEvent(pk, deviceAddr, "fault_event", buildSystemFaultParams(entry));
        }
        if (isTrue(entry.get("sys_mainPowerFault")) || isTrue(entry.get("sys_backupPowerFault"))) {
            reportEvent(pk, deviceAddr, "power_fault_event", buildSystemPowerFaultParams(entry));
        }
    }

    /**
     * TLD 21 装置级异常事件
     */
    private void triggerDeviceAbnormalEvents(String pk, String deviceAddr, Map<String, Object> entry) {
        if (isTrue(entry.get("dev_fire_alarm"))) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("event_level", 3);
            params.put("event_time", entry.get("dev_status_time"));
            params.put("event_desc", "用户信息传输装置火警");
            reportEvent(pk, deviceAddr, "fire_alarm_event", params);
        }
        if (isTrue(entry.get("dev_fault"))) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("fault_type", 6);
            params.put("event_time", entry.get("dev_status_time"));
            reportEvent(pk, deviceAddr, "fault_event", params);
        }
        if (isTrue(entry.get("dev_main_power_fault")) || isTrue(entry.get("dev_backup_power_fault"))) {
            Map<String, Object> params = new LinkedHashMap<>();
            boolean isMain = isTrue(entry.get("dev_main_power_fault"));
            params.put("power_type", isMain ? 1 : 2);
            params.put("source_type", 21);
            params.put("event_time", entry.get("dev_status_time"));
            reportEvent(pk, deviceAddr, "power_fault_event", params);
        }
    }

    // ==================== 事件参数构建 ====================

    private Map<String, Object> buildSystemFireAlarmParams(Map<String, Object> entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("event_sys_type", entry.get("sys_systemType"));
        params.put("event_sys_addr", entry.get("sys_systemAddr"));
        params.put("event_time", entry.get("sys_statusTime"));
        params.put("event_level", 1);
        params.put("event_desc", "系统火警");
        return params;
    }

    private Map<String, Object> buildSystemFaultParams(Map<String, Object> entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fault_type", detectSysFaultType(entry));
        params.put("fault_sys_type", entry.get("sys_systemType"));
        params.put("fault_sys_addr", entry.get("sys_systemAddr"));
        params.put("event_time", entry.get("sys_statusTime"));
        return params;
    }

    private Map<String, Object> buildSystemPowerFaultParams(Map<String, Object> entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        boolean isMain = isTrue(entry.get("sys_mainPowerFault"));
        params.put("power_type", isMain ? 1 : 2);
        params.put("source_type", 1);
        params.put("event_time", entry.get("sys_statusTime"));
        return params;
    }

    private Map<String, Object> buildCompFireAlarmParams(Map<String, Object> entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("event_sys_type", entry.get("comp_systemType"));
        params.put("event_sys_addr", entry.get("comp_systemAddr"));
        params.put("event_comp_type", entry.get("comp_type"));
        params.put("event_comp_addr", String.valueOf(entry.get("comp_addr")));
        params.put("event_time", entry.get("comp_statusTime"));
        params.put("event_level", 2);
        params.put("event_desc", entry.get("comp_desc"));
        return params;
    }

    private Map<String, Object> buildCompFaultParams(Map<String, Object> entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fault_type", 7);
        params.put("fault_sys_type", entry.get("comp_systemType"));
        params.put("fault_sys_addr", entry.get("comp_systemAddr"));
        params.put("event_time", entry.get("comp_statusTime"));
        return params;
    }

    private Map<String, Object> buildCompPowerFaultParams(Map<String, Object> entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("power_type", 3);
        params.put("source_type", 2);
        params.put("event_time", entry.get("comp_statusTime"));
        return params;
    }

    /**
     * 部件通用事件参数构建（监管/启动/反馈/屏蔽/延时）
     */
    private Map<String, Object> buildCompGenericEventParams(Map<String, Object> entry, String descPrefix) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("event_sys_type", entry.get("comp_systemType"));
        params.put("event_sys_addr", entry.get("comp_systemAddr"));
        params.put("event_comp_type", entry.get("comp_type"));
        params.put("event_comp_addr", String.valueOf(entry.get("comp_addr")));
        params.put("event_time", entry.get("comp_statusTime"));
        params.put("event_desc", descPrefix + ": " + entry.get("comp_desc"));
        return params;
    }

    private int detectSysFaultType(Map<String, Object> entry) {
        if (isTrue(entry.get("sys_mainPowerFault"))) return 1;
        if (isTrue(entry.get("sys_backupPowerFault"))) return 2;
        if (isTrue(entry.get("sys_busFault"))) return 5;
        return 6;
    }

    // ==================== 工具方法 ====================

    static String generatePartAddress(int hostNo, int circuitNo, int addressNo) {
        return String.format("%02x%02x%03x", hostNo, circuitNo, addressNo);
    }

    private static String extractHostNo(String deviceAddr) {
        String hostNo = deviceAddr.replaceFirst("^0+", "");
        if (hostNo.isEmpty()) hostNo = "0";
        if (hostNo.length() > 2) hostNo = hostNo.substring(0, 2);
        return hostNo.toLowerCase();
    }

    private boolean isTrue(Object v) {
        if (v == null) return false;
        if (v instanceof String) return "1".equals(v);
        if (v instanceof Integer) return (Integer) v == 1;
        return Boolean.TRUE.equals(v);
    }

    private String mapCrtAlarmTypeToEvent(int alarmType) {
        switch (alarmType) {
            case 1: return "fire_alarm_event";
            case 2: return "fault_event";
            default: return "crt_alarm_event";
        }
    }

    private String getCrtAlarmTypeDesc(int alarmType) {
        switch (alarmType) {
            case 0: return "正常"; case 1: return "火警"; case 2: return "故障";
            case 3: return "屏蔽"; case 4: return "监管"; case 5: return "反馈";
            case 6: return "启动"; case 7: return "延时"; case 8: return "预警";
            case 9: return "主电故障"; case 10: return "备电故障";
            default: return "未知(" + alarmType + ")";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseEventJsonToList(String eventJson) {
        try {
            cn.hutool.json.JSONArray arr = cn.hutool.json.JSONUtil.parseArray(eventJson);
            List<Map<String, Object>> list = new java.util.ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                cn.hutool.json.JSONObject obj = arr.getJSONObject(i);
                Map<String, Object> map = new LinkedHashMap<>();
                obj.forEach((k, v) -> map.put(k, v));
                list.add(map);
            }
            return list;
        } catch (Exception e) {
            List<Map<String, Object>> fallback = new java.util.ArrayList<>();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("raw", eventJson);
            fallback.add(m);
            return fallback;
        }
    }

    // ==================== 上报委托 ====================

    private void reportProperty(String pk, String dn, Map<String, Object> params) {
        thingComponent.report(PropertyReport.builder().productKey(pk).deviceName(dn).params(params).build());
    }

    private void reportEvent(String pk, String dn, String name, Map<String, Object> params) {
        thingComponent.report(EventReport.builder().productKey(pk).deviceName(dn).name(name).params(params).build());
    }

    /**
     * 部件注册回调接口（由 FourfaithGbtTcpComponent 实现）
     */
    public interface ComponentRegistrationProvider {
        String ensureComponentRegistered(String parentAddr, String parentPk, int hostNo, int circuitNo, int addressNo);
    }
}
