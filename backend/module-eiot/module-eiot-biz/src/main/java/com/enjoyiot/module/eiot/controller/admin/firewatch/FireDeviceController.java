package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.string.StrUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.DeviceListItemRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.SubDeviceStatsRespVO;
import com.enjoyiot.module.eiot.service.firewatch.FireDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 消防设备管理 Controller（设备控制室专用）
 *
 * 提供按企业查询父设备、按父设备查询子设备的能力。
 * 权限码与设备控制室其他接口保持一致：eiot:firewatch:dashboard:query
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 消防设备管理")
@RestController
@RequestMapping("/eiot/firewatch/device")
@Validated
public class FireDeviceController {

    @Resource
    private FireDeviceService fireDeviceService;

    @GetMapping("/list")
    @Operation(summary = "获取企业父设备列表", description = "按物业ID查询该企业的父设备（parentId 为空）")
    @Parameter(name = "propertyId", description = "物业ID")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<List<DeviceListItemRespVO>> listDevices(
            @RequestParam(value = "propertyId", required = false) Long propertyId) {
        return success(fireDeviceService.listParentDevices(propertyId));
    }

    @GetMapping("/sub-devices")
    @Operation(summary = "分页获取父设备绑定的子设备列表", description = "按父设备ID分页查询其绑定的子设备，可选按线路类型过滤（多个类型用逗号分隔，如 0,1）")
    @Parameter(name = "parentId", description = "父设备ID", required = true)
    @Parameter(name = "lineTypes", description = "线路类型列表（0=多线, 1=总线），逗号分隔，可空")
    @Parameter(name = "pageNo", description = "页码，从 1 开始")
    @Parameter(name = "pageSize", description = "每页条数")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<PageResult<DeviceListItemRespVO>> listSubDevices(
            @RequestParam("parentId") Long parentId,
            @RequestParam(value = "lineTypes", required = false) String lineTypes,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return success(fireDeviceService.pageSubDevices(parentId, parseLineTypes(lineTypes), pageNo, pageSize));
    }

    @GetMapping("/sub-devices-by-line-type")
    @Operation(summary = "按线路类型分页获取子设备列表", description = "按父设备ID和线路类型（0=多线, 1=总线）分页查询子设备")
    @Parameter(name = "parentId", description = "父设备ID", required = true)
    @Parameter(name = "lineType", description = "线路类型：0=多线, 1=总线", required = true)
    @Parameter(name = "pageNo", description = "页码，从 1 开始")
    @Parameter(name = "pageSize", description = "每页条数")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<PageResult<DeviceListItemRespVO>> listSubDevicesByLineType(
            @RequestParam("parentId") Long parentId,
            @RequestParam("lineType") Integer lineType,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return success(fireDeviceService.pageSubDevices(parentId,
                lineType == null ? Collections.emptyList() : Collections.singletonList(lineType), pageNo, pageSize));
    }

    @GetMapping("/sub-devices/stats")
    @Operation(summary = "获取父设备子设备统计", description = "按父设备ID统计子设备总数与在线数")
    @Parameter(name = "parentId", description = "父设备ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:dashboard:query')")
    public CommonResult<SubDeviceStatsRespVO> getSubDeviceStats(
            @RequestParam("parentId") Long parentId) {
        return success(fireDeviceService.getSubDeviceStats(parentId));
    }

    /**
     * 解析逗号分隔的线路类型字符串
     *
     * @param lineTypes 逗号分隔字符串（如 "0,1"），可空
     * @return 线路类型列表，空串或 null 返回空列表
     */
    private List<Integer> parseLineTypes(String lineTypes) {
        if (lineTypes == null || lineTypes.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return StrUtils.splitToInteger(lineTypes.trim(), ",");
    }

}