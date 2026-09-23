package com.enjoyiot.module.eiot.api.device.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceProperty {

    private String id;

    private String deviceId;

    private String name;

    private Object value;

    private Long time;

}
