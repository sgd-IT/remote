package com.enjoyiot.module.eiot.dal.dataobject.devicealert;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

@TableName("eiot_device_alert_config")
@KeySequence("eiot_device_alert_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAlertConfigDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String name;

    private String productKey;

    private Long deviceId;

    private String level;

    private String conditions;

    private String triggerOptions;

    private Integer status;

    private String remark;
}
