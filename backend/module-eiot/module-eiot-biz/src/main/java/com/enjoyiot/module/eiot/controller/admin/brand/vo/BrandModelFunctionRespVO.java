package com.enjoyiot.module.eiot.controller.admin.brand.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "型号功能响应 VO")
public class BrandModelFunctionRespVO {

    @Schema(description = "功能id")
    private Long id;

    @Schema(description = "品牌型号id")
    private Long brandModelId;

    @Schema(description = "四信原始功能ID")
    private String functionId;

    @Schema(description = "功能名称")
    private String name;

    @Schema(description = "规则号")
    private Integer ruleNum;

    @Schema(description = "等待时间(ms)")
    private Integer awaitTime;

    @Schema(description = "参数列表")
    private List<FunctionParamRespVO> params;

    @Data
    public static class FunctionParamRespVO {
        private Long id;
        private String paramName;
        private String paramCode;
        private String paramType;
    }
}
