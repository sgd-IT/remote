package com.enjoyiot.eiot.temporal.kw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KwDeviceProperty {

    private Timestamp time;

    private Long deviceId;

    private String name;

    private Object value;

}
