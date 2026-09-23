package com.enjoyiot.module.eiot.dal.mysql.device;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceRemoteConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceRemoteConfigMapper extends BaseMapperX<DeviceRemoteConfigDO> {

    @Select("SELECT * FROM eiot_device_remote_config WHERE device_id = #{deviceId} AND deleted = 0")
    DeviceRemoteConfigDO selectByDeviceId(@Param("deviceId") Long deviceId);
}
