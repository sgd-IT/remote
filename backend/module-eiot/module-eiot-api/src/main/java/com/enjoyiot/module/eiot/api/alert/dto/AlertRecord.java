package com.enjoyiot.module.eiot.api.alert.dto;


import com.enjoyiot.module.eiot.api.TenantModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRecord  extends TenantModel {

    private Long id;

    /**
     * 告警名称
     */
    private String name;

    /**
     * 告警严重度（1-5）
     */
    private String level;

    /**
     * 告警时间
     */
    private Long alertTime;

    /**
     * 告警详情
     */
    private String details;

    /**
     * 报警类型：0=主机报警 1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警
     */
    private Integer alarmType;

    /**
     * 是否已读
     */
    private Boolean readFlg;

    private Long tenantId;
    
    private Long deviceId;
    
    private Long subDeviceId;
    
    private String productKey;

    /**
     * 报警图片URL
     */
    private String alertImage;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备地址
     */
    private String deviceAddr;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 处置状态：0=待处置, 1=稍后处置, 2=处置中, 3=已处置
     */
    private Integer handleStatus;

    /**
     * 处置状态：pending/confirmed/handled/closed（对应 order_status 列）
     */
    private String orderStatus;

}
