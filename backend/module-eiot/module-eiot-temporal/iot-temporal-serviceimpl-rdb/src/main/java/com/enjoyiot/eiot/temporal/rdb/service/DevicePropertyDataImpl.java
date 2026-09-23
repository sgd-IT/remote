package com.enjoyiot.eiot.temporal.rdb.service;


import cn.hutool.core.convert.Convert;
import com.enjoyiot.eiot.IDevicePropertyData;
import com.enjoyiot.eiot.temporal.rdb.config.Constants;
import com.enjoyiot.eiot.temporal.rdb.dao.RdbTemplate;
import com.enjoyiot.eiot.temporal.rdb.dm.FieldParser;
import com.enjoyiot.eiot.temporal.rdb.dm.RdbField;
import com.enjoyiot.eiot.temporal.rdb.model.RdbDeviceProperty;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.DeviceProperty;
import com.enjoyiot.module.eiot.api.device.dto.DevicePropertyCache;
import com.enjoyiot.module.eiot.api.thingmodel.ThingModelApi;
import com.enjoyiot.module.eiot.api.thingmodel.dto.ThingModel;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DevicePropertyDataImpl implements IDevicePropertyData {

    @Autowired
    private RdbTemplate rdbTemplate;

    @Resource
    private DeviceApi deviceApi;

    @Resource
    private ThingModelApi thingModelApi;

    @Override
    public List<DeviceProperty> findDevicePropertyHistory(Long deviceId, String name, long start, long end, int size) {
        DeviceInfo device = deviceApi.getDeviceInfoFromCache(deviceId);
        if (device == null) {
            return new ArrayList<>();
        }

        String tableName = Constants.getProductPropertyTableName(device.getProductKey());
        String fieldName = name.toLowerCase().replace("-", "_");

        String sql = String.format(
                "SELECT time, %s as value, device_id FROM %s WHERE device_id=? AND time>=? AND time<=? ORDER BY time ASC LIMIT ?",
                fieldName, tableName
        );

        List<RdbDeviceProperty> deviceProperties = rdbTemplate.query(sql,
                new BeanPropertyRowMapper<>(RdbDeviceProperty.class),
                deviceId, start, end, size
        );

        return deviceProperties.stream().map(p -> new DeviceProperty(
                        String.valueOf(p.getTime()),
                        p.getDeviceId().toString(),
                        name,
                        p.getValue(),
                        p.getTime()))
                .collect(Collectors.toList());
    }

    @Override
    public void addProperties(Long deviceId, Map<String, DevicePropertyCache> properties, long time) {
        DeviceInfo device = deviceApi.getDeviceInfoFromCache(deviceId);
        if (device == null) {
            return;
        }

        ThingModel thingModel = thingModelApi.getThingModelByProductKeyFromCache(device.getProductKey());
        List<RdbField> fieldList = FieldParser.parse(thingModel);
        Map<String, String> fieldMap = fieldList.stream().collect(Collectors.toMap(RdbField::getName, RdbField::getType));

        Map<String, DevicePropertyCache> oldProperties = deviceApi.getPropertiesFromCache(deviceId);
        oldProperties.putAll(properties);

        StringBuilder sbFieldNames = new StringBuilder();
        StringBuilder sbFieldPlaces = new StringBuilder();
        List<Object> args = new ArrayList<>();
        args.add(time);

        oldProperties.forEach((key, val) -> {
            String fieldName = key.toLowerCase().replace("-", "_");
            sbFieldNames.append(fieldName).append(",");
            sbFieldPlaces.append("?,");

            String fieldType = fieldMap.getOrDefault(fieldName, "VARCHAR");
            Object value = convertValue(val.getValue(), fieldType);
            args.add(value);
        });
        args.add(deviceId);

        sbFieldNames.deleteCharAt(sbFieldNames.length() - 1);
        sbFieldPlaces.deleteCharAt(sbFieldPlaces.length() - 1);

        String sql = String.format("INSERT INTO %s (time,%s,device_id) VALUES (?,%s,?)",
                Constants.getProductPropertyTableName(device.getProductKey()),
                sbFieldNames,
                sbFieldPlaces
        );

        rdbTemplate.update(sql, args.toArray());
    }

    private Object convertValue(Object value, String fieldType) {
        if (value == null) {
            return null;
        }

        try {
            switch (fieldType) {
                case "BIGINT":
                    return Convert.toLong(value);
                case "DOUBLE":
                case "DOUBLE precision":
                    return Convert.toDouble(value);
                case "TINYINT":
                    return Convert.toInt(value) != 0 ? 1 : 0;
                case "INT":
                    return Convert.toInt(value);
                case "BOOL":
                    // 保持布尔类型，不转字符串
                    if (value instanceof Boolean) {
                        return value;
                    }
                    return Convert.toBool(value);
                case "TEXT":
                    // JSON 字段直接返回字符串
                    if (value instanceof String) {
                        return value;
                    }
                    return JsonUtils.toJsonString(value);
                default:
                    return Convert.toStr(value);
            }
        } catch (Exception e) {
            log.warn("转换字段值失败: value={}, type={}", value, fieldType);
            return Convert.toStr(value);
        }
    }
}
