package com.enjoyiot.module.eiot.controller.admin.device.vo.deviceconfig;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Schema(description = "DeviceConfigVo")
@Data
@ExcelIgnoreUnannotated
public class DeviceConfigVo implements Serializable {

    private static final long serialVersionUID = -1L;

    @Schema(description = "设备配置id")
    @ExcelProperty(value = "设备配置id")
    private String id;

    @Schema(description = "设备配置json内容")
    @ExcelProperty(value = "设备配置json内容")
    private String config;

    @Schema(description = "创建时间")
    @ExcelProperty(value = "创建时间")
    private Long createAt;

    @Schema(description = "设备id")
    @ExcelProperty(value = "设备id")
    private String deviceId;

    @Schema(description = "设备名称")
    @ExcelProperty(value = "设备名称")
    private String deviceName;

    @Schema(description = "产品key")
    @ExcelProperty(value = "产品key")
    private String productKey;


}
