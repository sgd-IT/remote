package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitUpdateReqVO;
import com.enjoyiot.module.eiot.service.firewatch.FireMaintenanceUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 维保单位管理 Controller
 *
 * 功能概述：
 * - 创建维保单位
 * - 更新维保单位
 * - 删除维保单位
 * - 查询维保单位详情
 * - 分页查询维保单位
 * - 获取维保单位列表（不分页，用于下拉选择）
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 维保单位管理")
@RestController
@RequestMapping("/eiot/firewatch/maintenance-unit")
@Validated
public class FireMaintenanceUnitController {

    @Resource
    private FireMaintenanceUnitService fireMaintenanceUnitService;

    @PostMapping("/create")
    @Operation(summary = "创建维保单位", description = "创建维保单位")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:maintenance-unit:create')")
    public CommonResult<Long> createUnit(@Valid @RequestBody MaintenanceUnitCreateReqVO reqVO) {
        return success(fireMaintenanceUnitService.createUnit(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新维保单位", description = "更新维保单位信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:maintenance-unit:update')")
    public CommonResult<Boolean> updateUnit(@Valid @RequestBody MaintenanceUnitUpdateReqVO reqVO) {
        fireMaintenanceUnitService.updateUnit(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除维保单位", description = "删除维保单位")
    @Parameter(name = "id", description = "单位ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:maintenance-unit:delete')")
    public CommonResult<Boolean> deleteUnit(@RequestParam("id") Long id) {
        fireMaintenanceUnitService.deleteUnit(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取维保单位详情", description = "获取维保单位详细信息")
    @Parameter(name = "id", description = "单位ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:maintenance-unit:query')")
    public CommonResult<MaintenanceUnitRespVO> getUnit(@RequestParam("id") Long id) {
        return success(fireMaintenanceUnitService.getUnit(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询维保单位", description = "分页查询维保单位列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:maintenance-unit:query')")
    public CommonResult<PageResult<MaintenanceUnitRespVO>> pageUnits(@Valid MaintenanceUnitPageReqVO pageReqVO) {
        return success(fireMaintenanceUnitService.pageUnits(pageReqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "获取维保单位列表（不分页）", description = "获取维保单位列表，系统管理员看全部，普通租户只看自己的，用于下拉选择")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:maintenance-unit:query')")
    public CommonResult<List<MaintenanceUnitRespVO>> listUnits() {
        return success(fireMaintenanceUnitService.listUnits());
    }
}
