package com.enjoyiot.module.eiot.controller.admin.brand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "品牌响应 VO")
public class BrandRespVO {

    @Schema(description = "品牌id")
    private Long id;

    @Schema(description = "品牌代码")
    private String brandCode;

    @Schema(description = "品牌名称")
    private String name;
}
