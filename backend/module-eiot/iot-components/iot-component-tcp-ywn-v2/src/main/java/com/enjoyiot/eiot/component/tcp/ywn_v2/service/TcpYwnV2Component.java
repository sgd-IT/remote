package com.enjoyiot.eiot.component.tcp.ywn_v2.service;

import cn.hutool.core.util.IdUtil;
import com.enjoyiot.eiot.component.core.ComponentServices;
import com.enjoyiot.eiot.component.core.ThingComponent;
import com.enjoyiot.eiot.common.enums.DeviceState;
import com.enjoyiot.eiot.component.core.model.down.*;
import com.enjoyiot.eiot.component.core.model.up.*;
import com.enjoyiot.eiot.component.tcp.ywn_v2.cilent.VertxTcpYwnV2Client;
import com.enjoyiot.eiot.component.tcp.ywn_v2.model.TcpYwnV2Config;
import com.enjoyiot.eiot.component.tcp.ywn_v2.parser.*;
import com.enjoyiot.eiot.component.tcp.ywn_v2.protobuf.YwnV2Protocol;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.RegisterDevice;
import com.enjoyiot.module.eiot.api.product.ProductApi;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * YWN V2 协议组件 - 基于 TCP + Protobuf 的消防远控通信
 *
 * 协议帧格式: 数据长度(2B BE) + 消息类型(2B BE) + 序列号(4B BE) + Protobuf载荷
 * 消息类型: REGISTER(1) / HEARTBEAT(2) / EVENT(3) / CONTROL(4)
 * 所有消息均需确认(同消息类型 + 同序列号)
 */
@Slf4j
@Component
public class TcpYwnV2Component extends ThingComponent implements Handler<NetSocket> {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 网关编号(String) -> TCP客户端 */
    private final Map<String, VertxTcpYwnV2Client> clientMap = new ConcurrentHashMap<>();

    /** 网关编号(String) -> productKey */
    private final Map<String, String> gatewayToPk = new ConcurrentHashMap<>();

    /** 网关编号(String) -> 最后活跃时间 */
    private final Map<String, Long> heartbeatDevice = new ConcurrentHashMap<>();

    private final TcpYwnV2Verticle tcpYwnV2Verticle;
    private final ProductApi productApi;
    private final DeviceApi deviceApi;

    private Vertx vertx;
    private NetServer server;

    /** 是否允许自动注册设备 */
    private boolean allowAutoRegister;

    /** 备用产品Key(仅当 allowAutoRegister=true 时生效) */
    private String fallbackProductKey;

    /**
     * 心跳超时: YWN V2心跳周期默认3分钟，超时判定为6分钟
     */
    @Setter
    private long keepAliveTimeout = Duration.ofMinutes(6).toMillis();

    protected TcpYwnV2Component(
            TcpYwnV2Verticle tcpYwnV2Verticle,
            ProductApi productApi,
            DeviceApi deviceApi,
            ComponentServices componentServices
    ) {
        super(componentServices);
        this.tcpYwnV2Verticle = tcpYwnV2Verticle;
        this.productApi = productApi;
        this.deviceApi = deviceApi;
    }

    public void setTcpServer(NetServer server, Vertx vertx) {
        this.vertx = vertx;
        this.server = server;
        server.connectHandler(this::handle);
    }

    // ==================== 连接处理 ====================

    @Override
    public void handle(NetSocket socket) {
        log.info("[YWN-V2] 收到客户端连接: {}", socket.remoteAddress());
        String clientId = IdUtil.simpleUUID() + "_" + socket.remoteAddress();
        VertxTcpYwnV2Client client = new VertxTcpYwnV2Client(clientId);
        client.setKeepAliveTimeoutMs(keepAliveTimeout);

        try {
            socket.exceptionHandler(err -> log.error("[YWN-V2] TCP异常: {}", err.getMessage()));

            client.setOnCloseCallback(() -> {
                log.warn("[YWN-V2] 客户端断连清理: {}", socket.remoteAddress());
                clientMap.entrySet().removeIf(e -> e.getValue() == client);
            });

            // 创建 V2 协议帧解析器(基于长度前缀)
            RecordParser parser = YwnV2FrameCodec.createParser(frame -> {
                try {
                    processFrame(frame, client, socket);
                } catch (Exception e) {
                    log.error("[YWN-V2] 处理帧异常", e);
                }
            });
            client.setParser(parser);
            client.setSocket(socket);
            log.info("[YWN-V2] 客户端已接入: {}", socket.remoteAddress());
        } catch (Exception e) {
            log.error("[YWN-V2] 连接初始化失败", e);
            client.shutdown();
        }
    }

    // ==================== 协议帧处理 ====================

    private void processFrame(YwnV2Frame frame, VertxTcpYwnV2Client client, NetSocket socket) {
        MessageType msgType = frame.getMessageType();
        int seqNo = frame.getSequenceNo();

        log.info("[YWN-V2] 收到帧: type={}, seq={}, dataLen={}",
                msgType, seqNo, frame.getProtobufData() != null ? frame.getProtobufData().length : 0);

        if (msgType == null) {
            log.warn("[YWN-V2] 未知消息类型，丢弃");
            return;
        }

        switch (msgType) {
            case REGISTER:
                handleRegister(frame, client, socket);
                break;

            case HEARTBEAT:
                handleHeartbeat(frame, client, socket);
                break;

            case EVENT:
                handleEvent(frame, client, socket);
                break;

            case CONTROL:
                // 服务端收到 CONTROL 类型消息 = 设备对控制指令的确认
                handleControlAck(frame);
                break;

            default:
                log.warn("[YWN-V2] 未处理的消息类型: {}", msgType);
                break;
        }
    }

    // ==================== 注册处理 ====================

    private void handleRegister(YwnV2Frame frame, VertxTcpYwnV2Client client, NetSocket socket) {
        YwnV2Protocol.RegisterReq req = frame.parseRegisterReq();
        if (req == null) {
            log.error("[YWN-V2] 注册请求解析失败");
            sendAck(socket, MessageType.REGISTER, frame.getSequenceNo(), false, "注册请求解析失败");
            return;
        }

        String gateway = req.getGateway();
        if (gateway == null || gateway.isEmpty()) {
            log.error("[YWN-V2] 注册请求缺少网关编号");
            sendAck(socket, MessageType.REGISTER, frame.getSequenceNo(), false, "网关编号不能为空");
            return;
        }

        log.info("[YWN-V2] 设备注册: gateway={}", gateway);

        // 绑定客户端映射
        clientMap.put(gateway, client);
        heartbeatDevice.put(gateway, System.currentTimeMillis());

        // 确保设备已注册到平台
        ensureDeviceRegistered(gateway);

        // 回复注册确认
        sendAck(socket, MessageType.REGISTER, frame.getSequenceNo(), true, "注册成功");

        // 设备上线
        online(gateway);
    }

    // ==================== 心跳处理 ====================

    private void handleHeartbeat(YwnV2Frame frame, VertxTcpYwnV2Client client, NetSocket socket) {
        // 心跳帧无 payload，需要通过 client 反查 gateway
        String gateway = findGatewayByClient(client);
        if (gateway == null) {
            log.warn("[YWN-V2] 心跳来自未注册客户端: {}", socket.remoteAddress());
            sendAck(socket, MessageType.HEARTBEAT, frame.getSequenceNo(), false, "请先注册");
            return;
        }

        log.debug("[YWN-V2] 心跳: gateway={}", gateway);

        heartbeatDevice.put(gateway, System.currentTimeMillis());

        // 确保设备已注册
        ensureDeviceRegistered(gateway);

        // 回复心跳确认
        sendAck(socket, MessageType.HEARTBEAT, frame.getSequenceNo(), true, "");

        // 上线
        online(gateway);
    }

    // ==================== 事件处理 ====================

    private void handleEvent(YwnV2Frame frame, VertxTcpYwnV2Client client, NetSocket socket) {
        // 通过 client 反查 gateway
        String gateway = findGatewayByClient(client);
        if (gateway == null) {
            log.warn("[YWN-V2] 事件来自未注册客户端: {}", socket.remoteAddress());
            sendAck(socket, MessageType.EVENT, frame.getSequenceNo(), false, "请先注册");
            return;
        }

        YwnV2Protocol.EventReq req = frame.parseEventReq();
        if (req == null) {
            log.error("[YWN-V2] 事件请求解析失败: gateway={}", gateway);
            sendAck(socket, MessageType.EVENT, frame.getSequenceNo(), false, "事件解析失败");
            return;
        }

        int eventTypeCode = req.getEvent();
        YwnV2EventType eventType = YwnV2EventType.fromCode(eventTypeCode);

        log.info("[YWN-V2] 事件上报: gateway={}, event={}, host={}, loop={}, once={}, devtype={}, twice={}, zongxian={}, deviceCode={}",
                gateway,
                eventType != null ? eventType.getDescription() : String.format("未知(%d)", eventTypeCode),
                req.getHost(), req.getLoop(), req.getOnce(),
                req.getDevtype(), req.getTwice(), req.getZongxian(), req.getDeviceCode());

        heartbeatDevice.put(gateway, System.currentTimeMillis());
        ensureDeviceRegistered(gateway);

        String pk = gatewayToPk.get(gateway);
        if (pk == null) {
            log.warn("[YWN-V2] 设备 {} 无productKey，跳过上报", gateway);
            sendAck(socket, MessageType.EVENT, frame.getSequenceNo(), false, "设备未注册到平台");
            return;
        }

        // 回复事件确认
        sendAck(socket, MessageType.EVENT, frame.getSequenceNo(), true, "");

        // === 属性上报 ===
        Map<String, Object> propertyParams = new LinkedHashMap<>();
        propertyParams.put("event_type_code", eventTypeCode);
        propertyParams.put("event_name", eventType != null ? eventType.getDescription() : String.format("未知(%d)", eventTypeCode));
        propertyParams.put("host", req.getHost());
        propertyParams.put("loop", req.getLoop());
        propertyParams.put("once", req.getOnce());
        propertyParams.put("devtype", req.getDevtype());
        propertyParams.put("twice", req.getTwice());
        propertyParams.put("zongxian", req.getZongxian());
        propertyParams.put("device_code", req.getDeviceCode());
        propertyParams.put("sub_device_code", generateSubDeviceCode(req.getHost(), req.getLoop(), req.getOnce()));

        // 状态派生
        appendStatusChanges(propertyParams, eventType);

        report(PropertyReport.builder()
                .productKey(pk)
                .deviceName(gateway)
                .params(propertyParams)
                .build());

        // === 异常事件上报 ===
        triggerAbnormalEvents(pk, gateway, eventType, req);

        // 上线
        online(gateway);
    }

    /**
     * 触发异常事件上报(火警/故障/屏蔽/启动/反馈/监管)
     */
    private void triggerAbnormalEvents(String pk, String gateway, YwnV2EventType eventType, YwnV2Protocol.EventReq req) {
        if (eventType == null) return;

        // 火警
        if (eventType.isFireAlarm()) {
            log.warn("[YWN-V2] 火警! gateway={}, host={}, loop={}, once={}, event={}",
                    gateway, req.getHost(), req.getLoop(), req.getOnce(), eventType.getDescription());

            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("event_code", eventType.getCode());
            eventParams.put("event_name", eventType.getDescription());
            eventParams.put("host", req.getHost());
            eventParams.put("loop", req.getLoop());
            eventParams.put("once", req.getOnce());
            eventParams.put("device_code", req.getDeviceCode());

            report(EventReport.builder()
                    .productKey(pk).deviceName(gateway)
                    .name("fire_alarm_event")
                    .params(eventParams)
                    .build());
        }

        // 故障
        if (eventType.isFault()) {
            log.warn("[YWN-V2] 故障! gateway={}, host={}, loop={}, once={}, event={}",
                    gateway, req.getHost(), req.getLoop(), req.getOnce(), eventType.getDescription());

            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("event_code", eventType.getCode());
            eventParams.put("event_name", eventType.getDescription());
            eventParams.put("host", req.getHost());
            eventParams.put("loop", req.getLoop());
            eventParams.put("once", req.getOnce());
            eventParams.put("device_code", req.getDeviceCode());

            report(EventReport.builder()
                    .productKey(pk).deviceName(gateway)
                    .name("fault_event")
                    .params(eventParams)
                    .build());
        }

        // 反馈
        if (eventType.isFeedback()) {
            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("event_code", eventType.getCode());
            eventParams.put("event_name", eventType.getDescription());
            eventParams.put("host", req.getHost());
            eventParams.put("loop", req.getLoop());
            eventParams.put("once", req.getOnce());

            report(EventReport.builder()
                    .productKey(pk).deviceName(gateway)
                    .name("feedback_event")
                    .params(eventParams)
                    .build());
        }
    }
    /**
     * 状态派生: 根据事件类型置位/清零独立状态属性
     */
    private void appendStatusChanges(Map<String, Object> props, YwnV2EventType eventType) {
        if (eventType == null) return;

        if (eventType.isFireAlarm()) {
            props.put("fire_status", true);
        }
        if (eventType.isFault()) {
            props.put("fault_status", true);
        }
        if (eventType.isShield()) {
            props.put("shield_status", true);
        }
        if (eventType.isStart()) {
            props.put("start_status", true);
        }
        if (eventType.isFeedback()) {
            props.put("feedback_status", true);
        }
        if (eventType.isSupervision()) {
            props.put("supervisory_status", true);
        }

        // 恢复事件: 清零对应状态
        if (eventType.isRecovery()) {
            switch (eventType) {
                case ALARM_RECOVER:
                    props.put("fire_status", false);
                    break;
                case FAULT_RECOVER:
                case MAIN_POWER_RECOVER:
                case EXTRA_POWER_RECOVER:
                    props.put("fault_status", false);
                    break;
                case SHIELD_RECOVER:
                    props.put("shield_status", false);
                    break;
                case START_RECOVER:
                    props.put("start_status", false);
                    break;
                case FEEDBACK_RECOVER:
                    props.put("feedback_status", false);
                    break;
                case SUPERVISION_RECOVER:
                    props.put("supervisory_status", false);
                    break;
            }
        }
    }

    // ==================== 控制确认处理 ====================

    private void handleControlAck(YwnV2Frame frame) {
        YwnV2Protocol.Message ack = frame.parseAck();
        if (ack != null) {
            log.info("[YWN-V2] 控制确认: seq={}, ok={}, msg={}",
                    frame.getSequenceNo(), ack.getOk(), ack.getMsg());
        } else {
            log.info("[YWN-V2] 控制确认: seq={}", frame.getSequenceNo());
        }
    }

    // ==================== 下行指令 ====================

    /**
     * 服务调用 - 远程控制
     *
     * 服务名: remote_control
     * 参数:
     *   control_type - 控制类型 (1-10, 必填, 对应 YwnV2ControlType)
     *   host         - 主机号 (默认1)
     *   loop         - 回路号 (默认0)
     *   once         - 一次码 (默认0)
     *   devtype      - 设备类型 (默认0)
     *   twice        - 二次码 (默认"")
     *   zongxian     - 总线标识 (默认false)
     */
    @Override
    protected void serviceInvoke(ServiceInvoke action) {
        log.info("[YWN-V2] 服务调用: name={}, device={}, params={}",
                action.getName(), action.getDeviceName(), action.getParams());
        String serviceName = action.getName();
        String gateway = action.getDeviceName();
        Map<String, ?> params = action.getParams();

        if (!"remote_control".equals(serviceName)) {
            log.warn("[YWN-V2] 未知服务: {}", serviceName);
            report(ServiceReply.builder()
                    .replyId(action.getId())
                    .productKey(gatewayToPk.getOrDefault(gateway, ""))
                    .deviceName(gateway)
                    .name(serviceName)
                    .code(1)
                    .params(Collections.singletonMap("error", "未知服务: " + serviceName + ", 仅支持 remote_control"))
                    .build());
            return;
        }

        int controlType = getIntParam(params, "control_type", 0);
        if (controlType < 1 || controlType > 10) {
            log.warn("[YWN-V2] 非法控制类型: {}", controlType);
            report(ServiceReply.builder()
                    .replyId(action.getId())
                    .productKey(gatewayToPk.getOrDefault(gateway, ""))
                    .deviceName(gateway)
                    .name(serviceName)
                    .code(1)
                    .params(Collections.singletonMap("error", "非法控制类型: " + controlType + ", 有效范围 1-10"))
                    .build());
            return;
        }

        int host = getIntParam(params, "host", 1);
        int loop = getIntParam(params, "loop", 0);
        int once = getIntParam(params, "once", 0);
        int devtype = getIntParam(params, "devtype", 0);
        String twice = getStringParam(params, "twice", "");
        boolean zongxian = getBoolParam(params, "zongxian", false);

        // 在线校验
        VertxTcpYwnV2Client tcpClient = clientMap.get(gateway);
        if (tcpClient == null || tcpClient.socket == null) {
            log.warn("[YWN-V2] 设备不在线, 拒绝下发: {}", gateway);
            report(ServiceReply.builder()
                    .replyId(action.getId())
                    .productKey(gatewayToPk.getOrDefault(gateway, ""))
                    .deviceName(gateway)
                    .name(serviceName)
                    .code(1)
                    .params(Collections.singletonMap("error", "设备不在线"))
                    .build());
            return;
        }

        Buffer msg = YwnV2FrameCodec.encodeControl(controlType, host, loop, once, devtype, twice, zongxian);
        sendMsg(gateway, msg);

        // 指令驱动状态清零
        reportCommandDrivenStatus(gateway, controlType);

        // 回复服务调用成功
        String pk = gatewayToPk.get(gateway);
        if (pk != null) {
            YwnV2ControlType ctrlType = YwnV2ControlType.fromCode(controlType);
            report(ServiceReply.builder()
                    .replyId(action.getId())
                    .productKey(pk)
                    .deviceName(gateway)
                    .name(serviceName)
                    .code(0)
                    .params(Collections.singletonMap("result",
                            "success: " + (ctrlType != null ? ctrlType.getDescription() : String.valueOf(controlType))))
                    .build());
        }
    }

    /**
     * 指令驱动状态清零: 复位(1)清火警/启动/反馈/监管; 取消屏蔽(6)清屏蔽
     */
    private void reportCommandDrivenStatus(String gateway, int controlType) {
        String pk = gatewayToPk.get(gateway);
        if (pk == null) return;

        Map<String, Object> props = new LinkedHashMap<>();
        if (controlType == YwnV2ControlType.RESET.getCode()) {
            props.put("fire_status", false);
            props.put("start_status", false);
            props.put("feedback_status", false);
            props.put("supervisory_status", false);
        } else if (controlType == YwnV2ControlType.UNSHIELD.getCode()) {
            props.put("shield_status", false);
        }
        if (!props.isEmpty()) {
            report(PropertyReport.builder()
                    .productKey(pk)
                    .deviceName(gateway)
                    .params(props)
                    .build());
        }
    }

    /**
     * 属性设置 - 通过 params 下发控制命令
     */
    @Override
    protected void propertySet(PropertySet action) {
        log.info("[YWN-V2] 属性设置: device={}, params={}", action.getDeviceName(), action.getParams());
        Map<String, ?> params = action.getParams();
        if (params == null) return;

        String gateway = action.getDeviceName();

        int controlType = getIntParam(params, "control_type", 0);
        if (controlType <= 0) {
            log.warn("[YWN-V2] 属性设置缺少 control_type 参数");
            return;
        }

        int host = getIntParam(params, "host", 1);
        int loop = getIntParam(params, "loop", 0);
        int once = getIntParam(params, "once", 0);
        int devtype = getIntParam(params, "devtype", 0);
        String twice = getStringParam(params, "twice", "");
        boolean zongxian = getBoolParam(params, "zongxian", false);

        Buffer msg = YwnV2FrameCodec.encodeControl(controlType, host, loop, once, devtype, twice, zongxian);
        sendMsg(gateway, msg);
    }

    @Override
    protected void propertyGet(PropertyGet action) {
        log.info("[YWN-V2] 属性获取(协议不支持): {}", action);
    }

    @Override
    protected void deviceOta(DeviceOta action) {
        // YWN V2协议不支持OTA
    }

    @Override
    protected void deviceConfig(DeviceConfig action) {
        // 暂不实现
    }

    @Override
    protected void deviceTopoChange(DeviceTopoChange action) {
        // 暂不实现
    }

    // ==================== 组件启停 ====================

    @Override
    public boolean stateChange(boolean enable, String config) {
        tcpYwnV2Verticle.setTcpYwnV2Component(this);

        if (!enable) {
            tcpYwnV2Verticle.stopServer();
            return true;
        }

        if (config == null) {
            return false;
        }

        TcpYwnV2Config tcpConfig = JsonUtils.parseObject(config, TcpYwnV2Config.class);
        if (tcpConfig == null) {
            log.error("[YWN-V2] 解析配置失败");
            return false;
        }

        this.allowAutoRegister = Boolean.TRUE.equals(tcpConfig.getAllowAutoRegister());
        this.fallbackProductKey = tcpConfig.getFallbackProductKey();

        tcpYwnV2Verticle.stopServer();
        tcpYwnV2Verticle.startServer(tcpConfig);
        return true;
    }

    // ==================== 设备管理 ====================

    /**
     * 确保设备已注册到平台
     */
    private void ensureDeviceRegistered(String gateway) {
        if (gatewayToPk.containsKey(gateway)) {
            return;
        }

        // 策略1: 通过 deviceName 查询设备是否已预注册
        DeviceInfo existingDevice = deviceApi.getDeviceByDn(gateway);
        if (existingDevice != null) {
            String pk = existingDevice.getProductKey();
            gatewayToPk.put(gateway, pk);
            cacheDeviceComponentInfo(pk, gateway);
            log.info("[YWN-V2] 设备已预注册: pk={}, gateway={}, tenantId={}",
                    pk, gateway, existingDevice.getTenantId());
            return;
        }

        // 检查是否允许自动注册
        if (!allowAutoRegister) {
            log.warn("[YWN-V2] 设备 {} 未预注册且不允许自动注册,拒绝接入", gateway);
            return;
        }

        // 策略2: 允许自动注册
        String pk = fallbackProductKey;
        if (pk == null || pk.isEmpty()) {
            log.warn("[YWN-V2] 未配置 fallbackProductKey,无法自动注册设备 {}", gateway);
            return;
        }

        gatewayToPk.put(gateway, pk);
        RegisterDevice build = RegisterDevice.builder()
                .productKey(pk)
                .deviceName(gateway)
                .build();
        DeviceInfo registered = deviceApi.registerDevice(build);
        if (registered != null) {
            cacheDeviceComponentInfo(pk, gateway);
            log.info("[YWN-V2] 设备自动注册成功: pk={}, gateway={}", pk, gateway);
        } else {
            log.error("[YWN-V2] 设备自动注册失败: gateway={}", gateway);
            gatewayToPk.remove(gateway);
        }
    }

    /**
     * 设备上线
     */
    public void online(String gateway) {
        heartbeatDevice.put(gateway, System.currentTimeMillis());
        String pk = gatewayToPk.get(gateway);
        if (pk != null && !pk.isEmpty()) {
            report(DeviceStateChange.builder()
                    .deviceName(gateway)
                    .productKey(pk)
                    .state(DeviceState.ONLINE)
                    .build());
        }
    }

    // ==================== 离线检测 ====================

    @Scheduled(fixedRate = 40, timeUnit = TimeUnit.SECONDS)
    private void offlineCheckTask() {
        log.debug("[YWN-V2] 执行离线检测");

        // 检查 TCP 客户端是否在线
        Set<String> clients = new HashSet<>(clientMap.keySet());
        for (String key : clients) {
            VertxTcpYwnV2Client client = clientMap.get(key);
            if (client != null && !client.isOnline()) {
                client.shutdown();
                clientMap.remove(key);
            }
        }

        // 检查心跳超时
        Set<String> heartbeatKeys = new HashSet<>(heartbeatDevice.keySet());
        heartbeatKeys.forEach(gateway -> {
            Long time = heartbeatDevice.get(gateway);
            if (time != null && System.currentTimeMillis() - time > keepAliveTimeout) {
                heartbeatDevice.remove(gateway);
                String pk = gatewayToPk.get(gateway);
                if (pk != null) {
                    log.warn("[YWN-V2] 设备离线(心跳超时): gateway={}", gateway);
                    report(DeviceStateChange.builder()
                            .productKey(pk)
                            .deviceName(gateway)
                            .state(DeviceState.OFFLINE)
                            .time(System.currentTimeMillis())
                            .build());
                }
            }
        });
    }

    // ==================== 工具方法 ====================

    /**
     * 发送确认帧
     */
    private void sendAck(NetSocket socket, MessageType msgType, int sequenceNo, boolean ok, String msg) {
        Buffer ack = YwnV2FrameCodec.encodeAck(msgType, sequenceNo, ok, msg);
        socket.write(ack, r -> {
            if (r.succeeded()) {
                log.debug("[YWN-V2] ACK发送成功: type={}, seq={}, ok={}", msgType, sequenceNo, ok);
            } else {
                log.error("[YWN-V2] ACK发送失败: type={}, seq={}", msgType, sequenceNo, r.cause());
            }
        });
    }

    /**
     * 向指定网关发送消息
     */
    public void sendMsg(String gateway, Buffer msg) {
        VertxTcpYwnV2Client tcpClient = clientMap.get(gateway);
        if (tcpClient != null && tcpClient.socket != null) {
            tcpClient.sendMessage(msg);
            log.info("[YWN-V2] 已发送: gateway={}, len={}", gateway, msg.length());
        } else {
            log.warn("[YWN-V2] 设备 {} 不在线，无法发送", gateway);
        }
    }

    /**
     * 通过 client 反查 gateway
     */
    private String findGatewayByClient(VertxTcpYwnV2Client client) {
        for (Map.Entry<String, VertxTcpYwnV2Client> entry : clientMap.entrySet()) {
            if (entry.getValue() == client) {
                return entry.getKey();
            }
        }
        return null;
    }

    private int getIntParam(Map<String, ?> params, String key, int defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private String getStringParam(Map<String, ?> params, String key, String defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    private boolean getBoolParam(Map<String, ?> params, String key, boolean defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return defaultValue;
    }

    /**
     * 生成子设备编码：机号-回路-地址 (如 01-02-003)
     */
    private String generateSubDeviceCode(int host, int loop, int once) {
        return String.format("%02d-%02d-%03d", host, loop, once);
    }
    @Override
    public String getType() {
        return "ywn-tcp-v2";
    }

    @Override
    public String getName() {
        return "YWN控制器监控TCP协议V2(Protobuf)组件";
    }
}