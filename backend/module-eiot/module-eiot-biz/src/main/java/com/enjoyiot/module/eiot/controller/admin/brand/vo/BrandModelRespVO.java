package com.enjoyiot.module.eiot.controller.admin.brand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "品牌型号版本响应 VO")
public class BrandModelRespVO {

    @Schema(description = "型号id")
    private Long id;

    @Schema(description = "品牌id")
    private Long brandId;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "同步时间")
    private LocalDateTime syncTime;
}
