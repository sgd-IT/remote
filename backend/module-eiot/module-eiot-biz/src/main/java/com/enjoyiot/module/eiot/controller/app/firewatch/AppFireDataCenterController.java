package com.enjoyiot.module.eiot.controller.app.firewatch;

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
 * 用户 App - 数据中心（移动端入口）
 *
 * 直调 Service（与 PC 共用实现）：若经 admin 版 Controller 转发，方法级安全会触发它自己的
 * @PreAuthorize 二次拦截（要求菜单权限点），移动端账号会被挡下。
 * 权限口径：账号允许移动端即可访问（system_users.user_type 含移动位，即 2/3），不校验菜单权限点。
 */
@Tag(name = "用户 App - 数据中心")
@RestController
@RequestMapping("/eiot/firewatch/data-center")
@Validated
public class AppFireDataCenterController {

    @Resource
    private FireDataCenterService fireDataCenterService;

    @GetMapping("/alarm-trend")
    @Operation(summary = "获取告警趋势数据")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<List<DataCenterRespVO.AlarmTrendItem>> getAlarmTrend(
            @RequestParam(value = "days", defaultValue = "30") Integer days,
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getAlarmTrend(days, propertyId));
    }

    @GetMapping("/alarm-type-distribution")
    @Operation(summary = "获取告警类型分布（近 N 天）")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<List<DataCenterRespVO.AlarmTypeItem>> getAlarmTypeDistribution(
            @RequestParam(value = "days", defaultValue = "30") Integer days,
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getAlarmTypeDistribution(days, propertyId));
    }

    @GetMapping("/hazard-stats")
    @Operation(summary = "获取隐患统计（近 N 天，按发现日期）")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<DataCenterRespVO.HazardStatsItem> getHazardStats(
            @RequestParam(value = "days", defaultValue = "30") Integer days,
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getHazardStats(days, propertyId));
    }

    @GetMapping("/duty-stats")
    @Operation(summary = "获取值班统计摘要")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<DataCenterRespVO.DutyStatsItem> getDutyStats(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDataCenterService.getDutyStats(propertyId));
    }

}
