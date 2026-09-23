package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "设备功能保存请求 VO")
public class DeviceFunctionSaveReqVO {

    @Schema(description = "关联型号功能id")
    @NotNull(message = "功能不能为空")
    private Long brandModelFunctionId;

    @Schema(description = "按钮名称（为空时使用功能名称）")
    private String buttonName;

    @Schema(description = "展示顺序")
    private Integer sortOrder;

    @Schema(description = "参数值")
    private Map<String, Object> paramValues;

    @Schema(description = "绑定的子设备ID")
    private Long subDeviceId;

    @Schema(description = "功能类型：BUS/MULTI/GENERAL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "功能类型不能为空")
    private String functionType;
}
