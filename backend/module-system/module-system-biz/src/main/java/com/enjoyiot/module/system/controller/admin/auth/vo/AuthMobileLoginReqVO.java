package com.enjoyiot.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

@Schema(description = "管理后台 - 账号密码登录 Request VO（小程序专用，免验证码）")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthMobileLoginReqVO {

    @Schema(description = "账号，手机号或用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "15601691300")
    @NotEmpty(message = "账号不能为空")
    @Length(min = 4, max = 16, message = "账号长度为 4-16 位")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "账号格式为数字以及字母")
    private String account;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "buzhidao")
    @NotEmpty(message = "密码不能为空")
    @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @Schema(description = "登录请求一次性随机令牌，用于防重放攻击", requiredMode = Schema.RequiredMode.REQUIRED, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotEmpty(message = "登录请求令牌不能为空")
    private String loginNonce;

}
