package com.enjoyiot.module.eiot.dal.mysql.brand;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrandModelFunctionMapper extends BaseMapperX<BrandModelFunctionDO> {

    @Select("SELECT * FROM eiot_brand_model_function WHERE brand_model_id = #{brandModelId} AND deleted = 0")
    List<BrandModelFunctionDO> selectByBrandModelId(@Param("brandModelId") Long brandModelId);
}
