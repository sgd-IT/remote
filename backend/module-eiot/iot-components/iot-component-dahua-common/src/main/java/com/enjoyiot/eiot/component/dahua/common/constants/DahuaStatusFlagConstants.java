package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 —— 状态标志位字典
 * <p>
 * 对应协议文档第四章 4.4 / 4.5 / 4.6 节。
 * 状态标志为双字节（16位）或扩展至8字节（64位），每个 bit 位表示一种状态。
 * <p>
 * 提供两组常量：
 * <ul>
 *   <li>{@code xxx_BIT_N} —— bit 位编号，用于 {@code (value >> N) & 1} 方式判断</li>
 *   <li>{@code xxx_MASK_N} —— 位掩码，用于 {@code (value & MASK) != 0} 方式判断</li>
 * </ul>
 */
public final class DahuaStatusFlagConstants {

    private DahuaStatusFlagConstants() {}

    // ==================== 系统状态标志位（双字节，4.4 节） ====================

    /** Bit 0：清0=正常运行，置1=测试 */
    public static final int SYS_FLAG_TEST_BIT = 0;
    public static final int SYS_FLAG_TEST_MASK = 1 << 0;

    /** Bit 1：清0=正常，置1=故障 */
    public static final int SYS_FLAG_FAULT_BIT = 1;
    public static final int SYS_FLAG_FAULT_MASK = 1 << 1;

    /** Bit 2：清0=正常，置1=屏蔽 */
    public static final int SYS_FLAG_SHIELD_BIT = 2;
    public static final int SYS_FLAG_SHIELD_MASK = 1 << 2;

    /** Bit 3：清0=无监管，置1=监管 */
    public static final int SYS_FLAG_SUPERVISION_BIT = 3;
    public static final int SYS_FLAG_SUPERVISION_MASK = 1 << 3;

    /** Bit 4：清0=停止，置1=启动 */
    public static final int SYS_FLAG_STARTED_BIT = 4;
    public static final int SYS_FLAG_STARTED_MASK = 1 << 4;

    /** Bit 5：清0=无反馈，置1=反馈 */
    public static final int SYS_FLAG_FEEDBACK_BIT = 5;
    public static final int SYS_FLAG_FEEDBACK_MASK = 1 << 5;

    /** Bit 6：清0=无延时，置1=延时 */
    public static final int SYS_FLAG_DELAY_BIT = 6;
    public static final int SYS_FLAG_DELAY_MASK = 1 << 6;

    /** Bit 7：清0=正常，置1=主电故障 */
    public static final int SYS_FLAG_MAIN_POWER_FAULT_BIT = 7;
    public static final int SYS_FLAG_MAIN_POWER_FAULT_MASK = 1 << 7;

    /** Bit 8：清0=正常，置1=备电故障 */
    public static final int SYS_FLAG_BACKUP_POWER_FAULT_BIT = 8;
    public static final int SYS_FLAG_BACKUP_POWER_FAULT_MASK = 1 << 8;

    /** Bit 9：清0=正常，置1=总线故障 */
    public static final int SYS_FLAG_BUS_FAULT_BIT = 9;
    public static final int SYS_FLAG_BUS_FAULT_MASK = 1 << 9;

    /** Bit 10：清0=自动状态，置1=手动状态 */
    public static final int SYS_FLAG_MANUAL_MODE_BIT = 10;
    public static final int SYS_FLAG_MANUAL_MODE_MASK = 1 << 10;

    /** Bit 11：清0=未改变，置1=配置改变 */
    public static final int SYS_FLAG_CONFIG_CHANGED_BIT = 11;
    public static final int SYS_FLAG_CONFIG_CHANGED_MASK = 1 << 11;

    /** Bit 12：清0=正常，置1=复位（优先级最高） */
    public static final int SYS_FLAG_RESET_BIT = 12;
    public static final int SYS_FLAG_RESET_MASK = 1 << 12;

    /** Bit 13：清0=正常，置1=设备被拆 */
    public static final int SYS_FLAG_DEVICE_DISMANTLED_BIT = 13;
    public static final int SYS_FLAG_DEVICE_DISMANTLED_MASK = 1 << 13;

    // ==================== 部件状态标志位（双字节，4.5 节） ====================

    /** Bit 0：清0=正常运行，置1=测试 */
    public static final int COMP_FLAG_TEST_BIT = 0;
    public static final int COMP_FLAG_TEST_MASK = 1 << 0;

    /** Bit 1：清0=正常，置1=火警 */
    public static final int COMP_FLAG_FIRE_ALARM_BIT = 1;
    public static final int COMP_FLAG_FIRE_ALARM_MASK = 1 << 1;

    /** Bit 2：清0=正常，置1=故障 */
    public static final int COMP_FLAG_FAULT_BIT = 2;
    public static final int COMP_FLAG_FAULT_MASK = 1 << 2;

    /** Bit 3：清0=正常，置1=屏蔽 */
    public static final int COMP_FLAG_SHIELD_BIT = 3;
    public static final int COMP_FLAG_SHIELD_MASK = 1 << 3;

    /** Bit 4：清0=无监管，置1=监管 */
    public static final int COMP_FLAG_SUPERVISION_BIT = 4;
    public static final int COMP_FLAG_SUPERVISION_MASK = 1 << 4;

    /** Bit 5：清0=停止，置1=启动 */
    public static final int COMP_FLAG_STARTED_BIT = 5;
    public static final int COMP_FLAG_STARTED_MASK = 1 << 5;

    /** Bit 6：清0=无反馈，置1=反馈 */
    public static final int COMP_FLAG_FEEDBACK_BIT = 6;
    public static final int COMP_FLAG_FEEDBACK_MASK = 1 << 6;

    /** Bit 7：清0=无延时，置1=延时 */
    public static final int COMP_FLAG_DELAY_BIT = 7;
    public static final int COMP_FLAG_DELAY_MASK = 1 << 7;

    /** Bit 8：清0=正常，置1=电源故障 */
    public static final int COMP_FLAG_POWER_FAULT_BIT = 8;
    public static final int COMP_FLAG_POWER_FAULT_MASK = 1 << 8;

    /** Bit 9：清0=正常，置1=失联 */
    public static final int COMP_FLAG_LOST_CONTACT_BIT = 9;
    public static final int COMP_FLAG_LOST_CONTACT_MASK = 1 << 9;

    /** Bit 10：清0=正常，置1=开路 */
    public static final int COMP_FLAG_OPEN_CIRCUIT_BIT = 10;
    public static final int COMP_FLAG_OPEN_CIRCUIT_MASK = 1 << 10;

    /** Bit 11：清0=正常，置1=短路 */
    public static final int COMP_FLAG_SHORT_CIRCUIT_BIT = 11;
    public static final int COMP_FLAG_SHORT_CIRCUIT_MASK = 1 << 11;

    /** Bit 12：清0=正常，置1=设备被拆 */
    public static final int COMP_FLAG_DISMANTLED_BIT = 12;
    public static final int COMP_FLAG_DISMANTLED_MASK = 1 << 12;

    /** Bit 13：清0=正常，置1=分体式欠压 */
    public static final int COMP_FLAG_UNDER_VOLTAGE_BIT = 13;
    public static final int COMP_FLAG_UNDER_VOLTAGE_MASK = 1 << 13;

    /** Bit 14：清0=正常，置1=传感器故障 */
    public static final int COMP_FLAG_SENSOR_FAULT_BIT = 14;
    public static final int COMP_FLAG_SENSOR_FAULT_MASK = 1 << 14;

    /** Bit 15：清0=正常，置1=升级故障 */
    public static final int COMP_FLAG_UPGRADE_FAULT_BIT = 15;
    public static final int COMP_FLAG_UPGRADE_FAULT_MASK = 1 << 15;

    // ==================== 部件状态扩展标志位（8字节扩展，4.6 节） ====================

    /** Bit 16：清0=PSM模式，置1=关机模式 */
    public static final int COMP_EXT_FLAG_SHUTDOWN_MODE_BIT = 16;
    public static final int COMP_EXT_FLAG_SHUTDOWN_MODE_MASK = 1 << 16;

    /** Bit 17：清0=寿命正常，置1=寿命故障 */
    public static final int COMP_EXT_FLAG_LIFE_FAULT_BIT = 17;
    public static final int COMP_EXT_FLAG_LIFE_FAULT_MASK = 1 << 17;

    /** Bit 18：清0=正常，置1=电弧/打火 */
    public static final int COMP_EXT_FLAG_ARC_BIT = 18;
    public static final int COMP_EXT_FLAG_ARC_MASK = 1 << 18;

    /** Bit 19：清0=波动报警解除，置1=波动报警 */
    public static final int COMP_EXT_FLAG_FLUCTUATION_ALARM_BIT = 19;
    public static final int COMP_EXT_FLAG_FLUCTUATION_ALARM_MASK = 1 << 19;

    /** Bit 20：清0=超上限报警解除，置1=超上限报警 */
    public static final int COMP_EXT_FLAG_OVER_UPPER_ALARM_BIT = 20;
    public static final int COMP_EXT_FLAG_OVER_UPPER_ALARM_MASK = 1 << 20;

    /** Bit 21：清0=超下限报警解除，置1=超下限报警 */
    public static final int COMP_EXT_FLAG_OVER_LOWER_ALARM_BIT = 21;
    public static final int COMP_EXT_FLAG_OVER_LOWER_ALARM_MASK = 1 << 21;

    /** Bit 22：清0=正常，置1=开盖报警 */
    public static final int COMP_EXT_FLAG_COVER_OPEN_BIT = 22;
    public static final int COMP_EXT_FLAG_COVER_OPEN_MASK = 1 << 22;

    /** Bit 23：清0=正常，置1=开门报警 */
    public static final int COMP_EXT_FLAG_DOOR_OPEN_BIT = 23;
    public static final int COMP_EXT_FLAG_DOOR_OPEN_MASK = 1 << 23;

    /** Bit 24：清0=正常，置1=SOS报警 */
    public static final int COMP_EXT_FLAG_SOS_ALARM_BIT = 24;
    public static final int COMP_EXT_FLAG_SOS_ALARM_MASK = 1 << 24;

    /** Bit 25：清0=正常，置1=感温报警 */
    public static final int COMP_EXT_FLAG_TEMP_ALARM_BIT = 25;
    public static final int COMP_EXT_FLAG_TEMP_ALARM_MASK = 1 << 25;

    /** Bit 26：清0=正常，置1=设备断电 */
    public static final int COMP_EXT_FLAG_POWER_OFF_BIT = 26;
    public static final int COMP_EXT_FLAG_POWER_OFF_MASK = 1 << 26;

    /** Bit 27：清0=正常，置1=移动报警 */
    public static final int COMP_EXT_FLAG_MOVE_ALARM_BIT = 27;
    public static final int COMP_EXT_FLAG_MOVE_ALARM_MASK = 1 << 27;

    /** Bit 28：清0=本底值正常，置1=本底值故障 */
    public static final int COMP_EXT_FLAG_BASELINE_FAULT_BIT = 28;
    public static final int COMP_EXT_FLAG_BASELINE_FAULT_MASK = 1 << 28;

    /** Bit 29：清0=功耗正常，置1=功耗故障 */
    public static final int COMP_EXT_FLAG_POWER_CONSUMPTION_FAULT_BIT = 29;
    public static final int COMP_EXT_FLAG_POWER_CONSUMPTION_FAULT_MASK = 1 << 29;

    /** Bit 30：清0=迷宫正常，置1=迷宫污染 */
    public static final int COMP_EXT_FLAG_MAZE_POLLUTION_BIT = 30;
    public static final int COMP_EXT_FLAG_MAZE_POLLUTION_MASK = 1 << 30;

    /** Bit 31：清0=电路正常，置1=电路失效 */
    public static final int COMP_EXT_FLAG_CIRCUIT_FAILURE_BIT = 31;
    public static final int COMP_EXT_FLAG_CIRCUIT_FAILURE_MASK = 1 << 31;
}
