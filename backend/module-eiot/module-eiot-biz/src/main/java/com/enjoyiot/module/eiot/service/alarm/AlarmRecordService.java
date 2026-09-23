package com.enjoyiot.module.eiot.service.alarm;

import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.common.enums.UserTypeEnum;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.infra.api.websocket.WebSocketSenderApi;
import com.enjoyiot.module.system.api.tenant.TenantApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警记录统一存储服务
 * 负责将 {@link AlarmStrategyFactory} 解析后的标准化 {@link AlarmMessage}
 * 存入 {@code eiot_alert_record} 表，并提供去重检查。
 * 设计要点：
 * 与现有 {@code AlertService} 的区别：
 * {@code AlertService} 负责告警记录的查询和确认，本服务专注告警的创建（写入侧）。
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class AlarmRecordService {

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private WebSocketSenderApi webSocketSenderApi;

    @Resource
    private TenantApi tenantApi;

    @Resource
    private AlarmRealtimeNotifyService alarmRealtimeNotifyService;

    // ==================== 去重缓存 ====================

    /**
     * 去重窗口（毫秒），默认 5 分钟
     */
    private static final long DEDUP_WINDOW_MS = 5 * 60 * 1000L;

    /**
     * 去重缓存：dedupKey → 最后入库时间戳
     * <p>
     * key 格式：{deviceId}:{alarmType}:{sourceType}
     */
    private final java.util.concurrent.ConcurrentHashMap<String, Long> dedupCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    // ==================== 公共方法 ====================

    /**
     * 批量保存告警消息（每条独立事务，单条失败不影响其他）
     * <p>
     * @param messages 告警消息列表
     * @return 成功入库的告警记录ID列表
     */
    public List<Long> saveAlarms(List<AlarmMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> savedIds = new ArrayList<>(messages.size());
        for (AlarmMessage msg : messages) {
            try {
                Long id = saveAlarm(msg);
                if (id != null) {
                    savedIds.add(id);
                    // 落库成功后推送（事务外，避免同步 IO 占用连接）
                    pushToWebSocket(msg, id);
                }
            } catch (Exception e) {
                log.error("[告警存储] 单条告警入库失败, deviceId={}, alarmType={}",
                        msg.getDeviceId(), msg.getAlarmType(), e);
                // 单条失败不中断其他告警处理
            }
        }
        log.info("[告警存储] 批量入库完成, 总数={}, 成功={}", messages.size(), savedIds.size());
        return savedIds;
    }

    /**
     * 保存单条告警消息（带去重检查）
     *
     * @param message 告警消息
     * @return 入库成功返回记录ID，去重/跳过返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveAlarm(AlarmMessage message) {
        if (message == null) {
            return null;
        }

        // ── 1. 去重检查 ──
        String dedupKey = buildDedupKey(message);
        if (isDuplicate(dedupKey)) {
            log.debug("[告警存储] 去重跳过, dedupKey={}, deviceId={}, alarmType={}",
                    dedupKey, message.getDeviceId(), message.getAlarmType());
            return null;
        }

        // ── 2. 构建 AlertRecordDO ──
        AlertRecordDO record = buildAlertRecord(message);

        // ── 3. 设置租户ID ──
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            record.setTenantId(tenantId);
        }

        // ── 4. 落库 ──
        alertRecordMapper.insert(record);
        log.info("[告警存储] 创建告警记录成功, recordId={}, deviceId={}, alarmType={}, alarmLevel={}, tenantId={}",
                record.getId(), message.getDeviceId(), message.getAlarmType(),
                message.getAlarmLevel(), record.getTenantId());

        // ── 5. 更新去重缓存 ──
        dedupCache.put(dedupKey, System.currentTimeMillis());

        return record.getId();
    }

    // ==================== 去重逻辑 ====================

    /**
     * 构建去重键
     * <p>
     * 格式：{deviceId}:{alarmType}:{sourceType.code}
     */
    private String buildDedupKey(AlarmMessage message) {
        String devicePart = message.getDeviceId() != null
                ? String.valueOf(message.getDeviceId()) : "unknown";
        String alarmType = message.getAlarmType() != null
                ? message.getAlarmType() : "unknown";
        String sourcePart = message.getSourceType() != null
                ? message.getSourceType().getCode() : "unknown";
        return devicePart + ":" + alarmType + ":" + sourcePart;
    }

    /**
     * 检查是否为重复告警（5 分钟窗口内相同 dedupKey 视为重复）
     */
    private boolean isDuplicate(String dedupKey) {
        Long lastTime = dedupCache.get(dedupKey);
        if (lastTime == null) {
            return false;
        }
        boolean duplicate = (System.currentTimeMillis() - lastTime) < DEDUP_WINDOW_MS;
        if (!duplicate) {
            // 过期条目自动清理
            dedupCache.remove(dedupKey);
        }
        return duplicate;
    }

    /**
     * 清理过期的去重缓存条目（可由定时任务调用，每 60 秒执行一次）
     */
    public void cleanExpiredDedupCache() {
        long now = System.currentTimeMillis();
        dedupCache.entrySet().removeIf(entry ->
                (now - entry.getValue()) >= DEDUP_WINDOW_MS);
    }

    // ==================== AlertRecordDO 构建 ====================

    /**
     * 将 AlarmMessage 转换为 AlertRecordDO
     */
    private AlertRecordDO buildAlertRecord(AlarmMessage message) {
        long alertTime = message.getAlarmTime() != null
                ? message.getAlarmTime() : System.currentTimeMillis();

        // 告警等级：根据告警类型映射为数据库数字编码
        // fire→1, fault→2, risk→3, offline→4, info→6
        String level = deriveLevelFromAlarmType(message.getAlarmType(), message.getAlarmLevel());

        // 名称：优先使用 buildAlarmName() 方法
        String name = message.buildAlarmName();

        // 详情：使用 buildDetails() 方法，拼接来源、位置等信息
        String details = message.buildDetails();

        AlertRecordDO record = AlertRecordDO.builder()
                .alertTime(alertTime)
                .level(level)
                .name(name)
                .details(details)
                .alarmType(0)
                .readFlg(false)
                .build();

        if (message.getDeviceId() != null) {
            record.setDeviceId(message.getDeviceId());
        }
        if (StrUtil.isNotBlank(message.getProductKey())) {
            record.setProductKey(message.getProductKey());
        }

        return record;
    }

    /**
     * 根据告警类型和等级派生数据库存储的数字等级编码
     * <p>
     * 优先按 alarmType 映射（与 FireAlarmOrderServiceImpl 保持一致）：
     * fire→1, fault→2, risk→3, offline→4, false_alarm→5, info→6
     * 若 alarmType 无法匹配，则回退到按 alarmLevel 映射：
     * critical→1, major→2, warning→3, info→6（info 不能落 4，否则设备屏蔽等 info 告警会被当成离线）
     */
    private String deriveLevelFromAlarmType(String alarmType, String alarmLevel) {
        // 优先按 alarmType 映射
        if (StrUtil.isNotBlank(alarmType)) {
            switch (alarmType) {
                case "fire":        return "1";
                case "fault":       return "2";
                case "risk":        return "3";
                case "offline":     return "4";
                case "false_alarm": return "5";
                case "info":        return "6";
            }
        }
        // 回退：按 alarmLevel 映射
        if (StrUtil.isNotBlank(alarmLevel)) {
            switch (alarmLevel.toLowerCase()) {
                case "critical": return "1";
                case "major":    return "2";
                case "warning":  return "3";
                case "info":     return "6";
            }
        }
        return "4"; // 默认
    }

    // ==================== WebSocket 推送 ====================

    /**
     * 落库成功后实时推送（alarm-push → monitoring-center，firewatch-alert-new → duty-screen）
     *
     * @param message  告警消息
     * @param recordId 入库后的告警记录 ID
     */
    private void pushToWebSocket(AlarmMessage message, Long recordId) {
        Long tenantId = TenantContextHolder.getTenantId();

        // ① alarm-push：monitoring-center 契约（info 不推送，保持原行为）
        if (!"info".equalsIgnoreCase(message.getAlarmLevel())) {
            try {
                AlarmWebSocketMessage wsMsg = AlarmWebSocketMessage.from(message, recordId);
                // 填充单位名称（租户名称），供前端大屏语音播报使用
                if (tenantId != null) {
                    try {
                        wsMsg.setTenantName(tenantApi.getTenantName(tenantId));
                    } catch (Exception e) {
                        log.warn("[告警推送] 查询租户名称失败, tenantId={}", tenantId, e);
                    }
                }
                webSocketSenderApi.sendObject(
                        UserTypeEnum.ADMIN.getValue(),
                        "alarm-push",
                        wsMsg);
                log.info("[告警推送] alarm-push 推送成功, recordId={}, alarmType={}, alarmLevel={}",
                        recordId, message.getAlarmType(), message.getAlarmLevel());
            } catch (Exception e) {
                // 推送失败不影响告警落库
                log.error("[告警推送] alarm-push 推送异常, recordId={}, deviceId={}",
                        recordId, message.getDeviceId(), e);
            }
        }

        // ② firewatch-alert-new：duty-screen 契约（过滤与双发由门面处理）
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("alertId", recordId);
            payload.put("alertName", message.buildAlarmName());
            payload.put("level", mapAlarmLevelToNumeric(message.getAlarmLevel()));
            payload.put("alarmType", 0);
            payload.put("deviceId", message.getDeviceId());
            payload.put("devSno", StrUtil.nullToEmpty(message.getDeviceName()));
            payload.put("alertTime", message.getAlarmTime() != null ? message.getAlarmTime() : System.currentTimeMillis());
            payload.put("alertImage", "");
            payload.put("deviceName", StrUtil.nullToEmpty(message.getDeviceName()));
            payload.put("tenantName", "");
            payload.put("subDeviceName", message.getComponentInfo() != null
                    ? StrUtil.nullToEmpty(message.getComponentInfo().getComponentDesc()) : "");
            payload.put("alertDetails", StrUtil.nullToEmpty(message.buildDetails()));
            alarmRealtimeNotifyService.notifyAlarmSaved(payload, tenantId);
        } catch (Exception e) {
            log.error("[告警推送] firewatch-alert-new 推送异常, recordId={}, deviceId={}",
                    recordId, message.getDeviceId(), e);
        }
    }

    /**
     * 告警等级英文名 → 数字编码（duty-screen 契约：1=紧急 2=较重 3=一般 6=信息）
     */
    private static String mapAlarmLevelToNumeric(String alarmLevel) {
        if (StrUtil.isBlank(alarmLevel)) {
            return "3";
        }
        switch (alarmLevel.toLowerCase()) {
            case "critical": return "1";
            case "major":    return "2";
            case "warning":  return "3";
            case "info":     return "6";
            default:         return "3";
        }
    }
}
