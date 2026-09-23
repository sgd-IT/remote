package com.enjoyiot.module.eiot.dal.dataobject.device;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_device_function")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceFunctionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deviceId;
    private Long brandModelFunctionId;
    private String buttonName;
    private Integer sortOrder;
    private String paramValues;
    private Boolean enabled;

    /**
     * 绑定的子设备ID
     */
    private Long subDeviceId;

    /**
     * 功能类型：BUS(总线按钮)/MULTI(多线按钮)/GENERAL(通用按钮)
     */
    private String functionType;
}
