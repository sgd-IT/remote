package com.enjoyiot.module.eiot.dal.dataobject.brand;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_brand")
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String brandCode;
    private String name;

    /** 协议厂商编码, 关联 eiot_protocol_vendor.vendor_code; 为空时默认 FOURFAITH */
    private String vendorCode;
}
