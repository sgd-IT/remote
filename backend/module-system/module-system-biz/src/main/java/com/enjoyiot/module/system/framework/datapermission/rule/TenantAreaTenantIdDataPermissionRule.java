package com.enjoyiot.module.system.framework.datapermission.rule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.SqlParserUtils;
import com.enjoyiot.framework.datapermission.core.rule.DataPermissionRule;
import com.enjoyiot.framework.mybatis.core.util.MyBatisUtils;
import com.enjoyiot.framework.security.core.LoginUser;
import com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils;
import com.enjoyiot.framework.tenant.config.TenantProperties;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.system.service.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户区域 TenantId 数据权限规则
 *
 * 针对系统租户监管角色用户，在查询所有业务表时自动追加：
 *   tenant_id IN (区域内所有tenantId列表)
 *
 * 过滤逻辑：
 * 1. 企业租户用户 → 返回 null（由 MyBatis Plus 多租户拦截器的 tenant_id = ? 隔离）
 * 2. 系统租户 + 非监管角色 → 返回 null（可查所有企业数据，不追加任何条件）
 * 3. 系统租户 + 监管角色 + 有 areaCode → 查出区域内所有 tenantId，追加 tenant_id IN (...)
 * 4. 系统租户 + 监管角色 + 无 areaCode → 安全兜底，追加 tenant_id = -1（空集）
 *
 * 注意：本 Rule 不作用于 ignore-tables 中配置的表（这些表无 tenant_id 列或无需租户隔离）
 *
 * @author EnjoyIot
 */
@Slf4j
public class TenantAreaTenantIdDataPermissionRule implements DataPermissionRule {

    /**
     * tenant_id 列名
     */
    private static final String TENANT_ID_COLUMN = "tenant_id";

    private final TenantService tenantService;

    /**
     * 需要排除的表名集合（与 TenantDatabaseInterceptor 的 ignoreTables 保持一致）
     */
    private final Set<String> ignoreTables = new HashSet<>();

    public TenantAreaTenantIdDataPermissionRule(TenantService tenantService, TenantProperties properties) {
        this.tenantService = tenantService;
        // 构建 ignore-tables 集合，大小写都添加，与 TenantDatabaseInterceptor 逻辑一致
        properties.getIgnoreTables().forEach(table -> {
            ignoreTables.add(table.toLowerCase());
            ignoreTables.add(table.toUpperCase());
        });
    }

    @Override
    public Set<String> getTableNames() {
        // 返回 null 表示全表匹配模式，配合 isMatchTable() 排除不需要过滤的表
        return null;
    }

    @Override
    public boolean isMatchTable(String tableName) {
        String cleanName = SqlParserUtils.removeWrapperSymbol(tableName);
        // 排除 ignore-tables 中配置的表（这些表无 tenant_id 列或不需要租户隔离）
        return !CollUtil.contains(ignoreTables, cleanName);
    }

    @Override
    public Expression getExpression(String tableName, Alias tableAlias) {
        // 全局忽略租户（@TenantIgnore 注解 或 ignore-urls 配置）→ 不追加 tenant_id 条件
        if (TenantContextHolder.isIgnore()) {
            return null;
        }

        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return null;
        }

        // 如果上下文租户与登录用户租户不同，说明是 TenantUtils.execute() 切换到了特定租户，
        // 由 TenantLineInnerInterceptor 按 tenant_id = contextTenantId 过滤即可
        Long contextTenantId = TenantContextHolder.getTenantId();
        if (contextTenantId != null && !contextTenantId.equals(loginUser.getTenantId())) {
            return null;
        }

        String isSystemTenantStr = getInfoValue(loginUser, LoginUser.INFO_KEY_IS_SYSTEM_TENANT);
        String isMonitorRoleStr  = getInfoValue(loginUser, LoginUser.INFO_KEY_IS_MONITOR_ROLE);

        // 情况一：企业租户用户 → 不干预，由多租户拦截器处理
        if (!"true".equals(isSystemTenantStr)) {
            return null;
        }

        // 情况二：系统租户 + 非监管角色 → 可看所有数据，不追加条件
        if (!"true".equals(isMonitorRoleStr)) {
            return null;
        }

        // 情况三：系统租户 + 监管角色 → 按区域过滤
        String areaCode = getInfoValue(loginUser, LoginUser.INFO_KEY_AREA_CODE);
        if (StrUtil.isEmpty(areaCode)) {
            // 安全兜底：监管角色没有区域信息时，返回不可能成立的条件，不让其查到任何数据
            log.warn("[getExpression][系统租户监管角色用户({}) 未配置区域信息，tenant_id 过滤为空集]",
                    loginUser.getId());
            return buildFalseExpression(tableName, tableAlias);
        }

        // 查询区域内的 tenantId 列表
        List<Long> tenantIds = new java.util.ArrayList<>(tenantService.getTenantIdListByAreaCode(areaCode));
        // 始终包含当前用户的 tenantId（系统租户自身），避免系统租户的记录被自己的数据权限规则过滤
        Long currentTenantId = loginUser.getTenantId();
        if (currentTenantId != null && !tenantIds.contains(currentTenantId)) {
            tenantIds.add(currentTenantId);
        }
        if (CollUtil.isEmpty(tenantIds)) {
            log.warn("[getExpression][系统租户监管角色用户({}) 区域({}) 下无匹配租户，tenant_id 过滤为空集]",
                    loginUser.getId(), areaCode);
            return buildFalseExpression(tableName, tableAlias);
        }

        // 构建 tenant_id IN (id1, id2, ...)
        return buildTenantIdInExpression(tableName, tableAlias, tenantIds);
    }

    /**
     * 构建 tenant_id IN (id1, id2, ...) 表达式
     */
    private Expression buildTenantIdInExpression(String tableName, Alias tableAlias, List<Long> tenantIds) {
        Column column = MyBatisUtils.buildColumn(tableName, tableAlias, TENANT_ID_COLUMN);
        ExpressionList<LongValue> values = new ExpressionList<>(
                tenantIds.stream().map(LongValue::new).collect(Collectors.toList())
        );
        return new InExpression(column, new ParenthesedExpressionList<>(values));
    }

    /**
     * 构建始终为 false 的表达式（安全兜底，让查询结果为空）
     * 使用 tenant_id = -1（不存在的 tenantId）
     */
    private Expression buildFalseExpression(String tableName, Alias tableAlias) {
        Column column = MyBatisUtils.buildColumn(tableName, tableAlias, TENANT_ID_COLUMN);
        net.sf.jsqlparser.expression.operators.relational.EqualsTo equalsTo =
                new net.sf.jsqlparser.expression.operators.relational.EqualsTo();
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(new LongValue(-1L));
        return equalsTo;
    }

    private String getInfoValue(LoginUser loginUser, String key) {
        if (loginUser.getInfo() == null) {
            return null;
        }
        return loginUser.getInfo().get(key);
    }

}
