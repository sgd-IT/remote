package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.service.firewatch.FireDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 监控中心大屏 Controller
 *
 * /eiot/firewatch/dashboard/*   - 监控中心大屏（Service层通过 isSystemTenant() 实现租户感知：admin看全部，企业看自己）
 * /eiot/firewatch/duty-screen/* - 值守端大屏（Service层通过 isSystemTenant() 实现租户感知：admin看全部，企业看自己）
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 监控中心大屏")
@RestController
@RequestMapping("/eiot/firewatch")
@Validated
public class FireDashboardController {

    @Resource
    private FireDashboardService fireDashboardService;

    // ==================== 监控中心大屏 ====================

    @GetMapping("/dashboard/statistics")
    @Operation(summary = "获取监控大屏统计数据")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<DashboardStatisticsRespVO> getDashboardStatistics() {
        return success(fireDashboardService.getDashboardStatistics());
    }

    @GetMapping("/dashboard/device-status")
    @Operation(summary = "获取设备状态统计")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<DeviceStatusStatisticsRespVO> getDeviceStatusStatistics() {
        return success(fireDashboardService.getDeviceStatusStatistics());
    }

    @GetMapping("/dashboard/today-alarm")
    @Operation(summary = "获取今日警情统计")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<TodayAlarmStatisticsRespVO> getTodayAlarmStatistics() {
        return success(fireDashboardService.getTodayAlarmStatistics());
    }

    @GetMapping("/dashboard/realtime-alarms")
    @Operation(summary = "获取实时告警列表")
    @Parameter(name = "limit", description = "数量限制", example = "10")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<List<AlarmOrderRespVO>> getRealtimeAlarms(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return success(fireDashboardService.getRealtimeAlarms(limit));
    }

    @GetMapping("/dashboard/risk-distribution")
    @Operation(summary = "获取风险单位分布")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<List<DashboardStatisticsRespVO.RiskDistributionVO>> getRiskDistribution() {
        return success(fireDashboardService.getRiskDistribution());
    }

    @GetMapping("/dashboard/alarm-trend")
    @Operation(summary = "获取告警趋势数据")
    @Parameter(name = "days", description = "天数", example = "7")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<List<DashboardStatisticsRespVO.AlarmTrendVO>> getAlarmTrend(
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        return success(fireDashboardService.getAlarmTrend(days));
    }

    @GetMapping("/dashboard/device-category")
    @Operation(summary = "获取设备类型分布")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<List<DashboardStatisticsRespVO.DeviceCategoryVO>> getDeviceCategoryDistribution() {
        return success(fireDashboardService.getDeviceCategoryDistribution());
    }

    @GetMapping("/dashboard/device-locations")
    @Operation(summary = "获取设备地理位置列表（用于地图展示）")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<List<Map<String, Object>>> getDeviceLocations() {
        return success(fireDashboardService.getDeviceLocations());
    }

    @GetMapping("/dashboard/device-list")
    @Operation(summary = "获取设备列表（用于设备展示页）")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<List<Map<String, Object>>> getDeviceList() {
        return success(fireDashboardService.getDeviceList());
    }

    // ==================== 值守端大屏（admin，全租户）====================

    @GetMapping("/duty-screen/statistics")
    @Operation(summary = "获取值守端大屏统计数据")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<DashboardStatisticsRespVO> getDutyScreenStatistics() {
        return success(fireDashboardService.getDutyScreenStatistics());
    }

}
