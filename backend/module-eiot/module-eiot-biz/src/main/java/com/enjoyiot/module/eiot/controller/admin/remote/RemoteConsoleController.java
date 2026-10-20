package com.enjoyiot.module.eiot.controller.admin.remote;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.*;
import com.enjoyiot.module.eiot.service.remote.RemoteConsoleService;
import com.enjoyiot.module.eiot.service.remote.RemoteOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 远程控制台")
@RestController
@RequestMapping("/eiot/remote-console")
public class RemoteConsoleController {

    @Resource
    private RemoteConsoleService remoteConsoleService;

    @Resource
    private RemoteOperationLogService remoteOperationLogService;

    @GetMapping("/device-tree")
    @Operation(summary = "获取设备树")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<List<RemoteConsoleDeviceTreeRespVO>> getDeviceTree() {
        return success(remoteConsoleService.getDeviceTree());
    }

    @GetMapping("/device/{deviceId}/detail")
    @Operation(summary = "获取设备详情")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<RemoteConsoleDeviceDetailRespVO> getDeviceDetail(
            @PathVariable("deviceId") Long deviceId) {
        return success(remoteConsoleService.getDeviceDetail(deviceId));
    }

    @GetMapping("/device/{deviceId}/params")
    @Operation(summary = "获取设备参数")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<List<RemoteConsoleDeviceParamRespVO>> getDeviceParams(
            @PathVariable("deviceId") Long deviceId) {
        return success(remoteConsoleService.getDeviceParams(deviceId));
    }

    @GetMapping("/device/{deviceId}/operation-logs")
    @Operation(summary = "获取设备操作日志")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<List<RemoteOperationLogRespVO>> getDeviceOperationLogs(
            @PathVariable("deviceId") Long deviceId,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return success(remoteOperationLogService.getRecentLogsByDeviceId(deviceId, limit));
    }

    @GetMapping("/device-status-stats")
    @Operation(summary = "获取设备状态统计")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<RemoteConsoleDeviceStatusStatsRespVO> getDeviceStatusStats() {
        return success(remoteConsoleService.getDeviceStatusStats());
    }

    @GetMapping("/console-persons")
    @Operation(summary = "获取右上角展示人员", description = "展示顺序：1. 展示岗位下的人员（按岗位顺序）；2. 当前时间值班人员（去重）")
    @Parameter(name = "tenantId", description = "租户ID")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<List<ConsolePersonRespVO>> getConsolePersons(
            @RequestParam("tenantId") Long tenantId) {
        return success(remoteConsoleService.getConsolePersons(tenantId));
    }

    @PostMapping("/device/{deviceId}/mode")
    @Operation(summary = "切换手自动模式")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:control')")
    public CommonResult<Boolean> switchDeviceMode(
            @PathVariable("deviceId") Long deviceId,
            @Valid @RequestBody SwitchDeviceModeReqVO reqVO) {
        return success(remoteConsoleService.switchDeviceMode(deviceId, reqVO));
    }

    @PostMapping("/valve/{action}")
    @Operation(summary = "智能空开控制（开闸/合闸）")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:control')")
    public CommonResult<Boolean> valveControl(
            @PathVariable("action") String action,
            @Valid @RequestBody ValveControlReqVO reqVO) {
        return success(remoteConsoleService.valveControl(action, reqVO));
    }

    @GetMapping("/valve/command/{deviceId}")
    @Operation(summary = "拉取并消费智能空开控制命令")
    @PermitAll // 外部空开控制服务（ValveCommandTask）轮询拉取，免登录；配合 platform.tenant.ignore-urls 忽略租户校验
    public CommonResult<ValveCommandRespVO> consumeValveCommand(
            @PathVariable("deviceId") Long deviceId) {
        return success(remoteConsoleService.consumeValveCommand(deviceId));
    }
}
