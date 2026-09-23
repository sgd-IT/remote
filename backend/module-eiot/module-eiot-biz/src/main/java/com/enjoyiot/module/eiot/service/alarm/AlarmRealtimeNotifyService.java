package com.enjoyiot.module.eiot.service.alarm;

import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.common.enums.UserTypeEnum;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.infra.api.websocket.WebSocketSenderApi;
import com.enjoyiot.module.system.api.tenant.TenantApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 告警实时推送门面：告警落库后统一推送 WebSocket 实时消息（duty-screen 大屏消费）
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class AlarmRealtimeNotifyService {

    /** WebSocket 消息类型：duty-screen 大屏契约 */
    public static final String MESSAGE_TYPE = "firewatch-alert-new";

    /** 系统租户编号（tenant_id=1，即 admin） */
    private static final Long SYSTEM_TENANT_ID = 1L;

    /** info 等级数字编码（不弹窗） */
    private static final String LEVEL_INFO = "6";

    @Resource
    private WebSocketSenderApi webSocketSenderApi;

    @Resource
    private TenantApi tenantApi;

    /**
     * 告警落库成功后实时推送到大屏
     *
     * @param payload       推送内容（为空时忽略），字段同摄像头链路：
     *                      alertId/alertName/level/alarmType/deviceId/devSno/alertTime/alertImage/
     *                      deviceName/tenantName/subDeviceName/alertDetails
     * @param alarmTenantId 告警所属租户编号
     */
    public void notifyAlarmSaved(Map<String, Object> payload, Long alarmTenantId) {
        if (payload == null) {
            return;
        }
        try {
            // 1. info 等级不弹窗（与 AEP alarm-push 的既有过滤规则对齐）
            Object level = payload.get("level");
            if (level != null && (LEVEL_INFO.equals(level.toString()) || "info".equalsIgnoreCase(level.toString()))) {
                return;
            }
            // 2. 填充单位名称（租户名称），供大屏语音播报使用
            Object tenantName = payload.get("tenantName");
            if ((tenantName == null || StrUtil.isBlank(tenantName.toString())) && alarmTenantId != null) {
                try {
                    payload.put("tenantName", StrUtil.nullToEmpty(tenantApi.getTenantName(alarmTenantId)));
                } catch (Exception e) {
                    log.warn("[告警推送] 查询租户名称失败, tenantId={}", alarmTenantId, e);
                }
            }
            // 3. 双发：告警租户 + 系统租户（系统租户可见全部告警）
            if (alarmTenantId != null && !SYSTEM_TENANT_ID.equals(alarmTenantId)) {
                sendToTenant(payload, alarmTenantId);
            }
            sendToTenant(payload, SYSTEM_TENANT_ID);
        } catch (Exception e) {
            // 推送失败不影响告警落库
            log.error("[告警推送] WebSocket推送异常, alarmTenantId={}, alertId={}",
                    alarmTenantId, payload.get("alertId"), e);
        }
    }

    /**
     * 在指定租户上下文内发送（会话管理器按调用线程租户过滤会话）
     */
    private void sendToTenant(Map<String, Object> payload, Long tenantId) {
        TenantUtils.execute(tenantId, () -> webSocketSenderApi.sendObject(
                UserTypeEnum.ADMIN.getValue(), MESSAGE_TYPE, payload));
    }
}
