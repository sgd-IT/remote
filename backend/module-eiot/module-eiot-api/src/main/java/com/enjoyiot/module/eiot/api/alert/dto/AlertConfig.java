package com.enjoyiot.module.eiot.api.alert.dto;


import com.enjoyiot.module.eiot.api.TenantModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 告警配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfig  extends TenantModel {

    private Long id;

    /**
     * 配置所属用户
     */
    private String uid;

    /**
     * 告警名称
     */
    private String name;

    /**
     * 告警严重度
     */
    private String level;

    /**
     * 关联规则引擎ID
     */
    private Long ruleInfoId;

    /**
     * 关联消息转发模板ID
     */
    private Long messageTemplateId;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Integer status;

    /**
     * 消息推送岗位ID数组（最多3个）
     */
    private List<Long> pushPostIds;

    public Boolean isEnable() {
        return status != null && status == 0;
    }


}
