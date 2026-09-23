package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.DataCenterRespVO;
import com.enjoyiot.module.eiot.service.firewatch.FireDataCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 数据中心 Controller
 *
 * <p>注意：本模块全部查询接口目前为开发中状态，Service 为空壳实现（固定返回空集合/空对象占位），
 * 前端已同步展示「开发中」标识与示例数据说明，详见 data-center/index.vue。</p>
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 数据中心", description = "开发中：以下接口均为空壳占位，尚未实现真实统计逻辑")
@RestController
@RequestMapping("/eiot/firewatch/data-center")
@Validated
public class FireDataCenterController {

    @Resource
    private FireDataCenterService fireDataCenterService;

    // ==================== 告警分析 ====================

    @GetMapping("/alarm-trend")
    @Operation(summary = "获取告警趋势数据")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.AlarmTrendItem>> getAlarmTrend(
            @RequestParam(value = "days", defaultValue = "30") Integer days,
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getAlarmTrend(days, propertyId));
    }

    @GetMapping("/alarm-type-distribution")
    @Operation(summary = "获取告警类型分布")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.AlarmTypeItem>> getAlarmTypeDistribution(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getAlarmTypeDistribution(propertyId));
    }

    @GetMapping("/alarm-property-rank")
    @Operation(summary = "获取各物业告警排名")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.PropertyAlarmRankItem>> getPropertyAlarmRank(
            @RequestParam(value = "days", defaultValue = "30") Integer days) {
        return success(fireDataCenterService.getPropertyAlarmRank(days));
    }

    @GetMapping("/alarm-hourly-heatmap")
    @Operation(summary = "获取24h告警时段热力数据")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.HourlyHeatmapItem>> getAlarmHourlyHeatmap(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getAlarmHourlyHeatmap(propertyId));
    }

    @GetMapping("/alarm-level-distribution")
    @Operation(summary = "获取告警等级分布")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.AlarmLevelItem>> getAlarmLevelDistribution(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getAlarmLevelDistribution(propertyId));
    }

    // ==================== 设备分析 ====================

    @GetMapping("/device-status")
    @Operation(summary = "获取设备状态统计")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<DataCenterRespVO.DeviceStatusItem> getDeviceStatus(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getDeviceStatus(propertyId));
    }

    @GetMapping("/device-category")
    @Operation(summary = "获取设备类型分布")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.DeviceCategoryItem>> getDeviceCategoryDistribution(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getDeviceCategoryDistribution(propertyId));
    }

    @GetMapping("/device-property-distribution")
    @Operation(summary = "获取各物业设备数量分布")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.PropertyDeviceItem>> getPropertyDeviceDistribution() {
        return success(fireDataCenterService.getPropertyDeviceDistribution());
    }

    // ==================== 值班统计 ====================

    @GetMapping("/duty-stats")
    @Operation(summary = "获取值班统计摘要")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<DataCenterRespVO.DutyStatsItem> getDutyStats(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getDutyStats(propertyId));
    }

    @GetMapping("/duty-user-rank")
    @Operation(summary = "获取值班人员排班天数排名")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.UserDutyRankItem>> getUserDutyRank(
            @RequestParam(value = "year") Integer year,
            @RequestParam(value = "month") Integer month,
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getUserDutyRank(year, month, propertyId));
    }

    // ==================== 巡检统计 ====================

    @GetMapping("/patrol-stats")
    @Operation(summary = "获取巡检统计摘要")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<DataCenterRespVO.PatrolStatsItem> getPatrolStats(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getPatrolStats(propertyId));
    }

    @GetMapping("/patrol-trend")
    @Operation(summary = "获取巡检任务趋势")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.PatrolTrendItem>> getPatrolTrend(
            @RequestParam(value = "days", defaultValue = "30") Integer days,
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getPatrolTrend(days, propertyId));
    }

    // ==================== 工单统计 ====================

    @GetMapping("/repair-stats")
    @Operation(summary = "获取维修工单统计")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<DataCenterRespVO.RepairStatsItem> getRepairStats(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getRepairStats(propertyId));
    }

    @GetMapping("/hazard-stats")
    @Operation(summary = "获取隐患统计")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<DataCenterRespVO.HazardStatsItem> getHazardStats(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getHazardStats(propertyId));
    }

    @GetMapping("/work-order-trend")
    @Operation(summary = "获取工单趋势（维修+隐患）")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public CommonResult<List<DataCenterRespVO.WorkOrderTrendItem>> getWorkOrderTrend(
            @RequestParam(value = "days", defaultValue = "30") Integer days,
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getWorkOrderTrend(days, propertyId));
    }
}
