package com.enjoyiot.module.eiot.api.devicealert.dto;

import com.enjoyiot.module.eiot.api.TenantModel;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAlertRecord extends TenantModel {

    private Long id;

    private Long deviceId;
    
    private Long tenantId;

    private String productKey;

    private Long alertConfigId;

    private Long alertTime;

    private String alertState;

    private Long recoverTime;

    private String level;

    private String name;

    private String details;

    private Boolean readFlg;
}
