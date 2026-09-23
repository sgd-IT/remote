package com.enjoyiot.module.eiot.service.remote;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogRespVO;
import com.enjoyiot.module.eiot.convert.remote.RemoteOperationLogConvert;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;
import com.enjoyiot.module.eiot.dal.mysql.remote.RemoteOperationLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RemoteOperationLogServiceImpl implements RemoteOperationLogService {

    @Resource
    private RemoteOperationLogMapper remoteOperationLogMapper;

    @Override
    public Long createLog(RemoteOperationLogDO log) {
        remoteOperationLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public PageResult<RemoteOperationLogRespVO> getOperationLogPage(RemoteOperationLogPageReqVO pageReqVO) {
        LambdaQueryWrapperX<RemoteOperationLogDO> wrapper = new LambdaQueryWrapperX<RemoteOperationLogDO>()
                .eqIfPresent(RemoteOperationLogDO::getDeviceId, pageReqVO.getDeviceId())
                .eqIfPresent(RemoteOperationLogDO::getPropertyId, pageReqVO.getPropertyId())
                .eqIfPresent(RemoteOperationLogDO::getOperationType, pageReqVO.getOperationType())
                .eqIfPresent(RemoteOperationLogDO::getOperatorId, pageReqVO.getOperatorId())
                .eqIfPresent(RemoteOperationLogDO::getCommandStatus, pageReqVO.getCommandStatus())
                .betweenIfPresent(RemoteOperationLogDO::getOperationTime, pageReqVO.getStartTime(), pageReqVO.getEndTime())
                .orderByDesc(RemoteOperationLogDO::getOperationTime);
        PageResult<RemoteOperationLogDO> pageResult = remoteOperationLogMapper.selectPage(pageReqVO, wrapper);
        return RemoteOperationLogConvert.INSTANCE.convertPage(pageResult);
    }

    @Override
    public List<RemoteOperationLogRespVO> getRecentLogsByDeviceId(Long deviceId, Integer limit) {
        LambdaQueryWrapperX<RemoteOperationLogDO> wrapper = new LambdaQueryWrapperX<RemoteOperationLogDO>()
                .eq(RemoteOperationLogDO::getDeviceId, deviceId)
                .orderByDesc(RemoteOperationLogDO::getOperationTime)
                .last("LIMIT " + limit);
        List<RemoteOperationLogDO> list = remoteOperationLogMapper.selectList(wrapper);
        return RemoteOperationLogConvert.INSTANCE.convertList(list);
    }
}
