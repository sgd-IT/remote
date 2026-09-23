package com.enjoyiot.module.eiot.controller.admin.video;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.dal.dataobject.video.DeviceCameraRelationDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.service.video.DeviceCameraRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 部件摄像头关联")
@RestController
@RequestMapping("/eiot/video/relation")
@Validated
public class DeviceCameraRelationController {

    @Resource
    private DeviceCameraRelationService deviceCameraRelationService;

    @PostMapping("/bind")
    @Operation(summary = "绑定部件与摄像头")
    public CommonResult<Boolean> bind(
            @RequestParam("deviceId") Long deviceId,
            @RequestParam("cameraId") Long cameraId) {
        deviceCameraRelationService.bind(deviceId, cameraId);
        return success(true);
    }

    @DeleteMapping("/unbind")
    @Operation(summary = "解绑")
    public CommonResult<Boolean> unbind(@RequestParam("id") Long id) {
        deviceCameraRelationService.unbind(id);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取部件关联的摄像头列表")
    public CommonResult<List<VideoCameraDO>> list(@RequestParam("deviceId") Long deviceId) {
        return success(deviceCameraRelationService.getCamerasByDevice(deviceId));
    }

    @GetMapping("/relations")
    @Operation(summary = "获取部件关联关系")
    public CommonResult<List<DeviceCameraRelationDO>> relations(@RequestParam("deviceId") Long deviceId) {
        return success(deviceCameraRelationService.getRelationsByDevice(deviceId));
    }
}
