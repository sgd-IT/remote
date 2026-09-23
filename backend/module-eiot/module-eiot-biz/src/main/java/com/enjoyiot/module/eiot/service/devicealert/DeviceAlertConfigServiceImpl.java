package com.enjoyiot.module.eiot.service.devicealert;

import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.devicealert.dto.DeviceAlertConfig;
import com.enjoyiot.module.eiot.api.devicealert.dto.DeviceAlertRecord;
import com.enjoyiot.module.eiot.api.enums.ErrorCodeConstants;
import com.enjoyiot.module.eiot.controller.admin.devicealert.vo.DeviceAlertConfigPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.devicealert.vo.DeviceAlertConfigSaveReqVO;
import com.enjoyiot.module.eiot.controller.app.cameraalert.enums.CameraEventTypeEnum;
import com.enjoyiot.module.eiot.controller.app.cameraalert.vo.CameraAlertReportReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.devicealert.DeviceAlertConfigDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.devicealert.DeviceAlertConfigMapper;
import com.enjoyiot.module.eiot.service.alarm.AlarmRealtimeNotifyService;
import com.enjoyiot.module.eiot.service.device.DeviceInfoService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

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
        // 2. 映射为统一告警记录，alarmType=0 主机报警
        long alertTime = record.getAlertTime() != null ? record.getAlertTime() : System.currentTimeMillis();
        AlertRecordDO recordDO = AlertRecordDO.builder()
                .alertTime(alertTime)
                .name(record.getName())
                .level(record.getLevel())
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
        pushRealtimeAlarm(record, recordDO.getId(), alertTime);
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
    private void pushRealtimeAlarm(DeviceAlertRecord record, Long alertId, long alertTime) {
        try {
            DeviceInfo device = record.getDeviceId() != null
                    ? deviceInfoService.getDeviceInfoFromCache(record.getDeviceId())
                    : null;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("alertId", alertId);
            payload.put("alertName", StrUtil.nullToEmpty(record.getName()));
            payload.put("level", StrUtil.nullToEmpty(record.getLevel()));
            payload.put("alarmType", 0);
            payload.put("deviceId", record.getDeviceId() != null ? record.getDeviceId() : 0);
            payload.put("devSno", device != null ? StrUtil.nullToEmpty(device.getDn()) : "");
            payload.put("alertTime", alertTime);
            payload.put("alertImage", "");
            payload.put("deviceName", device != null ? StrUtil.nullToEmpty(device.getName()) : "");
            payload.put("tenantName", "");
            payload.put("subDeviceName", "");
            payload.put("alertDetails", StrUtil.nullToEmpty(record.getDetails()));
            alarmRealtimeNotifyService.notifyAlarmSaved(payload, record.getTenantId());
        } catch (Exception e) {
            log.error("[告警推送] 设备告警实时推送异常, alertId={}, deviceId={}", alertId, record.getDeviceId(), e);
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
    public void reportCameraAlert(CameraAlertReportReqVO reqVO, String alertImageUrl) {
        // 使用 TenantUtils.executeIgnore 包裹，确保外部调用无租户上下文时也能正常工作
        TenantUtils.executeIgnore(() -> {
            // 1. 根据事件类型映射报警名称
            CameraEventTypeEnum alertNameByType = CameraEventTypeEnum.getAlertNameByType(reqVO.getEventType());

            String alertName = "";
            String level = "4";
            if (!ObjectUtils.isEmpty(alertNameByType)) {
                alertName = alertNameByType.getAlertName();
                level = alertNameByType.getLevel();
            }

            // 2. 尝试通过 devSno 查找设备，获取 deviceId
            Long deviceId = null;
            DeviceInfo device = null;
            String devSno = reqVO.getDevSno();
            if (devSno != null && !devSno.isEmpty()) {
                device = deviceInfoService.getDeviceByDn(devSno);
                if (device != null) {
                    deviceId = device.getId();
                } else {
                    log.warn("Camera alert device not found by devSno: {}, will save without deviceId", devSno);
                }
            }

            // 4. 构建报警记录，写入 eiot_alert_record
            // 报警类型：摄像头事件类型 1-5 与 alarm_type 定义一致（高温/热成像/烟火/离岗/消防通道占用）
            AlertRecordDO recordDO = AlertRecordDO.builder()
                    .alertTime(reqVO.getEventDate() != null ? reqVO.getEventDate() : System.currentTimeMillis())
                    .name(alertName)
                    .level(level)
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


            alertRecordMapper.insert(recordDO);
            log.info("Camera alert reported, deviceId: {}, devSno: {}, productKey: {}, eventType: {}, alertName: {}, image: {}",
                    deviceId, devSno, reqVO.getProductKey(), reqVO.getEventType(), alertName, alertImageUrl);

            // 5. 门面推送实时告警（过滤/双发由门面处理，修复原全租户广播问题）
            try {
                // 告警租户：优先设备租户，回退请求租户
                Long wsTenantId = recordDO.getTenantId();
                if (wsTenantId == null) {
                    wsTenantId = reqVO.getTenantId();
                }

                java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
                payload.put("alertId", recordDO.getId());
                payload.put("alertName", alertName);
                payload.put("level", level);
                payload.put("alarmType", reqVO.getEventType());
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
        });
    }
}
