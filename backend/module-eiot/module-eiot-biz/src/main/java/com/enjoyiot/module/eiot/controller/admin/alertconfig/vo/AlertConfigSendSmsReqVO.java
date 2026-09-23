package com.enjoyiot.module.eiot.controller.admin.alertconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

@Schema(description = "管理后台 - 发送短信通知 Request VO")
@Data
public class AlertConfigSendSmsReqVO {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotEmpty(message = "手机号不能为空")
    private String mobile;

    @Schema(description = "短信模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "fire_alert")
    @NotEmpty(message = "短信模板编码不能为空")
    private String templateCode;

    @Schema(description = "短信模板参数", example = "{\"deviceName\":\"烟感探测器01\",\"time\":\"2026-04-22 10:00\"}")
    private Map<String, Object> templateParams;

}
