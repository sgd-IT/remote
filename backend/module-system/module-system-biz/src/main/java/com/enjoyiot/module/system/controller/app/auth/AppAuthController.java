package com.enjoyiot.module.system.controller.app.auth;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.system.controller.admin.auth.AuthController;
import com.enjoyiot.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.enjoyiot.module.system.controller.admin.auth.vo.AuthMobileLoginReqVO;
import com.enjoyiot.module.system.controller.admin.auth.vo.AuthWxMaLoginReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 App - 认证（移动端入口）
 *
 * 薄转发到 admin 版 Controller：登录逻辑与 Web 端共用同一实现（mobile-login 本就是
 * /app-api 前缀由框架按 controller.app 包自动挂载，无需手工拼接。
 */
@Tag(name = "用户 App - 认证")
@RestController
@RequestMapping("/system/auth")
@Validated
public class AppAuthController {

    @Resource
    private AuthController authController;

    @GetMapping("/generate-login-nonce")
    @PermitAll
    @Operation(summary = "生成登录请求一次性随机令牌（防重放攻击）")
    public CommonResult<String> generateLoginNonce() {
        return authController.generateLoginNonce();
    }

    @PostMapping("/mobile-login")
    @PermitAll
    @Operation(summary = "账号密码登录（小程序）")
    public CommonResult<AuthLoginRespVO> mobileLogin(@RequestBody @Valid AuthMobileLoginReqVO reqVO) {
        return authController.mobileLogin(reqVO);
    }

    @PostMapping("/wx-ma-phone")
    @PermitAll
    @Operation(summary = "获取微信小程序手机号")
    public CommonResult<String> getWxMaPhoneNumber(@RequestBody @Valid AuthWxMaLoginReqVO reqVO) {
        return authController.getWxMaPhoneNumber(reqVO);
    }

    @PostMapping("/refresh-token")
    @PermitAll
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true)
    public CommonResult<AuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return authController.refreshToken(refreshToken);
    }

}
