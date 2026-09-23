package com.enjoyiot.module.eiot.controller.admin.iot;


import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.iot.vo.GetDeviceServicePropertyBo;
import com.enjoyiot.module.eiot.controller.admin.iot.vo.DeviceIdReqVo;
import com.enjoyiot.module.eiot.controller.admin.iot.vo.ServiceInvokeReqVo;
import com.enjoyiot.module.eiot.controller.admin.iot.vo.SetDeviceServicePropertyBo;
import com.enjoyiot.module.eiot.service.device.DeviceCtrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台-设备控制接口")
@Slf4j
@RestController
@RequestMapping("/eiot/device")
public class DeviceCtrlController {


    @Autowired
    private DeviceCtrlService deviceCtrlService;


    @Operation(summary = "服务调用")
    @PreAuthorize("@ss.hasPermission('iot:device:ctrl')")
    @PostMapping("/service/invoke")
    public CommonResult<String> invokeService(@RequestBody @Validated ServiceInvokeReqVo request) {
        deviceCtrlService.invokeService(request.getDeviceId(), request.getService(), request.getArgs());
        return success("");
    }

    @Operation(summary = "属性获取")
    @PreAuthorize("@ss.hasPermission('iot:device:ctrl')")
    @PostMapping("/service/property/get")
    public CommonResult<String> invokeServicePropertySet(@RequestBody @Validated GetDeviceServicePropertyBo request) {
        deviceCtrlService.getProperty(request.getDeviceId(), request.getPropertyNames(), true);
        return success("");
    }

    @Operation(summary = "属性设置")
    @PreAuthorize("@ss.hasPermission('iot:device:ctrl')")
    @PostMapping("/service/property/set")
    public CommonResult<String> setProperty(@RequestBody @Validated SetDeviceServicePropertyBo request) {
        deviceCtrlService.setProperty(request.getDeviceId(), request.getArgs());
        return success("");
    }

    /**
     * 设备配置下发
     */
    @Operation(summary = "设备配置下发")
    @PreAuthorize("@ss.hasPermission('iot:device:ctrl')")
    @PostMapping("/config/send")
    public CommonResult<String> sendConfig(@Validated @RequestBody DeviceIdReqVo reqVo) {
        deviceCtrlService.sendConfig(reqVo.getDeviceId());
        return success("");
    }


//    @Operation(summary = "模拟设备上报")
//    @PreAuthorize("@ss.hasPermission('iot:device:ctrl')")
//    @PostMapping("/simulateSend")
//    public boolean simulateSend(
//            @Validated @RequestBody ThingModelMessageBo bo) {
//        ThingModelMessage message = bo.to(ThingModelMessage.class);
//        return deviceServiceImpl.simulateSend(message);
//    }

}
