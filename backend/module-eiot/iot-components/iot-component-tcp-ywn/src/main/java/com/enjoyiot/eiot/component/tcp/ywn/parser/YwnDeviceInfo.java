package com.enjoyiot.eiot.component.tcp.ywn.parser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * 控制器监控TCP协议V27 — 设备信息模型
 *
 * 设备信息上传帧(0x81)载荷: 设备编号(2B) + ICCID(20B) + IMEI(15B) + 硬件版本(4B) + 软件版本(4B) = 45字节
 * 整帧: 命令码(1B) + 设备编号(2B) + ICCID(20B) + IMEI(15B) + 硬件版本(4B) + 软件版本(4B) = 46字节
 *
 * 说明：开机上报一次，每隔24小时上报一次
 */
public class YwnDeviceInfo {

    private int deviceNumber;       // 设备编号 (1-65535)
    private String iccid;           // ICCID (20字节, 以太网发'0')
    private String imei;            // IMEI (15字节, 以太网发'0')
    private String hardwareVersion; // 硬件版本 (4字节, 如 V1.0)
    private String softwareVersion; // 软件版本 (4字节, 如 V1.2)

    /** 从整帧字节数组解析(46字节, 含命令码) */
    public static YwnDeviceInfo fromFrameBytes(byte[] frame) {
        if (frame == null || frame.length < 46) {
            throw new IllegalArgumentException("设备信息帧至少需要46字节");
        }
        ByteBuffer buf = ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN);

        int cmd = buf.get() & 0xFF;
        if (cmd != 0x81) {
            throw new IllegalArgumentException("命令码应为0x81, 实际: 0x" + Integer.toHexString(cmd));
        }

        YwnDeviceInfo info = new YwnDeviceInfo();
        info.deviceNumber = buf.getShort() & 0xFFFF;

        byte[] iccidBytes = new byte[20];
        buf.get(iccidBytes);
        info.iccid = trimNull(iccidBytes);

        byte[] imeiBytes = new byte[15];
        buf.get(imeiBytes);
        info.imei = trimNull(imeiBytes);

        byte[] hwBytes = new byte[4];
        buf.get(hwBytes);
        info.hardwareVersion = trimNull(hwBytes);

        byte[] swBytes = new byte[4];
        buf.get(swBytes);
        info.softwareVersion = trimNull(swBytes);

        return info;
    }

    /** 从载荷字节数组解析(43字节, 不含命令码和设备编号，由 DataYwnPackage 的 payload 传入) */
    public static YwnDeviceInfo fromPayload(int deviceNumber, byte[] payload) {
        if (payload == null || payload.length < 43) {
            throw new IllegalArgumentException("设备信息载荷至少需要43字节");
        }
        ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        YwnDeviceInfo info = new YwnDeviceInfo();
        info.deviceNumber = deviceNumber;

        byte[] iccidBytes = new byte[20];
        buf.get(iccidBytes);
        info.iccid = trimNull(iccidBytes);

        byte[] imeiBytes = new byte[15];
        buf.get(imeiBytes);
        info.imei = trimNull(imeiBytes);

        byte[] hwBytes = new byte[4];
        buf.get(hwBytes);
        info.hardwareVersion = trimNull(hwBytes);

        byte[] swBytes = new byte[4];
        buf.get(swBytes);
        info.softwareVersion = trimNull(swBytes);

        return info;
    }

    /** 序列化为整帧字节数组(46字节) */
    public byte[] toFrameBytes() {
        ByteBuffer buf = ByteBuffer.allocate(46).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x81);
        buf.putShort((short) deviceNumber);
        buf.put(padString(iccid, 20));
        buf.put(padString(imei, 15));
        buf.put(padString(hardwareVersion, 4));
        buf.put(padString(softwareVersion, 4));
        return buf.array();
    }

    private static String trimNull(byte[] bytes) {
        int end = bytes.length;
        for (int i = bytes.length - 1; i >= 0; i--) {
            if (bytes[i] != 0) { end = i + 1; break; }
            if (i == 0) return "";
        }
        return new String(bytes, 0, end, StandardCharsets.US_ASCII).trim();
    }

    private static byte[] padString(String str, int len) {
        byte[] result = new byte[len];
        if (str != null) {
            byte[] src = str.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(src, 0, result, 0, Math.min(src.length, len));
        }
        return result;
    }

    // ========== Getters & Setters ==========

    public int getDeviceNumber() { return deviceNumber; }
    public void setDeviceNumber(int deviceNumber) { this.deviceNumber = deviceNumber; }
    public String getIccid() { return iccid; }
    public void setIccid(String iccid) { this.iccid = iccid; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public String getHardwareVersion() { return hardwareVersion; }
    public void setHardwareVersion(String hardwareVersion) { this.hardwareVersion = hardwareVersion; }
    public String getSoftwareVersion() { return softwareVersion; }
    public void setSoftwareVersion(String softwareVersion) { this.softwareVersion = softwareVersion; }

    @Override
    public String toString() {
        return String.format(
                "DeviceInfo{no=%d, iccid=%s, imei=%s, hw=%s, sw=%s}",
                deviceNumber, iccid, imei, hardwareVersion, softwareVersion
        );
    }
}
