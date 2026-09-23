package com.enjoyiot.module.eiot.dal.mysql.remote;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RemoteOperationLogMapper extends BaseMapperX<RemoteOperationLogDO> {
}
