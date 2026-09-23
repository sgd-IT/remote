package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 —— 事件类型字典
 * <p>
 * 对应协议文档第四章 4.1 节，一级数据类型 = 1（事件）时的二级类型字。
 * <p>
 * 编码规则：
 * <ul>
 *   <li>事件值双字节，<b>小端传输</b></li>
 *   <li>低字节 = 子类型（如烟雾类型、温度类型、电流类型等）</li>
 *   <li>高字节 = 报警类型（0=未区分 / 1=超上限预警 / 2=超上限报警 / 3=骤变 / 超下限等）</li>
 * </ul>
 * 命名约定：成对事件（奇数=开始, 偶数=结束），后缀 _START / _END。
 */
public final class DahuaEventTypeConstants {

    private DahuaEventTypeConstants() {}

    // ==================== 烟雾报警 ====================

    /** 烟雾报警开始 —— 值结构：烟雾类型 + 报警类型（预警/报警/行动线等） */
    public static final int EVT_SMOKE_START = 1;
    /** 烟雾报警结束 */
    public static final int EVT_SMOKE_END = 2;

    // ==================== 传感器故障 ====================

    /** 传感器故障开始 —— 值结构：传感器类型 + 故障类型（短路/断路等） */
    public static final int EVT_SENSOR_FAULT_START = 3;
    /** 传感器故障结束 */
    public static final int EVT_SENSOR_FAULT_END = 4;

    // ==================== 屏蔽 ====================

    /** 屏蔽开始 —— 值结构：预留 */
    public static final int EVT_SHIELD_START = 5;
    /** 屏蔽结束 */
    public static final int EVT_SHIELD_END = 6;

    // ==================== 监管 ====================

    /** 监管开始 —— 值结构：预留 */
    public static final int EVT_SUPERVISION_START = 7;
    /** 监管结束 */
    public static final int EVT_SUPERVISION_END = 8;

    // ==================== 启动 / 停止 ====================

    /** 启动开始 —— 值结构：预留 */
    public static final int EVT_START_START = 9;
    /** 启动结束 */
    public static final int EVT_START_END = 10;

    // ==================== 反馈 / 延时 ====================

    /** 反馈开始 —— 值结构：预留 */
    public static final int EVT_FEEDBACK_START = 11;
    /** 反馈结束 */
    public static final int EVT_FEEDBACK_END = 12;

    // ==================== 电源故障 ====================

    /** 电源故障开始 —— 值结构：板位 + 欠压等级 */
    public static final int EVT_POWER_FAULT_START = 13;
    /** 电源故障结束 */
    public static final int EVT_POWER_FAULT_END = 14;

    // ==================== 总线故障 ====================

    /** 总线故障开始 —— 值结构：预留 */
    public static final int EVT_BUS_FAULT_START = 15;
    /** 总线故障结束 */
    public static final int EVT_BUS_FAULT_END = 16;

    // ==================== 手动（非自动） ====================

    /** 手动事件开始 —— 值结构：预留 */
    public static final int EVT_MANUAL_START = 17;
    /** 手动事件结束 */
    public static final int EVT_MANUAL_END = 18;

    // ==================== 配置改变 ====================

    /** 配置改变 —— 值结构：预留 */
    public static final int EVT_CONFIG_CHANGE = 19;

    // ==================== 防拆 ====================

    /** 防拆开始 —— 值结构：预留 */
    public static final int EVT_ANTI_TAMPER_START = 20;
    /** 防拆结束 */
    public static final int EVT_ANTI_TAMPER_END = 21;

    // ==================== 气体浓度报警 ====================

    /** 气体浓度报警开始 —— 值结构：气体类型(CH4/CO/O2) + 报警类型 */
    public static final int EVT_GAS_CONCENTRATION_START = 22;
    /** 气体浓度报警结束 */
    public static final int EVT_GAS_CONCENTRATION_END = 23;

    // ==================== 温度报警 ====================

    /** 温度报警开始 —— 值结构：温度类型 + 报警类型 */
    public static final int EVT_TEMPERATURE_ALARM_START = 24;
    /** 温度报警结束 */
    public static final int EVT_TEMPERATURE_ALARM_END = 25;

    // ==================== 液压液位报警 ====================

    /** 液压液位报警开始 —— 值结构：类型 + 报警类型 */
    public static final int EVT_HYDRAULIC_LEVEL_ALARM_START = 26;
    /** 液压液位报警结束 */
    public static final int EVT_HYDRAULIC_LEVEL_ALARM_END = 27;

    // ==================== 电流报警 ====================

    /** 电流报警开始 —— 值结构：电流类型(A/B/C/零线/剩余/火线) + 报警类型 */
    public static final int EVT_CURRENT_ALARM_START = 28;
    /** 电流报警结束 */
    public static final int EVT_CURRENT_ALARM_END = 29;

    // ==================== 电压报警 ====================

    /** 电压报警开始 —— 值结构：电压类型 + 报警类型（含断相） */
    public static final int EVT_VOLTAGE_ALARM_START = 30;
    /** 电压报警结束 */
    public static final int EVT_VOLTAGE_ALARM_END = 31;

    // ==================== 功率报警 ====================

    /** 功率报警开始 —— 值结构：功率类型(视在/有功/无功) + 报警类型 */
    public static final int EVT_POWER_ALARM_START = 32;
    /** 功率报警结束 */
    public static final int EVT_POWER_ALARM_END = 33;

    // ==================== 三相不平衡 ====================

    /** 三相不平衡开始 —— 值结构：类型 + 报警类型 */
    public static final int EVT_THREE_PHASE_IMBALANCE_START = 34;
    /** 三相不平衡结束 */
    public static final int EVT_THREE_PHASE_IMBALANCE_END = 35;

    // ==================== 电弧/打火报警 ====================

    /** 电弧/打火报警开始 —— 值结构：相别 */
    public static final int EVT_ARC_ALARM_START = 36;
    /** 电弧/打火报警结束 */
    public static final int EVT_ARC_ALARM_END = 37;

    // ==================== 失联故障 ====================

    /** 失联故障开始 —— 值结构：预留 */
    public static final int EVT_LOST_CONTACT_START = 38;
    /** 失联故障结束 */
    public static final int EVT_LOST_CONTACT_END = 39;

    // ==================== 湿度报警 ====================

    /** 湿度报警开始 —— 值结构：报警类型 */
    public static final int EVT_HUMIDITY_ALARM_START = 40;
    /** 湿度报警结束 */
    public static final int EVT_HUMIDITY_ALARM_END = 41;

    // ==================== 谐波报警 ====================

    /** 谐波报警开始 —— 值结构：相别 + 报警类型 */
    public static final int EVT_HARMONIC_ALARM_START = 42;
    /** 谐波报警结束 */
    public static final int EVT_HARMONIC_ALARM_END = 43;

    // ==================== 开关量输入事件 ====================

    /** 开关量输入事件开始 —— 值结构：事件类型(复位/报警/故障) */
    public static final int EVT_SWITCH_INPUT_START = 44;
    /** 开关量输入事件结束 */
    public static final int EVT_SWITCH_INPUT_END = 45;

    // ==================== 逆相序报警 ====================

    /** 逆相序报警开始 —— 值结构：预留 */
    public static final int EVT_REVERSE_PHASE_SEQUENCE_START = 46;
    /** 逆相序报警结束 */
    public static final int EVT_REVERSE_PHASE_SEQUENCE_END = 47;

    // ==================== 浪涌报警 ====================

    /** 浪涌报警开始 —— 值结构：预留 */
    public static final int EVT_SURGE_ALARM_START = 48;
    /** 浪涌报警结束 */
    public static final int EVT_SURGE_ALARM_END = 49;

    // ==================== 短路报警 ====================

    /** 短路报警开始 —— 值结构：事件类型(A/B/C/火线短路) */
    public static final int EVT_SHORT_CIRCUIT_START = 50;
    /** 短路报警结束 */
    public static final int EVT_SHORT_CIRCUIT_END = 51;

    // ==================== 合闸事件 ====================

    /** 合闸事件开始 —— 值结构：预留 */
    public static final int EVT_CIRCUIT_CLOSURE_START = 52;
    /** 合闸事件结束 */
    public static final int EVT_CIRCUIT_CLOSURE_END = 53;

    // ==================== 气流故障 ====================

    /** 气流故障开始 —— 值结构：预留 */
    public static final int EVT_AIRFLOW_FAULT_START = 54;
    /** 气流故障结束 */
    public static final int EVT_AIRFLOW_FAULT_END = 55;

    // ==================== 探测故障 ====================

    /** 探测故障开始 —— 值结构：事件类型(风扇/CO传感器/探测腔) */
    public static final int EVT_DETECTION_FAULT_START = 56;
    /** 探测故障结束 */
    public static final int EVT_DETECTION_FAULT_END = 57;

    // ==================== 设备振动报警 ====================

    /** 设备振动报警开始 —— 值结构：预留 */
    public static final int EVT_VIBRATION_ALARM_START = 58;
    /** 设备振动报警结束 */
    public static final int EVT_VIBRATION_ALARM_END = 59;

    // ==================== 断电报警 ====================

    /** 断电报警开始 —— 值结构：事件类型(市电) */
    public static final int EVT_POWER_OFF_ALARM_START = 60;
    /** 断电报警结束 */
    public static final int EVT_POWER_OFF_ALARM_END = 61;

    // ==================== 空开分闸报警 ====================

    /** 空开分闸报警开始 —— 值结构：预留 */
    public static final int EVT_BREAKER_TRIP_START = 62;
    /** 空开分闸报警结束 */
    public static final int EVT_BREAKER_TRIP_END = 63;

    // ==================== 开关量输出联动事件 ====================

    /** 开关量输出联动事件开始 —— 值结构：事件类型(复位/报警/故障) */
    public static final int EVT_SWITCH_OUTPUT_LINKAGE_START = 64;
    /** 开关量输出联动事件结束 */
    public static final int EVT_SWITCH_OUTPUT_LINKAGE_END = 65;

    // ==================== 独立事件 ====================

    /** 反馈 —— 值结构：无 */
    public static final int EVT_FEEDBACK = 66;

    /** 延时 —— 值结构：无 */
    public static final int EVT_DELAY = 67;

    // ==================== 手动报警 ====================

    /** 手动报警开始 —— 值结构：预留 */
    public static final int EVT_MANUAL_ALARM_START = 68;
    /** 手动报警结束 */
    public static final int EVT_MANUAL_ALARM_END = 69;

    // ==================== 火焰报警 ====================

    /** 火焰报警开始 —— 值结构：预留 */
    public static final int EVT_FLAME_ALARM_START = 70;
    /** 火焰报警结束 */
    public static final int EVT_FLAME_ALARM_END = 71;

    // ==================== 设备故障 ====================

    /** 设备故障开始 —— 值结构：设备类型 + 详细事件内容(48种) */
    public static final int EVT_DEVICE_FAULT_START = 72;
    /** 设备故障结束 */
    public static final int EVT_DEVICE_FAULT_END = 73;

    // ==================== 设备重启 ====================

    /** 设备重启 —— 值结构：设备类型(主机/节点/节点+主机) */
    public static final int EVT_DEVICE_RESTART = 74;

    // ==================== 报警 ====================

    /** 报警开始 —— 值结构：事件类型 */
    public static final int EVT_ALARM_START = 75;
    /** 报警结束 */
    public static final int EVT_ALARM_END = 76;

    // ==================== 故障 ====================

    /** 故障开始 —— 值结构：事件类型(含井盖异动) */
    public static final int EVT_FAULT_START = 77;
    /** 故障结束 */
    public static final int EVT_FAULT_END = 78;

    // ==================== 消音 ====================

    /** 消音开始 —— 值结构：事件类型 */
    public static final int EVT_MUTE_START = 79;
    /** 消音结束 */
    public static final int EVT_MUTE_END = 80;
}
