package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "指令下发请求 VO")
public class InstructionSendReqVO {

    @Schema(description = "设备id")
    @NotNull(message = "设备id不能为空")
    private Long deviceId;

    @Schema(description = "设备功能实例id")
    @NotNull(message = "功能id不能为空")
    private Long deviceFunctionId;

    @Schema(description = "二次确认密码")
    private String password;

    @Schema(description = "指令类型 (0:关,1:开)")
    private String commandType;
}
