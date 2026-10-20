package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;

import java.util.List;
import java.util.Map;

/**
 * 监控中心大屏 Service 接口
 *
 * @author EnjoyIot
 */
public interface FireDashboardService {

    /**
     * 获取监控大屏统计数据（当前租户）
     */
    DashboardStatisticsRespVO getDashboardStatistics(Long tenantId);

    /**
     * 获取值守端大屏统计数据（全租户，admin使用）
     */
    DashboardStatisticsRespVO getDutyScreenStatistics();

    /**
     * 获取设备状态统计
     */
    DeviceStatusStatisticsRespVO getDeviceStatusStatistics();

    /**
     * 获取今日警情统计
     */
    TodayAlarmStatisticsRespVO getTodayAlarmStatistics();

    /**
     * 获取实时告警滚动列表
     */
    List<AlarmOrderRespVO> getRealtimeAlarms(int limit);

    /**
     * 获取风险单位分布
     */
    List<DashboardStatisticsRespVO.RiskDistributionVO> getRiskDistribution();

    /**
     * 获取告警趋势数据
     */
    List<DashboardStatisticsRespVO.AlarmTrendVO> getAlarmTrend(int days);

    /**
     * 获取设备类型分布
     */
    List<DashboardStatisticsRespVO.DeviceCategoryVO> getDeviceCategoryDistribution();

    /**
     * 获取设备地理位置列表（用于地图展示）
     * admin可看全部，企业只看自己租户下的设备
     */
    List<Map<String, Object>> getDeviceLocations();

    /**
     * 获取设备列表（用于设备展示页，全量设备）
     * admin可看全部，企业只看自己租户下的设备
     */
    List<Map<String, Object>> getDeviceList();

}
