package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "设备功能实例响应 VO")
public class DeviceFunctionRespVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "设备id")
    private Long deviceId;

    @Schema(description = "关联型号功能id")
    private Long brandModelFunctionId;

    @Schema(description = "功能名称")
    private String functionName;

    @Schema(description = "按钮名称")
    private String buttonName;

    @Schema(description = "展示顺序")
    private Integer sortOrder;

    @Schema(description = "参数值JSON")
    private String paramValues;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "绑定的子设备ID")
    private Long subDeviceId;

    @Schema(description = "功能类型：BUS(总线按钮)/MULTI(多线按钮)/GENERAL(通用按钮)")
    private String functionType;
}
