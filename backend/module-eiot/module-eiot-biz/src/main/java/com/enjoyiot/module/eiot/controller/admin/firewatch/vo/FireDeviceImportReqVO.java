package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 消防设备导入 Request VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 消防设备导入 Request VO")
@Data
public class FireDeviceImportReqVO {

    @Schema(description = "物业名称")
    private String propertyName;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备序列号")
    private String deviceSn;

    @Schema(description = "设施分类编码")
    private String facilityCategory;

    @Schema(description = "生产厂家")
    private String manufacturer;

    @Schema(description = "设备型号")
    private String model;

    @Schema(description = "安装日期")
    private LocalDate installDate;

    @Schema(description = "安装位置")
    private String installLocation;

    @Schema(description = "所在楼层")
    private String floor;

    @Schema(description = "所属区域")
    private String area;

    @Schema(description = "保修到期日")
    private LocalDate warrantyExpireDate;

    @Schema(description = "备注")
    private String remark;

}
