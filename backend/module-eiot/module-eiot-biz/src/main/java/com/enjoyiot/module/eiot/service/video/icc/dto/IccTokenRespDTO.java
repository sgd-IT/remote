package com.enjoyiot.module.eiot.service.video.icc.dto;

import lombok.Data;

@Data
public class IccTokenRespDTO {
    private String accessToken;
    private String tokenType;
    private String refreshToken;
    private Long expiresIn;
    private String userId;
    private String magicId;
}
