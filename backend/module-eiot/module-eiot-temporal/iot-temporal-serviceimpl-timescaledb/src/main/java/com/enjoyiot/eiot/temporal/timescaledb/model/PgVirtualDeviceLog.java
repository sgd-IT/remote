package com.enjoyiot.eiot.temporal.timescaledb.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@TableName("virtual_device_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PgVirtualDeviceLog {

    private Timestamp time;

    private Long virtualDeviceId;

    private String virtualDeviceName;

    private int deviceTotal;

    private String result;

}
