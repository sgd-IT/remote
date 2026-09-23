package com.enjoyiot.eiot.component.gbt.fourfaith.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * 四信 GB/T 26875.3-2011 私有协议测试客户端（交互式菜单）
 * 模拟四信用传设备，覆盖心跳、PRT、CRT、远程控制应答、否认等全部场景
 */
public class TcpTestClient {

    // ==================== 常量 ====================
    private static final byte START = 0x40;
    private static final byte END = 0x23;
    private static final byte CMD_SEND = 2;
    private static final Charset GB18030 = Charset.forName("GB18030");

    // 状态位常量 (系统/部件状态)
    private static final short S_NORMAL = 0x0001;
    private static final short S_FIRE_ALARM = 0x0002;
    private static final short S_FAULT = 0x0004;
    private static final short S_SHIELDED = 0x0008;
    private static final short S_SUPERVISED = 0x0010;
    private static final short S_ACTIVATED = 0x0020;
    private static final short S_FEEDBACK = 0x0040;
    private static final short S_DELAY = 0x0080;
    private static final short S_POWER_FAULT = 0x0100;
    private static final short S_MAIN_POWER_FAULT = 0x0100;
    private static final short S_BACKUP_POWER_FAULT = 0x0200;
    private static final short S_BUS_FAULT = 0x0400;
    // 装置状态 (Type 21)
    private static final int DS_NORMAL = 0x01;
    private static final int DS_FIRE_ALARM = 0x02;
    private static final int DS_FAULT = 0x04;
    private static final int DS_MAIN_POWER_FAULT = 0x08;
    private static final int DS_BACKUP_POWER_FAULT = 0x10;
    private static final int DS_COMM_FAULT = 0x20;
    private static final int DS_LINE_FAULT = 0x40;

    // ==================== 全局变量 ====================
    private static String host = "127.0.0.1";
    private static int port = 4444;
    private static byte[] deviceAddr;
    private static byte[] centerAddr;
    private static int serial = 1;
    private static volatile boolean running = true;
    private static volatile int lastDownlinkSn = -1;
    private static volatile byte[] lastDownlinkAppData = null;

    // ==================== 编码工具 ====================
    private static byte[] toGb18030(String s) {
        if (s == null || s.isEmpty()) return new byte[0];
        return s.getBytes(GB18030);
    }

    // ==================== 标准帧构建 ====================
    private static byte[] buildPacket(int sn, byte[] srcAddr, byte[] dstAddr,
                                       byte command, byte[] appData) {
        if (appData == null) appData = new byte[0];
        int dataLen = appData.length;
        int totalLen = 30 + dataLen;

        ByteBuffer buf = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(START);
        buf.put(START);
        buf.putShort((short) sn);
        buf.put((byte) 1);
        buf.put((byte) 0);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        buf.put((byte) now.getSecond());
        buf.put((byte) now.getMinute());
        buf.put((byte) now.getHour());
        buf.put((byte) now.getDayOfMonth());
        buf.put((byte) now.getMonthValue());
        buf.put((byte) (now.getYear() - 2000));
        buf.put(srcAddr);
        buf.put(dstAddr);
        buf.putShort((short) dataLen);
        buf.put(command);
        if (dataLen > 0) buf.put(appData);

        byte[] arr = buf.array();
        int sum = 0;
        for (int i = 2; i < 27 + dataLen; i++) {
            sum += (arr[i] & 0xFF);
        }
        buf.put((byte) (sum & 0xFF));
        buf.put(END);
        buf.put(END);
        return buf.array();
    }

    private static byte[] currentTimestamp() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return new byte[]{
            (byte) now.getSecond(), (byte) now.getMinute(), (byte) now.getHour(),
            (byte) now.getDayOfMonth(), (byte) now.getMonthValue(), (byte) (now.getYear() - 2000)
        };
    }

    // ==================== 心跳包 ====================
    private static void sendHeartbeat(OutputStream out) throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.put(START);
        buf.put(START);
        buf.put(deviceAddr);
        buf.put((byte) 0x04); // 通信模块
        buf.put((byte) 0x88); // 系统类型
        buf.put(END);
        buf.put(END);
        out.write(buf.array());
        out.flush();
        System.out.println(">>> 发送心跳包: " + bytesToHex(buf.array()));
    }

    // ==================== 标准帧应用数据 ====================
    private static byte[] buildSystemStatusData(int sysType, int sysAddr, short status) {
        ByteBuffer body = ByteBuffer.allocate(2 + 10).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 1);
        body.put((byte) 1);
        body.put((byte) sysType);
        body.put((byte) sysAddr);
        body.putShort(status);
        body.put(currentTimestamp());
        return body.array();
    }

    private static byte[] buildComponentStatusData(int sysType, int sysAddr,
                                                    int compType, int compAddr,
                                                    short status, String desc) {
        ByteBuffer body = ByteBuffer.allocate(2 + 46).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 2);
        body.put((byte) 1);
        body.put((byte) sysType);
        body.put((byte) sysAddr);
        body.put((byte) compType);
        body.putInt(compAddr);
        body.putShort(status);
        byte[] descBytes = new byte[31];
        if (desc != null) {
            byte[] raw = toGb18030(desc);
            System.arraycopy(raw, 0, descBytes, 0, Math.min(raw.length, 31));
        }
        body.put(descBytes);
        body.put(currentTimestamp());
        return body.array();
    }

    private static byte[] buildAnalogValueData(int sysType, int sysAddr,
                                                int compType, int compAddr,
                                                int analogType, short rawValue) {
        ByteBuffer body = ByteBuffer.allocate(2 + 16).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 3);
        body.put((byte) 1);
        body.put((byte) sysType);
        body.put((byte) sysAddr);
        body.put((byte) compType);
        body.putInt(compAddr);
        body.put((byte) analogType);
        body.putShort(rawValue);
        body.put(currentTimestamp());
        return body.array();
    }

    private static byte[] buildDeviceStatusData(int statusByte) {
        ByteBuffer body = ByteBuffer.allocate(2 + 7).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 21);
        body.put((byte) 1);
        body.put((byte) statusByte);
        body.put(currentTimestamp());
        return body.array();
    }

    // ==================== Type 4: 操作信息 ====================
    private static byte[] buildOperationInfoData(int sysType, int sysAddr, int operType, int operatorNum) {
        ByteBuffer body = ByteBuffer.allocate(2 + 10).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 4);
        body.put((byte) 1);
        body.put((byte) sysType);
        body.put((byte) sysAddr);
        body.put((byte) operType);
        body.put((byte) operatorNum);
        body.put(currentTimestamp());
        return body.array();
    }

    // ==================== Type 5: 软件版本 ====================
    private static byte[] buildSoftwareVersionData(int sysType, int sysAddr, int major, int minor) {
        ByteBuffer body = ByteBuffer.allocate(2 + 4).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 5);
        body.put((byte) 1);
        body.put((byte) sysType);
        body.put((byte) sysAddr);
        body.put((byte) major);
        body.put((byte) minor);
        return body.array();
    }

    // ==================== Type 6: 系统配置描述 ====================
    private static byte[] buildSystemConfigData(int sysType, int sysAddr, String desc) {
        byte[] descBytes = toGb18030(desc);
        ByteBuffer body = ByteBuffer.allocate(2 + 3 + descBytes.length).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 6);
        body.put((byte) 1);
        body.put((byte) sysType);
        body.put((byte) sysAddr);
        body.put((byte) descBytes.length);
        body.put(descBytes);
        return body.array();
    }

    // ==================== Type 7: 部件配置描述 ====================
    private static byte[] buildComponentConfigData(int sysType, int sysAddr, int compType, int compAddr, String desc) {
        byte[] descBytes = new byte[31];
        if (desc != null) {
            byte[] raw = toGb18030(desc);
            System.arraycopy(raw, 0, descBytes, 0, Math.min(raw.length, 31));
        }
        ByteBuffer body = ByteBuffer.allocate(2 + 38).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 7);
        body.put((byte) 1);
        body.put((byte) sysType);
        body.put((byte) sysAddr);
        body.put((byte) compType);
        body.putInt(compAddr);
        body.put(descBytes);
        return body.array();
    }

    // ==================== Type 8: 系统时间 ====================
    private static byte[] buildSystemTimeData(int sysType, int sysAddr) {
        ByteBuffer body = ByteBuffer.allocate(2 + 8).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 8);
        body.put((byte) 1);
        body.put((byte) sysType);
        body.put((byte) sysAddr);
        body.put(currentTimestamp());
        return body.array();
    }

    // ==================== Type 24: 传输装置操作信息 ====================
    private static byte[] buildDeviceOperationData(int operType, int operatorNum) {
        ByteBuffer body = ByteBuffer.allocate(2 + 8).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 24);
        body.put((byte) 1);
        body.put((byte) operType);
        body.put((byte) operatorNum);
        body.put(currentTimestamp());
        return body.array();
    }

    // ==================== Type 25: 传输装置软件版本 ====================
    private static byte[] buildDeviceVersionData(int major, int minor) {
        ByteBuffer body = ByteBuffer.allocate(2 + 2).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 25);
        body.put((byte) 1);
        body.put((byte) major);
        body.put((byte) minor);
        return body.array();
    }

    // ==================== Type 26: 传输装置配置描述 ====================
    private static byte[] buildDeviceConfigData(String desc) {
        byte[] descBytes = toGb18030(desc);
        ByteBuffer body = ByteBuffer.allocate(2 + 1 + descBytes.length).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 26);
        body.put((byte) 1);
        body.put((byte) descBytes.length);
        body.put(descBytes);
        return body.array();
    }

    // ==================== Type 28: 传输装置系统时间 ====================
    private static byte[] buildDeviceTimeData() {
        ByteBuffer body = ByteBuffer.allocate(2 + 6).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 28);
        body.put((byte) 1);
        body.put(currentTimestamp());
        return body.array();
    }

    // ==================== 四信私有帧：PRT ====================
    private static byte[] buildPrtData(int channelNo, int hostNo, int alarmType,
                                        String deviceName, String location, String alarmInfo) {
        byte[] nameBytes = toGb18030(deviceName);
        byte[] locBytes = toGb18030(location);
        byte[] infoBytes = toGb18030(alarmInfo);

        int bodyLen = 1 + 1 + 2 + 1 + 1 + nameBytes.length + 1 + locBytes.length + 1 + infoBytes.length + 6;
        ByteBuffer body = ByteBuffer.allocate(2 + bodyLen).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 0x09); // typeFlag
        body.put((byte) 0x01); // 信息对象数目

        body.put((byte) channelNo);
        body.put((byte) hostNo);
        body.putShort((short) 0x3333); // 预留
        body.put((byte) alarmType);

        body.put((byte) nameBytes.length);
        body.put(nameBytes);
        body.put((byte) locBytes.length);
        body.put(locBytes);
        body.put((byte) infoBytes.length);
        body.put(infoBytes);

        body.put(currentTimestamp());
        return body.array();
    }

    // ==================== 四信私有帧：CRT ====================
    private static byte[] buildCrtData(int channelNo, int hostNo, int alarmType,
                                        String field1Name, String field2Location,
                                        String field3Info, String field4Number) {
        byte[] f1 = toGb18030(field1Name);
        byte[] f2 = toGb18030(field2Location);
        byte[] f3 = toGb18030(field3Info);
        byte[] f4 = toGb18030(field4Number);

        int bodyLen = 1 + 1 + 1 + 1 + 1; // 通道号+主机号+预留+类型个数+报警类型
        bodyLen += 1 + 1 + f1.length; // 字段1
        bodyLen += 1 + 1 + f2.length; // 字段2
        bodyLen += 1 + 1 + f3.length; // 字段3
        bodyLen += 1 + 1 + f4.length; // 字段4
        bodyLen += 6; // 时间标签

        ByteBuffer body = ByteBuffer.allocate(2 + bodyLen).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 0x0A); // typeFlag
        body.put((byte) 0x01); // 信息对象数目

        body.put((byte) channelNo);
        body.put((byte) hostNo);
        body.put((byte) 0x33); // 预留
        body.put((byte) 0x04); // 类型个数
        body.put((byte) alarmType);

        // 字段1: 设备名称
        body.put((byte) 0x01);
        body.put((byte) f1.length);
        body.put(f1);

        // 字段2: 设备位置
        body.put((byte) 0x01);
        body.put((byte) f2.length);
        body.put(f2);

        // 字段3: 设备信息
        body.put((byte) 0x01);
        body.put((byte) f3.length);
        body.put(f3);

        // 字段4: 设备编号
        body.put((byte) 0x01);
        body.put((byte) f4.length);
        body.put(f4);

        body.put(currentTimestamp());
        return body.array();
    }

    // ==================== 四信私有帧：远程控制应答 (0x85) ====================
    private static byte[] buildRemoteResponseData(int sysType, int sysAddr, int compType,
                                                   int compAddr, int status, String content) {
        byte[] contentBytes = toGb18030(content);
        int bodyLen = 2 + 1 + 1 + 1 + 4 + 1 + 2 + contentBytes.length;
        ByteBuffer body = ByteBuffer.allocate(bodyLen).order(ByteOrder.LITTLE_ENDIAN);
        body.put((byte) 0x85); // typeFlag
        body.put((byte) 0x01); // 信息体数目
        body.put((byte) sysType);
        body.put((byte) sysAddr);
        body.put((byte) compType);
        body.putInt(compAddr);
        body.put((byte) status);
        body.putShort((short) contentBytes.length);
        body.put(contentBytes);
        return body.array();
    }

    // ==================== 否认帧 ====================
    private static void sendDeny(OutputStream out, int sn) throws Exception {
        byte[] pkt = buildPacket(sn, deviceAddr, centerAddr, (byte) 0x04, null);
        out.write(pkt);
        out.flush();
        System.out.println(">>> 发送否认帧 SN=" + sn);
        System.out.println("    HEX: " + bytesToHex(pkt));
    }

    // ==================== 发送方法 ====================
    private static void sendPacket(OutputStream out, String label, byte[] appData) throws Exception {
        System.out.println(label);
        byte[] pkt = buildPacket(serial++, deviceAddr, centerAddr, CMD_SEND, appData);
        out.write(pkt);
        out.flush();
        System.out.printf("    已发送 SN=%d 长度=%d%n", serial - 1, pkt.length);
        System.out.println("    HEX: " + bytesToHex(pkt));
    }

    // ==================== 解码工具 ====================
    private static byte[] readFully(InputStream in, int len) throws Exception {
        byte[] data = new byte[len];
        int off = 0;
        while (off < len) {
            int r = in.read(data, off, len - off);
            if (r < 0) throw new IllegalStateException("socket closed");
            off += r;
        }
        return data;
    }

    private static byte[] readPacket(InputStream in) throws Exception {
        int prev = -1;
        while (true) {
            int b = in.read();
            if (b < 0) throw new IllegalStateException("socket closed");
            if (prev == START && b == START) break;
            prev = b;
        }

        // 先读取 10 字节，判断是否为心跳包
        byte[] peek = readFully(in, 10);
        if (peek[8] == END && peek[9] == END) {
            byte[] packet = new byte[12];
            packet[0] = START;
            packet[1] = START;
            System.arraycopy(peek, 0, packet, 2, 10);
            return packet;
        }

        // 标准帧
        byte[] headerRest = readFully(in, 14);
        byte[] header = new byte[24];
        System.arraycopy(peek, 0, header, 0, 10);
        System.arraycopy(headerRest, 0, header, 10, 14);

        int dataLength = (header[22] & 0xFF) | ((header[23] & 0xFF) << 8);
        byte[] rest = readFully(in, dataLength + 4);
        byte[] packet = new byte[30 + dataLength];
        packet[0] = START;
        packet[1] = START;
        System.arraycopy(header, 0, packet, 2, 24);
        System.arraycopy(rest, 0, packet, 26, rest.length);
        return packet;
    }

    private static int getSerialNumber(byte[] packet) {
        return (packet[2] & 0xFF) | ((packet[3] & 0xFF) << 8);
    }

    private static byte getCommand(byte[] packet) {
        if (packet.length == 12) return (byte) 0xFF; // 心跳包
        return packet[26];
    }

    private static byte[] getAppData(byte[] packet) {
        if (packet.length == 12) return new byte[0];
        int dataLength = (packet[24] & 0xFF) | ((packet[25] & 0xFF) << 8);
        if (dataLength <= 0) return new byte[0];
        byte[] appData = new byte[dataLength];
        System.arraycopy(packet, 27, appData, 0, dataLength);
        return appData;
    }

    private static byte[] hexToAddr(String hex) {
        while (hex.length() < 12) hex = "0" + hex;
        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }

    // ==================== 下行监听线程 ====================
    private static void startDownlinkListener(InputStream in) {
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    byte[] pkt = readPacket(in);
                    byte cmd = getCommand(pkt);
                    int sn = getSerialNumber(pkt);

                    if (pkt.length == 12) {
                        System.out.printf("%n<<< [下行] 心跳应答 SN=%d 长度=12%n", sn);
                        System.out.println("    HEX: " + bytesToHex(pkt));
                        continue;
                    }

                    byte[] appData = getAppData(pkt);
                    System.out.printf("%n<<< [下行] 命令=%d SN=%d 长度=%d%n", cmd, sn, pkt.length);
                    System.out.println("    HEX: " + bytesToHex(pkt));

                    if (cmd == 0x01 && appData != null && appData.length > 0) {
                        int typeFlag = appData[0] & 0xFF;
                        if (typeFlag == 0x81) {
                            System.out.println("<<< 收到远程控制指令(0x81)");
                            lastDownlinkSn = sn;
                            lastDownlinkAppData = appData;
                            System.out.println("    输入 R=成功应答  E=失败应答  N=否认");
                        } else if (typeFlag == 0x89) {
                            System.out.println("<<< 收到初始化指令(0x89)");
                        } else if (typeFlag == 0x90) {
                            System.out.println("<<< 收到同步时间指令(0x90)");
                        } else if (typeFlag == 0x91) {
                            System.out.println("<<< 收到查岗指令(0x91)");
                        }
                    } else if (cmd == 0x03) {
                        System.out.println("<<< 收到确认帧(cmd=03)");
                    }
                } catch (Exception e) {
                    if (!running) break;
                    String msg = e.getMessage();
                    if ("socket closed".equals(msg)
                            || e instanceof java.io.EOFException
                            || e instanceof java.net.SocketException) {
                        System.out.println("\n<<< 连接已断开");
                        running = false;
                        break;
                    }
                    System.out.println("<<< 下行监听异常: " + msg);
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ==================== 输入辅助 ====================
    private static int readInt(BufferedReader reader, String prompt, int defaultVal) throws Exception {
        System.out.print(prompt);
        String input = reader.readLine().trim();
        if (input.isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("  输入无效，使用默认值: " + defaultVal);
            return defaultVal;
        }
    }

    private static String readLine(BufferedReader reader, String prompt, String defaultVal) throws Exception {
        System.out.print(prompt);
        String input = reader.readLine().trim();
        return input.isEmpty() ? defaultVal : input;
    }

    // ==================== 按类型标志组织的测试场景 ====================

    // ---- Type 1: 系统状态 ----
    private static void testType1(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 1 建筑消防设施系统状态 ---");
        System.out.println("1. 正常      2. 火警      3. 故障");
        System.out.println("4. 屏蔽      5. 主电故障  6. 备电故障");
        System.out.println("7. 总线故障  8. 配置改变");
        System.out.print("请选择状态: ");
        String choice = reader.readLine().trim();
        int sysType = readInt(reader, "系统类型 [1]: ", 1);
        int sysAddr = readInt(reader, "系统地址 [1]: ", 1);
        short status;
        String label;
        switch (choice) {
            case "2":  status = S_FIRE_ALARM;       label = "火警"; break;
            case "3":  status = S_FAULT;            label = "故障"; break;
            case "4":  status = S_SHIELDED;         label = "屏蔽"; break;
            case "5":  status = S_MAIN_POWER_FAULT; label = "主电故障"; break;
            case "6":  status = S_BACKUP_POWER_FAULT; label = "备电故障"; break;
            case "7":  status = S_BUS_FAULT;        label = "总线故障"; break;
            case "8":  status = (short) 0x1000;     label = "配置改变"; break;
            default:   status = S_NORMAL;           label = "正常";
        }
        sendPacket(out, ">>> Type 1 系统状态: " + label,
                buildSystemStatusData(sysType, sysAddr, status));
    }

    // ---- Type 2: 部件状态 ----
    private static void testType2(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 2 建筑消防设施部件运行状态 ---");
        int sysType = readInt(reader, "系统类型 [1]: ", 1);
        int sysAddr = readInt(reader, "系统地址 [1]: ", 1);
        int compType = readInt(reader, "部件类型 [10]: ", 10);
        int compAddr = readInt(reader, "部件地址 [16908289=0x01020001]: ", 0x01020001);
        String desc = readLine(reader, "部件说明 [1F-01烟感探测器]: ", "1F-01烟感探测器");
        System.out.println("1. 正常  2. 火警  3. 故障  4. 屏蔽");
        System.out.println("5. 监管  6. 启动  7. 反馈  8. 延时  9. 电源故障");
        System.out.print("请选择状态: ");
        String choice = reader.readLine().trim();
        short status;
        String label;
        switch (choice) {
            case "2":  status = (short) (S_NORMAL | S_FIRE_ALARM);  label = "火警"; break;
            case "3":  status = (short) (S_NORMAL | S_FAULT);       label = "故障"; break;
            case "4":  status = (short) (S_NORMAL | S_SHIELDED);    label = "屏蔽"; break;
            case "5":  status = (short) (S_NORMAL | S_SUPERVISED);  label = "监管"; break;
            case "6":  status = (short) (S_NORMAL | S_ACTIVATED);   label = "启动"; break;
            case "7":  status = (short) (S_NORMAL | S_FEEDBACK);    label = "反馈"; break;
            case "8":  status = (short) (S_NORMAL | S_DELAY);       label = "延时"; break;
            case "9":  status = (short) (S_NORMAL | S_POWER_FAULT); label = "电源故障"; break;
            default:   status = S_NORMAL;                            label = "正常";
        }
        sendPacket(out, ">>> Type 2 部件状态: " + label,
                buildComponentStatusData(sysType, sysAddr, compType, compAddr, status, desc));
    }

    // ---- Type 3: 模拟量 ----
    private static void testType3(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 3 建筑消防设施模拟量值 ---");
        int sysType = readInt(reader, "系统类型 [1]: ", 1);
        int sysAddr = readInt(reader, "系统地址 [1]: ", 1);
        int compType = readInt(reader, "部件类型 [10]: ", 10);
        int compAddr = readInt(reader, "部件地址 [16908289]: ", 0x01020001);
        System.out.println("模拟量类型: 3=温度 4=压力 其他=自定义");
        int analogType = readInt(reader, "模拟量类型 [3]: ", 3);
        int rawValue = readInt(reader, "模拟量原始值 [300]: ", 300);
        String unit = analogType == 3 ? "℃" : analogType == 4 ? "MPa" : "";
        sendPacket(out, String.format(">>> Type 3 模拟量 (%.1f%s)", rawValue * 0.1, unit),
                buildAnalogValueData(sysType, sysAddr, compType, compAddr, analogType, (short) rawValue));
    }

    // ---- Type 4: 操作信息 ----
    private static void testType4(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 4 建筑消防设施操作信息 ---");
        int sysType = readInt(reader, "系统类型 [1]: ", 1);
        int sysAddr = readInt(reader, "系统地址 [1]: ", 1);
        int operType = readInt(reader, "操作标志 [1]: ", 1);
        int operatorNum = readInt(reader, "操作员编号 [1]: ", 1);
        sendPacket(out, ">>> Type 4 操作信息",
                buildOperationInfoData(sysType, sysAddr, operType, operatorNum));
    }

    // ---- Type 5: 软件版本 ----
    private static void testType5(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 5 建筑消防设施软件版本 ---");
        int sysType = readInt(reader, "系统类型 [1]: ", 1);
        int sysAddr = readInt(reader, "系统地址 [1]: ", 1);
        int major = readInt(reader, "主版本号 [1]: ", 1);
        int minor = readInt(reader, "次版本号 [0]: ", 0);
        sendPacket(out, ">>> Type 5 软件版本 v" + major + "." + minor,
                buildSoftwareVersionData(sysType, sysAddr, major, minor));
    }

    // ---- Type 6: 系统配置 ----
    private static void testType6(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 6 建筑消防设施系统配置 ---");
        int sysType = readInt(reader, "系统类型 [1]: ", 1);
        int sysAddr = readInt(reader, "系统地址 [1]: ", 1);
        String desc = readLine(reader, "配置说明 [系统配置描述]: ", "系统配置描述");
        sendPacket(out, ">>> Type 6 系统配置",
                buildSystemConfigData(sysType, sysAddr, desc));
    }

    // ---- Type 7: 部件配置 ----
    private static void testType7(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 7 建筑消防设施部件配置 ---");
        int sysType = readInt(reader, "系统类型 [1]: ", 1);
        int sysAddr = readInt(reader, "系统地址 [1]: ", 1);
        int compType = readInt(reader, "部件类型 [10]: ", 10);
        int compAddr = readInt(reader, "部件地址 [16908289]: ", 0x01020001);
        String desc = readLine(reader, "部件说明 [1F-01烟感探测器]: ", "1F-01烟感探测器");
        sendPacket(out, ">>> Type 7 部件配置",
                buildComponentConfigData(sysType, sysAddr, compType, compAddr, desc));
    }

    // ---- Type 8: 系统时间 ----
    private static void testType8(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 8 建筑消防设施系统时间 ---");
        int sysType = readInt(reader, "系统类型 [1]: ", 1);
        int sysAddr = readInt(reader, "系统地址 [1]: ", 1);
        sendPacket(out, ">>> Type 8 系统时间",
                buildSystemTimeData(sysType, sysAddr));
    }

    // ---- Type 9 (PRT): 打印信息 ----
    private static void testType9(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 9 (PRT) 打印信息 ---");
        System.out.println("1. 正常  2. 火警  3. 故障  4. 屏蔽  5. 自定义");
        System.out.print("请选择报警类型: ");
        String choice = reader.readLine().trim();
        int alarmType;
        String alarmInfo;
        switch (choice) {
            case "2": alarmType = 1; alarmInfo = "火警"; break;
            case "3": alarmType = 2; alarmInfo = "故障"; break;
            case "4": alarmType = 3; alarmInfo = "屏蔽"; break;
            case "5":
                alarmType = readInt(reader, "自定义alarmType [5]: ", 5);
                alarmInfo = readLine(reader, "报警信息 [自定义报警]: ", "自定义报警");
                break;
            default:  alarmType = 0; alarmInfo = "正常";
        }
        int channel = readInt(reader, "通道号 [0]: ", 0);
        int host = readInt(reader, "主机号 [0]: ", 0);
        String deviceName = readLine(reader, "设备名称 [感烟探测器00001]: ", "感烟探测器00001");
        String location = readLine(reader, "设备位置 [1层走廊东侧]: ", "1层走廊东侧");
        sendPacket(out, ">>> Type 9 PRT: " + alarmInfo,
                buildPrtData(channel, host, alarmType, deviceName, location, alarmInfo));
    }

    // ---- Type 10 (CRT): 图形显示信息 ----
    private static void testType10(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 10 (CRT) 图形显示信息 ---");
        System.out.println("1. 火警  2. 故障  3. 自定义");
        System.out.print("请选择报警类型: ");
        String choice = reader.readLine().trim();
        int alarmType;
        String info;
        switch (choice) {
            case "2": alarmType = 2; info = "故障报警"; break;
            case "3":
                alarmType = readInt(reader, "自定义alarmType [3]: ", 3);
                info = readLine(reader, "报警信息 [自定义CRT报警]: ", "自定义CRT报警");
                break;
            default:  alarmType = 1; info = "火警报警";
        }
        int channel = readInt(reader, "通道号 [0]: ", 0);
        int host = readInt(reader, "主机号 [0]: ", 0);
        String field2 = readLine(reader, "点位描述 [&&QN01,机器01,回路01,部件00000001]: ",
                "&&QN01,机器01,回路01,部件00000001");
        sendPacket(out, ">>> Type 10 CRT: " + info,
                buildCrtData(channel, host, alarmType, "", field2, info, ""));
    }

    // ---- Type 21: 传输装置状态 ----
    private static void testType21(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 21 用户信息传输装置运行状态 ---");
        System.out.println("1. 正常      2. 火警      3. 故障");
        System.out.println("4. 主电故障  5. 备电故障  6. 通信故障  7. 线路故障");
        System.out.print("请选择状态: ");
        String choice = reader.readLine().trim();
        int status;
        String label;
        switch (choice) {
            case "2": status = DS_FIRE_ALARM;        label = "火警"; break;
            case "3": status = DS_FAULT;             label = "故障"; break;
            case "4": status = DS_MAIN_POWER_FAULT;  label = "主电故障"; break;
            case "5": status = DS_BACKUP_POWER_FAULT; label = "备电故障"; break;
            case "6": status = DS_COMM_FAULT;        label = "通信故障"; break;
            case "7": status = DS_LINE_FAULT;        label = "线路故障"; break;
            default:  status = DS_NORMAL;            label = "正常";
        }
        sendPacket(out, ">>> Type 21 装置状态: " + label, buildDeviceStatusData(status));
    }

    // ---- Type 24: 传输装置操作信息 ----
    private static void testType24(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 24 传输装置操作信息 ---");
        int operType = readInt(reader, "操作信息 [1]: ", 1);
        int operatorNum = readInt(reader, "操作员编号 [1]: ", 1);
        sendPacket(out, ">>> Type 24 传输装置操作",
                buildDeviceOperationData(operType, operatorNum));
    }

    // ---- Type 25: 传输装置软件版本 ----
    private static void testType25(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 25 传输装置软件版本 ---");
        int major = readInt(reader, "主版本号 [1]: ", 1);
        int minor = readInt(reader, "次版本号 [0]: ", 0);
        sendPacket(out, ">>> Type 25 传输装置版本 v" + major + "." + minor,
                buildDeviceVersionData(major, minor));
    }

    // ---- Type 26: 传输装置配置 ----
    private static void testType26(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 26 传输装置配置描述 ---");
        String desc = readLine(reader, "配置说明 [传输装置配置描述]: ", "传输装置配置描述");
        sendPacket(out, ">>> Type 26 传输装置配置",
                buildDeviceConfigData(desc));
    }

    // ---- Type 28: 传输装置系统时间 ----
    private static void testType28(BufferedReader reader, OutputStream out) throws Exception {
        System.out.println("\n--- Type 28 传输装置系统时间 ---");
        sendPacket(out, ">>> Type 28 传输装置时间", buildDeviceTimeData());
    }

    // ---- 心跳包 ----
    private static void testHeartbeat(OutputStream out) throws Exception {
        System.out.println("\n========== [心跳包测试] ==========");
        for (int i = 0; i < 3; i++) {
            sendHeartbeat(out);
            Thread.sleep(2000);
        }
    }

    // ---- 全部标准测试流程 ----
    private static void testAll(OutputStream out) throws Exception {
        System.out.println("\n========== [全部标准测试流程] ==========");
        sendPacket(out, ">>> 装置恢复正常 (Type 21)", buildDeviceStatusData(DS_NORMAL));
        sendPacket(out, ">>> 系统恢复正常 (Type 1)", buildSystemStatusData(1, 1, S_NORMAL));
        sendPacket(out, ">>> 部件恢复正常 (Type 2)",
                buildComponentStatusData(1, 1, 10, 0x01020001, S_NORMAL, "1F-01烟感探测器"));
        Thread.sleep(1000);
        sendPacket(out, ">>> 装置火警上报 (Type 21)", buildDeviceStatusData(DS_FIRE_ALARM));
        sendPacket(out, ">>> 部件火警上报 (Type 2)",
                buildComponentStatusData(1, 1, 10, 0x01020001,
                        (short) (S_NORMAL | S_FIRE_ALARM), "1F-01烟感探测器"));
        Thread.sleep(1000);
        sendPacket(out, ">>> 系统火警上报 (Type 1)", buildSystemStatusData(1, 1, S_FIRE_ALARM));
        Thread.sleep(1000);
        sendPacket(out, ">>> 部件故障上报 (Type 2)",
                buildComponentStatusData(1, 1, 10, 0x01020001,
                        (short) (S_NORMAL | S_FAULT), "1F-01烟感探测器"));
        Thread.sleep(1000);
        sendPacket(out, ">>> 部件电源故障 (Type 2)",
                buildComponentStatusData(1, 1, 10, 0x01020001,
                        (short) (S_NORMAL | S_POWER_FAULT), "1F-01烟感探测器"));
        Thread.sleep(1000);
        sendPacket(out, ">>> 恢复正常", buildDeviceStatusData(DS_NORMAL));
        sendPacket(out, ">>> 系统恢复正常 (Type 1)", buildSystemStatusData(1, 1, S_NORMAL));
        sendPacket(out, ">>> 部件恢复正常 (Type 2)",
                buildComponentStatusData(1, 1, 10, 0x01020001, S_NORMAL, "1F-01烟感探测器"));
        System.out.println("\n========== [全部测试完成] ==========");
    }

    // ---- 远程控制应答 ----
    private static void sendRemoteResponse(OutputStream out, boolean success) throws Exception {
        if (lastDownlinkSn < 0) {
            System.out.println("!!! 未收到远程控制指令，无法发送应答");
            return;
        }
        int status = success ? 0x80 : 0x00;
        String content = success ? "执行成功" : "执行失败";
        byte[] appData = buildRemoteResponseData(0x88, 0x00, 0x83,
                0x00000001, status, content);
        byte[] pkt = buildPacket(lastDownlinkSn, deviceAddr, centerAddr, (byte) 0x05, appData);
        out.write(pkt);
        out.flush();
        System.out.printf(">>> 发送远程控制%s应答 SN=%d%n", success ? "成功" : "失败", lastDownlinkSn);
        System.out.println("    HEX: " + bytesToHex(pkt));
        lastDownlinkSn = -1;
        lastDownlinkAppData = null;
    }

    // ==================== 配置 ====================
    private static void configConnection(BufferedReader reader) throws Exception {
        System.out.print("  服务器地址 [" + host + "]: ");
        String input = reader.readLine().trim();
        if (!input.isEmpty()) host = input;

        System.out.print("  端口 [" + port + "]: ");
        input = reader.readLine().trim();
        if (!input.isEmpty()) {
            try {
                port = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("  端口无效，使用默认值: " + port);
            }
        }

        System.out.print("  设备地址(12位hex) [1046945061FF]: ");
        input = reader.readLine().trim();
        deviceAddr = hexToAddr(input.isEmpty() ? "1046945061FF" : input);

        System.out.print("  中心地址(12位hex) [020000000001]: ");
        input = reader.readLine().trim();
        centerAddr = hexToAddr(input.isEmpty() ? "020000000001" : input);

        System.out.printf("  配置完成: %s:%d, 设备=%s%n", host, port, bytesToHex(deviceAddr));
    }

    // ==================== 主流程 ====================
    public static void main(String[] args) throws Exception {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║  四信 GB/T 26875.3-2011 协议测试客户端 (交互式)   ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("\n--- 连接配置 ---");
        configConnection(reader);

        System.out.println("\n--- 连接服务器 ---");
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(0);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            System.out.println("  连接成功!");

            // 启动下行监听线程
            startDownlinkListener(in);

            System.out.println("\n--- 设备注册 ---");
            sendPacket(out, ">>> 装置状态注册 (Type 21)",
                    buildDeviceStatusData(DS_NORMAL));

            while (true) {
                System.out.println();
                System.out.println("╔═══════════════════════════════════════════════════╗");
                System.out.println("║     请选择类型标志 (GB/T 26875.3 + 四信私有)      ║");
                System.out.println("╠═══════════════════════════════════════════════════╣");
                System.out.println("║  1. Type 1   建筑消防设施系统状态                 ║");
                System.out.println("║  2. Type 2   建筑消防设施部件运行状态             ║");
                System.out.println("║  3. Type 3   建筑消防设施模拟量值                 ║");
                System.out.println("║  4. Type 4   建筑消防设施操作信息                 ║");
                System.out.println("║  5. Type 5   建筑消防设施软件版本                 ║");
                System.out.println("║  6. Type 6   建筑消防设施系统配置                 ║");
                System.out.println("║  7. Type 7   建筑消防设施部件配置                 ║");
                System.out.println("║  8. Type 8   建筑消防设施系统时间                 ║");
                System.out.println("║  9. Type 9   四信PRT打印信息                      ║");
                System.out.println("║ 10. Type 10  四信CRT图形显示信息                  ║");
                System.out.println("║ 11. Type 21  用户信息传输装置运行状态             ║");
                System.out.println("║ 12. Type 24  传输装置操作信息                     ║");
                System.out.println("║ 13. Type 25  传输装置软件版本                     ║");
                System.out.println("║ 14. Type 26  传输装置配置描述                     ║");
                System.out.println("║ 15. Type 28  传输装置系统时间                     ║");
                System.out.println("╠═══════════════════════════════════════════════════╣");
                System.out.println("║  H. 发送心跳包                                    ║");
                System.out.println("║  R. 远程控制成功应答   E. 远程控制失败应答          ║");
                System.out.println("║  N. 发送否认帧     T. 全部标准测试流程            ║");
                System.out.println("╠═══════════════════════════════════════════════════╣");
                System.out.println("║  0. 重新配置连接    Q. 退出                        ║");
                System.out.println("╚═══════════════════════════════════════════════════╝");
                System.out.print("请输入选项: ");

                String choice = reader.readLine().trim().toUpperCase();
                if (choice.isEmpty()) continue;

                try {
                    switch (choice) {
                        case "1":  testType1(reader, out); break;
                        case "2":  testType2(reader, out); break;
                        case "3":  testType3(reader, out); break;
                        case "4":  testType4(reader, out); break;
                        case "5":  testType5(reader, out); break;
                        case "6":  testType6(reader, out); break;
                        case "7":  testType7(reader, out); break;
                        case "8":  testType8(reader, out); break;
                        case "9":  testType9(reader, out); break;
                        case "10": testType10(reader, out); break;
                        case "11": testType21(reader, out); break;
                        case "12": testType24(reader, out); break;
                        case "13": testType25(reader, out); break;
                        case "14": testType26(reader, out); break;
                        case "15": testType28(reader, out); break;
                        case "H":  testHeartbeat(out); break;
                        case "R":  sendRemoteResponse(out, true); break;
                        case "E":  sendRemoteResponse(out, false); break;
                        case "N":
                            if (lastDownlinkSn >= 0) {
                                sendDeny(out, lastDownlinkSn);
                                lastDownlinkSn = -1;
                            } else {
                                System.out.println("!!! 未收到指令，发送测试否认帧 SN=1");
                                sendDeny(out, 1);
                            }
                            break;
                        case "T":  testAll(out); break;
                        case "0":  configConnection(reader); break;
                        case "Q":
                        case "EXIT":
                            running = false;
                            System.out.println("退出测试客户端");
                            return;
                        default:
                            System.out.println("无效选项: " + choice);
                    }
                } catch (Exception e) {
                    System.out.println("操作失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}