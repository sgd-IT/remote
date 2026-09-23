package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 值班记录 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireDutyRecordMapper extends BaseMapperX<FireDutyRecordDO> {

    /**
     * 按排班ID查询记录
     */
    @Select("SELECT * FROM fire_duty_record WHERE tenant_id = #{tenantId} " +
            "AND schedule_id = #{scheduleId} AND deleted = 0 ORDER BY record_time DESC")
    List<FireDutyRecordDO> selectByScheduleId(@Param("tenantId") Long tenantId, @Param("scheduleId") Long scheduleId);

    /**
     * 按时间范围查询记录
     */
    @Select("SELECT * FROM fire_duty_record WHERE tenant_id = #{tenantId} " +
            "AND record_time BETWEEN #{startTime} AND #{endTime} AND deleted = 0 " +
            "ORDER BY record_time DESC")
    List<FireDutyRecordDO> selectByTimeRange(@Param("tenantId") Long tenantId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 按记录类型统计
     */
    @Select("SELECT record_type, COUNT(*) as count FROM fire_duty_record " +
            "WHERE tenant_id = #{tenantId} AND record_time BETWEEN #{startTime} AND #{endTime} " +
            "AND deleted = 0 GROUP BY record_type")
    List<Map<String, Object>> countByType(@Param("tenantId") Long tenantId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 按状态统计
     */
    @Select("SELECT status, COUNT(*) as count FROM fire_duty_record " +
            "WHERE tenant_id = #{tenantId} AND record_time BETWEEN #{startTime} AND #{endTime} " +
            "AND deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

}
