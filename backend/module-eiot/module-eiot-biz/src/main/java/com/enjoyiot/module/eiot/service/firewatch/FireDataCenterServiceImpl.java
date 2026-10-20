package com.enjoyiot.module.eiot.service.firewatch;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.DataCenterRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyScheduleDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireRepairOrderDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDeviceMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDutyScheduleMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireHandoverRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireHazardMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePatrolTaskMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireRepairOrderMapper;
import com.enjoyiot.module.eiot.service.UserTenantResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 数据中心 Service 实现类
 *
 * <p>统计口径说明（与月度报告 {@link FireMonthlyReportServiceImpl} 的口径对照见该类头）：
 * <ul>
 *   <li>时间窗口：近 N 天 = 含今天的 N 个自然日，窗口 [今天-(N-1) 00:00, 明天 00:00)；月报为自然月</li>
 *   <li>告警类型：双读口径 COALESCE(alarm_kind, level)，1=火警 2=故障 3=隐患 4=离线 5=误报 6=信息</li>
 *   <li>档位：tier 1-4 = Ⅰ-Ⅳ档（仅一级/火警事件有值，旧数据无档位）</li>
 *   <li>值班：本月排班口径；签到率/准时率分母为"班次已结束"的排班（completed+early_leave+absent）</li>
 *   <li>超期：截至查询时刻仍未完成且期限已过（当前值时点指标，非统计期内曾超期）</li>
 *   <li>租户：统一经 {@link #inScope} 使用 TenantUtils.execute + 租户拦截器——系统租户账号未下钻时看全部，
 *       企业账号恒看本租户；物业下钻按"物业→所属租户"过滤</li>
 *   <li>已知模型债：eiot_alert_record / eiot_device_info 无 property_id，告警与设备的物业筛选只能租户级近似
 *       （同租户多物业时含该租户全部物业，页面已标注"按联网单位聚合"）</li>
 * </ul>
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireDataCenterServiceImpl implements FireDataCenterService {

    /** 双读报警类型编码 → 名称（1=火警 2=故障 3=隐患 4=离线 5=误报 6=信息） */
    private static final Map<String, String> ALARM_KIND_NAME = Map.of(
            "1", "火警", "2", "故障", "3", "隐患", "4", "离线", "5", "误报", "6", "信息");

    /** 确认档位编码 → 名称（1-4 = Ⅰ-Ⅳ档） */
    private static final Map<String, String> TIER_NAME = Map.of(
            "1", "Ⅰ档", "2", "Ⅱ档", "3", "Ⅲ档", "4", "Ⅳ档");

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 物业不存在/越权时的隔离租户哨兵：把查询上下文限定到不存在的租户 → 恒返回空集，
     * 避免"物业查不到"被误当作"不限定租户"而泄露全量数据
     */
    private static final long ISOLATED_TENANT_ID = -1L;

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private FireDeviceMapper fireDeviceMapper;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Resource
    private FireDutyScheduleMapper fireDutyScheduleMapper;

    @Resource
    private FireHandoverRecordMapper fireHandoverRecordMapper;

    @Resource
    private FirePatrolTaskMapper firePatrolTaskMapper;

    @Resource
    private FireRepairOrderMapper fireRepairOrderMapper;

    @Resource
    private FireHazardMapper fireHazardMapper;

    @Resource
    private UserTenantResolver userTenantResolver;

    // ==================== 告警分析 ====================

    @Override
    public List<DataCenterRespVO.AlarmTrendItem> getAlarmTrend(Integer days, Long propertyId) {
        int n = normalizeDays(days);
        long[] win = resolveWindow(n);
        return inScope(propertyId, () -> {
            Map<String, Map<String, Object>> dayMap = new HashMap<>();
            for (Map<String, Object> row : alertRecordMapper.countByDayInRange(win[0], win[1])) {
                dayMap.put(str(row.get("alarm_date")), row);
            }
            List<DataCenterRespVO.AlarmTrendItem> result = new ArrayList<>(n);
            for (int i = n - 1; i >= 0; i--) {
                String dateStr = LocalDate.now().minusDays(i).format(DATE_FMT);
                DataCenterRespVO.AlarmTrendItem item = new DataCenterRespVO.AlarmTrendItem();
                item.setDate(dateStr);
                Map<String, Object> row = dayMap.get(dateStr);
                item.setAlarmCount(row != null ? toInt(row.get("alarm_count")) : 0);
                item.setHandleCount(row != null ? toInt(row.get("handle_count")) : 0);
                result.add(item);
            }
            return result;
        });
    }

    @Override
    public List<DataCenterRespVO.AlarmTypeItem> getAlarmTypeDistribution(Integer days, Long propertyId) {
        long[] win = resolveWindow(normalizeDays(days));
        return inScope(propertyId, () -> {
            List<DataCenterRespVO.AlarmTypeItem> result = new ArrayList<>();
            for (Map<String, Object> row : alertRecordMapper.countByAlarmKindInRange(win[0], win[1])) {
                String kind = str(row.get("alarmKind"));
                DataCenterRespVO.AlarmTypeItem item = new DataCenterRespVO.AlarmTypeItem();
                item.setTypeName(ALARM_KIND_NAME.getOrDefault(kind, "未知"));
                item.setCount(toInt(row.get("count")));
                result.add(item);
            }
            return result;
        });
    }

    @Override
    public List<DataCenterRespVO.PropertyAlarmRankItem> getPropertyAlarmRank(Integer days) {
        long[] win = resolveWindow(normalizeDays(days));
        return inScope(null, () -> {
            Map<Long, String> names = loadTenantPropertyNames();
            List<DataCenterRespVO.PropertyAlarmRankItem> result = new ArrayList<>();
            for (Map<String, Object> row : alertRecordMapper.countByTenantInRange(win[0], win[1])) {
                Long tenantId = row.get("tenant_id") != null ? Long.parseLong(row.get("tenant_id").toString()) : null;
                DataCenterRespVO.PropertyAlarmRankItem item = new DataCenterRespVO.PropertyAlarmRankItem();
                item.setPropertyName(names.getOrDefault(tenantId, "未知单位"));
                item.setAlarmCount(toInt(row.get("count")));
                result.add(item);
            }
            return result;
        });
    }

    @Override
    public List<DataCenterRespVO.HourlyDistributionItem> getAlarmHourlyDistribution(Integer days, Long propertyId) {
        long[] win = resolveWindow(normalizeDays(days));
        return inScope(propertyId, () -> {
            Map<Integer, Integer> hourMap = new HashMap<>();
            for (Map<String, Object> row : alertRecordMapper.countByHourInRange(win[0], win[1])) {
                hourMap.put(toInt(row.get("alarm_hour")), toInt(row.get("count")));
            }
            List<DataCenterRespVO.HourlyDistributionItem> result = new ArrayList<>(24);
            for (int h = 0; h < 24; h++) {
                DataCenterRespVO.HourlyDistributionItem item = new DataCenterRespVO.HourlyDistributionItem();
                item.setHour(h);
                item.setCount(hourMap.getOrDefault(h, 0));
                result.add(item);
            }
            return result;
        });
    }

    @Override
    public List<DataCenterRespVO.AlarmLevelItem> getAlarmLevelDistribution(Integer days, Long propertyId) {
        long[] win = resolveWindow(normalizeDays(days));
        return inScope(propertyId, () -> {
            List<DataCenterRespVO.AlarmLevelItem> result = new ArrayList<>();
            for (Map<String, Object> row : alertRecordMapper.countByTierInRange(win[0], win[1])) {
                String tier = str(row.get("tier"));
                DataCenterRespVO.AlarmLevelItem item = new DataCenterRespVO.AlarmLevelItem();
                item.setLevel(tier);
                item.setLevelName(TIER_NAME.getOrDefault(tier, tier + "档"));
                item.setCount(toInt(row.get("count")));
                result.add(item);
            }
            return result;
        });
    }

    // ==================== 设备分析 ====================

    @Override
    public DataCenterRespVO.DeviceStatusItem getDeviceStatus(Integer days, Long propertyId) {
        long[] win = resolveWindow(normalizeDays(days));
        return inScope(propertyId, () -> {
            // 快照组装：算法同 FireDashboardServiceImpl#buildDeviceStatus（未知状态归未激活，保证各项之和=total）
            int total = 0, online = 0, offline = 0, inactive = 0, fault = 0;
            for (Map<String, Object> row : fireDeviceMapper.countByStatus()) {
                int state = row.get("state") != null ? Integer.parseInt(row.get("state").toString()) : -1;
                int count = toInt(row.get("count"));
                total += count;
                switch (state) {
                    case 1:
                        online = count;
                        break;   // 在线
                    case 0:
                        offline = count;
                        break;  // 离线
                    case 3:
                        inactive = count;
                        break; // 未激活
                    case 4:
                        fault = count;
                        break;    // 禁用/故障
                    default:
                        break;     // 未知状态已计入 total，归入未激活
                }
            }
            int unaccounted = total - online - offline - inactive - fault;
            if (unaccounted > 0) {
                inactive += unaccounted;
            }

            Integer alarmDevices = alertRecordMapper.countDistinctAlarmDevicesInRange(win[0], win[1]);
            int alarmDeviceCount = alarmDevices != null ? alarmDevices : 0;

            DataCenterRespVO.DeviceStatusItem item = new DataCenterRespVO.DeviceStatusItem();
            item.setTotal(total);
            item.setNormal(Math.max(0, online - alarmDeviceCount));
            item.setAlarmDeviceCountInRange(alarmDeviceCount);
            item.setFault(fault);
            item.setOffline(offline);
            item.setInactive(inactive);
            return item;
        });
    }

    @Override
    public List<DataCenterRespVO.DeviceCategoryItem> getDeviceCategoryDistribution(Long propertyId) {
        return inScope(propertyId, () -> {
            List<DataCenterRespVO.DeviceCategoryItem> result = new ArrayList<>();
            for (Map<String, Object> row : fireDeviceMapper.countByCategory()) {
                int count = toInt(row.get("device_count"));
                if (count <= 0) {
                    continue; // 0 台设备的分类不进图表
                }
                DataCenterRespVO.DeviceCategoryItem item = new DataCenterRespVO.DeviceCategoryItem();
                item.setCategoryName(row.get("category_name") != null ? row.get("category_name").toString() : "未分类");
                item.setCount(count);
                result.add(item);
            }
            return result;
        });
    }

    @Override
    public List<DataCenterRespVO.PropertyDeviceItem> getPropertyDeviceDistribution() {
        return inScope(null, () -> {
            Map<Long, String> names = loadTenantPropertyNames();
            List<DataCenterRespVO.PropertyDeviceItem> result = new ArrayList<>();
            for (Map<String, Object> row : fireDeviceMapper.rankDeviceByTenant(5)) {
                Long tenantId = row.get("tenant_id") != null ? Long.parseLong(row.get("tenant_id").toString()) : null;
                DataCenterRespVO.PropertyDeviceItem item = new DataCenterRespVO.PropertyDeviceItem();
                item.setPropertyName(names.getOrDefault(tenantId, "未知单位"));
                item.setDeviceCount(toInt(row.get("count")));
                result.add(item);
            }
            return result;
        });
    }

    // ==================== 值班统计（固定本月口径） ====================

    @Override
    public DataCenterRespVO.DutyStatsItem getDutyStats(Long propertyId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        return inScope(propertyId, () -> {
            // 排班状态分布（DERIVED_STATUS：班次已结束的 scheduled→absent、on_duty→early_leave）
            int total = 0, completed = 0, earlyLeave = 0, absent = 0;
            for (Map<String, Object> row : fireDutyScheduleMapper.countByStatus(null, propertyId, monthStart, monthEnd)) {
                String status = str(row.get("status"));
                int count = toInt(row.get("count"));
                total += count;
                switch (status) {
                    case "completed":
                        completed = count;
                        break;
                    case "early_leave":
                        earlyLeave = count;
                        break;
                    case "absent":
                        absent = count;
                        break;
                    default:
                        break; // scheduled / on_duty 未结束，不计入率的分母
                }
            }
            int settled = completed + earlyLeave + absent;
            int checkedIn = completed + earlyLeave;
            Integer onTime = fireDutyScheduleMapper.countOnTimeCheckIn(propertyId, monthStart, monthEnd);
            int onTimeCount = onTime != null ? onTime : 0;

            // 交接班来自 fire_handover_record（勿用 fire_duty_record：该表在数据库并不存在，存量缺陷）；
            // 该表无 property_id，物业下钻时按租户级（联网单位）统计
            Integer handover = fireHandoverRecordMapper.countInRange(monthStart.atStartOfDay(), monthEnd.plusDays(1).atStartOfDay());

            DataCenterRespVO.DutyStatsItem item = new DataCenterRespVO.DutyStatsItem();
            item.setMonthDutyCount(total);
            item.setCheckInRate(settled > 0 ? round1(checkedIn * 100.0 / settled) : 0.0);
            item.setOnTimeRate(settled > 0 ? round1(onTimeCount * 100.0 / settled) : 0.0);
            item.setHandoverCount(handover != null ? handover : 0);
            return item;
        });
    }

    @Override
    public List<DataCenterRespVO.UserDutyRankItem> getUserDutyRank(Integer year, Integer month, Long propertyId) {
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        LocalDate start = LocalDate.of(y, m, 1);
        LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());
        return inScope(propertyId, () -> {
            // 月排班量可控，取排班列表后 Java 聚合每人"排班天数"（去重日期）
            Map<Long, Set<LocalDate>> userDays = new HashMap<>();
            Map<Long, String> userNameMap = new HashMap<>();
            for (FireDutyScheduleDO schedule : fireDutyScheduleMapper.selectByDateRange(null, start, end)) {
                if (schedule.getDutyUserId() == null || schedule.getScheduleDate() == null) {
                    continue;
                }
                if (propertyId != null && !propertyId.equals(schedule.getPropertyId())) {
                    continue;
                }
                userDays.computeIfAbsent(schedule.getDutyUserId(), k -> new HashSet<>()).add(schedule.getScheduleDate());
                if (schedule.getDutyUserName() != null) {
                    userNameMap.putIfAbsent(schedule.getDutyUserId(), schedule.getDutyUserName());
                }
            }
            return userDays.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                    .limit(10)
                    .map(e -> {
                        DataCenterRespVO.UserDutyRankItem item = new DataCenterRespVO.UserDutyRankItem();
                        item.setUserName(userNameMap.getOrDefault(e.getKey(), "未知人员"));
                        item.setDutyDays(e.getValue().size());
                        return item;
                    })
                    .collect(java.util.stream.Collectors.toList());
        });
    }

    // ==================== 巡检统计 ====================

    @Override
    public DataCenterRespVO.PatrolStatsItem getPatrolStats(Integer days, Long propertyId) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(normalizeDays(days) - 1L);
        return inScope(propertyId, () -> {
            int total = 0, completed = 0, inProgress = 0, overdue = 0;
            for (Map<String, Object> row : firePatrolTaskMapper.countByStatus(null, propertyId, start, today)) {
                String status = str(row.get("status"));
                int count = toInt(row.get("count"));
                total += count;
                switch (status) {
                    case "completed":
                        completed = count;
                        break;
                    case "in_progress":
                        inProgress = count;
                        break;
                    case "overdue":
                        overdue = count;
                        break;
                    default:
                        break; // pending / cancelled 只计入总数
                }
            }
            DataCenterRespVO.PatrolStatsItem item = new DataCenterRespVO.PatrolStatsItem();
            item.setTotal(total);
            item.setCompleted(completed);
            item.setInProgress(inProgress);
            item.setOverdue(overdue);
            item.setCompleteRate(total > 0 ? round1(completed * 100.0 / total) : 0.0);
            return item;
        });
    }

    @Override
    public List<DataCenterRespVO.PatrolTrendItem> getPatrolTrend(Integer days, Long propertyId) {
        int n = normalizeDays(days);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(n - 1L);
        return inScope(propertyId, () -> {
            Map<String, Map<String, Object>> dayMap = new HashMap<>();
            for (Map<String, Object> row : firePatrolTaskMapper.countTrendByDay(propertyId, start, today)) {
                dayMap.put(str(row.get("plan_date")), row);
            }
            List<DataCenterRespVO.PatrolTrendItem> result = new ArrayList<>(n);
            for (int i = n - 1; i >= 0; i--) {
                String dateStr = today.minusDays(i).format(DATE_FMT);
                DataCenterRespVO.PatrolTrendItem item = new DataCenterRespVO.PatrolTrendItem();
                item.setDate(dateStr);
                Map<String, Object> row = dayMap.get(dateStr);
                item.setCreatedCount(row != null ? toInt(row.get("created_count")) : 0);
                item.setCompletedCount(row != null ? toInt(row.get("completed_count")) : 0);
                result.add(item);
            }
            return result;
        });
    }

    // ==================== 工单统计 ====================

    @Override
    public DataCenterRespVO.RepairStatsItem getRepairStats(Long propertyId) {
        return inScope(propertyId, () -> {
            // 当前状态快照（query wrapper 生成 SQL，租户拦截器自动限定范围）
            int total = 0, pending = 0, inProgress = 0, completed = 0;
            List<Map<String, Object>> rows = fireRepairOrderMapper.selectMaps(
                    new QueryWrapper<FireRepairOrderDO>()
                            .select("order_status", "COUNT(*) AS count")
                            .eq(propertyId != null, "property_id", propertyId)
                            .groupBy("order_status"));
            for (Map<String, Object> row : rows) {
                String status = str(row.get("order_status"));
                int count = toInt(row.get("count"));
                total += count;
                switch (status) {
                    case "pending":
                    case "assigned":
                        pending += count;
                        break;
                    case "in_progress":
                        inProgress += count;
                        break;
                    case "completed":
                        completed += count;
                        break;
                    default:
                        break; // closed 等其他状态只计入总数
                }
            }
            // 超期=排查截止时间已过且状态未完成（当前值时点）
            Long overdue = fireRepairOrderMapper.selectCount(new LambdaQueryWrapperX<FireRepairOrderDO>()
                    .eqIfPresent(FireRepairOrderDO::getPropertyId, propertyId)
                    .in(FireRepairOrderDO::getOrderStatus, "pending", "assigned", "in_progress")
                    .isNotNull(FireRepairOrderDO::getDeadline)
                    .lt(FireRepairOrderDO::getDeadline, LocalDateTime.now()));

            DataCenterRespVO.RepairStatsItem item = new DataCenterRespVO.RepairStatsItem();
            item.setTotal(total);
            item.setPending(pending);
            item.setInProgress(inProgress);
            item.setCompleted(completed);
            item.setOverdue(overdue != null ? overdue.intValue() : 0);
            return item;
        });
    }

    @Override
    public DataCenterRespVO.HazardStatsItem getHazardStats(Integer days, Long propertyId) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(normalizeDays(days) - 1L);
        return inScope(propertyId, () -> {
            int total = 0, pendingBucket = 0, rectified = 0, closed = 0, cancelled = 0;
            for (Map<String, Object> row : fireHazardMapper.countByStatus(null, propertyId, start, today)) {
                String status = str(row.get("status"));
                int count = toInt(row.get("count"));
                total += count;
                switch (status) {
                    case "pending":
                    case "rectifying":
                        pendingBucket += count;
                        break;
                    case "reviewing":
                        rectified += count;
                        break;
                    case "closed":
                        closed = count;
                        break;
                    case "cancelled":
                        cancelled = count;
                        break;
                    default:
                        break;
                }
            }
            Integer overdue = fireHazardMapper.countOverdue(null, start, today);
            int settledTotal = total - cancelled;

            DataCenterRespVO.HazardStatsItem item = new DataCenterRespVO.HazardStatsItem();
            item.setTotal(total);
            item.setPending(pendingBucket);
            item.setRectified(rectified);
            item.setClosed(closed);
            item.setHazardOverdue(overdue != null ? overdue : 0);
            item.setRectificationRate(settledTotal > 0 ? round1((rectified + closed) * 100.0 / settledTotal) : 0.0);
            return item;
        });
    }

    @Override
    public List<DataCenterRespVO.WorkOrderTrendItem> getWorkOrderTrend(Integer days, Long propertyId) {
        int n = normalizeDays(days);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(n - 1L);
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = today.plusDays(1).atStartOfDay();
        return inScope(propertyId, () -> {
            // 处置事项口径：创建=维修新建+隐患新增，完成=维修完工+隐患闭环（按事件发生日归集）
            Map<String, int[]> dayMap = new HashMap<>(); // date -> [created, completed]
            for (Map<String, Object> row : fireRepairOrderMapper.countTrendByDay(propertyId, startDt, endDt)) {
                int[] pair = dayMap.computeIfAbsent(str(row.get("date")), k -> new int[2]);
                pair[0] += toInt(row.get("created_count"));
                pair[1] += toInt(row.get("completed_count"));
            }
            for (Map<String, Object> row : fireHazardMapper.countTrend(null, propertyId, start, today)) {
                int[] pair = dayMap.computeIfAbsent(str(row.get("date")), k -> new int[2]);
                pair[0] += toInt(row.get("newCount"));
                pair[1] += toInt(row.get("closedCount"));
            }
            List<DataCenterRespVO.WorkOrderTrendItem> result = new ArrayList<>(n);
            for (int i = n - 1; i >= 0; i--) {
                String dateStr = today.minusDays(i).format(DATE_FMT);
                DataCenterRespVO.WorkOrderTrendItem item = new DataCenterRespVO.WorkOrderTrendItem();
                item.setDate(dateStr);
                int[] pair = dayMap.get(dateStr);
                item.setCreatedCount(pair != null ? pair[0] : 0);
                item.setCompletedCount(pair != null ? pair[1] : 0);
                result.add(item);
            }
            return result;
        });
    }

    // ==================== 内部辅助方法 ====================

    /** days 归一化：null/非正数按 30，上限 366 */
    private static int normalizeDays(Integer days) {
        if (days == null || days <= 0) {
            return 30;
        }
        return Math.min(days, 366);
    }

    /** 近 N 天时间窗口 [startMs, endMs)：含今天的 N 个自然日（今天 00:00 ~ 明天 00:00） */
    private static long[] resolveWindow(int days) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now();
        long startMs = today.minusDays(days - 1L).atStartOfDay(zone).toInstant().toEpochMilli();
        long endMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        return new long[]{startMs, endMs};
    }

    /**
     * 解析查询租户上下文：
     * - propertyId 非空：取该物业所属租户（查不到=不存在或越权，返回隔离哨兵 → 查询恒空）
     * - propertyId 为空：登录用户账号所属租户（系统租户账号返回 1，由拦截器规则放行全量）
     */
    private Long resolveScopeTenantId(Long propertyId) {
        if (propertyId == null) {
            return userTenantResolver.resolveLoginUserTenantId();
        }
        FirePropertyDO property = firePropertyMapper.selectById(propertyId);
        return property != null && property.getTenantId() != null ? property.getTenantId() : ISOLATED_TENANT_ID;
    }

    /** 在解析出的租户上下文内执行查询（null=不限定，由租户拦截器规则决定） */
    private <T> T inScope(Long propertyId, Supplier<T> supplier) {
        Long scopeTenantId = resolveScopeTenantId(propertyId);
        return scopeTenantId != null ? TenantUtils.execute(scopeTenantId, supplier::get) : supplier.get();
    }

    /** 租户 id → 物业名称映射（同租户多物业用 / 连接；查询本身在租户上下文内执行） */
    private Map<Long, String> loadTenantPropertyNames() {
        Map<Long, String> map = new HashMap<>();
        for (FirePropertyDO p : firePropertyMapper.selectList(new LambdaQueryWrapperX<>())) {
            if (p.getTenantId() == null) {
                continue;
            }
            map.merge(p.getTenantId(), p.getName() != null ? p.getName() : "", (a, b) -> a + "/" + b);
        }
        return map;
    }

    private static int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(Object value) {
        return value != null ? value.toString() : "";
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
