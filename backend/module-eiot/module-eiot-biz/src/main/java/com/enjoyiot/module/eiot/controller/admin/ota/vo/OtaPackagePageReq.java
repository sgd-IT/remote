package com.enjoyiot.module.eiot.controller.admin.ota.vo;


import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/6/27 22:09
 * @Description:
 */
@Schema(description = "OtaPackagePageReq")
@Data
public class OtaPackagePageReq extends PageParam {
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
     * 版本
     */
    private String version;


    /**
     * 模块
     */
    private String module;


    /**
     * 产品key
     */
    private String productKey;
}
