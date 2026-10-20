package com.enjoyiot.module.eiot.service.iot;


import cn.hutool.core.util.StrUtil;
import com.enjoyiot.eiot.message.event.MessageEvent;
import com.enjoyiot.eiot.message.listener.MessageEventListener;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.api.alert.dto.AlertConfig;
import com.enjoyiot.module.eiot.api.alert.dto.Message;
import com.enjoyiot.module.eiot.service.alarm.AlarmRealtimeNotifyService;
import com.enjoyiot.module.eiot.service.alarm.AlarmSemantics;
import com.enjoyiot.module.eiot.service.alert.AlertConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author sjg
 */
@Slf4j
@Component
public class AlertMessageEventListener implements MessageEventListener {

    @Resource
    private AlertConfigService alertConfigService;

    @Resource
    private AlarmRealtimeNotifyService alarmRealtimeNotifyService;

    @Override
    @EventListener(classes = MessageEvent.class)
    public void doEvent(MessageEvent event) {
        Message message = event.getMessage();
        TenantContextHolder.setTenantId(message.getTenantId());
        AlertConfig alertConfig = alertConfigService.getAlertConfig(message.getAlertConfigId());
        Long recordId = alertConfigService.addAlertRecord(alertConfig, message);
        pushRealtimeAlarm(alertConfig, message, recordId);
    }

    /**
     * 落库成功后补发实时推送（recover 恢复事件不弹窗；异常不影响其它监听器）
     */
    private void pushRealtimeAlarm(AlertConfig alertConfig, Message message, Long recordId) {
        try {
            if (recordId == null || alertConfig == null) {
                return;
            }
            Map<String, Object> param = message.getParam();
            if (param != null && "recover".equals(param.get("alertState"))) {
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("alertId", recordId);
            payload.put("alertName", alertConfig.getName());
            // 级别出口对齐四维：与 addAlertRecord 同源归一（AlarmSemantics 纯函数保证一致）
            Integer alarmKind = AlarmSemantics.alertConfigLevelToKind(alertConfig.getLevel());
            payload.put("level", alarmKind != null
                    ? AlarmSemantics.deriveLevel(alarmKind)
                    : StrUtil.nullToEmpty(alertConfig.getLevel()));
            // 规则引擎告警均为主机报警（TTS 按 alarmType=0 播报）
            payload.put("alarmType", 0);
            payload.put("alarmKind", alarmKind);
            payload.put("tier", AlarmSemantics.defaultTierForKind(alarmKind));
            payload.put("deviceId", message.getDeviceId());
            payload.put("devSno", StrUtil.nullToEmpty(message.getDn()));
            payload.put("alertTime", System.currentTimeMillis());
            payload.put("alertImage", "");
            payload.put("deviceName", StrUtil.nullToEmpty(message.getDn()));
            payload.put("tenantName", "");
            Object subDeviceName = param != null ? param.get("subDeviceName") : null;
            payload.put("subDeviceName", subDeviceName != null ? subDeviceName.toString() : "");
            payload.put("alertDetails", resolveAlertDetails(message, param));
            alarmRealtimeNotifyService.notifyAlarmSaved(payload, alertConfig.getTenantId());
        } catch (Exception e) {
            log.error("[告警推送] 规则引擎告警实时推送异常, recordId={}, deviceId={}",
                    recordId, message.getDeviceId(), e);
        }
    }

    /**
     * 解析告警详情文本（param 为空或格式化异常时回退原始 content）
     */
    private String resolveAlertDetails(Message message, Map<String, Object> param) {
        if (param != null) {
            try {
                return StrUtil.nullToEmpty(message.getFormatContent());
            } catch (Exception e) {
                // 回退原始 content
            }
        }
        return StrUtil.nullToEmpty(message.getContent());
    }
}
