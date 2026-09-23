package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireRepairOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
