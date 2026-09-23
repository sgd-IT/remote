package com.enjoyiot.eiot.component.udp.service;

import com.alibaba.fastjson.JSON;
import com.enjoyiot.eiot.common.enums.DeviceState;
import com.enjoyiot.eiot.component.core.ComponentServices;
import com.enjoyiot.eiot.component.core.ThingComponent;
import com.enjoyiot.eiot.component.core.model.down.*;
import com.enjoyiot.eiot.component.core.model.up.DeviceStateChange;
import com.enjoyiot.eiot.component.core.model.up.EventReport;
import com.enjoyiot.eiot.component.core.model.up.PropertyReport;
import com.enjoyiot.eiot.component.dahua.common.constants.DahuaProtocolConstants;
import com.enjoyiot.eiot.component.dahua.common.parser.DahuaAduParser;
import com.enjoyiot.eiot.component.dahua.common.parser.GbtFrameBuilder;
import com.enjoyiot.eiot.component.udp.DTO.UWBDataBaseDTO;
import com.enjoyiot.eiot.component.udp.DTO.UWBForwardedDataDisplayDTO;
import com.enjoyiot.eiot.component.udp.DTO.UWBForwardedDataStatusOneDTO;
import com.enjoyiot.eiot.component.udp.enums.UDPReportTypeEnum;
import com.enjoyiot.eiot.component.udp.model.DeviceData;
import com.enjoyiot.eiot.component.udp.model.UdpConfig;
import com.enjoyiot.eiot.component.udp.parser.uwb.UWBMessageParserFactory;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.RegisterDevice;
import com.enjoyiot.module.eiot.api.product.ProductApi;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UdpComponent
 */
@Component
public class UdpComponent extends ThingComponent {
    private static final Logger log = LoggerFactory.getLogger(UdpComponent.class);

    private final UdpVerticle udpVerticle;
    private final ProductApi productApi;
    private final DeviceApi deviceApi;
    private UdpConfig udpConfigKey;
    /** 平台中心地址（6字节），用于大华协议下行帧的源地址 */
    private byte[] centerAddr = new byte[6];

    private final Map<String, String> lastDataHash = new ConcurrentHashMap<>();

    /** 大华协议 ADU 解析器（单例，内置事件/模拟量解析器） */
    private static final DahuaAduParser DAHUA_ADU_PARSER = new DahuaAduParser("UDP");

    private final MessageDigest md5;

    @Autowired
    public UdpComponent(ComponentServices services,
                        UdpVerticle verticle,
                        ProductApi productApi,
                        DeviceApi deviceApi) {
        super(services);
        this.udpVerticle = verticle;
        this.productApi  = productApi;
        this.deviceApi   = deviceApi;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 init failed", e);
        }
    }

    @Override
    public String getType() {
        return "udp";
    }

    @Override
    public String getName() {
        return "官方 UDP 组件 (Java8)";
    }

    @Override
    public boolean stateChange(boolean enable, String cfg) {
        udpVerticle.setUdpComponent(this);
        if (!enable) {
            udpVerticle.stopServer();
            return true;
        }
        if (cfg == null) {
            return false;
        }

        udpConfigKey = JsonUtils.parseObject(cfg, UdpConfig.class);
        if (udpConfigKey == null) {
            log.error("解析 UDP 配置失败: {}", cfg);
            return false;
        }

        // 初始化平台中心地址
        centerAddr = GbtFrameBuilder.hexToAddr(udpConfigKey.getCenterAddr());

        udpVerticle.stopServer();
        udpVerticle.startServer(udpConfigKey);
        return true;
    }

    @Override
    protected void deviceOta(DeviceOta action) {

    }

    /**
     * 提供外部访问
     */
    public UdpConfig getConfig() {
        return udpConfigKey;
    }

    /**
     * 接收并处理 UDP 字符串消息
     */
    public void handleUwb(String raw) {
        DeviceData entity = UWBMessageParserFactory.parse(raw);
        processData(entity,createSignatureStr((UWBDataBaseDTO) entity), udpConfigKey.getDefaultProductKey(), "udp_uwb");
    }

    public void handleJson(String jsonStr) {
        // todo 自己写json格式的解析 调用设备上报信息处理方法
        DeviceData entity = JSON.parseObject(jsonStr, DeviceData.class);
        // 加密明文 自己组装
        String signatureStr = entity.getDeviceId();
        processData(entity,signatureStr, udpConfigKey.getDefaultProductKey(), "udp_json");
    }
    public void handleHex(String hexStr) {
        // todo 自己写json格式的解析 调用设备上报信息处理方法
        try {
            //  解码
            byte[] raw = Hex.decodeHex(hexStr.trim().toCharArray());

            // 封装json
            Map<String,Object> m = new HashMap<>();
            m.put("deviceId", extractDeviceIdFromHex(raw));
            m.put("dataBin", Base64.getEncoder().encodeToString(raw));
            String json = JsonUtils.toJsonString(m);

            handleJson(json);

        } catch (DecoderException e) {
            log.error("HEX 解码失败: {}", hexStr, e);
        }
    }

    public void handleAscii(String txt) {
        // todo 自己写json格式的解析 调用设备上报信息处理方法
        Map<String,Object> m = new HashMap<>();
        m.put("deviceId", extractDeviceIdFromAscii(txt));
        m.put("dataTxt", txt);
        String json = JsonUtils.toJsonString(m);
        handleJson(json);
    }

    public void handleBinary(byte[] raw) {
        String protocolMode = udpConfigKey != null ? udpConfigKey.getProtocolMode() : "uwb";
        // 大华协议模式下，二进制数据先尝试大华帧解析（DahuaHandler 已匹配，这里兜底）
        if ("dahua".equalsIgnoreCase(protocolMode)) {
            log.info("[UDP] 大华协议模式，二进制兜底跳过 (应由 DahuaHandler 处理)");
            return;
        }
        // 原有逻辑
        Map<String,Object> m = new HashMap<>();
        m.put("deviceId", extractDeviceIdFromBinary(raw));
        m.put("dataBin", Base64.getEncoder().encodeToString(raw));
        String json = JsonUtils.toJsonString(m);
        handleJson(json);
    }
    /**
     * 解析出对应的参数 封装json字符串
     */
    private String extractDeviceIdFromHex(byte[] raw) {
        // TODO: 根据你的协议，解析出设备 ID，比如前 4 字节、或者某个字段
        // 这里只是个示例
        return String.format("%02X%02X%02X%02X", raw[0], raw[1], raw[2], raw[3]);
    }

    /**
     * 解析出对应的参数 封装json字符串
     */
    private String extractDeviceIdFromAscii(String txt) {
        // TODO: 根据你的 ASCII 协议解析 deviceId
        // 例如：txt 里有 “id=DEV123;...” 这样的字段
        int idx = txt.indexOf("id=");
        if (idx >= 0) {
            int end = txt.indexOf(';', idx);
            return txt.substring(idx + 3, end > 0 ? end : txt.length());
        }
        return "unknown";
    }

    /**
     * 解析出对应的参数 封装json字符串
     */
    private String extractDeviceIdFromBinary(byte[] raw) {
        // TODO: 二进制协议里，哪几个字节是 ID，就解析那几位
        return Base64.getEncoder().encodeToString(Arrays.copyOf(raw, Math.min(4, raw.length)));
    }

    /**
     * 设备上报信息处理
     *
     * <p>设备名取报文里的部件类型中文名（与 data_regSn/data_regModel/data_regSoftVer/data_regImei 同在一个 map，
     * 均来自 0x87 帧 dataType=0 的注册对象），这几个字段尚未入库，见 DeviceApi.registerDevice 的 TODO(eiot-udp-device-info)。
     */
    public void processData(DeviceData deviceData,String signatureStr,String productKey,String eventName) {
        if (deviceData == null || deviceData.getDeviceId() == null) {
            log.warn("[UDP协议] 无效或未识别的消息: {}", deviceData);
            return;
        }
        String devDevId = deviceData.getDeviceId();
        // 部件类型 0 = "通用"（类型未知时的默认值），不能当设备名；取在去重 return 之前
        Object componentName = deviceData.toMap().get("componentTypeName");
        String deviceName = (componentName instanceof String && !"通用".equals(componentName))
                ? (String) componentName : null;
        // 签名  防止重复上报
        String sig = computeSignatureStr(signatureStr);
        String prev = lastDataHash.put(devDevId, sig);
        if (sig.equals(prev)) {
            log.info("[UDP协议] 数据未变化，跳过上报: {}", devDevId);
            return;
        }
        DeviceInfo deviceInfo = getDeviceInfoByDnAndPk(devDevId, productKey, deviceName);
        if (deviceInfo == null) {
            log.warn("[UDP协议] 设备未注册，跳过: {}", devDevId);
            return;
        }
        // 上线上报
        handleOnline(deviceInfo);
        // 属性或事件上报
        UDPReportTypeEnum udpReportTypeEnum = deviceData.reportType();
        switch (udpReportTypeEnum){
            case PROPERTY_REPORT:
                propertyReport(deviceInfo, deviceData.toMap());
                break;
            case EVENT_REPORT:
                if (!StringUtils.hasText(eventName)){
                    eventName = "udp_other";
                }
                eventReport(deviceInfo, deviceData.toMap(),eventName);
                break;
            default:
                break;
        }
    }
    /**
     * 基于 DTO 生成签名，不包含时间戳
     */
    private String createSignatureStr(UWBDataBaseDTO e) {
        if (e == null) {
            return "";
        }
        // 拼接业务字段
        StringBuilder sb = new StringBuilder(e.getDevId());
        if (e instanceof UWBForwardedDataDisplayDTO) {
            UWBForwardedDataDisplayDTO d = (UWBForwardedDataDisplayDTO) e;
            sb.append('|').append(d.getPos());
        } else if (e instanceof UWBForwardedDataStatusOneDTO) {
            UWBForwardedDataStatusOneDTO s = (UWBForwardedDataStatusOneDTO) e;
            sb.append('|').append(s.getSubType())
                    .append('|').append(s.getVbat())
                    .append('|').append(s.getRgnid())
                    .append('|').append(s.getVelo());
        }
        return sb.toString();
    }
    private String computeSignature(UWBDataBaseDTO e) {
        // 拼接业务字段
        String signatureStr = createSignatureStr(e);
        return computeSignatureStr(signatureStr);
    }
    private String computeSignatureStr(String signatureStr) {
        byte[] data = signatureStr.getBytes(StandardCharsets.UTF_8);
        md5.reset();
        byte[] digest = md5.digest(data);
        return Hex.encodeHexString(digest);
    }

    /**
     * 获取设备信息（需要时自动注册，设备名用报文解析出的部件类型）
     */
    public DeviceInfo getDeviceInfoByDnAndPk(String devDn, String productKey, String deviceName) {
        // 注册或获取设备
        DeviceInfo deviceInfo;
        if (udpConfigKey.isEnableAutoRegister()) {
            deviceInfo = deviceApi.registerDevice(
                    RegisterDevice.builder()
                            .productKey(productKey)
                            .deviceName(devDn)
                            .name(deviceName)
                            .build()
            );
        } else {
            deviceInfo = deviceApi.getDeviceByPkDnByCache(udpConfigKey.getDefaultProductKey(), devDn);
        }
        return deviceInfo;
    }
    public void propertyReport(DeviceInfo deviceInfo,  Map<String, Object> params) {
        // 属性上报
        report(PropertyReport.builder()
                .productKey(deviceInfo.getProductKey())
                .deviceName(deviceInfo.getDn())
                .params(params)
                .time(System.currentTimeMillis())
                .build()
        );
    }
    public void eventReport(DeviceInfo deviceInfo,  Map<String, Object> params,String eventName) {
        // 事件上报
        report(EventReport.builder()
                .productKey(deviceInfo.getProductKey())
                .deviceName(deviceInfo.getDn())
                .name(eventName)
                .params(params)
                .time(System.currentTimeMillis())
                .build()
        );
    }
    // 离线判定不在此处：大华终端 1 小时一报，任何秒级/分钟级超时都会误判。
    // 统一由 DeviceStateCheckTask 按产品 keep_alive_time（当前 7200s = 2 轮上报）判定。
    public void handleOnline(DeviceInfo deviceInfo) {
        log.info("[UDP协议] 设备上线: {}", deviceInfo.getDn());
        // 上线上报
        report(DeviceStateChange.builder()
                .productKey(deviceInfo.getProductKey())
                .deviceName(deviceInfo.getDn())
                .state(DeviceState.ONLINE)
                .time(System.currentTimeMillis())
                .build()
        );
    }
    // 以下不支持的操作保持不变
    @Override protected void serviceInvoke(ServiceInvoke action)   { throw new UnsupportedOperationException("UDP 不支持服务下发"); }
    @Override protected void propertyGet(PropertyGet action)       { throw new UnsupportedOperationException("UDP 不支持属性获取"); }
    @Override protected void propertySet(PropertySet action)       { throw new UnsupportedOperationException("UDP 不支持属性设置"); }
    @Override protected void deviceConfig(DeviceConfig action)     { throw new UnsupportedOperationException("UDP 不支持配置下发"); }

    // ==================== 大华消防协议处理 ====================

    /**
     * 大华协议帧级路由处理（由 DahuaHandler 调用）
     * @param srcAddr      源地址（6字节HEX字符串，作为设备唯一标识）
     * @param cmd          命令字
     * @param typeFlag     类型标志（ADU首字节）
     * @param rawAdu       原始 ADU 字节（已通过校验和验证）
     * @param serialNum    原帧业务流水号
     * @param version      原帧协议版本号（LE读取值，如0x0302对应线上字节02 03）
     * @param dstAddrBytes 原帧目的地址（6字节，平台地址）
     * @param senderAddr   发送者网络地址（"host:port"）
     */
    public void handleDahua(String srcAddr, int cmd, int typeFlag, byte[] rawAdu,
                            int serialNum, int version, byte[] dstAddrBytes, String senderAddr) {
        if (udpConfigKey == null || !"dahua".equalsIgnoreCase(udpConfigKey.getProtocolMode())) {
            return;
        }

        log.info("[UDP] ← 设备={} CMD=0x{} type=0x{} len={}",
                srcAddr, String.format("%02X", cmd), String.format("%02X", typeFlag),
                rawAdu != null ? rawAdu.length : 0);

        switch (cmd) {
            case DahuaProtocolConstants.CMD_REGISTER:
                switch (typeFlag) {
                    case DahuaProtocolConstants.TYPE_REGISTER:
                    case DahuaProtocolConstants.TYPE_REGISTER_EXT:
                        handleRegister(srcAddr, rawAdu, serialNum, version, senderAddr);
                        break;
                    default:
                        log.warn("[UDP] 注册未知: src={}, type=0x{}", srcAddr, String.format("%02X", typeFlag));
                        break;
                }
                break;

            case DahuaProtocolConstants.CMD_CLOCK_SYNC:
                log.warn("[UDP] 时钟同步(下行误收): src={}", srcAddr);
                break;

            case DahuaProtocolConstants.CMD_DEVICE_REPORT:
                handleDeviceReport(srcAddr, typeFlag, rawAdu, serialNum, version, senderAddr);
                break;

            case DahuaProtocolConstants.CMD_CONFIRM:
                log.info("[UDP] 确认: src={}", srcAddr);
                break;

            case DahuaProtocolConstants.CMD_PLATFORM_COMMAND:
                log.info("[UDP] 下行(误收): src={}, type=0x{}", srcAddr, String.format("%02X", typeFlag));
                break;

            case DahuaProtocolConstants.CMD_DEVICE_RESPONSE:
                switch (typeFlag) {
                    case DahuaProtocolConstants.TYPE_PARAM_RESPONSE_FIXED:
                    case DahuaProtocolConstants.TYPE_PARAM_RESPONSE_VARIABLE:
                    case DahuaProtocolConstants.TYPE_QUERY_UNIVERSAL_RESPONSE:
                    case DahuaProtocolConstants.TYPE_QUERY_CAPABILITY_RESPONSE:
                        log.info("[UDP] 应答: src={}, type=0x{}", srcAddr, String.format("%02X", typeFlag));
                        reportRawData(srcAddr, "dahua_response_" + Integer.toHexString(typeFlag), rawAdu);
                        break;
                    default:
                        log.warn("[UDP] 应答未知: src={}, type=0x{}", srcAddr, String.format("%02X", typeFlag));
                        break;
                }
                break;

            case DahuaProtocolConstants.CMD_DENY:
                log.warn("[UDP] 否认: src={}", srcAddr);
                break;

            case DahuaProtocolConstants.CMD_KEEPALIVE:
                log.info("[UDP] 保活: src={}", srcAddr);
                // 协议 2.2.3.1：平台收到保活帧且校验正确必须回确认帧，否则设备启动重发
                sendConfirmFrame(srcAddr, serialNum, version, senderAddr);
                break;

            case DahuaProtocolConstants.CMD_FUNCTION_CONFIG:
                log.info("[UDP] 功能配置(下行误收): src={}", srcAddr);
                break;

            case DahuaProtocolConstants.CMD_PARAM_RESPONSE:
                if (typeFlag == DahuaProtocolConstants.TYPE_PARAM_RESPONSE) {
                    log.info("[UDP] 带参应答: src={}", srcAddr);
                    reportRawData(srcAddr, "dahua_param_response", rawAdu);
                } else {
                    log.warn("[UDP] 带参应答未知: src={}, type=0x{}", srcAddr, String.format("%02X", typeFlag));
                }
                break;

            default:
                log.warn("[UDP] 未知CMD: cmd=0x{}, src={}", String.format("%02X", cmd), srcAddr);
                break;
        }
    }

    /**
     * 处理设备上报帧 (CMD=0x02)
     * ponytail: 10 个非通用 typeFlag 行为相同，合并为 default
     */
    private void handleDeviceReport(String srcAddr, int typeFlag, byte[] rawAdu,
                                    int serialNum, int version, String senderAddr) {
        log.info("[UDP] 上报: src={}, type=0x{}, aduLen={}", srcAddr, String.format("%02X", typeFlag),
                rawAdu != null ? rawAdu.length : 0);
        try {
            if (typeFlag == DahuaProtocolConstants.TYPE_UNIVERSAL_DATA) {
                parseUniversalDataFrame(srcAddr, rawAdu);
            } else {
                parseSimpleReportFrame(srcAddr, typeFlag, rawAdu);
            }
        } finally {
            // 协议 5.1：信息上传=发送/确认，解析异常也必须回确认，否则设备按 5.2 重发
            sendConfirmFrame(srcAddr, serialNum, version, senderAddr);
        }
    }

    /**
     * 解析 0x87 通用数据帧并上报
     */
    private void parseUniversalDataFrame(String srcAddr, byte[] rawAdu) {
        if (rawAdu.length < 2) {
            log.warn("[UDP] ADU长度不足: {}", rawAdu.length);
            return;
        }

        int infoCount = rawAdu[1] & 0xFF;
        if (rawAdu.length < 2 + infoCount * 14) {
            log.warn("[UDP] ADU长度不匹配: aduLen={}, count={}", rawAdu.length, infoCount);
            return;
        }

        ByteBuffer buf = ByteBuffer.wrap(rawAdu, 2, rawAdu.length - 2)
                .order(ByteOrder.LITTLE_ENDIAN);

        List<Map<String, Object>> infoObjects = DAHUA_ADU_PARSER.parse(buf, infoCount);

        if (infoObjects.isEmpty()) {
            log.warn("[UDP] 解析为空: src={}", srcAddr);
            return;
        }

        for (int i = 0; i < infoObjects.size(); i++) {
            Map<String, Object> data = infoObjects.get(i);

            String eventName;
            int dataType = (Integer) data.getOrDefault("dataType", -1);
            if (dataType == DahuaProtocolConstants.DATA_TYPE_EVENT) {
                eventName = "dahua_event_report";
            } else if (dataType == DahuaProtocolConstants.DATA_TYPE_ANALOG) {
                eventName = "dahua_analog_report";
            } else {
                eventName = "dahua_data_report";
            }

            DeviceData deviceData = new DahuaDeviceData(srcAddr, data);
            processData(deviceData, dahuaSignature(srcAddr, ":", i, data),
                    udpConfigKey.getDefaultProductKey(), eventName);
        }
    }

    /**
     * 解析独立上报帧（CMD=0x02 非0x87帧，typeFlag=1~8）并上报
     */
    private void parseSimpleReportFrame(String srcAddr, int typeFlag, byte[] rawAdu) {
        List<Map<String, Object>> records = DahuaAduParser.parseSimpleFrame(rawAdu);
        if (records.isEmpty()) {
            log.warn("[UDP] 独立帧解析为空: src={}, type=0x{}", srcAddr, String.format("%02X", typeFlag));
            reportRawData(srcAddr, "dahua_report", rawAdu);
            return;
        }

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> data = records.get(i);
            String eventName;
            if (typeFlag == 2) {
                eventName = "dahua_component_report";
            } else if (typeFlag == 3 || typeFlag == 0x84) {
                eventName = "dahua_analog_report";
            } else {
                eventName = "dahua_data_report";
            }

            DeviceData deviceData = new DahuaDeviceData(srcAddr, data);
            processData(deviceData, dahuaSignature(srcAddr, ":sf:", i, data),
                    udpConfigKey.getDefaultProductKey(), eventName);
        }
    }

    /**
     * 处理注册帧 (CMD=0x00)
     * <p>
     * 收到注册帧后：
     * 1. 记录设备活跃时间
     * 2. 上报注册事件到平台
     * 3. 向设备发送确认帧（CMD=0x03），结束注册流程
     */
    private void handleRegister(String srcAddr, byte[] rawAdu, int serialNum, int version, String senderAddr) {
        log.info("[UDP] 注册: src={}, aduLen={}, serial={}", srcAddr, rawAdu.length, serialNum);
        reportRawData(srcAddr, "dahua_register", rawAdu);
        sendConfirmFrame(srcAddr, serialNum, version, senderAddr);
    }

    /**
     * 发送确认帧（CMD=0x03）到设备
     * <p>
     * 确认帧格式：@@ | 流水号 | 版本 | 时间标签 | 源地址(平台) | 目的地址(设备) | ADU长度=0 | CMD=0x03 | 校验和 | ##
     *
     * @param deviceAddr 设备地址（6字节HEX字符串）
     * @param serialNum  原帧流水号（回显）
     * @param version    原帧协议版本号（回显，如0x0302对应线上字节02 03）
     * @param senderAddr 设备网络地址（"host:port"）
     */
    private void sendConfirmFrame(String deviceAddr, int serialNum, int version, String senderAddr) {
        // 设备地址（6字节HEX → 字节数组，作为确认帧的目的地址）
        byte[] deviceAddrBytes = GbtFrameBuilder.hexToAddr(deviceAddr);
        // 构建确认帧：CMD=0x03，空ADU，版本号回显设备注册帧的版本
        byte[] confirmFrame = GbtFrameBuilder.buildConfirmFrame(serialNum, version,
                centerAddr, deviceAddrBytes);
        log.info("[UDP] → 确认帧: to={}, serial={}, len={}", deviceAddr, serialNum, confirmFrame.length);
        // 通过 UDP socket 发送到设备
        udpVerticle.sendPacket(senderAddr, confirmFrame);
    }

    /**
     * 上报原始数据（用于未完全解析的帧类型）
     */
    private void reportRawData(String srcAddr, String eventName, byte[] rawAdu) {
        log.info("[UDP] → 上报: src={}, event={}, aduLen={}", srcAddr, eventName,
                rawAdu != null ? rawAdu.length : 0);
        Map<String, Object> data = new HashMap<>();
        data.put("rawHex", DahuaAduParser.encodeHex(rawAdu));
        data.put("aduLength", rawAdu.length);

        DeviceData deviceData = new DahuaDeviceData(srcAddr, data);
        processData(deviceData, srcAddr + ":" + System.currentTimeMillis(),
                udpConfigKey.getDefaultProductKey(), eventName);
    }

    /**
     * 大华设备数据适配器 — 将解析后的 Map 适配为 DeviceData 接口
     */
    private static class DahuaDeviceData implements DeviceData {
        private final String deviceId;
        private final Map<String, Object> data;

        DahuaDeviceData(String deviceId, Map<String, Object> data) {
            this.deviceId = deviceId;
            this.data = data;
        }

        @Override public String getDeviceId() { return deviceId; }
        @Override public Map<String, Object> toMap() { return data; }
        @Override public UDPReportTypeEnum reportType() {
            // 含 subType/alarmType 字段的为事件上报
            if (data.containsKey("data_alarmType")) {
                return UDPReportTypeEnum.EVENT_REPORT;
            }
            return UDPReportTypeEnum.PROPERTY_REPORT;
        }
    }

    @Override
    protected void deviceTopoChange(DeviceTopoChange action) {

    }

    /**
     * 大华帧去重签名：设备地址 + 帧内序号 + 业务字段。
     * 排除时间戳字段（key 含 "time"），口径与 UWB createSignatureStr 一致：
     * 重发帧（同内容）被去重，业务数据变化后正常上报。
     */
    private String dahuaSignature(String srcAddr, String frameTag, int index, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder(srcAddr).append(frameTag).append(index);
        data.forEach((k, v) -> {
            if (!k.toLowerCase().contains("time")) {
                sb.append('|').append(k).append('=').append(v);
            }
        });
        return sb.toString();
    }
}
