package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.ai.api.AiChatApi;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.DeviceStatusStatisticsRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.MonthlyReportDataVO;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireHazardDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDeviceMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireHazardMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import com.enjoyiot.module.eiot.service.UserTenantResolver;
import com.enjoyiot.module.system.api.tenant.TenantApi;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智慧消防周期运行报告（月/季/年）Service 实现类
 *
 * <p>与数据中心同源（同一 Mapper 方法），窗口为自然周期左闭右开；告警类型双读 COALESCE(alarm_kind, level)。
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireMonthlyReportServiceImpl implements FireMonthlyReportService {

    /** 报警类型编码（双读语义：新数据 alarm_kind、旧数据 level，1-5 编码一致）→ 类型名称 */
    private static final Map<String, String> ALARM_KIND_NAME = Map.of(
            "1", "火警", "2", "故障", "3", "隐患", "4", "离线", "5", "误报", "6", "信息");

    /** 工单状态 → 名称 */
    private static final Map<String, String> ORDER_STATUS_NAME = Map.of(
            "pending", "待处置", "handling", "处置中", "handled", "已处置", "closed", "已关闭");

    /** 隐患状态 → 名称 */
    private static final Map<String, String> HAZARD_STATUS_NAME = Map.of(
            "pending", "待整改", "rectifying", "整改中", "reviewing", "待验收", "closed", "已闭环", "cancelled", "已关闭");

    /** 隐患等级 → 名称 */
    private static final Map<String, String> HAZARD_LEVEL_NAME = Map.of(
            "general", "一般隐患", "major", "较大隐患", "critical", "重大隐患");

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_COMPACT_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter MONTH_DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    /** A4 纵向页面布局参数 */
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN_LEFT = 40f;
    private static final float MARGIN_RIGHT = 40f;
    private static final float MARGIN_TOP = 46f;
    private static final float MARGIN_BOTTOM = 42f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    /** 表格式版面底色（章节标题行 / 子标题行 / 表头行） */
    private static final Color SECTION_BG = new Color(0.85f, 0.85f, 0.85f);
    private static final Color CAPTION_BG = new Color(0.94f, 0.94f, 0.94f);
    private static final Color HEADER_BG = new Color(0.88f, 0.88f, 0.88f);

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private FireDeviceMapper fireDeviceMapper;

    @Resource
    private FireHazardMapper fireHazardMapper;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Resource
    private FireDashboardService fireDashboardService;

    @Resource
    private TenantApi tenantApi;

    @Resource
    private UserTenantResolver userTenantResolver;

    @Resource
    private AiChatApi aiChatApi;

    /**
     * 导出周期运行报告 PDF
     */
    @Override
    public void exportReportPdf(String periodType, String period, HttpServletResponse response) throws IOException {
        ReportPeriod rp = parsePeriod(periodType, period);

        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        MonthlyReportDataVO data = userTenantId != null
                ? TenantUtils.execute(userTenantId, () -> buildReportData(rp))
                : buildReportData(rp);

        // AI 生成“本期结论 / 问题与建议 / 下期工作计划”（失败时渲染层回退）
        fillAiAnalysis(data, rp);

        renderPdf(data, rp, response);
        log.info("[统计报表] 导出完成: periodType={}, period={}, 告警总数={}, 设备总数={}", rp.type, rp.periodKey,
                data.getTotalAlarms(), data.getDeviceTotal());
    }

    /**
     * 解析报表周期参数：monthly=yyyy-MM、quarterly=yyyy-Qn、yearly=yyyy；为空时默认上一完整期
     */
    private ReportPeriod parsePeriod(String periodType, String period) {
        String type = StrUtil.isBlank(periodType) ? "monthly" : periodType.trim().toLowerCase();
        ReportPeriod rp = switch (type) {
            case "quarterly" -> quarterlyPeriod(period);
            case "yearly" -> yearlyPeriod(period);
            case "monthly" -> monthlyPeriod(period);
            default -> throw new IllegalArgumentException("周期类型错误，应为 monthly/quarterly/yearly：" + periodType);
        };
        rp.startMs = toEpochMs(rp.startDate);
        rp.endMs = toEpochMs(rp.endDate);
        rp.prevStartMs = toEpochMs(rp.prevStartDate);
        rp.prevEndMs = toEpochMs(rp.prevEndDate);
        return rp;
    }

    /**
     * 月报周期：yyyy-MM，为空默认上一个自然月
     */
    private ReportPeriod monthlyPeriod(String period) {
        YearMonth ym = parseMonth(period);
        ReportPeriod rp = new ReportPeriod();
        rp.type = "monthly";
        rp.cycleName = "月度";
        rp.term = "本月";
        rp.nextTerm = "下月";
        rp.trendGranularity = "每日";
        rp.periodKey = ym.format(MONTH_FMT);
        rp.startDate = ym.atDay(1);
        rp.endDate = ym.plusMonths(1).atDay(1);
        rp.prevStartDate = ym.minusMonths(1).atDay(1);
        rp.prevEndDate = rp.startDate;
        rp.periodText = ym.getMonthValue() + "月1日—" + ym.getMonthValue() + "月" + ym.lengthOfMonth() + "日";
        rp.reportTitle = "智慧消防平台 " + ym.getYear() + "年" + ym.getMonthValue() + "月运行报告";
        rp.reportNo = "编号：XF-" + ym.format(MONTH_COMPACT_FMT);
        rp.fileBaseName = "智慧消防系统月度运行报告_" + rp.periodKey;
        return rp;
    }

    /**
     * 季报周期：yyyy-Qn，为空默认上一个完整季度
     */
    private ReportPeriod quarterlyPeriod(String period) {
        String key;
        if (StrUtil.isNotBlank(period)) {
            key = period.trim().toUpperCase();
            if (!key.matches("\\d{4}-Q[1-4]")) {
                throw new IllegalArgumentException("季度格式错误，应为 yyyy-Qn：" + period);
            }
        } else {
            LocalDate now = LocalDate.now();
            int year = now.getYear();
            int quarter = (now.getMonthValue() - 1) / 3 + 1;
            if (quarter == 1) {
                year--;
                quarter = 4;
            } else {
                quarter--;
            }
            key = year + "-Q" + quarter;
        }
        int year = Integer.parseInt(key.substring(0, 4));
        int quarter = key.charAt(6) - '0';
        ReportPeriod rp = new ReportPeriod();
        rp.type = "quarterly";
        rp.cycleName = "季度";
        rp.term = "本季";
        rp.nextTerm = "下季";
        rp.trendGranularity = "按周汇总";
        rp.periodKey = key;
        rp.startDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        rp.endDate = rp.startDate.plusMonths(3);
        rp.prevStartDate = rp.startDate.minusMonths(3);
        rp.prevEndDate = rp.startDate;
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = quarter * 3;
        rp.periodText = year + "年" + startMonth + "月1日—" + endMonth + "月" + rp.endDate.minusDays(1).getDayOfMonth() + "日";
        rp.reportTitle = "智慧消防平台 " + year + "年第" + quarter + "季度运行报告";
        rp.reportNo = "编号：XF-" + year + "Q" + quarter;
        rp.fileBaseName = "智慧消防系统季度运行报告_" + key;
        return rp;
    }

    /**
     * 年报周期：yyyy，为空默认上一个完整年度
     */
    private ReportPeriod yearlyPeriod(String period) {
        int year;
        if (StrUtil.isNotBlank(period)) {
            String key = period.trim();
            if (!key.matches("\\d{4}")) {
                throw new IllegalArgumentException("年度格式错误，应为 yyyy：" + period);
            }
            year = Integer.parseInt(key);
        } else {
            year = LocalDate.now().getYear() - 1;
        }
        ReportPeriod rp = new ReportPeriod();
        rp.type = "yearly";
        rp.cycleName = "年度";
        rp.term = "本年度";
        rp.nextTerm = "来年";
        rp.trendGranularity = "按月汇总";
        rp.periodKey = String.valueOf(year);
        rp.startDate = LocalDate.of(year, 1, 1);
        rp.endDate = LocalDate.of(year + 1, 1, 1);
        rp.prevStartDate = LocalDate.of(year - 1, 1, 1);
        rp.prevEndDate = rp.startDate;
        rp.periodText = year + "年1月1日—12月31日";
        rp.reportTitle = "智慧消防平台 " + year + "年度运行报告";
        rp.reportNo = "编号：XF-" + year;
        rp.fileBaseName = "智慧消防系统年度运行报告_" + year;
        return rp;
    }

    /**
     * 报表周期（月/季/年）：展示文案与本期/上期时间窗口
     */
    private static class ReportPeriod {

        /** monthly / quarterly / yearly */
        String type;
        String cycleName;
        /** 本期词（本月/本季/本年度） */
        String term;
        /** 下期词（下月/下季/来年） */
        String nextTerm;
        String trendGranularity;
        /** yyyy-MM / yyyy-Qn / yyyy */
        String periodKey;
        String periodText;
        String reportTitle;
        String reportNo;
        String fileBaseName;
        /** 本期 [startDate, endDate) 与上期（环比）窗口；毫秒值由 parsePeriod 换算 */
        LocalDate startDate;
        LocalDate endDate;
        LocalDate prevStartDate;
        LocalDate prevEndDate;
        long startMs;
        long endMs;
        long prevStartMs;
        long prevEndMs;
    }

    /**
     * 解析月份参数，为空时取上一个自然月
     */
    private YearMonth parseMonth(String month) {
        if (StrUtil.isBlank(month)) {
            return YearMonth.now().minusMonths(1);
        }
        try {
            return YearMonth.parse(month.trim(), MONTH_FMT);
        } catch (Exception e) {
            throw new IllegalArgumentException("月份格式错误，应为 yyyy-MM：" + month);
        }
    }

    private long toEpochMs(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ==================== 数据聚合 ====================

    private MonthlyReportDataVO buildReportData(ReportPeriod rp) {
        long startMs = rp.startMs;
        long endMs = rp.endMs;
        long prevStartMs = rp.prevStartMs;
        long prevEndMs = rp.prevEndMs;
        MonthlyReportDataVO data = new MonthlyReportDataVO();
        data.setMonth(rp.periodKey);
        data.setPeriodText(rp.periodText);
        data.setGeneratedAt(LocalDateTime.now().format(DATETIME_FMT));

        // 1. 概览：本月按报警类型统计（双读：1=火警 2=故障 3=隐患 4=离线 5=误报）
        Map<String, Integer> kindCountMap = toAlarmKindCountMap(alertRecordMapper.countByAlarmKindInRange(startMs, endMs));
        int total = kindCountMap.values().stream().mapToInt(Integer::intValue).sum();
        data.setTotalAlarms(total);
        data.setFireAlarms(kindCountMap.getOrDefault("1", 0));
        data.setFaultAlarms(kindCountMap.getOrDefault("2", 0));
        data.setRiskAlarms(kindCountMap.getOrDefault("3", 0));
        data.setOfflineAlarms(kindCountMap.getOrDefault("4", 0));
        data.setFalseAlarmAlarms(kindCountMap.getOrDefault("5", 0));

        // 2. 已处置/未处置（按报警类型 + 已读状态）
        Map<String, Integer> handledByKind = new HashMap<>();
        int handledTotal = 0;
        for (Map<String, Object> row : alertRecordMapper.countByAlarmKindAndReadInRange(startMs, endMs)) {
            String kind = str(row.get("alarmKind"));
            int count = parseInt(row.get("count"));
            if (parseReadFlag(row.get("readFlg"))) {
                handledByKind.merge(kind, count, Integer::sum);
                handledTotal += count;
            }
        }
        data.setHandledAlarms(handledTotal);
        data.setHandleRate(total > 0 ? Math.round(handledTotal * 10000.0 / total) / 100.0 : 0.0);

        // 3. 环比（上月同口径告警总数）
        Map<String, Integer> prevKindCountMap = toAlarmKindCountMap(alertRecordMapper.countByAlarmKindInRange(prevStartMs, prevEndMs));
        int prevTotal = prevKindCountMap.values().stream().mapToInt(Integer::intValue).sum();
        data.setMomText(buildMomText(total, prevTotal));

        // 4. 平均响应时间（秒）：本月已处置记录中可解析处置时刻的均值
        data.setAvgResponseTime(calcAvgResponseTime(startMs, endMs));

        // 5. 设备快照（导出时刻，复用大屏稳定统计）
        DeviceStatusStatisticsRespVO deviceStatus = fireDashboardService.getDeviceStatusStatistics();
        if (deviceStatus != null) {
            data.setDeviceTotal(deviceStatus.getTotal());
            data.setDeviceOnline(deviceStatus.getOnline());
            data.setDeviceOffline(deviceStatus.getOffline());
            data.setDeviceFault(deviceStatus.getFault());
            data.setDeviceInactive(deviceStatus.getInactive());
            data.setDeviceOnlineRate(deviceStatus.getOnlineRate());
        } else {
            data.setDeviceTotal(0);
            data.setDeviceOnline(0);
            data.setDeviceOffline(0);
            data.setDeviceFault(0);
            data.setDeviceInactive(0);
            data.setDeviceOnlineRate(0.0);
        }
        Integer tenantCount = fireDeviceMapper.countTenants();
        data.setTenantCount(tenantCount != null ? tenantCount : 0);
        Integer propertyCount = firePropertyMapper.countTotal();
        data.setPropertyCount(propertyCount != null ? propertyCount : 0);

        // 6. 按类型统计（数量/占比/已处置/未处置）
        List<MonthlyReportDataVO.TypeStatItem> typeStats = new ArrayList<>();
        for (String kind : new String[]{"1", "2", "3", "4", "5"}) {
            int count = kindCountMap.getOrDefault(kind, 0);
            int handled = handledByKind.getOrDefault(kind, 0);
            MonthlyReportDataVO.TypeStatItem item = new MonthlyReportDataVO.TypeStatItem();
            item.setTypeName(ALARM_KIND_NAME.get(kind));
            item.setCount(count);
            item.setPercent(total > 0 ? String.format("%.1f%%", count * 100.0 / total) : "0.0%");
            item.setHandledCount(handled);
            item.setPendingCount(count - handled);
            typeStats.add(item);
        }
        data.setTypeStats(typeStats);

        // 7. 告警趋势（月报按日、季报按自然周、年报按月聚合；空桶补零保证连续）
        Map<String, Map<String, Object>> dayMap = new HashMap<>();
        for (Map<String, Object> row : alertRecordMapper.countByDayInRange(startMs, endMs)) {
            dayMap.put(str(row.get("alarm_date")), row);
        }
        data.setDailyTrend(buildTrend(rp, dayMap));

        // 8. 高峰时段 TOP5
        List<MonthlyReportDataVO.HourStatItem> hourStats = new ArrayList<>();
        for (Map<String, Object> row : alertRecordMapper.countByHourInRange(startMs, endMs)) {
            if (hourStats.size() >= 5) {
                break;
            }
            int hour = parseInt(row.get("alarm_hour"));
            MonthlyReportDataVO.HourStatItem item = new MonthlyReportDataVO.HourStatItem();
            item.setHourLabel(String.format("%02d:00-%02d:00", hour, (hour + 1) % 24));
            item.setCount(parseInt(row.get("count")));
            hourStats.add(item);
        }
        data.setHourStats(hourStats);

        // 9. 高发单位 TOP5（填充租户名称）
        List<MonthlyReportDataVO.TenantStatItem> tenantStats = new ArrayList<>();
        for (Map<String, Object> row : alertRecordMapper.countByTenantInRange(startMs, endMs)) {
            Long tenantId = row.get("tenant_id") != null ? Long.valueOf(row.get("tenant_id").toString()) : null;
            MonthlyReportDataVO.TenantStatItem item = new MonthlyReportDataVO.TenantStatItem();
            item.setTenantName(resolveTenantName(tenantId));
            item.setCount(parseInt(row.get("count")));
            tenantStats.add(item);
        }
        data.setTenantStats(tenantStats);

        // 10. 离线设备清单（最多 30 条）
        List<MonthlyReportDataVO.OfflineDeviceItem> offlineDevices = new ArrayList<>();
        for (Map<String, Object> row : fireDeviceMapper.selectOfflineDevices(30)) {
            MonthlyReportDataVO.OfflineDeviceItem item = new MonthlyReportDataVO.OfflineDeviceItem();
            item.setDeviceName(str(row.get("name")));
            item.setDeviceNo(str(row.get("dn")));
            item.setPropertyName(str(row.get("property_name")));
            item.setLocation(str(row.get("addr")));
            offlineDevices.add(item);
        }
        data.setOfflineDevices(offlineDevices);

        // 11. 重复告警设备/误报高发设备 TOP5
        data.setRepeatAlarmDevices(toDeviceStats(alertRecordMapper.countByDeviceInRange(startMs, endMs, null)));
        data.setFalseAlarmDevices(toDeviceStats(alertRecordMapper.countByDeviceInRange(startMs, endMs, "5")));

        // 12. 重点告警明细（火警或未处置，最多 50 条）
        data.setKeyAlarms(mapKeyAlarms(alertRecordMapper.selectKeyAlarmsInRange(startMs, endMs, 50)));

        // 13. 隐患与整改（按 found_date 落在本周期内统计）
        LocalDate rangeStart = rp.startDate;
        LocalDate rangeEnd = rp.endDate.minusDays(1);
        Map<String, Integer> hazardStatusCount = new HashMap<>();
        for (Map<String, Object> row : fireHazardMapper.countByStatus(null, null, rangeStart, rangeEnd)) {
            hazardStatusCount.put(str(row.get("status")), parseInt(row.get("count")));
        }
        int hazardFound = hazardStatusCount.values().stream().mapToInt(Integer::intValue).sum();
        int hazardClosed = hazardStatusCount.getOrDefault("closed", 0);
        data.setHazardFound(hazardFound);
        data.setHazardClosed(hazardClosed);
        data.setHazardOpen(hazardFound - hazardClosed);
        Integer overdue = fireHazardMapper.countOverdue(null, rangeStart, rangeEnd);
        data.setHazardOverdue(overdue != null ? overdue : 0);

        List<FireHazardDO> hazards = fireHazardMapper.selectList(new LambdaQueryWrapperX<FireHazardDO>()
                .between(FireHazardDO::getFoundDate, rangeStart, rangeEnd)
                .orderByDesc(FireHazardDO::getFoundDate)
                .last("LIMIT 20"));
        data.setHazardItems(mapHazardItems(hazards));

        return data;
    }

    /**
     * 构建告警趋势：monthly 按日、quarterly 按自然周（周一为周起点）、yearly 按月聚合；空桶补零
     */
    private List<MonthlyReportDataVO.TrendItem> buildTrend(ReportPeriod rp, Map<String, Map<String, Object>> dayMap) {
        List<MonthlyReportDataVO.TrendItem> trend = new ArrayList<>();
        if ("yearly".equals(rp.type)) {
            int[] alarms = new int[12];
            int[] handles = new int[12];
            for (LocalDate d = rp.startDate; d.isBefore(rp.endDate); d = d.plusDays(1)) {
                accumulateDay(dayMap, d, alarms, handles, d.getMonthValue() - 1);
            }
            for (int m = 0; m < 12; m++) {
                trend.add(trendItem(String.format("%02d", m + 1), alarms[m], handles[m]));
            }
        } else if ("quarterly".equals(rp.type)) {
            LocalDate firstMonday = rp.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            int buckets = (int) ((rp.endDate.minusDays(1).toEpochDay() - firstMonday.toEpochDay()) / 7) + 1;
            int[] alarms = new int[buckets];
            int[] handles = new int[buckets];
            for (LocalDate d = rp.startDate; d.isBefore(rp.endDate); d = d.plusDays(1)) {
                accumulateDay(dayMap, d, alarms, handles, (int) ((d.toEpochDay() - firstMonday.toEpochDay()) / 7));
            }
            for (int i = 0; i < buckets; i++) {
                trend.add(trendItem(firstMonday.plusWeeks(i).format(MONTH_DAY_FMT), alarms[i], handles[i]));
            }
        } else {
            for (LocalDate d = rp.startDate; d.isBefore(rp.endDate); d = d.plusDays(1)) {
                Map<String, Object> row = dayMap.get(d.format(DATE_FMT));
                trend.add(trendItem(String.format("%02d", d.getDayOfMonth()),
                        row != null ? parseInt(row.get("alarm_count")) : 0,
                        row != null ? parseInt(row.get("handle_count")) : 0));
            }
        }
        return trend;
    }

    private static void accumulateDay(Map<String, Map<String, Object>> dayMap, LocalDate d,
                                      int[] alarms, int[] handles, int idx) {
        Map<String, Object> row = dayMap.get(d.format(DATE_FMT));
        if (row != null) {
            alarms[idx] += parseInt(row.get("alarm_count"));
            handles[idx] += parseInt(row.get("handle_count"));
        }
    }

    private static MonthlyReportDataVO.TrendItem trendItem(String label, int alarmCount, int handleCount) {
        MonthlyReportDataVO.TrendItem item = new MonthlyReportDataVO.TrendItem();
        item.setLabel(label);
        item.setAlarmCount(alarmCount);
        item.setHandleCount(handleCount);
        return item;
    }

    /**
     * 平均响应时间（秒）：解析已处置记录 details 末尾处置时刻与告警时刻之差，取均值；无数据返回 null
     */
    private Integer calcAvgResponseTime(long startMs, long endMs) {
        List<AlertRecordDO> handled = alertRecordMapper.selectList(new LambdaQueryWrapperX<AlertRecordDO>()
                .select(AlertRecordDO::getAlertTime, AlertRecordDO::getDetails)
                .ge(AlertRecordDO::getAlertTime, startMs)
                .lt(AlertRecordDO::getAlertTime, endMs)
                .eq(AlertRecordDO::getReadFlg, true)
                .last("LIMIT 2000"));
        long sumSeconds = 0;
        int validCount = 0;
        for (AlertRecordDO record : handled) {
            Long seconds = parseResponseSeconds(record);
            if (seconds != null) {
                sumSeconds += seconds;
                validCount++;
            }
        }
        return validCount > 0 ? (int) (sumSeconds / validCount) : null;
    }

    private Long parseResponseSeconds(AlertRecordDO record) {
        if (record.getAlertTime() == null || StrUtil.isBlank(record.getDetails())) {
            return null;
        }
        String details = record.getDetails();
        int lastBracket = details.lastIndexOf('[');
        if (lastBracket < 0) {
            return null;
        }
        String lastRecord = details.substring(lastBracket);
        int bracketEnd = lastRecord.indexOf(']');
        if (bracketEnd <= 0) {
            return null;
        }
        try {
            LocalDateTime handleTime = LocalDateTime.parse(lastRecord.substring(1, bracketEnd));
            long deltaMs = handleTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - record.getAlertTime();
            return deltaMs >= 0 ? deltaMs / 1000 : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 环比描述文本
     */
    private String buildMomText(int current, int prev) {
        if (prev <= 0) {
            return current > 0 ? "上期无数据" : "—";
        }
        double pct = (current - prev) * 100.0 / prev;
        if (Math.abs(pct) < 0.05) {
            return "持平";
        }
        return String.format("%s %.1f%%", pct > 0 ? "上升" : "下降", Math.abs(pct));
    }

    private String resolveTenantName(Long tenantId) {
        if (tenantId == null) {
            return "未知单位";
        }
        try {
            String name = tenantApi.getTenantName(tenantId);
            return StrUtil.isNotBlank(name) ? name : ("租户" + tenantId);
        } catch (Exception e) {
            log.warn("[月度报告] 查询租户名称失败: tenantId={}", tenantId, e);
            return "租户" + tenantId;
        }
    }

    private List<MonthlyReportDataVO.DeviceAlertStatItem> toDeviceStats(List<Map<String, Object>> rows) {
        List<MonthlyReportDataVO.DeviceAlertStatItem> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            MonthlyReportDataVO.DeviceAlertStatItem item = new MonthlyReportDataVO.DeviceAlertStatItem();
            String deviceName = str(row.get("device_name"));
            item.setDeviceName(StrUtil.isNotBlank(deviceName) ? deviceName : "未知设备");
            item.setCount(parseInt(row.get("count")));
            result.add(item);
        }
        return result;
    }

    private List<MonthlyReportDataVO.KeyAlarmItem> mapKeyAlarms(List<AlertRecordDO> records) {
        List<MonthlyReportDataVO.KeyAlarmItem> result = new ArrayList<>();
        for (AlertRecordDO record : records) {
            MonthlyReportDataVO.KeyAlarmItem item = new MonthlyReportDataVO.KeyAlarmItem();
            if (record.getAlertTime() != null) {
                item.setAlarmTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(record.getAlertTime()), ZoneId.systemDefault()).format(DATETIME_FMT));
            }
            item.setDeviceName(StrUtil.isNotBlank(record.getDeviceName()) ? record.getDeviceName()
                    : (StrUtil.isNotBlank(record.getName()) ? record.getName() : "未知设备"));
            // 双读：新数据取 alarm_kind，旧数据回退 level 类型语义
            item.setAlarmTypeName(record.getAlarmKind() != null
                    ? ALARM_KIND_NAME.getOrDefault(String.valueOf(record.getAlarmKind()), "未知")
                    : ALARM_KIND_NAME.getOrDefault(record.getLevel(), "未知"));
            item.setLocation(record.getName() != null ? record.getName() : "");
            item.setStatusName(ORDER_STATUS_NAME.getOrDefault(AlertOrderStatusResolver.resolveOrderStatus(record), "待处置"));
            applyLastHandleInfo(record, item);
            result.add(item);
        }
        return result;
    }

    /**
     * 从 details 末尾解析最后一条处置记录（格式: [yyyy-MM-ddTHH:mm] 处理人 - 动作：备注）
     */
    private void applyLastHandleInfo(AlertRecordDO record, MonthlyReportDataVO.KeyAlarmItem item) {
        String details = record.getDetails();
        if (StrUtil.isBlank(details)) {
            return;
        }
        int lastBracket = details.lastIndexOf('[');
        if (lastBracket < 0) {
            return;
        }
        String lastRecord = details.substring(lastBracket);
        int bracketEnd = lastRecord.indexOf(']');
        if (bracketEnd <= 0) {
            return;
        }
        int dashIdx = lastRecord.indexOf(" - ");
        String result = lastRecord.substring(bracketEnd + 1).trim();
        if (dashIdx > bracketEnd) {
            item.setHandler(lastRecord.substring(bracketEnd + 1, dashIdx).trim());
            result = lastRecord.substring(dashIdx + 3).trim();
        }
        item.setResult(result);
    }

    private List<MonthlyReportDataVO.HazardItem> mapHazardItems(List<FireHazardDO> hazards) {
        List<MonthlyReportDataVO.HazardItem> result = new ArrayList<>();
        for (FireHazardDO hazard : hazards) {
            MonthlyReportDataVO.HazardItem item = new MonthlyReportDataVO.HazardItem();
            item.setHazardNo(hazard.getHazardNo());
            item.setLevelName(HAZARD_LEVEL_NAME.getOrDefault(hazard.getHazardLevel(), hazard.getHazardLevel()));
            item.setDesc(hazard.getHazardDesc());
            item.setLocation(hazard.getLocation());
            item.setFoundDate(hazard.getFoundDate() != null ? hazard.getFoundDate().format(DATE_FMT) : "");
            item.setDeadline(hazard.getDeadline() != null ? hazard.getDeadline().format(DATE_FMT) : "");
            item.setStatusName(HAZARD_STATUS_NAME.getOrDefault(hazard.getStatus(), hazard.getStatus()));
            result.add(item);
        }
        return result;
    }

    private static Map<String, Integer> toAlarmKindCountMap(List<Map<String, Object>> rows) {
        Map<String, Integer> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(str(row.get("alarmKind")), parseInt(row.get("count")));
        }
        return map;
    }

    private static int parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(Object value) {
        return value != null ? value.toString() : "";
    }

    private static boolean parseReadFlag(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString()) == 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    // ==================== AI 分析生成（结论 / 问题与建议 / 下期工作计划） ====================

    /** 报告分析场景 system 提示词主体（角色、约束与输出格式要求；周期名词由 buildAiSystemPrompt 注入） */
    private static final String AI_SYSTEM_PROMPT_TEMPLATE = "你是安为智慧消防远程值守系统的%s报告分析助手。"
            + "请严格基于给定的统计数据撰写内容，不得编造数据；使用纯文本输出，不要使用 Markdown 标记；语言简洁、专业、可执行。\n"
            + "输出格式要求：依次输出三个部分——第一部分第一行输出“%s结论”，随后一句话结论（不超过 60 字，概括平台运行是否正常、告警是否上升、处置是否及时）；"
            + "第二部分第一行输出“问题与建议”，随后以“1. ”“2. ”编号列出 4~6 条，每条一行，需覆盖：哪类告警最多、哪些单位处置慢、哪些设备经常离线、是否存在误报、%s重点整改对象；"
            + "第三部分第一行输出“%s工作计划”，随后以“1. ”“2. ”编号列出 4~6 条，每条一行，需覆盖：离线设备排查、误报治理、重点单位复查、设备维护、值班响应提升。"
            + "除上述三部分外不要输出任何其他标题、注释或解释文字。";

    /** 按报表周期填充提示词中的周期名词（参数：周期名、本期词、下期词、下期词） */
    private String buildAiSystemPrompt(ReportPeriod rp) {
        return String.format(AI_SYSTEM_PROMPT_TEMPLATE, rp.cycleName, rp.term, rp.nextTerm, rp.nextTerm);
    }

    /**
     * AI 生成本期结论 / 问题与建议 / 下期工作计划，失败时留空由渲染层回退
     */
    private void fillAiAnalysis(MonthlyReportDataVO data, ReportPeriod rp) {
        if (data.getTotalAlarms() == null || data.getTotalAlarms() == 0) {
            log.info("[统计报表] {}无告警，跳过 AI 分析生成", rp.term);
            return;
        }
        try {
            long start = System.currentTimeMillis();
            String content = aiChatApi.chatOnce(buildAiSystemPrompt(rp), buildAnalysisPrompt(data, rp));
            if (StrUtil.isBlank(content)) {
                log.warn("[统计报表] AI 返回内容为空，报告使用留白区");
                return;
            }
            String cleaned = sanitizeAiText(content);
            int idxSuggestion = cleaned.indexOf("问题与建议");
            int idxPlan = cleaned.indexOf(rp.nextTerm + "工作计划");
            if (idxSuggestion >= 0) {
                data.setAiConclusionText(stripSectionLabel(cleaned.substring(0, idxSuggestion), rp));
                int end = idxPlan > idxSuggestion ? idxPlan : cleaned.length();
                data.setAiSuggestionText(stripSectionLabel(cleaned.substring(idxSuggestion, end), rp));
                if (idxPlan > idxSuggestion) {
                    data.setAiPlanText(stripSectionLabel(cleaned.substring(idxPlan), rp));
                }
            } else if (idxPlan >= 0) {
                data.setAiConclusionText(stripSectionLabel(cleaned.substring(0, idxPlan), rp));
                data.setAiPlanText(stripSectionLabel(cleaned.substring(idxPlan), rp));
            } else {
                data.setAiSuggestionText(stripSectionLabel(cleaned, rp));
            }
            log.info("[统计报表] AI 分析生成完成: 耗时={}ms, 结论={}字, 建议={}字, 计划={}字",
                    System.currentTimeMillis() - start,
                    nvlStr(data.getAiConclusionText()).length(), nvlStr(data.getAiSuggestionText()).length(),
                    nvlStr(data.getAiPlanText()).length());
        } catch (Exception e) {
            log.warn("[统计报表] AI 分析生成失败，报告使用留白区: {}", e.getMessage());
        }
    }

    /** 数据摘要提示词（system 提示词为固定模板，此处仅动态拼装本期统计） */
    private String buildAnalysisPrompt(MonthlyReportDataVO data, ReportPeriod rp) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是 ").append(data.getMonth()).append(" 智慧消防系统").append(rp.cycleName).append("运行统计数据：\n");
        sb.append("- 告警总数：").append(nvl(data.getTotalAlarms())).append(" 起（环比：").append(nvlStr(data.getMomText())).append("）\n");
        sb.append("- 分类型：火警 ").append(nvl(data.getFireAlarms())).append(" 起、故障 ").append(nvl(data.getFaultAlarms()))
                .append(" 起、隐患 ").append(nvl(data.getRiskAlarms())).append(" 起、离线 ")
                .append(nvl(data.getOfflineAlarms())).append(" 起、误报 ").append(nvl(data.getFalseAlarmAlarms())).append(" 起\n");
        sb.append("- 处置：已处置 ").append(nvl(data.getHandledAlarms())).append(" 起，处置率 ").append(data.getHandleRate())
                .append("%，平均响应时间 ").append(data.getAvgResponseTime() != null ? data.getAvgResponseTime() + " 秒" : "无数据").append("\n");
        sb.append("- 设备：总数 ").append(nvl(data.getDeviceTotal())).append(" 台，在线 ").append(nvl(data.getDeviceOnline()))
                .append(" 台，离线 ").append(nvl(data.getDeviceOffline())).append(" 台，故障 ").append(nvl(data.getDeviceFault()))
                .append(" 台，在线率 ").append(nvlRate(data.getDeviceOnlineRate())).append("\n");

        List<String> offlineNames = new ArrayList<>();
        for (MonthlyReportDataVO.OfflineDeviceItem item : nvlList(data.getOfflineDevices())) {
            if (offlineNames.size() >= 5) {
                break;
            }
            offlineNames.add(item.getDeviceName());
        }
        sb.append("- 离线设备示例：").append(offlineNames.isEmpty() ? "无" : String.join("、", offlineNames)).append("\n");
        sb.append("- 重复告警设备 TOP5：").append(formatDeviceStats(data.getRepeatAlarmDevices())).append("\n");
        sb.append("- 误报高发设备 TOP5：").append(formatDeviceStats(data.getFalseAlarmDevices())).append("\n");

        List<String> units = new ArrayList<>();
        for (MonthlyReportDataVO.TenantStatItem item : nvlList(data.getTenantStats())) {
            units.add(item.getTenantName() + "（" + item.getCount() + " 起）");
        }
        sb.append("- 告警高发单位 TOP5：").append(units.isEmpty() ? "无" : String.join("；", units)).append("\n");
        sb.append("- 隐患：").append(rp.term).append("发现 ").append(nvl(data.getHazardFound())).append(" 项，已闭环 ").append(nvl(data.getHazardClosed()))
                .append(" 项，超期未整改 ").append(nvl(data.getHazardOverdue())).append(" 项\n");
        sb.append("- 火警或未处置的重点告警共 ").append(nvlList(data.getKeyAlarms()).size()).append(" 条（明细已列入报告）\n");
        sb.append("\n请按系统提示词要求的格式输出“").append(rp.term).append("结论 / 问题与建议 / ").append(rp.nextTerm).append("工作计划”三部分。");
        return sb.toString();
    }

    /** 设备维度 TOP 文本（如：烟火算法摄像头（72 次）；展厅烟感（4 次）） */
    private String formatDeviceStats(List<MonthlyReportDataVO.DeviceAlertStatItem> items) {
        List<String> parts = new ArrayList<>();
        for (MonthlyReportDataVO.DeviceAlertStatItem item : nvlList(items)) {
            parts.add(item.getDeviceName() + "（" + item.getCount() + " 次）");
        }
        return parts.isEmpty() ? "无" : String.join("；", parts);
    }

    /** 清洗 AI 输出中的 Markdown 痕迹与代码围栏 */
    private String sanitizeAiText(String text) {
        String cleaned = text.replace("```", "");
        cleaned = cleaned.replace("**", "").replace("##", "").replace("【", "").replace("】", "");
        cleaned = cleaned.replace("`", "");
        return cleaned.trim();
    }

    /** 去掉分段开头可能出现的标题行（如“问题与建议：”），保留正文 */
    private String stripSectionLabel(String section, ReportPeriod rp) {
        if (StrUtil.isBlank(section)) {
            return "";
        }
        String[] lines = section.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean firstContentKept = false;
        for (String raw : lines) {
            String line = raw.trim();
            if (!firstContentKept) {
                if (line.isEmpty()) {
                    continue;
                }
                String simplified = line.replace("：", "").replace(":", "").replace("#", "").trim();
                firstContentKept = true;
                if ((rp.term + "结论").equals(simplified) || "问题与建议".equals(simplified) || (rp.nextTerm + "工作计划").equals(simplified)) {
                    continue;
                }
            }
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    // ==================== PDF 渲染 ====================

    private void renderPdf(MonthlyReportDataVO data, ReportPeriod rp, HttpServletResponse response) throws IOException {
        try (ReportPdfWriter w = new ReportPdfWriter(rp.reportTitle, rp.reportNo)) {
            // 基本信息格
            w.gridRow(new float[]{1.4f, 2.6f, 1.6f, 3.0f},
                    new String[]{"统计周期", nvlStr(data.getPeriodText()), "生成时间", nvlStr(data.getGeneratedAt())},
                    18, null, 7.5f, false);
            w.gridRow(new float[]{1.4f, 7.2f},
                    new String[]{"覆盖范围", "联网单位 " + nvl(data.getTenantCount()) + " 个；设备 "
                            + nvl(data.getDeviceTotal()) + " 台；物业项目 " + nvl(data.getPropertyCount()) + " 个"},
                    18, null, 7.5f, false);

            renderOverview(w, data, rp);
            renderAlarmAnalysis(w, data, rp);
            renderDeviceStatus(w, data);
            renderKeyAlarms(w, data);
            renderHazards(w, data, rp);
            renderAnalysisSection(w, "六、问题与建议", data.getAiSuggestionText(), rp.term);
            renderAnalysisSection(w, "七、" + rp.nextTerm + "工作计划", data.getAiPlanText(), rp.term);
            renderAttachment(w);
            w.signatureRow();
            w.finish(response, rp.fileBaseName);
        }
    }

    private void renderOverview(ReportPdfWriter w, MonthlyReportDataVO data, ReportPeriod rp) throws IOException {
        w.sectionRow("一、" + rp.term + "概览");
        String conclusion = StrUtil.isNotBlank(data.getAiConclusionText()) ? data.getAiConclusionText() : "—";
        w.labelTextRow("结论", conclusion);
        float[] cols = {1.2f, 1.7f, 1.2f, 2.3f};
        String avgResponse = data.getAvgResponseTime() != null ? data.getAvgResponseTime() + " 秒" : "—";
        w.gridRow(cols, new String[]{"告警总数", nvl(data.getTotalAlarms()) + " 起", "环比", nvlStr(data.getMomText())}, 17, null, 7.5f, false);
        w.gridRow(cols, new String[]{"火警", nvl(data.getFireAlarms()) + " 起", "故障", nvl(data.getFaultAlarms()) + " 起"}, 17, null, 7.5f, false);
        w.gridRow(cols, new String[]{"隐患", nvl(data.getRiskAlarms()) + " 起", "离线", nvl(data.getOfflineAlarms()) + " 起"}, 17, null, 7.5f, false);
        w.gridRow(cols, new String[]{"误报", nvl(data.getFalseAlarmAlarms()) + " 起", "处置率", data.getHandleRate() + "%"}, 17, null, 7.5f, false);
        w.gridRow(cols, new String[]{"平均响应时间", avgResponse, "设备在线率", nvlRate(data.getDeviceOnlineRate())}, 17, null, 7.5f, false);
        w.gridRow(cols, new String[]{"设备总数", nvl(data.getDeviceTotal()) + " 台", "在线", nvl(data.getDeviceOnline()) + " 台"}, 17, null, 7.5f, false);
        w.gridRow(cols, new String[]{"离线", nvl(data.getDeviceOffline()) + " 台", "故障", nvl(data.getDeviceFault()) + " 台"}, 17, null, 7.5f, false);
    }

    private void renderAlarmAnalysis(ReportPdfWriter w, MonthlyReportDataVO data, ReportPeriod rp) throws IOException {
        w.sectionRow("二、告警事件分析");
        w.captionRow("（一）按类型统计");
        List<String[]> typeRows = new ArrayList<>();
        for (MonthlyReportDataVO.TypeStatItem item : nvlList(data.getTypeStats())) {
            typeRows.add(new String[]{item.getTypeName(), String.valueOf(item.getCount()), item.getPercent(),
                    String.valueOf(item.getHandledCount()), String.valueOf(item.getPendingCount())});
        }
        int handled = nvl(data.getHandledAlarms());
        typeRows.add(new String[]{"合计", String.valueOf(nvl(data.getTotalAlarms())), "100.0%",
                String.valueOf(handled), String.valueOf(nvl(data.getTotalAlarms()) - handled)});
        w.gridTable(new String[]{"类型", "数量", "占比", "已处置", "未处置"},
                new float[]{2f, 1.5f, 1.5f, 1.6f, 1.6f}, typeRows, 17);

        w.captionRow("（二）告警趋势（" + rp.trendGranularity + "；告警数 / 已处置数）");
        w.chartCell(data.getDailyTrend());

        w.captionRow("（三）高峰时段 / 高发单位 TOP5");
        List<MonthlyReportDataVO.HourStatItem> hours = nvlList(data.getHourStats());
        List<MonthlyReportDataVO.TenantStatItem> tenants = nvlList(data.getTenantStats());
        int size = Math.max(hours.size(), tenants.size());
        if (size == 0) {
            w.fullWidthTextCell("本期无数据。", 8f);
        } else {
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                rows.add(new String[]{String.valueOf(i + 1),
                        i < hours.size() ? hours.get(i).getHourLabel() : "—",
                        i < hours.size() ? String.valueOf(hours.get(i).getCount()) : "—",
                        i < tenants.size() ? tenants.get(i).getTenantName() : "—",
                        i < tenants.size() ? String.valueOf(tenants.get(i).getCount()) : "—"});
            }
            w.gridTable(new String[]{"排名", "高峰时段", "时段告警数", "高发单位", "单位告警数"},
                    new float[]{0.9f, 1.8f, 1.4f, 3.2f, 1.4f}, rows, 17);
        }
    }

    private void renderDeviceStatus(ReportPdfWriter w, MonthlyReportDataVO data) throws IOException {
        w.sectionRow("三、设备运行状态");
        w.gridTable(new String[]{"设备总数", "在线", "离线", "故障", "未激活", "在线率"},
                new float[]{1.4f, 1f, 1f, 1f, 1.2f, 1.2f},
                List.<String[]>of(new String[]{String.valueOf(nvl(data.getDeviceTotal())), String.valueOf(nvl(data.getDeviceOnline())),
                        String.valueOf(nvl(data.getDeviceOffline())), String.valueOf(nvl(data.getDeviceFault())),
                        String.valueOf(nvl(data.getDeviceInactive())), nvlRate(data.getDeviceOnlineRate())}),
                17);

        w.captionRow("（一）离线设备清单（最多显示 30 台，完整清单可在设备管理页查看）");
        List<MonthlyReportDataVO.OfflineDeviceItem> offlineDevices = nvlList(data.getOfflineDevices());
        if (offlineDevices.isEmpty()) {
            w.fullWidthTextCell("导出时刻无离线设备。", 8f);
        } else {
            List<String[]> rows = new ArrayList<>();
            for (MonthlyReportDataVO.OfflineDeviceItem item : offlineDevices) {
                rows.add(new String[]{item.getDeviceName(), item.getDeviceNo(), item.getPropertyName(), item.getLocation()});
            }
            w.gridTable(new String[]{"设备名称", "设备编号", "所属单位", "安装位置"},
                    new float[]{2.6f, 2.4f, 2.6f, 2.8f}, rows, 17);
        }

        w.captionRow("（二）重复告警 / 误报高发设备 TOP5");
        List<MonthlyReportDataVO.DeviceAlertStatItem> repeats = nvlList(data.getRepeatAlarmDevices());
        List<MonthlyReportDataVO.DeviceAlertStatItem> falseAlarms = nvlList(data.getFalseAlarmDevices());
        int size = Math.max(repeats.size(), falseAlarms.size());
        if (size == 0) {
            w.fullWidthTextCell("本期无数据。", 8f);
        } else {
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                rows.add(new String[]{String.valueOf(i + 1),
                        i < repeats.size() ? repeats.get(i).getDeviceName() : "—",
                        i < repeats.size() ? String.valueOf(repeats.get(i).getCount()) : "—",
                        i < falseAlarms.size() ? falseAlarms.get(i).getDeviceName() : "—",
                        i < falseAlarms.size() ? String.valueOf(falseAlarms.get(i).getCount()) : "—"});
            }
            w.gridTable(new String[]{"排名", "重复告警设备", "告警次数", "误报高发设备", "误报次数"},
                    new float[]{0.9f, 3f, 1.4f, 3f, 1.4f}, rows, 17);
        }
    }

    private void renderKeyAlarms(ReportPdfWriter w, MonthlyReportDataVO data) throws IOException {
        w.sectionRow("四、重点告警明细");
        w.captionRow("筛选规则：火警或未处置记录，最多显示 50 条。");
        List<MonthlyReportDataVO.KeyAlarmItem> keyAlarms = nvlList(data.getKeyAlarms());
        if (keyAlarms.isEmpty()) {
            w.fullWidthTextCell("本期无重点关注告警。", 8f);
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (MonthlyReportDataVO.KeyAlarmItem item : keyAlarms) {
            rows.add(new String[]{item.getAlarmTime(), item.getDeviceName(), item.getAlarmTypeName(),
                    item.getLocation(), item.getStatusName(), nvlStr(item.getHandler()), nvlStr(item.getResult())});
        }
        w.gridTable(new String[]{"告警时间", "设备名称", "类型", "位置", "处置状态", "处置人", "处置结果"},
                new float[]{2.2f, 2.2f, 0.9f, 1.6f, 1.1f, 1.1f, 2.9f}, rows, 17);
    }

    private void renderHazards(ReportPdfWriter w, MonthlyReportDataVO data, ReportPeriod rp) throws IOException {
        w.sectionRow("五、隐患与整改");
        w.gridTable(new String[]{rp.term + "发现", "已闭环", "未闭环", "超期未整改"},
                new float[]{1.6f, 1.4f, 1.4f, 1.6f},
                List.<String[]>of(new String[]{String.valueOf(nvl(data.getHazardFound())), String.valueOf(nvl(data.getHazardClosed())),
                        String.valueOf(nvl(data.getHazardOpen())), String.valueOf(nvl(data.getHazardOverdue()))}),
                17);

        w.captionRow("（隐患明细，最多显示 20 条）");
        List<MonthlyReportDataVO.HazardItem> hazards = nvlList(data.getHazardItems());
        if (hazards.isEmpty()) {
            w.fullWidthTextCell(rp.term + "无新增隐患。", 8f);
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (MonthlyReportDataVO.HazardItem item : hazards) {
            rows.add(new String[]{nvlStr(item.getHazardNo()), nvlStr(item.getLevelName()), nvlStr(item.getDesc()),
                    nvlStr(item.getLocation()), nvlStr(item.getFoundDate()), nvlStr(item.getDeadline()),
                    nvlStr(item.getStatusName())});
        }
        w.gridTable(new String[]{"编号", "等级", "描述", "位置", "发现日期", "整改期限", "状态"},
                new float[]{2.0f, 1.4f, 3.2f, 1.8f, 1.5f, 1.5f, 1.3f}, rows, 17);
    }

    /**
     * 渲染分析章节（六/七）：优先使用 AI 生成文本，为空时回退留白横线区
     */
    private void renderAnalysisSection(ReportPdfWriter w, String title, String aiText, String term) throws IOException {
        w.sectionRow(title);
        if (StrUtil.isNotBlank(aiText)) {
            w.captionRow("（由 AI 大模型基于" + term + "运行数据自动生成，仅供参考）");
            w.fullWidthTextCell(aiText, 8.5f);
        } else {
            w.captionRow("（AI 分析暂不可用，本区留白供人工填写）");
            w.blankGridArea(8);
        }
    }

    /**
     * 渲染附件说明（模板第 8 节）
     */
    private void renderAttachment(ReportPdfWriter w) throws IOException {
        w.sectionRow("八、附件说明");
        w.fullWidthTextCell("附件在报告内对应章节呈现：告警明细表（四、重点告警明细）、离线设备表（三、设备运行状态）、"
                + "未处置清单（四、重点告警明细中的未处置记录）、隐患整改台账（五、隐患与整改）；"
                + "完整明细可在系统「报警记录 / 隐患管理」页面另行导出。", 8f);
    }

    private static int nvl(Integer value) {
        return value != null ? value : 0;
    }

    private static String nvlStr(String value) {
        return value != null ? value : "";
    }

    private static String nvlRate(Double value) {
        return value != null ? value + "%" : "0.0%";
    }

    private static <T> List<T> nvlList(List<T> list) {
        return list != null ? list : List.of();
    }

    /**
     * 月度报告 PDF 分页渲染器（全网格表格式/作业票风格；持有单次导出的全部状态，每次导出新建实例）
     */
    private static class ReportPdfWriter implements AutoCloseable {

        final PDDocument document = new PDDocument();
        final PDType0Font font;
        final String reportTitle;
        final String reportNo;
        PDPageContentStream cs;
        int pageNum = 0;
        float y;

        ReportPdfWriter(String reportTitle, String reportNo) throws IOException {
            this.reportTitle = reportTitle;
            this.reportNo = reportNo;
            this.font = PdfReportUtils.loadChineseFont(document);
            newPage();
        }

        /**
         * 新建页面：首页绘制大标题与右上角编号，续页绘制简明表头；每页绘制页脚
         */
        void newPage() throws IOException {
            if (cs != null) {
                cs.close();
            }
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            document.addPage(page);
            cs = new PDPageContentStream(document, page);
            pageNum++;
            if (pageNum == 1) {
                float tw = PdfReportUtils.textWidth(font, reportTitle, 16);
                PdfReportUtils.showText(cs, font, 16, reportTitle, (PAGE_WIDTH - tw) / 2, PAGE_HEIGHT - MARGIN_TOP + 4);
                float nw = PdfReportUtils.textWidth(font, reportNo, 8);
                PdfReportUtils.showText(cs, font, 8, reportNo, PAGE_WIDTH - MARGIN_RIGHT - nw, PAGE_HEIGHT - MARGIN_TOP + 4);
                y = PAGE_HEIGHT - MARGIN_TOP - 20;
            } else {
                PdfReportUtils.showText(cs, font, 7.5f, reportTitle + "（续）", MARGIN_LEFT, PAGE_HEIGHT - MARGIN_TOP + 12);
                float nw = PdfReportUtils.textWidth(font, reportNo, 7.5f);
                PdfReportUtils.showText(cs, font, 7.5f, reportNo, PAGE_WIDTH - MARGIN_RIGHT - nw, PAGE_HEIGHT - MARGIN_TOP + 12);
                cs.setStrokingColor(new Color(0.7f, 0.7f, 0.7f));
                cs.setLineWidth(0.8f);
                cs.moveTo(MARGIN_LEFT, PAGE_HEIGHT - MARGIN_TOP + 4);
                cs.lineTo(PAGE_WIDTH - MARGIN_RIGHT, PAGE_HEIGHT - MARGIN_TOP + 4);
                cs.stroke();
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.BLACK);
                y = PAGE_HEIGHT - MARGIN_TOP - 4;
            }
            // 页脚（每页固定位置，位于内容下边距之外）
            PdfReportUtils.showText(cs, font, 6.5f,
                    "注：设备状态为导出时刻快照；完整明细可在系统「报警记录 / 隐患管理」页面导出。", MARGIN_LEFT, 26);
            String pageText = "第 " + pageNum + " 页";
            float pw = PdfReportUtils.textWidth(font, pageText, 6.5f);
            PdfReportUtils.showText(cs, font, 6.5f, pageText, PAGE_WIDTH - MARGIN_RIGHT - pw, 26);
        }

        void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN_BOTTOM) {
                newPage();
            }
        }

        private float[] scale(float[] rel) {
            float total = 0;
            for (float v : rel) {
                total += v;
            }
            float[] out = new float[rel.length];
            for (int i = 0; i < rel.length; i++) {
                out[i] = rel[i] / total * CONTENT_WIDTH;
            }
            return out;
        }

        /**
         * 单行网格（多单元格，文本垂直居中；bg 为空时不填充底色）
         */
        void gridRow(float[] relWidths, String[] cells, float height, Color bg, float fontSize, boolean center) throws IOException {
            ensureSpace(height);
            float[] widths = scale(relWidths);
            float x = MARGIN_LEFT;
            for (int i = 0; i < cells.length && i < widths.length; i++) {
                PdfReportUtils.drawGridCell(cs, font, cells[i], x, y, widths[i], height, bg, fontSize, center);
                x += widths[i];
            }
            y -= height;
        }

        /**
         * 章节标题行（灰底、跨列）
         */
        void sectionRow(String title) throws IOException {
            gridRow(new float[]{1f}, new String[]{title}, 21, SECTION_BG, 10.5f, false);
        }

        /**
         * 子标题行（浅灰底、跨列）
         */
        void captionRow(String text) throws IOException {
            gridRow(new float[]{1f}, new String[]{text}, 17, CAPTION_BG, 8f, false);
        }

        /**
         * 标签 + 自动折行文本单元格（概览结论行）
         */
        void labelTextRow(String label, String text) throws IOException {
            float[] widths = scale(new float[]{1.2f, 7.4f});
            List<String> lines = PdfReportUtils.wrapText(font, text, 8f, widths[1] - 12);
            if (lines.isEmpty()) {
                lines.add("—");
            }
            float lineHeight = 13.5f;
            float height = Math.max(18f, lines.size() * lineHeight + 8);
            ensureSpace(height);
            PdfReportUtils.drawGridCell(cs, font, label, MARGIN_LEFT, y, widths[0], height, null, 7.5f, false);
            PdfReportUtils.drawGridCellRect(cs, MARGIN_LEFT + widths[0], y, widths[1], height, null);
            float ty = y - 6 - 8;
            for (String line : lines) {
                PdfReportUtils.showText(cs, font, 8, line, MARGIN_LEFT + widths[0] + 6, ty);
                ty -= lineHeight;
            }
            y -= height;
        }

        /**
         * 全宽网格单元格（自动折行文本，用于附件说明 / AI 正文等）
         */
        void fullWidthTextCell(String text, float fontSize) throws IOException {
            List<String> lines = PdfReportUtils.wrapText(font, text, fontSize, CONTENT_WIDTH - 14);
            if (lines.isEmpty()) {
                lines.add("—");
            }
            float lineHeight = fontSize + 5.5f;
            float height = lines.size() * lineHeight + 9;
            ensureSpace(height);
            PdfReportUtils.drawGridCellRect(cs, MARGIN_LEFT, y, CONTENT_WIDTH, height, null);
            float ty = y - 5 - fontSize;
            for (String line : lines) {
                PdfReportUtils.showText(cs, font, fontSize, line, MARGIN_LEFT + 7, ty);
                ty -= lineHeight;
            }
            y -= height;
        }

        /**
         * 全宽留白横线区（AI 不可用时的回退）
         */
        void blankGridArea(int lineCount) throws IOException {
            float height = lineCount * 16f + 8;
            ensureSpace(height);
            PdfReportUtils.drawGridCellRect(cs, MARGIN_LEFT, y, CONTENT_WIDTH, height, null);
            PdfReportUtils.drawBlankLines(cs, MARGIN_LEFT + 8, y - 18, CONTENT_WIDTH - 16, lineCount, 16);
            y -= height;
        }

        /**
         * 网格表（灰底表头 + 数据行，跨页时自动续绘表头）
         */
        void gridTable(String[] headers, float[] relWidths, List<String[]> rows, float rowHeight) throws IOException {
            float[] widths = scale(relWidths);
            ensureSpace(rowHeight * 2 + 4);
            drawGridHeaderRow(headers, widths, rowHeight);
            for (String[] row : rows) {
                if (y - rowHeight < MARGIN_BOTTOM) {
                    newPage();
                    drawGridHeaderRow(headers, widths, rowHeight);
                }
                float x = MARGIN_LEFT;
                for (int i = 0; i < row.length && i < widths.length; i++) {
                    PdfReportUtils.drawGridCell(cs, font, row[i], x, y, widths[i], rowHeight, null, 7.5f, false);
                    x += widths[i];
                }
                y -= rowHeight;
            }
            y -= 4;
        }

        private void drawGridHeaderRow(String[] headers, float[] widths, float rowHeight) throws IOException {
            float x = MARGIN_LEFT;
            for (int i = 0; i < headers.length; i++) {
                PdfReportUtils.drawGridCell(cs, font, headers[i], x, y, widths[i], rowHeight, HEADER_BG, 7.5f, true);
                x += widths[i];
            }
            y -= rowHeight;
        }

        /**
         * 每日趋势柱状图（带框单元格）
         */
        void chartCell(List<MonthlyReportDataVO.TrendItem> trend) throws IOException {
            if (trend == null || trend.isEmpty()) {
                fullWidthTextCell("本期无数据。", 8f);
                return;
            }
            float height = 122f;
            ensureSpace(height);
            PdfReportUtils.drawGridCellRect(cs, MARGIN_LEFT, y, CONTENT_WIDTH, height, null);
            String[] labels = new String[trend.size()];
            int[] alarms = new int[trend.size()];
            int[] handles = new int[trend.size()];
            for (int i = 0; i < trend.size(); i++) {
                MonthlyReportDataVO.TrendItem item = trend.get(i);
                labels[i] = item.getLabel();
                alarms[i] = item.getAlarmCount() != null ? item.getAlarmCount() : 0;
                handles[i] = item.getHandleCount() != null ? item.getHandleCount() : 0;
            }
            PdfReportUtils.drawBarChart(cs, font, labels, alarms, handles,
                    MARGIN_LEFT + 8, y - height + 20, CONTENT_WIDTH - 16, 78);
            y -= height;
        }

        /**
         * 底部签署区（留空供打印后手填）
         */
        void signatureRow() throws IOException {
            y -= 6;
            gridRow(new float[]{1f, 1.4f, 1f, 1.4f, 1f, 1.4f, 1f, 1.8f},
                    new String[]{"制表人", "", "审核人", "", "批准人", "", "日期", ""},
                    32, null, 7.5f, false);
        }

        /**
         * 收尾：关闭内容流并输出响应
         */
        void finish(HttpServletResponse response, String fileBaseName) throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
            response.setContentType("application/pdf");
            String encodedFilename = URLEncoder.encode(fileBaseName + ".pdf", StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
            document.save(response.getOutputStream());
        }

        @Override
        public void close() throws IOException {
            document.close();
        }
    }
}
