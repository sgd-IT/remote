package com.enjoyiot.eiot.temporal.rdb.dm;


import com.enjoyiot.module.eiot.api.thingmodel.dto.ThingModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.enjoyiot.eiot.common.enums.ErrorCodeConstants.FILED_DEFINE;
import static com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil.exception;

public class FieldParser {

    private static final Map<String, String> TYPE_MAPPING = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("int32", "BIGINT");
        put("float", "DOUBLE precision");
        put("double", "DOUBLE precision");
        put("bool", "INT");
        put("enum", "INT");
        put("text", "TEXT");
        put("varchar", "VARCHAR");
        put("date", "VARCHAR");
        put("position", "VARCHAR");
    }});

    public static RdbField parse(ThingModel.Property property) {
        String fieldName = property.getIdentifier().toLowerCase();
        ThingModel.DataType dataType = property.getDataType();
        String type = dataType.getType();

        String fType = TYPE_MAPPING.get(type);
        Object specs = dataType.getSpecs();
        int len = -1;
        if (specs instanceof Map) {
            Object objLen = ((Map<?, ?>) specs).get("length");
            if (objLen != null) {
                try {
                    len = Integer.parseInt(objLen.toString());
                } catch (Exception e) {
                    throw exception(FILED_DEFINE, fieldName + " 长度错误");
                }
            }
            if ("VARCHAR".equals(fType) && len < 1) {
                len = 255;
            }
        }

        if ("VARCHAR".equals(fType) && len < 0) {
            len = 255;
        }

        // TEXT 类型不需要长度限制（PostgreSQL/MySQL等原生 TEXT）
        if ("TEXT".equals(fType)) {
            len = -1;
        }

        return new RdbField(fieldName, fType, len);
    }

    public static List<RdbField> parse(ThingModel thingModel) {
        return thingModel.getModel().getProperties().stream().map(FieldParser::parse).collect(Collectors.toList());
    }

    public static String getFieldDefine(RdbField field) {
        String name = field.getName().replace("-", "_");
        return name + " " + (field.getLength() > 0 ?
                String.format("%s(%d)", field.getType(), field.getLength())
                : field.getType());
    }
}
