
/*
 *
 *  * | Licensed 未经许可不能去掉「Enjoy-iot」相关版权
 *  * +----------------------------------------------------------------------
 *  * | Author: xw2sy@163.com | Tel: 19918996474
 *  * +----------------------------------------------------------------------
 *
 *  Copyright [2025] [Enjoy-iot] | Tel: 19918996474
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package com.enjoyiot.module.system.controller.admin.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.common.enums.CommonStatusEnum;
import com.enjoyiot.framework.common.enums.UserTypeEnum;
import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.security.config.SecurityProperties;
import com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.module.system.controller.admin.auth.vo.*;
import com.enjoyiot.module.system.convert.auth.AuthConvert;
import com.enjoyiot.module.system.dal.dataobject.permission.MenuDO;
import com.enjoyiot.module.system.dal.dataobject.permission.RoleDO;
import com.enjoyiot.module.system.dal.dataobject.user.AdminUserDO;
import com.enjoyiot.module.system.enums.logger.LoginLogTypeEnum;
import com.enjoyiot.module.system.service.auth.AdminAuthService;
import com.enjoyiot.module.system.service.permission.MenuService;
import com.enjoyiot.module.system.service.permission.PermissionService;
import com.enjoyiot.module.system.service.permission.RoleService;
import com.enjoyiot.module.system.service.social.SocialClientService;
import com.enjoyiot.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;
import static com.enjoyiot.framework.common.util.collection.CollectionUtils.convertSet;
import static com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 认证")
@RestController
@RequestMapping("/system/auth")
@Validated
@Slf4j
public class AuthController {

    @Resource
    private AdminAuthService authService;
    @Resource
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private SocialClientService socialClientService;

    @Resource
    private SecurityProperties securityProperties;

    @GetMapping("/get-tenant-id-by-username")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "根据用户名获取租户ID（需 nonce 防重放）")
    @Parameter(name = "username", description = "用户名", required = true)
    @Parameter(name = "nonce", description = "一次性随机令牌", required = true)
    public CommonResult<Long> getTenantIdByUsername(@RequestParam("username") String username,
                                                     @RequestParam("nonce") String nonce) {
        // 校验 nonce 一次性有效，防止重放攻击
        authService.validateLoginNonce(nonce);
        AdminUserDO user = userService.getUserByUsername(username);
        if (user == null) {
            // 用户不存在时返回 0（而非 null），避免攻击者通过响应内容差异枚举用户名
            return success(0L);
        }
        return success(user.getTenantId());
    }

    @PostMapping("/login")
    @PermitAll
    @Operation(summary = "使用账号密码登录")
    public CommonResult<AuthLoginRespVO> login(@RequestBody @Valid AuthLoginReqVO reqVO) {
        return success(authService.login(reqVO));
    }

    @GetMapping("/generate-login-nonce")
    @PermitAll
    @Operation(summary = "生成登录请求一次性随机令牌（防重放攻击）")
    public CommonResult<String> generateLoginNonce() {
        return success(authService.generateLoginNonce());
    }

    @PostMapping("/logout")
    @PermitAll
    @Operation(summary = "登出系统")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        }
        return success(true);
    }

    @PostMapping("/refresh-token")
    @PermitAll
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true)
    public CommonResult<AuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return success(authService.refreshToken(refreshToken));
    }

    @GetMapping("/get-permission-info")
    @Operation(summary = "获取登录用户的权限信息")
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            return success(null);
        }

        // 1.2 获得角色列表
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(getLoginUserId());
        if (CollUtil.isEmpty(roleIds)) {
            return success(AuthConvert.INSTANCE.convert(user, Collections.emptyList(), Collections.emptyList()));
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色

        // 1.3 获得菜单列表
        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
        List<MenuDO> menuList = menuService.getMenuList(menuIds);
        menuList = menuService.filterDisableMenus(menuList);

        // 2. 拼接结果返回
        return success(AuthConvert.INSTANCE.convert(user, roles, menuList));
    }

    // @PostMapping("/register")
    // @PermitAll
    // @Operation(summary = "注册用户")
    // public CommonResult<AuthLoginRespVO> register(@RequestBody @Valid AuthRegisterReqVO registerReqVO) {
    //     return success(authService.register(registerReqVO));
    // }

    // ========== 短信登录相关 ==========

    @PostMapping("/sms-login")
    @PermitAll
    @Operation(summary = "使用短信验证码登录")
    public CommonResult<AuthLoginRespVO> smsLogin(@RequestBody @Valid AuthSmsLoginReqVO reqVO) {
        return success(authService.smsLogin(reqVO));
    }

    @PostMapping("/send-sms-code")
    @PermitAll
    @Operation(summary = "发送手机验证码")
    public CommonResult<Boolean> sendLoginSmsCode(@RequestBody @Valid AuthSmsSendReqVO reqVO) {
        authService.sendSmsCode(reqVO);
        return success(true);
    }

    @PostMapping("/reset-password")
    @PermitAll
    @Operation(summary = "重置密码")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AuthResetPasswordReqVO reqVO) {
        authService.resetPassword(reqVO);
        return success(true);
    }

    // ========== 社交登录相关 ==========

    @PostMapping("/mobile-login")
    @PermitAll
    @Operation(summary = "账号密码登录（小程序）", description = "小程序专用，免验证码；账号可为手机号或用户名，跨租户定位后在其所属租户内登录")
    public CommonResult<AuthLoginRespVO> mobileLogin(@RequestBody @Valid AuthMobileLoginReqVO reqVO) {
        return success(authService.mobileLogin(reqVO));
    }

    @PostMapping("/wx-ma-phone")
    @PermitAll
    @Operation(summary = "获取微信小程序手机号", description = "通过 getPhoneNumber 事件获得的 phoneCode 换取手机号，用于登录页账号框一键回填")
    public CommonResult<String> getWxMaPhoneNumber(@RequestBody @Valid AuthWxMaLoginReqVO reqVO) {
        return success(authService.getWxMaPhoneNumber(reqVO));
    }

    @GetMapping("/social-auth-redirect")
    @PermitAll
    @Operation(summary = "社交授权的跳转")
    @Parameters({
            @Parameter(name = "type", description = "社交类型", required = true),
            @Parameter(name = "redirectUri", description = "回调路径")
    })
    public CommonResult<String> socialLogin(@RequestParam("type") Integer type,
                                            @RequestParam("redirectUri") String redirectUri) {
        return success(socialClientService.getAuthorizeUrl(
                type, UserTypeEnum.ADMIN.getValue(), redirectUri));
    }

    @PostMapping("/social-login")
    @PermitAll
    @Operation(summary = "社交快捷登录，使用 code 授权码", description = "适合未登录的用户，但是社交账号已绑定用户")
    public CommonResult<AuthLoginRespVO> socialQuickLogin(@RequestBody @Valid AuthSocialLoginReqVO reqVO) {
        return success(authService.socialLogin(reqVO));
    }

    // ========== SSO 免登录相关 ==========

    @PostMapping("/sso-login")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "SSO ticket 免登录", description = "第三方系统内嵌时，用一次性 ticket 换取 access_token")
    public CommonResult<AuthLoginRespVO> ssoLogin(@RequestBody @Valid AuthSsoLoginReqVO reqVO) {
        return success(authService.ssoLogin(reqVO.getTicket()));
    }

}
