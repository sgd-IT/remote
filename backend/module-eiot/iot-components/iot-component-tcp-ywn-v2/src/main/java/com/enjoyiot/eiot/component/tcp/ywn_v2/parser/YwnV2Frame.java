package com.enjoyiot.eiot.component.tcp.ywn_v2.parser;

import cn.hutool.core.util.HexUtil;
import com.enjoyiot.eiot.component.tcp.ywn_v2.protobuf.YwnV2Protocol;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * YWN V2 协议 — 帧数据模型
 *
 * 帧格式: 数据长度(2B BE) + 消息类型(2B BE) + 序列号(4B BE) + Protobuf载荷
 * 所有字段均使用大端序(BE)
 *
 * 按协议文档定义: 数据长度 = 整个数据包的长度 - 协议头(8B) = Protobuf 载荷长度
 * 整帧长度 = 8 + 数据长度
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Slf4j
public class YwnV2Frame {

    /** 协议头固定长度: 数据长度(2) + 消息类型(2) + 序列号(4) = 8 */
    public static final int HEADER_LENGTH = 8;

    /** 消息类型 */
    private MessageType messageType;

    /** 序列号 (唯一标识消息，用于请求-响应匹配) */
    private int sequenceNo;

    /** Protobuf 载荷字节数组 */
    private byte[] protobufData;

    // ========== 便捷方法 ==========

    /** 数据长度(载荷长度): Protobuf 载荷字节数 */
    public int getDataLength() {
        return protobufData != null ? protobufData.length : 0;
    }

    /** 整帧长度: 协议头(8B) + 载荷 */
    public int getFrameLength() {
        return HEADER_LENGTH + getDataLength();
    }

    /** 消息类型码 */
    public int getMessageTypeCode() {
        return messageType != null ? messageType.getCode() : 0;
    }

    // ========== Protobuf 解析 ==========

    /** 解析为通用确认消息 */
    public YwnV2Protocol.Message parseAck() {
        if (protobufData == null || protobufData.length == 0) return null;
        try {
            return YwnV2Protocol.Message.parseFrom(protobufData);
        } catch (InvalidProtocolBufferException e) {
            log.error("[YWN-V2] 解析确认消息失败", e);
            return null;
        }
    }

    /** 解析为注册请求 */
    public YwnV2Protocol.RegisterReq parseRegisterReq() {
        if (protobufData == null || protobufData.length == 0) return null;
        try {
            return YwnV2Protocol.RegisterReq.parseFrom(protobufData);
        } catch (InvalidProtocolBufferException e) {
            log.error("[YWN-V2] 解析注册请求失败", e);
            return null;
        }
    }

    /** 解析为事件上报 */
    public YwnV2Protocol.EventReq parseEventReq() {
        if (protobufData == null || protobufData.length == 0) return null;
        try {
            return YwnV2Protocol.EventReq.parseFrom(protobufData);
        } catch (InvalidProtocolBufferException e) {
            log.error("[YWN-V2] 解析事件请求失败", e);
            return null;
        }
    }

    /** 解析为控制下发 */
    public YwnV2Protocol.ControlReq parseControlReq() {
        if (protobufData == null || protobufData.length == 0) return null;
        try {
            return YwnV2Protocol.ControlReq.parseFrom(protobufData);
        } catch (InvalidProtocolBufferException e) {
            log.error("[YWN-V2] 解析控制请求失败", e);
            return null;
        }
    }

    // ========== 序列化 ==========

    /** 编码为字节数组 */
    public byte[] toBytes() {
        int dataLen = getDataLength();
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH + dataLen).order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) dataLen);
        buf.putShort((short) getMessageTypeCode());
        buf.putInt(sequenceNo);
        if (protobufData != null && protobufData.length > 0) {
            buf.put(protobufData);
        }
        return buf.array();
    }

    /** 转为hex字符串(用于日志) */
    public String toHexString() {
        return HexUtil.encodeHexStr(toBytes());
    }

    @Override
    public String toString() {
        return String.format("YwnV2Frame{type=%s, seq=%d, dataLen=%d}",
                messageType != null ? messageType : "UNKNOWN",
                sequenceNo, protobufData != null ? protobufData.length : 0);
    }
}
