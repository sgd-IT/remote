package com.enjoyiot.eiot.component.tcp.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;

/**
 * GB/T 26875.3-2011 协议测试客户端（交互式菜单）
 * 模拟用户信息传输装置，向监控中心发送标准数据包
 */
public class TcpTestClient {

    // ==================== 常量 ====================
    private static final byte START = 0x40;
    private static final byte END = 0x23;
    private static final byte CMD_SEND = 2;

    // ==================== 状态位常量 ====================
    // Type 1/2 系统/部件状态 (2字节, LE)
    private static final short S_NORMAL = 0x0001;
    private static final short S_FIRE_ALARM = 0x0002;
    private static final short S_FAULT = 0x0004;
    private static final short S_MAIN_PWR_FAULT = 0x0100;
    private static final short S_BACKUP_PWR_FAULT = 0x0200;

    // Type 21 装置状态 (1字节)
    private static final int DS_NORMAL = 0x01;
    private static final int DS_FIRE_ALARM = 0x02;
    private static final int DS_FAULT = 0x04;
    private static final int DS_MAIN_PWR_FAULT = 0x08;
    private static final int DS_BACKUP_PWR_FAULT = 0x10;

    // 预定义状态组合
    private static final short SYS_NORMAL = S_NORMAL;
    private static final short SYS_FIRE_ALARM = (short) (S_NORMAL | S_FIRE_ALARM);
    private static final short SYS_FAULT = (short) (S_NORMAL | S_FAULT);
    private static final short SYS_PWR_FAULT = (short) (S_NORMAL | S_MAIN_PWR_FAULT | S_BACKUP_PWR_FAULT);

    private static final int DEV_NORMAL = DS_NORMAL;
    private static final int DEV_FIRE_ALARM = DS_NORMAL | DS_FIRE_ALARM;
    private static final int DEV_FAULT = DS_NORMAL | DS_FAULT;
    private static final int DEV_PWR_FAULT = DS_NORMAL | DS_MAIN_PWR_FAULT | DS_BACKUP_PWR_FAULT;

    // ==================== 全局变量 ====================
    private static String host = "127.0.0.1";
    private static int port = 8888;
    private static byte[] deviceAddr;
    private static byte[] centerAddr;
    private static int serial = 1;

    // ==================== 编码 ====================

    private static byte[] buildPacket(int serial, byte[] srcAddr, byte[] dstAddr,
                                       byte command, byte[] appData) {
        if (appData == null) appData = new byte[0];
        int dataLen = appData.length;
        int totalLen = 30 + dataLen;

        ByteBuffer buf = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN);

        buf.put(START);
        buf.put(START);
        buf.putShort((short) serial);
        buf.put((byte) 1);
        buf.put((byte) 0);
        LocalDateTime now = LocalDateTime.now();
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
        LocalDateTime now = LocalDateTime.now();
        return new byte[]{
                (byte) now.getSecond(), (byte) now.getMinute(), (byte) now.getHour(),
                (byte) now.getDayOfMonth(), (byte) now.getMonthValue(), (byte) (now.getYear() - 2000)
        };
    }

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
            try {
                byte[] raw = desc.getBytes("GB18030");
                System.arraycopy(raw, 0, descBytes, 0, Math.min(raw.length, 31));
            } catch (Exception ignored) {}
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

    // ==================== 解码 ====================

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
        byte[] header = readFully(in, 24);
        int dataLength = (header[22] & 0xFF) | ((header[23] & 0xFF) << 8);
        byte[] rest = readFully(in, dataLength + 4);
        byte[] packet = new byte[30 + dataLength];
        packet[0] = START;
        packet[1] = START;
        System.arraycopy(header, 0, packet, 2, 24);
        System.arraycopy(rest, 0, packet, 26, rest.length);
        return packet;
    }

    private static byte getCommand(byte[] packet) {
        return packet[26];
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

    // ==================== 发送方法 ====================

    private static int sendAndConfirm(OutputStream out, InputStream in,
                                      String label, byte[] appData) throws Exception {
        System.out.println(label);
        byte[] pkt = buildPacket(serial++, deviceAddr, centerAddr, CMD_SEND, appData);
        out.write(pkt);
        out.flush();

        try {
            socketSetTimeout(in, 2000);
            byte[] ack = readPacket(in);
            System.out.printf("    <<< 收到确认: 命令=%d%n", getCommand(ack));
        } catch (Exception e) {
            System.out.println("    <<< 未收到确认: " + e.getMessage());
        }
        return serial;
    }

    private static void socketSetTimeout(InputStream in, int ms) throws Exception {
        // 简化处理
    }

    // ==================== 测试场景 ====================

    private static void testFireAlarm(OutputStream out, InputStream in) throws Exception {
        System.out.println("\n========== [火警测试] ==========");
        serial = sendAndConfirm(out, in, ">>> 装置火警上报 (Type 21)",
                buildDeviceStatusData(DEV_FIRE_ALARM));
        // Thread.sleep(300);
        // serial = sendAndConfirm(out, in, ">>> 系统火警上报 (Type 1)",
        //         buildSystemStatusData(1, 1, SYS_FIRE_ALARM));
        // Thread.sleep(300);
        serial = sendAndConfirm(out, in, ">>> 部件火警上报 (Type 2)",
                buildComponentStatusData(1, 1, 10, 0x01020001,
                        (short) (S_NORMAL | S_FIRE_ALARM), "1F-01烟感探测器"));
    }

    private static void testFault(OutputStream out, InputStream in) throws Exception {
        System.out.println("\n========== [故障测试] ==========");
        // serial = sendAndConfirm(out, in, ">>> 系统故障上报 (Type 1)",
        //         buildSystemStatusData(1, 1, SYS_FAULT));
        // Thread.sleep(300);
        serial = sendAndConfirm(out, in, ">>> 部件故障上报 (Type 2)",
                buildComponentStatusData(1, 1, 10, 0x01020001,
                        (short) (S_NORMAL | S_FAULT), "1F-01烟感探测器"));
    }

    private static void testPowerFault(OutputStream out, InputStream in) throws Exception {
        System.out.println("\n========== [电源故障测试] ==========");
        // serial = sendAndConfirm(out, in, ">>> 系统状态上报 (Type 1)",
        //         buildSystemStatusData(1, 1, SYS_NORMAL));
        // Thread.sleep(300);
        serial = sendAndConfirm(out, in, ">>> 部件电源故障上报 (Type 2)",
                buildComponentStatusData(1, 1, 10, 0x01020001,
                        (short) (S_NORMAL | S_MAIN_PWR_FAULT), "1F-01烟感探测器"));
        // Thread.sleep(300);
        // serial = sendAndConfirm(out, in, ">>> 装置电源故障上报 (Type 21)",
        //         buildDeviceStatusData(DEV_PWR_FAULT));
    }

    private static void testNormal(OutputStream out, InputStream in) throws Exception {
        System.out.println("\n========== [恢复正常] ==========");
        serial = sendAndConfirm(out, in, ">>> 装置恢复正常 (Type 21)",
                buildDeviceStatusData(DEV_NORMAL));
        Thread.sleep(300);
        serial = sendAndConfirm(out, in, ">>> 系统恢复正常 (Type 1)",
                buildSystemStatusData(1, 1, SYS_NORMAL));
        Thread.sleep(300);
        serial = sendAndConfirm(out, in, ">>> 部件恢复正常 (Type 2)",
                buildComponentStatusData(1, 1, 10, 0x01020001, S_NORMAL, "1F-01烟感探测器"));
    }

    private static void testAnalog(OutputStream out, InputStream in) throws Exception {
        System.out.println("\n========== [模拟量上报] ==========");
        int temp = 250 + (int) (Math.random() * 100);
        serial = sendAndConfirm(out, in, String.format(">>> 温度模拟量 (%.1f℃)", temp * 0.1),
                buildAnalogValueData(1, 1, 10, 0x01020001, 3, (short) temp));
        Thread.sleep(300);
        int pressure = 10 + (int) (Math.random() * 50);
        serial = sendAndConfirm(out, in, String.format(">>> 压力模拟量 (%.1fMPa)", pressure * 0.1),
                buildAnalogValueData(1, 1, 10, 0x01020001, 4, (short) pressure));
    }

    private static void testAll(OutputStream out, InputStream in) throws Exception {
        testNormal(out, in);
        Thread.sleep(1000);
        testFireAlarm(out, in);
        Thread.sleep(1000);
        testNormal(out, in);
        Thread.sleep(1000);
        testFault(out, in);
        Thread.sleep(1000);
        testNormal(out, in);
        Thread.sleep(1000);
        testPowerFault(out, in);
        Thread.sleep(1000);
        testNormal(out, in);
        System.out.println("\n========== [全部测试完成] ==========");
    }

    // ==================== 配置方法 ====================

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

        // 模拟设备：海康国标用传设备，productKey=7eHb4fKeSaDn4a3K，设备唯一标识=000000000000
        System.out.print("  设备地址(12位hex) [000000000000]: ");
        input = reader.readLine().trim();
        deviceAddr = hexToAddr(input.isEmpty() ? "000000000000" : input);

        System.out.print("  中心地址(12位hex) [020000000001]: ");
        input = reader.readLine().trim();
        centerAddr = hexToAddr(input.isEmpty() ? "020000000001" : input);

        System.out.printf("  配置完成: %s:%d, 设备=%s%n", host, port, bytesToHex(deviceAddr));
    }

    // ==================== 主流程 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║     GB/T 26875.3-2011 协议测试客户端 (交互式)     ║");
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

            System.out.println("\n--- 设备注册 ---");
            serial = sendAndConfirm(out, in, ">>> 装置状态注册 (Type 21)",
                    buildDeviceStatusData(DEV_NORMAL));

            while (true) {
                System.out.println();
                System.out.println("╔══════════════════════════════════════╗");
                System.out.println("║           请选择操作                  ║");
                System.out.println("╠══════════════════════════════════════╣");
                System.out.println("║  1. 火警测试                        ║");
                System.out.println("║  2. 故障测试                        ║");
                System.out.println("║  3. 电源故障测试                    ║");
                System.out.println("║  4. 恢复正常                        ║");
                System.out.println("║  5. 模拟量上报                      ║");
                System.out.println("║  6. 全部测试流程                    ║");
                System.out.println("║  0. 重新配置连接                    ║");
                System.out.println("║  Q. 退出                            ║");
                System.out.println("╚══════════════════════════════════════╝");
                System.out.print("请输入选项: ");

                String choice = reader.readLine().trim().toUpperCase();
                if (choice.isEmpty()) continue;

                try {
                    switch (choice) {
                        case "1":
                            testFireAlarm(out, in);
                            break;
                        case "2":
                            testFault(out, in);
                            break;
                        case "3":
                            testPowerFault(out, in);
                            break;
                        case "4":
                            testNormal(out, in);
                            break;
                        case "5":
                            testAnalog(out, in);
                            break;
                        case "6":
                            testAll(out, in);
                            break;
                        case "0":
                            configConnection(reader);
                            break;
                        case "Q":
                        case "EXIT":
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
