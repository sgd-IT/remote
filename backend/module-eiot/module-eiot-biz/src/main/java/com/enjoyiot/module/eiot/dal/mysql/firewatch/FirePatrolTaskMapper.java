package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePatrolTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 巡检任务 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FirePatrolTaskMapper extends BaseMapperX<FirePatrolTaskDO> {

    /**
     * 按日期范围查询任务
     */
    @Select("<script>" +
            "SELECT * FROM fire_patrol_task " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND plan_date BETWEEN #{startDate} AND #{endDate}" +
            "  AND deleted = 0" +
            "</where>" +
            "ORDER BY plan_date DESC" +
            "</script>")
    List<FirePatrolTaskDO> selectByDateRange(@Param("tenantId") Long tenantId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * 查询某人的任务
     */
    @Select("<script>" +
            "SELECT * FROM fire_patrol_task " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND patrol_user_id = #{userId}" +
            "  AND status IN ('pending', 'in_progress')" +
            "  AND deleted = 0" +
            "</where>" +
            "ORDER BY plan_date" +
            "</script>")
    List<FirePatrolTaskDO> selectUserPendingTasks(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /**
     * 按状态统计任务
     */
    @Select("<script>" +
            "SELECT status, COUNT(*) as count FROM fire_patrol_task " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            "  AND plan_date BETWEEN #{startDate} AND #{endDate}" +
            "  AND deleted = 0" +
            "</where>" +
            "GROUP BY status" +
            "</script>")
    List<Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId,
                                            @Param("propertyId") Long propertyId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 按类型统计任务
     */
    @Select("<script>" +
            "SELECT task_type AS taskType, COUNT(*) as count FROM fire_patrol_task " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            "  AND plan_date BETWEEN #{startDate} AND #{endDate}" +
            "  AND deleted = 0" +
            "</where>" +
            "GROUP BY task_type" +
            "</script>")
    List<Map<String, Object>> countByType(@Param("tenantId") Long tenantId,
                                          @Param("propertyId") Long propertyId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * 统计今日待执行任务数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM fire_patrol_task " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND plan_date = CURDATE()" +
            "  AND status = 'pending'" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    Integer countTodayPendingTasks(@Param("tenantId") Long tenantId);

    /**
     * 统计已完成任务数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM fire_patrol_task " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND status = 'completed'" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    Integer countCompletedTasks(@Param("tenantId") Long tenantId);

    /**
     * 统计超期任务数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM fire_patrol_task " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND status = 'overdue'" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    Integer countOverdueTasks(@Param("tenantId") Long tenantId);

    /**
     * 批量标记超期任务状态
     * 将计划日期早于今天的日常巡检任务标记为超期
     * plan_date IS NULL 表示“每天”重复任务，不参与超期判定
     */
    @Update("<script>" +
            "UPDATE fire_patrol_task " +
            "<set>" +
            "  status = 'overdue'" +
            "</set>" +
            "<where>" +
            "  plan_date &lt; CURDATE()" +
            "  AND plan_date IS NOT NULL" +
            "  AND task_type = 'routine'" +
            "  AND status IN ('pending', 'in_progress')" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    void batchRefreshPartolTaskStatus();

    /**
     * 批量重置日常巡检任务状态
     */
    @Update("<script>" +
            "UPDATE fire_patrol_task " +
            "<set>" +
            "  status = 'pending'," +
            "  completed_points = 0," +
            "  normal_points = 0," +
            "  abnormal_points = 0," +
            "  progress = 0," +
            "  result = NULL," +
            "  summary = NULL," +
            "  actual_start_time = NULL," +
            "  actual_end_time = NULL" +
            "</set>" +
            "<where>" +
            "  task_type = 'routine'" +
            "  AND status IN ('pending', 'overdue', 'in_progress')" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    void batchResetRoutineTaskStatus();
}
