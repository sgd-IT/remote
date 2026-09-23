package com.enjoyiot.module.ai.dto;

import lombok.Data;

@Data
public class AiDataPageReqDTO {

    private Integer pageNo = 1;

    private Integer pageSize = 20;

    private Long propertyId;

    private Integer state;

    private String alarmType;

    private String orderStatus;

    private String alarmLevel;

    private String startTime;

    private String endTime;

    private String keyword;
}
