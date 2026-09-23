package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.service.firewatch.FireDutyRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 值班记录管理 Controller
 *
 * 功能概述：
 * - 值班日志记录：记录值班期间的各项工作
 * - 巡查记录：记录设备巡查情况
 * - 交接班记录：记录交接班事项
 * - 告警处理记录：记录告警处理过程
 * - 记录查询：支持多条件查询历史记录
 * - 记录统计：统计各类记录数量
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 值班记录管理")
@RestController
@RequestMapping("/eiot/firewatch/duty-record")
@Validated
public class FireDutyRecordController {

    @Resource
    private FireDutyRecordService fireDutyRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建记录", description = "创建值班记录")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:create')")
    public CommonResult<Long> createRecord(@Valid @RequestBody FireDutyRecordCreateReqVO createReqVO) {
        return success(fireDutyRecordService.createRecord(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新记录", description = "更新值班记录")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:update')")
    public CommonResult<Boolean> updateRecord(@Valid @RequestBody FireDutyRecordUpdateReqVO updateReqVO) {
        fireDutyRecordService.updateRecord(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除记录", description = "删除值班记录")
    @Parameter(name = "id", description = "记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:delete')")
    public CommonResult<Boolean> deleteRecord(@RequestParam("id") Long id) {
        fireDutyRecordService.deleteRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取记录详情", description = "获取值班记录详细信息")
    @Parameter(name = "id", description = "记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<FireDutyRecordRespVO> getRecord(@RequestParam("id") Long id) {
        return success(fireDutyRecordService.getRecord(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询记录列表", description = "分页查询值班记录列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<PageResult<FireDutyRecordRespVO>> pageRecords(@Valid FireDutyRecordPageReqVO pageReqVO) {
        return success(fireDutyRecordService.pageRecords(pageReqVO));
    }

    @GetMapping("/list-by-schedule")
    @Operation(summary = "按排班查询记录", description = "获取指定排班的所有记录")
    @Parameter(name = "scheduleId", description = "排班ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<List<FireDutyRecordRespVO>> listRecordsBySchedule(@RequestParam("scheduleId") Long scheduleId) {
        return success(fireDutyRecordService.listRecordsBySchedule(scheduleId));
    }

    @GetMapping("/list-by-time-range")
    @Operation(summary = "按时间范围查询记录", description = "获取指定时间范围的记录")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "startTime", description = "开始时间", required = true)
    @Parameter(name = "endTime", description = "结束时间", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<List<FireDutyRecordRespVO>> listRecordsByTimeRange(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return success(fireDutyRecordService.listRecordsByTimeRange(propertyId, startTime, endTime));
    }

    @PostMapping("/handover")
    @Operation(summary = "创建交接班记录", description = "记录交接班事项")
    @Parameter(name = "scheduleId", description = "排班ID", required = true)
    @Parameter(name = "handoverContent", description = "交接内容", required = true)
    @Parameter(name = "nextDutyUserId", description = "下一班值班人ID")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:create')")
    public CommonResult<Long> createHandoverRecord(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("handoverContent") String handoverContent,
            @RequestParam(value = "nextDutyUserId", required = false) Long nextDutyUserId) {
        return success(fireDutyRecordService.createHandoverRecord(scheduleId, handoverContent, nextDutyUserId));
    }

    @PostMapping("/patrol")
    @Operation(summary = "创建巡查记录", description = "记录巡查情况")
    @Parameter(name = "scheduleId", description = "排班ID", required = true)
    @Parameter(name = "content", description = "巡查内容", required = true)
    @Parameter(name = "status", description = "状态: normal-正常, abnormal-异常")
    @Parameter(name = "attachments", description = "附件JSON")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:create')")
    public CommonResult<Long> createPatrolRecord(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("content") String content,
            @RequestParam(value = "status", required = false, defaultValue = "normal") String status,
            @RequestParam(value = "attachments", required = false) String attachments) {
        return success(fireDutyRecordService.createPatrolRecord(scheduleId, content, status, attachments));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取记录统计", description = "获取记录统计数据")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "startTime", description = "开始时间", required = true)
    @Parameter(name = "endTime", description = "结束时间", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty:query')")
    @TenantIgnore
    public CommonResult<Map<String, Object>> getRecordStatistics(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return success(fireDutyRecordService.getRecordStatistics(propertyId, startTime, endTime));
    }

}
