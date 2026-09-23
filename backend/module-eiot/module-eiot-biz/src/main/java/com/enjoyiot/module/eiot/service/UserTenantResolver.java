package com.enjoyiot.module.eiot.service;

import com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.system.api.user.AdminUserApi;
import com.enjoyiot.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户账号所属租户解析器
 * <p>
 * 读取登录用户账号记录（system_users.tenant_id）确定其所属租户，与请求的 tenant-id header 无关，
 * 用于告警等数据的按租户过滤（系统租户账号仍可看全部，由租户拦截器规则保证）。
 */
@Slf4j
@Component
public class UserTenantResolver {

    @Resource
    private AdminUserApi adminUserApi;

    /**
     * 解析当前登录用户账号记录所属租户 ID；未登录或用户不存在时返回 null
     */
    public Long resolveLoginUserTenantId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return null;
        }
        try {
            // 忽略租户上下文读取账号记录，避免被租户拦截器按请求租户过滤而查不到跨租户账号
            AdminUserRespDTO user = TenantUtils.executeIgnoreWithResult(() -> adminUserApi.getUser(userId));
            return user != null ? user.getTenantId() : null;
        } catch (Exception e) {
            log.warn("[resolveLoginUserTenantId] 解析登录用户租户失败，userId={}", userId, e);
            return null;
        }
    }
}