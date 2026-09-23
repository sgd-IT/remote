package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 值班记录更新 Request VO")
@Data
public class FireDutyRecordUpdateReqVO {

    @Schema(description = "记录ID", required = true)
    @NotNull(message = "记录ID不能为空")
    private Long id;

    @Schema(description = "记录标题")
    private String title;

    @Schema(description = "记录内容")
    private String content;

    @Schema(description = "附件(JSON数组)")
    private String attachments;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;

}
