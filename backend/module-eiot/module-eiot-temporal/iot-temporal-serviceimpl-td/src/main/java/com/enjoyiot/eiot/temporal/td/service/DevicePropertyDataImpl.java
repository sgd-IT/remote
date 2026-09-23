package com.enjoyiot.eiot.temporal.td.service;


import com.enjoyiot.eiot.IDevicePropertyData;
import com.enjoyiot.eiot.temporal.td.config.Constants;
import com.enjoyiot.eiot.temporal.td.dao.TdTemplate;
import com.enjoyiot.eiot.temporal.td.model.TbDeviceProperty;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.DeviceProperty;
import com.enjoyiot.module.eiot.api.device.dto.DevicePropertyCache;
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
    private TdTemplate tdTemplate;

    @Resource
    private DeviceApi deviceApi;

    @Override
    public List<DeviceProperty> findDevicePropertyHistory(Long deviceId, String name, long start, long end, int size) {
        DeviceInfo device = deviceApi.getDeviceInfoFromCache(deviceId);
        if (device == null) {
            return new ArrayList<>();
        }

        String tbName = Constants.getProductPropertySTableName(device.getProductKey());
        String filedName = name;
        if (name.contains(".")) {
            filedName = "`" + name + "`";
        }
        List<TbDeviceProperty> deviceProperties = tdTemplate.query(String.format(
                        "select time,%s as `value`,device_id from %s where device_id=? and time>=? and time<=? " +
                                "and %s is not null " +
                                "order by time asc" ,
                        filedName.toLowerCase(), tbName, filedName.toLowerCase()),
                new BeanPropertyRowMapper<>(TbDeviceProperty.class),
                deviceId, start, end
        );
        return deviceProperties.stream().map(p -> new DeviceProperty(
                        p.getTime().toString(),
                        p.getDeviceId(),
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

        // 直接使用传入的属性，不合并旧属性缓存（只写变化值）
        StringBuilder sbFieldNames = new StringBuilder();
        StringBuilder sbFieldPlaces = new StringBuilder();
        List<Object> args = new ArrayList<>();
        args.add(time);

        // 组织 SQL：只写实际收到的属性列
        properties.forEach((key, val) -> {
            if (key.contains(".")) {
                key = "`" + key + "`";
            }
            sbFieldNames.append(key)
                    .append(",");
            sbFieldPlaces.append("?,");
            args.add(val.getValue());
        });
        sbFieldNames.deleteCharAt(sbFieldNames.length() - 1);
        sbFieldPlaces.deleteCharAt(sbFieldPlaces.length() - 1);

        String sql = String.format("INSERT INTO %s (time,%s) USING %s TAGS ('%s') VALUES (?,%s);",
                Constants.getDevicePropertyTableName(deviceId),
                sbFieldNames,
                Constants.getProductPropertySTableName(device.getProductKey()),
                deviceId,
                sbFieldPlaces);

        tdTemplate.update(sql, args.toArray());
    }

}
