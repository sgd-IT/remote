package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 —— 部件类型字典
 * <p>
 * 对应协议文档第四章 4.3 节，通用数据帧信息对象中的"部件类型"(1B)字段。
 * 部件地址由 节点号(2B) + 回路号(2B) 组成，小端传输。
 */
public final class DahuaComponentTypeConstants {

    private DahuaComponentTypeConstants() {}

    /** 通用 */
    public static final int COMP_GENERAL = 0;
    /** 火灾报警控制器 */
    public static final int COMP_FIRE_ALARM_CONTROLLER = 1;
    /** 可燃气体探测器 */
    public static final int COMP_COMBUSTIBLE_GAS_DETECTOR = 10;
    /** 剩余电流式电气火灾探测器 */
    public static final int COMP_RESIDUAL_CURRENT_DETECTOR = 17;
    /** 测温式电气火灾探测器 */
    public static final int COMP_TEMPERATURE_ELECTRIC_DETECTOR = 18;
    /** 手动火灾报警按钮 */
    public static final int COMP_MANUAL_ALARM_BUTTON = 23;
    /** 感温火灾探测器 */
    public static final int COMP_HEAT_DETECTOR = 30;
    /** 感烟火灾探测器 */
    public static final int COMP_SMOKE_DETECTOR = 40;
    /** 吸气式感烟火灾探测器 */
    public static final int COMP_ASPIRATION_SMOKE_DETECTOR = 44;
    /** 气体探测器(CO) */
    public static final int COMP_GAS_DETECTOR_CO = 74;
    /** 气体灭火控制器 */
    public static final int COMP_GAS_EXTINGUISH_CONTROLLER = 81;
    /** 消防电气控制装置 */
    public static final int COMP_FIRE_ELECTRICAL_CONTROL = 82;
    /** 消防控制室图形显示装置 */
    public static final int COMP_GRAPHICAL_DISPLAY = 83;
    /** 模块 */
    public static final int COMP_MODULE = 84;
    /** 输入模块 */
    public static final int COMP_INPUT_MODULE = 85;
    /** 输出模块 */
    public static final int COMP_OUTPUT_MODULE = 86;
    /** 输入/输出模块 */
    public static final int COMP_IO_MODULE = 87;
    /** 中继模块 */
    public static final int COMP_RELAY_MODULE = 88;
    /** 消防水泵 */
    public static final int COMP_FIRE_WATER_PUMP = 91;
    /** 喷淋泵 */
    public static final int COMP_SPRINKLER_PUMP = 95;
    /** 水流指示器 */
    public static final int COMP_WATER_FLOW_INDICATOR = 96;
    /** 信号阀 */
    public static final int COMP_SIGNAL_VALVE = 97;
    /** 报警阀 */
    public static final int COMP_ALARM_VALVE = 98;
    /** 压力开关 */
    public static final int COMP_PRESSURE_SWITCH = 99;
    /** 防火门 */
    public static final int COMP_FIRE_DOOR = 102;
    /** 防火阀 */
    public static final int COMP_FIRE_DAMPER = 103;
    /** 通风空调 */
    public static final int COMP_HVAC = 104;
    /** 排烟机 */
    public static final int COMP_SMOKE_EXHAUST_FAN = 109;
    /** 送风机 */
    public static final int COMP_SUPPLY_FAN = 110;
    /** 单相市电探测器 */
    public static final int COMP_SINGLE_PHASE_POWER_DETECTOR = 127;
    /** 压力传感器 */
    public static final int COMP_PRESSURE_SENSOR = 128;
    /** 电弧探测器 */
    public static final int COMP_ARC_DETECTOR = 129;
    /** 市电探测器 */
    public static final int COMP_POWER_DETECTOR = 130;
    /** 液位传感器 */
    public static final int COMP_LEVEL_SENSOR = 131;
    /** 湿度传感器 */
    public static final int COMP_HUMIDITY_SENSOR = 132;
    /** 温度传感器 */
    public static final int COMP_TEMPERATURE_SENSOR = 133;
    /** NB型感烟火灾探测器 */
    public static final int COMP_NB_SMOKE_DETECTOR = 134;
    /** 电能探测模块 */
    public static final int COMP_ENERGY_DETECTION_MODULE = 135;
    /** 电流探测器 */
    public static final int COMP_CURRENT_DETECTOR = 136;
    /** 声光报警器 */
    public static final int COMP_AUDIO_VISUAL_ALARM = 137;
    /** 电压探测器 */
    public static final int COMP_VOLTAGE_DETECTOR = 138;
    /** 功率探测器 */
    public static final int COMP_POWER_DETECTOR_DEVICE = 139;
    /** 断路器 */
    public static final int COMP_CIRCUIT_BREAKER = 140;
    /** 电能探测器(1P+N) */
    public static final int COMP_ENERGY_DETECTOR_1PN = 141;
    /** 电能探测器(2P) */
    public static final int COMP_ENERGY_DETECTOR_2P = 142;
    /** 电能探测器(3P+N) */
    public static final int COMP_ENERGY_DETECTOR_3PN = 143;
    /** 电能探测器(4P) */
    public static final int COMP_ENERGY_DETECTOR_4P = 144;
    /** 智能空开(1P) */
    public static final int COMP_SMART_BREAKER_1P = 145;
    /** 智能空开(2P) */
    public static final int COMP_SMART_BREAKER_2P = 146;
    /** 智能空开(2P+漏保) */
    public static final int COMP_SMART_BREAKER_2P_LEAK = 147;
    /** 智能空开(3P) */
    public static final int COMP_SMART_BREAKER_3P = 148;
    /** 智能空开(4P) */
    public static final int COMP_SMART_BREAKER_4P = 149;
    /** 智能空开(4P+漏保) */
    public static final int COMP_SMART_BREAKER_4P_LEAK = 150;
    /** 气体传感器 */
    public static final int COMP_GAS_SENSOR = 151;
    /** 通讯模组 */
    public static final int COMP_COMM_MODULE = 152;
    /** 水浸传感器 */
    public static final int COMP_WATER_IMMERSION_SENSOR = 153;
    /** 隔离模块 */
    public static final int COMP_ISOLATION_MODULE = 154;
    /** 可燃气体探测器(丙烷) */
    public static final int COMP_COMBUSTIBLE_GAS_PROPANE = 155;
    /** 门磁设备 */
    public static final int COMP_DOOR_MAGNETIC = 156;
    /** 气体释放警报器 */
    public static final int COMP_GAS_RELEASE_ALARM = 157;
    /** 紧急启停按钮 */
    public static final int COMP_EMERGENCY_START_STOP = 158;
    /** 电表 */
    public static final int COMP_ELECTRIC_METER = 159;
    /** 应急照明 */
    public static final int COMP_EMERGENCY_LIGHTING = 160;
    /** 照明配电 */
    public static final int COMP_LIGHTING_DISTRIBUTION = 161;
    /** 广播模块 */
    public static final int COMP_BROADCAST_MODULE = 162;
}
