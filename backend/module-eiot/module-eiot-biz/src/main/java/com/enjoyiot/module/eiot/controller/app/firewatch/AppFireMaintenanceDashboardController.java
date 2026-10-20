package com.enjoyiot.module.eiot.controller.app.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanRespVO;
import com.enjoyiot.module.eiot.service.firewatch.FireMaintenanceContractService;
import com.enjoyiot.module.eiot.service.firewatch.FireMaintenancePlanService;
import com.enjoyiot.module.eiot.service.firewatch.FireRepairOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 用户 App - 维护保养仪表板（移动端入口）
 *
 * 直调 Service（与 PC 共用实现）：若经 admin 版 Controller 转发，方法级安全会触发它自己的
 * @PreAuthorize 二次拦截（要求菜单权限点），移动端账号会被挡下。
 * 权限口径：账号允许移动端即可访问（system_users.user_type 含移动位，即 2/3），不校验菜单权限点。
 * 注意：聚合逻辑与 admin 版 FireMaintenanceDashboardController#getDashboard 是两份拷贝，
 * 改统计口径时两边要同步，别只改一处。
 */
@Tag(name = "用户 App - 维护保养仪表板")
@RestController
@RequestMapping("/eiot/firewatch/maintenance")
@Validated
public class AppFireMaintenanceDashboardController {

    @Resource
    private FireRepairOrderService fireRepairOrderService;

    @Resource
    private FireMaintenanceContractService contractService;

    @Resource
    private FireMaintenancePlanService planService;

    @GetMapping("/dashboard")
    @Operation(summary = "获取维护保养仪表板数据")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
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

}
