package com.enjoyiot.module.eiot.controller.admin.brand;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandCascadeRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandModelFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandRespVO;
import com.enjoyiot.module.eiot.service.brand.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备品牌")
@RestController
@RequestMapping("/eiot/brand")
public class BrandController {

    @Resource
    private BrandService brandService;

    @GetMapping("/page")
    @Operation(summary = "品牌分页查询")
    @PreAuthorize("@ss.hasPermission('iot:brand:query')")
    public CommonResult<PageResult<BrandRespVO>> getBrandPage(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return success(brandService.getBrandPage(pageNo, pageSize));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步品牌数据")
    @PreAuthorize("@ss.hasPermission('iot:brand:sync')")
    public CommonResult<Boolean> syncBrandData() {
        brandService.syncBrandData();
        return success(true);
    }

    @GetMapping("/cascade")
    @Operation(summary = "获取品牌级联数据")
    @PreAuthorize("@ss.hasPermission('iot:brand:query')")
    public CommonResult<List<BrandCascadeRespVO>> getBrandCascade() {
        return success(brandService.getBrandCascade());
    }

    @GetMapping("/model/{brandModelId}/function")
    @Operation(summary = "获取型号下功能列表")
    @PreAuthorize("@ss.hasPermission('iot:brand:query')")
    public CommonResult<List<BrandModelFunctionRespVO>> getBrandModelFunctions(
            @PathVariable("brandModelId") Long brandModelId) {
        return success(brandService.getBrandModelFunctions(brandModelId));
    }
}
