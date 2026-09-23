package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.excel.core.util.ExcelUtils;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.service.firewatch.FireDutyScheduleService;
import com.enjoyiot.module.eiot.service.firewatch.FirePropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 值班管理管理 Controller
 *
 * 功能概述：
 * - 排班计划管理：支持日历视图和列表视图，快速安排值班人员
 * - 班次设置：白班、夜班、全天班等多种班次类型
 * - 自动排班：按规则自动生成排班计划
 * - 签到签退：支持人脸识别、刷卡、手动签到
 * - 换班管理：支持换班申请和审批
 * - 值班统计：统计值班时长、出勤率等
 *
 */
@Tag(name = "管理后台 - 值班管理管理")
@RestController
@RequestMapping("/eiot/firewatch/duty-schedule")
@Validated
public class FireDutyScheduleController {

    @Resource
    private FireDutyScheduleService fireDutyScheduleService;

    @Resource
    private FirePropertyService firePropertyService;

    @PostMapping("/create")
    @Operation(summary = "创建排班", description = "创建值班管理记录")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:create')")
    public CommonResult<Long> createSchedule(@Valid @RequestBody FireDutyScheduleCreateReqVO createReqVO) {
        return success(fireDutyScheduleService.createSchedule(createReqVO));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建排班", description = "批量创建值班管理记录")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:create')")
    public CommonResult<Integer> batchCreateSchedule(@Valid @RequestBody List<FireDutyScheduleCreateReqVO> createReqVOList) {
        return success(fireDutyScheduleService.batchCreateSchedule(createReqVOList));
    }

    @PutMapping("/update")
    @Operation(summary = "更新排班", description = "更新值班管理信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:update')")
    public CommonResult<Boolean> updateSchedule(@Valid @RequestBody FireDutyScheduleUpdateReqVO updateReqVO) {
        fireDutyScheduleService.updateSchedule(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除排班", description = "删除值班管理记录")
    @Parameter(name = "id", description = "排班ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:delete')")
    public CommonResult<Boolean> deleteSchedule(@RequestParam("id") Long id) {
        fireDutyScheduleService.deleteSchedule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取排班详情", description = "获取值班管理详细信息")
    @Parameter(name = "id", description = "排班ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<FireDutyScheduleRespVO> getSchedule(@RequestParam("id") Long id) {
        return success(fireDutyScheduleService.getSchedule(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询排班列表", description = "分页查询值班管理列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<PageResult<FireDutyScheduleRespVO>> pageSchedules(@Valid FireDutySchedulePageReqVO pageReqVO) {
        return success(fireDutyScheduleService.pageSchedules(pageReqVO));
    }

    @GetMapping("/list-by-date-range")
    @Operation(summary = "按日期范围查询排班", description = "获取指定日期范围的排班列表")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "startDate", description = "开始日期", required = true)
    @Parameter(name = "endDate", description = "结束日期", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<List<FireDutyScheduleRespVO>> listSchedulesByDateRange(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return success(fireDutyScheduleService.listSchedulesByDateRange(propertyId, startDate, endDate));
    }

    @GetMapping("/list-by-date")
    @Operation(summary = "按日期查询排班", description = "获取指定日期的排班列表")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "date", description = "日期", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<List<FireDutyScheduleRespVO>> listSchedulesByDate(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam("date") LocalDate date) {
        return success(fireDutyScheduleService.listSchedulesByDate(propertyId, date));
    }

    @GetMapping("/current-duty")
    @Operation(summary = "获取当前值班人员", description = "获取当前正在值班的人员列表")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "tenantId", description = "租户ID（与propertyId二选一，自动转换为物业ID后过滤）")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<List<FireDutyScheduleRespVO>> getCurrentDuty(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        // 未传物业ID时，尝试按租户ID定位物业
        if (propertyId == null && tenantId != null) {
            FirePropertyRespVO property = firePropertyService.getPropertyByTenantId(tenantId);
            propertyId = property != null ? property.getId() : null;
        }
        return success(fireDutyScheduleService.getCurrentDuty(propertyId));
    }

    @PutMapping("/check-in")
    @Operation(summary = "签到", description = "值班人员签到（仅限当前租户）")
    @Parameter(name = "id", description = "排班ID", required = true)
    @Parameter(name = "checkInType", description = "签到方式: manual-手动, face-人脸, card-刷卡", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:operate')")
    public CommonResult<Boolean> checkIn(
            @RequestParam("id") Long id,
            @RequestParam("checkInType") String checkInType) {
        fireDutyScheduleService.checkIn(id, checkInType);
        return success(true);
    }

    // 与 check-in 保持一致用 PUT：前端（PC 工作台 / 移动端）均按 PUT 调用
    @PutMapping("/check-out")
    @Operation(summary = "签退", description = "值班人员签退（仅限当前租户）")
    @Parameter(name = "id", description = "排班ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:operate')")
    public CommonResult<Boolean> checkOut(@RequestParam("id") Long id) {
        fireDutyScheduleService.checkOut(id);
        return success(true);
    }

    @PutMapping("/change-duty")
    @Operation(summary = "换班", description = "申请换班（仅限当前租户）")
    @Parameter(name = "id", description = "排班ID", required = true)
    @Parameter(name = "newUserId", description = "新值班人ID", required = true)
    @Parameter(name = "reason", description = "换班原因")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:operate')")
    public CommonResult<Boolean> changeDuty(
            @RequestParam("id") Long id,
            @RequestParam("newUserId") Long newUserId,
            @RequestParam(value = "reason", required = false) String reason) {
        fireDutyScheduleService.changeDuty(id, newUserId, reason);
        return success(true);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取排班统计", description = "获取排班统计数据，支持按日期范围筛选")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "startDate", description = "开始日期（yyyy-MM-dd），不传时统计全部")
    @Parameter(name = "endDate", description = "结束日期（yyyy-MM-dd），不传时统计全部")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<Map<String, Object>> getScheduleStatistics(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        return success(fireDutyScheduleService.getScheduleStatistics(propertyId, start, end));
    }

    @GetMapping("/user-duty-days")
    @Operation(summary = "获取用户值班天数", description = "获取某用户某月的值班天数")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @Parameter(name = "year", description = "年份", required = true)
    @Parameter(name = "month", description = "月份", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<Integer> getUserDutyDays(
            @RequestParam("userId") Long userId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        return success(fireDutyScheduleService.getUserDutyDays(userId, year, month));
    }

    @PostMapping("/auto-schedule")
    @Operation(summary = "自动排班", description = "按规则自动生成排班计划")
    @Parameter(name = "propertyId", description = "物业ID", required = true)
    @Parameter(name = "year", description = "年份", required = true)
    @Parameter(name = "month", description = "月份", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:create')")
    public CommonResult<Integer> autoSchedule(
            @RequestParam("propertyId") Long propertyId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestBody List<Long> dutyUsers) {
        return success(fireDutyScheduleService.autoSchedule(propertyId, year, month, dutyUsers));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "下载排班导入模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<ImportScheduleDataModel> list = Arrays.asList(
                ImportScheduleDataModel.builder()
                        .scheduleDate(LocalDate.of(2026, 8, 4))
                        .shiftType("白班")
                        .shiftName("白班")
                        .dutyUserName("张三")
                        .dutyUserPhone("13800138000")
                        .build(),
                ImportScheduleDataModel.builder()
                        .scheduleDate(LocalDate.of(2026, 8, 4))
                        .shiftType("night")
                        .shiftName("夜班")
                        .dutyUserName("李四")
                        .dutyUserPhone("13900139000")
                        .build(),
                ImportScheduleDataModel.builder()
                        .scheduleDate(LocalDate.of(2026, 8, 5))
                        .shiftType("全天班")
                        .shiftName("全天班")
                        .dutyUserName("王五")
                        .dutyUserPhone("13700137000")
                        .build()
        );
        ExcelUtils.write(response, "排班导入模板.xls", "排班数据", ImportScheduleDataModel.class, list);
    }

    @PostMapping("/import-excel")
    @Operation(summary = "导入排班计划", description = "通过Excel文件批量导入排班数据")
    @Parameter(name = "propertyId", description = "所属物业ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:create')")
    public CommonResult<Map<String, Object>> importExcelSchedule(
            @RequestParam("propertyId") Long propertyId,
            @RequestParam("file") MultipartFile file) {
        return success(fireDutyScheduleService.importExcelSchedule(file, propertyId));
    }

}
