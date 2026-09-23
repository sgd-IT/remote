package com.enjoyiot.module.eiot.dal.mysql.device;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceFunctionMapper extends BaseMapperX<DeviceFunctionDO> {

    @Select("SELECT * FROM eiot_device_function WHERE device_id = #{deviceId} AND deleted = 0 AND enabled = 1 ORDER BY sort_order")
    List<DeviceFunctionDO> selectByDeviceId(@Param("deviceId") Long deviceId);
}
