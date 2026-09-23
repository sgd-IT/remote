package com.enjoyiot.eiot.temporal.es.dao;

import com.enjoyiot.eiot.temporal.es.document.DocVirtualDeviceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VirtualDeviceLogRepository extends ElasticsearchRepository<DocVirtualDeviceLog, Long> {

    Page<DocVirtualDeviceLog> findByVirtualDeviceId(Long virtualDeviceId, Pageable pageable);

}
