package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 维保单位 DO
 *
 * 用于维护可选的维保单位列表，支持数据隔离（按租户隔离）
 *
 * @author EnjoyIot
 */
@TableName("fire_maintenance_unit")
@KeySequence("fire_maintenance_unit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireMaintenanceUnitDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 单位名称
     */
    private String unitName;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 地址
     */
    private String address;

    /**
     * 资质等级: 甲级/乙级/丙级
     */
    private String qualificationLevel;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者（数据库表缺失该列，临时排除）
     */
    @TableField(exist = false)
    private String creator;

    /**
     * 更新者（数据库表缺失该列，临时排除）
     */
    @TableField(exist = false)
    private String updater;
}
