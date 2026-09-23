package com.enjoyiot.eiot.component.ff.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异步远程控制请求上下文
 *
 * 用于关联下行 remote_control 服务调用与设备后续上行 cmd=05 应答。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRequest {

    /** 服务调用的 invokeId */
    private String invokeId;

    /** 父设备 productKey */
    private String productKey;

    /** 父设备 deviceName */
    private String deviceName;

    /** 业务流水号（对应帧中的 serialNumber） */
    private int serialNumber;

    /** 服务名称，如 remote_control */
    private String serviceName;

    /** 请求发送时间戳（毫秒） */
    private long sendTime;
}
