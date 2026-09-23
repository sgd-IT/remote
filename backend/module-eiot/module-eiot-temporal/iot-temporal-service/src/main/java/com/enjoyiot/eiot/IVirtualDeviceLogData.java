package com.enjoyiot.eiot;


import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.virtualdevice.dto.VirtualDeviceLog;

public interface IVirtualDeviceLogData {

    /**
     * 根据虚拟设备id分页查询日志
     *
     * @param virtualDeviceId
     * @param page
     * @param size
     * @return
     */
    PageResult<VirtualDeviceLog> findByVirtualDeviceId(Long virtualDeviceId, int page, int size);

    /**
     * 新增虚拟设备日志
     *
     * @param log
     */
    void add(VirtualDeviceLog log);
}
