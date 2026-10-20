package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyScheduleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 值班管理 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireDutyScheduleMapper extends BaseMapperX<FireDutyScheduleDO> {

    /**
     * 班次结束时刻表达式：夜班（start_time >= end_time）跨天到次日，
     * 所有“班次是否已结束”的判定均必须复用此常量，否则会出现凌晨判定偏差
     */
    String SHIFT_END = "TIMESTAMP(IF(start_time >= end_time, DATE_ADD(schedule_date, INTERVAL 1 DAY), schedule_date), end_time)";

    /**
     * “班次已结束”条件。写法上避开 < 号，以便同一常量既能用于 @Select 的 XML 脚本、也能用于 Wrapper.apply
     */
    String SHIFT_ENDED = "NOW() > " + SHIFT_END;

    /**
     * 展示态口径：班次一旦结束，scheduled 未签到归 absent、on_duty 未签退归 early_leave，
     * 使统计桶互斥且与前端 cardStatus / 列表筛选一致
     */
    String DERIVED_STATUS = "CASE WHEN status = 'scheduled' AND check_in_time IS NULL AND " + SHIFT_ENDED + " THEN 'absent'" +
            " WHEN status = 'on_duty' AND check_out_time IS NULL AND " + SHIFT_ENDED + " THEN 'early_leave'" +
            " ELSE status END";

    /**
     * 按日期范围查询排班
     * tenantId 为 null 时不按租户过滤（用于跨租户查询）
     */
    @Select("<script>" +
            "SELECT * FROM fire_duty_schedule " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND schedule_date BETWEEN #{startDate} AND #{endDate}" +
            "  AND deleted = 0" +
            "</where>" +
            "ORDER BY schedule_date, shift_type" +
            "</script>")
    List<FireDutyScheduleDO> selectByDateRange(@Param("tenantId") Long tenantId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    /**
     * 查询指定日期的排班
     * tenantId 为 null 时不按租户过滤（用于跨租户查询）
     */
    @Select("<script>" +
            "SELECT * FROM fire_duty_schedule " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND schedule_date = #{date}" +
            "  AND deleted = 0" +
            "</where>" +
            "ORDER BY shift_type" +
            "</script>")
    List<FireDutyScheduleDO> selectByDate(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    /**
     * 查询用户在某日期范围内的排班
     * tenantId 为 null 时不按租户过滤（用于跨租户查询）
     */
    @Select("<script>" +
            "SELECT * FROM fire_duty_schedule " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND duty_user_id = #{userId}" +
            "  AND schedule_date BETWEEN #{startDate} AND #{endDate}" +
            "  AND deleted = 0" +
            "</where>" +
            "ORDER BY schedule_date" +
            "</script>")
    List<FireDutyScheduleDO> selectByUserAndDateRange(@Param("tenantId") Long tenantId,
                                                       @Param("userId") Long userId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    /**
     * 统计排班状态分布（按 DERIVED_STATUS 现场推导，含尚未落库纠正的历史数据）
     * tenantId 为 null 时不按租户过滤（用于跨租户查询）
     */
    @Select("<script>" +
            "SELECT " + DERIVED_STATUS + " AS status, COUNT(*) as count FROM fire_duty_schedule " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            "  <if test='startDate != null and endDate != null'>AND schedule_date BETWEEN #{startDate} AND #{endDate}</if>" +
            "  AND deleted = 0" +
            "</where>" +
            "GROUP BY 1" +
            "</script>")
    List<Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId,
                                            @Param("propertyId") Long propertyId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 统计某人某月的值班天数
     * tenantId 为 null 时不按租户过滤（用于跨租户查询）
     */
    @Select("<script>" +
            "SELECT COUNT(DISTINCT schedule_date) FROM fire_duty_schedule " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND duty_user_id = #{userId}" +
            "  AND YEAR(schedule_date) = #{year}" +
            "  AND MONTH(schedule_date) = #{month}" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    Integer countDutyDays(@Param("tenantId") Long tenantId,
                          @Param("userId") Long userId,
                          @Param("year") int year,
                          @Param("month") int month);

    /**
     * 指定日期范围内准时签到数（签到时间不晚于班次开始时刻；夜班跨天只影响结束时刻，不影响准时判定）
     * 租户过滤由拦截器自动追加，propertyId 为 null 时不按物业过滤
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM fire_duty_schedule " +
            "<where>" +
            "  AND check_in_time IS NOT NULL" +
            "  AND check_in_time &lt;= TIMESTAMP(schedule_date, start_time)" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            "  AND schedule_date BETWEEN #{startDate} AND #{endDate}" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    Integer countOnTimeCheckIn(@Param("propertyId") Long propertyId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    /**
     * 查询当前值班人员
     * tenantId 为 null 时不按租户过滤（用于跨租户查询）
     */
    @Select("<script>" +
            "SELECT * FROM fire_duty_schedule " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND schedule_date = CURDATE()" +
            "  AND status IN ('on_duty', 'scheduled')" +
            // “当前值班”只看尚未结束的班次，否则过点未签退的早退班会一直算值班中
            "  AND NOT (" + SHIFT_ENDED + ")" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    List<FireDutyScheduleDO> selectCurrentDuty(@Param("tenantId") Long tenantId);

}
