package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "智能空开控制请求 VO")
public class ValveControlReqVO {

    @NotNull(message = "设备ID不能为空")
    @Schema(description = "设备ID（用于操作日志记录）")
    private Long deviceId;
}
