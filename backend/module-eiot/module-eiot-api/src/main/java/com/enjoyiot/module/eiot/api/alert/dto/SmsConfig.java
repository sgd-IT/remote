package com.enjoyiot.module.eiot.api.alert.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * author: EnjoyIot
 * date: 2023-05-08 15:58
 * description:
 **/
@Data
public class SmsConfig implements Serializable {
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String phoneNumbers;
}
