package com.enjoyiot.module.eiot.api.rule.dto;

import com.enjoyiot.module.eiot.api.task.dto.RuleAction;
import com.enjoyiot.module.eiot.api.rule.dto.TriggerOptions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleInfo {

    public static final Integer STATE_STOPPED = 0;
    public static final Integer STATE_RUNNING = 1;

    public static final String TYPE_SCENE = "scene";
    public static final String TYPE_FLOW = "flow";

    private Long id;

    private String name;

    private String typ;

    private List<FilterConfig> listeners;

    private List<FilterConfig> filters;

    private List<RuleAction> actions;

    /**
     * 触发控制配置：频率限制 / 延时触发 / 告警解除
     */
    private TriggerOptions triggerOptions;



    private Integer state;

    private String remark;


    private Long tenantId;

    private LocalDateTime createTime;

    public List<FilterConfig> getListeners() {
        return listeners == null ? new ArrayList<>() : listeners;
    }

    public List<FilterConfig> getFilters() {
        return filters == null ? new ArrayList<>() : filters;
    }

    public List<RuleAction> getActions() {
        return actions == null ? new ArrayList<>() : actions;
    }

    public TriggerOptions getTriggerOptions() {
        return triggerOptions == null ? new TriggerOptions() : triggerOptions;
    }
}
