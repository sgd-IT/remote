
package com.enjoyiot.eiot.component.gbttcp.service;

import cn.hutool.core.util.IdUtil;
import com.enjoyiot.eiot.component.core.ComponentServices;
import com.enjoyiot.eiot.component.core.ThingComponent;
import com.enjoyiot.eiot.common.enums.DeviceState;
import com.enjoyiot.eiot.component.core.model.down.*;
import com.enjoyiot.eiot.component.core.model.up.*;
import com.enjoyiot.eiot.component.gbttcp.cilent.GbtVertxTcpClient;
import com.enjoyiot.eiot.component.gbttcp.model.GbtTcpConfig;
import com.enjoyiot.eiot.component.gbttcp.parser.GbtConstants;
import com.enjoyiot.eiot.component.gbttcp.parser.GbtDataDecoder;
import com.enjoyiot.eiot.component.gbttcp.parser.GbtDataEncoder;
import com.enjoyiot.eiot.component.gbttcp.parser.GbtDataPackage;
import com.enjoyiot.eiot.component.gbttcp.parser.GbtDataParser;
import com.enjoyiot.eiot.component.gbttcp.parser.GbtDataReader;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
// @Component  // 已停用：由 iot-component-tcp-remote-monitor 替代
public class GbtTcpComponent extends ThingComponent implements Handler<NetSocket> {

    /** 设备地址(hex) → TCP客户端 */
    private final Map<String, GbtVertxTcpClient> clientMap = new ConcurrentHashMap<>();

    /** 设备地址(hex) → productKey */
    private final Map<String, String> dnToPk = new ConcurrentHashMap<>();

    /** 部件子设备 deviceName → productKey */
    private final Map<String, String> compDnToPk = new ConcurrentHashMap<>();

    /** 设备地址(hex) → 最后活跃时间 */
    private final Map<String, Long> heartbeatDevice = new ConcurrentHashMap<>();

    private final GbtTcpVerticle gbtTcpVerticle;
    private final ProductApi productApi;
    private final DeviceApi deviceApi;

    private Vertx vertx;
    private NetServer server;

    /** 监控中心地址(6字节) */
    private byte[] centerAddr = new byte[GbtConstants.ADDRESS_LENGTH];

    /** 是否允许自动注册设备 */
    private boolean allowAutoRegister;

    /** 备用产品Key(仅当 allowAutoRegister=true 时生效) */
    private String fallbackProductKey;

    /** 出站业务流水号计数器 */
    private final AtomicInteger outboundSerial = new AtomicInteger(0);

    @Setter
    private long keepAliveTimeout = Duration.ofSeconds(60).toMillis();

    protected GbtTcpComponent(
            GbtTcpVerticle gbtTcpVerticle,
            ProductApi productApi,
            DeviceApi deviceApi,
            ComponentServices componentServices
    ) {
        super(componentServices);
        this.gbtTcpVerticle = gbtTcpVerticle;
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
        log.info("[GBT26875] 收到客户端连接: {}", socket.remoteAddress());
        String clientId = IdUtil.simpleUUID() + "_" + socket.remoteAddress();
        GbtVertxTcpClient client = new GbtVertxTcpClient(clientId);
        client.setKeepAliveTimeoutMs(keepAliveTimeout);

        try {
            socket.exceptionHandler(err -> log.error("[GBT26875] TCP异常: {}", err.getMessage()))
                    .closeHandler(nil -> {
                        log.warn("[GBT26875] 客户端断开: {}", socket.remoteAddress());
                        client.shutdown();
                        clientMap.entrySet().removeIf(e -> e.getValue() == client);
                    });

            // 先创建 parser，再 setSocket（setSocket 内会注册 handler 引用 parser）
            RecordParser parser = GbtDataReader.getParser(buffer -> {
                try {
                    GbtDataPackage pkg = GbtDataDecoder.decode(buffer);
                    if (pkg == null) {
                        log.warn("[GBT26875] 数据包解码失败，丢弃");
                        return;
                    }
                    processPacket(pkg, client, socket);
                } catch (Exception e) {
                    log.error("[GBT26875] 处理数据包异常", e);
                }
            });
            client.setParser(parser);
            client.setSocket(socket);
            log.warn("[GBT26875] 客户端已接入: {}", socket.remoteAddress());
        } catch (Exception e) {
            log.error("[GBT26875] 连接初始化失败", e);
            client.shutdown();
        }
    }

    // ==================== 协议报文处理 ====================

    /**
     * 处理收到的标准数据包
     */
    private void processPacket(GbtDataPackage pkg, GbtVertxTcpClient client, NetSocket socket) {
        String deviceAddr = pkg.getSourceAddrHex();
        byte command = pkg.getCommand();

        log.info("[GBT26875] 收到数据包: 源={}, 目的={}, 命令={}, 流水号={}, 时间={}",
                deviceAddr, pkg.getDestAddrHex(), command, pkg.getSerialNumber(), pkg.getTimestampStr());

        // 绑定客户端映射
        clientMap.put(deviceAddr, client);
        heartbeatDevice.put(deviceAddr, System.currentTimeMillis());

        // 首次见到此设备 → 自动注册
        ensureDeviceRegistered(deviceAddr);

        switch (command) {
            case GbtConstants.CMD_SEND_DATA:
                // 设备上传数据，解析并上报平台，回复确认
                handleDataUpload(pkg, deviceAddr);
                sendConfirm(deviceAddr, pkg);
                break;

            case GbtConstants.CMD_CONFIRM:
                // 设备对我方下发指令的确认
                log.info("[GBT26875] 收到确认: 设备={}, 流水号={}", deviceAddr, pkg.getSerialNumber());
                break;

            case GbtConstants.CMD_RESPONSE:
                // 设备对我方查询的应答
                handleQueryResponse(pkg, deviceAddr);
                break;

            case GbtConstants.CMD_DENY:
                // 设备否认
                log.warn("[GBT26875] 收到否认: 设备={}, 流水号={}", deviceAddr, pkg.getSerialNumber());
                break;

            case GbtConstants.CMD_CONTROL:
                // 设备发来的控制命令（一般不会出现，但兼容处理）
                log.info("[GBT26875] 收到控制命令: 设备={}", deviceAddr);
                handleDataUpload(pkg, deviceAddr);
                sendConfirm(deviceAddr, pkg);
                break;

            default:
                log.warn("[GBT26875] 未识别的命令字节: {}", command);
                break;
        }
    }

    /**
     * 设备数据上传处理: 解析应用数据并向平台上报属性/事件
     *
     * 上报策略:
     *   - 属性层（PropertyReport）: 所有类型都更新对应属性
     *     - Type 1~4: 聚合为 JSON 数组存入 text 属性 + 提取关键状态(bool)
     *     - Type 5~8: 聚合为 JSON 数组（平台可选存储）
     *     - Type 21/24/25/26/28: 扁平字段直接上报
     *   - 事件层（EventReport）: 仅在异常状态时触发
     *     - fire_alarm_event: 任何来源的火警
     *     - fault_event: 任何来源的故障
     *     - power_fault_event: 电源故障
     */
    private void handleDataUpload(GbtDataPackage pkg, String deviceAddr) {
        byte[] appData = pkg.getApplicationData();
        if (appData == null || appData.length < 2) {
            log.warn("[GBT26875] 数据上传无应用数据单元");
            return;
        }

        int typeFlag = GbtDataParser.getTypeFlag(appData);
        List<Map<String, Object>> entries = GbtDataParser.parse(appData);
        String pk = dnToPk.get(deviceAddr);
        if (pk == null) {
            log.warn("[GBT26875] 设备 {} 无productKey，跳过上报", deviceAddr);
            return;
        }

        log.info("[GBT26875] 数据上报: 设备={}, 类型={}, 记录数={}", deviceAddr, typeFlag, entries.size());

        // === 属性上报 ===
        Map<String, Object> propertyParams = new LinkedHashMap<>();
        // 连接层元数据
        propertyParams.put("seq_no", pkg.getSerialNumber());
        propertyParams.put("cmd_byte", (int) pkg.getCommand());
        propertyParams.put("packet_time", pkg.getTimestampStr());

        if (typeFlag == GbtConstants.TYPE_UP_COMPONENT_STATUS) {
            // Type 2: 部件状态 → 分流到部件子设备
            for (Map<String, Object> entry : entries) {
                int sysType = (Integer) entry.get("comp_systemType");
                int sysAddr = (Integer) entry.get("comp_systemAddr");
                int compType = (Integer) entry.get("comp_type");
                int compAddr = (Integer) entry.get("comp_addr");

                // 确保部件子设备已注册
                String compPk = ensureComponentDeviceRegistered(deviceAddr, pk,
                    sysType, sysAddr, compType, compAddr);
                if (compPk == null) continue;

                String compDn = generateComponentDeviceName(deviceAddr, sysType, sysAddr, compType, compAddr);

                // 提取部件级属性（与物模型字段一一对应）
                Map<String, Object> compProps = new LinkedHashMap<>();
                compProps.put("comp_system_type", sysType);
                compProps.put("comp_system_addr", sysAddr);
                compProps.put("comp_type", compType);
                compProps.put("comp_addr", compAddr);
                compProps.put("comp_status", entry.get("comp_status"));
                compProps.put("comp_desc", entry.get("comp_desc"));
                compProps.put("comp_normal_mode", entry.get("comp_normalMode"));
                compProps.put("comp_fire_alarm", entry.get("comp_fireAlarm"));
                compProps.put("comp_fault", entry.get("comp_fault"));
                compProps.put("comp_shielded", entry.get("comp_shielded"));
                compProps.put("comp_supervised", entry.get("comp_supervised"));
                compProps.put("comp_activated", entry.get("comp_activated"));
                compProps.put("comp_feedback", entry.get("comp_feedback"));
                compProps.put("comp_delay", entry.get("comp_delay"));
                compProps.put("comp_power_fault", entry.get("comp_powerFault"));
                compProps.put("comp_status_time", entry.get("comp_statusTime"));

                report(PropertyReport.builder()
                    .productKey(compPk)
                    .deviceName(compDn)
                    .params(compProps)
                    .build());
            }
        }

        if (isDeviceFlatType(typeFlag)) {
            // Type 21/25/26/28: 装置自身信息 → 扁平字段直接上报
            for (Map<String, Object> entry : entries) {
                entry.forEach((key, value) -> {
                    if (!"gbtType".equals(key)) {
                        propertyParams.put(key, value);
                    }
                });
            }
        } else {
            // Type 1/3/4/5/6/7/8: 建筑消防设施信息 → 不再上报 JSON 数组，只提取关键状态
            extractKeyStatus(typeFlag, entries, propertyParams);
        }

        report(PropertyReport.builder()
                .productKey(pk)
                .deviceName(deviceAddr)
                .params(propertyParams)
                .build());

        // === 异常事件上报（仅在异常状态时触发） ===
        triggerAbnormalEvents(pk, deviceAddr, typeFlag, entries);

        // 上线状态
        online(deviceAddr);
    }

    /**
     * 判断类型标志是否为装置扁平字段类型（Type 21/25/26/28）
     * 这些类型的字段直接作为独立属性上报物模型。
     * Type 24 除外，它走 dev_operation_json JSON数组。
     */
    private boolean isDeviceFlatType(int typeFlag) {
        return typeFlag == GbtConstants.TYPE_UP_DEVICE_STATUS
                || typeFlag == GbtConstants.TYPE_UP_DEVICE_VERSION
                || typeFlag == GbtConstants.TYPE_UP_DEVICE_CONFIG
                || typeFlag == GbtConstants.TYPE_UP_DEVICE_TIME;
    }

    /**
     * 获取建筑消防设施信息的 JSON 属性 key
     */
    private String getJsonPropertyKey(int typeFlag) {
        switch (typeFlag) {
            case GbtConstants.TYPE_UP_SYSTEM_STATUS: return "system_status_json";
            case GbtConstants.TYPE_UP_COMPONENT_STATUS: return "component_status_json";
            case GbtConstants.TYPE_UP_ANALOG_VALUE: return "analog_value_json";
            case GbtConstants.TYPE_UP_OPERATION_INFO: return "operation_log_json";
            case GbtConstants.TYPE_UP_SOFTWARE_VERSION: return "software_version_json";
            case GbtConstants.TYPE_UP_SYSTEM_CONFIG: return "system_config_json";
            case GbtConstants.TYPE_UP_COMPONENT_CONFIG: return "component_config_json";
            case GbtConstants.TYPE_UP_SYSTEM_TIME: return "system_time_json";
            default: return null;
        }
    }

    /**
     * 判断枚举状态值是否为"异常/真" (支持 String/int/bool 类型)
     */
    private boolean isTrue(Object v) {
        if (v == null) return false;
        if (v instanceof String) return "1".equals(v);  // 枚举值 "1"=异常
        if (v instanceof Integer) return (Integer) v == 1;
        return Boolean.TRUE.equals(v);
    }

    /**
     * 从解析条目中提取关键状态缓存（用于规则引擎直接订阅 bool 字段）
     */
    private void extractKeyStatus(int typeFlag, List<Map<String, Object>> entries, Map<String, Object> params) {
        switch (typeFlag) {
            case GbtConstants.TYPE_UP_SYSTEM_STATUS:
                boolean sysFireAlarm = entries.stream().anyMatch(e -> isTrue(e.get("sys_fireAlarm")));
                boolean sysFault = entries.stream().anyMatch(e -> isTrue(e.get("sys_fault")));
                params.put("last_fire_alarm", sysFireAlarm ? "1" : "0");  // enum: 0=正常, 1=火警
                params.put("last_fault", sysFault ? "1" : "0");          // enum: 0=正常, 1=故障
                break;
            case GbtConstants.TYPE_UP_COMPONENT_STATUS:
                long compFireCount = entries.stream().filter(e -> isTrue(e.get("comp_fireAlarm"))).count();
                params.put("comp_fire_count", (int) compFireCount);
                break;
            case GbtConstants.TYPE_UP_ANALOG_VALUE:
                extractLastAnalogValues(entries, params);
                break;
        }
    }

    /**
     * 从模拟量条目中提取最新温度和压力值
     */
    private void extractLastAnalogValues(List<Map<String, Object>> entries, Map<String, Object> params) {
        for (Map<String, Object> entry : entries) {
            Object analogTypeObj = entry.get("analog_type");
            if (analogTypeObj == null) continue;
            int analogType = (Integer) analogTypeObj;
            Object value = entry.get("analog_value");
            if (value == null) continue;

            if (analogType == GbtConstants.ANALOG_TEMPERATURE) {
                params.put("last_temperature", value);
            } else if (analogType == GbtConstants.ANALOG_PRESSURE_MPA || analogType == GbtConstants.ANALOG_PRESSURE_KPA) {
                params.put("last_pressure", value);
            }
        }
    }

    // ==================== 异常事件触发 ====================

    /**
     * 检查解析条目中的异常状态，触发对应事件上报
     * 不维护上次状态缓存，每次检测到异常均触发事件
     */
    private void triggerAbnormalEvents(String pk, String deviceAddr, int typeFlag, List<Map<String, Object>> entries) {
        for (Map<String, Object> entry : entries) {
            // 火警检测
            if (hasFireAlarm(entry)) {
                report(EventReport.builder()
                        .productKey(pk).deviceName(deviceAddr)
                        .name("fire_alarm_event")
                        .params(buildFireAlarmParams(typeFlag, entry))
                        .build());
            }

            // 故障检测
            if (hasFault(entry)) {
                report(EventReport.builder()
                        .productKey(pk).deviceName(deviceAddr)
                        .name("fault_event")
                        .params(buildFaultParams(typeFlag, entry))
                        .build());
            }

            // 电源故障检测
            if (hasPowerFault(entry)) {
                report(EventReport.builder()
                        .productKey(pk).deviceName(deviceAddr)
                        .name("power_fault_event")
                        .params(buildPowerFaultParams(typeFlag, entry))
                        .build());
            }
        }
    }

    private boolean hasFireAlarm(Map<String, Object> entry) {
        return isTrue(entry.get("sys_fireAlarm"))
                || isTrue(entry.get("comp_fireAlarm"))
                || isTrue(entry.get("dev_fire_alarm"));
    }

    private boolean hasFault(Map<String, Object> entry) {
        return isTrue(entry.get("sys_fault"))
                || isTrue(entry.get("comp_fault"))
                || isTrue(entry.get("dev_fault"));
    }

    private boolean hasPowerFault(Map<String, Object> entry) {
        return isTrue(entry.get("sys_mainPowerFault"))
                || isTrue(entry.get("sys_backupPowerFault"))
                || isTrue(entry.get("comp_powerFault"))
                || isTrue(entry.get("dev_main_power_fault"))
                || isTrue(entry.get("dev_backup_power_fault"));
    }

    /**
     * 构建火警事件参数
     */
    private Map<String, Object> buildFireAlarmParams(int typeFlag, Map<String, Object> entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        switch (typeFlag) {
            case GbtConstants.TYPE_UP_SYSTEM_STATUS:
                params.put("event_sys_type", entry.get("sys_systemType"));
                params.put("event_sys_addr", entry.get("sys_systemAddr"));
                params.put("event_time", entry.get("sys_statusTime"));
                params.put("event_level", 1); // 系统级
                params.put("event_desc", "系统火警");
                break;
            case GbtConstants.TYPE_UP_COMPONENT_STATUS:
                params.put("event_sys_type", entry.get("comp_systemType"));
                params.put("event_sys_addr", entry.get("comp_systemAddr"));
                params.put("event_comp_type", entry.get("comp_type"));
                params.put("event_comp_addr", String.valueOf(entry.get("comp_addr")));
                params.put("event_time", entry.get("comp_statusTime"));
                params.put("event_level", 2); // 部件级
                params.put("event_desc", entry.get("comp_desc"));
                break;
            case GbtConstants.TYPE_UP_DEVICE_STATUS:
                params.put("event_time", entry.get("dev_statusTime"));
                params.put("event_level", 3); // 装置级
                params.put("event_desc", "用户信息传输装置火警");
                break;
        }
        return params;
    }

    /**
     * 构建故障事件参数
     */
    private Map<String, Object> buildFaultParams(int typeFlag, Map<String, Object> entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        switch (typeFlag) {
            case GbtConstants.TYPE_UP_SYSTEM_STATUS:
                params.put("fault_type", detectSysFaultType(entry));
                params.put("fault_sys_type", entry.get("sys_systemType"));
                params.put("fault_sys_addr", entry.get("sys_systemAddr"));
                params.put("event_time", entry.get("sys_statusTime"));
                break;
            case GbtConstants.TYPE_UP_COMPONENT_STATUS:
                params.put("fault_type", 7); // 部件故障
                params.put("fault_sys_type", entry.get("comp_systemType"));
                params.put("fault_sys_addr", entry.get("comp_systemAddr"));
                params.put("event_time", entry.get("comp_statusTime"));
                break;
            case GbtConstants.TYPE_UP_DEVICE_STATUS:
                params.put("fault_type", 6); // 装置故障
                params.put("event_time", entry.get("dev_statusTime"));
                break;
        }
        return params;
    }

    /**
     * 检测系统故障子类型
     */
    private int detectSysFaultType(Map<String, Object> entry) {
        if (Boolean.TRUE.equals(entry.get("sys_mainPowerFault"))) return 1; // 主电故障
        if (Boolean.TRUE.equals(entry.get("sys_backupPowerFault"))) return 2; // 备电故障
        if (Boolean.TRUE.equals(entry.get("sys_busFault"))) return 5; // 总线故障
        return 6; // 系统故障
    }

    /**
     * 构建电源故障事件参数
     */
    private Map<String, Object> buildPowerFaultParams(int typeFlag, Map<String, Object> entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        // 判断电源故障类型
        boolean isMain = Boolean.TRUE.equals(entry.get("sys_mainPowerFault"))
                || Boolean.TRUE.equals(entry.get("dev_main_power_fault"));
        boolean isBackup = Boolean.TRUE.equals(entry.get("sys_backupPowerFault"))
                || Boolean.TRUE.equals(entry.get("dev_backup_power_fault"));
        params.put("power_type", isMain ? 1 : (isBackup ? 2 : 0));

        switch (typeFlag) {
            case GbtConstants.TYPE_UP_SYSTEM_STATUS:
                params.put("source_type", 1); // 系统
                params.put("event_time", entry.get("sys_statusTime"));
                break;
            case GbtConstants.TYPE_UP_COMPONENT_STATUS:
                params.put("source_type", 2); // 部件
                params.put("event_time", entry.get("comp_statusTime"));
                break;
            case GbtConstants.TYPE_UP_DEVICE_STATUS:
                params.put("source_type", 21); // 装置
                params.put("event_time", entry.get("dev_statusTime"));
                break;
        }
        return params;
    }

    /**
     * 处理设备对查询的应答（command=5）
     */
    private void handleQueryResponse(GbtDataPackage pkg, String deviceAddr) {
        byte[] appData = pkg.getApplicationData();
        if (appData == null || appData.length < 2) return;

        // 类型标志
        int typeFlag = GbtDataParser.getTypeFlag(appData);
        // 解析应用数据单元
        List<Map<String, Object>> entries = GbtDataParser.parse(appData);
        String pk = dnToPk.get(deviceAddr);
        if (pk == null) return;

        log.info("[GBT26875] 查询应答: 设备={}, 类型={}, 记录数={}", deviceAddr, typeFlag, entries.size());
        for (Map<String, Object> entry : entries) {
            report(PropertyReport.builder()
                    .productKey(pk)
                    .deviceName(deviceAddr)
                    .params(entry)
                    .build());
        }
    }

    // ==================== 下行指令 ====================

    /**
     * 向设备发送确认报文（command=3）
     * 确认方将收到包的业务流水号返回
     */
    private void sendConfirm(String deviceAddr, GbtDataPackage receivedPkg) {
        GbtDataPackage confirm = GbtDataPackage.builder()
                .serialNumber(receivedPkg.getSerialNumber())
                .sourceAddr(centerAddr)
                .destAddr(receivedPkg.getSourceAddr())
                .command(GbtConstants.CMD_CONFIRM)
                .applicationData(new byte[0])
                .build();
        sendGbtPacket(deviceAddr, confirm);
    }

    /**
     * 向设备发送否认报文（command=6）
     */
    private void sendDeny(String deviceAddr, GbtDataPackage receivedPkg) {
        GbtDataPackage deny = GbtDataPackage.builder()
                .serialNumber(receivedPkg.getSerialNumber())
                .sourceAddr(centerAddr)
                .destAddr(receivedPkg.getSourceAddr())
                .command(GbtConstants.CMD_DENY)
                .applicationData(new byte[0])
                .build();
        sendGbtPacket(deviceAddr, deny);
    }

    /**
     * 向设备发送查询请求（command=4）
     */
    private void sendQuery(String deviceAddr, int queryTypeFlag, byte[] queryBody) {
        byte[] destAddr = GbtDataPackage.hexToAddr(deviceAddr);
        byte[] appData;
        if (queryBody != null && queryBody.length > 0) {
            appData = new byte[1 + queryBody.length];
            appData[0] = (byte) queryTypeFlag;
            System.arraycopy(queryBody, 0, appData, 1, queryBody.length);
        } else {
            appData = new byte[]{(byte) queryTypeFlag};
        }

        GbtDataPackage query = GbtDataPackage.builder()
                .serialNumber(nextSerial())
                .sourceAddr(centerAddr)
                .destAddr(destAddr)
                .command(GbtConstants.CMD_REQUEST)
                .applicationData(appData)
                .build();
        sendGbtPacket(deviceAddr, query);
    }

    /**
     * 向设备发送控制命令（command=1），如时间同步
     */
    private void sendControl(String deviceAddr, int typeFlag, byte[] controlBody) {
        byte[] destAddr = GbtDataPackage.hexToAddr(deviceAddr);
        byte[] appData;
        if (controlBody != null && controlBody.length > 0) {
            appData = new byte[1 + controlBody.length];
            appData[0] = (byte) typeFlag;
            System.arraycopy(controlBody, 0, appData, 1, controlBody.length);
        } else {
            appData = new byte[]{(byte) typeFlag};
        }

        GbtDataPackage ctrl = GbtDataPackage.builder()
                .serialNumber(nextSerial())
                .sourceAddr(centerAddr)
                .destAddr(destAddr)
                .command(GbtConstants.CMD_CONTROL)
                .applicationData(appData)
                .build();
        sendGbtPacket(deviceAddr, ctrl);
    }

    /**
     * 编码并发送 GBT 数据包
     */
    private void sendGbtPacket(String deviceAddr, GbtDataPackage pkg) {
        Buffer encoded = GbtDataEncoder.encode(pkg);
        GbtVertxTcpClient client = clientMap.get(deviceAddr);
        if (client != null && client.socket != null) {
            client.sendMessage(encoded);
            log.info("[GBT26875] 已发送: 目的={}, 命令={}, 流水号={}",
                    deviceAddr, pkg.getCommand(), pkg.getSerialNumber());
        } else {
            log.warn("[GBT26875] 设备 {} 不在线，无法发送", deviceAddr);
        }
    }

    private int nextSerial() {
        return outboundSerial.incrementAndGet() & 0xFFFF;
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
            log.error("[GBT26875] 解析配置失败");
            return false;
        }

        // 初始化监控中心地址
        if (tcpConfig.getCenterAddr() != null && !tcpConfig.getCenterAddr().isEmpty()) {
            this.centerAddr = GbtDataPackage.hexToAddr(tcpConfig.getCenterAddr());
        }

        // 初始化设备注册策略
        this.allowAutoRegister = Boolean.TRUE.equals(tcpConfig.getAllowAutoRegister());
        this.fallbackProductKey = tcpConfig.getFallbackProductKey();

        gbtTcpVerticle.stopServer();
        gbtTcpVerticle.startServer(tcpConfig);
        return true;
    }

    /**
     * 属性设置 → 编码为 GBT 控制命令下发
     *
     * params 约定:
     *   gbtCommand: 命令类型 (1=控制, 4=查询)，默认 4(查询)
     *   gbtTypeFlag: 类型标志，如 61/62/90 等
     *   gbtBody: 可选，附加数据的十六进制字符串
     */
    @Override
    protected void propertySet(PropertySet action) {
        log.info("[GBT26875] 属性设置: {}", action);
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
            sendControl(action.getDeviceName(), gbtTypeFlag, body);
        } else {
            sendQuery(action.getDeviceName(), gbtTypeFlag, body);
        }
    }

    /**
     * 属性获取 → 向设备发送查询请求
     */
    @Override
    protected void propertyGet(PropertyGet action) {
        log.info("[GBT26875] 属性获取: {}", action);
        List<String> keys = action.getKeys();
        if (keys == null || keys.isEmpty()) return;

        // 根据 key 映射查询类型
        for (String key : keys) {
            int queryType = mapPropertyKeyToQueryType(key);
            if (queryType > 0) {
                sendQuery(action.getDeviceName(), queryType, null);
            }
        }
    }

    /**
     * 服务调用
     *
     * 支持的服务名:
     *   syncTime   - 同步用户信息传输装置时间 (type=90)
     *   initDevice - 初始化用户信息传输装置 (type=89)
     *   inspect    - 检查命令 (type=91)
     *   query      - 通用查询，params.typeFlag 指定查询类型
     */
    @Override
    protected void serviceInvoke(ServiceInvoke action) {
        log.info("[GBT26875] 服务调用: name={}", action.getName());
        String serviceName = action.getName();
        String dn = action.getDeviceName();

        switch (serviceName) {
            case "syncTime": {
                // 同步时间: command=1, typeFlag=90, body=当前时间戳(6字节)
                byte[] ts = GbtDataEncoder.buildCurrentTimestamp();
                sendControl(dn, GbtConstants.TYPE_DOWN_SYNC_TIME, ts);
                break;
            }
            case "initDevice": {
                // 初始化设备: command=1, typeFlag=89
                sendControl(dn, GbtConstants.TYPE_DOWN_INIT_DEVICE, null);
                break;
            }
            case "inspection_cmd": {
                // 检查命令: command=1, typeFlag=91
                sendControl(dn, GbtConstants.TYPE_DOWN_INSPECT, null);
                break;
            }
            case "query": {
                // 通用查询: params 中指定 typeFlag
                Map<String, ?> params = action.getParams();
                int typeFlag = getIntParam(params, "typeFlag", 0);
                if (typeFlag > 0) {
                    sendQuery(dn, typeFlag, null);
                }
                break;
            }
            default:
                log.warn("[GBT26875] 未知服务: {}", serviceName);
        }
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

    // ==================== 设备管理 ====================

    /**
     * 生成部件子设备 deviceName
     * 规则: {父设备地址}_{系统类型}_{系统地址}_{部件类型}_{部件地址}
     */
    static String generateComponentDeviceName(String parentAddr, int sysType, int sysAddr,
                                               int compType, int compAddr) {
        return String.format("%s_%d_%d_%d_%d", parentAddr, sysType, sysAddr, compType, compAddr);
    }

    /**
     * 确保部件子设备已注册
     *
     * @param parentAddr 父设备地址(hex)
     * @param parentPk   父设备 productKey
     * @param sysType    系统类型
     * @param sysAddr    系统地址
     * @param compType   部件类型
     * @param compAddr   部件地址
     * @return 部件子设备 productKey（= parentPk + "sub"）
     */
    String ensureComponentDeviceRegistered(String parentAddr, String parentPk,
                                           int sysType, int sysAddr,
                                           int compType, int compAddr) {
        String compDn = generateComponentDeviceName(parentAddr, sysType, sysAddr, compType, compAddr);

        // 已缓存
        String cachedPk = compDnToPk.get(compDn);
        if (cachedPk != null) {
            return cachedPk;
        }

        // 查询是否已注册
        DeviceInfo existing = deviceApi.getDeviceByDn(compDn);
        if (existing != null) {
            String pk = existing.getProductKey();
            compDnToPk.put(compDn, pk);
            return pk;
        }

        // 自动注册
        String compPk = parentPk + "sub";
        DeviceInfo parentDevice = deviceApi.getDeviceByDn(parentAddr);
        Long parentId = parentDevice != null ? parentDevice.getId() : null;

        RegisterDevice req = RegisterDevice.builder()
            .productKey(compPk)
            .deviceName(compDn)
            .parentId(parentId)
            .build();
        DeviceInfo registered = deviceApi.registerDevice(req);
        if (registered != null) {
            compDnToPk.put(compDn, compPk);
            log.info("[GBT26875] 部件子设备注册成功: dn={}, pk={}, parent={}", compDn, compPk, parentAddr);
            return compPk;
        } else {
            log.error("[GBT26875] 部件子设备注册失败: dn={}", compDn);
            return null;
        }
    }

    /**
     * 确保设备已注册(首次通信时检查)
     *
     * 策略:
     * 1. 优先通过 deviceName(dn) 查询设备是否已在平台预注册(多租户场景推荐)
     * 2. 如果 allowAutoRegister=true,则使用 fallbackProductKey 自动注册
     * 3. 如果 allowAutoRegister=false 且设备未预注册,则拒绝接入
     */
    private void ensureDeviceRegistered(String deviceAddr) {
        // 已缓存,跳过
        if (dnToPk.containsKey(deviceAddr)) {
            return;
        }

        // 策略1: 通过 deviceName(dn) 查询设备是否已预注册(跨租户查询)
        DeviceInfo existingDevice = deviceApi.getDeviceByDn(deviceAddr);
        if (existingDevice != null) {
            // 设备已预注册,使用其 productKey
            String pk = existingDevice.getProductKey();
            dnToPk.put(deviceAddr, pk);
            cacheDeviceComponentInfo(pk, deviceAddr);
            log.info("[GBT26875] 设备已预注册: pk={}, dn={}, tenantId={}", 
                    pk, deviceAddr, existingDevice.getTenantId());
            return;
        }

        // 设备未预注册,检查是否允许自动注册
        if (!allowAutoRegister) {
            log.warn("[GBT26875] 设备 {} 未预注册且不允许自动注册,拒绝接入。请先在平台创建设备", deviceAddr);
            return;
        }

        // 策略2: 允许自动注册,使用 fallbackProductKey
        String pk = fallbackProductKey;
        if (pk == null || pk.isEmpty()) {
            log.warn("[GBT26875] 未配置 fallbackProductKey,无法自动注册设备 {}", deviceAddr);
            return;
        }

        dnToPk.put(deviceAddr, pk);
        RegisterDevice build = RegisterDevice.builder()
                .productKey(pk)
                .deviceName(deviceAddr)
                .build();
        DeviceInfo parentDevice = deviceApi.registerDevice(build);
        if (parentDevice != null) {
            cacheDeviceComponentInfo(pk, deviceAddr);
            log.info("[GBT26875] 设备自动注册成功: pk={}, dn={}", pk, deviceAddr);
        } else {
            log.error("[GBT26875] 设备自动注册失败: dn={}", deviceAddr);
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

    /**
     * 定时离线检测
     * 检查心跳超时和客户端连接状态
     */
    @Scheduled(fixedRate = 40, timeUnit = TimeUnit.SECONDS)
    private void offlineCheckTask() {
        log.warn("[GBT26875] 执行离线检测");

        // 检查 TCP 客户端是否在线
        Set<String> clients = new HashSet<>(clientMap.keySet());
        for (String key : clients) {
            GbtVertxTcpClient client = clientMap.get(key);
            if (client != null && !client.isOnline()) {
                client.shutdown();
                clientMap.remove(key);
            }
        }

        // 检查心跳超时
        Set<String> heartbeatKeys = new HashSet<>(heartbeatDevice.keySet());
        heartbeatKeys.forEach(addr -> {
            Long time = heartbeatDevice.get(addr);
            if (time != null && System.currentTimeMillis() - time > keepAliveTimeout * 2) {
                heartbeatDevice.remove(addr);
                String pk = dnToPk.get(addr);
                if (pk != null) {
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

    private int getIntParam(Map<String, ?> params, String key, int defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * 将属性 key 映射为 GBT 查询类型标志
     */
    private int mapPropertyKeyToQueryType(String key) {
        switch (key) {
            // 建筑消防设施信息（JSON属性 或 缓存属性）
            case "system_status_json":
            case "last_fire_alarm":
            case "last_fault":
                return GbtConstants.TYPE_DOWN_QUERY_SYSTEM_STATUS;
            case "component_status_json":
            case "comp_fire_count":
                return GbtConstants.TYPE_DOWN_QUERY_COMPONENT_STATUS;
            case "analog_value_json":
            case "last_temperature":
            case "last_pressure":
                return GbtConstants.TYPE_DOWN_QUERY_ANALOG_VALUE;
            case "operation_log_json":
                return GbtConstants.TYPE_DOWN_QUERY_OPERATION_INFO;
            // 装置自身信息（扁平属性）
            case "dev_fire_alarm":
            case "dev_fault":
            case "dev_normal_monitor":
            case "dev_status_raw":
            case "dev_main_power_fault":
            case "dev_backup_power_fault":
            case "dev_comm_fault":
            case "dev_line_fault":
                return GbtConstants.TYPE_DOWN_QUERY_DEVICE_STATUS;
            case "dev_main_ver":
            case "dev_sub_ver":
                return GbtConstants.TYPE_DOWN_QUERY_DEVICE_VERSION;
            case "device_time":
                return GbtConstants.TYPE_DOWN_QUERY_DEVICE_TIME;
            default:
                log.warn("[GBT26875] 未知的属性key: {}", key);
                return -1;
        }
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
