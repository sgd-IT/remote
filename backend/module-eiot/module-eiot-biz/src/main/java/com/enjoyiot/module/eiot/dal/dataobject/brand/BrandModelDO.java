package com.enjoyiot.module.eiot.dal.dataobject.brand;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("eiot_brand_model")
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandModelDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long brandId;
    private String model;
    private String version;
    private LocalDateTime syncTime;
}
