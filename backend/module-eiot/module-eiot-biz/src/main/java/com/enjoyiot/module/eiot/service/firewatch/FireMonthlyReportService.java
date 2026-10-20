package com.enjoyiot.module.eiot.service.firewatch;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 智慧消防周期运行报告（月/季/年）Service 接口
 *
 * @author EnjoyIot
 */
public interface FireMonthlyReportService {

    /**
     * 导出周期运行报告 PDF（月/季/年同一模板）
     *
     * @param periodType 周期类型：monthly/quarterly/yearly，为空默认 monthly
     * @param period     周期标识（yyyy-MM / yyyy-Qn / yyyy），为空时默认上一完整期
     * @param response   HTTP 响应
     */
    void exportReportPdf(String periodType, String period, HttpServletResponse response) throws IOException;
}
