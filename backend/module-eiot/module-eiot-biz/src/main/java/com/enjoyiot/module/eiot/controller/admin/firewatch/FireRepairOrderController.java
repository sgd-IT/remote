package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.*;
import com.enjoyiot.module.eiot.service.firewatch.FireRepairOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Map;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 维修工单管理 Controller
 *
 * 功能概述：
 * - 手动创建维修工单
 * - 从故障告警自动生成维修工单
 * - 派单给企业（自动填充企业联系人作为指派人）
 * - 签单/开始维修/完成/关闭工单
 * - 工单查询、统计与评价
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 维修工单管理")
@RestController
@RequestMapping("/eiot/firewatch/repair-order")
@Validated
public class FireRepairOrderController {

    @Resource
    private FireRepairOrderService fireRepairOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建维修工单", description = "手动创建维修工单")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:create')")
    public CommonResult<Long> createRepairOrder(@Valid @RequestBody RepairOrderCreateReqVO reqVO) {
        return success(fireRepairOrderService.createRepairOrder(reqVO));
    }

    @PostMapping("/create-from-alert")
    @Operation(summary = "从故障告警生成维修工单", description = "根据告警记录ID自动生成维修工单")
    @Parameter(name = "alertRecordId", description = "告警记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:create')")
    public CommonResult<Long> createFromAlert(@RequestParam("alertRecordId") Long alertRecordId) {
        return success(fireRepairOrderService.createFromAlert(alertRecordId));
    }

    @PutMapping("/update")
    @Operation(summary = "更新维修工单", description = "更新维修工单信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:update')")
    public CommonResult<Boolean> updateRepairOrder(@Valid @RequestBody RepairOrderUpdateReqVO reqVO) {
        fireRepairOrderService.updateRepairOrder(reqVO);
        return success(true);
    }

    @PutMapping("/sign")
    @Operation(summary = "签单", description = "签收维修工单")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:assign')")
    public CommonResult<Boolean> signOrder(@RequestParam("id") Long id) {
        fireRepairOrderService.signOrder(id);
        return success(true);
    }

    @PutMapping("/assign")
    @Operation(summary = "派单", description = "指派给维修人员进行排查维修")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:assign')")
    public CommonResult<Boolean> assignOrder(@Valid @RequestBody RepairOrderAssignReqVO reqVO) {
        fireRepairOrderService.assignOrder(reqVO);
        return success(true);
    }

    @PutMapping("/start")
    @Operation(summary = "开始维修", description = "开始执行维修任务")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:assign')")
    public CommonResult<Boolean> startRepair(@RequestParam("id") Long id) {
        fireRepairOrderService.startRepair(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成维修", description = "维修完成后标记工单为已完成")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:complete')")
    public CommonResult<Boolean> completeOrder(
            @RequestParam("id") Long id,
            @RequestParam(value = "completeRemark", required = false) String completeRemark,
            @RequestParam(value = "completeImages", required = false) String completeImages) {
        fireRepairOrderService.completeOrder(id, completeRemark, completeImages);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭工单", description = "关闭维修工单")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:close')")
    public CommonResult<Boolean> closeOrder(@RequestParam("id") Long id) {
        fireRepairOrderService.closeOrder(id);
        return success(true);
    }

    @PutMapping("/rate")
    @Operation(summary = "评价工单", description = "对完成的维修工单进行满意度评价")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:query')")
    public CommonResult<Boolean> rateOrder(
            @RequestParam("id") Long id,
            @RequestParam("satisfaction") Integer satisfaction,
            @RequestParam(value = "suggestion", required = false) String suggestion) {
        fireRepairOrderService.rateOrder(id, satisfaction, suggestion);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取工单详情", description = "获取维修工单详细信息")
    @Parameter(name = "id", description = "工单ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:query')")
    public CommonResult<RepairOrderRespVO> getRepairOrder(@RequestParam("id") Long id) {
        return success(fireRepairOrderService.getRepairOrder(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询工单列表", description = "分页查询维修工单列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:query')")
    public CommonResult<PageResult<RepairOrderRespVO>> pageRepairOrders(@Valid RepairOrderPageReqVO pageReqVO) {
        return success(fireRepairOrderService.pageRepairOrders(pageReqVO));
    }

    @GetMapping("/statistics")
    @Operation(summary = "维修工单统计", description = "统计各状态工单数量")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:query')")
    public CommonResult<Map<String, Integer>> getStatistics() {
        return success(fireRepairOrderService.getOrderStatusStatistics());
    }

    @GetMapping("/overdue")
    @Operation(summary = "获取超期工单", description = "获取已超期未完成的工单列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:query')")
    public CommonResult<Integer> getOverdueCount() {
        return success(fireRepairOrderService.getOverdueOrderCount());
    }

}
