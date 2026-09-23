package com.enjoyiot.eiot.component.ff.parser;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟量类型定义枚举（对应协议表8.1.3.3 模拟量定义-AVD）
 * <p>
 * 集中管理所有模拟量的类型值、名称、单位、分辨率，替代分散的 switch-case。
 * 新增模拟量类型只需在此枚举中添加一行。
 */
@Getter
public enum AnalogTypeDef {

    // ==================== 基础模拟量 (1~12) ====================
    EVENT_COUNT(1, "事件计数", "件", 1.0),
    HEIGHT(2, "高度", "m", 0.01),
    TEMPERATURE(3, "温度", "℃", 0.1),
    PRESSURE_MPA(4, "压力(MPa)", "MPa", 0.1),
    PRESSURE_KPA(5, "压力(kPa)", "kPa", 0.1),
    GAS_CONCENTRATION(6, "气体浓度", "%LEL", 0.1),
    TIME(7, "时间", "s", 1.0),
    VOLTAGE(8, "电压", "V", 0.1),
    CURRENT(9, "电流", "A", 0.1),
    FLOW_LS(10, "流量(L/s)", "L/s", 0.1),
    FLOW_M3MIN(11, "流量(m³/min)", "m³/min", 0.1),
    WIND_SPEED(12, "风速", "m/s", 1.0),

    // ==================== 信号/电池 (128~136) ====================
    SIGNAL_STRENGTH(128, "信号强度", "dBm", 1.0),
    BATTERY_CAPACITY(129, "电量", "mAh", 1.0),
    RESIDUAL_CURRENT_I1(130, "剩余电流i1", "mA", 1.0),
    RESIDUAL_CURRENT_I2(131, "剩余电流i2", "mA", 1.0),
    RESIDUAL_CURRENT_I3(132, "剩余电流i3", "mA", 1.0),
    RESIDUAL_CURRENT_I4(133, "剩余电流i4", "mA", 1.0),
    ADC_RAW_VALUE(135, "液压/液位ADC原始值", "", 1.0),
    BATTERY_VOLTAGE(136, "电池电压", "mV", 1.0),

    // ==================== 电气火灾-剩余电流 (137~140) ====================
    RESIDUAL_CURRENT_I5(137, "剩余电流i5", "mA", 1.0),
    RESIDUAL_CURRENT_I6(138, "剩余电流i6", "mA", 1.0),
    RESIDUAL_CURRENT_I7(139, "剩余电流i7", "mA", 1.0),
    RESIDUAL_CURRENT_I8(140, "剩余电流i8", "mA", 1.0),

    // ==================== 电气火灾-温度 (141~147) ====================
    TEMPERATURE_T1(141, "温度T1", "℃", 0.1),
    TEMPERATURE_T2(142, "温度T2", "℃", 0.1),
    TEMPERATURE_T3(143, "温度T3", "℃", 0.1),
    TEMPERATURE_T4(144, "温度T4", "℃", 0.1),
    TEMPERATURE_T5(145, "温度T5", "℃", 0.1),
    TEMPERATURE_T6(146, "温度T6", "℃", 0.1),
    TEMPERATURE_T7(147, "温度T7", "℃", 0.1),
    TEMPERATURE_T8(148, "温度T8", "℃", 0.1),

    // ==================== 电气火灾-三相电压/电流 (148~153) ====================
    UA(148, "Ua电压", "V", 0.1),
    UB(149, "Ub电压", "V", 0.1),
    UC(150, "Uc电压", "V", 0.1),
    IA(151, "Ia电流", "A", 0.01),
    IB(152, "Ib电流", "A", 0.01),
    IC(153, "Ic电流", "A", 0.01),

    // ==================== 液位 (154) ====================
    LIQUID_LEVEL(154, "液位", "cm", 1.0),

    // ==================== 电气火灾-告警/预警限值 (155~176) ====================
    IA_ALARM_LIMIT(155, "Ia告警限值", "A", 0.01),
    IA_WARNING_LIMIT(156, "Ia预警限值", "A", 0.01),
    IB_ALARM_LIMIT(157, "Ib告警限值", "A", 0.01),
    IB_WARNING_LIMIT(158, "Ib预警限值", "A", 0.01),
    IC_ALARM_LIMIT(159, "Ic告警限值", "A", 0.01),
    IC_WARNING_LIMIT(160, "Ic预警限值", "A", 0.01),
    IN_ALARM_LIMIT(161, "In告警限值", "mA", 1.0),
    IN_WARNING_LIMIT(162, "In预警限值", "mA", 1.0),
    UA_ALARM_LIMIT(163, "Ua告警限值", "V", 0.1),
    UA_WARNING_LIMIT(164, "Ua预警限值", "V", 0.1),
    UB_ALARM_LIMIT(165, "Ub告警限值", "V", 0.1),
    UB_WARNING_LIMIT(166, "Ub预警限值", "V", 0.1),
    UC_ALARM_LIMIT(167, "Uc告警限值", "V", 0.1),
    UC_WARNING_LIMIT(168, "Uc预警限值", "V", 0.1),
    T1_ALARM_LIMIT(169, "T1告警限值", "℃", 0.1),
    T1_WARNING_LIMIT(170, "T1预警限值", "℃", 0.1),
    T2_ALARM_LIMIT(171, "T2告警限值", "℃", 0.1),
    T2_WARNING_LIMIT(172, "T2预警限值", "℃", 0.1),
    T3_ALARM_LIMIT(173, "T3告警限值", "℃", 0.1),
    T3_WARNING_LIMIT(174, "T3预警限值", "℃", 0.1),
    T4_ALARM_LIMIT(175, "T4告警限值", "℃", 0.1),
    T4_WARNING_LIMIT(176, "T4预警限值", "℃", 0.1),

    // ==================== 传感器状态 (177~179) ====================
    RESIDUAL_CURRENT_SENSOR_STATUS(177, "剩余电流传感器状态", "", 1.0),
    TEMPERATURE_SENSOR_STATUS(178, "温度传感器状态", "", 1.0),
    THREE_PHASE_STATUS(179, "三相电压/电流状态", "", 1.0),

    // ==================== 网络信息 (180~183) ====================
    CELL_PCI(180, "蜂窝网络物理小区标识", "", 1.0),
    CELL_LOCATION_HIGH(181, "蜂窝网络小区位置信息高16bit", "", 1.0),
    CELL_LOCATION_LOW(182, "蜂窝网络小区位置信息低16bit", "", 1.0),
    BACKUP_BATTERY_VOLTAGE(183, "备用电池电压", "mV", 1.0),

    // ==================== 市政消防栓 (184~204) ====================
    VERTICAL_ANGLE(184, "垂直角度值", "度", 1.0),
    DEVICE_SUB_TEMP(185, "设备副温度", "℃", 0.1),
    LIGHT_VALUE(186, "光感值", "", 1.0),
    WATER_FLOW_SPEED_HIGH(187, "水流量速度高16bit", "ml/min", 1.0),
    WATER_FLOW_SPEED_LOW(188, "水流量速度低16bit", "ml/min", 1.0),
    WATER_FLOW_TOTAL_HIGH(189, "水流流量统计高16bit", "ml", 1.0),
    WATER_FLOW_TOTAL_LOW(190, "水流流量统计低16bit", "ml", 1.0),
    SHORT_RANGE_LOCAL_RSSI(191, "短距离无线通信本机信号强度", "dBm", 1.0),
    SHORT_RANGE_REMOTE_RSSI(192, "短距离无线通信对象机信号强度", "dBm", 1.0),
    MAIN_LOW_VOLTAGE_THRESHOLD(193, "主电低电压报警阈值", "mV", 1.0),
    BACKUP_LOW_VOLTAGE_THRESHOLD(194, "备电低电量报警阈值", "mV", 1.0),
    TILT_ALARM_THRESHOLD(195, "倾斜报警阈值", "度", 1.0),
    PRESSURE_HIGH_THRESHOLD(196, "压力过高报警阈值", "kPa", 1.0),
    PRESSURE_LOW_THRESHOLD(197, "压力过低报警阈值", "kPa", 1.0),
    PRESSURE_CHANGE_THRESHOLD(198, "压力变化报警阈值", "kPa", 1.0),
    BURIED_ALARM_THRESHOLD(199, "掩埋报警阈值", "", 1.0),
    COLLISION_THRESHOLD(200, "碰撞报警阈值", "mg", 1.0),
    WATER_OVER_LIMIT_HIGH(201, "出水超量报警阈值高16bit", "ml", 1.0),
    WATER_OVER_LIMIT_LOW(202, "出水超量报警阈值低16bit", "ml", 1.0),
    SHORT_RANGE_ID_HIGH(203, "短距离无线通信ID高16bit", "", 1.0),
    SHORT_RANGE_ID_LOW(204, "短距离无线通信ID低16bit", "", 1.0),

    // ==================== 控制/配置类 (248~255) ====================
    ALARM_CONTROL(248, "告警控制", "", 1.0),
    FIRE_ALARM_CONTROL(249, "火灾报警控制", "", 1.0),
    MISREPORT_FUNC_CTRL(250, "设备疑似误报功能控制", "", 1.0),
    MISREPORT_JUMP_PERIOD(251, "设备疑似误报跳变周期设置", "s", 1.0),
    MISREPORT_JUMP_COUNT(252, "设备疑似误报跳变次数设置", "次", 1.0),
    MISREPORT_DISABLE_PERIOD(253, "设备疑似误报禁用周期设置", "s", 1.0),
    SMOKE_TAMPER_REPORT_CTRL(254, "设备烟感以及防拆上报控制", "", 1.0),
    REPORT_PERIOD(255, "上报周期", "s", 1.0),
    ;

    private final int typeValue;
    private final String name;
    private final String unit;
    private final double scale;

    private static final Map<Integer, AnalogTypeDef> TYPE_MAP = new HashMap<>();

    static {
        for (AnalogTypeDef def : values()) {
            TYPE_MAP.put(def.typeValue, def);
        }
    }

    AnalogTypeDef(int typeValue, String name, String unit, double scale) {
        this.typeValue = typeValue;
        this.name = name;
        this.unit = unit;
        this.scale = scale;
    }

    /**
     * 根据类型值获取定义
     */
    public static AnalogTypeDef fromType(int typeValue) {
        return TYPE_MAP.get(typeValue);
    }

    /**
     * 获取模拟量类型名称
     */
    public static String getName(int typeValue) {
        AnalogTypeDef def = fromType(typeValue);
        return def != null ? def.name : "未知";
    }

    /**
     * 获取模拟量单位
     */
    public static String getUnit(int typeValue) {
        AnalogTypeDef def = fromType(typeValue);
        return def != null ? def.unit : "";
    }

    /**
     * 获取模拟量分辨率
     */
    public static double getScale(int typeValue) {
        AnalogTypeDef def = fromType(typeValue);
        return def != null ? def.scale : 1.0;
    }
}
