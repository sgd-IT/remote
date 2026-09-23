package com.enjoyiot.eiot.component.ff.parser;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * 应用数据单元类型处理器接口（策略模式）
 * <p>
 * 每种 TLD 类型标志对应一个 Handler 实现，注册到 {@link TypeHandlerRegistry} 后自动分发。
 * 新增 TLD 类型只需：1. 实现 Handler  2. 注册到 Registry，无需修改现有 switch-case。
 */
public interface TypeHandler {

    /**
     * 获取此处理器对应的类型标志（TLD）
     */
    int getTypeFlag();

    /**
     * 解析应用数据单元中的信息体
     *
     * @param buf      ByteBuffer（已跳过类型标志和信息体数目，position 指向第一条信息体）
     * @param count    信息对象数目
     * @param typeFlag 类型标志（冗余，便于一个 Handler 处理多个相近类型）
     * @return 解析结果列表，每条记录为一个 Map
     */
    List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag);
}
