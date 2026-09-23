package com.enjoyiot.module.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiAlarmDetailDTO {

    private Long id;

    private String orderNo;

    private String alarmType;

    private String alarmTypeName;

    private String alarmLevel;

    private String alarmLevelName;

    private LocalDateTime alarmTime;

    private String alarmLocation;

    private String alarmDescription;

    private Long deviceId;

    private String deviceName;

    private String deviceCode;

    private String deviceTypeName;

    private Long propertyId;

    private String propertyName;

    private String orderStatus;

    private String orderStatusName;

    private String handler;

    private LocalDateTime handleTime;

    private String handleType;

    private String handleRemark;

    private Integer responseDuration;

    private String aiSuggestion;
}
