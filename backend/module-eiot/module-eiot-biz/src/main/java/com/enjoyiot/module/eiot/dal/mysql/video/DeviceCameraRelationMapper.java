package com.enjoyiot.module.eiot.dal.mysql.video;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.video.DeviceCameraRelationDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DeviceCameraRelationMapper extends BaseMapperX<DeviceCameraRelationDO> {

    @Select("SELECT * FROM device_camera_relation WHERE device_id = #{deviceId} AND deleted = 0")
    List<DeviceCameraRelationDO> selectListByDeviceId(@Param("deviceId") Long deviceId);

    @Select("<script>SELECT * FROM device_camera_relation WHERE deleted = 0 AND device_id IN " +
            "<foreach collection='deviceIds' item='item' open='(' separator=',' close=')'>#{item}</foreach></script>")
    List<DeviceCameraRelationDO> selectListByDeviceIds(@Param("deviceIds") Collection<Long> deviceIds);

    @Delete("DELETE FROM device_camera_relation WHERE id = #{id}")
    int deleteByIdPhysically(@Param("id") Long id);
}
