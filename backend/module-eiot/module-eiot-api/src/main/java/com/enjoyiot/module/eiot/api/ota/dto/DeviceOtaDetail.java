package com.enjoyiot.module.eiot.api.ota.dto;

import com.enjoyiot.module.eiot.api.TenantModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/6/15 22:00
 * @Description:
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceOtaDetail  extends TenantModel {

    private Long id;

    private Integer step;

    private String taskId;

    private String remark;

    private String version;

    private String module;

    private Long deviceId;

    private String productKey;

    private String deviceName;

    private Long otaInfoId;
}
