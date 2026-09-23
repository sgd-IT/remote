package com.enjoyiot.framework.common.util.json.databind;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Long 反序列化规则
 * 兼容 JSON 中 Long 类型为字符串的情况，解决前端发送超长 Long 值时已转为字符串的问题
 */
public class NumberDeserializer extends JsonDeserializer<Long> {

    public static final NumberDeserializer INSTANCE = new NumberDeserializer();

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return p.getLongValue();
        } else if (p.currentToken() == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
            if (text.isEmpty()) {
                return null;
            }
            return Long.parseLong(text);
        }
        return null;
    }
}
