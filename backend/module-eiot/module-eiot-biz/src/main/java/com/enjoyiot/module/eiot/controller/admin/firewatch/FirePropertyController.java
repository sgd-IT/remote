package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.service.firewatch.FirePropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 物业单位管理 Controller
 *
 * 功能概述：
 * - 单位信息管理：维护联网单位基本信息
 * - 风险等级评估：根据单位类型、规模评估风险等级
 * - 位置管理：记录单位地理位置，支持地图展示
 * - 联系人管理：维护单位联系人信息
 * - 设备统计：统计各单位下的设备数量和状态
 *
 * @author EnjoyIot
 */
@Slf4j
@Tag(name = "管理后台 - 物业单位管理")
@RestController
@RequestMapping("/eiot/firewatch/property")
@Validated
public class FirePropertyController {

    @Resource
    private FirePropertyService firePropertyService;

    @PostMapping("/create")
    @Operation(summary = "创建物业", description = "创建物业单位")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:create')")
    public CommonResult<Long> createProperty(@Valid @RequestBody FirePropertyCreateReqVO createReqVO) {
        return success(firePropertyService.createProperty(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新物业", description = "更新物业单位信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:update')")
    @TenantIgnore
    public CommonResult<Boolean> updateProperty(@Valid @RequestBody FirePropertyUpdateReqVO updateReqVO) {
        try {
            firePropertyService.updateProperty(updateReqVO);
            return success(true);
        } catch (Exception e) {
            log.error("[updateProperty] 更新物业失败，ID: {}", updateReqVO.getId(), e);
            return CommonResult.error(500, e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除物业", description = "删除物业单位")
    @Parameter(name = "id", description = "物业ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:delete')")
    @TenantIgnore
    public CommonResult<Boolean> deleteProperty(@RequestParam("id") Long id) {
        firePropertyService.deleteProperty(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取物业详情", description = "获取物业单位详细信息")
    @Parameter(name = "id", description = "物业ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:query')")
    @TenantIgnore
    public CommonResult<FirePropertyRespVO> getProperty(@RequestParam("id") Long id) {
        return success(firePropertyService.getProperty(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询物业列表", description = "分页查询物业单位列表，admin看全部，企业只看自己租户")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:query')")
    public CommonResult<PageResult<FirePropertyRespVO>> pageProperties(@Valid FirePropertyPageReqVO pageReqVO) {
        return success(firePropertyService.pageProperties(pageReqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "获取物业列表", description = "获取物业列表（不分页），系统管理员看全部，普通租户只看自己的")
    @Parameter(name = "status", description = "状态")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:query')")
    public CommonResult<List<FirePropertyRespVO>> listProperties(
            @RequestParam(value = "status", required = false) Integer status) {
        return success(firePropertyService.listProperties(status));
    }

    @GetMapping("/list-all")
    @Operation(summary = "获取全部物业列表（跨租户）", description = "值守端大屏专用，仅系统管理员可访问")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:query') and @ss.hasRole('super_admin')")
    @TenantIgnore
    public CommonResult<PageResult<FirePropertyRespVO>> listAllProperties(@Valid FirePropertyPageReqVO pageReqVO) {
        return success(firePropertyService.pageProperties(pageReqVO));
    }

    @GetMapping("/own")
    @Operation(summary = "获取当前租户的企业信息", description = "企业账号获取自己所属租户的企业信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:query')")
    public CommonResult<FirePropertyRespVO> getOwnProperty() {
        Long tenantId = TenantContextHolder.getTenantId();
        return success(firePropertyService.getPropertyByTenantId(tenantId));
    }

    @PutMapping("/status")
    @Operation(summary = "更新物业状态", description = "启用或停用物业")
    @Parameter(name = "id", description = "物业ID", required = true)
    @Parameter(name = "status", description = "状态(0-停用,1-启用)", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:update')")
    @TenantIgnore
    public CommonResult<Boolean> updateStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        firePropertyService.updateStatus(id, status);
        return success(true);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取物业统计", description = "获取物业统计信息")
    @Parameter(name = "id", description = "物业ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:property:query')")
    @TenantIgnore
    public CommonResult<FirePropertyRespVO> getPropertyStatistics(@RequestParam("id") Long id) {
        return success(firePropertyService.getPropertyStatistics(id));
    }

}
