package com.enjoyiot.eiot.component.gbttcp.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GB/T 26875.3-2011 标准数据包
 *
 * 包结构:
 *   @@(2B) | 业务流水号(2B,LE) | 协议版本(2B) | 时间标签(6B)
 *   | 源地址(6B,LE) | 目的地址(6B,LE) | 数据长度(2B,LE)
 *   | 命令字节(1B) | 应用数据(≤1024B) | 校验和(1B) | ##(2B)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GbtDataPackage {

    /** 业务流水号 (0~65535) */
    private int serialNumber;

    /** 协议主版本号 */
    @Builder.Default
    private byte majorVersion = GbtConstants.PROTOCOL_MAJOR_VERSION;

    /** 协议用户版本号 */
    @Builder.Default
    private byte userVersion = GbtConstants.PROTOCOL_USER_VERSION;

    /** 时间标签（6字节：秒、分、时、日、月、年-2000） */
    private byte[] timestamp;

    /** 源地址（6字节，低字节在前） */
    private byte[] sourceAddr;

    /** 目的地址（6字节，低字节在前） */
    private byte[] destAddr;

    /** 命令字节 */
    private byte command;

    /** 应用数据单元 */
    private byte[] applicationData;

    /**
     * 获取源地址的十六进制字符串表示
     */
    public String getSourceAddrHex() {
        return bytesToHex(sourceAddr);
    }

    /**
     * 获取目的地址的十六进制字符串表示
     */
    public String getDestAddrHex() {
        return bytesToHex(destAddr);
    }

    /**
     * 字节数组 → 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * 十六进制字符串 → 6字节地址数组 (不足12位前补0)
     */
    public static byte[] hexToAddr(String hex) {
        if (hex == null || hex.isEmpty()) return new byte[GbtConstants.ADDRESS_LENGTH];
        hex = hex.replaceAll("\\s+", "");
        while (hex.length() < GbtConstants.ADDRESS_LENGTH * 2) {
            hex = "0" + hex;
        }
        byte[] bytes = new byte[GbtConstants.ADDRESS_LENGTH];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    /**
     * 解析时间标签为可读字符串 yyyy-MM-dd HH:mm:ss
     */
    public String getTimestampStr() {
        if (timestamp == null || timestamp.length < 6) return "";
        int sec = timestamp[0] & 0xFF;
        int min = timestamp[1] & 0xFF;
        int hour = timestamp[2] & 0xFF;
        int day = timestamp[3] & 0xFF;
        int month = timestamp[4] & 0xFF;
        int year = 2000 + (timestamp[5] & 0xFF);
        return String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, min, sec);
    }
}
