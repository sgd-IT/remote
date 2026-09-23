package com.enjoyiot.module.eiot.service.remote;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;

import java.util.List;

public interface RemoteOperationLogService {

    /**
     * 创建操作日志
     */
    Long createLog(RemoteOperationLogDO log);

    /**
     * 分页查询操作日志
     */
    PageResult<RemoteOperationLogRespVO> getOperationLogPage(RemoteOperationLogPageReqVO pageReqVO);

    /**
     * 查询设备的最近操作日志
     */
    List<RemoteOperationLogRespVO> getRecentLogsByDeviceId(Long deviceId, Integer limit);
}
