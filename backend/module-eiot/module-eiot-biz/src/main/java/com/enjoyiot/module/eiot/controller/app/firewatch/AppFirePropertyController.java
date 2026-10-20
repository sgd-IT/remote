package com.enjoyiot.module.eiot.controller.app.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FirePropertyRespVO;
import com.enjoyiot.module.eiot.service.firewatch.FirePropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 用户 App - 物业单位（移动端入口）
 *
 * 直调 Service（与 PC 共用实现）：若经 admin 版 Controller 转发，方法级安全会触发它自己的
 * @PreAuthorize 二次拦截（要求菜单权限点），移动端账号会被挡下。
 * 权限口径：账号允许移动端即可访问（system_users.user_type 含移动位，即 2/3），不校验菜单权限点。
 */
@Tag(name = "用户 App - 物业单位")
@RestController
@RequestMapping("/eiot/firewatch/property")
@Validated
public class AppFirePropertyController {

    @Resource
    private FirePropertyService firePropertyService;

    @GetMapping("/own")
    @Operation(summary = "获取当前租户的企业信息")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<FirePropertyRespVO> getOwnProperty() {
        Long tenantId = TenantContextHolder.getTenantId();
        return success(firePropertyService.getPropertyByTenantId(tenantId));
    }

}
