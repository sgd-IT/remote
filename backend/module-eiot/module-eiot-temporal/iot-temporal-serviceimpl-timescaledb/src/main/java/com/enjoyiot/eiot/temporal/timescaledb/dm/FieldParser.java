package com.enjoyiot.eiot.temporal.timescaledb.dm;


import com.enjoyiot.module.eiot.api.thingmodel.dto.ThingModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.enjoyiot.eiot.common.enums.ErrorCodeConstants.FILED_DEFINE;
import static com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil.exception;

public class FieldParser {

    /**
     * 物模型到td数据类型映射
     */
    private static final Map<String, String> TYPE_MAPPING = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("int32", "INTEGER");
        put("float", "DOUBLE PRECISION");
        put("double", "DOUBLE PRECISION");
        put("bool", "BOOLEAN");
        put("enum", "SMALLINT");
        put("text", "TEXT");
        put("date", "VARCHAR");
        put("position", "VARCHAR");
    }});

    /**
     * 将物模型字段转换为td字段
     */
    public static PgField parse(ThingModel.Property property) {
        String filedName = property.getIdentifier().toLowerCase();
        ThingModel.DataType dataType = property.getDataType();
        String type = dataType.getType();

        //将物模型字段类型映射为td字段类型
        String fType = TYPE_MAPPING.get(type);
        Object specs = dataType.getSpecs();
        int len = -1;
        if (specs instanceof Map) {
            Object objLen = ((Map<?, ?>) specs).get("length");
            if (objLen != null) {
                try {
                    len = Integer.parseInt(objLen.toString());
                } catch (Exception e) {
                    throw exception(FILED_DEFINE, filedName + " 长度错误");
                }
            }
            if ("VARCHAR".equals(fType) && len < 1) {
                len = 255;
            }
        }

        // TEXT 类型不需要长度限制
        if ("TEXT".equals(fType)) {
            len = -1;
        }

        return new PgField(filedName, fType, len);
    }

    /**
     * 获取物模型中的字段列表
     */
    public static List<PgField> parse(ThingModel thingModel) {
        return thingModel.getModel().getProperties().stream().map(FieldParser::parse).collect(Collectors.toList());
    }

    /**
     * 获取字段字义
     */
    public static String getFieldDefine(PgField field) {
        return field.getName() + " " + (field.getLength() > 0 ?
                String.format("%s(%d)", field.getType(), field.getLength())
                : field.getType());
    }
}
