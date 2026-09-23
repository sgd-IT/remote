package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 隐患更新 Request VO")
@Data
public class FireHazardUpdateReqVO {

    @Schema(description = "主键ID", required = true)
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "隐患等级")
    private String hazardLevel;

    @Schema(description = "隐患描述")
    private String hazardDesc;

    @Schema(description = "隐患位置")
    private String location;

    @Schema(description = "关联设备ID")
    private Long deviceId;

    @Schema(description = "发现人")
    private String foundUserName;

    @Schema(description = "发现日期")
    private LocalDate foundDate;

    @Schema(description = "整改截止日期")
    private LocalDate deadline;

    @Schema(description = "整改建议")
    private String suggestion;

    @Schema(description = "现场照片(JSON数组)")
    private String photos;

    @Schema(description = "备注")
    private String remark;

}
