package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenancePlanDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 维保计划 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireMaintenancePlanMapper extends BaseMapperX<FireMaintenancePlanDO> {

    /**
     * 查询已逾期的计划（待执行或执行中且计划日期已过当前时间）
     */
    @Select("SELECT * FROM fire_maintenance_plan WHERE tenant_id = #{tenantId} " +
            "AND plan_status IN ('pending', 'executing') AND plan_date IS NOT NULL " +
            "AND plan_date < NOW() AND deleted = 0")
    List<FireMaintenancePlanDO> selectOverduePlans(@Param("tenantId") Long tenantId);

    /**
     * 统计已逾期的计划数
     */
    @Select("SELECT COUNT(*) FROM fire_maintenance_plan WHERE tenant_id = #{tenantId} " +
            "AND plan_status IN ('pending', 'executing') AND plan_date IS NOT NULL " +
            "AND plan_date < NOW() AND deleted = 0")
    Integer countOverduePlans(@Param("tenantId") Long tenantId);

    /**
     * 查询所有已逾期的计划（不按租户过滤，用于管理员）
     */
    @Select("SELECT * FROM fire_maintenance_plan " +
            "WHERE plan_status IN ('pending', 'executing') AND plan_date IS NOT NULL " +
            "AND plan_date < NOW() AND deleted = 0")
    List<FireMaintenancePlanDO> selectAllOverduePlans();

    /**
     * 批量更新催办状态
     */
    @Update({"<script>",
             "UPDATE fire_maintenance_plan SET urge_sent = 1, urge_time = NOW() ",
             "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
             "</script>"})
    void batchMarkUrgeSent(@Param("ids") List<Long> ids);
}
