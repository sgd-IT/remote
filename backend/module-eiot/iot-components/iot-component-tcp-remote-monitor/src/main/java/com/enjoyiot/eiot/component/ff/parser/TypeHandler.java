package com.enjoyiot.eiot.component.ff.parser;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * 应用数据单元类型处理器接口（策略模式）
 */
public interface TypeHandler {

    /**
     * 获取此处理器对应的类型标志（TLD）
     */
    int getTypeFlag();

    /**
     * 解析应用数据单元中的信息体
     */
    List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag);
}
