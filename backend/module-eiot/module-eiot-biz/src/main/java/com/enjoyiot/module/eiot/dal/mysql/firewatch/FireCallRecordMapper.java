package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireCallRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 话单通话记录 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireCallRecordMapper extends BaseMapperX<FireCallRecordDO> {
}
