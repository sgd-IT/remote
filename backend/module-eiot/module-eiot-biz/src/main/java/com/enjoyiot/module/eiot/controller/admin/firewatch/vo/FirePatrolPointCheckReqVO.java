package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理后台 - 巡检点位检查 Request VO")
@Data
public class FirePatrolPointCheckReqVO {

    @Schema(description = "点位ID", required = true)
    @NotNull(message = "点位ID不能为空")
    private Long id;

    @Schema(description = "巡检结果: normal-正常, abnormal-异常", required = true)
    @NotBlank(message = "巡检结果不能为空")
    private String result;

    @Schema(description = "问题描述")
    private String problemDesc;

    @Schema(description = "整改建议")
    private String suggestion;

    @Schema(description = "现场照片(JSON数组)")
    private String photos;

    @Schema(description = "备注")
    private String remark;

}
