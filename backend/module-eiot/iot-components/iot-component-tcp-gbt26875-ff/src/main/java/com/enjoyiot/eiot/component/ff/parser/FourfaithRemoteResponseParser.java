package com.enjoyiot.eiot.component.ff.parser;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TLD-133 远程应答解析器
 *
 * 解析 Type 0x85 远程控制应答的应用数据单元：
 *   系统类型标志(1) + 系统地址(1) + 部件类型(1) + 部件地址(4,LE)
 *   + 执行结果(1) + 应答内容长度(2,LE) + 应答内容(L, GB18030)
 */
@Slf4j
public class FourfaithRemoteResponseParser {

    private static final Charset GB18030 = Charset.forName("GB18030");

    /**
     * 解析 TLD-133 远程应答数据
     *
     * @param appData 应用数据单元字节数组（含类型标志和计数）
     * @return 解析后的字段映射；无法解析时返回空 map
     */
    public static Map<String, Object> parse(byte[] appData) {
        if (appData == null || appData.length < 10) {
            log.warn("TLD-133 数据长度不足: {}", appData != null ? appData.length : 0);
            return Collections.emptyMap();
        }

        ByteBuffer buf = ByteBuffer.wrap(appData).order(ByteOrder.LITTLE_ENDIAN);
        Map<String, Object> result = new LinkedHashMap<>();

        // 跳过类型标志(1)和信息体数目(1)
        buf.get();
        buf.get();

        result.put("resp_sys_type", buf.get() & 0xFF);
        result.put("resp_sys_addr", buf.get() & 0xFF);
        result.put("resp_comp_type", buf.get() & 0xFF);
        result.put("resp_comp_addr", buf.getInt());

        int status = buf.get() & 0xFF;
        boolean success = (status & FourfaithGbtConstants.REMOTE_RESP_SUCCESS_MASK) != 0;
        boolean executing = (status & FourfaithGbtConstants.REMOTE_RESP_EXECUTING_MASK) != 0;
        int timeoutMin = status & FourfaithGbtConstants.REMOTE_RESP_TIMEOUT_MASK;

        result.put("resp_success", success ? 1 : 0);
        result.put("resp_executing", executing ? 1 : 0);
        result.put("resp_timeout_min", timeoutMin);
        result.put("resp_status_code", status);

        int respLen = buf.getShort() & 0xFFFF;
        if (respLen > 0 && buf.remaining() >= respLen) {
            byte[] respBytes = new byte[respLen];
            buf.get(respBytes);
            result.put("resp_content", new String(respBytes, 0, respLen, GB18030).trim());
        }

        return result;
    }
}
