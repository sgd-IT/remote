
/*
 *
 *  * | Licensed 未经许可不能去掉「Enjoy-iot」相关版权
 *  * +----------------------------------------------------------------------
 *  * | Author: xw2sy@163.com | Tel: 19918996474
 *  * +----------------------------------------------------------------------
 *
 *  Copyright [2025] [Enjoy-iot] | Tel: 19918996474
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.enjoyiot.module.eiot.dal.mysql;


import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.enjoyiot.module.eiot.api.alert.dto.AlertRecord;
import com.enjoyiot.module.eiot.controller.admin.alertconfig.vo.AlertRecordPageReq;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.dataobject.product.ProductDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 告警记录 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface AlertRecordMapper extends BaseMapperX<AlertRecordDO> {

    default PageResult<AlertRecord> selectPage(AlertRecordPageReq reqVO) {
        MPJLambdaWrapperX<AlertRecordDO> query = new MPJLambdaWrapperX<AlertRecordDO>()
                .selectAll(AlertRecordDO.class)
                .selectAs(EiotDeviceInfoDO::getName, AlertRecord::getDeviceName)
                .selectAs(EiotDeviceInfoDO::getAddr, AlertRecord::getDeviceAddr)
                .selectAs(ProductDO::getName, AlertRecord::getProductName)
                .eqIfPresent(AlertRecordDO::getLevel, reqVO.getLevel())
                .likeIfPresent(AlertRecordDO::getName, reqVO.getName())
                .eqIfPresent(AlertRecordDO::getReadFlg, reqVO.getReadFlg())
                .eqIfPresent(AlertRecordDO::getDeviceId, reqVO.getDeviceId())
                .eqIfPresent(AlertRecordDO::getProductKey, reqVO.getProductKey())
                .orderByDesc(AlertRecordDO::getId);
        query.leftJoin(EiotDeviceInfoDO.class, EiotDeviceInfoDO::getId, AlertRecordDO::getDeviceId);
        query.leftJoin(ProductDO.class, ProductDO::getProductKey, AlertRecordDO::getProductKey);
        return selectJoinPage(reqVO, AlertRecord.class, query);
    }

    /**
     * 查询指定设备+告警名称的最新活动告警（未到处置终态：handle_status 为 0/1/2 或 NULL）
     */
    default AlertRecordDO selectActiveAlert(Long deviceId, String alertName) {
        return selectOne(new LambdaQueryWrapperX<AlertRecordDO>()
                .eq(AlertRecordDO::getDeviceId, deviceId)
                .eq(AlertRecordDO::getName, alertName)
                .and(w -> w.isNull(AlertRecordDO::getHandleStatus)
                        .or().lt(AlertRecordDO::getHandleStatus, 3))
                .orderByDesc(AlertRecordDO::getAlertTime)
                .last("LIMIT 1"));
    }

    // ==================== 大屏统计方法 ====================

    /** 今日告警总数 */
    @Select("SELECT COUNT(*) FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND DATE(FROM_UNIXTIME(alert_time / 1000)) = CURDATE()")
    Integer countTodayAlerts();

    /** 今日已读告警数（已读视为已处理） */
    @Select("SELECT COUNT(*) FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND DATE(FROM_UNIXTIME(alert_time / 1000)) = CURDATE() AND read_flg = 1")
    Integer countTodayReadAlerts();

    /** 今日未读告警数（未读视为待处理） */
    @Select("SELECT COUNT(*) FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND DATE(FROM_UNIXTIME(alert_time / 1000)) = CURDATE() " +
            "AND (read_flg = 0 OR read_flg IS NULL)")
    Integer countTodayUnreadAlerts();

    /** 未读告警数（未读视为待处理） */
    @Select("SELECT COUNT(*) FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND (read_flg = 0 OR read_flg IS NULL)")
    Integer countUnreadAlerts();

    /** 今日按告警等级分组统计 */
    @Select("SELECT level, COUNT(*) as count FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND DATE(FROM_UNIXTIME(alert_time / 1000)) = CURDATE() GROUP BY level")
    List<Map<String, Object>> countTodayAlertsByLevel();

    /** 最新告警列表（关联设备信息，供大屏语音播报/展示使用） */
    @Select("SELECT a.*, d.name AS device_name " +
            "FROM eiot_alert_record a " +
            "LEFT JOIN eiot_device_info d ON d.id = a.device_id " +
            "WHERE (a.deleted = 0 OR a.deleted IS NULL) " +
            "ORDER BY a.alert_time DESC LIMIT #{limit}")
    List<AlertRecordDO> selectLatestAlerts(@Param("limit") int limit);

    /**
     * 统计指定天数内的报警趋势
     * 按日期分组统计每天的报警数量和已读数量
     *
     * @param days 天数
     * @return 报警趋势数据列表，包含 date(日期), alarm_count(报警数量), handle_count(已处置数量)
     */
    @Select("SELECT " +
            "DATE(FROM_UNIXTIME(alert_time / 1000)) as alarm_date, " +
            "COUNT(*) as alarm_count, " +
            "SUM(CASE WHEN read_flg = 1 THEN 1 ELSE 0 END) as handle_count " +
            "FROM eiot_alert_record " +
            "WHERE (deleted = 0 OR deleted IS NULL) " +
            "AND alert_time >= UNIX_TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL #{days} DAY)) * 1000 " +
            "GROUP BY DATE(FROM_UNIXTIME(alert_time / 1000)) " +
            "ORDER BY alarm_date")
    List<Map<String, Object>> countAlarmTrendByDay(@Param("days") int days);

    /** 按告警等级分组统计（全量） */
    @Select("SELECT level, COUNT(*) as count FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) GROUP BY level")
    List<Map<String, Object>> countAlertsByLevel();

    /** 安全天数（距最后一次告警的天数） */
    @Select("SELECT DATEDIFF(CURDATE(), MAX(alarm_date)) FROM (" +
            "SELECT DATE(FROM_UNIXTIME(alert_time / 1000)) as alarm_date FROM eiot_alert_record " +
            "WHERE (deleted = 0 OR deleted IS NULL) GROUP BY DATE(FROM_UNIXTIME(alert_time / 1000))" +
            ") t ORDER BY alarm_date DESC LIMIT 1")
    Integer getSafeDays();

    // ==================== 报警记录页统计方法 ====================

    /** 报警总数（全量） */
    @Select("SELECT COUNT(*) FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) limit 1000")
    Integer countTotalAlerts();

    /** 已处置报警数（已读视为已处置） */
    @Select("SELECT COUNT(*) FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) AND read_flg = 1")
    Integer countReadAlerts();

    /** 按告警等级分组统计（全量，含数量） */
    @Select("SELECT level, COUNT(*) as count FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) GROUP BY level")
    List<Map<String, Object>> countAlertsByLevelAll();

    /** 按告警等级+已读状态分组统计（用于按类型计算处置率） */
    @Select("SELECT level, read_flg as readFlg, COUNT(*) as count FROM eiot_alert_record WHERE (deleted = 0 OR deleted IS NULL) GROUP BY level, read_flg")
    List<Map<String, Object>> countAlertsByLevelAndReadStatus();

    /** 查询近 N 分钟内未处置的告警（关联设备、产品、租户信息） */
    @Select("SELECT a.*, d.name AS device_name, d.addr AS device_addr, p.name AS product_name, t.name AS tenant_name " +
            "FROM eiot_alert_record a " +
            "LEFT JOIN eiot_device_info d ON d.id = a.device_id " +
            "LEFT JOIN eiot_product p ON p.product_key = a.product_key " +
            "LEFT JOIN system_tenant t ON t.id = a.tenant_id " +
            "WHERE (a.deleted = 0 OR a.deleted IS NULL) " +
            "AND ((a.handle_status IS NULL AND (a.read_flg = 0 OR a.read_flg IS NULL)) OR a.handle_status < 2) " +
            "AND a.alert_time >= #{threshold} " +
            "ORDER BY a.alert_time DESC")
    List<AlertRecord> selectRecentUnprocessedAlerts(@Param("threshold") long threshold);

    /** 查询指定父设备（含其子设备）近 N 分钟内未处置的告警 */
    @Select("<script>" +
            "SELECT a.*, d.name AS device_name, d.addr AS device_addr, p.name AS product_name, t.name AS tenant_name " +
            "FROM eiot_alert_record a " +
            "LEFT JOIN eiot_device_info d ON d.id = a.device_id " +
            "LEFT JOIN eiot_product p ON p.product_key = a.product_key " +
            "LEFT JOIN system_tenant t ON t.id = a.tenant_id " +
            "WHERE (a.deleted = 0 OR a.deleted IS NULL) " +
            "AND ((a.handle_status IS NULL AND (a.read_flg = 0 OR a.read_flg IS NULL)) OR a.handle_status &lt; 2) " +
            "AND a.alert_time >= #{threshold} " +
            "AND (a.device_id = #{parentDeviceId} " +
            "OR a.device_id IN (SELECT id FROM eiot_device_info WHERE parent_id = #{parentDeviceId} " +
            "AND (visible IS NULL OR visible = 1))) " +
            "ORDER BY a.alert_time DESC" +
            "</script>")
    List<AlertRecord> selectUnprocessedAlertsByParentDeviceId(@Param("parentDeviceId") Long parentDeviceId, @Param("threshold") long threshold);

    /**
     *
     */
}
