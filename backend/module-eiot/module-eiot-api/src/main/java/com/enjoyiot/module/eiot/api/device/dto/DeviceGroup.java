package com.enjoyiot.module.eiot.api.device.dto;


import com.enjoyiot.module.eiot.api.TenantModel;
import lombok.Data;

/**
 * 设备分组
 */
@Data
public class DeviceGroup  extends TenantModel {

    /**
     * 分组id
     */
    private Long id;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 所属用户
     */
    private Long uid;

    /**
     * 分组说明
     */
    private String remark;


    /**
     * 分组类型(0系统, 1用户, 字典)
     */
    private Integer typ;

}
