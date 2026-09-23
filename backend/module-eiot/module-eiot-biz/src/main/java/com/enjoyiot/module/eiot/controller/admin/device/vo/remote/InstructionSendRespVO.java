package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "指令下发响应 VO")
public class InstructionSendRespVO {

    @Schema(description = "下发结果：true成功 false失败")
    private Boolean success;

    @Schema(description = "结果消息")
    private String message;
}
