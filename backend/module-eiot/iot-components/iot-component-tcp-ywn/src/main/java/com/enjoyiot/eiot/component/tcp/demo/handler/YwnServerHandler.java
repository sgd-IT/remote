package com.enjoyiot.eiot.component.tcp.demo.handler;

import com.enjoyiot.eiot.component.tcp.demo.listener.YwnEventListener;
import com.enjoyiot.eiot.component.tcp.demo.model.YwnCommand;
import com.enjoyiot.eiot.component.tcp.demo.model.YwnDeviceInfo;
import com.enjoyiot.eiot.component.tcp.demo.model.YwnEvent;
import com.enjoyiot.eiot.component.tcp.demo.model.YwnFrame;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 控制器监控TCP协议V27 — 服务端消息处理器
 *
 * 处理逻辑:
 * 1. 事件上传(0x80) → 解析事件 → 回调onEvent → 自动回复ACK
 * 2. 设备信息(0x81) → 解析设备信息 → 回调onDeviceInfo → 自动回复ACK
 * 3. 心跳(0x82) → 回调onHeartbeat → 自动回复心跳ACK(0x83)
 * 4. 控制器故障(0x86) → 回调onControllerFault
 * 5. 控制器故障恢复(0x87) → 回调onControllerFaultRecovery
 * 6. ACK(0x01) → 回调onControlAck
 * 7. 空闲超时(2分钟无数据) → 触发控制器故障逻辑
 */
public class YwnServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(YwnServerHandler.class);

    /** 设备编号 → Channel 映射 */
    private static final ConcurrentHashMap<Integer, Channel> deviceChannelMap = new ConcurrentHashMap<>();

    /** Channel → 设备编号映射 */
    private static final ConcurrentHashMap<String, Integer> channelDeviceMap = new ConcurrentHashMap<>();

    /** Channel → 最后活跃时间 */
    private static final ConcurrentHashMap<String, LocalDateTime> channelActiveTime = new ConcurrentHashMap<>();

    private final YwnEventListener listener;

    public YwnServerHandler(YwnEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("设备连接: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asLongText();
        Integer deviceNumber = channelDeviceMap.remove(channelId);
        if (deviceNumber != null) {
            deviceChannelMap.remove(deviceNumber);
            log.warn("设备断开: dev={}, channel={}", deviceNumber, ctx.channel().remoteAddress());
            if (listener != null) {
                listener.onDeviceDisconnected(deviceNumber);
            }
        }
        channelActiveTime.remove(channelId);
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof byte[] frameData)) {
            return;
        }

        YwnFrame frame;
        try {
            frame = YwnFrame.fromBytes(frameData);
        } catch (Exception e) {
            log.error("帧解析失败: {}", e.getMessage());
            if (listener != null) {
                listener.onUnknownFrame(frameData);
            }
            return;
        }

        // 更新活跃时间
        String channelId = ctx.channel().id().asLongText();
        channelActiveTime.put(channelId, LocalDateTime.now());

        int deviceNumber = frame.getDeviceNumber();
        YwnCommand cmd = frame.getCommand();

        // 注册设备映射(首次收到该设备编号时)
        if (cmd == YwnCommand.DEVICE_INFO_UPLOAD || cmd == YwnCommand.HEARTBEAT || cmd == YwnCommand.EVENT_UPLOAD) {
            registerDevice(deviceNumber, ctx.channel());
        }

        log.info("收到帧: cmd={}, dev={}, hex={}", cmd, deviceNumber, frame.toHexString());

        switch (cmd) {
            case EVENT_UPLOAD -> handleEvent(ctx, frame);
            case DEVICE_INFO_UPLOAD -> handleDeviceInfo(ctx, frame);
            case HEARTBEAT -> handleHeartbeat(ctx, frame);
            case ACK -> handleAck(ctx, frame);
            case CONTROLLER_FAULT -> handleControllerFault(frame);
            case CONTROLLER_FAULT_RECOVERY -> handleControllerFaultRecovery(frame);
            default -> log.warn("服务端不处理命令: {}", cmd);
        }
    }

    /** 处理事件上传 (0x80) */
    private void handleEvent(ChannelHandlerContext ctx, YwnFrame frame) {
        YwnEvent event = frame.parseEvent();
        if (event == null) {
            log.error("事件数据解析失败: dev={}", frame.getDeviceNumber());
            return;
        }

        log.info("事件上报: dev={}, event={}", frame.getDeviceNumber(), event);

        // 火警立即高亮
        if (event.isFireAlarm()) {
            log.warn("🔥 火警! dev={}, machine={}, loop={}, addr={}, event={}",
                    frame.getDeviceNumber(), event.getMachineNo(), event.getLoopNo(),
                    event.getAddressNo(), event.getEventCodeName());
        }

        if (listener != null) {
            listener.onEvent(frame.getDeviceNumber(), event);
        }

        // 自动回复ACK
        sendFrame(ctx, YwnFrame.ack(frame.getDeviceNumber()));
    }

    /** 处理设备信息上传 (0x81) */
    private void handleDeviceInfo(ChannelHandlerContext ctx, YwnFrame frame) {
        YwnDeviceInfo deviceInfo = YwnDeviceInfo.fromFrameBytes(frame.toBytes());
        log.info("设备信息: {}", deviceInfo);

        if (listener != null) {
            listener.onDeviceInfo(deviceInfo);
        }

        // 自动回复ACK
        sendFrame(ctx, YwnFrame.ack(frame.getDeviceNumber()));
    }

    /** 处理心跳 (0x82) */
    private void handleHeartbeat(ChannelHandlerContext ctx, YwnFrame frame) {
        log.debug("心跳: dev={}", frame.getDeviceNumber());

        if (listener != null) {
            listener.onHeartbeat(frame.getDeviceNumber());
        }

        // 回复心跳应答 (0x83)
        sendFrame(ctx, YwnFrame.heartbeatAck(frame.getDeviceNumber()));
    }

    /** 处理ACK (0x01) */
    private void handleAck(ChannelHandlerContext ctx, YwnFrame frame) {
        log.debug("ACK: dev={}", frame.getDeviceNumber());
        if (listener != null) {
            listener.onControlAck(frame.getDeviceNumber(), null);
        }
    }

    /** 处理控制器故障 (0x86) */
    private void handleControllerFault(YwnFrame frame) {
        log.warn("⚠ 控制器故障: dev={}", frame.getDeviceNumber());
        if (listener != null) {
            listener.onControllerFault(frame.getDeviceNumber());
        }
    }

    /** 处理控制器故障恢复 (0x87) */
    private void handleControllerFaultRecovery(YwnFrame frame) {
        log.info("控制器故障恢复: dev={}", frame.getDeviceNumber());
        if (listener != null) {
            listener.onControllerFaultRecovery(frame.getDeviceNumber());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            if (idleEvent.state() == IdleState.READER_IDLE) {
                String channelId = ctx.channel().id().asLongText();
                Integer deviceNumber = channelDeviceMap.get(channelId);
                log.warn("读空闲超时(2分钟无数据), 视为控制器故障: dev={}", deviceNumber);

                if (deviceNumber != null && listener != null) {
                    listener.onControllerFault(deviceNumber);
                }
                // 不关闭连接, 等待恢复
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("通道异常: {}", cause.getMessage(), cause);
        ctx.close();
    }

    // ========== 设备注册与帧发送 ==========

    private void registerDevice(int deviceNumber, Channel channel) {
        String channelId = channel.id().asLongText();
        Channel oldChannel = deviceChannelMap.put(deviceNumber, channel);
        channelDeviceMap.put(channelId, deviceNumber);

        if (oldChannel != null && oldChannel != channel && oldChannel.isActive()) {
            log.warn("设备{}重复连接, 关闭旧连接", deviceNumber);
            oldChannel.close();
        }

        if (listener != null) {
            listener.onDeviceConnected(deviceNumber);
        }
    }

    private void sendFrame(ChannelHandlerContext ctx, YwnFrame frame) {
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(frame);
            log.debug("发送帧: cmd={}, dev={}, hex={}",
                    frame.getCommand(), frame.getDeviceNumber(), frame.toHexString());
        }
    }

    // ========== 静态方法: 向指定设备发送控制命令 ==========

    /**
     * 向指定设备发送控制帧
     * @param deviceNumber 设备编号
     * @param frame 控制帧
     * @return 是否发送成功
     */
    public static boolean sendControl(int deviceNumber, YwnFrame frame) {
        Channel channel = deviceChannelMap.get(deviceNumber);
        if (channel == null || !channel.isActive()) {
            log.warn("设备{}不在线, 无法发送控制命令", deviceNumber);
            return false;
        }
        channel.writeAndFlush(frame);
        log.info("下发控制: dev={}, cmd={}, hex={}",
                deviceNumber, frame.getCommand(), frame.toHexString());
        return true;
    }

    /** 获取在线设备数量 */
    public static int getOnlineDeviceCount() {
        return deviceChannelMap.size();
    }

    /** 判断设备是否在线 */
    public static boolean isDeviceOnline(int deviceNumber) {
        Channel ch = deviceChannelMap.get(deviceNumber);
        return ch != null && ch.isActive();
    }
}
