package com.enjoyiot.eiot.component.ff.parser;

/**
 * GB/T 26875.3-2011 城市消防远程监控系统 报警传输网络通信协议 常量定义
 * <p>
 * 对应协议文档《城市消防远程监控系统协议_拓展版 V1.53》
 */
public final class FourfaithGbtConstants {

    private FourfaithGbtConstants() {}

    // ==================== 帧定界符 ====================
    /** 启动符 '@' */
    public static final byte START_BYTE = 0x40;
    /** 结束符 '#' */
    public static final byte END_BYTE = 0x23;

    // ==================== 包结构尺寸 ====================
    /** 包头大小: @@(2) + serial(2) + version(2) + timestamp(6) + src(6) + dst(6) + length(2) = 26 */
    public static final int HEADER_SIZE = 26;
    /** 最小数据包大小: HEADER(26) + command(1) + checksum(1) + ##(2) = 30 */
    public static final int MIN_PACKET_SIZE = 30;
    /** 应用数据单元最大长度 */
    public static final int MAX_DATA_LENGTH = 1024;
    /** 地址长度(字节) */
    public static final int ADDRESS_LENGTH = 6;
    /** 时间标签长度(字节) */
    public static final int TIMESTAMP_SIZE = 6;

    // ==================== 协议版本 ====================
    /** 协议主版本号(协议文档固定值0x02) */
    public static final byte PROTOCOL_MAJOR_VERSION = 0x02;
    /** 协议用户版本号(默认0, 高4bit识别协议类型, 低4bit表示版本升级) */
    public static final byte PROTOCOL_USER_VERSION = 0x10; // GB协议: 高4bit=1

    // ==================== 命令字节 (表6.2) ====================
    /** 控制命令 (时间同步、下发远程控制) */
    public static final byte CMD_CONTROL = 1;
    /** 发送数据（需应答） */
    public static final byte CMD_SEND_DATA = 2;
    /** 确认：对控制命令和发送信息的确认回答 */
    public static final byte CMD_CONFIRM = 3;
    /** 请求查询 */
    public static final byte CMD_REQUEST = 4;
    /** 应答：返回查询的信息、上报远程应答 */
    public static final byte CMD_RESPONSE = 5;
    /** 否认 */
    public static final byte CMD_DENY = 6;
    /** 发送心跳（需应答）—— 国标框架心跳 */
    public static final int CMD_HEARTBEAT_ACK = 128;
    /** 发送心跳（不需应答）—— 国标框架心跳 */
    public static final int CMD_HEARTBEAT_NO_ACK = 129;
    /** 平台心跳应答（只针对CMD=128的应答） */
    public static final int CMD_HEARTBEAT_REPLY = 130;

    // ==================== 类型标志——上行 (表8.1.1) ====================
    /** 上传建筑消防设施系统状态 */
    public static final int TYPE_UP_SYSTEM_STATUS = 1;
    /** 上传建筑消防设施部件运行状态 */
    public static final int TYPE_UP_COMPONENT_STATUS = 2;
    /** 上传建筑消防设施部件模拟量值 */
    public static final int TYPE_UP_ANALOG_VALUE = 3;
    /** 上传建筑消防设施操作信息 */
    public static final int TYPE_UP_OPERATION_INFO = 4;
    /** 上传建筑消防设施软件版本 */
    public static final int TYPE_UP_SOFTWARE_VERSION = 5;
    /** 上传建筑消防设施系统说明描述 */
    public static final int TYPE_UP_SYSTEM_CONFIG = 6;
    /** 上传建筑消防设施部件配置描述 */
    public static final int TYPE_UP_COMPONENT_CONFIG = 7;
    /** 上传建筑消防设施系统时间 */
    public static final int TYPE_UP_SYSTEM_TIME = 8;
    /** 上传用户信息传输装置运行状态 */
    public static final int TYPE_UP_DEVICE_STATUS = 21;
    /** 上传用户信息传输装置操作信息 */
    public static final int TYPE_UP_DEVICE_OPERATION = 24;
    /** 上传用户信息传输装置软件版本 */
    public static final int TYPE_UP_DEVICE_VERSION = 25;
    /** 上传用户信息传输装置配置描述 */
    public static final int TYPE_UP_DEVICE_CONFIG = 26;
    /** 上传用户信息传输装置系统时间 */
    public static final int TYPE_UP_DEVICE_TIME = 28;

    // ==================== 类型标志——拓展版新增上行 ====================
    /** 上报系统状态（支持通道编号和通道类型） */
    public static final int TYPE_UP_SYSTEM_STATUS_CHAN = 130;
    /** 上报部件状态（支持通道编号和通道类型） */
    public static final int TYPE_UP_COMPONENT_STATUS_CHAN = 131;
    /** 上报模拟量值（支持通道编号和通道类型） */
    public static final int TYPE_UP_ANALOG_VALUE_CHAN = 132;
    /** 上报远程应答 (TLD-133) */
    public static final int TYPE_UP_REMOTE_RESPONSE = 0x85;
    /** 上报电能量 (TLD-138) */
    public static final int TYPE_UP_ENERGY = 138;
    /** 上报多通道设备状态、模拟量 (TLD-140) */
    public static final int TYPE_UP_MULTI_CHAN_STATUS = 140;
    /** 上报设备状态、模拟量（合并包）(TLD-142) */
    public static final int TYPE_UP_DEVICE_STATUS_ANALOG = 142;
    /** 上报32bit设备状态、模拟量 (TLD-143) */
    public static final int TYPE_UP_STATUS_ANALOG_32BIT = 143;
    /** 上报32bit设备报警开关、报警阈值 (TLD-144) */
    public static final int TYPE_UP_ALARM_SWITCH_32BIT = 144;
    /** 上行对时请求 (TLD-145) */
    public static final int TYPE_UP_TIME_SYNC_REQUEST = 145;
    /** 上报多通道设备各个通道的部件类型 (TLD-147) */
    public static final int TYPE_UP_MULTI_CHAN_COMP_TYPE = 147;
    /** 上报多通道设备各个通道数据 (TLD-148) */
    public static final int TYPE_UP_MULTI_CHAN_DATA = 148;

    // ==================== 类型标志——下行 (表8.1.1) ====================
    /** 查询建筑消防设施系统状态 */
    public static final int TYPE_DOWN_QUERY_SYSTEM_STATUS = 61;
    /** 查询建筑消防设施部件运行状态 */
    public static final int TYPE_DOWN_QUERY_COMPONENT_STATUS = 62;
    /** 查询建筑消防设施部件模拟量值 */
    public static final int TYPE_DOWN_QUERY_ANALOG_VALUE = 63;
    /** 查询建筑消防设施操作信息 */
    public static final int TYPE_DOWN_QUERY_OPERATION_INFO = 64;
    /** 查询建筑消防设施软件版本 */
    public static final int TYPE_DOWN_QUERY_SOFTWARE_VERSION = 65;
    /** 查询建筑消防设施系统说明描述 */
    public static final int TYPE_DOWN_QUERY_SYSTEM_CONFIG = 66;
    /** 查询建筑消防设施部件配置描述 */
    public static final int TYPE_DOWN_QUERY_COMPONENT_CONFIG = 67;
    /** 查询建筑消防设施系统时间 */
    public static final int TYPE_DOWN_QUERY_SYSTEM_TIME = 68;
    /** 查询用户信息传输装置运行状态 */
    public static final int TYPE_DOWN_QUERY_DEVICE_STATUS = 81;
    /** 查询用户信息传输装置操作信息 */
    public static final int TYPE_DOWN_QUERY_DEVICE_OPERATION = 84;
    /** 查询用户信息传输装置软件版本 */
    public static final int TYPE_DOWN_QUERY_DEVICE_VERSION = 85;
    /** 查询用户信息传输装置配置描述 */
    public static final int TYPE_DOWN_QUERY_DEVICE_CONFIG = 86;
    /** 查询用户信息传输装置系统时间 */
    public static final int TYPE_DOWN_QUERY_DEVICE_TIME = 88;
    /** 初始化用户信息传输装置 */
    public static final int TYPE_DOWN_INIT_DEVICE = 89;
    /** 同步用户信息传输装置时间 */
    public static final int TYPE_DOWN_SYNC_TIME = 90;
    /** 检查命令（查岗） */
    public static final int TYPE_DOWN_INSPECT = 91;
    /** 修改建筑消防设施部件模拟量值 (TLD-128) */
    public static final int TYPE_DOWN_MODIFY_ANALOG = 128;
    /** 下发远程控制 (TLD-129) */
    public static final int TYPE_DOWN_REMOTE_CONTROL = 0x81;
    /** 下行对时数据 (TLD-146) */
    public static final int TYPE_DOWN_TIME_SYNC_DATA = 146;

    // ==================== 四信私有类型标志 ====================
    /** PRT 打印信息 (TLD-9) */
    public static final int TYPE_UP_PRT = 0x09;
    /** CRT 图形显示信息 (TLD-10) */
    public static final int TYPE_UP_CRT = 0x0A;

    // ==================== 系统类型代码 (表8.1.3.1) ====================
    public static final int SYS_TYPE_GENERAL = 0;
    public static final int SYS_TYPE_FIRE_ALARM = 1;
    public static final int SYS_TYPE_BREAKER = 4;
    public static final int SYS_TYPE_FIRE_LINKAGE = 10;
    public static final int SYS_TYPE_HYDRANT = 11;
    public static final int SYS_TYPE_SPRINKLER = 12;
    public static final int SYS_TYPE_GAS_EXTINGUISH = 13;
    public static final int SYS_TYPE_WATER_SPRAY_ELECTRIC = 14;
    public static final int SYS_TYPE_WATER_SPRAY_PIPE = 15;
    public static final int SYS_TYPE_FOAM = 16;
    public static final int SYS_TYPE_DRY_POWDER = 17;
    public static final int SYS_TYPE_SMOKE_CONTROL = 18;
    public static final int SYS_TYPE_FIRE_DOOR = 19;
    public static final int SYS_TYPE_FIRE_ELEVATOR = 20;
    public static final int SYS_TYPE_EMERGENCY_BROADCAST = 21;
    public static final int SYS_TYPE_EMERGENCY_LIGHTING = 22;
    public static final int SYS_TYPE_FIRE_POWER = 23;
    public static final int SYS_TYPE_FIRE_PHONE = 24;
    // 拓展系统类型
    public static final int SYS_TYPE_INDEPENDENT = 128;
    public static final int SYS_TYPE_ELECTRICAL_FIRE = 129;
    public static final int SYS_TYPE_LORA_GATEWAY = 130;
    public static final int SYS_TYPE_PIPE_PRESSURE = 131;
    public static final int SYS_TYPE_POOL_LEVEL = 132;
    public static final int SYS_TYPE_HYDRANT_MONITOR = 133;
    public static final int SYS_TYPE_TANK_LEVEL = 134;
    public static final int SYS_TYPE_SMOKE_DETECT = 135;
    public static final int SYS_TYPE_PROTOCOL_PARSER = 136;
    public static final int SYS_TYPE_DTU = 137;
    public static final int SYS_TYPE_WATER_LEAK = 138;
    public static final int SYS_TYPE_MULTI_CHANNEL_HOST = 139;

    // ==================== 部件类型代码 (表8.1.3.2) ====================
    public static final int PTC_GENERAL = 0;
    public static final int PTC_FIRE_ALARM_CONTROLLER = 1;
    public static final int PTC_ELECTRICAL_FIRE_MONITOR = 16;
    public static final int PTC_RESIDUAL_CURRENT_PROBE = 17;
    public static final int PTC_TEMP_PROBE = 18;
    public static final int PTC_ELECTRICAL_FIRE_2PHASE = 19;
    public static final int PTC_MANUAL_ALARM_BUTTON = 23;
    public static final int PTC_HYDRANT_BUTTON = 24;
    public static final int PTC_TEMP_DETECTOR = 30;
    public static final int PTC_SMOKE_DETECTOR = 40;
    public static final int PTC_FLAME_DETECTOR_UV = 61;
    public static final int PTC_FLAME_DETECTOR_IR = 62;
    public static final int PTC_SOUND_LIGHT_ALARM = 80;
    public static final int PTC_INPUT_MODULE = 85;
    public static final int PTC_FIRE_PUMP = 91;
    public static final int PTC_SPRINKLER_PUMP = 95;
    public static final int PTC_FIRE_DOOR = 102;
    public static final int PTC_FIRE_VALVE = 103;
    public static final int PTC_SMOKE_EXHAUST_FAN = 111;
    public static final int PTC_FIRE_WATER_PRESSURE = 128;
    public static final int PTC_FIRE_LIQUID_LEVEL = 129;
    public static final int PTC_HYDRANT_SMART_CAP = 130;
    public static final int PTC_PROTOCOL_PARSER = 131;
    public static final int PTC_WATER_LEAK_DETECTOR = 134;
    public static final int PTC_MULTI_CHANNEL_HOST = 135;

    // ==================== 通信模块类型 (表9.1.1) ====================
    public static final int COMM_MODULE_ETHERNET = 0;
    public static final int COMM_MODULE_NB = 1;
    public static final int COMM_MODULE_LORA = 2;
    public static final int COMM_MODULE_LORAWAN = 3;
    public static final int COMM_MODULE_GPRS = 4;

    // ==================== 心跳包常量 ====================
    /** 旧版心跳包总字节数 */
    public static final int HEARTBEAT_PACKET_SIZE = 12;
    /** @@ 之后字节数（用于嗅探） */
    public static final int HEARTBEAT_BODY_SIZE = 10;

    // ==================== 远程控制常量 ====================
    public static final int REMOTE_CONTROL_TYPE_RESET = 10;
    public static final int REMOTE_CONTROL_TYPE_MUTE = 20;
    public static final int REMOTE_CONTROL_TYPE_SWITCH = 30;
    public static final int REMOTE_CONTROL_TYPE_BUS_SWITCH = 40;
    public static final int REMOTE_CONTROL_TYPE_MULTI_SWITCH = 41;

    // ==================== 远程应答状态位掩码 ====================
    /** 执行结果成功位 */
    public static final int REMOTE_RESP_SUCCESS_MASK = 0x80;
    /** 执行结果执行中位 */
    public static final int REMOTE_RESP_EXECUTING_MASK = 0x40;
    /** 执行结果超时时间掩码 */
    public static final int REMOTE_RESP_TIMEOUT_MASK = 0x3F;

    // ==================== 下行重试参数 ====================
    /** 下行指令超时时间（秒），协议建议≤10s */
    public static final int DOWN_RETRY_TIMEOUT_SEC = 10;
    /** 下行指令重发次数，协议建议3次 */
    public static final int DOWN_RETRY_MAX = 3;
}
