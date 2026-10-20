package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.module.eiot.service.firewatch.FireMonthlyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 智慧消防周期运行报告（月/季/年）Controller
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 智慧消防周期运行报告")
@RestController
@RequestMapping("/eiot/firewatch/monthly-report")
@Validated
public class FireMonthlyReportController {

    @Resource
    private FireMonthlyReportService fireMonthlyReportService;

    @GetMapping("/export-pdf")
    @Operation(summary = "导出周期运行报告 PDF", description = "按周期导出智慧消防运行报告 PDF（概览/告警分析/设备状态/重点明细/隐患整改），月/季/年同一模板")
    @Parameter(name = "periodType", description = "周期类型(monthly/quarterly/yearly)，为空默认 monthly")
    @Parameter(name = "period", description = "周期(yyyy-MM / yyyy-Qn / yyyy)，为空时默认上一完整期")
    @Parameter(name = "month", description = "（兼容旧参数）报告月份 yyyy-MM，等价于 periodType=monthly 时的 period")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:data-center:query')")
    public void exportReportPdf(
            @RequestParam(value = "periodType", required = false) String periodType,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "month", required = false) String month,
            HttpServletResponse response) throws IOException {
        String normalizedPeriod = (period != null && !period.isEmpty()) ? period : month;
        fireMonthlyReportService.exportReportPdf(periodType, normalizedPeriod, response);
    }
}
