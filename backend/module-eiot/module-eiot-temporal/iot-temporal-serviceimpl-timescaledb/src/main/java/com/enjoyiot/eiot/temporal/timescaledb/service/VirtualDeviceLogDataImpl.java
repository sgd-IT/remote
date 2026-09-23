package com.enjoyiot.eiot.temporal.timescaledb.service;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enjoyiot.eiot.IVirtualDeviceLogData;
import com.enjoyiot.eiot.temporal.timescaledb.dao.PgVirtualDeviceLogMapper;
import com.enjoyiot.eiot.temporal.timescaledb.model.PgVirtualDeviceLog;
import com.enjoyiot.framework.common.pojo.PageParam;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.module.eiot.api.virtualdevice.dto.VirtualDeviceLog;
import org.postgresql.util.PGTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class VirtualDeviceLogDataImpl implements IVirtualDeviceLogData {

    @Autowired
    private PgVirtualDeviceLogMapper virtualDeviceLogMapper;

    @Override
    public PageResult<VirtualDeviceLog> findByVirtualDeviceId(Long virtualDeviceId, int page, int size) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(page);
        pageParam.setPageSize(size);
        PageResult<PgVirtualDeviceLog> result = virtualDeviceLogMapper.selectPage(pageParam,
                Wrappers.lambdaQuery(PgVirtualDeviceLog.class)
                        .eq(PgVirtualDeviceLog::getVirtualDeviceId, virtualDeviceId)
                        .orderByDesc(PgVirtualDeviceLog::getTime)
        );

        return new PageResult<>(result.getList().stream().map(r ->
                        new VirtualDeviceLog(r.getTime().getTime(), virtualDeviceId,
                                r.getVirtualDeviceName(),
                                r.getDeviceTotal(), r.getResult(), r.getTime().getTime()))
                .collect(Collectors.toList()), result.getTotal());
    }

    @Override
    public void add(VirtualDeviceLog log) {
        PgVirtualDeviceLog deviceLog = BeanUtils.toBean(log, PgVirtualDeviceLog.class);
        deviceLog.setTime(new PGTimestamp(System.currentTimeMillis()));
        virtualDeviceLogMapper.insert(deviceLog);
    }
}
