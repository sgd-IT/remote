package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.excel.core.util.ExcelUtils;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.service.firewatch.FirePatrolTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 巡检任务管理 Controller
 *
 * 功能概述：
 * - 任务创建：支持日常巡检、专项检查、维保检查等多种类型
 * - 任务分配：将任务分配给指定巡检人员
 * - 进度跟踪：实时查看任务执行进度
 * - 点位管理：管理巡检点位，支持批量添加
 * - 结果记录：记录巡检结果，支持拍照上传
 * - 统计分析：统计任务完成情况和异常情况
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 巡检任务管理")
@RestController
@RequestMapping("/eiot/firewatch/patrol-task")
@Validated
public class FirePatrolTaskController {

    @Resource
    private FirePatrolTaskService firePatrolTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建任务", description = "创建巡检任务")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:create')")
    public CommonResult<Long> createTask(@Valid @RequestBody FirePatrolTaskCreateReqVO createReqVO) {
        return success(firePatrolTaskService.createTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新任务", description = "更新巡检任务信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:update')")
    public CommonResult<Boolean> updateTask(@Valid @RequestBody FirePatrolTaskUpdateReqVO updateReqVO) {
        firePatrolTaskService.updateTask(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除任务", description = "删除巡检任务")
    @Parameter(name = "id", description = "任务ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:delete')")
    public CommonResult<Boolean> deleteTask(@RequestParam("id") Long id) {
        firePatrolTaskService.deleteTask(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取任务详情", description = "获取巡检任务详细信息")
    @Parameter(name = "id", description = "任务ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:query')")
    public CommonResult<FirePatrolTaskRespVO> getTask(@RequestParam("id") Long id) {
        return success(firePatrolTaskService.getTask(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询任务列表", description = "分页查询巡检任务列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:query')")
    public CommonResult<PageResult<FirePatrolTaskRespVO>> pageTasks(@Valid FirePatrolTaskPageReqVO pageReqVO) {
        return success(firePatrolTaskService.pageTasks(pageReqVO));
    }

    @PutMapping("/start")
    @Operation(summary = "开始任务", description = "开始执行巡检任务")
    @Parameter(name = "id", description = "任务ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:execute')")
    public CommonResult<Boolean> startTask(@RequestParam("id") Long id) {
        firePatrolTaskService.startTask(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成任务", description = "完成巡检任务")
    @Parameter(name = "id", description = "任务ID", required = true)
    @Parameter(name = "summary", description = "巡检总结")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:execute')")
    public CommonResult<Boolean> completeTask(
            @RequestParam("id") Long id,
            @RequestParam(value = "summary", required = false) String summary) {
        firePatrolTaskService.completeTask(id, summary);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消任务", description = "取消巡检任务")
    @Parameter(name = "id", description = "任务ID", required = true)
    @Parameter(name = "reason", description = "取消原因")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:operate')")
    public CommonResult<Boolean> cancelTask(
            @RequestParam("id") Long id,
            @RequestParam(value = "reason", required = false) String reason) {
        firePatrolTaskService.cancelTask(id, reason);
        return success(true);
    }

    @GetMapping("/points")
    @Operation(summary = "获取任务点位列表", description = "获取巡检任务的所有点位")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:query')")
    public CommonResult<List<FirePatrolPointRespVO>> listTaskPoints(@RequestParam("taskId") Long taskId) {
        return success(firePatrolTaskService.listTaskPoints(taskId));
    }

    @PutMapping("/point/check")
    @Operation(summary = "检查点位", description = "检查巡检点位并记录结果")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:execute')")
    public CommonResult<Boolean> checkPoint(@Valid @RequestBody FirePatrolPointCheckReqVO checkReqVO) {
        firePatrolTaskService.checkPoint(checkReqVO);
        return success(true);
    }

    @PostMapping("/points/add")
    @Operation(summary = "添加巡检点位", description = "批量添加巡检点位到任务")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:update')")
    public CommonResult<Integer> addTaskPoints(
            @RequestParam("taskId") Long taskId,
            @RequestBody List<Long> deviceIds) {
        return success(firePatrolTaskService.addTaskPoints(taskId, deviceIds));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取任务统计", description = "获取巡检任务统计数据")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "startDate", description = "开始日期", required = true)
    @Parameter(name = "endDate", description = "结束日期", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:query')")
    public CommonResult<Map<String, Object>> getTaskStatistics(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return success(firePatrolTaskService.getTaskStatistics(propertyId, startDate, endDate));
    }

    @GetMapping("/user-pending-tasks")
    @Operation(summary = "获取用户待执行任务", description = "获取指定用户的待执行任务列表")
    @Parameter(name = "userId", description = "用户ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:query')")
    public CommonResult<List<FirePatrolTaskRespVO>> getUserPendingTasks(@RequestParam("userId") Long userId) {
        return success(firePatrolTaskService.getUserPendingTasks(userId));
    }

    @GetMapping("/today-pending-count")
    @Operation(summary = "获取今日待执行任务数", description = "统计今日待执行的巡检任务数量")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:query')")
    public CommonResult<Integer> countTodayPendingTasks() {
        return success(firePatrolTaskService.countTodayPendingTasks());
    }

    @PutMapping("/check-overdue")
    @Operation(summary = "检测超期任务", description = "将计划日期已过且状态为待执行的任务标记为已超期")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:execute')")
    @TenantIgnore
    public CommonResult<Integer> checkOverdueTasks() {
        return success(firePatrolTaskService.checkOverdueTasks());
    }

    @GetMapping("/export")
    @Operation(summary = "导出巡检任务", description = "导出巡检任务列表到Excel")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:query')")
    public void exportPatrolTasks(@Valid FirePatrolTaskPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(com.enjoyiot.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        PageResult<FirePatrolTaskRespVO> pageResult = firePatrolTaskService.pageTasks(pageReqVO);
        ExcelUtils.write(response, "巡检任务.xls", "巡检任务", FirePatrolTaskRespVO.class, pageResult.getList());
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "下载巡检任务导入模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<FirePatrolTaskImportVO> list = Arrays.asList(
                FirePatrolTaskImportVO.builder()
                        .taskName("示例任务")
                        .taskType("routine")
                        .planDate(LocalDate.now())
                        .patrolUserName("张三")
                        .patrolUserPhone("13800138000")
                        .remark("示例备注")
                        .build()
        );
        ExcelUtils.write(response, "巡检任务导入模板.xls", "巡检任务", FirePatrolTaskImportVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入巡检任务")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:patrol:create')")
    public CommonResult<Map<String, Object>> importPatrolTasks(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        List<FirePatrolTaskImportVO> list = ExcelUtils.read(file, FirePatrolTaskImportVO.class);
        return success(firePatrolTaskService.importTasks(list));
    }

}
