package com.enjoyiot.eiot.temporal.timescaledb.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@TableName("thing_model_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PgThingModelMessage {

    private Timestamp time;

    private String mid;

    private Long deviceId;

    private String productKey;

    private String deviceName;

    private String uid;

    private String type;

    private String identifier;

    private int code;

    private String data;

    private Long reportTime;

}
