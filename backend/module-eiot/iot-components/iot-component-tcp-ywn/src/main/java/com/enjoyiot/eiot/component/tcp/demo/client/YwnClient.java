package com.enjoyiot.eiot.component.tcp.demo.client;

import com.enjoyiot.eiot.component.tcp.demo.codec.YwnFrameDecoder;
import com.enjoyiot.eiot.component.tcp.demo.codec.YwnFrameEncoder;
import com.enjoyiot.eiot.component.tcp.demo.model.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 控制器监控TCP协议V27 — 测试客户端
 *
 * 模拟远控模块(YWN网关)连接服务端，支持:
 * - 设备信息上报 (0x81)
 * - 心跳发送 (0x82, 3分钟周期)
 * - 事件上报 (0x80): 火警/故障/反馈/恢复
 * - 控制命令ACK (0x01)
 * - 控制器故障/恢复 (0x86/0x87)
 */
public class YwnClient {

    private static final Logger log = LoggerFactory.getLogger(YwnClient.class);

    private static final String DEFAULT_HOST = "**";
    private static final int DEFAULT_PORT = 8839;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 180; // 3分钟
    private static final int WRITER_IDLE_SECONDS = 30;

    private final String host;
    private final int port;
    private final int deviceNumber;
    private EventLoopGroup group;
    private Channel channel;

    public YwnClient(String host, int port, int deviceNumber) {
        this.host = host;
        this.port = port;
        this.deviceNumber = deviceNumber;
    }

    public void connect() throws InterruptedException {
        group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();

                    // 写空闲30秒 → 自动发心跳
                    pipeline.addLast("idleState", new IdleStateHandler(
                        0, WRITER_IDLE_SECONDS, 0, TimeUnit.SECONDS));

                    pipeline.addLast("frameDecoder", new YwnFrameDecoder());
                    pipeline.addLast("frameEncoder", new YwnFrameEncoder());

                    // 客户端消息处理
                    pipeline.addLast("clientHandler", new SimpleChannelInboundHandler<byte[]>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) {
                            YwnFrame frame = YwnFrame.fromBytes(msg);
                            handleServerCommand(ctx, frame);
                        }

                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                            if (evt instanceof IdleStateEvent idleEvent) {
                                if (idleEvent.state() == IdleState.WRITER_IDLE) {
                                    // 自动发心跳
                                    sendHeartbeat();
                                }
                            }
                            super.userEventTriggered(ctx, evt);
                        }
                    });
                }
            });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();

        log.info("客户端已连接: {}:{}, 设备编号={}", host, port, deviceNumber);

        // 连接成功后立即上报设备信息
        sendDeviceInfo();
    }

    /** 处理服务端下发的控制命令 */
    private void handleServerCommand(ChannelHandlerContext ctx, YwnFrame frame) {
        YwnCommand cmd = frame.getCommand();
        log.info("收到服务端命令: cmd={}, dev={}, hex={}", cmd, frame.getDeviceNumber(), frame.toHexString());

        switch (cmd) {
            case ACK, HEARTBEAT_ACK -> log.debug("收到应答: {}", cmd);

            case RESET -> {
                log.info("📋 执行复位: 机号={}", frame.getMachineNo());
                sendAck();
            }
            case SILENCE -> {
                log.info("🔇 执行消音: 机号={}", frame.getMachineNo());
                sendAck();
            }
            case BUS_START -> {
                log.info("▶ 启动总线设备: 机号={}, 回路={}, 地址={}",
                    frame.getMachineNo(), frame.getLoopNo(), frame.getAddressNo());
                sendAck();
            }
            case BUS_STOP -> {
                log.info("⏹ 停止总线设备: 机号={}, 回路={}, 地址={}",
                    frame.getMachineNo(), frame.getLoopNo(), frame.getAddressNo());
                sendAck();
            }
            case MANUAL_AUTO_SWITCH -> {
                String mode = frame.getManualAutoMode() == 2 ? "手动" : "自动";
                log.info("🔄 手自动转换: 机号={}, 模式={}", frame.getMachineNo(), mode);
                sendAck();
            }
            case SHIELD -> {
                log.info("🚫 屏蔽: 机号={}, 回路={}, 地址={}",
                    frame.getMachineNo(), frame.getLoopNo(), frame.getAddressNo());
                sendAck();
            }
            case CANCEL_SHIELD -> {
                log.info("✅ 取消屏蔽: 机号={}, 回路={}, 地址={}",
                    frame.getMachineNo(), frame.getLoopNo(), frame.getAddressNo());
                sendAck();
            }
            default -> log.warn("未处理命令: {}", cmd);
        }
    }

    // ========== 发送方法 ==========

    /** 发送设备信息上报 (0x81) */
    public void sendDeviceInfo() {
        YwnDeviceInfo info = new YwnDeviceInfo();
        info.setDeviceNumber(deviceNumber);
        info.setIccid("89860000000000000000");
        info.setImei("000000000000000");
        info.setHardwareVersion("V2.7");
        info.setSoftwareVersion("V1.0");
        channel.writeAndFlush(YwnFrame.deviceInfoUpload(info));
        log.info("📤 上报设备信息: {}", info);
    }

    /** 发送心跳 (0x82) */
    public void sendHeartbeat() {
        channel.writeAndFlush(YwnFrame.heartbeat(deviceNumber));
        log.debug("💓 发送心跳: dev={}", deviceNumber);
    }

    /** 发送ACK (0x01) */
    public void sendAck() {
        channel.writeAndFlush(YwnFrame.ack(deviceNumber));
    }

    /** 上报火警事件 */
    public void sendFireAlarm(int machineNo, int loopNo, int addressNo) {
        YwnEvent event = new YwnEvent();
        // 通过反射或setter设置字段后发送
        sendEvent(0x01, machineNo, loopNo, addressNo, "火警");
    }

    /** 上报故障事件 */
    public void sendFault(int machineNo, int loopNo, int addressNo) {
        sendEvent(0x50, machineNo, loopNo, addressNo, "总线设备故障");
    }

    /** 上报故障恢复 */
    public void sendFaultRecovery(int machineNo, int loopNo, int addressNo) {
        sendEvent(0x64, machineNo, loopNo, addressNo, "故障恢复");
    }

    /** 上报反馈 */
    public void sendFeedback(int machineNo, int loopNo, int addressNo) {
        sendEvent(0x1A, machineNo, loopNo, addressNo, "总线设备反馈");
    }

    /** 上报反馈撤销 */
    public void sendFeedbackCancel(int machineNo, int loopNo, int addressNo) {
        sendEvent(0x1C, machineNo, loopNo, addressNo, "反馈撤销");
    }

    /** 通用事件上报 */
    private void sendEvent(int eventCode, int machineNo, int loopNo, int addressNo, String description) {
        YwnEvent event = buildEvent(eventCode, machineNo, loopNo, addressNo, description);
        channel.writeAndFlush(YwnFrame.eventUpload(deviceNumber, event));
        log.info("📤 上报事件: code=0x{}, machine={}, loop={}, addr={}, desc={}",
            Integer.toHexString(eventCode), machineNo, loopNo, addressNo, description);
    }

    /** 构建事件对象 */
    private YwnEvent buildEvent(int eventCode, int machineNo, int loopNo, int addressNo, String description) {
        YwnEvent event = new YwnEvent();
        // 使用fromBytes的反向操作：先构建64字节数组再解析
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(64).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0);                          // 事件类型码
        buf.put((byte) 0);                          // 产品类型
        buf.put((byte) 0);                          // 通道类型
        buf.put((byte) 0);                          // 备用
        buf.putShort((short) eventCode);            // 事件代码
        buf.putShort((short) 0);                    // 设备类型
        buf.put((byte) machineNo);                  // 机号
        buf.put((byte) loopNo);                     // 回路
        buf.put((byte) addressNo);                  // 地址号
        buf.put((byte) 0);                          // 通道号
        buf.putFloat(-1);                           // 报警值浮点(-1不显示)
        buf.putShort((short) -1);                   // 报警值整型(-1不显示)
        LocalDateTime now = LocalDateTime.now();
        buf.put((byte) (now.getYear() - 2000));     // 年
        buf.put((byte) now.getMonthValue());        // 月
        buf.put((byte) now.getDayOfMonth());        // 日
        buf.put((byte) now.getHour());              // 时
        buf.put((byte) now.getMinute());            // 分
        buf.put((byte) now.getSecond());            // 秒
        // 说明文字 (40字节, GBK)
        if (description != null) {
            try {
                byte[] descBytes = description.getBytes("GBK");
                buf.put(descBytes, 0, Math.min(descBytes.length, 40));
            } catch (Exception e) {
                byte[] descBytes = description.getBytes();
                buf.put(descBytes, 0, Math.min(descBytes.length, 40));
            }
        }
        return YwnEvent.fromBytes(buf.array());
    }

    /** 上报控制器故障 (0x86) */
    public void sendControllerFault() {
        channel.writeAndFlush(YwnFrame.controllerFault(deviceNumber));
        log.info("📤 上报控制器故障: dev={}", deviceNumber);
    }

    /** 上报控制器故障恢复 (0x87) */
    public void sendControllerFaultRecovery() {
        channel.writeAndFlush(YwnFrame.controllerFaultRecovery(deviceNumber));
        log.info("📤 上报控制器故障恢复: dev={}", deviceNumber);
    }

    public void disconnect() {
        if (channel != null) channel.close();
        if (group != null) group.shutdownGracefully();
    }

    // ========== 交互式测试入口 ==========

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        int devNo = args.length > 2 ? Integer.parseInt(args[2]) : 3000;

        YwnClient client = new YwnClient(host, port, devNo);

        try {
            client.connect();

            Scanner scanner = new Scanner(System.in);
            System.out.println("\n===== YWN控制器监控 V27 测试客户端 =====");
            System.out.println("设备编号: " + devNo);
            System.out.println("命令列表:");
            System.out.println("  1  - 上报火警 (机1/回路1/地址1)");
            System.out.println("  2  - 上报故障 (机1/回路1/地址1)");
            System.out.println("  3  - 上报故障恢复");
            System.out.println("  4  - 上报反馈 (机1/回路1/地址2)");
            System.out.println("  5  - 上报反馈撤销");
            System.out.println("  6  - 上报设备信息");
            System.out.println("  7  - 发送心跳");
            System.out.println("  8  - 上报控制器故障");
            System.out.println("  9  - 上报控制器故障恢复");
            System.out.println("  10 - 自定义事件(code/machine/loop/addr)");
            System.out.println("  0  - 退出");
            System.out.println("=========================================\n");

            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                try {
                    int choice = Integer.parseInt(line);
                    switch (choice) {
                        case 0 -> { client.disconnect(); return; }
                        case 1 -> client.sendFireAlarm(1, 1, 1);
                        case 2 -> client.sendFault(1, 1, 1);
                        case 3 -> client.sendFaultRecovery(1, 1, 1);
                        case 4 -> client.sendFeedback(1, 1, 2);
                        case 5 -> client.sendFeedbackCancel(1, 1, 2);
                        case 6 -> client.sendDeviceInfo();
                        case 7 -> client.sendHeartbeat();
                        case 8 -> client.sendControllerFault();
                        case 9 -> client.sendControllerFaultRecovery();
                        case 10 -> {
                            System.out.print("  事件码(hex, 如01/50/64): ");
                            int code = Integer.parseInt(scanner.nextLine().trim(), 16);
                            System.out.print("  机号: ");
                            int m = Integer.parseInt(scanner.nextLine().trim());
                            System.out.print("  回路: ");
                            int l = Integer.parseInt(scanner.nextLine().trim());
                            System.out.print("  地址: ");
                            int a = Integer.parseInt(scanner.nextLine().trim());
                            client.sendEvent(code, m, l, a, "");
                        }
                        default -> System.out.println("未知命令");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("请输入数字");
                }
            }
        } catch (Exception e) {
            log.error("客户端异常", e);
        } finally {
            client.disconnect();
        }
    }
}
