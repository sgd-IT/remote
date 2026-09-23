package com.enjoyiot.module.eiot.dal.mysql.deviceconfig;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.module.eiot.dal.dataobject.deviceconfig.DeviceConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备配置 Mapper
 */
@Mapper
public interface DeviceConfigMapper extends BaseMapperX<DeviceConfigDO> {

    /**
     * 根据产品key和设备dn获取配置
     */
    default DeviceConfigDO selectByPkDn(String productKey, String dn) {
        return selectOne(new LambdaQueryWrapperX<DeviceConfigDO>()
                .eq(DeviceConfigDO::getProductKey, productKey)
                .eq(DeviceConfigDO::getDn, dn));
    }
}

