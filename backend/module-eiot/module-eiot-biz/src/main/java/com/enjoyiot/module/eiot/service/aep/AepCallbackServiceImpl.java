package com.enjoyiot.module.eiot.service.aep;

import cn.hutool.core.util.IdUtil;
import com.enjoyiot.eiot.common.constant.Constants;
import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.message.core.MqProducer;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.RegisterDevice;
import com.enjoyiot.module.eiot.api.product.dto.Product;
import com.enjoyiot.module.eiot.controller.admin.aep.dto.AepPushMessage;
import com.enjoyiot.module.eiot.controller.admin.device.vo.DeviceInfoSaveReqVO;
import com.enjoyiot.module.eiot.controller.admin.product.vo.ProductSaveReqVO;
import com.enjoyiot.module.eiot.service.alarm.AlarmMessage;
import com.enjoyiot.module.eiot.service.alarm.AlarmProcessingStrategy.AlarmParseContext;
import com.enjoyiot.module.eiot.service.alarm.AlarmRecordService;
import com.enjoyiot.module.eiot.service.alarm.AlarmSourceType;
import com.enjoyiot.module.eiot.service.alarm.AlarmStrategyFactory;
import com.enjoyiot.module.eiot.service.device.DeviceInfoService;
import com.enjoyiot.module.eiot.service.product.ProductService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AEP 推送回调处理实现
 * 使用 @Async 异步处理，确保 Controller 立即返回 HTTP 200
 */
@Slf4j
@Service
public class AepCallbackServiceImpl implements AepCallbackService {

    private static final String MSG_DATA_REPORT = "dataReport";
    private static final String MSG_COMMAND_RESPONSE = "commandResponse";
    private static final String MSG_EVENT_REPORT = "eventReport";
    private static final String MSG_ONLINE_OFFLINE = "deviceOnlineOfflineReport";
    private static final String MSG_ADD_DELETE_DEVICE = "addDeleteDevice";
    private static final String MSG_DATA_REPORT_TUP = "dataReportTupUnion";

    @Resource
    private DeviceInfoService deviceInfoService;

    @Resource
    private MqProducer<ThingModelMessage> producer;

    @Resource
    private ProductService productService;

    @Resource
    private AlarmStrategyFactory alarmStrategyFactory;

    @Resource
    private AlarmRecordService alarmRecordService;

    @Override
    @Async
    @TenantIgnore
    public void processPushMessage(AepPushMessage message) {
        try {
            String messageType = message.getMessageType();
            String aepDeviceId = message.getDeviceId();
            log.info("[AEP推送] messageType={}, deviceId={}, tenantId={}, protocol={}, timestamp={}",
                    messageType, aepDeviceId, message.getTenantId(), message.getProtocol(), message.getTimestamp());

            // 设备增删消息单独处理
            if (MSG_ADD_DELETE_DEVICE.equals(messageType)) {
                handleAddDeleteDevice(message);
                return;
            }

            // 根据 AEP deviceId(即本系统 dn) 查找设备
            DeviceInfo deviceInfo = deviceInfoService.getDeviceByDn(aepDeviceId);
            if (Objects.isNull(deviceInfo)) {
                log.warn("[AEP推送] 设备不存在, aepDeviceId={}, productId={}, messageType={}, " +
                                "请先在AEP平台触发设备注册(addDeleteDevice消息)或在本地手动录入设备",
                        aepDeviceId, message.getProductId(), messageType);
                return;
            }

            // NB 烟感是 24 小时一报的休眠设备，AEP 的上下线推送反映的是模组寻呼/PSM 状态而非设备存活，
            if (MSG_ONLINE_OFFLINE.equals(messageType)) {
                log.info("[AEP推送] 忽略上下线推送, deviceId={}, eventType={}, 离线按保活超时判定",
                        aepDeviceId, message.getEventType());
                return;
            }

            // 根据消息类型构建 ThingModelMessage
            ThingModelMessage tmm = switch (messageType) {
                case MSG_DATA_REPORT, MSG_DATA_REPORT_TUP -> buildDataReportMessage(message, deviceInfo);
                case MSG_EVENT_REPORT -> buildEventReportMessage(message, deviceInfo);
                case MSG_COMMAND_RESPONSE -> buildCommandResponseMessage(message, deviceInfo);
                default -> {
                    log.warn("[AEP推送] 未知消息类型: {}", messageType);
                    yield null;
                }
            };

            // 发布到消息总线
            if (tmm != null) {
                producer.publish(Constants.THING_MODEL_MESSAGE_TOPIC, tmm);
                log.info("[AEP推送] 已发布到MQ, type={}, deviceId={}", messageType, aepDeviceId);
            }

            // ── eventReport / dataReport → 告警处理 ──
            processAlarmIfNeeded(message, deviceInfo);
        } catch (Exception e) {
            log.error("[AEP推送] 处理异常", e);
        }
    }

    /**
     * 设备数据变化 → ThingModelMessage(type=property, identifier=report)
     */
    private ThingModelMessage buildDataReportMessage(AepPushMessage msg, DeviceInfo device) {
        Object data = msg.getPayload();
        return ThingModelMessage.builder()
                .id(IdUtil.fastSimpleUUID())
                .mid(IdUtil.fastSimpleUUID())
                .deviceId(device.getId())
                .productKey(device.getProductKey())
                .dn(device.getDn())
                .type(ThingModelMessage.TYPE_PROPERTY)
                .identifier(ThingModelMessage.ID_PROPERTY_REPORT)
                .data(data)
                .time(msg.getTimestamp())
                .occurred(msg.getTimestamp())
                .build();
    }

    /**
     * 设备事件上报 → ThingModelMessage(type=event, identifier=eventType)
     * eventType: 1=信息, 2=警告, 3=故障
     */
    private ThingModelMessage buildEventReportMessage(AepPushMessage msg, DeviceInfo device) {
        String eventName = resolveEventName(msg.getEventType());
        return ThingModelMessage.builder()
                .id(IdUtil.fastSimpleUUID())
                .mid(IdUtil.fastSimpleUUID())
                .deviceId(device.getId())
                .productKey(device.getProductKey())
                .dn(device.getDn())
                .type(ThingModelMessage.TYPE_EVENT)
                .identifier(eventName)
                .data(msg.getEventContent())
                .time(msg.getTimestamp())
                .occurred(msg.getTimestamp())
                .build();
    }

    /**
     * 设备命令响应 → ThingModelMessage(type=service, identifier=taskId_reply)
     */
    private ThingModelMessage buildCommandResponseMessage(AepPushMessage msg, DeviceInfo device) {
        String taskIdStr = msg.getTaskId() != null ? String.valueOf(msg.getTaskId()) : "unknown";
        Object resultData = null;
        if (msg.getResult() != null) {
            resultData = msg.getResult().getResultDetail();
        }
        return ThingModelMessage.builder()
                .id(IdUtil.fastSimpleUUID())
                .mid(IdUtil.fastSimpleUUID())
                .deviceId(device.getId())
                .productKey(device.getProductKey())
                .dn(device.getDn())
                .type(ThingModelMessage.TYPE_SERVICE)
                .identifier(taskIdStr + ThingModelMessage.SERVICE_REPLY_SUFFIX)
                .data(resultData)
                .time(msg.getTimestamp())
                .occurred(msg.getTimestamp())
                .build();
    }

    /**
     * 设备增加/删除通知
     */
    private void handleAddDeleteDevice(AepPushMessage msg) {
        String operation = msg.getOperation();
        String productKey = msg.getProductId();
        List<AepPushMessage.DeviceItem> deviceList = msg.getDeviceList();

        log.info("[AEP推送] 设备增删, operation={}, productKey={}, deviceList={}",
                operation, productKey, deviceList);

        if (deviceList == null || deviceList.isEmpty()) {
            log.warn("[AEP推送] 设备增删消息 deviceList 为空");
            return;
        }

        // 检查产品是否存在，不存在则自动创建
        Product product = TenantUtils.executeIgnoreWithResult(() -> productService.getByPk(productKey));
        if (product == null) {
            log.info("[AEP推送] 产品不存在，自动创建默认产品, productKey={}", productKey);
            try {
                ProductSaveReqVO createReq = new ProductSaveReqVO();
                createReq.setProductKey(productKey);
                createReq.setName("AEP产品-" + productKey);
                createReq.setCategoryId(1L);
                createReq.setNodeType(2);
                createReq.setProtocolCode("MQTT");
                createReq.setKeepAliveTime(0L);
                createReq.setTransparent(false);
                createReq.setStatus(0);
                Long productId = TenantUtils.executeIgnoreWithResult(() -> productService.createProduct(createReq));
                log.info("[AEP推送] 自动创建产品成功, productKey={}, productId={}", productKey, productId);
            } catch (Exception e) {
                log.error("[AEP推送] 自动创建产品失败, productKey={}", productKey, e);
                return;
            }
        }

        for (AepPushMessage.DeviceItem item : deviceList) {
            String aepDeviceId = item.getDeviceId();
            String deviceName = item.getDeviceName();

            if ("add".equals(operation)) {
                // 自动注册设备：AEP productId 直接作为 productKey
                // 使用 TenantUtils.executeIgnoreWithResult 确保 tenant_id 不被填充（保持为 0）
                try {
                    final String finalAepDeviceId = aepDeviceId;
                    final String finalDeviceName = deviceName;
                    DeviceInfo registeredDevice = TenantUtils.executeIgnoreWithResult(() -> {
                        RegisterDevice registerDevice = RegisterDevice.builder()
                                .productKey(productKey)
                                .deviceName(finalAepDeviceId)  // AEP deviceId 作为系统 dn
                                .model(finalDeviceName)         // 设备名称作为型号
                                .build();
                        return deviceInfoService.registerDevice(registerDevice);
                    });
                    // 使用 addDeleteDevice 消息中的 deviceName 更新设备显示名称
                    if (finalDeviceName != null && !finalDeviceName.isEmpty()) {
                        DeviceInfoSaveReqVO updateReq = new DeviceInfoSaveReqVO();
                        updateReq.setId(registeredDevice.getId());
                        updateReq.setName(finalDeviceName);
                        updateReq.setDn(finalAepDeviceId);
                        updateReq.setProductKey(productKey);
                        updateReq.setSerialNo(finalAepDeviceId);
                        deviceInfoService.updateDeviceInfo(updateReq);
                    }
                    log.info("[AEP 推送] 设备自动注册成功，aepDeviceId={}, deviceName={}, deviceId={}, productKey={}",
                            aepDeviceId, finalDeviceName, registeredDevice.getId(), productKey);
                } catch (Exception e) {
                    log.error("[AEP 推送] 设备自动注册失败，aepDeviceId={}, productKey={}",
                            aepDeviceId, productKey, e);
                }
            } else if ("delete".equals(operation)) {
                // 设备删除（暂仅记录日志）
                log.info("[AEP推送] 设备删除通知, aepDeviceId={}, productKey={}, 如需同步删除请在本地手动处理",
                        aepDeviceId, productKey);
                deviceInfoService.deleteDeviceInfo(Long.valueOf(aepDeviceId));
            } else {
                log.warn("[AEP推送] 未知的设备增删操作: {}", operation);
            }
        }
    }

    // ==================== 告警处理 ====================

    /**
     * 检查是否需要触发告警处理
     * <p>
     * {@code eventReport} 与 {@code dataReport} 会进入告警管道：构建 AlarmParseContext → 策略工厂解析 → 落库存储。
     * 其中 dataReport 携带烟感属性快照（烟雾/故障/防拆等），由策略按属性值判定告警。
     * 上下线不在此处（见 processPushMessage 的过滤），离线告警需要时按保活超时补。
     * 调用失败不影响 MQ 发布和原有业务流程。
     */
    private void processAlarmIfNeeded(AepPushMessage message, DeviceInfo deviceInfo) {
        String messageType = message.getMessageType();

        // 仅处理 eventReport / dataReport
        if (!MSG_EVENT_REPORT.equals(messageType) && !MSG_DATA_REPORT.equals(messageType)) {
            return;
        }

        try {
            // 构建上下文
            Map<String, Object> eventParams = new java.util.LinkedHashMap<>();
            eventParams.put("aepMessageType", messageType);
            eventParams.put("aepEventType", message.getEventType());
            if (message.getEventContent() != null) {
                eventParams.put("eventContent", message.getEventContent());
            }
            if (message.getPayload() != null) {
                eventParams.put("aepPayload", message.getPayload());
            }

            AlarmParseContext context = AlarmParseContext.builder()
                    .sourceType(AlarmSourceType.AEP_HTTP_PUSH)
                    .deviceId(deviceInfo.getId())
                    .deviceName(deviceInfo.getDn())
                    .productKey(deviceInfo.getProductKey())
                    .eventName(messageType)
                    .eventParams(eventParams)
                    .occurredTime(message.getTimestamp())
                    .rawData(cn.hutool.json.JSONUtil.toJsonStr(message))
                    .build();

            // 以设备所属租户执行告警落库：外层 @TenantIgnore 会跳过租户填充，导致 tenant_id 落库为 0
            TenantUtils.execute(deviceInfo.getTenantId(), () -> {
                // 策略解析 → 落库（落库成功会自动触发 WebSocket 推送）
                List<AlarmMessage> alarmMessages = alarmStrategyFactory.parse(
                        AlarmSourceType.AEP_HTTP_PUSH, context);

                if (!alarmMessages.isEmpty()) {
                    alarmRecordService.saveAlarms(alarmMessages);
                }
            });
        } catch (Exception e) {
            // 告警处理失败不影响 MQ 发布
            log.error("[AEP推送] 告警处理异常, messageType={}, deviceId={}",
                    messageType, deviceInfo.getId(), e);
        }
    }

    // ==================== 工具方法 ====================

    private String resolveEventName(Integer eventType) {
        if (eventType == null) {
            return "unknown";
        }
        return switch (eventType) {
            case 1 -> "info";
            case 2 -> "warning";
            case 3 -> "fault";
            default -> "event_" + eventType;
        };
    }
}
