package com.enjoyiot.module.eiot.controller.admin.ota.vo;





import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/6/27 22:09
 * @Description:
 */
@Schema(description = "OtaPackageBo")
@Data

public class OtaPackageBo {

    /**
     * 文件包大小
     */
    private Long size;

    /**
     * 签名
     */
    private String sign;

    /**
     * 是否差分升级
     */
    private Boolean isDiff;

    /**
     * 文件MD5后的值
     */
    private String md5;

    /**
     * 包名
     */
    private String name;

    /**
     * 描述
     */
    private String remark;

    /**
     * 版本
     */
    private String version;

    /**
     * 升级包地址
     */
    private String url;

    /**
     * 签名方式
     */
    private String signMethod;

    /**
     * 模块
     */
    private String module;

    /**
     * 扩展数据
     */
    private String extData;
    /**
     * 产品key
     */
    private String productKey;
}
