package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 —— 定长参数类型字典
 * <p>
 * 对应协议文档第四章 4.8 节，一级数据类型 = 3（定长参数）时的二级类型字。
 * <p>
 * 注释格式：参数名 [最小单位] 读/写 | 说明
 */
public final class DahuaFixedParamTypeConstants {

    private DahuaFixedParamTypeConstants() {}

    // ==================== 系统配置类（98~115） ====================

    /** 总配置 [R] 读取总配置 */
    public static final int PARAM_TOTAL_CONFIG = 98;

    /** 部件总数 [R] 最小单位 1个 */
    public static final int PARAM_COMPONENT_COUNT = 99;

    /** 部件MAC [R/W] 4字节 */
    public static final int PARAM_COMPONENT_MAC = 100;

    /** 报警上限 [R/W] 按类型，支持48种子类型（历史遗留: 预警阈值） */
    public static final int PARAM_ALARM_UPPER = 101;

    /** 报警下限 [R/W] 按类型，同101子类型（历史遗留: 预警下限） */
    public static final int PARAM_ALARM_LOWER = 102;

    /** 电池欠压阈值 [R/W] 最小单位 1mV，范围 2800~3000 */
    public static final int PARAM_BATTERY_UNDER_VOLTAGE = 103;

    /** 消音周期 [R/W] 最小单位 1s，范围 1~99 */
    public static final int PARAM_MUTE_CYCLE = 105;

    /** 本底值补偿周期 [R/W] 最小单位 1s，范围 1~259200 */
    public static final int PARAM_BASELINE_COMPENSATION_CYCLE = 106;

    /** 烟雾浓度本底值 [R] 范围 0~4095 */
    public static final int PARAM_SMOKE_BASELINE = 107;

    /** 烟雾浓度值 [R] 范围 0~4095 */
    public static final int PARAM_SMOKE_CONCENTRATION = 108;

    /** 当前电池电压 [R] 最小单位 1mV，范围 0~3200 */
    public static final int PARAM_BATTERY_VOLTAGE = 109;

    /** sub 1G无线信道 [R/W] */
    public static final int PARAM_SUB_1G_WIRELESS_CHANNEL = 111;

    /** 通信失联超时 [R/W] 最小单位 1min */
    public static final int PARAM_COMM_LOST_TIMEOUT = 112;

    /** 声音开关 [R/W] 按键音/故障音/火警音 */
    public static final int PARAM_SOUND_SWITCH = 113;

    /** 功能开关 [R/W] 演示/设备模式/协议项 */
    public static final int PARAM_FUNCTION_SWITCH = 114;

    /** 报警流程配置 [R/W] 流程1/流程2/继续 */
    public static final int PARAM_ALARM_FLOW_CONFIG = 115;

    // ==================== 设备配置类（150~152） ====================

    /** 灵敏度 [R/W] 0=高 / 1=中 / 2=低 */
    public static final int PARAM_SENSITIVITY = 150;

    /** 工作模式 [R/W] 0=声 / 1=光 / 2=声光 */
    public static final int PARAM_WORK_MODE = 151;

    /** 是否允许消音 [R/W] 0=禁止 / 1=允许 */
    public static final int PARAM_MUTE_ALLOWED = 152;

    // ==================== 上报 & 采集类（201~219） ====================

    /** 上报间隔 [R/W] 最小单位 1s，全F表示不上报 */
    public static final int PARAM_REPORT_INTERVAL = 201;

    /** 波动阈值 [R/W] 液位1cm / 液压1KPa */
    public static final int PARAM_FLUCTUATION_THRESHOLD = 202;

    /** 心跳时间间隔 [R/W] 最小单位 1s */
    public static final int PARAM_HEARTBEAT_INTERVAL = 203;

    /** 传感器类型 [R/W] 0~11多种类型 */
    public static final int PARAM_SENSOR_TYPE = 204;

    /** 传感器量程设置 [R/W] 液位1cm / 液压1KPa */
    public static final int PARAM_SENSOR_RANGE = 205;

    /** 报警上限 [R/W] 同定长参数101类型 */
    public static final int PARAM_ALARM_UPPER_LIMIT = 206;

    /** 报警下限 [R/W] 同定长参数101类型 */
    public static final int PARAM_ALARM_LOWER_LIMIT = 207;

    /** 通道关闭 [R/W] 按位关闭 */
    public static final int PARAM_CHANNEL_CLOSE = 208;

    /** 协议接入号 [R/W] 最小单位 1，范围 0~255 */
    public static final int PARAM_PROTOCOL_ACCESS_NO = 209;

    /** 报警复合次数 [R/W] 最小单位 1，范围 1~100 */
    public static final int PARAM_ALARM_COMPOSITE_COUNT = 210;

    /** NB 频段 [R] */
    public static final int PARAM_NB_BAND = 211;

    /** TAU 时间 [R] 取自模块 T3412 和 T3324 */
    public static final int PARAM_TAU_TIME = 212;

    /** 工作时长 [R/W] 0=不启用; 1=开启/2=切断/3=备电 + 时:分:秒 */
    public static final int PARAM_WORK_DURATION = 213;

    /** 输入输出开关量 [R/W] 状态/联动/地址 */
    public static final int PARAM_IO_SWITCH = 214;

    /** 工作时刻 [R/W] 激活时刻配置 */
    public static final int PARAM_WORK_TIME = 215;

    /** 采样周期 [R/W] 最小单位 1s，范围 30~300s */
    public static final int PARAM_SAMPLE_CYCLE = 216;

    /** 开关量状态 [R/W] 0=关闭 / 1=常开 / 2=常闭 */
    public static final int PARAM_SWITCH_STATUS = 217;

    /** 联动输入动作 [R/W] 0=复位 / 1=报警 / 2=故障 */
    public static final int PARAM_LINKAGE_INPUT_ACTION = 218;

    /** 输出联动配置 [R/W] 触发条件+节点+回路 */
    public static final int PARAM_OUTPUT_LINKAGE_CONFIG = 219;

    // ==================== 吸气式探测器专用（220~229） ====================

    /** 烟雾流速上限 [R] 单位 0.01m/s，小端 */
    public static final int PARAM_SMOKE_FLOW_SPEED_UPPER = 220;
    /** 烟雾流速下限 [R] 单位 0.01m/s，小端 */
    public static final int PARAM_SMOKE_FLOW_SPEED_LOWER = 221;
    /** 烟雾预警浓度上限 [R] 单位 0.001%OBS/M，小端 */
    public static final int PARAM_SMOKE_WARNING_CONCENTRATION_UPPER = 222;
    /** 烟雾预警浓度下限 [R] 单位 0.001%OBS/M，小端 */
    public static final int PARAM_SMOKE_WARNING_CONCENTRATION_LOWER = 223;
    /** 烟雾行动浓度上限 [R] 单位 0.001%OBS/M，小端 */
    public static final int PARAM_SMOKE_ACTION_CONCENTRATION_UPPER = 224;
    /** 烟雾行动浓度下限 [R] 单位 0.001%OBS/M，小端 */
    public static final int PARAM_SMOKE_ACTION_CONCENTRATION_LOWER = 225;
    /** 烟雾火警1浓度上限 [R] 单位 0.001%OBS/M，小端 */
    public static final int PARAM_SMOKE_FIRE1_CONCENTRATION_UPPER = 226;
    /** 烟雾火警1浓度下限 [R] 单位 0.001%OBS/M，小端 */
    public static final int PARAM_SMOKE_FIRE1_CONCENTRATION_LOWER = 227;
    /** 烟雾火警2浓度上限 [R] 单位 0.001%OBS/M，小端 */
    public static final int PARAM_SMOKE_FIRE2_CONCENTRATION_UPPER = 228;
    /** 烟雾火警2浓度下限 [R] 单位 0.001%OBS/M，小端 */
    public static final int PARAM_SMOKE_FIRE2_CONCENTRATION_LOWER = 229;

    // ==================== 高级配置类（230~234） ====================

    /** 自学习时间 [R/W] 最小单位 1min */
    public static final int PARAM_SELF_LEARN_TIME = 230;

    /** 开关量联动使能 [R] 开/关 */
    public static final int PARAM_SWITCH_LINKAGE_ENABLE = 231;

    /** 滞回时间 [R/W] 温度/电流滞回 */
    public static final int PARAM_HYSTERESIS_TIME = 232;

    /** 剩余电流基底值 [R/W] 单位 mA */
    public static final int PARAM_RESIDUAL_CURRENT_BASELINE = 233;

    /** 湿度报警阈值 [R/W] 打开/关闭+阈值 */
    public static final int PARAM_HUMIDITY_ALARM_THRESHOLD = 234;
}
