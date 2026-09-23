package com.enjoyiot.eiot.component.dahua.common.constants;

/**
 * 大华消防协议 v1.04 —— 不定长参数类型字典
 * <p>
 * 对应协议文档第四章 4.9 节，一级数据类型 = 4（不定长参数）时的二级类型字。
 * <p>
 * 注释格式：参数名 [编码格式] 读/写
 */
public final class DahuaVariableParamTypeConstants {

    private DahuaVariableParamTypeConstants() {}

    // ==================== 网络参数（0~5） ====================

    /** 域名/IP [ASCII] R/W */
    public static final int PARAM_DOMAIN_IP = 0;

    /** PORT [ASCII] R/W */
    public static final int PARAM_PORT = 1;

    /** 本地IP [ASCII] R/W */
    public static final int PARAM_LOCAL_IP = 2;

    /** 掩码 [ASCII] R/W */
    public static final int PARAM_SUBNET_MASK = 3;

    /** 网关 [ASCII] R/W */
    public static final int PARAM_GATEWAY = 4;

    /** DNS [ASCII] R/W */
    public static final int PARAM_DNS = 5;

    // ==================== 业务参数（6~9） ====================

    /** 报警电话 [ASCII] R/W */
    public static final int PARAM_ALARM_PHONE = 6;

    /** 密码 [ASCII] R/W */
    public static final int PARAM_PASSWORD = 7;

    /** 时间区间 [ASCII] R/W */
    public static final int PARAM_TIME_RANGE = 8;

    /** 主机配置及部件信息 [Hex] R/W */
    public static final int PARAM_HOST_CONFIG = 9;

    // ==================== SIM/运营商信息（10~14） ====================

    /** IMEI [BCD] R/W */
    public static final int PARAM_IMEI = 10;

    /** IMSI [BCD] R/W */
    public static final int PARAM_IMSI = 11;

    /** CCID [BCD] R/W */
    public static final int PARAM_CCID = 12;

    /** Cell ID [ASCII] R/W */
    public static final int PARAM_CELL_ID = 13;

    /** APN [ASCII] R/W */
    public static final int PARAM_APN = 14;

    // ==================== 设备信息（15~19） ====================

    /** 产品型号 [ASCII] R/W */
    public static final int PARAM_PRODUCT_MODEL = 15;

    /** 时间 [ASCII] R/W */
    public static final int PARAM_TIME = 16;

    /** MAC地址 [Hex] R/W */
    public static final int PARAM_MAC_ADDRESS = 17;

    /** SN [ASCII] R/W */
    public static final int PARAM_SN = 18;

    /** 软件版本 [ASCII] R/W */
    public static final int PARAM_SOFTWARE_VERSION = 19;

    // ==================== 探测器配置（20~22） ====================

    /** 粒子鉴别开关 [Hex] R/W */
    public static final int PARAM_PARTICLE_DISCRIMINATION = 20;

    /** 辅助传感器开关 [Hex] R/W */
    public static final int PARAM_AUXILIARY_SENSOR_SWITCH = 21;

    /** 过滤器寿命监控开关 [Hex] R/W */
    public static final int PARAM_FILTER_LIFE_MONITOR = 22;

    // ==================== 扩展参数（23~26） ====================

    /** 设备地址信息 [ASCII] R/W */
    public static final int PARAM_DEVICE_ADDRESS = 23;

    /** 联动报警地址 [ASCII] R/W */
    public static final int PARAM_LINKAGE_ALARM_ADDRESS = 24;

    /** 设备服务码 [Hex] W */
    public static final int PARAM_DEVICE_SERVICE_CODE = 25;

    /** 雷达灵敏度 [Hex] W */
    public static final int PARAM_RADAR_SENSITIVITY = 26;

    // ==================== 系统信息（256~263） ====================

    /** 系统探测器数量 [Hex] R */
    public static final int PARAM_SYSTEM_DETECTOR_COUNT = 256;

    /** 设备UC码 [Hex] R */
    public static final int PARAM_DEVICE_UC_CODE = 257;

    /** 设备ID [Hex] R */
    public static final int PARAM_DEVICE_ID = 258;

    /** 事件内容 [ASCII] R */
    public static final int PARAM_EVENT_CONTENT = 259;

    /** 数据存储位置 [Hex] R */
    public static final int PARAM_DATA_STORAGE_LOCATION = 260;

    /** 设备地址信息 [ASCII] R */
    public static final int PARAM_DEVICE_ADDRESS_INFO = 261;

    /** 主机指示灯状态 [Hex] R/W */
    public static final int PARAM_HOST_INDICATOR_STATUS = 262;

    /** 主机部件数量 [Hex] R */
    public static final int PARAM_HOST_COMPONENT_COUNT = 263;
}
