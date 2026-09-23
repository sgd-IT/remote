package com.enjoyiot.eiot.component.tcp.ywn.parser;

import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

/**
 * YWN控制器监控TCP协议V27 — 帧编码器
 *
 * 将 DataYwnPackage 编码为 Vertx Buffer:
 * - 写入命令码(1B) + 设备编号(2B LE) + 载荷
 * - 控制类命令帧长固定15字节，不足补零
 */
@Slf4j
public class DataYwnEncoder {

    /**
     * 编码 DataYwnPackage 为完整帧 Buffer
     */
    public static Buffer encode(DataYwnPackage data) {
        byte[] frameBytes = data.toBytes();
        return Buffer.buffer(frameBytes);
    }

    /**
     * 快速构建并编码一个下行控制帧 (15字节)
     *
     * @param cmdCode      命令码
     * @param deviceNumber 设备编号
     * @param machineNo    机号
     * @param loopNo       回路号 (手自动转换时为模式: 2=手动, 3=自动)
     * @param addressNo    地址号
     * @return 编码后的 Buffer
     */
    public static Buffer encodeControl(int cmdCode, int deviceNumber, int machineNo, int loopNo, int addressNo) {
        byte[] payload = new byte[12]; // 15 - 3(命令码+设备编号)
        payload[0] = (byte) machineNo;
        payload[1] = (byte) loopNo;
        payload[2] = (byte) addressNo;

        DataYwnPackage pkg = DataYwnPackage.builder()
                .command(cmdCode)
                .deviceNumber(deviceNumber)
                .payload(payload)
                .build();
        return encode(pkg);
    }

    /**
     * 快速构建 ACK 应答帧 (3字节)
     */
    public static Buffer encodeAck(int deviceNumber) {
        DataYwnPackage pkg = DataYwnPackage.builder()
                .command(DataYwnPackage.CMD_ACK)
                .deviceNumber(deviceNumber)
                .payload(new byte[0])
                .build();
        return encode(pkg);
    }

    /**
     * 快速构建心跳应答帧 (3字节)
     */
    public static Buffer encodeHeartbeatAck(int deviceNumber) {
        DataYwnPackage pkg = DataYwnPackage.builder()
                .command(DataYwnPackage.CMD_HEARTBEAT_ACK)
                .deviceNumber(deviceNumber)
                .payload(new byte[0])
                .build();
        return encode(pkg);
    }
}
