package com.enjoyiot.module.eiot.dal.dataobject.deviceconfig;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备配置 DO
 */
@TableName("eiot_device_config")
@KeySequence("eiot_device_config_seq") // 兼容 Oracle/PostgreSQL 等，MySQL 可忽略
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceConfigDO extends TenantBaseDO {

    /**
     * 配置id
     */
    @TableId
    private Long id;

    /**
     * 配置内容（JSON）
     */
    private String config;

    /**
     * 产品key
     */
    private String productKey;

    /**
     * 设备唯一编码
     */
    private String dn;
}

