package com.enjoyiot.module.eiot.dal.mysql.brand;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionParamDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrandModelFunctionParamMapper extends BaseMapperX<BrandModelFunctionParamDO> {

    @Select("SELECT * FROM eiot_brand_model_function_param WHERE function_id = #{functionId} AND deleted = 0")
    List<BrandModelFunctionParamDO> selectByFunctionId(@Param("functionId") Long functionId);
}
