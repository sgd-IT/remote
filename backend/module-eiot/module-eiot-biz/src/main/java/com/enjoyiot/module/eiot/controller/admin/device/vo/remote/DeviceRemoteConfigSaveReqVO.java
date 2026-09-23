package com.enjoyiot.module.eiot.controller.admin.device.vo.remote;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "设备远程配置保存请求 VO")
public class DeviceRemoteConfigSaveReqVO {

    @Schema(description = "品牌型号id")
    @NotNull(message = "品牌型号不能为空")
    private Long brandModelId;

    @Schema(description = "二次确认密码（空表示不设置）")
    private String confirmPassword;
}
