package com.enjoyiot.module.eiot.controller.app.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FireDutySchedulePageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FireDutyScheduleRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FirePropertyRespVO;
import com.enjoyiot.module.eiot.service.firewatch.FireDutyScheduleService;
import com.enjoyiot.module.eiot.service.firewatch.FirePropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 用户 App - 值班管理（移动端入口）
 *
 * 直调 Service（与 PC 共用实现）：若经 admin 版 Controller 转发，方法级安全会触发它自己的
 * @PreAuthorize 二次拦截（要求菜单权限点），移动端账号会被挡下。
 * 权限口径：账号允许移动端即可访问（system_users.user_type 含移动位，即 2/3），不校验菜单权限点。
 * 注意：@TenantIgnore 与 admin 版逐方法对齐，改动时两边需同步核对。
 */
@Tag(name = "用户 App - 值班管理")
@RestController
@RequestMapping("/eiot/firewatch/duty-schedule")
@Validated
public class AppFireDutyScheduleController {

    @Resource
    private FireDutyScheduleService fireDutyScheduleService;

    @Resource
    private FirePropertyService firePropertyService;

    @GetMapping("/current-duty")
    @Operation(summary = "获取当前值班人员")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "tenantId", description = "租户ID（与propertyId二选一，自动转换为物业ID后过滤）")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    @TenantIgnore
    public CommonResult<List<FireDutyScheduleRespVO>> getCurrentDuty(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        // 未传物业ID时，尝试按租户ID定位物业（与 admin 版逻辑保持一致）
        if (propertyId == null && tenantId != null) {
            FirePropertyRespVO property = firePropertyService.getPropertyByTenantId(tenantId);
            propertyId = property != null ? property.getId() : null;
        }
        return success(fireDutyScheduleService.getCurrentDuty(propertyId));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询排班列表")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    @TenantIgnore
    public CommonResult<PageResult<FireDutyScheduleRespVO>> pageSchedules(@Valid FireDutySchedulePageReqVO pageReqVO) {
        return success(fireDutyScheduleService.pageSchedules(pageReqVO));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取排班统计")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "startDate", description = "开始日期（yyyy-MM-dd），不传时统计全部")
    @Parameter(name = "endDate", description = "结束日期（yyyy-MM-dd），不传时统计全部")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    @TenantIgnore
    public CommonResult<Map<String, Object>> getScheduleStatistics(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        return success(fireDutyScheduleService.getScheduleStatistics(propertyId, start, end));
    }

    @PutMapping("/check-in")
    @Operation(summary = "签到")
    @Parameter(name = "id", description = "排班ID", required = true)
    @Parameter(name = "checkInType", description = "签到方式: manual-手动, face-人脸, card-刷卡", required = true)
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<Boolean> checkIn(
            @RequestParam("id") Long id,
            @RequestParam("checkInType") String checkInType) {
        fireDutyScheduleService.checkIn(id, checkInType);
        return success(true);
    }

    @PutMapping("/check-out")
    @Operation(summary = "签退")
    @Parameter(name = "id", description = "排班ID", required = true)
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<Boolean> checkOut(@RequestParam("id") Long id) {
        fireDutyScheduleService.checkOut(id);
        return success(true);
    }

}
