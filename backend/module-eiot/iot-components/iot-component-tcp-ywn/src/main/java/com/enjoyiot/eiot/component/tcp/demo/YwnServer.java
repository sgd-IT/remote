package com.enjoyiot.eiot.component.tcp.demo;

import com.enjoyiot.eiot.component.tcp.demo.codec.YwnFrameDecoder;
import com.enjoyiot.eiot.component.tcp.demo.codec.YwnFrameEncoder;
import com.enjoyiot.eiot.component.tcp.demo.handler.YwnServerHandler;
import com.enjoyiot.eiot.component.tcp.demo.listener.YwnEventListener;
import com.enjoyiot.eiot.component.tcp.demo.model.YwnDeviceInfo;
import com.enjoyiot.eiot.component.tcp.demo.model.YwnEvent;
import com.enjoyiot.eiot.component.tcp.demo.model.YwnFrame;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * 控制器监控TCP协议V27 — 服务端
 *
 * 监听远控模块连接，按协议V27解析二进制帧:
 * - 帧格式: 命令码(1B) + 设备编号(2B LE) + 载荷(变长)
 * - 无长度前缀，帧长度由命令码决定
 * - 心跳周期3分钟，2分钟无数据触发控制器故障
 *
 * 默认端口8839(对齐YWN平台)
 *
 * 交互式命令行:
 *   格式: 命令 设备号 [机号 [回路号 地址号]]
 *   命令列表:
 *     reset    <设备号> <机号>                     - 设备复位
 *     silence  <设备号> <机号>                     - 消音
 *     start    <设备号> <机号> <回路号> <地址号>    - 启动总线设备
 *     stop     <设备号> <机号> <回路号> <地址号>    - 停止总线设备
 *     manual   <设备号> <机号>                     - 切换手动模式
 *     auto     <设备号> <机号>                     - 切换自动模式
 *     shield   <设备号> <机号> <回路号> <地址号>    - 屏蔽
 *     unshield <设备号> <机号> <回路号> <地址号>    - 取消屏蔽
 *     ml-start <设备号> <机号> <盘号> <路号>       - 多线盘启动
 *     ml-stop  <设备号> <机号> <盘号> <路号>       - 多线盘停止
 *     auto-on  <设备号> <机号>                     - 自动允许(利达)
 *     auto-off <设备号> <机号>                     - 自动禁止(利达)
 *     manual-on  <设备号> <机号>                   - 手动允许(利达/海湾)
 *     manual-off <设备号> <机号>                   - 手动禁止(利达/海湾)
 *     list     - 查看在线设备
 *     online   <设备号>  - 检查设备是否在线
 *     help     - 帮助
 *     quit     - 退出
 */
public class YwnServer {

    private static final Logger log = LoggerFactory.getLogger(YwnServer.class);

    private static final int DEFAULT_PORT = 8839;
    private static final int READER_IDLE_SECONDS = 120;  // 2分钟无数据 → 控制器故障

    private final int port;
    private final YwnEventListener listener;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public YwnServer() {
        this(DEFAULT_PORT, null);
    }

    public YwnServer(int port, YwnEventListener listener) {
        this.port = port;
        this.listener = listener;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 空闲检测: 2分钟无读数据 → 触发控制器故障
                        pipeline.addLast("idleState", new IdleStateHandler(
                                READER_IDLE_SECONDS, 0, 0, TimeUnit.SECONDS));

                        // 帧解码器
                        pipeline.addLast("frameDecoder", new YwnFrameDecoder());

                        // 帧编码器
                        pipeline.addLast("frameEncoder", new YwnFrameEncoder());

                        // 业务处理器
                        pipeline.addLast("serverHandler", new YwnServerHandler(listener));
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();

        log.info("============================================");
        log.info("  YWN控制器监控TCP服务端 V27 启动成功");
        log.info("  监听端口: {}", port);
        log.info("  心跳超时: {}秒 (控制器故障判定)", READER_IDLE_SECONDS);
        log.info("  协议特点: 纯二进制帧, 无长度前缀");
        log.info("============================================");
    }

    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("YWN服务端已停止");
    }

    /**
     * 向指定设备发送控制命令
     */
    public static boolean sendControl(int deviceNumber, YwnFrame frame) {
        return YwnServerHandler.sendControl(deviceNumber, frame);
    }

    /**
     * 获取在线设备数量
     */
    public static int getOnlineDeviceCount() {
        return YwnServerHandler.getOnlineDeviceCount();
    }

    /**
     * 判断设备是否在线
     */
    public static boolean isDeviceOnline(int deviceNumber) {
        return YwnServerHandler.isDeviceOnline(deviceNumber);
    }

    // ========== 交互式控制台 ==========

    private static void printHelp() {
        System.out.println("""
        
        ══════════════════════════════════════════════════════
          YWN V27 服务端 — 下发控制命令
        ══════════════════════════════════════════════════════
          格式: 命令 设备号 [机号 [回路号 地址号]]
        
          基础控制:
            reset    <设备号> <机号>                    设备复位
            silence  <设备号> <机号>                    消音
            start    <设备号> <机号> <回路号> <地址号>   启动总线设备
            stop     <设备号> <机号> <回路号> <地址号>   停止总线设备
            shield   <设备号> <机号> <回路号> <地址号>   屏蔽
            unshield <设备号> <机号> <回路号> <地址号>   取消屏蔽
        
          手自动转换:
            manual   <设备号> <机号>                    切换手动模式
            auto     <设备号> <机号>                    切换自动模式
        
          多线盘:
            ml-start <设备号> <机号> <盘号> <路号>      多线盘启动
            ml-stop  <设备号> <机号> <盘号> <路号>      多线盘停止
        
          利达/海湾专用:
            auto-on    <设备号> <机号>                  自动允许
            auto-off   <设备号> <机号>                  自动禁止
            manual-on  <设备号> <机号>                  手动允许
            manual-off <设备号> <机号>                  手动禁止
        
          状态查询:
            list     查看在线设备数
            online   <设备号>                          检查设备是否在线
        
          其他:
            help     显示帮助
            quit     退出服务端
        ══════════════════════════════════════════════════════
        """);
    }

    /** 解析并执行控制命令 */
    private static boolean executeCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) return true;

        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                // ---- 只需设备号+机号的命令 ----
                case "reset" -> {
                    if (parts.length < 3) { System.out.println("用法: reset <设备号> <机号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    sendAndPrint(dev, "复位", YwnFrame.reset(dev, machine));
                }
                case "silence" -> {
                    if (parts.length < 3) { System.out.println("用法: silence <设备号> <机号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    sendAndPrint(dev, "消音", YwnFrame.silence(dev, machine));
                }
                case "manual" -> {
                    if (parts.length < 3) { System.out.println("用法: manual <设备号> <机号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    sendAndPrint(dev, "手动模式", YwnFrame.manualAutoSwitch(dev, machine, 2));
                }
                case "auto" -> {
                    if (parts.length < 3) { System.out.println("用法: auto <设备号> <机号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    sendAndPrint(dev, "自动模式", YwnFrame.manualAutoSwitch(dev, machine, 3));
                }
                case "auto-on" -> {
                    if (parts.length < 3) { System.out.println("用法: auto-on <设备号> <机号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    sendAndPrint(dev, "自动允许", YwnFrame.autoAllow(dev, machine));
                }
                case "auto-off" -> {
                    if (parts.length < 3) { System.out.println("用法: auto-off <设备号> <机号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    sendAndPrint(dev, "自动禁止", YwnFrame.autoProhibit(dev, machine));
                }
                case "manual-on" -> {
                    if (parts.length < 3) { System.out.println("用法: manual-on <设备号> <机号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    sendAndPrint(dev, "手动允许", YwnFrame.manualAllow(dev, machine));
                }
                case "manual-off" -> {
                    if (parts.length < 3) { System.out.println("用法: manual-off <设备号> <机号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    sendAndPrint(dev, "手动禁止", YwnFrame.manualProhibit(dev, machine));
                }

                // ---- 需要设备号+机号+回路号+地址号的命令 ----
                case "start" -> {
                    if (parts.length < 5) { System.out.println("用法: start <设备号> <机号> <回路号> <地址号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    int loop = Integer.parseInt(parts[3]);
                    int addr = Integer.parseInt(parts[4]);
                    sendAndPrint(dev, "启动总线", YwnFrame.busStart(dev, machine, loop, addr));
                }
                case "stop" -> {
                    if (parts.length < 5) { System.out.println("用法: stop <设备号> <机号> <回路号> <地址号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    int loop = Integer.parseInt(parts[3]);
                    int addr = Integer.parseInt(parts[4]);
                    sendAndPrint(dev, "停止总线", YwnFrame.busStop(dev, machine, loop, addr));
                }
                case "shield" -> {
                    if (parts.length < 5) { System.out.println("用法: shield <设备号> <机号> <回路号> <地址号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    int loop = Integer.parseInt(parts[3]);
                    int addr = Integer.parseInt(parts[4]);
                    sendAndPrint(dev, "屏蔽", YwnFrame.shield(dev, machine, loop, addr));
                }
                case "unshield" -> {
                    if (parts.length < 5) { System.out.println("用法: unshield <设备号> <机号> <回路号> <地址号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    int loop = Integer.parseInt(parts[3]);
                    int addr = Integer.parseInt(parts[4]);
                    sendAndPrint(dev, "取消屏蔽", YwnFrame.cancelShield(dev, machine, loop, addr));
                }

                // ---- 多线盘: 设备号+机号+盘号+路号 ----
                case "ml-start" -> {
                    if (parts.length < 5) { System.out.println("用法: ml-start <设备号> <机号> <盘号> <路号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    int disk = Integer.parseInt(parts[3]);
                    int road = Integer.parseInt(parts[4]);
                    sendAndPrint(dev, "多线盘启动", YwnFrame.multiLineStart(dev, machine, disk, road));
                }
                case "ml-stop" -> {
                    if (parts.length < 5) { System.out.println("用法: ml-stop <设备号> <机号> <盘号> <路号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    int machine = Integer.parseInt(parts[2]);
                    int disk = Integer.parseInt(parts[3]);
                    int road = Integer.parseInt(parts[4]);
                    sendAndPrint(dev, "多线盘停止", YwnFrame.multiLineStop(dev, machine, disk, road));
                }

                // ---- 状态查询 ----
                case "list" -> {
                    int count = getOnlineDeviceCount();
                    System.out.printf("在线设备数: %d%n", count);
                }
                case "online" -> {
                    if (parts.length < 2) { System.out.println("用法: online <设备号>"); break; }
                    int dev = Integer.parseInt(parts[1]);
                    System.out.printf("设备 %d %s%n", dev, isDeviceOnline(dev) ? "✅ 在线" : "❌ 离线");
                }

                // ---- 其他 ----
                case "help" -> printHelp();
                case "quit", "exit" -> {
                    return false;
                }
                default -> System.out.println("未知命令: " + cmd + "  输入 help 查看帮助");
            }
        } catch (NumberFormatException e) {
            System.out.println("参数格式错误，需要整数: " + e.getMessage());
        }
        return true;
    }

    /** 发送控制帧并打印结果 */
    private static void sendAndPrint(int deviceNumber, String action, YwnFrame frame) {
        if (!isDeviceOnline(deviceNumber)) {
            System.out.printf("❌ 设备 %d 不在线，无法发送【%s】%n", deviceNumber, action);
            return;
        }
        boolean ok = sendControl(deviceNumber, frame);
        if (ok) {
            System.out.printf("✅ 下发成功【%s】设备=%d 帧=%s%n", action, deviceNumber, frame.toHexString());
        } else {
            System.out.printf("❌ 下发失败【%s】设备=%d%n", action, deviceNumber);
        }
    }

    // ========== 启动入口 ==========

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException e) { /* 使用默认 */ }
        }

        YwnServer server = new YwnServer(port, new YwnEventListener() {
            @Override
            public void onDeviceConnected(int deviceNumber) {
                System.out.printf("🟢 [设备上线] 设备%d 已连接%n", deviceNumber);
            }

            @Override
            public void onDeviceDisconnected(int deviceNumber) {
                System.out.printf("🔴 [设备下线] 设备%d 已断开%n", deviceNumber);
            }

            @Override
            public void onEvent(int deviceNumber, YwnEvent event) {
                if (event.isFireAlarm()) {
                    System.out.printf("🔥 [火警] 设备%d %s 机号%d 回路%d 地址%d%n",
                            deviceNumber, event.getEventCodeName(),
                            event.getMachineNo(), event.getLoopNo(), event.getAddressNo());
                } else if (event.isFault()) {
                    System.out.printf("⚠ [故障] 设备%d %s 机号%d 回路%d 地址%d%n",
                            deviceNumber, event.getEventCodeName(),
                            event.getMachineNo(), event.getLoopNo(), event.getAddressNo());
                } else {
                    System.out.printf("[事件] 设备%d %s%n", deviceNumber, event);
                }
            }

            @Override
            public void onDeviceInfo(YwnDeviceInfo deviceInfo) {
                System.out.printf("[设备信息] %s%n", deviceInfo);
            }

            @Override
            public void onControllerFault(int deviceNumber) {
                System.out.printf("🔴 [控制器故障] 设备%d 2分钟无数据%n", deviceNumber);
            }

            @Override
            public void onControllerFaultRecovery(int deviceNumber) {
                System.out.printf("🟢 [控制器恢复] 设备%d%n", deviceNumber);
            }
        });

        // 优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n正在关闭服务端...");
            server.stop();
        }));

        try {
            server.start();

            // 在新线程中等待服务端Channel关闭
            Thread serverThread = new Thread(() -> {
                try {
                    server.serverChannel.closeFuture().sync();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "server-wait");
            serverThread.setDaemon(true);
            serverThread.start();

            // 主线程进入交互式命令行
            printHelp();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            boolean running = true;
            while (running) {
                System.out.print("ywn> ");
                System.out.flush();
                String line = reader.readLine();
                if (line == null || line.trim().isEmpty()) continue;
                running = executeCommand(line.trim());
            }

            server.stop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            server.stop();
        }
    }
}
