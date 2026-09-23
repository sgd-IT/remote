package com.enjoyiot.eiot.temporal.rdb.service;


import com.enjoyiot.eiot.IRuleLogData;
import com.enjoyiot.eiot.temporal.rdb.dao.RdbTemplate;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.rule.dto.RuleLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RuleLogDataImpl implements IRuleLogData {

    @Autowired
    private RdbTemplate rdbTemplate;

    @Override
    public void deleteByRuleId(Long ruleId) {
        rdbTemplate.update("delete from rule_log where rule_id=?", ruleId);
    }

    @Override
    public PageResult<RuleLog> findByRuleId(Long ruleId, int page, int size) {
        String sql = "select time,state1,content,success,rule_id from rule_log where rule_id=? order by time desc limit ? offset ?";
        List<Map<String, Object>> ruleLogs = rdbTemplate.queryForList(sql, ruleId, size, (page - 1) * size);

        List<RuleLog> logs = ruleLogs.stream().map(r -> {
            Long time = ((Number) r.get("time")).longValue();
            return new RuleLog(
                    time,
                    ruleId,
                    (String) r.get("state1"),
                    (String) r.get("content"),
                    ((Number) r.get("success")).intValue() == 1,
                    time
            );
        }).collect(Collectors.toList());

        sql = "select count(*) from rule_log where rule_id=?";
        Long count = rdbTemplate.queryForObject(sql, Long.class, ruleId);

        return new PageResult<>(logs, count != null ? count : 0L);
    }

    @Override
    public void add(RuleLog log) {
        String sql = "INSERT INTO rule_log (time, state1, content, success, rule_id) VALUES (?, ?, ?, ?, ?)";
        rdbTemplate.update(sql, System.currentTimeMillis(), log.getState(), log.getContent(), log.getSuccess() ? 1 : 0, log.getRuleId());
    }
}
