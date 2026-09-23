package com.enjoyiot.eiot.component.ff.parser.handler;

import cn.hutool.json.JSONUtil;
import com.enjoyiot.eiot.component.ff.parser.GbtConstants;
import com.enjoyiot.eiot.component.ff.parser.HandlerUtils;
import com.enjoyiot.eiot.component.ff.parser.TypeHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 四信私有类型处理器（TLD 9: PRT打印信息, TLD 10: CRT图形显示信息）
 */
@Slf4j
public class PrivateHandlers {

    private PrivateHandlers() {}

    // ==================== TLD 9: PRT 打印信息 ====================
    public static class PrtHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_PRT; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 8; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("prt_channel", buf.get() & 0xFF);
                m.put("prt_host_no", buf.get() & 0xFF);
                buf.position(buf.position() + 2); // 跳过预留 2 字节 (0x3333)
                m.put("prt_alarm_type", buf.get() & 0xFF);

                int nameLen = buf.get() & 0xFF;
                if (buf.remaining() < nameLen) break;
                m.put("prt_device_name", HandlerUtils.readGb18030String(buf, nameLen));

                int locLen = buf.get() & 0xFF;
                if (buf.remaining() < locLen) break;
                m.put("prt_device_location", HandlerUtils.readGb18030String(buf, locLen));

                int infoLen = buf.get() & 0xFF;
                if (buf.remaining() < infoLen) break;
                m.put("prt_alarm_info", HandlerUtils.readGb18030String(buf, infoLen));

                m.put("prt_time", HandlerUtils.readTimestampStr(buf));
                list.add(m);
            }
            return list;
        }
    }

    // ==================== TLD 10: CRT 图形显示信息 ====================
    public static class CrtHandler implements TypeHandler {
        @Override
        public int getTypeFlag() { return GbtConstants.TYPE_UP_CRT; }

        @Override
        public List<Map<String, Object>> parse(ByteBuffer buf, int count, int typeFlag) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < count && buf.remaining() >= 5; i++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gbtType", typeFlag);
                m.put("crt_channel", buf.get() & 0xFF);
                m.put("crt_host_no", buf.get() & 0xFF);
                buf.get(); // 跳过预留 1 字节 (0x33)
                int fieldCount = buf.get() & 0xFF;
                m.put("crt_alarm_type", buf.get() & 0xFF);

                List<Map<String, Object>> eventFields = new ArrayList<>();
                for (int j = 0; j < fieldCount; j++) {
                    int fieldType = buf.get() & 0xFF;
                    Map<String, Object> field = new LinkedHashMap<>();
                    field.put("seq", j + 1);
                    if (fieldType == 0) {
                        field.put("type", 0);
                        field.put("value", null);
                    } else if (fieldType == 1) {
                        int len = buf.get() & 0xFF;
                        if (buf.remaining() < len) break;
                        field.put("type", 1);
                        field.put("value", HandlerUtils.readGb18030String(buf, len));
                    } else if (fieldType == 2) {
                        field.put("type", 2);
                        field.put("value", buf.getInt());
                    }
                    eventFields.add(field);
                }
                m.put("crt_event_json", JSONUtil.toJsonStr(eventFields));
                m.put("crt_time", HandlerUtils.readTimestampStr(buf));
                list.add(m);
            }
            return list;
        }
    }
}
