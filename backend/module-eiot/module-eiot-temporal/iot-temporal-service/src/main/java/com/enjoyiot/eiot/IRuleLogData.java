package com.enjoyiot.eiot;


import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.rule.dto.RuleLog;

public interface IRuleLogData {

    void deleteByRuleId(Long ruleId);

    PageResult<RuleLog> findByRuleId(Long ruleId, int page, int size);

    void add(RuleLog log);

}
