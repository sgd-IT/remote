package com.enjoyiot.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 微信小程序手机号换取 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthWxMaLoginReqVO {

    @Schema(description = "手机号 code，通过 getPhoneNumber 事件获得", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "手机号 code 不能为空")
    private String phoneCode;

}
