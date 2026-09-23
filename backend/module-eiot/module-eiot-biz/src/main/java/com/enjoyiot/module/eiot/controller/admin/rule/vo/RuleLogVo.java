package com.enjoyiot.module.eiot.controller.admin.rule.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema(description = "RuleLogVo")
@Data

public class RuleLogVo implements Serializable {
    private static final long serialVersionUID = -1L;

    @Schema(description = "时间")
    private Long logAt;

    @Schema(description = "规则id")
    private String ruleId;

    @Schema(description = "状态")
    private String state;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "是否成功")
    private Boolean success;

}
