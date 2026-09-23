package com.enjoyiot.module.eiot.dal.dataobject.brand;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_brand_model_function")
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandModelFunctionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long brandModelId;
    private String functionId;
    private String name;
    private Integer ruleNum;
    private Integer awaitTime;

    /** 厂商指令标识, 关联 eiot_vendor_command.command_key (LOCAL_COMMAND 类厂商使用) */
    private String vendorCommandKey;
}
