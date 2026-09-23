package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "摄像头更新请求对象")
public class VideoCameraUpdateReqVO {

    @Schema(description = "摄像头ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "摄像头ID不能为空")
    private Long id;

    @Schema(description = "设备编码/录像机编码", example = "100001")
    private String deviceId;

    @Schema(description = "通道编码", example = "100001$1$0$0")
    private String channelId;

    @Schema(description = "摄像头名称", example = "一号通道")
    private String channelName;

    @Schema(description = "经度", example = "118.796877")
    private Double longitude;

    @Schema(description = "纬度", example = "32.060255")
    private Double latitude;

    @Schema(description = "安装地址", example = "南京市XX路XX号")
    private String address;

    @Schema(description = "备注", example = "NVR通道1")
    private String remark;

    @Schema(description = "码流类型：1-主码流，2-辅码流", example = "2")
    private String quality;

    @Schema(description = "拉流类型：0-海康威视，1-大华ICC", example = "0")
    private Integer pullType;

    @Schema(description = "摄像头类型：-1录像机,0普通,1漏铝,2AI人员统计,3热成像,4其他", example = "0")
    private Integer type;

    @Schema(description = "是否支持回放：0-不支持，1-支持", example = "1")
    private Integer supportReplay;
}
