package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 —— 模拟量类型字典
 * <p>
 * 对应协议文档第四章 4.2 节，一级数据类型 = 2（模拟量）时的二级类型字。
 * <p>
 * 通用约束：模拟量值为双字节有符号数，取值范围 (-32768, +32767)。
 * <p>
 * 注释格式：[单位] 精度
 */
public final class DahuaAnalogTypeConstants {

    private DahuaAnalogTypeConstants() {}

    // ==================== 基础模拟量（1~12） ====================

    /** 事件计数 [件] 精度 1 */
    public static final int ANALOG_EVENT_COUNT = 1;

    /** 高度 [m] 精度 0.01m */
    public static final int ANALOG_HEIGHT = 2;

    /** 温度 [℃] 精度 0.1℃ */
    public static final int ANALOG_TEMPERATURE = 3;

    /** 压力 [MPa] 精度 0.1MPa */
    public static final int ANALOG_PRESSURE_MPA = 4;

    /** 压力 [KPa] 精度 0.1KPa */
    public static final int ANALOG_PRESSURE_KPA = 5;

    /** 气体浓度 [%LEL] 精度 0.1%LEL */
    public static final int ANALOG_GAS_CONCENTRATION_LEL = 6;

    /** 时间 [s] 精度 1s */
    public static final int ANALOG_TIME = 7;

    /** 电压 [V] 精度 0.1V */
    public static final int ANALOG_VOLTAGE = 8;

    /** 电流 [A] 精度 0.1A */
    public static final int ANALOG_CURRENT = 9;

    /** 流量 [L/s] 精度 0.1L/s */
    public static final int ANALOG_FLOW_RATE = 10;

    /** 风量 [m³/min] 精度 0.1m³/min */
    public static final int ANALOG_AIR_VOLUME = 11;

    /** 风速 [m/s] 精度 1m/s */
    public static final int ANALOG_WIND_SPEED = 12;

    // ==================== 扩展模拟量（128~137） ====================

    /** 剩余电流 [mA] 精度 1mA */
    public static final int ANALOG_RESIDUAL_CURRENT = 128;

    /** 湿度 [%RH] 精度 0.1%RH */
    public static final int ANALOG_HUMIDITY = 129;

    /** 信号强度(CSQ) 精度 1 */
    public static final int ANALOG_CSQ = 130;

    /** 信号强度(RSSI) [dBm] 精度 1dBm */
    public static final int ANALOG_RSSI = 131;

    /** 重发次数 精度 1 */
    public static final int ANALOG_RETRANSMIT_COUNT = 132;

    /** 气体浓度 [PPM] 精度 1PPM */
    public static final int ANALOG_GAS_CONCENTRATION_PPM = 133;

    /** A相电流 [mA] 精度 0.1mA */
    public static final int ANALOG_CURRENT_A_MA = 134;
    /** B相电流 [mA] 精度 0.1mA */
    public static final int ANALOG_CURRENT_B_MA = 135;
    /** C相电流 [mA] 精度 0.1mA */
    public static final int ANALOG_CURRENT_C_MA = 136;
    /** 零线电流 [mA] 精度 0.1mA */
    public static final int ANALOG_CURRENT_NEUTRAL_MA = 137;

    // ==================== 电流 A 级（138~141） ====================

    /** A相电流 [A] 精度 0.1A */
    public static final int ANALOG_CURRENT_A = 138;
    /** B相电流 [A] 精度 0.1A */
    public static final int ANALOG_CURRENT_B = 139;
    /** C相电流 [A] 精度 0.1A */
    public static final int ANALOG_CURRENT_C = 140;
    /** 零线电流 [A] 精度 0.1A */
    public static final int ANALOG_CURRENT_NEUTRAL = 141;

    // ==================== 电压 mV 级（142~144） ====================

    /** A相电压 [mV] 精度 0.1mV */
    public static final int ANALOG_VOLTAGE_A_MV = 142;
    /** B相电压 [mV] 精度 0.1mV */
    public static final int ANALOG_VOLTAGE_B_MV = 143;
    /** C相电压 [mV] 精度 0.1mV */
    public static final int ANALOG_VOLTAGE_C_MV = 144;

    // ==================== 电压 V 级（145~147） ====================

    /** A相电压 [V] 精度 0.1V */
    public static final int ANALOG_VOLTAGE_A = 145;
    /** B相电压 [V] 精度 0.1V */
    public static final int ANALOG_VOLTAGE_B = 146;
    /** C相电压 [V] 精度 0.1V */
    public static final int ANALOG_VOLTAGE_C = 147;

    // ==================== 视在功率 VA 级（148~151） ====================

    /** A相视在功率 [VA] 精度 0.1VA */
    public static final int ANALOG_APPARENT_POWER_A_VA = 148;
    /** B相视在功率 [VA] 精度 0.1VA */
    public static final int ANALOG_APPARENT_POWER_B_VA = 149;
    /** C相视在功率 [VA] 精度 0.1VA */
    public static final int ANALOG_APPARENT_POWER_C_VA = 150;
    /** 总视在功率 [VA] 精度 0.1VA */
    public static final int ANALOG_APPARENT_POWER_TOTAL_VA = 151;

    // ==================== 视在功率 kVA 级（152~155） ====================

    /** A相视在功率 [kVA] 精度 0.01kVA */
    public static final int ANALOG_APPARENT_POWER_A_KVA = 152;
    /** B相视在功率 [kVA] 精度 0.01kVA */
    public static final int ANALOG_APPARENT_POWER_B_KVA = 153;
    /** C相视在功率 [kVA] 精度 0.01kVA */
    public static final int ANALOG_APPARENT_POWER_C_KVA = 154;
    /** 总视在功率 [kVA] 精度 0.01kVA */
    public static final int ANALOG_APPARENT_POWER_TOTAL_KVA = 155;

    // ==================== 有功功率 W 级（156~159） ====================

    /** A相有功功率 [W] 精度 0.1W */
    public static final int ANALOG_ACTIVE_POWER_A_W = 156;
    /** B相有功功率 [W] 精度 0.1W */
    public static final int ANALOG_ACTIVE_POWER_B_W = 157;
    /** C相有功功率 [W] 精度 0.1W */
    public static final int ANALOG_ACTIVE_POWER_C_W = 158;
    /** 总有功功率 [W] 精度 0.1W */
    public static final int ANALOG_ACTIVE_POWER_TOTAL_W = 159;

    // ==================== 有功功率 kW 级（160~163） ====================

    /** A相有功功率 [kW] 精度 0.01kW */
    public static final int ANALOG_ACTIVE_POWER_A_KW = 160;
    /** B相有功功率 [kW] 精度 0.01kW */
    public static final int ANALOG_ACTIVE_POWER_B_KW = 161;
    /** C相有功功率 [kW] 精度 0.01kW */
    public static final int ANALOG_ACTIVE_POWER_C_KW = 162;
    /** 总有功功率 [kW] 精度 0.01kW */
    public static final int ANALOG_ACTIVE_POWER_TOTAL_KW = 163;

    // ==================== 无功功率 Var 级（164~167） ====================

    /** A相无功功率 [Var] 精度 0.1Var */
    public static final int ANALOG_REACTIVE_POWER_A_VAR = 164;
    /** B相无功功率 [Var] 精度 0.1Var */
    public static final int ANALOG_REACTIVE_POWER_B_VAR = 165;
    /** C相无功功率 [Var] 精度 0.1Var */
    public static final int ANALOG_REACTIVE_POWER_C_VAR = 166;
    /** 总无功功率 [Var] 精度 0.1Var */
    public static final int ANALOG_REACTIVE_POWER_TOTAL_VAR = 167;

    // ==================== 无功功率 kVar 级（168~171） ====================

    /** A相无功功率 [kVar] 精度 0.01kVar */
    public static final int ANALOG_REACTIVE_POWER_A_KVAR = 168;
    /** B相无功功率 [kVar] 精度 0.01kVar */
    public static final int ANALOG_REACTIVE_POWER_B_KVAR = 169;
    /** C相无功功率 [kVar] 精度 0.01kVar */
    public static final int ANALOG_REACTIVE_POWER_C_KVAR = 170;
    /** 总无功功率 [kVar] 精度 0.01kVar */
    public static final int ANALOG_REACTIVE_POWER_TOTAL_KVAR = 171;

    // ==================== 扩展模拟量（172~187） ====================

    /** 开关量（小华定制） */
    public static final int ANALOG_SWITCH_QUANTITY_XH = 172;

    /** A相温度 [℃] 精度 0.1℃ */
    public static final int ANALOG_TEMPERATURE_A = 173;
    /** B相温度 [℃] 精度 0.1℃ */
    public static final int ANALOG_TEMPERATURE_B = 174;
    /** C相温度 [℃] 精度 0.1℃ */
    public static final int ANALOG_TEMPERATURE_C = 175;
    /** 零线温度 [℃] 精度 0.1℃ */
    public static final int ANALOG_TEMPERATURE_NEUTRAL = 176;

    /** A相功率因数 精度 0.01 */
    public static final int ANALOG_POWER_FACTOR_A = 177;
    /** B相功率因数 精度 0.01 */
    public static final int ANALOG_POWER_FACTOR_B = 178;
    /** C相功率因数 精度 0.01 */
    public static final int ANALOG_POWER_FACTOR_C = 179;

    /** A相电能 [Wh] 范围 [0,999]Wh */
    public static final int ANALOG_ENERGY_A_WH = 180;
    /** B相电能 [Wh] 范围 [0,999]Wh */
    public static final int ANALOG_ENERGY_B_WH = 181;
    /** C相电能 [Wh] 范围 [0,999]Wh */
    public static final int ANALOG_ENERGY_C_WH = 182;
    /** 总电能 [Wh] 范围 [0,999]Wh */
    public static final int ANALOG_ENERGY_TOTAL_WH = 183;

    /** A相电能 [kWh] 范围 [0,9999]kWh */
    public static final int ANALOG_ENERGY_A_KWH = 184;
    /** B相电能 [kWh] 范围 [0,9999]kWh */
    public static final int ANALOG_ENERGY_B_KWH = 185;
    /** C相电能 [kWh] 范围 [0,9999]kWh */
    public static final int ANALOG_ENERGY_C_KWH = 186;
    /** 总电能 [kWh] 范围 [0,9999]kWh */
    public static final int ANALOG_ENERGY_TOTAL_KWH = 187;

    // ==================== 电能翻转次数（188~191） ====================

    /** A相电能翻转次数 精度 1 */
    public static final int ANALOG_ENERGY_FLIP_COUNT_A = 188;
    /** B相电能翻转次数 精度 1 */
    public static final int ANALOG_ENERGY_FLIP_COUNT_B = 189;
    /** C相电能翻转次数 精度 1 */
    public static final int ANALOG_ENERGY_FLIP_COUNT_C = 190;
    /** 总电能翻转次数 精度 1 */
    public static final int ANALOG_ENERGY_FLIP_COUNT_TOTAL = 191;

    // ==================== 电网参数（192~193） ====================

    /** 电网频率 [Hz] 精度 0.1Hz */
    public static final int ANALOG_GRID_FREQUENCY = 192;

    /** 总功率因数 精度 0.01 */
    public static final int ANALOG_TOTAL_POWER_FACTOR = 193;

    // ==================== 高精度 & 信号（256~261） ====================

    /** 高精度电压 [V] 精度 0.01V */
    public static final int ANALOG_HIGH_PRECISION_VOLTAGE = 256;

    /** 开关量 */
    public static final int ANALOG_SWITCH_QUANTITY = 257;

    /** 信噪比(SNR) [dBm] 精度 0.1dBm */
    public static final int ANALOG_SNR = 258;

    /** 信号功率 [dBm] 精度 0.1dBm */
    public static final int ANALOG_SIGNAL_POWER = 259;

    /** 覆盖等级(ECL) */
    public static final int ANALOG_ECL = 260;

    /** 物理小区标识(PCI) */
    public static final int ANALOG_PCI = 261;

    // ==================== 烟雾传感器（264~269） ====================

    /** 红光传感器采样值 精度 1 */
    public static final int ANALOG_RED_LIGHT_SAMPLE = 264;

    /** 灵敏度 精度 1 */
    public static final int ANALOG_SENSITIVITY = 265;

    /** 电池电量百分比 [%] 精度 1% */
    public static final int ANALOG_BATTERY_PERCENT = 266;

    /** 红光本底值 精度 1 */
    public static final int ANALOG_RED_LIGHT_BASELINE = 267;

    /** 蓝光传感器采样值 精度 1 */
    public static final int ANALOG_BLUE_LIGHT_SAMPLE = 268;

    /** 蓝光本底值 精度 1 */
    public static final int ANALOG_BLUE_LIGHT_BASELINE = 269;

    // ==================== 日电能系列（270~277） ====================

    /** A相日电能 [Wh] 精度同 180~183 */
    public static final int ANALOG_DAILY_ENERGY_A_WH = 270;
    /** B相日电能 [Wh] */
    public static final int ANALOG_DAILY_ENERGY_B_WH = 271;
    /** C相日电能 [Wh] */
    public static final int ANALOG_DAILY_ENERGY_C_WH = 272;
    /** 总日电能 [Wh] */
    public static final int ANALOG_DAILY_ENERGY_TOTAL_WH = 273;
    /** A相日电能 [kWh] */
    public static final int ANALOG_DAILY_ENERGY_A_KWH = 274;
    /** B相日电能 [kWh] */
    public static final int ANALOG_DAILY_ENERGY_B_KWH = 275;
    /** C相日电能 [kWh] */
    public static final int ANALOG_DAILY_ENERGY_C_KWH = 276;
    /** 总日电能 [kWh] */
    public static final int ANALOG_DAILY_ENERGY_TOTAL_KWH = 277;

    // ==================== 火线电参数（278~284） ====================

    /** 火线电流 [A] 精度 0.1A */
    public static final int ANALOG_FIRE_LINE_CURRENT = 278;

    /** 火线电压 [V] 精度 0.1V */
    public static final int ANALOG_FIRE_LINE_VOLTAGE = 279;

    /** 火线有功功率 [kW] 精度 0.01kW */
    public static final int ANALOG_FIRE_LINE_ACTIVE_POWER = 280;

    /** 火线功率因数 精度 0.01 */
    public static final int ANALOG_FIRE_LINE_POWER_FACTOR = 281;

    /** 火线电能 [kWh] 精度 1kWh */
    public static final int ANALOG_FIRE_LINE_ENERGY = 282;

    /** 火线无功功率 [kW] 精度 0.01kW */
    public static final int ANALOG_FIRE_LINE_REACTIVE_POWER = 283;

    /** 火线视在功率 [kVA] 精度 0.01kVA */
    public static final int ANALOG_FIRE_LINE_APPARENT_POWER = 284;

    // ==================== 基波（285~292） ====================

    /** A相基波电流 [A] 精度 0.1A */
    public static final int ANALOG_FUNDAMENTAL_CURRENT_A = 285;
    /** B相基波电流 [A] 精度 0.1A */
    public static final int ANALOG_FUNDAMENTAL_CURRENT_B = 286;
    /** C相基波电流 [A] 精度 0.1A */
    public static final int ANALOG_FUNDAMENTAL_CURRENT_C = 287;
    /** 火线基波电流 [A] 精度 0.1A */
    public static final int ANALOG_FUNDAMENTAL_CURRENT_FIRE = 288;

    /** A相基波电压 [V] 精度 0.1V */
    public static final int ANALOG_FUNDAMENTAL_VOLTAGE_A = 289;
    /** B相基波电压 [V] 精度 0.1V */
    public static final int ANALOG_FUNDAMENTAL_VOLTAGE_B = 290;
    /** C相基波电压 [V] 精度 0.1V */
    public static final int ANALOG_FUNDAMENTAL_VOLTAGE_C = 291;
    /** 火线基波电压 [V] 精度 0.1V */
    public static final int ANALOG_FUNDAMENTAL_VOLTAGE_FIRE = 292;

    // ==================== 谐波（293~300） ====================

    /** A相谐波电压总量 [%] 精度 0.01% */
    public static final int ANALOG_HARMONIC_VOLTAGE_A = 293;
    /** B相谐波电压总量 [%] 精度 0.01% */
    public static final int ANALOG_HARMONIC_VOLTAGE_B = 294;
    /** C相谐波电压总量 [%] 精度 0.01% */
    public static final int ANALOG_HARMONIC_VOLTAGE_C = 295;
    /** A相谐波电流总量 [%] 精度 0.01% */
    public static final int ANALOG_HARMONIC_CURRENT_A = 296;
    /** B相谐波电流总量 [%] 精度 0.01% */
    public static final int ANALOG_HARMONIC_CURRENT_B = 297;
    /** C相谐波电流总量 [%] 精度 0.01% */
    public static final int ANALOG_HARMONIC_CURRENT_C = 298;
    /** 火线电压谐波总量 [%] 精度 0.01% */
    public static final int ANALOG_HARMONIC_VOLTAGE_FIRE = 299;
    /** 火线电流谐波总量 [%] 精度 0.01% */
    public static final int ANALOG_HARMONIC_CURRENT_FIRE = 300;

    // ==================== 烟雾 & 环境（301~313） ====================

    /** 烟雾浓度 [OBS/M] 精度 1 */
    public static final int ANALOG_SMOKE_CONCENTRATION = 301;

    /** 烟雾比例值 [%] 精度 1% */
    public static final int ANALOG_SMOKE_RATIO = 302;

    /** 气泵转速 [RPM] 精度 1 */
    public static final int ANALOG_PUMP_SPEED = 303;

    /** 气流比例 [%] 精度 1% */
    public static final int ANALOG_AIRFLOW_RATIO = 304;

    /** PM2.5 [mg/m³] 精度 1 */
    public static final int ANALOG_PM25_MG = 305;

    /** 环境温度 [℃] 精度 0.1℃ */
    public static final int ANALOG_ENV_TEMPERATURE = 306;

    /** 环境湿度 [%RH] 精度 0.1%RH */
    public static final int ANALOG_ENV_HUMIDITY = 307;

    /** 高精度烟雾浓度 [OBS/M] 精度 0.001 */
    public static final int ANALOG_HIGH_PRECISION_SMOKE = 308;

    /** 电表有功总电量 [kWh] 精度 0.01kWh */
    public static final int ANALOG_METER_ACTIVE_ENERGY = 309;

    /** 本底值 [mv] 精度 1 */
    public static final int ANALOG_BASELINE_VALUE = 310;

    /** 迷宫污染指数 [%] 精度 1% */
    public static final int ANALOG_MAZE_POLLUTION_INDEX = 311;

    /** 气体浓度 [%VOL] 精度 0.1%VOL */
    public static final int ANALOG_GAS_CONCENTRATION_VOL = 312;

    /** PM2.5 [ug/m³] 精度 1 */
    public static final int ANALOG_PM25_UG = 313;
}
