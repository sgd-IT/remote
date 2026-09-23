package com.enjoyiot.module.eiot.api.product.dto;


import com.enjoyiot.module.eiot.api.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 产品配置对象 product_config
 *
 * @author huamu
 * @date 2024-06-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductConfig extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 产品key
     */
    private String productKey;

    /**
     * 配置信息
     */
    private String config;

    /**
     * 配置项目(basic,interface,model,subBasic,subInterface,subModel)
     */
    private String configItems;


}
