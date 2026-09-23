/*
 * Copyright [2025] [Enjoy-iot]
 * Licensed under the Apache License, Version 2.0
 */
package com.enjoyiot.module.eiot.controller.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "视频监控摄像头树节点（按企业分组）")
public class VideoCameraTreeRespVO {

    @Schema(description = "企业/租户ID")
    private Long id;

    @Schema(description = "企业名称")
    private String name;

    @Schema(description = "摄像头数量")
    private Integer cameraCount;

    @Schema(description = "摄像头列表")
    private List<CameraItem> cameras;

    @Data
    public static class CameraItem {
        @Schema(description = "摄像头ID")
        private Long id;

        @Schema(description = "摄像头名称")
        private String channelName;

        @Schema(description = "通道编码")
        private String channelId;

        @Schema(description = "在线状态：0=离线 1=在线")
        private Integer status;

        @Schema(description = "安装地址")
        private String address;

        @Schema(description = "设备编码（ICC 设备编码，WSPlayer 预览/对讲必填）")
        private String deviceId;

        @Schema(description = "码流类型：1-主码流，2-辅码流")
        private String quality;
    }
}
