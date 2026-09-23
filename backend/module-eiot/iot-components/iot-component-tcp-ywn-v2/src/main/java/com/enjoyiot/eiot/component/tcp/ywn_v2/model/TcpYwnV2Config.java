package com.enjoyiot.eiot.component.tcp.ywn_v2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * YWN V2 TCP 组件配置模型
 *
 * 协议: TCP + 协议头(8B BE) + Protobuf
 * 消息类型: REGISTER(1) / HEARTBEAT(2) / EVENT(3) / CONTROL(4)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcpYwnV2Config {

    /** 监听地址 */
    private String host;

    /** 监听端口 */
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

    /**
     * 心跳超时时间(毫秒)
     * 默认: 6分钟 (设备心跳周期3分钟，2倍超时)
     */
    private Long keepAliveTimeoutMs = 360000L;

    /**
     * 注册超时时间(毫秒)
     * 设备连接后需在此时间内完成注册，否则断开
     * 默认: 30秒
     */
    private Long registerTimeoutMs = 30000L;
}