package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 设备信息 Mapper（基于 eiot_device_info 表，仅用于大屏统计查询）
 *
 * eiot_device_info.state 字段说明：
 * - 0: 离线
 * - 1: 在线
 * - 3: 未激活
 * - 4: 禁用/故障
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireDeviceMapper {

    /** 设备总数 */
    @Select("SELECT COUNT(*) FROM eiot_device_info WHERE (deleted = 0 OR deleted IS NULL)")
    Integer countTotalDevices();

    /** 在线设备数(state=1) */
    @Select("SELECT COUNT(*) FROM eiot_device_info WHERE (deleted = 0 OR deleted IS NULL) AND state = 1")
    Integer countOnlineDevices();

    /** 离线设备数(state=0) */
    @Select("SELECT COUNT(*) FROM eiot_device_info WHERE (deleted = 0 OR deleted IS NULL) AND state = 0")
    Integer countOfflineDevices();

    /** 告警设备数（eiot_alert_record 无 device_id，暂返回0，待后续扩展） */
    @Select("SELECT 0")
    Integer countAlarmDevices();

    /** 故障设备数(state=4 禁用视为故障) */
    @Select("SELECT COUNT(*) FROM eiot_device_info WHERE (deleted = 0 OR deleted IS NULL) AND state = 4")
    Integer countFaultDevices();

    /** 未激活设备数(state=3) */
    @Select("SELECT COUNT(*) FROM eiot_device_info WHERE (deleted = 0 OR deleted IS NULL) AND state = 3")
    Integer countInactiveDevices();

    /** 按设备状态分组统计 */
    @Select("SELECT state, COUNT(*) as count FROM eiot_device_info WHERE (deleted = 0 OR deleted IS NULL) GROUP BY state")
    List<Map<String, Object>> countByStatus();

    /** 按设备分类分组统计(关联 eiot_product + eiot_device_info 获取设备数量) */
    @Select("SELECT c.id as category_id, c.name as category_name, COUNT(d.id) as device_count " +
            "FROM eiot_category c " +
            "LEFT JOIN eiot_product p ON p.category_id = c.id AND (p.deleted = 0 OR p.deleted IS NULL) " +
            "LEFT JOIN eiot_device_info d ON d.product_key = p.product_key AND (d.deleted = 0 OR d.deleted IS NULL) " +
            "WHERE (c.deleted = 0 OR c.deleted IS NULL) " +
            "GROUP BY c.id, c.name " +
            "ORDER BY c.name ASC")
    List<Map<String, Object>> countByCategory();

    /** 查询有经纬度的设备位置列表（用于地图展示） */
    @Select("SELECT id, name, dn, addr, lat, lon, state, product_key, tenant_id " +
            "FROM eiot_device_info " +
            "WHERE (deleted = 0 OR deleted IS NULL) AND lon IS NOT NULL AND lat IS NOT NULL " +
            "AND lon != 0 AND lat != 0")
    List<Map<String, Object>> selectDeviceLocations();

    /** 查询全部设备列表（用于设备展示页），含所属企业名称 */
    @Select("SELECT d.id, d.name, d.dn, d.state, d.addr, d.tenant_id, d.product_key, d.serial_no, " +
            "p.property_name " +
            "FROM eiot_device_info d " +
            "LEFT JOIN (" +
            "  SELECT tenant_id, GROUP_CONCAT(name SEPARATOR '/') AS property_name " +
            "  FROM fire_property WHERE (deleted = 0 OR deleted IS NULL) GROUP BY tenant_id" +
            ") p ON p.tenant_id = d.tenant_id " +
            "WHERE (d.deleted = 0 OR d.deleted IS NULL) " +
            "ORDER BY d.state ASC, d.name ASC")
    List<Map<String, Object>> selectDeviceList();

}
