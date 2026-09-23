package com.enjoyiot.module.eiot.controller.admin.ota.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/6/17 20:49
 * @Description:
 */
@Data
@ExcelIgnoreUnannotated
@Schema(description = "DeviceOtaDetailVo")
public class DeviceOtaDetailVo implements Serializable {
    private Long id;

    private Integer step;

    private String taskId;

    private String remark;

    private String version;

    private String module;

    private String deviceId;

    private String productKey;

    private String deviceName;

    private Long otaInfoId;
}
