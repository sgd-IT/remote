package com.enjoyiot.eiot.component.ff.parser;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型处理器注册表
 */
@Slf4j
public class TypeHandlerRegistry {

    private final Map<Integer, TypeHandler> handlerMap = new ConcurrentHashMap<>();

    /**
     * 注册类型处理器
     */
    public void register(TypeHandler handler) {
        handlerMap.put(handler.getTypeFlag(), handler);
        log.debug("[TypeHandlerRegistry] 注册: TLD={}", handler.getTypeFlag());
    }

    /**
     * 批量注册
     */
    public void registerAll(TypeHandler... handlers) {
        for (TypeHandler handler : handlers) {
            register(handler);
        }
    }

    /**
     * 解析应用数据单元
     *
     * @param data 完整应用数据单元字节数组（含类型标志和信息体数目）
     * @return 解析结果列表；无法识别时返回包含 rawHex 的兜底结果
     */
    public List<Map<String, Object>> parse(byte[] data) {
        if (data == null || data.length < 2) {
            return Collections.emptyList();
        }
        // 诊断日志：协议规定应用数据单元 ≤512B，超过则告警
        if (data.length > 512) {
            log.warn("[TypeHandlerRegistry] 应用数据单元长度{}超过协议上限512B，"
                    + "可能导致解析异常", data.length);
        }
        int typeFlag = data[0] & 0xFF;
        int count = data[1] & 0xFF;
        ByteBuffer buf = ByteBuffer.wrap(data, 2, data.length - 2)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);

        TypeHandler handler = handlerMap.get(typeFlag);
        if (handler != null) {
            return handler.parse(buf, count, typeFlag);
        }

        // 兜底：未识别类型，返回原始 hex
        log.info("[TypeHandlerRegistry] 未识别的类型标志: {}", typeFlag);
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("gbtType", typeFlag);
        m.put("count", count);
        m.put("rawHex", GbtDataPackage.bytesToHex(data));
        list.add(m);
        return list;
    }

    /**
     * 获取应用数据中的类型标志（第1字节）
     */
    public static int getTypeFlag(byte[] data) {
        return (data != null && data.length > 0) ? (data[0] & 0xFF) : -1;
    }

    /**
     * 是否已注册指定类型
     */
    public boolean isRegistered(int typeFlag) {
        return handlerMap.containsKey(typeFlag);
    }
}
