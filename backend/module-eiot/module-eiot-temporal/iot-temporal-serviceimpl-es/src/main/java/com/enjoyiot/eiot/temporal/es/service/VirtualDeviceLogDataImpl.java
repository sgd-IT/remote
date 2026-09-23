package com.enjoyiot.eiot.temporal.es.service;


import com.enjoyiot.eiot.com.enjoyiot.eiot.IVirtualDeviceLogData;
import com.enjoyiot.eiot.temporal.es.convert.EsVirtualLogConvert;
import com.enjoyiot.eiot.temporal.es.dao.VirtualDeviceLogRepository;
import com.enjoyiot.eiot.temporal.es.document.DocVirtualDeviceLog;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.virtualdevice.dto.VirtualDeviceLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class VirtualDeviceLogDataImpl implements IVirtualDeviceLogData {

    @Autowired
    private VirtualDeviceLogRepository virtualDeviceLogRepository;

    @Override
    public PageResult<VirtualDeviceLog> findByVirtualDeviceId(Long virtualDeviceId, int page, int size) {
        Page<DocVirtualDeviceLog> paged = virtualDeviceLogRepository
                .findByVirtualDeviceId(virtualDeviceId,
                        Pageable.ofSize(size).withPage(page - 1));
        return new PageResult<>( paged.getContent().stream()
                .map(o -> EsVirtualLogConvert.INSTANCE.convert(o))
                .collect(Collectors.toList()), paged.getTotalElements());
    }

    @Override
    public void add(VirtualDeviceLog log) {
        virtualDeviceLogRepository.save(EsVirtualLogConvert.INSTANCE.convertDoc(log));
    }
}
