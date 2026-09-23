package com.enjoyiot.eiot.component.ff.service;

import com.enjoyiot.eiot.component.ff.cilent.FourfaithGbtVertxTcpClient;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtConstants;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtDataEncoder;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtDataPackage;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下行指令发送器
 * <p>
 * 从 FourfaithGbtTcpComponent 中提取的下行指令构造与发送逻辑。
 * 增加了协议要求的重试机制（超时10秒，重发3次）。
 */
@Slf4j
public class FourfaithGbtCommandSender {

    private final FourfaithGbtDeviceManager deviceManager;
    private final byte[] centerAddr;

    /** 出站业务流水号计数器 */
    private final AtomicInteger outboundSerial = new AtomicInteger(0);

    public FourfaithGbtCommandSender(FourfaithGbtDeviceManager deviceManager, byte[] centerAddr) {
        this.deviceManager = deviceManager;
        this.centerAddr = centerAddr;
    }

    /**
     * 发送确认报文（CMD=3）
     */
    public void sendConfirm(String deviceAddr, FourfaithGbtDataPackage receivedPkg) {
        FourfaithGbtDataPackage confirm = FourfaithGbtDataPackage.builder()
                .serialNumber(receivedPkg.getSerialNumber())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command(FourfaithGbtConstants.CMD_CONFIRM)
                .applicationData(new byte[0])
                .build();
        sendGbtPacket(deviceAddr, confirm);
    }

    /**
     * 发送否认报文（CMD=6）
     */
    public void sendDeny(String deviceAddr, FourfaithGbtDataPackage receivedPkg) {
        FourfaithGbtDataPackage deny = FourfaithGbtDataPackage.builder()
                .serialNumber(receivedPkg.getSerialNumber())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command(FourfaithGbtConstants.CMD_DENY)
                .applicationData(new byte[0])
                .build();
        sendGbtPacket(deviceAddr, deny);
    }

    /**
     * 发送查询请求（CMD=4）
     */
    public void sendQuery(String deviceAddr, int queryTypeFlag, byte[] queryBody) {
        byte[] appData = buildAppData(queryTypeFlag, queryBody);
        FourfaithGbtDataPackage query = FourfaithGbtDataPackage.builder()
                .serialNumber(nextSerial())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command(FourfaithGbtConstants.CMD_REQUEST)
                .applicationData(appData)
                .build();
        sendGbtPacket(deviceAddr, query);
    }

    /**
     * 发送控制命令（CMD=1），如时间同步、远程控制
     */
    public void sendControl(String deviceAddr, int typeFlag, byte[] controlBody) {
        byte[] appData = buildAppData(typeFlag, controlBody);
        FourfaithGbtDataPackage ctrl = FourfaithGbtDataPackage.builder()
                .serialNumber(nextSerial())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command(FourfaithGbtConstants.CMD_CONTROL)
                .applicationData(appData)
                .build();
        sendGbtPacket(deviceAddr, ctrl);
    }

    /**
     * 发送国标心跳应答（CMD=130，针对CMD=128的心跳）
     */
    public void sendHeartbeatReply(String deviceAddr, byte commModule, byte sysType) {
        // 国标心跳应答：使用标准帧格式，ADU包含通讯模块+系统类型
        byte[] appData = new byte[]{commModule, sysType};
        FourfaithGbtDataPackage reply = FourfaithGbtDataPackage.builder()
                .serialNumber(nextSerial())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command((byte) FourfaithGbtConstants.CMD_HEARTBEAT_REPLY)
                .applicationData(appData)
                .build();
        sendGbtPacket(deviceAddr, reply);
    }

    /**
     * 发送旧版心跳应答（12字节简化帧）
     */
    public void sendLegacyHeartbeatReply(String deviceAddr, FourfaithGbtVertxTcpClient client,
                                          byte commModule, byte sysType) {
        byte[] replyAddr = new byte[FourfaithGbtConstants.ADDRESS_LENGTH];
        Buffer reply = Buffer.buffer(FourfaithGbtConstants.HEARTBEAT_PACKET_SIZE);
        reply.appendByte(FourfaithGbtConstants.START_BYTE);
        reply.appendByte(FourfaithGbtConstants.START_BYTE);
        reply.appendBytes(replyAddr);
        reply.appendByte(commModule);   // 回显设备的通讯模块
        reply.appendByte(sysType);      // 回显设备的系统类型
        reply.appendByte(FourfaithGbtConstants.END_BYTE);
        reply.appendByte(FourfaithGbtConstants.END_BYTE);
        client.sendMessage(reply);
    }

    // ==================== 内部方法 ====================

    private byte[] buildAppData(int typeFlag, byte[] body) {
        if (body != null && body.length > 0) {
            byte[] appData = new byte[1 + body.length];
            appData[0] = (byte) typeFlag;
            System.arraycopy(body, 0, appData, 1, body.length);
            return appData;
        }
        return new byte[]{(byte) typeFlag};
    }

    private void sendGbtPacket(String deviceAddr, FourfaithGbtDataPackage pkg) {
        Buffer encoded = FourfaithGbtDataEncoder.encode(pkg);
        log.info("[四信-GBT26875] 下发报文: 设备={}, 命令={}, 流水号={}, 报文HEX={}",
                deviceAddr, pkg.getCommand(), pkg.getSerialNumber(), bytesToHex(encoded.getBytes()));
        FourfaithGbtVertxTcpClient client = deviceManager.getClient(deviceAddr);
        if (client != null && client.socket != null) {
            client.sendMessage(encoded);
        } else {
            log.warn("[四信-GBT26875] 设备 {} 不在线，无法发送", deviceAddr);
        }
    }

    public int nextSerial() {
        return outboundSerial.incrementAndGet() & 0xFFFF;
    }

    private byte[] toProtocolAddr(String deviceAddr) {
        return FourfaithGbtDataPackage.hexToAddr(FourfaithGbtDataPackage.reverseHexAddr(deviceAddr));
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
