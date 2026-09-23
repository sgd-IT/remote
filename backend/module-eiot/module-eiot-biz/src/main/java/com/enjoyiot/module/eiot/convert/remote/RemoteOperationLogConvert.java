package com.enjoyiot.module.eiot.convert.remote;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface RemoteOperationLogConvert {

    RemoteOperationLogConvert INSTANCE = Mappers.getMapper(RemoteOperationLogConvert.class);

    RemoteOperationLogRespVO convert(RemoteOperationLogDO bean);

    List<RemoteOperationLogRespVO> convertList(List<RemoteOperationLogDO> list);

    PageResult<RemoteOperationLogRespVO> convertPage(PageResult<RemoteOperationLogDO> page);
}
