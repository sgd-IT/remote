package com.enjoyiot.module.eiot.controller.admin.brand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "品牌级联数据 VO")
public class BrandCascadeRespVO {

    @Schema(description = "品牌id")
    private Long id;

    @Schema(description = "品牌代码")
    private String brandCode;

    @Schema(description = "品牌名称")
    private String name;

    @Schema(description = "型号列表")
    private List<ModelVO> models;

    @Data
    public static class ModelVO {
        private Long id;
        private String model;
        private List<String> versions;
    }
}
