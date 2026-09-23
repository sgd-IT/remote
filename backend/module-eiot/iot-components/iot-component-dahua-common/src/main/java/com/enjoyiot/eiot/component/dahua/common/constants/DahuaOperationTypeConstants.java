package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 —— 操作类型字典
 * <p>
 * 对应协议文档第四章 4.7 节，一级数据类型 = 5（操作）时的二级类型字。
 * 操作方向：平台 → 终端。
 */
public final class DahuaOperationTypeConstants {

    private DahuaOperationTypeConstants() {}

    // ==================== 操作类型字 ====================

    /** 重启：0=预留 / 1=重启 / 2=恢复出厂并重启 */
    public static final int OP_RESTART = 1;

    /** 静默：0=解除静默 / 1=静默 */
    public static final int OP_SILENCE = 2;

    /** 下控命令：0=关闭 / 1=开启 / 2=联动 */
    public static final int OP_DOWN_CONTROL = 3;

    /** 低功耗待机模式：0=PSM模式 / 1=关机模式 */
    public static final int OP_LOW_POWER_STANDBY = 4;

    /** 自检：0=正常 / 1=自检操作 */
    public static final int OP_SELF_CHECK = 5;

    /** 校时请求：方式+参数（立即/间隔秒/分/时） */
    public static final int OP_TIME_SYNC_REQUEST = 6;

    /** 消音：0=正常 / 1=消音 */
    public static final int OP_MUTE = 7;

    /** 查找：方式+参数（提示时长秒/分/时） */
    public static final int OP_FIND = 8;

    /** 通配启动：0=停止 / 1=启动 */
    public static final int OP_WILDCARD_START = 9;

    /** 联动启动：0=停止 / 1=启动 */
    public static final int OP_LINKAGE_START = 10;

    // ==================== 重启子值 ====================

    /** 重启 - 预留 */
    public static final int OP_RESTART_RESERVED = 0;
    /** 重启 - 重启 */
    public static final int OP_RESTART_DO = 1;
    /** 重启 - 恢复出厂并重启 */
    public static final int OP_RESTART_FACTORY_RESET = 2;

    // ==================== 静默子值 ====================

    /** 解除静默 */
    public static final int OP_SILENCE_OFF = 0;
    /** 静默 */
    public static final int OP_SILENCE_ON = 1;

    // ==================== 下控命令子值 ====================

    /** 下控 - 关闭 */
    public static final int OP_DOWN_CONTROL_OFF = 0;
    /** 下控 - 开启 */
    public static final int OP_DOWN_CONTROL_ON = 1;
    /** 下控 - 联动 */
    public static final int OP_DOWN_CONTROL_LINKAGE = 2;

    // ==================== 低功耗待机子值 ====================

    /** PSM模式 */
    public static final int OP_STANDBY_PSM = 0;
    /** 关机模式 */
    public static final int OP_STANDBY_SHUTDOWN = 1;

    // ==================== 自检子值 ====================

    /** 正常 */
    public static final int OP_SELF_CHECK_NORMAL = 0;
    /** 自检操作 */
    public static final int OP_SELF_CHECK_DO = 1;

    // ==================== 消音子值 ====================

    /** 正常 */
    public static final int OP_MUTE_NORMAL = 0;
    /** 消音 */
    public static final int OP_MUTE_ON = 1;

    // ==================== 通配/联动启动子值 ====================

    /** 停止 */
    public static final int OP_START_STOP = 0;
    /** 启动 */
    public static final int OP_START_RUN = 1;
}
