package com.enjoyiot.eiot.temporal.iotdb.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotdbDeviceProperty {

    private String id;

    private String deviceId;

    private String name;

    private Object value;

    private Long time;
}
