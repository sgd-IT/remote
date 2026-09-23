package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "摄像头批量创建请求对象（ICC 自动获取，仅平台租户）")
@Data
public class VideoCameraBatchCreateReqVO {

    @Schema(description = "目标租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "目标租户不能为空")
    private Long tenantId;

    @Schema(description = "摄像头列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "摄像头列表不能为空")
    @Valid
    private List<VideoCameraCreateReqVO> cameras;
}
