package com.enjoyiot.eiot.component.ff.parser.handler;

import com.enjoyiot.eiot.component.dahua.common.parser.DahuaAduParser;
import com.enjoyiot.eiot.component.dahua.common.constants.DahuaProtocolConstants;
import com.enjoyiot.eiot.component.ff.parser.TypeHandler;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * 大华 0x87 通用数据帧 TypeHandler 适配器
 * <p>
 * 将公共模块的 {@link DahuaAduParser} 适配到 TCP 模块的 {@link TypeHandler} 接口，
 * 注册到 {@link com.enjoyiot.eiot.component.ff.parser.GbtDataParser} 的默认注册表。
 */
public class DahuaUniversalDataHandler implements TypeHandler {

    private final DahuaAduParser delegate = new DahuaAduParser("TCP");

    @Override
    public int getTypeFlag() {
        return DahuaProtocolConstants.TYPE_UNIVERSAL_DATA; // 0x87
    }

    @Override
    public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
        return delegate.parse(buf, count);
    }
}
