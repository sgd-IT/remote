package com.enjoyiot.module.eiot.dal.dataobject.devicealert;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

@TableName("eiot_device_alert_record")
@KeySequence("eiot_device_alert_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAlertRecordDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long deviceId;

    private String productKey;

    private Long alertConfigId;

    private Long alertTime;

    private String alertState;

    private Long recoverTime;

    private String level;

    private String name;

    private String details;

    private Boolean readFlg;

    private Long tenantId;
}
