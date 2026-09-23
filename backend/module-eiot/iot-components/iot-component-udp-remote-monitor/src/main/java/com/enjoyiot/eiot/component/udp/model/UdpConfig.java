package com.enjoyiot.eiot.component.udp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UDP配置类
 *@createtime 2025/5/22 13:26
 *@version 1.0
 *@author stuil
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UdpConfig {

    private int port;
    private String  host;
    private boolean enableAutoRegister = false;
    private String  defaultProductKey = "UWB_DEFAULT_PRODUCT";

    /** 协议模式：dahua(默认，大华定制国标协议) | uwb | fourfaith */
    private String  protocolMode = "dahua";

    /** 平台中心地址（6字节hex字符串，如 "020000000001"），用于大华协议下行帧的源地址 */
    private String  centerAddr = "020000000001";
}
