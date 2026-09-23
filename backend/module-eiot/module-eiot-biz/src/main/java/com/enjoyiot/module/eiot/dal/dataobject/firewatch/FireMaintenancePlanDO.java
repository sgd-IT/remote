package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 维保计划 DO
 *
 * 计划类型说明：
 * - monthly: 月度计划
 * - quarterly: 季度计划
 * - yearly: 年度计划
 *
 * 计划状态说明：
 * - pending: 待执行
 * - executing: 执行中
 * - completed: 已完成
 * - overdue: 已逾期
 *
 * @author EnjoyIot
 */
@TableName("fire_maintenance_plan")
@KeySequence("fire_maintenance_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireMaintenancePlanDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 计划编号
     */
    private String planNo;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 物业ID
     */
    private Long propertyId;

    /**
     * 物业名称
     */
    private String propertyName;

    /**
     * 计划类型: monthly-月度, quarterly-季度, yearly-年度
     */
    private String planType;

    /**
     * 计划日期
     */
    private LocalDateTime planDate;

    /**
     * 执行人
     */
    private String executePerson;

    /**
     * 执行单位
     */
    private String executeUnit;

    /**
     * 计划状态: pending-待执行, executing-执行中, completed-已完成, overdue-已逾期
     */
    private String planStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否已发送催促通知
     */
    private Boolean urgeSent;

    /**
     * 催促发送时间
     */
    private LocalDateTime urgeTime;
}
