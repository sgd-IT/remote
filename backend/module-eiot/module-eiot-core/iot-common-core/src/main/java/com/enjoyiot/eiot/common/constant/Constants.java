package com.enjoyiot.eiot.common.constant;

/**
 * 通用常量信息
 *
 * @author ruoyi
 */
public interface Constants {

    /**
     * 设备物模型消息的topic
     */
    String THING_MODEL_MESSAGE_TOPIC = "device_thing";

    /**
     * 设备属性上报消息的topic
     */
    String DEVICE_PROPERTY_REPORT_TOPIC = "device_property_report";

    /**
     * 设备配置消息topic
     */
    String DEVICE_CONFIG_TOPIC = "device_config";

    /**
     * http消费设备信息的topic
     */
    String HTTP_CONSUMER_DEVICE_INFO_TOPIC = "device_info:";

    /**
     * 写权限
     */
    String PERMISSION_WRITE = "write";

    /**
     * 设备属性缓存key
     */
    String PROPERTY_CACHE_KEY = "str:iotkit:device:property:%s";

    /**
     * 设备最后一次通讯缓存key
     */
    String LAST_TIME_CACHE_KEY = "str:iotkit:device:lasttime:%s";

    /**
     * 组件发现topic
     */
    String COMPONENT_DISCOVER_TOPIC = "component_discover";

    /**
     * 组件发现回复topic
     */
    String COMPONENT_DISCOVER_REPLY_TOPIC = "component_discover_reply";

    /**
     * 设备路由
     */
    String REDIS_DEVICE_ROUTER = "str:iot:device:router:%s:%s";

    static String getRedisDeviceRouter(String pk, String dn) {
        return String.format(REDIS_DEVICE_ROUTER, pk, dn);
    }

    /**
     * 发送到设备消息的topic
     * @param componentType
     * @return
     */
    static String getSendToDeviceTopic(String componentType) {
        return String.format("%s_%s", componentType, THING_MODEL_MESSAGE_TOPIC);
    }
}

