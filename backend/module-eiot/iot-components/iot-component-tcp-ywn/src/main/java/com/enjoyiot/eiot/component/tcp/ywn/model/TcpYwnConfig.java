package com.enjoyiot.eiot.component.tcp.ywn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcpYwnConfig {

    private String host;

    private Integer port;

    /**
     * 是否允许自动注册设备
     * true: 允许未知设备自动注册(使用 fallbackProductKey)
     * false: 仅允许已预注册的设备接入(推荐用于多租户场景)
     * 默认: false
     */
    private Boolean allowAutoRegister = false;

    /**
     * 备用产品Key(仅当 allowAutoRegister=true 时生效)
     * 用于未预注册设备的 fallback 产品
     * 多租户场景建议留空,强制使用预注册模式
     */
    private String fallbackProductKey;
}
