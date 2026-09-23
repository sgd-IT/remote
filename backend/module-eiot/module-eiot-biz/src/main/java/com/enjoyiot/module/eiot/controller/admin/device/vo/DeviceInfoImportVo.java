package com.enjoyiot.module.eiot.controller.admin.device.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = false) // 设置 chain = false，避免用户导入有问题
public class DeviceInfoImportVo {
    private static final long serialVersionUID = -1L;

    @ExcelProperty(value = "序列号")
    private String serialNo;

    @ExcelProperty(value = "设备名称")
    private String name;

    @Schema(description = "设备唯一标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "设备唯一标识不能为空")
    @ExcelProperty(value = "设备唯一标识")
    private String dn;

    @Schema(description = "设备序列号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "设备序列号不能为空")


    @ExcelProperty(value = "设备型号")
    private String model;

    @Schema(description = "设备地址")
    @ExcelProperty(value = "设备地址")
    private String addr;
}
