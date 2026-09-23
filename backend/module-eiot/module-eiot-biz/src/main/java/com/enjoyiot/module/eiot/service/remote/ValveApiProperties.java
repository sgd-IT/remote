package com.enjoyiot.module.eiot.service.remote;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 智能空开 API 配置属性
 *
 * 从 application.yml 中读取 valve.api.* 前缀的配置项
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "valve.api")
public class ValveApiProperties {

    /**
     * 智能空开 API 基础地址
     */
    private String url;

    /**
     * 空开设备 IP
     */
    private String ip;

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * API 调用超时（毫秒）
     */
    private Integer timeoutMs = 10000;
}
