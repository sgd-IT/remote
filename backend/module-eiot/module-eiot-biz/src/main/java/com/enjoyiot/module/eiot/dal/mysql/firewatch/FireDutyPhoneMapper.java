package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyPhoneDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 值班中心座机号码 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireDutyPhoneMapper extends BaseMapperX<FireDutyPhoneDO> {
}
