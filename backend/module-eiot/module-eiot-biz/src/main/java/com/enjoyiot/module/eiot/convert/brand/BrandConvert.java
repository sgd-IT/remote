package com.enjoyiot.module.eiot.convert.brand;

import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BrandConvert {

    BrandConvert INSTANCE = Mappers.getMapper(BrandConvert.class);

    BrandRespVO convert(BrandDO bean);

    List<BrandRespVO> convertList(List<BrandDO> list);
}
