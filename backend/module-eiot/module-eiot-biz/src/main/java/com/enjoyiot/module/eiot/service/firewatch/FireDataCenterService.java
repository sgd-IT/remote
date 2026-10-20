package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.DataCenterRespVO;

import java.util.List;

/**
 * 数据中心 Service 接口
 *
 * <p>口径说明：时间窗口为"近 N 天"（含今天的 N 个自然日）；告警类型为双读口径
 * （COALESCE(alarm_kind, level)，1=火警 2=故障 3=隐患 4=离线 5=误报 6=信息）；
 * 告警/设备无 property_id，物业筛选按其所属租户近似（巡检/隐患/值班/工单按 property_id 精确）；
 * 超期口径：截至查询时刻仍未完成且期限已过（当前值时点指标）。
 * 与月度报告（自然月、导出用）的口径对照见 FireDataCenterServiceImpl 类头。</p>
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
     * 获取告警类型分布（近 N 天，双读按报警类型分组）
     * @param days 查询天数（7/30/90）
     * @param propertyId 物业ID（可选，null表示全部）
     */
    List<DataCenterRespVO.AlarmTypeItem> getAlarmTypeDistribution(Integer days, Long propertyId);

    /**
     * 获取各物业告警排名
     */
    List<DataCenterRespVO.PropertyAlarmRankItem> getPropertyAlarmRank(Integer days);

    /**
     * 获取24小时告警时段分布（近 N 天，按小时补零 0-23）
     * @param days 查询天数（7/30/90）
     * @param propertyId 物业ID（可选，null表示全部）
     */
    List<DataCenterRespVO.HourlyDistributionItem> getAlarmHourlyDistribution(Integer days, Long propertyId);

    /**
     * 获取告警档位分布（近 N 天，按确认档位 tier 1-4=Ⅰ-Ⅳ档，仅一级/火警事件有档位）
     * @param days 查询天数（7/30/90）
     * @param propertyId 物业ID（可选，null表示全部）
     */
    List<DataCenterRespVO.AlarmLevelItem> getAlarmLevelDistribution(Integer days, Long propertyId);

    /**
     * 获取设备状态统计（快照；仅 alarmDeviceCountInRange 与 normal 为近 N 天口径）
     * @param days 查询天数（7/30/90）
     * @param propertyId 物业ID（可选，null表示全部）
     */
    DataCenterRespVO.DeviceStatusItem getDeviceStatus(Integer days, Long propertyId);

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
     * 获取巡检统计摘要（近 N 天；超期=当前仍超期，即状态为 overdue 的任务）
     * @param days 查询天数（7/30/90）
     * @param propertyId 物业ID（可选，null表示全部）
     */
    DataCenterRespVO.PatrolStatsItem getPatrolStats(Integer days, Long propertyId);

    /**
     * 获取巡检任务趋势
     */
    List<DataCenterRespVO.PatrolTrendItem> getPatrolTrend(Integer days, Long propertyId);

    /**
     * 获取维修工单统计（当前状态快照；超期=排查截止时间已过且未完成）
     */
    DataCenterRespVO.RepairStatsItem getRepairStats(Long propertyId);

    /**
     * 获取隐患统计（近 N 天，按发现日期范围；超期=整改期限已过且未闭环/未关闭）
     * @param days 查询天数（7/30/90）
     * @param propertyId 物业ID（可选，null表示全部）
     */
    DataCenterRespVO.HazardStatsItem getHazardStats(Integer days, Long propertyId);

    /**
     * 获取处置事项趋势（近 N 天；维修工单与隐患合计：创建=维修新建+隐患新增，完成=维修完工+隐患闭环）
     * @param days 查询天数（7/30/90）
     * @param propertyId 物业ID（可选，null表示全部）
     */
    List<DataCenterRespVO.WorkOrderTrendItem> getWorkOrderTrend(Integer days, Long propertyId);
}
