package com.enjoyiot.eiot.temporal.kw.service;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enjoyiot.eiot.IVirtualDeviceLogData;
import com.enjoyiot.eiot.temporal.kw.dao.KwVirtualDeviceLogMapper;
import com.enjoyiot.eiot.temporal.kw.model.KwVirtualDeviceLog;
import com.enjoyiot.framework.common.pojo.PageParam;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.module.eiot.api.virtualdevice.dto.VirtualDeviceLog;
import com.kaiwudb.util.KWTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class VirtualDeviceLogDataImpl implements IVirtualDeviceLogData {

    @Autowired
    private KwVirtualDeviceLogMapper virtualDeviceLogMapper;

    @Override
    public PageResult<VirtualDeviceLog> findByVirtualDeviceId(Long virtualDeviceId, int page, int size) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(page);
        pageParam.setPageSize(size);
        PageResult<KwVirtualDeviceLog> result = virtualDeviceLogMapper.selectPage(pageParam,
                Wrappers.lambdaQuery(KwVirtualDeviceLog.class)
                        .eq(KwVirtualDeviceLog::getVirtualDeviceId, virtualDeviceId)
                        .orderByDesc(KwVirtualDeviceLog::getTime)
        );

        return new PageResult<>(result.getList().stream().map(r ->
                        new VirtualDeviceLog(r.getTime().getTime(), virtualDeviceId,
                                r.getVirtualDeviceName(),
                                r.getDeviceTotal(), r.getResult(), r.getTime().getTime()))
                .collect(Collectors.toList()), result.getTotal());
    }

    @Override
    public void add(VirtualDeviceLog log) {
        KwVirtualDeviceLog deviceLog = BeanUtils.toBean(log, KwVirtualDeviceLog.class);
        deviceLog.setTime(new KWTimestamp(System.currentTimeMillis()));
        virtualDeviceLogMapper.insert(deviceLog);
    }
}
