package com.enjoyiot.module.eiot.controller.admin.aep.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * AEP 平台 HTTP 推送消息 DTO
 * 扁平结构，字段对齐 AEP 推送 JSON
 * 仅保留各消息类型的必填字段，非必填字段已注释
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AepPushMessage {

    // ---- 通用字段 ----
    private String tenantId;       // 所有类型必填
    private String productId;      // 所有类型必填
    private String deviceId;       // 除 addDeleteDevice 外必填
    private String messageType;    // 所有类型必填
    private Long timestamp;        // dataReport/eventReport/deviceOnlineOfflineReport 必填
    private String protocol;       // dataReport/eventReport/deviceOnlineOfflineReport 必填

    // ---- dataReport 字段 ----
    private Object payload;        // 必填
    private String topic;          // 必填
    // private String IMEI;        // 非必填
    // private String IMSI;        // 非必填
    // private String deviceType;  // 非必填
    // private String assocAssetId;// 非必填
    // private Integer upPacketSN; // 非必填（Tlink协议特有，其他协议默认-1）
    // private Integer upDataSN;   // 非必填（Tlink协议特有，其他协议默认-1）
    // private Object serviceId;   // 非必填

    // ---- deviceOnlineOfflineReport / eventReport 字段 ----
    private Integer eventType;     // eventReport/deviceOnlineOfflineReport 必填

    // ---- eventReport 字段 ----
    private Object eventContent;   // 必填
    private String deviceSn;       // 必填

    // ---- commandResponse 字段 ----
    private Object taskId;         // 必填
    private CommandResult result;  // 必填

    // ---- deviceOnlineOfflineReport 附加字段 ----
    private String iccid;          // 必填
    // private String ipv4Address; // 非必填
    // private String ipv6Address; // 非必填

    // ---- addDeleteDevice 字段 ----
    private String operation;      // 必填
    private List<DeviceItem> deviceList; // 必填

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandResult {
        private String resultCode;
        private Object resultDetail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceItem {
        private String deviceId;
        private String deviceName;
    }
}
