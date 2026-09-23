package com.enjoyiot.module.eiot.controller.admin.virtualdevice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


/**
 * @author clickear
 */
@Schema(description = "管理后台 - 规则引擎设置状态 VO")
@Data
public class EiotVirtualSaveScriptVo {

    private static final long serialVersionUID = -1L;

    @NotNull(message = "id不能为空")
    @Schema(description = "id", example = "1")
    private Long id;


    @Schema(description = "执行脚本", example = "1")
    @NotEmpty(message = "脚本不能为空")
    private String script;

}
