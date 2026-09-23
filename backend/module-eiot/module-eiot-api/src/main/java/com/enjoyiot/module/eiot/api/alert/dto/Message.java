package com.enjoyiot.module.eiot.api.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * author: EnjoyIot
 * date: 2023-05-08 15:15
 * description:
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    private String content;
    private Map<String, Object> param;
    private String channelCode;
    private String channelConfig;
    private Long alertConfigId;
    private String templateCode;
    private Long tenantId;
    private Long deviceId;
    private String productKey;

    /**
     * 设备名称(dn)
     */
    private String dn;

    /**
     * 系统短信模板编码（关联 system_sms_template.code）
     */
    private String smsTemplateCode;

    public String getFormatContent() {
        String fmt = content;
        for (String key : param.keySet()) {
            Object val = param.get(key);
            fmt = fmt.replace("${" + key + "}", val == null ? "" : val.toString());
        }
        return fmt;
    }
}
