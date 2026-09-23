package com.enjoyiot.eiot.component.ff.service;

import cn.hutool.core.util.IdUtil;
import com.enjoyiot.eiot.component.core.ComponentServices;
import com.enjoyiot.eiot.component.core.ThingComponent;
import com.enjoyiot.eiot.component.core.model.down.*;
import com.enjoyiot.eiot.component.core.model.up.*;
import com.enjoyiot.eiot.component.ff.cilent.GbtVertxTcpClient;
import com.enjoyiot.eiot.component.ff.model.GbtTcpConfig;
import com.enjoyiot.eiot.component.ff.model.PendingRequest;
import com.enjoyiot.eiot.component.ff.parser.GbtConstants;
import com.enjoyiot.eiot.component.ff.parser.GbtDataDecoder;
import com.enjoyiot.eiot.component.ff.parser.GbtDataEncoder;
import com.enjoyiot.eiot.component.ff.parser.GbtDataPackage;
import com.enjoyiot.eiot.component.ff.parser.GbtDataParser;
import com.enjoyiot.eiot.component.ff.parser.GbtDataReader;
import com.enjoyiot.eiot.component.ff.parser.RemoteResponseParser;
import com.enjoyiot.eiot.component.dahua.common.constants.DahuaProtocolConstants;
import com.enjoyiot.eiot.component.dahua.common.parser.DahuaAduBuilder;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.enjoyiot.module.eiot.api.product.ProductApi;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 1. TCP 连接管理（accept、close）
 * 2. 协议报文分发（command 路由）
 * 3. 委托数据上报 → {@link GbtDataReporter}
 * 4. 委托设备管理 → {@link GbtDeviceManager}
 * 5. 委托下行指令 → {@link GbtCommandSender}
 * 6. 平台下发动作（propertySet/propertyGet/serviceInvoke）
 */
@Slf4j
@Component("gbtTcpComponent1")
public class GbtTcpComponent extends ThingComponent implements Handler<NetSocket> {

    private final GbtTcpVerticle gbtTcpVerticle;
    private final ProductApi productApi;
    private final DeviceApi deviceApi;
    private final PendingRequestManager pendingRequestManager;

    private Vertx vertx;
    private NetServer server;

    /** 监控中心地址(6字节) */
    private byte[] centerAddr = new byte[GbtConstants.ADDRESS_LENGTH];

    /** 协议模式：fourfaith(默认) | dahua */
    private String protocolMode = "fourfaith";

    /** 委托组件 */
    private GbtDeviceManager deviceManager;
    private GbtDataReporter dataReporter;
    private GbtCommandSender commandSender;

    @Setter
    private long keepAliveTimeout = Duration.ofSeconds(600).toMillis();

    protected GbtTcpComponent(
            GbtTcpVerticle gbtTcpVerticle,
            ProductApi productApi,
            DeviceApi deviceApi,
            PendingRequestManager pendingRequestManager,
            ComponentServices componentServices
    ) {
        super(componentServices);
        this.gbtTcpVerticle = gbtTcpVerticle;
        this.productApi = productApi;
        this.deviceApi = deviceApi;
        this.pendingRequestManager = pendingRequestManager;
    }

    public void setTcpServer(NetServer server, Vertx vertx) {
        this.vertx = vertx;
        this.server = server;
        server.connectHandler(this::handle);
    }

    /**
     * 初始化委托组件（在 stateChange 中配置加载后调用）
     */
    private void initDelegates(GbtTcpConfig tcpConfig) {
        this.deviceManager = new GbtDeviceManager(this, deviceApi);
        this.deviceManager.updateConfig(tcpConfig);
        this.deviceManager.setOfflineTimeoutMs(keepAliveTimeout * 2);

        this.dataReporter = new GbtDataReporter(this);

        this.commandSender = new GbtCommandSender(deviceManager, centerAddr);
    }

    // ==================== 连接处理 ====================

    /**
     * 处理客户端连接
     * @param socket  the event to handle
     */
    @Override
    public void handle(NetSocket socket) {
        log.info("[TCP协议] 收到客户端连接: {}", socket.remoteAddress());
        // 生成客户端ID
        String clientId = IdUtil.simpleUUID() + "_" + socket.remoteAddress();
        GbtVertxTcpClient client = new GbtVertxTcpClient(clientId);
        client.setKeepAliveTimeoutMs(keepAliveTimeout);

        try {
            socket.exceptionHandler(err -> log.error("[TCP协议] TCP异常: {}", err.getMessage()))
                    .closeHandler(nil -> {
                        log.warn("[TCP协议] 客户端断开: {}", socket.remoteAddress());
                        client.shutdown();
                        if (deviceManager != null) {
                            deviceManager.removeClient(client);
                        }
                    });

            RecordParser parser = GbtDataReader.getParser(buffer -> {
                try {
                    log.info("[TCP协议] 收到原始报文: hex={}", bytesToHex(buffer.getBytes()));
                    //解析数据包内容
                    GbtDataPackage pkg = GbtDataDecoder.decode(buffer);
                    if (pkg == null) {
                        log.warn("[TCP协议] 数据包解码失败，丢弃");
                        return;
                    }
                    //处理解析后的数据
                    processPacket(pkg, client, socket);
                } catch (Exception e) {
                    log.error("[TCP协议] 处理数据包异常", e);
                }
            });
            client.setParser(parser);
            client.setSocket(socket);
            log.info("[TCP协议] 客户端已接入: {}", socket.remoteAddress());
        } catch (Exception e) {
            log.error("[TCP协议] 连接初始化失败", e);
            client.shutdown();
        }
    }

    // ==================== 协议报文处理 ====================

    private void processPacket(GbtDataPackage pkg, GbtVertxTcpClient client, NetSocket socket) {
        String rawSourceAddr = pkg.getSourceAddrHex();
        String deviceAddr = GbtDataPackage.reverseHexAddr(rawSourceAddr);

        // 心跳包特殊处理
        if (pkg.isHeartbeat()) {
            handleHeartbeat(deviceAddr, client, pkg);
            return;
        }

        byte command = pkg.getCommand();
        int cmdInt = command & 0xFF;
        int typeFlag = (pkg.getApplicationData() != null && pkg.getApplicationData().length > 0)
                ? (pkg.getApplicationData()[0] & 0xFF) : 0;
        int aduLen = pkg.getApplicationData() != null ? pkg.getApplicationData().length : 0;
        log.info("[TCP] ← 设备={} CMD=0x{} type=0x{} len={}",
                deviceAddr, String.format("%02X", cmdInt), String.format("%02X", typeFlag), aduLen);

        // 绑定客户端映射 + 更新活跃时间
        deviceManager.bindClient(deviceAddr, client);

        // 首次见到此设备 → 自动注册
        String pk = deviceManager.ensureDeviceRegistered(deviceAddr);
        if (pk != null) {
            cacheDeviceComponentInfo(pk, deviceAddr);
        }
        deviceManager.online(deviceAddr);

        // 按协议模式路由
        if ("dahua".equalsIgnoreCase(protocolMode)) {
            processDahuaPacket(pkg, deviceAddr, cmdInt);
        } else {
            processFourfaithPacket(pkg, deviceAddr, cmdInt);
        }
    }

    /**
     * Fourfaith 协议路由（原有逻辑）
     */
    private void processFourfaithPacket(GbtDataPackage pkg, String deviceAddr, int cmdInt) {
        switch (cmdInt) {
            case GbtConstants.CMD_SEND_DATA:
                handleDataUpload(pkg, deviceAddr);
                commandSender.sendConfirm(deviceAddr, pkg);
                break;

            case GbtConstants.CMD_CONFIRM:
                handleConfirm(pkg, deviceAddr);
                break;

            case GbtConstants.CMD_RESPONSE:
                handleResponse(pkg, deviceAddr);
                break;

            case GbtConstants.CMD_DENY:
                handleDeny(pkg, deviceAddr);
                break;

            case GbtConstants.CMD_CONTROL:
                log.info("[TCP协议] 收到控制命令: 设备={}", deviceAddr);
                handleDataUpload(pkg, deviceAddr);
                commandSender.sendConfirm(deviceAddr, pkg);
                break;

            // 国标框架心跳（CMD=128/129）
            case GbtConstants.CMD_HEARTBEAT_ACK:
            case GbtConstants.CMD_HEARTBEAT_NO_ACK:
                handleStandardHeartbeat(deviceAddr, pkg, cmdInt);
                break;

            default:
                log.warn("[TCP协议] 未识别的命令字节: {}", cmdInt);
                break;
        }
    }

    /**
     * 大华消防协议 v1.04 二维路由（命令字 + 类型标志）
     * <p>
     * 表 1-1 定义的 33 种帧类型完整实现。
     * 设备→平台方向：0x00/0x02/0x05/0x82；平台→设备方向：0x01/0x03/0x04/0x06/0x07/0x81（上行不应收到，仅记录日志）。
     */
    private void processDahuaPacket(GbtDataPackage pkg, String deviceAddr, int cmdInt) {
        byte[] appData = pkg.getApplicationData();
        int typeFlag = (appData != null && appData.length > 0) ? (appData[0] & 0xFF) : -1;

        switch (cmdInt) {
            case DahuaProtocolConstants.CMD_REGISTER:
                switch (typeFlag) {
                    case DahuaProtocolConstants.TYPE_REGISTER:
                        handleDahuaRegister(deviceAddr, appData, pkg);
                        break;
                    case DahuaProtocolConstants.TYPE_REGISTER_EXT:
                        log.info("[TCP] 注册扩展: dev={}", deviceAddr);
                        reportRawDahua(deviceAddr, "dahua_register_ext", appData);
                        break;
                    default:
                        log.warn("[TCP] 注册未知: dev={}, type=0x{}", deviceAddr, String.format("%02X", typeFlag));
                        break;
                }
                break;

            case DahuaProtocolConstants.CMD_CLOCK_SYNC:
                log.warn("[TCP] 时钟同步(下行误收): dev={}", deviceAddr);
                break;

            case DahuaProtocolConstants.CMD_DEVICE_REPORT:
                handleDahuaDeviceReport(deviceAddr, typeFlag, appData, pkg);
                break;

            case DahuaProtocolConstants.CMD_CONFIRM:
                log.info("[TCP] 确认: dev={}", deviceAddr);
                handleConfirm(pkg, deviceAddr);
                break;

            case DahuaProtocolConstants.CMD_PLATFORM_COMMAND:
                log.info("[TCP] 下行(误收): dev={}, type=0x{}", deviceAddr, String.format("%02X", typeFlag));
                break;

            case DahuaProtocolConstants.CMD_DEVICE_RESPONSE:
                handleDahuaDeviceResponse(deviceAddr, typeFlag, appData, pkg);
                break;

            case DahuaProtocolConstants.CMD_DENY:
                log.warn("[TCP] 否认: dev={}", deviceAddr);
                handleDeny(pkg, deviceAddr);
                break;

            case DahuaProtocolConstants.CMD_KEEPALIVE:
                deviceManager.updateActive(deviceAddr);
                log.info("[TCP] 保活: dev={}", deviceAddr);
                // 协议 2.2.3.1：平台收到保活帧且校验正确必须回确认帧，否则设备启动重发
                commandSender.sendConfirm(deviceAddr, pkg);
                break;

            case DahuaProtocolConstants.CMD_FUNCTION_CONFIG:
                log.info("[TCP] 功能配置(下行误收): dev={}", deviceAddr);
                break;

            case DahuaProtocolConstants.CMD_PARAM_RESPONSE:
                if (typeFlag == DahuaProtocolConstants.TYPE_PARAM_RESPONSE) {
                    handleDahuaParamResponse(deviceAddr, appData, pkg);
                } else {
                    log.warn("[TCP] 带参应答未知: dev={}, type=0x{}", deviceAddr, String.format("%02X", typeFlag));
                }
                break;

            default:
                log.warn("[TCP] 未知CMD: cmd=0x{}, dev={}", String.format("%02X", cmdInt), deviceAddr);
                break;
        }
    }

    /**
     * 大华 CMD=0x00 注册帧处理
     */
    private void handleDahuaRegister(String deviceAddr, byte[] appData, GbtDataPackage pkg) {
        log.info("[TCP] 注册: dev={}, aduLen={}", deviceAddr, appData != null ? appData.length : 0);
        deviceManager.updateActive(deviceAddr);
        reportRawDahua(deviceAddr, "dahua_register", appData);
        commandSender.sendConfirm(deviceAddr, pkg);
        log.info("[TCP] → 确认帧: dev={}", deviceAddr);
    }

    /**
     * 大华 CMD=0x02 设备上报帧路由（12种子类型）
     * ponytail: 10 个非通用 typeFlag 行为相同，合并为 default
     */
    private void handleDahuaDeviceReport(String deviceAddr, int typeFlag, byte[] appData, GbtDataPackage pkg) {
        log.info("[TCP] 上报: dev={}, type=0x{}, aduLen={}", deviceAddr, String.format("%02X", typeFlag),
                appData != null ? appData.length : 0);
        try {
            handleDataUpload(pkg, deviceAddr);
        } finally {
            // 协议 5.1：信息上传=发送/确认，解析异常也必须回确认，否则设备按 5.2 重发
            commandSender.sendConfirm(deviceAddr, pkg);
            log.info("[TCP] → 确认帧: dev={}", deviceAddr);
        }
    }

    /**
     * 大华 CMD=0x05 设备应答帧路由（4种子类型）
     * ponytail: 4 个已知 typeFlag 行为相同，合并为 default
     */
    private void handleDahuaDeviceResponse(String deviceAddr, int typeFlag, byte[] appData, GbtDataPackage pkg) {
        switch (typeFlag) {
            case DahuaProtocolConstants.TYPE_PARAM_RESPONSE_FIXED:
            case DahuaProtocolConstants.TYPE_PARAM_RESPONSE_VARIABLE:
            case DahuaProtocolConstants.TYPE_QUERY_UNIVERSAL_RESPONSE:
            case DahuaProtocolConstants.TYPE_QUERY_CAPABILITY_RESPONSE:
                log.info("[TCP] 应答: dev={}, type=0x{}", deviceAddr, String.format("%02X", typeFlag));
                handleResponse(pkg, deviceAddr);
                break;
            default:
                log.warn("[TCP] 应答未知: dev={}, type=0x{}", deviceAddr, String.format("%02X", typeFlag));
                reportRawDahua(deviceAddr, "dahua_unknown_response", appData);
                break;
        }
    }

    /**
     * 大华 CMD=0x82 带参应答帧处理
     */
    private void handleDahuaParamResponse(String deviceAddr, byte[] appData, GbtDataPackage pkg) {
        log.info("[TCP] 带参应答: dev={}, aduLen={}", deviceAddr,
                appData != null ? appData.length : 0);
        handleResponse(pkg, deviceAddr);
    }

    /**
     * 大华原始数据上报（用于未完全解析的帧类型）
     */
    private void reportRawDahua(String deviceAddr, String eventName, byte[] rawAdu) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rawHex", bytesToHex(rawAdu));
        data.put("aduLength", rawAdu != null ? rawAdu.length : 0);
        String pk = deviceManager.getProductKey(deviceAddr);
        if (pk != null) {
            report(PropertyReport.builder()
                    .productKey(pk)
                    .deviceName(deviceAddr)
                    .params(data)
                    .build());
        }
    }

    // ==================== 心跳处理 ====================

    private void handleHeartbeat(String deviceAddr, GbtVertxTcpClient client, GbtDataPackage pkg) {
        log.warn("[TCP协议] 收到旧版心跳包: 设备={}", deviceAddr);

        // 首次见到此设备 → 自动注册
        String pk = deviceManager.ensureDeviceRegistered(deviceAddr);
        if (pk != null) {
            cacheDeviceComponentInfo(pk, deviceAddr);
        }
        deviceManager.bindClient(deviceAddr, client);
        deviceManager.online(deviceAddr);

        // 旧版心跳应答：回显设备发来的通讯模块和系统类型（parseHeartbeat 已提取保存）
        commandSender.sendLegacyHeartbeatReply(deviceAddr, client, pkg.getCommModule(), pkg.getSysType());
    }

    /**
     * 处理国标框架心跳（CMD=128/129）
     */
    private void handleStandardHeartbeat(String deviceAddr, GbtDataPackage pkg, int command) {
        log.info("[TCP协议] 收到国标心跳: 设备={}, CMD={}", deviceAddr, command);

        deviceManager.updateActive(deviceAddr);

        // CMD=128 需要应答（CMD=130），CMD=129 不需要应答
        if (command == GbtConstants.CMD_HEARTBEAT_ACK) {
            // 从应用数据中提取通讯模块和系统类型
            byte commModule = 0;
            byte sysType = 0;
            byte[] appData = pkg.getApplicationData();
            if (appData != null && appData.length >= 2) {
                commModule = appData[0];
                sysType = appData[1];
            }
            commandSender.sendHeartbeatReply(deviceAddr, commModule, sysType);
        }
    }

    // ==================== 确认/否认/应答处理 ====================

    private void handleConfirm(GbtDataPackage pkg, String deviceAddr) {
        int confirmSn = pkg.getSerialNumber();
        PendingRequest req = pendingRequestManager.get(confirmSn);
        if (req != null) {
            log.info("[TCP协议] 收到确认: 设备={}, SN={}, 服务={}",
                deviceAddr, confirmSn, req.getServiceName());
        } else {
            log.info("[TCP协议] 收到确认: 设备={}, SN={}", deviceAddr, confirmSn);
        }
    }

    private void handleDeny(GbtDataPackage pkg, String deviceAddr) {
        int denySn = pkg.getSerialNumber();
        PendingRequest req = pendingRequestManager.remove(denySn);
        if (req != null && "remote_control".equals(req.getServiceName())) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("success", 0);
            output.put("result_desc", "设备拒绝执行");
            report(ServiceReply.builder()
                .productKey(req.getProductKey())
                .deviceName(req.getDeviceName())
                .replyId(req.getInvokeId())
                .code(0)
                .params(output)
                .build());
        }
        log.warn("[TCP协议] 收到否认: 设备={}, SN={}", deviceAddr, denySn);
    }

    private void handleResponse(GbtDataPackage pkg, String deviceAddr) {
        byte[] appData = pkg.getApplicationData();
        if (appData == null || appData.length < 2) return;

        int typeFlag = GbtDataParser.getTypeFlag(appData);
        log.info("[TCP协议] 收到应答: 设备={}, typeFlag={}, 应用数据HEX={}",
                deviceAddr, typeFlag, GbtDataPackage.bytesToHex(appData));

        if (typeFlag == GbtConstants.TYPE_UP_REMOTE_RESPONSE) {
            // 四信远程控制应答：精确解析应答状态并回复 ServiceReply
            handleRemoteControlResponse(pkg, deviceAddr);
        } else {
            // 其他应答帧（含大华参数查询应答/通用应答等）：上报属性
            handleQueryResponse(pkg, deviceAddr);
            // 大华异步下行服务（remote_mute/query_universal/config_universal）的应答闭环
            matchAndReplyDahuaPending(pkg);
        }
    }

    /**
     * 大华异步下行服务的应答闭环
     */
    private void matchAndReplyDahuaPending(GbtDataPackage pkg) {
        int sn = pkg.getSerialNumber();
        PendingRequest req = pendingRequestManager.remove(sn);
        if (req == null) return;
        String serviceName = req.getServiceName();
        if (serviceName == null || "remote_control".equals(serviceName)) return;

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("success", 1);
        output.put("result_desc", "设备已应答");
        report(ServiceReply.builder()
            .productKey(req.getProductKey())
            .deviceName(req.getDeviceName())
            .replyId(req.getInvokeId())
            .code(0)
            .params(output)
            .build());
        log.info("[TCP协议] 大华异步服务应答闭环: SN={}, service={}", sn, serviceName);
    }

    private void handleRemoteControlResponse(GbtDataPackage pkg, String deviceAddr) {
        int serialNumber = pkg.getSerialNumber();
        PendingRequest req = pendingRequestManager.remove(serialNumber);
        byte[] appData = pkg.getApplicationData();
        log.info("[TCP协议] 远程控制应答: 设备={}, SN={}, req={}, 应用数据HEX={}",
                deviceAddr, serialNumber, req != null ? req.getServiceName() : "无",
                GbtDataPackage.bytesToHex(appData));

        Map<String, Object> resp = RemoteResponseParser.parse(appData);
        if (resp.isEmpty()) return;

        if (req != null && "remote_control".equals(req.getServiceName())) {
            Map<String, Object> output = new LinkedHashMap<>();
            int success = (Integer) resp.getOrDefault("resp_success", 0);
            output.put("success", success);
            output.put("result_code", resp.get("resp_status_code"));
            output.put("result_desc", success == 1 ? "执行成功" : "执行失败");

            report(ServiceReply.builder()
                .productKey(req.getProductKey())
                .deviceName(req.getDeviceName())
                .replyId(req.getInvokeId())
                .code(0)
                .params(output)
                .build());
        } else if (req == null) {
            log.warn("[TCP协议] 收到未知流水号的远程应答: SN={}", serialNumber);
        }
    }

    private void handleQueryResponse(GbtDataPackage pkg, String deviceAddr) {
        byte[] appData = pkg.getApplicationData();
        if (appData == null || appData.length < 2) return;

        int typeFlag = GbtDataParser.getTypeFlag(appData);
        List<Map<String, Object>> entries = GbtDataParser.parse(appData);
        String pk = deviceManager.getProductKey(deviceAddr);
        if (pk == null) return;

        log.info("[TCP协议] 查询应答: 设备={}, 类型={}, 记录数={}", deviceAddr, typeFlag, entries.size());
        for (Map<String, Object> entry : entries) {
            report(PropertyReport.builder()
                    .productKey(pk)
                    .deviceName(deviceAddr)
                    .params(entry)
                    .build());
        }
    }

    // ==================== 数据上传处理（委托 DataReporter） ====================

    private void handleDataUpload(GbtDataPackage pkg, String deviceAddr) {
        byte[] appData = pkg.getApplicationData();
        if (appData == null || appData.length < 2) {
            log.warn("[TCP协议] 数据上传无应用数据单元");
            return;
        }

        log.info("[TCP协议] 收到数据上传: 设备={}, 应用数据HEX={}",
                deviceAddr, GbtDataPackage.bytesToHex(appData));

        String pk = deviceManager.getProductKey(deviceAddr);
        dataReporter.handleDataUpload(pkg, deviceAddr, pk, null,
                (parentAddr, parentPk, hostNo, circuitNo, addressNo) ->
                        deviceManager.ensureComponentRegistered(parentAddr, parentPk, hostNo, circuitNo, addressNo));
    }

    // ==================== 平台下发动作 ====================

    @Override
    public boolean stateChange(boolean enable, String config) {
        gbtTcpVerticle.setTcpComponent(this);

        if (!enable) {
            gbtTcpVerticle.stopServer();
            return true;
        }

        if (config == null) {
            return false;
        }

        GbtTcpConfig tcpConfig = JsonUtils.parseObject(config, GbtTcpConfig.class);
        if (tcpConfig == null) {
            log.error("[TCP协议] 解析配置失败");
            return false;
        }

        // 初始化监控中心地址
        if (tcpConfig.getCenterAddr() != null && !tcpConfig.getCenterAddr().isEmpty()) {
            this.centerAddr = GbtDataPackage.hexToAddr(tcpConfig.getCenterAddr());
        }

        // 协议模式
        this.protocolMode = tcpConfig.getProtocolMode() != null ? tcpConfig.getProtocolMode() : "dahua";
        log.info("[TCP协议] 协议模式: {}", this.protocolMode);

        // 初始化委托组件
        initDelegates(tcpConfig);

        gbtTcpVerticle.stopServer();
        gbtTcpVerticle.startServer(tcpConfig);
        return true;
    }

    @Override
    protected void propertySet(PropertySet action) {
        log.info("[TCP协议] 属性设置: {}", action);
        Map<String, ?> params = action.getParams();
        if (params == null) return;

        int gbtCommand = getIntParam(params, "gbtCommand", GbtConstants.CMD_REQUEST);
        int gbtTypeFlag = getIntParam(params, "gbtTypeFlag", 0);
        byte[] body = null;
        Object bodyObj = params.get("gbtBody");
        if (bodyObj instanceof String && !((String) bodyObj).isEmpty()) {
            body = hexStringToBytes((String) bodyObj);
        }

        if (gbtCommand == GbtConstants.CMD_CONTROL) {
            commandSender.sendControl(action.getDeviceName(), gbtTypeFlag, body);
        } else {
            commandSender.sendQuery(action.getDeviceName(), gbtTypeFlag, body);
        }
    }

    @Override
    protected void propertyGet(PropertyGet action) {
        log.info("[TCP协议] 属性获取: {}", action);
        List<String> keys = action.getKeys();
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            int queryType = mapPropertyKeyToQueryType(key);
            if (queryType > 0) {
                commandSender.sendQuery(action.getDeviceName(), queryType, null);
            }
        }
    }

    @Override
    protected void serviceInvoke(ServiceInvoke action) {
        log.info("[TCP协议] 服务调用: name={}", action.getName());
        String serviceName = action.getName();
        String dn = action.getDeviceName();
        String pk = action.getProductKey();

        switch (serviceName) {
            case "syncTime": {
                byte[] ts = GbtDataEncoder.buildCurrentTimestamp();
                commandSender.sendControl(dn, GbtConstants.TYPE_DOWN_SYNC_TIME, ts);
                break;
            }
            case "initDevice": {
                commandSender.sendControl(dn, GbtConstants.TYPE_DOWN_INIT_DEVICE, null);
                break;
            }
            case "inspection_cmd": {
                commandSender.sendControl(dn, GbtConstants.TYPE_DOWN_INSPECT, null);
                break;
            }
            case "query": {
                Map<String, ?> params = action.getParams();
                int typeFlag = getIntParam(params, "typeFlag", 0);
                if (typeFlag > 0) {
                    commandSender.sendQuery(dn, typeFlag, null);
                }
                break;
            }
            case "remote_control": {
                handleRemoteControl(action, dn, pk);
                break;
            }

            // ── 大华协议 v1.04 下行服务 ──
            case "dahua_clock_sync": {
                commandSender.sendDahuaClockSync(dn);
                break;
            }
            case "dahua_set_param": {
                handleDahuaSetParam(action, dn);
                break;
            }
            case "dahua_query_param": {
                handleDahuaQueryParam(action, dn);
                break;
            }
            case "dahua_remote_mute": {
                handleDahuaRemoteMute(action, dn, pk);
                break;
            }
            case "dahua_query_universal": {
                handleDahuaQueryUniversal(action, dn, pk);
                break;
            }
            case "dahua_config_universal": {
                handleDahuaConfigUniversal(action, dn, pk);
                break;
            }
            default:
                log.warn("[TCP协议] 未知服务: {}", serviceName);
        }
    }

    // ==================== 大华协议 v1.04 下行服务实现 ====================

    /**
     * 大华设置参数：根据 variable 参数选择定长(0x64)/不定长(0x81)
     * params: paramType(int), paramValue(hex string), variable(bool, optional, default false)
     */
    private void handleDahuaSetParam(ServiceInvoke action, String dn) {
        Map<String, ?> params = action.getParams();
        int paramType = getIntParam(params, "paramType", 0);
        String paramHex = getStringParam(params, "paramValue", "");
        boolean variable = Boolean.TRUE.equals(params.get("variable"));
        byte[] value = hexStringToBytes(paramHex);
        if (variable) {
            commandSender.sendDahuaSetParamVariable(dn, paramType, value);
        } else {
            commandSender.sendDahuaSetParamFixed(dn, paramType, value);
        }
    }

    /**
     * 大华查询参数：根据 variable 参数选择定长(0x82)/不定长(0x83)
     * params: paramType(int), variable(bool, optional, default false)
     */
    private void handleDahuaQueryParam(ServiceInvoke action, String dn) {
        Map<String, ?> params = action.getParams();
        int paramType = getIntParam(params, "paramType", 0);
        boolean variable = Boolean.TRUE.equals(params.get("variable"));
        if (variable) {
            commandSender.sendDahuaQueryParamVariable(dn, paramType);
        } else {
            commandSender.sendDahuaQueryParamFixed(dn, paramType);
        }
    }

    /**
     * 大华远程消音
     * params: sysType(int), sysAddr(int), compType(int), compAddr(int)
     */
    private void handleDahuaRemoteMute(ServiceInvoke action, String dn, String pk) {
        Map<String, ?> params = action.getParams();
        int sysType = getIntParam(params, "sysType", 0);
        int sysAddr = getIntParam(params, "sysAddr", 0);
        int compType = getIntParam(params, "compType", 0);
        int compAddr = getIntParam(params, "compAddr", 0);

        int sn = commandSender.nextSerial();
        pendingRequestManager.put(sn, PendingRequest.builder()
            .invokeId(action.getId())
            .productKey(pk)
            .deviceName(dn)
            .serialNumber(sn)
            .serviceName("dahua_remote_mute")
            .sendTime(System.currentTimeMillis())
            .build());

        commandSender.sendDahuaRemoteMute(dn, sysType, sysAddr, compType, compAddr);
    }

    /**
     * 大华查询通用帧
     * params: info_objects(JSON数组, 每项含 component_type, component_addr(hex), data_type)
     */
    private void handleDahuaQueryUniversal(ServiceInvoke action, String dn, String pk) {
        Map<String, ?> params = action.getParams();
        List<DahuaAduBuilder.QueryInfoObject> infoObjects = parseQueryInfoObjects(params);
        if (infoObjects.isEmpty()) {
            log.warn("[TCP协议] dahua_query_universal 无有效信息对象: device={}", dn);
            return;
        }
        int sn = commandSender.nextSerial();
        pendingRequestManager.put(sn, PendingRequest.builder()
            .invokeId(action.getId())
            .productKey(pk)
            .deviceName(dn)
            .serialNumber(sn)
            .serviceName("dahua_query_universal")
            .sendTime(System.currentTimeMillis())
            .build());
        commandSender.sendDahuaQueryUniversal(dn, infoObjects);
    }

    /**
     * 大华设置/配置通用帧
     * params: info_objects(JSON数组, 每项含 component_type, component_addr(hex), data_type, data_content(hex))
     */
    private void handleDahuaConfigUniversal(ServiceInvoke action, String dn, String pk) {
        Map<String, ?> params = action.getParams();
        List<DahuaAduBuilder.ConfigInfoObject> infoObjects = parseConfigInfoObjects(params);
        if (infoObjects.isEmpty()) {
            log.warn("[TCP协议] dahua_config_universal 无有效信息对象: device={}", dn);
            return;
        }
        int sn = commandSender.nextSerial();
        pendingRequestManager.put(sn, PendingRequest.builder()
            .invokeId(action.getId())
            .productKey(pk)
            .deviceName(dn)
            .serialNumber(sn)
            .serviceName("dahua_config_universal")
            .sendTime(System.currentTimeMillis())
            .build());
        commandSender.sendDahuaConfigUniversal(dn, infoObjects);
    }

    /**
     * 解析查询通用帧信息对象列表
     * JSON格式: [{"component_type":1, "component_addr":"00010001", "data_type":1}, ...]
     */
    @SuppressWarnings("unchecked")
    private List<DahuaAduBuilder.QueryInfoObject> parseQueryInfoObjects(Map<String, ?> params) {
        List<DahuaAduBuilder.QueryInfoObject> result = new ArrayList<>();
        Object raw = params.get("info_objects");
        if (raw == null) return result;
        String json = JsonUtils.toJsonString(raw);
        List rawList = JsonUtils.parseArray(json, Map.class);
        List<Map<String, Object>> list = (List<Map<String, Object>>) (List<?>) rawList;
        for (Map<String, Object> item : list) {
            int compType = toInt(item.get("component_type"));
            byte[] compAddr = hexStringToBytes((String) item.get("component_addr"));
            int dataType = toInt(item.get("data_type"));
            result.add(new DahuaAduBuilder.QueryInfoObject(compType, compAddr, dataType));
        }
        return result;
    }

    /**
     * 解析配置通用帧信息对象列表
     * JSON格式: [{"component_type":1, "component_addr":"00010001", "data_type":3, "data_content":"0102"}, ...]
     */
    @SuppressWarnings("unchecked")
    private List<DahuaAduBuilder.ConfigInfoObject> parseConfigInfoObjects(Map<String, ?> params) {
        List<DahuaAduBuilder.ConfigInfoObject> result = new ArrayList<>();
        Object raw = params.get("info_objects");
        if (raw == null) return result;
        String json = JsonUtils.toJsonString(raw);
        List rawList = JsonUtils.parseArray(json, Map.class);
        List<Map<String, Object>> list = (List<Map<String, Object>>) (List<?>) rawList;
        for (Map<String, Object> item : list) {
            int compType = toInt(item.get("component_type"));
            byte[] compAddr = hexStringToBytes((String) item.get("component_addr"));
            int dataType = toInt(item.get("data_type"));
            byte[] dataContent = hexStringToBytes((String) item.get("data_content"));
            result.add(new DahuaAduBuilder.ConfigInfoObject(compType, compAddr, dataType, dataContent));
        }
        return result;
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private void handleRemoteControl(ServiceInvoke action, String dn, String pk) {
        Map<String, ?> params = action.getParams();
        String commandCode = getStringParam(params, "command_code", "");

        if (commandCode.isEmpty()) {
            log.error("[TCP协议] 远程控制缺少指令码: device={}", dn);
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("success", 0);
            output.put("result_desc", "缺少指令码");
            report(ServiceReply.builder()
                .productKey(pk).deviceName(dn).replyId(action.getId())
                .code(1).params(output).build());
            return;
        }

        String deviceAddr = GbtDataPackage.reverseHexAddr(dn);
        log.info("[TCP协议] 远程控制地址转换: dn(大端)={}, 反转后(小端)={}, commandCode={}",
                dn, deviceAddr, commandCode);
        if (deviceAddr == null || deviceAddr.length() < 8) {
            log.error("[TCP协议] 远程控制设备编码格式异常: deviceAddr={}", deviceAddr);
            return;
        }
        String partAddrHex = deviceAddr.substring(0, 8);

        int sn = commandSender.nextSerial();
        log.info("[TCP协议] 远程控制: partAddrHex={}, SN={}, commandCode={}",
                partAddrHex, sn, commandCode);

        pendingRequestManager.put(sn, PendingRequest.builder()
            .invokeId(action.getId())
            .productKey(pk)
            .deviceName(dn)
            .serialNumber(sn)
            .serviceName("remote_control")
            .sendTime(System.currentTimeMillis())
            .build());

        byte[] commandBytes = hexStringToBytes(commandCode);
        byte[] appData = GbtDataEncoder.buildType81AppData(commandBytes, partAddrHex);
        commandSender.sendControl(dn, GbtConstants.TYPE_DOWN_REMOTE_CONTROL, appData);
    }

    @Override
    protected void deviceOta(DeviceOta action) {
        // GBT26875 协议不支持OTA
    }

    @Override
    protected void deviceConfig(DeviceConfig action) {
        // 暂不实现
    }

    @Override
    protected void deviceTopoChange(DeviceTopoChange action) {
        // 暂不实现
    }

    // ==================== 定时任务 ====================

    /**
     * 统一离线检测（每40秒执行一次）
     * 委托给 DeviceManager，不再维护两套独立的 Map
     */
    @Scheduled(fixedRate = 40, timeUnit = TimeUnit.SECONDS)
    private void offlineCheckTask() {
        if (deviceManager != null) {
            deviceManager.checkOffline();
        }
    }

    /**
     * 异步请求超时检测（每10秒执行一次）
     */
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void handlePendingTimeouts() {
        List<PendingRequest> timeouts = pendingRequestManager.timeoutCheck();
        for (PendingRequest req : timeouts) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("success", 0);
            output.put("result_desc", "执行超时");
            report(ServiceReply.builder()
                .productKey(req.getProductKey())
                .deviceName(req.getDeviceName())
                .replyId(req.getInvokeId())
                .code(0)
                .params(output)
                .build());
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 生成部件地址（7位十六进制）
     * 规则: hostNo(2位) + circuitNo(2位) + addressNo(3位)
     */
    static String generatePartAddress(int hostNo, int circuitNo, int addressNo) {
        return String.format("%02x%02x%03x", hostNo, circuitNo, addressNo);
    }

    private int mapPropertyKeyToQueryType(String key) {
        switch (key) {
            // 告警属性 → TLD 1 系统状态查询
            case "alarm_fire":
            case "alarm_fault":
            case "alarm_main_power":
            case "alarm_backup_power":
            case "alarm_bus_fault":
            case "alarm_shield":
            case "alarm_normal_monitor":
                return GbtConstants.TYPE_DOWN_QUERY_SYSTEM_STATUS;

            // 遥测属性 → TLD 3 模拟量查询
            case "telemetry_temperature":
            case "telemetry_pressure":
            case "telemetry_voltage":
            case "telemetry_current":
            case "telemetry_wind_speed":
            case "telemetry_residual_current":
            case "telemetry_water_level":
                return GbtConstants.TYPE_DOWN_QUERY_ANALOG_VALUE;

            // 装置告警 → TLD 21 装置状态查询
            case "alarm_device_fire":
            case "alarm_device_fault":
            case "alarm_device_main_power":
            case "alarm_device_backup_power":
            case "alarm_device_comm":
            case "alarm_device_line":
                return GbtConstants.TYPE_DOWN_QUERY_DEVICE_STATUS;

            // 设备信息 → TLD 25 装置版本查询
            case "info_firmware_ver_major":
            case "info_firmware_ver_minor":
                return GbtConstants.TYPE_DOWN_QUERY_DEVICE_VERSION;

            // 设备时间 → TLD 28 装置时间查询
            case "info_system_time":
                return GbtConstants.TYPE_DOWN_QUERY_DEVICE_TIME;

            default:
                log.warn("[TCP协议] 未知的属性key: {}", key);
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

    private String getStringParam(Map<String, ?> params, String key, String defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        if (v instanceof String) return (String) v;
        if (v != null) return v.toString();
        return defaultValue;
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private static byte[] hexStringToBytes(String hex) {
        if (hex == null || hex.isEmpty()) return new byte[0];
        hex = hex.replaceAll("\\s+", "");
        if (hex.length() % 2 != 0) hex = "0" + hex;
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    @Override
    public String getType() {
        return "GBT26875-tcp";
    }

    @Override
    public String getName() {
        return "国标用户传输协议GBT26875.3-2011-tcp协议组件";
    }
}
