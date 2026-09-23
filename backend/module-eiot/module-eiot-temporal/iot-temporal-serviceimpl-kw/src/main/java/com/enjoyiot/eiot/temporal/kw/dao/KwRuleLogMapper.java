package com.enjoyiot.eiot.temporal.kw.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.enjoyiot.eiot.temporal.kw.model.KwRuleLog;
import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@DS("kwDataSource")
@Mapper
public interface KwRuleLogMapper extends BaseMapperX<KwRuleLog> {
}
