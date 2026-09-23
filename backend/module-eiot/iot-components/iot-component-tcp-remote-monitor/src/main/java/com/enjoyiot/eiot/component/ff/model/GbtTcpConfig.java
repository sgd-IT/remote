package com.enjoyiot.eiot.component.ff.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GbtTcpConfig {

    private String host;

    private Integer port;

    /**
     * 监控中心地址（6字节十六进制字符串，如 "010000000001"）
     * 作为发送数据包的源地址
     */
    private String centerAddr;

    /**
     * 是否允许自动注册设备
     * true: 允许未知设备自动注册(使用 fallbackProductKey)
     * false: 仅允许已预注册的设备接入(推荐用于多租户场景)
     * 默认: false
     */
    @Builder.Default
    private Boolean allowAutoRegister = false;

    /**
     * 备用产品Key(仅当 allowAutoRegister=true 时生效)
     * 用于未预注册设备的 fallback 产品
     * 多租户场景建议留空,强制使用预注册模式
     */
    private String fallbackProductKey;

    /**
     * 协议模式：dahua(默认，大华定制国标协议) | fourfaith(四信拓展版)
     * 控制 processPacket 的路由分支
     */
    @Builder.Default
    private String protocolMode = "dahua";
}
