package com.enjoyiot.eiot.temporal.es.service;


import cn.hutool.core.collection.CollectionUtil;
import com.enjoyiot.eiot.IDevicePropertyData;
import com.enjoyiot.eiot.temporal.es.convert.EsDevicePropertyConvert;
import com.enjoyiot.eiot.temporal.es.document.DocDeviceProperty;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.DeviceProperty;
import com.enjoyiot.module.eiot.api.device.dto.DevicePropertyCache;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DevicePropertyDataImpl implements IDevicePropertyData {

    @Autowired
    private ElasticsearchRestTemplate template;

    @Autowired
    private DeviceApi deviceApi;

    private final Set<String> indexSet = new HashSet<>();

    @Override
    public List<DeviceProperty> findDevicePropertyHistory(Long deviceId, String name, long start, long end, int size) {
        DeviceInfo deviceInfo = deviceApi.getDeviceInfoFromCache(deviceId);
       
        String index = getIndex(deviceInfo.getProductKey(), name);
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(
                        QueryBuilders.boolQuery()
                                .must(QueryBuilders.termQuery("deviceId", deviceId))
                                .must(QueryBuilders.rangeQuery("time")
                                        .from(start, true).to(end, true))
                )
                .withSorts(new FieldSortBuilder("time").order(SortOrder.ASC))
                .build();
        SearchHits<DocDeviceProperty> result = template.search(query, DocDeviceProperty.class, IndexCoordinates.of(index));
        return result.getSearchHits().stream()
                .map(h -> EsDevicePropertyConvert.INSTANCE.convert(h.getContent()))
                .collect(Collectors.toList());
    }

    @Override
    public void addProperties(Long deviceId, Map<String, DevicePropertyCache> properties, long time) {
        if (Objects.isNull(deviceId) || CollectionUtil.isEmpty(properties)) {
            return;
        }
        DeviceInfo deviceInfo = deviceApi.getDeviceInfoFromCache(deviceId);
        // TODO:改成批量
        String deviceIdStr = String.valueOf(deviceId);
        properties.forEach((key, val) -> {
            DevicePropertyCache propertyCache = (DevicePropertyCache) val;
            String index = getIndex(deviceInfo.getProductKey(), key);
            long occurred = Objects.nonNull(propertyCache.getOccurred()) ? propertyCache.getOccurred() : time;
            template.save(
                    new DocDeviceProperty(UUID.randomUUID().toString(), deviceIdStr, key, propertyCache.getValue(), occurred),
                    IndexCoordinates.of(index)
            );
        });
    }

    private String getIndex(String pk, String name) {

        String index = String.format("device_property_%s_%s", pk, name).toLowerCase();
        if (!indexSet.contains(index)) {
            IndexCoordinates indexCoordinates = IndexCoordinates.of(index);
            if (!template.indexOps(indexCoordinates).exists()) {
                // 根据索引实体，获取mapping字段
                Document mapping = template.indexOps(indexCoordinates).createMapping(DocDeviceProperty.class);
                template.indexOps(indexCoordinates).create();
                // 创建索引mapping
                template.indexOps(indexCoordinates).putMapping(mapping);
            }
            indexSet.add(index);
        }
        return index;
    }

}
