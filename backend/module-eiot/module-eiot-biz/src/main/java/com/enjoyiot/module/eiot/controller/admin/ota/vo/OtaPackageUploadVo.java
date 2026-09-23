package com.enjoyiot.module.eiot.controller.admin.ota.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/6/27 22:35
 * @Description:
 */
@Data
@ExcelIgnoreUnannotated
@Schema(description = "OtaPackageUploadVo")
public class OtaPackageUploadVo implements Serializable {
    private String url;
    private Long size;
    private String md5;
    private Long ossId;
    private String originalName;
}
