package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePatrolPointDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 巡检点位 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FirePatrolPointMapper extends BaseMapperX<FirePatrolPointDO> {

    /**
     * 按任务ID查询点位
     */
    @Select("<script>" +
            "SELECT * FROM fire_patrol_point " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND task_id = #{taskId}" +
            "  AND deleted = 0" +
            "</where>" +
            "ORDER BY id" +
            "</script>")
    List<FirePatrolPointDO> selectByTaskId(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId);

    /**
     * 按任务ID统计点位结果
     */
    @Select("<script>" +
            "SELECT result, COUNT(*) as count FROM fire_patrol_point " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND task_id = #{taskId}" +
            "  AND deleted = 0" +
            "</where>" +
            "GROUP BY result" +
            "</script>")
    List<Map<String, Object>> countByResult(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId);

    /**
     * 按任务ID统计待检点位数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM fire_patrol_point " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND task_id = #{taskId}" +
            "  AND status = 'pending'" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    Integer countPendingPoints(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId);

    /**
     * 按任务ID统计已检点位数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM fire_patrol_point " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND task_id = #{taskId}" +
            "  AND status = 'completed'" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    Integer countCompletedPoints(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId);

    /**
     * 按任务ID统计正常点位数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM fire_patrol_point " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND task_id = #{taskId}" +
            "  AND result = 'normal'" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    Integer countNormalPoints(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId);

    /**
     * 按任务ID统计异常点位数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM fire_patrol_point " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  AND task_id = #{taskId}" +
            "  AND result = 'abnormal'" +
            "  AND deleted = 0" +
            "</where>" +
            "</script>")
    Integer countAbnormalPoints(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId);

}
