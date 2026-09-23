package com.enjoyiot.module.eiot.controller.admin.product.vo;


import com.enjoyiot.eiot.common.validate.AddGroup;
import com.enjoyiot.eiot.common.validate.EditGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 产品配置业务对象 product_config
 *
 * @author EnjoyIot
 * @date 2024-06-19
 */
@Data
public class ProductConfigBo {

    /**
     * 产品key
     */
    @NotBlank(message = "产品key不能为空", groups = {AddGroup.class, EditGroup.class})
    @Schema(description = "产品key", required = true)
    private String productKey;

    /**
     * 配置信息
     */
//    @NotBlank(message = "配置信息不能为空", groups = { AddGroup.class, EditGroup.class })
    @Schema(description = "配置信息", required = true)
    private String config;

    /**
     * 配置项目(basic,interface,model,subBasic,subInterface,subModel)
     */
//    @NotBlank(message = "配置项目(basic,interface,model,subBasic,subInterface,subModel)不能为空", groups = { AddGroup.class, EditGroup.class })
    @Schema(description = "配置项目(basic,interface,model,subBasic,subInterface,subModel)", required = true)
    private String configItems;


}
