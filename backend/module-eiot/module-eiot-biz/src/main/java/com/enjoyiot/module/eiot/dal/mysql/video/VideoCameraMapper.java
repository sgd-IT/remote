package com.enjoyiot.module.eiot.dal.mysql.video;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoCameraMapper extends BaseMapperX<VideoCameraDO> {
}
