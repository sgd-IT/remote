package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 命令字与类型标志定义
 * <p>
 * 对应协议文档《消防终端与平台通讯国标协议 v1.04》表1-1
 * 帧路由采用二维结构：命令字(1B) + 类型标志(1B) + ADU + 校验和
 */
public final class DahuaProtocolConstants {

    private DahuaProtocolConstants() {}

    // ==================== 命令字 ====================

    /** 注册帧（设备→平台） */
    public static final byte CMD_REGISTER = 0x00;

    /** 时钟同步帧（平台→设备，广播帧） */
    public static final byte CMD_CLOCK_SYNC = 0x01;

    /** 设备上报帧（设备→平台）：系统状态/部件状态/模拟量/通用数据等 */
    public static final byte CMD_DEVICE_REPORT = 0x02;

    /** 确认帧（双向） */
    public static final byte CMD_CONFIRM = 0x03;

    /** 平台下发帧（平台→设备）：设置参数/查询/消音/通用下发等 */
    public static final byte CMD_PLATFORM_COMMAND = 0x04;

    /** 设备应答帧（设备→平台）：参数查询应答/通用应答/能力集应答等 */
    public static final byte CMD_DEVICE_RESPONSE = 0x05;

    /** 否认帧（双向） */
    public static final byte CMD_DENY = 0x06;

    /** 保活帧（设备→平台） */
    public static final byte CMD_KEEPALIVE = 0x07;

    /** 功能配置帧（平台→设备） */
    public static final byte CMD_FUNCTION_CONFIG = (byte) 0x81;

    /** 带参应答帧（设备→平台） */
    public static final byte CMD_PARAM_RESPONSE = (byte) 0x82;

    // ==================== 类型标志 —— 注册帧（CMD=0x00） ====================

    /** 注册帧 */
    public static final int TYPE_REGISTER = 0x00;

    /** 注册扩展帧 */
    public static final int TYPE_REGISTER_EXT = 0x8C;

    // ==================== 类型标志 —— 时钟同步帧（CMD=0x01） ====================

    /** 时钟同步帧（广播） */
    public static final int TYPE_CLOCK_SYNC = 0x5A;

    // ==================== 类型标志 —— 设备上报帧（CMD=0x02） ====================

    /** 系统状态帧 */
    public static final int TYPE_SYSTEM_STATUS = 0x01;

    /** 部件状态帧 */
    public static final int TYPE_COMPONENT_STATUS = 0x02;

    /** 部件模拟量帧（可替代保活帧） */
    public static final int TYPE_COMPONENT_ANALOG = 0x03;

    /** 系统模拟量帧 */
    public static final int TYPE_SYSTEM_ANALOG = 0x84;

    /** 部件状态扩展帧 */
    public static final int TYPE_COMPONENT_STATUS_EXT = 0x85;

    /** 部件模拟量扩展帧 */
    public static final int TYPE_COMPONENT_ANALOG_EXT = 0x86;

    /** 终端参数帧 / 同步参数帧（定长 & 不定长共用此类型标志） */
    public static final int TYPE_TERMINAL_PARAM = 0xBF;

    /** 通用数据帧（推荐，可替代所有上报帧） */
    public static final int TYPE_UNIVERSAL_DATA = 0x87;

    /** 删除能力集 */
    public static final int TYPE_DELETE_CAPABILITY = 0x8A;

    /** 系统状态扩展帧 */
    public static final int TYPE_SYSTEM_STATUS_EXT = 0x8D;

    /** 部件状态（CMD=0x02 下的独立类型） */
    public static final int TYPE_COMPONENT_STATUS_RAW = 0x8E;

    // ==================== 类型标志 —— 平台下发帧（CMD=0x04） ====================

    /** 设置参数帧（定长） */
    public static final int TYPE_SET_PARAM_FIXED = 0x64;

    /** 设置参数帧（不定长） */
    public static final int TYPE_SET_PARAM_VARIABLE = 0x81;

    /** 查询参数帧（定长） */
    public static final int TYPE_QUERY_PARAM_FIXED = 0x82;

    /** 查询参数帧（不定长） */
    public static final int TYPE_QUERY_PARAM_VARIABLE = 0x83;

    /** 远程消音帧 */
    public static final int TYPE_REMOTE_MUTE = 0xF9;

    /** 查询通用帧 */
    public static final int TYPE_QUERY_UNIVERSAL = 0x88;

    /** 设置/配置通用帧 */
    public static final int TYPE_CONFIG_UNIVERSAL = 0x89;

    /** 查询能力集 */
    public static final int TYPE_QUERY_CAPABILITY = 0x8B;

    // ==================== 类型标志 —— 设备应答帧（CMD=0x05） ====================

    /** 参数查询应答帧（定长） */
    public static final int TYPE_PARAM_RESPONSE_FIXED = 0xBE;

    /** 参数查询应答帧（不定长） */
    public static final int TYPE_PARAM_RESPONSE_VARIABLE = 0xBF;

    /** 查询通用应答帧 */
    public static final int TYPE_QUERY_UNIVERSAL_RESPONSE = 0x88;

    /** 查询能力集应答帧 */
    public static final int TYPE_QUERY_CAPABILITY_RESPONSE = 0x8B;

    // ==================== 类型标志 —— 功能配置帧（CMD=0x81） ====================

    /** 功能配置帧类型标志（固定值 250） */
    public static final int TYPE_FUNCTION_CONFIG = 250;

    // ==================== 类型标志 —— 带参应答帧（CMD=0x82） ====================

    /** 带参应答帧类型标志 */
    public static final int TYPE_PARAM_RESPONSE = 0x8F;

    // ==================== 一级数据类型（通用数据帧 TLV 核心） ====================

    /** 注册包类型 */
    public static final int DATA_TYPE_REGISTER = 0;

    /** 事件类型 */
    public static final int DATA_TYPE_EVENT = 1;

    /** 模拟量类型 */
    public static final int DATA_TYPE_ANALOG = 2;

    /** 参数类型（定长） */
    public static final int DATA_TYPE_PARAM_FIXED = 3;

    /** 参数类型（不定长） */
    public static final int DATA_TYPE_PARAM_VARIABLE = 4;

    /** 操作类型 */
    public static final int DATA_TYPE_OPERATION = 5;

    /** 升级类型 */
    public static final int DATA_TYPE_UPGRADE = 6;

    /** 能力类型 */
    public static final int DATA_TYPE_CAPABILITY = 7;

    /** 确认类型（含参） */
    public static final int DATA_TYPE_CONFIRM = 8;

    /** 加密类型 */
    public static final int DATA_TYPE_ENCRYPT = 9;

    // ==================== 帧结构常量 ====================

    /** 部件地址长度（字节）：节点号(2B) + 回路号(2B) */
    public static final int COMPONENT_ADDR_LENGTH = 4;

    /** 时间标签长度（字节） */
    public static final int TIMESTAMP_LENGTH = 6;

    /** 事件值长度（字节）：双字节，低字节=子类型，高字节=报警类型 */
    public static final int EVENT_VALUE_LENGTH = 2;
}
