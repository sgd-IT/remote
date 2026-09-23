package com.enjoyiot.eiot.temporal.es.dao;

import com.enjoyiot.eiot.temporal.es.document.DocThingModelMessage;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ThingModelMessageRepository extends ElasticsearchRepository<DocThingModelMessage, String> {
}
