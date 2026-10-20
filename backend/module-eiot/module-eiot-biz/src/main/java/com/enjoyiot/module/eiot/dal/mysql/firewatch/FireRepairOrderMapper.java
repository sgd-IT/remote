package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireRepairOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 维修工单 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireRepairOrderMapper extends BaseMapperX<FireRepairOrderDO> {

    /**
     * 按状态统计工单数（按租户过滤）
     */
    @Select("SELECT order_status, COUNT(*) as count FROM fire_repair_order " +
            "WHERE tenant_id = #{tenantId} AND deleted = 0 GROUP BY order_status")
    List<Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId);

    /**
     * 按状态统计所有工单数（不按租户过滤，用于管理员）
     */
    @Select("SELECT order_status, COUNT(*) as count FROM fire_repair_order " +
            "WHERE deleted = 0 GROUP BY order_status")
    List<Map<String, Object>> countAllByStatus();

    /**
     * 按天统计维修工单创建数与完成数（创建按 create_time 归日、完成按 complete_time 归日，UNION 后合并同一日期；
     * 用于数据中心"处置事项趋势"；租户过滤由拦截器自动追加）
     */
    @Select("<script>" +
            "SELECT d.date, SUM(d.created) as created_count, SUM(d.completed) as completed_count FROM (" +
            "  SELECT DATE(create_time) as date, 1 as created, 0 as completed FROM fire_repair_order " +
            "  WHERE deleted = 0 AND create_time >= #{startDateTime} AND create_time &lt; #{endDateTime} " +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if> " +
            "  UNION ALL " +
            "  SELECT DATE(complete_time) as date, 0 as created, 1 as completed FROM fire_repair_order " +
            "  WHERE deleted = 0 AND complete_time IS NOT NULL " +
            "  AND complete_time >= #{startDateTime} AND complete_time &lt; #{endDateTime} " +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if> " +
            ") d GROUP BY d.date ORDER BY d.date" +
            "</script>")
    List<Map<String, Object>> countTrendByDay(@Param("propertyId") Long propertyId,
                                              @Param("startDateTime") LocalDateTime startDateTime,
                                              @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 统计企业待处理工单数
     */
    @Select("SELECT COUNT(*) FROM fire_repair_order WHERE tenant_id = #{tenantId} " +
            "AND property_id = #{propertyId} AND order_status IN ('pending', 'assigned') AND deleted = 0")
    Integer countPendingByPropertyId(@Param("tenantId") Long tenantId,
                                      @Param("propertyId") Long propertyId);

    /**
     * 查询超期未排查的工单
     */
    @Select("SELECT * FROM fire_repair_order WHERE tenant_id = #{tenantId} " +
            "AND order_status IN ('pending', 'assigned') AND deadline IS NOT NULL " +
            "AND deadline < NOW() AND deleted = 0")
    List<FireRepairOrderDO> selectOverdueOrders(@Param("tenantId") Long tenantId);

    /**
     * 统计超期未完成的工单数（按租户过滤）
     */
    @Select("SELECT COUNT(*) FROM fire_repair_order WHERE tenant_id = #{tenantId} " +
            "AND order_status IN ('pending', 'assigned', 'in_progress') AND deadline IS NOT NULL " +
            "AND deadline < NOW() AND deleted = 0")
    Integer countOverdueOrders(@Param("tenantId") Long tenantId);

    /**
     * 统计超期未完成的工单总数（不按租户过滤，用于管理员）
     */
    @Select("SELECT COUNT(*) FROM fire_repair_order " +
            "WHERE order_status IN ('pending', 'assigned', 'in_progress') AND deadline IS NOT NULL " +
            "AND deadline < NOW() AND deleted = 0")
    Integer countAllOverdueOrders();

    /**
     * 查询所有超期未完成的工单（不按租户过滤，用于管理员）
     */
    @Select("SELECT * FROM fire_repair_order " +
            "WHERE order_status IN ('pending', 'assigned', 'in_progress') AND deadline IS NOT NULL " +
            "AND deadline < NOW() AND deleted = 0")
    List<FireRepairOrderDO> selectAllOverdueOrders();

    /**
     * 批量更新催办状态
     */
    @Update({"<script>",
             "UPDATE fire_repair_order SET urge_sent = 1, urge_time = NOW() ",
             "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
             "</script>"})
    void batchMarkUrgeSent(@Param("ids") List<Long> ids);

}
