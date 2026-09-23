package com.enjoyiot.module.system.api.tenant.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 租户创建事件
 * 当租户创建时发布，其他模块可监听此事件进行数据同步
 *
 * @author EnjoyIot
 */
@Getter
public class TenantCreatedEvent extends ApplicationEvent {

    /**
     * 租户ID
     */
    private final Long tenantId;

    /**
     * 租户名称
     */
    private final String tenantName;

    /**
     * 联系人
     */
    private final String contactName;

    /**
     * 联系电话
     */
    private final String contactMobile;

    /**
     * 客户类型
     */
    private final String customerType;

    /**
     * 详细地址
     */
    private final String address;

    /**
     * 省份
     */
    private final String province;

    /**
     * 城市
     */
    private final String city;

    /**
     * 区县
     */
    private final String district;

    /**
     * 经度
     */
    private final BigDecimal longitude;

    /**
     * 纬度
     */
    private final BigDecimal latitude;

    /**
     * 风险等级
     */
    private final String riskLevel;

    /**
     * 统一社会信用代码
     */
    private final String creditCode;

    public TenantCreatedEvent(Object source, Long tenantId, String tenantName,
                              String contactName, String contactMobile, String customerType) {
        this(source, tenantId, tenantName, contactName, contactMobile, customerType,
                null, null, null, null, null, null, null, null);
    }

    public TenantCreatedEvent(Object source, Long tenantId, String tenantName,
                              String contactName, String contactMobile, String customerType,
                              String address, String province, String city, String district,
                              BigDecimal longitude, BigDecimal latitude,
                              String riskLevel, String creditCode) {
        super(source);
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.contactName = contactName;
        this.contactMobile = contactMobile;
        this.customerType = customerType;
        this.address = address;
        this.province = province;
        this.city = city;
        this.district = district;
        this.longitude = longitude;
        this.latitude = latitude;
        this.riskLevel = riskLevel;
        this.creditCode = creditCode;
    }
}
