package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionSaveReqVO;

import java.util.List;

public interface DeviceFunctionService {

    List<DeviceFunctionRespVO> getDeviceFunctions(Long deviceId);

    Long addDeviceFunction(Long deviceId, DeviceFunctionSaveReqVO reqVO);

    void updateDeviceFunction(Long id, DeviceFunctionSaveReqVO reqVO);

    void deleteDeviceFunction(Long id);
}
