package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 物业/联网单位 Mapper
 *
 * 注意：租户过滤由 MyBatis-Plus 租户拦截器自动添加，SQL 中不手动写 tenant_id。
 * 值守端大屏 Controller 使用 @TenantIgnore 跳过拦截器，查全部数据。
 *
 * @author EnjoyIot
 */
@Mapper
public interface FirePropertyMapper extends BaseMapperX<FirePropertyDO> {

    @Select("SELECT risk_level, COUNT(*) as count FROM fire_property WHERE (deleted = 0 OR deleted IS NULL) GROUP BY risk_level")
    List<Map<String, Object>> countByRiskLevel();

    @Select("SELECT COUNT(*) FROM fire_property WHERE (deleted = 0 OR deleted IS NULL)")
    Integer countTotal();

}
