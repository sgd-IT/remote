package com.enjoyiot.module.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiDeviceDetailDTO {

    private Long id;

    private String name;

    private String dn;

    private String serialNo;

    private Integer state;

    private String stateName;

    private String model;

    private Long propertyId;

    private String propertyName;

    private String addr;

    private Double lon;

    private Double lat;

    private String productKey;

    private String firmVersion;

    private Long tenantId;

    private LocalDateTime createTime;
}