package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 —— 升级类型字典
 * <p>
 * 对应协议文档第四章 4.8 节，一级数据类型 = 6（升级）时的二级类型字。
 * 值均为四字节，小端传输。
 */
public final class DahuaUpgradeTypeConstants {

    private DahuaUpgradeTypeConstants() {}

    // ==================== 升级命令（类型字 1） ====================

    /** 升级命令 - 未定义 */
    public static final int UPGRADE_CMD_UNDEFINED = 0;
    /** 升级命令 - 升级开始 */
    public static final int UPGRADE_CMD_START = 1;
    /** 升级命令 - 升级结束 */
    public static final int UPGRADE_CMD_END = 2;
    /** 升级命令 - 拒绝 */
    public static final int UPGRADE_CMD_REJECT = 3;

    /** 升级命令类型字 */
    public static final int TYPE_UPGRADE_COMMAND = 1;

    // ==================== 升级参数 ====================

    /** 升级总字节数 类型字 */
    public static final int TYPE_TOTAL_BYTES = 2;

    /** 偏移地址 类型字 */
    public static final int TYPE_OFFSET_ADDRESS = 3;

    /** 升级长度 类型字 */
    public static final int TYPE_UPGRADE_LENGTH = 4;

    // ==================== 数据帧 ====================

    /** 数据帧 类型字（升级小包数据） */
    public static final int TYPE_DATA_FRAME = 128;
}
