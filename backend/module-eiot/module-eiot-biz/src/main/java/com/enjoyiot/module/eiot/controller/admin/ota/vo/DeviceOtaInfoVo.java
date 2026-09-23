package com.enjoyiot.module.eiot.controller.admin.ota.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/6/17 20:49
 * @Description:
 */
@Data
@ExcelIgnoreUnannotated
@Schema(description = "DeviceOtaInfoVo")
public class DeviceOtaInfoVo implements Serializable {

    private Long id;

    private Long packageId;

    private String remark;

    private String version;

    private String module;

    private Integer total;

    private Integer success;

    private Integer fail;

    private String productKey;

    private LocalDateTime createTime;
}
