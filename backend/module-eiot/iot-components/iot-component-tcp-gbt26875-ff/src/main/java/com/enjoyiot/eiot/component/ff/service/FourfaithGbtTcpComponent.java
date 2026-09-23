package com.enjoyiot.eiot.component.ff.service;

import cn.hutool.core.util.IdUtil;
import com.enjoyiot.eiot.component.core.ComponentServices;
import com.enjoyiot.eiot.component.core.ThingComponent;
import com.enjoyiot.eiot.component.core.model.down.*;
import com.enjoyiot.eiot.component.core.model.up.*;
import com.enjoyiot.eiot.component.ff.cilent.FourfaithGbtVertxTcpClient;
import com.enjoyiot.eiot.component.ff.model.FourfaithGbtTcpConfig;
import com.enjoyiot.eiot.component.ff.model.PendingRequest;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtConstants;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtDataDecoder;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtDataEncoder;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtDataPackage;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtDataParser;
import com.enjoyiot.eiot.component.ff.parser.FourfaithGbtDataReader;
import com.enjoyiot.eiot.component.ff.parser.FourfaithRemoteResponseParser;
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
 * 四信 GB/T 26875.3-2011 TCP 协议组件（门面模式）
 * <p>
 * 重构后职责：
 * 1. TCP 连接管理（accept、close）
 * 2. 协议报文分发（command 路由）
 * 3. 委托数据上报 → {@link FourfaithGbtDataReporter}
 * 4. 委托设备管理 → {@link FourfaithGbtDeviceManager}
 * 5. 委托下行指令 → {@link FourfaithGbtCommandSender}
 * 6. 平台下发动作（propertySet/propertyGet/serviceInvoke）
 */
@Slf4j
@Component
public class FourfaithGbtTcpComponent extends ThingComponent implements Handler<NetSocket> {

    private final FourfaithGbtTcpVerticle fourfaithGbtTcpVerticle;
    private final ProductApi productApi;
    private final DeviceApi deviceApi;
    private final PendingRequestManager pendingRequestManager;

    private Vertx vertx;
    private NetServer server;

    /** 监控中心地址(6字节) */
    private byte[] centerAddr = new byte[FourfaithGbtConstants.ADDRESS_LENGTH];

    /** 委托组件 */
    private FourfaithGbtDeviceManager deviceManager;
    private FourfaithGbtDataReporter dataReporter;
    private FourfaithGbtCommandSender commandSender;

    @Setter
    private long keepAliveTimeout = Duration.ofSeconds(600).toMillis();

    protected FourfaithGbtTcpComponent(
            FourfaithGbtTcpVerticle fourfaithGbtTcpVerticle,
            ProductApi productApi,
            DeviceApi deviceApi,
            PendingRequestManager pendingRequestManager,
            ComponentServices componentServices
    ) {
        super(componentServices);
        this.fourfaithGbtTcpVerticle = fourfaithGbtTcpVerticle;
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
    private void initDelegates(FourfaithGbtTcpConfig tcpConfig) {
        this.deviceManager = new FourfaithGbtDeviceManager(this, deviceApi);
        this.deviceManager.updateConfig(tcpConfig);
        this.deviceManager.setOfflineTimeoutMs(keepAliveTimeout * 2);

        this.dataReporter = new FourfaithGbtDataReporter(this);

        this.commandSender = new FourfaithGbtCommandSender(deviceManager, centerAddr);
    }

    // ==================== 连接处理 ====================

    @Override
    public void handle(NetSocket socket) {
        log.info("[四信-GBT26875] 收到客户端连接: {}", socket.remoteAddress());
        String clientId = IdUtil.simpleUUID() + "_" + socket.remoteAddress();
        FourfaithGbtVertxTcpClient client = new FourfaithGbtVertxTcpClient(clientId);
        client.setKeepAliveTimeoutMs(keepAliveTimeout);

        try {
            socket.exceptionHandler(err -> log.error("[四信-GBT26875] TCP异常: {}", err.getMessage()))
                    .closeHandler(nil -> {
                        log.warn("[四信-GBT26875] 客户端断开: {}", socket.remoteAddress());
                        client.shutdown();
                        if (deviceManager != null) {
                            deviceManager.removeClient(client);
                        }
                    });

            RecordParser parser = FourfaithGbtDataReader.getParser(buffer -> {
                try {
                    log.info("[四信-GBT26875] 收到原始报文: hex={}", bytesToHex(buffer.getBytes()));
                    FourfaithGbtDataPackage pkg = FourfaithGbtDataDecoder.decode(buffer);
                    if (pkg == null) {
                        log.warn("[四信-GBT26875] 数据包解码失败，丢弃");
                        return;
                    }
                    processPacket(pkg, client, socket);
                } catch (Exception e) {
                    log.error("[四信-GBT26875] 处理数据包异常", e);
                }
            });
            client.setParser(parser);
            client.setSocket(socket);
            log.warn("[四信-GBT26875] 客户端已接入: {}", socket.remoteAddress());
        } catch (Exception e) {
            log.error("[四信-GBT26875] 连接初始化失败", e);
            client.shutdown();
        }
    }

    // ==================== 协议报文处理 ====================

    private void processPacket(FourfaithGbtDataPackage pkg, FourfaithGbtVertxTcpClient client, NetSocket socket) {
        String rawSourceAddr = pkg.getSourceAddrHex();
        String deviceAddr = FourfaithGbtDataPackage.reverseHexAddr(rawSourceAddr);
        log.info("[四信-GBT26875] 地址转换: 协议小端={}, 设备编号大端={}", rawSourceAddr, deviceAddr);

        // 心跳包特殊处理
        if (pkg.isHeartbeat()) {
            handleHeartbeat(deviceAddr, client, pkg);
            return;
        }

        byte command = pkg.getCommand();
        int cmdInt = command & 0xFF;
        log.info("[四信-GBT26875] 收到数据包: 源={}, 目的={}, 命令={}, 流水号={}, 时间={}",
                deviceAddr, pkg.getDestAddrHex(), cmdInt, pkg.getSerialNumber(), pkg.getTimestampStr());

        // 绑定客户端映射 + 更新活跃时间
        deviceManager.bindClient(deviceAddr, client);

        // 首次见到此设备 → 自动注册
        String pk = deviceManager.ensureDeviceRegistered(deviceAddr);
        if (pk != null) {
            cacheDeviceComponentInfo(pk, deviceAddr);
        }
        deviceManager.online(deviceAddr);

        switch (cmdInt) {
            case FourfaithGbtConstants.CMD_SEND_DATA:
                handleDataUpload(pkg, deviceAddr);
                commandSender.sendConfirm(deviceAddr, pkg);
                break;

            case FourfaithGbtConstants.CMD_CONFIRM:
                handleConfirm(pkg, deviceAddr);
                break;

            case FourfaithGbtConstants.CMD_RESPONSE:
                handleResponse(pkg, deviceAddr);
                break;

            case FourfaithGbtConstants.CMD_DENY:
                handleDeny(pkg, deviceAddr);
                break;

            case FourfaithGbtConstants.CMD_CONTROL:
                log.info("[四信-GBT26875] 收到控制命令: 设备={}", deviceAddr);
                handleDataUpload(pkg, deviceAddr);
                commandSender.sendConfirm(deviceAddr, pkg);
                break;

            // 国标框架心跳（CMD=128/129）
            case FourfaithGbtConstants.CMD_HEARTBEAT_ACK:
            case FourfaithGbtConstants.CMD_HEARTBEAT_NO_ACK:
                handleStandardHeartbeat(deviceAddr, pkg, cmdInt);
                break;

            default:
                log.warn("[四信-GBT26875] 未识别的命令字节: {}", cmdInt);
                break;
        }
    }

    // ==================== 心跳处理 ====================

    private void handleHeartbeat(String deviceAddr, FourfaithGbtVertxTcpClient client, FourfaithGbtDataPackage pkg) {
        log.warn("[四信-GBT26875] 收到旧版心跳包: 设备={}", deviceAddr);

        // 首次见到此设备 → 自动注册
        String pk = deviceManager.ensureDeviceRegistered(deviceAddr);
        if (pk != null) {
            cacheDeviceComponentInfo(pk, deviceAddr);
        }
        deviceManager.bindClient(deviceAddr, client);
        deviceManager.online(deviceAddr);

        // 旧版心跳应答：回显设备发来的通讯模块和系统类型
        // 从心跳包中提取通讯模块(byte 8)和系统类型(byte 9)
        byte commModule = 0;
        byte sysType = 0;
        if (pkg.getSourceAddr() != null && pkg.getSourceAddr().length >= 2) {
            // 心跳包格式: @@ + srcAddr(6) + commModule(1) + sysType(1) + ##
            // parseHeartbeat 已将 srcAddr 解析出来，但 commModule/sysType 未保存
            // 此处暂时用默认值0，实际应从原始buffer解析
        }
        commandSender.sendLegacyHeartbeatReply(deviceAddr, client, commModule, sysType);
    }

    /**
     * 处理国标框架心跳（CMD=128/129）
     */
    private void handleStandardHeartbeat(String deviceAddr, FourfaithGbtDataPackage pkg, int command) {
        log.info("[四信-GBT26875] 收到国标心跳: 设备={}, CMD={}", deviceAddr, command);

        deviceManager.updateActive(deviceAddr);

        // CMD=128 需要应答（CMD=130），CMD=129 不需要应答
        if (command == FourfaithGbtConstants.CMD_HEARTBEAT_ACK) {
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

    private void handleConfirm(FourfaithGbtDataPackage pkg, String deviceAddr) {
        int confirmSn = pkg.getSerialNumber();
        PendingRequest req = pendingRequestManager.get(confirmSn);
        if (req != null) {
            log.info("[四信-GBT26875] 收到确认: 设备={}, SN={}, 服务={}",
                deviceAddr, confirmSn, req.getServiceName());
        } else {
            log.info("[四信-GBT26875] 收到确认: 设备={}, SN={}", deviceAddr, confirmSn);
        }
    }

    private void handleDeny(FourfaithGbtDataPackage pkg, String deviceAddr) {
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
        log.warn("[四信-GBT26875] 收到否认: 设备={}, SN={}", deviceAddr, denySn);
    }

    private void handleResponse(FourfaithGbtDataPackage pkg, String deviceAddr) {
        byte[] appData = pkg.getApplicationData();
        if (appData == null || appData.length < 2) return;

        int typeFlag = FourfaithGbtDataParser.getTypeFlag(appData);
        log.info("[四信-GBT26875] 收到应答: 设备={}, typeFlag={}, 应用数据HEX={}",
                deviceAddr, typeFlag, FourfaithGbtDataPackage.bytesToHex(appData));

        if (typeFlag == FourfaithGbtConstants.TYPE_UP_REMOTE_RESPONSE) {
            handleRemoteControlResponse(pkg, deviceAddr);
        } else {
            handleQueryResponse(pkg, deviceAddr);
        }
    }

    private void handleRemoteControlResponse(FourfaithGbtDataPackage pkg, String deviceAddr) {
        int serialNumber = pkg.getSerialNumber();
        PendingRequest req = pendingRequestManager.remove(serialNumber);
        byte[] appData = pkg.getApplicationData();
        log.info("[四信-GBT26875] 远程控制应答: 设备={}, SN={}, req={}, 应用数据HEX={}",
                deviceAddr, serialNumber, req != null ? req.getServiceName() : "无",
                FourfaithGbtDataPackage.bytesToHex(appData));

        Map<String, Object> resp = FourfaithRemoteResponseParser.parse(appData);
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
            log.warn("[四信-GBT26875] 收到未知流水号的远程应答: SN={}", serialNumber);
        }
    }

    private void handleQueryResponse(FourfaithGbtDataPackage pkg, String deviceAddr) {
        byte[] appData = pkg.getApplicationData();
        if (appData == null || appData.length < 2) return;

        int typeFlag = FourfaithGbtDataParser.getTypeFlag(appData);
        List<Map<String, Object>> entries = FourfaithGbtDataParser.parse(appData);
        String pk = deviceManager.getProductKey(deviceAddr);
        if (pk == null) return;

        log.info("[四信-GBT26875] 查询应答: 设备={}, 类型={}, 记录数={}", deviceAddr, typeFlag, entries.size());
        for (Map<String, Object> entry : entries) {
            report(PropertyReport.builder()
                    .productKey(pk)
                    .deviceName(deviceAddr)
                    .params(entry)
                    .build());
        }
    }

    // ==================== 数据上传处理（委托 DataReporter） ====================

    private void handleDataUpload(FourfaithGbtDataPackage pkg, String deviceAddr) {
        byte[] appData = pkg.getApplicationData();
        if (appData == null || appData.length < 2) {
            log.warn("[四信-GBT26875] 数据上传无应用数据单元");
            return;
        }

        log.info("[四信-GBT26875] 收到数据上传: 设备={}, 应用数据HEX={}",
                deviceAddr, FourfaithGbtDataPackage.bytesToHex(appData));

        String pk = deviceManager.getProductKey(deviceAddr);
        dataReporter.handleDataUpload(pkg, deviceAddr, pk, null,
                (parentAddr, parentPk, hostNo, circuitNo, addressNo) ->
                        deviceManager.ensureComponentRegistered(parentAddr, parentPk, hostNo, circuitNo, addressNo));
    }

    // ==================== 平台下发动作 ====================

    @Override
    public boolean stateChange(boolean enable, String config) {
        fourfaithGbtTcpVerticle.setTcpComponent(this);

        if (!enable) {
            fourfaithGbtTcpVerticle.stopServer();
            return true;
        }

        if (config == null) {
            return false;
        }

        FourfaithGbtTcpConfig tcpConfig = JsonUtils.parseObject(config, FourfaithGbtTcpConfig.class);
        if (tcpConfig == null) {
            log.error("[四信-GBT26875] 解析配置失败");
            return false;
        }

        // 初始化监控中心地址
        if (tcpConfig.getCenterAddr() != null && !tcpConfig.getCenterAddr().isEmpty()) {
            this.centerAddr = FourfaithGbtDataPackage.hexToAddr(tcpConfig.getCenterAddr());
        }

        // 初始化委托组件
        initDelegates(tcpConfig);

        fourfaithGbtTcpVerticle.stopServer();
        fourfaithGbtTcpVerticle.startServer(tcpConfig);
        return true;
    }

    @Override
    protected void propertySet(PropertySet action) {
        log.info("[四信-GBT26875] 属性设置: {}", action);
        Map<String, ?> params = action.getParams();
        if (params == null) return;

        int gbtCommand = getIntParam(params, "gbtCommand", FourfaithGbtConstants.CMD_REQUEST);
        int gbtTypeFlag = getIntParam(params, "gbtTypeFlag", 0);
        byte[] body = null;
        Object bodyObj = params.get("gbtBody");
        if (bodyObj instanceof String && !((String) bodyObj).isEmpty()) {
            body = hexStringToBytes((String) bodyObj);
        }

        if (gbtCommand == FourfaithGbtConstants.CMD_CONTROL) {
            commandSender.sendControl(action.getDeviceName(), gbtTypeFlag, body);
        } else {
            commandSender.sendQuery(action.getDeviceName(), gbtTypeFlag, body);
        }
    }

    @Override
    protected void propertyGet(PropertyGet action) {
        log.info("[四信-GBT26875] 属性获取: {}", action);
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
        log.info("[四信-GBT26875] 服务调用: name={}", action.getName());
        String serviceName = action.getName();
        String dn = action.getDeviceName();
        String pk = action.getProductKey();

        switch (serviceName) {
            case "syncTime": {
                byte[] ts = FourfaithGbtDataEncoder.buildCurrentTimestamp();
                commandSender.sendControl(dn, FourfaithGbtConstants.TYPE_DOWN_SYNC_TIME, ts);
                break;
            }
            case "initDevice": {
                commandSender.sendControl(dn, FourfaithGbtConstants.TYPE_DOWN_INIT_DEVICE, null);
                break;
            }
            case "inspection_cmd": {
                commandSender.sendControl(dn, FourfaithGbtConstants.TYPE_DOWN_INSPECT, null);
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
            default:
                log.warn("[四信-GBT26875] 未知服务: {}", serviceName);
        }
    }

    private void handleRemoteControl(ServiceInvoke action, String dn, String pk) {
        Map<String, ?> params = action.getParams();
        String commandCode = getStringParam(params, "command_code", "");

        if (commandCode.isEmpty()) {
            log.error("[四信-GBT26875] 远程控制缺少指令码: device={}", dn);
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("success", 0);
            output.put("result_desc", "缺少指令码");
            report(ServiceReply.builder()
                .productKey(pk).deviceName(dn).replyId(action.getId())
                .code(1).params(output).build());
            return;
        }

        String deviceAddr = FourfaithGbtDataPackage.reverseHexAddr(dn);
        log.info("[四信-GBT26875] 远程控制地址转换: dn(大端)={}, 反转后(小端)={}, commandCode={}",
                dn, deviceAddr, commandCode);
        if (deviceAddr == null || deviceAddr.length() < 8) {
            log.error("[四信-GBT26875] 远程控制设备编码格式异常: deviceAddr={}", deviceAddr);
            return;
        }
        String partAddrHex = deviceAddr.substring(0, 8);

        int sn = commandSender.nextSerial();
        log.info("[四信-GBT26875] 远程控制: partAddrHex={}, SN={}, commandCode={}",
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
        byte[] appData = FourfaithGbtDataEncoder.buildType81AppData(commandBytes, partAddrHex);
        commandSender.sendControl(dn, FourfaithGbtConstants.TYPE_DOWN_REMOTE_CONTROL, appData);
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
                return FourfaithGbtConstants.TYPE_DOWN_QUERY_SYSTEM_STATUS;

            // 遥测属性 → TLD 3 模拟量查询
            case "telemetry_temperature":
            case "telemetry_pressure":
            case "telemetry_voltage":
            case "telemetry_current":
            case "telemetry_wind_speed":
            case "telemetry_residual_current":
            case "telemetry_water_level":
                return FourfaithGbtConstants.TYPE_DOWN_QUERY_ANALOG_VALUE;

            // 装置告警 → TLD 21 装置状态查询
            case "alarm_device_fire":
            case "alarm_device_fault":
            case "alarm_device_main_power":
            case "alarm_device_backup_power":
            case "alarm_device_comm":
            case "alarm_device_line":
                return FourfaithGbtConstants.TYPE_DOWN_QUERY_DEVICE_STATUS;

            // 设备信息 → TLD 25 装置版本查询
            case "info_firmware_ver_major":
            case "info_firmware_ver_minor":
                return FourfaithGbtConstants.TYPE_DOWN_QUERY_DEVICE_VERSION;

            // 设备时间 → TLD 28 装置时间查询
            case "info_system_time":
                return FourfaithGbtConstants.TYPE_DOWN_QUERY_DEVICE_TIME;

            default:
                log.warn("[四信-GBT26875] 未知的属性key: {}", key);
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
        return "GBT26875-Fourfaith-tcp";
    }

    @Override
    public String getName() {
        return "国标用户传输协议GBT26875.3-2011-tcp协议组件";
    }
}
