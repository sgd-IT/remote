package com.enjoyiot.module.eiot.api.device.dto;


import com.enjoyiot.module.eiot.api.TenantModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConfig  extends TenantModel {

    private Long id;

    private Long deviceId;

    /**
     * 产品key
     */
    private String productKey;

    private String deviceName;

    /**
     * 设备配置json内容
     */
    private String config;

    private Long createAt;
}
