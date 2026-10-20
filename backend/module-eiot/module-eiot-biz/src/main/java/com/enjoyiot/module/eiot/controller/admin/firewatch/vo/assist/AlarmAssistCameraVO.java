package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 告警辅助判断 - 摄像头 VO
 *
 * @author sgd
 */
@Schema(description = "管理后台 - 告警辅助判断摄像头 VO")
@Data
public class AlarmAssistCameraVO {

    @Schema(description = "摄像头ID", example = "24")
    private Long cameraId;

    @Schema(description = "摄像头名称", example = "安为值班室前右")
    private String name;

    @Schema(description = "状态（1=在线）", example = "1")
    private Integer status;
}
