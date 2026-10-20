package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.security.core.service.SecurityFrameworkService;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyScheduleDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePatrolTaskDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDeviceMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDutyScheduleMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePatrolTaskMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import com.enjoyiot.module.system.api.tenant.TenantApi;
import com.enjoyiot.module.eiot.service.UserTenantResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.module.system.enums.permission.RoleCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 * 大屏 Service 实现类
 * <p>
 * 数据源：eiot_alert_record（IoT 告警记录表）
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireDashboardServiceImpl implements FireDashboardService {

    @Resource
    private AlertRecordMapper alertRecordMapper;
    @Resource
    private FireDeviceMapper fireDeviceMapper;
    @Resource
    private FirePropertyMapper firePropertyMapper;
    @Resource
    private EiotDeviceInfoMapper eiotDeviceInfoMapper;
    @Resource
    private FirePatrolTaskMapper firePatrolTaskMapper;
    @Resource
    private FireDutyScheduleMapper fireDutyScheduleMapper;
    @Resource
    private TenantApi tenantApi;

    @Resource
    private UserTenantResolver userTenantResolver;
    @Resource
    private SecurityFrameworkService securityFrameworkService;

    // ==================== 监控中心大屏（admin 看全部，企业只看自己租户）====================

    @Override
    public DashboardStatisticsRespVO getDashboardStatistics(Long tenantId) {
        // 仅 super_admin 指定了租户时按该租户查询，其余情况按登录用户所属租户
        return executeWithTenantAware(this::buildStatistics, tenantId);
    }

    // ==================== 值守端大屏（admin 看全部，企业只看自己租户）====================

    @Override
    public DashboardStatisticsRespVO getDutyScreenStatistics() {
        return executeWithTenantAware(this::buildStatistics, null);
    }

    // ==================== 以下方法保留兼容 ====================

    @Override
    public DeviceStatusStatisticsRespVO getDeviceStatusStatistics() {
        return executeWithTenantAware(this::buildDeviceStatus, null);
    }

    @Override
    public TodayAlarmStatisticsRespVO getTodayAlarmStatistics() {
        return executeWithTenantAware(this::buildTodayAlarm, null);
    }

    @Override
    public List<AlarmOrderRespVO> getRealtimeAlarms(int limit) {
        return executeWithTenantAware(() -> buildRealtimeAlarms(limit), null);
    }

    @Override
    public List<DashboardStatisticsRespVO.RiskDistributionVO> getRiskDistribution() {
        return executeWithTenantAware(this::buildRiskDistribution, null);
    }

    @Override
    public List<DashboardStatisticsRespVO.AlarmTrendVO> getAlarmTrend(int days) {
        return executeWithTenantAware(() -> buildAlarmTrend(days), null);
    }

    @Override
    public List<DashboardStatisticsRespVO.DeviceCategoryVO> getDeviceCategoryDistribution() {
        return executeWithTenantAware(this::buildDeviceCategory, null);
    }

    @Override
    public List<Map<String, Object>> getDeviceLocations() {
        // 使用 EiotDeviceInfoMapper（继承 BaseMapperX，租户拦截器自动生效）
        // admin：executeIgnore 绕过拦截器看全部；企业租户：拦截器自动加 WHERE tenant_id = ?
        List<EiotDeviceInfoDO> devices;
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        if (userTenantId != null) {
            devices = TenantUtils.execute(userTenantId, () ->
                    eiotDeviceInfoMapper.selectList(new LambdaQueryWrapper<EiotDeviceInfoDO>()
                            .isNotNull(EiotDeviceInfoDO::getLat)
                            .isNotNull(EiotDeviceInfoDO::getLon)
                            .ne(EiotDeviceInfoDO::getLat, 0)
                            .ne(EiotDeviceInfoDO::getLon, 0)));
        } else {
            devices = eiotDeviceInfoMapper.selectList(new LambdaQueryWrapper<EiotDeviceInfoDO>()
                    .isNotNull(EiotDeviceInfoDO::getLat)
                    .isNotNull(EiotDeviceInfoDO::getLon)
                    .ne(EiotDeviceInfoDO::getLat, 0)
                    .ne(EiotDeviceInfoDO::getLon, 0));
        }

        // 转换为前端需要的格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (EiotDeviceInfoDO d : devices) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("name", d.getName());
            map.put("dn", d.getDn());
            map.put("addr", d.getAddr());
            map.put("lat", d.getLat());
            map.put("lon", d.getLon());
            map.put("state", d.getState());
            map.put("product_key", d.getProductKey());
            map.put("tenant_id", d.getTenantId());
            result.add(map);
        }
        log.info("[大屏] 设备位置查询: tenantId={}, 结果数={}", TenantContextHolder.getTenantId(), result.size());
        return result;
    }

    @Override
    public List<Map<String, Object>> getDeviceList() {
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        if (userTenantId != null) {
            return TenantUtils.execute(userTenantId, () -> fireDeviceMapper.selectDeviceList());
        }
        return fireDeviceMapper.selectDeviceList();
    }

    // ==================== 核心构建方法 ====================

    private DashboardStatisticsRespVO buildStatistics() {
        DashboardStatisticsRespVO result = new DashboardStatisticsRespVO();
        result.setDeviceStatus(buildDeviceStatus());
        result.setTodayAlarm(buildTodayAlarm());
        result.setRealtimeAlarms(buildRealtimeAlarms(10));
        result.setRiskDistribution(buildRiskDistribution());
        result.setAlarmTrend(buildAlarmTrend(7));
        result.setDeviceCategoryDistribution(buildDeviceCategory());
        result.setAlarmTypeDistribution(buildAlarmTypeDistribution());
        Long loginTenantId = userTenantResolver.resolveLoginUserTenantId();
        result.setTenantId(loginTenantId);
        if (loginTenantId != null) {
            try {
                result.setTenantName(tenantApi.getTenantName(loginTenantId));
            } catch (Exception e) {
                log.warn("[大屏] 获取租户名称失败 tenantId={}", loginTenantId, e);
            }
        }

        // 安全天数
        Integer safeDays = alertRecordMapper.getSafeDays();
        result.setSafeDays(safeDays != null && safeDays >= 0 ? safeDays : 0);

        // 设备健康率
        DeviceStatusStatisticsRespVO ds = result.getDeviceStatus();
        if (ds != null && ds.getTotal() > 0) {
            result.setDeviceHealthRate(Math.round(ds.getNormal() * 10000.0 / ds.getTotal()) / 100.0);
        } else {
            result.setDeviceHealthRate(0.0);
        }

        // 隐患待整改
        TodayAlarmStatisticsRespVO ta = result.getTodayAlarm();
        result.setPendingIssues(ta != null ? ta.getPendingCount() : 0);

        // 联网单位总数
        Integer propertyCount = firePropertyMapper.countTotal();
        result.setPropertyCount(propertyCount != null ? propertyCount : 0);

        // 巡检任务摘要（try-catch 保护，避免影响主流程）
        try {
            result.setPatrolSummary(buildPatrolSummary());
        } catch (Exception e) {
            log.warn("[大屏] 获取巡检摘要失败: {}", e.getMessage());
        }

        // 当前值班列表
        try {
            result.setCurrentDutyList(buildCurrentDutyList());
        } catch (Exception e) {
            log.warn("[大屏] 获取当前值班失败: {}", e.getMessage());
        }

        // 物业列表
        try {
            result.setProperties(buildPropertyList());
        } catch (Exception e) {
            log.warn("[大屏] 获取物业列表失败: {}", e.getMessage());
        }

        log.info("[大屏] 统计结果: deviceStatus={}, todayAlarm={}, propertyCount={}, safeDays={}",
                result.getDeviceStatus(), result.getTodayAlarm(), propertyCount, safeDays);
        return result;
    }

    private DeviceStatusStatisticsRespVO buildDeviceStatus() {
        DeviceStatusStatisticsRespVO result = new DeviceStatusStatisticsRespVO();

        // 使用 GROUP BY 一次性获取所有状态分布，避免遗漏未知状态
        List<Map<String, Object>> statusCounts = fireDeviceMapper.countByStatus();
        int total = 0, online = 0, offline = 0, inactive = 0, fault = 0;
        for (Map<String, Object> row : statusCounts) {
            int state = row.get("state") != null ? Integer.parseInt(row.get("state").toString()) : -1;
            int count = row.get("count") != null ? Integer.parseInt(row.get("count").toString()) : 0;
            total += count;
            switch (state) {
                case 1:
                    online = count;
                    break;       // 在线
                case 0:
                    offline = count;
                    break;      // 离线
                case 3:
                    inactive = count;
                    break;     // 未激活
                case 4:
                    fault = count;
                    break;        // 禁用/故障
                default:
                    break;                       // 未知状态已计入total，归入未激活
            }
        }
        log.info("[大屏] 设备统计(raw-GROUP BY): total={}, online={}, offline={}, inactive={}, fault={}",
                total, online, offline, inactive, fault);

        // 告警设备数（eiot_alert_record 无 device_id，暂返回0）
        int alarm = 0;

        // 未激活 = 查到的state=3 + 总数中不在任何已知状态的设备（确保数字对得上）
        int unaccounted = total - online - offline - inactive - fault;
        if (unaccounted > 0) {
            inactive += unaccounted;
            log.info("[大屏] 有{}台设备状态不在已知值(0,1,3,4)中，归入未激活", unaccounted);
        }

        // 正常 = 在线 - 告警（在线中可能有告警的）
        int normal = Math.max(0, online - alarm);

        result.setTotal(total);
        result.setOnline(online);
        result.setNormal(normal);
        result.setAlarm(alarm);
        result.setFault(fault);
        result.setOffline(offline);
        result.setInactive(inactive);

        log.info("[大屏] 设备统计(result): total={}, online={}, normal={}, alarm={}, fault={}, offline={}, inactive={}",
                total, online, normal, alarm, fault, offline, inactive);

        if (total > 0) {
            result.setOnlineRate(Math.round(online * 10000.0 / total) / 100.0);
            result.setAlarmRate(Math.round(alarm * 10000.0 / total) / 100.0);
            result.setFaultRate(Math.round(fault * 10000.0 / total) / 100.0);
        } else {
            result.setOnlineRate(0.0);
            result.setAlarmRate(0.0);
            result.setFaultRate(0.0);
        }

        return result;
    }

    private TodayAlarmStatisticsRespVO buildTodayAlarm() {
        TodayAlarmStatisticsRespVO result = new TodayAlarmStatisticsRespVO();

        Integer totalAlarms = alertRecordMapper.countTodayAlerts();
        result.setTotalAlarms(totalAlarms != null ? totalAlarms : 0);

        Integer handledCount = alertRecordMapper.countTodayReadAlerts();
        result.setHandledCount(handledCount != null ? handledCount : 0);

        Integer pendingCount = alertRecordMapper.countTodayUnreadAlerts();
        result.setPendingCount(pendingCount != null ? pendingCount : 0);

        if (result.getTotalAlarms() > 0) {
            result.setHandleRate(Math.round(result.getHandledCount() * 10000.0 / result.getTotalAlarms()) / 100.0);
        } else {
            result.setHandleRate(0.0);
        }

        result.setAvgResponseTime(0);

        // 按报警类型统计火警/故障/误报数（双读：alarmKind 为类型编码）
        List<Map<String, Object>> kindStats = alertRecordMapper.countTodayAlertsByAlarmKind();
        int fireAlarmCount = 0;
        int faultAlarmCount = 0;
        int falseAlarmCount = 0;
        for (Map<String, Object> stat : kindStats) {
            String kind = stat.get("alarmKind") != null ? stat.get("alarmKind").toString() : "";
            int count = ((Number) stat.get("count")).intValue();
            // kind=1 为火警, 2 为故障, 5 为误报
            if ("1".equals(kind)) {
                fireAlarmCount += count;
            } else if ("2".equals(kind)) {
                faultAlarmCount += count;
            } else if ("5".equals(kind)) {
                falseAlarmCount += count;
            }
        }
        result.setFireAlarmCount(fireAlarmCount);
        result.setFaultAlarmCount(faultAlarmCount);
        result.setFalseAlarmCount(falseAlarmCount);

        return result;
    }

    private List<AlarmOrderRespVO> buildRealtimeAlarms(int limit) {
        List<AlertRecordDO> alertRecords = alertRecordMapper.selectLatestAlerts(limit);
        List<AlarmOrderRespVO> result = new ArrayList<>();
        for (AlertRecordDO record : alertRecords) {
            result.add(convertToRespVO(record));
        }
        return result;
    }

    private List<DashboardStatisticsRespVO.RiskDistributionVO> buildRiskDistribution() {
        List<Map<String, Object>> stats = firePropertyMapper.countByRiskLevel();
        Map<String, String> levelNameMap = new HashMap<>();
        levelNameMap.put("critical", "重大风险");
        levelNameMap.put("major", "较大风险");
        levelNameMap.put("normal", "一般风险");
        levelNameMap.put("minor", "较小风险");

        List<DashboardStatisticsRespVO.RiskDistributionVO> result = new ArrayList<>();
        for (Map<String, Object> stat : stats) {
            DashboardStatisticsRespVO.RiskDistributionVO vo = new DashboardStatisticsRespVO.RiskDistributionVO();
            String riskLevel = stat.get("risk_level") != null ? stat.get("risk_level").toString() : "normal";
            vo.setRiskLevel(riskLevel);
            vo.setRiskLevelName(levelNameMap.getOrDefault(riskLevel, "未评估"));
            vo.setCount(stat.get("count") != null ? Integer.parseInt(stat.get("count").toString()) : 0);
            result.add(vo);
        }
        return result;
    }

    private List<DashboardStatisticsRespVO.AlarmTrendVO> buildAlarmTrend(int days) {
        List<Map<String, Object>> dbData = alertRecordMapper.countAlarmTrendByDay(days);
        Map<String, Map<String, Object>> dateMap = new LinkedHashMap<>();
        for (Map<String, Object> row : dbData) {
            String dateStr = row.get("alarm_date") != null ? row.get("alarm_date").toString() : "";
            dateMap.put(dateStr, row);
        }

        List<DashboardStatisticsRespVO.AlarmTrendVO> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            String dateStr = LocalDate.now().minusDays(i).format(fmt);
            DashboardStatisticsRespVO.AlarmTrendVO vo = new DashboardStatisticsRespVO.AlarmTrendVO();
            vo.setDate(dateStr);
            Map<String, Object> row = dateMap.get(dateStr);
            if (row != null) {
                vo.setAlarmCount(row.get("alarm_count") != null ? Integer.parseInt(row.get("alarm_count").toString()) : 0);
                vo.setHandleCount(row.get("handle_count") != null ? Integer.parseInt(row.get("handle_count").toString()) : 0);
            } else {
                vo.setAlarmCount(0);
                vo.setHandleCount(0);
            }
            result.add(vo);
        }
        return result;
    }

    private List<DashboardStatisticsRespVO.DeviceCategoryVO> buildDeviceCategory() {
        List<Map<String, Object>> stats = fireDeviceMapper.countByCategory();
        List<DashboardStatisticsRespVO.DeviceCategoryVO> result = new ArrayList<>();
        for (Map<String, Object> stat : stats) {
            DashboardStatisticsRespVO.DeviceCategoryVO item = new DashboardStatisticsRespVO.DeviceCategoryVO();
            item.setCategoryCode(stat.get("category_id") != null ? stat.get("category_id").toString() : "");
            item.setCategoryName(stat.get("category_name") != null ? stat.get("category_name").toString() : "");
            item.setCount(stat.get("device_count") != null ? Integer.parseInt(stat.get("device_count").toString()) : 0);
            result.add(item);
        }
        return result;
    }

    private List<DashboardStatisticsRespVO.AlarmTypeDistributionVO> buildAlarmTypeDistribution() {
        // 今日按报警类型统计（kind 映射到告警类型: 1=火警, 2=故障, 3=隐患, 4=离线, 5=误报）
        List<Map<String, Object>> stats = alertRecordMapper.countTodayAlertsByAlarmKind();
        Map<String, String> kindToType = new HashMap<>();
        kindToType.put("1", "fire");
        kindToType.put("2", "fault");
        kindToType.put("3", "risk");
        kindToType.put("4", "offline");
        kindToType.put("5", "false_alarm");

        Map<String, String> typeNameMap = new HashMap<>();
        typeNameMap.put("fire", "火警");
        typeNameMap.put("fault", "故障");
        typeNameMap.put("risk", "隐患");
        typeNameMap.put("offline", "离线");
        typeNameMap.put("false_alarm", "误报");

        // 按类型聚合
        Map<String, Integer> typeCountMap = new HashMap<>();
        for (Map<String, Object> stat : stats) {
            String kind = stat.get("alarmKind") != null ? stat.get("alarmKind").toString() : "1";
            int count = stat.get("count") != null ? Integer.parseInt(stat.get("count").toString()) : 0;
            String alarmType = kindToType.getOrDefault(kind, "smoke");
            typeCountMap.merge(alarmType, count, Integer::sum);
        }

        List<DashboardStatisticsRespVO.AlarmTypeDistributionVO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
            DashboardStatisticsRespVO.AlarmTypeDistributionVO vo = new DashboardStatisticsRespVO.AlarmTypeDistributionVO();
            vo.setAlarmType(entry.getKey());
            vo.setTypeName(typeNameMap.getOrDefault(entry.getKey(), "其他"));
            vo.setCount(entry.getValue());
            result.add(vo);
        }
        return result;
    }

    /**
     * 构建巡检任务摘要（admin看全部，企业只看自己租户）
     * 使用 selectList + LambdaQueryWrapper，由 MyBatis-Plus 租户拦截器自动隔离
     */
    private DashboardStatisticsRespVO.PatrolSummaryVO buildPatrolSummary() {
        DashboardStatisticsRespVO.PatrolSummaryVO summary = new DashboardStatisticsRespVO.PatrolSummaryVO();

        // 查询最近巡检任务（selectList 自动走租户拦截器，逻辑删除自动过滤）
        List<FirePatrolTaskDO> allTasks = firePatrolTaskMapper.selectList(
                new LambdaQueryWrapper<FirePatrolTaskDO>()
                        .orderByDesc(FirePatrolTaskDO::getPlanDate)
                        .last("LIMIT 20"));

        int total = allTasks.size();
        int completed = (int) allTasks.stream().filter(t -> "completed".equals(t.getStatus())).count();
        int pending = (int) allTasks.stream()
                .filter(t -> "pending".equals(t.getStatus()) || "in_progress".equals(t.getStatus()))
                .count();

        summary.setTotal(total);
        summary.setCompleted(completed);
        summary.setPending(pending);

        // 取前5条作为展示列表
        List<DashboardStatisticsRespVO.PatrolTaskItemVO> taskItems = new ArrayList<>();
        for (FirePatrolTaskDO task : allTasks.subList(0, Math.min(5, allTasks.size()))) {
            DashboardStatisticsRespVO.PatrolTaskItemVO item = new DashboardStatisticsRespVO.PatrolTaskItemVO();
            item.setId(task.getId());
            item.setTaskName(task.getTaskName() != null ? task.getTaskName() : "巡检任务");
            item.setInspectorName(task.getPatrolUserName() != null ? task.getPatrolUserName() : "未指定");
            item.setPlanTime(task.getPlanDate() != null ? task.getPlanDate().toString() : "");
            item.setStatus(task.getStatus() != null ? task.getStatus() : "pending");
            taskItems.add(item);
        }
        summary.setTasks(taskItems);

        log.info("[大屏] 巡检摘要: total={}, completed={}, pending={}", total, completed, pending);
        return summary;
    }

    /**
     * 构建当前值班列表（admin看全部，企业只看自己租户）
     */
    private List<DashboardStatisticsRespVO.DutyScheduleItemVO> buildCurrentDutyList() {
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();

        // 使用 Mapper 中已有的 selectCurrentDuty 方法（需要 tenantId 参数）
        List<FireDutyScheduleDO> dutyList;
        if (userTenantId != null && userTenantId != 1L) {
            // 企业账号：只查本租户（SQL 带有 tenant_id 条件）
            dutyList = TenantUtils.execute(userTenantId, () -> fireDutyScheduleMapper.selectCurrentDuty(userTenantId));
        } else if (userTenantId != null) {
            // 系统租户账号：查全部租户，手动绕过
            dutyList = TenantUtils.executeIgnoreWithResult(() ->
                    fireDutyScheduleMapper.selectList(
                            new LambdaQueryWrapper<FireDutyScheduleDO>()
                                    .eq(FireDutyScheduleDO::getScheduleDate, LocalDate.now())
                                    .in(FireDutyScheduleDO::getStatus, "on_duty", "scheduled")
                                    .orderByAsc(FireDutyScheduleDO::getShiftType)
                                    .last("LIMIT 10")));
        } else {
            dutyList = fireDutyScheduleMapper.selectList(
                    new LambdaQueryWrapper<FireDutyScheduleDO>()
                            .eq(FireDutyScheduleDO::getScheduleDate, LocalDate.now())
                            .in(FireDutyScheduleDO::getStatus, "on_duty", "scheduled")
                            .orderByAsc(FireDutyScheduleDO::getShiftType)
                            .last("LIMIT 10"));
        }

        List<DashboardStatisticsRespVO.DutyScheduleItemVO> result = new ArrayList<>();
        if (dutyList != null) {
            for (FireDutyScheduleDO schedule : dutyList) {
                DashboardStatisticsRespVO.DutyScheduleItemVO item = new DashboardStatisticsRespVO.DutyScheduleItemVO();
                item.setId(schedule.getId());
                item.setUserName(schedule.getDutyUserName() != null ? schedule.getDutyUserName() : "值班人员");
                item.setShiftName(schedule.getShiftName() != null ? schedule.getShiftName() : "值班");
                item.setStartTime(schedule.getStartTime() != null ? schedule.getStartTime() : "08:00");
                item.setEndTime(schedule.getEndTime() != null ? schedule.getEndTime() : "18:00");
                // 状态映射
                String status = schedule.getStatus();
                if ("on_duty".equals(status)) {
                    item.setStatus("ongoing");
                } else if ("completed".equals(status)) {
                    item.setStatus("completed");
                } else {
                    item.setStatus("pending");
                }
                result.add(item);
            }
        }

        log.info("[大屏] 当前值班: tenantId={}, 结果数={}", userTenantId, result.size());
        return result;
    }

    /**
     * 构建物业列表（admin看全部，企业只看自己租户）
     * 使用 selectList + LambdaQueryWrapper，由 MyBatis-Plus 租户拦截器自动隔离
     */
    private List<DashboardStatisticsRespVO.PropertyItemVO> buildPropertyList() {
        List<FirePropertyDO> properties = firePropertyMapper.selectList(
                new LambdaQueryWrapper<FirePropertyDO>()
                        .orderByDesc(FirePropertyDO::getId)
                        .last("LIMIT 100"));

        List<DashboardStatisticsRespVO.PropertyItemVO> result = new ArrayList<>();
        for (FirePropertyDO prop : properties) {
            DashboardStatisticsRespVO.PropertyItemVO item = new DashboardStatisticsRespVO.PropertyItemVO();
            item.setId(prop.getId());
            item.setName(prop.getName() != null ? prop.getName() : "未命名企业");
            item.setDistrict(prop.getDistrict());
            item.setRiskLevel(prop.getRiskLevel() != null ? prop.getRiskLevel() : "normal");
            item.setDeviceCount(0); // 设备数量需要关联查询，暂设为0，后续可优化
            item.setLongitude(prop.getLongitude() != null ? prop.getLongitude().doubleValue() : null);
            item.setLatitude(prop.getLatitude() != null ? prop.getLatitude().doubleValue() : null);
            item.setAddress(prop.getAddress());
            item.setContactName(prop.getContactName());
            item.setContactPhone(prop.getContactPhone());
            result.add(item);
        }

        log.info("[大屏] 物业列表: 结果数={}", result.size());
        return result;
    }

    private AlarmOrderRespVO convertToRespVO(AlertRecordDO record) {
        AlarmOrderRespVO vo = new AlarmOrderRespVO();
        vo.setId(record.getId());
        vo.setOrderNo("ALERT-" + record.getId());

        // level/alarm_kind 映射（双读：alarm_kind 非空=新语义；为空=旧语义回退）
        if (record.getAlarmKind() != null) {
            vo.setAlarmLevel(mapLevelNew(record.getLevel()));
            vo.setAlarmType(mapAlarmKindToAlarmType(record.getAlarmKind()));
            vo.setTier(record.getTier());
        } else {
            String level = record.getLevel();
            vo.setAlarmLevel(mapLevel(level));
            vo.setAlarmType(mapLevelToAlarmType(level));
        }

        // 时间
        if (record.getAlertTime() != null) {
            vo.setAlarmTime(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(record.getAlertTime()), ZoneId.systemDefault()));
        }

        // 告警信息
        vo.setAlarmDescription(record.getDetails() != null ? record.getDetails() : record.getName());
        vo.setAlarmLocation(record.getName());

        // 状态映射：通过 handle_status / details 处置记录 / readFlg 判断具体状态
        vo.setOrderStatus(AlertOrderStatusResolver.resolveOrderStatus(record));
        // 设备信息
        vo.setDeviceId(record.getDeviceId());
        vo.setDeviceName(record.getDeviceName());

        // 报警类型分类（0-5），供前端大屏按报警类型生成语音播报文本
        vo.setAlarmCategory(record.getAlarmType());

        // 单位名称（租户名称），供前端大屏语音播报使用
        if (record.getTenantId() != null) {
            try {
                vo.setTenantName(tenantApi.getTenantName(record.getTenantId()));
            } catch (Exception e) {
                log.warn("Failed to get tenant name for alarm record, tenantId: {}", record.getTenantId(), e);
            }
        }

        // 状态映射：未读 → pending, 已读 → closed
        vo.setOrderStatus(Boolean.TRUE.equals(record.getReadFlg()) ? "closed" : "pending");

        return vo;
    }

    private String mapLevel(String level) {
        if (level == null) return "warning";
        switch (level) {
            case "1":
                return "critical";
            case "2":
                return "major";
            case "3":
                return "warning";
            case "4":
                return "info";
            case "5":
                return "info";
            case "6":
                return "info";
            default:
                return "warning";
        }
    }

    private String mapLevelToAlarmType(String level) {
        if (level == null) return "fire";
        switch (level) {
            case "1":
                return "fire";        // 火警
            case "2":
                return "fault";       // 故障
            case "3":
                return "risk";        // 隐患
            case "4":
                return "offline";     // 离线
            case "5":
                return "false_alarm"; // 误报
            case "6":
                return "info";        // 信息
            default:
                return "fire";
        }
    }

    /**
     * 新语义：alarm_kind(1-6) → 前端报警类型（值域与 mapLevelToAlarmType 完全一致）
     */
    private String mapAlarmKindToAlarmType(Integer alarmKind) {
        if (alarmKind == null) return "fire";
        switch (alarmKind) {
            case 1:
                return "fire";        // 火警
            case 2:
                return "fault";       // 故障
            case 3:
                return "risk";        // 隐患
            case 4:
                return "offline";     // 离线
            case 5:
                return "false_alarm"; // 误报
            case 6:
                return "info";        // 信息
            default:
                return "fire";
        }
    }

    /**
     * 新语义：level(1/2/3) → 前端告警级别（1 为最大优先级）
     */
    private String mapLevelNew(String level) {
        if (level == null) return "warning";
        switch (level) {
            case "1":
                return "critical";
            case "2":
                return "major";
            case "3":
                return "warning";
            default:
                return "warning";
        }
    }

    // ==================== 租户隔离工具方法 ====================

    /**
     * 执行带租户意识的操作
     * @param supplier
     * @param overrideTenantId
     * @return
     * @param <T>
     */
    private <T> T executeWithTenantAware(Supplier<T> supplier, Long overrideTenantId) {
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        if (userTenantId == null) {
            return supplier.get();
        }
        // 仅超级管理员且显式指定租户时，才允许覆盖登录租户的查询上下文
        boolean canOverride = overrideTenantId != null
                && securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode());
        return TenantUtils.execute(canOverride ? overrideTenantId : userTenantId, supplier::get);
    }
}
