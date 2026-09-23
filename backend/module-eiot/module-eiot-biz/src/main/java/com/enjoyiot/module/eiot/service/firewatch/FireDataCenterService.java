package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.DataCenterRespVO;

import java.util.List;

/**
 * 数据中心 Service 接口
 *
 * @author EnjoyIot
 */
public interface FireDataCenterService {

    /**
     * 获取告警趋势数据
     * @param days 查询天数（7/30/90）
     * @param propertyId 物业ID（可选，null表示全部）
     */
    List<DataCenterRespVO.AlarmTrendItem> getAlarmTrend(Integer days, Long propertyId);

    /**
     * 获取告警类型分布
     */
    List<DataCenterRespVO.AlarmTypeItem> getAlarmTypeDistribution(Long propertyId);

    /**
     * 获取各物业告警排名
     */
    List<DataCenterRespVO.PropertyAlarmRankItem> getPropertyAlarmRank(Integer days);

    /**
     * 获取24h告警时段热力数据
     */
    List<DataCenterRespVO.HourlyHeatmapItem> getAlarmHourlyHeatmap(Long propertyId);

    /**
     * 获取告警等级分布
     */
    List<DataCenterRespVO.AlarmLevelItem> getAlarmLevelDistribution(Long propertyId);

    /**
     * 获取设备状态统计
     */
    DataCenterRespVO.DeviceStatusItem getDeviceStatus(Long propertyId);

    /**
     * 获取设备类型分布
     */
    List<DataCenterRespVO.DeviceCategoryItem> getDeviceCategoryDistribution(Long propertyId);

    /**
     * 获取各物业设备数量分布
     */
    List<DataCenterRespVO.PropertyDeviceItem> getPropertyDeviceDistribution();

    /**
     * 获取值班统计摘要
     */
    DataCenterRespVO.DutyStatsItem getDutyStats(Long propertyId);

    /**
     * 获取值班人员排班天数排名
     */
    List<DataCenterRespVO.UserDutyRankItem> getUserDutyRank(Integer year, Integer month, Long propertyId);

    /**
     * 获取巡检统计摘要
     */
    DataCenterRespVO.PatrolStatsItem getPatrolStats(Long propertyId);

    /**
     * 获取巡检任务趋势
     */
    List<DataCenterRespVO.PatrolTrendItem> getPatrolTrend(Integer days, Long propertyId);

    /**
     * 获取维修工单统计
     */
    DataCenterRespVO.RepairStatsItem getRepairStats(Long propertyId);

    /**
     * 获取隐患统计
     */
    DataCenterRespVO.HazardStatsItem getHazardStats(Long propertyId);

    /**
     * 获取工单趋势（维修+隐患）
     */
    List<DataCenterRespVO.WorkOrderTrendItem> getWorkOrderTrend(Integer days, Long propertyId);
}
