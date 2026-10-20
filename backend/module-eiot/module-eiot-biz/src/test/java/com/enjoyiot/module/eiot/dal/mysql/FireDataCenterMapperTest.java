package com.enjoyiot.module.eiot.dal.mysql;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.enjoyiot.framework.datasource.config.EnjoyDataSourceAutoConfiguration;
import com.enjoyiot.framework.mybatis.config.EnjoyMybatisAutoConfiguration;
import com.enjoyiot.framework.mybatis.core.util.MyBatisUtils;
import com.enjoyiot.framework.tenant.config.TenantProperties;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDeviceMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDutyScheduleMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireHandoverRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePatrolTaskMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireRepairOrderMapper;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据中心新增统计 SQL 的 Mapper 测试（直连开发库，application-unit-test.yaml 提供数据源）
 *
 * 为什么不用框架的 BaseDbUnitTest：其 @Sql("/sql/clean.sql") 为 H2 语法，且本项目业务模块统一 MySQL 方言；
 * 本测试改用 @Transactional 每方法回滚，数据不落库。
 *
 * 覆盖的数据中心新 SQL（每条断言：租户 A/B 交叉隔离、时间窗口左闭右开、去重/分桶正确）：
 * - AlertRecordMapper.countByTierInRange / countDistinctAlarmDevicesInRange
 * - FireDeviceMapper.rankDeviceByTenant
 * - FireDutyScheduleMapper.countOnTimeCheckIn
 * - FirePatrolTaskMapper.countTrendByDay
 * - FireRepairOrderMapper.countTrendByDay
 *
 * 注意：租户过滤依赖 TenantDatabaseInterceptor（mybatis 拦截器），因此本测试必须装配租户拦截器
 * （等价 EnjoyTenantAutoConfiguration，但该自动配置还依赖 Web/TenantApi 组件，测试上下文不引入）；
 * Map 结果键名与 SQL 别名一致（MyBatis 3.5.17 不对 Map 键做驼峰转换）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = FireDataCenterMapperTest.Application.class)
@ActiveProfiles("unit-test")
@Transactional
class FireDataCenterMapperTest {

    /** 测试租户号段：取大数值，避免与开发库真实数据碰撞 */
    private static final long TENANT_A = 9800001L;
    private static final long TENANT_B = 9800002L;
    private static final long PROPERTY_A = 9800001L;
    private static final long PROPERTY_B = 9800002L;

    /** 告警/维修趋势窗口：2026-01-05 00:00 [含) ~ 2026-01-08 00:00 [不含) */
    private static final long WIN_START_MS = LocalDate.of(2026, 1, 5).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    private static final long WIN_END_MS = LocalDate.of(2026, 1, 8).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    private static final LocalDateTime WIN_START_DT = LocalDate.of(2026, 1, 5).atStartOfDay();
    private static final LocalDateTime WIN_END_DT = LocalDate.of(2026, 1, 8).atStartOfDay();

    /** 值班/巡检业务日期窗口：2026-01-01 ~ 2026-01-10 */
    private static final LocalDate DAY_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY_END = LocalDate.of(2026, 1, 10);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TenantProperties.class)
    @Import({
            EnjoyDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            EnjoyMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            SpringUtil.class
    })
    static class Application {

        /** 等价 EnjoyTenantAutoConfiguration#tenantLineInnerInterceptor（测试的核心：验证租户隔离） */
        @Bean
        public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties properties,
                                                                     MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor inner = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(properties));
            MyBatisUtils.addInterceptor(interceptor, inner, 0);
            return inner;
        }
    }

    @Autowired
    private DataSource dataSource;
    @Autowired
    private AlertRecordMapper alertRecordMapper;
    @Autowired
    private FireDeviceMapper fireDeviceMapper;
    @Autowired
    private FireDutyScheduleMapper fireDutyScheduleMapper;
    @Autowired
    private FireHandoverRecordMapper fireHandoverRecordMapper;
    @Autowired
    private FirePatrolTaskMapper firePatrolTaskMapper;
    @Autowired
    private FireRepairOrderMapper fireRepairOrderMapper;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        // 拦截器 getTenantId() 依赖 TenantContextHolder，不设置会抛 NPE
        TenantContextHolder.setTenantId(TENANT_A);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== eiot_alert_record ====================

    @Test
    @DisplayName("countByTierInRange：按档位分桶（NULL 不计入），仅当前租户")
    void countByTierInRange_bucketsByTier_tenantIsolated() {
        insertAlert(9900101L, TENANT_A, WIN_START_MS + 1_000, 1, null);
        insertAlert(9900102L, TENANT_A, WIN_START_MS + 2_000, 1, null);
        insertAlert(9900103L, TENANT_A, WIN_START_MS + 3_000, 2, null);
        insertAlert(9900104L, TENANT_A, WIN_START_MS + 4_000, 3, null);
        insertAlert(9900105L, TENANT_A, WIN_START_MS + 5_000, null, null); // 无档位：不产生桶
        insertAlert(9900106L, TENANT_B, WIN_START_MS + 1_000, 1, null);   // 他租户：不可见

        Map<String, Integer> byTier = indexCount(alertRecordMapper.countByTierInRange(WIN_START_MS, WIN_END_MS), "tier");

        assertThat(byTier).containsOnlyKeys("1", "2", "3");
        assertThat(byTier.get("1")).isEqualTo(2);
        assertThat(byTier.get("2")).isEqualTo(1);
        assertThat(byTier.get("3")).isEqualTo(1);
    }

    @Test
    @DisplayName("countByTierInRange：时间窗口左闭右开（alert_time >= start 且 < end）")
    void countByTierInRange_windowIsHalfOpen() {
        insertAlert(9900111L, TENANT_A, WIN_START_MS - 1, 1, null); // 起点前 1ms：不含
        insertAlert(9900112L, TENANT_A, WIN_START_MS, 1, null);     // 起点：含
        insertAlert(9900113L, TENANT_A, WIN_END_MS - 1, 1, null);   // 终点前 1ms：含
        insertAlert(9900114L, TENANT_A, WIN_END_MS, 1, null);       // 终点：不含

        Map<String, Integer> byTier = indexCount(alertRecordMapper.countByTierInRange(WIN_START_MS, WIN_END_MS), "tier");

        assertThat(byTier).containsExactlyEntriesOf(Map.of("1", 2));
    }

    @Test
    @DisplayName("countDistinctAlarmDevicesInRange：设备去重、无设备不计、窗口与租户隔离")
    void countDistinctAlarmDevicesInRange_distinctAndIsolated() {
        insertAlert(9900121L, TENANT_A, WIN_START_MS + 1_000, 1, 101L);
        insertAlert(9900122L, TENANT_A, WIN_START_MS + 2_000, 1, 101L); // 同设备重复告警：只算一次
        insertAlert(9900123L, TENANT_A, WIN_START_MS + 3_000, 1, 102L);
        insertAlert(9900124L, TENANT_A, WIN_START_MS + 4_000, 1, null); // 无设备：不计
        insertAlert(9900125L, TENANT_A, WIN_START_MS - 1, 1, 103L);     // 窗口外：不计
        insertAlert(9900126L, TENANT_B, WIN_START_MS + 1_000, 1, 201L); // 他租户：不计

        assertThat(alertRecordMapper.countDistinctAlarmDevicesInRange(WIN_START_MS, WIN_END_MS)).isEqualTo(2);
    }

    // ==================== eiot_device_info ====================

    @Test
    @DisplayName("rankDeviceByTenant：按租户聚合设备数，仅当前租户可见")
    void rankDeviceByTenant_tenantIsolated() {
        insertDevice(9900131L, TENANT_A);
        insertDevice(9900132L, TENANT_A);
        insertDevice(9900133L, TENANT_A);
        insertDevice(9900134L, TENANT_B);

        List<Map<String, Object>> rows = fireDeviceMapper.rankDeviceByTenant(5);

        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("tenant_id")).longValue()).isEqualTo(TENANT_A);
        assertThat(((Number) rows.get(0).get("count")).intValue()).isEqualTo(3);
    }

    // ==================== fire_duty_schedule ====================

    @Test
    @DisplayName("countOnTimeCheckIn：签到时间 <= TIMESTAMP(schedule_date, start_time) 为准时，窗口/物业/租户过滤")
    void countOnTimeCheckIn_onTimeBoundary() {
        // 表有唯一键 (tenant_id, property_id, schedule_date, shift_type)，同一物业同一天的三个班次用不同 shift_type 区分
        insertDuty(9900141L, TENANT_A, PROPERTY_A, "2026-01-05", "day", "08:00", "2026-01-05 08:00:00");      // 准点：算
        insertDuty(9900142L, TENANT_A, PROPERTY_A, "2026-01-05", "night", "20:00", "2026-01-05 19:59:59");   // 提前：算
        insertDuty(9900143L, TENANT_A, PROPERTY_A, "2026-01-05", "wholeday", "00:00", "2026-01-05 00:00:01"); // 迟 1 秒：不算
        insertDuty(9900144L, TENANT_A, PROPERTY_A, "2026-01-06", "day", "08:00", null);                       // 未签到：不算
        insertDuty(9900145L, TENANT_A, PROPERTY_A, "2025-12-31", "day", "08:00", "2025-12-31 07:00:00");     // schedule_date 窗口外：不算
        insertDuty(9900146L, TENANT_B, PROPERTY_B, "2026-01-05", "day", "08:00", "2026-01-05 07:00:00");     // 他租户：不可见
        insertDuty(9900147L, TENANT_A, PROPERTY_B, "2026-01-05", "day", "08:00", "2026-01-05 07:00:00");     // 同租户他物业：物业过滤时排除

        assertThat(fireDutyScheduleMapper.countOnTimeCheckIn(null, DAY_START, DAY_END)).isEqualTo(3);
        assertThat(fireDutyScheduleMapper.countOnTimeCheckIn(PROPERTY_A, DAY_START, DAY_END)).isEqualTo(2);
        assertThat(fireDutyScheduleMapper.countOnTimeCheckIn(PROPERTY_B, DAY_START, DAY_END)).isEqualTo(1);
    }

    // ==================== fire_patrol_task ====================

    @Test
    @DisplayName("patrol countTrendByDay：按 plan_date 分桶（创建/完成），仅当前租户")
    void patrolCountTrendByDay_bucketsByPlanDate_tenantIsolated() {
        insertPatrol(9900151L, TENANT_A, PROPERTY_A, "2026-01-05", "completed");
        insertPatrol(9900152L, TENANT_A, PROPERTY_A, "2026-01-05", "completed");
        insertPatrol(9900153L, TENANT_A, PROPERTY_A, "2026-01-05", "pending");
        insertPatrol(9900154L, TENANT_A, PROPERTY_A, "2026-01-06", "completed");
        insertPatrol(9900155L, TENANT_A, PROPERTY_A, "2025-12-31", "completed"); // 窗口外：不计
        insertPatrol(9900156L, TENANT_B, PROPERTY_B, "2026-01-05", "completed"); // 他租户：不可见

        List<Map<String, Object>> rows = firePatrolTaskMapper.countTrendByDay(null, DAY_START, DAY_END);

        assertThat(rows).hasSize(2);
        Map<String, Object> jan5 = findByKey(rows, "plan_date", "2026-01-05");
        assertThat(((Number) jan5.get("created_count")).intValue()).isEqualTo(3);
        assertThat(((Number) jan5.get("completed_count")).intValue()).isEqualTo(2);
        Map<String, Object> jan6 = findByKey(rows, "plan_date", "2026-01-06");
        assertThat(((Number) jan6.get("created_count")).intValue()).isEqualTo(1);
        assertThat(((Number) jan6.get("completed_count")).intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("fire_handover_record countInRange：窗口左闭右开，仅当前租户")
    void handoverCountInRange_windowAndTenant() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 5, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 8, 0, 0);
        insertHandover(9900171L, TENANT_A, "2026-01-05 00:00:00"); // 起点：含
        insertHandover(9900172L, TENANT_A, "2026-01-06 12:00:00"); // 窗口内：含
        insertHandover(9900173L, TENANT_A, "2026-01-08 00:00:00"); // 终点：不含
        insertHandover(9900174L, TENANT_A, "2026-01-04 23:59:59"); // 起点前：不含
        insertHandover(9900175L, TENANT_B, "2026-01-06 12:00:00"); // 他租户：不可见

        assertThat(fireHandoverRecordMapper.countInRange(start, end)).isEqualTo(2);
    }

    // ==================== fire_repair_order ====================

    @Test
    @DisplayName("repair countTrendByDay：创建/完成按不同日期归集并合并同一天，物业过滤与租户隔离")
    void repairCountTrendByDay_unionMerge_propertyFilter() {
        insertRepair(9900161L, TENANT_A, PROPERTY_A, "2026-01-05 10:00:00", "2026-01-06 12:00:00"); // 01-05 创建、01-06 完成
        insertRepair(9900162L, TENANT_A, PROPERTY_A, "2026-01-05 11:00:00", null);                  // 01-05 创建
        insertRepair(9900163L, TENANT_A, PROPERTY_B, "2026-01-05 09:00:00", null);                  // 同租户他物业
        insertRepair(9900164L, TENANT_A, PROPERTY_A, "2026-01-04 09:00:00", null);                  // 创建窗口外：不计
        insertRepair(9900165L, TENANT_B, PROPERTY_B, "2026-01-05 09:00:00", null);                  // 他租户：不可见
        insertRepair(9900166L, TENANT_A, PROPERTY_A, "2025-12-01 09:00:00", "2026-01-06 18:00:00"); // 创建窗口外、01-06 完成

        List<Map<String, Object>> all = fireRepairOrderMapper.countTrendByDay(null, WIN_START_DT, WIN_END_DT);
        Map<String, Object> jan5 = findByKey(all, "date", "2026-01-05");
        assertThat(((Number) jan5.get("created_count")).intValue()).isEqualTo(3); // 161/162/163
        assertThat(((Number) jan5.get("completed_count")).intValue()).isEqualTo(0);
        Map<String, Object> jan6 = findByKey(all, "date", "2026-01-06");
        assertThat(((Number) jan6.get("created_count")).intValue()).isEqualTo(0);
        assertThat(((Number) jan6.get("completed_count")).intValue()).isEqualTo(2); // 161/166

        List<Map<String, Object>> filtered = fireRepairOrderMapper.countTrendByDay(PROPERTY_A, WIN_START_DT, WIN_END_DT);
        assertThat(((Number) findByKey(filtered, "date", "2026-01-05").get("created_count")).intValue()).isEqualTo(2);
        assertThat(((Number) findByKey(filtered, "date", "2026-01-06").get("completed_count")).intValue()).isEqualTo(2);
    }

    // ==================== 测试数据插入（JdbcTemplate 不走租户拦截器，可自由构造双租户数据） ====================

    private void insertAlert(long id, long tenantId, long alertTime, Integer tier, Long deviceId) {
        jdbc.update("INSERT INTO eiot_alert_record (id, alert_time, tenant_id, tier, device_id, deleted) VALUES (?, ?, ?, ?, ?, 0)",
                id, alertTime, tenantId, tier, deviceId);
    }

    private void insertDevice(long id, long tenantId) {
        jdbc.update("INSERT INTO eiot_device_info (id, dn, product_key, serial_no, node_type, tenant_id, deleted) VALUES (?, ?, 'PK-JUNIT', ?, 0, ?, 0)",
                id, "DN-JUNIT-" + id, "SN-JUNIT-" + id, tenantId);
    }

    private void insertDuty(long id, long tenantId, long propertyId, String scheduleDate, String shiftType, String startTime, String checkInTime) {
        jdbc.update("INSERT INTO fire_duty_schedule (id, tenant_id, property_id, schedule_date, shift_type, start_time, end_time, check_in_time, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, '23:59', ?, 0)",
                id, tenantId, propertyId, scheduleDate, shiftType, startTime, checkInTime);
    }

    private void insertHandover(long id, long tenantId, String handoverTime) {
        jdbc.update("INSERT INTO fire_handover_record (id, tenant_id, handover_time, outgoing_person, incoming_person, deleted) "
                        + "VALUES (?, ?, ?, 'JUnit 交班人', 'JUnit 接班人', 0)",
                id, tenantId, handoverTime);
    }

    private void insertPatrol(long id, long tenantId, long propertyId, String planDate, String status) {
        jdbc.update("INSERT INTO fire_patrol_task (id, tenant_id, property_id, task_no, task_name, task_type, plan_date, status, deleted) "
                        + "VALUES (?, ?, ?, ?, 'JUnit 测试巡检', 'routine', ?, ?, 0)",
                id, tenantId, propertyId, "PT-JUNIT-" + id, planDate, status);
    }

    private void insertRepair(long id, long tenantId, long propertyId, String createTime, String completeTime) {
        jdbc.update("INSERT INTO fire_repair_order (id, tenant_id, property_id, order_no, fault_description, order_status, create_time, complete_time, deleted) "
                        + "VALUES (?, ?, ?, ?, 'JUnit 测试故障', 'pending', ?, ?, 0)",
                id, tenantId, propertyId, "RO-JUNIT-" + id, createTime, completeTime);
    }

    // ==================== 结果提取辅助 ====================

    /** [统计行] -> {分组键: count} */
    private Map<String, Integer> indexCount(List<Map<String, Object>> rows, String groupKey) {
        Map<String, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get(groupKey)), ((Number) row.get("count")).intValue());
        }
        return result;
    }

    /** 在按天分组的行中定位指定日期（plan_date/date 的 String 值形如 2026-01-05） */
    private Map<String, Object> findByKey(List<Map<String, Object>> rows, String column, String value) {
        return rows.stream()
                .filter(row -> value.equals(String.valueOf(row.get(column))))
                .findFirst()
                .orElseThrow(() -> new AssertionError("结果中缺少 " + column + " = " + value + " 的行"));
    }

}
