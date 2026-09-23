
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
package com.enjoyiot.framework.datapermission.core.rule.tenant;

import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.datapermission.core.rule.DataPermissionRule;
import com.enjoyiot.framework.mybatis.core.util.MyBatisUtils;
import com.enjoyiot.framework.security.core.LoginUser;
import com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;

import java.util.Collections;
import java.util.Set;

/**
 * 基于租户区域的数据权限规则
 *
 * 针对 system_tenant 表，实现以下过滤逻辑：
 * 1. 企业租户用户：返回 null，由 MyBatis Plus 的 tenant_id 自动隔离处理
 * 2. 系统租户非监管角色用户：返回 null，可看全部租户
 * 3. 系统租户监管角色用户：仅允许查看其所在区域内的租户
 *    （area_code 存储数字编码路径如"330000/330100/330106"，与 system_tenant 表的
 *    province_code/city_code/district_code 编码字段通过 EqualsTo 精确匹配）
 *
 * @author EnjoyIot
 */
@Slf4j
public class TenantAreaDataPermissionRule implements DataPermissionRule {

    private static final String TABLE_NAME = "system_tenant";
    private static final String PACKAGE_ID_COLUMN = "package_id";
    private static final String PROVINCE_CODE_COLUMN = "province_code";
    private static final String CITY_CODE_COLUMN = "city_code";
    private static final String DISTRICT_CODE_COLUMN = "district_code";

    /**
     * 监管角色标识，与 AdminAuthServiceImpl.buildUserInfo 中判断逻辑保持一致
     */
    private static final String MONITOR_ROLE_CODE = "monitor_role";

    static final Expression EXPRESSION_NULL = new NullValue();

    /**
     * 需要作用的表名集合
     */
    private final Set<String> tableNames = Collections.singleton(TABLE_NAME);

    @Override
    public Set<String> getTableNames() {
        return tableNames;
    }

    @Override
    public Expression getExpression(String tableName, Alias tableAlias) {
        // 全局忽略租户（@TenantIgnore 注解 或 ignore-urls 配置）→ 不追加区域过滤条件
        if (TenantContextHolder.isIgnore()) {
            return null;
        }

        // 只有已登录用户才进行数据权限处理
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return null;
        }

        // 如果上下文租户与登录用户租户不同，说明是 TenantUtils.execute() 切换到了特定租户，
        // 不再追加区域过滤，由 TenantLineInnerInterceptor 按上下文租户过滤
        Long contextTenantId = TenantContextHolder.getTenantId();
        if (contextTenantId != null && !contextTenantId.equals(loginUser.getTenantId())) {
            return null;
        }

        // 从 LoginUser.info 获取租户类型与角色信息
        String isSystemTenantStr = getInfoValue(loginUser, LoginUser.INFO_KEY_IS_SYSTEM_TENANT);
        String isMonitorRoleStr = getInfoValue(loginUser, LoginUser.INFO_KEY_IS_MONITOR_ROLE);

        // 情况一：非系统租户用户 → 返回 null，由 MyBatis Plus tenant_id 自动隔离
        if (!"true".equals(isSystemTenantStr)) {
            return null;
        }

        // 情况二：系统租户 + 非监管角色 → 返回 null，可看全部租户
        if (!"true".equals(isMonitorRoleStr)) {
            return null;
        }

        // 情况三：系统租户 + 监管角色 → 按区域过滤
        String areaCode = getInfoValue(loginUser, LoginUser.INFO_KEY_AREA_CODE);
        if (StrUtil.isEmpty(areaCode)) {
            // 安全兜底：监管角色没有区域信息时，不返回任何数据
            log.warn("[getExpression][系统租户监管角色用户({}) 未配置区域信息，数据过滤为空]",
                    loginUser.getId());
            return EXPRESSION_NULL;
        }

        // 构建区域过滤条件：WHERE (province_code = ? OR city_code = ? OR district_code = ?)
        return buildAreaFilterExpression(tableName, tableAlias, areaCode);
    }

    /**
     * 构建区域过滤表达式
     *
     * areaCode 存储格式为数字编码路径，如 "330000/330100/330106"，与 system_tenant 表的
     * province_code/city_code/district_code 编码字段通过 EqualsTo 精确匹配。
     *
     * 生成 SQL 示例：(package_id = 0 OR province_code = 330000 OR city_code = 330100 OR district_code = 330106)
     *
     * 其中 package_id = 0 确保系统租户自身始终可见（系统租户未设置区域编码）
     *
     * @param tableName  表名
     * @param tableAlias 表别名
     * @param areaCode   区域数字编码路径，用 / 分隔
     * @return 过滤表达式
     */
    private Expression buildAreaFilterExpression(String tableName, Alias tableAlias, String areaCode) {
        // 解析编码路径："330000/330100/330106" → [330000, 330100, 330106]
        String[] parts = areaCode.split("/");
        Long provinceCode = parts.length > 0 ? Long.valueOf(parts[0].trim()) : null;
        Long cityCode = parts.length > 1 ? Long.valueOf(parts[1].trim()) : null;
        Long districtCode = parts.length > 2 ? Long.valueOf(parts[2].trim()) : null;

        // 系统租户自身始终可见（package_id = 0），避免监管角色查询不到自己所在的租户
        EqualsTo systemTenantExpr = new EqualsTo();
        systemTenantExpr.setLeftExpression(MyBatisUtils.buildColumn(tableName, tableAlias, PACKAGE_ID_COLUMN));
        systemTenantExpr.setRightExpression(new LongValue(0L));
        Expression combined = systemTenantExpr;

        // 构建区域编码 EqualsTo 精确匹配条件，始终与 package_id = 0 进行 OR 组合
        if (provinceCode != null) {
            EqualsTo expr = new EqualsTo();
            expr.setLeftExpression(MyBatisUtils.buildColumn(tableName, tableAlias, PROVINCE_CODE_COLUMN));
            expr.setRightExpression(new LongValue(provinceCode));
            combined = new OrExpression(combined, expr);
        }
        if (cityCode != null) {
            EqualsTo cityExpr = new EqualsTo();
            cityExpr.setLeftExpression(MyBatisUtils.buildColumn(tableName, tableAlias, CITY_CODE_COLUMN));
            cityExpr.setRightExpression(new LongValue(cityCode));
            combined = (combined == null) ? cityExpr : new OrExpression(combined, cityExpr);
        }
        if (districtCode != null) {
            EqualsTo districtExpr = new EqualsTo();
            districtExpr.setLeftExpression(MyBatisUtils.buildColumn(tableName, tableAlias, DISTRICT_CODE_COLUMN));
            districtExpr.setRightExpression(new LongValue(districtCode));
            combined = (combined == null) ? districtExpr : new OrExpression(combined, districtExpr);
        }

        // 使用 ParenthesedExpressionList 包裹表达式（JSqlParser 5.0 中 Parenthesis.setExpression() 存在 IndexOutOfBoundsException）
        return new ParenthesedExpressionList<>(combined);
    }

    /**
     * 安全获取 LoginUser.info 中的值
     *
     * @param loginUser 登录用户
     * @param key       info 键
     * @return 值，不存在返回 null
     */
    private String getInfoValue(LoginUser loginUser, String key) {
        if (loginUser.getInfo() == null) {
            return null;
        }
        return loginUser.getInfo().get(key);
    }

}
