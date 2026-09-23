package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 值守中心座机号码 DO
 * <p>
 * 由值守平台（系统租户）统一录入，归属固定为系统租户（tenantId = 1）；
 * 话单推送到达后，按 callerNbr / calledNbr 匹配本表号码确定座机是主叫还是被叫，
 * 再以另一方号码匹配企业租户归属。
 *
 * @author EnjoyIot
 */
@TableName("eiot_duty_phone")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireDutyPhoneDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 座机电话号码
     */
    private String phoneNumber;

    /**
     * 座机名称（如：XX值班中心前台）
     */
    private String name;

    /**
     * 备注
     */
    private String remark;
}
