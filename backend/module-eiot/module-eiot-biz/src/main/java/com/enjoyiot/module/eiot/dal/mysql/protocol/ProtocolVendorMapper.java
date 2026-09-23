package com.enjoyiot.module.eiot.dal.mysql.protocol;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.protocol.ProtocolVendorDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProtocolVendorMapper extends BaseMapperX<ProtocolVendorDO> {

    default ProtocolVendorDO selectByCode(String vendorCode) {
        return selectOne(ProtocolVendorDO::getVendorCode, vendorCode);
    }
}
