package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 物业/联网单位 DO
 *
 * @author EnjoyIot
 */
@TableName("fire_property")
@KeySequence("fire_property_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirePropertyDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 物业名称
     */
    private String name;
    
    /**
     * 物业编码
     */
    private String code;
    
    /**
     * 详细地址
     */
    private String address;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 区县
     */
    private String district;
    
    /**
     * 经度
     */
    private BigDecimal longitude;
    
    /**
     * 纬度
     */
    private BigDecimal latitude;
    
    /**
     * 联系人
     */
    private String contactName;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 风险等级: critical-重大, major-较大, normal-一般
     */
    private String riskLevel;
    
    /**
     * 物业类型
     */
    private String propertyType;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 客户类型
     */
    private String customerType;

    /**
     * 楼层数
     */
    private Integer floorCount;
    
    /**
     * 建筑面积(平方米)
     */
    private BigDecimal buildingArea;

    /**
     * 主要负责人
     */
    private String mainPerson;

    /**
     * 安全管理人
     */
    private String safetyPerson;

    /**
     * 建筑层数
     */
    private String buildingFloors;
    
    /**
     * 状态: 0-停用, 1-启用
     */
    private Integer status;

    // ========== 许可证信息 ==========

    /**
     * 许可证编号
     */
    private String licenseNo;

    /**
     * 发证机关
     */
    private String licenseAuthority;

    /**
     * 许可证有效期开始
     */
    private LocalDate licenseStartDate;

    /**
     * 许可证有效期截止
     */
    private LocalDate licenseEndDate;

    /**
     * 许可范围
     */
    private String licenseScope;

    // ========== 仓储信息 ==========

    /**
     * 仓储类型
     */
    private String storageType;

    /**
     * 仓储面积(平方米)
     */
    private String storageArea;

    /**
     * 仓储容量
     */
    private String storageCapacity;

    /**
     * 存储物品类型
     */
    private String storageGoodsType;

    // ========== 其他 ==========

    /**
     * 备注
     */
    private String remark;

    /**
     * 获取物业名称(兼容方法)
     */
    public String getPropertyName() {
        return this.name;
    }

}
