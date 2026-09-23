package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维保合同 DO
 *
 * 合同状态说明：
 * - valid: 有效
 * - expired: 已到期
 * - renewing: 续签中
 *
 * @author EnjoyIot
 */
@TableName("fire_maintenance_contract")
@KeySequence("fire_maintenance_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireMaintenanceContractDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 物业ID（租户ID）
     */
    private Long propertyId;

    /**
     * 物业名称
     */
    private String propertyName;

    /**
     * 维保单位
     */
    private String maintenanceUnit;

    /**
     * 维保联系人
     */
    private String maintenanceContact;

    /**
     * 维保联系电话
     */
    private String maintenancePhone;

    /**
     * 合同开始日期
     */
    private LocalDateTime startDate;

    /**
     * 合同结束日期
     */
    private LocalDateTime endDate;

    /**
     * 合同金额
     */
    private BigDecimal contractAmount;

    /**
     * 合同状态: valid-有效, expired-已到期, renewing-续签中
     */
    private String contractStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 合同文件URL（PDF等）
     */
    private String contractFile;
}
