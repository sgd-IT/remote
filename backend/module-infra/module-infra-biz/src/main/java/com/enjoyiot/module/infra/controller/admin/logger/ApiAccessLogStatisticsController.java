package com.enjoyiot.module.infra.controller.admin.logger;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.infra.controller.admin.logger.vo.apiaccesslog.ApiAccessLogStatisticsRespVO;
import com.enjoyiot.module.infra.service.logger.ApiAccessLogStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * API 使用统计 Controller：给超管看各接口被调用的次数，用来判断哪些功能入口用得最多
 */
@Tag(name = "管理后台 - API 使用统计")
@RestController
@RequestMapping("/infra/api-access-log/statistics")
@Validated
public class ApiAccessLogStatisticsController {

    @Resource
    private ApiAccessLogStatisticsService apiAccessLogStatisticsService;

    @GetMapping
    @Operation(summary = "获得接口使用统计", description = "超级管理员专用：调用总量、每日趋势、接口与用户排行")
    @Parameter(name = "beginTime", description = "开始时间，格式 yyyy-MM-dd HH:mm:ss，默认最近 7 天")
    @Parameter(name = "endTime", description = "结束时间，格式 yyyy-MM-dd HH:mm:ss，默认当前时间")
    @Parameter(name = "top", description = "排行榜条数，默认 20，上限 100")
    @PreAuthorize("@ss.hasRole('super_admin')")
    public CommonResult<ApiAccessLogStatisticsRespVO> getStatistics(
            @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(value = "top", required = false, defaultValue = "20") Integer top) {
        return success(apiAccessLogStatisticsService.getStatistics(beginTime, endTime, top));
    }

}
