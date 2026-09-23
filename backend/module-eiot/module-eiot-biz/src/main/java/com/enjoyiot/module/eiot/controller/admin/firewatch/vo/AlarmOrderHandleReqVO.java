package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 火警处置工单处置 Request VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 火警处置工单处置 Request VO")
@Data
public class AlarmOrderHandleReqVO {

    @Schema(description = "工单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工单ID不能为空")
    private Long id;

    @Schema(description = "处理类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "处理类型不能为空")
    private String handleType;

    @Schema(description = "处理备注")
    private String handleRemark;

    @Schema(description = "处理人")
    private String handler;

}
