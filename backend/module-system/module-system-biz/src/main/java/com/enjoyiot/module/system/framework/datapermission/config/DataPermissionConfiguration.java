
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
package com.enjoyiot.module.system.framework.datapermission.config;

import com.enjoyiot.framework.datapermission.core.rule.tenant.TenantAreaDataPermissionRule;
import com.enjoyiot.framework.tenant.config.TenantProperties;
import com.enjoyiot.module.system.dal.dataobject.dept.DeptDO;
import com.enjoyiot.module.system.dal.dataobject.user.AdminUserDO;
import com.enjoyiot.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import com.enjoyiot.module.system.framework.datapermission.rule.TenantAreaTenantIdDataPermissionRule;
import com.enjoyiot.module.system.service.tenant.TenantService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * system 模块的数据权限 Configuration
 *
 * @author EnjoyIot
 */
@Configuration(proxyBeanMethods = false)
public class DataPermissionConfiguration {

    @Bean
    public DeptDataPermissionRuleCustomizer sysDeptDataPermissionRuleCustomizer() {
        return rule -> {
            // dept
            rule.addDeptColumn(AdminUserDO.class);
            rule.addDeptColumn(DeptDO.class, "id");
            // user
            rule.addUserColumn(AdminUserDO.class, "id");
        };
    }

    /**
     * 租户区域 TenantId 数据权限规则
     *
     * 用于监管角色用户查询所有业务表时自动追加 tenant_id IN (区域内租户Id列表) 条件
     */
    @Bean
    public TenantAreaTenantIdDataPermissionRule tenantAreaTenantIdDataPermissionRule(
            TenantService tenantService, TenantProperties tenantProperties) {
        return new TenantAreaTenantIdDataPermissionRule(tenantService, tenantProperties);
    }

    /**
     * 租户区域数据权限规则
     *
     * 用于监管角色的区域过滤：仅允许查看其所在区域编码下的租户
     */
    @Bean
    public TenantAreaDataPermissionRule tenantAreaDataPermissionRule() {
        return new TenantAreaDataPermissionRule();
    }

}
