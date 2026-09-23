package com.enjoyiot.module.eiot.service.remote;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 四信远程控制 API 配置属性
 *
 * 从 application.yml / application.properties 中读取 fourfaith.api.* 前缀的配置项
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fourfaith.api")
public class FourfaithApiProperties {

    /**
     * 四信远程控制 API 基础地址
     */
    private String url;

    /**
     * 品牌代码（四信 API 参数）
     */
    private String brandCode;

    /**
     * 设备型号（四信 API 参数）
     */
    private String model;

    /**
     * 协议版本（四信 API 参数）
     */
    private String version;

    /**
     * 四信 API App ID
     */
    private String appId;

    /**
     * 四信 API App Secret
     */
    private String appSecret;

    /**
     * 四信 API 调用超时（毫秒）
     */
    private Integer timeoutMs = 10000;
}
