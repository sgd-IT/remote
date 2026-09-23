package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageParam;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.excel.core.util.ExcelUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanUpdateReqVO;
import com.enjoyiot.module.eiot.service.firewatch.FireMaintenancePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 维保计划管理 Controller
 *
 * 功能概述：
 * - 创建维保计划
 * - 更新维保计划
 * - 删除维保计划
 * - 查询维保计划详情
 * - 分页查询维保计划
 * - 完成维保计划
 * - 获取逾期维保计划
 * - 统计逾期维保计划数
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 维保计划管理")
@RestController
@RequestMapping("/eiot/firewatch/plan")
@Validated
@Slf4j
public class FireMaintenancePlanController {

    @Resource
    private FireMaintenancePlanService fireMaintenancePlanService;

    @PostMapping("/create")
    @Operation(summary = "创建维保计划", description = "创建维保计划")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:plan:create')")
    public CommonResult<Long> createPlan(@Valid @RequestBody PlanCreateReqVO reqVO) {
        log.info("[createPlan][Controller] 收到创建维保计划请求: {}", reqVO);
        try {
            Long id = fireMaintenancePlanService.createPlan(reqVO);
            log.info("[createPlan][Controller] 创建维保计划成功, id={}", id);
            return success(id);
        } catch (Exception e) {
            log.error("[createPlan][Controller] 创建维保计划Controller异常", e);
            throw e;
        }
    }

    @PutMapping("/update")
    @Operation(summary = "更新维保计划", description = "更新维保计划信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:plan:update')")
    public CommonResult<Boolean> updatePlan(@Valid @RequestBody PlanUpdateReqVO reqVO) {
        fireMaintenancePlanService.updatePlan(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除维保计划", description = "删除维保计划")
    @Parameter(name = "id", description = "计划ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:plan:delete')")
    public CommonResult<Boolean> deletePlan(@RequestParam("id") Long id) {
        fireMaintenancePlanService.deletePlan(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取维保计划详情", description = "获取维保计划详细信息")
    @Parameter(name = "id", description = "计划ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:plan:query')")
    public CommonResult<PlanRespVO> getPlan(@RequestParam("id") Long id) {
        return success(fireMaintenancePlanService.getPlan(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询维保计划", description = "分页查询维保计划列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:plan:query')")
    public CommonResult<PageResult<PlanRespVO>> pagePlans(@Valid PlanPageReqVO pageReqVO) {
        return success(fireMaintenancePlanService.pagePlans(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出维保计划 Excel", description = "导出维保计划列表为Excel表格")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:plan:export')")
    public void exportPlanExcel(@Valid PlanPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<PlanRespVO> pageResult = fireMaintenancePlanService.pagePlans(pageReqVO);
        ExcelUtils.write(response, "维保计划.xls", "维保计划", PlanRespVO.class, pageResult.getList());
    }

    @PutMapping("/complete")
    @Operation(summary = "完成维保计划", description = "标记维保计划为已完成")
    @Parameter(name = "id", description = "计划ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:plan:update')")
    public CommonResult<Boolean> completePlan(@RequestParam("id") Long id) {
        fireMaintenancePlanService.completePlan(id);
        return success(true);
    }

    @GetMapping("/overdue")
    @Operation(summary = "获取逾期维保计划", description = "获取已逾期的维保计划列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:plan:query')")
    public CommonResult<List<PlanRespVO>> getOverduePlans() {
        return success(fireMaintenancePlanService.getOverduePlans());
    }

    @GetMapping("/overdue-count")
    @Operation(summary = "统计逾期维保计划数", description = "统计已逾期的维保计划数量")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:plan:query')")
    public CommonResult<Integer> countOverdue() {
        return success(fireMaintenancePlanService.countOverdue());
    }
}
