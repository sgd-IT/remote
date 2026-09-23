package com.enjoyiot.eiot.component.ff.parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Handler 共享解析工具方法
 */
public final class HandlerUtils {

    private HandlerUtils() {}

    public static final Charset GB18030 = Charset.forName("GB18030");

    /**
     * 读取6字节时间标签并转为可读字符串
     */
    public static String readTimestampStr(ByteBuffer buf) {
        int sec = buf.get() & 0xFF;
        int min = buf.get() & 0xFF;
        int hour = buf.get() & 0xFF;
        int day = buf.get() & 0xFF;
        int month = buf.get() & 0xFF;
        int year = 2000 + (buf.get() & 0xFF);
        return String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, min, sec);
    }

    /**
     * 读取4字节整数（小端序）
     */
    public static int readIntLE(ByteBuffer buf) {
        return buf.getInt();
    }

    /**
     * 读取指定长度的GB18030编码字符串，去除尾部零字节
     */
    public static String readGb18030String(ByteBuffer buf, int length) {
        byte[] bytes = new byte[length];
        buf.get(bytes);
        int end = length;
        for (int i = 0; i < length; i++) {
            if (bytes[i] == 0) {
                end = i;
                break;
            }
        }
        return new String(bytes, 0, end, GB18030).trim();
    }
}
