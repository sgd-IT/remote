package com.enjoyiot.eiot.component.dahua.common.parser;

import com.enjoyiot.eiot.component.dahua.common.constants.DahuaProtocolConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 大华协议模拟量类型解析器（dataType=2）
 */
@Slf4j
public class DahuaAnalogParser implements DahuaDataTypeParser {

    @Override
    public int getDataType() {
        return DahuaProtocolConstants.DATA_TYPE_ANALOG;
    }

    @Override
    public Map<String, Object> parse(byte[] dataContent) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data_type", DahuaProtocolConstants.DATA_TYPE_ANALOG);
        result.put("data_typeName", "模拟量");

        if (dataContent == null || dataContent.length < 2) {
            log.warn("[解析器] 数据内容不足: length={}", dataContent == null ? 0 : dataContent.length);
            result.put("data_rawHex", encodeHex(dataContent));
            return result;
        }

        ByteBuffer buf = ByteBuffer.wrap(dataContent).order(ByteOrder.LITTLE_ENDIAN);
        int position = 0;

        // 一个 dataContent 中可能包含多个模拟量条目（每个2B）
        // 当 dataContent 正好2B时，解析为单个模拟量值
        while (buf.remaining() >= 2) {
            short rawValue = buf.getShort();
            Map<String, Object> analogEntry = parseAnalogEntry(rawValue, position);
            // 将条目字段合并到结果 Map 中（带索引后缀区分多个条目）
            if (dataContent.length == 2) {
                // 单条目：不加索引后缀
                result.putAll(analogEntry);
            } else {
                // 多条目：加索引后缀 _0, _1, _2...
                for (Map.Entry<String, Object> e : analogEntry.entrySet()) {
                    result.put(e.getKey() + "_" + position, e.getValue());
                }
            }
            position++;
        }

        // 如果有不足2B的剩余字节
        if (buf.remaining() > 0) {
            byte[] remaining = new byte[buf.remaining()];
            buf.get(remaining);
            result.put("data_remainHex", encodeHex(remaining));
        }

        return result;
    }

    /**
     * 解析单个模拟量条目（2B有符号值）
     */
    private static Map<String, Object> parseAnalogEntry(short rawValue, int index) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("data_analogRaw", (int) rawValue);

        // 注意：模拟量类型在 0x87 TLV 中由外围的"数据类型"字段指定为2，
        // 具体的模拟量类型（温度/电压/电流等）由设备端按协议表定义，
        // 此处 rawValue 即为有符号的原始值，实际物理值 = rawValue × 对应类型的 scale
        // 由于0x87帧的数据内容不携带具体模拟量类型号，
        // 调用方需根据部件类型（componentType）来确定模拟量类型并换算

        // 提供常见模拟量类型的预换算（温度/电压/电流等常见类型）
        // 业务方可根据部件类型映射到具体 AnalogTypeDef 再做精确换算
        entry.put("data_analogValue", rawValue); // 原始值，业务层自行换算

        return entry;
    }

    private static String encodeHex(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
