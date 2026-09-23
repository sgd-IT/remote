package com.enjoyiot.module.eiot.controller.admin.device;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.*;
import com.enjoyiot.module.eiot.service.device.DeviceFunctionService;
import com.enjoyiot.module.eiot.service.device.DeviceInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备功能管理")
@RestController
@RequestMapping("/eiot/device")
public class DeviceFunctionController {

    @Resource
    private DeviceFunctionService deviceFunctionService;
    @Resource
    private DeviceInstructionService deviceInstructionService;

    @GetMapping("/{deviceId}/function")
    @Operation(summary = "获取设备已绑定功能列表")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<List<DeviceFunctionRespVO>> getDeviceFunctions(
            @PathVariable("deviceId") Long deviceId) {
        return success(deviceFunctionService.getDeviceFunctions(deviceId));
    }

    @PostMapping("/{deviceId}/function")
    @Operation(summary = "新增设备功能")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:config')")
    public CommonResult<Long> addDeviceFunction(
            @PathVariable("deviceId") Long deviceId,
            @Valid @RequestBody DeviceFunctionSaveReqVO reqVO) {
        return success(deviceFunctionService.addDeviceFunction(deviceId, reqVO));
    }

    @PutMapping("/{deviceId}/function/{id}")
    @Operation(summary = "修改设备功能")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:config')")
    public CommonResult<Boolean> updateDeviceFunction(
            @PathVariable("id") Long id,
            @Valid @RequestBody DeviceFunctionSaveReqVO reqVO) {
        deviceFunctionService.updateDeviceFunction(id, reqVO);
        return success(true);
    }

    @DeleteMapping("/{deviceId}/function/{id}")
    @Operation(summary = "删除设备功能")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:config')")
    public CommonResult<Boolean> deleteDeviceFunction(@PathVariable("id") Long id) {
        deviceFunctionService.deleteDeviceFunction(id);
        return success(true);
    }

    @PostMapping("/instruction/send")
    @Operation(summary = "下发控制指令")
    @PreAuthorize("@ss.hasPermission('iot:device:instruction:send')")
    public CommonResult<InstructionSendRespVO> sendInstruction(
            @Valid @RequestBody InstructionSendReqVO reqVO) {
        return success(deviceInstructionService.sendInstruction(reqVO));
    }
}
