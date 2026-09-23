package com.enjoyiot.module.eiot.controller.admin.alertconfig.vo;


import com.enjoyiot.framework.common.pojo.PageParam;
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
public class AlertRecordPageReq extends PageParam {

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
     * 是否已读
     */
    private Boolean readFlg;

    /**
     * 设备id
     */
    private Long deviceId;

    /**
     * 产品key
     */
    private String productKey;

}
