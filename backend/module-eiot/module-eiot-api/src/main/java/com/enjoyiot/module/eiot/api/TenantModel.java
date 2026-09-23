package com.enjoyiot.module.eiot.api;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户基类
 *
 * @author Michelle.Chung
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantModel extends BaseModel {

    /**
     * 租户编号
     */
    private Long tenantId;

}
