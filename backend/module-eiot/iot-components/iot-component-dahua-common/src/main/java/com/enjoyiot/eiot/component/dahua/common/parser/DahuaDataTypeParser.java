package com.enjoyiot.eiot.component.dahua.common.parser;

import java.util.Map;

/**
 * 大华协议一级数据类型解析器接口（策略模式）
 */
public interface DahuaDataTypeParser {

    /**
     * 返回此解析器处理的一级数据类型
     *
     * @return 数据类型字（0=注册包, 1=事件, 2=模拟量, 3=定长参数, 4=不定长参数,
     *         5=操作, 6=升级, 7=能力, 8=确认, 9=加密）
     */
    int getDataType();

    /**
     * 解析数据内容
     * <p>
     * 传入的 dataContent 是已从信息对象中提取的纯数据内容字节，
     * 不含 dataType(1B) 和 dataContentLength(2B) 前缀。
     *
     * @param dataContent 数据内容字节数组
     * @return 解析后的结构化 Map（字段名统一加前缀 "data_"）
     */
    Map<String, Object> parse(byte[] dataContent);
}
