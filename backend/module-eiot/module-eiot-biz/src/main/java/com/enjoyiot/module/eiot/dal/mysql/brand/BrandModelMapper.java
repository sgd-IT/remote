package com.enjoyiot.module.eiot.dal.mysql.brand;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrandModelMapper extends BaseMapperX<BrandModelDO> {

    @Select("SELECT * FROM eiot_brand_model WHERE brand_id = #{brandId} AND deleted = 0")
    List<BrandModelDO> selectByBrandId(@Param("brandId") Long brandId);
}
