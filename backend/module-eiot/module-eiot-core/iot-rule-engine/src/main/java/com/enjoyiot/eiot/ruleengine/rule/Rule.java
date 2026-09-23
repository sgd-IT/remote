package com.enjoyiot.eiot.ruleengine.rule;


import com.enjoyiot.eiot.ruleengine.action.Action;
import com.enjoyiot.eiot.ruleengine.filter.Filter;
import com.enjoyiot.eiot.ruleengine.listener.Listener;
import com.enjoyiot.module.eiot.api.rule.dto.TriggerOptions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rule {

    private Long id;

    private String name;

    private List<Listener<?>> listeners;

    private List<Filter<?>> filters;

    private List<Action<?>> actions;

    private Long tenantId;

    /**
     * 触发控制配置（频率限制、延时、告警解除）
     */
    private TriggerOptions triggerOptions;

}
