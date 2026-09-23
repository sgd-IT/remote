package com.enjoyiot.eiot.component.ff.service;

import com.enjoyiot.eiot.component.ff.cilent.GbtVertxTcpClient;
import com.enjoyiot.eiot.component.dahua.common.constants.DahuaProtocolConstants;
import com.enjoyiot.eiot.component.ff.parser.GbtConstants;
import com.enjoyiot.eiot.component.ff.parser.GbtDataEncoder;
import com.enjoyiot.eiot.component.ff.parser.GbtDataPackage;
import com.enjoyiot.eiot.component.dahua.common.parser.DahuaAduBuilder;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下行指令发送器
 */
@Slf4j
public class GbtCommandSender {

    private final GbtDeviceManager deviceManager;
    private final byte[] centerAddr;

    /** 出站业务流水号计数器 */
    private final AtomicInteger outboundSerial = new AtomicInteger(0);

    public GbtCommandSender(GbtDeviceManager deviceManager, byte[] centerAddr) {
        this.deviceManager = deviceManager;
        this.centerAddr = centerAddr;
    }

    /**
     * 发送确认报文（CMD=3）
     */
    public void sendConfirm(String deviceAddr, GbtDataPackage receivedPkg) {
        GbtDataPackage confirm = GbtDataPackage.builder()
                .serialNumber(receivedPkg.getSerialNumber())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command(GbtConstants.CMD_CONFIRM)
                .applicationData(new byte[0])
                .build();
        sendGbtPacket(deviceAddr, confirm);
    }

    /**
     * 发送否认报文（CMD=6）
     */
    public void sendDeny(String deviceAddr, GbtDataPackage receivedPkg) {
        GbtDataPackage deny = GbtDataPackage.builder()
                .serialNumber(receivedPkg.getSerialNumber())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command(GbtConstants.CMD_DENY)
                .applicationData(new byte[0])
                .build();
        sendGbtPacket(deviceAddr, deny);
    }

    /**
     * 发送查询请求（CMD=4）
     */
    public void sendQuery(String deviceAddr, int queryTypeFlag, byte[] queryBody) {
        byte[] appData = buildAppData(queryTypeFlag, queryBody);
        GbtDataPackage query = GbtDataPackage.builder()
                .serialNumber(nextSerial())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command(GbtConstants.CMD_REQUEST)
                .applicationData(appData)
                .build();
        sendGbtPacket(deviceAddr, query);
    }

    /**
     * 发送控制命令（CMD=1），如时间同步、远程控制
     */
    public void sendControl(String deviceAddr, int typeFlag, byte[] controlBody) {
        byte[] appData = buildAppData(typeFlag, controlBody);
        GbtDataPackage ctrl = GbtDataPackage.builder()
                .serialNumber(nextSerial())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command(GbtConstants.CMD_CONTROL)
                .applicationData(appData)
                .build();
        sendGbtPacket(deviceAddr, ctrl);
    }

    // ==================== 大华协议 v1.04 下行指令 ====================

    /**
     * 发送大华协议下行帧（ADU 已由 DahuaAduBuilder 组装完整）
     *
     * @param deviceAddr 设备编号（大端 hex 字符串）
     * @param command    命令字（使用 DahuaProtocolConstants.CMD_* 常量）
     * @param adu        完整应用数据单元（含 typeFlag，由 DahuaAduBuilder 构建）
     */
    public void sendDahuaPacket(String deviceAddr, byte command, byte[] adu) {
        GbtDataPackage pkg = GbtDataPackage.builder()
                .serialNumber(nextSerial())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command(command)
                .applicationData(adu != null ? adu : new byte[0])
                .build();
        sendGbtPacket(deviceAddr, pkg);
    }

    /**
     * 大华时钟同步（CMD=0x01, typeFlag=0x5A）
     * 广播帧，同步设备时间到平台当前时间
     */
    public void sendDahuaClockSync(String deviceAddr) {
        byte[] adu = DahuaAduBuilder.buildClockSyncAdu();
        sendDahuaPacket(deviceAddr, DahuaProtocolConstants.CMD_CLOCK_SYNC, adu);
    }

    /**
     * 大华设置定长参数（CMD=0x04, typeFlag=0x64）
     *
     * @param paramType 参数类型（0~255）
     * @param value     参数值字节
     */
    public void sendDahuaSetParamFixed(String deviceAddr, int paramType, byte[] value) {
        byte[] adu = DahuaAduBuilder.buildSetParamFixedAdu(paramType, value);
        sendDahuaPacket(deviceAddr, DahuaProtocolConstants.CMD_PLATFORM_COMMAND, adu);
    }

    /**
     * 大华设置不定长参数（CMD=0x04, typeFlag=0x81）
     */
    public void sendDahuaSetParamVariable(String deviceAddr, int paramType, byte[] value) {
        byte[] adu = DahuaAduBuilder.buildSetParamVariableAdu(paramType, value);
        sendDahuaPacket(deviceAddr, DahuaProtocolConstants.CMD_PLATFORM_COMMAND, adu);
    }

    /**
     * 大华查询定长参数（CMD=0x04, typeFlag=0x82）
     */
    public void sendDahuaQueryParamFixed(String deviceAddr, int paramType) {
        byte[] adu = DahuaAduBuilder.buildQueryParamFixedAdu(paramType);
        sendDahuaPacket(deviceAddr, DahuaProtocolConstants.CMD_PLATFORM_COMMAND, adu);
    }

    /**
     * 大华查询不定长参数（CMD=0x04, typeFlag=0x83）
     */
    public void sendDahuaQueryParamVariable(String deviceAddr, int paramType) {
        byte[] adu = DahuaAduBuilder.buildQueryParamVariableAdu(paramType);
        sendDahuaPacket(deviceAddr, DahuaProtocolConstants.CMD_PLATFORM_COMMAND, adu);
    }

    /**
     * 大华远程消音（CMD=0x04, typeFlag=0xF9）
     *
     * @param sysType    系统类型（如 0x01=火灾报警控制器）
     * @param sysAddr    系统地址
     * @param compType   部件类型
     * @param compAddrInt 部件地址（整数，按 LE 写入4字节）
     */
    public void sendDahuaRemoteMute(String deviceAddr, int sysType, int sysAddr,
                                     int compType, int compAddrInt) {
        byte[] adu = DahuaAduBuilder.buildRemoteMuteAdu(sysType, sysAddr, compType, compAddrInt);
        sendDahuaPacket(deviceAddr, DahuaProtocolConstants.CMD_PLATFORM_COMMAND, adu);
    }

    /**
     * 大华查询通用帧（CMD=0x04, typeFlag=0x88）
     * 查询指定部件的指定数据类型信息
     *
     * @param infoObjects 查询信息对象列表（部件类型+部件地址+数据类型）
     */
    public void sendDahuaQueryUniversal(String deviceAddr,
                                          List<DahuaAduBuilder.QueryInfoObject> infoObjects) {
        byte[] adu = DahuaAduBuilder.buildQueryUniversalAdu(infoObjects);
        sendDahuaPacket(deviceAddr, DahuaProtocolConstants.CMD_PLATFORM_COMMAND, adu);
    }

    /**
     * 大华设置/配置通用帧（CMD=0x04, typeFlag=0x89）
     * 下发配置信息到指定部件
     *
     * @param infoObjects 配置信息对象列表（部件类型+地址+数据类型+数据内容）
     */
    public void sendDahuaConfigUniversal(String deviceAddr,
                                          List<DahuaAduBuilder.ConfigInfoObject> infoObjects) {
        byte[] adu = DahuaAduBuilder.buildConfigUniversalAdu(infoObjects);
        sendDahuaPacket(deviceAddr, DahuaProtocolConstants.CMD_PLATFORM_COMMAND, adu);
    }

    /**
     * 发送国标心跳应答（CMD=130，针对CMD=128的心跳）
     */
    public void sendHeartbeatReply(String deviceAddr, byte commModule, byte sysType) {
        // 国标心跳应答：使用标准帧格式，ADU包含通讯模块+系统类型
        byte[] appData = new byte[]{commModule, sysType};
        GbtDataPackage reply = GbtDataPackage.builder()
                .serialNumber(nextSerial())
                .sourceAddr(centerAddr)
                .destAddr(toProtocolAddr(deviceAddr))
                .command((byte) GbtConstants.CMD_HEARTBEAT_REPLY)
                .applicationData(appData)
                .build();
        sendGbtPacket(deviceAddr, reply);
    }

    /**
     * 发送旧版心跳应答（12字节简化帧）
     */
    public void sendLegacyHeartbeatReply(String deviceAddr, GbtVertxTcpClient client,
                                          byte commModule, byte sysType) {
        byte[] replyAddr = new byte[GbtConstants.ADDRESS_LENGTH];
        Buffer reply = Buffer.buffer(GbtConstants.HEARTBEAT_PACKET_SIZE);
        reply.appendByte(GbtConstants.START_BYTE);
        reply.appendByte(GbtConstants.START_BYTE);
        reply.appendBytes(replyAddr);
        reply.appendByte(commModule);   // 回显设备的通讯模块
        reply.appendByte(sysType);      // 回显设备的系统类型
        reply.appendByte(GbtConstants.END_BYTE);
        reply.appendByte(GbtConstants.END_BYTE);
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

    private void sendGbtPacket(String deviceAddr, GbtDataPackage pkg) {
        Buffer encoded = GbtDataEncoder.encode(pkg);
        log.info("[TCP] → 平台下发: dev={}, CMD=0x{}, serial={}, len={}",
                deviceAddr, String.format("%02X", pkg.getCommand() & 0xFF),
                pkg.getSerialNumber(), encoded.length());
        GbtVertxTcpClient client = deviceManager.getClient(deviceAddr);
        if (client != null && client.socket != null) {
            client.sendMessage(encoded);
        } else {
            log.warn("[TCP] 设备不在线，无法发送: dev={}", deviceAddr);
        }
    }

    public int nextSerial() {
        return outboundSerial.incrementAndGet() & 0xFFFF;
    }

    private byte[] toProtocolAddr(String deviceAddr) {
        return GbtDataPackage.hexToAddr(GbtDataPackage.reverseHexAddr(deviceAddr));
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
