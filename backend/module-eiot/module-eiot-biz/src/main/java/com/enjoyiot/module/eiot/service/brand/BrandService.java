package com.enjoyiot.module.eiot.service.brand;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandCascadeRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandModelFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandRespVO;

import java.util.List;

public interface BrandService {

    PageResult<BrandRespVO> getBrandPage(int pageNo, int pageSize);

    void syncBrandData();

    List<BrandCascadeRespVO> getBrandCascade();

    List<BrandModelFunctionRespVO> getBrandModelFunctions(Long brandModelId);
}
