package com.enjoyiot.module.eiot.api.devicealert.dto;

import com.enjoyiot.module.eiot.api.TenantModel;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAlertConfig extends TenantModel {

    private Long id;

    private String name;

    private String productKey;

    private Long deviceId;

    private String level;

    private List<AlertCondition> conditions;

    private TriggerOptions triggerOptions;

    private Integer status;

    private String remark;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertCondition {
        private String type;
        private String key;
        private String operator;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerOptions {
        private Integer durationSec;
        private Integer silentSec;
        private Boolean enableRecover;
    }

    public Boolean isEnable() {
        return status != null && status == 0;
    }
}
