package com.enjoyiot.module.eiot.service.alarm;

import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.common.enums.UserTypeEnum;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.infra.api.websocket.WebSocketSenderApi;
import com.enjoyiot.module.system.api.tenant.TenantApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final ConcurrentHashMap<String, Long> dedupCache = new ConcurrentHashMap<>();

    // ==================== 公共方法 ====================

    /**
     * 批量保存告警消息（每条独立事务，单条失败不影响其他）
     * <p>
     *
     * @param messages 告警消息列表
     * @return 成功入库的告警记录ID列表
     */
    public List<Long> saveAlarms(List<AlarmMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> savedIds = new ArrayList<>(messages.size());
        // 遍历消息列表，逐条保存
        for (AlarmMessage msg : messages) {
            try {
                //保存
                AlertRecordDO record = saveAlarm(msg);
                //校验不为空
                if (record != null) {
                    log.info("[告警存储] 保存成功, recordId={}, deviceId={}, alarmType={}", record.getId(), msg.getDeviceId(), msg.getAlarmType());
                    savedIds.add(record.getId());
                    // 落库成功后推送（事务外，避免同步 IO 占用连接）
                    pushToWebSocket(msg, record);
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
     * 保存单条告警消息
     *
     * @param message 告警消息
     * @return 入库成功返回记录（含生成ID），去重/跳过返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public AlertRecordDO saveAlarm(AlarmMessage message) {
        if (message == null) {
            return null;
        }

        // ── 1. 去重检查 ──
        String dedupKey = buildDedupKey(message);

        long now = System.currentTimeMillis();
        Long last = dedupCache.putIfAbsent(dedupKey, now);

        if (last != null && now - last < DEDUP_WINDOW_MS){
            return null;
        }else {
            dedupCache.replace(dedupKey, last, now);
        }

        // ── 2. 解析租户（详情拼租户名与落库设租户ID共用） ──
        Long tenantId = TenantContextHolder.getTenantId();
        String tenantName = resolveTenantName(tenantId);

        // ── 3. 构建 AlertRecordDO（详情口径：具体事件描述-租户名称） ──
        AlertRecordDO record = buildAlertRecord(message, tenantName);
        if (tenantId != null) {
            record.setTenantId(tenantId);
        }

        // ── 4. 落库 ──
        alertRecordMapper.insert(record);
        log.info("[告警存储] 创建告警记录成功, recordId={}, deviceId={}, alarmType={}, alarmLevel={}, tenantId={}",
                record.getId(), message.getDeviceId(), message.getAlarmType(),
                message.getAlarmLevel(), record.getTenantId());



        return record;
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
     * 查询租户名称（告警详情/推送复用；查询失败不阻断告警落库）
     */
    private String resolveTenantName(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        try {
            return tenantApi.getTenantName(tenantId);
        } catch (Exception e) {
            log.warn("[告警存储] 查询租户名称失败, tenantId={}", tenantId, e);
            return null;
        }
    }

    // ==================== AlertRecordDO 构建 ====================

    /**
     * 将 AlarmMessage 转换为 AlertRecordDO
     * <p>
     * 写侧三写（定稿 4.1 W1）：接入层归一报警类型（alarm_kind），定级层派生等级（level），
     * 一级事件按判定链写档位（tier）
     */
    private AlertRecordDO buildAlertRecord(AlarmMessage message, String tenantName) {
        long alertTime = message.getAlarmTime() != null
                ? message.getAlarmTime() : System.currentTimeMillis();

        // 接入层归一：类型字符串 → 报警类型；类型无法识别时回退告警级别（保持旧回退口径）
        Integer alarmKind = AlarmSemantics.alarmTypeToKind(message.getAlarmType());
        if (alarmKind == null) {
            alarmKind = AlarmSemantics.alarmLevelToKind(message.getAlarmLevel());
        }

        // 定级层：等级由类型派生（定稿 1.2）；类型无法判定时保持旧默认"4"（alarm_kind 为 NULL，走双读回退）
        String level = alarmKind != null ? AlarmSemantics.deriveLevel(alarmKind) : "4";

        // 名称：优先使用 buildAlarmName() 方法
        String name = message.buildAlarmName();

        // 详情：具体事件描述-租户名称（租户名缺失时回退「描述 | 来源」）
        String details = message.buildDetails(tenantName);

        AlertRecordDO record = AlertRecordDO.builder()
                .alertTime(alertTime)
                .level(level)
                .alarmKind(alarmKind)
                .tier(resolveTier(message.getAlarmType(), alarmKind))
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
     * 档位判定（定稿 1.5 判定链）
     * <p>
     * 手报/消火栓=人工直接触发→Ⅰ档；复合信号→Ⅱ档（防御性映射，当前 AEP 策略未产出）；
     * 其余一级事件暂无部件类型 / 视频覆盖 / 多信号判定原料，兜底Ⅳ档（到场核查，向安全侧）；
     * 非一级不设档位（1.6：tier 有值 ⇔ level=1）
     */
    private Integer resolveTier(String alarmType, Integer alarmKind) {
        if (alarmKind == null || alarmKind != AlarmSemantics.KIND_FIRE) {
            return null;
        }
        if ("manual".equals(alarmType)) {
            return AlarmSemantics.TIER_I;
        }
        if ("composite".equals(alarmType)) {
            return AlarmSemantics.TIER_II;
        }
        return AlarmSemantics.TIER_IV;
    }

    // ==================== WebSocket 推送 ====================

    /**
     * 落库成功后实时推送（alarm-push → monitoring-center，firewatch-alert-new → duty-screen）
     *
     * @param message 告警消息
     * @param record  入库后的告警记录（含四维字段）
     */
    private void pushToWebSocket(AlarmMessage message, AlertRecordDO record) {
        Long tenantId = TenantContextHolder.getTenantId();
        Long recordId = record.getId();
        // 单位名称（租户名称）：语音播报与详情内容共用，只查一次
        String tenantName = resolveTenantName(tenantId);

        // ① alarm-push：monitoring-center 契约（info 不推送，保持原行为）
        if (!"info".equalsIgnoreCase(message.getAlarmLevel())) {
            try {
                AlarmWebSocketMessage wsMsg = AlarmWebSocketMessage.from(message, record);
                // 填充单位名称（租户名称），供前端大屏语音播报使用
                if (tenantName != null) {
                    wsMsg.setTenantName(tenantName);
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
            // 级别出口对齐四维（R8）：取定级层产出的等级（1/2/3），替代旧的"由输入级别转数字"
            payload.put("level", record.getLevel());
            payload.put("alarmType", 0);
            payload.put("alarmKind", record.getAlarmKind());
            payload.put("tier", record.getTier());
            payload.put("deviceId", message.getDeviceId());
            payload.put("devSno", StrUtil.nullToEmpty(message.getDeviceName()));
            payload.put("alertTime", message.getAlarmTime() != null ? message.getAlarmTime() : System.currentTimeMillis());
            payload.put("alertImage", "");
            payload.put("deviceName", StrUtil.nullToEmpty(message.getDeviceName()));
            payload.put("tenantName", "");
            payload.put("subDeviceName", message.getComponentInfo() != null
                    ? StrUtil.nullToEmpty(message.getComponentInfo().getComponentDesc()) : "");
            payload.put("alertDetails", StrUtil.nullToEmpty(message.buildDetails(tenantName)));
            alarmRealtimeNotifyService.notifyAlarmSaved(payload, tenantId);
        } catch (Exception e) {
            log.error("[告警推送] firewatch-alert-new 推送异常, recordId={}, deviceId={}",
                    recordId, message.getDeviceId(), e);
        }
    }

}
