/*  
 *  * | Licensed 未经许可不能去掉「Enjoy-iot」相关版权
 *  * +----------------------------------------------------------------------
 *  * | Author: xw2sy@163.com | Tel: 19918996474
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
package com.enjoyiot.module.system.controller.admin.sso;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.module.system.framework.sso.config.SsoProperties;
import com.enjoyiot.module.system.service.auth.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

import static com.enjoyiot.framework.common.pojo.CommonResult.error;
import static com.enjoyiot.framework.common.pojo.CommonResult.success;
import static com.enjoyiot.module.system.enums.ErrorCodeConstants.AUTH_SSO_APP_KEY_INVALID;

@Tag(name = "管理后台 - SSO ticket 签发")
@RestController
@RequestMapping("/system/sso")
@Validated
@Slf4j
public class SsoTicketController {

    @Resource
    private AdminAuthService authService;

    @Resource
    private SsoProperties ssoProperties;

    @PostMapping("/ticket/create")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "创建 SSO ticket（供第三方系统后端调用）")
    public CommonResult<SsoTicketCreateRespVO> createTicket(@RequestBody @Valid SsoTicketCreateReqVO reqVO) {
        // 校验 appKey + secret，并获取绑定的用户名
        SsoProperties.SsoApp app = ssoProperties.getApps().stream()
                .filter(item -> item.getAppKey().equals(reqVO.getAppKey()))
                .findFirst().orElse(null);
        if (app == null || !app.getSecret().equals(reqVO.getSecret())) {
            log.warn("[SSO] ticket 创建失败：appKey 或 secret 校验不通过，appKey={}", reqVO.getAppKey());
            return error(AUTH_SSO_APP_KEY_INVALID);
        }
        String ticket = authService.generateSsoTicket(app.getUsername());
        log.info("[SSO] ticket 已创建，appKey={}，username={}，ticket={}", reqVO.getAppKey(), app.getUsername(), ticket);
        return success(SsoTicketCreateRespVO.builder().ticket(ticket).build());
    }

}