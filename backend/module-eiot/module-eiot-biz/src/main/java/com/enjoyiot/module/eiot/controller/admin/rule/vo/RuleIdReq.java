package com.enjoyiot.module.eiot.controller.admin.rule.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Schema(description = "RuleIdReq")
@Data


public class RuleIdReq {

    private static final long serialVersionUID = -1L;


    @Schema(description = "规则id")
    @NotNull(message = "规则id不许为空")
    private Long id;


}
