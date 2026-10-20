package com.enjoyiot.module.eiot.service.devicealert;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.alert.AlertApi;
import com.enjoyiot.module.eiot.api.alert.dto.AlertConfig;
import com.enjoyiot.module.eiot.api.alert.dto.Message;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.devicealert.dto.DeviceAlertConfig;
import com.enjoyiot.module.eiot.api.devicealert.dto.DeviceAlertRecord;
import com.enjoyiot.module.eiot.api.enums.ErrorCodeConstants;
import com.enjoyiot.module.eiot.controller.admin.devicealert.vo.DeviceAlertConfigPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.devicealert.vo.DeviceAlertConfigSaveReqVO;
import com.enjoyiot.module.eiot.controller.app.cameraalert.enums.CameraEventTypeEnum;
import com.enjoyiot.module.eiot.controller.app.cameraalert.vo.CameraAlertReportReqVO;
import com.enjoyiot.module.eiot.convert.AlertConfigConvert;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.alertconfig.AlertConfigDO;
import com.enjoyiot.module.eiot.dal.dataobject.devicealert.DeviceAlertConfigDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.alertconfig.AlertConfigMapper;
import com.enjoyiot.module.eiot.dal.mysql.devicealert.DeviceAlertConfigMapper;
import com.enjoyiot.module.eiot.service.alarm.AlarmRealtimeNotifyService;
import com.enjoyiot.module.eiot.service.alarm.AlarmSemantics;
import com.enjoyiot.module.eiot.service.device.DeviceInfoService;
import com.enjoyiot.module.infra.api.file.FileApi;
import com.enjoyiot.module.system.api.sms.SmsSendApi;
import com.enjoyiot.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import com.enjoyiot.module.system.api.tenant.TenantApi;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@Validated
public class DeviceAlertConfigServiceImpl implements DeviceAlertConfigService {

    @Resource
    private DeviceAlertConfigMapper deviceAlertConfigMapper;

    @Resource
    private DeviceInfoService deviceInfoService;

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private AlarmRealtimeNotifyService alarmRealtimeNotifyService;

    @Resource
    private AlertConfigMapper alertConfigMapper;

    @Resource
    private AlertApi alertApi;

    @Resource
    private SmsSendApi smsSendApi;

    @Resource
    private TenantApi tenantApi;
    
    @Resource
    private FileApi fileApi;

    /**
     * 去重窗口（毫秒），默认 10 秒
     */
    private static final long CAMERA_DEDUP_WINDOW_MS = 10 * 1000L;

    @Override
    public Long createDeviceAlertConfig(DeviceAlertConfigSaveReqVO createReqVO) {
        DeviceAlertConfigDO config = BeanUtils.toBean(createReqVO, DeviceAlertConfigDO.class);
        if (createReqVO.getConditions() != null) {
            config.setConditions(JsonUtils.toJsonString(createReqVO.getConditions()));
        }
        if (createReqVO.getTriggerOptions() != null) {
            config.setTriggerOptions(JsonUtils.toJsonString(createReqVO.getTriggerOptions()));
        }
        deviceAlertConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateDeviceAlertConfig(DeviceAlertConfigSaveReqVO updateReqVO) {
        validateDeviceAlertConfigExists(updateReqVO.getId());
        DeviceAlertConfigDO updateObj = BeanUtils.toBean(updateReqVO, DeviceAlertConfigDO.class);
        if (updateReqVO.getConditions() != null) {
            updateObj.setConditions(JsonUtils.toJsonString(updateReqVO.getConditions()));
        }
        if (updateReqVO.getTriggerOptions() != null) {
            updateObj.setTriggerOptions(JsonUtils.toJsonString(updateReqVO.getTriggerOptions()));
        }
        deviceAlertConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteDeviceAlertConfig(Long id) {
        validateDeviceAlertConfigExists(id);
        deviceAlertConfigMapper.deleteById(id);
    }

    @Override
    public DeviceAlertConfig getDeviceAlertConfig(Long id) {
        return tenantAware(() -> convertToDeviceAlertConfig(deviceAlertConfigMapper.selectById(id)));
    }

    @Override
    public PageResult<DeviceAlertConfig> getDeviceAlertConfigPage(DeviceAlertConfigPageReqVO pageReqVO) {
        return tenantAware(() -> {
            PageResult<DeviceAlertConfigDO> pageResult = deviceAlertConfigMapper.selectPage(pageReqVO);
            return new PageResult<>(
                    pageResult.getList().stream()
                            .map(this::convertToDeviceAlertConfig)
                            .toList(),
                    pageResult.getTotal()
            );
        });
    }

    @Override
    public List<DeviceAlertConfig> getDeviceAlertConfigListByDeviceId(Long deviceId) {
        return tenantAware(() ->
                deviceAlertConfigMapper.selectByDeviceId(deviceId).stream()
                        .map(this::convertToDeviceAlertConfig)
                        .toList()
        );
    }

    @Override
    public List<DeviceAlertConfig> getDeviceAlertConfigListByProductKey(String productKey) {
        return tenantAware(() ->
                deviceAlertConfigMapper.selectByProductKey(productKey).stream()
                        .map(this::convertToDeviceAlertConfig)
                        .toList()
        );
    }

    @Override
    public void addDeviceAlertRecord(DeviceAlertRecord record) {
        // 1. 同设备同名且未到处置终态的告警不重复入库
        AlertRecordDO existActiveAlert = alertRecordMapper.selectActiveAlert(record.getDeviceId(), record.getName());
        if (existActiveAlert != null) {
            log.info("当前设备 {} 下已存在同名告警 {}", record.getDeviceId(), existActiveAlert.getName());
            return;
        }
        // 2. 映射为统一告警记录：alarmType=0 主机报警；
        //    写侧三写（定稿 4.1 W2）：接入层归一配置 level → 报警类型，定级层派生等级，一级兜底Ⅳ档
        long alertTime = record.getAlertTime() != null ? record.getAlertTime() : System.currentTimeMillis();
        Integer alarmKind = AlarmSemantics.deviceConfigLevelToKind(record.getLevel());
        AlertRecordDO recordDO = AlertRecordDO.builder()
                .alertTime(alertTime)
                .name(record.getName())
                .level(alarmKind != null ? AlarmSemantics.deriveLevel(alarmKind) : record.getLevel())
                .alarmKind(alarmKind)
                .tier(AlarmSemantics.defaultTierForKind(alarmKind))
                .details(record.getDetails())
                .alarmType(0)
                .readFlg(false)
                .handleStatus(0)
                .deviceId(record.getDeviceId())
                .productKey(record.getProductKey())
                .build();
        // 3. 调用链在 TenantUtils.executeIgnore 上下文，租户插件跳过 INSERT 的 tenant_id 填充，需显式设置
        recordDO.setTenantId(record.getTenantId());
        // 4. 落库
        alertRecordMapper.insert(recordDO);
        // 5. 落库成功后推送 duty-screen（失败不影响落库）
        pushRealtimeAlarm(recordDO, alertTime);
    }

    @Override
    public void recoverDeviceAlertRecord(Long deviceId, String alertName) {
        AlertRecordDO activeAlert = alertRecordMapper.selectActiveAlert(deviceId, alertName);
        if (activeAlert != null) {
            // 设备恢复视为告警闭环（置为已处置），否则活动告警会挡住所设备后续同类报警的重新入库与弹窗
            activeAlert.setHandleStatus(3);
            alertRecordMapper.updateById(activeAlert);
        }
    }

    /**
     * 设备告警落库成功后实时推送 duty-screen
     */
    private void pushRealtimeAlarm(AlertRecordDO recordDO, long alertTime) {
        try {
            DeviceInfo device = recordDO.getDeviceId() != null
                    ? deviceInfoService.getDeviceInfoFromCache(recordDO.getDeviceId())
                    : null;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("alertId", recordDO.getId());
            payload.put("alertName", StrUtil.nullToEmpty(recordDO.getName()));
            // 级别出口对齐四维：取定级层产出的等级（1/2/3），与落库值一致
            payload.put("level", StrUtil.nullToEmpty(recordDO.getLevel()));
            payload.put("alarmType", 0);
            payload.put("alarmKind", recordDO.getAlarmKind());
            payload.put("tier", recordDO.getTier());
            payload.put("deviceId", recordDO.getDeviceId() != null ? recordDO.getDeviceId() : 0);
            payload.put("devSno", device != null ? StrUtil.nullToEmpty(device.getDn()) : "");
            payload.put("alertTime", alertTime);
            payload.put("alertImage", "");
            payload.put("deviceName", device != null ? StrUtil.nullToEmpty(device.getName()) : "");
            payload.put("tenantName", "");
            payload.put("subDeviceName", "");
            payload.put("alertDetails", StrUtil.nullToEmpty(recordDO.getDetails()));
            alarmRealtimeNotifyService.notifyAlarmSaved(payload, recordDO.getTenantId());
        } catch (Exception e) {
            log.error("[告警推送] 设备告警实时推送异常, alertId={}, deviceId={}", recordDO.getId(), recordDO.getDeviceId(), e);
        }
    }

    private DeviceAlertConfig convertToDeviceAlertConfig(DeviceAlertConfigDO configDO) {
        if (configDO == null) {
            return null;
        }
        DeviceAlertConfig config = new DeviceAlertConfig();
        config.setId(configDO.getId());
        config.setName(configDO.getName());
        config.setProductKey(configDO.getProductKey());
        config.setDeviceId(configDO.getDeviceId());
        config.setLevel(configDO.getLevel());
        config.setStatus(configDO.getStatus());
        config.setRemark(configDO.getRemark());
        config.setTenantId(configDO.getTenantId());

        if (configDO.getConditions() != null && !configDO.getConditions().isEmpty()) {
            try {
                List<DeviceAlertConfig.AlertCondition> conditions = JsonUtils.parseObject(
                        configDO.getConditions(),
                        new TypeReference<List<DeviceAlertConfig.AlertCondition>>() {
                        }
                );
                config.setConditions(conditions);
            } catch (Exception e) {
                log.error("Failed to parse conditions JSON: {}", configDO.getConditions(), e);
            }
        }

        if (configDO.getTriggerOptions() != null && !configDO.getTriggerOptions().isEmpty()) {
            try {
                DeviceAlertConfig.TriggerOptions triggerOptions = JsonUtils.parseObject(
                        configDO.getTriggerOptions(),
                        DeviceAlertConfig.TriggerOptions.class
                );
                config.setTriggerOptions(triggerOptions);
            } catch (Exception e) {
                log.error("Failed to parse triggerOptions JSON: {}", configDO.getTriggerOptions(), e);
            }
        }

        return config;
    }

    private void validateDeviceAlertConfigExists(Long id) {
        if (deviceAlertConfigMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.NOT_EXISTS);
        }
    }

    private boolean isSystemTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null && tenantId == 1L;
    }

    private <T> T tenantAware(Supplier<T> supplier) {
        if (isSystemTenant()) {
            return TenantUtils.executeIgnoreWithResult(supplier::get);
        }
        return supplier.get();
    }

    @Override
    public void reportCameraAlert(CameraAlertReportReqVO reqVO) {
        // 使用 TenantUtils.executeIgnore 包裹，确保外部调用无租户上下文时也能正常工作
        TenantUtils.executeIgnore(() -> {
            // 1. 根据事件类型映射报警名称
            CameraEventTypeEnum alertNameByType = CameraEventTypeEnum.getAlertNameByType(reqVO.getEventType());

            String alertName = "";
            if (!ObjectUtils.isEmpty(alertNameByType)) {
                alertName = alertNameByType.getAlertName();
            }

            // 写侧三写（定稿 4.1 W3）：接入层归一——高温/热成像/烟火→火警一级（自带画面复核→Ⅲ档）；
            // 离岗/通道占用→隐患二级；未知事件类型保持旧默认"4"（不写新语义，走双读回退）
            Integer alarmKind = cameraEventToKind(reqVO.getEventType());
            String level = alarmKind != null ? AlarmSemantics.deriveLevel(alarmKind) : "4";
            Integer tier = (alarmKind != null && alarmKind == AlarmSemantics.KIND_FIRE)
                    ? AlarmSemantics.TIER_III : null;

            // 2. 尝试通过 devSno 查找设备，获取 deviceId
            Long deviceId = null;
            DeviceInfo device = null;
            String devSno = reqVO.getDevSno();
            if (devSno != null && !devSno.isEmpty()) {
                // 必须要先添加设备
                device = deviceInfoService.getDeviceByDn(devSno);
                if (device != null) {
                    deviceId = device.getId();
                } else {
                    log.warn("Camera alert device not found by devSno: {}, will save without deviceId", devSno);
                }
            }

            String alertImageUrl = null;
            try {
                MultipartFile file = reqVO.getFile();
                // 2. 上传抓拍到文件存储，获取图片 URL
                if (file != null && !file.isEmpty()) {
                    alertImageUrl = fileApi.createFile(file.getOriginalFilename(), null, file.getBytes());
                    log.info("摄像头报警图片上传成功：{}", alertImageUrl);
                }

            } catch (Exception e) {
                log.error("图片转换失败, cameraNum={}", reqVO.getDevSno(), e);
            }

            // 4. 构建报警记录，写入 eiot_alert_record
            // 报警类型：摄像头事件类型 1-5 与 alarm_type 定义一致（高温/热成像/烟火/离岗/消防通道占用）
            AlertRecordDO recordDO = AlertRecordDO.builder()
                    .alertTime(reqVO.getEventDate() != null ? reqVO.getEventDate() : System.currentTimeMillis())
                    .name(alertName)
                    .level(level)
                    .alarmKind(alarmKind)
                    .tier(tier)
                    .details("事件类型=" + alertName + ", devSno=" + devSno)
                    .alarmType(reqVO.getEventType())
                    .readFlg(false)
                    .deviceId(deviceId)
                    .productKey(reqVO.getProductKey())
                    .alertImage(alertImageUrl)
                    .build();

            if (!ObjectUtils.isEmpty(device)) {
                recordDO.setTenantId(device.getTenantId());
            } else {
                recordDO.setTenantId(reqVO.getTenantId());
            }

            // 查询最近的摄像头报警记录，避免重复报警
            if (deviceId != null && StrUtil.isNotBlank(alertImageUrl)) {
                long alertTimeMs = recordDO.getAlertTime();
                AlertRecordDO cameraAlertRecord = alertRecordMapper.selectRecentCameraAlert(deviceId, alertImageUrl, alertTimeMs - CAMERA_DEDUP_WINDOW_MS, alertTimeMs + CAMERA_DEDUP_WINDOW_MS);
                if (cameraAlertRecord != null) {
                    log.warn("报警记录存在，将不重复保存与推送: {}, devSno: {}, productKey: {}, eventType: {}, alertName: {}, image: {}",
                            cameraAlertRecord.getId(), devSno, reqVO.getProductKey(), reqVO.getEventType(), alertName, alertImageUrl);
                    return;
                }
            }

            alertRecordMapper.insert(recordDO);
            log.info("Camera alert reported, deviceId: {}, devSno: {}, productKey: {}, eventType: {}, alertName: {}, image: {}",
                    deviceId, devSno, reqVO.getProductKey(), reqVO.getEventType(), alertName, alertImageUrl);

            // 5. 门面推送实时告警（过滤/双发由门面处理，修复原全租户广播问题）
            try {
                // 告警租户：优先设备租户，回退请求租户
                Long wsTenantId = recordDO.getTenantId();
                LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
                payload.put("alertId", recordDO.getId());
                payload.put("alertName", alertName);
                payload.put("level", level);
                payload.put("alarmType", reqVO.getEventType());
                payload.put("alarmKind", alarmKind);
                payload.put("tier", tier);
                payload.put("deviceId", deviceId != null ? deviceId : 0);
                payload.put("devSno", devSno != null ? devSno : "");
                payload.put("alertTime", recordDO.getAlertTime());
                payload.put("alertImage", alertImageUrl != null ? alertImageUrl : "");
                payload.put("deviceName", device != null && device.getName() != null ? device.getName() : "");
                payload.put("tenantName", "");
                payload.put("subDeviceName", "");
                payload.put("alertDetails", "");

                alarmRealtimeNotifyService.notifyAlarmSaved(payload, wsTenantId);
                log.info("Camera alert WebSocket notification sent, alertId: {}", recordDO.getId());
            } catch (Exception e) {
                log.warn("Failed to send WebSocket notification for camera alert: {}", e.getMessage());
            }

            // 6. 算法报警短信通知（失败不影响落库与推送主流程）
            try {
                sendCameraAlertSmsNotify(recordDO.getTenantId(), reqVO.getEventType(), alertName, device, recordDO.getAlertTime(), !ObjectUtils.isEmpty(device) ? device.getAddr() : null);
            } catch (Exception e) {
                log.warn("[摄像头报警短信] 通知异常, eventType={}, deviceId={}, 原因: {}",
                        reqVO.getEventType(), deviceId, e.getMessage());
            }
        });
    }

    /**
     * 摄像头事件类型（来源值域 1-5）→ 报警类型（定稿 1.4 / 3.1）
     * <p>
     * 1=高温 / 2=热成像 / 3=烟火→火警（火灾迹象）；4=离岗 / 5=消防通道占用→隐患；未知→null（不写新语义）
     */
    private static Integer cameraEventToKind(Integer eventType) {
        if (eventType == null) {
            return null;
        }
        switch (eventType) {
            case 1:
            case 2:
            case 3:
                return AlarmSemantics.KIND_FIRE;
            case 4:
            case 5:
                return AlarmSemantics.KIND_RISK;
            default:
                return null;
        }
    }

    /**
     * 摄像头算法报警短信通知
     *
     * 匹配规则：告警所属租户下启用中（status=0）且算法报警类型=事件类型的告警配置；
     * 多条配置匹配同一事件时仅取最新一条发送，避免重复短信骚扰；
     * 接收人仅通过 AlertApi#getAlertPushPhones 获取（消息推送岗位用户 + 当前值班人员，内部已去重）；
     * 短信模板编码取配置关联消息模板的系统短信模板编码（system_sms_template.code）。
     */
    private void sendCameraAlertSmsNotify(Long tenantId, Integer eventType, String alertName,
                                          DeviceInfo device, Long alertTime, String deviceAddr) {
        if (tenantId == null || eventType == null) {
            return;
        }
        // 1. 在告警所属租户上下文内匹配启用中的算法告警配置（多条匹配仅取最新一条）
        List<AlertConfigDO> matchedConfigs = TenantUtils.execute(tenantId, () ->
                alertConfigMapper.selectEnabledByAlgorithmAlertType(eventType));
        if (CollUtil.isEmpty(matchedConfigs)) {
            return;
        }
        AlertConfigDO configDO = matchedConfigs.get(0);

        // 2. 接收人：仅限告警配置的推送岗位用户 + 当前值班人员
        List<String> phones = alertApi.getAlertPushPhones(configDO.getId());
        if (CollUtil.isEmpty(phones)) {
            log.info("[摄像头报警短信] 告警配置无有效接收人，跳过发送, alertConfigId={}, eventType={}",
                    configDO.getId(), eventType);
            return;
        }

        // 3. 短信模板编码：取配置关联消息模板的系统短信模板编码
        Message message = alertApi.getNotifyMessage(AlertConfigConvert.INSTANCE.convert(configDO));
        String templateCode = message != null ? message.getSmsTemplateCode() : null;
        if (StrUtil.isBlank(templateCode)) {
            log.warn("[摄像头报警短信] 消息模板缺少系统短信模板编码，跳过发送, alertConfigId={}, messageTemplateId={}",
                    configDO.getId(), configDO.getMessageTemplateId());
            return;
        }

        // 4. 模板参数
        Map<String, Object> templateParams = new LinkedHashMap<>();
        templateParams.put("deviceName", device != null && device.getName() != null ? device.getName() : "");
        templateParams.put("alertName", StrUtil.nullToEmpty(alertName));
        templateParams.put("time", DateUtil.format(
                new Date(alertTime != null ? alertTime : System.currentTimeMillis()), "yyyy-MM-dd HH:mm:ss"));
        templateParams.put("propertyName", getTenantNameQuietly(tenantId));
        templateParams.put("deviceAddr", deviceAddr);

        // 5. 逐个发送，单号失败不影响其余
        for (String phone : phones) {
            try {
                SmsSendSingleToUserReqDTO reqDTO = new SmsSendSingleToUserReqDTO();
                reqDTO.setMobile(phone);
                reqDTO.setTemplateCode(templateCode);
                reqDTO.setTemplateParams(templateParams);
                smsSendApi.sendSingleSmsToAdmin(reqDTO);
                log.info("[摄像头报警短信] 已发送, phone={}, alertConfigId={}, eventType={}, alertName={}",
                        phone, configDO.getId(), eventType, alertName);
            } catch (Exception e) {
                log.warn("[摄像头报警短信] 发送失败, phone={}, alertConfigId={}, 原因: {}",
                        phone, configDO.getId(), e.getMessage());
            }
        }
    }

    private String getTenantNameQuietly(Long tenantId) {
        try {
            return StrUtil.nullToEmpty(tenantApi.getTenantName(tenantId));
        } catch (Exception e) {
            return "";
        }
    }
}
