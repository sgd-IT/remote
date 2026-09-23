package com.enjoyiot.module.eiot.dal.mysql.devicealert;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.module.eiot.controller.admin.devicealert.vo.DeviceAlertConfigPageReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.devicealert.DeviceAlertConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeviceAlertConfigMapper extends BaseMapperX<DeviceAlertConfigDO> {

    default PageResult<DeviceAlertConfigDO> selectPage(DeviceAlertConfigPageReqVO reqVO) {
        String productKey = reqVO.getProductKey();
        Long deviceId = reqVO.getDeviceId();
        String name = reqVO.getName();
        Integer status = reqVO.getStatus();

        return selectPage(reqVO, new LambdaQueryWrapperX<DeviceAlertConfigDO>()
                .eqIfPresent(DeviceAlertConfigDO::getProductKey, productKey)
                .eqIfPresent(DeviceAlertConfigDO::getDeviceId, deviceId)
                .likeIfPresent(DeviceAlertConfigDO::getName, name)
                .eqIfPresent(DeviceAlertConfigDO::getStatus, status)
                .orderByDesc(DeviceAlertConfigDO::getId));
    }

    default List<DeviceAlertConfigDO> selectByProductKey(String productKey) {
        return selectList(new LambdaQueryWrapperX<DeviceAlertConfigDO>()
                .eq(DeviceAlertConfigDO::getProductKey, productKey)
                .eq(DeviceAlertConfigDO::getStatus, 0));
    }

    default List<DeviceAlertConfigDO> selectByDeviceId(Long deviceId) {
        return selectList(new LambdaQueryWrapperX<DeviceAlertConfigDO>()
                .eq(DeviceAlertConfigDO::getDeviceId, deviceId)
                .eq(DeviceAlertConfigDO::getStatus, 0));
    }
}
