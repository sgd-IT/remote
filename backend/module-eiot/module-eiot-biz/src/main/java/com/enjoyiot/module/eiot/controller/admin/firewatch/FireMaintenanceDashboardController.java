package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenancePlanDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireRepairOrderDO;
import com.enjoyiot.module.eiot.service.firewatch.FireRepairOrderService;
import com.enjoyiot.module.eiot.service.firewatch.FireMaintenanceContractService;
import com.enjoyiot.module.eiot.service.firewatch.FireMaintenancePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 维护保养仪表板 Controller
 *
 * 聚合展示维护保养相关统计数据，包括：
 * - 维修工单状态统计（待派单、维护中）
 * - 合同即将到期提醒
 * - 逾期计划提醒
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 维护保养仪表板")
@RestController
@RequestMapping("/eiot/firewatch/maintenance")
@Validated
public class FireMaintenanceDashboardController {

    @Resource
    private FireRepairOrderService fireRepairOrderService;

    @Resource
    private FireMaintenanceContractService contractService;

    @Resource
    private FireMaintenancePlanService planService;

    @GetMapping("/dashboard")
    @Operation(summary = "获取维护保养仪表板数据", description = "获取统计指标卡和告警提醒数据")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:query')")
    public CommonResult<Map<String, Object>> getDashboard() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 获取维修工单统计
        Map<String, Integer> orderStats = fireRepairOrderService.getOrderStatusStatistics();
        result.put("pendingDispatch", orderStats.getOrDefault("pending", 0));
        result.put("inMaintenance", orderStats.getOrDefault("in_progress", 0));
        result.put("pendingOrders", orderStats.getOrDefault("pending", 0));

        // 获取超期工单数
        result.put("overdueOrders", fireRepairOrderService.getOverdueOrderCount());

        // 获取合同即将到期数
        result.put("contractExpiring", contractService.countExpiring());

        // 获取逾期计划数
        result.put("overduePlans", planService.countOverdue());

        // 获取待催办数（超期但尚未发送催促通知的）
        result.put("pendingUrgeCount",
                fireRepairOrderService.countUnurgedOverdueOrders() +
                planService.countUnurgedOverduePlans());

        // 获取合同到期提醒列表
        List<Map<String, Object>> contractReminders = new ArrayList<>();
        List<ContractRespVO> expiringContracts = contractService.getExpiringContracts();
        for (ContractRespVO c : expiringContracts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("contractNo", c.getContractNo());
            item.put("contractName", c.getContractName());
            item.put("propertyName", c.getPropertyName());
            item.put("maintenanceUnit", c.getMaintenanceUnit());
            item.put("endDate", c.getEndDate());
            if (c.getEndDate() != null) {
                item.put("remainingDays", ChronoUnit.DAYS.between(LocalDateTime.now(), c.getEndDate()));
            } else {
                item.put("remainingDays", null);
            }
            contractReminders.add(item);
        }
        result.put("contractExpiryReminders", contractReminders);

        // 获取维保逾期提醒列表
        List<Map<String, Object>> overdueReminders = new ArrayList<>();
        List<PlanRespVO> overduePlans = planService.getOverduePlans();
        for (PlanRespVO p : overduePlans) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("planNo", p.getPlanNo());
            item.put("planName", p.getPlanName());
            item.put("propertyName", p.getPropertyName());
            item.put("planDate", p.getPlanDate());
            item.put("planStatus", p.getPlanStatus());
            overdueReminders.add(item);
        }
        result.put("overdueMaintenanceReminders", overdueReminders);

        return success(result);
    }

    @PostMapping("/send-urge")
    @Operation(summary = "发送催促执行通知", description = "对尚未催办的超期工单和逾期计划的企业联系人发送短信提醒")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:repair:query')")
    public CommonResult<Map<String, Object>> sendUrgeNotification() {
        // 只获取尚未催办的项目
        List<FireRepairOrderDO> unurgedOrders = fireRepairOrderService.getUnurgedOverdueOrderList();
        List<FireMaintenancePlanDO> unurgedPlans = planService.getUnurgedOverduePlanList();

        if (unurgedOrders.isEmpty() && unurgedPlans.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("successCount", 0);
            result.put("message", "所有超期项目已自动发送催办通知，无需重复发送");
            return success(result);
        }

        Map<String, Object> result = fireRepairOrderService.sendUrgeNotifications(unurgedOrders, unurgedPlans);
        return success(result);
    }

}
