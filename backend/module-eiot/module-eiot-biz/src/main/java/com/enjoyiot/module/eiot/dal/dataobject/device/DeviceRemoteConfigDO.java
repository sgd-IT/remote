package com.enjoyiot.module.eiot.dal.dataobject.device;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("eiot_device_remote_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceRemoteConfigDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deviceId;
    private Long brandModelId;
    private String confirmPassword;
}
