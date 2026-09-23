package com.enjoyiot.eiot.temporal.rdb.model;

import lombok.Data;

@Data
public class RdbDeviceProperty {
    private Long time;
    private Long deviceId;
    private Object value;
}
