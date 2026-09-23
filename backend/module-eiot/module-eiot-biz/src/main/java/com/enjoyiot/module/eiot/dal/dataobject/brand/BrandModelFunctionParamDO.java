package com.enjoyiot.module.eiot.dal.dataobject.brand;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_brand_model_function_param")
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandModelFunctionParamDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long functionId;
    private String paramName;
    private String paramCode;
    private String paramType;
}
