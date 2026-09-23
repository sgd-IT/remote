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
public class DeviceOtaInfo  extends TenantModel {

    private Long id;

    private Long packageId;

    private String remark;

    private String version;

    private String module;

    private Integer total;

    private Integer success;

    private Integer fail;

    private String productKey;

    private Long createAt;

}
