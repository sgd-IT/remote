package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 —— 确认类型、加密类型、能力类型字典
 * <p>
 * 对应协议文档第四章/六章/七章。
 * <ul>
 *   <li>确认类型：一级数据类型 = 8（确认类型，含参）</li>
 *   <li>加密类型：一级数据类型 = 9（加密类型）</li>
 *   <li>能力类型：一级数据类型 = 7（能力类型），采用二级/三级 TLV 嵌套结构</li>
 * </ul>
 */
public final class DahuaConfirmEncryptConstants {

    private DahuaConfirmEncryptConstants() {}

    // ==================== 确认类型（类型字 1~5） ====================

    /**
     * 密码确认
     * <p>值含义：1=权限级别 / 2=密码正确 / 3=密码错误</p>
     */
    public static final int CONFIRM_PASSWORD = 1;

    /** 二级密码 —— 值为当前二级密码内容 */
    public static final int CONFIRM_SECONDARY_PASSWORD = 2;

    /** 三级密码 —— 值为当前三级密码内容 */
    public static final int CONFIRM_TERTIARY_PASSWORD = 3;

    /** 四级密码 —— 值为当前四级密码内容 */
    public static final int CONFIRM_QUATERNARY_PASSWORD = 4;

    /** 操作确认 —— 值：0=未定义 / 1=操作确认 */
    public static final int CONFIRM_OPERATION = 5;

    // ==================== 确认类型子值（表 3.2.8-1） ====================

    /** 密码确认 - 未定义 */
    public static final int CONFIRM_PASSWORD_UNDEFINED = 0;
    /** 密码确认 - 需要二级操作权限 */
    public static final int CONFIRM_PASSWORD_NEED_LEVEL2 = 1;
    /** 密码确认 - 需要三级操作权限 */
    public static final int CONFIRM_PASSWORD_NEED_LEVEL3 = 2;
    /** 密码确认 - 需要四级操作权限 */
    public static final int CONFIRM_PASSWORD_NEED_LEVEL4 = 3;
    /** 密码确认 - 需要五级操作权限 */
    public static final int CONFIRM_PASSWORD_NEED_LEVEL5 = 4;
    /** 密码确认 - 密码正确 */
    public static final int CONFIRM_PASSWORD_CORRECT = 5;
    /** 密码确认 - 密码错误 */
    public static final int CONFIRM_PASSWORD_WRONG = 6;

    /** 操作确认 - 未定义 */
    public static final int CONFIRM_OP_UNDEFINED = 0;
    /** 操作确认 - 操作确认 */
    public static final int CONFIRM_OP_CONFIRMED = 1;

    // ==================== 加密类型（类型字 0~1） ====================

    /** 无加密 */
    public static final int ENCRYPT_NONE = 0;

    /** 异或校验方式 —— 值为密码内容 */
    public static final int ENCRYPT_XOR = 1;

    // ==================== 能力类型结构说明 ====================
    // 能力类型（一级数据类型 = 7）采用二级/三级 TLV 嵌套结构，
    // 描述设备支持的功能和参数范围。
    //
    // 数据类型分类：
    //   - 未定义类型：仅代表功能有无（如设备判断报警、服务器地址）
    //   - Bool型：TRUE/FALSE（如故障音能力、报警音能力）
    //   - 整型：有上限/下限范围（如烟雾浓度预警上限）
    //   - 枚举互斥：只能选一项（如灵敏度：高/中/低）
    //   - 枚举：可选多项
    //
    // 批量能力上报规则：
    //   - 一个信息体内只允许一个批量上报节点内容
    //   - "批量上报的节点内容"（类型字 65535）需在其他能力类型之后
    //   - 平台只读取能力集(0)或全能力集(1)，不单独读取单个能力项
    //
    // 能力集查询类型字：
    /** 读取能力集 */
    public static final int CAPABILITY_SET = 0;
    /** 读取全能力集 */
    public static final int CAPABILITY_FULL_SET = 1;
    /** 批量上报的节点内容（需排在其他能力类型之后） */
    public static final int CAPABILITY_BATCH_NODE = 65535;

    // ==================== 能力子值（表 3.2.7-2） ====================

    // ── 阀值类（整型） ──
    /** 能力上限 */
    public static final int CAP_SUB_UPPER = 0;
    /** 能力下限 */
    public static final int CAP_SUB_LOWER = 1;

    // ── Bool 能力类型 ──
    public static final int CAP_BOOL_FALSE = 0;
    public static final int CAP_BOOL_TRUE = 1;

    // ── 开关量状态能力类型 ──
    public static final int CAP_SWITCH_OFF = 0;
    public static final int CAP_SWITCH_NO = 1;   // 常开
    public static final int CAP_SWITCH_NC = 2;   // 常闭

    // ── 输入开关量模式类型 ──
    public static final int CAP_INPUT_MODE_RESET = 0;
    public static final int CAP_INPUT_MODE_ALARM = 1;
    public static final int CAP_INPUT_MODE_FAULT = 2;

    // ── 开关量联动输出模式类型 ──
    public static final int CAP_LINKAGE_OUTPUT_SMART_DETECT = 0;
    public static final int CAP_LINKAGE_OUTPUT_FULL_ALARM = 1;
    public static final int CAP_LINKAGE_OUTPUT_NON_FULL = 2;
    public static final int CAP_LINKAGE_OUTPUT_OFFLINE = 3;
    public static final int CAP_LINKAGE_OUTPUT_POWER_MGMT = 4;
    public static final int CAP_LINKAGE_OUTPUT_FULL_FAULT = 5;

    // ── 灵敏度能力类型 ──
    public static final int CAP_SENSITIVITY_HIGH = 0;
    public static final int CAP_SENSITIVITY_MID = 1;
    public static final int CAP_SENSITIVITY_LOW = 2;

    // ── 联动模式 ──
    public static final int CAP_LINKAGE_NORMAL = 0;
    public static final int CAP_LINKAGE_SOUND_LIGHT = 1;

    // ── 数据加密方式 ──
    public static final int CAP_ENCRYPT_NONE = 0;
    public static final int CAP_ENCRYPT_XOR = 1;

    // ── 节点批量上报内容 ──
    public static final int CAP_BATCH_COMPONENT_TYPE = 0;
    public static final int CAP_BATCH_LOOP_ID = 1;
    public static final int CAP_BATCH_NODE_ID = 2;
    public static final int CAP_BATCH_ALL_DETECTORS = 3;
}
