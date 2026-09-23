package com.enjoyiot.eiot.component.tcp.ywn.service;

import cn.hutool.core.util.IdUtil;
import com.enjoyiot.eiot.component.core.ComponentServices;
import com.enjoyiot.eiot.component.core.ThingComponent;
import com.enjoyiot.eiot.common.enums.DeviceState;
import com.enjoyiot.eiot.component.core.model.down.*;
import com.enjoyiot.eiot.component.core.model.up.*;
import com.enjoyiot.eiot.component.tcp.ywn.cilent.VertxTcpYwnClient;
import com.enjoyiot.eiot.component.tcp.ywn.model.TcpYwnConfig;
import com.enjoyiot.eiot.component.tcp.ywn.parser.*;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.framework.common.util.string.SubDeviceCodeUtil;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TcpYwnComponent extends ThingComponent implements Handler<NetSocket> {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 设备编号(String) → TCP客户端 */
    private final Map<String, VertxTcpYwnClient> clientMap = new ConcurrentHashMap<>();

    /** 设备编号(String) → productKey */
    private final Map<String, String> dnToPk = new ConcurrentHashMap<>();

    /** 设备编号(String) → 最后活跃时间 */
    private final Map<String, Long> heartbeatDevice = new ConcurrentHashMap<>();

    private final TcpYwnVerticle tcpYwnVerticle;
    private final ProductApi productApi;
    private final DeviceApi deviceApi;

    private Vertx vertx;
    private NetServer server;

    /** 是否允许自动注册设备 */
    private boolean allowAutoRegister;

    /** 备用产品Key(仅当 allowAutoRegister=true 时生效) */
    private String fallbackProductKey;

    /**
     * 心跳超时: YWN心跳周期3分钟，超时判定为6分钟
     */
    @Setter
    private long keepAliveTimeout = Duration.ofMinutes(6).toMillis();

    protected TcpYwnComponent(
            TcpYwnVerticle tcpYwnVerticle,
            ProductApi productApi,
            DeviceApi deviceApi,
            ComponentServices componentServices
    ) {
        super(componentServices);
        this.tcpYwnVerticle = tcpYwnVerticle;
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
        log.info("[YWN] 收到客户端连接: {}", socket.remoteAddress());
        String clientId = IdUtil.simpleUUID() + "_" + socket.remoteAddress();
        VertxTcpYwnClient client = new VertxTcpYwnClient(clientId);
        client.setKeepAliveTimeoutMs(keepAliveTimeout);

        try {
            socket.exceptionHandler(err -> log.error("[YWN] TCP异常: {}", err.getMessage()));

            // 设置断连回调：当 shutdown() 被触发时自动清理 clientMap
            client.setOnCloseCallback(() -> {
                log.warn("[YWN] 客户端断连清理: {}", socket.remoteAddress());
                clientMap.entrySet().removeIf(e -> e.getValue() == client);
            });

            // 创建 YWN 协议帧解析器
            RecordParser parser = DataYwnReader.getParser(buffer -> {
                try {
                    DataYwnPackage pkg = DataYwnDecoder.decode(buffer);
                    if (pkg == null) {
                        log.warn("[YWN] 数据包解码失败，丢弃");
                        return;
                    }
                    processPacket(pkg, client, socket);
                } catch (Exception e) {
                    log.error("[YWN] 处理数据包异常", e);
                }
            });
            client.setParser(parser);
            client.setSocket(socket);
            log.warn("[YWN] 客户端已接入: {}", socket.remoteAddress());
        } catch (Exception e) {
            log.error("[YWN] 连接初始化失败", e);
            client.shutdown();
        }
    }

    // ==================== 协议帧处理 ====================

    /**
     * 处理收到的 YWN 帧
     */
    private void processPacket(DataYwnPackage pkg, VertxTcpYwnClient client, NetSocket socket) {
        int deviceNumber = pkg.getDeviceNumber();
        String deviceAddr = String.valueOf(deviceNumber);
        int cmdCode = pkg.getCommand();
        YwnCommand cmd = YwnCommand.fromCode(cmdCode);

        log.info("[YWN] 收到帧: cmd={}, dev={}, hex={}",
                cmd != null ? cmd : String.format("0x%02X", cmdCode),
                deviceNumber, pkg.toHexString());

        // 绑定客户端映射（上行命令时）
        if (cmd == YwnCommand.EVENT_UPLOAD || cmd == YwnCommand.DEVICE_INFO_UPLOAD
                || cmd == YwnCommand.HEARTBEAT || cmd == YwnCommand.CONTROLLER_FAULT
                || cmd == YwnCommand.CONTROLLER_FAULT_RECOVERY) {
            clientMap.put(deviceAddr, client);
            heartbeatDevice.put(deviceAddr, System.currentTimeMillis());
        }

        switch (cmdCode) {
            case DataYwnPackage.CMD_EVENT_UPLOAD:
                handleEventUpload(pkg, deviceAddr);
                sendAck(deviceAddr, socket);
                break;

            case DataYwnPackage.CMD_DEVICE_INFO_UPLOAD:
                handleDeviceInfoUpload(pkg, deviceAddr);
                sendAck(deviceAddr, socket);
                break;

            case DataYwnPackage.CMD_HEARTBEAT:
                handleHeartbeat(pkg, deviceAddr, socket);
                break;

            case DataYwnPackage.CMD_ACK:
                handleControlAck(pkg, deviceAddr);
                break;

            case DataYwnPackage.CMD_CONTROLLER_FAULT:
                handleControllerFault(pkg, deviceAddr);
                break;

            case DataYwnPackage.CMD_CONTROLLER_FAULT_RECOVERY:
                handleControllerFaultRecovery(pkg, deviceAddr);
                break;

            default:
                log.warn("[YWN] 服务端不处理命令: {}", cmd != null ? cmd : String.format("0x%02X", cmdCode));
                break;
        }
    }

    // ==================== 上行命令处理 ====================

    /**
     * 处理事件上传 (0x80)
     */
    private void handleEventUpload(DataYwnPackage pkg, String deviceAddr) {
        YwnEvent event = pkg.parseEvent();
        if (event == null) {
            log.error("[YWN] 事件数据解析失败: dev={}", deviceAddr);
            return;
        }

        log.info("[YWN] 事件上报: dev={}, event={}", deviceAddr, event);

        // 确保设备已注册
        ensureDeviceRegistered(deviceAddr);
        String pk = dnToPk.get(deviceAddr);
        if (pk == null) {
            log.warn("[YWN] 设备 {} 无productKey，跳过上报", deviceAddr);
            return;
        }

        // === 属性上报: 所有事件都更新属性 ===
        Map<String, Object> propertyParams = new LinkedHashMap<>();
        propertyParams.put("event_type_code", event.getEventTypeCode());
        propertyParams.put("event_code", event.getEventCode());
        propertyParams.put("event_name", event.getEventCodeName());
        propertyParams.put("machine_no", event.getMachineNo());
        propertyParams.put("loop_no", event.getLoopNo());
        propertyParams.put("address_no", event.getAddressNo());
        propertyParams.put("sub_device_code", SubDeviceCodeUtil.generateDeviceCode(event.getMachineNo(),event.getLoopNo(), event.getAddressNo()));
        propertyParams.put("channel_no", event.getChannelNo());
        propertyParams.put("device_type", event.getDeviceType());
        propertyParams.put("product_type", event.getProductType());
        propertyParams.put("channel_type", event.getChannelType());
        propertyParams.put("description", event.getDescription());
        if (event.getAlarmValueFloat() != -1.0f) {
            propertyParams.put("alarm_value_float", event.getAlarmValueFloat());
        }
        if (event.getAlarmValueInt() != -1) {
            propertyParams.put("alarm_value_int", event.getAlarmValueInt());
        }
        if (event.getEventTime() != null) {
            propertyParams.put("event_time", event.getEventTime().format(TIME_FMT));
        }

        // 状态派生: 根据事件码置位/清零独立状态属性(与event_code并存,互不覆盖)
        appendStatusChanges(propertyParams, event);

        report(PropertyReport.builder()
                .productKey(pk)
                .deviceName(deviceAddr)
                .params(propertyParams)
                .build());

        // === 异常事件上报 ===
        triggerAbnormalEvents(pk, deviceAddr, event);

        // // 上线状态
        // online(deviceAddr);
    }

    /**
     * 触发异常事件上报（火警/故障/反馈）
     */
    private void triggerAbnormalEvents(String pk, String deviceAddr, YwnEvent event) {
        if (event.isFireAlarm()) {
            log.warn("[YWN] 火警! dev={}, machine={}, loop={}, addr={}, event={}",
                    deviceAddr, event.getMachineNo(), event.getLoopNo(),
                    event.getAddressNo(), event.getEventCodeName());

            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("event_code", event.getEventCode());
            eventParams.put("event_name", event.getEventCodeName());
            eventParams.put("machine_no", event.getMachineNo());
            eventParams.put("loop_no", event.getLoopNo());
            eventParams.put("address_no", event.getAddressNo());
            eventParams.put("description", event.getDescription());
            if (event.getEventTime() != null) {
                eventParams.put("event_time", event.getEventTime().format(TIME_FMT));
            }

            report(EventReport.builder()
                    .productKey(pk).deviceName(deviceAddr)
                    .name("fire_alarm_event")
                    .params(eventParams)
                    .build());
        }

        if (event.isFault()) {
            log.warn("[YWN] 故障! dev={}, machine={}, loop={}, addr={}, event={}",
                    deviceAddr, event.getMachineNo(), event.getLoopNo(),
                    event.getAddressNo(), event.getEventCodeName());

            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("event_code", event.getEventCode());
            eventParams.put("event_name", event.getEventCodeName());
            eventParams.put("machine_no", event.getMachineNo());
            eventParams.put("loop_no", event.getLoopNo());
            eventParams.put("address_no", event.getAddressNo());
            eventParams.put("description", event.getDescription());
            if (event.getEventTime() != null) {
                eventParams.put("event_time", event.getEventTime().format(TIME_FMT));
            }

            report(EventReport.builder()
                    .productKey(pk).deviceName(deviceAddr)
                    .name("fault_event")
                    .params(eventParams)
                    .build());
        }

        // 反馈类事件
        if (event.getEventCode() == YwnEventCode.BUS_DEVICE_FEEDBACK.getCode()
                || event.getEventCode() == YwnEventCode.BUS_DEVICE_FEEDBACK_CANCEL.getCode()) {
            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("event_code", event.getEventCode());
            eventParams.put("event_name", event.getEventCodeName());
            eventParams.put("machine_no", event.getMachineNo());
            eventParams.put("loop_no", event.getLoopNo());
            eventParams.put("address_no", event.getAddressNo());

            report(EventReport.builder()
                    .productKey(pk).deviceName(deviceAddr)
                    .name("feedback_event")
                    .params(eventParams)
                    .build());
        }
    }

    /**
     * 处理设备信息上传 (0x81)
     */
    private void handleDeviceInfoUpload(DataYwnPackage pkg, String deviceAddr) {
        try {
            YwnDeviceInfo deviceInfo = YwnDeviceInfo.fromPayload(pkg.getDeviceNumber(), pkg.getPayload());
            log.info("[YWN] 设备信息: {}", deviceInfo);

            // 确保设备已注册
            ensureDeviceRegistered(deviceAddr);
            String pk = dnToPk.get(deviceAddr);
            if (pk == null) {
                log.warn("[YWN] 设备 {} 无productKey，跳过上报", deviceAddr);
                return;
            }

            // 属性上报: 设备信息
            Map<String, Object> propertyParams = new LinkedHashMap<>();
            propertyParams.put("iccid", deviceInfo.getIccid());
            propertyParams.put("imei", deviceInfo.getImei());
            propertyParams.put("hardware_version", deviceInfo.getHardwareVersion());
            propertyParams.put("software_version", deviceInfo.getSoftwareVersion());

            report(PropertyReport.builder()
                    .productKey(pk)
                    .deviceName(deviceAddr)
                    .params(propertyParams)
                    .build());

            // 上线
            online(deviceAddr);
        } catch (Exception e) {
            log.error("[YWN] 设备信息解析失败: dev={}", deviceAddr, e);
        }
    }

    /**
     * 处理心跳 (0x82)
     */
    private void handleHeartbeat(DataYwnPackage pkg, String deviceAddr, NetSocket socket) {
        log.debug("[YWN] 心跳: dev={}", deviceAddr);

        heartbeatDevice.put(deviceAddr, System.currentTimeMillis());

        // 确保设备已注册
        ensureDeviceRegistered(deviceAddr);

        // 回复心跳应答 (0x83)
        Buffer heartbeatAck = DataYwnEncoder.encodeHeartbeatAck(pkg.getDeviceNumber());
        socket.write(heartbeatAck, r -> {
            if (r.succeeded()) {
                log.debug("[YWN] 心跳应答发送成功: dev={}", deviceAddr);
            } else {
                log.error("[YWN] 心跳应答发送失败: dev={}", deviceAddr, r.cause());
            }
        });

        // 上线
        online(deviceAddr);
    }

    /**
     * 处理ACK (0x01) — 设备对控制命令的应答
     */
    private void handleControlAck(DataYwnPackage pkg, String deviceAddr) {
        log.info("[YWN] 控制应答: dev={}", deviceAddr);
    }

    /**
     * 处理控制器故障 (0x86)
     */
    private void handleControllerFault(DataYwnPackage pkg, String deviceAddr) {
        log.warn("[YWN] 控制器故障: dev={}", deviceAddr);

        String pk = dnToPk.get(deviceAddr);
        if (pk != null) {
            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("fault_type", "controller_fault");
            eventParams.put("event_time", LocalDateTime.now().format(TIME_FMT));

            report(EventReport.builder()
                    .productKey(pk).deviceName(deviceAddr)
                    .name("controller_fault_event")
                    .params(eventParams)
                    .build());

            // 状态置位: 控制器故障
            report(PropertyReport.builder()
                    .productKey(pk).deviceName(deviceAddr)
                    .params(Collections.singletonMap("controller_fault_status", true))
                    .build());
        }
    }

    /**
     * 处理控制器故障恢复 (0x87)
     */
    private void handleControllerFaultRecovery(DataYwnPackage pkg, String deviceAddr) {
        log.info("[YWN] 控制器故障恢复: dev={}", deviceAddr);

        String pk = dnToPk.get(deviceAddr);
        if (pk != null) {
            Map<String, Object> eventParams = new LinkedHashMap<>();
            eventParams.put("recovery_type", "controller_fault_recovery");
            eventParams.put("event_time", LocalDateTime.now().format(TIME_FMT));

            report(EventReport.builder()
                    .productKey(pk).deviceName(deviceAddr)
                    .name("controller_fault_recovery_event")
                    .params(eventParams)
                    .build());

            // 状态清零: 控制器故障恢复
            report(PropertyReport.builder()
                    .productKey(pk).deviceName(deviceAddr)
                    .params(Collections.singletonMap("controller_fault_status", false))
                    .build());
        }

        online(deviceAddr);
    }

    // ==================== 下行指令 ====================

    /**
     * 服务调用 — 统一为单一远程控制服务(与四信协议对齐)
     *
     * 服务名: remote_control
     * 参数(4槽位):
     *   command_code - YWN下行命令码 (0x02~0x11, 必填)
     *   machineNo    - 机号 (默认1)
     *   param1       - 参数1: 回路号/盘号/模式(2=手动,3=自动) (默认0)
     *   param2       - 参数2: 地址号/路号 (默认0)
     */
    @Override
    protected void serviceInvoke(ServiceInvoke action) {
        log.info("[YWN] 服务调用: name={}, device={}, params={}",
                action.getName(), action.getDeviceName(), action.getParams());
        String serviceName = action.getName();
        String dn = action.getDeviceName();
        Map<String, ?> params = action.getParams();

        int deviceNumber = parseDeviceNumber(dn);
        if (deviceNumber <= 0) {
            log.warn("[YWN] 无法解析设备编号: {}", dn);
            return;
        }

        if (!"remote_control".equals(serviceName)) {
            log.warn("[YWN] 未知服务: {}", serviceName);
            report(ServiceReply.builder()
                    .replyId(action.getId())
                    .productKey(dnToPk.getOrDefault(dn, ""))
                    .deviceName(dn)
                    .name(serviceName)
                    .code(1)
                    .params(Collections.singletonMap("error", "未知服务: " + serviceName + ", 仅支持 remote_control"))
                    .build());
            return;
        }

        int commandCode = getIntParam(params, "command_code", 0);
        if (commandCode < DataYwnPackage.CMD_RESET || commandCode > DataYwnPackage.CMD_AUTO_MANUAL_PROHIBIT) {
            log.warn("[YWN] 非法命令码: {}", commandCode);
            report(ServiceReply.builder()
                    .replyId(action.getId())
                    .productKey(dnToPk.getOrDefault(dn, ""))
                    .deviceName(dn)
                    .name(serviceName)
                    .code(1)
                    .params(Collections.singletonMap("error", "非法命令码: " + commandCode))
                    .build());
            return;
        }

        int machineNo = getIntParam(params, "machineNo", 1);
        int param1 = getIntParam(params, "param1", 0);
        int param2 = getIntParam(params, "param2", 0);

        // 在线校验: 离线设备直接回复失败, 避免误报成功
        VertxTcpYwnClient tcpClient = clientMap.get(dn);
        if (tcpClient == null || tcpClient.socket == null) {
            log.warn("[YWN] 设备不在线, 拒绝下发: {}", dn);
            report(ServiceReply.builder()
                    .replyId(action.getId())
                    .productKey(dnToPk.getOrDefault(dn, ""))
                    .deviceName(dn)
                    .name(serviceName)
                    .code(1)
                    .params(Collections.singletonMap("error", "设备不在线"))
                    .build());
            return;
        }

        Buffer msg = DataYwnEncoder.encodeControl(commandCode, deviceNumber, machineNo, param1, param2);
        sendMsg(dn, msg);

        // 指令驱动状态清零(复位/取消屏蔽)
        reportCommandDrivenStatus(dn, commandCode);

        // 回复服务调用成功
        String pk = dnToPk.get(dn);
        if (pk != null) {
            report(ServiceReply.builder()
                    .replyId(action.getId())
                    .productKey(pk)
                    .deviceName(dn)
                    .name(serviceName)
                    .code(0)
                    .params(Collections.singletonMap("result", "success"))
                    .build());
        }
    }

    /**
     * 状态派生: 根据事件码置位/清零独立状态属性
     * 状态属性与event_code并存于属性缓存, saveProperties按key合并, 未上报的状态key不受影响
     */
    private void appendStatusChanges(Map<String, Object> props, YwnEvent event) {
        if (event.isFireAlarm()) {
            props.put("fire_status", true);
        }
        if (event.isFault()) {
            props.put("fault_status", true);
        }
        int code = event.getEventCode();
        // 反馈: 0x1A总线设备反馈/0x04火警反馈 置位, 0x1C反馈撤销 清零
        if (code == YwnEventCode.BUS_DEVICE_FEEDBACK.getCode()
                || code == YwnEventCode.FIRE_ALARM_FEEDBACK.getCode()) {
            props.put("feedback_status", true);
        }
        if (code == YwnEventCode.BUS_DEVICE_FEEDBACK_CANCEL.getCode()) {
            props.put("feedback_status", false);
        }
        // 屏蔽类: 0x30~0x3F
        if (code >= 0x30 && code <= 0x3F) {
            props.put("shield_status", true);
        }
        // 启动/停止
        if (code == YwnEventCode.BUS_DEVICE_STARTED.getCode()
                || code == YwnEventCode.FIRE_ALARM_START.getCode()) {
            props.put("start_status", true);
        }
        if (code == YwnEventCode.BUS_DEVICE_STOPPED.getCode()) {
            props.put("start_status", false);
        }
        // 故障恢复
        if (code == YwnEventCode.BUS_DEVICE_FAULT_RECOVERY.getCode()) {
            props.put("fault_status", false);
        }
        // 监管报警
        if (code == YwnEventCode.SUPERVISORY.getCode()) {
            props.put("supervisory_status", true);
        }
    }

    /**
     * 指令驱动状态清零: 协议无对应恢复事件的状态, 由下发指令主动清零
     * 复位(0x02) → 清火警/启动/反馈/监管; 取消屏蔽(0x08) → 清屏蔽
     */
    private void reportCommandDrivenStatus(String dn, int commandCode) {
        String pk = dnToPk.get(dn);
        if (pk == null) {
            return;
        }
        Map<String, Object> props = new LinkedHashMap<>();
        if (commandCode == DataYwnPackage.CMD_RESET) {
            props.put("fire_status", false);
            props.put("start_status", false);
            props.put("feedback_status", false);
            props.put("supervisory_status", false);
        } else if (commandCode == DataYwnPackage.CMD_CANCEL_SHIELD) {
            props.put("shield_status", false);
        }
        if (!props.isEmpty()) {
            report(PropertyReport.builder()
                    .productKey(pk)
                    .deviceName(dn)
                    .params(props)
                    .build());
        }
    }

    /**
     * 属性设置 → 通过 params 中的 ywnCommand 字段指定下发命令
     *
     * params 约定:
     *   ywnCommand: 命令码 (整数或十六进制字符串, 如 2=复位, 4=启动总线)
     *   machineNo:  机号 (默认1)
     *   loopNo:     回路号 (默认0)
     *   addressNo:  地址号 (默认0)
     */
    @Override
    protected void propertySet(PropertySet action) {
        log.info("[YWN] 属性设置: device={}, params={}", action.getDeviceName(), action.getParams());
        Map<String, ?> params = action.getParams();
        if (params == null) return;

        String dn = action.getDeviceName();
        int deviceNumber = parseDeviceNumber(dn);
        if (deviceNumber <= 0) {
            log.warn("[YWN] 无法解析设备编号: {}", dn);
            return;
        }

        int ywnCommand = getIntParam(params, "ywnCommand", 0);
        if (ywnCommand <= 0) {
            log.warn("[YWN] 属性设置缺少 ywnCommand 参数");
            return;
        }

        int machineNo = getIntParam(params, "machineNo", 1);
        int loopNo = getIntParam(params, "loopNo", 0);
        int addressNo = getIntParam(params, "addressNo", 0);

        Buffer msg = DataYwnEncoder.encodeControl(ywnCommand, deviceNumber, machineNo, loopNo, addressNo);
        sendMsg(dn, msg);
    }

    @Override
    protected void propertyGet(PropertyGet action) {
        // YWN协议不支持属性查询
        log.info("[YWN] 属性获取(协议不支持): {}", action);
    }

    @Override
    protected void deviceOta(DeviceOta action) {
        // YWN协议不支持OTA
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
        tcpYwnVerticle.setTcpYwnComponent(this);

        if (!enable) {
            tcpYwnVerticle.stopServer();
            return true;
        }

        if (config == null) {
            return false;
        }

        TcpYwnConfig tcpConfig = JsonUtils.parseObject(config, TcpYwnConfig.class);
        if (tcpConfig == null) {
            log.error("[YWN] 解析配置失败");
            return false;
        }

        // 初始化设备注册策略
        this.allowAutoRegister = Boolean.TRUE.equals(tcpConfig.getAllowAutoRegister());
        this.fallbackProductKey = tcpConfig.getFallbackProductKey();

        tcpYwnVerticle.stopServer();
        tcpYwnVerticle.startServer(tcpConfig);
        return true;
    }

    // ==================== 设备管理 ====================

    /**
     * 确保设备已注册
     *
     * 策略:
     * 1. 优先通过 deviceName(设备编号) 查询设备是否已在平台预注册
     * 2. 如果 allowAutoRegister=true, 则使用 fallbackProductKey 自动注册
     * 3. 如果 allowAutoRegister=false 且设备未预注册, 则拒绝接入
     */
    private void ensureDeviceRegistered(String deviceAddr) {
        if (dnToPk.containsKey(deviceAddr)) {
            return;
        }

        // 策略1: 通过 deviceName 查询设备是否已预注册
        DeviceInfo existingDevice = deviceApi.getDeviceByDn(deviceAddr);
        if (existingDevice != null) {
            String pk = existingDevice.getProductKey();
            dnToPk.put(deviceAddr, pk);
            cacheDeviceComponentInfo(pk, deviceAddr);
            log.info("[YWN] 设备已预注册: pk={}, dn={}, tenantId={}",
                    pk, deviceAddr, existingDevice.getTenantId());
            return;
        }

        // 检查是否允许自动注册
        if (!allowAutoRegister) {
            log.warn("[YWN] 设备 {} 未预注册且不允许自动注册,拒绝接入。请先在平台创建设备", deviceAddr);
            return;
        }

        // 策略2: 允许自动注册
        String pk = fallbackProductKey;
        if (pk == null || pk.isEmpty()) {
            log.warn("[YWN] 未配置 fallbackProductKey,无法自动注册设备 {}", deviceAddr);
            return;
        }

        dnToPk.put(deviceAddr, pk);
        RegisterDevice build = RegisterDevice.builder()
                .productKey(pk)
                .deviceName(deviceAddr)
                .build();
        DeviceInfo registered = deviceApi.registerDevice(build);
        if (registered != null) {
            cacheDeviceComponentInfo(pk, deviceAddr);
            log.info("[YWN] 设备自动注册成功: pk={}, dn={}", pk, deviceAddr);
        } else {
            log.error("[YWN] 设备自动注册失败: dn={}", deviceAddr);
            dnToPk.remove(deviceAddr);
        }
    }

    /**
     * 设备上线
     */
    public void online(String addr) {
        heartbeatDevice.put(addr, System.currentTimeMillis());
        String pk = dnToPk.get(addr);
        if (pk != null && !pk.isEmpty()) {
            report(DeviceStateChange.builder()
                    .deviceName(addr)
                    .productKey(pk)
                    .state(DeviceState.ONLINE)
                    .build());
        }
    }

    // ==================== 离线检测 ====================

    /**
     * 定时离线检测
     * 检查心跳超时和客户端连接状态
     */
    @Scheduled(fixedRate = 40, timeUnit = TimeUnit.SECONDS)
    private void offlineCheckTask() {
        log.debug("[YWN] 执行离线检测");

        // 检查 TCP 客户端是否在线
        Set<String> clients = new HashSet<>(clientMap.keySet());
        for (String key : clients) {
            VertxTcpYwnClient client = clientMap.get(key);
            if (client != null && !client.isOnline()) {
                client.shutdown();
                clientMap.remove(key);
            }
        }

        // 检查心跳超时
        Set<String> heartbeatKeys = new HashSet<>(heartbeatDevice.keySet());
        heartbeatKeys.forEach(addr -> {
            Long time = heartbeatDevice.get(addr);
            if (time != null && System.currentTimeMillis() - time > keepAliveTimeout) {
                heartbeatDevice.remove(addr);
                String pk = dnToPk.get(addr);
                if (pk != null) {
                    log.warn("[YWN] 设备离线(心跳超时): dev={}", addr);
                    report(DeviceStateChange.builder()
                            .productKey(pk)
                            .deviceName(addr)
                            .state(DeviceState.OFFLINE)
                            .time(System.currentTimeMillis())
                            .build());
                }
            }
        });
    }

    // ==================== 工具方法 ====================

    /**
     * 发送 ACK 应答
     */
    private void sendAck(String deviceAddr, NetSocket socket) {
        int deviceNumber = parseDeviceNumber(deviceAddr);
        Buffer ack = DataYwnEncoder.encodeAck(deviceNumber);
        socket.write(ack, r -> {
            if (r.succeeded()) {
                log.debug("[YWN] ACK发送成功: dev={}", deviceAddr);
            } else {
                log.error("[YWN] ACK发送失败: dev={}", deviceAddr, r.cause());
            }
        });
    }

    /**
     * 向指定设备发送消息
     */
    public void sendMsg(String addr, Buffer msg) {
        VertxTcpYwnClient tcpClient = clientMap.get(addr);
        if (tcpClient != null && tcpClient.socket != null) {
            tcpClient.sendMessage(msg);
            log.info("[YWN] 已发送: dev={}, len={}", addr, msg.length());
        } else {
            log.warn("[YWN] 设备 {} 不在线，无法发送", addr);
        }
    }

    /**
     * 解析设备编号（deviceName 即为设备编号的字符串形式）
     */
    private int parseDeviceNumber(String deviceName) {
        if (deviceName == null || deviceName.isEmpty()) return -1;
        try {
            return Integer.parseInt(deviceName);
        } catch (NumberFormatException e) {
            log.warn("[YWN] 设备编号解析失败: {}", deviceName);
            return -1;
        }
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

    @Override
    public String getType() {
        return "ywn-tcp";
    }

    @Override
    public String getName() {
        return "YWN控制器监控TCP协议V27组件";
    }
}
