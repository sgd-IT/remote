package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigSaveReqVO;

public interface DeviceRemoteConfigService {

    DeviceRemoteConfigRespVO getRemoteConfig(Long deviceId);

    void saveRemoteConfig(Long deviceId, DeviceRemoteConfigSaveReqVO reqVO);
}
