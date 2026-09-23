package com.enjoyiot.eiot.component.gbttcp.parser;

/**
 * GB/T 26875.3-2011 城市消防远程监控系统 报警传输网络通信协议 常量定义
 */
public final class GbtConstants {

    private GbtConstants() {}

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
    /** 协议主版本号(固定值1) */
    public static final byte PROTOCOL_MAJOR_VERSION = 1;
    /** 协议用户版本号(默认0) */
    public static final byte PROTOCOL_USER_VERSION = 0;

    // ==================== 命令字节 (表2) ====================
    /** 控制命令 (时间同步、初始化等) */
    public static final byte CMD_CONTROL = 1;
    /** 发送/确认：发送火灾报警和建筑消防设施运行状态等信息 */
    public static final byte CMD_SEND_DATA = 2;
    /** 确认：对控制命令和发送信息的确认回复 */
    public static final byte CMD_CONFIRM = 3;
    /** 请求：查询火灾报警和建筑消防设施运行状态等信息 */
    public static final byte CMD_REQUEST = 4;
    /** 应答：返回查询的信息 */
    public static final byte CMD_RESPONSE = 5;
    /** 否认：对控制命令和发送信息的否认回复 */
    public static final byte CMD_DENY = 6;

    // ==================== 类型标志——上行 (表3) ====================
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

    // ==================== 类型标志——下行 (表3) ====================
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
    /** 检查命令 */
    public static final int TYPE_DOWN_INSPECT = 91;

    // ==================== 系统类型 (表4) ====================
    /** 通用 */
    public static final int SYS_TYPE_GENERAL = 0;
    /** 火灾报警系统 */
    public static final int SYS_TYPE_FIRE_ALARM = 1;
    /** 消防联动控制器 */
    public static final int SYS_TYPE_FIRE_LINKAGE = 10;
    /** 消火栓系统 */
    public static final int SYS_TYPE_HYDRANT = 11;
    /** 自动喷水灭火系统 */
    public static final int SYS_TYPE_SPRINKLER = 12;
    /** 气体灭火系统 */
    public static final int SYS_TYPE_GAS_EXTINGUISH = 13;
    /** 水喷雾灭火系统（电动启动方式） */
    public static final int SYS_TYPE_WATER_SPRAY_ELECTRIC = 14;
    /** 水喷雾灭火系统（传动管启动方式） */
    public static final int SYS_TYPE_WATER_SPRAY_PIPE = 15;
    /** 泡沫灭火系统 */
    public static final int SYS_TYPE_FOAM = 16;
    /** 干粉灭火系统 */
    public static final int SYS_TYPE_DRY_POWDER = 17;
    /** 防烟排烟系统 */
    public static final int SYS_TYPE_SMOKE_CONTROL = 18;
    /** 防火门及卷帘系统 */
    public static final int SYS_TYPE_FIRE_DOOR = 19;
    /** 消防电梯 */
    public static final int SYS_TYPE_FIRE_ELEVATOR = 20;
    /** 消防应急广播 */
    public static final int SYS_TYPE_EMERGENCY_BROADCAST = 21;
    /** 消防应急照明和疏散指示系统 */
    public static final int SYS_TYPE_EMERGENCY_LIGHTING = 22;
    /** 消防电源 */
    public static final int SYS_TYPE_FIRE_POWER = 23;
    /** 消防电话 */
    public static final int SYS_TYPE_FIRE_PHONE = 24;

    // ==================== 模拟量类型 (表6) ====================
    /** 事件计数 (件) */
    public static final int ANALOG_EVENT_COUNT = 1;
    /** 高度 (0.01m) */
    public static final int ANALOG_HEIGHT = 2;
    /** 温度 (0.1℃) */
    public static final int ANALOG_TEMPERATURE = 3;
    /** 压力 MPa (0.1MPa) */
    public static final int ANALOG_PRESSURE_MPA = 4;
    /** 压力 kPa (0.1kPa) */
    public static final int ANALOG_PRESSURE_KPA = 5;
    /** 气体浓度 (%LEL) */
    public static final int ANALOG_GAS_CONCENTRATION = 6;
    /** 时间 (s) */
    public static final int ANALOG_TIME = 7;
    /** 电压 (0.1V) */
    public static final int ANALOG_VOLTAGE = 8;
    /** 电流 (0.1A) */
    public static final int ANALOG_CURRENT = 9;
    /** 流量 (0.1L/s) */
    public static final int ANALOG_FLOW_LS = 10;
    /** 流量 (0.1m³/min) */
    public static final int ANALOG_FLOW_M3MIN = 11;
    /** 风速 (m/s) */
    public static final int ANALOG_WIND_SPEED = 12;

    /**
     * 获取模拟量类型名称
     */
    public static String getAnalogTypeName(int type) {
        switch (type) {
            case ANALOG_EVENT_COUNT: return "事件计数";
            case ANALOG_HEIGHT: return "高度";
            case ANALOG_TEMPERATURE: return "温度";
            case ANALOG_PRESSURE_MPA: return "压力(MPa)";
            case ANALOG_PRESSURE_KPA: return "压力(kPa)";
            case ANALOG_GAS_CONCENTRATION: return "气体浓度";
            case ANALOG_TIME: return "时间";
            case ANALOG_VOLTAGE: return "电压";
            case ANALOG_CURRENT: return "电流";
            case ANALOG_FLOW_LS: return "流量(L/s)";
            case ANALOG_FLOW_M3MIN: return "流量(m³/min)";
            case ANALOG_WIND_SPEED: return "风速";
            default: return "未知";
        }
    }

    /**
     * 获取模拟量单位
     */
    public static String getAnalogUnit(int type) {
        switch (type) {
            case ANALOG_EVENT_COUNT: return "件";
            case ANALOG_HEIGHT: return "m";
            case ANALOG_TEMPERATURE: return "℃";
            case ANALOG_PRESSURE_MPA: return "MPa";
            case ANALOG_PRESSURE_KPA: return "kPa";
            case ANALOG_GAS_CONCENTRATION: return "%LEL";
            case ANALOG_TIME: return "s";
            case ANALOG_VOLTAGE: return "V";
            case ANALOG_CURRENT: return "A";
            case ANALOG_FLOW_LS: return "L/s";
            case ANALOG_FLOW_M3MIN: return "m³/min";
            case ANALOG_WIND_SPEED: return "m/s";
            default: return "";
        }
    }

    /**
     * 获取模拟量分辨率(最小分辨单元)
     */
    public static double getAnalogScale(int type) {
        switch (type) {
            case ANALOG_EVENT_COUNT: return 1.0;
            case ANALOG_HEIGHT: return 0.01;
            case ANALOG_TEMPERATURE: return 0.1;
            case ANALOG_PRESSURE_MPA: return 0.1;
            case ANALOG_PRESSURE_KPA: return 0.1;
            case ANALOG_GAS_CONCENTRATION: return 0.1;
            case ANALOG_TIME: return 1.0;
            case ANALOG_VOLTAGE: return 0.1;
            case ANALOG_CURRENT: return 0.1;
            case ANALOG_FLOW_LS: return 0.1;
            case ANALOG_FLOW_M3MIN: return 0.1;
            case ANALOG_WIND_SPEED: return 1.0;
            default: return 1.0;
        }
    }
}
