package com.enjoyiot.module.eiot.controller.app.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.AlarmOrderPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.AlarmOrderRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.AlarmRecordStatisticsRespVO;
import com.enjoyiot.module.eiot.service.firewatch.FireAlarmOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 用户 App - 火警处置中心（移动端入口）
 *
 * 直调 Service（与 PC 共用实现）：若经 admin 版 Controller 转发，方法级安全会触发它自己的
 * @PreAuthorize 二次拦截（要求菜单权限点），移动端账号会被挡下。
 * 权限口径：账号允许移动端即可访问（system_users.user_type 含移动位，即 2/3），不校验菜单权限点。
 */
@Tag(name = "用户 App - 火警处置中心")
@RestController
@RequestMapping("/eiot/firewatch/alarm-order")
@Validated
public class AppFireAlarmOrderController {

    @Resource
    private FireAlarmOrderService fireAlarmOrderService;

    @GetMapping("/get")
    @Operation(summary = "获取火警工单详情")
    @Parameter(name = "id", description = "工单ID", required = true)
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<AlarmOrderRespVO> getAlarmOrder(@RequestParam("id") Long id) {
        return success(fireAlarmOrderService.getAlarmOrder(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询火警工单")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<PageResult<AlarmOrderRespVO>> pageAlarmOrders(@Valid AlarmOrderPageReqVO pageReqVO) {
        return success(fireAlarmOrderService.pageAlarmOrders(pageReqVO));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取报警记录统计")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<AlarmRecordStatisticsRespVO> getAlarmRecordStatistics() {
        return success(fireAlarmOrderService.getAlarmRecordStatistics());
    }

}
