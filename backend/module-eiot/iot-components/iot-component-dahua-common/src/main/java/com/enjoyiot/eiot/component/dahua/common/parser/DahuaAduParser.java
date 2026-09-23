package com.enjoyiot.eiot.component.dahua.common.parser;

import com.enjoyiot.eiot.component.dahua.common.constants.DahuaProtocolConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大华协议通用数据帧（0x87）ADU 解析器
 */
@Slf4j
public class DahuaAduParser {

    /**
     * 数据来源标识（如 "TCP" / "UDP"），用于日志区分解析链路
     */
    private final String sourceTag;

    /**
     * 一级数据类型 → 解析器映射（V1 扁平路径使用）
     */
    private final Map<Integer, DahuaDataTypeParser> dataTypeParserMap = new LinkedHashMap<>();

    public DahuaAduParser() {
        this("未知");
    }

    public DahuaAduParser(String sourceTag) {
        this.sourceTag = (sourceTag == null || sourceTag.isEmpty()) ? "未知" : sourceTag;
        // 内置解析器注册
        register(new DahuaEventParser());
        register(new DahuaAnalogParser());
    }

    /**
     * 注册一级数据类型解析器
     *
     * @param parser 数据类型解析器
     */
    public void register(DahuaDataTypeParser parser) {
        dataTypeParserMap.put(parser.getDataType(), parser);
        log.debug("[{}] 注册解析器: dataType={}({})",
                sourceTag, parser.getDataType(), getDataTypeName(parser.getDataType()));
    }

    // ==================== 解析入口 ====================

    /**
     * 解析通用数据帧的信息对象列表
     *
     * @param buf   ByteBuffer，position 已指向第一条信息对象（类型标志和信息体数目已被外部消费）
     * @param count 信息对象数目
     * @return 解析后的信息对象列表，每个元素为结构化 Map
     */
    public List<Map<String, Object>> parse(ByteBuffer buf, int count) {
        List<Map<String, Object>> results = new ArrayList<>(count);

        // ===== 整帧格式预判（试走验证）=====
        // 多对象帧中，逐对象的 V2 检测（peekLen == remaining-8）只对末位对象成立，
        // 首对象必然误判回退 V1，导致 V2 帧被错位解析。
        // 改为：先按 V2_LE/V2_BE/V1_LE 三种假设各试走全部对象，哪个恰好耗尽缓冲区就整帧用哪个格式。
        String frameFormat = determineFrameFormat(buf, count);
        if (frameFormat != null) {
            log.info("[{}] 帧格式: {}, 对象数={}", sourceTag, frameFormat, count);
        }

        for (int i = 0; i < count; i++) {
            if (!hasEnoughBytes(buf, i)) {
                break;
            }

            Map<String, Object> infoObj;
            if ("V2_LE".equals(frameFormat)) {
                infoObj = parseV2Object(buf, i);
            } else if ("V2_BE".equals(frameFormat)) {
                infoObj = parseV2ObjectBE(buf, i);
            } else if ("V1_LE".equals(frameFormat)) {
                infoObj = parseV1InfoObject(buf, i);
            } else {
                // 预判失败（三种假设都不成立），回退逐对象自动检测
                infoObj = parseOneInfoObject(buf, i);
            }
            results.add(infoObj);

            // 数据内容被截断：当前帧已丢失同步，终止后续解析避免级联错位
            if (Boolean.TRUE.equals(infoObj.remove("_truncated"))) {
                log.warn("[{}] 帧数据截断，已解析 {}/{} 个对象", sourceTag, i + 1, count);
                break;
            }
        }

        for (int i = 0; i < results.size(); i++) {
            log.info("[{}] 解析#{}: {}", sourceTag, i, summarizeResult(results.get(i)));
        }

        if (buf.remaining() > 0) {
            log.info("[{}] 剩余未消费字节: {}B", sourceTag, buf.remaining());
        }

        return results;
    }

    private static String summarizeResult(Map<String, Object> obj) {
        int dataType = toInt(obj.get("dataType"), -1);
        if (dataType == -1) dataType = toInt(obj.get("gbtType"), -1);
        switch (dataType) {
            case 0:
                return String.format("注册 部件=%s IMEI=%s 型号=%s SN=%s 版本=%s",
                        obj.get("componentTypeName"),
                        obj.getOrDefault("data_regImei", "-"),
                        obj.getOrDefault("data_regModel", "-"),
                        obj.getOrDefault("data_regSn", "-"),
                        obj.getOrDefault("data_regSoftVer", "-"));
            case 1:
                return String.format("事件 subType=%s(%s) alarmType=%s(%s)",
                        obj.get("data_subType"),
                        obj.get("data_subTypeName"),
                        obj.get("data_alarmType"),
                        obj.get("data_alarmTypeName"));
            case 2:
                return String.format("模拟量 type=%s raw=%s",
                        obj.get("data_analogType"),
                        obj.getOrDefault("data_analogValue", obj.get("data_analogValue_0")));
            case 3: case 4: case 5: case 6: case 7: case 8: case 9:
                return "参数 dataType=" + dataType + " keys=" + obj.keySet();
            case 0x84:
                return String.format("系统模拟量 type=%s raw=%s",
                        obj.get("sysAnalog_type"), obj.get("sysAnalog_rawValue"));
            default:
                return "raw keys=" + obj.keySet();
        }
    }

    private static int toInt(Object v, int def) {
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    // ==================== 整帧格式预判 ====================

    /**
     * 预判整帧格式：按 V2_LE → V2_BE → V1_LE 顺序试走
     *
     * @return "V2_LE" / "V2_BE" / "V1_LE" / null（全部失败，回退逐对象检测）
     */
    private String determineFrameFormat(ByteBuffer buf, int count) {
        if (count <= 0) {
            return null;
        }
        if (validateV2Frame(buf, count)) {
            return "V2_LE";
        }
        if (validateV2FrameBE(buf, count)) {
            return "V2_BE";
        }
        if (validateV1Frame(buf, count)) {
            return "V1_LE";
        }
        log.warn("[{}] [解析器] 整帧 V2_LE/V2_BE/V1_LE 试走均失败 (count={}, remaining={})，回退逐对象检测",
                sourceTag, count, buf.remaining());
        return null;
    }

    /**
     * V2 试走（LE）：按 V2 格式迭代 count 个对象
     */
    private boolean validateV2Frame(ByteBuffer buf, int count) {
        int pos = buf.position();
        int remaining = buf.remaining();
        for (int i = 0; i < count; i++) {
            if (remaining < 15) {
                return false;
            }
            int lenLE = (buf.get(pos + 7) & 0xFF) | ((buf.get(pos + 8) & 0xFF) << 8);
            int objLen = 15 + lenLE;
            if (objLen > remaining) {
                return false;
            }
            pos += objLen;
            remaining -= objLen;
        }
        return remaining == 0;
    }

    /**
     * V2 试走（BE）：兼容设备固件将地址/长度发成大端的非合规情况
     * <p>
     * V2 对象：sysType(1)+sysAddr(1)+compType(1)+addr(4,BE)+dataTotalLen(2,BE)+TLV(m')+ts(6)
     * 其中 dataTotalLen(BE) 包含 TLV + timestamp，即 m' = dataTotalLen - 6
     * 对象总长 = 9 + dataTotalLen(BE) 字节
     * 长度字段在对象内偏移 7-8（BE）
     */
    private boolean validateV2FrameBE(ByteBuffer buf, int count) {
        int pos = buf.position();
        int remaining = buf.remaining();
        for (int i = 0; i < count; i++) {
            if (remaining < 15) {
                return false;
            }
            // BE 读 dataTotalLen（包含 TLV + timestamp）
            int lenBE = ((buf.get(pos + 7) & 0xFF) << 8) | (buf.get(pos + 8) & 0xFF);
            // 合理性检查：dataTotalLen 应 >= 6（至少容纳 timestamp）且 <= 剩余字节
            if (lenBE < 6 || lenBE > remaining - 9) {
                return false;
            }
            int objLen = 9 + lenBE;  // 9B头 + dataTotalLen(含ts)
            if (objLen > remaining) {
                return false;
            }
            pos += objLen;
            remaining -= objLen;
        }
        return remaining == 0;
    }

    /**
     * V1 试走：按 V1 格式迭代 count 个对象
     * <p>
     * V1 对象：compType(1)+addr(4)+dataType(1)+dataContentLen(2,LE)+data(N)+ts(6) = 14+N 字节
     * 长度字段在对象内偏移 6-7
     */
    private boolean validateV1Frame(ByteBuffer buf, int count) {
        int pos = buf.position();
        int remaining = buf.remaining();
        for (int i = 0; i < count; i++) {
            if (remaining < 14) {
                return false;
            }
            int lenLE = (buf.get(pos + 6) & 0xFF) | ((buf.get(pos + 7) & 0xFF) << 8);
            int objLen = 14 + lenLE;
            if (objLen > remaining) {
                return false;
            }
            pos += objLen;
            remaining -= objLen;
        }
        return remaining == 0;
    }

    /**
     * 按已确认的 V2 格式解析单个信息对象（无检测逻辑，LE 字节序）
     */
    private Map<String, Object> parseV2Object(ByteBuffer buf, int index) {
        Map<String, Object> infoObj = new LinkedHashMap<>();
        infoObj.put("systemType", buf.get() & 0xFF);
        infoObj.put("systemAddr", buf.get() & 0xFF);
        int compType = buf.get() & 0xFF;
        infoObj.put("componentType", compType);
        infoObj.put("componentTypeName", getComponentTypeName(compType));
        int nodeAddr = buf.getShort() & 0xFFFF;
        int loopAddr = buf.getShort() & 0xFFFF;
        infoObj.put("componentNodeAddr", nodeAddr);
        infoObj.put("componentLoopAddr", loopAddr);
        infoObj.put("componentAddr", String.format("%04X%04X", nodeAddr, loopAddr));

        // peek 长度字段（parseV2InfoObject 内部会消费）
        int p = buf.position();
        int peekLen = (buf.get(p) & 0xFF) | ((buf.get(p + 1) & 0xFF) << 8);
        parseV2InfoObject(buf, index, infoObj, peekLen);
        return infoObj;
    }

    /**
     * 按已确认的 V2 格式解析单个信息对象（BE 字节序，兼容设备固件 bug）
     * <p>
     * 与 parseV2Object 的区别：
     * 1. addr(4B) 和 dataTotalLen(2B) 按 BE 读取
     * 2. dataTotalLen 包含 TLV + timestamp，实际 TLV 长度 = dataTotalLen - 6
     */
    private Map<String, Object> parseV2ObjectBE(ByteBuffer buf, int index) {
        Map<String, Object> infoObj = new LinkedHashMap<>();
        infoObj.put("systemType", buf.get() & 0xFF);
        infoObj.put("systemAddr", buf.get() & 0xFF);
        int compType = buf.get() & 0xFF;
        infoObj.put("componentType", compType);
        infoObj.put("componentTypeName", getComponentTypeName(compType));

        // BE 读 addr(4B): nodeAddr(2B,BE) + loopAddr(2B,BE)
        int nodeAddr = ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);
        int loopAddr = ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);
        infoObj.put("componentNodeAddr", nodeAddr);
        infoObj.put("componentLoopAddr", loopAddr);
        infoObj.put("componentAddr", String.format("%04X%04X", nodeAddr, loopAddr));

        // BE 读 dataTotalLen（包含 TLV + timestamp）
        int dataTotalLen = ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);
        infoObj.put("dataTotalLength", dataTotalLen);

        // 实际 TLV 长度 = dataTotalLen - 6（timestamp 占 6B）
        int tlvLen = dataTotalLen - 6;
        if (tlvLen > 0 && buf.remaining() >= tlvLen) {
            byte[] tlvBytes = new byte[tlvLen];
            buf.get(tlvBytes);
            decodeTlvData(tlvBytes, infoObj);
        } else if (tlvLen > 0) {
            log.warn("[{}] [解析器] infoObject[{}] V2_BE TLV长度{}超过剩余字节{}，终止解析",
                    sourceTag, index, tlvLen, buf.remaining());
            int available = buf.remaining();
            if (available > 0) {
                byte[] partial = new byte[available];
                buf.get(partial);
                infoObj.put("data_rawHex", encodeHex(partial));
            }
            infoObj.put("_truncated", true);
            return infoObj;
        }

        // 读 timestamp(6B)
        parseTimestamp(buf, index, infoObj);
        return infoObj;
    }

    // ==================== 信息对象解析 ====================

    /**
     * 检查是否有足够字节解析下一条信息对象
     * <p>
     * V2 最小：系统类型1 + 系统地址1 + 部件类型1 + 部件地址4 + 长度2 + 时间标签6 = 15B
     * V1 最小：部件类型1 + 部件地址4 + 数据类型1 + 长度2 + 时间标签6 = 14B
     */
    private boolean hasEnoughBytes(ByteBuffer buf, int index) {
        int v2Min = 1 + 1 + 1 + DahuaProtocolConstants.COMPONENT_ADDR_LENGTH + 2
                + DahuaProtocolConstants.TIMESTAMP_LENGTH; // 15B
        if (buf.remaining() < v2Min) {
            int v1Min = 1 + DahuaProtocolConstants.COMPONENT_ADDR_LENGTH + 1 + 2
                    + DahuaProtocolConstants.TIMESTAMP_LENGTH; // 14B
            if (buf.remaining() < v1Min) {
                log.warn("[{}] [解析器] infoObject[{}] 剩余字节不足: {} < {}", sourceTag, index, buf.remaining(), v1Min);
                return false;
            }
        }
        return true;
    }

    /**
     * 解析单个信息对象（自动检测 V1/V2 格式）
     * <p>
     * 检测策略：
     * 1. 先假设 V2：读 sysType(1)+sysAddr(1)+compType(1)+addr(4)，
     * peek 后续 2B 作为 LE short，若 == remaining - 8 则确认为 V2
     * 2. 否则回退 V1：回退 position，重新按 V1 格式解析
     */
    private Map<String, Object> parseOneInfoObject(ByteBuffer buf, int index) {
        int startPos = buf.position();
        int remaining = buf.remaining();

        // ===== 假设一：V2（协议推荐格式） =====
        // V2: sysType(1) + sysAddr(1) + compType(1) + addr(4) + 总长(2,LE) + TLV(m) + 时间(6)
        // 读 7B 头后 remaining = 2 + m + 6，故 m == remaining - 8
        if (remaining >= 15) {
            int sysType = buf.get() & 0xFF;
            int sysAddr = buf.get() & 0xFF;
            int compType = buf.get() & 0xFF;
            int nodeAddr = buf.getShort() & 0xFFFF;
            int loopAddr = buf.getShort() & 0xFFFF;

            int p = buf.position();
            int peekLen = (remaining >= 9)
                    ? (buf.get(p) & 0xFF) | ((buf.get(p + 1) & 0xFF) << 8)
                    : -1;

            if (peekLen >= 0 && peekLen == remaining - 8) {
                // V2 确认
                Map<String, Object> infoObj = new LinkedHashMap<>();
                infoObj.put("systemType", sysType);
                infoObj.put("systemAddr", sysAddr);
                infoObj.put("componentType", compType);
                infoObj.put("componentTypeName", getComponentTypeName(compType));
                infoObj.put("componentNodeAddr", nodeAddr);
                infoObj.put("componentLoopAddr", loopAddr);
                infoObj.put("componentAddr", String.format("%04X%04X", nodeAddr, loopAddr));
                parseV2InfoObject(buf, index, infoObj, peekLen);
                return infoObj;
            }

            // V2 不匹配，回退
            buf.position(startPos);
        }

        // ===== 假设二：V1（旧格式，无系统类型/地址前缀） =====
        return parseV1InfoObject(buf, index);
    }

    // ==================== V2 格式解析（TLV） ====================

    /**
     * V2 格式：数据总长度(2B,LE) + 数据TLV结构(mB) + 时间标签(6B)
     *
     * @param peekLen 已 peek 到的 dataTotalLength 值（2B,LE）
     */
    private void parseV2InfoObject(ByteBuffer buf, int index, Map<String, Object> infoObj, int peekLen) {
        // 消费已 peek 的 2B 长度字段
        int dataTotalLength = buf.getShort() & 0xFFFF;
        infoObj.put("dataTotalLength", dataTotalLength);

        if (dataTotalLength > 0 && buf.remaining() >= dataTotalLength) {
            byte[] tlvBytes = new byte[dataTotalLength];
            buf.get(tlvBytes);
            decodeTlvData(tlvBytes, infoObj);
        } else if (dataTotalLength > 0) {
            log.warn("[{}] [解析器] infoObject[{}] V2数据总长度{}超过剩余字节{}，终止解析",
                    sourceTag, index, dataTotalLength, buf.remaining());
            int available = buf.remaining();
            if (available > 0) {
                byte[] partial = new byte[available];
                buf.get(partial);
                infoObj.put("data_rawHex", encodeHex(partial));
            }
            infoObj.put("_truncated", true);
            return;
        }

        parseTimestamp(buf, index, infoObj);
    }

    /**
     * 解析 TLV 数据并分发解码
     */
    private void decodeTlvData(byte[] tlvBytes, Map<String, Object> infoObj) {
        List<DahuaTlv.Node> nodes;
        try {
            nodes = DahuaTlv.parse(tlvBytes);
        } catch (Exception e) {
            log.warn("[{}] [解析器] TLV解析异常: {}", sourceTag, e.getMessage());
            infoObj.put("data_rawHex", encodeHex(tlvBytes));
            return;
        }
        if (nodes.isEmpty()) {
            infoObj.put("data_rawHex", encodeHex(tlvBytes));
            return;
        }
        for (DahuaTlv.Node node : nodes) {
            decodeFirstLevel(node, infoObj);
        }
    }

    // ==================== V1 格式解析（扁平） ====================

    /**
     * V1 格式：部件类型(1B) + 部件地址(4B) + 数据类型(1B) + 数据内容长度(2B) + 数据内容(NB) + 时间标签(6B)
     * <p>
     * 数据内容长度字节序自动裁决：尝试 LE 和 BE 两种读法，
     * 选择使 dataContentLength + 6 == remaining - 8 的那一种。
     */
    private Map<String, Object> parseV1InfoObject(ByteBuffer buf, int index) {
        Map<String, Object> infoObj = new LinkedHashMap<>();

        // 1. 部件类型 (1B)
        int componentType = buf.get() & 0xFF;
        infoObj.put("componentType", componentType);
        infoObj.put("componentTypeName", getComponentTypeName(componentType));

        // 2. 部件地址 (4B, LE: nodeAddr(2B) + loopAddr(2B))
        int nodeAddr = buf.getShort() & 0xFFFF;
        int loopAddr = buf.getShort() & 0xFFFF;
        infoObj.put("componentNodeAddr", nodeAddr);
        infoObj.put("componentLoopAddr", loopAddr);
        infoObj.put("componentAddr", String.format("%04X%04X", nodeAddr, loopAddr));

        // 3. 数据类型 (1B)
        int dataType = buf.get() & 0xFF;
        infoObj.put("dataType", dataType);
        infoObj.put("dataTypeName", getDataTypeName(dataType));

        // 4. 数据内容长度 (2B) — 字节序自动裁决
        // 协议 V1.10 明确规定："除字符串，其它类型数据统一为小端传输模式"
        // 因此 LE 为首选，BE 仅在 LE 明显不合理时作为回退
        int p = buf.position();
        int lenLE = (buf.get(p) & 0xFF) | ((buf.get(p + 1) & 0xFF) << 8);
        int lenBE = ((buf.get(p) & 0xFF) << 8) | (buf.get(p + 1) & 0xFF);
        int remaining = buf.remaining();
        int afterLenRemaining = remaining - 2; // 消费长度字段后的剩余 = dataContent + timestamp + 后续对象
        // 合理性上限：协议规定应用数据单元 ≤512B，单个信息对象数据内容不应超过此值
        final int MAX_REASONABLE_LENGTH = 512;
        int dataContentLength;
        if (lenLE + 6 == afterLenRemaining) {
            dataContentLength = lenLE;           // LE 精确匹配（末位对象）
        } else if (lenBE + 6 == afterLenRemaining) {
            dataContentLength = lenBE;           // BE 精确匹配（末位对象）
        } else if (lenLE > afterLenRemaining && lenBE <= afterLenRemaining) {
            dataContentLength = lenBE;           // LE 明显不合理，选 BE
        } else if (lenBE > afterLenRemaining && lenLE <= afterLenRemaining) {
            dataContentLength = lenLE;           // BE 明显不合理，选 LE（协议优先）
        } else if (lenLE <= afterLenRemaining && lenBE <= afterLenRemaining) {
            // 都合理（多对象场景）：协议规定 LE，优先选 LE
            dataContentLength = lenLE;
        } else {
            // 两者都超过剩余字节——取较小值，但标记为可疑
            dataContentLength = Math.min(lenLE, lenBE);
            if (dataContentLength > MAX_REASONABLE_LENGTH) {
                log.warn("[{}] [解析器] infoObject[{}] 长度异常大: lenLE={}, lenBE={}, "
                                + "afterLenRemaining={}, compType={}, dataType={}，"
                                + "疑似帧对齐偏移或设备编码错误",
                        sourceTag, index, lenLE, lenBE, afterLenRemaining,
                        componentType, dataType);
            }
        }
        buf.position(p + 2);
        infoObj.put("dataContentLength", dataContentLength);

        // 5. 数据内容 (NB)
        if (dataContentLength > 0 && buf.remaining() >= dataContentLength) {
            byte[] dataContent = new byte[dataContentLength];
            buf.get(dataContent);

            // 分发到对应的一级数据类型解析器
            DahuaDataTypeParser typeParser = dataTypeParserMap.get(dataType);
            if (typeParser != null) {
                try {
                    Map<String, Object> parsed = typeParser.parse(dataContent);
                    infoObj.putAll(parsed);
                } catch (Exception e) {
                    log.error("[{}] [解析器] 一级数据类型解析异常: dataType={}, err={}", sourceTag, dataType, e.getMessage());
                    infoObj.put("data_rawHex", encodeHex(dataContent));
                    infoObj.put("data_parseError", e.getMessage());
                }
            } else {
                infoObj.put("data_rawHex", encodeHex(dataContent));
                log.info("[解析器] 未注册的一级数据类型: dataType={}", dataType);
            }
        } else if (dataContentLength > 0) {
            int available = buf.remaining();
            log.warn("[{}] [解析器] infoObject[{}] 数据内容长度{}超过剩余字节{}，"
                            + "lenLE={}, lenBE={}, compType={}, dataType={}，读取可用部分并终止解析",
                    sourceTag, index, dataContentLength, available,
                    lenLE, lenBE, componentType, dataType);
            if (available > 0) {
                byte[] partial = new byte[available];
                buf.get(partial);
                infoObj.put("data_rawHex", encodeHex(partial));
                infoObj.put("data_truncated", true);
            }
            infoObj.put("_truncated", true);
            return infoObj;
        }

        // 6. 时间标签 (6B: sec/min/hour/day/month/year-2000)
        parseTimestamp(buf, index, infoObj);

        return infoObj;
    }

    // ==================== 共用工具 ====================

    /**
     * 解析时间标签 (6B: sec/min/hour/day/month/year-2000)
     */
    private void parseTimestamp(ByteBuffer buf, int index, Map<String, Object> infoObj) {
        if (buf.remaining() >= DahuaProtocolConstants.TIMESTAMP_LENGTH) {
            byte[] ts = new byte[DahuaProtocolConstants.TIMESTAMP_LENGTH];
            buf.get(ts);
            infoObj.put("timestamp", encodeHex(ts));
            infoObj.put("timestampStr", formatTimestamp(ts));
        } else {
            log.warn("[{}] [解析器] infoObject[{}] 时间标签不足，剩余{}B", sourceTag, index, buf.remaining());
        }
    }

    // ==================== V2 TLV 一级数据类型解码 ====================

    /**
     * 解码一级 TLV 节点（V2 格式使用）
     */
    private void decodeFirstLevel(DahuaTlv.Node node, Map<String, Object> obj) {
        int dt = node.type;
        obj.put("dataType", dt);
        obj.put("dataTypeName", getDataTypeName(dt));

        switch (dt) {
            case DahuaProtocolConstants.DATA_TYPE_REGISTER:
                decodeRegister(node, obj);
                break;
            case DahuaProtocolConstants.DATA_TYPE_EVENT:
                decodeEvent(node, obj);
                break;
            case DahuaProtocolConstants.DATA_TYPE_ANALOG:
                decodeAnalog(node, obj);
                break;
            case DahuaProtocolConstants.DATA_TYPE_PARAM_FIXED:
            case DahuaProtocolConstants.DATA_TYPE_PARAM_VARIABLE:
                decodeParam(node, obj);
                break;
            case DahuaProtocolConstants.DATA_TYPE_OPERATION:
                decodeOperation(node, obj);
                break;
            case DahuaProtocolConstants.DATA_TYPE_UPGRADE:
                decodeUpgrade(node, obj);
                break;
            case DahuaProtocolConstants.DATA_TYPE_CAPABILITY:
                decodeCapability(node, obj);
                break;
            case DahuaProtocolConstants.DATA_TYPE_CONFIRM:
                decodeConfirm(node, obj);
                break;
            case DahuaProtocolConstants.DATA_TYPE_ENCRYPT:
                decodeEncrypt(node, obj);
                break;
            default:
                putLeafOrRaw(node, obj, "data_unknown");
                break;
        }
    }

    /**
     * 注册包（表3.2.1-1，定长字段顺序）：
     * 协议版本2B | 软件版本2B | SN 16B ASCII | IMEI 8B BCD | IMSI 8B BCD
     * | CCID 10B BCD | 保活4B LE | 产品型号16B ASCII（共66B，后续为扩展字段）
     */
    private void decodeRegister(DahuaTlv.Node node, Map<String, Object> obj) {
        byte[] v = leafValue(node);
        if (v == null) return;
        ByteBuffer b = ByteBuffer.wrap(v).order(ByteOrder.LITTLE_ENDIAN);
        obj.put("data_regProtoVer", readVersion(b));
        obj.put("data_regSoftVer", readVersion(b));
        obj.put("data_regSn", readAscii(b, 16));
        obj.put("data_regImei", readBcd(b, 8));
        obj.put("data_regImsi", readBcd(b, 8));
        obj.put("data_regCcid", readBcd(b, 10));
        if (b.remaining() >= 4) {
            obj.put("data_regKeepaliveSec", b.getInt() & 0xFFFFFFFFL);
        }
        obj.put("data_regModel", readAscii(b, 16));
        if (b.remaining() > 0) {
            byte[] ext = new byte[b.remaining()];
            b.get(ext);
            obj.put("data_regExtHex", encodeHex(ext));
        }
    }

    /**
     * 事件：子节点 = 事件类型（表3.2.2-1类型字），子节点value = 事件值2B（低字节子类型/高字节报警类型）
     */
    private void decodeEvent(DahuaTlv.Node node, Map<String, Object> obj) {
        int i = 0;
        for (DahuaTlv.Node child : childrenOf(node)) {
            String suffix = "_" + i++;
            obj.put("data_eventType" + suffix, child.type);
            byte[] ev = child.nested ? null : child.value;
            if (ev != null) {
                Map<String, Object> decoded = new DahuaEventParser().parse(ev);
                decoded.forEach((k, val) -> obj.put(k + suffix, val));
            }
        }
        if (i == 0 && !node.nested && node.value != null && node.value.length >= 6) {
            // ponytail: 叶子平铺6B — eventType(2B LE) + len(2B) + eventValue(2B)
            // 码表3.2.2-1: eventValue = 低字节(类型) + 高字节(报警类型)
            ByteBuffer buf = ByteBuffer.wrap(node.value).order(ByteOrder.LITTLE_ENDIAN);
            int eventType = buf.getShort() & 0xFFFF;
            buf.getShort(); // len(恒=2), skip
            byte[] evVal = new byte[Math.min(2, buf.remaining())];
            buf.get(evVal);
            int alarmType = evVal.length >= 2 ? (evVal[1] & 0xFF) : 0;
            // 复用 DahuaEventParser 码表: 传 [eventType 低][alarmType 高]
            new DahuaEventParser().parse(new byte[]{(byte) eventType, (byte) alarmType})
                    .forEach(obj::put);
            obj.put("data_eventType", eventType);
            obj.put("data_eventValueType", evVal.length >= 1 ? (evVal[0] & 0xFF) : 0);
        } else if (i == 0) {
            putLeafOrRaw(node, obj, "data_event");
        }
    }

    /**
     * 模拟量：子节点 = 模拟量类型（表3.2.3-1类型字，2B），子节点value = 模拟量值
     * <p>也支持平铺6B叶子格式：analogType(2B LE) + length(2B LE) + value(2B LE)</p>
     */
    private void decodeAnalog(DahuaTlv.Node node, Map<String, Object> obj) {
        int i = 0;
        for (DahuaTlv.Node child : childrenOf(node)) {
            String suffix = "_" + i++;
            obj.put("data_analogType" + suffix, child.type);
            if (!child.nested && child.value != null) {
                Map<String, Object> decoded = new DahuaAnalogParser().parse(child.value);
                decoded.forEach((k, val) -> obj.put(k + suffix, val));
            }
        }
        if (i == 0 && !node.nested && node.value != null && node.value.length == 6) {
            // 平铺6B叶子: analogType(2B LE) + length(2B LE) + analogValue(2B LE)
            ByteBuffer buf = ByteBuffer.wrap(node.value).order(ByteOrder.LITTLE_ENDIAN);
            int analogType = buf.getShort() & 0xFFFF;
            buf.getShort(); // length field (恒=2), skip
            int analogValue = buf.getShort() & 0xFFFF;
            obj.put("data_analogType", analogType);
            obj.put("data_analogRaw_0", analogValue);
            obj.put("data_analogValue_0", analogValue);
            obj.put("data_analogValue", analogValue); // summarizeResult 读此 key
        } else if (i == 0) {
            putLeafOrRaw(node, obj, "data_analog");
        }
    }

    /**
     * 参数：子节点 = 参数类型，value：定长4B LE / 不定长字节串
     */
    private void decodeParam(DahuaTlv.Node node, Map<String, Object> obj) {
        int i = 0;
        for (DahuaTlv.Node child : childrenOf(node)) {
            String suffix = "_" + i++;
            obj.put("data_paramType" + suffix, child.type);
            if (!child.nested && child.value != null) {
                if (child.value.length == 4) {
                    obj.put("data_paramValue" + suffix, child.valueAsU32LE());
                } else {
                    obj.put("data_paramValueHex" + suffix, encodeHex(child.value));
                }
            }
        }
        if (i == 0) {
            putLeafOrRaw(node, obj, "data_param");
        }
    }

    /**
     * 操作：子节点 = 操作类型（表3.2.5-1），value 2B LE
     */
    private void decodeOperation(DahuaTlv.Node node, Map<String, Object> obj) {
        int i = 0;
        for (DahuaTlv.Node child : childrenOf(node)) {
            String suffix = "_" + i++;
            obj.put("data_opType" + suffix, child.type);
            obj.put("data_opTypeName" + suffix, operationName(child.type));
            if (!child.nested) {
                obj.put("data_opValue" + suffix, child.valueAsU16LE());
            }
        }
        if (i == 0) {
            putLeafOrRaw(node, obj, "data_op");
        }
    }

    /**
     * 升级：子节点 = 升级类型（表3.2.6-1），value 2B/4B/不定长
     */
    private void decodeUpgrade(DahuaTlv.Node node, Map<String, Object> obj) {
        int i = 0;
        for (DahuaTlv.Node child : childrenOf(node)) {
            String suffix = "_" + i++;
            obj.put("data_upgradeType" + suffix, child.type);
            if (!child.nested && child.value != null) {
                if (child.value.length == 4) {
                    obj.put("data_upgradeValue" + suffix, child.valueAsU32LE());
                } else if (child.value.length == 2) {
                    obj.put("data_upgradeValue" + suffix, child.valueAsU16LE());
                } else {
                    obj.put("data_upgradeValueHex" + suffix, encodeHex(child.value));
                }
            }
        }
        if (i == 0) {
            putLeafOrRaw(node, obj, "data_upgrade");
        }
    }

    /**
     * 能力：子节点 = 能力类型（表3.2.7-1），value：Bool 1B / uint32 4B LE
     */
    private void decodeCapability(DahuaTlv.Node node, Map<String, Object> obj) {
        int i = 0;
        for (DahuaTlv.Node child : childrenOf(node)) {
            String suffix = "_" + i++;
            obj.put("data_capType" + suffix, child.type);
            if (!child.nested && child.value != null) {
                if (child.value.length == 1) {
                    obj.put("data_capBool" + suffix, (child.value[0] & 0xFF) != 0);
                } else if (child.value.length == 4) {
                    obj.put("data_capValue" + suffix, child.valueAsU32LE());
                } else {
                    obj.put("data_capValueHex" + suffix, encodeHex(child.value));
                }
            }
        }
        if (i == 0) {
            putLeafOrRaw(node, obj, "data_cap");
        }
    }

    /**
     * 含参确认：子节点 = 确认类型（表3.2.8-1），value 4B（高字节预留+低3字节参数）或不定长
     */
    private void decodeConfirm(DahuaTlv.Node node, Map<String, Object> obj) {
        int i = 0;
        for (DahuaTlv.Node child : childrenOf(node)) {
            String suffix = "_" + i++;
            obj.put("data_confirmType" + suffix, child.type);
            if (!child.nested && child.value != null) {
                if (child.value.length == 4) {
                    obj.put("data_confirmStatus" + suffix, child.value[3] & 0xFF);
                    obj.put("data_confirmParam" + suffix,
                            (child.value[0] & 0xFF) | ((child.value[1] & 0xFF) << 8)
                                    | ((child.value[2] & 0xFF) << 16));
                } else {
                    obj.put("data_confirmValueHex" + suffix, encodeHex(child.value));
                }
            }
        }
        if (i == 0) {
            putLeafOrRaw(node, obj, "data_confirm");
        }
    }

    /**
     * 加密：子节点 = 加密类型（表3.2.9-1，0=无加密，1=异或）
     */
    private void decodeEncrypt(DahuaTlv.Node node, Map<String, Object> obj) {
        int i = 0;
        for (DahuaTlv.Node child : childrenOf(node)) {
            String suffix = "_" + i++;
            obj.put("data_encryptType" + suffix, child.type);
            if (!child.nested && child.value != null && child.value.length > 0) {
                obj.put("data_encryptValueHex" + suffix, encodeHex(child.value));
            }
        }
        if (i == 0) {
            putLeafOrRaw(node, obj, "data_encrypt");
        }
    }

    // ==================== TLV 解码工具 ====================

    /**
     * 取叶子节点value；嵌套节点返回其首个叶子子孙或null
     */
    private static byte[] leafValue(DahuaTlv.Node node) {
        if (!node.nested) return node.value;
        for (DahuaTlv.Node c : node.children) {
            byte[] v = leafValue(c);
            if (v != null) return v;
        }
        return null;
    }

    private static List<DahuaTlv.Node> childrenOf(DahuaTlv.Node node) {
        return node.nested ? node.children : Collections.emptyList();
    }

    private static void putLeafOrRaw(DahuaTlv.Node node, Map<String, Object> obj, String prefix) {
        if (!node.nested && node.value != null) {
            obj.put(prefix + "Hex", encodeHex(node.value));
        } else {
            obj.put(prefix + "NestedRaw", String.valueOf(node));
        }
    }

    private static String readVersion(ByteBuffer b) {
        if (b.remaining() < 2) return "";
        int lo = b.get() & 0xFF;
        int hi = b.get() & 0xFF;
        return "V" + hi + "." + String.format("%02d", lo);
    }

    private static String readAscii(ByteBuffer b, int len) {
        if (b.remaining() < len) return "";
        byte[] s = new byte[len];
        b.get(s);
        return new String(s, StandardCharsets.US_ASCII).replace("\0", "").trim();
    }

    private static String readBcd(ByteBuffer b, int len) {
        if (b.remaining() < len) return "";
        byte[] s = new byte[len];
        b.get(s);
        StringBuilder sb = new StringBuilder(len * 2);
        for (byte x : s) {
            sb.append(String.format("%02X", x & 0xFF));
        }
        return sb.toString();
    }

    private static String operationName(int opType) {
        switch (opType) {
            case 1:
                return "重启";
            case 2:
                return "静默";
            case 3:
                return "下控命令";
            case 4:
                return "低功耗待机模式";
            case 5:
                return "自检";
            case 6:
                return "校时请求";
            case 7:
                return "消音";
            case 8:
                return "查找";
            case 9:
                return "通配启动";
            case 10:
                return "联动启动";
            default:
                return "操作:" + opType;
        }
    }

    // ==================== 格式化工具 ====================

    /**
     * 格式化时间标签为可读字符串
     *
     * @param ts 6字节时间标签: sec(0), min(1), hour(2), day(3), month(4), year-2000(5)
     */
    static String formatTimestamp(byte[] ts) {
        if (ts == null || ts.length < 6) return "";
        int sec = ts[0] & 0xFF;
        int min = ts[1] & 0xFF;
        int hour = ts[2] & 0xFF;
        int day = ts[3] & 0xFF;
        int month = ts[4] & 0xFF;
        int year = 2000 + (ts[5] & 0xFF);
        return String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, min, sec);
    }

    public static String encodeHex(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    // ==================== 名称字典 ====================

    /**
     * 一级数据类型名称
     */
    public static String getDataTypeName(int dataType) {
        switch (dataType) {
            case DahuaProtocolConstants.DATA_TYPE_REGISTER:
                return "注册包";
            case DahuaProtocolConstants.DATA_TYPE_EVENT:
                return "事件";
            case DahuaProtocolConstants.DATA_TYPE_ANALOG:
                return "模拟量";
            case DahuaProtocolConstants.DATA_TYPE_PARAM_FIXED:
                return "参数(定长)";
            case DahuaProtocolConstants.DATA_TYPE_PARAM_VARIABLE:
                return "参数(不定长)";
            case DahuaProtocolConstants.DATA_TYPE_OPERATION:
                return "操作";
            case DahuaProtocolConstants.DATA_TYPE_UPGRADE:
                return "升级";
            case DahuaProtocolConstants.DATA_TYPE_CAPABILITY:
                return "能力";
            case DahuaProtocolConstants.DATA_TYPE_CONFIRM:
                return "确认(含参)";
            case DahuaProtocolConstants.DATA_TYPE_ENCRYPT:
                return "加密";
            default:
                return "未知类型:" + dataType;
        }
    }

    /**
     * 部件类型名称
     */
    // ==================== 独立帧解析（CMD=0x02 非0x87帧） ====================

    private static final Charset GB18030 = Charset.forName("GB18030");

    /**
     * 解析独立上报帧（CMD=0x02 下 typeFlag=1~8 的定长记录格式）
     * <p>
     * ADU 结构：typeFlag(1B) + count(1B) + records...
     * 与 TCP 侧 StandardHandlers 输出相同的 Map key，便于下游统一处理。
     *
     * @param adu 完整应用数据单元（含 typeFlag 和 count）
     * @return 解析结果列表；未知类型返回含 rawHex 的兜底结果
     */
    public static List<Map<String, Object>> parseSimpleFrame(byte[] adu) {
        if (adu == null || adu.length < 2) return Collections.emptyList();

        int typeFlag = adu[0] & 0xFF;
        int count = adu[1] & 0xFF;
        ByteBuffer buf = ByteBuffer.wrap(adu, 2, adu.length - 2).order(ByteOrder.LITTLE_ENDIAN);

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gbtType", typeFlag);
            switch (typeFlag) {
                case 1: // 系统状态 10B
                    if (buf.remaining() < 10) return list;
                    m.put("sys_systemType", buf.get() & 0xFF);
                    m.put("sys_systemAddr", buf.get() & 0xFF);
                    int s1 = buf.getShort() & 0xFFFF;
                    m.put("sys_status", s1);
                    m.put("sys_statusTime", readTimestamp6(buf));
                    putStatusBits(m, "sys_", s1, SYS_STATUS_KEYS);
                    break;
                case 2: // 部件状态 46B
                    if (buf.remaining() < 46) return list;
                    m.put("comp_systemType", buf.get() & 0xFF);
                    m.put("comp_systemAddr", buf.get() & 0xFF);
                    m.put("comp_type", buf.get() & 0xFF);
                    m.put("comp_addr", buf.getInt());
                    int s2 = buf.getShort() & 0xFFFF;
                    m.put("comp_status", s2);
                    m.put("comp_desc", readGB18030(buf, 31));
                    m.put("comp_statusTime", readTimestamp6(buf));
                    putStatusBits(m, "comp_", s2, COMP_STATUS_KEYS);
                    break;
                case 3: // 部件模拟量 16B
                    if (buf.remaining() < 16) return list;
                    m.put("analog_systemType", buf.get() & 0xFF);
                    m.put("analog_systemAddr", buf.get() & 0xFF);
                    m.put("analog_compType", buf.get() & 0xFF);
                    m.put("analog_compAddr", buf.getInt());
                    int aType = buf.get() & 0xFF;
                    short aRaw = buf.getShort();
                    m.put("analog_type", aType);
                    m.put("analog_rawValue", (int) aRaw);
                    m.put("analog_sampleTime", readTimestamp6(buf));
                    break;
                case 4: // 操作信息 10B
                    if (buf.remaining() < 10) return list;
                    m.put("oper_systemType", buf.get() & 0xFF);
                    m.put("oper_systemAddr", buf.get() & 0xFF);
                    m.put("oper_type", buf.get() & 0xFF);
                    m.put("oper_operatorNum", buf.get() & 0xFF);
                    m.put("oper_recordTime", readTimestamp6(buf));
                    break;
                case 5: // 软件版本 4B
                    if (buf.remaining() < 4) return list;
                    m.put("ver_systemType", buf.get() & 0xFF);
                    m.put("ver_systemAddr", buf.get() & 0xFF);
                    m.put("ver_major", buf.get() & 0xFF);
                    m.put("ver_minor", buf.get() & 0xFF);
                    break;
                case 6: // 系统配置 变长
                    if (buf.remaining() < 3) return list;
                    m.put("sysConf_systemType", buf.get() & 0xFF);
                    m.put("sysConf_systemAddr", buf.get() & 0xFF);
                    int descLen = buf.get() & 0xFF;
                    if (buf.remaining() < descLen) return list;
                    m.put("sysConf_desc", readGB18030(buf, descLen));
                    break;
                case 7: // 部件配置 38B
                    if (buf.remaining() < 38) return list;
                    m.put("compConf_systemType", buf.get() & 0xFF);
                    m.put("compConf_systemAddr", buf.get() & 0xFF);
                    m.put("compConf_compType", buf.get() & 0xFF);
                    m.put("compConf_compAddr", buf.getInt());
                    m.put("compConf_desc", readGB18030(buf, 31));
                    break;
                case 8: // 系统时间 8B
                    if (buf.remaining() < 8) return list;
                    m.put("sysTime_systemType", buf.get() & 0xFF);
                    m.put("sysTime_systemAddr", buf.get() & 0xFF);
                    m.put("sysTime_time", readTimestamp6(buf));
                    break;
                case 0x84: // 系统模拟量帧 12B
                    if (buf.remaining() < 12) return list;
                    m.put("sysAnalog_systemType", buf.get() & 0xFF);
                    m.put("sysAnalog_systemAddr", buf.get() & 0xFF);
                    m.put("sysAnalog_type", buf.getShort() & 0xFFFF);
                    m.put("sysAnalog_rawValue", buf.getShort() & 0xFFFF);
                    m.put("sysAnalog_sampleTime", readTimestamp6(buf));
                    break;
                default:
                    // 未知类型，返回 rawHex
                    m.put("rawHex", encodeHex(adu));
                    list.add(m);
                    return list;
            }
            list.add(m);
        }
        return list;
    }

    private static final String[] SYS_STATUS_KEYS = {
            "sys_normalMode", "sys_fireAlarm", "sys_fault", "sys_shielded",
            "sys_supervised", "sys_activated", "sys_feedback", "sys_delay",
            "sys_mainPowerFault", "sys_backupPowerFault", "sys_busFault",
            "sys_manualMode", "sys_configChanged"
    };
    private static final String[] COMP_STATUS_KEYS = {
            "comp_normalMode", "comp_fireAlarm", "comp_fault", "comp_shielded",
            "comp_supervised", "comp_activated", "comp_feedback", "comp_delay",
            "comp_powerFault"
    };

    private static void putStatusBits(Map<String, Object> m, String prefix, int status, String[] keys) {
        for (int b = 0; b < keys.length && b < 16; b++) {
            m.put(keys[b], (status & (1 << b)) != 0 ? 1 : 0);
        }
    }

    private static String readTimestamp6(ByteBuffer buf) {
        byte[] ts = new byte[6];
        buf.get(ts);
        return formatTimestamp(ts);
    }

    private static String readGB18030(ByteBuffer buf, int len) {
        byte[] bytes = new byte[len];
        buf.get(bytes);
        int end = len;
        for (int i = 0; i < len; i++) {
            if (bytes[i] == 0) { end = i; break; }
        }
        return new String(bytes, 0, end, GB18030).trim();
    }

    public static String getComponentTypeName(int componentType) {
        switch (componentType) {
            case 0: return "通用";
            case 1: return "火灾报警控制器";
            case 10: return "可燃气体探测器";
            case 11: return "点型可燃气体探测器";
            case 12: return "独立式可燃气体探测器";
            case 13: return "线型可燃气体探测器";
            case 16: return "电气火灾监控报警器";
            case 17: return "剩余电流式电气火灾监控探测器";
            case 18: return "测温式电气火灾监控探测器";
            case 21: return "探测回路";
            case 22: return "火灾显示盘";
            case 23: return "手动火灾报警按钮";
            case 24: return "消火栓按钮";
            case 25: return "火灾探测器";
            case 30: return "感温火灾探测器";
            case 31: return "点型感温火灾探测器";
            case 32: return "点型感温火灾探测器（S 型）";
            case 33: return "点型感温火灾探测器（R 型）";
            case 34: return "线型感温火灾探测器";
            case 35: return "线型感温火灾探测器（S 型）";
            case 36: return "线型感温火灾探测器（R 型）";
            case 37: return "光纤感温火灾探测器";
            case 40: return "感烟火灾探测器";
            case 41: return "点型离子感烟火灾探测器";
            case 42: return "点型光电感烟火灾探测器";
            case 43: return "线型光束感烟火灾探测器";
            case 44: return "吸气式感烟火灾探测器";
            case 50: return "复合式火灾探测器";
            case 51: return "复合式感烟感温火灾探测器";
            case 52: return "复合式感光感温火灾探测器";
            case 53: return "复合式感光感烟火灾探测器";
            case 61: return "紫外火焰探测器";
            case 62: return "红外火焰探测器";
            case 69: return "感光火灾探测器";
            case 74: return "气体探测器";
            case 78: return "图像摄像方式火灾探测器";
            case 79: return "感声火灾探测器";
            case 81: return "气体灭火控制器";
            case 82: return "消防电气控制装置";
            case 83: return "消防控制室图形显示装置";
            case 84: return "模块";
            case 85: return "输入模块";
            case 86: return "输出模块";
            case 87: return "输入/输出模块";
            case 88: return "中继模块";
            case 91: return "消防水泵";
            case 92: return "消防水箱";
            case 95: return "喷淋泵";
            case 96: return "水流指示器";
            case 97: return "信号阀";
            case 98: return "报警阀";
            case 99: return "压力开关";
            case 101: return "阀驱动装置";
            case 102: return "防火门";
            case 103: return "防火阀";
            case 104: return "通风空调";
            case 105: return "泡沫液泵";
            case 106: return "管网电磁阀";
            case 109: return "排烟机";
            case 110: return "送风机";
            case 111: return "防烟排烟风机";
            case 112: return "电梯";
            case 113: return "排烟防火阀";
            case 114: return "常闭送风口";
            case 115: return "排烟口";
            case 116: return "电控挡烟垂壁";
            case 117: return "防火卷帘控制器";
            case 118: return "防火门监控器";
            case 121: return "警报装置";
            case 127: return "单相市电探测器";
            case 128: return "压力传感器";
            case 129: return "电弧探测器";
            case 130: return "市电探测器";
            case 131: return "液位传感器";
            case 132: return "湿度传感器";
            case 133: return "温度传感器";
            case 134: return "NB 型感烟火灾探测器";
            case 135: return "电能探测模块";
            case 136: return "电流探测器";
            case 137: return "声光报警器";
            case 138: return "电压探测器";
            case 139: return "功率探测器";
            case 140: return "断路器";
            case 141: return "电能探测器（1P+N）";
            case 142: return "电能探测器（2P）";
            case 143: return "电能探测器（3P+N）";
            case 144: return "电能探测器（4P）";
            case 145: return "智能空开（1P）";
            case 146: return "智能空开（2P）";
            case 147: return "智能空开（2P+漏保）";
            case 148: return "智能空开（3P）";
            case 149: return "智能空开（4P）";
            case 150: return "智能空开（4P+漏保）";
            case 151: return "气体传感器";
            case 152: return "通讯模组";
            case 153: return "水浸传感器";
            case 154: return "隔离模块";
            case 155: return "可燃气体探测器(丙烷)";
            case 156: return "门磁设备";
            case 157: return "气体释放警报器";
            case 158: return "紧急启停按钮";
            case 159: return "电表";
            case 160: return "应急照明";
            case 161: return "照明配电";
            case 162: return "广播模块";
            default: return "部件类型:" + componentType;
        }
    }
}
