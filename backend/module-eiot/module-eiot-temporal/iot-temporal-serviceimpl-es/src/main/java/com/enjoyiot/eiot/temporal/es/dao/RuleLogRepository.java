package com.enjoyiot.eiot.temporal.es.dao;

import com.enjoyiot.eiot.temporal.es.document.DocRuleLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RuleLogRepository extends ElasticsearchRepository<DocRuleLog, String> {

    void deleteByRuleId(Long ruleId);

    Page<DocRuleLog> findByRuleIdOrderByLogAtDesc(Long ruleId, Pageable pageable);

}
