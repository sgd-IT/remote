package com.enjoyiot.module.eiot.controller.app.cameraalert.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 摄像头报警上报数据（从 multipart 的 data JSON 参数解析）
 * <p>
 * 字段来源：摄像头设备端推送的报警事件数据
 */
@Schema(description = "用户 App - 摄像头报警上报数据")
@Data
public class CameraAlertReportReqVO {

    @Schema(description = "产品Key", example = "camera_product_001")
    private String productKey;

    @Schema(description = "事件类型: 1-三合一事件报警 2-热成像事件报警 3-烟火事件报警 4-离岗报警 5-通道堵塞报警", example = "3")
    private Integer eventType;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "设备序列号(devSno)", example = "CAM20250001")
    private String devSno;

    @Schema(description = "事件时间(时间戳)", example = "1722844800000")
    private Long eventDate;
}
