package com.enjoyiot.module.eiot.controller.admin.product.vo;


import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * 产品配置视图对象 product_config
 *
 * @author EnjoyIot
 * @date 2024-06-19
 */
@Data
public class ProductConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产品key
     */
    @ExcelProperty(value = "产品key")
    @Schema(description = "产品key")
    private String productKey;

    /**
     * 配置信息
     */
    @ExcelProperty(value = "配置信息")
    @Schema(description = "配置信息")
    private String config;

    /**
     * 配置项目(basic,interface,model,subBasic,subInterface,subModel)
     */
    @ExcelProperty(value = "配置项目(basic,interface,model,subBasic,subInterface,subModel)")
    @Schema(description = "配置项目(basic,interface,model,subBasic,subInterface,subModel)")
    private String configItems;


}
