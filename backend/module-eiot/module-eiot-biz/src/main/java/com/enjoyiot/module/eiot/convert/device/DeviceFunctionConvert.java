package com.enjoyiot.module.eiot.convert.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface DeviceFunctionConvert {

    DeviceFunctionConvert INSTANCE = Mappers.getMapper(DeviceFunctionConvert.class);

    DeviceFunctionRespVO convert(DeviceFunctionDO bean);

    List<DeviceFunctionRespVO> convertList(List<DeviceFunctionDO> list);
}
