package com.enjoyiot.module.eiot.controller.admin.device;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigSaveReqVO;
import com.enjoyiot.module.eiot.service.device.DeviceRemoteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备远程配置")
@RestController
@RequestMapping("/eiot/device")
public class DeviceRemoteConfigController {

    @Resource
    private DeviceRemoteConfigService deviceRemoteConfigService;

    @GetMapping("/{deviceId}/remote-config")
    @Operation(summary = "获取设备远程配置")
    @PreAuthorize("@ss.hasPermission('iot:device:query')")
    public CommonResult<DeviceRemoteConfigRespVO> getRemoteConfig(
            @PathVariable("deviceId") Long deviceId) {
        return success(deviceRemoteConfigService.getRemoteConfig(deviceId));
    }

    @PostMapping("/{deviceId}/remote-config")
    @Operation(summary = "保存设备远程配置")
    @PreAuthorize("@ss.hasPermission('iot:device:remote:config')")
    public CommonResult<Boolean> saveRemoteConfig(
            @PathVariable("deviceId") Long deviceId,
            @Valid @RequestBody DeviceRemoteConfigSaveReqVO reqVO) {
        deviceRemoteConfigService.saveRemoteConfig(deviceId, reqVO);
        return success(true);
    }
}
