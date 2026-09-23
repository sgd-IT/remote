package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireAlarmOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 火警处置工单 Mapper
 *
 * 租户隔离策略：
 * - 简单单表查询：由 MyBatis-Plus 租户拦截器自动添加 WHERE tenant_id = ?
 * - 多表 JOIN 查询：手动添加 AND o.tenant_id = #{tenantId}，避免拦截器误加到关联表
 *   admin 传 tenantId=null 时不加条件，企业租户传实际 tenantId 值
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireAlarmOrderMapper extends BaseMapperX<FireAlarmOrderDO> {

    @Select("SELECT COUNT(*) FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) AND DATE(alarm_time) = CURDATE()")
    Integer countTodayAlarms();

    @Select("SELECT COUNT(*) FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND DATE(alarm_time) = CURDATE() AND order_status IN ('confirmed', 'false_alarm', 'real_fire', 'closed')")
    Integer countTodayHandledAlarms();

    @Select("SELECT COUNT(*) FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) AND order_status = 'pending'")
    Integer countPendingAlarms();

    @Select("SELECT alarm_level, COUNT(*) as count FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND DATE(alarm_time) = CURDATE() GROUP BY alarm_level")
    List<Map<String, Object>> countTodayAlarmsByLevel();

    @Select("SELECT alarm_type, COUNT(*) as count FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND DATE(alarm_time) = CURDATE() GROUP BY alarm_type")
    List<Map<String, Object>> countTodayAlarmsByType();

    @Select("SELECT * FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) ORDER BY alarm_time DESC LIMIT #{limit}")
    List<FireAlarmOrderDO> selectLatestAlarms(@Param("limit") int limit);

    @Select("SELECT IFNULL(AVG(response_duration), 0) FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND DATE(alarm_time) = CURDATE() AND response_duration IS NOT NULL")
    Integer avgTodayResponseTime();

    @Select("SELECT DATE(alarm_time) as alarm_date, COUNT(*) as alarm_count, " +
            "SUM(CASE WHEN order_status IN ('confirmed', 'false_alarm', 'real_fire', 'closed') THEN 1 ELSE 0 END) as handle_count " +
            "FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND alarm_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(alarm_time) ORDER BY alarm_date")
    List<Map<String, Object>> countAlarmTrendByDay(@Param("days") int days);

    @Select("SELECT alarm_type, COUNT(*) as count FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) GROUP BY alarm_type")
    List<Map<String, Object>> countAlarmsByType();

    @Select("SELECT DATEDIFF(CURDATE(), MAX(alarm_date)) FROM (" +
            "SELECT DATE(alarm_time) as alarm_date FROM fire_alarm_order " +
            "WHERE (deleted = 0 OR deleted IS NULL) GROUP BY DATE(alarm_time)" +
            ") t ORDER BY alarm_date DESC LIMIT 1")
    Integer getSafeDays();

    @Select("SELECT COUNT(*) FROM fire_alarm_order WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND DATE(alarm_time) = CURDATE() AND order_status = 'false_alarm'")
    Integer countTodayFalseAlarms();

    /**
     * 分页查询火警工单，关联设备信息、产品、分类表获取设备类型
     * tenantId: null=不过滤(admin看全部), 非null=按租户过滤(企业只看自己)
     */
    @Select("<script>" +
            "SELECT o.*, c.name as device_type_name, c.id as device_type " +
            "FROM fire_alarm_order o " +
            "LEFT JOIN eiot_device_info d ON o.device_id = d.id AND (d.deleted = 0 OR d.deleted IS NULL) " +
            "LEFT JOIN eiot_product p ON d.product_key = p.product_key AND (p.deleted = 0 OR p.deleted IS NULL) " +
            "LEFT JOIN eiot_category c ON p.category_id = c.id AND (c.deleted = 0 OR c.deleted IS NULL) " +
            "WHERE (o.deleted = 0 OR o.deleted IS NULL) " +
            "<if test='tenantId != null'> AND o.tenant_id = #{tenantId} </if>" +
            "<if test='reqVO.orderNo != null and reqVO.orderNo != \"\"'> AND o.order_no = #{reqVO.orderNo} </if>" +
            "<if test='reqVO.propertyId != null'> AND o.property_id = #{reqVO.propertyId} </if>" +
            "<if test='reqVO.alarmType != null and reqVO.alarmType != \"\"'> AND o.alarm_type = #{reqVO.alarmType} </if>" +
            "<if test='reqVO.alarmLevel != null and reqVO.alarmLevel != \"\"'> AND o.alarm_level = #{reqVO.alarmLevel} </if>" +
            "<if test='reqVO.orderStatus != null and reqVO.orderStatus != \"\"'> AND o.order_status = #{reqVO.orderStatus} </if>" +
            "<if test='reqVO.alarmTimeBegin != null'> AND o.alarm_time &gt;= #{reqVO.alarmTimeBegin} </if>" +
            "<if test='reqVO.alarmTimeEnd != null'> AND o.alarm_time &lt;= #{reqVO.alarmTimeEnd} </if>" +
            " ORDER BY o.alarm_time DESC" +
            "</script>")
    List<Map<String, Object>> selectPageWithDeviceType(@Param("reqVO") com.enjoyiot.module.eiot.controller.admin.firewatch.vo.AlarmOrderPageReqVO reqVO,
                                                        @Param("tenantId") Long tenantId);

    /**
     * 查询满足条件的总记录数（用于分页）
     * tenantId: null=不过滤(admin看全部), 非null=按租户过滤(企业只看自己)
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM fire_alarm_order o " +
            "WHERE (o.deleted = 0 OR o.deleted IS NULL) " +
            "<if test='tenantId != null'> AND o.tenant_id = #{tenantId} </if>" +
            "<if test='reqVO.orderNo != null and reqVO.orderNo != \"\"'> AND o.order_no = #{reqVO.orderNo} </if>" +
            "<if test='reqVO.propertyId != null'> AND o.property_id = #{reqVO.propertyId} </if>" +
            "<if test='reqVO.alarmType != null and reqVO.alarmType != \"\"'> AND o.alarm_type = #{reqVO.alarmType} </if>" +
            "<if test='reqVO.alarmLevel != null and reqVO.alarmLevel != \"\"'> AND o.alarm_level = #{reqVO.alarmLevel} </if>" +
            "<if test='reqVO.orderStatus != null and reqVO.orderStatus != \"\"'> AND o.order_status = #{reqVO.orderStatus} </if>" +
            "<if test='reqVO.alarmTimeBegin != null'> AND o.alarm_time &gt;= #{reqVO.alarmTimeBegin} </if>" +
            "<if test='reqVO.alarmTimeEnd != null'> AND o.alarm_time &lt;= #{reqVO.alarmTimeEnd} </if>" +
            "</script>")
    Long selectPageWithDeviceTypeCount(@Param("reqVO") com.enjoyiot.module.eiot.controller.admin.firewatch.vo.AlarmOrderPageReqVO reqVO,
                                       @Param("tenantId") Long tenantId);

}
