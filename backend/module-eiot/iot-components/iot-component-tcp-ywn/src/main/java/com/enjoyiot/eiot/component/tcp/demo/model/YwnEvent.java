package com.enjoyiot.eiot.component.tcp.demo.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 控制器监控TCP协议V27 — 事件数据模型 (64字节)
 *
 * 帧内偏移(事件上传帧 0x80 的载荷部分，紧跟命令码+设备编号之后):
 * | 偏移 | 字段           | 字节数 | 说明                    |
 * |------|---------------|--------|------------------------|
 * | 0    | 事件类型码     | 1      |                        |
 * | 1    | 产品类型       | 1      | 0值不用显示             |
 * | 2    | 通道类型       | 1      | 0值不用显示             |
 * | 3    | 备用           | 1      | 0值不用显示             |
 * | 4-5  | 事件代码       | 2      | LE, 0值不用显示         |
 * | 6-7  | 设备类型       | 2      | LE, 0值不用显示         |
 * | 8    | 机号           | 1      | 0值不用显示             |
 * | 9    | 回路           | 1      | 0值不用显示             |
 * | 10   | 地址号         | 1      | 0值不用显示             |
 * | 11   | 通道号         | 1      | 0值不用显示             |
 * | 12-15| 报警值(浮点)   | 4      | IEEE754, -1不显示       |
 * | 16-17| 报警值(整型)   | 2      | LE, 数据×100, -1不显示  |
 * | 18-23| 时间           | 6      | 年月日时分秒, 0值不显示  |
 * | 24-63| 说明文字       | 40     | GBK, 可配20个汉字       |
 */
public class YwnEvent {

    private static final int EVENT_DATA_LENGTH = 64;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int eventTypeCode;      // 偏移0
    private int productType;        // 偏移1
    private int channelType;        // 偏移2
    private int reserved;           // 偏移3
    private int eventCode;          // 偏移4-5
    private int deviceType;         // 偏移6-7
    private int machineNo;          // 偏移8
    private int loopNo;             // 偏移9
    private int addressNo;          // 偏移10
    private int channelNo;          // 偏移11
    private float alarmValueFloat;  // 偏移12-15
    private int alarmValueInt;      // 偏移16-17
    private LocalDateTime eventTime;// 偏移18-23
    private String description;     // 偏移24-63

    /** 从64字节数组解析事件 */
    public static YwnEvent fromBytes(byte[] data) {
        if (data == null || data.length != EVENT_DATA_LENGTH) {
            throw new IllegalArgumentException("事件数据必须为64字节, 实际: " + (data == null ? 0 : data.length));
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        YwnEvent event = new YwnEvent();

        event.eventTypeCode = buf.get() & 0xFF;         // 0
        event.productType = buf.get() & 0xFF;            // 1
        event.channelType = buf.get() & 0xFF;            // 2
        event.reserved = buf.get() & 0xFF;               // 3
        event.eventCode = buf.getShort() & 0xFFFF;       // 4-5
        event.deviceType = buf.getShort() & 0xFFFF;      // 6-7
        event.machineNo = buf.get() & 0xFF;              // 8
        event.loopNo = buf.get() & 0xFF;                 // 9
        event.addressNo = buf.get() & 0xFF;              // 10
        event.channelNo = buf.get() & 0xFF;              // 11
        event.alarmValueFloat = buf.getFloat();           // 12-15
        event.alarmValueInt = buf.getShort();             // 16-17

        // 18-23: 年月日时分秒 (每个字段1字节, 年=2000+byte)
        int year = (buf.get() & 0xFF) + 2000;
        int month = buf.get() & 0xFF;
        int day = buf.get() & 0xFF;
        int hour = buf.get() & 0xFF;
        int minute = buf.get() & 0xFF;
        int second = buf.get() & 0xFF;
        if (year >= 2000 && month > 0 && day > 0) {
            event.eventTime = LocalDateTime.of(year, month, day, hour, minute, second);
        }

        // 24-63: 说明文字 (GBK编码, 40字节)
        byte[] descBytes = new byte[40];
        buf.get(descBytes);
        event.description = decodeGbkTrim(descBytes);

        return event;
    }

    /** 序列化为64字节数组 */
    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(EVENT_DATA_LENGTH).order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) eventTypeCode);
        buf.put((byte) productType);
        buf.put((byte) channelType);
        buf.put((byte) reserved);
        buf.putShort((short) eventCode);
        buf.putShort((short) deviceType);
        buf.put((byte) machineNo);
        buf.put((byte) loopNo);
        buf.put((byte) addressNo);
        buf.put((byte) channelNo);
        buf.putFloat(alarmValueFloat);
        buf.putShort((short) alarmValueInt);

        if (eventTime != null) {
            buf.put((byte) (eventTime.getYear() - 2000));
            buf.put((byte) eventTime.getMonthValue());
            buf.put((byte) eventTime.getDayOfMonth());
            buf.put((byte) eventTime.getHour());
            buf.put((byte) eventTime.getMinute());
            buf.put((byte) eventTime.getSecond());
        } else {
            buf.put(new byte[6]);
        }

        byte[] descBytes = encodeGbkPad(description, 40);
        buf.put(descBytes);

        return buf.array();
    }

    /** GBK解码并去除尾部零 */
    private static String decodeGbkTrim(byte[] bytes) {
        int end = bytes.length;
        for (int i = bytes.length - 1; i >= 0; i--) {
            if (bytes[i] != 0) { end = i + 1; break; }
            if (i == 0) return "";
        }
        try {
            return new String(bytes, 0, end, "GBK").trim();
        } catch (Exception e) {
            return new String(bytes, 0, end, StandardCharsets.ISO_8859_1).trim();
        }
    }

    /** GBK编码并补零到指定长度 */
    private static byte[] encodeGbkPad(String str, int len) {
        byte[] result = new byte[len];
        if (str == null || str.isEmpty()) return result;
        try {
            byte[] src = str.getBytes("GBK");
            System.arraycopy(src, 0, result, 0, Math.min(src.length, len));
        } catch (Exception e) {
            byte[] src = str.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(src, 0, result, 0, Math.min(src.length, len));
        }
        return result;
    }

    // ========== 事件分类判断 ==========

    public boolean isFireAlarm() {
        YwnEventCode ec = YwnEventCode.fromCode(eventCode);
        return ec != null && ec.isFireAlarm();
    }

    public boolean isFault() {
        YwnEventCode ec = YwnEventCode.fromCode(eventCode);
        return ec != null && ec.isFault();
    }

    public boolean isRecovery() {
        YwnEventCode ec = YwnEventCode.fromCode(eventCode);
        return ec != null && ec.isRecovery();
    }

    public String getEventCodeName() {
        YwnEventCode ec = YwnEventCode.fromCode(eventCode);
        return ec != null ? ec.getDescription() : String.format("未知(0x%02X)", eventCode);
    }

    // ========== Getters ==========

    public int getEventTypeCode() { return eventTypeCode; }
    public int getProductType() { return productType; }
    public int getChannelType() { return channelType; }
    public int getEventCode() { return eventCode; }
    public int getDeviceType() { return deviceType; }
    public int getMachineNo() { return machineNo; }
    public int getLoopNo() { return loopNo; }
    public int getAddressNo() { return addressNo; }
    public int getChannelNo() { return channelNo; }
    public float getAlarmValueFloat() { return alarmValueFloat; }
    public int getAlarmValueInt() { return alarmValueInt; }
    public LocalDateTime getEventTime() { return eventTime; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format(
                "Event{code=0x%02X(%s), machine=%d, loop=%d, addr=%d, time=%s, desc=%s}",
                eventCode, getEventCodeName(), machineNo, loopNo, addressNo,
                eventTime != null ? eventTime.format(TIME_FMT) : "N/A",
                description != null ? description : ""
        );
    }
}
