package com.enjoyiot.module.eiot.controller.admin.virtualdevice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;


/**
 * @author clickear
 */
@Schema(description = "管理后台 - 规则引擎设置状态 VO")
@Data
public class EiotVirtualSaveDevicesMappingVo {

    private static final long serialVersionUID = -1L;

    @NotNull(message = "id不能为空")
    @Schema(description = "id", example = "1")
    private Long id;


    @Schema(description = "设备列表", example = "[1]")
    private List<Long> devices;

}
