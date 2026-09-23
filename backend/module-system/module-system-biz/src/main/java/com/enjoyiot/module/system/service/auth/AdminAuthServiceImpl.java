
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
package com.enjoyiot.module.system.service.auth;

import cn.hutool.core.util.ObjectUtil;
import com.enjoyiot.framework.common.enums.CommonStatusEnum;
import com.enjoyiot.framework.common.enums.UserTypeEnum;
import com.enjoyiot.framework.common.util.monitor.TracerUtils;
import com.enjoyiot.framework.common.util.servlet.ServletUtils;
import com.enjoyiot.framework.common.util.validation.ValidationUtils;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.system.api.logger.dto.LoginLogCreateReqDTO;
import com.enjoyiot.module.system.api.sms.SmsCodeApi;
import com.enjoyiot.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import com.enjoyiot.module.system.api.social.dto.SocialUserBindReqDTO;
import com.enjoyiot.module.system.api.social.dto.SocialUserRespDTO;
import com.enjoyiot.module.system.controller.admin.auth.vo.*;
import com.enjoyiot.module.system.convert.auth.AuthConvert;
import com.enjoyiot.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.enjoyiot.module.system.dal.dataobject.user.AdminUserDO;
import com.enjoyiot.module.system.enums.logger.LoginLogTypeEnum;
import com.enjoyiot.module.system.enums.logger.LoginResultEnum;
import com.enjoyiot.module.system.enums.oauth2.OAuth2ClientConstants;
import com.enjoyiot.module.system.enums.sms.SmsSceneEnum;
import com.enjoyiot.module.system.service.logger.LoginLogService;
import com.enjoyiot.module.system.service.member.MemberService;
import com.enjoyiot.module.system.service.oauth2.OAuth2TokenService;
import com.enjoyiot.module.system.service.social.SocialClientService;
import com.enjoyiot.module.system.service.social.SocialUserService;
import com.enjoyiot.module.system.service.user.AdminUserService;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.google.common.annotations.VisibleForTesting;
import com.xingyuv.captcha.model.common.ResponseModel;
import com.xingyuv.captcha.model.vo.CaptchaVO;
import com.xingyuv.captcha.service.CaptchaService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.validation.Validator;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.enjoyiot.framework.common.util.servlet.ServletUtils.getClientIP;
import static com.enjoyiot.module.system.enums.ErrorCodeConstants.*;

/**
 * Auth Service 实现类
 *
 * @author EnjoyIot
 */
@Service
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    @Resource
    private AdminUserService userService;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private SocialUserService socialUserService;
    @Resource
    private MemberService memberService;
    @Resource
    private Validator validator;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private SmsCodeApi smsCodeApi;
    @Resource
    private SocialClientService socialClientService;

    /**
     * 验证码的开关，默认为 true
     */
    @Value("${platform.captcha.enable:true}")
    @Setter // 为了单测：开启或者关闭验证码
    private Boolean captchaEnable;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 密码连续错误次数上限，达到后锁定账号，默认为 5 次
     */
    @Value("${platform.login.fail-max-count:5}")
    private Integer loginFailMaxCount;

    /**
     * 账号锁定时长，默认为 10 分钟
     */
    @Value("${platform.login.lock-duration:10m}")
    private Duration loginLockDuration;

    /**
     * 登录失败计数 Redis Key 前缀（按用户名）
     */
    private static final String LOGIN_FAIL_COUNT_KEY_PREFIX = "admin_login:fail_count:";

    /**
     * 登录锁定 Redis Key 前缀（按用户名）
     */
    private static final String LOGIN_LOCK_KEY_PREFIX = "admin_login:locked:";

    /**
     * 登录失败计数 Redis Key 前缀（按 IP）
     */
    private static final String LOGIN_IP_FAIL_COUNT_KEY_PREFIX = "admin_login:ip_fail_count:";

    /**
     * 登录锁定 Redis Key 前缀（按 IP）
     */
    private static final String LOGIN_IP_LOCK_KEY_PREFIX = "admin_login:ip_locked:";

    /**
     * 登录防重放 nonce Redis Key 前缀
     */
    private static final String LOGIN_NONCE_KEY_PREFIX = "admin_login:nonce:";

    /**
     * 登录 nonce 有效期，默认 5 分钟
     */
    @Value("${platform.login.nonce-ttl:5m}")
    private Duration loginNonceTtl;

    /**
     * SSO ticket 共享密钥，默认 demo-secret
     */
    @Value("${platform.sso.ticket-secret:demo-secret}")
    private String ssoTicketSecret;

    /**
     * SSO ticket Redis Key 前缀
     */
    private static final String SSO_TICKET_KEY_PREFIX = "sso:ticket:";

    /**
     * SSO ticket 有效期，默认 60 秒
     */
    @Value("${platform.sso.ticket-ttl:60s}")
    private Duration ssoTicketTtl;

    @Override
    public String generateSsoTicket(String username) {
        String ticket = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(SSO_TICKET_KEY_PREFIX + ticket, username, ssoTicketTtl);
        return ticket;
    }


    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString()) ;
    }

    @Override
    public AuthLoginRespVO ssoLogin(String ticket) {
        // 通过 Lua 脚本原子性地获取并删除 ticket，保证一次性使用
        String key = SSO_TICKET_KEY_PREFIX + ticket;
        String username = stringRedisTemplate.execute(LOGIN_NONCE_CONSUME_SCRIPT, List.of(key));
        if (username == null) {
            throw exception(AUTH_SSO_TICKET_INVALID);
        }
        // 查找用户
        AdminUserDO user = userService.getUserByUsername(username);
        if (user == null) {
            throw exception(AUTH_SSO_USER_NOT_FOUND);
        }
        // 设置租户上下文，确保创建的 Token 携带租户编号（否则后续请求会因缺失租户标识被拦截）
        if (user.getTenantId() != null) {
            TenantContextHolder.setTenantId(user.getTenantId());
        }
        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), username, LoginLogTypeEnum.LOGIN_SSO);
    }

    @Override
    public AdminUserDO authenticate(String username, String password) {
        final LoginLogTypeEnum logTypeEnum = LoginLogTypeEnum.LOGIN_USERNAME;
        final String clientIp = getClientIP();
        // 校验账号是否已被锁定（密码错误次数过多）
        checkLoginLocked(username);
        // 校验 IP 是否已被锁定（同一 IP 失败次数过多）
        checkIpLoginLocked(clientIp);
        // 校验账号是否存在
        AdminUserDO user = userService.getUserByUsername(username);
        if (user == null) {
            createLoginLog(null, username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
            handleLoginFailure(username, clientIp);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        if (!userService.isPasswordMatch(password, user.getPassword())) {
            createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
            handleLoginFailure(username, clientIp);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        // 校验是否禁用
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.USER_DISABLED);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
        // 登录成功，清除失败计数与锁定状态
        clearLoginFailure(username, clientIp);
        return user;
    }

    /**
     * 检查账号是否处于锁定状态
     *
     * @param username 用户名
     */
    private void checkLoginLocked(String username) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(loginLockKey(username)))) {
            throw exception(AUTH_LOGIN_FAIL_LOCKED, (int) loginLockDuration.toMinutes());
        }
    }

    /**
     * 检查 IP 是否处于锁定状态
     *
     * @param ip 客户端 IP
     */
    private void checkIpLoginLocked(String ip) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(ipLoginLockKey(ip)))) {
            throw exception(AUTH_LOGIN_IP_LOCKED, (int) loginLockDuration.toMinutes());
        }
    }

    /**
     * 处理登录失败：累计失败次数，达到上限后锁定账号和 IP
     *
     * @param username 用户名
     * @param ip       客户端 IP
     */
    private void handleLoginFailure(String username, String ip) {
        // --- 按用户名维度 ---
        String failCountKey = loginFailCountKey(username);
        Long failCount = stringRedisTemplate.opsForValue().increment(failCountKey);
        // 每次失败都刷新过期时间：INCR 与 EXPIRE 非原子，只在首次设置可能因进程中断留下永不过期的计数而永久锁死账号
        if (failCount != null) {
            stringRedisTemplate.expire(failCountKey, loginLockDuration);
        }
        if (failCount != null && failCount >= loginFailMaxCount) {
            stringRedisTemplate.opsForValue().set(loginLockKey(username), "1", loginLockDuration);
            stringRedisTemplate.delete(failCountKey);
        }

        // --- 按 IP 维度 ---
        String ipFailCountKey = ipLoginFailCountKey(ip);
        Long ipFailCount = stringRedisTemplate.opsForValue().increment(ipFailCountKey);
        if (ipFailCount != null) { // 同上：每次失败刷新过期时间，避免计数键永不过期把 IP 永久锁死
            stringRedisTemplate.expire(ipFailCountKey, loginLockDuration);
        }
        if (ipFailCount != null && ipFailCount >= loginFailMaxCount) {
            stringRedisTemplate.opsForValue().set(ipLoginLockKey(ip), "1", loginLockDuration);
            stringRedisTemplate.delete(ipFailCountKey);
        }
    }

    /**
     * 登录成功后，清除用户名和 IP 维度的失败计数与锁定状态
     *
     * @param username 用户名
     * @param ip       客户端 IP
     */
    private void clearLoginFailure(String username, String ip) {
        stringRedisTemplate.delete(loginFailCountKey(username));
        stringRedisTemplate.delete(loginLockKey(username));
        stringRedisTemplate.delete(ipLoginFailCountKey(ip));
        stringRedisTemplate.delete(ipLoginLockKey(ip));
    }

    private static String loginFailCountKey(String username) {
        return LOGIN_FAIL_COUNT_KEY_PREFIX + username;
    }

    private static String loginLockKey(String username) {
        return LOGIN_LOCK_KEY_PREFIX + username;
    }

    private static String ipLoginFailCountKey(String ip) {
        return LOGIN_IP_FAIL_COUNT_KEY_PREFIX + ip;
    }

    private static String ipLoginLockKey(String ip) {
        return LOGIN_IP_LOCK_KEY_PREFIX + ip;
    }

    @Override
    public AuthLoginRespVO login(AuthLoginReqVO reqVO) {
        // 校验登录 nonce（防重放攻击）
        validateLoginNonce(reqVO.getLoginNonce());

        // 校验验证码
        validateCaptcha(reqVO);

        // 使用账号密码，进行登录
        AdminUserDO user = authenticate(reqVO.getUsername(), reqVO.getPassword());

        // 如果 socialType 非空，说明需要绑定社交用户
        if (reqVO.getSocialType() != null) {
            socialUserService.bindSocialUser(new SocialUserBindReqDTO(user.getId(), getUserType().getValue(),
                    reqVO.getSocialType(), reqVO.getSocialCode(), reqVO.getSocialState()));
        }
        // 更新用户类型：Web登录
        userService.updateUserType(user.getId(), 1);
        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME);
    }

    @Override
    public void sendSmsCode(AuthSmsSendReqVO reqVO) {
        // 如果是重置密码场景，需要校验图形验证码是否正确
        if (Objects.equals(SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene(), reqVO.getScene())) {
            ResponseModel response = doValidateCaptcha(reqVO);
            if (!response.isSuccess()) {
                throw exception(AUTH_REGISTER_CAPTCHA_CODE_ERROR, response.getRepMsg());
            }
        }

        // 登录场景，验证是否存在
        if (userService.getUserByMobile(reqVO.getMobile()) == null) {
            throw exception(AUTH_MOBILE_NOT_EXISTS);
        }
        // 发送验证码
        smsCodeApi.sendSmsCode(AuthConvert.INSTANCE.convert(reqVO).setCreateIp(getClientIP()));
    }

    @Override
    public AuthLoginRespVO smsLogin(AuthSmsLoginReqVO reqVO) {
        // 校验验证码
        smsCodeApi.useSmsCode(AuthConvert.INSTANCE.convert(reqVO, SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene(), getClientIP()));

        // 获得用户信息
        AdminUserDO user = userService.getUserByMobile(reqVO.getMobile());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }

        // 更新用户类型：App登录
        userService.updateUserType(user.getId(), 2);
        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), reqVO.getMobile(), LoginLogTypeEnum.LOGIN_MOBILE);
    }

    private void createLoginLog(Long userId, String username,
                                LoginLogTypeEnum logTypeEnum, LoginResultEnum loginResult) {
        // 插入登录日志
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logTypeEnum.getType());
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(getUserType().getValue());
        reqDTO.setUsername(username);
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(loginResult.getResult());
        loginLogService.createLoginLog(reqDTO);
        // 更新最后登录时间
        if (userId != null && Objects.equals(LoginResultEnum.SUCCESS.getResult(), loginResult.getResult())) {
            userService.updateUserLogin(userId, ServletUtils.getClientIP());
        }
    }

    @Override
    public AuthLoginRespVO socialLogin(AuthSocialLoginReqVO reqVO) {
        // 使用 code 授权码，进行登录。然后，获得到绑定的用户编号
        SocialUserRespDTO socialUser = socialUserService.getSocialUserByCode(UserTypeEnum.ADMIN.getValue(), reqVO.getType(),
                reqVO.getCode(), reqVO.getState());
        if (socialUser == null || socialUser.getUserId() == null) {
            throw exception(AUTH_THIRD_LOGIN_NOT_BIND);
        }

        // 获得用户
        AdminUserDO user = userService.getUser(socialUser.getUserId());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }

        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), user.getUsername(), LoginLogTypeEnum.LOGIN_SOCIAL);
    }

    @VisibleForTesting
    void validateCaptcha(AuthLoginReqVO reqVO) {
        ResponseModel response = doValidateCaptcha(reqVO);
        // 校验验证码
        if (!response.isSuccess()) {
            // 创建登录失败日志（验证码不正确)
            createLoginLog(null, reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.CAPTCHA_CODE_ERROR);
            throw exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
    }

    private ResponseModel doValidateCaptcha(CaptchaVerificationReqVO reqVO) {
        // 如果验证码关闭，则不进行校验
        if (!captchaEnable) {
            return ResponseModel.success();
        }
        ValidationUtils.validate(validator, reqVO, CaptchaVerificationReqVO.CodeEnableGroup.class);
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(reqVO.getCaptchaVerification());
        return captchaService.verification(captchaVO);
    }

    private AuthLoginRespVO createTokenAfterLoginSuccess(Long userId, String username, LoginLogTypeEnum logType) {
        // 插入登陆日志
        createLoginLog(userId, username, logType, LoginResultEnum.SUCCESS);
        // 创建访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToken(userId, getUserType().getValue(),
                OAuth2ClientConstants.CLIENT_ID_DEFAULT, null);
        // 构建返回结果
        return AuthConvert.INSTANCE.convert(accessTokenDO);
    }

    @Override
    public AuthLoginRespVO refreshToken(String refreshToken) {
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.refreshAccessToken(refreshToken, OAuth2ClientConstants.CLIENT_ID_DEFAULT);
        return AuthConvert.INSTANCE.convert(accessTokenDO);
    }

    @Override
    public void logout(String token, Integer logType) {
        // 删除访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.removeAccessToken(token);
        if (accessTokenDO == null) {
            return;
        }
        // 删除成功，则记录登出日志
        createLogoutLog(accessTokenDO.getUserId(), accessTokenDO.getUserType(), logType);
    }

    private void createLogoutLog(Long userId, Integer userType, Integer logType) {
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logType);
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(userType);
        if (ObjectUtil.equal(getUserType().getValue(), userType)) {
            reqDTO.setUsername(getUsername(userId));
        } else {
            reqDTO.setUsername(memberService.getMemberUserMobile(userId));
        }
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(LoginResultEnum.SUCCESS.getResult());
        loginLogService.createLoginLog(reqDTO);
    }

    private String getUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserDO user = userService.getUser(userId);
        return user != null ? user.getUsername() : null;
    }

    private UserTypeEnum getUserType() {
        return UserTypeEnum.ADMIN;
    }

    @Override
    public AuthLoginRespVO register(AuthRegisterReqVO registerReqVO) {
        // 1. 校验验证码
        validateCaptcha(registerReqVO);

        // 2. 校验用户名是否已存在
        Long userId = userService.registerUser(registerReqVO);

        // 3. 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(userId, registerReqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME);
    }

    @VisibleForTesting
    void validateCaptcha(AuthRegisterReqVO reqVO) {
        ResponseModel response = doValidateCaptcha(reqVO);
        // 验证不通过
        if (!response.isSuccess()) {
            throw exception(AUTH_REGISTER_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
    }

    @Override
    public AuthLoginRespVO mobileLogin(AuthMobileLoginReqVO reqVO) {
        // 1. 校验登录 nonce（防重放攻击）
        validateLoginNonce(reqVO.getLoginNonce());

        // 2. 跨租户定位用户：小程序固定携带租户头，按当前租户查询会漏掉其它租户的已有账号
        List<AdminUserDO> users = userService.getUserListByAccountIgnoreTenant(reqVO.getAccount());
        if (users.isEmpty()) {
            // 不写登录日志：此时租户上下文仍为请求头租户，日志会归属到错误的租户
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        if (users.size() > 1) {
            // 同一手机号归属多个租户，无法判断本次要进入哪个单位，交由运营处理重复账号（用户名全局唯一，不会进此分支）
            throw exception(AUTH_LOGIN_MOBILE_TENANT_DUPLICATE);
        }

        // 3. 在用户所属租户的上下文内校验密码并发令牌：authenticate 复用账号密码登录的
        //    失败计数锁定、禁用校验与登录日志；令牌的租户编号取自上下文（见 OAuth2TokenServiceImpl）
        AdminUserDO user = users.get(0);
        return TenantUtils.execute(user.getTenantId(), () -> {
            AdminUserDO loginUser = authenticate(user.getUsername(), reqVO.getPassword());
            userService.updateUserType(loginUser.getId(), 3); // 3 = Wx 小程序端
            return createTokenAfterLoginSuccess(loginUser.getId(), reqVO.getAccount(), LoginLogTypeEnum.LOGIN_MOBILE);
        });
    }

    @Override
    public String getWxMaPhoneNumber(AuthWxMaLoginReqVO reqVO) {
        // phoneCode 一次性且由微信保证为本人号码，手机号仅回传给本人，不打印到日志
        WxMaPhoneNumberInfo phoneInfo = socialClientService.getWxMaPhoneNumberInfo(
                UserTypeEnum.ADMIN.getValue(), reqVO.getPhoneCode());
        String mobile = phoneInfo.getPhoneNumber();
        if (mobile == null || mobile.isEmpty()) {
            throw exception(AUTH_MOBILE_NOT_EXISTS);
        }
        return mobile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AuthResetPasswordReqVO reqVO) {
        AdminUserDO userByMobile = userService.getUserByMobile(reqVO.getMobile());
        if (userByMobile == null) {
            throw exception(USER_MOBILE_NOT_EXISTS);
        }

        smsCodeApi.useSmsCode(new SmsCodeUseReqDTO()
                .setCode(reqVO.getCode())
                .setMobile(reqVO.getMobile())
                .setScene(SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene())
                .setUsedIp(getClientIP())
        );

        userService.updateUserPassword(userByMobile.getId(), reqVO.getPassword());
    }

    // ========== 登录防重放 nonce 相关 ==========

    /**
     * Lua 脚本：原子性地获取并删除 nonce，兼容旧版本 Redis（GETDEL 需要 Redis 6.2+）
     */
    private static final DefaultRedisScript<String> LOGIN_NONCE_CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); if v then redis.call('DEL', KEYS[1]); end; return v",
            String.class);

    @Override
    public String generateLoginNonce() {
        String nonce = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(LOGIN_NONCE_KEY_PREFIX + nonce, "1", loginNonceTtl);
        return nonce;
    }

    /**
     * 校验登录 nonce，确保每个 nonce 仅可使用一次，防止重放攻击
     *
     * @param nonce 客户端传入的 nonce
     */
    @Override
    public void validateLoginNonce(String nonce) {
        // 通过 Lua 脚本原子性地获取并删除 nonce，保证一次性使用
        String key = LOGIN_NONCE_KEY_PREFIX + nonce;
        String value = stringRedisTemplate.execute(LOGIN_NONCE_CONSUME_SCRIPT, List.of(key));
        if (value == null) {
            // nonce 不存在或已被使用
            throw exception(AUTH_LOGIN_NONCE_INVALID);
        }
    }
}
