
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
package com.enjoyiot.framework.tenant.core.db;

import cn.hutool.core.collection.CollUtil;
import com.enjoyiot.framework.security.core.LoginUser;
import com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils;
import com.enjoyiot.framework.tenant.config.TenantProperties;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.toolkit.SqlParserUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 基于 MyBatis Plus 多租户的功能，实现 DB 层面的多租户的功能
 *
 * @author EnjoyIot
 */
public class TenantDatabaseInterceptor implements TenantLineHandler {

    private final Set<String> ignoreTables = new HashSet<>();

    public TenantDatabaseInterceptor(TenantProperties properties) {
        // 不同 DB 下，大小写的习惯不同，所以需要都添加进去
        properties.getIgnoreTables().forEach(table -> {
            ignoreTables.add(table.toLowerCase());
            ignoreTables.add(table.toUpperCase());
        });
        // 在 OracleKeyGenerator 中，生成主键时，会查询这个表，查询这个表后，会自动拼接 TENANT_ID 导致报错
        ignoreTables.add("DUAL");
    }

    @Override
    public Expression getTenantId() {
        return new LongValue(TenantContextHolder.getRequiredTenantId());
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 情况一：全局忽略多租户
        if (TenantContextHolder.isIgnore()) {
            return true;
        }
        // 情况二：忽略配置的表（如 system_tenant）
        if (CollUtil.contains(ignoreTables, SqlParserUtils.removeWrapperSymbol(tableName))) {
            return true;
        }
        // 情况三：系统租户用户，跳过内置 tenant_id = ? 过滤
        // 由 TenantAreaTenantIdDataPermissionRule 统一接管：
        //   - 非监管角色：不全限制，可查全部企业数据
        //   - 监管角色：追加 tenant_id IN (区域内租户Id列表)
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null) {
            Map<String, String> info = loginUser.getInfo();
            if (info != null && "true".equals(info.get(LoginUser.INFO_KEY_IS_SYSTEM_TENANT))) {
                // 如果上下文租户与登录用户租户不同，说明是 TenantUtils.execute() 切换到了特定租户，
                // 需要按上下文租户过滤（tenant_id = contextTenantId），不跳过
                Long contextTenantId = TenantContextHolder.getTenantId();
                if (contextTenantId != null && !contextTenantId.equals(loginUser.getTenantId())) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

}
