package com.enjoyiot.module.eiot.api.virtualdevice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 虚拟设备日志
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualDeviceLog {

    private Long id;

    /**
     * 虚拟设备id
     */
    private Long virtualDeviceId;

    /**
     * 虚拟设备名称
     */
    private String virtualDeviceName;

    /**
     * 关联设备数量
     */
    private int deviceTotal;

    /**
     * 虚拟设备执行结果
     */
    private String result;

    private Long logAt;

}
