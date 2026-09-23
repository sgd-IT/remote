package com.enjoyiot.module.eiot.controller.admin.channeltemplate.vo;


import com.enjoyiot.module.eiot.api.TenantModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author: EnjoyIot
 * date: 2023-05-11 16:30
 * description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelTemplate extends TenantModel {

    private Long id;

    private Long channelConfigId;

    private String title;

    private String content;

    /**
     * Template status: 0-pending, 1-approved, 2-rejected
     */
    private Integer status;

    /**
     * Template code from cloud provider (e.g., Alibaba Cloud SMS template code)
     */
    private String templateCode;

    /**
     * 系统短信模板编码（关联 system_sms_template.code）
     */
    private String smsTemplateCode;

    private String channelCode;

}
