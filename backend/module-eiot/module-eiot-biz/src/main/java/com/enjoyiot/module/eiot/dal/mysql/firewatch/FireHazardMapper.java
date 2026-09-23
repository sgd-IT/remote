package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireHazardDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 隐患管理 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireHazardMapper extends BaseMapperX<FireHazardDO> {

    /**
     * 按等级统计隐患
     */
    @Select("<script>" +
            "SELECT hazard_level, COUNT(*) as count FROM fire_hazard " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            "  AND found_date BETWEEN #{startDate} AND #{endDate}" +
            "  AND deleted = 0" +
            "</where>" +
            "GROUP BY hazard_level" +
            "</script>")
    List<Map<String, Object>> countByLevel(@Param("tenantId") Long tenantId,
                                           @Param("propertyId") Long propertyId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 按状态统计隐患
     */
    @Select("<script>" +
            "SELECT status, COUNT(*) as count FROM fire_hazard " +
            "<where>" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            "  AND found_date BETWEEN #{startDate} AND #{endDate}" +
            "  AND deleted = 0" +
            "</where>" +
            "GROUP BY status" +
            "</script>")
    List<Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId,
                                            @Param("propertyId") Long propertyId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 按日期统计隐患趋势（新增和闭环）
     */
    @Select("<script>" +
            "SELECT date_stat.date, " +
            "  COALESCE(new_cnt.new_count, 0) as newCount, " +
            "  COALESCE(closed_cnt.closed_count, 0) as closedCount " +
            "FROM (" +
            "  SELECT DISTINCT found_date AS date FROM fire_hazard " +
            "  WHERE found_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            "  UNION " +
            "  SELECT DISTINCT DATE(review_time) FROM fire_hazard " +
            "  WHERE review_time BETWEEN #{startDate} AND #{endDate} AND deleted = 0" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            ") date_stat " +
            "LEFT JOIN (" +
            "  SELECT found_date, COUNT(*) AS new_count FROM fire_hazard " +
            "  WHERE found_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            "  GROUP BY found_date" +
            ") new_cnt ON date_stat.date = new_cnt.found_date " +
            "LEFT JOIN (" +
            "  SELECT DATE(review_time) AS review_date, COUNT(*) AS closed_count FROM fire_hazard " +
            "  WHERE status = 'closed' AND review_time BETWEEN #{startDate} AND #{endDate} AND deleted = 0" +
            "  <if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "  <if test='propertyId != null'>AND property_id = #{propertyId}</if>" +
            "  GROUP BY DATE(review_time)" +
            ") closed_cnt ON date_stat.date = closed_cnt.review_date " +
            "ORDER BY date_stat.date" +
            "</script>")
    List<Map<String, Object>> countTrend(@Param("tenantId") Long tenantId,
                                         @Param("propertyId") Long propertyId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

}
