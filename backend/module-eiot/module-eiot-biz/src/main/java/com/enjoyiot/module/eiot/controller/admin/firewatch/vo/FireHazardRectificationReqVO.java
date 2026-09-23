package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 隐患整改提交 Request VO")
@Data
public class FireHazardRectificationReqVO {

    @Schema(description = "主键ID", required = true)
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "整改人ID", required = true)
    @NotNull(message = "整改人不能为空")
    private Long rectificationUserId;

    @Schema(description = "整改措施", required = true)
    @NotNull(message = "整改措施不能为空")
    private String measure;

    @Schema(description = "整改进度(0-100)", required = true)
    @NotNull(message = "整改进度不能为空")
    private Integer progress;

    @Schema(description = "整改说明")
    private String remark;

    @Schema(description = "整改照片(JSON数组)")
    private String photos;

    @Schema(description = "是否申请验收")
    private Boolean applyReview;

}
