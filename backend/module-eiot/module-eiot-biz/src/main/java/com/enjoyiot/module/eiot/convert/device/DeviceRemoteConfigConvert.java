package com.enjoyiot.module.eiot.convert.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceRemoteConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DeviceRemoteConfigConvert {

    DeviceRemoteConfigConvert INSTANCE = Mappers.getMapper(DeviceRemoteConfigConvert.class);

    DeviceRemoteConfigRespVO convert(DeviceRemoteConfigDO bean);
}
