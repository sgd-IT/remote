package com.enjoyiot.module.eiot.dal.mysql.protocol;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.protocol.VendorCommandDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VendorCommandMapper extends BaseMapperX<VendorCommandDO> {

    default VendorCommandDO selectByVendorAndKey(String vendorCode, String commandKey) {
        return selectOne(VendorCommandDO::getVendorCode, vendorCode,
                VendorCommandDO::getCommandKey, commandKey);
    }
}
